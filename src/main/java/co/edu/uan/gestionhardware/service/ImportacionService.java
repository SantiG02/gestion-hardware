package co.edu.uan.gestionhardware.service;

import co.edu.uan.gestionhardware.dto.FilaInconsistente;
import co.edu.uan.gestionhardware.dto.ResultadoImportacion;
import co.edu.uan.gestionhardware.model.Area;
import co.edu.uan.gestionhardware.model.Equipo;
import co.edu.uan.gestionhardware.model.EstadoEquipo;
import co.edu.uan.gestionhardware.model.TipoEquipo;
import co.edu.uan.gestionhardware.repository.AreaRepository;
import co.edu.uan.gestionhardware.repository.EquipoRepository;
import co.edu.uan.gestionhardware.repository.EstadoEquipoRepository;
import co.edu.uan.gestionhardware.repository.TipoEquipoRepository;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Plantilla de carga masiva de equipos e importacion desde CSV o Excel
 * (RF-07, RF-08). Las columnas se leen por POSICION, en el mismo orden en
 * que las trae la plantilla descargable, para no depender del texto exacto
 * del encabezado (mayusculas, tildes, etc.).
 *
 * Cada equipo importado queda con estado inicial "Estable" y sin responsable
 * asignado; ambos se pueden ajustar despues editando el equipo desde el
 * inventario.
 */
@Service
public class ImportacionService {

    private static final String[] ENCABEZADOS = {
            "Código interno*", "Serial*", "Marca*", "Modelo*", "Tipo de equipo",
            "Área*", "Procesador", "RAM (GB)", "Tipo de almacenamiento",
            "Capacidad (GB)", "Sistema operativo", "Fecha de compra* (dd/MM/aaaa)",
            "Fecha puesta en operación (dd/MM/aaaa)", "Observaciones"
    };

    private static final String[] FILA_EJEMPLO = {
            "INV-0100", "SN123456789", "Dell", "Latitude 5420", "Portatil",
            "Contabilidad", "Intel Core i5", "8", "SSD", "256", "Windows 11",
            "15/01/2024", "20/01/2024", "Equipo de prueba"
    };

    private static final int COL_FECHA_COMPRA = 11;
    private static final int COL_FECHA_PUESTA = 12;

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String ESTADO_INICIAL = "Estable";

    private final EquipoRepository equipoRepository;
    private final AreaRepository areaRepository;
    private final TipoEquipoRepository tipoEquipoRepository;
    private final EstadoEquipoRepository estadoEquipoRepository;

    public ImportacionService(EquipoRepository equipoRepository, AreaRepository areaRepository,
                              TipoEquipoRepository tipoEquipoRepository,
                              EstadoEquipoRepository estadoEquipoRepository) {
        this.equipoRepository = equipoRepository;
        this.areaRepository = areaRepository;
        this.tipoEquipoRepository = tipoEquipoRepository;
        this.estadoEquipoRepository = estadoEquipoRepository;
    }

