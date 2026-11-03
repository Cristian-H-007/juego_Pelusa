package com.pelusas.api.dto;

import jakarta.validation.constraints.NotNull;

/** Después de sacar una carta el jugador decide si continúa (saca otra) o para (se planta). */
public record DecisionRequest(
        @NotNull(message = "El jugadorId es obligatorio") Long jugadorId,
        @NotNull(message = "La decisión es obligatoria: CONTINUAR o PARAR") Decision decision) {

    public enum Decision {
        CONTINUAR,
        PARAR
    }
}
