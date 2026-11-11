package com.pelusas.api.dto;

import com.pelusas.dominio.Jugador;
import com.pelusas.dominio.Partida;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Un empate total es muy difícil de reproducir jugando una partida real, así que la partida y los jugadores
 * se simulan con Mockito para probar solo la transformación al DTO.
 */
class ResultadoResponseTest {

    private static Jugador jugador(long id, String nombre, List<Integer> monton, boolean ganador) {
        Jugador jugador = mock(Jugador.class);
        when(jugador.getId()).thenReturn(id);
        when(jugador.getNombre()).thenReturn(nombre);
        when(jugador.getMontonPuntos()).thenReturn(monton);
        when(jugador.getPuntuacionFinal()).thenReturn(monton.stream().mapToInt(Integer::intValue).sum());
        when(jugador.isGanador()).thenReturn(ganador);
        return jugador;
    }

    @Test
    @DisplayName("Con dos ganadores el resultado se marca como empate")
    void empate() {
        Jugador ana = jugador(1L, "Ana", List.of(3, 7), true);
        Jugador luis = jugador(2L, "Luis", List.of(7, 3), true);
        Partida partida = mock(Partida.class);
        LocalDateTime fin = LocalDateTime.of(2026, 11, 3, 10, 0);
        LocalDateTime inicio = LocalDateTime.of(2026, 11, 3, 9, 30);
        when(partida.getId()).thenReturn(5L);
        when(partida.getFechaCreacion()).thenReturn(inicio);
        when(partida.getFechaFin()).thenReturn(fin);
        when(partida.getGanadores()).thenReturn(List.of(ana, luis));
        when(partida.getJugadores()).thenReturn(List.of(ana, luis));

        ResultadoResponse resultado = ResultadoResponse.desde(partida);

        assertThat(resultado.partidaId()).isEqualTo(5L);
        assertThat(resultado.fechaCreacion()).isEqualTo(inicio);
        assertThat(resultado.fechaFin()).isEqualTo(fin);
        assertThat(resultado.jugadores()).extracting(ResultadoResponse.ResultadoJugador::id).containsExactly(1L, 2L);
        assertThat(resultado.jugadores()).extracting(ResultadoResponse.ResultadoJugador::nombre)
                .containsExactly("Ana", "Luis");
        assertThat(resultado.empate()).isTrue();
        assertThat(resultado.ganadores()).containsExactly("Ana", "Luis");
        assertThat(resultado.jugadores()).extracting(ResultadoResponse.ResultadoJugador::puntuacionFinal)
                .containsExactly(10, 10);
        assertThat(resultado.jugadores()).extracting(ResultadoResponse.ResultadoJugador::cantidadCartas)
                .containsExactly(2, 2);
    }

    @Test
    @DisplayName("Con un solo ganador no hay empate")
    void sinEmpate() {
        Jugador ana = jugador(1L, "Ana", List.of(10), true);
        Jugador luis = jugador(2L, "Luis", List.of(3), false);
        Partida partida = mock(Partida.class);
        when(partida.getGanadores()).thenReturn(List.of(ana));
        when(partida.getJugadores()).thenReturn(List.of(ana, luis));

        ResultadoResponse resultado = ResultadoResponse.desde(partida);

        assertThat(resultado.empate()).isFalse();
        assertThat(resultado.ganadores()).containsExactly("Ana");
        assertThat(resultado.jugadores()).extracting(ResultadoResponse.ResultadoJugador::ganador)
                .containsExactly(true, false);
    }
}
