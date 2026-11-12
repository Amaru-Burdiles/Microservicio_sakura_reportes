package com.sakura.reporte.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "gastos")
@Data
@NoArgsConstructor
public class Gasto {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotNull(message = "La fecha es obligatoria")
	@Column(nullable = false)
	private LocalDate fecha;

	@NotBlank(message = "La categoría es obligatoria")
	@Column(nullable = false, length = 80)
	private String categoria;

	@NotBlank(message = "El concepto es obligatorio")
	@Column(nullable = false, length = 200)
	private String concepto;

	@NotNull(message = "El monto es obligatorio")
	@Positive(message = "El monto debe ser mayor a cero")
	@Column(nullable = false)
	private Integer monto;

}
