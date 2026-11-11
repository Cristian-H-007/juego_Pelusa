import { test, expect } from '@playwright/test';
import { SEMILLA, cartasDelante, crearPartida, plantarse, sacar } from './utilidades.js';

test.describe('Comodín', () => {
  test('protege las cartas boca arriba: el otro jugador no puede robarlas hasta el siguiente turno', async ({ page }) => {
    await crearPartida(page, ['Ana', 'Luis'], SEMILLA.ROBO);
    await expect(page.getByTestId('comodin-0')).toHaveText('Comodín: disponible');
    await expect(page.getByTestId('btn-comodin')).toBeEnabled();

    // Ana saca un 5 y usa su comodín
    await sacar(page, 5);
    await page.getByTestId('btn-comodin').click();

    await expect(page.getByTestId('evento')).toHaveAttribute('data-tipo', 'COMODIN_USADO');
    await expect(page.getByTestId('protegido-0')).toBeVisible();
    await expect(page.getByTestId('comodin-0')).toHaveText('Comodín: usado');
    await expect(page.getByTestId('btn-comodin')).toBeDisabled();
    await expect(page.getByTestId('btn-comodin')).toHaveText('Comodín usado');

    // Luis saca el mismo valor pero no puede robar porque Ana está protegida
    await plantarse(page, 'Luis');
    await expect(page.getByTestId('btn-comodin')).toBeEnabled(); // Luis tiene su propio comodín
    await sacar(page, 5);
    await expect(page.getByTestId('btn-robar')).toBeDisabled();
    expect(await cartasDelante(page, 0)).toEqual([5]);

    // Al volver el turno de Ana la protección termina, pero el comodín sigue gastado
    await plantarse(page, 'Ana');
    await expect(page.getByTestId('protegido-0')).toHaveCount(0);
    await expect(page.getByTestId('comodin-0')).toHaveText('Comodín: usado');
    await expect(page.getByTestId('btn-comodin')).toBeDisabled();
  });
});
