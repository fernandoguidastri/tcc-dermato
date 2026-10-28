package com.tcc.dermato.repository;

import com.tcc.dermato.model.ResultadoIA;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ResultadoIARepository extends JpaRepository<ResultadoIA, Long> {
    Optional<ResultadoIA> findByImagemId(Long imagemId);
}
