import java.io.File;

import java.util.Arrays;
import java.util.List;

public class Cursar {
    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            deleteClassFiles();
        }));

        config.Installer.checkAndRun(args);

        List<String> options = Arrays.asList(
                "🎓 Ir para o Cursar Normal (Faculdade)",
                "🧠 Gerenciar Obsidian (Segundo Cérebro)",
                "📦 Gerar ZIP (Preparar Projeto para Envio)",
                "❌ Sair");

        while (true) {
            int choice = config.InteractiveMenu.select("🧠 SEGUNDO CÉREBRO & GESTÃO STUDYFLOW", options);
            if (choice == 0) {
                config.Main.run(args);
            } else if (choice == 1) {
                config.ObsidianManager.run(args);
            } else if (choice == 2) {
                config.Main.runZipExport(args);
            } else if (choice == 3 || choice == -1) {
                System.out.println("\nAté logo!");
                break;
            }
        }
    }

    private static void deleteClassFiles() {
        try {
            File rootDir = new File(".");
            File[] rootClasses = rootDir.listFiles((dir, name) -> name.endsWith(".class"));
            if (rootClasses != null) {
                for (File f : rootClasses) {
                    f.delete();
                }
            }

            File configDir = new File("config");
            if (configDir.exists() && configDir.isDirectory()) {
                File[] configClasses = configDir.listFiles((dir, name) -> name.endsWith(".class"));
                if (configClasses != null) {
                    for (File f : configClasses) {
                        f.delete();
                    }
                }
            }
        } catch (Exception e) {
            // Ignore errors on shutdown cleanup
        }
    }
}
