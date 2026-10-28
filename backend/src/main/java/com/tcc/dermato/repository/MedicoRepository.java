package com.tcc.dermato.repository;

import com.tcc.dermato.model.Medico;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MedicoRepository extends JpaRepository<Medico, Long> {
    Optional<Medico> findByEmail(String email);
    boolean existsByEmail(String email);

    boolean existsByCrm(String crm);
}
