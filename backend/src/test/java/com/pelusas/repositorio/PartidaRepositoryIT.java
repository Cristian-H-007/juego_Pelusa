package com.pelusas.repositorio;

import com.pelusas.dominio.EstadoPartida;
import com.pelusas.dominio.Jugador;
import com.pelusas.dominio.Partida;
import com.pelusas.dominio.TipoEvento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.List;

import static com.pelusas.dominio.PartidasDePrueba.jugarHastaTerminar;
import static com.pelusas.dominio.PartidasDePrueba.partida;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integración con la base de datos: usa una base H2 real (en memoria), sin mocks.
 * Cada prueba guarda, limpia la caché de JPA y vuelve a leer desde la base para comprobar lo persistido.
 */
@DataJpaTest
class PartidaRepositoryIT {

    @Autowired
    private PartidaRepository repositorio;

    @Autowired
    private TestEntityManager em;

    private Partida guardarYRecargar(Partida partida) {
        Long id = repositorio.saveAndFlush(partida).getId();
        em.clear();
        return repositorio.findById(id).orElseThrow();
    }

    @Test
    @DisplayName("Persiste la partida con sus jugadores y su mazo")
    void persisteLaPartida() {
        Partida recuperada = guardarYRecargar(partida(List.of("Ana", "Luis", "Eva"), 9, 4));

        assertThat(recuperada.getId()).isNotNull();
        assertThat(recuperada.getEstado()).isEqualTo(EstadoPartida.EN_CURSO);
        assertThat(recuperada.getFechaCreacion()).isNotNull();
        assertThat(recuperada.getMazo().cantidad()).isEqualTo(110);
        assertThat(recuperada.getMazo().cantidadDeValor(1)).isEqualTo(13);
        assertThat(recuperada.getUltimoEvento()).isEqualTo(TipoEvento.PARTIDA_CREADA);
        assertThat(recuperada.getJugadores()).extracting(Jugador::getNombre).containsExactly("Ana", "Luis", "Eva");
        assertThat(recuperada.getJugadores()).allSatisfy(j -> assertThat(j.getId()).isNotNull());
    }

    @Test
    @DisplayName("El orden del mazo se conserva: la carta superior sigue siendo la misma")
    void conservaElOrdenDelMazo() {
        Partida recuperada = guardarYRecargar(partida(List.of("Ana", "Luis"), 9, 4));

        assertThat(recuperada.getMazo().sacar()).isEqualTo(9);
        assertThat(recuperada.getMazo().sacar()).isEqualTo(4);
    }

    @Test
    @DisplayName("Persiste la actualización del estado del juego")
    void actualizaElEstado() {
        Partida partida = guardarYRecargar(partida(List.of("Ana", "Luis"), 7, 3, 7));
        Jugador ana = partida.getJugadores().get(0);
        Jugador luis = partida.getJugadores().get(1);
        partida.sacarCarta(ana);
        partida.sacarCarta(ana);
        partida.plantarse(ana);
        partida.sacarCarta(luis);

        Partida recuperada = guardarYRecargar(partida);

        assertThat(recuperada.getMazo().cantidad()).isEqualTo(107);
        assertThat(recuperada.getJugadorActual().getNombre()).isEqualTo("Luis");
        assertThat(recuperada.isHaSacadoEnTurno()).isTrue();
        assertThat(recuperada.getValorRobable()).isEqualTo(7);
        assertThat(recuperada.getUltimaCarta()).isEqualTo(7);
        assertThat(recuperada.puedeRobar()).isTrue();
        assertThat(recuperada.getJugadores().get(0).getCartasDelante()).containsExactly(7, 3);
        assertThat(recuperada.getJugadores().get(1).getCartasDelante()).containsExactly(7);
        assertThat(recuperada.getMensajeEvento()).isEqualTo("Luis sacó un 7");
    }

    @Test
    @DisplayName("Persiste el uso del comodín y la protección")
    void persisteElComodin() {
        Partida partida = guardarYRecargar(partida(List.of("Ana", "Luis"), 7));
        partida.usarComodin(partida.getJugadores().get(0));

        Partida recuperada = guardarYRecargar(partida);

        assertThat(recuperada.getJugadores().get(0).isComodinUsado()).isTrue();
        assertThat(recuperada.getJugadores().get(0).isProtegido()).isTrue();
        assertThat(recuperada.getJugadores().get(1).isComodinUsado()).isFalse();
        assertThat(recuperada.getUltimoEvento()).isEqualTo(TipoEvento.COMODIN_USADO);
    }

    @Test
    @DisplayName("Persiste los resultados de una partida terminada")
    void persisteElResultado() {
        Partida recuperada = guardarYRecargar(jugarHastaTerminar(partida(List.of("Ana", "Luis"))));

        assertThat(recuperada.getEstado()).isEqualTo(EstadoPartida.FINALIZADA);
        assertThat(recuperada.getFechaFin()).isNotNull();
        assertThat(recuperada.getMazo().estaVacio()).isTrue();
        assertThat(recuperada.getGanadores()).isNotEmpty();
        assertThat(recuperada.getJugadores()).allSatisfy(j -> {
            assertThat(j.getMontonPuntos()).hasSize(55);
            assertThat(j.getPuntuacionFinal()).isEqualTo(j.puntos());
        });
    }

    @Test
    @DisplayName("Relación partida-jugadores: cada jugador queda asociado a su partida")
    void relacionEntrePartidaYJugadores() {
        Partida primera = guardarYRecargar(partida(List.of("Ana", "Luis")));
        guardarYRecargar(partida(List.of("Eva", "Juan", "Sara")));

        List<String> nombres = em.getEntityManager()
                .createQuery("select j.nombre from Jugador j where j.partida.id = :id order by j.posicion", String.class)
                .setParameter("id", primera.getId())
                .getResultList();

        assertThat(nombres).containsExactly("Ana", "Luis");
    }

    @Test
    @DisplayName("Histórico: solo partidas finalizadas, de la más reciente a la más antigua")
    void historicoDePartidas() {
        Long antigua = guardarYRecargar(jugarHastaTerminar(partida(List.of("Ana", "Luis")))).getId();
        guardarYRecargar(partida(List.of("Eva", "Juan")));
        Long reciente = guardarYRecargar(jugarHastaTerminar(partida(List.of("Sara", "Leo")))).getId();

        List<Partida> historico = repositorio.buscarPorEstado(EstadoPartida.FINALIZADA);

        assertThat(historico).extracting(Partida::getId).containsExactly(reciente, antigua);
        assertThat(repositorio.buscarPorEstado(EstadoPartida.EN_CURSO)).hasSize(1);
    }
}
