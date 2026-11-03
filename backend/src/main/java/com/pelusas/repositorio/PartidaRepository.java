package com.pelusas.repositorio;

import com.pelusas.dominio.EstadoPartida;
import com.pelusas.dominio.Partida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PartidaRepository extends JpaRepository<Partida, Long> {

    /** Histórico: partidas en el estado indicado, de la más reciente a la más antigua. */
    @Query("select p from Partida p where p.estado = :estado order by p.fechaFin desc, p.id desc")
    List<Partida> buscarPorEstado(@Param("estado") EstadoPartida estado);
}
