// Cliente de la API REST del backend. El frontend solo solicita acciones:
// todas las reglas (turnos, pérdidas, robos, fin de partida) las valida el backend.

const BASE = '/api/partidas';

async function solicitar(url, opciones = {}) {
  const respuesta = await fetch(url, {
    headers: { 'Content-Type': 'application/json' },
    ...opciones,
  });
  const cuerpo = await respuesta.json().catch(() => null);
  if (!respuesta.ok) {
    throw new Error(cuerpo?.mensaje ?? `Error ${respuesta.status}`);
  }
  return cuerpo;
}

const post = (url, datos) => solicitar(url, { method: 'POST', body: JSON.stringify(datos) });

export const api = {
  crearPartida: (jugadores, semilla) => post(BASE, { jugadores, semilla }),
  obtenerPartida: (id) => solicitar(`${BASE}/${id}`),
  guardar: (id, jugadorId) => post(`${BASE}/${id}/turno/guardar`, { jugadorId }),
  sacar: (id, jugadorId) => post(`${BASE}/${id}/turno/sacar`, { jugadorId }),
  robar: (id, jugadorId) => post(`${BASE}/${id}/turno/robar`, { jugadorId }),
  comodin: (id, jugadorId) => post(`${BASE}/${id}/turno/comodin`, { jugadorId }),
  decidir: (id, jugadorId, decision) => post(`${BASE}/${id}/turno/decision`, { jugadorId, decision }),
  resultado: (id) => solicitar(`${BASE}/${id}/resultado`),
  historico: () => solicitar(`${BASE}/historico`),
};
