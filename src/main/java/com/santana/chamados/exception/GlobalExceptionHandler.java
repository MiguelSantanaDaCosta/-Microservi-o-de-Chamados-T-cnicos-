package com.santana.chamados.exception;

import java.time.Instant;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {
	public record ErroResponse(int status, String erro, String mensagem, Instant timestamp) {
	}

	@ExceptionHandler(ChamadoJaResolvidoException.class)
	public ResponseEntity<ErroResponse> handleJaResolvido(ChamadoJaResolvidoException ex) {
		return resposta(HttpStatus.CONFLICT, ex.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErroResponse> handleValidacao(MethodArgumentNotValidException ex) {
		String mensagem = ex.getBindingResult().getFieldErrors().stream()
				.map(e -> e.getField() + ": " + e.getDefaultMessage())
				.collect(Collectors.joining("; "));
		return resposta(HttpStatus.BAD_REQUEST, mensagem);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErroResponse> handleCorpoInvalido(HttpMessageNotReadableException ex) {
		return resposta(HttpStatus.BAD_REQUEST, "Corpo da requisição ausente ou JSON inválido");
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ErroResponse> handleStatus(ResponseStatusException ex) {
		HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
		return resposta(status, ex.getReason());
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ErroResponse> handleAcessoNegado(AccessDeniedException ex) {
		return resposta(HttpStatus.FORBIDDEN, "Você não tem permissão para esta operação");
	}

	private ResponseEntity<ErroResponse> resposta(HttpStatus status, String mensagem) {
		return ResponseEntity.status(status)
				.body(new ErroResponse(status.value(), status.getReasonPhrase(), mensagem, Instant.now()));
	}
}
