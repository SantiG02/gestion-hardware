package co.edu.uan.gestionhardware.controller;

import co.edu.uan.gestionhardware.dto.ResumenDashboard;
import co.edu.uan.gestionhardware.service.IndicadorService;
import co.edu.uan.gestionhardware.service.NotificacionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private final IndicadorService indicadorService;
    private final NotificacionService notificacionService;

    public DashboardController(IndicadorService indicadorService,
                               NotificacionService notificacionService) {
        this.indicadorService = indicadorService;
        this.notificacionService = notificacionService;
    }

    @GetMapping
    public String panel(Model model) {
        model.addAttribute("resumen", indicadorService.construirResumen());
        return "dashboard/panel";
    }

    @PostMapping("/notificar")
    public String notificar(RedirectAttributes flash) {

        ResumenDashboard resumen = indicadorService.construirResumen();

        if (resumen.getAlertas().isEmpty()) {
            flash.addFlashAttribute("aviso", "No hay alertas activas para notificar");
            return "redirect:/dashboard";
        }

        try {
            notificacionService.enviarAlertas(resumen.getAlertas());
            flash.addFlashAttribute("exito", "Se enviaron " + resumen.getAlertas().size()
                    + " alerta(s) a los Gestores y Técnicos activos");
        } catch (Exception e) {
            flash.addFlashAttribute("error", "No se pudo enviar el correo: " + e.getMessage());
        }

        return "redirect:/dashboard";
    }
}