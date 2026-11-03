package com.pelusas.dominio;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.OrderColumn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Montón de polvo: las cartas boca abajo de las que se saca en cada turno.
 * La carta superior es la última de la lista.
 */
@Embeddable
public class Mazo {

    public static final int VALOR_MINIMO = 1;
    public static final int VALOR_MAXIMO = 10;
    public static final int TOTAL_CARTAS = 110;

    /** Los valores 1 a 5 tienen 13 copias; los valores 6 a 10 tienen 9 copias. */
    static final int ULTIMO_VALOR_COMUN = 5;
    static final int COPIAS_VALOR_COMUN = 13;
    static final int COPIAS_VALOR_ALTO = 9;

    @ElementCollection
    @CollectionTable(name = "partida_mazo")
    @OrderColumn(name = "posicion")
    @Column(name = "valor", nullable = false)
    private List<Integer> cartas = new ArrayList<>();

    protected Mazo() {
    }

    private Mazo(List<Integer> cartas) {
        this.cartas = new ArrayList<>(cartas);
    }

    /** Cantidad de copias que el mazo oficial contiene de un valor. */
    public static int copiasDeValor(int valor) {
        validarValor(valor);
        return valor <= ULTIMO_VALOR_COMUN ? COPIAS_VALOR_COMUN : COPIAS_VALOR_ALTO;
    }

    /** Las 110 cartas del juego, ordenadas de menor a mayor valor. */
    public static List<Integer> cartasOficiales() {
        List<Integer> cartas = new ArrayList<>(TOTAL_CARTAS);
        for (int valor = VALOR_MINIMO; valor <= VALOR_MAXIMO; valor++) {
            cartas.addAll(Collections.nCopies(copiasDeValor(valor), valor));
        }
        return cartas;
    }

    /** Crea el mazo a partir de las cartas ya mezcladas, validando que respeten la distribución oficial. */
    public static Mazo desde(List<Integer> cartasMezcladas) {
        if (!esDistribucionOficial(cartasMezcladas)) {
            throw new ReglaJuegoException(CodigoError.MAZO_INVALIDO,
                    "El mazo debe contener exactamente las 110 cartas oficiales de Pelusas");
        }
        return new Mazo(cartasMezcladas);
    }

    static boolean esDistribucionOficial(List<Integer> cartas) {
        return contarPorValor(cartas).equals(contarPorValor(cartasOficiales()));
    }

    private static Map<Integer, Integer> contarPorValor(List<Integer> cartas) {
        Map<Integer, Integer> conteo = new TreeMap<>();
        for (Integer carta : cartas) {
            conteo.merge(carta, 1, Integer::sum);
        }
        return conteo;
    }

    private static void validarValor(int valor) {
        if (valor < VALOR_MINIMO || valor > VALOR_MAXIMO) {
            throw new ReglaJuegoException(CodigoError.VALOR_INVALIDO,
                    "El valor de una carta debe estar entre 1 y 10: " + valor);
        }
    }

    /** Saca la carta superior del montón de polvo. */
    public int sacar() {
        if (estaVacio()) {
            throw new ReglaJuegoException(CodigoError.MAZO_VACIO, "No quedan cartas en el montón de polvo");
        }
        return cartas.remove(cartas.size() - 1);
    }

    public boolean estaVacio() {
        return cartas.isEmpty();
    }

    public int cantidad() {
        return cartas.size();
    }

    public int cantidadDeValor(int valor) {
        return Collections.frequency(cartas, valor);
    }
}
