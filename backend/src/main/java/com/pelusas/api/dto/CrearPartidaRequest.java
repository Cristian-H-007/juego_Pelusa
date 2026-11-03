package com.pelusas.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * @param jugadores nombres en orden de juego; el primero inicia
 * @param semilla   opcional: hace reproducible la mezcla del mazo (pruebas y demostraciones)
 */
public record CrearPartidaRequest(
        @NotNull(message = "La lista de jugadores es obligatoria") List<String> jugadores,
        Long semilla) {
}
