package dev.ingamecreator.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public final class CreatorPreferences {
    private final Path file;
    private Language language = Language.ENGLISH;

    public CreatorPreferences(Path file) {
        this.file = file;
        load();
    }

    public Language language() { return language; }

    public void cycleLanguage() {
        Language[] values = Language.values();
        language = values[(language.ordinal() + 1) % values.length];
        save();
    }

    public String text(String key) {
        return language.text(key);
    }

    private void load() {
        if (!Files.isRegularFile(file)) return;
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            p.load(in);
            language = Language.valueOf(p.getProperty("language", "ENGLISH"));
        } catch (Exception ignored) {
            language = Language.ENGLISH;
        }
    }

    private void save() {
        try {
            Files.createDirectories(file.getParent());
            Properties p = new Properties();
            p.setProperty("language", language.name());
            try (OutputStream out = Files.newOutputStream(file)) { p.store(out, "InGame Creator client preferences"); }
        } catch (IOException ignored) {
            // A settings write failure must never prevent the IDE from opening.
        }
    }

    public enum Language {
        ENGLISH("English", map(
                "dashboard","Dashboard","projects","Projects","commands","Commands","roles","Roles","blueprints","Blueprints","elements","Elements","test","Test","settings","Settings",
                "create_project","Create Project","create_blueprint","Create Blueprint","save","Save","language","Creator language","close","ESC — close")),
        GERMAN("Deutsch", map(
                "dashboard","Übersicht","projects","Projekte","commands","Befehle","roles","Rollen","blueprints","Blueprints","elements","Elemente","test","Test","settings","Einstellungen",
                "create_project","Projekt erstellen","create_blueprint","Blueprint erstellen","save","Speichern","language","Creator-Sprache","close","ESC — schließen")),
        SPANISH("Español", map(
                "dashboard","Inicio","projects","Proyectos","commands","Comandos","roles","Roles","blueprints","Blueprints","elements","Elementos","test","Prueba","settings","Ajustes",
                "create_project","Crear proyecto","create_blueprint","Crear Blueprint","save","Guardar","language","Idioma de Creator","close","ESC — cerrar"));

        private final String displayName;
        private final Map<String,String> strings;
        Language(String displayName, Map<String,String> strings) { this.displayName = displayName; this.strings = strings; }
        public String displayName() { return displayName; }
        public String text(String key) { return strings.getOrDefault(key, ENGLISH.strings.getOrDefault(key, key)); }

        private static Map<String,String> map(String... values) {
            LinkedHashMap<String,String> map = new LinkedHashMap<>();
            for (int i = 0; i + 1 < values.length; i += 2) map.put(values[i], values[i + 1]);
            return Map.copyOf(map);
        }
    }
}
