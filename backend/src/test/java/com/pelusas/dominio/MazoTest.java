package com.pelusas.dominio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import static com.pelusas.dominio.PartidasDePrueba.mazoConCartasSuperiores;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MazoTest {

    @Test
    @DisplayName("El mazo oficial tiene 110 cartas")
    void mazoOficialTiene110Cartas() {
        assertThat(Mazo.cartasOficiales()).hasSize(110);
        assertThat(Mazo.TOTAL_CARTAS).isEqualTo(110);
    }

    @ParameterizedTest(name = "valor {0} tiene 13 copias")
    @ValueSource(ints = {1, 2, 3, 4, 5})
    void valoresDel1Al5Tienen13Copias(int valor) {
        assertThat(Collections.frequency(Mazo.cartasOficiales(), valor)).isEqualTo(13);
        assertThat(Mazo.copiasDeValor(valor)).isEqualTo(13);
    }

    @ParameterizedTest(name = "valor {0} tiene 9 copias")
    @ValueSource(ints = {6, 7, 8, 9, 10})
    void valoresDel6Al10Tienen9Copias(int valor) {
        assertThat(Collections.frequency(Mazo.cartasOficiales(), valor)).isEqualTo(9);
        assertThat(Mazo.copiasDeValor(valor)).isEqualTo(9);
    }

    @Test
    @DisplayName("Solo existen los valores del 1 al 10")
    void soloContieneValoresPermitidos() {
        assertThat(new HashSet<>(Mazo.cartasOficiales())).containsExactlyInAnyOrder(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    }

    @ParameterizedTest(name = "el valor {0} no es válido")
    @ValueSource(ints = {-1, 0, 11, 99})
    void rechazaValoresFueraDeRango(int valor) {
        assertThatThrownBy(() -> Mazo.copiasDeValor(valor))
                .isInstanceOf(ReglaJuegoException.class)
                .hasMessageContaining(String.valueOf(valor))
                .extracting("codigo").isEqualTo(CodigoError.VALOR_INVALIDO);
    }

    @Test
    void aceptaCualquierMezclaDeLasCartasOficiales() {
        List<Integer> mezcladas = new ArrayList<>(Mazo.cartasOficiales());
        Collections.shuffle(mezcladas, new Random(7));

        Mazo mazo = Mazo.desde(mezcladas);

        assertThat(mazo.cantidad()).isEqualTo(110);
        assertThat(mazo.estaVacio()).isFalse();
        assertThat(mazo.cantidadDeValor(1)).isEqualTo(13);
        assertThat(mazo.cantidadDeValor(10)).isEqualTo(9);
    }

    @Test
    void rechazaUnMazoIncompleto() {
        List<Integer> cartas = new ArrayList<>(Mazo.cartasOficiales());
        cartas.removeLast();

        assertThatThrownBy(() -> Mazo.desde(cartas))
                .isInstanceOf(ReglaJuegoException.class)
                .extracting("codigo").isEqualTo(CodigoError.MAZO_INVALIDO);
    }

    @Test
    @DisplayName("Rechaza 110 cartas con la misma cantidad de cada valor")
    void rechazaDistribucionUniforme() {
        List<Integer> uniforme = new ArrayList<>();
        for (int valor = 1; valor <= 10; valor++) {
            uniforme.addAll(Collections.nCopies(11, valor));
        }

        assertThat(uniforme).hasSize(110);
        assertThatThrownBy(() -> Mazo.desde(uniforme))
                .isInstanceOf(ReglaJuegoException.class)
                .extracting("codigo").isEqualTo(CodigoError.MAZO_INVALIDO);
    }

    @Test
    void rechazaCartasConValoresNoPermitidos() {
        List<Integer> cartas = new ArrayList<>(Mazo.cartasOficiales());
        cartas.set(0, 11);

        assertThatThrownBy(() -> Mazo.desde(cartas))
                .isInstanceOf(ReglaJuegoException.class)
                .extracting("codigo").isEqualTo(CodigoError.MAZO_INVALIDO);
    }

    @Test
    @DisplayName("Sacar devuelve la carta superior y la retira del montón")
    void sacarRetiraLaCartaSuperior() {
        Mazo mazo = Mazo.desde(mazoConCartasSuperiores(7, 3));

        assertThat(mazo.sacar()).isEqualTo(7);
        assertThat(mazo.cantidad()).isEqualTo(109);
        assertThat(mazo.cantidadDeValor(7)).isEqualTo(8);
        assertThat(mazo.sacar()).isEqualTo(3);
        assertThat(mazo.cantidad()).isEqualTo(108);
        assertThat(mazo.cantidadDeValor(3)).isEqualTo(12);
    }

    @Test
    void sacarDeUnMazoVacioLanzaError() {
        Mazo mazo = Mazo.desde(Mazo.cartasOficiales());
        for (int i = 0; i < 110; i++) {
            mazo.sacar();
        }

        assertThat(mazo.estaVacio()).isTrue();
        assertThat(mazo.cantidad()).isZero();
        assertThatThrownBy(mazo::sacar)
                .isInstanceOf(ReglaJuegoException.class)
                .extracting("codigo").isEqualTo(CodigoError.MAZO_VACIO);
    }

    @Test
    void elMazoNoDependeDeLaListaOriginal() {
        List<Integer> cartas = new ArrayList<>(Mazo.cartasOficiales());
        Mazo mazo = Mazo.desde(cartas);

        cartas.clear();

        assertThat(mazo.cantidad()).isEqualTo(110);
    }
}
