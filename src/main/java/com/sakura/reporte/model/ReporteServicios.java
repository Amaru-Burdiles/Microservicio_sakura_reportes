package com.sakura.reporte.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReporteServicios {

	private List<FilaServicio> servicios;

	private List<ParticipacionCategoria> categorias;

}
