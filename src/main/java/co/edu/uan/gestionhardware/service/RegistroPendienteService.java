package co.edu.uan.gestionhardware.service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Guarda en memoria los registros de cuenta que esperan que su correo se
 * confirme con un codigo de 6 digitos. La cuenta real (Usuario) solo se crea
 * cuando el codigo es correcto, asi que la base de datos nunca se llena de
 * cuentas sin verificar. Si la aplicacion se reinicia, los registros
 * pendientes se pierden y la persona simplemente vuelve a registrarse.
 */
@Service
public class RegistroPendienteService {

    public static final int MAX_INTENTOS = 5;

    private static final Duration VIGENCIA = Duration.ofMinutes(15);
    private static final Duration ESPERA_REENVIO = Duration.ofSeconds(60);
    private static final int MAX_REGISTROS = 200;

    private final SecureRandom azar = new SecureRandom();
    private final Map<String, Registro> registros = new HashMap<>();

    /** Datos que se conservan mientras se espera el codigo. */
    public static class Registro {
        private final String nombre;
        private final String email;
        private final String passwordHash;
        private final String ip;
        private final String rol;
        private final LocalDateTime aceptadoEn;
        private String codigo;
        private LocalDateTime expira;
        private LocalDateTime ultimoEnvio;
        private int intentos;

        private Registro(String nombre, String email, String passwordHash, String ip, String rol) {
            this.nombre = nombre;
            this.email = email;
            this.passwordHash = passwordHash;
            this.ip = ip;
            this.rol = rol;
            this.aceptadoEn = LocalDateTime.now();
        }

        public String getNombre() { return nombre; }
        public String getEmail() { return email; }
        public String getPasswordHash() { return passwordHash; }
        public String getIp() { return ip; }
        public LocalDateTime getAceptadoEn() { return aceptadoEn; }
        public String getRol() { return rol; }
    }

    /** Error que se le muestra a la persona; reiniciar = debe volver a llenar el formulario. */
    public static class VerificacionException extends RuntimeException {
        private final boolean reiniciar;

        public VerificacionException(String mensaje, boolean reiniciar) {
            super(mensaje);
            this.reiniciar = reiniciar;
        }

        public boolean isReiniciar() { return reiniciar; }
    }

    /** Crea (o reemplaza) el registro pendiente de ese correo y devuelve el codigo a enviar. */
        public synchronized String iniciar(String nombre, String email, String passwordHash, String ip, String rol) {

        limpiarVencidos();

        if (!registros.containsKey(email) && registros.size() >= MAX_REGISTROS) {
            throw new IllegalStateException("Hay demasiados registros en espera. Inténtalo en unos minutos.");
        }

        Registro registro = new Registro(nombre, email, passwordHash, ip, rol);
        String codigo = nuevoCodigo(registro);
        registros.put(email, registro);
        return codigo;
    }

    /** Genera un codigo nuevo para un registro pendiente (reenvio). */
    public synchronized String reenviar(String email) {

        limpiarVencidos();
        Registro registro = registros.get(email);

        if (registro == null) {
            throw new VerificacionException(
                    "No hay un registro pendiente para ese correo. Crea la cuenta de nuevo.", true);
        }

        if (registro.ultimoEnvio.plus(ESPERA_REENVIO).isAfter(LocalDateTime.now())) {
            throw new VerificacionException(
                    "Espera un minuto antes de pedir otro código.", false);
        }

        return nuevoCodigo(registro);
    }

    /** Si el codigo es correcto, devuelve el registro y lo retira; si no, lanza VerificacionException. */
    public synchronized Registro verificar(String email, String codigo) {

        limpiarVencidos();
        Registro registro = registros.get(email);

        if (registro == null) {
            throw new VerificacionException(
                    "El código venció o no hay un registro pendiente. Crea la cuenta de nuevo.", true);
        }

        registro.intentos++;

        boolean coincide = codigo != null && MessageDigest.isEqual(
                registro.codigo.getBytes(StandardCharsets.UTF_8),
                codigo.trim().getBytes(StandardCharsets.UTF_8));

        if (!coincide) {
            if (registro.intentos >= MAX_INTENTOS) {
                registros.remove(email);
                throw new VerificacionException(
                        "Demasiados intentos incorrectos. Crea la cuenta de nuevo.", true);
            }
            throw new VerificacionException(
                    "El código no es correcto. Te quedan " + (MAX_INTENTOS - registro.intentos) + " intento(s).", false);
        }

        registros.remove(email);
        return registro;
    }

    /** Descarta el registro (por ejemplo si no se pudo enviar el correo). */
    public synchronized void descartar(String email) {
        registros.remove(email);
    }

    private String nuevoCodigo(Registro registro) {
        String codigo = String.format("%06d", azar.nextInt(1_000_000));
        registro.codigo = codigo;
        registro.expira = LocalDateTime.now().plus(VIGENCIA);
        registro.ultimoEnvio = LocalDateTime.now();
        registro.intentos = 0;
        return codigo;
    }

    private void limpiarVencidos() {
        LocalDateTime ahora = LocalDateTime.now();
        registros.values().removeIf(r -> r.expira.isBefore(ahora));
    }
}