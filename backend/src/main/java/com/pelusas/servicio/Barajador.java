package com.pelusas.servicio;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Mezcla las cartas del mazo. Con una semilla la mezcla es reproducible,
 * lo que permite preparar escenarios exactos para las pruebas funcionales y la demostración.
 */
@Component
public class Barajador {

    public List<Integer> barajar(List<Integer> cartas, Long semilla) {
        List<Integer> mezcladas = new ArrayList<>(cartas);
        Collections.shuffle(mezcladas, semilla == null ? new Random() : new Random(semilla));
        return mezcladas;
    }
}
