package com.tcc.dermato.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.tcc.dermato.model.Consulta;
import com.tcc.dermato.model.ImagemDermatologica;
import com.tcc.dermato.model.ResultadoIA;
import com.tcc.dermato.repository.ConsultaRepository;
import com.tcc.dermato.repository.ImagemDermatologicaRepository;
import com.tcc.dermato.repository.ResultadoIARepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/imagens")
@Tag(name = "4. Imagens e IA", description = "Upload de imagens dermatológicas e integração com o microsserviço de IA")
public class ImagemController {

    private final ImagemDermatologicaRepository imagemRepository;
    private final ConsultaRepository consultaRepository;
    private final ResultadoIARepository resultadoIARepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${dermato.upload-dir:uploads}")
    private String uploadDir;

    @Value("${dermato.ai-service-url:http://localhost:8000/predict}")
    private String aiServiceUrl;

    public ImagemController(ImagemDermatologicaRepository imagemRepository,
                             ConsultaRepository consultaRepository,
                             ResultadoIARepository resultadoIARepository,
                             RestTemplate restTemplate) {
        this.imagemRepository = imagemRepository;
        this.consultaRepository = consultaRepository;
        this.resultadoIARepository = resultadoIARepository;
        this.restTemplate = restTemplate;
    }

    // Upload da imagem + chamada ao microservico de IA + persistencia do resultado
    @Operation(summary = "Enviar imagem da lesão e obter análise da IA",
            description = "Salva o arquivo em disco, envia a imagem ao microsserviço de IA (Python/FastAPI) e persiste o vetor de probabilidades retornado. O resultado nasce como não validado (validado = false).")
    @PostMapping(value = "/upload/{consultaId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(@PathVariable Long consultaId,
                                     @RequestParam("arquivo") MultipartFile arquivo) throws IOException {
        return ResponseEntity.ok(salvarEAnalisar(consultaId, arquivo));
    }

    // Rota documentada no Cap. 4: o consultaId vai como campo do formulario multipart,
    // junto com o arquivo. Mesmo comportamento de /upload/{consultaId}.
    @Operation(summary = "Enviar imagem da lesão (consultaId no formulário) e obter análise da IA",
            description = "Equivalente a POST /api/imagens/upload/{consultaId}, mas o ID da consulta vai como campo do formulário multipart. Campos: \"consultaId\" e \"arquivo\". O resultado nasce como não validado (validado = false).")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadComConsultaNoFormulario(@RequestParam("consultaId") Long consultaId,
                                                           @RequestParam("arquivo") MultipartFile arquivo) throws IOException {
        return ResponseEntity.ok(salvarEAnalisar(consultaId, arquivo));
    }

    // Logica comum as duas rotas de upload
    private ResultadoIA salvarEAnalisar(Long consultaId, MultipartFile arquivo) throws IOException {
        Consulta consulta = consultaRepository.findById(consultaId)
                .orElseThrow(() -> new RuntimeException("Consulta nao encontrada"));

        // 1. Salva o arquivo em disco
        Path dir = Paths.get(uploadDir);
        Files.createDirectories(dir);
        String nomeArquivo = UUID.randomUUID() + "_" + arquivo.getOriginalFilename();
        Path destino = dir.resolve(nomeArquivo);
        Files.write(destino, arquivo.getBytes());

        // 2. Persiste metadados da imagem.
        // Guardamos o caminho sempre com "/" (independente do SO) para que o frontend
        // consiga montar a URL de preview como "/" + caminhoArquivo -> "/uploads/arquivo.jpg"
        ImagemDermatologica imagem = new ImagemDermatologica();
        imagem.setConsulta(consulta);
        imagem.setCaminhoArquivo(uploadDir.replace("\\", "/") + "/" + nomeArquivo);
        imagem = imagemRepository.save(imagem);

        // 3. Envia a imagem ao microservico de IA (Python/FastAPI)
        String vetorProbabilidadesJson;
        try {
            vetorProbabilidadesJson = chamarIA(arquivo.getBytes(), arquivo.getOriginalFilename());
        } catch (Exception e) {
            // Se o microservico de IA estiver indisponivel, registra um resultado pendente
            vetorProbabilidadesJson = "{\"erro\":\"microservico de IA indisponivel\"}";
        }

        // 4. Persiste o resultado retornado pela IA (ainda nao validado pelo medico)
        ResultadoIA resultado = new ResultadoIA();
        resultado.setImagem(imagem);
        resultado.setVetorProbabilidades(vetorProbabilidadesJson);
        resultado.setValidado(false);
        return resultadoIARepository.save(resultado);
    }

    // Reanalise de uma imagem ja enviada (ex.: a IA estava fora do ar no upload,
    // ou o modelo foi atualizado). Nunca altera um resultado ja validado pelo medico.
    @Operation(summary = "Reanalisar uma imagem já enviada",
            description = "Reenvia ao microsserviço de IA o arquivo salvo da imagem {id} e atualiza o vetor de probabilidades. "
                    + "Recusado se o diagnóstico já foi validado pelo médico, se o arquivo não existir mais no servidor ou se a IA estiver indisponível (nesses casos o resultado anterior é mantido).")
    @PostMapping("/analisar/{id}")
    public ResponseEntity<?> analisar(@PathVariable Long id) throws IOException {
        ImagemDermatologica imagem = imagemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Imagem nao encontrada"));

        ResultadoIA resultado = resultadoIARepository.findByImagemId(id).orElseGet(() -> {
            ResultadoIA novo = new ResultadoIA();
            novo.setImagem(imagem);
            return novo;
        });
        if (resultado.isValidado()) {
            return ResponseEntity.badRequest().body(Map.of("erro",
                    "Esta imagem ja tem diagnostico validado pelo medico e nao pode ser reanalisada."));
        }

        // No Render o disco e efemero: o arquivo pode ter sumido apos um reinicio
        Path caminho = Paths.get(imagem.getCaminhoArquivo());
        if (!Files.exists(caminho)) {
            return ResponseEntity.badRequest().body(Map.of("erro",
                    "O arquivo desta imagem nao esta mais disponivel no servidor. Envie a foto novamente."));
        }

        String vetorProbabilidadesJson;
        try {
            vetorProbabilidadesJson = chamarIA(Files.readAllBytes(caminho), caminho.getFileName().toString());
        } catch (Exception e) {
            // Diferente do upload: aqui mantemos o resultado anterior em vez de sobrescrever com erro
            return ResponseEntity.badRequest().body(Map.of("erro",
                    "Microservico de IA indisponivel. Tente novamente em instantes."));
        }

        resultado.setVetorProbabilidades(vetorProbabilidadesJson);
        resultado.setDataResultado(LocalDateTime.now());
        return ResponseEntity.ok(resultadoIARepository.save(resultado));
    }

    // Envia a imagem ao microservico de IA (POST multipart, campo "arquivo")
    // e devolve o vetor de probabilidades em JSON (texto)
    private String chamarIA(byte[] conteudo, String nomeArquivo) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("arquivo", new ByteArrayResource(conteudo) {
            @Override
            public String getFilename() {
                return nomeArquivo;
            }
        });
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        @SuppressWarnings("unchecked")
        Map<String, Object> respostaIA = restTemplate.postForObject(aiServiceUrl, requestEntity, Map.class);
        return objectMapper.writeValueAsString(respostaIA);
    }

    @Operation(summary = "Listar imagens de uma consulta")
    @GetMapping("/consulta/{consultaId}")
    public Iterable<ImagemDermatologica> listarPorConsulta(@PathVariable Long consultaId) {
        return imagemRepository.findByConsultaId(consultaId);
    }
}
