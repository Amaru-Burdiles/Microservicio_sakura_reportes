package com.sakura.reporte.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReporteInasistencias {

	private int noConcretadas;

	private int porcentaje;

	private List<ClienteAusente> clientes;

}
