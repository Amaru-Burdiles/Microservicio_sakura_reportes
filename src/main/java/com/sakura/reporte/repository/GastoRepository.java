package com.sakura.reporte.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sakura.reporte.model.Gasto;

@Repository
public interface GastoRepository extends JpaRepository<Gasto, Long> {

	List<Gasto> findByFechaBetweenOrderByFechaAsc(LocalDate desde, LocalDate hasta);

}
