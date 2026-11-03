import { useCallback, useEffect, useState } from 'react';
import { api } from '../api.js';
import { Carta, GruposDeCartas } from './Carta.jsx';
import Resultado from './Resultado.jsx';

const ESTILO_EVENTO = {
  TURNO_PERDIDO: 'peligro',
  CARTAS_ROBADAS: 'aviso',
  PELUSAS_GUARDADAS: 'exito',
  PARTIDA_FINALIZADA: 'exito',
};

export default function Mesa({ partidaId, onNueva, onHistorico }) {
  const [partida, setPartida] = useState(null);
  const [resultado, setResultado] = useState(null);
  const [error, setError] = useState(null);
  const [ocupado, setOcupado] = useState(false);

  const actualizar = useCallback(async (estado) => {
    setPartida(estado);
    if (estado.estado === 'FINALIZADA') {
      setResultado(await api.resultado(estado.id));
    }
  }, []);

  useEffect(() => {
    setResultado(null);
    api.obtenerPartida(partidaId).then(actualizar).catch((e) => setError(e.message));
  }, [partidaId, actualizar]);

  if (!partida) {
    return <p className="cargando">{error ?? 'Cargando partida…'}</p>;
  }

  const actual = partida.jugadores.find((j) => j.id === partida.jugadorActualId);
  const { acciones } = partida;

  const ejecutar = async (accion) => {
    setError(null);
    setOcupado(true);
    try {
      await actualizar(await accion());
    } catch (e) {
      setError(e.message);
    } finally {
      setOcupado(false);
    }
  };

  const guardar = () => ejecutar(() => api.guardar(partida.id, actual.id));
  const sacar = () =>
    ejecutar(() =>
      partida.acciones.puedePlantarse
        ? api.decidir(partida.id, actual.id, 'CONTINUAR')
        : api.sacar(partida.id, actual.id),
    );
  const robar = () => ejecutar(() => api.robar(partida.id, actual.id));
  const plantarse = () => ejecutar(() => api.decidir(partida.id, actual.id, 'PARAR'));

  const cartasRobables = acciones.puedeRobar
    ? partida.jugadores
        .filter((j) => j.id !== actual.id)
        .flatMap((j) => j.cartasDelante)
        .filter((c) => c === acciones.valorRobable).length
    : 0;

  const terminada = partida.estado === 'FINALIZADA';

  return (
    <section className="mesa" data-testid="mesa" data-estado={partida.estado}>
      <div className="tablero-superior">
        <div className="monton" data-testid="monton-polvo">
          <div className="dorso" aria-hidden="true" />
          <div>
            <strong data-testid="cartas-mazo">{partida.cartasEnMazo}</strong>
            <span>cartas en el montón de polvo</span>
          </div>
        </div>

        <div className="ultima-carta" data-testid="ultima-carta" data-valor={partida.ultimaCarta ?? ''}>
          {partida.ultimaCarta ? <Carta valor={partida.ultimaCarta} grande /> : <div className="carta-vacia">?</div>}
          <span>Última carta</span>
        </div>

        <div className="descartes">
          <strong data-testid="cartas-descartadas">{partida.cartasDescartadas}</strong>
          <span>cartas descartadas</span>
        </div>
      </div>

      <p
        className={`evento ${ESTILO_EVENTO[partida.ultimoEvento] ?? ''}`}
        data-testid="evento"
        data-tipo={partida.ultimoEvento}
        role="status"
      >
        {partida.mensajeEvento}
      </p>

      {!terminada && (
        <div className="barra-turno">
          <p className="turno">
            Turno de <strong data-testid="turno-actual">{actual.nombre}</strong>
          </p>
          <div className="acciones">
            <button className="boton exito" onClick={guardar} disabled={ocupado || !acciones.puedeGuardar} data-testid="btn-guardar">
              Guardar pelusas
            </button>
            <button className="boton principal boton-sacar" onClick={sacar} disabled={ocupado || !acciones.puedeSacar} data-testid="btn-sacar">
              {acciones.puedePlantarse ? 'Continuar: sacar otra' : 'Sacar carta'}
            </button>
            <button className="boton aviso" onClick={robar} disabled={ocupado || !acciones.puedeRobar} data-testid="btn-robar">
              {acciones.puedeRobar ? `Robar los ${acciones.valorRobable} (${cartasRobables})` : 'Robar'}
            </button>
            <button className="boton secundario" onClick={plantarse} disabled={ocupado || !acciones.puedePlantarse} data-testid="btn-plantarse">
              Plantarse
            </button>
          </div>
          <p className="ayuda">
            {acciones.puedeGuardar
              ? 'Inicio de turno: puedes guardar tus pelusas antes de sacar. Si sacas sin guardar, seguirán en riesgo.'
              : acciones.puedeRobar
                ? 'Otro jugador tiene cartas de este valor: róbalas ahora o perderás la oportunidad al sacar otra carta.'
                : acciones.puedePlantarse
                  ? 'Ojo: con 3 o más cartas delante, si sale un valor repetido pierdes todas.'
                  : 'Saca una carta del montón de polvo.'}
          </p>
        </div>
      )}

      {error && (
        <p className="error" role="alert" data-testid="error">
          {error}
        </p>
      )}

      <div className="jugadores">
        {partida.jugadores.map((jugador) => {
          const esActual = !terminada && jugador.id === partida.jugadorActualId;
          return (
            <article
              key={jugador.id}
              className={`jugador ${esActual ? 'actual' : ''} ${jugador.ganador ? 'ganador' : ''}`}
              data-testid={`jugador-${jugador.posicion}`}
              data-actual={esActual}
            >
              <header>
                <h3 data-testid={`nombre-${jugador.posicion}`}>{jugador.nombre}</h3>
                {esActual && <span className="insignia">Su turno</span>}
                {jugador.posicion === 0 && <span className="insignia suave">Inicia</span>}
              </header>
              <p className="etiqueta">Delante (en riesgo): {jugador.cartasDelante.length}</p>
              <GruposDeCartas cartas={jugador.cartasDelante} testId={`cartas-delante-${jugador.posicion}`} />
              <footer>
                <span>
                  Montón de puntos: <strong data-testid={`monton-${jugador.posicion}`}>{jugador.montonPuntos.length}</strong> cartas
                </span>
                <span>
                  <strong data-testid={`puntos-${jugador.posicion}`}>{jugador.puntos}</strong> puntos
                </span>
              </footer>
            </article>
          );
        })}
      </div>

      {resultado && <Resultado resultado={resultado} onNueva={onNueva} onHistorico={onHistorico} />}
    </section>
  );
}
