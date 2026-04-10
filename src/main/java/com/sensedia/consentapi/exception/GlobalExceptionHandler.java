package com.sensedia.consentapi.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> ((FieldError) error).getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());

        log.warn("Tentativa de requisição com dados inválidos (400 Bad Request). Erros: {}", errors);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Erro de validação nos dados enviados", errors);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.warn("Recurso não encontrado (404 Not Found): {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Regra de negócio violada (400 Bad Request): {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = "O parâmetro '" + ex.getName() + "' recebeu um valor inválido. Formato esperado: UUID.";
        log.warn("Tipo de argumento incompatível (400 Bad Request): {}", message);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, message, null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        String message = "O corpo da requisição está malformado ou contém valores inválidos (ex: status inexistente).";
        log.warn("Mensagem HTTP ilegível (400 Bad Request): {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, message, null);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Credenciais inválidas (401 Unauthorized): {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Credenciais inválidas. Verifique username e senha.", null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Acesso negado (403 Forbidden): {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.FORBIDDEN,
                "Acesso negado. Você não possui permissão para realizar esta ação.", null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllUncaughtException(Exception ex) {
        log.error("Erro interno inesperado (500 Internal Server Error)", ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro interno inesperado", null);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException ex) {
        log.warn("Header obrigatório ausente (400 Bad Request): {}", ex.getHeaderName());
        return buildErrorResponse(HttpStatus.BAD_REQUEST,
                "O header '" + ex.getHeaderName() + "' é obrigatório.", null);
    }


    private ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus status, String message, List<String> validationErrors) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .validationErrors(validationErrors)
                .build();

        return ResponseEntity.status(status).body(errorResponse);
    }
}