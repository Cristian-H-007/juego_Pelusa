package com.pelusas.api.dto;

import com.pelusas.dominio.Jugador;
import com.pelusas.dominio.Partida;

import java.time.LocalDateTime;
import java.util.List;

/** Resultado de una partida terminada; también es cada elemento del histórico. */
public record ResultadoResponse(
        Long partidaId,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaFin,
        List<String> ganadores,
        boolean empate,
        List<ResultadoJugador> jugadores) {

    public record ResultadoJugador(
            Long id,
            String nombre,
            List<Integer> cartasAcumuladas,
            int cantidadCartas,
            int puntuacionFinal,
            boolean ganador) {

        static ResultadoJugador desde(Jugador jugador) {
            return new ResultadoJugador(
                    jugador.getId(),
                    jugador.getNombre(),
                    List.copyOf(jugador.getMontonPuntos()),
                    jugador.getMontonPuntos().size(),
                    jugador.getPuntuacionFinal(),
                    jugador.isGanador());
        }
    }

    public static ResultadoResponse desde(Partida partida) {
        List<String> ganadores = partida.getGanadores().stream().map(Jugador::getNombre).toList();
        return new ResultadoResponse(
                partida.getId(),
                partida.getFechaCreacion(),
                partida.getFechaFin(),
                ganadores,
                ganadores.size() > 1,
                partida.getJugadores().stream().map(ResultadoJugador::desde).toList());
    }
}
