package com.tcc.dermato.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.tcc.dermato.dto.ValidacaoDiagnosticoRequest;
import com.tcc.dermato.model.ResultadoIA;
import com.tcc.dermato.repository.ResultadoIARepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/resultados")
@Tag(name = "5. Resultados da IA", description = "Consulta e validação médica (Human-in-the-loop)")
public class ResultadoIAController {

    private final ResultadoIARepository resultadoIARepository;

    public ResultadoIAController(ResultadoIARepository resultadoIARepository) {
        this.resultadoIARepository = resultadoIARepository;
    }

    @Operation(summary = "Buscar resultado da IA por ID")
    @GetMapping("/{id}")
    public ResponseEntity<ResultadoIA> buscar(@PathVariable Long id) {
        return resultadoIARepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Usado pela tela de historico: a partir do id da imagem, recupera o resultado da IA associado
    @Operation(summary = "Buscar resultado da IA a partir do ID da imagem")
    @GetMapping("/imagem/{imagemId}")
    public ResponseEntity<ResultadoIA> buscarPorImagem(@PathVariable Long imagemId) {
        return resultadoIARepository.findByImagemId(imagemId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Etapa "human-in-the-loop": o medico confirma/corrige o diagnostico sugerido pela IA
    @Operation(summary = "Validar diagnóstico (Human-in-the-loop)",
            description = "O médico confirma ou corrige a sugestão da IA. Corpo esperado: {\"diagnosticoValidado\": \"texto\"}. "
                    + "Responde 400 {\"erro\"} se o diagnóstico vier vazio.")
    @PutMapping("/{id}/validar")
    public ResponseEntity<?> validar(@PathVariable Long id, @RequestBody ValidacaoDiagnosticoRequest req) {
        String diagnostico = req.getDiagnosticoValidado() == null ? "" : req.getDiagnosticoValidado().trim();
        if (diagnostico.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("erro", "Informe o diagnóstico final antes de validar."));
        }
        return resultadoIARepository.findById(id).<ResponseEntity<?>>map(resultado -> {
            resultado.setDiagnosticoValidado(diagnostico);
            resultado.setValidado(true);
            resultado.setDataValidacao(LocalDateTime.now());
            return ResponseEntity.ok(resultadoIARepository.save(resultado));
        }).orElse(ResponseEntity.notFound().build());
    }
}
