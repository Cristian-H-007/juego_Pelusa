package com.pelusas.servicio;

public class PartidaNoEncontradaException extends RuntimeException {

    public PartidaNoEncontradaException(Long id) {
        super("No existe la partida " + id);
    }
}
