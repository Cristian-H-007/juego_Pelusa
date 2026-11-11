import { test, expect } from '@playwright/test';
import { SEMILLA, crearPartida, sacar } from './utilidades.js';

test.describe('Finalización e histórico', () => {
  test('al agotarse el montón de polvo se muestra el ganador y la partida queda en el histórico', async ({ page }) => {
    test.setTimeout(300_000);
    await crearPartida(page, ['Ana', 'Luis'], SEMILLA.EXITO);
    const mesa = page.getByTestId('mesa');
    const textoPartida = await page.getByTestId('nav-mesa').textContent();
    const partidaId = textoPartida.replace(/\D/g, '');

    // Cada jugador guarda si puede, saca una carta y se planta, hasta terminar las 110 cartas
    // Antes de cada decisión se espera a que la interfaz termine de actualizarse (botón Sacar habilitado),
    // para no leer el estado de los botones mientras la petición anterior sigue en curso.
    const sacarHabilitado = () => expect(page.getByTestId('btn-sacar')).toBeEnabled();
    while ((await mesa.getAttribute('data-estado')) === 'EN_CURSO') {
      await sacarHabilitado();
      if (await page.getByTestId('btn-guardar').isEnabled()) {
        await page.getByTestId('btn-guardar').click();
        await expect(page.getByTestId('btn-guardar')).toBeDisabled();
        await sacarHabilitado();
      }
      await sacar(page);
      if ((await mesa.getAttribute('data-estado')) === 'EN_CURSO') {
        await expect(page.getByTestId('btn-plantarse')).toBeEnabled();
        await page.getByTestId('btn-plantarse').click();
        await expect(page.getByTestId('btn-sacar')).toHaveText('Sacar carta');
      }
    }

    await expect(page.getByTestId('cartas-mazo')).toHaveText('0');
    await expect(page.getByTestId('evento')).toHaveAttribute('data-tipo', 'PARTIDA_FINALIZADA');
    await expect(page.getByTestId('resultado')).toBeVisible();
    await expect(page.getByTestId('ganador')).toContainText(/Gana (Ana|Luis)/);
    await expect(page.getByTestId('btn-sacar')).toHaveCount(0);

    // Las cartas que quedaban delante se guardaron automáticamente: entre ambos suman 110 cartas y 555 puntos
    const monton = async (p) => Number(await page.getByTestId(`monton-${p}`).textContent());
    const puntos = async (p) => Number(await page.getByTestId(`puntos-${p}`).textContent());
    expect((await monton(0)) + (await monton(1))).toBe(110);
    expect((await puntos(0)) + (await puntos(1))).toBe(555);
    await expect(page.getByTestId('cartas-delante-0')).toHaveText('Sin cartas delante');
    await expect(page.getByTestId('cartas-delante-1')).toHaveText('Sin cartas delante');

    await page.getByTestId('nav-historico').click();
    await expect(page.getByTestId('historico')).toBeVisible();
    await expect(page.getByTestId(`historico-${partidaId}`)).toContainText('Ana: 55');
    await expect(page.getByTestId(`historico-${partidaId}`)).toContainText('Luis: 55');
  });
});
