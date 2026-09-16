package co.edu.uan.gestionhardware.service;

import co.edu.uan.gestionhardware.dto.ReporteEquipo;
import co.edu.uan.gestionhardware.dto.ReportePeriodo;
import co.edu.uan.gestionhardware.model.Equipo;
import co.edu.uan.gestionhardware.model.Incidencia;
import co.edu.uan.gestionhardware.model.Mantenimiento;
import co.edu.uan.gestionhardware.repository.EquipoRepository;
import co.edu.uan.gestionhardware.repository.IncidenciaRepository;
import co.edu.uan.gestionhardware.repository.MantenimientoRepository;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Genera los reportes periodicos de RF-19: fallas, indisponibilidad,
 * mantenimientos y clasificacion final de activos de un rango de fechas
 * (mensual o anual), exportables en PDF (OpenPDF) o Excel (Apache POI).
 */
@Service
@Transactional(readOnly = true)
public class ReporteService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Color AZUL_MARCA = new Color(37, 99, 235);

    private final EquipoRepository equipoRepository;
    private final IncidenciaRepository incidenciaRepository;
    private final MantenimientoRepository mantenimientoRepository;

    public ReporteService(EquipoRepository equipoRepository,
                          IncidenciaRepository incidenciaRepository,
                          MantenimientoRepository mantenimientoRepository) {
        this.equipoRepository = equipoRepository;
        this.incidenciaRepository = incidenciaRepository;
        this.mantenimientoRepository = mantenimientoRepository;
    }

    /**
     * Arma el reporte de un periodo: fallas y horas de indisponibilidad
     * reportadas en el rango, mantenimientos ejecutados en el rango, y la
     * clasificacion (estado) actual de cada equipo activo.
     */
    public ReportePeriodo generar(LocalDate desde, LocalDate hasta, String etiquetaPeriodo) {

        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime fin = hasta.atTime(LocalTime.MAX);

        List<Incidencia> incidencias = incidenciaRepository.findPorPeriodo(inicio, fin);
        List<Mantenimiento> mantenimientos = mantenimientoRepository.findEjecutadosPorPeriodo(desde, hasta);
        List<Equipo> equipos = equipoRepository.findByActivoTrue();

        Map<Long, Long> fallasPorEquipo = new HashMap<>();
        Map<Long, BigDecimal> horasPorEquipo = new HashMap<>();
        BigDecimal totalHoras = BigDecimal.ZERO;

        for (Incidencia i : incidencias) {
            Long equipoId = i.getEquipo().getId();
            fallasPorEquipo.merge(equipoId, 1L, Long::sum);
            BigDecimal horas = i.getHorasIndisponibilidad() != null ? i.getHorasIndisponibilidad() : BigDecimal.ZERO;
            horasPorEquipo.merge(equipoId, horas, BigDecimal::add);
            totalHoras = totalHoras.add(horas);
        }

        Map<Long, Long> mantenimientosPorEquipo = new HashMap<>();
        for (Mantenimiento m : mantenimientos) {
            mantenimientosPorEquipo.merge(m.getEquipo().getId(), 1L, Long::sum);
        }

        Map<String, Long> equiposPorEstado = new LinkedHashMap<>();
        List<ReporteEquipo> filas = new ArrayList<>();

        for (Equipo equipo : equipos) {
            long fallas = fallasPorEquipo.getOrDefault(equipo.getId(), 0L);
            BigDecimal horas = horasPorEquipo.getOrDefault(equipo.getId(), BigDecimal.ZERO);
            long mant = mantenimientosPorEquipo.getOrDefault(equipo.getId(), 0L);
            String estado = equipo.getEstadoEquipo().getNombre();

            filas.add(new ReporteEquipo(equipo.getCodigoInterno(), equipo.getArea().getNombre(),
                    fallas, horas, mant, estado));

            equiposPorEstado.merge(estado, 1L, Long::sum);
        }

        return new ReportePeriodo(etiquetaPeriodo, desde, hasta,
                incidencias.size(), totalHoras, mantenimientos.size(),
                equiposPorEstado, filas);
    }

    public byte[] generarPdf(ReportePeriodo reporte) {

        Document documento = new Document(PageSize.A4, 36, 36, 54, 54);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(documento, salida);
            documento.open();

            Font tituloFont = new Font(Font.HELVETICA, 18, Font.BOLD);
            Font subtituloFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.GRAY);
            Font seccionFont = new Font(Font.HELVETICA, 13, Font.BOLD);
            Font celdaFont = new Font(Font.HELVETICA, 9);
            Font valorGrandeFont = new Font(Font.HELVETICA, 16, Font.BOLD);
            Font encabezadoFont = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);

            documento.add(new Paragraph("HardTrack - Reporte " + reporte.getEtiquetaPeriodo(), tituloFont));

            Paragraph subtitulo = new Paragraph(
                    "Periodo: " + reporte.getDesde().format(FORMATO_FECHA) + " - "
                            + reporte.getHasta().format(FORMATO_FECHA)
                            + "   ·   Generado: " + LocalDate.now().format(FORMATO_FECHA),
                    subtituloFont);
            subtitulo.setSpacingAfter(16);
            documento.add(subtitulo);

            PdfPTable resumen = new PdfPTable(3);
            resumen.setWidthPercentage(100);
            resumen.setSpacingAfter(20);
            agregarCeldaResumen(resumen, "Fallas reportadas", String.valueOf(reporte.getTotalFallas()),
                    valorGrandeFont, celdaFont);
            agregarCeldaResumen(resumen, "Horas de indisponibilidad",
                    formatearDecimal(reporte.getTotalHorasIndisponibilidad()), valorGrandeFont, celdaFont);
            agregarCeldaResumen(resumen, "Mantenimientos ejecutados", String.valueOf(reporte.getTotalMantenimientos()),
                    valorGrandeFont, celdaFont);
            documento.add(resumen);

            documento.add(new Paragraph("Clasificación final de activos", seccionFont));
            PdfPTable tablaEstados = new PdfPTable(2);
            tablaEstados.setWidthPercentage(60);
            tablaEstados.setSpacingBefore(8);
            tablaEstados.setSpacingAfter(20);
            agregarEncabezado(tablaEstados, encabezadoFont, "Estado", "Cantidad");
            for (Map.Entry<String, Long> entrada : reporte.getEquiposPorEstado().entrySet()) {
                tablaEstados.addCell(new PdfPCell(new Phrase(entrada.getKey(), celdaFont)));
                tablaEstados.addCell(new PdfPCell(new Phrase(String.valueOf(entrada.getValue()), celdaFont)));
            }
            documento.add(tablaEstados);

            documento.add(new Paragraph("Detalle por equipo", seccionFont));
            PdfPTable tablaEquipos = new PdfPTable(6);
            tablaEquipos.setWidthPercentage(100);
            tablaEquipos.setSpacingBefore(8);
            tablaEquipos.setWidths(new float[]{2, 2, 1.3f, 1.6f, 1.6f, 2});
            agregarEncabezado(tablaEquipos, encabezadoFont, "Equipo", "Área", "Fallas", "Horas indisp.", "Mantenim.", "Estado");

            for (ReporteEquipo fila : reporte.getEquipos()) {
                tablaEquipos.addCell(new PdfPCell(new Phrase(fila.getCodigoInterno(), celdaFont)));
                tablaEquipos.addCell(new PdfPCell(new Phrase(fila.getArea(), celdaFont)));
                tablaEquipos.addCell(new PdfPCell(new Phrase(String.valueOf(fila.getFallas()), celdaFont)));
                tablaEquipos.addCell(new PdfPCell(new Phrase(formatearDecimal(fila.getHorasIndisponibilidad()), celdaFont)));
                tablaEquipos.addCell(new PdfPCell(new Phrase(String.valueOf(fila.getMantenimientos()), celdaFont)));
                tablaEquipos.addCell(new PdfPCell(new Phrase(fila.getEstadoActual(), celdaFont)));
            }
            documento.add(tablaEquipos);

            documento.close();

        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo generar el PDF del reporte", e);
        }

        return salida.toByteArray();
    }

    public byte[] generarExcel(ReportePeriodo reporte) {

        try (XSSFWorkbook libro = new XSSFWorkbook()) {

            CellStyle estiloEncabezado = libro.createCellStyle();
            org.apache.poi.ss.usermodel.Font fuenteEncabezado = libro.createFont();
            fuenteEncabezado.setBold(true);
            fuenteEncabezado.setColor(IndexedColors.WHITE.getIndex());
            estiloEncabezado.setFont(fuenteEncabezado);
            estiloEncabezado.setFillForegroundColor(IndexedColors.BLUE.getIndex());
            estiloEncabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Sheet hojaResumen = libro.createSheet("Resumen");
            int fila = 0;

            hojaResumen.createRow(fila++).createCell(0)
                    .setCellValue("HardTrack - Reporte " + reporte.getEtiquetaPeriodo());
            hojaResumen.createRow(fila++).createCell(0)
                    .setCellValue("Periodo: " + reporte.getDesde().format(FORMATO_FECHA)
                            + " - " + reporte.getHasta().format(FORMATO_FECHA));
            fila++;

            Row filaResumen1 = hojaResumen.createRow(fila++);
            filaResumen1.createCell(0).setCellValue("Fallas reportadas");
            filaResumen1.createCell(1).setCellValue(reporte.getTotalFallas());

            Row filaResumen2 = hojaResumen.createRow(fila++);
            filaResumen2.createCell(0).setCellValue("Horas de indisponibilidad");
            filaResumen2.createCell(1).setCellValue(reporte.getTotalHorasIndisponibilidad().doubleValue());

            Row filaResumen3 = hojaResumen.createRow(fila++);
            filaResumen3.createCell(0).setCellValue("Mantenimientos ejecutados");
            filaResumen3.createCell(1).setCellValue(reporte.getTotalMantenimientos());

            fila++;
            Row encabezadoEstados = hojaResumen.createRow(fila++);
            crearCeldaEncabezado(encabezadoEstados, 0, "Estado", estiloEncabezado);
            crearCeldaEncabezado(encabezadoEstados, 1, "Cantidad", estiloEncabezado);
            for (Map.Entry<String, Long> entrada : reporte.getEquiposPorEstado().entrySet()) {
                Row filaEstado = hojaResumen.createRow(fila++);
                filaEstado.createCell(0).setCellValue(entrada.getKey());
                filaEstado.createCell(1).setCellValue(entrada.getValue());
            }

            hojaResumen.autoSizeColumn(0);
            hojaResumen.autoSizeColumn(1);

            Sheet hojaDetalle = libro.createSheet("Detalle por equipo");
            String[] columnas = {"Equipo", "Área", "Fallas", "Horas indisponibilidad", "Mantenimientos", "Estado"};
            Row encabezadoDetalle = hojaDetalle.createRow(0);
            for (int c = 0; c < columnas.length; c++) {
                crearCeldaEncabezado(encabezadoDetalle, c, columnas[c], estiloEncabezado);
            }

            int filaDetalle = 1;
            for (ReporteEquipo item : reporte.getEquipos()) {
                Row r = hojaDetalle.createRow(filaDetalle++);
                r.createCell(0).setCellValue(item.getCodigoInterno());
                r.createCell(1).setCellValue(item.getArea());
                r.createCell(2).setCellValue(item.getFallas());
                r.createCell(3).setCellValue(item.getHorasIndisponibilidad().doubleValue());
                r.createCell(4).setCellValue(item.getMantenimientos());
                r.createCell(5).setCellValue(item.getEstadoActual());
            }

            for (int c = 0; c < columnas.length; c++) {
                hojaDetalle.autoSizeColumn(c);
            }

            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            libro.write(salida);
            return salida.toByteArray();

        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar el Excel del reporte", e);
        }
    }

    private String formatearDecimal(BigDecimal valor) {
        return valor.setScale(1, RoundingMode.HALF_UP).toString();
    }

    private void agregarCeldaResumen(PdfPTable tabla, String etiqueta, String valor, Font valorFont, Font etiquetaFont) {
        PdfPCell celda = new PdfPCell();
        celda.setPadding(10);
        celda.addElement(new Paragraph(valor, valorFont));
        celda.addElement(new Paragraph(etiqueta, etiquetaFont));
        tabla.addCell(celda);
    }

    private void agregarEncabezado(PdfPTable tabla, Font font, String... columnas) {
        for (String columna : columnas) {
            PdfPCell celda = new PdfPCell(new Phrase(columna, font));
            celda.setBackgroundColor(AZUL_MARCA);
            celda.setPadding(6);
            tabla.addCell(celda);
        }
    }

    private void crearCeldaEncabezado(Row fila, int columna, String texto, CellStyle estilo) {
        Cell celda = fila.createCell(columna);
        celda.setCellValue(texto);
        celda.setCellStyle(estilo);
    }
}