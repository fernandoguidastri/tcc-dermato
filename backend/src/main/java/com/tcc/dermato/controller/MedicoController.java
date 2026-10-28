package com.tcc.dermato.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.tcc.dermato.dto.CadastroMedicoRequest;
import com.tcc.dermato.model.Medico;
import com.tcc.dermato.repository.MedicoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medicos")
@Tag(name = "7. Médicos", description = "Cadastro e consulta de médicos")
public class MedicoController {

    private final MedicoRepository medicoRepository;
    private final AuthController authController;

    public MedicoController(MedicoRepository medicoRepository, AuthController authController) {
        this.medicoRepository = medicoRepository;
        this.authController = authController;
    }

    // Rota documentada no Cap. 4. Reaproveita a mesma logica de POST /api/auth/cadastro
    // (validacoes, e-mail unico, senha em BCrypt). Liberada sem login no SecurityConfig.
    @Operation(summary = "Cadastrar médico",
            description = "Endpoint público, equivalente a POST /api/auth/cadastro. Corpo: {\"nome\", \"email\", \"senha\", \"crm\"}.")
    @SecurityRequirements
    @PostMapping
    public ResponseEntity<?> cadastrar(@RequestBody CadastroMedicoRequest req) {
        return authController.cadastrar(req);
    }

    @Operation(summary = "Listar médicos cadastrados")
    @GetMapping
    public List<Medico> listar() {
        return medicoRepository.findAll();
    }

    @Operation(summary = "Buscar médico por ID",
            description = "Retorna os dados do médico (o hash da senha nunca é enviado). 404 se o ID não existir.")
    @GetMapping("/{id}")
    public ResponseEntity<Medico> buscar(@PathVariable Long id) {
        return medicoRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
