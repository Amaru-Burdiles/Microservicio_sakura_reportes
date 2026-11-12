package com.sakura.reporte.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sakura.reporte.client.SesionGuard;
import com.sakura.reporte.exception.NoAutenticadoException;
import com.sakura.reporte.model.FilaServicio;
import com.sakura.reporte.model.FranjaDemanda;
import com.sakura.reporte.model.Gasto;
import com.sakura.reporte.model.IngresosDia;
import com.sakura.reporte.model.RendimientoProfesional;
import com.sakura.reporte.model.ReporteInasistencias;
import com.sakura.reporte.model.ReporteServicios;
import com.sakura.reporte.model.ResumenDia;
import com.sakura.reporte.service.ExportadorService;
import com.sakura.reporte.service.ReporteService;

@WebMvcTest(ReporteController.class)
@ActiveProfiles("test")
class ReporteControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ReporteService reporteService;

	@MockitoBean
	private ExportadorService exportadorService;

	@MockitoBean
	private SesionGuard sesionGuard;

	private static final String AUTH = "Bearer token-admin";

	@Test
	void profesionalesJson_Exito() throws Exception {
		when(reporteService.rendimiento(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), AUTH))
				.thenReturn(List.of(new RendimientoProfesional("Ana", 3, 15000)));

		mockMvc.perform(get("/api/v1/reportes/profesionales")
				.header("Authorization", AUTH)
				.param("desde", "2026-05-01")
				.param("hasta", "2026-05-31"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].profesional").value("Ana"));
	}

	@Test
	void profesionalesExcel_Exito() throws Exception {
		when(reporteService.rendimiento(any(), any(), eq(AUTH)))
				.thenReturn(List.of(new RendimientoProfesional("Ana", 3, 15000)));
		when(exportadorService.excel(eq("rendimiento"), any(), any())).thenReturn("xlsx".getBytes());

		mockMvc.perform(get("/api/v1/reportes/profesionales")
				.header("Authorization", AUTH)
				.param("desde", "2026-05-01")
				.param("hasta", "2026-05-31")
				.param("formato", "excel"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=rendimiento.xlsx"));
	}

	@Test
	void profesionales_SinSesion_Responde401() throws Exception {
		doThrow(new NoAutenticadoException("Debe iniciar sesión")).when(sesionGuard).exigirAdmin(any());

		mockMvc.perform(get("/api/v1/reportes/profesionales").param("desde", "2026-05-01").param("hasta", "2026-05-31"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void dia_Exito() throws Exception {
		when(reporteService.resumenDia(LocalDate.of(2026, 5, 10), AUTH)).thenReturn(new ResumenDia(4, 3, 1));

		mockMvc.perform(get("/api/v1/reportes/dia").header("Authorization", AUTH).param("fecha", "2026-05-10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.agendadas").value(4));
	}

	@Test
	void ingresosPdf_Exito() throws Exception {
		when(reporteService.ingresos(LocalDate.of(2026, 5, 10), AUTH)).thenReturn(new IngresosDia(80, 70, 150));
		when(exportadorService.pdf(eq("ingresos"), any())).thenReturn("%PDF".getBytes());

		mockMvc.perform(get("/api/v1/reportes/ingresos")
				.header("Authorization", AUTH)
				.param("fecha", "2026-05-10")
				.param("formato", "pdf"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_PDF));
	}

	@Test
	void serviciosJson_Exito() throws Exception {
		when(reporteService.servicios(any(), any(), eq(AUTH)))
				.thenReturn(new ReporteServicios(List.of(new FilaServicio("Corte", "Peluquería", 2)), List.of()));

		mockMvc.perform(get("/api/v1/reportes/servicios")
				.header("Authorization", AUTH)
				.param("desde", "2026-05-01")
				.param("hasta", "2026-05-31"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.servicios[0].servicio").value("Corte"));
	}

	@Test
	void serviciosPng_Exito() throws Exception {
		when(reporteService.servicios(any(), any(), eq(AUTH)))
				.thenReturn(new ReporteServicios(List.of(new FilaServicio("Corte", "Peluquería", 2)), List.of()));
		when(exportadorService.png(eq("servicios"), any(), any())).thenReturn(new byte[] { 1, 2, 3 });

		mockMvc.perform(get("/api/v1/reportes/servicios")
				.header("Authorization", AUTH)
				.param("desde", "2026-05-01")
				.param("hasta", "2026-05-31")
				.param("formato", "png"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.IMAGE_PNG))
				.andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=servicios.png"));
	}

	@Test
	void gastos_Exito() throws Exception {
		Gasto gasto = new Gasto();
		gasto.setFecha(LocalDate.of(2026, 5, 10));
		gasto.setCategoria("Insumos");
		gasto.setConcepto("Shampoo");
		gasto.setMonto(3000);
		when(reporteService.gastosSemana(LocalDate.of(2026, 5, 10))).thenReturn(List.of(gasto));

		mockMvc.perform(get("/api/v1/reportes/gastos").header("Authorization", AUTH).param("semana", "2026-05-10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].concepto").value("Shampoo"));
	}

	@Test
	void inasistencias_Exito() throws Exception {
		when(reporteService.inasistencias(any(), any(), eq(AUTH))).thenReturn(new ReporteInasistencias(3, 75, List.of()));

		mockMvc.perform(get("/api/v1/reportes/inasistencias")
				.header("Authorization", AUTH)
				.param("desde", "2026-05-01")
				.param("hasta", "2026-05-31"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.porcentaje").value(75));
	}

	@Test
	void demanda_Exito() throws Exception {
		when(reporteService.demanda(LocalDate.of(2026, 5, 1), AUTH))
				.thenReturn(List.of(new FranjaDemanda("Lunes", "09:00", 50, false)));

		mockMvc.perform(get("/api/v1/reportes/demanda").header("Authorization", AUTH).param("mes", "2026-05-01"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].dia").value("Lunes"));
	}
}
