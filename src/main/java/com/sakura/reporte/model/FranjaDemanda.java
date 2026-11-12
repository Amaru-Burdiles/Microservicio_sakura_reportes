package com.sakura.reporte.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FranjaDemanda {

	private String dia;

	private String hora;

	private int ocupacion;

	private boolean baja;

}
