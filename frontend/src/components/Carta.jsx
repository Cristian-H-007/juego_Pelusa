/** Una carta de Pelusas. Cada valor tiene su propio color para reconocerlo de un vistazo. */
export function Carta({ valor, grande = false }) {
  return (
    <div className={`carta valor-${valor} ${grande ? 'grande' : ''}`} data-testid="carta" data-valor={valor}>
      <span className="esquina">{valor}</span>
      <span className="pelusa" aria-hidden="true">
        <span className="ojo" />
        <span className="ojo" />
      </span>
      <span className="numero">{valor}</span>
    </div>
  );
}

/** Agrupa las cartas del mismo valor en pilas para que el estado sea fácil de leer. */
export function GruposDeCartas({ cartas, testId }) {
  const conteo = new Map();
  for (const carta of cartas) {
    conteo.set(carta, (conteo.get(carta) ?? 0) + 1);
  }
  const grupos = [...conteo.entries()].sort(([a], [b]) => a - b);

  if (grupos.length === 0) {
    return (
      <div className="grupos vacio" data-testid={testId}>
        Sin cartas delante
      </div>
    );
  }

  return (
    <div className="grupos" data-testid={testId}>
      {grupos.map(([valor, cantidad]) => (
        <div className="grupo" key={valor} data-testid="grupo" data-valor={valor} data-cantidad={cantidad}>
          <div className="pila">
            {Array.from({ length: cantidad }, (_, i) => (
              <Carta key={i} valor={valor} />
            ))}
          </div>
          {cantidad > 1 && <span className="contador">×{cantidad}</span>}
        </div>
      ))}
    </div>
  );
}
