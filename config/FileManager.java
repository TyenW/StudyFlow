package config;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

public class FileManager {
    public static List<Subject> loadSubjects(Path planningPath) throws IOException {
        List<String> lines = Files.readAllLines(planningPath);
        List<Subject> subjects = new ArrayList<>();
        String currentPeriod = "Geral";
        Pattern periodPattern = Pattern.compile("^##\\s*.*(\\dº\\s*Período).*");
        Pattern subjectPattern = Pattern.compile("^\\s*-\\s*\\[([ xcdXCD])\\]\\s*(.+)$");

        for (String line : lines) {
            Matcher periodMatcher = periodPattern.matcher(line);
            if (periodMatcher.matches()) {
                currentPeriod = periodMatcher.group(1);
            }

            Matcher subjectMatcher = subjectPattern.matcher(line);
            if (subjectMatcher.matches()) {
                String status = subjectMatcher.group(1).toLowerCase();
                String name = subjectMatcher.group(2);
                subjects.add(new Subject(line, status, name, currentPeriod));
            }
        }
        return subjects;
    }

    public static void updateSubjectStatus(Path planningPath, Subject target, String newStatusChar) throws IOException {
        backupFile(planningPath);
        List<String> lines = Files.readAllLines(planningPath);
        String oldLine = target.originalLine;

        int index = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).equals(oldLine)) {
                index = i;
                break;
            }
        }

        if (index != -1) {
            String newLine = oldLine.replaceFirst("\\[[ xcdXCD]\\]", "[" + newStatusChar + "]");
            lines.set(index, newLine);
            Files.write(planningPath, lines);
            target.originalLine = newLine;
            target.statusChar = newStatusChar;
        } else {
            throw new IOException("Linha correspondente à matéria não encontrada no arquivo.");
        }
    }

    public static List<String> loadTasks(Path subjectFolder) throws IOException {
        Path taskFile = subjectFolder.resolve("Tarefas.md");
        if (!Files.exists(taskFile)) {
            return new ArrayList<>();
        }
        List<String> lines = Files.readAllLines(taskFile);
        List<String> tasks = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("- [ ]") || trimmed.startsWith("- [x]")) {
                tasks.add(line);
            }
        }
        return tasks;
    }

    public static void addTask(Path subjectFolder, String subjectName, String description) throws IOException {
        Path taskFile = subjectFolder.resolve("Tarefas.md");
        backupFile(taskFile);
        List<String> lines;
        if (!Files.exists(taskFile)) {
            lines = new ArrayList<>();
            lines.add("# Tarefas - " + subjectName);
            lines.add("");
        } else {
            lines = new ArrayList<>(Files.readAllLines(taskFile));
        }
        lines.add("- [ ] " + description);
        Files.write(taskFile, lines);
    }

    public static void updateTaskStatus(Path subjectFolder, String taskLine, String newStatusChar) throws IOException {
        Path taskFile = subjectFolder.resolve("Tarefas.md");
        backupFile(taskFile);
        if (!Files.exists(taskFile)) {
            throw new FileNotFoundException("Arquivo Tarefas.md não encontrado.");
        }
        List<String> lines = Files.readAllLines(taskFile);
        int index = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).equals(taskLine)) {
                index = i;
                break;
            }
        }

        if (index != -1) {
            String newLine = taskLine.replaceFirst("\\[[ x]\\]", "[" + newStatusChar + "]");
            lines.set(index, newLine);
            Files.write(taskFile, lines);
        } else {
            throw new IOException("Tarefa correspondente não encontrada no arquivo.");
        }
    }

    public static List<SubjectJson> loadCurriculum(Path jsonPath) throws IOException {
        String content = Files.readString(jsonPath);
        List<SubjectJson> list = new ArrayList<>();

        Pattern blockPattern = Pattern.compile("\\{[^{}]+\\}");
        Matcher blockMatcher = blockPattern.matcher(content);

        Pattern periodoPat = Pattern.compile("\"Periodo\"\\s*:\\s*(\\d+)");
        Pattern nomePat = Pattern.compile("\"Nome\"\\s*:\\s*\"([^\"]+)\"");
        Pattern chPat = Pattern.compile("\"Ch\"\\s*:\\s*(\\d+)");
        Pattern prereqPat = Pattern.compile("\"Pre_requisito_ou_coorequisito\"\\s*:\\s*(?:\"([^\"]+)\"|null)");
        Pattern idPat = Pattern.compile("\"Id\"\\s*:\\s*\"([^\"]+)\"");

        while (blockMatcher.find()) {
            String block = blockMatcher.group();
            SubjectJson sj = new SubjectJson();

            Matcher m = periodoPat.matcher(block);
            if (m.find())
                sj.Periodo = Integer.parseInt(m.group(1));

            m = nomePat.matcher(block);
            if (m.find())
                sj.Nome = m.group(1);

            m = chPat.matcher(block);
            if (m.find())
                sj.Ch = Integer.parseInt(m.group(1));

            m = prereqPat.matcher(block);
            if (m.find() && m.group(1) != null)
                sj.Pre_requisito_ou_coorequisito = m.group(1);

            m = idPat.matcher(block);
            if (m.find())
                sj.Id = m.group(1);

            list.add(sj);
        }
        return list;
    }

    public static Map<String, Integer> loadAbsences(Path path) throws IOException {
        Map<String, Integer> map = new HashMap<>();
        if (!Files.exists(path)) {
            return map;
        }
        String content = Files.readString(path);
        Pattern p = Pattern.compile("\"([^\"]+)\"\\s*:\\s*(\\d+)");
        Matcher m = p.matcher(content);
        while (m.find()) {
            map.put(m.group(1), Integer.parseInt(m.group(2)));
        }
        return map;
    }

    public static void saveAbsences(Path path, Map<String, Integer> map) throws IOException {
        backupFile(path);
        List<String> lines = new ArrayList<>();
        lines.add("{");
        int count = 0;
        for (Map.Entry<String, Integer> entry : map.entrySet()) {
            count++;
            String comma = (count < map.size()) ? "," : "";
            lines.add("  \"" + entry.getKey() + "\": " + entry.getValue() + comma);
        }
        lines.add("}");
        Files.write(path, lines);
    }

    private static void backupFile(Path targetFile) {
        if (targetFile == null || !Files.exists(targetFile)) {
            return;
        }
        try {
            Path rootDir = Paths.get(System.getProperty("user.dir"));
            Path backupDir = rootDir.resolve(".backups");
            if (!Files.exists(backupDir)) {
                Files.createDirectories(backupDir);
            }

            String fullFilename = targetFile.getFileName().toString();
            final String finalName;
            final String finalExt;
            int dotIndex = fullFilename.lastIndexOf('.');
            if (dotIndex > 0) {
                finalName = fullFilename.substring(0, dotIndex);
                finalExt = fullFilename.substring(dotIndex);
            } else {
                finalName = fullFilename;
                finalExt = "";
            }

            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter
                    .ofPattern("yyyy-MM-dd_HHmmss");
            String timestamp = now.format(formatter);

            Path backupFilePath = backupDir.resolve(finalName + "_" + timestamp + finalExt);
            Files.copy(targetFile, backupFilePath, StandardCopyOption.REPLACE_EXISTING);

            File[] files = backupDir.toFile()
                    .listFiles((dir, fileName) -> fileName.startsWith(finalName + "_") && fileName.endsWith(finalExt));
            if (files != null && files.length > 5) {
                Arrays.sort(files, Comparator.comparing(File::getName));
                int toDelete = files.length - 5;
                for (int i = 0; i < toDelete; i++) {
                    files[i].delete();
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao criar backup de " + targetFile.getFileName() + ": " + e.getMessage());
        }
    }

    private static void ensureGitIgnore(Path rootDir) {
        if (rootDir == null) return;
        Path gitIgnorePath = rootDir.resolve(".gitignore");
        try {
            List<String> lines = new ArrayList<>();
            if (Files.exists(gitIgnorePath)) {
                lines = Files.readAllLines(gitIgnorePath);
            }
            boolean hasConfig = false;
            for (String line : lines) {
                if (line.trim().equals("config/json/config.json") || line.trim().equals("config/config.json") || line.trim().equals("Faculdade/config.json")) {
                    hasConfig = true;
                    break;
                }
            }
            if (!hasConfig) {
                lines.add("config/json/config.json");
                Files.write(gitIgnorePath, lines);
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, String> loadConfig(Path configPath) throws IOException {
        Map<String, String> config = new HashMap<>();
        Path rootDir = configPath.getParent() != null ? configPath.getParent().getParent() : null;
        if (rootDir != null) {
            ensureGitIgnore(rootDir);
        }

        if (!Files.exists(configPath)) {
            if (configPath.getParent() != null && !Files.exists(configPath.getParent())) {
                Files.createDirectories(configPath.getParent());
            }
            config.put("canvas_url", "https://pucminas.instructure.com");
            config.put("canvas_token", "");
            saveConfig(configPath, config);
            return config;
        }

        try {
            String content = Files.readString(configPath);
            Object parsed = JsonParser.parse(content);
            if (parsed instanceof Map) {
                Map<String, Object> rawMap = (Map<String, Object>) parsed;
                for (Map.Entry<String, Object> entry : rawMap.entrySet()) {
                    config.put(entry.getKey(), String.valueOf(entry.getValue()));
                }
            }
        } catch (Exception e) {
            config.put("canvas_url", "https://pucminas.instructure.com");
            config.put("canvas_token", "");
        }
        return config;
    }

    public static void saveConfig(Path configPath, Map<String, String> config) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("{");
        int count = 0;
        for (Map.Entry<String, String> entry : config.entrySet()) {
            count++;
            String comma = (count < config.size()) ? "," : "";
            lines.add("  \"" + entry.getKey() + "\": \"" + entry.getValue() + "\"" + comma);
        }
        lines.add("}");
        Files.write(configPath, lines);
    }

    public static String getCanvasCourseId(Path subjectFolder) {
        Path metaFile = subjectFolder.resolve(".canvas_meta.json");
        if (!Files.exists(metaFile)) return null;
        try {
            String content = Files.readString(metaFile);
            Object parsed = JsonParser.parse(content);
            if (parsed instanceof Map) {
                Map<?, ?> rawMap = (Map<?, ?>) parsed;
                Object val = rawMap.get("canvas_course_id");
                if (val != null) return String.valueOf(val);
            }
        } catch (Exception e) {}
        return null;
    }

    public static void saveCanvasCourseId(Path subjectFolder, String courseId) {
        Path metaFile = subjectFolder.resolve(".canvas_meta.json");
        try {
            Files.writeString(metaFile, "{\n  \"canvas_course_id\": \"" + courseId + "\"\n}");
        } catch (Exception e) {}
    }

    public static String cleanHtml(String html) {
        if (html == null) return "";
        // Remove tags HTML
        String text = html.replaceAll("<[^>]*>", "");
        // Substitui entidades HTML comuns
        text = text.replace("&nbsp;", " ")
                   .replace("&lt;", "<")
                   .replace("&gt;", ">")
                   .replace("&amp;", "&")
                   .replace("&quot;", "\"")
                   .replace("&#39;", "'");
        return text.trim();
    }

    public static void addOrUpdateCanvasTask(Path subjectFolder, String subjectName, String assignmentName, String newDeadlineText, String canvasId, String description) throws IOException {
        Path taskFile = subjectFolder.resolve("Tarefas.md");
        backupFile(taskFile);

        List<String> lines = new ArrayList<>();
        if (Files.exists(taskFile)) {
            lines = new ArrayList<>(Files.readAllLines(taskFile));
        } else {
            lines.add("# Tarefas - " + subjectName);
            lines.add("");
        }

        String commentStr = "<!-- canvas_id: " + canvasId + " -->";
        
        String escapedNotes = "";
        if (description != null) {
            escapedNotes = cleanHtml(description).replace("\r\n", "\\n").replace("\n", "\\n").replace("\r", "\\n");
        }
        String notesComment = !escapedNotes.isEmpty() ? " <!-- google_task_notes: " + escapedNotes + " -->" : "";

        int index = -1;
        String existingLine = null;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).contains(commentStr)) {
                index = i;
                existingLine = lines.get(i);
                break;
            }
        }

        if (index != -1) {
            String status = " ";
            if (existingLine.contains("-[x]") || existingLine.contains("- [x]")) {
                status = "x";
            }
            
            // Preservar google_task_id se houver
            String googleIdStr = "";
            Pattern p = Pattern.compile("<!--\\s*google_task_id:\\s*(\\S+)\\s*-->");
            Matcher m = p.matcher(existingLine);
            if (m.find()) {
                googleIdStr = " <!-- google_task_id: " + m.group(1) + " -->";
            }

            String updatedLine = "- [" + status + "] [Canvas] " + assignmentName + " (" + newDeadlineText + ") " + commentStr + notesComment + googleIdStr;
            if (!existingLine.equals(updatedLine)) {
                lines.set(index, updatedLine);
                Files.write(taskFile, lines);
                System.out.println("🔄 Prazo/Descrição atualizada: \"" + assignmentName + "\" (" + newDeadlineText + ")");
            }
        } else {
            String newLine = "- [ ] [Canvas] " + assignmentName + " (" + newDeadlineText + ") " + commentStr + notesComment;
            lines.add(newLine);
            Files.write(taskFile, lines);
            System.out.println("📥 Importada: \"" + assignmentName + "\" (" + newDeadlineText + ")");
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> loadGoogleTasksMeta(Path subjectFolder) {
        Path metaFile = subjectFolder.resolve(".google_tasks_meta.json");
        Map<String, Object> meta = new HashMap<>();
        if (!Files.exists(metaFile)) {
            meta.put("google_task_list_id", null);
            meta.put("synced_task_ids", new ArrayList<String>());
            return meta;
        }
        try {
            String content = Files.readString(metaFile);
            Object parsed = JsonParser.parse(content);
            if (parsed instanceof Map) {
                Map<String, Object> rawMap = (Map<String, Object>) parsed;
                meta.put("google_task_list_id", rawMap.get("google_task_list_id"));
                Object synced = rawMap.get("synced_task_ids");
                if (synced instanceof List) {
                    List<String> list = new ArrayList<>();
                    for (Object o : (List<?>) synced) {
                        list.add(String.valueOf(o));
                    }
                    meta.put("synced_task_ids", list);
                } else {
                    meta.put("synced_task_ids", new ArrayList<String>());
                }
            }
        } catch (Exception e) {
            meta.put("google_task_list_id", null);
            meta.put("synced_task_ids", new ArrayList<String>());
        }
        return meta;
    }

    public static void saveGoogleTasksMeta(Path subjectFolder, String listId, List<String> syncedTaskIds) {
        Path metaFile = subjectFolder.resolve(".google_tasks_meta.json");
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            if (listId != null) {
                sb.append("  \"google_task_list_id\": \"").append(listId).append("\",\n");
            } else {
                sb.append("  \"google_task_list_id\": null,\n");
            }
            sb.append("  \"synced_task_ids\": [\n");
            for (int i = 0; i < syncedTaskIds.size(); i++) {
                sb.append("    \"").append(syncedTaskIds.get(i)).append("\"");
                if (i < syncedTaskIds.size() - 1) {
                    sb.append(",");
                }
                sb.append("\n");
            }
            sb.append("  ]\n");
            sb.append("}");
            Files.writeString(metaFile, sb.toString());
        } catch (Exception e) {
            System.err.println("Erro ao salvar metadados do Google Tasks: " + e.getMessage());
        }
    }

    public static class GradeItem {
        public String name;
        public double score;
        public double maxScore;
        public boolean extra;
        public boolean reav;
        public String canvasId;

        public GradeItem(String name, double score, double maxScore, boolean extra, boolean reav, String canvasId) {
            this.name = name;
            this.score = score;
            this.maxScore = maxScore;
            this.extra = extra;
            this.reav = reav;
            this.canvasId = canvasId;
        }

        public GradeItem(String name, double score, double maxScore, boolean extra, String canvasId) {
            this(name, score, maxScore, extra, false, canvasId);
        }
    }

    public static class GradeSummary {
        public double totalScore;
        public double totalMax;

        public GradeSummary(double totalScore, double totalMax) {
            this.totalScore = totalScore;
            this.totalMax = totalMax;
        }
    }

    public static GradeSummary getGradeSummary(List<GradeItem> grades) {
        List<GradeItem> standardItems = new ArrayList<>();
        List<GradeItem> extraItems = new ArrayList<>();
        List<GradeItem> reavItems = new ArrayList<>();

        for (GradeItem item : grades) {
            if (item.extra) {
                extraItems.add(item);
            } else if (item.reav) {
                reavItems.add(item);
            } else {
                standardItems.add(item);
            }
        }

        // Encontra o menor item standard
        GradeItem lowestStandard = null;
        for (GradeItem item : standardItems) {
            if (lowestStandard == null || item.score < lowestStandard.score) {
                lowestStandard = item;
            }
        }

        // Encontra a maior reavaliação
        GradeItem bestReav = null;
        for (GradeItem item : reavItems) {
            if (bestReav == null || item.score > bestReav.score) {
                bestReav = item;
            }
        }

        boolean reavApplied = bestReav != null && lowestStandard != null && bestReav.score > lowestStandard.score;

        double extraSum = 0.0;
        for (GradeItem item : extraItems) {
            extraSum += item.score;
        }

        double standardSum = 0.0;
        double totalMax = 0.0;
        for (GradeItem item : standardItems) {
            totalMax += item.maxScore;
            if (item == lowestStandard && reavApplied) {
                standardSum += bestReav.score;
            } else {
                standardSum += item.score;
            }
        }

        double totalScore = standardSum + extraSum;
        return new GradeSummary(totalScore, totalMax);
    }

    @SuppressWarnings("unchecked")
    public static List<GradeItem> loadGrades(Path subjectFolder) {
        List<GradeItem> list = new ArrayList<>();
        Path file = subjectFolder.resolve("notas.json");
        if (!Files.exists(file)) {
            return list;
        }
        try {
            String content = Files.readString(file);
            Object parsed = JsonParser.parse(content);
            if (parsed instanceof Map) {
                Object avs = ((Map<String, Object>) parsed).get("avaliacoes");
                if (avs instanceof List) {
                    for (Object item : (List<?>) avs) {
                        if (item instanceof Map) {
                            Map<String, Object> map = (Map<String, Object>) item;
                            String name = (String) map.get("nome");
                            
                            double score = 0.0;
                            Object sObj = map.get("nota_obtida");
                            if (sObj instanceof Number) {
                                score = ((Number) sObj).doubleValue();
                            }
                            
                            double maxScore = 0.0;
                            Object mObj = map.get("valor_total");
                            if (mObj instanceof Number) {
                                maxScore = ((Number) mObj).doubleValue();
                            }
                            
                            boolean extra = false;
                            Object eObj = map.get("extra");
                            if (eObj instanceof Boolean) {
                                extra = (Boolean) eObj;
                            }

                            boolean reav = false;
                            Object rObj = map.get("reav");
                            if (rObj instanceof Boolean) {
                                reav = (Boolean) rObj;
                            }
                            
                            String canvasId = (String) map.get("canvas_id");
                            
                            list.add(new GradeItem(name, score, maxScore, extra, reav, canvasId));
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao carregar notas de " + subjectFolder + ": " + e.getMessage());
        }
        return list;
    }

    public static void saveGrades(Path subjectFolder, List<GradeItem> grades) {
        Path jsonFile = subjectFolder.resolve("notas.json");
        Path mdFile = subjectFolder.resolve("Notas.md");
        
        try {
            // 1. Salva notas.json
            StringBuilder json = new StringBuilder();
            json.append("{\n  \"avaliacoes\": [\n");
            for (int i = 0; i < grades.size(); i++) {
                GradeItem item = grades.get(i);
                json.append("    {\n");
                json.append("      \"nome\": \"").append(escapeJson(item.name)).append("\",\n");
                json.append("      \"nota_obtida\": ").append(item.score).append(",\n");
                json.append("      \"valor_total\": ").append(item.maxScore).append(",\n");
                json.append("      \"extra\": ").append(item.extra).append(",\n");
                json.append("      \"reav\": ").append(item.reav);
                if (item.canvasId != null) {
                    json.append(",\n      \"canvas_id\": \"").append(item.canvasId).append("\"\n");
                } else {
                    json.append("\n");
                }
                json.append("    }");
                if (i < grades.size() - 1) {
                    json.append(",");
                }
                json.append("\n");
            }
            json.append("  ]\n}");
            Files.writeString(jsonFile, json.toString());
            
            // 2. Gera Notas.md
            StringBuilder md = new StringBuilder();
            md.append("# Notas - ").append(subjectFolder.getFileName().toString()).append("\n\n");
            md.append("| Avaliação | Nota Obtida | Valor Total | Tipo |\n");
            md.append("| :--- | :---: | :---: | :---: |\n");
            
            List<GradeItem> standardItems = new ArrayList<>();
            List<GradeItem> extraItems = new ArrayList<>();
            List<GradeItem> reavItems = new ArrayList<>();
            for (GradeItem item : grades) {
                if (item.extra) {
                    extraItems.add(item);
                } else if (item.reav) {
                    reavItems.add(item);
                } else {
                    standardItems.add(item);
                }
            }

            // Encontra menor standard com nota obtida
            GradeItem lowestStandard = null;
            for (GradeItem item : standardItems) {
                if (lowestStandard == null || item.score < lowestStandard.score) {
                    lowestStandard = item;
                }
            }

            // Encontra maior reav
            GradeItem bestReav = null;
            for (GradeItem item : reavItems) {
                if (bestReav == null || item.score > bestReav.score) {
                    bestReav = item;
                }
            }

            boolean reavApplied = bestReav != null && lowestStandard != null && bestReav.score > lowestStandard.score;

            double totalScore = 0.0;
            double totalMax = 0.0;

            for (GradeItem item : grades) {
                String typeStr = "Padrão";
                if (item.extra) {
                    typeStr = "Ponto Extra";
                } else if (item.reav) {
                    typeStr = "Reavaliação";
                }

                String displayScore = String.format("%.2f", item.score);
                if (item == lowestStandard && reavApplied) {
                    displayScore = String.format("~~%.2f~~ (Subst. por %.2f)", item.score, bestReav.score);
                }

                md.append(String.format("| %s | %s | %.2f | %s |\n", item.name, displayScore, item.maxScore, typeStr));
            }

            GradeSummary summary = getGradeSummary(grades);
            totalScore = summary.totalScore;
            totalMax = summary.totalMax;
            
            md.append(String.format("| **Total** | **%.2f** | **%.2f** | |\n", totalScore, totalMax));
            
            double percentage = totalMax > 0 ? (totalScore / totalMax) * 100.0 : 0.0;
            md.append(String.format("\n**Aproveitamento Acumulado:** %.2f%%\n", percentage));
            
            Files.writeString(mdFile, md.toString());
            
        } catch (Exception e) {
            System.err.println("Erro ao salvar notas de " + subjectFolder + ": " + e.getMessage());
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '\\' || ch == '"') {
                sb.append('\\').append(ch);
            } else if (ch == '\n') {
                sb.append("\\n");
            } else if (ch == '\r') {
                sb.append("\\r");
            } else if (ch == '\t') {
                sb.append("\\t");
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }
}
