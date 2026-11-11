package com.pelusas.dominio;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Partida de Pelusas. Contiene todas las reglas del juego: el frontend solo solicita acciones
 * y esta clase valida si están permitidas y las ejecuta.
 */
@Entity
public class Partida {

    public static final int MINIMO_JUGADORES = 2;
    public static final int MAXIMO_JUGADORES = 6;
    public static final int LONGITUD_MAXIMA_NOMBRE = 20;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPartida estado;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    private LocalDateTime fechaFin;

    @Embedded
    private Mazo mazo;

    private int cartasDescartadas;

    private int posicionJugadorActual;

    /** Indica si el jugador actual ya sacó al menos una carta en este turno. */
    private boolean haSacadoEnTurno;

    /** Valor de la última carta sacada mientras todavía se puede robar; null si no hay robo disponible. */
    private Integer valorRobable;

    private Integer ultimaCarta;

    @Enumerated(EnumType.STRING)
    private TipoEvento ultimoEvento;

    private String mensajeEvento;

    @OneToMany(mappedBy = "partida", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("posicion")
    private List<Jugador> jugadores = new ArrayList<>();

    protected Partida() {
    }

    /**
     * Crea una partida con los jugadores en el orden indicado (el primero inicia) y un mazo ya mezclado.
     */
    public static Partida crear(List<String> nombres, List<Integer> cartasMezcladas) {
        validarNombres(nombres);
        Partida partida = new Partida();
        partida.mazo = Mazo.desde(cartasMezcladas);
        for (int posicion = 0; posicion < nombres.size(); posicion++) {
            partida.jugadores.add(new Jugador(nombres.get(posicion).trim(), posicion, partida));
        }
        partida.estado = EstadoPartida.EN_CURSO;
        partida.fechaCreacion = LocalDateTime.now();
        partida.registrarEvento(TipoEvento.PARTIDA_CREADA,
                "Partida creada. Inicia " + partida.getJugadorActual().getNombre());
        return partida;
    }

    private static void validarNombres(List<String> nombres) {
        if (nombres == null || nombres.size() < MINIMO_JUGADORES || nombres.size() > MAXIMO_JUGADORES) {
            throw new ReglaJuegoException(CodigoError.JUGADORES_INVALIDOS,
                    "La partida debe tener entre " + MINIMO_JUGADORES + " y " + MAXIMO_JUGADORES + " jugadores");
        }
        Set<String> nombresUnicos = new HashSet<>();
        for (String nombre : nombres) {
            if (nombre == null || nombre.isBlank()) {
                throw new ReglaJuegoException(CodigoError.JUGADORES_INVALIDOS, "Todos los jugadores deben tener nombre");
            }
            String limpio = nombre.trim();
            if (limpio.length() > LONGITUD_MAXIMA_NOMBRE) {
                throw new ReglaJuegoException(CodigoError.JUGADORES_INVALIDOS,
                        "El nombre no puede superar " + LONGITUD_MAXIMA_NOMBRE + " caracteres: " + limpio);
            }
            if (!nombresUnicos.add(limpio.toLowerCase(Locale.ROOT))) {
                throw new ReglaJuegoException(CodigoError.JUGADORES_INVALIDOS, "Nombre de jugador repetido: " + limpio);
            }
        }
    }

    // ---------------------------------------------------------------- acciones del turno

    /** Paso 1: al inicio del turno, antes de sacar, el jugador puede guardar las cartas que sobrevivieron. */
    public void guardarPelusas(Jugador jugador) {
        validarTurno(jugador);
        if (!puedeGuardar()) {
            throw new ReglaJuegoException(CodigoError.NO_PUEDE_GUARDAR, haSacadoEnTurno
                    ? "Solo se pueden guardar pelusas al inicio del turno, antes de sacar una carta"
                    : "No tienes cartas delante para guardar");
        }
        int guardadas = jugador.guardarCartasDelante();
        registrarEvento(TipoEvento.PELUSAS_GUARDADAS,
                jugador.getNombre() + " guardó " + guardadas + " carta(s) en su montón de puntos");
    }

    /** Paso 2: saca la carta superior del montón de polvo y aplica la regla de pérdida. */
    public void sacarCarta(Jugador jugador) {
        validarTurno(jugador);
        int carta = mazo.sacar();
        ultimaCarta = carta;
        haSacadoEnTurno = true;
        if (jugador.pierdeAlSacar(carta)) {
            int descartadas = jugador.descartarCartasDelante() + 1;
            cartasDescartadas += descartadas;
            pasarTurno();
            registrarEvento(TipoEvento.TURNO_PERDIDO, jugador.getNombre() + " sacó un " + carta
                    + " repetido y perdió el turno. Se descartaron " + descartadas + " carta(s). Turno de "
                    + getJugadorActual().getNombre());
        } else {
            jugador.colocarDelante(carta);
            valorRobable = carta;
            registrarEvento(TipoEvento.CARTA_SACADA, jugador.getNombre() + " sacó un " + carta);
        }
        if (mazo.estaVacio()) {
            finalizar();
        }
    }

    /** Paso 3: justo después de sacar una carta, toma las cartas de ese valor que tengan los demás jugadores. */
    public void robarCartas(Jugador jugador) {
        validarTurno(jugador);
        if (!puedeRobar()) {
            throw new ReglaJuegoException(CodigoError.NO_PUEDE_ROBAR,
                    "Solo puedes robar justo después de sacar una carta cuyo valor tenga otro jugador delante");
        }
        int valor = valorRobable;
        int robadas = 0;
        for (Jugador otro : jugadores) {
            if (otro != jugador && !otro.isProtegido()) {
                List<Integer> cartas = otro.entregarCartasDeValor(valor);
                robadas += cartas.size();
                jugador.colocarDelante(cartas);
            }
        }
        valorRobable = null;
        registrarEvento(TipoEvento.CARTAS_ROBADAS,
                jugador.getNombre() + " robó " + robadas + " carta(s) de valor " + valor);
    }

    /** Finaliza voluntariamente el turno: las cartas quedan delante para guardarlas en el siguiente turno. */
    public void plantarse(Jugador jugador) {
        validarTurno(jugador);
        if (!puedePlantarse()) {
            throw new ReglaJuegoException(CodigoError.NO_PUEDE_PLANTARSE,
                    "Debes sacar al menos una carta antes de plantarte");
        }
        pasarTurno();
        registrarEvento(TipoEvento.PLANTADO,
                jugador.getNombre() + " se plantó. Turno de " + getJugadorActual().getNombre());
    }

    /**
     * Comodín: una vez por partida, en cualquier momento de su turno. Protege todas sus cartas boca arriba
     * para que nadie se las robe hasta que vuelva a empezar su turno.
     */
    public void usarComodin(Jugador jugador) {
        validarTurno(jugador);
        if (!puedeUsarComodin()) {
            throw new ReglaJuegoException(CodigoError.COMODIN_YA_USADO, "Ya usaste tu comodín en esta partida");
        }
        jugador.usarComodin();
        registrarEvento(TipoEvento.COMODIN_USADO, jugador.getNombre()
                + " usó su comodín: nadie podrá robarle sus cartas hasta su próximo turno");
    }

    // ---------------------------------------------------------------- consultas

    public Jugador buscarJugador(Long jugadorId) {
        return jugadores.stream()
                .filter(jugador -> jugadorId.equals(jugador.getId()))
                .findFirst()
                .orElseThrow(() -> new ReglaJuegoException(CodigoError.JUGADOR_NO_PERTENECE,
                        "El jugador " + jugadorId + " no pertenece a la partida"));
    }

    public boolean estaEnCurso() {
        return estado == EstadoPartida.EN_CURSO;
    }

    public boolean puedeGuardar() {
        return estaEnCurso() && !haSacadoEnTurno && !getJugadorActual().getCartasDelante().isEmpty();
    }

    public boolean puedeSacar() {
        return estaEnCurso();
    }

    public boolean puedeRobar() {
        return estaEnCurso() && valorRobable != null && jugadores.stream()
                .anyMatch(otro -> otro != getJugadorActual() && otro.puedenRobarle(valorRobable));
    }

    public boolean puedeUsarComodin() {
        return estaEnCurso() && !getJugadorActual().isComodinUsado();
    }

    public boolean puedePlantarse() {
        return estaEnCurso() && haSacadoEnTurno;
    }

    public Jugador getJugadorActual() {
        return jugadores.get(posicionJugadorActual);
    }

    public Jugador getJugadorInicial() {
        return jugadores.get(0);
    }

    public List<Jugador> getGanadores() {
        return jugadores.stream().filter(Jugador::isGanador).toList();
    }

    // ---------------------------------------------------------------- internos

    private void validarTurno(Jugador jugador) {
        if (!estaEnCurso()) {
            throw new ReglaJuegoException(CodigoError.PARTIDA_FINALIZADA, "La partida ya terminó");
        }
        if (jugador != getJugadorActual()) {
            throw new ReglaJuegoException(CodigoError.NO_ES_SU_TURNO,
                    "No es el turno de " + jugador.getNombre() + ". Es el turno de " + getJugadorActual().getNombre());
        }
    }

    private void pasarTurno() {
        posicionJugadorActual = (posicionJugadorActual + 1) % jugadores.size();
        haSacadoEnTurno = false;
        valorRobable = null;
        getJugadorActual().finalizarProteccion();
    }

    /** Al agotarse el montón de polvo todos guardan lo que tienen delante y se decide el ganador. */
    private void finalizar() {
        jugadores.forEach(Jugador::guardarCartasDelante);
        List<Jugador> ganadores = Desempate.ganadores(jugadores);
        jugadores.forEach(jugador -> jugador.registrarResultado(ganadores.contains(jugador)));
        estado = EstadoPartida.FINALIZADA;
        fechaFin = LocalDateTime.now();
        haSacadoEnTurno = false;
        valorRobable = null;
        registrarEvento(TipoEvento.PARTIDA_FINALIZADA, mensajeResultado(ganadores));
    }

    static String mensajeResultado(List<Jugador> ganadores) {
        String nombres = ganadores.stream().map(Jugador::getNombre).collect(Collectors.joining(" y "));
        return (ganadores.size() == 1 ? "Ganó " : "Empate entre ") + nombres
                + " con " + ganadores.getFirst().puntos() + " puntos";
    }

    private void registrarEvento(TipoEvento tipo, String mensaje) {
        this.ultimoEvento = tipo;
        this.mensajeEvento = mensaje;
    }

    // ---------------------------------------------------------------- getters

    public Long getId() {
        return id;
    }

    public EstadoPartida getEstado() {
        return estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaFin() {
        return fechaFin;
    }

    public Mazo getMazo() {
        return mazo;
    }

    public int getCartasDescartadas() {
        return cartasDescartadas;
    }

    public boolean isHaSacadoEnTurno() {
        return haSacadoEnTurno;
    }

    public Integer getValorRobable() {
        return valorRobable;
    }

    public Integer getUltimaCarta() {
        return ultimaCarta;
    }

    public TipoEvento getUltimoEvento() {
        return ultimoEvento;
    }

    public String getMensajeEvento() {
        return mensajeEvento;
    }

    public List<Jugador> getJugadores() {
        return Collections.unmodifiableList(jugadores);
    }
}
