# Pelusas: proyecto final de Calidad de Software

Aplicación web para jugar una partida completa del juego de mesa **Pelusas**. Tiene un backend en Java (Spring Boot)
que implementa y valida todas las reglas, una API REST, persistencia con JPA y H2, y un frontend en React.

El proyecto está respaldado por pruebas automatizadas en tres niveles: unitarias, de integración y funcionales.

| Indicador | Resultado |
|---|---|
| Pruebas unitarias (JUnit 5 + Mockito) | 137 |
| Pruebas de integración (MockMvc + base H2 real) | 35 |
| Pruebas funcionales (Playwright + JavaScript) | 8 |
| Cobertura (JaCoCo: líneas, ramas, instrucciones y métodos) | **100 %** |
| Mutation score (PIT) | **100 %** (183 de 183 mutantes eliminados) |

## Integrantes

- Cristian Hernández

## Tecnologías

| Capa | Tecnología |
|---|---|
| Backend | Java 25, Spring Boot 4.1.2 (Web MVC, Data JPA, Validation), Maven Wrapper |
| Base de datos | H2: en archivo para la aplicación y en memoria para las pruebas |
| Frontend | React 19, Vite 7 |
| Pruebas unitarias | JUnit 5, Mockito, AssertJ |
| Pruebas de integración | Spring Boot Test, MockMvc, `@DataJpaTest` |
| Pruebas funcionales | Playwright 1.64 (JavaScript) |
| Calidad | JaCoCo 0.8.16 (cobertura), PIT 1.30.1 (mutación) |

## Arquitectura

```
┌──────────────────────┐   HTTP/JSON    ┌──────────────────────────────────────────────────────┐
│ Frontend (React)     │ ─────────────► │ Backend (Spring Boot)                                │
│ localhost:5173       │   /api/...     │                                                      │
│                      │ ◄───────────── │  api/          PartidaController, ManejadorErrores   │
│ Solo SOLICITA        │  estado +      │       │        DTOs (request/response)               │
│ acciones y muestra   │  acciones      │       ▼                                              │
│ el estado            │  permitidas    │  servicio/     PartidaService, Barajador             │
└──────────────────────┘                │       │                                              │
                                        │       ▼                                              │
                                        │  dominio/      Partida, Jugador, Mazo, Desempate     │
                                        │                (TODAS las reglas del juego)          │
                                        │       │                                              │
                                        │       ▼                                              │
                                        │  repositorio/  PartidaRepository (Spring Data JPA)   │
                                        └───────┼──────────────────────────────────────────────┘
                                                ▼
                                        ┌──────────────────┐
                                        │ H2 (JPA)         │  partida, jugador, partida_mazo,
                                        │ backend/data/    │  jugador_cartas_delante, jugador_monton_puntos
                                        └──────────────────┘
```

**Responsabilidades**

- **`dominio`**: el modelo con su comportamiento (modelo de dominio rico). `Partida` valida turnos y ejecuta las
  acciones (guardar, sacar, robar, plantarse); `Jugador` aplica la regla de pérdida y maneja sus cartas; `Mazo`
  garantiza la distribución oficial de las 110 cartas; `Desempate` decide el ganador. Es Java puro, por lo que se
  prueba sin Spring ni base de datos.
- **`servicio`**: casos de uso. Carga la partida, delega en el dominio y devuelve el nuevo estado dentro de una
  transacción. `Barajador` mezcla el mazo y acepta una semilla para que las partidas sean reproducibles.
- **`api`**: endpoints REST, validación de entrada (Bean Validation) y traducción de errores a códigos HTTP.
- **`repositorio`**: acceso a datos con JPA, incluida la consulta JPQL del histórico.

**Principio:** el frontend solicita acciones y el backend valida y ejecuta las reglas. El backend también calcula
qué acciones están permitidas (`acciones.puedeGuardar`, `puedeRobar`, etc.); el frontend solo las usa para
habilitar o deshabilitar botones.

### Estructura del repositorio

