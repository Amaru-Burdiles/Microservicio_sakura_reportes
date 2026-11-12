package com.sakura.reporte.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RendimientoProfesional {

	private String profesional;

	private int citasAtendidas;

	private int ingresos;

}
