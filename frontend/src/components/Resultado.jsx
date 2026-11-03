export default function Resultado({ resultado, onNueva, onHistorico }) {
  const clasificacion = [...resultado.jugadores].sort((a, b) => b.puntuacionFinal - a.puntuacionFinal);

  return (
    <section className="panel resultado" data-testid="resultado">
      <h2>{resultado.empate ? '¡Empate!' : '¡Partida terminada!'}</h2>
      <p className="ganador" data-testid="ganador">
        {resultado.empate ? 'Ganan ' : 'Gana '}
        <strong>{resultado.ganadores.join(' y ')}</strong>
      </p>
      <table className="tabla">
        <thead>
          <tr>
            <th>#</th>
            <th>Jugador</th>
            <th>Cartas</th>
            <th>Puntos</th>
          </tr>
        </thead>
        <tbody>
          {clasificacion.map((jugador, i) => (
            <tr key={jugador.id} className={jugador.ganador ? 'fila-ganador' : ''}>
              <td>{i + 1}</td>
              <td>
                {jugador.nombre} {jugador.ganador && '🏆'}
              </td>
              <td>{jugador.cantidadCartas}</td>
              <td>{jugador.puntuacionFinal}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <p className="ayuda">En caso de empate en puntos gana quien tenga más cartas de valor 1; si persiste, de valor 2, y así sucesivamente.</p>
      <div className="acciones">
        <button className="boton principal" onClick={onNueva}>
          Nueva partida
        </button>
        <button className="boton secundario" onClick={onHistorico}>
          Ver histórico
        </button>
      </div>
    </section>
  );
}
