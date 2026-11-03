package com.pelusas.api.dto;

import com.pelusas.dominio.Jugador;

import java.util.List;

public record JugadorResponse(
        Long id,
        String nombre,
        int posicion,
        List<Integer> cartasDelante,
        List<Integer> montonPuntos,
        int puntos,
        Integer puntuacionFinal,
        boolean ganador) {

    public static JugadorResponse desde(Jugador jugador) {
        return new JugadorResponse(
                jugador.getId(),
                jugador.getNombre(),
                jugador.getPosicion(),
                List.copyOf(jugador.getCartasDelante()),
                List.copyOf(jugador.getMontonPuntos()),
                jugador.puntos(),
                jugador.getPuntuacionFinal(),
                jugador.isGanador());
    }
}
