import { test, expect } from '@playwright/test';
import { SEMILLA, cartasDelante, crearPartida, plantarse, sacar } from './utilidades.js';

test.describe('Robo de cartas', () => {
  test('al sacar un valor que otro jugador tiene delante, puede robarle esas cartas', async ({ page }) => {
    await crearPartida(page, ['Ana', 'Luis'], SEMILLA.ROBO);
    await sacar(page, 5);
    await plantarse(page, 'Luis');
    await expect(page.getByTestId('btn-robar')).toBeDisabled();

    await sacar(page, 5);

    const robar = page.getByTestId('btn-robar');
    await expect(robar).toBeEnabled();
    await expect(robar).toHaveText('Robar los 5 (1)');
    await robar.click();

    await expect(page.getByTestId('evento')).toHaveAttribute('data-tipo', 'CARTAS_ROBADAS');
    await expect(page.getByTestId('evento')).toHaveText('Luis robó 1 carta(s) de valor 5');
    await expect(page.getByTestId('cartas-delante-0')).toHaveText('Sin cartas delante');
    expect(await cartasDelante(page, 1)).toEqual([5, 5]);
    await expect(page.getByTestId('cartas-delante-1').getByTestId('grupo')).toHaveAttribute('data-cantidad', '2');
    await expect(robar).toBeDisabled();
    await expect(page.getByTestId('turno-actual')).toHaveText('Luis');
  });

  test('si saca otra carta sin robar, pierde la oportunidad de robar', async ({ page }) => {
    await crearPartida(page, ['Ana', 'Luis'], SEMILLA.ROBO);
    await sacar(page, 5);
    await plantarse(page, 'Luis');
    await sacar(page, 5);
    await expect(page.getByTestId('btn-robar')).toBeEnabled();

    await sacar(page, 4);

    await expect(page.getByTestId('btn-robar')).toBeDisabled();
    expect(await cartasDelante(page, 0)).toEqual([5]);
    expect(await cartasDelante(page, 1)).toEqual([4, 5]);
  });
});
