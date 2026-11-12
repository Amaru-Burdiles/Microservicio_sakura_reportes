package com.sakura.reporte.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sakura.reporte.client.SesionGuard;
import com.sakura.reporte.exception.AccesoDenegadoException;
import com.sakura.reporte.model.Gasto;
import com.sakura.reporte.repository.GastoRepository;

@WebMvcTest(GastoController.class)
@ActiveProfiles("test")
class GastoControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private GastoRepository gastoRepository;

	@MockitoBean
	private SesionGuard sesionGuard;

	private static final String AUTH = "Bearer token-admin";

	@Test
	void listar_Exito() throws Exception {
		when(gastoRepository.findAll()).thenReturn(List.of(gasto(1L)));

		mockMvc.perform(get("/api/v1/gastos").header("Authorization", AUTH))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].concepto").value("Shampoo"));
	}

	@Test
	void listar_SinPermiso_Responde403() throws Exception {
		doThrow(new AccesoDenegadoException("No tiene permiso")).when(sesionGuard).exigirAdmin(any());

		mockMvc.perform(get("/api/v1/gastos").header("Authorization", AUTH))
				.andExpect(status().isForbidden());
	}

	@Test
	void crear_Exito() throws Exception {
		when(gastoRepository.save(any(Gasto.class))).thenReturn(gasto(2L));

		mockMvc.perform(post("/api/v1/gastos")
				.header("Authorization", AUTH)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"fecha\":\"2026-05-10\",\"categoria\":\"Insumos\",\"concepto\":\"Shampoo\",\"monto\":3000}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.monto").value(3000));
	}

	@Test
	void crear_CuerpoVacio_Responde400() throws Exception {
		mockMvc.perform(post("/api/v1/gastos")
				.header("Authorization", AUTH)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void eliminar_Exito() throws Exception {
		doNothing().when(gastoRepository).deleteById(2L);

		mockMvc.perform(delete("/api/v1/gastos/2").header("Authorization", AUTH))
				.andExpect(status().isNoContent());
	}

	private Gasto gasto(Long id) {
		Gasto gasto = new Gasto();
		gasto.setId(id);
		gasto.setFecha(LocalDate.of(2026, 5, 10));
		gasto.setCategoria("Insumos");
		gasto.setConcepto("Shampoo");
		gasto.setMonto(3000);
		return gasto;
	}
}
