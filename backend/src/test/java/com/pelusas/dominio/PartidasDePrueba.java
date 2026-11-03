package com.pelusas.dominio;

import java.util.ArrayList;
import java.util.List;

/** Utilidades para construir escenarios exactos sin depender del azar. */
public final class PartidasDePrueba {

    private PartidasDePrueba() {
    }

    /**
     * Mazo oficial válido (110 cartas) en el que las primeras cartas que se sacarán son las indicadas, en ese orden.
     * El resto queda ordenado de forma que después salen primero los valores altos.
     */
    public static List<Integer> mazoConCartasSuperiores(int... primeras) {
        List<Integer> cartas = new ArrayList<>(Mazo.cartasOficiales());
        for (int carta : primeras) {
            cartas.remove(Integer.valueOf(carta));
        }
        for (int i = primeras.length - 1; i >= 0; i--) {
            cartas.add(primeras[i]);
        }
        return cartas;
    }

    public static Partida partida(List<String> nombres, int... primeras) {
        return Partida.crear(nombres, mazoConCartasSuperiores(primeras));
    }

    /** Juega hasta el final: cada jugador guarda si puede, saca una carta y se planta. Nadie pierde. */
    public static Partida jugarHastaTerminar(Partida partida) {
        while (partida.estaEnCurso()) {
            Jugador actual = partida.getJugadorActual();
            if (partida.puedeGuardar()) {
                partida.guardarPelusas(actual);
            }
            partida.sacarCarta(actual);
            if (partida.estaEnCurso()) {
                partida.plantarse(actual);
            }
        }
        return partida;
    }
}
