package spyke.iptables.util;

public class OperatingSystem {
    public static boolean isLinux(){
        String OS = System.getProperty("os.name").toLowerCase();
        return OS.contains("nix") || OS.contains("nux") || OS.contains("aix");
    }
}
