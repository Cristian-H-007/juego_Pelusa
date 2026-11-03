# CURL de la API de Pelusas

Base: `http://localhost:8080/api/partidas`

Todas las peticiones y respuestas usan JSON. Los ejemplos usan la **semilla 3**, con la que las primeras
cartas del montón de polvo salen en este orden: **9, 5, 1, 5**. Así los resultados son reproducibles.

> **Windows (PowerShell):** usa `curl.exe` en lugar de `curl`, y escribe el JSON entre comillas simples tal como
> aparece aquí. En CMD, reemplaza las comillas simples por dobles y escapa las internas (`\"`).

En los ejemplos, `1` es el id de la partida y `1` / `2` son los ids de los jugadores. Usa los ids que
devuelva tu respuesta de creación.

---

## 1. Crear una partida

`POST /api/partidas` → **201 Created** (encabezado `Location: /api/partidas/{id}`)

```bash
curl -i -X POST http://localhost:8080/api/partidas \
  -H "Content-Type: application/json" \
  -d '{"jugadores":["Ana","Luis","Eva"],"semilla":3}'
```

- `jugadores`: de 2 a 6 nombres, en orden de juego. Inicia el primero.
- `semilla`: opcional. Si se omite, la mezcla es aleatoria.

Respuesta (resumida):

```json
{
  "id": 1,
  "estado": "EN_CURSO",
  "cartasEnMazo": 110,
  "cartasDescartadas": 0,
  "jugadorInicialId": 1,
  "jugadorActualId": 1,
  "ultimaCarta": null,
  "ultimoEvento": "PARTIDA_CREADA",
  "mensajeEvento": "Partida creada. Inicia Ana",
  "acciones": { "puedeGuardar": false, "puedeSacar": true, "puedeRobar": false, "puedePlantarse": false, "valorRobable": null },
  "jugadores": [
    { "id": 1, "nombre": "Ana", "posicion": 0, "cartasDelante": [], "montonPuntos": [], "puntos": 0, "puntuacionFinal": null, "ganador": false }
  ]
}
```

### Errores al crear

```bash
# 400 JUGADORES_INVALIDOS: menos de 2 jugadores
curl -i -X POST http://localhost:8080/api/partidas -H "Content-Type: application/json" -d '{"jugadores":["Ana"]}'

# 400 JUGADORES_INVALIDOS: nombres repetidos
curl -i -X POST http://localhost:8080/api/partidas -H "Content-Type: application/json" -d '{"jugadores":["Ana","ana"]}'

# 400 SOLICITUD_INVALIDA: falta la lista de jugadores
curl -i -X POST http://localhost:8080/api/partidas -H "Content-Type: application/json" -d '{}'

# 400 SOLICITUD_INVALIDA: JSON mal formado
curl -i -X POST http://localhost:8080/api/partidas -H "Content-Type: application/json" -d '{jugadores:'
```

Formato de todos los errores:

```json
{ "estado": 409, "codigo": "NO_ES_SU_TURNO", "mensaje": "No es el turno de Luis. Es el turno de Ana" }
```

---

## 2. Consultar el estado de una partida

`GET /api/partidas/{id}` → **200 OK**

```bash
curl -i http://localhost:8080/api/partidas/1
```

```bash
# 404 PARTIDA_NO_ENCONTRADA
curl -i http://localhost:8080/api/partidas/999999

# 400 SOLICITUD_INVALIDA: id no numérico
curl -i http://localhost:8080/api/partidas/abc
```

## 3. Consultar los jugadores

`GET /api/partidas/{id}/jugadores` → **200 OK**

```bash
curl -i http://localhost:8080/api/partidas/1/jugadores
```

---

## 4. Turno: sacar una carta (paso 2, buscar pelusas)

`POST /api/partidas/{id}/turno/sacar` → **200 OK**, devuelve el nuevo estado de la partida.

```bash
curl -i -X POST http://localhost:8080/api/partidas/1/turno/sacar \
  -H "Content-Type: application/json" -d '{"jugadorId":1}'
```

```bash
# 409 NO_ES_SU_TURNO: Luis intenta sacar en el turno de Ana
curl -i -X POST http://localhost:8080/api/partidas/1/turno/sacar -H "Content-Type: application/json" -d '{"jugadorId":2}'

# 400 SOLICITUD_INVALIDA: falta jugadorId
curl -i -X POST http://localhost:8080/api/partidas/1/turno/sacar -H "Content-Type: application/json" -d '{}'

# 400 JUGADOR_NO_PERTENECE: el jugador no es de esta partida
curl -i -X POST http://localhost:8080/api/partidas/1/turno/sacar -H "Content-Type: application/json" -d '{"jugadorId":999}'
```

## 5. Turno: decidir continuar o parar

`POST /api/partidas/{id}/turno/decision` → **200 OK**

- `CONTINUAR`: saca otra carta.
- `PARAR`: finaliza voluntariamente el turno. Las cartas quedan delante del jugador.

