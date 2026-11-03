package com.pelusas.dominio;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Determina el ganador: gana la mayor puntuación. Si hay empate, gana quien tenga más cartas de valor 1;
 * si persiste, más cartas de valor 2, y así sucesivamente hasta el 10.
 */
public final class Desempate {

    private static final List<Integer> VALORES_EN_ORDEN =
            IntStream.rangeClosed(Mazo.VALOR_MINIMO, Mazo.VALOR_MAXIMO).boxed().toList();

    public static final Comparator<Jugador> CRITERIO = Desempate::comparar;

    private Desempate() {
    }

    static int comparar(Jugador a, Jugador b) {
        int resultado = Integer.compare(a.puntos(), b.puntos());
        for (int valor : VALORES_EN_ORDEN) {
            if (resultado != 0) {
                return resultado;
            }
            resultado = Integer.compare(a.cantidadEnMonton(valor), b.cantidadEnMonton(valor));
        }
        return resultado;
    }

    /** Devuelve el ganador; si dos jugadores quedan idénticos en todos los criterios, ambos ganan. */
    public static List<Jugador> ganadores(List<Jugador> jugadores) {
        Jugador mejor = Collections.max(jugadores, CRITERIO);
        return jugadores.stream()
                .filter(jugador -> comparar(jugador, mejor) == 0)
                .toList();
    }
}
