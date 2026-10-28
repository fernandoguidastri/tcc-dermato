package com.tcc.dermato.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "medicos")
public class Medico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    // armazenada em hash (BCrypt). WRITE_ONLY: nunca aparece nas respostas JSON
    // (lista de medicos, consultas que carregam o medico, etc.)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false)
    private String senha;

    @Column(nullable = false, unique = true)
    private String crm;

    private LocalDateTime criadoEm = LocalDateTime.now();
}