```bash
# Continuar
curl -i -X POST http://localhost:8080/api/partidas/1/turno/decision \
  -H "Content-Type: application/json" -d '{"jugadorId":1,"decision":"CONTINUAR"}'

# Parar (plantarse)
curl -i -X POST http://localhost:8080/api/partidas/1/turno/decision \
  -H "Content-Type: application/json" -d '{"jugadorId":1,"decision":"PARAR"}'
```

```bash
# 409 NO_PUEDE_PLANTARSE: parar antes de haber sacado una carta
# 400 SOLICITUD_INVALIDA: decisión desconocida
curl -i -X POST http://localhost:8080/api/partidas/1/turno/decision -H "Content-Type: application/json" -d '{"jugadorId":1,"decision":"SALTAR"}'
```

## 6. Turno: guardar pelusas (paso 1)

`POST /api/partidas/{id}/turno/guardar` → **200 OK**

Solo se permite al inicio del turno, antes de sacar, y si el jugador tiene cartas delante.

```bash
curl -i -X POST http://localhost:8080/api/partidas/1/turno/guardar \
  -H "Content-Type: application/json" -d '{"jugadorId":1}'
```

Si no se puede guardar, responde **409 NO_PUEDE_GUARDAR**.

## 7. Turno: robar cartas (paso 3)

`POST /api/partidas/{id}/turno/robar` → **200 OK**

Solo se permite justo después de sacar una carta cuyo valor tengan delante otros jugadores. Roba las cartas
de ese valor a todos ellos.

```bash
curl -i -X POST http://localhost:8080/api/partidas/1/turno/robar \
  -H "Content-Type: application/json" -d '{"jugadorId":2}'
```

Si no se puede robar, responde **409 NO_PUEDE_ROBAR**.

---

## 8. Consultar el resultado de la partida

`GET /api/partidas/{id}/resultado` → **200 OK** (solo si la partida terminó)

```bash
curl -i http://localhost:8080/api/partidas/1/resultado
```

Si la partida sigue en curso, responde **409 PARTIDA_NO_FINALIZADA**.

```json
{
  "partidaId": 1,
  "fechaCreacion": "2026-11-03T10:00:00",
  "fechaFin": "2026-11-03T10:25:00",
  "ganadores": ["Ana"],
  "empate": false,
  "jugadores": [
    { "id": 1, "nombre": "Ana", "cartasAcumuladas": [7, 3, 9], "cantidadCartas": 3, "puntuacionFinal": 19, "ganador": true }
  ]
}
```

## 9. Consultar el histórico de partidas

`GET /api/partidas/historico` → **200 OK**. Devuelve las partidas terminadas, de la más reciente a la más antigua.

```bash
curl -i http://localhost:8080/api/partidas/historico
```

---

## Guion completo: turno perdido (semilla 3)

```bash
# Crear: Ana (id 1) y Luis (id 2). Cartas: 9, 5, 1, 5
curl -s -X POST http://localhost:8080/api/partidas -H "Content-Type: application/json" -d '{"jugadores":["Ana","Luis"],"semilla":3}'

curl -s -X POST http://localhost:8080/api/partidas/1/turno/sacar    -H "Content-Type: application/json" -d '{"jugadorId":1}'                          # 9
curl -s -X POST http://localhost:8080/api/partidas/1/turno/decision -H "Content-Type: application/json" -d '{"jugadorId":1,"decision":"CONTINUAR"}'  # 5
curl -s -X POST http://localhost:8080/api/partidas/1/turno/decision -H "Content-Type: application/json" -d '{"jugadorId":1,"decision":"CONTINUAR"}'  # 1 (3 cartas delante)
curl -s -X POST http://localhost:8080/api/partidas/1/turno/decision -H "Content-Type: application/json" -d '{"jugadorId":1,"decision":"CONTINUAR"}'  # 5 repetido -> TURNO_PERDIDO

curl -s http://localhost:8080/api/partidas/1   # Ana sin cartas, cartasDescartadas = 4, turno de Luis
```

## Guion completo: robo (semilla 4)

```bash
# Cartas: 5, 5, 4, 4
curl -s -X POST http://localhost:8080/api/partidas -H "Content-Type: application/json" -d '{"jugadores":["Ana","Luis"],"semilla":4}'
curl -s -X POST http://localhost:8080/api/partidas/2/turno/sacar    -H "Content-Type: application/json" -d '{"jugadorId":3}'                       # Ana saca 5
curl -s -X POST http://localhost:8080/api/partidas/2/turno/decision -H "Content-Type: application/json" -d '{"jugadorId":3,"decision":"PARAR"}'  # Ana se planta
curl -s -X POST http://localhost:8080/api/partidas/2/turno/sacar    -H "Content-Type: application/json" -d '{"jugadorId":4}'                       # Luis saca 5
curl -s -X POST http://localhost:8080/api/partidas/2/turno/robar    -H "Content-Type: application/json" -d '{"jugadorId":4}'                       # Luis roba el 5 de Ana
```
