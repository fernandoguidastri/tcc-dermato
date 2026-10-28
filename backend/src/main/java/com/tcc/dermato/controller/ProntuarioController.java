package com.tcc.dermato.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.tcc.dermato.repository.PacienteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

// O prontuario nao e uma tabela propria: e a visao consolidada do paciente
// (dados cadastrais + consultas + imagens + resultado da IA e diagnostico validado).
@RestController
@RequestMapping("/api/prontuarios")
@Tag(name = "8. Prontuários", description = "Visão consolidada do paciente: dados, consultas, imagens e resultados")
public class ProntuarioController {

    private final PacienteRepository pacienteRepository;
    private final ConsultaController consultaController;

    public ProntuarioController(PacienteRepository pacienteRepository, ConsultaController consultaController) {
        this.pacienteRepository = pacienteRepository;
        this.consultaController = consultaController;
    }

    // Reaproveita a mesma montagem de GET /api/consultas/paciente/{id}/detalhado
    @Operation(summary = "Obter prontuário do paciente",
            description = "Retorna {\"paciente\": {...}, \"consultas\": [{\"consulta\", \"imagens\": [{\"imagem\", \"resultado\"}]}]}. "
                    + "O resultado traz o vetor de probabilidades da IA (apoio) e, quando houver, o diagnóstico validado pelo médico. 404 se o paciente não existir.")
    @GetMapping("/{pacienteId}")
    public ResponseEntity<?> obter(@PathVariable Long pacienteId) {
        return pacienteRepository.findById(pacienteId)
                .<ResponseEntity<?>>map(paciente -> {
                    Map<String, Object> prontuario = new LinkedHashMap<>();
                    prontuario.put("paciente", paciente);
                    prontuario.put("consultas", consultaController.listarDetalhadoPorPaciente(pacienteId));
                    return ResponseEntity.ok(prontuario);
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", "Paciente nao encontrado")));
    }
}