    public byte[] generarPlantillaCsv() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.join(",", ENCABEZADOS)).append("\n");
        sb.append(String.join(",", FILA_EJEMPLO)).append("\n");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] generarPlantillaExcel() {
        try (XSSFWorkbook libro = new XSSFWorkbook()) {

            CellStyle estiloEncabezado = libro.createCellStyle();
            Font fuente = libro.createFont();
            fuente.setBold(true);
            fuente.setColor(IndexedColors.WHITE.getIndex());
            estiloEncabezado.setFont(fuente);
            estiloEncabezado.setFillForegroundColor(IndexedColors.BLUE.getIndex());
            estiloEncabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Sheet hoja = libro.createSheet("Equipos");
            Row filaEncabezado = hoja.createRow(0);
            for (int c = 0; c < ENCABEZADOS.length; c++) {
                Cell celda = filaEncabezado.createCell(c);
                celda.setCellValue(ENCABEZADOS[c]);
                celda.setCellStyle(estiloEncabezado);
            }

            Row filaEjemplo = hoja.createRow(1);
            for (int c = 0; c < FILA_EJEMPLO.length; c++) {
                filaEjemplo.createCell(c).setCellValue(FILA_EJEMPLO[c]);
            }

            for (int c = 0; c < ENCABEZADOS.length; c++) {
                hoja.autoSizeColumn(c);
            }

            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            libro.write(salida);
            return salida.toByteArray();

        } catch (IOException e) {
            throw new IllegalStateException("No se pudo generar la plantilla de Excel", e);
        }
    }

    @Transactional
    public ResultadoImportacion procesar(MultipartFile archivo) {

        String nombre = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "archivo";
        List<String[]> filas;

        try {
            String nombreMinuscula = nombre.toLowerCase();
            if (nombreMinuscula.endsWith(".xlsx") || nombreMinuscula.endsWith(".xls")) {
                filas = leerExcel(archivo);
            } else {
                filas = leerCsv(archivo);
            }
        } catch (Exception e) {
            List<FilaInconsistente> error = List.of(
                    new FilaInconsistente(0, null, "No se pudo leer el archivo: " + e.getMessage()));
            return new ResultadoImportacion(nombre, 0, 0, error);
        }

        if (filas.isEmpty()) {
            return new ResultadoImportacion(nombre, 0, 0, List.of());
        }

        // La primera fila es el encabezado; se descarta.
        List<String[]> datos = filas.subList(1, filas.size());

        Map<String, Area> areasPorNombre = new HashMap<>();
        for (Area a : areaRepository.findAll()) {
            areasPorNombre.put(a.getNombre().trim().toLowerCase(), a);
        }

        Map<String, TipoEquipo> tiposPorNombre = new HashMap<>();
        for (TipoEquipo t : tipoEquipoRepository.findAll()) {
            tiposPorNombre.put(t.getNombre().trim().toLowerCase(), t);
        }

        EstadoEquipo estadoInicial = estadoEquipoRepository.findByNombre(ESTADO_INICIAL)
                .orElseThrow(() -> new IllegalStateException(
                        "No existe el estado '" + ESTADO_INICIAL + "' en la base de datos"));

        Set<String> codigosEnArchivo = new HashSet<>();
        Set<String> serialesEnArchivo = new HashSet<>();

        List<FilaInconsistente> inconsistencias = new ArrayList<>();
        int cargados = 0;

        for (int i = 0; i < datos.size(); i++) {

            String[] fila = datos.get(i);
            int numeroFila = i + 2; // +1 por el encabezado, +1 porque las filas empiezan en 1

            if (esFilaVacia(fila)) {
                continue;
            }

            String codigoInterno = valor(fila, 0);
            String serial = valor(fila, 1);
            String marca = valor(fila, 2);
            String modelo = valor(fila, 3);
            String nombreTipo = valor(fila, 4);
            String nombreArea = valor(fila, 5);
            String procesador = valor(fila, 6);
            String ramTexto = valor(fila, 7);
            String tipoAlmacenamiento = valor(fila, 8);
            String capacidadTexto = valor(fila, 9);
            String sistemaOperativo = valor(fila, 10);
            String fechaCompraTexto = valor(fila, COL_FECHA_COMPRA);
            String fechaPuestaTexto = valor(fila, COL_FECHA_PUESTA);
            String observaciones = valor(fila, 13);

            String motivo = validarFila(codigoInterno, serial, marca, modelo, nombreArea,
                    fechaCompraTexto, ramTexto, capacidadTexto, fechaPuestaTexto,
                    areasPorNombre, nombreTipo, tiposPorNombre,
                    codigosEnArchivo, serialesEnArchivo);

            if (motivo != null) {
                inconsistencias.add(new FilaInconsistente(numeroFila, codigoInterno, motivo));
                continue;
            }

            Equipo equipo = new Equipo();
            equipo.setCodigoInterno(codigoInterno);
            equipo.setSerial(serial);
            equipo.setMarca(marca);
            equipo.setModelo(modelo);
            equipo.setArea(areasPorNombre.get(nombreArea.trim().toLowerCase()));
            equipo.setEstadoEquipo(estadoInicial);
            equipo.setActivo(true);

            if (!nombreTipo.isBlank()) equipo.setTipoEquipo(tiposPorNombre.get(nombreTipo.trim().toLowerCase()));
            if (!procesador.isBlank()) equipo.setProcesador(procesador);
            if (!ramTexto.isBlank()) equipo.setRamGb(Integer.parseInt(ramTexto.trim()));
            if (!tipoAlmacenamiento.isBlank()) equipo.setTipoAlmacenamiento(tipoAlmacenamiento);
            if (!capacidadTexto.isBlank()) equipo.setCapacidadGb(Integer.parseInt(capacidadTexto.trim()));
            if (!sistemaOperativo.isBlank()) equipo.setSistemaOperativo(sistemaOperativo);
            equipo.setFechaCompra(LocalDate.parse(fechaCompraTexto.trim(), FORMATO_FECHA));
            if (!fechaPuestaTexto.isBlank()) {
                equipo.setFechaPuestaOperacion(LocalDate.parse(fechaPuestaTexto.trim(), FORMATO_FECHA));
            }
            if (!observaciones.isBlank()) equipo.setObservaciones(observaciones);

            equipoRepository.save(equipo);
            codigosEnArchivo.add(codigoInterno.trim().toLowerCase());
            serialesEnArchivo.add(serial.trim().toLowerCase());
            cargados++;
        }

        return new ResultadoImportacion(nombre, datos.size(), cargados, inconsistencias);
    }

    private String validarFila(String codigoInterno, String serial, String marca, String modelo,
                               String nombreArea, String fechaCompraTexto, String ramTexto,
                               String capacidadTexto, String fechaPuestaTexto,
                               Map<String, Area> areasPorNombre, String nombreTipo,
                               Map<String, TipoEquipo> tiposPorNombre,
                               Set<String> codigosEnArchivo, Set<String> serialesEnArchivo) {

        if (codigoInterno.isBlank()) return "Falta el código interno";
        if (serial.isBlank()) return "Falta el serial";
        if (marca.isBlank()) return "Falta la marca";
        if (modelo.isBlank()) return "Falta el modelo";
        if (nombreArea.isBlank()) return "Falta el área";
        if (fechaCompraTexto.isBlank()) return "Falta la fecha de compra";

        String codigoClave = codigoInterno.trim().toLowerCase();
        String serialClave = serial.trim().toLowerCase();

        if (codigosEnArchivo.contains(codigoClave)) return "Código interno repetido en el archivo";
        if (serialesEnArchivo.contains(serialClave)) return "Serial repetido en el archivo";
        if (equipoRepository.existsByCodigoInterno(codigoInterno.trim())) return "Ya existe un equipo con ese código interno";
        if (equipoRepository.existsBySerial(serial.trim())) return "Ya existe un equipo con ese serial";

        if (!areasPorNombre.containsKey(nombreArea.trim().toLowerCase())) {
            return "El área '" + nombreArea + "' no existe";
        }

        if (!nombreTipo.isBlank() && !tiposPorNombre.containsKey(nombreTipo.trim().toLowerCase())) {
            return "El tipo de equipo '" + nombreTipo + "' no existe";
        }

        try {
            LocalDate fechaCompra = LocalDate.parse(fechaCompraTexto.trim(), FORMATO_FECHA);
            if (fechaCompra.isAfter(LocalDate.now())) {
                return "La fecha de compra no puede ser futura";
            }
        } catch (DateTimeParseException e) {
            return "Fecha de compra inválida (use dd/MM/aaaa)";
        }

        if (!fechaPuestaTexto.isBlank()) {
            try {
                LocalDate.parse(fechaPuestaTexto.trim(), FORMATO_FECHA);
            } catch (DateTimeParseException e) {
                return "Fecha de puesta en operación inválida (use dd/MM/aaaa)";
            }
        }

        if (!ramTexto.isBlank() && !esEnteroPositivo(ramTexto)) {
            return "RAM (GB) debe ser un número entero mayor a cero";
        }

        if (!capacidadTexto.isBlank() && !esEnteroPositivo(capacidadTexto)) {
            return "Capacidad (GB) debe ser un número entero mayor a cero";
        }

        return null;
    }

    private boolean esEnteroPositivo(String texto) {
        try {
            return Integer.parseInt(texto.trim()) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean esFilaVacia(String[] fila) {
        for (String celda : fila) {
            if (celda != null && !celda.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String valor(String[] fila, int indice) {
        if (indice >= fila.length || fila[indice] == null) {
            return "";
        }
        return fila[indice].trim();
    }

    private List<String[]> leerCsv(MultipartFile archivo) throws IOException, CsvException {
        try (Reader lector = new InputStreamReader(archivo.getInputStream(), StandardCharsets.UTF_8);
             CSVReader csvReader = new CSVReader(lector)) {
            return csvReader.readAll();
        }
    }

    private List<String[]> leerExcel(MultipartFile archivo) throws IOException {

        List<String[]> filas = new ArrayList<>();
        DataFormatter formateador = new DataFormatter();

        try (Workbook libro = WorkbookFactory.create(archivo.getInputStream())) {
            Sheet hoja = libro.getSheetAt(0);

            for (Row fila : hoja) {
                int ultimaColumna = Math.max(fila.getLastCellNum(), ENCABEZADOS.length);
                String[] valores = new String[ultimaColumna];
                                for (int c = 0; c < ultimaColumna; c++) {
                    Cell celda = fila.getCell(c, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    if ((c == COL_FECHA_COMPRA || c == COL_FECHA_PUESTA)
                            && celda.getCellType() == CellType.NUMERIC
                            && DateUtil.isCellDateFormatted(celda)) {
                        valores[c] = FORMATO_FECHA.format(celda.getLocalDateTimeCellValue().toLocalDate());
                    } else if (celda.getCellType() == CellType.NUMERIC) {
                        // Lee el numero real de la celda, sin importar que formato visual
                        // le haya quedado puesto por error (por ejemplo, fecha).
                        double numero = celda.getNumericCellValue();
                        valores[c] = (numero == Math.floor(numero))
                                ? String.valueOf((long) numero)
                                : String.valueOf(numero);
                    } else {
                        valores[c] = formateador.formatCellValue(celda);
                    }
                }
                filas.add(valores);
            }
        }

        return filas;
    }
}