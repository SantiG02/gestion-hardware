package co.edu.uan.gestionhardware.service;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LimitadorSolicitudes {

    private static final int MAXIMO = 5;
    private static final Duration VENTANA = Duration.ofHours(1);

    private final ConcurrentHashMap<String, Deque<Instant>> intentos = new ConcurrentHashMap<>();

    public boolean permitir(String clave) {
        Instant ahora = Instant.now();
        Deque<Instant> cola = intentos.computeIfAbsent(clave, k -> new ArrayDeque<>());

        synchronized (cola) {
            while (!cola.isEmpty() && cola.peekFirst().isBefore(ahora.minus(VENTANA))) {
                cola.pollFirst();
            }
            if (cola.size() >= MAXIMO) {
                return false;
            }
            cola.addLast(ahora);
            return true;
        }
    }
}