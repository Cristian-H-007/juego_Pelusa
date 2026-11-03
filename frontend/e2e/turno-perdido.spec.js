import { test, expect } from '@playwright/test';
import { SEMILLA, cartasDelante, crearPartida, sacar } from './utilidades.js';

test.describe('Caso funcional 2: turno perdido', () => {
  test('con 3 cartas delante saca un valor repetido: pierde las cartas y el turno pasa al siguiente', async ({ page }, testInfo) => {
    // 1-2. Crear/configurar la partida e iniciar el juego
    await crearPartida(page, ['Ana', 'Luis', 'Eva'], SEMILLA.PIERDE);
    await expect(page.getByTestId('turno-actual')).toHaveText('Ana');

    // 3. Llevar al jugador a un estado con al menos 3 cartas delante
    await sacar(page, 9);
    await sacar(page, 5);
    await sacar(page, 1);
    expect(await cartasDelante(page, 0)).toEqual([1, 5, 9]);
    await expect(page.getByTestId('cartas-descartadas')).toHaveText('0');

    // 4. Sacar una carta cuyo valor ya tiene
    await sacar(page, 5);

    // 5. El sistema detecta la pérdida
    const evento = page.getByTestId('evento');
    await expect(evento).toHaveAttribute('data-tipo', 'TURNO_PERDIDO');
    await expect(evento).toHaveText('Ana sacó un 5 repetido y perdió el turno. Se descartaron 4 carta(s). Turno de Luis');

    // 6. Las cartas se descartan (no van al montón de puntos)
    await expect(page.getByTestId('cartas-delante-0')).toHaveText('Sin cartas delante');
    await expect(page.getByTestId('monton-0')).toHaveText('0');
    await expect(page.getByTestId('puntos-0')).toHaveText('0');
    await expect(page.getByTestId('cartas-descartadas')).toHaveText('4');
    await expect(page.getByTestId('cartas-mazo')).toHaveText('106');

    // 7. El turno pasa al siguiente jugador
    await expect(page.getByTestId('turno-actual')).toHaveText('Luis');
    await expect(page.getByTestId('jugador-1')).toHaveAttribute('data-actual', 'true');
    await expect(page.getByTestId('jugador-0')).toHaveAttribute('data-actual', 'false');
    await expect(page.getByTestId('btn-sacar')).toHaveText('Sacar carta');
    await expect(page.getByTestId('btn-plantarse')).toBeDisabled();

    // 8. Verificación visual del nuevo estado del juego
    await expect(evento).toBeVisible();
    await expect(evento).toHaveClass(/peligro/);
    await expect(page.getByTestId('jugador-1').getByText('Su turno')).toBeVisible();
    await testInfo.attach('estado-tras-perder-el-turno', {
      body: await page.screenshot({ fullPage: true }),
      contentType: 'image/png',
    });
  });

  test('una carta repetida con menos de 3 cartas delante no hace perder', async ({ page }) => {
    await crearPartida(page, ['Ana', 'Luis'], SEMILLA.ROBO);
    await sacar(page, 5);
    await sacar(page, 5); // segundo 5 con solo 1 carta delante

    await expect(page.getByTestId('evento')).toHaveAttribute('data-tipo', 'CARTA_SACADA');
    expect(await cartasDelante(page, 0)).toEqual([5, 5]);
    await expect(page.getByTestId('turno-actual')).toHaveText('Ana');
    await expect(page.getByTestId('cartas-delante-0').getByTestId('grupo')).toHaveAttribute('data-cantidad', '2');
  });
});
