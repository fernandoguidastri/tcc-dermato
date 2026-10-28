package com.tcc.dermato.validacao;

import java.time.LocalDate;
import java.util.regex.Pattern;

/**
 * Regras de validacao usadas pelos controllers (as mesmas regras existem no
 * front, em js/api.js, para o erro aparecer antes de enviar o formulario).
 */
public final class ValidadorDados {

    public static final int SENHA_TAMANHO_MINIMO = 8;

    // Formato simples: algo@algo.algo, sem espacos
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private ValidadorDados() {
    }

    /** CPF valido = 11 digitos (com ou sem pontuacao), nao repetidos e com os 2 digitos verificadores corretos. */
    public static boolean cpfValido(String cpf) {
        if (cpf == null) return false;
        String d = cpf.replaceAll("\\D", "");
        if (d.length() != 11 || d.chars().distinct().count() == 1) return false;
        return digitoVerificador(d, 9) == d.charAt(9) - '0'
                && digitoVerificador(d, 10) == d.charAt(10) - '0';
    }

    // Calcula o digito na posicao "tamanho" a partir dos digitos anteriores (pesos decrescentes, modulo 11)
    private static int digitoVerificador(String digitos, int tamanho) {
        int soma = 0;
        for (int i = 0; i < tamanho; i++) {
            soma += (digitos.charAt(i) - '0') * (tamanho + 1 - i);
        }
        int resto = (soma * 10) % 11;
        return resto == 10 ? 0 : resto;
    }

    /** Padroniza o CPF como 000.000.000-00 (e assim que fica gravado no banco). Use so depois de cpfValido. */
    public static String formatarCpf(String cpf) {
        String d = cpf.replaceAll("\\D", "");
        return d.substring(0, 3) + "." + d.substring(3, 6) + "." + d.substring(6, 9) + "-" + d.substring(9);
    }

    public static boolean emailValido(String email) {
        return email != null && EMAIL.matcher(email.trim()).matches();
    }

    public static boolean senhaValida(String senha) {
        return senha != null && senha.length() >= SENHA_TAMANHO_MINIMO;
    }

    public static boolean vazio(String valor) {
        return valor == null || valor.isBlank();
    }

    /**
     * Valida os dados comuns de cadastro e edicao de paciente (o CPF e conferido a parte,
     * porque nao pode ser alterado depois do cadastro). Retorna a mensagem de erro ou null.
     */
    public static String erroDadosPaciente(String nome, String email, LocalDate dataNascimento) {
        if (vazio(nome)) return "O nome do paciente e obrigatorio.";
        if (!vazio(email) && !emailValido(email)) return "E-mail invalido. Use o formato nome@exemplo.com.";
        if (dataNascimento != null && dataNascimento.isAfter(LocalDate.now())) {
            return "A data de nascimento nao pode ser no futuro.";
        }
        return null;
    }
}
