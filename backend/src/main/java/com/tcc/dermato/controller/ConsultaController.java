package com.tcc.dermato.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.tcc.dermato.model.Consulta;
import com.tcc.dermato.model.ImagemDermatologica;
import com.tcc.dermato.model.Medico;
import com.tcc.dermato.model.Paciente;
import com.tcc.dermato.repository.ConsultaRepository;
import com.tcc.dermato.repository.ImagemDermatologicaRepository;
import com.tcc.dermato.repository.MedicoRepository;
import com.tcc.dermato.repository.PacienteRepository;
import com.tcc.dermato.repository.ResultadoIARepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/consultas")
@Tag(name = "3. Consultas", description = "Registro e histórico de consultas")
public class ConsultaController {

    private final ConsultaRepository consultaRepository;
    private final PacienteRepository pacienteRepository;
    private final MedicoRepository medicoRepository;
    private final ImagemDermatologicaRepository imagemRepository;
    private final ResultadoIARepository resultadoIARepository;

    public ConsultaController(ConsultaRepository consultaRepository,
                               PacienteRepository pacienteRepository,
                               MedicoRepository medicoRepository,
                               ImagemDermatologicaRepository imagemRepository,
                               ResultadoIARepository resultadoIARepository) {
        this.consultaRepository = consultaRepository;
        this.pacienteRepository = pacienteRepository;
        this.medicoRepository = medicoRepository;
        this.imagemRepository = imagemRepository;
        this.resultadoIARepository = resultadoIARepository;
    }

    // Sem parametro: lista todas (comportamento original, usado pelo front).
    // Com ?pacienteId=: historico do paciente, conforme o Cap. 4.
    @Operation(summary = "Listar consultas (todas ou de um paciente)",
            description = "Sem parâmetros, lista todas as consultas. Com ?pacienteId=1, retorna apenas o histórico de consultas desse paciente.")
    @GetMapping
    public List<Consulta> listar(@RequestParam(required = false) Long pacienteId) {
        if (pacienteId != null) {
            return consultaRepository.findByPacienteId(pacienteId);
        }
        return consultaRepository.findAll();
    }

    @Operation(summary = "Listar consultas de um paciente")
    @GetMapping("/paciente/{pacienteId}")
    public List<Consulta> listarPorPaciente(@PathVariable Long pacienteId) {
        return consultaRepository.findByPacienteId(pacienteId);
    }

    // Usado pela tela de historico: para cada consulta do paciente, ja traz
    // as imagens enviadas e o respectivo resultado da IA (evita N chamadas soltas no frontend).
    @Operation(summary = "Histórico detalhado do paciente (consultas + imagens + resultado da IA)")
    @GetMapping("/paciente/{pacienteId}/detalhado")
    public List<Map<String, Object>> listarDetalhadoPorPaciente(@PathVariable Long pacienteId) {
        List<Consulta> consultas = consultaRepository.findByPacienteId(pacienteId);
        List<Map<String, Object>> resposta = new ArrayList<>();

        for (Consulta consulta : consultas) {
            List<ImagemDermatologica> imagens = imagemRepository.findByConsultaId(consulta.getId());
            List<Map<String, Object>> imagensComResultado = new ArrayList<>();

            for (ImagemDermatologica imagem : imagens) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("imagem", imagem);
                resultadoIARepository.findByImagemId(imagem.getId())
                        .ifPresent(resultado -> item.put("resultado", resultado));
                imagensComResultado.add(item);
            }

            Map<String, Object> consultaDetalhada = new LinkedHashMap<>();
            consultaDetalhada.put("consulta", consulta);
            consultaDetalhada.put("imagens", imagensComResultado);
            resposta.add(consultaDetalhada);
        }

        return resposta;
    }

    // Corpo esperado: { "pacienteId": 1, "observacoes": "..." }
    @Operation(summary = "Registrar nova consulta",
            description = "Corpo esperado: {\"pacienteId\": 1, \"observacoes\": \"texto livre\"}. O médico responsável é o usuário autenticado (token JWT).")
    @PostMapping
    public ResponseEntity<?> criar(@RequestBody Map<String, Object> body) {
        if (body.get("pacienteId") == null) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Informe o paciente (pacienteId)."));
        }
        Long pacienteId = Long.valueOf(body.get("pacienteId").toString());
        String observacoes = (String) body.getOrDefault("observacoes", "");
        return ResponseEntity.ok(registrarConsulta(pacienteId, observacoes));
    }

    // Rota documentada no Cap. 4: o {id} da URL e o ID do paciente.
    // Corpo opcional: { "observacoes": "..." }
    @Operation(summary = "Registrar nova consulta para o paciente informado na URL",
            description = "Equivalente a POST /api/consultas, mas o ID do paciente vai na URL. Corpo opcional: {\"observacoes\": \"texto livre\"}. O médico responsável é o usuário autenticado (token JWT).")
    @PostMapping("/{id}")
    public ResponseEntity<?> criarParaPaciente(@PathVariable("id") Long pacienteId,
                                               @RequestBody(required = false) Map<String, Object> body) {
        String observacoes = body != null ? (String) body.getOrDefault("observacoes", "") : "";
        return ResponseEntity.ok(registrarConsulta(pacienteId, observacoes));
    }

    // Logica comum as duas rotas de cadastro de consulta
    private Consulta registrarConsulta(Long pacienteId, String observacoes) {
        Paciente paciente = pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new RuntimeException("Paciente nao encontrado"));

        // medico logado (via token JWT -> email no SecurityContext)
        String emailMedico = SecurityContextHolder.getContext().getAuthentication().getName();
        Medico medico = medicoRepository.findByEmail(emailMedico)
                .orElseThrow(() -> new RuntimeException("Medico nao encontrado"));

        Consulta consulta = new Consulta();
        consulta.setPaciente(paciente);
        consulta.setMedico(medico);
        consulta.setObservacoes(observacoes);

        return consultaRepository.save(consulta);
    }
}
