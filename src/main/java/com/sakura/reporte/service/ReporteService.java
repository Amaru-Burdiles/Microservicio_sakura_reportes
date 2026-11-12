package com.sakura.reporte.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sakura.reporte.client.AgendaCliente;
import com.sakura.reporte.client.PagoCliente;
import com.sakura.reporte.client.PagoDTO;
import com.sakura.reporte.client.PersonalCliente;
import com.sakura.reporte.client.ProfesionalDTO;
import com.sakura.reporte.client.ReservaDTO;
import com.sakura.reporte.model.ClienteAusente;
import com.sakura.reporte.model.FilaServicio;
import com.sakura.reporte.model.FranjaDemanda;
import com.sakura.reporte.model.Gasto;
import com.sakura.reporte.model.IngresosDia;
import com.sakura.reporte.model.ParticipacionCategoria;
import com.sakura.reporte.model.RendimientoProfesional;
import com.sakura.reporte.model.ReporteInasistencias;
import com.sakura.reporte.model.ReporteServicios;
import com.sakura.reporte.model.ResumenDia;
import com.sakura.reporte.repository.GastoRepository;

@Service
public class ReporteService {

	private static final String CONFIRMADA = "CONFIRMADA";
	private static final String PAGADA = "PAGADA_COMPLETA";
	private static final String EN_PROCESO = "EN_PROCESO";
	private static final String LIBERADA = "LIBERADA";
	private static final String CANCELADA = "CANCELADA";
	private static final String INASISTENCIA = "INASISTENCIA";

	@Autowired
	private PersonalCliente personalCliente;

	@Autowired
	private AgendaCliente agendaCliente;

	@Autowired
	private PagoCliente pagoCliente;

	@Autowired
	private GastoRepository gastoRepository;

	public List<RendimientoProfesional> rendimiento(LocalDate desde, LocalDate hasta, String authorization) {
		List<ProfesionalDTO> profesionales = personalCliente.todos(authorization);
		List<ReservaDTO> citas = agendaCliente.rango(desde, hasta, authorization).stream()
				.filter(reserva -> CONFIRMADA.equals(reserva.getEstado()) || PAGADA.equals(reserva.getEstado()))
				.toList();
		List<RendimientoProfesional> filas = new ArrayList<>();
		for (ProfesionalDTO profesional : profesionales) {
			int citasAtendidas = 0;
			int ingresos = 0;
			for (ReservaDTO reserva : citas) {
				for (Aporte aporte : aportes(reserva)) {
					if (profesional.getId().equals(aporte.profesionalId())) {
						citasAtendidas++;
						ingresos += aporte.monto();
					}
				}
			}
			filas.add(new RendimientoProfesional(profesional.getNombre(), citasAtendidas, ingresos));
		}
		return filas;
	}

	private List<Aporte> aportes(ReservaDTO reserva) {
		if (reserva.getPrestaciones() == null || reserva.getPrestaciones().isEmpty()) {
			int monto = reserva.getMontoPagado() == null ? 0 : reserva.getMontoPagado();
			return List.of(new Aporte(reserva.getProfesionalId(), monto));
		}
		int total = reserva.getPrestaciones().stream().mapToInt(prestacion -> prestacion.getPrecio() == null ? 0 : prestacion.getPrecio())
				.sum();
		int pagado = reserva.getMontoPagado() == null ? 0 : reserva.getMontoPagado();
		List<Aporte> aportes = new ArrayList<>();
		int acumulado = 0;
		List<com.sakura.reporte.client.PrestacionDTO> lineas = reserva.getPrestaciones();
		for (int i = 0; i < lineas.size(); i++) {
			int precio = lineas.get(i).getPrecio() == null ? 0 : lineas.get(i).getPrecio();
			int parte = i == lineas.size() - 1 || total == 0 ? pagado - acumulado : pagado * precio / total;
			acumulado += parte;
			aportes.add(new Aporte(lineas.get(i).getProfesionalId(), parte));
		}
		return aportes;
	}

	private record Aporte(Long profesionalId, int monto) {
	}

	public ResumenDia resumenDia(LocalDate fecha, String authorization) {
		List<ReservaDTO> citas = agendaCliente.rango(fecha, fecha, authorization).stream()
				.filter(reserva -> !LIBERADA.equals(reserva.getEstado()))
				.toList();
		int atendidas = (int) citas.stream().filter(this::atendida).count();
		int canceladas = (int) citas.stream().filter(reserva -> CANCELADA.equals(reserva.getEstado())).count();
		return new ResumenDia(citas.size(), atendidas, canceladas);
	}

