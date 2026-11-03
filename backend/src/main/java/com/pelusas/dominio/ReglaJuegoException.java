package com.pelusas.dominio;

/** Se lanza cuando una acción solicitada viola una regla del juego. */
public class ReglaJuegoException extends RuntimeException {

    private final CodigoError codigo;

    public ReglaJuegoException(CodigoError codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public CodigoError getCodigo() {
        return codigo;
    }
}
