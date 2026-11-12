package com.sakura.reporte.service;

import com.sakura.reporte.client.AgendaCliente;
import com.sakura.reporte.client.PagoCliente;
import com.sakura.reporte.client.PagoDTO;
import com.sakura.reporte.client.PersonalCliente;
import com.sakura.reporte.client.ProfesionalDTO;
import com.sakura.reporte.client.ReservaDTO;
import com.sakura.reporte.model.FranjaDemanda;
import com.sakura.reporte.model.Gasto;
import com.sakura.reporte.model.IngresosDia;
import com.sakura.reporte.model.RendimientoProfesional;
import com.sakura.reporte.model.ReporteInasistencias;
import com.sakura.reporte.model.ReporteServicios;
import com.sakura.reporte.model.ResumenDia;
import com.sakura.reporte.repository.GastoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    @Mock
    private PersonalCliente personalCliente;

    @Mock
    private AgendaCliente agendaCliente;

    @Mock
    private PagoCliente pagoCliente;

    @Mock
    private GastoRepository gastoRepository;

    @InjectMocks
    private ReporteService reporteService;

    private final String AUTH_TOKEN = "Bearer token_xyz";
    private final LocalDate FECHA_PRUEBA = LocalDate.of(2026, 3, 15); // Domingo

    @Nested
    @DisplayName("Pruebas de Rendimiento de Profesionales")
    class RendimientoTests {

        @Test
        @DisplayName("Debe calcular correctamente el rendimiento y sumatoria de ingresos por profesional")
        void rendimiento_DebeCalcularIngresosYCitasCorrectamente() {
            // Arrange
            LocalDate desde = FECHA_PRUEBA.minusDays(7);
            LocalDate hasta = FECHA_PRUEBA;

            ProfesionalDTO p1 = crearProfesional(1L, "Ana");
            ProfesionalDTO p2 = crearProfesional(2L, "Carlos");

            ReservaDTO r1 = crearReserva(1L, "CONFIRMADA", 100);
            ReservaDTO r2 = crearReserva(1L, "PAGADA_COMPLETA", 150);
            ReservaDTO r3 = crearReserva(1L, "CANCELADA", 200); // Excluida por estado
            ReservaDTO r4 = crearReserva(2L, "CONFIRMADA", null); // Manejo de monto null

            when(personalCliente.todos(AUTH_TOKEN)).thenReturn(List.of(p1, p2));
            when(agendaCliente.rango(desde, hasta, AUTH_TOKEN)).thenReturn(List.of(r1, r2, r3, r4));

            // Act
            List<RendimientoProfesional> resultado = reporteService.rendimiento(desde, hasta, AUTH_TOKEN);

            // Assert
            assertThat(resultado).hasSize(2);

            RendimientoProfesional rendAna = resultado.stream().filter(r -> r.getProfesional().equals("Ana")).findFirst().orElseThrow();
            assertThat(rendAna.getCitasAtendidas()).isEqualTo(2);
            assertThat(rendAna.getIngresos()).isEqualTo(250);

            RendimientoProfesional rendCarlos = resultado.stream().filter(r -> r.getProfesional().equals("Carlos")).findFirst().orElseThrow();
            assertThat(rendCarlos.getCitasAtendidas()).isEqualTo(1);
            assertThat(rendCarlos.getIngresos()).isZero();
        }

        @Test
        @DisplayName("Debe repartir el monto pagado según el precio de cada prestación")
        void rendimiento_ReparteMontoEntrePrestaciones() {
            ProfesionalDTO ana = crearProfesional(1L, "Ana");
            ProfesionalDTO carla = crearProfesional(2L, "Carla");
            ReservaDTO cita = crearReserva(1L, "CONFIRMADA", 15000);
            com.sakura.reporte.client.PrestacionDTO lifting = new com.sakura.reporte.client.PrestacionDTO();
            lifting.setProfesionalId(1L);
            lifting.setPrecio(20000);
            com.sakura.reporte.client.PrestacionDTO unas = new com.sakura.reporte.client.PrestacionDTO();
            unas.setProfesionalId(2L);
            unas.setPrecio(10000);
            cita.setPrestaciones(List.of(lifting, unas));
            cita.setMontoTotal(30000);
            when(personalCliente.todos(AUTH_TOKEN)).thenReturn(List.of(ana, carla));
            when(agendaCliente.rango(any(), any(), eq(AUTH_TOKEN))).thenReturn(List.of(cita));

            List<RendimientoProfesional> resultado = reporteService.rendimiento(FECHA_PRUEBA, FECHA_PRUEBA, AUTH_TOKEN);

            RendimientoProfesional rendAna = resultado.stream().filter(r -> r.getProfesional().equals("Ana")).findFirst().orElseThrow();
            RendimientoProfesional rendCarla = resultado.stream().filter(r -> r.getProfesional().equals("Carla")).findFirst().orElseThrow();
            assertThat(rendAna.getCitasAtendidas()).isEqualTo(1);
            assertThat(rendAna.getIngresos()).isEqualTo(10000);
            assertThat(rendCarla.getCitasAtendidas()).isEqualTo(1);
            assertThat(rendCarla.getIngresos()).isEqualTo(5000);
        }
    }

    @Nested
    @DisplayName("Pruebas de Resumen del Día")
    class ResumenDiaTests {

        @Test
        @DisplayName("Debe contar citas atendidas y canceladas omitiendo las liberadas")
        void resumenDia_DebeFiltrarYContarCorrectamente() {
            // Arrange
            ReservaDTO confirmada = crearReserva(1L, "CONFIRMADA", 100);
            ReservaDTO pagada = crearReserva(1L, "PAGADA_COMPLETA", 120);
            ReservaDTO enProceso = crearReserva(1L, "EN_PROCESO", 90);
            ReservaDTO cancelada = crearReserva(1L, "CANCELADA", 0);
            ReservaDTO liberada = crearReserva(1L, "LIBERADA", 0); // Debe ignorarse totalmente

            when(agendaCliente.rango(FECHA_PRUEBA, FECHA_PRUEBA, AUTH_TOKEN))
                    .thenReturn(List.of(confirmada, pagada, enProceso, cancelada, liberada));

            // Act
            ResumenDia resumen = reporteService.resumenDia(FECHA_PRUEBA, AUTH_TOKEN);

            // Assert
            assertThat(resumen.getAgendadas()).isEqualTo(4); // Excluye la LIBERADA
            assertThat(resumen.getAtendidas()).isEqualTo(3);  // CONFIRMADA, PAGADA_COMPLETA, EN_PROCESO
            assertThat(resumen.getCanceladas()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Pruebas de Ingresos")
    class IngresosTests {

        @Test
        @DisplayName("Debe clasificar abonos y saldos acumulando el total general")
        void ingresos_DebeSumarAbonosYSaldos() {
            // Arrange
            PagoDTO p1 = crearPago("ABONO", 50);
            PagoDTO p2 = crearPago("ABONO", 30);
            PagoDTO p3 = crearPago("SALDO", 70);

            when(pagoCliente.periodo(FECHA_PRUEBA, FECHA_PRUEBA, AUTH_TOKEN))
                    .thenReturn(List.of(p1, p2, p3));

            // Act
            IngresosDia ingresos = reporteService.ingresos(FECHA_PRUEBA, AUTH_TOKEN);

            // Assert
            assertThat(ingresos.getAbonos()).isEqualTo(80);
            assertThat(ingresos.getSaldos()).isEqualTo(70);
            assertThat(ingresos.getTotal()).isEqualTo(150);
        }

        @Test
        @DisplayName("Debe ignorar pagos que no son abono ni saldo")
        void ingresos_TipoDesconocido_NoSuma() {
            when(pagoCliente.periodo(FECHA_PRUEBA, FECHA_PRUEBA, AUTH_TOKEN))
                    .thenReturn(List.of(crearPago("PROPINA", 40)));

            IngresosDia ingresos = reporteService.ingresos(FECHA_PRUEBA, AUTH_TOKEN);

            assertThat(ingresos.getAbonos()).isZero();
            assertThat(ingresos.getSaldos()).isZero();
            assertThat(ingresos.getTotal()).isZero();
        }
    }

    @Nested
    @DisplayName("Pruebas de Reporte de Servicios")
    class ServiciosTests {

        @Test
        @DisplayName("Debe calcular participación por categoría y ordenar servicios de mayor a menor")
        void servicios_DebeCalcularFilasYCategorias() {
            // Arrange
            LocalDate desde = FECHA_PRUEBA.minusDays(1);
            LocalDate hasta = FECHA_PRUEBA;

            ReservaDTO r1 = crearReservaDetallada("Corte", "Peluquería", "CONFIRMADA");
            ReservaDTO r2 = crearReservaDetallada("Corte", "Peluquería", "CONFIRMADA");
            ReservaDTO r3 = crearReservaDetallada("Manicura", "Estética", "PAGADA_COMPLETA");
            ReservaDTO r4 = crearReservaDetallada("Tinturado", "Peluquería", "EN_PROCESO");
            ReservaDTO cancelada = crearReservaDetallada("Depilación", "Estética", "CANCELADA");

            when(agendaCliente.rango(desde, hasta, AUTH_TOKEN))
                    .thenReturn(List.of(r1, r2, r3, r4, cancelada));

            // Act
            ReporteServicios reporte = reporteService.servicios(desde, hasta, AUTH_TOKEN);

            // Assert
            assertThat(reporte.getServicios()).hasSize(3);
            // El servicio más demandado debe ser Corte (2 citas)
            assertThat(reporte.getServicios().get(0).getServicio()).isEqualTo("Corte");
            assertThat(reporte.getServicios().get(0).getCantidad()).isEqualTo(2);

            // Participación: Peluquería 3/4 (75%), Estética 1/4 (25%)
            assertThat(reporte.getCategorias()).hasSize(2);
            assertThat(reporte.getCategorias())
                    .extracting("porcentaje")
                    .containsExactlyInAnyOrder(75, 25);
        }

        @Test
        @DisplayName("Debe retornar listas vacías cuando no hay citas atendidas")
        void servicios_SinCitas_RetornaVacio() {
            LocalDate desde = FECHA_PRUEBA.minusDays(1);
            when(agendaCliente.rango(desde, FECHA_PRUEBA, AUTH_TOKEN)).thenReturn(Collections.emptyList());

            ReporteServicios reporte = reporteService.servicios(desde, FECHA_PRUEBA, AUTH_TOKEN);

            assertThat(reporte.getServicios()).isEmpty();
            assertThat(reporte.getCategorias()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Pruebas de Gastos de la Semana")
    class GastosSemanaTests {

        @Test
        @DisplayName("Debe calcular el inicio del lunes de esa semana y consultar el rango de 7 días")
        void gastosSemana_DebeCalcularRangoLunesADomingo() {
            // Arrange
            // Supongamos FECHA_PRUEBA = Domingo 15-Mar-2026.
            // El lunes de esa semana es 09-Mar-2026 y el domingo es 15-Mar-2026.
            LocalDate lunesEsperado = LocalDate.of(2026, 3, 9);
            LocalDate domingoEsperado = LocalDate.of(2026, 3, 15);

            when(gastoRepository.findByFechaBetweenOrderByFechaAsc(lunesEsperado, domingoEsperado))
                    .thenReturn(Collections.emptyList());

            // Act
            List<Gasto> resultado = reporteService.gastosSemana(FECHA_PRUEBA);

            // Assert
            assertThat(resultado).isNotNull();
            verify(gastoRepository).findByFechaBetweenOrderByFechaAsc(lunesEsperado, domingoEsperado);
        }
    }

    @Nested
    @DisplayName("Pruebas de Reporte de Inasistencias")
    class InasistenciasTests {

        @Test
        @DisplayName("Debe filtrar clientes recurrentes con al menos 2 inasistencias o cancelaciones")
        void inasistencias_DebeFiltrarClientesRecurrentes() {
            // Arrange
            LocalDate desde = FECHA_PRUEBA.minusDays(10);
            LocalDate hasta = FECHA_PRUEBA;

            ReservaDTO r1 = crearReservaCliente("Juan", "CANCELADA");
            ReservaDTO r2 = crearReservaCliente("Juan", "INASISTENCIA");
            ReservaDTO r3 = crearReservaCliente("María", "CANCELADA"); // Solo 1 inasistencia
            ReservaDTO r4 = crearReservaCliente("Pedro", "CONFIRMADA");

            when(agendaCliente.rango(desde, hasta, AUTH_TOKEN))
                    .thenReturn(List.of(r1, r2, r3, r4));

            // Act
            ReporteInasistencias reporte = reporteService.inasistencias(desde, hasta, AUTH_TOKEN);

            // Assert
            assertThat(reporte.getNoConcretadas()).isEqualTo(3);
            assertThat(reporte.getPorcentaje()).isEqualTo(75); // 3 perdidas de 4 totales

            assertThat(reporte.getClientes()).hasSize(1);
            assertThat(reporte.getClientes().get(0).getCliente()).isEqualTo("Juan");
            assertThat(reporte.getClientes().get(0).getCantidad()).isEqualTo(2);
        }

        @Test
        @DisplayName("Debe reportar cero cuando no hay citas o solo hay liberadas")
        void inasistencias_SinCitasOSoloLiberadas_PorcentajeCero() {
            LocalDate desde = FECHA_PRUEBA.minusDays(1);
            when(agendaCliente.rango(desde, FECHA_PRUEBA, AUTH_TOKEN)).thenReturn(Collections.emptyList());

            ReporteInasistencias vacio = reporteService.inasistencias(desde, FECHA_PRUEBA, AUTH_TOKEN);

            assertThat(vacio.getNoConcretadas()).isZero();
            assertThat(vacio.getPorcentaje()).isZero();
            assertThat(vacio.getClientes()).isEmpty();

            when(agendaCliente.rango(desde, FECHA_PRUEBA, AUTH_TOKEN))
                    .thenReturn(List.of(crearReservaCliente("Ana", "LIBERADA")));

            ReporteInasistencias soloLiberadas = reporteService.inasistencias(desde, FECHA_PRUEBA, AUTH_TOKEN);

            assertThat(soloLiberadas.getNoConcretadas()).isZero();
            assertThat(soloLiberadas.getPorcentaje()).isZero();
        }
    }

    @Nested
    @DisplayName("Pruebas de Matriz de Demanda")
    class DemandaTests {

        @Test
        @DisplayName("Debe mapear la matriz de 7 días x 10 horas calculando el % de ocupación según profesionales")
        void demanda_DebeGenerar70FranjasHorarias() {
            // Arrange
            LocalDate mes = LocalDate.of(2026, 3, 1);
            LocalDate primerDia = LocalDate.of(2026, 3, 1);
            LocalDate ultimoDia = LocalDate.of(2026, 3, 31);

            // 2 profesionales activos -> Capacidad máxima por hora = 2
            when(personalCliente.activos()).thenReturn(List.of(
                    crearProfesional(1L, "Pro 1"),
                    crearProfesional(2L, "Pro 2")
            ));

            // Cita el Lunes 2 de Marzo de 2026 a las 09:00
            ReservaDTO cita = new ReservaDTO();
            cita.setEstado("CONFIRMADA");
            cita.setFecha(LocalDate.of(2026, 3, 2)); // Lunes
            cita.setHoraInicio(LocalTime.of(9, 0));

            when(agendaCliente.rango(primerDia, ultimoDia, AUTH_TOKEN))
                    .thenReturn(List.of(cita));

            // Act
            List<FranjaDemanda> franjas = reporteService.demanda(mes, AUTH_TOKEN);

            // Assert
            // 7 días * 10 horas (9:00 a 18:00 inclusive) = 70 franjas
            assertThat(franjas).hasSize(70);

            // Buscar la franja correspondiente a Lunes 09:00
            FranjaDemanda franjaLunes9 = franjas.stream()
                    .filter(f -> f.getDia().equals("Lunes") && f.getHora().equals("09:00"))
                    .findFirst()
                    .orElseThrow();

            // 1 cita / 2 profesionales = 50% ocupación
            assertThat(franjaLunes9.getOcupacion()).isEqualTo(50);
            assertThat(franjaLunes9.isBaja()).isFalse(); // 50% >= 20%

            FranjaDemanda franjaVacia = franjas.stream()
                    .filter(f -> f.getDia().equals("Martes") && f.getHora().equals("10:00"))
                    .findFirst()
                    .orElseThrow();
            assertThat(franjaVacia.getOcupacion()).isZero();
            assertThat(franjaVacia.isBaja()).isTrue();
        }

        @Test
        @DisplayName("Debe usar al menos un profesional y limitar la ocupación al 100%")
        void demanda_SinProfesionales_LimitaOcupacionAl100() {
            LocalDate mes = LocalDate.of(2026, 3, 1);
            when(personalCliente.activos()).thenReturn(Collections.emptyList());

            ReservaDTO cita1 = new ReservaDTO();
            cita1.setEstado("CONFIRMADA");
            cita1.setFecha(LocalDate.of(2026, 3, 2));
            cita1.setHoraInicio(LocalTime.of(9, 0));
            ReservaDTO cita2 = new ReservaDTO();
            cita2.setEstado("PAGADA_COMPLETA");
            cita2.setFecha(LocalDate.of(2026, 3, 2));
            cita2.setHoraInicio(LocalTime.of(9, 0));
            when(agendaCliente.rango(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31), AUTH_TOKEN))
                    .thenReturn(List.of(cita1, cita2));

            List<FranjaDemanda> franjas = reporteService.demanda(mes, AUTH_TOKEN);

            FranjaDemanda lunes9 = franjas.stream()
                    .filter(f -> f.getDia().equals("Lunes") && f.getHora().equals("09:00"))
                    .findFirst()
                    .orElseThrow();
            assertThat(lunes9.getOcupacion()).isEqualTo(100);
            assertThat(lunes9.isBaja()).isFalse();
        }
    }

    // Helper methods para construir los DTOs de prueba
    private ProfesionalDTO crearProfesional(Long id, String nombre) {
        ProfesionalDTO dto = new ProfesionalDTO();
        dto.setId(id);
        dto.setNombre(nombre);
        return dto;
    }

    private ReservaDTO crearReserva(Long profesionalId, String estado, Integer monto) {
        ReservaDTO dto = new ReservaDTO();
        dto.setProfesionalId(profesionalId);
        dto.setEstado(estado);
        dto.setMontoPagado(monto);
        return dto;
    }

    private ReservaDTO crearReservaDetallada(String servicio, String categoria, String estado) {
        ReservaDTO dto = new ReservaDTO();
        dto.setNombreServicio(servicio);
        dto.getNombreCategoria(); // opcional
        dto.setNombreCategoria(categoria);
        dto.setEstado(estado);
        return dto;
    }

    private ReservaDTO crearReservaCliente(String cliente, String estado) {
        ReservaDTO dto = new ReservaDTO();
        dto.setNombreCliente(cliente);
        dto.setEstado(estado);
        return dto;
    }

    private PagoDTO crearPago(String tipo, int monto) {
        PagoDTO dto = new PagoDTO();
        dto.setTipo(tipo);
        dto.setMonto(monto);
        return dto;
    }
}