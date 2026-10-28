package com.tcc.dermato.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.tcc.dermato.model.Paciente;
import com.tcc.dermato.repository.PacienteRepository;
import com.tcc.dermato.validacao.ValidadorDados;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/pacientes")
@Tag(name = "2. Pacientes", description = "CRUD de pacientes")
public class PacienteController {

    private final PacienteRepository pacienteRepository;

    public PacienteController(PacienteRepository pacienteRepository) {
        this.pacienteRepository = pacienteRepository;
    }

    // Campos aceitos em "ordenar" (lista fechada para nao expor qualquer coluna)
    private static final Set<String> CAMPOS_ORDENACAO = Set.of("nome", "criadoEm", "dataNascimento");
    private static final int TAMANHO_MAXIMO = 50;

    @Operation(summary = "Listar pacientes (com busca, ordenação e paginação opcionais)",
            description = "Sem o parâmetro \"pagina\", retorna a lista completa (comportamento original). "
                    + "Com \"pagina\", retorna {\"conteudo\": [...], \"pagina\", \"tamanho\", \"totalElementos\", \"totalPaginas\"}. "
                    + "A busca procura no nome ou no CPF (com ou sem pontuação).")
    @GetMapping
    public ResponseEntity<?> listar(
            @Parameter(description = "Trecho do nome ou do CPF") @RequestParam(required = false) String busca,
            @Parameter(description = "Página, começando em 0. Se omitido, retorna a lista completa") @RequestParam(required = false) Integer pagina,
            @Parameter(description = "Itens por página (1 a 50)") @RequestParam(defaultValue = "10") int tamanho,
            @Parameter(description = "Campo de ordenação: nome, criadoEm ou dataNascimento") @RequestParam(defaultValue = "nome") String ordenar,
            @Parameter(description = "Direção: asc ou desc") @RequestParam(defaultValue = "asc") String direcao) {

        if (pagina == null) {
            return ResponseEntity.ok(pacienteRepository.findAll());
        }
        if (pagina < 0 || tamanho < 1 || tamanho > TAMANHO_MAXIMO) {
            return ResponseEntity.badRequest().body(Map.of("erro",
                    "Pagina deve ser 0 ou maior e tamanho deve estar entre 1 e " + TAMANHO_MAXIMO + "."));
        }
        if (!CAMPOS_ORDENACAO.contains(ordenar)) {
            return ResponseEntity.badRequest().body(Map.of("erro",
                    "Ordenacao invalida. Use: nome, criadoEm ou dataNascimento."));
        }
        if (!direcao.equalsIgnoreCase("asc") && !direcao.equalsIgnoreCase("desc")) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Direcao invalida. Use: asc ou desc."));
        }

        // Desempate pelo id para a ordem ser estavel entre paginas
        Sort.Direction dir = Sort.Direction.fromString(direcao);
        Pageable pageable = PageRequest.of(pagina, tamanho, Sort.by(dir, ordenar).and(Sort.by(dir, "id")));

        String termo = busca == null ? "" : busca.trim();
        Page<Paciente> resultado = termo.isEmpty()
                ? pacienteRepository.findAll(pageable)
                : pacienteRepository.buscar(termo, termo.replaceAll("\\D", ""), pageable);

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("conteudo", resultado.getContent());
        resposta.put("pagina", resultado.getNumber());
        resposta.put("tamanho", resultado.getSize());
        resposta.put("totalElementos", resultado.getTotalElements());
        resposta.put("totalPaginas", resultado.getTotalPages());
        return ResponseEntity.ok(resposta);
    }

    @Operation(summary = "Buscar paciente por ID")
    @GetMapping("/{id}")
    public ResponseEntity<Paciente> buscar(@PathVariable Long id) {
        return pacienteRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Cadastrar paciente (CPF deve ser único)",
            description = "Nome e CPF obrigatorios. O CPF precisa ter digitos verificadores validos e pode ser "
                    + "enviado com ou sem pontuacao (e gravado como 000.000.000-00). E-mail, se informado, "
                    + "precisa ter formato valido; data de nascimento nao pode ser no futuro.")
    @PostMapping
    public ResponseEntity<?> criar(@RequestBody Paciente paciente) {
        String erro = ValidadorDados.erroDadosPaciente(paciente.getNome(), paciente.getEmail(), paciente.getDataNascimento());
        if (erro == null && ValidadorDados.vazio(paciente.getCpf())) erro = "O CPF do paciente e obrigatorio.";
        if (erro == null && !ValidadorDados.cpfValido(paciente.getCpf())) {
            erro = "CPF invalido. Confira os 11 numeros digitados.";
        }
        if (erro != null) {
            return ResponseEntity.badRequest().body(Map.of("erro", erro));
        }
        paciente.setCpf(ValidadorDados.formatarCpf(paciente.getCpf()));
        if (pacienteRepository.existsByCpf(paciente.getCpf())) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Este CPF ja esta cadastrado."));
        }
        paciente.setId(null); // um "id" vindo no JSON nao pode sobrescrever outro paciente
        normalizarTextos(paciente);
        return ResponseEntity.ok(pacienteRepository.save(paciente));
    }

    @Operation(summary = "Atualizar dados do paciente",
            description = "Atualiza nome, data de nascimento, telefone e e-mail (mesmas validacoes do cadastro). "
                    + "O CPF nao pode ser alterado e e ignorado se vier no corpo.")
    @PutMapping("/{id}")
    public ResponseEntity<?> atualizar(@PathVariable Long id, @RequestBody Paciente dados) {
        String erro = ValidadorDados.erroDadosPaciente(dados.getNome(), dados.getEmail(), dados.getDataNascimento());
        if (erro != null) {
            return ResponseEntity.badRequest().body(Map.of("erro", erro));
        }
        return pacienteRepository.findById(id).<ResponseEntity<?>>map(p -> {
            p.setNome(dados.getNome());
            p.setTelefone(dados.getTelefone());
            p.setEmail(dados.getEmail());
            p.setDataNascimento(dados.getDataNascimento());
            normalizarTextos(p);
            return ResponseEntity.ok(pacienteRepository.save(p));
        }).orElse(ResponseEntity.notFound().build());
    }

    // Tira espacos das pontas e grava campos opcionais vazios como null
    private void normalizarTextos(Paciente p) {
        p.setNome(p.getNome().trim());
        p.setTelefone(ValidadorDados.vazio(p.getTelefone()) ? null : p.getTelefone().trim());
        p.setEmail(ValidadorDados.vazio(p.getEmail()) ? null : p.getEmail().trim());
    }

    @Operation(summary = "Excluir paciente (bloqueado se houver consultas vinculadas)")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> remover(@PathVariable Long id) {
        pacienteRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
