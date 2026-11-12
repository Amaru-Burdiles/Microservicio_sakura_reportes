package com.sakura.reporte.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FilaServicio {

	private String servicio;

	private String categoria;

	private int cantidad;

}
