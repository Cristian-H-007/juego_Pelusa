package com.pelusas.api.dto;

import com.pelusas.dominio.EstadoPartida;
import com.pelusas.dominio.Partida;
import com.pelusas.dominio.TipoEvento;

import java.time.LocalDateTime;
import java.util.List;

/** Estado completo de la partida. Las acciones permitidas las calcula el backend. */
public record PartidaResponse(
        Long id,
        EstadoPartida estado,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaFin,
        int cartasEnMazo,
        int cartasDescartadas,
        Long jugadorInicialId,
        Long jugadorActualId,
        Integer ultimaCarta,
        TipoEvento ultimoEvento,
        String mensajeEvento,
        Acciones acciones,
        List<JugadorResponse> jugadores) {

    public record Acciones(
            boolean puedeGuardar,
            boolean puedeSacar,
            boolean puedeRobar,
            boolean puedePlantarse,
            Integer valorRobable) {
    }

    public static PartidaResponse desde(Partida partida) {
        Acciones acciones = new Acciones(
                partida.puedeGuardar(),
                partida.puedeSacar(),
                partida.puedeRobar(),
                partida.puedePlantarse(),
                partida.getValorRobable());
        return new PartidaResponse(
                partida.getId(),
                partida.getEstado(),
                partida.getFechaCreacion(),
                partida.getFechaFin(),
                partida.getMazo().cantidad(),
                partida.getCartasDescartadas(),
                partida.getJugadorInicial().getId(),
                partida.getJugadorActual().getId(),
                partida.getUltimaCarta(),
                partida.getUltimoEvento(),
                partida.getMensajeEvento(),
                acciones,
                partida.getJugadores().stream().map(JugadorResponse::desde).toList());
    }
}
