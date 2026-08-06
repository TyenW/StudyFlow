package config;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class FrontmatterParser {

    public static class NoteMetadata {
        public String criado_em = "";
        public String atualizado_em = "";
        public List<String> tags = new ArrayList<>();
        public List<String> aliases = new ArrayList<>();
        public String content = "";
    }

    public static NoteMetadata parse(Path file) throws IOException {
        NoteMetadata metadata = new NoteMetadata();
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);

        if (lines.isEmpty()) {
            return metadata;
        }

        boolean hasFrontmatter = lines.get(0).trim().equals("---");
        int endFrontmatterIdx = -1;

        if (hasFrontmatter) {
            for (int i = 1; i < lines.size(); i++) {
                if (lines.get(i).trim().equals("---")) {
                    endFrontmatterIdx = i;
                    break;
                }
            }
        }

        if (hasFrontmatter && endFrontmatterIdx != -1) {
            // Parse frontmatter lines
            List<String> fmLines = lines.subList(1, endFrontmatterIdx);
            parseFrontmatterLines(fmLines, metadata);

            // Reconstruct content
            StringBuilder contentBuilder = new StringBuilder();
            for (int i = endFrontmatterIdx + 1; i < lines.size(); i++) {
                contentBuilder.append(lines.get(i)).append("\n");
            }
            metadata.content = contentBuilder.toString();
        } else {
            // No frontmatter
            StringBuilder contentBuilder = new StringBuilder();
            for (String line : lines) {
                contentBuilder.append(line).append("\n");
            }
            metadata.content = contentBuilder.toString();
        }

        return metadata;
    }

    public static void save(Path file, NoteMetadata metadata) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("---\n");
        sb.append("criado_em: ").append(metadata.criado_em != null ? metadata.criado_em : "").append("\n");
        sb.append("atualizado_em: ").append(metadata.atualizado_em != null ? metadata.atualizado_em : "").append("\n");
        
        // Tags
        sb.append("tags: [");
        for (int i = 0; i < metadata.tags.size(); i++) {
            sb.append(metadata.tags.get(i));
            if (i < metadata.tags.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("]\n");

        // Aliases
        sb.append("aliases: [");
        for (int i = 0; i < metadata.aliases.size(); i++) {
            String alias = metadata.aliases.get(i);
            // escape quotes in alias if necessary
            sb.append("\"").append(alias.replace("\"", "\\\"")).append("\"");
            if (i < metadata.aliases.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("]\n");
        sb.append("---\n");

        // Content
        sb.append(metadata.content);

        Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
    }

    private static void parseFrontmatterLines(List<String> lines, NoteMetadata metadata) {
        String currentKey = null;
        List<String> listAccumulator = new ArrayList<>();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            // Check if it's a list item under a block-style key
            if (trimmed.startsWith("-") && currentKey != null) {
                String val = trimmed.substring(1).trim();
                val = stripQuotes(val);
                listAccumulator.add(val);
                continue;
            }

            // If we find a new key, save the accumulated list first
            if (line.contains(":") && !trimmed.startsWith("-")) {
                if (currentKey != null && !listAccumulator.isEmpty()) {
                    assignList(currentKey, listAccumulator, metadata);
                    listAccumulator = new ArrayList<>();
                }

                int colonIdx = line.indexOf(":");
                String key = line.substring(0, colonIdx).trim();
                String val = line.substring(colonIdx + 1).trim();

                currentKey = key;

                if (!val.isEmpty()) {
                    if (val.startsWith("[") && val.endsWith("]")) {
                        // Inline list: [a, b, c]
                        List<String> items = parseInlineList(val);
                        assignList(key, items, metadata);
                        currentKey = null; // List closed
                    } else {
                        // Simple string value
                        val = stripQuotes(val);
                        assignValue(key, val, metadata);
                    }
                }
            }
        }

        // Final accumulation
        if (currentKey != null && !listAccumulator.isEmpty()) {
            assignList(currentKey, listAccumulator, metadata);
        }
    }

    private static List<String> parseInlineList(String val) {
        List<String> list = new ArrayList<>();
        String content = val.substring(1, val.length() - 1).trim();
        if (content.isEmpty()) return list;

        // Split by commas, taking care of potentially quoted strings
        // Simple splitter for simplicity, but robust for most lists
        String[] parts = content.split(",");
        for (String part : parts) {
            String p = part.trim();
            p = stripQuotes(p);
            if (!p.isEmpty()) {
                list.add(p);
            }
        }
        return list;
    }

    private static String stripQuotes(String str) {
        if (str.startsWith("\"") && str.endsWith("\"") && str.length() >= 2) {
            return str.substring(1, str.length() - 1).replace("\\\"", "\"");
        }
        if (str.startsWith("'") && str.endsWith("'") && str.length() >= 2) {
            return str.substring(1, str.length() - 1).replace("\\'", "'");
        }
        return str;
    }

    private static void assignValue(String key, String val, NoteMetadata metadata) {
        if (key.equals("criado_em")) {
            metadata.criado_em = val;
        } else if (key.equals("atualizado_em")) {
            metadata.atualizado_em = val;
        }
    }

    private static void assignList(String key, List<String> list, NoteMetadata metadata) {
        if (key.equals("tags")) {
            metadata.tags.addAll(list);
        } else if (key.equals("aliases")) {
            metadata.aliases.addAll(list);
        }
    }
}