```
juego_Pelusa/
├── backend/                 Spring Boot (Maven Wrapper: no requiere instalar Maven)
│   ├── src/main/java/com/pelusas/{api,servicio,dominio,repositorio}
│   ├── src/test/java/...    *Test = unitarias, *IT = integración
│   └── pom.xml              JaCoCo (exige 100 %), PIT (exige ≥ 90 %), Surefire y Failsafe
├── frontend/                React + Vite
│   ├── src/                 componentes y cliente de la API
│   ├── e2e/                 pruebas funcionales de Playwright
│   └── playwright.config.js levanta backend y frontend automáticamente
└── docs/
    ├── CURL.md              CURL de todos los endpoints
    └── reportes/            evidencia: cobertura (JaCoCo) y mutación (PIT)
```

## Reglas implementadas

- **Mazo:** 110 cartas. Los valores 1 a 5 tienen 13 copias cada uno y los valores 6 a 10 tienen 9 copias cada uno.
  Cualquier mazo con otra distribución se rechaza.
- **Jugadores:** de 2 a 6, con nombres no vacíos, de hasta 20 caracteres y sin repetir. Inicia el primero de la lista.
- **Turno:**
  1. **Guardar pelusas (opcional):** solo al inicio del turno, antes de sacar. Pasa todas las cartas de delante al
     montón de puntos. Si el jugador saca sin haber guardado, ya no puede guardar en ese turno y sus cartas siguen en riesgo.
  2. **Sacar una carta:** la carta queda delante del jugador. Después puede **continuar** (sacar otra) o **plantarse**.
     Puede plantarse desde la primera carta.
  3. **Robar (opcional):** solo justo después de sacar una carta. Toma las cartas de **ese valor** que tengan
     delante **todos** los demás jugadores. Si saca otra carta sin robar, pierde esa oportunidad. Las cartas ya
     guardadas no se pueden robar.
- **Pérdida del turno:** si la carta sacada tiene un valor que el jugador ya tiene delante **y** tiene 3 o más cartas
  delante (contadas **antes** de colocar la nueva), pierde el turno. Todas sus cartas de delante y la carta sacada se
  descartan (no suman puntos) y el turno pasa al siguiente jugador. Con 2 cartas y una repetida no pierde, y además
  puede robar ese valor.
- **Fin:** cuando se saca la última carta del montón de polvo. Todos guardan automáticamente lo que tengan delante,
  se suman los puntos y gana el mayor puntaje. **Desempate:** gana quien tenga más cartas de valor 1; si persiste,
  más de valor 2, y así hasta el 10. Si dos jugadores quedan idénticos en todo, ambos se declaran ganadores (empate).
- **Histórico:** cada partida terminada conserva su id, fechas, jugadores, cartas acumuladas, puntuación final y ganador.

## Requisitos

- **Java 21 o superior** (el proyecto compila con Java 25). No hace falta instalar Maven: se usa el wrapper `mvnw`.
- **Node.js 20.19 o superior** (probado con Node 22) y npm.
- No hace falta instalar ninguna base de datos: H2 viene incluida.

## Instalación

```bash
# Backend: descarga Maven y las dependencias la primera vez
cd backend
./mvnw clean install -DskipTests        # Windows: .\mvnw.cmd clean install -DskipTests

# Frontend
cd ../frontend
npm install
npx playwright install chromium         # navegador para las pruebas funcionales
```

> **Si Maven falla con `PKIX path building failed`** (un antivirus o proxy inspecciona HTTPS), en Windows crea el
> archivo `backend/.mvn/jvm.config` con esta línea, que no se sube al repositorio:
> `-Djavax.net.ssl.trustStoreType=Windows-ROOT`

## Configuración y base de datos

`backend/src/main/resources/application.properties`:

| Perfil | Base de datos | Uso |
|---|---|---|
| por defecto | H2 en archivo `backend/data/pelusas` (`ddl-auto=update`) | aplicación: el histórico se conserva entre reinicios |
| `e2e` | H2 en memoria (`create-drop`) | pruebas de Playwright: base limpia en cada ejecución |
| pruebas (`src/test/resources`) | H2 en memoria | pruebas unitarias y de integración |

