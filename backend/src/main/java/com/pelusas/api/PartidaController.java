package com.pelusas.api;

import com.pelusas.api.dto.AccionRequest;
import com.pelusas.api.dto.CrearPartidaRequest;
import com.pelusas.api.dto.DecisionRequest;
import com.pelusas.api.dto.JugadorResponse;
import com.pelusas.api.dto.PartidaResponse;
import com.pelusas.api.dto.ResultadoResponse;
import com.pelusas.servicio.PartidaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/partidas")
public class PartidaController {

    private final PartidaService servicio;

    public PartidaController(PartidaService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public ResponseEntity<PartidaResponse> crear(@Valid @RequestBody CrearPartidaRequest solicitud) {
        PartidaResponse partida = servicio.crear(solicitud);
        return ResponseEntity.created(URI.create("/api/partidas/" + partida.id())).body(partida);
    }

    @GetMapping("/historico")
    public List<ResultadoResponse> historico() {
        return servicio.historico();
    }

    @GetMapping("/{id}")
    public PartidaResponse obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @GetMapping("/{id}/jugadores")
    public List<JugadorResponse> jugadores(@PathVariable Long id) {
        return servicio.jugadores(id);
    }

    @PostMapping("/{id}/turno/guardar")
    public PartidaResponse guardarPelusas(@PathVariable Long id, @Valid @RequestBody AccionRequest solicitud) {
        return servicio.guardarPelusas(id, solicitud.jugadorId());
    }

    @PostMapping("/{id}/turno/sacar")
    public PartidaResponse sacarCarta(@PathVariable Long id, @Valid @RequestBody AccionRequest solicitud) {
        return servicio.sacarCarta(id, solicitud.jugadorId());
    }

    @PostMapping("/{id}/turno/robar")
    public PartidaResponse robarCartas(@PathVariable Long id, @Valid @RequestBody AccionRequest solicitud) {
        return servicio.robarCartas(id, solicitud.jugadorId());
    }

    @PostMapping("/{id}/turno/decision")
    public PartidaResponse decidir(@PathVariable Long id, @Valid @RequestBody DecisionRequest solicitud) {
        return servicio.decidir(id, solicitud.jugadorId(), solicitud.decision());
    }

    @GetMapping("/{id}/resultado")
    public ResultadoResponse resultado(@PathVariable Long id) {
        return servicio.resultado(id);
    }
}
