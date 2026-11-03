package com.pelusas.api.dto;

import jakarta.validation.constraints.NotNull;

/** Identifica al jugador que solicita la acción; el backend verifica que sea su turno. */
public record AccionRequest(@NotNull(message = "El jugadorId es obligatorio") Long jugadorId) {
}
