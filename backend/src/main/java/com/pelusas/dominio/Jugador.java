package com.pelusas.dominio;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
public class Jugador {

    /** Cantidad mínima de cartas delante para que una carta repetida haga perder el turno. */
    public static final int CARTAS_PARA_PERDER = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    /** Orden del jugador en la mesa (0 es quien inicia). */
    @Column(nullable = false)
    private int posicion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partida_id")
    private Partida partida;

    /** Cartas boca arriba delante del jugador: aún están en riesgo. */
    @ElementCollection
    @CollectionTable(name = "jugador_cartas_delante")
    @OrderColumn(name = "posicion")
    @Column(name = "valor", nullable = false)
    private List<Integer> cartasDelante = new ArrayList<>();

    /** Cartas guardadas en el montón de puntos: ya no se pueden perder ni robar. */
    @ElementCollection
    @CollectionTable(name = "jugador_monton_puntos")
    @OrderColumn(name = "posicion")
    @Column(name = "valor", nullable = false)
    private List<Integer> montonPuntos = new ArrayList<>();

    /** Se asigna al terminar la partida. */
    private Integer puntuacionFinal;

    private boolean ganador;

    /** Cada jugador tiene un único comodín por partida. */
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean comodinUsado;

    /** Mientras está activo el comodín, nadie puede robarle sus cartas boca arriba. */
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean protegido;

    protected Jugador() {
    }

    Jugador(String nombre, int posicion, Partida partida) {
        this.nombre = nombre;
        this.posicion = posicion;
        this.partida = partida;
    }

    /** Regla de pérdida: la carta ya está delante y el jugador tiene 3 o más cartas antes de colocarla. */
    boolean pierdeAlSacar(int carta) {
        return cartasDelante.size() >= CARTAS_PARA_PERDER && cartasDelante.contains(carta);
    }

    void colocarDelante(int carta) {
        cartasDelante.add(carta);
    }

    void colocarDelante(List<Integer> cartas) {
        cartasDelante.addAll(cartas);
    }

    boolean tieneCartasDeValor(int valor) {
        return cartasDelante.contains(valor);
    }

    /** Se le pueden robar cartas de ese valor si las tiene delante y no está protegido por su comodín. */
    boolean puedenRobarle(int valor) {
        return !protegido && tieneCartasDeValor(valor);
    }

    void usarComodin() {
        comodinUsado = true;
        protegido = true;
    }

    /** El comodín protege hasta que vuelve a empezar el turno de este jugador. */
    void finalizarProteccion() {
        protegido = false;
    }

    /** Retira y devuelve todas las cartas de ese valor que el jugador tiene delante. */
    List<Integer> entregarCartasDeValor(int valor) {
        List<Integer> entregadas = new ArrayList<>(Collections.nCopies(Collections.frequency(cartasDelante, valor), valor));
        cartasDelante.removeIf(carta -> carta == valor);
        return entregadas;
    }

    /** Pasa las cartas de delante al montón de puntos y devuelve cuántas se guardaron. */
    int guardarCartasDelante() {
        int guardadas = cartasDelante.size();
        montonPuntos.addAll(cartasDelante);
        cartasDelante.clear();
        return guardadas;
    }

    /** Descarta las cartas de delante (no suman puntos) y devuelve cuántas se descartaron. */
    int descartarCartasDelante() {
        int descartadas = cartasDelante.size();
        cartasDelante.clear();
        return descartadas;
    }

    public int puntos() {
        return montonPuntos.stream().mapToInt(Integer::intValue).sum();
    }

    public int cantidadEnMonton(int valor) {
        return Collections.frequency(montonPuntos, valor);
    }

    void registrarResultado(boolean esGanador) {
        this.puntuacionFinal = puntos();
        this.ganador = esGanador;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public int getPosicion() {
        return posicion;
    }

    public List<Integer> getCartasDelante() {
        return Collections.unmodifiableList(cartasDelante);
    }

    public List<Integer> getMontonPuntos() {
        return Collections.unmodifiableList(montonPuntos);
    }

    public Integer getPuntuacionFinal() {
        return puntuacionFinal;
    }

    public boolean isGanador() {
        return ganador;
    }

    public boolean isComodinUsado() {
        return comodinUsado;
    }

    public boolean isProtegido() {
        return protegido;
    }
}
