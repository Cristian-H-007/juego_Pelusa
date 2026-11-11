package com.pelusas.dominio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JugadorTest {

    private static Jugador conCartasDelante(Integer... cartas) {
        Jugador jugador = new Jugador("Ana", 0, null);
        jugador.colocarDelante(List.of(cartas));
        return jugador;
    }

    @Nested
    @DisplayName("Regla de pérdida")
    class ReglaDePerdida {

        @Test
        @DisplayName("Repetición con menos de 3 cartas: no pierde")
        void repeticionConDosCartasNoPierde() {
            assertThat(conCartasDelante(4, 7).pierdeAlSacar(7)).isFalse();
        }

        @Test
        @DisplayName("Repetición con 1 carta: no pierde")
        void repeticionConUnaCartaNoPierde() {
            assertThat(conCartasDelante(7).pierdeAlSacar(7)).isFalse();
        }

        @Test
        @DisplayName("Repetición teniendo exactamente 3 cartas: pierde")
        void repeticionConTresCartasPierde() {
            assertThat(conCartasDelante(4, 7, 9).pierdeAlSacar(7)).isTrue();
        }

        @Test
        @DisplayName("Repetición teniendo más de 3 cartas: pierde")
        void repeticionConMasDeTresCartasPierde() {
            assertThat(conCartasDelante(4, 7, 9, 2, 5).pierdeAlSacar(7)).isTrue();
        }

        @Test
        @DisplayName("Carta no repetida con 3 o más cartas: no pierde")
        void cartaNoRepetidaNoPierde() {
            assertThat(conCartasDelante(4, 7, 9).pierdeAlSacar(3)).isFalse();
            assertThat(conCartasDelante(4, 7, 9, 2, 5, 6).pierdeAlSacar(10)).isFalse();
        }

        @Test
        @DisplayName("Sin cartas delante nunca pierde")
        void sinCartasNoPierde() {
            assertThat(conCartasDelante().pierdeAlSacar(7)).isFalse();
        }

        @Test
        @DisplayName("Tres cartas iguales y sale otra igual: pierde")
        void tresCartasIgualesPierde() {
            assertThat(conCartasDelante(7, 7, 7).pierdeAlSacar(7)).isTrue();
        }

        @Test
        void elUmbralEsTresCartas() {
            assertThat(Jugador.CARTAS_PARA_PERDER).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Comodín")
    class Comodin {

        @Test
        @DisplayName("Al inicio tiene el comodín disponible y no está protegido")
        void estadoInicial() {
            Jugador jugador = conCartasDelante(7);

            assertThat(jugador.isComodinUsado()).isFalse();
            assertThat(jugador.isProtegido()).isFalse();
            assertThat(jugador.puedenRobarle(7)).isTrue();
        }

        @Test
        @DisplayName("Al usarlo queda protegido: no le pueden robar")
        void usarComodinProtege() {
            Jugador jugador = conCartasDelante(7);

            jugador.usarComodin();

            assertThat(jugador.isComodinUsado()).isTrue();
            assertThat(jugador.isProtegido()).isTrue();
            assertThat(jugador.puedenRobarle(7)).isFalse();
            assertThat(jugador.tieneCartasDeValor(7)).isTrue();
        }

        @Test
        @DisplayName("Al terminar la protección se le puede robar, pero el comodín sigue gastado")
        void finalizarProteccion() {
            Jugador jugador = conCartasDelante(7);
            jugador.usarComodin();

            jugador.finalizarProteccion();

            assertThat(jugador.isProtegido()).isFalse();
            assertThat(jugador.isComodinUsado()).isTrue();
            assertThat(jugador.puedenRobarle(7)).isTrue();
        }

        @Test
        @DisplayName("No le pueden robar un valor que no tiene, esté o no protegido")
        void valorQueNoTiene() {
            assertThat(conCartasDelante(3).puedenRobarle(7)).isFalse();
        }
    }

    @Test
    void colocarDelanteAgregaLasCartas() {
        Jugador jugador = conCartasDelante(4);
        jugador.colocarDelante(7);

        assertThat(jugador.getCartasDelante()).containsExactly(4, 7);
        assertThat(jugador.tieneCartasDeValor(7)).isTrue();
        assertThat(jugador.tieneCartasDeValor(5)).isFalse();
    }

    @Test
    @DisplayName("Entregar cartas de un valor retira solo las de ese valor")
    void entregarCartasDeValor() {
        Jugador jugador = conCartasDelante(7, 3, 7, 5);

        List<Integer> entregadas = jugador.entregarCartasDeValor(7);

        assertThat(entregadas).containsExactly(7, 7);
        assertThat(jugador.getCartasDelante()).containsExactly(3, 5);
    }

    @Test
    void entregarUnValorQueNoTieneNoCambiaNada() {
        Jugador jugador = conCartasDelante(3, 5);

        assertThat(jugador.entregarCartasDeValor(7)).isEmpty();
        assertThat(jugador.getCartasDelante()).containsExactly(3, 5);
    }

    @Test
    @DisplayName("Guardar pasa las cartas de delante al montón de puntos")
    void guardarCartasDelante() {
        Jugador jugador = conCartasDelante(4, 7, 7);

        int guardadas = jugador.guardarCartasDelante();

        assertThat(guardadas).isEqualTo(3);
        assertThat(jugador.getCartasDelante()).isEmpty();
        assertThat(jugador.getMontonPuntos()).containsExactly(4, 7, 7);
        assertThat(jugador.puntos()).isEqualTo(18);
        assertThat(jugador.cantidadEnMonton(7)).isEqualTo(2);
        assertThat(jugador.cantidadEnMonton(4)).isEqualTo(1);
        assertThat(jugador.cantidadEnMonton(9)).isZero();
    }

    @Test
    void guardarAcumulaConLoYaGuardado() {
        Jugador jugador = conCartasDelante(4);
        jugador.guardarCartasDelante();
        jugador.colocarDelante(List.of(6, 2));

        jugador.guardarCartasDelante();

        assertThat(jugador.getMontonPuntos()).containsExactly(4, 6, 2);
        assertThat(jugador.puntos()).isEqualTo(12);
    }

    @Test
    @DisplayName("Descartar elimina las cartas sin sumarlas al montón")
    void descartarCartasDelante() {
        Jugador jugador = conCartasDelante(4, 7, 9);

        int descartadas = jugador.descartarCartasDelante();

        assertThat(descartadas).isEqualTo(3);
        assertThat(jugador.getCartasDelante()).isEmpty();
        assertThat(jugador.getMontonPuntos()).isEmpty();
        assertThat(jugador.puntos()).isZero();
    }

    @Test
    void registrarResultadoGuardaPuntuacionYGanador() {
        Jugador jugador = conCartasDelante(10, 5);
        jugador.guardarCartasDelante();

        jugador.registrarResultado(true);

        assertThat(jugador.getPuntuacionFinal()).isEqualTo(15);
        assertThat(jugador.isGanador()).isTrue();
    }

    @Test
    void registrarResultadoPerdedor() {
        Jugador jugador = conCartasDelante(3);
        jugador.guardarCartasDelante();

        jugador.registrarResultado(false);

        assertThat(jugador.getPuntuacionFinal()).isEqualTo(3);
        assertThat(jugador.isGanador()).isFalse();
    }

    @Test
    void datosBasicos() {
        Jugador jugador = new Jugador("Luis", 2, null);

        assertThat(jugador.getNombre()).isEqualTo("Luis");
        assertThat(jugador.getPosicion()).isEqualTo(2);
        assertThat(jugador.getId()).isNull();
        assertThat(jugador.getPuntuacionFinal()).isNull();
        assertThat(jugador.isGanador()).isFalse();
    }

    @Test
    @DisplayName("Las listas expuestas no se pueden modificar desde fuera")
    void listasNoModificables() {
        Jugador jugador = conCartasDelante(4);

        assertThatThrownBy(() -> jugador.getCartasDelante().add(5)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> jugador.getMontonPuntos().add(5)).isInstanceOf(UnsupportedOperationException.class);
    }
}
