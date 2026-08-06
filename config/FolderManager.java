package config;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class FolderManager {
    public static void createSubjectSubfolders(Path subjectFolder, String subjectName) throws IOException {
        String[] subfolders = {
            "1. Notas de Aula",
            "2. Exercícios",
            "3. Trabalhos",
            "4. Provas",
            "5. Materiais de Apoio"
        };

        for (int i = 0; i < subfolders.length; i++) {
            Path subPath = subjectFolder.resolve(subfolders[i]);
            if (!Files.exists(subPath)) {
                Files.createDirectories(subPath);
            }
        }
    }

    public static void moveDirectory(Path source, Path target) throws IOException {
        Files.walk(source).forEach(sourcePath -> {
            try {
                Path targetPath = target.resolve(source.relativize(sourcePath));
                if (Files.isDirectory(sourcePath)) {
                    if (!Files.exists(targetPath)) {
                        Files.createDirectories(targetPath);
                    }
                } else {
                    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        deleteDirectory(source);
    }

    public static void deleteDirectory(Path path) throws IOException {
        Files.walk(path)
             .sorted(Comparator.reverseOrder())
             .map(Path::toFile)
             .forEach(File::delete);
    }
}
