package com.sakura.reporte.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sakura.reporte.client.SesionGuard;
import com.sakura.reporte.model.FilaServicio;
import com.sakura.reporte.model.FranjaDemanda;
import com.sakura.reporte.model.Gasto;
import com.sakura.reporte.model.IngresosDia;
import com.sakura.reporte.model.RendimientoProfesional;
import com.sakura.reporte.model.ReporteServicios;
import com.sakura.reporte.service.ExportadorService;
import com.sakura.reporte.service.ReporteService;

@RestController
@RequestMapping("/api/v1/reportes")
public class ReporteController {

	@Autowired
	private ReporteService reporteService;

	@Autowired
	private ExportadorService exportadorService;

	@Autowired
	private SesionGuard sesionGuard;

	@GetMapping("/profesionales")
	public ResponseEntity<?> profesionales(
			@RequestHeader(value = "Authorization", required = false) String authorization,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
			@RequestParam(required = false) String formato) {
		sesionGuard.exigirAdmin(authorization);
		List<RendimientoProfesional> filas = reporteService.rendimiento(desde, hasta, authorization);
		return entregar("rendimiento", List.of("Profesional", "Citas", "Ingresos"), filas.stream()
				.map(fila -> List.of(fila.getProfesional(), String.valueOf(fila.getCitasAtendidas()),
						String.valueOf(fila.getIngresos())))
				.toList(), filas.stream().map(RendimientoProfesional::getProfesional).toList(),
				filas.stream().map(RendimientoProfesional::getIngresos).toList(), formato, filas);
	}

	@GetMapping("/dia")
	public Object dia(@RequestHeader(value = "Authorization", required = false) String authorization,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
		sesionGuard.exigirAdmin(authorization);
		return reporteService.resumenDia(fecha, authorization);
	}

	@GetMapping("/ingresos")
	public ResponseEntity<?> ingresos(@RequestHeader(value = "Authorization", required = false) String authorization,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
			@RequestParam(required = false) String formato) {
		sesionGuard.exigirAdmin(authorization);
		IngresosDia ingresos = reporteService.ingresos(fecha, authorization);
		return entregar("ingresos", List.of("Concepto", "Monto"),
				List.of(List.of("Abonos", String.valueOf(ingresos.getAbonos())),
						List.of("Saldos", String.valueOf(ingresos.getSaldos())),
						List.of("Total", String.valueOf(ingresos.getTotal()))),
				List.of("Abonos", "Saldos"), List.of(ingresos.getAbonos(), ingresos.getSaldos()), formato, ingresos);
	}

	@GetMapping("/servicios")
	public ResponseEntity<?> servicios(@RequestHeader(value = "Authorization", required = false) String authorization,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
			@RequestParam(required = false) String formato) {
		sesionGuard.exigirAdmin(authorization);
		ReporteServicios reporte = reporteService.servicios(desde, hasta, authorization);
		if ("png".equalsIgnoreCase(formato)) {
			return png("servicios", reporte.getServicios().stream().map(FilaServicio::getServicio).toList(),
					reporte.getServicios().stream().map(FilaServicio::getCantidad).toList());
		}
		return ResponseEntity.ok(reporte);
	}

	@GetMapping("/gastos")
	public ResponseEntity<?> gastos(@RequestHeader(value = "Authorization", required = false) String authorization,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate semana,
			@RequestParam(required = false) String formato) {
		sesionGuard.exigirAdmin(authorization);
		List<Gasto> gastos = reporteService.gastosSemana(semana);
		return entregar("gastos", List.of("Fecha", "Categoría", "Concepto", "Monto"), gastos.stream()
				.map(gasto -> List.of(gasto.getFecha().toString(), gasto.getCategoria(), gasto.getConcepto(),
						String.valueOf(gasto.getMonto())))
				.toList(), gastos.stream().map(Gasto::getConcepto).toList(),
				gastos.stream().map(Gasto::getMonto).toList(), formato, gastos);
	}

	@GetMapping("/inasistencias")
	public Object inasistencias(@RequestHeader(value = "Authorization", required = false) String authorization,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
		sesionGuard.exigirAdmin(authorization);
		return reporteService.inasistencias(desde, hasta, authorization);
	}

	@GetMapping("/demanda")
	public List<FranjaDemanda> demanda(@RequestHeader(value = "Authorization", required = false) String authorization,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate mes) {
		sesionGuard.exigirAdmin(authorization);
		return reporteService.demanda(mes, authorization);
	}

	private ResponseEntity<?> entregar(String nombre, List<String> columnas, List<List<String>> filas,
			List<String> etiquetas, List<Integer> valores, String formato, Object json) {
		if ("excel".equalsIgnoreCase(formato)) {
			return archivo(exportadorService.excel(nombre, columnas, filas), nombre + ".xlsx",
					MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
		}
		if ("pdf".equalsIgnoreCase(formato)) {
			List<String> lineas = new ArrayList<>();
			for (List<String> fila : filas) {
				lineas.add(String.join(" | ", fila));
			}
			return archivo(exportadorService.pdf(nombre, lineas), nombre + ".pdf", MediaType.APPLICATION_PDF);
		}
		if ("png".equalsIgnoreCase(formato)) {
			return png(nombre, etiquetas, valores);
		}
		return ResponseEntity.ok(json);
	}

	private ResponseEntity<byte[]> png(String nombre, List<String> etiquetas, List<Integer> valores) {
		return archivo(exportadorService.png(nombre, etiquetas, valores), nombre + ".png", MediaType.IMAGE_PNG);
	}

	private ResponseEntity<byte[]> archivo(byte[] cuerpo, String nombre, MediaType tipo) {
		return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + nombre)
				.contentType(tipo).body(cuerpo);
	}

}
