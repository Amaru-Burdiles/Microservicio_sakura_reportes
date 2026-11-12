package com.sakura.reporte.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PrestacionDTO {

	private Long profesionalId;

	private String nombreServicio;

	private Integer precio;

}
