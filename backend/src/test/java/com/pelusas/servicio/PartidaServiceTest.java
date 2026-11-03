package com.pelusas.servicio;

import com.pelusas.api.dto.CrearPartidaRequest;
import com.pelusas.api.dto.DecisionRequest.Decision;
import com.pelusas.api.dto.JugadorResponse;
import com.pelusas.api.dto.PartidaResponse;
import com.pelusas.api.dto.ResultadoResponse;
import com.pelusas.dominio.CodigoError;
import com.pelusas.dominio.EstadoPartida;
import com.pelusas.dominio.Jugador;
import com.pelusas.dominio.Mazo;
import com.pelusas.dominio.Partida;
import com.pelusas.dominio.ReglaJuegoException;
import com.pelusas.dominio.TipoEvento;
import com.pelusas.repositorio.PartidaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static com.pelusas.dominio.PartidasDePrueba.jugarHastaTerminar;
import static com.pelusas.dominio.PartidasDePrueba.mazoConCartasSuperiores;
import static com.pelusas.dominio.PartidasDePrueba.partida;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Prueba unitaria del servicio: el repositorio (base de datos) y el barajador (azar) se simulan con Mockito
 * para aislar la lógica del caso de uso. Las reglas del juego se ejecutan con objetos reales del dominio.
 */
@ExtendWith(MockitoExtension.class)
class PartidaServiceTest {

    private static final long PARTIDA_ID = 1L;
    private static final long ANA_ID = 10L;
    private static final long LUIS_ID = 11L;

    @Mock
    private PartidaRepository repositorio;

    @Mock
    private Barajador barajador;

    @InjectMocks
    private PartidaService servicio;

    private Partida partida;

    /** Simula los identificadores que asignaría la base de datos. */
    private static Partida conIds(Partida partida) {
        ReflectionTestUtils.setField(partida, "id", PARTIDA_ID);
        List<Jugador> jugadores = partida.getJugadores();
        for (int i = 0; i < jugadores.size(); i++) {
            ReflectionTestUtils.setField(jugadores.get(i), "id", ANA_ID + i);
        }
        return partida;
    }

    @BeforeEach
    void prepararPartida() {
        partida = conIds(partida(List.of("Ana", "Luis"), 7, 7, 3));
    }

    private void repositorioDevuelvePartida() {
        when(repositorio.findById(PARTIDA_ID)).thenReturn(Optional.of(partida));
    }

    @Test
    @DisplayName("Crear: mezcla las 110 cartas oficiales con la semilla recibida y guarda la partida")
    void crearPartida() {
        when(barajador.barajar(Mazo.cartasOficiales(), 42L)).thenReturn(mazoConCartasSuperiores(9));
        when(repositorio.save(any(Partida.class))).thenAnswer(invocacion -> conIds(invocacion.getArgument(0)));

        PartidaResponse respuesta = servicio.crear(new CrearPartidaRequest(List.of("Ana", "Luis"), 42L));

        ArgumentCaptor<Partida> guardada = ArgumentCaptor.forClass(Partida.class);
        verify(repositorio).save(guardada.capture());
        assertThat(guardada.getValue().getJugadores()).extracting(Jugador::getNombre).containsExactly("Ana", "Luis");
        assertThat(respuesta.id()).isEqualTo(PARTIDA_ID);
        assertThat(respuesta.estado()).isEqualTo(EstadoPartida.EN_CURSO);
        assertThat(respuesta.cartasEnMazo()).isEqualTo(110);
        assertThat(respuesta.jugadorInicialId()).isEqualTo(ANA_ID);
        assertThat(respuesta.jugadorActualId()).isEqualTo(ANA_ID);
        assertThat(respuesta.ultimoEvento()).isEqualTo(TipoEvento.PARTIDA_CREADA);
        assertThat(respuesta.jugadores()).extracting(JugadorResponse::id).containsExactly(ANA_ID, LUIS_ID);
    }

    @Test
    @DisplayName("Crear con jugadores inválidos no llega a guardar nada")
    void crearInvalidaNoGuarda() {
        when(barajador.barajar(Mazo.cartasOficiales(), null)).thenReturn(Mazo.cartasOficiales());

        assertThatThrownBy(() -> servicio.crear(new CrearPartidaRequest(List.of("Solo"), null)))
                .isInstanceOf(ReglaJuegoException.class);
        verify(repositorio, never()).save(any());
    }

    @Test
    void obtenerPartida() {
        repositorioDevuelvePartida();

        PartidaResponse respuesta = servicio.obtener(PARTIDA_ID);

        assertThat(respuesta.id()).isEqualTo(PARTIDA_ID);
        assertThat(respuesta.acciones().puedeSacar()).isTrue();
    }

    @Test
    void obtenerPartidaInexistente() {
        when(repositorio.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.obtener(99L))
                .isInstanceOf(PartidaNoEncontradaException.class)
                .hasMessage("No existe la partida 99");
    }

    @Test
    void consultarJugadores() {
        repositorioDevuelvePartida();

        List<JugadorResponse> jugadores = servicio.jugadores(PARTIDA_ID);

        assertThat(jugadores).extracting(JugadorResponse::nombre).containsExactly("Ana", "Luis");
        assertThat(jugadores).extracting(JugadorResponse::posicion).containsExactly(0, 1);
    }

    @Test
    @DisplayName("Sacar: delega en el dominio y devuelve el nuevo estado")
    void sacarCarta() {
        repositorioDevuelvePartida();

        PartidaResponse respuesta = servicio.sacarCarta(PARTIDA_ID, ANA_ID);

        assertThat(respuesta.ultimaCarta()).isEqualTo(7);
        assertThat(respuesta.cartasEnMazo()).isEqualTo(109);
        assertThat(respuesta.jugadores().getFirst().cartasDelante()).containsExactly(7);
        assertThat(respuesta.acciones().puedePlantarse()).isTrue();
    }

