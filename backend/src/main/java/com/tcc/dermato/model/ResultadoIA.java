package com.tcc.dermato.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "resultados_ia")
public class ResultadoIA {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "imagem_id", nullable = false, unique = true)
    private ImagemDermatologica imagem;

    // vetor de probabilidades em JSON, ex: {"Melanoma":0.12,"Nevo":0.85,"Ceratose":0.03}
    @Column(length = 1000, nullable = false)
    private String vetorProbabilidades;

    // diagnostico final definido pelo medico (human-in-the-loop)
    private String diagnosticoValidado;

    private boolean validado = false;

    private LocalDateTime dataResultado = LocalDateTime.now();
    private LocalDateTime dataValidacao;
}
