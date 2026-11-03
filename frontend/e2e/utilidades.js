import { expect } from '@playwright/test';

/**
 * Semillas del mazo usadas en las pruebas (orden de las primeras cartas que salen):
 *   2 -> 7, 5, 9, 8   (sin repeticiones: turno exitoso)
 *   3 -> 9, 5, 1, 5   (la cuarta carta repite con 3 cartas delante: turno perdido)
 *   4 -> 5, 5, 4, 4   (el segundo jugador puede robar el 5 del primero)
 */
export const SEMILLA = { EXITO: 2, PIERDE: 3, ROBO: 4 };

/** Abre la aplicación y crea una partida desde la interfaz, como lo haría un usuario. */
export async function crearPartida(page, nombres, semilla) {
  await page.goto('/');
  await expect(page.getByRole('heading', { name: 'Nueva partida' })).toBeVisible();

  for (let i = 2; i < nombres.length; i++) {
    await page.getByTestId('agregar-jugador').click();
  }
  for (let i = 0; i < nombres.length; i++) {
    await page.getByTestId(`nombre-jugador-${i}`).fill(nombres[i]);
  }
  if (semilla !== undefined) {
    await page.getByText('Opciones avanzadas').click();
    await page.getByTestId('semilla').fill(String(semilla));
  }
  await page.getByTestId('iniciar-partida').click();

  await expect(page.getByTestId('mesa')).toBeVisible();
  await expect(page.getByTestId('cartas-mazo')).toHaveText('110');
}

/** Valores de las cartas que un jugador tiene delante, en el orden en que se muestran. */
export async function cartasDelante(page, posicion) {
  const cartas = page.getByTestId(`cartas-delante-${posicion}`).getByTestId('carta');
  return (await cartas.evaluateAll((nodos) => nodos.map((n) => Number(n.dataset.valor))));
}

/** Pulsa "Sacar carta" y espera a que el backend responda con la carta esperada. */
export async function sacar(page, valorEsperado) {
  const antes = Number(await page.getByTestId('cartas-mazo').textContent());
  await page.getByTestId('btn-sacar').click();
  await expect(page.getByTestId('cartas-mazo')).toHaveText(String(antes - 1));
  if (valorEsperado !== undefined) {
    await expect(page.getByTestId('ultima-carta')).toHaveAttribute('data-valor', String(valorEsperado));
  }
}

export async function plantarse(page, siguienteJugador) {
  await page.getByTestId('btn-plantarse').click();
  await expect(page.getByTestId('turno-actual')).toHaveText(siguienteJugador);
}
