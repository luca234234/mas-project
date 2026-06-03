package com.example.marketplace.util;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class Events {
    private static final List<Consumer<String>> LOG_LISTENERS = new CopyOnWriteArrayList<>();
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private Events() {
    }

    public static void onLog(Consumer<String> listener) {
        LOG_LISTENERS.add(listener);
    }

    public static void log(String source, String message) {
        String line = LocalTime.now().format(TIME_FORMAT) + " [" + source + "] " + message;
        System.out.println(line);
        for (Consumer<String> listener : LOG_LISTENERS) {
            listener.accept(line);
        }
    }
}
