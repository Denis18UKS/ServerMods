package dev.ingamecreator.core.ui;

public enum IdeSection {
    DASHBOARD("Dashboard"),
    PROJECTS("Projects"),
    COMMANDS("Commands"),
    ROLES("Roles"),
    BLUEPRINTS("Blueprints"),
    ELEMENTS("Elements"),
    TEST("Test"),
    SETTINGS("Settings");

    private final String label;

    IdeSection(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
