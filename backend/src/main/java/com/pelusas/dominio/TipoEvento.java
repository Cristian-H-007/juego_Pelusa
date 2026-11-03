package com.pelusas.dominio;

/** Última acción ocurrida en la partida, para que el frontend pueda informarla. */
public enum TipoEvento {
    PARTIDA_CREADA,
    PELUSAS_GUARDADAS,
    CARTA_SACADA,
    TURNO_PERDIDO,
    CARTAS_ROBADAS,
    PLANTADO,
    PARTIDA_FINALIZADA
}
