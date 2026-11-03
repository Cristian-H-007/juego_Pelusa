import { useState } from 'react';
import { api } from '../api.js';

const MINIMO = 2;
const MAXIMO = 6;

export default function NuevaPartida({ onCreada }) {
  const [nombres, setNombres] = useState(['', '']);
  const [semilla, setSemilla] = useState('');
  const [error, setError] = useState(null);
  const [creando, setCreando] = useState(false);

  const cambiarNombre = (indice, valor) => setNombres(nombres.map((n, i) => (i === indice ? valor : n)));
  const agregar = () => setNombres([...nombres, '']);
  const quitar = (indice) => setNombres(nombres.filter((_, i) => i !== indice));

  const crear = async (evento) => {
    evento.preventDefault();
    setError(null);
    setCreando(true);
    try {
      const partida = await api.crearPartida(nombres, semilla === '' ? null : Number(semilla));
      onCreada(partida.id);
    } catch (e) {
      setError(e.message);
    } finally {
      setCreando(false);
    }
  };

  return (
    <section className="panel nueva-partida">
      <h2>Nueva partida</h2>
      <p className="ayuda">
        Escribe los nombres en el orden de juego. Inicia el primer jugador. Se juega entre {MINIMO} y {MAXIMO} jugadores.
      </p>

      <form onSubmit={crear}>
        <ol className="lista-jugadores">
          {nombres.map((nombre, i) => (
            <li key={i}>
              <input
                value={nombre}
                onChange={(e) => cambiarNombre(i, e.target.value)}
                placeholder={`Jugador ${i + 1}`}
                maxLength={20}
                aria-label={`Nombre del jugador ${i + 1}`}
                data-testid={`nombre-jugador-${i}`}
              />
              {nombres.length > MINIMO && (
                <button type="button" className="boton-icono" onClick={() => quitar(i)} aria-label={`Quitar jugador ${i + 1}`}>
                  ✕
                </button>
              )}
            </li>
          ))}
        </ol>

        {nombres.length < MAXIMO && (
          <button type="button" className="boton secundario" onClick={agregar} data-testid="agregar-jugador">
            + Agregar jugador
          </button>
        )}

        <details className="avanzado">
          <summary>Opciones avanzadas</summary>
          <label>
            Semilla del mazo (opcional)
            <input
              type="number"
              value={semilla}
              onChange={(e) => setSemilla(e.target.value)}
              placeholder="Aleatorio"
              data-testid="semilla"
            />
          </label>
          <p className="ayuda">Con la misma semilla las cartas salen en el mismo orden. Útil para pruebas y demostraciones.</p>
        </details>

        {error && (
          <p className="error" role="alert" data-testid="error">
            {error}
          </p>
        )}

        <button type="submit" className="boton principal" disabled={creando} data-testid="iniciar-partida">
          {creando ? 'Creando…' : 'Iniciar partida'}
        </button>
      </form>
    </section>
  );
}