	public IngresosDia ingresos(LocalDate fecha, String authorization) {
		List<PagoDTO> pagos = pagoCliente.periodo(fecha, fecha, authorization);
		int abonos = pagos.stream().filter(pago -> "ABONO".equals(pago.getTipo())).mapToInt(PagoDTO::getMonto).sum();
		int saldos = pagos.stream().filter(pago -> "SALDO".equals(pago.getTipo())).mapToInt(PagoDTO::getMonto).sum();
		return new IngresosDia(abonos, saldos, abonos + saldos);
	}

	public ReporteServicios servicios(LocalDate desde, LocalDate hasta, String authorization) {
		List<ReservaDTO> citas = atendidasEntre(desde, hasta, authorization);
		Map<String, Integer> porServicio = new HashMap<>();
		Map<String, String> categoriaDe = new HashMap<>();
		Map<String, Integer> porCategoria = new HashMap<>();
		for (ReservaDTO reserva : citas) {
			String nombre = reserva.getNombreServicio();
			String categoria = reserva.getNombreCategoria();
			porServicio.merge(nombre, 1, Integer::sum);
			categoriaDe.put(nombre, categoria);
			porCategoria.merge(categoria, 1, Integer::sum);
		}
		List<FilaServicio> filas = porServicio.entrySet().stream()
				.map(entry -> new FilaServicio(entry.getKey(), categoriaDe.get(entry.getKey()), entry.getValue()))
				.sorted(Comparator.comparing(FilaServicio::getCantidad).reversed())
				.toList();
		int total = citas.size();
		List<ParticipacionCategoria> categorias = porCategoria.entrySet().stream()
				.map(entry -> new ParticipacionCategoria(entry.getKey(), total == 0 ? 0 : entry.getValue() * 100 / total))
				.toList();
		return new ReporteServicios(filas, categorias);
	}

	public List<Gasto> gastosSemana(LocalDate dia) {
		LocalDate desde = dia.minusDays(dia.getDayOfWeek().getValue() - 1L);
		return gastoRepository.findByFechaBetweenOrderByFechaAsc(desde, desde.plusDays(6));
	}

	public ReporteInasistencias inasistencias(LocalDate desde, LocalDate hasta, String authorization) {
		List<ReservaDTO> citas = agendaCliente.rango(desde, hasta, authorization).stream()
				.filter(reserva -> !LIBERADA.equals(reserva.getEstado()))
				.toList();
		List<ReservaDTO> perdidas = citas.stream()
				.filter(reserva -> CANCELADA.equals(reserva.getEstado()) || INASISTENCIA.equals(reserva.getEstado()))
				.toList();
		int porcentaje = citas.isEmpty() ? 0 : perdidas.size() * 100 / citas.size();
		Map<String, Integer> porCliente = new HashMap<>();
		for (ReservaDTO reserva : perdidas) {
			porCliente.merge(reserva.getNombreCliente(), 1, Integer::sum);
		}
		List<ClienteAusente> clientes = porCliente.entrySet().stream()
				.filter(entry -> entry.getValue() >= 2)
				.map(entry -> new ClienteAusente(entry.getKey(), entry.getValue()))
				.toList();
		return new ReporteInasistencias(perdidas.size(), porcentaje, clientes);
	}

	public List<FranjaDemanda> demanda(LocalDate mes, String authorization) {
		LocalDate desde = mes.withDayOfMonth(1);
		LocalDate hasta = mes.withDayOfMonth(mes.lengthOfMonth());
		List<ReservaDTO> citas = atendidasEntre(desde, hasta, authorization);
		int profesionales = Math.max(1, personalCliente.activos().size());
		String[] dias = { "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo" };
		List<FranjaDemanda> filas = new ArrayList<>();
		for (int dia = 1; dia <= 7; dia++) {
			for (int hora = 9; hora <= 18; hora++) {
				int finalDia = dia;
				int finalHora = hora;
				long cantidad = citas.stream()
						.filter(reserva -> reserva.getFecha().getDayOfWeek().getValue() == finalDia
								&& reserva.getHoraInicio().getHour() == finalHora)
						.count();
				int ocupacion = (int) Math.min(100, cantidad * 100 / profesionales);
				filas.add(new FranjaDemanda(dias[dia - 1], String.format("%02d:00", hora), ocupacion, ocupacion < 20));
			}
		}
		return filas;
	}

	private List<ReservaDTO> atendidasEntre(LocalDate desde, LocalDate hasta, String authorization) {
		return agendaCliente.rango(desde, hasta, authorization).stream()
				.filter(reserva -> CONFIRMADA.equals(reserva.getEstado()) || PAGADA.equals(reserva.getEstado())
						|| EN_PROCESO.equals(reserva.getEstado()))
				.toList();
	}

	private boolean atendida(ReservaDTO reserva) {
		return CONFIRMADA.equals(reserva.getEstado()) || PAGADA.equals(reserva.getEstado())
				|| EN_PROCESO.equals(reserva.getEstado());
	}

}
