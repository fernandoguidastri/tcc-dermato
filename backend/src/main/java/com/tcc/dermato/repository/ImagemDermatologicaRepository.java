package com.tcc.dermato.repository;

import com.tcc.dermato.model.ImagemDermatologica;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ImagemDermatologicaRepository extends JpaRepository<ImagemDermatologica, Long> {
    List<ImagemDermatologica> findByConsultaId(Long consultaId);
}
