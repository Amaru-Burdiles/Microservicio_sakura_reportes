package com.sakura.reporte.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ResumenDia {

	private int agendadas;

	private int atendidas;

	private int canceladas;

}
