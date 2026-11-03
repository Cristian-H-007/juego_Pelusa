package com.pelusas.servicio;

import com.pelusas.api.dto.CrearPartidaRequest;
import com.pelusas.api.dto.DecisionRequest;
import com.pelusas.api.dto.JugadorResponse;
import com.pelusas.api.dto.PartidaResponse;
import com.pelusas.api.dto.ResultadoResponse;
import com.pelusas.dominio.CodigoError;
import com.pelusas.dominio.EstadoPartida;
import com.pelusas.dominio.Jugador;
import com.pelusas.dominio.Mazo;
import com.pelusas.dominio.Partida;
import com.pelusas.dominio.ReglaJuegoException;
import com.pelusas.repositorio.PartidaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Casos de uso de la API. Carga la partida, delega la regla en el dominio y devuelve el nuevo estado.
 * Los cambios se persisten al confirmar la transacción.
 */
@Service
@Transactional
public class PartidaService {

    private final PartidaRepository repositorio;
    private final Barajador barajador;

    public PartidaService(PartidaRepository repositorio, Barajador barajador) {
        this.repositorio = repositorio;
        this.barajador = barajador;
    }

    public PartidaResponse crear(CrearPartidaRequest solicitud) {
        List<Integer> cartas = barajador.barajar(Mazo.cartasOficiales(), solicitud.semilla());
        Partida partida = Partida.crear(solicitud.jugadores(), cartas);
        return PartidaResponse.desde(repositorio.save(partida));
    }

    @Transactional(readOnly = true)
    public PartidaResponse obtener(Long partidaId) {
        return PartidaResponse.desde(buscar(partidaId));
    }

    @Transactional(readOnly = true)
    public List<JugadorResponse> jugadores(Long partidaId) {
        return buscar(partidaId).getJugadores().stream().map(JugadorResponse::desde).toList();
    }

    public PartidaResponse guardarPelusas(Long partidaId, Long jugadorId) {
        Partida partida = buscar(partidaId);
        partida.guardarPelusas(partida.buscarJugador(jugadorId));
        return PartidaResponse.desde(partida);
    }

    public PartidaResponse sacarCarta(Long partidaId, Long jugadorId) {
        Partida partida = buscar(partidaId);
        partida.sacarCarta(partida.buscarJugador(jugadorId));
        return PartidaResponse.desde(partida);
    }

    public PartidaResponse robarCartas(Long partidaId, Long jugadorId) {
        Partida partida = buscar(partidaId);
        partida.robarCartas(partida.buscarJugador(jugadorId));
        return PartidaResponse.desde(partida);
    }

    /** CONTINUAR saca otra carta; PARAR finaliza voluntariamente el turno. */
    public PartidaResponse decidir(Long partidaId, Long jugadorId, DecisionRequest.Decision decision) {
        Partida partida = buscar(partidaId);
        Jugador jugador = partida.buscarJugador(jugadorId);
        if (decision == DecisionRequest.Decision.CONTINUAR) {
            partida.sacarCarta(jugador);
        } else {
            partida.plantarse(jugador);
        }
        return PartidaResponse.desde(partida);
    }

    @Transactional(readOnly = true)
    public ResultadoResponse resultado(Long partidaId) {
        Partida partida = buscar(partidaId);
        if (partida.estaEnCurso()) {
            throw new ReglaJuegoException(CodigoError.PARTIDA_NO_FINALIZADA,
                    "La partida " + partidaId + " todavía está en curso");
        }
        return ResultadoResponse.desde(partida);
    }

    @Transactional(readOnly = true)
    public List<ResultadoResponse> historico() {
        return repositorio.buscarPorEstado(EstadoPartida.FINALIZADA).stream()
                .map(ResultadoResponse::desde)
                .toList();
    }

    private Partida buscar(Long partidaId) {
        return repositorio.findById(partidaId).orElseThrow(() -> new PartidaNoEncontradaException(partidaId));
    }
}
