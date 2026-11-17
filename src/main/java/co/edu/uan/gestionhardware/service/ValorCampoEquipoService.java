package co.edu.uan.gestionhardware.service;

import co.edu.uan.gestionhardware.model.CampoTipoEquipo;
import co.edu.uan.gestionhardware.model.Equipo;
import co.edu.uan.gestionhardware.model.TipoEquipo;
import co.edu.uan.gestionhardware.model.ValorCampoEquipo;
import co.edu.uan.gestionhardware.repository.CampoTipoEquipoRepository;
import co.edu.uan.gestionhardware.repository.ValorCampoEquipoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Valores de los campos propios de cada tipo de equipo: lectura,
 * validacion segun el tipo de dato del campo y almacenamiento.
 */
@Service
@Transactional(readOnly = true)
public class ValorCampoEquipoService {

    private static final int MAX_TEXTO = 255;

    private final ValorCampoEquipoRepository valorRepository;
    private final CampoTipoEquipoRepository campoRepository;

    public ValorCampoEquipoService(ValorCampoEquipoRepository valorRepository,
                                   CampoTipoEquipoRepository campoRepository) {
        this.valorRepository = valorRepository;
        this.campoRepository = campoRepository;
    }

    public List<ValorCampoEquipo> listarPorEquipo(Long equipoId) {
        return valorRepository.findPorEquipo(equipoId);
    }

    /** Valores del equipo indexados por id de campo, para rellenar el formulario. */
    public Map<Long, String> valoresPorCampo(Long equipoId) {
        Map<Long, String> mapa = new HashMap<>();
        for (ValorCampoEquipo v : listarPorEquipo(equipoId)) {
            mapa.put(v.getCampo().getId(), v.getValor());
        }
        return mapa;
    }

    /**
     * Valida los valores enviados contra los campos activos del tipo.
     * Devuelve un mapa idCampo -> mensaje de error (vacio si todo es valido).
     */
    public Map<Long, String> validar(TipoEquipo tipo, Map<Long, String> valores) {
        Map<Long, String> errores = new HashMap<>();
        if (tipo == null || tipo.getId() == null) {
            return errores;
        }

        for (CampoTipoEquipo campo
                : campoRepository.findByTipoEquipoIdAndActivoTrueOrderByIdAsc(tipo.getId())) {

            String valor = valores.get(campo.getId());
            valor = (valor == null) ? "" : valor.trim();

            if (valor.isEmpty()) {
                if (Boolean.TRUE.equals(campo.getObligatorio())) {
                    errores.put(campo.getId(), "Este campo es obligatorio");
                }
                continue;
            }

            String error = validarValor(campo, valor);
            if (error != null) {
                errores.put(campo.getId(), error);
            }
        }
        return errores;
    }

    private String validarValor(CampoTipoEquipo campo, String valor) {
        if (valor.length() > MAX_TEXTO) {
            return "Maximo " + MAX_TEXTO + " caracteres";
        }
        return switch (campo.getTipoDato()) {
            case NUMERO -> esNumero(valor) ? null : "Debe ser un numero";
            case FECHA -> esFecha(valor) ? null : "Fecha invalida";
            case LISTA -> opcionesDe(campo).contains(valor) ? null : "Seleccione una opcion de la lista";
            case TEXTO -> null;
        };
    }

    private boolean esNumero(String valor) {
        try {
            new BigDecimal(valor);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean esFecha(String valor) {
        try {
            LocalDate.parse(valor);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private List<String> opcionesDe(CampoTipoEquipo campo) {
        if (campo.getOpciones() == null) {
            return List.of();
        }
        return Arrays.stream(campo.getOpciones().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /**
     * Reemplaza los valores del equipo por los enviados, guardando solo los
     * de campos activos del tipo actual (si el equipo cambia de tipo, los
     * valores del tipo anterior se descartan).
     */
    @Transactional
    public void reemplazar(Equipo equipo, Map<Long, String> valores) {
        valorRepository.eliminarPorEquipo(equipo.getId());

        if (equipo.getTipoEquipo() == null) {
            return;
        }

        for (CampoTipoEquipo campo
                : campoRepository.findByTipoEquipoIdAndActivoTrueOrderByIdAsc(equipo.getTipoEquipo().getId())) {

            String valor = valores.get(campo.getId());
            if (valor == null || valor.isBlank()) {
                continue;
            }

            ValorCampoEquipo nuevo = new ValorCampoEquipo();
            nuevo.setEquipo(equipo);
            nuevo.setCampo(campo);
            nuevo.setValor(valor.trim());
            valorRepository.save(nuevo);
        }
    }
}
