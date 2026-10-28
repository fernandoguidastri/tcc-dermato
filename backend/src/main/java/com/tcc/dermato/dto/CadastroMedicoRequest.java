package com.tcc.dermato.dto;

import lombok.Data;

@Data
public class CadastroMedicoRequest {
    private String nome;
    private String email;
    private String senha;
    private String crm;
}
