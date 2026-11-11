package com.pelusas.api.dto;

import com.pelusas.dominio.Jugador;
import com.pelusas.dominio.Partida;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.pelusas.dominio.PartidasDePrueba.jugarHastaTerminar;
import static com.pelusas.dominio.PartidasDePrueba.partida;
import static org.assertj.core.api.Assertions.assertThat;

/** Verifica que el estado de la partida se traslade completo y sin alteraciones a la respuesta de la API. */
class PartidaResponseTest {

    @Test
    @DisplayName("Partida recién creada: fechas, acciones permitidas y datos de los jugadores")
    void partidaEnCurso() {
        Partida partida = partida(List.of("Ana", "Luis"));

        PartidaResponse respuesta = PartidaResponse.desde(partida);

        assertThat(respuesta.fechaCreacion()).isEqualTo(partida.getFechaCreacion()).isNotNull();
        assertThat(respuesta.fechaFin()).isNull();
        assertThat(respuesta.acciones().puedeSacar()).isTrue();
        assertThat(respuesta.acciones().puedeGuardar()).isFalse();
        assertThat(respuesta.acciones().puedeRobar()).isFalse();
        assertThat(respuesta.acciones().puedePlantarse()).isFalse();
        assertThat(respuesta.acciones().puedeUsarComodin()).isTrue();
        assertThat(respuesta.jugadores()).allSatisfy(jugador -> {
            assertThat(jugador.protegido()).isFalse();
            assertThat(jugador.comodinUsado()).isFalse();
            assertThat(jugador.ganador()).isFalse();
            assertThat(jugador.puntuacionFinal()).isNull();
        });
    }

    @Test
    @DisplayName("Partida terminada: fecha de fin, sin acciones y ganador con su puntuación final")
    void partidaTerminada() {
        Partida partida = jugarHastaTerminar(partida(List.of("Ana", "Luis")));
        Jugador ganador = partida.getGanadores().getFirst();

        PartidaResponse respuesta = PartidaResponse.desde(partida);

        assertThat(respuesta.fechaFin()).isEqualTo(partida.getFechaFin()).isNotNull();
        assertThat(respuesta.acciones().puedeSacar()).isFalse();
        assertThat(respuesta.jugadores()).filteredOn(JugadorResponse::ganador)
                .extracting(JugadorResponse::nombre).containsExactly(ganador.getNombre());
        assertThat(respuesta.jugadores()).filteredOn(jugador -> !jugador.ganador()).hasSize(1);
        assertThat(respuesta.jugadores()).allSatisfy(jugador ->
                assertThat(jugador.puntuacionFinal()).isEqualTo(jugador.puntos()).isPositive());
    }
}
