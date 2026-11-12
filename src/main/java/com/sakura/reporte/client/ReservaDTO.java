package com.sakura.reporte.client;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReservaDTO {

	private Long profesionalId;

	private String nombreServicio;

	private String nombreCategoria;

	private String nombreCliente;

	private LocalDate fecha;

	private LocalTime horaInicio;

	private String estado;

	private Integer montoPagado;

	private Integer montoTotal;

	private List<PrestacionDTO> prestaciones;

}
