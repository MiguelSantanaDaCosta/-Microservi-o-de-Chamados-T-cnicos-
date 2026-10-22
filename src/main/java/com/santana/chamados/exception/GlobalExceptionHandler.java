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

// trata os erros de todos os controllers num lugar só
// transforma a exceção numa resposta JSON padrão
@RestControllerAdvice
public class GlobalExceptionHandler {
	// formato único de erro: status, nome do erro, mensagem e a hora
	public record ErroResponse(int status, String erro, String mensagem, Instant timestamp) {
	}

	// chamado já resolvido: 409
	@ExceptionHandler(ChamadoJaResolvidoException.class)
	public ResponseEntity<ErroResponse> handleJaResolvido(ChamadoJaResolvidoException ex) {
		return resposta(HttpStatus.CONFLICT, ex.getMessage());
	}

	// validação do corpo falhou: 400
	// junta os erros de todos os campos numa frase só
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErroResponse> handleValidacao(MethodArgumentNotValidException ex) {
		String mensagem = ex.getBindingResult().getFieldErrors().stream()
				.map(e -> e.getField() + ": " + e.getDefaultMessage())
				.collect(Collectors.joining("; "));
		return resposta(HttpStatus.BAD_REQUEST, mensagem);
	}

	// corpo vazio ou JSON quebrado: 400
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErroResponse> handleCorpoInvalido(HttpMessageNotReadableException ex) {
		return resposta(HttpStatus.BAD_REQUEST, "Corpo da requisição ausente ou JSON inválido");
	}

	// erros que o service lança com status próprio (404 e 403), mantém o status e a mensagem
	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ErroResponse> handleStatus(ResponseStatusException ex) {
		HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
		return resposta(status, ex.getReason());
	}

	// o preauthorize barrou quem não é tecnico nem admin: 403
	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ErroResponse> handleAcessoNegado(AccessDeniedException ex) {
		return resposta(HttpStatus.FORBIDDEN, "Você não tem permissão para esta operação");
	}

	// monta a resposta padrão com status, mensagem e a hora do erro
	private ResponseEntity<ErroResponse> resposta(HttpStatus status, String mensagem) {
		return ResponseEntity.status(status)
				.body(new ErroResponse(status.value(), status.getReasonPhrase(), mensagem, Instant.now()));
	}
}
