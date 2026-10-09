package co.edu.uan.gestionhardware.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Pagina publica con los Terminos y Condiciones y la politica de tratamiento de datos. */
@Controller
public class TerminosController {

    @GetMapping("/terminos")
    public String mostrar() {
        return "terminos";
    }
}