package com.sakura.reporte.service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.lowagie.text.Document;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

@Service
public class ExportadorService {

	public byte[] excel(String titulo, List<String> columnas, List<List<String>> filas) {
		try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
			XSSFSheet hoja = libro.createSheet(titulo);
			Row encabezado = hoja.createRow(0);
			for (int i = 0; i < columnas.size(); i++) {
				encabezado.createCell(i).setCellValue(columnas.get(i));
			}
			for (int f = 0; f < filas.size(); f++) {
				Row row = hoja.createRow(f + 1);
				List<String> datos = filas.get(f);
				for (int c = 0; c < datos.size(); c++) {
					row.createCell(c).setCellValue(datos.get(c));
				}
			}
			libro.write(salida);
			return salida.toByteArray();
		} catch (Exception ex) {
			throw new IllegalStateException("No se pudo generar el Excel");
		}
	}

	public byte[] pdf(String titulo, List<String> lineas) {
		try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
			Document documento = new Document();
			PdfWriter.getInstance(documento, salida);
			documento.open();
			documento.add(new Paragraph(titulo, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
			for (String linea : lineas) {
				documento.add(new Paragraph(linea, FontFactory.getFont(FontFactory.HELVETICA, 11)));
			}
			documento.close();
			return salida.toByteArray();
		} catch (Exception ex) {
			throw new IllegalStateException("No se pudo generar el PDF");
		}
	}

	public byte[] png(String titulo, List<String> etiquetas, List<Integer> valores) {
		int ancho = 640;
		int alto = 400;
		BufferedImage imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = imagen.createGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, ancho, alto);
		g.setColor(Color.DARK_GRAY);
		g.drawString(titulo, 20, 30);
		int maximo = valores.stream().mapToInt(Integer::intValue).max().orElse(1);
		if (maximo == 0) {
			maximo = 1;
		}
		int cantidad = Math.max(1, etiquetas.size());
		int barra = Math.max(10, (ancho - 40) / cantidad - 10);
		for (int i = 0; i < etiquetas.size(); i++) {
			int valor = i < valores.size() ? valores.get(i) : 0;
			int altura = valor * 280 / maximo;
			int x = 20 + i * (barra + 10);
			g.setColor(new Color(180, 60, 90));
			g.fillRect(x, 340 - altura, barra, altura);
			g.setColor(Color.DARK_GRAY);
			g.drawString(etiquetas.get(i), x, 360);
		}
		g.dispose();
		try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
			ImageIO.write(imagen, "png", salida);
			return salida.toByteArray();
		} catch (Exception ex) {
			throw new IllegalStateException("No se pudo generar el gráfico");
		}
	}

}
