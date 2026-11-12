package com.sakura.reporte.service;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExportadorServiceTest {

    private ExportadorService exportadorService;

    @BeforeEach
    void setUp() {
        exportadorService = new ExportadorService();
    }

    @Nested
    @DisplayName("Pruebas para generación de Excel")
    class ExcelTests {

        @Test
        @DisplayName("Debe generar un archivo Excel válido con encabezados y filas correctamente mapeadas")
        void excel_DebeGenerarBytesValidos() throws IOException {
            // Arrange
            String titulo = "ReporteServicios";
            List<String> columnas = List.of("Servicio", "Categoría", "Cantidad");
            List<List<String>> filas = List.of(
                    List.of("Corte de Cabello", "Peluquería", "15"),
                    List.of("Manicura Spa", "Estética", "8")
            );

            // Act
            byte[] resultado = exportadorService.excel(titulo, columnas, filas);

            // Assert
            assertThat(resultado).isNotEmpty();

            try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(resultado))) {
                Sheet sheet = workbook.getSheet(titulo);
                assertThat(sheet).isNotNull();

                // Validar fila de encabezados
                Row headerRow = sheet.getRow(0);
                assertThat(headerRow.getCell(0).getStringCellValue()).isEqualTo("Servicio");
                assertThat(headerRow.getCell(1).getStringCellValue()).isEqualTo("Categoría");
                assertThat(headerRow.getCell(2).getStringCellValue()).isEqualTo("Cantidad");

                // Validar primera fila de datos
                Row dataRow1 = sheet.getRow(1);
                assertThat(dataRow1.getCell(0).getStringCellValue()).isEqualTo("Corte de Cabello");
                assertThat(dataRow1.getCell(1).getStringCellValue()).isEqualTo("Peluquería");
                assertThat(dataRow1.getCell(2).getStringCellValue()).isEqualTo("15");

                // Validar segunda fila de datos
                Row dataRow2 = sheet.getRow(2);
                assertThat(dataRow2.getCell(0).getStringCellValue()).isEqualTo("Manicura Spa");
                assertThat(dataRow2.getCell(1).getStringCellValue()).isEqualTo("Estética");
                assertThat(dataRow2.getCell(2).getStringCellValue()).isEqualTo("8");
            }
        }

        @Test
        @DisplayName("Debe generar Excel vacío cuando las filas están vacías")
        void excel_ConFilasVacias_DebeGenerarSoloEncabezados() throws IOException {
            // Arrange
            String titulo = "GastoVacio";
            List<String> columnas = List.of("Fecha", "Categoría", "Monto");
            List<List<String>> filas = Collections.emptyList();

            // Act
            byte[] resultado = exportadorService.excel(titulo, columnas, filas);

            // Assert
            assertThat(resultado).isNotEmpty();

            try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(resultado))) {
                Sheet sheet = workbook.getSheet(titulo);
                assertThat(sheet).isNotNull();
                assertThat(sheet.getLastRowNum()).isEqualTo(0); // Solo contiene el header
            }
        }

        @Test
        @DisplayName("Debe lanzar IllegalStateException si el título de la hoja no es válido")
        void excel_TituloInvalido_LanzaExcepcion() {
            assertThatThrownBy(() -> exportadorService.excel("titulo:invalido", List.of("Columna"), List.of()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("No se pudo generar el Excel");
        }
    }

    @Nested
    @DisplayName("Pruebas para generación de PDF")
    class PdfTests {

        @Test
        @DisplayName("Debe generar un arreglo de bytes válido correspondiente a un PDF")
        void pdf_DebeGenerarDocumentoValido() {
            // Arrange
            String titulo = "Resumen del Día";
            List<String> lineas = List.of(
                    "Citas Agendadas: 20",
                    "Citas Atendidas: 18",
                    "Citas Canceladas: 2"
            );

            // Act
            byte[] resultado = exportadorService.pdf(titulo, lineas);

            // Assert
            assertThat(resultado).isNotEmpty();
            // Los archivos PDF válidos comienzan con la firma "%PDF-" (bytes: 0x25, 0x50, 0x44, 0x46, 0x2D)
            String cabeceraPdf = new String(resultado, 0, 5);
            assertThat(cabeceraPdf).isEqualTo("%PDF-");
        }

        @Test
        @DisplayName("Debe generar un PDF válido incluso sin líneas de contenido")
        void pdf_SinLineas_DebeGenerarPDFConTitulo() {
            // Arrange
            String titulo = "Reporte de Inasistencias";
            List<String> lineas = Collections.emptyList();

            // Act
            byte[] resultado = exportadorService.pdf(titulo, lineas);

            // Assert
            assertThat(resultado).isNotEmpty();
            String cabeceraPdf = new String(resultado, 0, 5);
            assertThat(cabeceraPdf).isEqualTo("%PDF-");
        }
    }

    @Nested
    @DisplayName("Pruebas para generación de imagen PNG")
    class PngTests {

        @Test
        @DisplayName("Debe generar una imagen PNG con las dimensiones especificadas (640x400)")
        void png_DebeGenerarImagenConDimensionesCorrectas() throws IOException {
            // Arrange
            String titulo = "Rendimiento Profesional";
            List<String> etiquetas = List.of("Ana", "Carlos", "Beatriz");
            List<Integer> valores = List.of(1500, 2300, 1800);

            // Act
            byte[] resultado = exportadorService.png(titulo, etiquetas, valores);

            // Assert
            assertThat(resultado).isNotEmpty();

            BufferedImage imagen = ImageIO.read(new ByteArrayInputStream(resultado));
            assertThat(imagen).isNotNull();
            assertThat(imagen.getWidth()).isEqualTo(640);
            assertThat(imagen.getHeight()).isEqualTo(400);
        }

        @Test
        @DisplayName("Debe manejar correctamente valores con 0 o listas de valores vacías sin lanzar divisiones por cero")
        void png_ConValoresCeroOVacios_DebeProcesarSinExcepcion() throws IOException {
            // Arrange
            String titulo = "Demanda Cero";
            List<String> etiquetas = List.of("Mañana", "Tarde");
            List<Integer> valores = List.of(0, 0);

            // Act
            byte[] resultado = exportadorService.png(titulo, etiquetas, valores);

            // Assert
            assertThat(resultado).isNotEmpty();
            BufferedImage imagen = ImageIO.read(new ByteArrayInputStream(resultado));
            assertThat(imagen).isNotNull();
        }

        @Test
        @DisplayName("Debe manejar desbalance entre cantidad de etiquetas y valores correctamente")
        void png_MasEtiquetasQueValores_DebeProcesarSinLanzarExcepcion() throws IOException {
            // Arrange
            String titulo = "Participación";
            List<String> etiquetas = List.of("Cat A", "Cat B", "Cat C");
            List<Integer> valores = List.of(50); // Menos valores que etiquetas

            // Act
            byte[] resultado = exportadorService.png(titulo, etiquetas, valores);

            // Assert
            assertThat(resultado).isNotEmpty();
            BufferedImage imagen = ImageIO.read(new ByteArrayInputStream(resultado));
            assertThat(imagen).isNotNull();
        }
    }
}