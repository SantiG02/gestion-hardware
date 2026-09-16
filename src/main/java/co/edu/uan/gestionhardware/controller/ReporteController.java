package co.edu.uan.gestionhardware.controller;

import co.edu.uan.gestionhardware.dto.ReportePeriodo;
import co.edu.uan.gestionhardware.service.ReporteService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;

@Controller
@RequestMapping("/reportes")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping
    public String formulario(Model model) {
        model.addAttribute("anioActual", LocalDate.now().getYear());
        model.addAttribute("mesActual", LocalDate.now().getMonthValue());
        return "reportes/formulario";
    }

    @GetMapping("/pdf")
    public ResponseEntity<byte[]> descargarPdf(@RequestParam String tipo,
                                               @RequestParam int anio,
                                               @RequestParam(required = false) Integer mes) {

        RangoPeriodo rango = calcularRango(tipo, anio, mes);
        ReportePeriodo reporte = reporteService.generar(rango.desde(), rango.hasta(), rango.etiqueta());
        byte[] pdf = reporteService.generarPdf(reporte);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte-" + rango.archivo() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/excel")
    public ResponseEntity<byte[]> descargarExcel(@RequestParam String tipo,
                                                 @RequestParam int anio,
                                                 @RequestParam(required = false) Integer mes) {

        RangoPeriodo rango = calcularRango(tipo, anio, mes);
        ReportePeriodo reporte = reporteService.generar(rango.desde(), rango.hasta(), rango.etiqueta());
        byte[] excel = reporteService.generarExcel(reporte);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte-" + rango.archivo() + ".xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }

    private RangoPeriodo calcularRango(String tipo, int anio, Integer mes) {

        if ("ANUAL".equalsIgnoreCase(tipo)) {
            LocalDate desde = LocalDate.of(anio, 1, 1);
            LocalDate hasta = LocalDate.of(anio, 12, 31);
            return new RangoPeriodo(desde, hasta, "Anual " + anio, String.valueOf(anio));
        }

        int mesValido = (mes != null) ? mes : LocalDate.now().getMonthValue();
        YearMonth ym = YearMonth.of(anio, mesValido);
        String nombreMes = ym.getMonth().getDisplayName(TextStyle.FULL, new Locale("es", "ES"));
        String etiqueta = "Mensual " + nombreMes + " " + anio;
        String archivo = anio + "-" + String.format("%02d", mesValido);

        return new RangoPeriodo(ym.atDay(1), ym.atEndOfMonth(), etiqueta, archivo);
    }

    private record RangoPeriodo(LocalDate desde, LocalDate hasta, String etiqueta, String archivo) {}
}