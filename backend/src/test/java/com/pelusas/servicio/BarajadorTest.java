package com.pelusas.servicio;

import com.pelusas.dominio.Mazo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BarajadorTest {

    private final Barajador barajador = new Barajador();
    private final List<Integer> oficiales = Mazo.cartasOficiales();

    @Test
    @DisplayName("Con la misma semilla la mezcla es idéntica (reproducible)")
    void mismaSemillaMismaMezcla() {
        assertThat(barajador.barajar(oficiales, 42L)).isEqualTo(barajador.barajar(oficiales, 42L));
    }

    @Test
    void semillasDistintasDanMezclasDistintas() {
        assertThat(barajador.barajar(oficiales, 1L)).isNotEqualTo(barajador.barajar(oficiales, 2L));
    }

    @Test
    @DisplayName("La mezcla cambia el orden pero conserva exactamente las mismas cartas")
    void conservaLasCartas() {
        List<Integer> mezcladas = barajador.barajar(oficiales, 42L);

        assertThat(mezcladas).isNotEqualTo(oficiales);
        assertThat(mezcladas).containsExactlyInAnyOrderElementsOf(oficiales);
    }

    @Test
    @DisplayName("Sin semilla mezcla al azar")
    void sinSemillaMezclaAlAzar() {
        List<Integer> mezcladas = barajador.barajar(oficiales, null);

        assertThat(mezcladas).isNotEqualTo(oficiales);
        assertThat(mezcladas).containsExactlyInAnyOrderElementsOf(oficiales);
    }

    @Test
    void noModificaLaListaRecibida() {
        List<Integer> original = new ArrayList<>(oficiales);

        barajador.barajar(original, 42L);

        assertThat(original).isEqualTo(oficiales);
    }
}