JPA crea las tablas automáticamente; no hay scripts que ejecutar. La consola web de la base de datos está en
**http://localhost:8080/h2-console**: JDBC URL `jdbc:h2:file:./data/pelusas;AUTO_SERVER=TRUE`, usuario `sa`, sin
contraseña. Para borrar el histórico, elimina la carpeta `backend/data/` con el backend detenido.

## Ejecución

```bash
# 1. Backend: http://localhost:8080
cd backend
./mvnw spring-boot:run                  # Windows: .\mvnw.cmd spring-boot:run

# 2. Frontend: http://localhost:5173 (en otra terminal)
cd frontend
npm run dev
```

Abre **http://localhost:5173**, escribe los nombres de los jugadores y pulsa **Iniciar partida**. En
**Opciones avanzadas** puedes poner una **semilla** para que las cartas salgan siempre en el mismo orden:

| Semilla | Primeras cartas | Útil para mostrar |
|---|---|---|
| 2 | 7, 5, 9, 8 | turno exitoso y guardar pelusas |
| 3 | 9, 5, 1, 5 | turno perdido (la 4.ª carta repite con 3 cartas delante) |
| 4 | 5, 5, 4, 4 | robo (el 2.º jugador saca 5 y roba el 5 del 1.º) |

## API REST

Todos los ejemplos con CURL están en [docs/CURL.md](docs/CURL.md).

| Método | Endpoint | Descripción | Códigos |
|---|---|---|---|
| POST | `/api/partidas` | Crear partida `{jugadores, semilla?}` | 201, 400 |
| GET | `/api/partidas/{id}` | Estado de la partida y acciones permitidas | 200, 400, 404 |
| GET | `/api/partidas/{id}/jugadores` | Jugadores | 200, 404 |
| POST | `/api/partidas/{id}/turno/guardar` | Guardar pelusas `{jugadorId}` | 200, 400, 404, 409 |
| POST | `/api/partidas/{id}/turno/sacar` | Sacar una carta `{jugadorId}` | 200, 400, 404, 409 |
| POST | `/api/partidas/{id}/turno/decision` | Continuar o parar `{jugadorId, decision: CONTINUAR\|PARAR}` | 200, 400, 404, 409 |
| POST | `/api/partidas/{id}/turno/robar` | Robar cartas `{jugadorId}` | 200, 400, 404, 409 |
| GET | `/api/partidas/{id}/resultado` | Resultado (solo si terminó) | 200, 404, 409 |
| GET | `/api/partidas/historico` | Histórico de partidas terminadas | 200 |

Los errores responden `{estado, codigo, mensaje}`:

- **400:** solicitud inválida, jugadores inválidos o jugador ajeno a la partida.
- **404:** la partida no existe.
- **409:** acción no permitida por las reglas, por ejemplo `NO_ES_SU_TURNO`, `NO_PUEDE_ROBAR` o `PARTIDA_FINALIZADA`.

## Pruebas

Todos los comandos de backend se ejecutan dentro de `backend/`. En Windows usa `.\mvnw.cmd` en lugar de `./mvnw`.

### Pruebas unitarias (JUnit 5 + Mockito): clases `*Test`

```bash
./mvnw test
```

| Clase | Qué prueba |
|---|---|
| `MazoTest` | 110 cartas, 13 copias del 1 al 5 y 9 del 6 al 10, valores permitidos, extracción, mazo vacío, distribuciones inválidas |
| `JugadorTest` | regla de pérdida (1, 2, 3 y más de 3 cartas, carta repetida y no repetida, 7-7-7), guardar, descartar, entregar cartas |
| `PartidaTest` | creación y validaciones, sacar, continuar, plantarse, guardar, robar, pérdida, rotación de turnos, fin de partida, invariante de 110 cartas |
| `DesempateTest` | ganador por puntos, desempate por valor 1, 2… hasta encontrar diferencia, empate total |
| `BarajadorTest` | la semilla hace la mezcla reproducible y se conservan las cartas |
| `PartidaServiceTest` | **Mockito:** simula `PartidaRepository` (la base de datos) y `Barajador` (el azar) para probar los casos de uso aislados |
| `ResultadoResponseTest` | **Mockito:** simula una partida empatada para probar el DTO de resultado |

