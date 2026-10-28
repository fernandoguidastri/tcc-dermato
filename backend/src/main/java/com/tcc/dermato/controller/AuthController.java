package com.tcc.dermato.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import com.tcc.dermato.dto.CadastroMedicoRequest;
import com.tcc.dermato.dto.LoginRequest;
import com.tcc.dermato.dto.LoginResponse;
import com.tcc.dermato.model.Medico;
import com.tcc.dermato.repository.MedicoRepository;
import com.tcc.dermato.security.JwtUtil;
import com.tcc.dermato.validacao.ValidadorDados;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "1. Autenticação", description = "Cadastro e login de médicos (endpoints públicos)")
@SecurityRequirements
public class AuthController {

    private final MedicoRepository medicoRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(MedicoRepository medicoRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.medicoRepository = medicoRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    // Cadastro de medico (para o prototipo, sem restricao de acesso)
    @Operation(summary = "Cadastrar médico",
            description = "Todos os campos sao obrigatorios. E-mail com formato valido, senha com no minimo "
                    + ValidadorDados.SENHA_TAMANHO_MINIMO + " caracteres; e-mail e CRM nao podem estar cadastrados.")
    @PostMapping("/cadastro")
    public ResponseEntity<?> cadastrar(@RequestBody CadastroMedicoRequest req) {
        if (ValidadorDados.vazio(req.getNome()) || ValidadorDados.vazio(req.getEmail())
                || ValidadorDados.vazio(req.getSenha()) || ValidadorDados.vazio(req.getCrm())) {
            return ResponseEntity.badRequest().body(java.util.Map.of("erro", "Preencha todos os campos: nome, e-mail, senha e CRM."));
        }
        if (!ValidadorDados.emailValido(req.getEmail())) {
            return ResponseEntity.badRequest().body(java.util.Map.of("erro", "E-mail invalido. Use o formato nome@exemplo.com."));
        }
        if (!ValidadorDados.senhaValida(req.getSenha())) {
            return ResponseEntity.badRequest().body(java.util.Map.of("erro",
                    "A senha deve ter no minimo " + ValidadorDados.SENHA_TAMANHO_MINIMO + " caracteres."));
        }
        String email = req.getEmail().trim();
        String crm = req.getCrm().trim();
        if (medicoRepository.existsByEmail(email)) {
            return ResponseEntity.badRequest().body(java.util.Map.of("erro", "Este e-mail ja esta cadastrado."));
        }
        if (medicoRepository.existsByCrm(crm)) {
            return ResponseEntity.badRequest().body(java.util.Map.of("erro", "Este CRM ja esta cadastrado."));
        }
        Medico medico = new Medico();
        medico.setNome(req.getNome().trim());
        medico.setEmail(email);
        medico.setSenha(passwordEncoder.encode(req.getSenha()));
        medico.setCrm(crm);
        medicoRepository.save(medico);
        return ResponseEntity.ok().body(java.util.Map.of("mensagem", "Medico cadastrado com sucesso."));
    }

    @Operation(summary = "Autenticar médico e obter token JWT")
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        var medicoOpt = medicoRepository.findByEmail(req.getEmail());

        if (medicoOpt.isEmpty() || !passwordEncoder.matches(req.getSenha(), medicoOpt.get().getSenha())) {
            return ResponseEntity.status(401).body(java.util.Map.of("erro", "E-mail ou senha invalidos."));
        }

        Medico medico = medicoOpt.get();
        String token = jwtUtil.gerarToken(medico.getEmail());
        return ResponseEntity.ok(new LoginResponse(token, medico.getNome(), medico.getEmail()));
    }
}
