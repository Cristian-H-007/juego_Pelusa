package com.pelusas.dominio;

/** Errores de reglas de negocio y el código HTTP con el que se informan al cliente. */
public enum CodigoError {
    JUGADORES_INVALIDOS(400),
    MAZO_INVALIDO(400),
    VALOR_INVALIDO(400),
    JUGADOR_NO_PERTENECE(400),
    PARTIDA_FINALIZADA(409),
    PARTIDA_NO_FINALIZADA(409),
    NO_ES_SU_TURNO(409),
    MAZO_VACIO(409),
    NO_PUEDE_GUARDAR(409),
    NO_PUEDE_ROBAR(409),
    NO_PUEDE_PLANTARSE(409);

    private final int estadoHttp;

    CodigoError(int estadoHttp) {
        this.estadoHttp = estadoHttp;
    }

    public int getEstadoHttp() {
        return estadoHttp;
    }
}
