package com.pelusas.api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integración de los servicios REST: petición HTTP -> controller -> servicio -> dominio -> base H2 real.
 * No hay mocks: se verifica el estado real devuelto y persistido.
 *
 * Semillas usadas (orden de las primeras cartas que salen del montón de polvo):
 *   2 -> 7, 5, 9, 8 (sin repeticiones)
 *   3 -> 9, 5, 1, 5 (la cuarta carta repite con 3 cartas delante: pierde)
 *   4 -> 5, 5, 4, 4 (el segundo jugador puede robar el 5 del primero)
 */
@SpringBootTest
@AutoConfigureMockMvc
class PartidaControllerIT {

    private static final String BASE = "/api/partidas";

    @Autowired
    private MockMvc mvc;

    // ---------------------------------------------------------------- utilidades

    private ResultActions crear(String json) throws Exception {
        return mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private long crearPartida(Long semilla) throws Exception {
        String json = "{\"jugadores\":[\"Ana\",\"Luis\"],\"semilla\":" + semilla + "}";
        return leerLong(crear(json).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
    }

    private String consultar(long partidaId) throws Exception {
        return mvc.perform(get(BASE + "/" + partidaId)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private long jugadorId(long partidaId, int posicion) throws Exception {
        return leerLong(consultar(partidaId), "$.jugadores[" + posicion + "].id");
    }

    private ResultActions accion(long partidaId, String accion, long jugadorId) throws Exception {
        return mvc.perform(post(BASE + "/" + partidaId + "/turno/" + accion)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"jugadorId\":" + jugadorId + "}"));
    }

    private ResultActions decidir(long partidaId, long jugadorId, String decision) throws Exception {
        return mvc.perform(post(BASE + "/" + partidaId + "/turno/decision")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"jugadorId\":" + jugadorId + ",\"decision\":\"" + decision + "\"}"));
    }

    private static long leerLong(String json, String ruta) {
        return ((Number) JsonPath.read(json, ruta)).longValue();
    }

    // ---------------------------------------------------------------- crear y consultar

    @Nested
    @DisplayName("POST /api/partidas")
    class CrearPartida {

        @Test
        @DisplayName("201: crea la partida con estado inicial válido")
        void creaLaPartida() throws Exception {
            crear("{\"jugadores\":[\"Ana\",\"Luis\",\"Eva\"]}")
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", org.hamcrest.Matchers.matchesPattern("/api/partidas/\\d+")))
                    .andExpect(jsonPath("$.id", notNullValue()))
                    .andExpect(jsonPath("$.estado").value("EN_CURSO"))
                    .andExpect(jsonPath("$.cartasEnMazo").value(110))
                    .andExpect(jsonPath("$.cartasDescartadas").value(0))
                    .andExpect(jsonPath("$.ultimoEvento").value("PARTIDA_CREADA"))
                    .andExpect(jsonPath("$.mensajeEvento").value("Partida creada. Inicia Ana"))
                    .andExpect(jsonPath("$.acciones.puedeSacar").value(true))
                    .andExpect(jsonPath("$.acciones.puedeGuardar").value(false))
                    .andExpect(jsonPath("$.acciones.puedeRobar").value(false))
                    .andExpect(jsonPath("$.acciones.puedePlantarse").value(false))
                    .andExpect(jsonPath("$.jugadores", hasSize(3)))
                    .andExpect(jsonPath("$.jugadores[*].nombre", contains("Ana", "Luis", "Eva")))
                    .andExpect(jsonPath("$.jugadores[0].cartasDelante", empty()));
        }

        @Test
        @DisplayName("El jugador inicial es el actual y es el primero de la lista")
        void jugadorInicial() throws Exception {
            String json = consultar(crearPartida(null));

            long primero = leerLong(json, "$.jugadores[0].id");
            assertThat(leerLong(json, "$.jugadorInicialId")).isEqualTo(primero);
            assertThat(leerLong(json, "$.jugadorActualId")).isEqualTo(primero);
        }

        @Test
        @DisplayName("Con semilla la partida es reproducible")
        void semillaReproducible() throws Exception {
            long partidaId = crearPartida(3L);

            accion(partidaId, "sacar", jugadorId(partidaId, 0))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ultimaCarta").value(9));
        }

        @Test
        @DisplayName("400: menos de 2 jugadores")
        void rechazaUnJugador() throws Exception {
            crear("{\"jugadores\":[\"Ana\"]}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.estado").value(400))
                    .andExpect(jsonPath("$.codigo").value("JUGADORES_INVALIDOS"))
                    .andExpect(jsonPath("$.mensaje").value("La partida debe tener entre 2 y 6 jugadores"));
        }

        @Test
        @DisplayName("400: más de 6 jugadores")
        void rechazaSieteJugadores() throws Exception {
            crear("{\"jugadores\":[\"A\",\"B\",\"C\",\"D\",\"E\",\"F\",\"G\"]}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("JUGADORES_INVALIDOS"));
        }

        @Test
        @DisplayName("400: nombres repetidos")
        void rechazaNombresRepetidos() throws Exception {
            crear("{\"jugadores\":[\"Ana\",\"ana\"]}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("JUGADORES_INVALIDOS"));
        }

        @Test
        @DisplayName("400: falta la lista de jugadores")
        void rechazaSinJugadores() throws Exception {
            crear("{}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"))
                    .andExpect(jsonPath("$.mensaje").value("La lista de jugadores es obligatoria"));
        }

        @Test
        @DisplayName("400: JSON mal formado")
        void rechazaJsonInvalido() throws Exception {
            crear("{jugadores:")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"))
                    .andExpect(jsonPath("$.mensaje").value("El cuerpo de la solicitud no es un JSON válido"));
        }
    }

    @Nested
    @DisplayName("GET /api/partidas/{id} y /jugadores")
    class Consultas {

        @Test
        void consultaLaPartida() throws Exception {
            long partidaId = crearPartida(2L);

            mvc.perform(get(BASE + "/" + partidaId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(partidaId))
                    .andExpect(jsonPath("$.estado").value("EN_CURSO"));
        }

        @Test
        void consultaLosJugadores() throws Exception {
            long partidaId = crearPartida(2L);

            mvc.perform(get(BASE + "/" + partidaId + "/jugadores"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].nombre").value("Ana"))
                    .andExpect(jsonPath("$[0].posicion").value(0))
                    .andExpect(jsonPath("$[1].nombre").value("Luis"))
                    .andExpect(jsonPath("$[1].puntos").value(0));
        }

        @Test
        @DisplayName("404: la partida no existe")
        void partidaInexistente() throws Exception {
            mvc.perform(get(BASE + "/999999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo").value("PARTIDA_NO_ENCONTRADA"))
                    .andExpect(jsonPath("$.mensaje").value("No existe la partida 999999"));
            mvc.perform(get(BASE + "/999999/jugadores")).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("400: identificador no numérico")
        void identificadorInvalido() throws Exception {
            mvc.perform(get(BASE + "/abc"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"))
                    .andExpect(jsonPath("$.mensaje").value("Valor inválido para 'id': abc"));
        }
    }

    // ---------------------------------------------------------------- turno

    @Nested
    @DisplayName("Turno por HTTP")
    class Turno {

        @Test
        @DisplayName("Turno exitoso: crear -> consultar -> sacar -> plantarse -> guardar, verificando cada estado")
        void turnoExitoso() throws Exception {
            long partidaId = crearPartida(2L);
            long ana = jugadorId(partidaId, 0);
            long luis = jugadorId(partidaId, 1);

            accion(partidaId, "sacar", ana)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ultimaCarta").value(7))
                    .andExpect(jsonPath("$.ultimoEvento").value("CARTA_SACADA"))
                    .andExpect(jsonPath("$.acciones.puedePlantarse").value(true));

            String despuesDeSacar = consultar(partidaId);
            assertThat(leerLong(despuesDeSacar, "$.cartasEnMazo")).isEqualTo(109);
            assertThat(JsonPath.<List<Integer>>read(despuesDeSacar, "$.jugadores[0].cartasDelante")).containsExactly(7);

            decidir(partidaId, ana, "PARAR")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.jugadorActualId").value(luis))
                    .andExpect(jsonPath("$.ultimoEvento").value("PLANTADO"))
                    .andExpect(jsonPath("$.jugadores[0].cartasDelante", contains(7)));

            accion(partidaId, "sacar", luis).andExpect(status().isOk());
            decidir(partidaId, luis, "PARAR").andExpect(status().isOk());

            mvc.perform(get(BASE + "/" + partidaId))
                    .andExpect(jsonPath("$.acciones.puedeGuardar").value(true));
            accion(partidaId, "guardar", ana)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ultimoEvento").value("PELUSAS_GUARDADAS"))
                    .andExpect(jsonPath("$.jugadores[0].cartasDelante", empty()))
                    .andExpect(jsonPath("$.jugadores[0].montonPuntos", contains(7)))
                    .andExpect(jsonPath("$.jugadores[0].puntos").value(7));
        }

        @Test
        @DisplayName("CONTINUAR saca otra carta en el mismo turno")
        void continuar() throws Exception {
            long partidaId = crearPartida(2L);
            long ana = jugadorId(partidaId, 0);
            accion(partidaId, "sacar", ana);

            decidir(partidaId, ana, "CONTINUAR")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.jugadorActualId").value(ana))
                    .andExpect(jsonPath("$.cartasEnMazo").value(108))
                    .andExpect(jsonPath("$.jugadores[0].cartasDelante", contains(7, 5)));
        }

        @Test
        @DisplayName("Turno perdido: con 3 cartas delante sale una repetida, se descartan y pasa el turno")
        void turnoPerdido() throws Exception {
            long partidaId = crearPartida(3L);
            long ana = jugadorId(partidaId, 0);
            long luis = jugadorId(partidaId, 1);
            accion(partidaId, "sacar", ana);
            decidir(partidaId, ana, "CONTINUAR");
            decidir(partidaId, ana, "CONTINUAR")
                    .andExpect(jsonPath("$.jugadores[0].cartasDelante", contains(9, 5, 1)));

            decidir(partidaId, ana, "CONTINUAR")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ultimaCarta").value(5))
                    .andExpect(jsonPath("$.ultimoEvento").value("TURNO_PERDIDO"))
                    .andExpect(jsonPath("$.jugadorActualId").value(luis));

            mvc.perform(get(BASE + "/" + partidaId))
                    .andExpect(jsonPath("$.jugadores[0].cartasDelante", empty()))
                    .andExpect(jsonPath("$.jugadores[0].montonPuntos", empty()))
                    .andExpect(jsonPath("$.cartasDescartadas").value(4))
                    .andExpect(jsonPath("$.cartasEnMazo").value(106))
                    .andExpect(jsonPath("$.acciones.puedePlantarse").value(false));
        }

        @Test
        @DisplayName("Robo: el segundo jugador saca un 5 y roba el 5 del primero")
        void robo() throws Exception {
            long partidaId = crearPartida(4L);
            long ana = jugadorId(partidaId, 0);
            long luis = jugadorId(partidaId, 1);
            accion(partidaId, "sacar", ana);
            decidir(partidaId, ana, "PARAR");

            accion(partidaId, "sacar", luis)
                    .andExpect(jsonPath("$.acciones.puedeRobar").value(true))
                    .andExpect(jsonPath("$.acciones.valorRobable").value(5));
            accion(partidaId, "robar", luis)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.ultimoEvento").value("CARTAS_ROBADAS"))
                    .andExpect(jsonPath("$.mensajeEvento").value("Luis robó 1 carta(s) de valor 5"))
                    .andExpect(jsonPath("$.acciones.puedeRobar").value(false));

            mvc.perform(get(BASE + "/" + partidaId))
                    .andExpect(jsonPath("$.jugadores[0].cartasDelante", empty()))
                    .andExpect(jsonPath("$.jugadores[1].cartasDelante", contains(5, 5)));
        }

        @Test
        @DisplayName("409: no puede robar dos veces la misma carta")
        void robarDosVeces() throws Exception {
            long partidaId = crearPartida(4L);
            long ana = jugadorId(partidaId, 0);
            long luis = jugadorId(partidaId, 1);
            accion(partidaId, "sacar", ana);
            decidir(partidaId, ana, "PARAR");
            accion(partidaId, "sacar", luis);
            accion(partidaId, "robar", luis);

            accion(partidaId, "robar", luis)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("NO_PUEDE_ROBAR"));
        }

        @Test
        @DisplayName("409: robar si sacó otra carta después de la que podía robar")
        void robarTarde() throws Exception {
            long partidaId = crearPartida(4L);
            long ana = jugadorId(partidaId, 0);
            long luis = jugadorId(partidaId, 1);
            accion(partidaId, "sacar", ana);
            decidir(partidaId, ana, "PARAR");
            accion(partidaId, "sacar", luis);
            decidir(partidaId, luis, "CONTINUAR");

            accion(partidaId, "robar", luis)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("NO_PUEDE_ROBAR"));
        }

        @Test
        @DisplayName("409: no es el turno del jugador")
        void fueraDeTurno() throws Exception {
            long partidaId = crearPartida(2L);
            long luis = jugadorId(partidaId, 1);

            accion(partidaId, "sacar", luis)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.estado").value(409))
                    .andExpect(jsonPath("$.codigo").value("NO_ES_SU_TURNO"))
                    .andExpect(jsonPath("$.mensaje").value("No es el turno de Luis. Es el turno de Ana"));
            mvc.perform(get(BASE + "/" + partidaId)).andExpect(jsonPath("$.cartasEnMazo").value(110));
        }

        @Test
        @DisplayName("409: guardar sin cartas delante")
        void guardarSinCartas() throws Exception {
            long partidaId = crearPartida(2L);

            accion(partidaId, "guardar", jugadorId(partidaId, 0))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("NO_PUEDE_GUARDAR"));
        }

        @Test
        @DisplayName("409: plantarse sin haber sacado")
        void plantarseSinSacar() throws Exception {
            long partidaId = crearPartida(2L);

            decidir(partidaId, jugadorId(partidaId, 0), "PARAR")
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("NO_PUEDE_PLANTARSE"));
        }

        @Test
        @DisplayName("400: falta el jugadorId")
        void faltaJugadorId() throws Exception {
            long partidaId = crearPartida(2L);

            mvc.perform(post(BASE + "/" + partidaId + "/turno/sacar").contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.mensaje").value("El jugadorId es obligatorio"));
        }

        @Test
        @DisplayName("400: decisión ausente o desconocida")
        void decisionInvalida() throws Exception {
            long partidaId = crearPartida(2L);
            long ana = jugadorId(partidaId, 0);

            mvc.perform(post(BASE + "/" + partidaId + "/turno/decision").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"jugadorId\":" + ana + "}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.mensaje").value("La decisión es obligatoria: CONTINUAR o PARAR"));
            decidir(partidaId, ana, "SALTAR")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"));
        }

        @Test
        @DisplayName("400: el jugador pertenece a otra partida")
        void jugadorDeOtraPartida() throws Exception {
            long partidaId = crearPartida(2L);
            long ajeno = jugadorId(crearPartida(2L), 0);

            accion(partidaId, "sacar", ajeno)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("JUGADOR_NO_PERTENECE"));
        }

        @Test
        @DisplayName("404: acción sobre una partida que no existe")
        void accionEnPartidaInexistente() throws Exception {
            accion(999999, "sacar", 1).andExpect(status().isNotFound());
            accion(999999, "guardar", 1).andExpect(status().isNotFound());
            accion(999999, "robar", 1).andExpect(status().isNotFound());
            decidir(999999, 1, "PARAR").andExpect(status().isNotFound());
        }
    }

    // ---------------------------------------------------------------- resultado e histórico

    @Nested
    @DisplayName("Resultado e histórico")
    class ResultadoEHistorico {

        /** Juega toda la partida por HTTP: cada jugador guarda si puede, saca una carta y se planta. */
        private void jugarHastaTerminar(long partidaId) throws Exception {
            String estado = consultar(partidaId);
            while ("EN_CURSO".equals(JsonPath.read(estado, "$.estado"))) {
                long actual = leerLong(estado, "$.jugadorActualId");
                if (JsonPath.<Boolean>read(estado, "$.acciones.puedeGuardar")) {
                    accion(partidaId, "guardar", actual).andExpect(status().isOk());
                }
                estado = accion(partidaId, "sacar", actual).andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString();
                if ("EN_CURSO".equals(JsonPath.read(estado, "$.estado"))) {
                    estado = decidir(partidaId, actual, "PARAR").andExpect(status().isOk())
                            .andReturn().getResponse().getContentAsString();
                }
            }
        }

        @Test
        @DisplayName("409: el resultado no existe mientras la partida está en curso")
        void resultadoEnCurso() throws Exception {
            long partidaId = crearPartida(2L);

            mvc.perform(get(BASE + "/" + partidaId + "/resultado"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("PARTIDA_NO_FINALIZADA"));
        }

        @Test
        void resultadoDePartidaInexistente() throws Exception {
            mvc.perform(get(BASE + "/999999/resultado")).andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Partida completa: termina al agotarse el mazo, se consulta el resultado y aparece en el histórico")
        void partidaCompleta() throws Exception {
            long partidaId = crearPartida(2L);

            jugarHastaTerminar(partidaId);

            mvc.perform(get(BASE + "/" + partidaId))
                    .andExpect(jsonPath("$.estado").value("FINALIZADA"))
                    .andExpect(jsonPath("$.cartasEnMazo").value(0))
                    .andExpect(jsonPath("$.ultimoEvento").value("PARTIDA_FINALIZADA"))
                    .andExpect(jsonPath("$.fechaFin", notNullValue()))
                    .andExpect(jsonPath("$.acciones.puedeSacar").value(false));

            String resultado = mvc.perform(get(BASE + "/" + partidaId + "/resultado"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.partidaId").value(partidaId))
                    .andExpect(jsonPath("$.ganadores", hasSize(1)))
                    .andExpect(jsonPath("$.empate").value(false))
                    .andExpect(jsonPath("$.jugadores", hasSize(2)))
                    .andExpect(jsonPath("$.jugadores[0].cantidadCartas").value(55))
                    .andExpect(jsonPath("$.jugadores[1].cantidadCartas").value(55))
                    .andReturn().getResponse().getContentAsString();
            int puntosAna = JsonPath.read(resultado, "$.jugadores[0].puntuacionFinal");
            int puntosLuis = JsonPath.read(resultado, "$.jugadores[1].puntuacionFinal");
            assertThat(puntosAna + puntosLuis).isEqualTo(555);
            String ganador = JsonPath.read(resultado, "$.ganadores[0]");
            assertThat(ganador).isEqualTo(puntosAna > puntosLuis ? "Ana" : "Luis");

            mvc.perform(get(BASE + "/historico"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].partidaId", hasItem((int) partidaId)));

            accion(partidaId, "sacar", leerLong(consultar(partidaId), "$.jugadorActualId"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("PARTIDA_FINALIZADA"));
        }

        @Test
        @DisplayName("El histórico no incluye partidas en curso")
        void historicoSinPartidasEnCurso() throws Exception {
            long enCurso = crearPartida(2L);

            String historico = mvc.perform(get(BASE + "/historico"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            List<Integer> ids = JsonPath.read(historico, "$[*].partidaId");
            assertThat(ids).doesNotContain((int) enCurso);
        }
    }
}
