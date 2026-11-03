import { useEffect, useState } from 'react';
import { api } from '../api.js';

const formatoFecha = new Intl.DateTimeFormat('es-CO', { dateStyle: 'medium', timeStyle: 'short' });

export default function Historico() {
  const [partidas, setPartidas] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    api.historico().then(setPartidas).catch((e) => setError(e.message));
  }, []);

  if (error) {
    return <p className="error">{error}</p>;
  }
  if (!partidas) {
    return <p className="cargando">Cargando histórico…</p>;
  }

  return (
    <section className="panel historico" data-testid="historico">
      <h2>Histórico de partidas</h2>
      {partidas.length === 0 ? (
        <p className="ayuda">Todavía no hay partidas terminadas.</p>
      ) : (
        <table className="tabla">
          <thead>
            <tr>
              <th>Partida</th>
              <th>Fecha</th>
              <th>Jugadores (cartas / puntos)</th>
              <th>Ganador</th>
            </tr>
          </thead>
          <tbody>
            {partidas.map((p) => (
              <tr key={p.partidaId} data-testid={`historico-${p.partidaId}`}>
                <td>#{p.partidaId}</td>
                <td>{p.fechaFin ? formatoFecha.format(new Date(p.fechaFin)) : '-'}</td>
                <td>
                  {p.jugadores.map((j) => (
                    <span key={j.id} className={`chip ${j.ganador ? 'ganador' : ''}`}>
                      {j.nombre}: {j.cantidadCartas} / {j.puntuacionFinal}
                    </span>
                  ))}
                </td>
                <td>
                  {p.empate ? 'Empate: ' : ''}
                  {p.ganadores.join(' y ')}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
