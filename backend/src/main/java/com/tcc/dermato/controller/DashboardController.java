package com.tcc.dermato.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.dermato.model.ResultadoIA;
import com.tcc.dermato.repository.ConsultaRepository;
import com.tcc.dermato.repository.ImagemDermatologicaRepository;
import com.tcc.dermato.repository.PacienteRepository;
import com.tcc.dermato.repository.ResultadoIARepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Consolida os dados brutos cadastrados no sistema (pacientes, consultas,
 * imagens e resultados de IA) em indicadores (KPIs) para apoiar a tomada de
 * decisao do medico - conforme exigido no capitulo 5.2.3 (Dashboard).
 */
@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "6. Dashboard", description = "Indicadores (KPIs) para tomada de decisão")
public class DashboardController {

    private final PacienteRepository pacienteRepository;
    private final ConsultaRepository consultaRepository;
    private final ImagemDermatologicaRepository imagemRepository;
    private final ResultadoIARepository resultadoIARepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DashboardController(PacienteRepository pacienteRepository,
                                ConsultaRepository consultaRepository,
                                ImagemDermatologicaRepository imagemRepository,
                                ResultadoIARepository resultadoIARepository) {
        this.pacienteRepository = pacienteRepository;
        this.consultaRepository = consultaRepository;
        this.imagemRepository = imagemRepository;
        this.resultadoIARepository = resultadoIARepository;
    }

    @Operation(summary = "Resumo de KPIs do dashboard",
            description = "Totais de pacientes, consultas e imagens; diagnósticos validados x pendentes; e distribuição das categorias mais prováveis indicadas pela IA.")
    @GetMapping("/resumo")
    public Map<String, Object> resumo() {
        List<ResultadoIA> todosResultados = resultadoIARepository.findAll();

        long totalPacientes = pacienteRepository.count();
        long totalConsultas = consultaRepository.count();
        long totalImagens = imagemRepository.count();
        long totalValidados = todosResultados.stream().filter(ResultadoIA::isValidado).count();
        long totalPendentes = todosResultados.size() - totalValidados;

        // Para cada resultado da IA, identifica a categoria com maior probabilidade
        // (ex: "Melanoma": 0.62 -> vence) e conta quantas vezes cada categoria "venceu".
        // Isso alimenta o grafico de distribuicao de suspeitas no dashboard.
        Map<String, Long> distribuicaoDiagnosticoIA = new LinkedHashMap<>();
        for (ResultadoIA resultado : todosResultados) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> probs = objectMapper.readValue(resultado.getVetorProbabilidades(), Map.class);
                String categoriaMaisProvavel = null;
                double maiorValor = -1;
                for (var entrada : probs.entrySet()) {
                    if (entrada.getValue() instanceof Number numero && numero.doubleValue() > maiorValor) {
                        maiorValor = numero.doubleValue();
                        categoriaMaisProvavel = entrada.getKey();
                    }
                }
                if (categoriaMaisProvavel != null) {
                    distribuicaoDiagnosticoIA.merge(categoriaMaisProvavel, 1L, Long::sum);
                }
            } catch (Exception e) {
                // resultado sem vetor valido (ex: IA indisponivel no momento do upload) - ignora na estatistica
            }
        }

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("totalPacientes", totalPacientes);
        resposta.put("totalConsultas", totalConsultas);
        resposta.put("totalImagens", totalImagens);
        resposta.put("totalValidados", totalValidados);
        resposta.put("totalPendentes", totalPendentes);
        resposta.put("distribuicaoDiagnosticoIA", distribuicaoDiagnosticoIA);
        return resposta;
    }
}
