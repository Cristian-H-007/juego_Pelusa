package com.pelusas.dominio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DesempateTest {

    private static Jugador conMonton(String nombre, Integer... cartas) {
        Jugador jugador = new Jugador(nombre, 0, null);
        jugador.colocarDelante(List.of(cartas));
        jugador.guardarCartasDelante();
        return jugador;
    }

    @Test
    @DisplayName("Gana la mayor puntuación aunque tenga menos cartas de valor 1")
    void ganaLaMayorPuntuacion() {
        Jugador ana = conMonton("Ana", 10);
        Jugador luis = conMonton("Luis", 1, 1, 1);

        assertThat(Desempate.ganadores(List.of(ana, luis))).containsExactly(ana);
        assertThat(Desempate.comparar(ana, luis)).isPositive();
        assertThat(Desempate.comparar(luis, ana)).isNegative();
    }

    @Test
    @DisplayName("Empate en puntos: gana quien tenga más cartas de valor 1")
    void desempatePorCartasDeValor1() {
        Jugador ana = conMonton("Ana", 1, 4);       // 5 puntos, un 1
        Jugador luis = conMonton("Luis", 1, 1, 3);  // 5 puntos, dos 1

        assertThat(Desempate.ganadores(List.of(ana, luis))).containsExactly(luis);
    }

    @Test
    @DisplayName("Empate en puntos y en valor 1: gana quien tenga más cartas de valor 2")
    void desempatePorCartasDeValor2() {
        Jugador ana = conMonton("Ana", 1, 2, 2);  // 5 puntos, un 1, dos 2
        Jugador luis = conMonton("Luis", 1, 4);   // 5 puntos, un 1, ningún 2

        assertThat(Desempate.ganadores(List.of(ana, luis))).containsExactly(ana);
        assertThat(Desempate.comparar(ana, luis)).isPositive();
    }

    @Test
    @DisplayName("El desempate continúa valor por valor hasta encontrar una diferencia")
    void desempateEnValoresSuperiores() {
        Jugador ana = conMonton("Ana", 5, 5);  // 10 puntos, dos 5
        Jugador luis = conMonton("Luis", 10);  // 10 puntos, ningún 1 a 5

        assertThat(Desempate.ganadores(List.of(luis, ana))).containsExactly(ana);
    }

    @Test
    @DisplayName("Si son idénticos en todos los criterios, ambos ganan (empate)")
    void empateTotal() {
        Jugador ana = conMonton("Ana", 3, 7);
        Jugador luis = conMonton("Luis", 7, 3);
        Jugador eva = conMonton("Eva", 2);

        assertThat(Desempate.comparar(ana, luis)).isZero();
        assertThat(Desempate.ganadores(List.of(ana, luis, eva))).containsExactly(ana, luis);
    }

    @Test
    void conTresJugadoresGanaElMejor() {
        Jugador ana = conMonton("Ana", 3);
        Jugador luis = conMonton("Luis", 9, 1);
        Jugador eva = conMonton("Eva", 8);

        assertThat(Desempate.ganadores(List.of(ana, luis, eva))).containsExactly(luis);
    }

    @Test
    void elCriterioPublicoUsaLaMismaComparacion() {
        Jugador ana = conMonton("Ana", 1, 4);
        Jugador luis = conMonton("Luis", 1, 1, 3);

        assertThat(Desempate.CRITERIO.compare(luis, ana)).isPositive();
    }
}
