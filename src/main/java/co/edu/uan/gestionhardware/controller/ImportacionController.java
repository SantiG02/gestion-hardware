package co.edu.uan.gestionhardware.controller;

import co.edu.uan.gestionhardware.dto.ResultadoImportacion;
import co.edu.uan.gestionhardware.service.ImportacionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/equipos/importar")
public class ImportacionController {

    private final ImportacionService importacionService;

    public ImportacionController(ImportacionService importacionService) {
        this.importacionService = importacionService;
    }

    @GetMapping
    public String formulario() {
        return "equipo/importar";
    }

    @GetMapping("/plantilla.csv")
    public ResponseEntity<byte[]> plantillaCsv() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"plantilla-equipos.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(importacionService.generarPlantillaCsv());
    }

    @GetMapping("/plantilla.xlsx")
    public ResponseEntity<byte[]> plantillaExcel() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"plantilla-equipos.xlsx\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(importacionService.generarPlantillaExcel());
    }

    @PostMapping
    public String procesar(@RequestParam("archivo") MultipartFile archivo, Model model) {

        if (archivo.isEmpty()) {
            model.addAttribute("error", "Selecciona un archivo CSV o Excel para continuar");
            return "equipo/importar";
        }

        ResultadoImportacion resultado = importacionService.procesar(archivo);
        model.addAttribute("resultado", resultado);
        return "equipo/importar";
    }
}