    @Test
    @DisplayName("Sacar fuera de turno propaga la regla violada")
    void sacarFueraDeTurno() {
        repositorioDevuelvePartida();

        assertThatThrownBy(() -> servicio.sacarCarta(PARTIDA_ID, LUIS_ID))
                .isInstanceOf(ReglaJuegoException.class)
                .extracting("codigo").isEqualTo(CodigoError.NO_ES_SU_TURNO);
    }

    @Test
    void jugadorQueNoPerteneceALaPartida() {
        repositorioDevuelvePartida();

        assertThatThrownBy(() -> servicio.sacarCarta(PARTIDA_ID, 500L))
                .isInstanceOf(ReglaJuegoException.class)
                .extracting("codigo").isEqualTo(CodigoError.JUGADOR_NO_PERTENECE);
    }

    @Test
    @DisplayName("Decidir CONTINUAR saca otra carta")
    void decidirContinuar() {
        repositorioDevuelvePartida();
        servicio.sacarCarta(PARTIDA_ID, ANA_ID);

        PartidaResponse respuesta = servicio.decidir(PARTIDA_ID, ANA_ID, Decision.CONTINUAR);

        assertThat(respuesta.cartasEnMazo()).isEqualTo(108);
        assertThat(respuesta.jugadorActualId()).isEqualTo(ANA_ID);
        assertThat(respuesta.jugadores().getFirst().cartasDelante()).containsExactly(7, 7);
    }

    @Test
    @DisplayName("Decidir PARAR finaliza el turno")
    void decidirParar() {
        repositorioDevuelvePartida();
        servicio.sacarCarta(PARTIDA_ID, ANA_ID);

        PartidaResponse respuesta = servicio.decidir(PARTIDA_ID, ANA_ID, Decision.PARAR);

        assertThat(respuesta.jugadorActualId()).isEqualTo(LUIS_ID);
        assertThat(respuesta.ultimoEvento()).isEqualTo(TipoEvento.PLANTADO);
        assertThat(respuesta.jugadores().getFirst().cartasDelante()).containsExactly(7);
    }

    @Test
    void robarCartas() {
        repositorioDevuelvePartida();
        servicio.sacarCarta(PARTIDA_ID, ANA_ID);
        servicio.decidir(PARTIDA_ID, ANA_ID, Decision.PARAR);
        servicio.sacarCarta(PARTIDA_ID, LUIS_ID);

        PartidaResponse respuesta = servicio.robarCartas(PARTIDA_ID, LUIS_ID);

        assertThat(respuesta.ultimoEvento()).isEqualTo(TipoEvento.CARTAS_ROBADAS);
        assertThat(respuesta.jugadores().get(0).cartasDelante()).isEmpty();
        assertThat(respuesta.jugadores().get(1).cartasDelante()).containsExactly(7, 7);
    }

    @Test
    void guardarPelusas() {
        repositorioDevuelvePartida();
        servicio.sacarCarta(PARTIDA_ID, ANA_ID);
        servicio.decidir(PARTIDA_ID, ANA_ID, Decision.PARAR);
        servicio.sacarCarta(PARTIDA_ID, LUIS_ID);
        servicio.decidir(PARTIDA_ID, LUIS_ID, Decision.PARAR);

        PartidaResponse respuesta = servicio.guardarPelusas(PARTIDA_ID, ANA_ID);

        assertThat(respuesta.jugadores().getFirst().montonPuntos()).containsExactly(7);
        assertThat(respuesta.jugadores().getFirst().puntos()).isEqualTo(7);
        assertThat(respuesta.jugadores().getFirst().cartasDelante()).isEmpty();
    }

    @Test
    @DisplayName("El resultado solo está disponible cuando la partida terminó")
    void resultadoDePartidaEnCurso() {
        repositorioDevuelvePartida();

        assertThatThrownBy(() -> servicio.resultado(PARTIDA_ID))
                .isInstanceOf(ReglaJuegoException.class)
                .hasMessage("La partida 1 todavía está en curso")
                .extracting("codigo").isEqualTo(CodigoError.PARTIDA_NO_FINALIZADA);
    }

    @Test
    void resultadoDePartidaTerminada() {
        jugarHastaTerminar(partida);
        repositorioDevuelvePartida();

        ResultadoResponse resultado = servicio.resultado(PARTIDA_ID);

        assertThat(resultado.partidaId()).isEqualTo(PARTIDA_ID);
        assertThat(resultado.fechaFin()).isNotNull();
        assertThat(resultado.ganadores()).isNotEmpty();
        assertThat(resultado.jugadores()).hasSize(2);
        assertThat(resultado.jugadores()).allSatisfy(j ->
                assertThat(j.puntuacionFinal()).isEqualTo(j.cartasAcumuladas().stream().mapToInt(Integer::intValue).sum()));
    }

    @Test
    @DisplayName("El histórico consulta solo partidas finalizadas")
    void historico() {
        jugarHastaTerminar(partida);
        when(repositorio.buscarPorEstado(EstadoPartida.FINALIZADA)).thenReturn(List.of(partida));

        List<ResultadoResponse> historico = servicio.historico();

        assertThat(historico).hasSize(1);
        assertThat(historico.getFirst().partidaId()).isEqualTo(PARTIDA_ID);
        verify(repositorio).buscarPorEstado(EstadoPartida.FINALIZADA);
    }
}
