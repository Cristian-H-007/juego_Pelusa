import { test, expect } from '@playwright/test';
import { SEMILLA, cartasDelante, crearPartida, plantarse, sacar } from './utilidades.js';

test.describe('Caso funcional 1: turno exitoso', () => {
  test('el jugador saca cartas, finaliza su turno y en su siguiente turno guarda sus pelusas', async ({ page }) => {
    // 1-3. Abrir la aplicación, configurar la partida e iniciar el juego
    await crearPartida(page, ['Ana', 'Luis'], SEMILLA.EXITO);

    await expect(page.getByTestId('turno-actual')).toHaveText('Ana');
    await expect(page.getByTestId('jugador-0')).toHaveAttribute('data-actual', 'true');
    await expect(page.getByTestId('btn-guardar')).toBeDisabled();
    await expect(page.getByTestId('btn-plantarse')).toBeDisabled();
    await expect(page.getByTestId('btn-sacar')).toHaveText('Sacar carta');

    // 4-5. Realizar un turno: sacar una carta y continuar
    await sacar(page, 7);
    await expect(page.getByTestId('evento')).toHaveText('Ana sacó un 7');
    expect(await cartasDelante(page, 0)).toEqual([7]);
    await expect(page.getByTestId('btn-sacar')).toHaveText('Continuar: sacar otra');
    await expect(page.getByTestId('btn-plantarse')).toBeEnabled();

    await sacar(page, 5);
    expect(await cartasDelante(page, 0)).toEqual([5, 7]); // agrupadas y ordenadas por valor

    // 6-7. Finalizar voluntariamente: el turno termina y pasa al siguiente jugador
    await plantarse(page, 'Luis');
    await expect(page.getByTestId('evento')).toHaveAttribute('data-tipo', 'PLANTADO');
    await expect(page.getByTestId('jugador-1')).toHaveAttribute('data-actual', 'true');
    await expect(page.getByTestId('jugador-0')).toHaveAttribute('data-actual', 'false');

    // 8. Las cartas permanecen delante de Ana (en riesgo) hasta su siguiente turno
    expect(await cartasDelante(page, 0)).toEqual([5, 7]);
    await expect(page.getByTestId('monton-0')).toHaveText('0');

    await sacar(page, 9);
    await plantarse(page, 'Ana');

    // De vuelta en su turno, Ana guarda las pelusas que sobrevivieron
    await expect(page.getByTestId('btn-guardar')).toBeEnabled();
    await page.getByTestId('btn-guardar').click();

    await expect(page.getByTestId('evento')).toHaveAttribute('data-tipo', 'PELUSAS_GUARDADAS');
    await expect(page.getByTestId('monton-0')).toHaveText('2');
    await expect(page.getByTestId('puntos-0')).toHaveText('12');
    await expect(page.getByTestId('cartas-delante-0')).toHaveText('Sin cartas delante');
    await expect(page.getByTestId('btn-guardar')).toBeDisabled();
    expect(await cartasDelante(page, 1)).toEqual([9]);
  });

  test('si saca sin haber guardado, ya no puede guardar en ese turno', async ({ page }) => {
    await crearPartida(page, ['Ana', 'Luis'], SEMILLA.EXITO);
    await sacar(page, 7);
    await plantarse(page, 'Luis');
    await sacar(page, 5);
    await plantarse(page, 'Ana');

    await expect(page.getByTestId('btn-guardar')).toBeEnabled();
    await sacar(page, 9);

    await expect(page.getByTestId('btn-guardar')).toBeDisabled();
    expect(await cartasDelante(page, 0)).toEqual([7, 9]);
    await expect(page.getByTestId('monton-0')).toHaveText('0');
  });

  test('el backend valida la configuración de la partida', async ({ page }) => {
    await page.goto('/');
    await page.getByTestId('nombre-jugador-0').fill('Ana');
    await page.getByTestId('iniciar-partida').click();

    await expect(page.getByTestId('error')).toHaveText('Todos los jugadores deben tener nombre');
    await expect(page.getByTestId('mesa')).toHaveCount(0);
  });
});
