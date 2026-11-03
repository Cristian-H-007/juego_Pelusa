# Guía de sustentación

Respuestas rápidas a las preguntas típicas del profesor, con la ubicación exacta en el código.

## ¿Dónde está implementada cada regla?

| Regla | Ubicación |
|---|---|
| Distribución del mazo (13 copias del 1 al 5 y 9 del 6 al 10) | `Mazo.copiasDeValor` y `Mazo.cartasOficiales`: [Mazo.java:45](../backend/src/main/java/com/pelusas/dominio/Mazo.java) |
| Validar que un mazo tenga las 110 cartas oficiales | `Mazo.desde`: [Mazo.java:60](../backend/src/main/java/com/pelusas/dominio/Mazo.java) |
| Sacar la carta superior / mazo vacío | `Mazo.sacar`: [Mazo.java:88](../backend/src/main/java/com/pelusas/dominio/Mazo.java) |
| **Regla de pérdida** | `Jugador.pierdeAlSacar`: [Jugador.java:69](../backend/src/main/java/com/pelusas/dominio/Jugador.java) |
| Aplicar la pérdida (descartar y pasar el turno) | `Partida.sacarCarta`: [Partida.java:129](../backend/src/main/java/com/pelusas/dominio/Partida.java) |
| Guardar pelusas solo al inicio del turno | `Partida.guardarPelusas` y `puedeGuardar`: [Partida.java:116](../backend/src/main/java/com/pelusas/dominio/Partida.java) |
| Robar solo justo después de sacar, a todos los jugadores | `Partida.robarCartas` y `puedeRobar`: [Partida.java:152](../backend/src/main/java/com/pelusas/dominio/Partida.java) |
| Plantarse (finalizar voluntariamente) | `Partida.plantarse`: [Partida.java:173](../backend/src/main/java/com/pelusas/dominio/Partida.java) |
| ¿Es el turno del jugador? ¿Terminó la partida? | `Partida.validarTurno`: [Partida.java:229](../backend/src/main/java/com/pelusas/dominio/Partida.java) |
| Fin de partida y guardado automático | `Partida.finalizar`: [Partida.java:246](../backend/src/main/java/com/pelusas/dominio/Partida.java) |
| Desempate (valor 1, luego 2, …) | `Desempate.comparar`: [Desempate.java:22](../backend/src/main/java/com/pelusas/dominio/Desempate.java) |
| Consulta del histórico (JPQL) | `PartidaRepository.buscarPorEstado`: [PartidaRepository.java:14](../backend/src/main/java/com/pelusas/repositorio/PartidaRepository.java) |
| Códigos HTTP de los errores | `ManejadorErrores` y `CodigoError` (cada error define su código HTTP) |

## Preguntas frecuentes

**¿Qué ocurre si se repite una carta cuando hay solamente dos cartas delante?**
No pierde. `pierdeAlSacar` exige `cartasDelante.size() >= 3` **y** que el valor ya esté delante. Las cartas se
cuentan antes de colocar la nueva, así que con 2 cartas la condición es falsa: la carta se coloca y el jugador queda
con 3. Además puede robar ese valor a los demás. Pruebas: `JugadorTest.repeticionConDosCartasNoPierde` y
`PartidaTest...repeticionConDosCartasNoPierde`.

**¿Y cuando ya existen tres cartas?**
Pierde. Se descartan sus 3 cartas más la carta sacada (4 en total), no van al montón de puntos y el turno pasa al
siguiente jugador. Pruebas: `JugadorTest.repeticionConTresCartasPierde`, `PartidaTest...repeticionConTresCartasPierde`,
`PartidaControllerIT...turnoPerdido` y `e2e/turno-perdido.spec.js`.

**¿Qué pasaría si eliminamos esta condición?** (por ejemplo, `size() >= 3`)
El jugador perdería con cualquier repetida, incluso con 1 o 2 cartas. Fallarían
`repeticionConDosCartasNoPierde`, `repeticionConUnaCartaNoPierde` y el caso límite 7-7-7. PIT comprueba exactamente
esto: cambia `>=` por `>`, elimina condiciones, etc., y verifica que alguna prueba falle. Por eso el mutation score es 100 %.

