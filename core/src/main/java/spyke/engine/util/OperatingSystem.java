package spyke.engine.util;

public class OperatingSystem {

    /**
     * Checks is the current running OS is Linux.
     *
     * @return {@code true} if the current OS is linux, {@code false} otherwise.
     */
    public static boolean isLinux() {
        final String OS = System.getProperty("os.name").toLowerCase();
        return OS.contains("nix") || OS.contains("nux") || OS.contains("aix");
    }
}
