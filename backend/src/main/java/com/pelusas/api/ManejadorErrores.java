package com.pelusas.api;

import com.pelusas.api.dto.ErrorResponse;
import com.pelusas.dominio.ReglaJuegoException;
import com.pelusas.servicio.PartidaNoEncontradaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Traduce las excepciones a respuestas JSON con un código de error legible. */
@RestControllerAdvice
public class ManejadorErrores {

    static final String SOLICITUD_INVALIDA = "SOLICITUD_INVALIDA";

    @ExceptionHandler(ReglaJuegoException.class)
    public ResponseEntity<ErrorResponse> reglaDeJuego(ReglaJuegoException ex) {
        return responder(HttpStatus.valueOf(ex.getCodigo().getEstadoHttp()), ex.getCodigo().name(), ex.getMessage());
    }

    @ExceptionHandler(PartidaNoEncontradaException.class)
    public ResponseEntity<ErrorResponse> partidaNoEncontrada(PartidaNoEncontradaException ex) {
        return responder(HttpStatus.NOT_FOUND, "PARTIDA_NO_ENCONTRADA", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacion(MethodArgumentNotValidException ex) {
        return responder(HttpStatus.BAD_REQUEST, SOLICITUD_INVALIDA,
                ex.getBindingResult().getFieldErrors().getFirst().getDefaultMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> cuerpoIlegible(HttpMessageNotReadableException ex) {
        return responder(HttpStatus.BAD_REQUEST, SOLICITUD_INVALIDA, "El cuerpo de la solicitud no es un JSON válido");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> tipoInvalido(MethodArgumentTypeMismatchException ex) {
        return responder(HttpStatus.BAD_REQUEST, SOLICITUD_INVALIDA,
                "Valor inválido para '" + ex.getName() + "': " + ex.getValue());
    }

    private ResponseEntity<ErrorResponse> responder(HttpStatus estado, String codigo, String mensaje) {
        return ResponseEntity.status(estado).body(new ErrorResponse(estado.value(), codigo, mensaje));
    }
}
