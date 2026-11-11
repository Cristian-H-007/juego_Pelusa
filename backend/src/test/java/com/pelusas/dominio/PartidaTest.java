package com.pelusas.dominio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static com.pelusas.dominio.PartidasDePrueba.jugarHastaTerminar;
import static com.pelusas.dominio.PartidasDePrueba.mazoConCartasSuperiores;
import static com.pelusas.dominio.PartidasDePrueba.partida;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PartidaTest {

    private static final List<String> DOS = List.of("Ana", "Luis");
    private static final List<String> TRES = List.of("Ana", "Luis", "Eva");

    private static Jugador jugador(Partida partida, int posicion) {
        return partida.getJugadores().get(posicion);
    }

    private static void assertError(Runnable accion, CodigoError codigo) {
        assertThatThrownBy(accion::run)
                .isInstanceOf(ReglaJuegoException.class)
                .extracting("codigo").isEqualTo(codigo);
    }

    // ================================================================== creación

    @Nested
    @DisplayName("Creación de la partida")
    class Creacion {

        @Test
        @DisplayName("El estado inicial es válido")
        void estadoInicialValido() {
            Partida partida = Partida.crear(List.of(" Ana ", "Luis", "Eva"), Mazo.cartasOficiales());

            assertThat(partida.getEstado()).isEqualTo(EstadoPartida.EN_CURSO);
            assertThat(partida.estaEnCurso()).isTrue();
            assertThat(partida.getMazo().cantidad()).isEqualTo(110);
            assertThat(partida.getCartasDescartadas()).isZero();
            assertThat(partida.getFechaCreacion()).isNotNull();
            assertThat(partida.getFechaFin()).isNull();
            assertThat(partida.getUltimaCarta()).isNull();
            assertThat(partida.isHaSacadoEnTurno()).isFalse();
            assertThat(partida.getValorRobable()).isNull();
            assertThat(partida.getId()).isNull();
            assertThat(partida.getGanadores()).isEmpty();
            assertThat(partida.getJugadores()).extracting(Jugador::getNombre).containsExactly("Ana", "Luis", "Eva");
            assertThat(partida.getJugadores()).extracting(Jugador::getPosicion).containsExactly(0, 1, 2);
            assertThat(partida.getJugadores()).allSatisfy(j -> {
                assertThat(j.getCartasDelante()).isEmpty();
                assertThat(j.getMontonPuntos()).isEmpty();
            });
        }

        @Test
        @DisplayName("Inicia el primer jugador registrado")
        void iniciaElPrimerJugador() {
            Partida partida = partida(TRES);

            assertThat(partida.getJugadorInicial().getNombre()).isEqualTo("Ana");
            assertThat(partida.getJugadorActual()).isSameAs(partida.getJugadorInicial());
            assertThat(partida.getUltimoEvento()).isEqualTo(TipoEvento.PARTIDA_CREADA);
            assertThat(partida.getMensajeEvento()).isEqualTo("Partida creada. Inicia Ana");
        }

        @Test
        @DisplayName("Al inicio solo se puede sacar carta")
        void accionesIniciales() {
            Partida partida = partida(DOS);

            assertThat(partida.puedeSacar()).isTrue();
            assertThat(partida.puedeGuardar()).isFalse();
            assertThat(partida.puedeRobar()).isFalse();
            assertThat(partida.puedePlantarse()).isFalse();
        }

        @ParameterizedTest(name = "{0} jugadores es válido")
        @ValueSource(ints = {2, 3, 4, 5, 6})
        void aceptaEntre2Y6Jugadores(int cantidad) {
            List<String> nombres = new ArrayList<>();
            for (int i = 1; i <= cantidad; i++) {
                nombres.add("J" + i);
            }

            assertThat(Partida.crear(nombres, Mazo.cartasOficiales()).getJugadores()).hasSize(cantidad);
        }

        @ParameterizedTest(name = "{0} jugadores no es válido")
        @ValueSource(ints = {0, 1, 7})
        void rechazaCantidadInvalidaDeJugadores(int cantidad) {
            List<String> nombres = new ArrayList<>();
            for (int i = 1; i <= cantidad; i++) {
                nombres.add("J" + i);
            }

            assertThatThrownBy(() -> Partida.crear(nombres, Mazo.cartasOficiales()))
                    .isInstanceOf(ReglaJuegoException.class)
                    .hasMessage("La partida debe tener entre 2 y 6 jugadores")
                    .extracting("codigo").isEqualTo(CodigoError.JUGADORES_INVALIDOS);
        }

        @Test
        void rechazaListaNula() {
            assertError(() -> Partida.crear(null, Mazo.cartasOficiales()), CodigoError.JUGADORES_INVALIDOS);
        }

        @Test
        void rechazaNombresVaciosONulos() {
            assertError(() -> Partida.crear(List.of("Ana", "  "), Mazo.cartasOficiales()), CodigoError.JUGADORES_INVALIDOS);
            assertError(() -> Partida.crear(Arrays.asList("Ana", null), Mazo.cartasOficiales()), CodigoError.JUGADORES_INVALIDOS);
        }

        @Test
        @DisplayName("El nombre admite hasta 20 caracteres")
        void longitudMaximaDelNombre() {
            String veinte = "A".repeat(20);

            assertThat(Partida.crear(List.of(veinte, "Luis"), Mazo.cartasOficiales()).getJugadorInicial().getNombre())
                    .isEqualTo(veinte);
            assertThatThrownBy(() -> Partida.crear(List.of(veinte + "B", "Luis"), Mazo.cartasOficiales()))
                    .isInstanceOf(ReglaJuegoException.class)
                    .hasMessageContaining("20 caracteres");
        }

        @Test
        @DisplayName("No se permiten nombres repetidos (sin importar mayúsculas)")
        void rechazaNombresRepetidos() {
            assertThatThrownBy(() -> Partida.crear(List.of("Ana", " ana"), Mazo.cartasOficiales()))
                    .isInstanceOf(ReglaJuegoException.class)
                    .hasMessage("Nombre de jugador repetido: ana");
        }

        @Test
        void rechazaUnMazoInvalido() {
            assertError(() -> Partida.crear(DOS, List.of(1, 2, 3)), CodigoError.MAZO_INVALIDO);
        }
    }

    // ================================================================== sacar carta

    @Nested
    @DisplayName("Sacar una carta")
    class SacarCarta {

        @Test
        @DisplayName("La carta queda visible delante del jugador")
        void laCartaQuedaDelante() {
            Partida partida = partida(DOS, 7);
            Jugador ana = jugador(partida, 0);

            partida.sacarCarta(ana);

            assertThat(ana.getCartasDelante()).containsExactly(7);
            assertThat(partida.getMazo().cantidad()).isEqualTo(109);
            assertThat(partida.getUltimaCarta()).isEqualTo(7);
            assertThat(partida.isHaSacadoEnTurno()).isTrue();
            assertThat(partida.getUltimoEvento()).isEqualTo(TipoEvento.CARTA_SACADA);
            assertThat(partida.getMensajeEvento()).isEqualTo("Ana sacó un 7");
            assertThat(partida.getJugadorActual()).isSameAs(ana);
        }

        @Test
        @DisplayName("Después de sacar puede continuar o plantarse, pero ya no guardar")
        void accionesDespuesDeSacar() {
            Partida partida = partida(DOS, 7);
            partida.sacarCarta(jugador(partida, 0));

            assertThat(partida.puedeSacar()).isTrue();
            assertThat(partida.puedePlantarse()).isTrue();
            assertThat(partida.puedeGuardar()).isFalse();
        }

        @Test
        @DisplayName("Continuar jugando: puede sacar varias cartas en el mismo turno")
        void continuarJugando() {
            Partida partida = partida(DOS, 7, 3, 9);
            Jugador ana = jugador(partida, 0);

            partida.sacarCarta(ana);
            partida.sacarCarta(ana);
            partida.sacarCarta(ana);

            assertThat(ana.getCartasDelante()).containsExactly(7, 3, 9);
            assertThat(partida.getJugadorActual()).isSameAs(ana);
            assertThat(partida.getMazo().cantidad()).isEqualTo(107);
        }

        @Test
        void soloPuedeSacarElJugadorDelTurno() {
            Partida partida = partida(DOS);

            assertThatThrownBy(() -> partida.sacarCarta(jugador(partida, 1)))
                    .isInstanceOf(ReglaJuegoException.class)
                    .hasMessage("No es el turno de Luis. Es el turno de Ana")
                    .extracting("codigo").isEqualTo(CodigoError.NO_ES_SU_TURNO);
            assertThat(partida.getMazo().cantidad()).isEqualTo(110);
        }
    }

    // ================================================================== regla de pérdida

    @Nested
    @DisplayName("Regla de pérdida del turno")
    class ReglaDePerdida {

        @Test
        @DisplayName("Repetición con menos de 3 cartas: no pierde y la carta queda delante")
        void repeticionConDosCartasNoPierde() {
            Partida partida = partida(DOS, 4, 7, 7);
            Jugador ana = jugador(partida, 0);

            partida.sacarCarta(ana);
            partida.sacarCarta(ana);
            partida.sacarCarta(ana);

            assertThat(ana.getCartasDelante()).containsExactly(4, 7, 7);
            assertThat(partida.getJugadorActual()).isSameAs(ana);
            assertThat(partida.getCartasDescartadas()).isZero();
            assertThat(partida.getUltimoEvento()).isEqualTo(TipoEvento.CARTA_SACADA);
        }

        @Test
        @DisplayName("Repetición teniendo exactamente 3 cartas: pierde")
        void repeticionConTresCartasPierde() {
            Partida partida = partida(DOS, 4, 7, 9, 7);
            Jugador ana = jugador(partida, 0);
            partida.sacarCarta(ana);
            partida.sacarCarta(ana);
            partida.sacarCarta(ana);

            partida.sacarCarta(ana);

            assertThat(ana.getCartasDelante()).isEmpty();
            assertThat(ana.getMontonPuntos()).isEmpty();
            assertThat(partida.getCartasDescartadas()).isEqualTo(4);
            assertThat(partida.getUltimaCarta()).isEqualTo(7);
            assertThat(partida.getJugadorActual().getNombre()).isEqualTo("Luis");
            assertThat(partida.getUltimoEvento()).isEqualTo(TipoEvento.TURNO_PERDIDO);
            assertThat(partida.getMensajeEvento()).isEqualTo(
                    "Ana sacó un 7 repetido y perdió el turno. Se descartaron 4 carta(s). Turno de Luis");
        }

        @Test
        @DisplayName("Repetición teniendo más de 3 cartas: pierde todas")
        void repeticionConMasDeTresCartasPierde() {
            Partida partida = partida(DOS, 4, 7, 9, 2, 9);
            Jugador ana = jugador(partida, 0);
            for (int i = 0; i < 5; i++) {
                partida.sacarCarta(ana);
            }

            assertThat(ana.getCartasDelante()).isEmpty();
            assertThat(partida.getCartasDescartadas()).isEqualTo(5);
            assertThat(partida.getJugadorActual().getNombre()).isEqualTo("Luis");
        }

        @Test
        @DisplayName("Carta no repetida con 3 cartas delante: no pierde")
        void cartaNoRepetidaNoPierde() {
            Partida partida = partida(DOS, 4, 7, 9, 2);
            Jugador ana = jugador(partida, 0);
            for (int i = 0; i < 4; i++) {
                partida.sacarCarta(ana);
            }

            assertThat(ana.getCartasDelante()).containsExactly(4, 7, 9, 2);
            assertThat(partida.getJugadorActual()).isSameAs(ana);
        }

        @Test
        @DisplayName("Caso límite: 7, 7, 7 no pierde (se cuenta antes de colocar); el cuarto 7 sí")
        void casoLimiteCartasIguales() {
            Partida partida = partida(DOS, 7, 7, 7, 7);
            Jugador ana = jugador(partida, 0);
            partida.sacarCarta(ana);
            partida.sacarCarta(ana);
            partida.sacarCarta(ana);

            assertThat(ana.getCartasDelante()).containsExactly(7, 7, 7);

            partida.sacarCarta(ana);

            assertThat(ana.getCartasDelante()).isEmpty();
            assertThat(partida.getCartasDescartadas()).isEqualTo(4);
        }

        @Test
        @DisplayName("Al perder, el siguiente jugador empieza su turno limpio")
        void elSiguienteEmpiezaLimpio() {
            Partida partida = partida(DOS, 4, 7, 9, 7);
            Jugador ana = jugador(partida, 0);
            for (int i = 0; i < 4; i++) {
                partida.sacarCarta(ana);
            }

            assertThat(partida.isHaSacadoEnTurno()).isFalse();
            assertThat(partida.getValorRobable()).isNull();
            assertThat(partida.puedePlantarse()).isFalse();
            assertThat(partida.puedeRobar()).isFalse();
        }

        @Test
        @DisplayName("Las cartas guardadas no se pierden al perder el turno")
        void lasGuardadasEstanASalvo() {
            Partida partida = partida(DOS, 5, 1, 4, 7, 9, 4);
            Jugador ana = jugador(partida, 0);
            Jugador luis = jugador(partida, 1);
            partida.sacarCarta(ana);
            partida.plantarse(ana);
            partida.sacarCarta(luis);
            partida.plantarse(luis);
            partida.guardarPelusas(ana);

            partida.sacarCarta(ana);
            partida.sacarCarta(ana);
            partida.sacarCarta(ana);
            partida.sacarCarta(ana);

            assertThat(ana.getMontonPuntos()).containsExactly(5);
            assertThat(ana.getCartasDelante()).isEmpty();
        }

        @Test
        @DisplayName("Los descartes se acumulan entre turnos")
        void descartesAcumulados() {
            Partida partida = partida(DOS, 1, 2, 3, 1, 4, 5, 6, 4);
            Jugador ana = jugador(partida, 0);
            Jugador luis = jugador(partida, 1);
            for (int i = 0; i < 4; i++) {
                partida.sacarCarta(ana);
            }
            for (int i = 0; i < 4; i++) {
                partida.sacarCarta(luis);
            }

            assertThat(partida.getCartasDescartadas()).isEqualTo(8);
            assertThat(partida.getJugadorActual()).isSameAs(ana);
        }
    }

    // ================================================================== guardar pelusas

    @Nested
    @DisplayName("Guardar pelusas")
    class GuardarPelusas {

        private Partida conCartasDeTurnoAnterior() {
            Partida partida = partida(DOS, 7, 3, 3, 8);
            Jugador ana = jugador(partida, 0);
            Jugador luis = jugador(partida, 1);
            partida.sacarCarta(ana);
            partida.sacarCarta(ana);
            partida.plantarse(ana);
            partida.sacarCarta(luis);
            partida.plantarse(luis);
            return partida;
        }

        @Test
        @DisplayName("Al inicio del turno guarda las cartas que sobrevivieron")
        void guardaAlInicioDelTurno() {
            Partida partida = conCartasDeTurnoAnterior();
            Jugador ana = jugador(partida, 0);

            assertThat(partida.puedeGuardar()).isTrue();
            partida.guardarPelusas(ana);

            assertThat(ana.getCartasDelante()).isEmpty();
            assertThat(ana.getMontonPuntos()).containsExactly(7, 3);
            assertThat(ana.puntos()).isEqualTo(10);
            assertThat(partida.getUltimoEvento()).isEqualTo(TipoEvento.PELUSAS_GUARDADAS);
            assertThat(partida.getMensajeEvento()).isEqualTo("Ana guardó 2 carta(s) en su montón de puntos");
            assertThat(partida.getJugadorActual()).isSameAs(ana);
            assertThat(partida.puedeGuardar()).isFalse();
            assertThat(partida.puedePlantarse()).isFalse();
        }

        @Test
        @DisplayName("Si saca sin haber guardado, ya no puede guardar en este turno")
        void noPuedeGuardarDespuesDeSacar() {
            Partida partida = conCartasDeTurnoAnterior();
            Jugador ana = jugador(partida, 0);
            partida.sacarCarta(ana);

            assertThat(partida.puedeGuardar()).isFalse();
            assertThatThrownBy(() -> partida.guardarPelusas(ana))
                    .isInstanceOf(ReglaJuegoException.class)
                    .hasMessage("Solo se pueden guardar pelusas al inicio del turno, antes de sacar una carta")
                    .extracting("codigo").isEqualTo(CodigoError.NO_PUEDE_GUARDAR);
            assertThat(ana.getCartasDelante()).containsExactly(7, 3, 8);
            assertThat(ana.getMontonPuntos()).isEmpty();
        }

        @Test
        void noPuedeGuardarSinCartas() {
            Partida partida = partida(DOS);

            assertThatThrownBy(() -> partida.guardarPelusas(jugador(partida, 0)))
                    .isInstanceOf(ReglaJuegoException.class)
                    .hasMessage("No tienes cartas delante para guardar")
                    .extracting("codigo").isEqualTo(CodigoError.NO_PUEDE_GUARDAR);
        }

        @Test
        void noPuedeGuardarFueraDeSuTurno() {
            Partida partida = conCartasDeTurnoAnterior();

            assertError(() -> partida.guardarPelusas(jugador(partida, 1)), CodigoError.NO_ES_SU_TURNO);
        }

        @Test
        @DisplayName("Después de guardar puede seguir sacando cartas")
        void puedeSacarDespuesDeGuardar() {
            Partida partida = conCartasDeTurnoAnterior();
            Jugador ana = jugador(partida, 0);
            partida.guardarPelusas(ana);

            partida.sacarCarta(ana);

            assertThat(ana.getCartasDelante()).hasSize(1);
            assertThat(ana.getMontonPuntos()).containsExactly(7, 3);
        }
    }

    // ================================================================== robar

    @Nested
    @DisplayName("Robar cartas")
    class RobarCartas {

        @Test
        @DisplayName("Roba las cartas del mismo valor a todos los demás jugadores")
        void robaATodos() {
            Partida partida = partida(TRES, 7, 3, 7, 7);
            Jugador ana = jugador(partida, 0);
            Jugador luis = jugador(partida, 1);
            Jugador eva = jugador(partida, 2);
            partida.sacarCarta(ana);
            partida.sacarCarta(ana);
            partida.plantarse(ana);
            partida.sacarCarta(luis);
            partida.plantarse(luis);
            partida.sacarCarta(eva);

            assertThat(partida.puedeRobar()).isTrue();
            assertThat(partida.getValorRobable()).isEqualTo(7);
            partida.robarCartas(eva);

            assertThat(eva.getCartasDelante()).containsExactly(7, 7, 7);
            assertThat(ana.getCartasDelante()).containsExactly(3);
            assertThat(luis.getCartasDelante()).isEmpty();
            assertThat(partida.getUltimoEvento()).isEqualTo(TipoEvento.CARTAS_ROBADAS);
            assertThat(partida.getMensajeEvento()).isEqualTo("Eva robó 2 carta(s) de valor 7");
            assertThat(partida.getJugadorActual()).isSameAs(eva);
        }

        @Test
        @DisplayName("Después de robar no puede volver a robar, pero sí continuar o plantarse")
        void soloUnRoboPorCarta() {
            Partida partida = partida(DOS, 7, 7);
            Jugador ana = jugador(partida, 0);
            Jugador luis = jugador(partida, 1);
            partida.sacarCarta(ana);
            partida.plantarse(ana);
            partida.sacarCarta(luis);
            partida.robarCartas(luis);

            assertThat(partida.puedeRobar()).isFalse();
            assertThat(partida.getValorRobable()).isNull();
            assertThat(partida.puedeSacar()).isTrue();
            assertThat(partida.puedePlantarse()).isTrue();
            assertError(() -> partida.robarCartas(luis), CodigoError.NO_PUEDE_ROBAR);
        }

        @Test
        @DisplayName("Puede robar después de sacar una repetida sin perder")
        void roboTrasRepetidaSinPerder() {
            Partida partida = partida(DOS, 7, 4, 7, 7);
            Jugador ana = jugador(partida, 0);
            Jugador luis = jugador(partida, 1);
            partida.sacarCarta(ana);
            partida.plantarse(ana);
            partida.sacarCarta(luis);
            partida.sacarCarta(luis);

            partida.sacarCarta(luis); // segundo 7 con 2 cartas delante: no pierde

            assertThat(luis.getCartasDelante()).containsExactly(4, 7, 7);
            assertThat(partida.puedeRobar()).isTrue();
            partida.robarCartas(luis);
            assertThat(luis.getCartasDelante()).containsExactly(4, 7, 7, 7);
            assertThat(ana.getCartasDelante()).isEmpty();
        }

        @Test
        @DisplayName("Si saca otra carta sin robar, pierde la oportunidad de robar el valor anterior")
        void laOportunidadSeCierraAlSacarOtraCarta() {
            Partida partida = partida(DOS, 7, 7, 5);
            Jugador ana = jugador(partida, 0);
            Jugador luis = jugador(partida, 1);
            partida.sacarCarta(ana);
            partida.plantarse(ana);
            partida.sacarCarta(luis);
            assertThat(partida.puedeRobar()).isTrue();

            partida.sacarCarta(luis);

            assertThat(partida.getValorRobable()).isEqualTo(5);
            assertThat(partida.puedeRobar()).isFalse();
            assertError(() -> partida.robarCartas(luis), CodigoError.NO_PUEDE_ROBAR);
            assertThat(ana.getCartasDelante()).containsExactly(7);
        }

        @Test
        @DisplayName("No puede robar si ningún otro jugador tiene ese valor")
        void nadieTieneEseValor() {
            Partida partida = partida(DOS, 7, 7, 3);
            Jugador ana = jugador(partida, 0);
            partida.sacarCarta(ana);
            partida.sacarCarta(ana); // Ana tiene 7, 7 pero son suyas

            assertThat(partida.puedeRobar()).isFalse();
            assertThatThrownBy(() -> partida.robarCartas(ana))
                    .isInstanceOf(ReglaJuegoException.class)
                    .hasMessage("Solo puedes robar justo después de sacar una carta cuyo valor tenga otro jugador delante");
        }

        @Test
        @DisplayName("No puede robar al inicio del turno, antes de sacar")
        void noPuedeRobarAntesDeSacar() {
            Partida partida = partida(DOS, 7, 3);
            Jugador ana = jugador(partida, 0);
            Jugador luis = jugador(partida, 1);
            partida.sacarCarta(ana);
            partida.plantarse(ana);

            assertThat(partida.puedeRobar()).isFalse();
            assertError(() -> partida.robarCartas(luis), CodigoError.NO_PUEDE_ROBAR);
        }

        @Test
        @DisplayName("Las cartas guardadas no se pueden robar")
        void lasGuardadasNoSeRoban() {
            Partida partida = partida(DOS, 7, 3, 9, 7);
            Jugador ana = jugador(partida, 0);
            Jugador luis = jugador(partida, 1);
            partida.sacarCarta(ana);
            partida.plantarse(ana);
            partida.sacarCarta(luis);
            partida.plantarse(luis);
            partida.guardarPelusas(ana);
            partida.sacarCarta(ana);
            partida.plantarse(ana);

            partida.sacarCarta(luis);

            assertThat(partida.puedeRobar()).isFalse();
            assertThat(ana.getMontonPuntos()).containsExactly(7);
        }

        @Test
        void noPuedeRobarFueraDeSuTurno() {
            Partida partida = partida(DOS, 7, 7);
            Jugador ana = jugador(partida, 0);
            Jugador luis = jugador(partida, 1);
            partida.sacarCarta(ana);
            partida.plantarse(ana);
            partida.sacarCarta(luis);

            assertError(() -> partida.robarCartas(ana), CodigoError.NO_ES_SU_TURNO);
        }
    }

    // ================================================================== plantarse y turnos

    @Nested
    @DisplayName("Finalizar voluntariamente el turno")
    class Plantarse {

        @Test
        @DisplayName("Al plantarse las cartas quedan delante y el turno pasa al siguiente")
        void plantarseConservaLasCartas() {
            Partida partida = partida(DOS, 5);
            Jugador ana = jugador(partida, 0);
            partida.sacarCarta(ana);

            partida.plantarse(ana);

            assertThat(ana.getCartasDelante()).containsExactly(5);
            assertThat(ana.getMontonPuntos()).isEmpty();
            assertThat(partida.getJugadorActual().getNombre()).isEqualTo("Luis");
            assertThat(partida.isHaSacadoEnTurno()).isFalse();
            assertThat(partida.getUltimoEvento()).isEqualTo(TipoEvento.PLANTADO);
            assertThat(partida.getMensajeEvento()).isEqualTo("Ana se plantó. Turno de Luis");
        }

        @Test
        @DisplayName("Plantarse cierra la oportunidad de robar")
        void plantarseCierraElRobo() {
            Partida partida = partida(DOS, 7, 7);
            Jugador ana = jugador(partida, 0);
            Jugador luis = jugador(partida, 1);
            partida.sacarCarta(ana);
            partida.plantarse(ana);
            partida.sacarCarta(luis);

            partida.plantarse(luis);

            assertThat(partida.getValorRobable()).isNull();
            assertThat(partida.puedeRobar()).isFalse();
        }

        @Test
        @DisplayName("Debe sacar al menos una carta antes de plantarse")
        void debeSacarAntesDePlantarse() {
            Partida partida = partida(DOS);

            assertThatThrownBy(() -> partida.plantarse(jugador(partida, 0)))
                    .isInstanceOf(ReglaJuegoException.class)
                    .hasMessage("Debes sacar al menos una carta antes de plantarte")
                    .extracting("codigo").isEqualTo(CodigoError.NO_PUEDE_PLANTARSE);
        }

        @Test
        void noPuedePlantarseFueraDeSuTurno() {
            Partida partida = partida(DOS, 5);
            partida.sacarCarta(jugador(partida, 0));

            assertError(() -> partida.plantarse(jugador(partida, 1)), CodigoError.NO_ES_SU_TURNO);
        }

        @Test
        @DisplayName("Los turnos rotan en orden y vuelven al primer jugador")
        void rotacionDeTurnos() {
            Partida partida = partida(TRES, 1, 2, 3, 4);
            List<String> turnos = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                Jugador actual = partida.getJugadorActual();
                turnos.add(actual.getNombre());
                partida.sacarCarta(actual);
                partida.plantarse(actual);
            }

            assertThat(turnos).containsExactly("Ana", "Luis", "Eva", "Ana");
            assertThat(partida.getJugadorActual().getNombre()).isEqualTo("Luis");
        }
    }

    // ================================================================== comodín

    @Nested
    @DisplayName("Comodín")
    class Comodin {

        @Test
        @DisplayName("Cada jugador inicia con su comodín disponible")
        void disponibleAlInicio() {
            Partida partida = partida(DOS);

            assertThat(partida.puedeUsarComodin()).isTrue();
            assertThat(partida.getJugadores()).allSatisfy(j -> {
                assertThat(j.isComodinUsado()).isFalse();
                assertThat(j.isProtegido()).isFalse();
            });
        }

        @Test
        @DisplayName("Usar el comodín protege al jugador y lo gasta")
        void usarComodin() {
            Partida partida = partida(DOS, 7);
            Jugador ana = jugador(partida, 0);
            partida.sacarCarta(ana);

            partida.usarComodin(ana);

            assertThat(ana.isComodinUsado()).isTrue();
            assertThat(ana.isProtegido()).isTrue();
            assertThat(partida.puedeUsarComodin()).isFalse();
            assertThat(partida.getUltimoEvento()).isEqualTo(TipoEvento.COMODIN_USADO);
            assertThat(partida.getMensajeEvento())
                    .isEqualTo("Ana usó su comodín: nadie podrá robarle sus cartas hasta su próximo turno");
            assertThat(partida.getJugadorActual()).isSameAs(ana);
            assertThat(partida.puedePlantarse()).isTrue();
        }

        @Test
        @DisplayName("Con el comodín activo no le pueden robar sus cartas")
        void bloqueaElRobo() {
            Partida partida = partida(DOS, 7, 7);
            Jugador ana = jugador(partida, 0);
            Jugador luis = jugador(partida, 1);
            partida.sacarCarta(ana);
            partida.usarComodin(ana);
            partida.plantarse(ana);

            partida.sacarCarta(luis);

            assertThat(partida.puedeRobar()).isFalse();
            assertError(() -> partida.robarCartas(luis), CodigoError.NO_PUEDE_ROBAR);
            assertThat(ana.getCartasDelante()).containsExactly(7);
            assertThat(ana.isProtegido()).isTrue();
        }

        @Test
        @DisplayName("Si varios tienen el valor, solo se roba a quienes no están protegidos")
        void soloRobaALosNoProtegidos() {
            Partida partida = partida(TRES, 7, 7, 7);
            Jugador ana = jugador(partida, 0);
            Jugador luis = jugador(partida, 1);
            Jugador eva = jugador(partida, 2);
            partida.sacarCarta(ana);
            partida.usarComodin(ana);
            partida.plantarse(ana);
            partida.sacarCarta(luis);
            partida.plantarse(luis);
            partida.sacarCarta(eva);

            assertThat(partida.puedeRobar()).isTrue();
            partida.robarCartas(eva);

            assertThat(ana.getCartasDelante()).containsExactly(7);
            assertThat(luis.getCartasDelante()).isEmpty();
            assertThat(eva.getCartasDelante()).containsExactly(7, 7);
            assertThat(partida.getMensajeEvento()).isEqualTo("Eva robó 1 carta(s) de valor 7");
        }

        @Test
        @DisplayName("Protege también las cartas que saca después de activarlo en el mismo turno")
        void protegeCartasSacadasDespues() {
            Partida partida = partida(DOS, 7, 7);
            Jugador ana = jugador(partida, 0);
            Jugador luis = jugador(partida, 1);
            partida.usarComodin(ana); // al inicio del turno, antes de sacar
            partida.sacarCarta(ana);
            partida.plantarse(ana);

            partida.sacarCarta(luis);

            assertThat(partida.puedeRobar()).isFalse();
            assertThat(ana.getCartasDelante()).containsExactly(7);
        }

        @Test
        @DisplayName("La protección termina cuando vuelve a empezar su turno")
        void protegeHastaSuProximoTurno() {
            Partida partida = partida(DOS, 7, 3, 9, 7);
            Jugador ana = jugador(partida, 0);
            Jugador luis = jugador(partida, 1);
            partida.sacarCarta(ana);
            partida.usarComodin(ana);
            partida.plantarse(ana);
            partida.sacarCarta(luis);
            partida.plantarse(luis);

            assertThat(ana.isProtegido()).isFalse();
            assertThat(ana.isComodinUsado()).isTrue();

            partida.sacarCarta(ana);
            partida.plantarse(ana);
            partida.sacarCarta(luis);

            assertThat(partida.puedeRobar()).isTrue();
            partida.robarCartas(luis);
            assertThat(ana.getCartasDelante()).containsExactly(9);
        }

        @Test
        @DisplayName("La protección también termina si el turno le vuelve por una pérdida de otro jugador")
        void terminaTrasPerdidaDeOtro() {
            Partida partida = partida(DOS, 8, 1, 2, 3, 1);
            Jugador ana = jugador(partida, 0);
            Jugador luis = jugador(partida, 1);
            partida.sacarCarta(ana);
            partida.usarComodin(ana);
            partida.plantarse(ana);
            for (int i = 0; i < 4; i++) {
                partida.sacarCarta(luis);
            }

            assertThat(partida.getJugadorActual()).isSameAs(ana);
            assertThat(ana.isProtegido()).isFalse();
        }

        @Test
        @DisplayName("Solo se puede usar una vez en toda la partida")
        void soloUnaVez() {
            Partida partida = partida(DOS, 7, 3);
            Jugador ana = jugador(partida, 0);
            Jugador luis = jugador(partida, 1);
            partida.usarComodin(ana);

            assertThatThrownBy(() -> partida.usarComodin(ana))
                    .isInstanceOf(ReglaJuegoException.class)
                    .hasMessage("Ya usaste tu comodín en esta partida")
                    .extracting("codigo").isEqualTo(CodigoError.COMODIN_YA_USADO);

            partida.sacarCarta(ana);
            partida.plantarse(ana);
            partida.sacarCarta(luis);
            partida.plantarse(luis);

            assertThat(partida.puedeUsarComodin()).isFalse();
            assertError(() -> partida.usarComodin(ana), CodigoError.COMODIN_YA_USADO);
        }

        @Test
        @DisplayName("Cada jugador tiene su propio comodín")
        void cadaJugadorTieneElSuyo() {
            Partida partida = partida(DOS, 7);
            Jugador ana = jugador(partida, 0);
            Jugador luis = jugador(partida, 1);
            partida.usarComodin(ana);
            partida.sacarCarta(ana);
            partida.plantarse(ana);

            assertThat(partida.puedeUsarComodin()).isTrue();
            partida.usarComodin(luis);
            assertThat(luis.isComodinUsado()).isTrue();
        }

        @Test
        void soloEnSuTurno() {
            Partida partida = partida(DOS);

            assertError(() -> partida.usarComodin(jugador(partida, 1)), CodigoError.NO_ES_SU_TURNO);
            assertThat(jugador(partida, 1).isComodinUsado()).isFalse();
        }

        @Test
        void noSePuedeUsarConLaPartidaTerminada() {
            Partida partida = jugarHastaTerminar(partida(DOS));

            assertThat(partida.puedeUsarComodin()).isFalse();
            assertError(() -> partida.usarComodin(partida.getJugadorActual()), CodigoError.PARTIDA_FINALIZADA);
        }
    }

    // ================================================================== fin de la partida

    @Nested
    @DisplayName("Finalización de la partida")
    class Finalizacion {

        @Test
        @DisplayName("La partida termina al agotarse el montón de polvo")
        void terminaAlAgotarseElMazo() {
            Partida partida = jugarHastaTerminar(partida(DOS));

            assertThat(partida.getEstado()).isEqualTo(EstadoPartida.FINALIZADA);
            assertThat(partida.estaEnCurso()).isFalse();
            assertThat(partida.getMazo().estaVacio()).isTrue();
            assertThat(partida.getFechaFin()).isNotNull();
            assertThat(partida.getUltimoEvento()).isEqualTo(TipoEvento.PARTIDA_FINALIZADA);
        }

        @Test
        @DisplayName("Al terminar, las cartas de delante se guardan automáticamente y se suman los puntos")
        void guardadoAutomaticoYPuntuacion() {
            Partida partida = jugarHastaTerminar(partida(DOS));

            int cartasTotales = 0;
            for (Jugador jugador : partida.getJugadores()) {
                assertThat(jugador.getCartasDelante()).isEmpty();
                assertThat(jugador.getPuntuacionFinal()).isEqualTo(jugador.puntos());
                cartasTotales += jugador.getMontonPuntos().size();
            }
            assertThat(cartasTotales + partida.getCartasDescartadas()).isEqualTo(110);
        }

        @Test
        @DisplayName("La última carta sacada también se guarda")
        void laUltimaCartaSeGuarda() {
            Partida partida = jugarHastaTerminar(partida(DOS));
            Jugador ana = jugador(partida, 0);
            Jugador luis = jugador(partida, 1);

            assertThat(ana.getMontonPuntos()).hasSize(55);
            assertThat(luis.getMontonPuntos()).hasSize(55);
            assertThat(ana.puntos() + luis.puntos()).isEqualTo(555);
        }

        @Test
        @DisplayName("Se marca como ganador a quien indica el criterio de desempate")
        void seDeterminaElGanador() {
            Partida partida = jugarHastaTerminar(partida(TRES));

            List<Jugador> esperados = Desempate.ganadores(partida.getJugadores());
            assertThat(partida.getGanadores()).containsExactlyElementsOf(esperados);
            assertThat(partida.getJugadores()).filteredOn(Jugador::isGanador).hasSize(esperados.size());
            assertThat(partida.getMensajeEvento()).isEqualTo(Partida.mensajeResultado(esperados));
        }

        @Test
        @DisplayName("Una partida terminada no admite más acciones")
        void noAdmiteAccionesAlTerminar() {
            Partida partida = jugarHastaTerminar(partida(DOS));
            Jugador actual = partida.getJugadorActual();

            assertThat(partida.puedeSacar()).isFalse();
            assertThat(partida.puedeGuardar()).isFalse();
            assertThat(partida.puedeRobar()).isFalse();
            assertThat(partida.puedePlantarse()).isFalse();
            assertThat(partida.isHaSacadoEnTurno()).isFalse();
            assertThat(partida.getValorRobable()).isNull();
            assertThatThrownBy(() -> partida.sacarCarta(actual))
                    .isInstanceOf(ReglaJuegoException.class)
                    .hasMessage("La partida ya terminó")
                    .extracting("codigo").isEqualTo(CodigoError.PARTIDA_FINALIZADA);
            assertError(() -> partida.guardarPelusas(actual), CodigoError.PARTIDA_FINALIZADA);
            assertError(() -> partida.robarCartas(actual), CodigoError.PARTIDA_FINALIZADA);
            assertError(() -> partida.plantarse(actual), CodigoError.PARTIDA_FINALIZADA);
        }

        @Test
        @DisplayName("Si nadie se planta, los jugadores pierden turnos y la partida igual termina con las 110 cartas")
        void partidaSinPlantarse() {
            List<Integer> cartas = new ArrayList<>(Mazo.cartasOficiales());
            Collections.shuffle(cartas, new Random(3));
            Partida partida = Partida.crear(DOS, cartas);
            // Nadie se planta ni guarda: acumulan cartas y pierden el turno hasta que se agota el mazo
            while (partida.estaEnCurso()) {
                partida.sacarCarta(partida.getJugadorActual());
            }

            assertThat(partida.getEstado()).isEqualTo(EstadoPartida.FINALIZADA);
            int guardadas = partida.getJugadores().stream().mapToInt(j -> j.getMontonPuntos().size()).sum();
            assertThat(guardadas + partida.getCartasDescartadas()).isEqualTo(110);
            assertThat(partida.getCartasDescartadas()).isPositive();
        }

        @Test
        void mensajeConUnGanador() {
            Jugador ana = new Jugador("Ana", 0, null);
            ana.colocarDelante(List.of(10, 5));
            ana.guardarCartasDelante();

            assertThat(Partida.mensajeResultado(List.of(ana))).isEqualTo("Ganó Ana con 15 puntos");
        }

        @Test
        void mensajeConEmpate() {
            Jugador ana = new Jugador("Ana", 0, null);
            Jugador luis = new Jugador("Luis", 1, null);
            ana.colocarDelante(List.of(4));
            luis.colocarDelante(List.of(4));
            ana.guardarCartasDelante();
            luis.guardarCartasDelante();

            assertThat(Partida.mensajeResultado(List.of(ana, luis))).isEqualTo("Empate entre Ana y Luis con 4 puntos");
        }
    }

    // ================================================================== invariantes

    @Nested
    @DisplayName("Invariantes")
    class Invariantes {

        private int cartasEnJuego(Partida partida) {
            int total = partida.getMazo().cantidad() + partida.getCartasDescartadas();
            for (Jugador jugador : partida.getJugadores()) {
                total += jugador.getCartasDelante().size() + jugador.getMontonPuntos().size();
            }
            return total;
        }

        @ParameterizedTest(name = "semilla {0}")
        @ValueSource(longs = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10})
        @DisplayName("Nunca se crean ni se pierden cartas: siempre suman 110")
        void siempreHay110Cartas(long semilla) {
            List<Integer> cartas = new ArrayList<>(Mazo.cartasOficiales());
            Random azar = new Random(semilla);
            Collections.shuffle(cartas, azar);
            Partida partida = Partida.crear(List.of("A", "B", "C", "D"), cartas);

            while (partida.estaEnCurso()) {
                Jugador actual = partida.getJugadorActual();
                if (partida.puedeGuardar() && azar.nextBoolean()) {
                    partida.guardarPelusas(actual);
                }
                partida.sacarCarta(actual);
                if (partida.puedeRobar()) {
                    partida.robarCartas(actual);
                }
                if (partida.puedePlantarse() && azar.nextInt(3) == 0) {
                    partida.plantarse(actual);
                }
                assertThat(cartasEnJuego(partida)).isEqualTo(110);
            }
            assertThat(partida.getGanadores()).isNotEmpty();
        }

        @Test
        void buscarJugadorInexistenteLanzaError() {
            Partida partida = partida(DOS);

            assertThatThrownBy(() -> partida.buscarJugador(99L))
                    .isInstanceOf(ReglaJuegoException.class)
                    .hasMessage("El jugador 99 no pertenece a la partida")
                    .extracting("codigo").isEqualTo(CodigoError.JUGADOR_NO_PERTENECE);
        }

        @Test
        void listaDeJugadoresNoModificable() {
            Partida partida = partida(DOS);

            assertThatThrownBy(() -> partida.getJugadores().clear()).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void mazoConCartasSuperioresEsValido() {
            assertThat(mazoConCartasSuperiores(7, 7, 1)).hasSize(110);
        }
    }
}
