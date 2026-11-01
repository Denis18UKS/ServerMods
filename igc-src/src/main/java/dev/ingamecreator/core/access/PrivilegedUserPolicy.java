package dev.ingamecreator.core.access;

public final class PrivilegedUserPolicy {
    public static final String OWNER_USERNAME = "Zunserz";

    private PrivilegedUserPolicy() {}

    public static boolean isOwner(String username) {
        return OWNER_USERNAME.equals(username);
    }
}