**¿Por qué esta prueba es unitaria?** (por ejemplo, `PartidaTest`)
Prueba una sola unidad de lógica (la clase de dominio) aislada, en memoria, sin Spring, sin base de datos y sin HTTP.
Se ejecuta en milisegundos.

**¿Por qué esta otra es de integración?** (`PartidaControllerIT`, `PartidaRepositoryIT`)
Prueban varios componentes reales trabajando juntos. `PartidaControllerIT` levanta Spring y hace peticiones HTTP
con MockMvc que pasan por controller, servicio, dominio, JPA y H2. `PartidaRepositoryIT` guarda y relee de una base
H2 real. No hay mocks en ninguna de las dos.

**¿Qué parte del sistema está siendo simulada mediante un mock?**
Solo en pruebas unitarias:

- En `PartidaServiceTest`, `@Mock PartidaRepository` simula la **base de datos** y `@Mock Barajador` simula el
  **azar** de la mezcla. Así se prueba el servicio aislado y se verifica la interacción, por ejemplo con
  `verify(repositorio).save(...)` y `verify(repositorio, never()).save(any())` cuando la creación es inválida.
- En `ResultadoResponseTest` se simulan `Partida` y `Jugador` para construir un empate, que es muy difícil de
  obtener jugando.

**¿Dónde se está utilizando la base de datos?**
`PartidaRepository` (Spring Data JPA) en `PartidaService`. Las entidades son `Partida` y `Jugador`. El mazo, las
cartas de delante y el montón de puntos se guardan como `@ElementCollection` con `@OrderColumn`, para conservar el
orden. Cada acción carga la partida, la modifica y JPA guarda los cambios al confirmar la transacción
(`@Transactional`, *dirty checking*). Consola: http://localhost:8080/h2-console.

**¿Por qué esta clase contiene esta responsabilidad?**
- `Partida`: es la raíz del agregado, así que solo ella conoce el turno, el mazo y a todos los jugadores, y puede
  validar reglas que involucran a varios jugadores (robar, pasar el turno).
- `Jugador`: la regla de pérdida depende solo de sus propias cartas.
- `PartidaService`: orquesta la transacción y la persistencia, sin reglas de juego.
- `PartidaController`: solo traduce HTTP a llamadas del servicio.

**¿Por qué el frontend no decide si alguien perdió?**
Porque cualquiera podría saltarse una regla llamando a la API directamente. El backend devuelve el estado y las
acciones permitidas; si llega una acción inválida responde 409.

**¿Cómo se hacen reproducibles las pruebas funcionales?**
Con la `semilla` al crear la partida: `new Random(semilla)` produce siempre la misma mezcla. El mazo sigue siendo
válido (las 110 cartas oficiales), solo cambia el orden. Las semillas 2, 3 y 4 están documentadas en el README.

## Modificaciones que podría pedir el profesor

| Pedido | Dónde se cambia y qué prueba ajustar |
|---|---|
| "Perder con 4 cartas en vez de 3" | `Jugador.CARTAS_PARA_PERDER`, y actualizar `JugadorTest` y `PartidaTest.ReglaDePerdida` |
| "Máximo 4 jugadores" | `Partida.MAXIMO_JUGADORES`, y actualizar los `@ValueSource` de `PartidaTest.Creacion` |
| "Nombre de máximo 15 caracteres" | `Partida.LONGITUD_MAXIMA_NOMBRE` |
| "Nuevo endpoint" | método en `PartidaController`, método en `PartidaService`, prueba en `PartidaControllerIT` |
| "Nueva validación" | lanzar `ReglaJuegoException` con un nuevo valor en `CodigoError` (indicando su código HTTP) |
| "Cambiar el orden del histórico" | la JPQL de `PartidaRepository.buscarPorEstado` |

Después de cualquier cambio, ejecuta `.\mvnw.cmd verify` (pruebas y cobertura) y PIT.
