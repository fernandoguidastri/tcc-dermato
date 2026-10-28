package com.tcc.dermato.repository;

import com.tcc.dermato.model.Paciente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {
    boolean existsByCpf(String cpf);

    // Busca por nome (sem diferenciar maiusculas/minusculas) ou por CPF.
    // O CPF e comparado sem pontuacao, entao "111222" encontra "111.222.333-44".
    // "digitos" vem vazio quando o termo nao tem numeros (ai so o nome e comparado).
    @Query("SELECT p FROM Paciente p WHERE "
            + "LOWER(p.nome) LIKE LOWER(CONCAT('%', :termo, '%')) "
            + "OR (:digitos <> '' AND REPLACE(REPLACE(p.cpf, '.', ''), '-', '') LIKE CONCAT('%', :digitos, '%'))")
    Page<Paciente> buscar(@Param("termo") String termo, @Param("digitos") String digitos, Pageable pageable);
}