### Pruebas de integración: clases `*IT`

```bash
./mvnw test-compile failsafe:integration-test failsafe:verify
```

- `PartidaRepositoryIT` (`@DataJpaTest`, **base H2 real, sin mocks**): persistencia de partidas y jugadores,
  recuperación, actualización del estado, relación partida–jugadores y consulta del histórico.
- `PartidaControllerIT` (`@SpringBootTest` + **MockMvc**, sin mocks): todos los endpoints, con códigos HTTP,
  request y response, validaciones, manejo de errores y flujos completos. Por ejemplo: crear por HTTP, consultar,
  sacar y volver a consultar para comprobar que el estado cambió. Incluye turno exitoso, turno perdido, robo,
  partida completa, resultado e histórico.

### Todas las pruebas y cobertura (JaCoCo)

```bash
./mvnw verify
```

Ejecuta las unitarias y las de integración y genera el reporte en `backend/target/site/jacoco/index.html`.
**El build falla si la cobertura de líneas, ramas o métodos baja del 100 %.** Solo se excluye
`PelusasBackendApplication` (el `main` que arranca Spring, sin lógica).

### Pruebas de mutación (PIT)

```bash
./mvnw test-compile org.pitest:pitest-maven:mutationCoverage
```

El reporte queda en `backend/target/pit-reports/index.html`. **El build falla si el mutation score baja del 90 %.**

### Pruebas funcionales (Playwright + JavaScript)

```bash
cd frontend
npx playwright test               # sin ventana del navegador
npx playwright test --headed      # viendo el navegador
npx playwright show-report        # reporte HTML con capturas
```

Playwright **levanta solo** el backend (perfil `e2e`, puerto 8081) y el frontend (puerto 5174), así que no hace
falta tener nada corriendo y no interfiere con la aplicación abierta en 8080 y 5173. Usa semillas fijas, por lo que
los resultados son reproducibles.

| Archivo | Escenario |
|---|---|
| `e2e/turno-exitoso.spec.js` | **Caso obligatorio 1:** abrir, configurar, sacar, continuar, plantarse, verificar el cambio de turno y que las cartas permanecen; luego guardarlas. También: no poder guardar después de sacar y la validación de nombres en el backend. |
| `e2e/turno-perdido.spec.js` | **Caso obligatorio 2:** 3 cartas delante, sale una repetida, se detecta la pérdida, se descartan las cartas, pasa el turno y se verifica visualmente (con captura adjunta). También: una repetida con menos de 3 cartas no hace perder. |
| `e2e/robo.spec.js` | Robo de cartas y pérdida de la oportunidad de robar al sacar otra carta |
| `e2e/partida-completa.spec.js` | Partida completa de 110 cartas: ganador, guardado automático e histórico |

## Reportes (evidencia)

- Cobertura: [docs/reportes/cobertura/index.html](docs/reportes/cobertura/index.html)
- Mutación: [docs/reportes/mutacion/index.html](docs/reportes/mutacion/index.html)

## Decisiones de diseño

- **Las reglas viven en el dominio, no en el servicio ni en el controller.** Así se prueban con pruebas unitarias
  rápidas, sin Spring, y no existe forma de saltarse una regla desde la API.
- **El backend calcula las acciones permitidas** y el frontend no decide nada: si el usuario fuerza una petición,
  el backend responde 409.
- **Semilla opcional en la creación:** el mazo se mezcla con `Random(semilla)`, de modo que las pruebas
  funcionales y la demostración son reproducibles sin falsear el mazo, que siempre tiene la distribución oficial.
- **La regla de pérdida cuenta las cartas antes de colocar la nueva** (reglamento oficial): con 2 cartas y una
  repetida no se pierde.
- **H2:** no requiere instalación, así el proyecto corre igual en cualquier equipo y durante la presentación. Las
  pruebas de integración usan una base H2 real, no mocks. Cambiar a PostgreSQL solo requiere el driver y la URL.
- **Unitarias e integración separadas** con Surefire (`*Test`) y Failsafe (`*IT`), y un único reporte de cobertura
  que combina ambas.
