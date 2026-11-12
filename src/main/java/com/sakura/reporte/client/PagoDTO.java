package com.sakura.reporte.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PagoDTO {

	private String tipo;

	private Integer monto;

}
