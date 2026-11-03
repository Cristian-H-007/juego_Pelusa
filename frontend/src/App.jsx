import { useState } from 'react';
import NuevaPartida from './components/NuevaPartida.jsx';
import Mesa from './components/Mesa.jsx';
import Historico from './components/Historico.jsx';

export default function App() {
  const [vista, setVista] = useState('inicio');
  const [partidaId, setPartidaId] = useState(null);

  const iniciar = (id) => {
    setPartidaId(id);
    setVista('mesa');
  };

  return (
    <div className="app">
      <header className="cabecera">
        <h1 className="logo" onClick={() => setVista('inicio')}>
          <span className="logo-pelusa" aria-hidden="true" /> Pelusas
        </h1>
        <nav className="navegacion">
          <button
            className={vista === 'inicio' ? 'activo' : ''}
            onClick={() => setVista('inicio')}
            data-testid="nav-nueva"
          >
            Nueva partida
          </button>
          {partidaId && (
            <button className={vista === 'mesa' ? 'activo' : ''} onClick={() => setVista('mesa')} data-testid="nav-mesa">
              Partida #{partidaId}
            </button>
          )}
          <button
            className={vista === 'historico' ? 'activo' : ''}
            onClick={() => setVista('historico')}
            data-testid="nav-historico"
          >
            Histórico
          </button>
        </nav>
      </header>

      <main className="contenido">
        {vista === 'inicio' && <NuevaPartida onCreada={iniciar} />}
        {vista === 'mesa' && partidaId && (
          <Mesa partidaId={partidaId} onNueva={() => setVista('inicio')} onHistorico={() => setVista('historico')} />
        )}
        {vista === 'historico' && <Historico />}
      </main>
    </div>
  );
}
