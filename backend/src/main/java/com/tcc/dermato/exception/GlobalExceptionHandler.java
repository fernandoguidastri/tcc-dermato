package com.tcc.dermato.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.Map;

/**
 * Centraliza o tratamento de erros para que o frontend sempre receba um JSON
 * no formato {"erro": "mensagem legivel"} em vez de stack traces ou respostas
 * inconsistentes. Isso facilita exibir mensagens claras nas telas.
 */
@RestControllerAdvice(basePackages = "com.tcc.dermato.controller")
public class GlobalExceptionHandler {

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> tratarViolacaoDeIntegridade(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("erro", "Nao foi possivel concluir a operacao: verifique se algum dado (CPF, e-mail, CRM) ja esta cadastrado, ou se este registro possui consultas/imagens vinculadas que impedem a exclusao."));
    }

    // Campo obrigatorio ausente na requisicao (ex.: "consultaId" ou "arquivo" no upload)
    @ExceptionHandler({MissingServletRequestParameterException.class, MissingServletRequestPartException.class})
    public ResponseEntity<Map<String, String>> tratarCampoAusente(Exception ex) {
        String campo = ex instanceof MissingServletRequestParameterException p ? p.getParameterName()
                : ((MissingServletRequestPartException) ex).getRequestPartName();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("erro", "Campo obrigatorio ausente: " + campo + "."));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> tratarArgumentoInvalido(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("erro", "Dados invalidos: " + ex.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> tratarErroGenerico(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("erro", ex.getMessage() != null ? ex.getMessage() : "Ocorreu um erro ao processar a solicitacao."));
    }
}
