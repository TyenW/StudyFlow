package config;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;


public class Main {
    private static final String PLANNING_FILE_NAME = "Planejamento  faculdade.md";
    public static Path rootDir;
    public static Path planningPath;

    // ANSI Colors for premium visual style
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BOLD = "\u001B[1m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_WHITE = "\u001B[37m";

    private static final String ANSI_BRIGHT_RED = "\u001B[91m";
    private static final String ANSI_BRIGHT_YELLOW = "\u001B[93m";
    private static final String ANSI_BRIGHT_GREEN = "\u001B[92m";
    private static final String ANSI_BRIGHT_CYAN = "\u001B[96m";

    private static void initConsole() {
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            try {
                String initCmd = "$sig = '[DllImport(\"kernel32.dll\")] public static extern IntPtr GetStdHandle(int n); " +
                                 "[DllImport(\"kernel32.dll\")] public static extern bool GetConsoleMode(IntPtr h, out uint m); " +
                                 "[DllImport(\"kernel32.dll\")] public static extern bool SetConsoleMode(IntPtr h, uint m);'; " +
                                 "if (-not ([System.Management.Automation.PSTypeName]'Win.Win32').Type) { Add-Type -MemberDefinition $sig -Name Win32 -Namespace Win }; " +
                                 "$hOut = [Win.Win32]::GetStdHandle(-11); $mOut = [uint32]0; [Win.Win32]::GetConsoleMode($hOut, [ref]$mOut); " +
                                 "$mOut = $mOut -bor 4; [Win.Win32]::SetConsoleMode($hOut, $mOut);";
                byte[] bytes = initCmd.getBytes(java.nio.charset.StandardCharsets.UTF_16LE);
                String b64 = java.util.Base64.getEncoder().encodeToString(bytes);
                ProcessBuilder pb = new ProcessBuilder("powershell", "-NoProfile", "-NonInteractive", "-EncodedCommand", b64);
                pb.inheritIO().start().waitFor();
            } catch (Exception e) {
                // Ignore initialization failures, keep going
            }
        }
    }

    public static void run(String[] args) {
        initConsole();

        // Resolve paths
        rootDir = Paths.get(System.getProperty("user.dir"));
        planningPath = rootDir.resolve("Faculdade").resolve(PLANNING_FILE_NAME);
        if (!Files.exists(planningPath)) {
            // fallback absolute path using user.home to make it cross-platform
            String userHome = System.getProperty("user.home");
            rootDir = Paths.get(userHome, "Repositorios", "PedroAnotacoes");
            planningPath = rootDir.resolve("Faculdade").resolve(PLANNING_FILE_NAME);
        }

        if (!Files.exists(planningPath)) {
            System.err.println("Erro: Não foi possível encontrar o arquivo " + PLANNING_FILE_NAME);
            System.err.println("Caminho verificado: " + planningPath.toAbsolutePath());
            System.exit(1);
        }

        if (args.length > 0) {
            handleDirectCommand(args);
            return;
        }

        Scanner scanner = new Scanner(System.in);

        try {
            showDashboard();
            System.out.print("\nPressione " + ANSI_BOLD + "ENTER" + ANSI_RESET + " para abrir o menu...");
            scanner.nextLine();
        } catch (IOException e) {
            // Ignore dashboard errors on startup
        }

        List<String> options = Arrays.asList(
            "📋 Listar planejamento por período",
            "📚 Começar a cursar uma nova matéria",
            "🎓 Concluir uma matéria (arquivar em Faculdade)",
            "🔄 Sincronizar/Criar pastas das matérias cursando",
            "📝 Gerenciar tarefas de uma matéria",
            "🔍 Visão geral de tarefas pendentes (\"O que tenho para hoje?\")",
            "📊 Visão geral de notas",
            "📌 Gerenciar faltas de uma matéria",
            "📊 Gerenciar notas de uma matéria",
            "📥 Importar tarefas do Canvas",
            "🔄 Sincronizar entregas do Canvas",
            "🔗 Vincular matérias ao Canvas",
            "📂 Importar arquivos do Canvas",
            "⚙️ Configurar Google Tasks",
            "🔄 Sincronizar com Google Tasks",
            "⚙️ Controle de Versão (Git / GitHub)",
            "📦 Preparar Projeto para Envio (Zip limpo sem credenciais)",
            "❌ Sair"
        );

        while (true) {
            String title = getContextHeader();
            int choice = InteractiveMenu.select(title, options);
            if (choice == -1 || choice == 17) { // Sair or Esc
                exitProgram(scanner);
                break;
            }
            switch (choice) {
                case 0:
                    listPlanning(scanner);
                    break;
                case 1:
                    startSubject(scanner);
                    break;
                case 2:
                    concludeSubject(scanner);
                    break;
                case 3:
                    syncCursandoFolders(scanner);
                    break;
                case 4:
                    manageSubjectTasks(scanner);
                    break;
                case 5:
                    viewAllPendingTasks(scanner);
                    break;
                case 6:
                    viewGradesOverview(scanner);
                    break;
                case 7:
                    manageAbsences(scanner);
                    break;
                case 8:
                    manageSubjectGrades(scanner);
                    break;
                case 9:
                    importFromCanvas(scanner);
                    break;
                case 10:
                    syncCanvasSubmissions(scanner);
                    break;
                case 11:
                    syncCanvasCoursesMenu(scanner);
                    break;
                case 12:
                    importCanvasFiles(scanner);
                    break;
                case 13:
                    configureGoogleTasks(scanner);
                    break;
                case 14:
                    syncGoogleTasks(scanner);
                    break;
                case 15:
                    gitMenu(scanner);
                    break;
                case 16:
                    prepareCleanZipExport(scanner);
                    break;
            }
        }
    }

    private static void printPlanning() throws IOException {
        List<Subject> subjects = FileManager.loadSubjects(planningPath);
        if (subjects.isEmpty()) {
            System.out.println("\nNenhuma matéria encontrada no planejamento.");
            return;
        }

        System.out.println("\n" + ANSI_CYAN + "==================================================" + ANSI_RESET);
        System.out.println(ANSI_BOLD + ANSI_BLUE + "             📋 PLANILHA DE ESTUDOS" + ANSI_RESET);
        System.out.println(ANSI_CYAN + "==================================================" + ANSI_RESET);

        String lastPeriod = "";
        for (Subject s : subjects) {
            if (!s.period.equals(lastPeriod)) {
                System.out.println("\n" + ANSI_BOLD + ANSI_CYAN + "🔹 " + s.period + ":" + ANSI_RESET);
                lastPeriod = s.period;
            }
            String statusIndicator;
            if (s.statusChar.equals("x")) {
                statusIndicator = ANSI_GREEN + "✅ DONE" + ANSI_RESET;
            } else if (s.statusChar.equals("c")) {
                statusIndicator = ANSI_YELLOW + "⏳ CURS" + ANSI_RESET;
            } else {
                statusIndicator = ANSI_WHITE + "❌ TODO" + ANSI_RESET;
            }
            System.out.printf("  [%s] %s\n", statusIndicator, s.name);
        }
        System.out.println(ANSI_CYAN + "==================================================" + ANSI_RESET);
    }

    private static void listPlanning(Scanner scanner) {
        try {
            printPlanning();
            pressEnterToContinue(scanner);
        } catch (IOException e) {
            System.err.println("Erro ao carregar matérias: " + e.getMessage());
        }
    }

    private static void startSubject(Scanner scanner) {
        try {
            List<Subject> subjects = FileManager.loadSubjects(planningPath);
            List<Subject> todoList = new ArrayList<>();
            for (Subject s : subjects) {
                if (s.statusChar.equals(" ")) {
                    todoList.add(s);
                }
            }

            if (todoList.isEmpty()) {
                System.out.println("\n🎉 Todas as matérias já estão cursando ou concluídas!");
                return;
            }

            List<String> options = new ArrayList<>();
            for (Subject s : todoList) {
                options.add("(" + s.period + ") " + s.name);
            }
            int choice = InteractiveMenu.select("        📚 SELECIONE A MATÉRIA PARA CURSAR", options);
            if (choice == -1) {
                return;
            }
            Subject selected = todoList.get(choice);
            FileManager.updateSubjectStatus(planningPath, selected, "c");

            String folderName = selected.getSanitizedFolderName();
            Path newFolderPath = rootDir.resolve(folderName);
            if (!Files.exists(newFolderPath)) {
                Files.createDirectories(newFolderPath);
            }
            FolderManager.createSubjectSubfolders(newFolderPath, folderName);
            System.out.println("\n✔ Status atualizado, pasta principal e estrutura interna de subpastas criadas/verificadas.");
        } catch (IOException e) {
            System.err.println("Erro ao processar: " + e.getMessage());
        }
    }

    private static void concludeSubject(Scanner scanner) {
        try {
            List<Subject> subjects = FileManager.loadSubjects(planningPath);
            List<Subject> enrolledList = new ArrayList<>();
            for (Subject s : subjects) {
                if (s.statusChar.equals("c")) {
                    enrolledList.add(s);
                }
            }

            if (enrolledList.isEmpty()) {
                System.out.println("\n⏳ Nenhuma matéria cursando atualmente.");
                return;
            }

            List<String> options = new ArrayList<>();
            for (Subject s : enrolledList) {
                options.add("(" + s.period + ") " + s.name);
            }
            int choice = InteractiveMenu.select("       🎓 SELECIONE A MATÉRIA PARA CONCLUIR", options);
            if (choice == -1) {
                return;
            }
            Subject selected = enrolledList.get(choice);
            FileManager.updateSubjectStatus(planningPath, selected, "x");

            String folderName = selected.getSanitizedFolderName();
            Path sourcePath = rootDir.resolve(folderName);
            Path targetPath = rootDir.resolve("Faculdade").resolve(folderName);

            if (Files.exists(sourcePath)) {
                FolderManager.moveDirectory(sourcePath, targetPath);
                System.out.println("\n✔ Status atualizado e pasta arquivada em: " + targetPath.toAbsolutePath());
            } else {
                System.out.println("\n✔ Status atualizado no planejamento (a pasta original não foi encontrada na raiz para arquivamento).");
            }
        } catch (IOException e) {
            System.err.println("Erro ao processar: " + e.getMessage());
        }
    }

    private static void syncCursandoFolders(Scanner scanner) {
        try {
            List<Subject> subjects = FileManager.loadSubjects(planningPath);
            List<Subject> enrolledList = new ArrayList<>();
            for (Subject s : subjects) {
                if (s.statusChar.equals("c")) {
                    enrolledList.add(s);
                }
            }

            if (enrolledList.isEmpty()) {
                System.out.println("\n" + ANSI_YELLOW + "⏳ Nenhuma matéria sendo cursada no momento." + ANSI_RESET);
                return;
            }

            System.out.println("\n" + ANSI_CYAN + "==================================================" + ANSI_RESET);
            System.out.println(ANSI_BOLD + ANSI_BLUE + "   🔄 SINCRONIZANDO PASTAS DAS MATÉRIAS CURSANDO" + ANSI_RESET);
            System.out.println(ANSI_CYAN + "==================================================" + ANSI_RESET);

            for (Subject s : enrolledList) {
                String folderName = s.getSanitizedFolderName();
                Path folderPath = rootDir.resolve(folderName);
                boolean mainCreated = false;
                if (!Files.exists(folderPath)) {
                    Files.createDirectories(folderPath);
                    mainCreated = true;
                }

                boolean subCreated = false;
                String[] subfolders = {
                    "1. Notas de Aula",
                    "2. Exercícios",
                    "3. Trabalhos",
                    "4. Provas",
                    "5. Materiais de Apoio"
                };
                for (String sub : subfolders) {
                    Path subPath = folderPath.resolve(sub);
                    if (!Files.exists(subPath)) {
                        Files.createDirectories(subPath);
                        subCreated = true;
                    }
                }

                if (mainCreated) {
                    System.out.println(" " + ANSI_GREEN + "✔" + ANSI_RESET + " Criada pasta principal e subpastas para: " + ANSI_BOLD + folderName + ANSI_RESET);
                } else if (subCreated) {
                    System.out.println(" " + ANSI_GREEN + "✔" + ANSI_RESET + " Subpastas ausentes recriadas para: " + ANSI_BOLD + folderName + ANSI_RESET);
                } else {
                    System.out.println(" • Pastas já em sincronia para: " + folderName);
                }
            }

            System.out.println(ANSI_CYAN + "==================================================" + ANSI_RESET);
            System.out.println(ANSI_BOLD + ANSI_GREEN + " Sincronização concluída." + ANSI_RESET);
            System.out.println(ANSI_CYAN + "==================================================" + ANSI_RESET);
            pressEnterToContinue(scanner);
        } catch (IOException e) {
            System.err.println("Erro durante a sincronização: " + e.getMessage());
        }
    }

    private static void manageSubjectTasks(Scanner scanner) {
        try {
            List<Subject> subjects = FileManager.loadSubjects(planningPath);
            List<Subject> enrolledList = new ArrayList<>();
            for (Subject s : subjects) {
                if (s.statusChar.equals("c")) {
                    enrolledList.add(s);
                }
            }

            if (enrolledList.isEmpty()) {
                System.out.println("\n⏳ Nenhuma matéria cursando no momento.");
                return;
            }

            List<String> options = new ArrayList<>();
            for (Subject s : enrolledList) {
                options.add("(" + s.period + ") " + s.name);
            }
            int choice = InteractiveMenu.select("       📝 SELECIONE A MATÉRIA PARA GERENCIAR TAREFAS", options);
            if (choice == -1) {
                return;
            }
            Subject selected = enrolledList.get(choice);
            runSubjectTaskMenu(scanner, selected);

        } catch (IOException e) {
            System.err.println("Erro ao carregar matérias: " + e.getMessage());
        }
    }

    private static void runSubjectTaskMenu(Scanner scanner, Subject subject) {
        String folderName = subject.getSanitizedFolderName();
        Path folderPath = rootDir.resolve(folderName);

        if (!Files.exists(folderPath)) {
            System.out.println("\n❌ A pasta da matéria não existe na raiz. Sincronize primeiro (opção 4).");
            return;
        }

        List<String> options = Arrays.asList(
            "📋 Listar tarefas existentes",
            "➕ Adicionar nova tarefa",
            "✔ Concluir/Marcar tarefa como feita",
            "❌ Deletar tarefas (1 ou mais)",
            "⬅ Voltar ao menu anterior"
        );

        while (true) {
            String title = "📝 TAREFAS DE: " + folderName.toUpperCase();
            int choice = InteractiveMenu.select(title, options);
            if (choice == -1 || choice == 4) {
                break;
            }
            try {
                if (choice == 0) {
                    listTasksOfSubject(folderPath);
                    pressEnterToContinue(scanner);
                } else if (choice == 1) {
                    addTaskToSubject(scanner, folderPath, folderName);
                    pressEnterToContinue(scanner);
                } else if (choice == 2) {
                    concludeTaskOfSubject(scanner, folderPath);
                    pressEnterToContinue(scanner);
                } else if (choice == 3) {
                    deleteTasksOfSubject(scanner, folderPath);
                    pressEnterToContinue(scanner);
                }
            } catch (IOException e) {
                System.err.println("Erro ao processar tarefas: " + e.getMessage());
                pressEnterToContinue(scanner);
            }
        }
    }

    private static void listTasksOfSubject(Path subjectFolder) throws IOException {
        List<String> tasks = FileManager.loadTasks(subjectFolder);
        if (tasks.isEmpty()) {
            System.out.println("\n🎉 Nenhuma tarefa registrada nesta matéria.");
            return;
        }

        System.out.println("\n--- Lista de Tarefas ---");
        for (String task : tasks) {
            System.out.println("  " + task);
        }
    }

    private static void addTaskToSubject(Scanner scanner, Path subjectFolder, String subjectName) throws IOException {
        System.out.print("\nDigite a descrição da nova tarefa: ");
        String description = scanner.nextLine().trim();
        if (description.isEmpty()) {
            System.out.println("❌ A descrição não pode ser vazia.");
            return;
        }

        FileManager.addTask(subjectFolder, subjectName, description);
        System.out.println("✔ Tarefa adicionada com sucesso!");
    }

    private static void concludeTaskOfSubject(Scanner scanner, Path subjectFolder) throws IOException {
        List<String> tasks = FileManager.loadTasks(subjectFolder);
        List<String> pendingTasks = new ArrayList<>();
        for (String task : tasks) {
            if (task.trim().startsWith("- [ ]")) {
                pendingTasks.add(task);
            }
        }

        if (pendingTasks.isEmpty()) {
            System.out.println("\n🎉 Nenhuma tarefa pendente nesta matéria.");
            return;
        }

        List<String> options = new ArrayList<>();
        for (String pt : pendingTasks) {
            String cleanTitle = pt.replaceFirst("^\\s*-\\s*\\[\\s*\\]\\s*", "");
            cleanTitle = cleanTitle.replaceAll("<!--\\s*google_task_id:\\s*\\S+\\s*-->", "");
            cleanTitle = cleanTitle.replaceAll("<!--\\s*canvas_id:\\s*\\S+\\s*-->", "");
            cleanTitle = cleanTitle.replaceAll("<!--\\s*google_task_notes:\\s*.*?\\s*-->", "");
            cleanTitle = cleanTitle.trim();
            options.add(cleanTitle);
        }

        int choice = InteractiveMenu.select("Selecione a tarefa para Concluir:", options);
        if (choice == -1) return;

        String selectedTask = pendingTasks.get(choice);
        FileManager.updateTaskStatus(subjectFolder, selectedTask, "x");
        System.out.println("✔ Tarefa marcada como concluída!");
    }

    private static void deleteTasksOfSubject(Scanner scanner, Path subjectFolder) throws IOException {
        Path taskFile = subjectFolder.resolve("Tarefas.md");
        if (!Files.exists(taskFile)) {
            System.out.println("\n⚠️ Nenhuma tarefa cadastrada para esta matéria.");
            return;
        }

        List<String> allLines = new ArrayList<>(Files.readAllLines(taskFile));
        List<String> taskLines = new ArrayList<>();
        List<Integer> taskLineIndices = new ArrayList<>();

        for (int i = 0; i < allLines.size(); i++) {
            String line = allLines.get(i);
            String trimmed = line.trim();
            if (trimmed.startsWith("- [ ]") || trimmed.startsWith("- [x]") || trimmed.startsWith("- [X]")) {
                taskLines.add(line);
                taskLineIndices.add(i);
            }
        }

        if (taskLines.isEmpty()) {
            System.out.println("\n🎉 Nenhuma tarefa cadastrada nesta matéria.");
            return;
        }

        List<String> options = new ArrayList<>();
        for (String line : taskLines) {
            String cleanTitle = line.replaceFirst("^\\s*-\\s*\\[[ xX]\\]\\s*", "");
            cleanTitle = cleanTitle.replaceAll("<!--\\s*google_task_id:\\s*\\S+\\s*-->", "");
            cleanTitle = cleanTitle.replaceAll("<!--\\s*canvas_id:\\s*\\S+\\s*-->", "");
            cleanTitle = cleanTitle.replaceAll("<!--\\s*google_task_notes:\\s*.*?\\s*-->", "");
            cleanTitle = cleanTitle.trim();
            
            String status = (line.trim().startsWith("- [ ]") ? "[ ] " : "[x] ");
            options.add(status + cleanTitle);
        }

        List<Integer> selectedChoices = InteractiveMenu.selectMultiple("Selecione as tarefas para DELETAR (Espaço/Seta Direita para marcar):", options);
        if (selectedChoices == null || selectedChoices.isEmpty()) {
            System.out.println("❌ Nenhuma tarefa selecionada. Operação cancelada.");
            return;
        }

        // Tenta buscar tokens do Google Tasks para deletar na nuvem também
        Path configPath = getConfigFilePath();
        Map<String, String> config = null;
        try {
            config = FileManager.loadConfig(configPath);
        } catch (Exception e) {}
        
        String accessToken = null;
        String taskListId = null;
        if (config != null) {
            String clientId = config.get("google_client_id");
            String clientSecret = config.get("google_client_secret");
            String refreshToken = config.get("google_refresh_token");
            if (clientId != null && clientSecret != null && refreshToken != null) {
                try {
                    accessToken = GoogleTasksManager.getAccessToken(config, configPath);
                    Map<String, Object> meta = FileManager.loadGoogleTasksMeta(subjectFolder);
                    taskListId = (String) meta.get("google_task_list_id");
                } catch (Exception e) {}
            }
        }

        // Deleta as linhas de trás para frente para não alterar os índices
        Collections.sort(selectedChoices, Collections.reverseOrder());
        int deletedCount = 0;
        for (int choiceIdx : selectedChoices) {
            int originalLineIdx = taskLineIndices.get(choiceIdx);
            String lineToDelete = allLines.get(originalLineIdx);
            
            Pattern p = Pattern.compile("<!--\\s*google_task_id:\\s*(\\S+)\\s*-->");
            Matcher m = p.matcher(lineToDelete);
            if (m.find() && accessToken != null && taskListId != null) {
                String googleTaskId = m.group(1);
                try {
                    GoogleTasksManager.deleteTask(accessToken, taskListId, googleTaskId);
                } catch (Exception e) {
                    System.err.println("Erro ao deletar no Google Tasks: " + e.getMessage());
                }
            }

            allLines.remove(originalLineIdx);
            deletedCount++;
        }

        Files.write(taskFile, allLines);
        System.out.println("✔ " + deletedCount + " tarefa(s) deletada(s) com sucesso!");
    }

    private static void showDashboard() throws IOException {
        List<Subject> subjects = FileManager.loadSubjects(planningPath);
        List<Subject> enrolledList = new ArrayList<>();
        for (Subject s : subjects) {
            if (s.statusChar.equals("c")) {
                enrolledList.add(s);
            }
        }

        if (enrolledList.isEmpty()) {
            System.out.println("\n" + ANSI_YELLOW + "⏳ Nenhuma matéria sendo cursada no momento." + ANSI_RESET);
            return;
        }

        System.out.println("\n" + ANSI_CYAN + "==================================================" + ANSI_RESET);
        System.out.println(ANSI_BOLD + ANSI_BLUE + "   🔍 VISÃO GERAL DE TAREFAS PENDENTES" + ANSI_RESET);
        System.out.println(ANSI_CYAN + "==================================================" + ANSI_RESET);

        boolean hasAnyPending = false;
        for (Subject s : enrolledList) {
            String folderName = s.getSanitizedFolderName();
            Path folderPath = rootDir.resolve(folderName);

            if (Files.exists(folderPath)) {
                List<String> tasks = FileManager.loadTasks(folderPath);
                List<String> pending = new ArrayList<>();
                for (String task : tasks) {
                    if (task.trim().startsWith("- [ ]")) {
                        pending.add(task);
                    }
                }

                if (!pending.isEmpty()) {
                    hasAnyPending = true;
                    System.out.println(ANSI_BOLD + ANSI_CYAN + "🔹 " + folderName + ":" + ANSI_RESET);
                    for (String p : pending) {
                        String display = p.replaceFirst("^\\s*-\\s*\\[\\s*\\]\\s*", "");
                        System.out.println("  " + ANSI_YELLOW + "[ ]" + ANSI_RESET + " " + display);
                    }
                    System.out.println();
                }
            }
        }

        if (!hasAnyPending) {
            System.out.println(ANSI_BOLD + ANSI_GREEN + "🎉 Parabéns! Você não tem nenhuma tarefa pendente hoje." + ANSI_RESET);
        }
        System.out.println(ANSI_CYAN + "==================================================" + ANSI_RESET);
    }

    private static void viewGradesOverview(Scanner scanner) {
        List<Subject> enrolledList;
        try {
            List<Subject> subjects = FileManager.loadSubjects(planningPath);
            enrolledList = new ArrayList<>();
            for (Subject s : subjects) {
                if (s.statusChar.equals("c")) {
                    enrolledList.add(s);
                }
            }
        } catch (IOException e) {
            System.out.println(ANSI_RED + "❌ Erro ao carregar matérias: " + e.getMessage() + ANSI_RESET);
            pressEnterToContinue(scanner);
            return;
        }

        if (enrolledList.isEmpty()) {
            System.out.println(ANSI_YELLOW + "⏳ Nenhuma matéria cursando no momento." + ANSI_RESET);
            pressEnterToContinue(scanner);
            return;
        }

        List<String> options = Arrays.asList(
            "📊 Resumo simplificado por matéria",
            "📋 Visão detalhada de todas as notas",
            "⬅ Voltar"
        );

        while (true) {
            int choice = InteractiveMenu.select("📊 VISÃO GERAL DE NOTAS", options);
            if (choice == -1 || choice == 2) {
                break;
            }
            if (choice == 0) {
                printSimpleGradesOverview(enrolledList);
                pressEnterToContinue(scanner);
            } else if (choice == 1) {
                printDetailedGradesOverview(enrolledList);
                pressEnterToContinue(scanner);
            }
        }
    }

    private static void printSimpleGradesOverview(List<Subject> enrolledList) {
        System.out.println("\n==================================================");
        System.out.println(ANSI_BOLD + ANSI_BLUE + "        📊 RESUMO ACUMULADO POR MATÉRIA" + ANSI_RESET);
        System.out.println("==================================================");

        for (Subject s : enrolledList) {
            Path folder = rootDir.resolve(s.getSanitizedFolderName());
            List<FileManager.GradeItem> grades = FileManager.loadGrades(folder);
            FileManager.GradeSummary summary = FileManager.getGradeSummary(grades);
            double totalScore = summary.totalScore;
            double totalMax = summary.totalMax;
            
            System.out.printf("🔹 %-50s: %.2f / %.2f\n", s.name, totalScore, totalMax);
        }
        System.out.println("==================================================");
    }

    private static void printDetailedGradesOverview(List<Subject> enrolledList) {
        System.out.println("\n==================================================");
        System.out.println(ANSI_BOLD + ANSI_BLUE + "      📋 VISÃO DETALHADA DE TODAS AS NOTAS" + ANSI_RESET);
        System.out.println("==================================================");

        for (Subject s : enrolledList) {
            Path folder = rootDir.resolve(s.getSanitizedFolderName());
            List<FileManager.GradeItem> grades = FileManager.loadGrades(folder);
            FileManager.GradeSummary summary = FileManager.getGradeSummary(grades);
            double totalScore = summary.totalScore;
            double totalMax = summary.totalMax;

            System.out.printf("🔹 %-50s: %.2f / %.2f\n", s.name, totalScore, totalMax);
            
            if (grades.isEmpty()) {
                System.out.println("  (Nenhuma nota cadastrada)");
            } else {
                for (FileManager.GradeItem item : grades) {
                    String typeStr = "";
                    if (item.extra) {
                        typeStr = " [Ponto Extra]";
                    } else if (item.reav) {
                        typeStr = " [Reavaliação]";
                    }
                    System.out.printf("  • %-35s %6.2f / %-6.2f%s\n", 
                        item.name.length() > 35 ? item.name.substring(0, 32) + "..." : item.name,
                        item.score, item.maxScore, typeStr);
                }
            }
            System.out.println();
        }
        System.out.println("==================================================");
    }

    private static void viewAllPendingTasks(Scanner scanner) {
        try {
            showDashboard();
            pressEnterToContinue(scanner);
        } catch (IOException e) {
            System.err.println("Erro ao buscar tarefas: " + e.getMessage());
        }
    }

    private static void manageAbsences(Scanner scanner) {
        try {
            Path jsonPath = getMateriasJsonPath();
            Path absencesPath = rootDir.resolve("Faculdade").resolve("faltas.json");

            if (!Files.exists(jsonPath)) {
                System.out.println("\n" + ANSI_RED + "❌ Arquivo materias.json não encontrado na raiz." + ANSI_RESET);
                return;
            }

            List<SubjectJson> jsonList = FileManager.loadCurriculum(jsonPath);
            Map<String, Integer> absencesMap = FileManager.loadAbsences(absencesPath);

            List<Subject> subjects = FileManager.loadSubjects(planningPath);
            List<Subject> enrolledList = new ArrayList<>();
            for (Subject s : subjects) {
                if (s.statusChar.equals("c")) {
                    enrolledList.add(s);
                }
            }

            if (enrolledList.isEmpty()) {
                System.out.println("\n" + ANSI_YELLOW + "⏳ Nenhuma matéria cursando atualmente." + ANSI_RESET);
                return;
            }

            while (true) {
                List<SubjectJson> matchedList = new ArrayList<>();
                List<String> options = new ArrayList<>();

                for (int i = 0; i < enrolledList.size(); i++) {
                    Subject s = enrolledList.get(i);
                    SubjectJson sj = matchSubject(s.name, jsonList);
                    matchedList.add(sj);

                    String key = (sj != null) ? sj.Id : s.getSanitizedFolderName();
                    int currentAbsences = absencesMap.getOrDefault(key, 0);

                    if (sj != null) {
                        int maxAbsences = (int) Math.ceil((sj.Ch * 0.25) / 2.0) - 1;
                        int remaining = maxAbsences - currentAbsences;
                        double percentage = ((double) currentAbsences / maxAbsences) * 100;

                        String progressBar = getProgressBar(currentAbsences, maxAbsences);
                        String alertColor = ANSI_GREEN;

                        if (remaining < 0) {
                            alertColor = ANSI_BOLD + ANSI_RED;
                        } else if (remaining == 0 || percentage >= 80) {
                            alertColor = ANSI_BRIGHT_RED;
                        } else if (percentage >= 50) {
                            alertColor = ANSI_YELLOW;
                        }

                        options.add(String.format("%s: %s%d/%d faltas (Restam %d)%s %s - %.1f%%",
                            s.name, alertColor, currentAbsences, maxAbsences, remaining, ANSI_RESET, progressBar, percentage));
                    } else {
                        options.add(String.format("%s: %d faltas (Sem limite em materias.json)", s.name, currentAbsences));
                    }
                }

                int choice = InteractiveMenu.select("            📌 GERENCIAMENTO DE FALTAS", options);
                if (choice == -1) {
                    break;
                }

                Subject selected = enrolledList.get(choice);
                SubjectJson selectedJson = matchedList.get(choice);
                String key = (selectedJson != null) ? selectedJson.Id : selected.getSanitizedFolderName();
                int current = absencesMap.getOrDefault(key, 0);

                int max = (selectedJson != null) ? (int) Math.ceil((selectedJson.Ch * 0.25) / 2.0) - 1 : 999;

                while (true) {
                    System.out.println("\n----------------------------------------");
                    System.out.println(ANSI_BOLD + " Matéria: " + selected.name + ANSI_RESET);
                    System.out.println(" Faltas atuais: " + current + (selectedJson != null ? " (Limite Máximo Seguro: " + max + ")" : ""));
                    System.out.println("----------------------------------------");
                    System.out.println(" [" + ANSI_GREEN + "1" + ANSI_RESET + "] Adicionar falta (+1)");
                    System.out.println(" [" + ANSI_GREEN + "2" + ANSI_RESET + "] Retirar falta (-1)");
                    System.out.println(" [" + ANSI_GREEN + "3" + ANSI_RESET + "] Definir valor personalizado");
                    System.out.println(" [" + ANSI_GREEN + "4" + ANSI_RESET + "] Voltar");
                    System.out.println("----------------------------------------");
                    System.out.print("Escolha uma opção: ");

                    String opt = scanner.nextLine().trim();
                    if (opt.equals("1")) {
                        current++;
                        absencesMap.put(key, current);
                        FileManager.saveAbsences(absencesPath, absencesMap);
                        System.out.println(ANSI_GREEN + "✔ Falta adicionada!" + ANSI_RESET);
                        checkAbsenceAlert(current, max);
                    } else if (opt.equals("2")) {
                        if (current > 0) {
                            current--;
                            absencesMap.put(key, current);
                            FileManager.saveAbsences(absencesPath, absencesMap);
                            System.out.println(ANSI_GREEN + "✔ Falta removida!" + ANSI_RESET);
                            checkAbsenceAlert(current, max);
                        } else {
                            System.out.println(" • Nenhuma falta registrada para remover.");
                        }
                    } else if (opt.equals("3")) {
                        System.out.print("Digite o novo valor de faltas: ");
                        try {
                            int newVal = Integer.parseInt(scanner.nextLine().trim());
                            if (newVal >= 0) {
                                current = newVal;
                                absencesMap.put(key, current);
                                FileManager.saveAbsences(absencesPath, absencesMap);
                                System.out.println(ANSI_GREEN + "✔ Faltas atualizadas!" + ANSI_RESET);
                                checkAbsenceAlert(current, max);
                            } else {
                                System.out.println(ANSI_RED + "❌ Valor inválido. Deve ser maior ou igual a 0." + ANSI_RESET);
                            }
                        } catch (NumberFormatException e) {
                            System.out.println(ANSI_RED + "❌ Valor não numérico inválido." + ANSI_RESET);
                        }
                    } else if (opt.equals("4")) {
                        break;
                    } else {
                        System.out.println(ANSI_RED + "❌ Opção inválida." + ANSI_RESET);
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("Erro ao gerenciar faltas: " + e.getMessage());
        }
    }

    private static void manageSubjectGrades(Scanner scanner) {
        try {
            List<Subject> subjects = FileManager.loadSubjects(planningPath);
            List<Subject> enrolledList = new ArrayList<>();
            for (Subject s : subjects) {
                if (s.statusChar.equals("c")) {
                    enrolledList.add(s);
                }
            }

            if (enrolledList.isEmpty()) {
                System.out.println("\n" + ANSI_YELLOW + "⏳ Nenhuma matéria cursando no momento." + ANSI_RESET);
                pressEnterToContinue(scanner);
                return;
            }

            List<String> options = new ArrayList<>();
            for (Subject s : enrolledList) {
                options.add("(" + s.period + ") " + s.name);
            }
            int choice = InteractiveMenu.select("       📊 SELECIONE A MATÉRIA PARA GERENCIAR NOTAS", options);
            if (choice == -1) {
                return;
            }
            Subject selected = enrolledList.get(choice);
            runSubjectGradeMenu(scanner, selected);

        } catch (IOException e) {
            System.err.println("Erro ao carregar matérias: " + e.getMessage());
            pressEnterToContinue(scanner);
        }
    }

    private static void runSubjectGradeMenu(Scanner scanner, Subject subject) {
        String folderName = subject.getSanitizedFolderName();
        Path folderPath = rootDir.resolve(folderName);

        if (!Files.exists(folderPath)) {
            System.out.println("\n❌ A pasta da matéria não existe na raiz. Sincronize primeiro (opção 4).");
            pressEnterToContinue(scanner);
            return;
        }

        List<String> options = Arrays.asList(
            "📋 Visualizar notas e resumo acumulativo",
            "➕ Adicionar nova nota/avaliação",
            "✏️ Editar nota/avaliação",
            "❌ Remover nota/avaliação",
            "🔄 Importar/Sincronizar notas do Canvas",
            "⬅ Voltar ao menu anterior"
        );

        while (true) {
            String title = "📊 NOTAS DE: " + folderName.toUpperCase();
            int choice = InteractiveMenu.select(title, options);
            if (choice == -1 || choice == 5) {
                break;
            }
            if (choice == 0) {
                viewGradesOfSubject(folderPath);
                pressEnterToContinue(scanner);
            } else if (choice == 1) {
                addGradeToSubject(scanner, folderPath);
                pressEnterToContinue(scanner);
            } else if (choice == 2) {
                editGradeOfSubject(scanner, folderPath);
                pressEnterToContinue(scanner);
            } else if (choice == 3) {
                removeGradeFromSubject(scanner, folderPath);
                pressEnterToContinue(scanner);
            } else if (choice == 4) {
                syncGradesFromCanvas(scanner, subject, folderPath);
                pressEnterToContinue(scanner);
            }
        }
    }

    private static void viewGradesOfSubject(Path subjectFolder) {
        List<FileManager.GradeItem> grades = FileManager.loadGrades(subjectFolder);
        if (grades.isEmpty()) {
            System.out.println("\n⚠️ Nenhuma nota cadastrada para esta matéria.");
            return;
        }

        System.out.println("\n--------------------------------------------------");
        System.out.printf("%-20s | %-15s | %-10s | %-12s\n", "Avaliação", "Nota Obt.", "Valor Max.", "Tipo");
        System.out.println("--------------------------------------------------");
        
        List<FileManager.GradeItem> standardItems = new ArrayList<>();
        List<FileManager.GradeItem> extraItems = new ArrayList<>();
        List<FileManager.GradeItem> reavItems = new ArrayList<>();
        for (FileManager.GradeItem item : grades) {
            if (item.extra) {
                extraItems.add(item);
            } else if (item.reav) {
                reavItems.add(item);
            } else {
                standardItems.add(item);
            }
        }

        FileManager.GradeItem lowestStandard = null;
        for (FileManager.GradeItem item : standardItems) {
            if (lowestStandard == null || item.score < lowestStandard.score) {
                lowestStandard = item;
            }
        }

        FileManager.GradeItem bestReav = null;
        for (FileManager.GradeItem item : reavItems) {
            if (bestReav == null || item.score > bestReav.score) {
                bestReav = item;
            }
        }

        boolean reavApplied = bestReav != null && lowestStandard != null && bestReav.score > lowestStandard.score;

        for (FileManager.GradeItem item : grades) {
            String typeStr = "Padrão";
            if (item.extra) {
                typeStr = "Ponto Extra";
            } else if (item.reav) {
                typeStr = "Reavaliação";
            }

            String displayScore = String.format("%.2f", item.score);
            if (item == lowestStandard && reavApplied) {
                displayScore = String.format("~~%.2f~~ (-> %.2f)", item.score, bestReav.score);
            }

            System.out.printf("%-20s | %-15s | %-10.2f | %-12s\n", 
                item.name.length() > 20 ? item.name.substring(0, 17) + "..." : item.name,
                displayScore, item.maxScore, typeStr);
        }
        System.out.println("--------------------------------------------------");
        
        FileManager.GradeSummary summary = FileManager.getGradeSummary(grades);
        double totalScore = summary.totalScore;
        double totalMax = summary.totalMax;

        System.out.printf("%-20s | %-15.2f | %-10.2f |\n", "TOTAL", totalScore, totalMax);
        System.out.println("--------------------------------------------------");
        
        double percentage = totalMax > 0 ? (totalScore / totalMax) * 100.0 : 0.0;
        System.out.printf("Aproveitamento das atividades avaliadas: %.2f%%\n", percentage);
        if (totalScore >= 60.0) {
            System.out.println(ANSI_GREEN + "🎉 Situação: Aprovado! (Nota >= 60.0)" + ANSI_RESET);
        } else {
            double remaining = 60.0 - totalScore;
            System.out.printf(ANSI_YELLOW + "⏳ Situação: Cursando (Faltam %.2f pontos para aprovação - meta 60.0)\n" + ANSI_RESET, remaining);
        }
    }

    private static void addGradeToSubject(Scanner scanner, Path subjectFolder) {
        System.out.println("\n➕ ADICIONAR NOVA NOTA");
        System.out.print("Nome da avaliação: ");
        String name = scanner.nextLine().trim();
        if (name.isEmpty()) return;

        double score = 0.0;
        System.out.print("Nota obtida: ");
        try {
            score = Double.parseDouble(scanner.nextLine().trim());
        } catch (Exception e) {
            System.out.println(ANSI_RED + "❌ Valor inválido. Abortado." + ANSI_RESET);
            return;
        }

        double maxScore = 0.0;
        System.out.print("Valor total: ");
        try {
            maxScore = Double.parseDouble(scanner.nextLine().trim());
        } catch (Exception e) {
            System.out.println(ANSI_RED + "❌ Valor inválido. Abortado." + ANSI_RESET);
            return;
        }

        System.out.print("É ponto extra? (s/N): ");
        String extraAns = scanner.nextLine().trim().toLowerCase();
        boolean extra = extraAns.equals("s") || extraAns.equals("sim");

        boolean reav = false;
        if (!extra) {
            System.out.print("É reavaliação (recuperação)? (s/N): ");
            String reavAns = scanner.nextLine().trim().toLowerCase();
            reav = reavAns.equals("s") || reavAns.equals("sim");
        }

        List<FileManager.GradeItem> grades = FileManager.loadGrades(subjectFolder);
        grades.add(new FileManager.GradeItem(name, score, maxScore, extra, reav, null));
        FileManager.saveGrades(subjectFolder, grades);

        System.out.println(ANSI_GREEN + "✔ Nota adicionada com sucesso!" + ANSI_RESET);
    }

    private static void editGradeOfSubject(Scanner scanner, Path subjectFolder) {
        List<FileManager.GradeItem> grades = FileManager.loadGrades(subjectFolder);
        if (grades.isEmpty()) {
            System.out.println("\n⚠️ Nenhuma nota cadastrada.");
            return;
        }

        List<String> options = new ArrayList<>();
        for (FileManager.GradeItem item : grades) {
            String typeStr = "";
            if (item.extra) {
                typeStr = " [Extra]";
            } else if (item.reav) {
                typeStr = " [Reavaliação]";
            }
            options.add(String.format("%s (Nota: %.2f/%.2f)%s", item.name, item.score, item.maxScore, typeStr));
        }

        int choice = InteractiveMenu.select("Selecione a nota para editar:", options);
        if (choice == -1) return;

        FileManager.GradeItem selected = grades.get(choice);
        System.out.println("\n✏️ EDITAR: " + selected.name);

        System.out.print("Novo nome (deixe vazio para manter \"" + selected.name + "\"): ");
        String name = scanner.nextLine().trim();
        if (!name.isEmpty()) {
            selected.name = name;
        }

        System.out.print("Nova nota obtida (deixe vazio para manter " + selected.score + "): ");
        String scoreStr = scanner.nextLine().trim();
        if (!scoreStr.isEmpty()) {
            try {
                selected.score = Double.parseDouble(scoreStr);
            } catch (Exception e) {
                System.out.println(ANSI_RED + "❌ Valor inválido. Mantendo anterior." + ANSI_RESET);
            }
        }

        System.out.print("Novo valor total (deixe vazio para manter " + selected.maxScore + "): ");
        String maxStr = scanner.nextLine().trim();
        if (!maxStr.isEmpty()) {
            try {
                selected.maxScore = Double.parseDouble(maxStr);
            } catch (Exception e) {
                System.out.println(ANSI_RED + "❌ Valor inválido. Mantendo anterior." + ANSI_RESET);
            }
        }

        String currentExtra = selected.extra ? "Sim" : "Não";
        System.out.print("É ponto extra? Atual: " + currentExtra + " (s/N): ");
        String extraAns = scanner.nextLine().trim().toLowerCase();
        if (!extraAns.isEmpty()) {
            selected.extra = extraAns.equals("s") || extraAns.equals("sim");
        }

        String currentReav = selected.reav ? "Sim" : "Não";
        System.out.print("É reavaliação? Atual: " + currentReav + " (s/N): ");
        String reavAns = scanner.nextLine().trim().toLowerCase();
        if (!reavAns.isEmpty()) {
            selected.reav = reavAns.equals("s") || reavAns.equals("sim");
        }

        FileManager.saveGrades(subjectFolder, grades);
        System.out.println(ANSI_GREEN + "✔ Nota atualizada com sucesso!" + ANSI_RESET);
    }

    private static void removeGradeFromSubject(Scanner scanner, Path subjectFolder) {
        List<FileManager.GradeItem> grades = FileManager.loadGrades(subjectFolder);
        if (grades.isEmpty()) {
            System.out.println("\n⚠️ Nenhuma nota cadastrada.");
            return;
        }

        List<String> options = new ArrayList<>();
        for (FileManager.GradeItem item : grades) {
            String typeStr = "";
            if (item.extra) {
                typeStr = " [Extra]";
            } else if (item.reav) {
                typeStr = " [Reavaliação]";
            }
            options.add(String.format("%s (Nota: %.2f/%.2f)%s", item.name, item.score, item.maxScore, typeStr));
        }

        int choice = InteractiveMenu.select("Selecione a nota para REMOVER:", options);
        if (choice == -1) return;

        FileManager.GradeItem selected = grades.get(choice);
        System.out.print("Tem certeza que deseja remover a nota \"" + selected.name + "\"? (s/N): ");
        String confirm = scanner.nextLine().trim().toLowerCase();
        if (confirm.equals("s") || confirm.equals("sim")) {
            grades.remove(choice);
            FileManager.saveGrades(subjectFolder, grades);
            System.out.println(ANSI_GREEN + "✔ Nota removida com sucesso!" + ANSI_RESET);
        }
    }

    private static void syncGradesFromCanvas(Scanner scanner, Subject subject, Path subjectFolder) {
        String courseId = FileManager.getCanvasCourseId(subjectFolder);
        if (courseId == null || courseId.isEmpty()) {
            System.out.println(ANSI_RED + "\n❌ Esta matéria não está vinculada a nenhum curso do Canvas." + ANSI_RESET);
            System.out.println("Use a opção de importar tarefas primeiro para vincular.");
            return;
        }

        Path configPath = getConfigFilePath();
        Map<String, String> config = null;
        try {
            config = FileManager.loadConfig(configPath);
        } catch (IOException e) {
            System.out.println(ANSI_RED + "❌ Falha ao carregar Faculdade/config.json: " + e.getMessage() + ANSI_RESET);
            return;
        }

        String canvasUrl = config.get("canvas_url");
        String token = config.get("canvas_token");
        if (canvasUrl == null || canvasUrl.isEmpty() || token == null || token.isEmpty()) {
            System.out.println(ANSI_RED + "❌ Credenciais do Canvas não configuradas." + ANSI_RESET);
            return;
        }

        System.out.println("\n📡 Conectando ao Canvas para sincronizar notas...");
        try {
            List<Map<String, Object>> assignments = CanvasManager.getAssignments(canvasUrl, token, courseId);
            if (assignments.isEmpty()) {
                System.out.println("Nenhuma atividade encontrada no Canvas.");
                return;
            }

            List<FileManager.GradeItem> localGrades = FileManager.loadGrades(subjectFolder);
            boolean localModified = false;
            int countNew = 0;
            int countUpdated = 0;

            for (Map<String, Object> a : assignments) {
                Object submissionObj = a.get("submission");
                double score = 0.0;
                if (submissionObj instanceof Map) {
                    Map<?, ?> subMap = (Map<?, ?>) submissionObj;
                    Object scoreObj = subMap.get("score");
                    if (scoreObj != null) {
                        if (scoreObj instanceof Number) {
                            score = ((Number) scoreObj).doubleValue();
                        } else {
                            try {
                                score = Double.parseDouble(String.valueOf(scoreObj));
                            } catch (Exception e) {}
                        }
                    }
                }

                String canvasId = String.valueOf(a.get("id"));
                String name = (String) a.get("name");
                if (name == null) {
                    name = "Atividade " + canvasId;
                }
                
                double maxScore = 0.0;
                Object pPossible = a.get("points_possible");
                if (pPossible != null) {
                    if (pPossible instanceof Number) {
                        maxScore = ((Number) pPossible).doubleValue();
                    } else {
                        try {
                            maxScore = Double.parseDouble(String.valueOf(pPossible));
                        } catch (Exception e) {}
                    }
                }

                boolean extra = maxScore == 0.0 
                    || name.toLowerCase().contains("extra") 
                    || name.toLowerCase().contains("opcional");

                boolean reav = name.toLowerCase().contains("reavaliação") 
                    || name.toLowerCase().contains("reaval")
                    || name.toLowerCase().contains("recuperação")
                    || name.toLowerCase().contains("substitutiva");

                FileManager.GradeItem existing = null;
                for (FileManager.GradeItem item : localGrades) {
                    if (canvasId.equals(item.canvasId) || (item.canvasId == null && item.name != null && item.name.trim().equalsIgnoreCase(name.trim()))) {
                        existing = item;
                        existing.canvasId = canvasId;
                        break;
                    }
                }

                if (existing != null) {
                    if (existing.score != score || existing.maxScore != maxScore || !existing.name.equals(name) || existing.extra != extra || existing.reav != reav) {
                        System.out.printf("  • 🔄 Atualizando: \"%s\" (%.2f/%.2f) -> (%.2f/%.2f)\n", 
                            name, existing.score, existing.maxScore, score, maxScore);
                        existing.name = name;
                        existing.score = score;
                        existing.maxScore = maxScore;
                        existing.extra = extra;
                        existing.reav = reav;
                        localModified = true;
                        countUpdated++;
                    }
                } else {
                    String typeS = extra ? " [Extra]" : (reav ? " [Reavaliação]" : "");
                    System.out.printf("  • 📥 Importando: \"%s\" (Nota: %.2f/%.2f)%s\n", 
                        name, score, maxScore, typeS);
                    localGrades.add(new FileManager.GradeItem(name, score, maxScore, extra, reav, canvasId));
                    localModified = true;
                    countNew++;
                }
            }

            if (localModified) {
                FileManager.saveGrades(subjectFolder, localGrades);
                System.out.println(ANSI_GREEN + "\n✔ Sincronização concluída!" + ANSI_RESET);
                System.out.printf("  - %d nova(s) nota(s) importada(s)\n", countNew);
                System.out.printf("  - %d nota(s) atualizada(s)\n", countUpdated);
            } else {
                System.out.println(ANSI_GREEN + "\n✔ Tudo atualizado! Nenhuma alteração de notas encontrada no Canvas." + ANSI_RESET);
            }

        } catch (Exception e) {
            System.out.println(ANSI_RED + "❌ Falha ao buscar notas do Canvas: " + e.getMessage() + ANSI_RESET);
        }
    }

    private static void syncGradesFromCanvasSilently(Subject subject, Path subjectFolder) {
        String courseId = FileManager.getCanvasCourseId(subjectFolder);
        if (courseId == null || courseId.isEmpty()) {
            return;
        }

        Path configPath = getConfigFilePath();
        Map<String, String> config = null;
        try {
            config = FileManager.loadConfig(configPath);
        } catch (IOException e) {
            return;
        }

        String canvasUrl = config.get("canvas_url");
        String token = config.get("canvas_token");
        if (canvasUrl == null || canvasUrl.isEmpty() || token == null || token.isEmpty()) {
            return;
        }

        try {
            List<Map<String, Object>> assignments = CanvasManager.getAssignments(canvasUrl, token, courseId);
            if (assignments.isEmpty()) {
                return;
            }

            List<FileManager.GradeItem> localGrades = FileManager.loadGrades(subjectFolder);
            boolean localModified = false;

            for (Map<String, Object> a : assignments) {
                Object submissionObj = a.get("submission");
                double score = 0.0;
                if (submissionObj instanceof Map) {
                    Map<?, ?> subMap = (Map<?, ?>) submissionObj;
                    Object scoreObj = subMap.get("score");
                    if (scoreObj != null) {
                        if (scoreObj instanceof Number) {
                            score = ((Number) scoreObj).doubleValue();
                        } else {
                            try {
                                score = Double.parseDouble(String.valueOf(scoreObj));
                            } catch (Exception e) {}
                        }
                    }
                }

                String canvasId = String.valueOf(a.get("id"));
                String name = (String) a.get("name");
                if (name == null) {
                    name = "Atividade " + canvasId;
                }
                
                double maxScore = 0.0;
                Object pPossible = a.get("points_possible");
                if (pPossible != null) {
                    if (pPossible instanceof Number) {
                        maxScore = ((Number) pPossible).doubleValue();
                    } else {
                        try {
                            maxScore = Double.parseDouble(String.valueOf(pPossible));
                        } catch (Exception e) {}
                    }
                }

                boolean extra = maxScore == 0.0 
                    || name.toLowerCase().contains("extra") 
                    || name.toLowerCase().contains("opcional");

                boolean reav = name.toLowerCase().contains("reavaliação") 
                    || name.toLowerCase().contains("reaval")
                    || name.toLowerCase().contains("recuperação")
                    || name.toLowerCase().contains("substitutiva");

                FileManager.GradeItem existing = null;
                for (FileManager.GradeItem item : localGrades) {
                    if (canvasId.equals(item.canvasId) || (item.canvasId == null && item.name != null && item.name.trim().equalsIgnoreCase(name.trim()))) {
                        existing = item;
                        existing.canvasId = canvasId;
                        break;
                    }
                }

                if (existing != null) {
                    if (existing.score != score || existing.maxScore != maxScore || !existing.name.equals(name) || existing.extra != extra || existing.reav != reav) {
                        existing.name = name;
                        existing.score = score;
                        existing.maxScore = maxScore;
                        existing.extra = extra;
                        existing.reav = reav;
                        localModified = true;
                    }
                } else {
                    localGrades.add(new FileManager.GradeItem(name, score, maxScore, extra, reav, canvasId));
                    localModified = true;
                }
            }

            if (localModified) {
                FileManager.saveGrades(subjectFolder, localGrades);
            }
        } catch (Exception e) {
            // Silently ignore Canvas grade errors
        }
    }

    private static void checkAbsenceAlert(int current, int max) {
        if (max > 0) {
            int remaining = max - current;
            if (remaining < 0) {
                System.out.println("\n" + ANSI_BOLD + ANSI_RED + "🚨 🚨 🚨 PERIGO: Você ultrapassou o limite seguro de faltas (" + current + "/" + max + ")! Risco iminente de reprovação!" + ANSI_RESET);
            } else if (remaining == 0) {
                System.out.println("\n" + ANSI_BOLD + ANSI_BRIGHT_RED + "⚠️ ALERTA LIMITE: Você está exatamente no limite seguro de faltas (" + current + "/" + max + "). Você NÃO pode mais faltar!" + ANSI_RESET);
            } else if (remaining <= 2) {
                System.out.println("\n" + ANSI_BOLD + ANSI_YELLOW + "⚠️ ALERTA: Muito próximo do limite! Restam apenas " + remaining + " faltas seguras." + ANSI_RESET);
            }
        }
    }

    private static SubjectJson matchSubject(String planningName, List<SubjectJson> jsonList) {
        String normalizedPlanning = planningName.toLowerCase()
            .replaceAll("\\*\\s*$", "")
            .replaceAll("\\(.*\\)", "")
            .trim();

        for (SubjectJson j : jsonList) {
            String normalizedJson = j.Nome.toLowerCase().trim();
            if (normalizedJson.equals(normalizedPlanning) ||
                normalizedJson.contains(normalizedPlanning) ||
                normalizedPlanning.contains(normalizedJson)) {
                return j;
            }
        }

        Pattern p = Pattern.compile("\\(([^)]+)\\)");
        Matcher m = p.matcher(planningName);
        if (m.find()) {
            String abbrev = m.group(1).toLowerCase().replaceAll("\\*\\s*$", "").trim();
            for (SubjectJson j : jsonList) {
                String id = j.Id.toLowerCase().trim();
                if (id.equals(abbrev) ||
                    id.replaceAll("[es]", "").equals(abbrev) ||
                    abbrev.replaceAll("[es]", "").equals(id) ||
                    (abbrev.equals("redes1") && id.equals("r1")) ||
                    (abbrev.equals("ti5") && id.equals("t15")) ||
                    (abbrev.equals("ti1") && id.equals("t11")) ||
                    (abbrev.equals("ti2") && id.equals("t12")) ||
                    (abbrev.equals("ti3") && id.equals("t13")) ||
                    (abbrev.equals("ti4") && id.equals("t14")) ||
                    (abbrev.equals("ti6") && id.equals("t16"))) {
                    return j;
                }
            }
        }

        return null;
    }

    private static void pressEnterToContinue(Scanner scanner) {
        System.out.print("\nPressione " + ANSI_BOLD + "ENTER" + ANSI_RESET + " para voltar ao menu...");
        try {
            if (scanner != null && scanner.hasNextLine()) {
                scanner.nextLine();
            } else {
                new Scanner(System.in).nextLine();
            }
        } catch (Exception e) {
            // Ignore EOF
        }
    }

    private static void exitProgram(Scanner scanner) {
        System.out.print("\nDeseja sincronizar com o GitHub antes de sair? (S/n): ");
        String syncOpt = scanner.nextLine().trim().toLowerCase();
        if (syncOpt.isEmpty() || syncOpt.equals("s") || syncOpt.equals("sim")) {
            gitSync(scanner, false);
        }
        System.out.println("\nAté logo!");
        System.exit(0);
    }

    private static void gitMenu(Scanner scanner) {
        List<String> gitOptions = Arrays.asList(
            "📤 Sincronizar Alterações (Commit, Pull & Push)",
            "📜 Ver Histórico de Commits (git log)",
            "⏪ Desfazer Último Commit Local (Soft Reset)",
            "🗑️ Descartar Alterações Não Salvas (Hard Reset)",
            "🔙 Voltar ao Menu Principal"
        );

        while (true) {
            String title = "\n" + ANSI_BOLD + ANSI_BLUE + "⚙️  CONTROLE DE VERSÃO (GIT / GITHUB)" + ANSI_RESET;
            int choice = InteractiveMenu.select(title, gitOptions);
            if (choice == -1 || choice == 4) { // Voltar ou Esc
                break;
            }

            switch (choice) {
                case 0:
                    gitSync(scanner, true);
                    break;
                case 1:
                    showGitLog(scanner);
                    break;
                case 2:
                    undoLastCommit(scanner);
                    break;
                case 3:
                    discardUnsavedChanges(scanner);
                    break;
            }
        }
    }

    private static void gitSync(Scanner scanner, boolean pressEnter) {
        System.out.println("\n" + ANSI_CYAN + "==================================================" + ANSI_RESET);
        System.out.println(ANSI_BOLD + ANSI_BLUE + "         📤 SINCRONIZANDO COM GITHUB" + ANSI_RESET);
        System.out.println(ANSI_CYAN + "==================================================" + ANSI_RESET);

        // Gera a mensagem detalhada baseada no staging atual
        String defaultMsg = generateDetailedCommitMessage();
        
        System.out.println("\n📝 Mensagem de commit sugerida:");
        System.out.println(ANSI_YELLOW + defaultMsg + ANSI_RESET);
        System.out.println("----------------------------------------");
        System.out.println(" [" + ANSI_GREEN + "1" + ANSI_RESET + "] Usar mensagem sugerida (Padrão)");
        System.out.println(" [" + ANSI_GREEN + "2" + ANSI_RESET + "] Digitar mensagem personalizada");
        System.out.println(" [" + ANSI_GREEN + "3" + ANSI_RESET + "] Cancelar sincronização");
        System.out.println("----------------------------------------");
        System.out.print("Escolha uma opção (ENTER para Padrão): ");

        String commitChoice = scanner.nextLine().trim();
        String commitMessage = defaultMsg;

        if (commitChoice.equals("2")) {
            System.out.print("Digite a mensagem de commit: ");
            String msg = scanner.nextLine().trim();
            if (!msg.isEmpty()) {
                commitMessage = msg;
            }
        } else if (commitChoice.equals("3")) {
            try {
                // Desfazer o git add provisório
                ProcessBuilder pbReset = new ProcessBuilder("git", "reset");
                pbReset.directory(rootDir.toFile());
                pbReset.start().waitFor();
            } catch (Exception e) {}
            System.out.println("\n❌ Sincronização cancelada pelo usuário.");
            System.out.println(ANSI_CYAN + "==================================================" + ANSI_RESET);
            if (pressEnter) {
                pressEnterToContinue(scanner);
            }
            return;
        }

        try {
            System.out.println("\n🔹 Executando: git add .");
            runGitCommand("git", "add", ".");

            System.out.println("🔹 Executando: git commit...");
            Path commitMsgFile = rootDir.resolve(".git").resolve("COMMIT_EDITMSG_TMP");
            try {
                Files.writeString(commitMsgFile, commitMessage, java.nio.charset.StandardCharsets.UTF_8);
                ProcessBuilder pbCommit = createGitProcessBuilder("git", "commit", "--no-verify", "-F", commitMsgFile.toAbsolutePath().toString());
                Process pCommit = pbCommit.start();
                int commitExit = pCommit.waitFor();
                if (commitExit != 0 && commitExit != 1) {
                    throw new IOException("git commit falhou com código " + commitExit);
                }
            } finally {
                try {
                    Files.deleteIfExists(commitMsgFile);
                } catch (Exception ignored) {}
            }

            System.out.println("🔹 Buscando atualizações remotas (git pull --rebase)...");
            try {
                runGitCommand("git", "pull", "--rebase");
            } catch (IOException e) {
                System.out.println("\n" + ANSI_BOLD + ANSI_RED + "⚠️  Conflito ou falha detectada ao fazer pull!" + ANSI_RESET);
                System.out.println("Por favor, resolva os conflitos manualmente no terminal.");
                throw e;
            }

            System.out.println("🔹 Executando: git push --force-with-lease...");
            try {
                runGitCommand("git", "push", "--force-with-lease");
            } catch (IOException e) {
                System.out.println("⚠️  Tentando git push -u origin main...");
                runGitCommand("git", "push", "-u", "origin", "main");
            }

            System.out.println("\n" + ANSI_BOLD + ANSI_GREEN + "✔ Sincronização com GitHub concluída com sucesso!" + ANSI_RESET);
        } catch (Exception e) {
            System.out.println("\n" + ANSI_BOLD + ANSI_RED + "❌ Falha na sincronização: " + e.getMessage() + ANSI_RESET);
        }
        System.out.println(ANSI_CYAN + "==================================================" + ANSI_RESET);
        if (pressEnter) {
            pressEnterToContinue(scanner);
        }
    }

    private static String generateDetailedCommitMessage() {
        try {
            // Provisoriamente adiciona ao staging para que diff/status funcionem perfeitamente
            ProcessBuilder pbAdd = new ProcessBuilder("git", "add", ".");
            pbAdd.directory(rootDir.toFile());
            pbAdd.start().waitFor();

            List<String> statusLines = runGitCommandWithOutput("git", "status", "--porcelain");
            if (statusLines.isEmpty()) {
                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
                return "Auto-sync: sem modificações " + now.format(formatter);
            }

            List<String> modified = new ArrayList<>();
            List<String> added = new ArrayList<>();
            List<String> deleted = new ArrayList<>();

            for (String line : statusLines) {
                if (line.length() > 3) {
                    char st1 = line.charAt(0);
                    char st2 = line.charAt(1);
                    String file = line.substring(3).trim();

                    if (st1 == 'D' || st2 == 'D') {
                        deleted.add(file);
                    } else if (st1 == 'A' || st2 == 'A' || st1 == '?' || st2 == '?') {
                        added.add(file);
                    } else {
                        modified.add(file);
                    }
                }
            }

            // Analisa mudanças de tarefas nas anotações
            List<String> diffLines = runGitCommandWithOutput("git", "diff", "--cached", "-U0");
            List<String> completedTasks = new ArrayList<>();
            List<String> newTasks = new ArrayList<>();

            for (String line : diffLines) {
                if (line.startsWith("+") && !line.startsWith("+++")) {
                    String content = line.substring(1).trim();
                    if (content.contains("[x]") || content.contains("[X]")) {
                        String task = extractTaskText(content);
                        if (!task.isEmpty() && !completedTasks.contains(task)) {
                            completedTasks.add(task);
                        }
                    } else if (content.contains("[ ]")) {
                        String task = extractTaskText(content);
                        if (!task.isEmpty() && !newTasks.contains(task)) {
                            newTasks.add(task);
                        }
                    }
                }
            }

            // Monta o assunto do commit
            StringBuilder subject = new StringBuilder("Sync:");
            int totalFiles = modified.size() + added.size() + deleted.size();

            if (!completedTasks.isEmpty()) {
                subject.append(" ").append(completedTasks.size()).append(completedTasks.size() == 1 ? " tarefa concluída" : " tarefas concluídas");
            }
            if (!newTasks.isEmpty()) {
                if (!completedTasks.isEmpty()) subject.append(",");
                subject.append(" ").append(newTasks.size()).append(newTasks.size() == 1 ? " nova tarefa" : " novas tarefas");
            }
            if (completedTasks.isEmpty() && newTasks.isEmpty()) {
                if (totalFiles == 1) {
                    String singleFile = "";
                    if (!modified.isEmpty()) singleFile = getFileName(modified.get(0));
                    else if (!added.isEmpty()) singleFile = getFileName(added.get(0));
                    else if (!deleted.isEmpty()) singleFile = getFileName(deleted.get(0));
                    subject.append(" atualizado ").append(singleFile);
                } else {
                    subject.append(" atualizados ").append(totalFiles).append(" arquivos");
                }
            } else {
                subject.append(" e ").append(totalFiles).append(totalFiles == 1 ? " arquivo" : " arquivos");
            }

            StringBuilder body = new StringBuilder();

            if (!completedTasks.isEmpty() || !newTasks.isEmpty()) {
                body.append("\n\n📋 Tarefas:");
                for (String t : completedTasks) {
                    body.append("\n  ✔ Concluída: ").append(t);
                }
                for (String t : newTasks) {
                    body.append("\n  ➕ Nova: ").append(t);
                }
            }

            Map<String, List<String>> groups = new LinkedHashMap<>();
            for (String f : modified) addToGrouped(groups, f, "Modificado");
            for (String f : added) addToGrouped(groups, f, "Adicionado");
            for (String f : deleted) addToGrouped(groups, f, "Removido");

            if (!groups.isEmpty()) {
                body.append("\n\n📂 Detalhes dos arquivos:");
                for (Map.Entry<String, List<String>> entry : groups.entrySet()) {
                    body.append("\n  * ").append(entry.getKey()).append(":");
                    for (String d : entry.getValue()) {
                        body.append("\n    - ").append(d);
                    }
                }
            }

            return subject.toString() + body.toString();
        } catch (Exception e) {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            return "Auto-sync: " + now.format(formatter);
        }
    }

    private static String getFileName(String path) {
        try {
            return Paths.get(path).getFileName().toString();
        } catch (Exception e) {
            return path;
        }
    }

    private static void addToGrouped(Map<String, List<String>> groups, String file, String action) {
        String category = categorizeFile(file);
        String detail = file.replace('\\', '/') + " (" + action + ")";
        groups.computeIfAbsent(category, k -> new ArrayList<>()).add(detail);
    }

    private static String categorizeFile(String file) {
        String normalized = file.replace('\\', '/');
        if (normalized.startsWith("config/")) return "Código do Sistema";
        if (normalized.equals("materias.json")) return "Banco de Dados (Faltas/Notas)";
        if (normalized.startsWith("Faculdade/Planejamento")) return "Planejamento da Faculdade";

        String[] parts = normalized.split("/");
        if (parts.length > 1) {
            return parts[0];
        }
        return "Geral";
    }

    private static String extractTaskText(String line) {
        String cleaned = line;
        int idx = cleaned.indexOf("[x]");
        if (idx == -1) idx = cleaned.indexOf("[X]");
        if (idx == -1) idx = cleaned.indexOf("[ ]");
        if (idx != -1) {
            cleaned = cleaned.substring(idx + 3);
        }

        int commentStart = cleaned.indexOf("<!--");
        if (commentStart != -1) {
            cleaned = cleaned.substring(0, commentStart);
        }

        return cleaned.replaceAll("^[-*+\\s]+", "").trim();
    }

    private static List<String> runGitCommandWithOutput(String... command) {
        List<String> lines = new ArrayList<>();
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(rootDir.toFile());
            Process p = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            }
            p.waitFor();
        } catch (Exception e) {
            // Silently ignore or return empty
        }
        return lines;
    }

    private static void showGitLog(Scanner scanner) {
        System.out.println("\n" + ANSI_CYAN + "==================================================" + ANSI_RESET);
        System.out.println(ANSI_BOLD + ANSI_BLUE + "         📜 HISTÓRICO DE COMMITS (git log)" + ANSI_RESET);
        System.out.println(ANSI_CYAN + "==================================================" + ANSI_RESET);

        try {
            List<String> logLines = runGitCommandWithOutput(
                "git", "log", "--oneline", "-n", "10", 
                "--pretty=format:%h - %ad | %s (%an)", 
                "--date=short"
            );

            if (logLines.isEmpty()) {
                System.out.println("Nenhum commit encontrado no histórico local.");
            } else {
                for (String line : logLines) {
                    int firstDash = line.indexOf(" - ");
                    if (firstDash != -1) {
                        String hash = line.substring(0, firstDash);
                        String rest = line.substring(firstDash);
                        System.out.println(ANSI_YELLOW + hash + ANSI_RESET + rest);
                    } else {
                        System.out.println(line);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(ANSI_RED + "Erro ao ler histórico: " + e.getMessage() + ANSI_RESET);
        }

        System.out.println(ANSI_CYAN + "==================================================" + ANSI_RESET);
        pressEnterToContinue(scanner);
    }

    private static void undoLastCommit(Scanner scanner) {
        System.out.println("\n" + ANSI_CYAN + "==================================================" + ANSI_RESET);
        System.out.println(ANSI_BOLD + ANSI_RED + "      ⏪ DESFAZER ÚLTIMO COMMIT (SOFT RESET)" + ANSI_RESET);
        System.out.println(ANSI_CYAN + "==================================================" + ANSI_RESET);

        try {
            List<String> lastCommit = runGitCommandWithOutput("git", "log", "-1", "--oneline");
            if (lastCommit.isEmpty()) {
                System.out.println("Nenhum commit recente para desfazer.");
                System.out.println(ANSI_CYAN + "==================================================" + ANSI_RESET);
                pressEnterToContinue(scanner);
                return;
            }

            System.out.println("Último commit local encontrado:");
            System.out.println("   " + ANSI_YELLOW + lastCommit.get(0) + ANSI_RESET);
            System.out.println("\n" + ANSI_BOLD + ANSI_YELLOW + "⚠️  Isso irá desfazer apenas o commit no repositório local." + ANSI_RESET);
            System.out.println("Suas alterações nos arquivos continuarão salvas para que você possa editá-las ou refazer o commit.");
            System.out.print("\nDeseja mesmo desfazer este commit? (s/N): ");

            String confirm = scanner.nextLine().trim().toLowerCase();
            if (confirm.equals("s") || confirm.equals("sim")) {
                runGitCommand("git", "reset", "--soft", "HEAD~1");
                System.out.println("\n" + ANSI_GREEN + "✔ Último commit desfeito com sucesso! Alterações mantidas no seu workspace." + ANSI_RESET);
            } else {
                System.out.println("\nOperação cancelada.");
            }
        } catch (Exception e) {
            System.out.println(ANSI_RED + "Erro ao desfazer commit: " + e.getMessage() + ANSI_RESET);
        }

        System.out.println(ANSI_CYAN + "==================================================" + ANSI_RESET);
        pressEnterToContinue(scanner);
    }

    private static void discardUnsavedChanges(Scanner scanner) {
        System.out.println("\n" + ANSI_CYAN + "==================================================" + ANSI_RESET);
        System.out.println(ANSI_BOLD + ANSI_RED + "     🗑️  DESCARTAR ALTERAÇÕES LOCAIS (HARD RESET)" + ANSI_RESET);
        System.out.println(ANSI_CYAN + "==================================================" + ANSI_RESET);

        try {
            List<String> status = runGitCommandWithOutput("git", "status", "--porcelain");
            if (status.isEmpty()) {
                System.out.println("Nenhuma alteração local pendente para descartar.");
                System.out.println(ANSI_CYAN + "==================================================" + ANSI_RESET);
                pressEnterToContinue(scanner);
                return;
            }

            System.out.println("Alterações não salvas encontradas:");
            for (String s : status) {
                System.out.println("   " + s);
            }

            System.out.println("\n" + ANSI_BOLD + ANSI_RED + "🚨 AVISO CRÍTICO: ISSO IRÁ DELETAR PERMANENTEMENTE TODAS AS" + ANSI_RESET);
            System.out.println(ANSI_BOLD + ANSI_RED + "ALTERAÇÕES NÃO SALVAS E ARQUIVOS NOVOS NÃO RASTREADOS!" + ANSI_RESET);
            System.out.print("\nDigite 'CONFIRMAR' para prosseguir: ");

            String confirm = scanner.nextLine().trim();
            if (confirm.equals("CONFIRMAR")) {
                runGitCommand("git", "reset", "--hard");
                runGitCommand("git", "clean", "-fd");
                System.out.println("\n" + ANSI_GREEN + "✔ Workspace limpo! Todas as alterações locais foram descartadas." + ANSI_RESET);
            } else {
                System.out.println("\nOperação cancelada.");
            }
        } catch (Exception e) {
            System.out.println(ANSI_RED + "Erro ao descartar alterações: " + e.getMessage() + ANSI_RESET);
        }

        System.out.println(ANSI_CYAN + "==================================================" + ANSI_RESET);
        pressEnterToContinue(scanner);
    }

    private static ProcessBuilder createGitProcessBuilder(String... command) {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(rootDir.toFile());
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);

        Map<String, String> env = pb.environment();
        String gitName = firstNonBlank(
            env.get("GIT_AUTHOR_NAME"),
            env.get("GIT_COMMITTER_NAME"),
            readGitConfigValue("user.name"),
            "Pedro Anotacoes"
        );
        String gitEmail = firstNonBlank(
            env.get("GIT_AUTHOR_EMAIL"),
            env.get("GIT_COMMITTER_EMAIL"),
            readGitConfigValue("user.email"),
            "pedrogaf55@gmail.com"
        );

        env.put("GIT_AUTHOR_NAME", gitName);
        env.put("GIT_COMMITTER_NAME", gitName);
        env.put("GIT_AUTHOR_EMAIL", gitEmail);
        env.put("GIT_COMMITTER_EMAIL", gitEmail);
        return pb;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return "";
    }

    private static String readGitConfigValue(String key) {
        try {
            ProcessBuilder pb = new ProcessBuilder("git", "config", "--get", key);
            pb.directory(rootDir.toFile());
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8).trim();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                return output;
            }
        } catch (Exception e) {
            // Ignora e usa fallback
        }
        return "";
    }

    private static void runGitCommand(String... command) throws IOException, InterruptedException {
        ProcessBuilder pb = createGitProcessBuilder(command);
        Process p = pb.start();
        int exitCode = p.waitFor();
        if (exitCode != 0) {
            throw new IOException("Falha no comando git: " + String.join(" ", command) + " (Código: " + exitCode + ")");
        }
    }

    private static String getProgressBar(int current, int max) {
        if (max <= 0) return "[N/A]";
        int barLength = 10;
        double ratio = (double) current / max;
        int filled = (int) Math.round(ratio * barLength);
        if (filled > barLength) filled = barLength;
        if (filled < 0) filled = 0;

        StringBuilder bar = new StringBuilder("[");
        String color = ANSI_GREEN;
        if (ratio >= 1.0) {
            color = ANSI_BOLD + ANSI_RED;
        } else if (ratio >= 0.8) {
            color = ANSI_BRIGHT_RED;
        } else if (ratio >= 0.5) {
            color = ANSI_YELLOW;
        }

        bar.append(color);
        for (int i = 0; i < barLength; i++) {
            if (i < filled) {
                bar.append("█");
            } else {
                bar.append("░");
            }
        }
        bar.append(ANSI_RESET).append("]");
        return bar.toString();
    }

    private static void handleDirectCommand(String[] args) {
        String cmd = args[0].toLowerCase().trim();
        try {
            if (cmd.equals("hoje")) {
                showDashboard();
            } else if (cmd.equals("status")) {
                printPlanning();
            } else if (cmd.equals("sync")) {
                gitSyncDirect();
            } else if (cmd.equals("faltas")) {
                if (args.length < 3) {
                    System.out.println("Uso: java Cursar faltas [+1/-1/valor] [materia]");
                    return;
                }
                String action = args[1].trim();
                String subjNameOrAbbrev = args[2].trim();
                handleFaltasDirect(action, subjNameOrAbbrev);
            } else if (cmd.equals("tarefa")) {
                if (args.length < 4 || !args[1].toLowerCase().equals("add")) {
                    System.out.println("Uso: java Cursar tarefa add [materia] \"[descricao]\"");
                    return;
                }
                String subjNameOrAbbrev = args[2].trim();
                String desc = args[3].trim();
                handleTarefaAddDirect(subjNameOrAbbrev, desc);
            } else {
                System.out.println("Comando não reconhecido: " + cmd);
                System.out.println("Comandos disponíveis: hoje, status, sync, faltas, tarefa");
            }
        } catch (Exception e) {
            System.err.println("Erro ao executar comando: " + e.getMessage());
        }
    }

    private static void gitSyncDirect() {
        String defaultMsg = generateDetailedCommitMessage();
        System.out.println("Sincronizando com GitHub usando mensagem:\n\"" + defaultMsg + "\"");
        try {
            System.out.println("🔹 git add .");
            runGitCommand("git", "add", ".");

            System.out.println("🔹 git commit...");
            ProcessBuilder pbCommit = createGitProcessBuilder("git", "commit", "--no-verify", "-m", defaultMsg);
            Process pCommit = pbCommit.start();
            int commitExit = pCommit.waitFor();

            if (commitExit != 0 && commitExit != 1) {
                throw new IOException("git commit falhou com código " + commitExit);
            }

            System.out.println("🔹 git pull --rebase...");
            try {
                runGitCommand("git", "pull", "--rebase");
            } catch (Exception e) {
                System.out.println("⚠️  git pull falhou, prosseguindo com o commit local: " + e.getMessage());
            }

            System.out.println("🔹 git push --force-with-lease...");
            boolean pushed = false;
            try {
                runGitCommand("git", "push", "--force-with-lease");
                pushed = true;
            } catch (Exception e) {
                System.out.println("⚠️  push remoto falhou: " + e.getMessage());
                try {
                    runGitCommand("git", "push", "-u", "origin", "main");
                    pushed = true;
                } catch (Exception fallback) {
                    System.out.println("⚠️  Não foi possível enviar as alterações para o remoto. O commit foi criado localmente.");
                    System.out.println("   Configure credenciais do GitHub ou conectividade para concluir o push.");
                }
            }

            if (pushed) {
                System.out.println("\n✔ Sincronizado com sucesso!");
            } else {
                System.out.println("\n✔ Sincronização local concluída. O envio para o GitHub não foi possível.");
            }
        } catch (Exception e) {
            System.out.println("❌ Falha na sincronização: " + e.getMessage());
        }
    }

    private static void handleFaltasDirect(String action, String subjNameOrAbbrev) throws IOException {
        Path jsonPath = getMateriasJsonPath();
        Path absencesPath = rootDir.resolve("Faculdade").resolve("faltas.json");

        if (!Files.exists(jsonPath)) {
            System.out.println("Erro: materias.json não encontrado.");
            return;
        }

        List<SubjectJson> jsonList = FileManager.loadCurriculum(jsonPath);
        Map<String, Integer> absencesMap = FileManager.loadAbsences(absencesPath);

        List<Subject> subjects = FileManager.loadSubjects(planningPath);
        Subject matched = null;
        for (Subject s : subjects) {
            if (s.name.toLowerCase().contains(subjNameOrAbbrev.toLowerCase()) ||
                s.getSanitizedFolderName().toLowerCase().contains(subjNameOrAbbrev.toLowerCase())) {
                matched = s;
                break;
            }
        }

        if (matched == null) {
            System.out.println("Matéria não encontrada no planejamento: " + subjNameOrAbbrev);
            return;
        }

        SubjectJson sj = matchSubject(matched.name, jsonList);
        String key = (sj != null) ? sj.Id : matched.getSanitizedFolderName();
        int current = absencesMap.getOrDefault(key, 0);

        int max = (sj != null) ? (int) Math.ceil((sj.Ch * 0.25) / 2.0) - 1 : 999;

        if (action.equals("+1")) {
            current++;
        } else if (action.equals("-1")) {
            if (current > 0) current--;
        } else {
            try {
                int custom = Integer.parseInt(action);
                if (custom >= 0) {
                    current = custom;
                } else {
                    System.out.println("Erro: Faltas devem ser >= 0.");
                    return;
                }
            } catch (NumberFormatException e) {
                System.out.println("Ação inválida: " + action + ". Use +1, -1 ou um número >= 0.");
                return;
            }
        }

        absencesMap.put(key, current);
        FileManager.saveAbsences(absencesPath, absencesMap);
        System.out.println("✔ Faltas atualizadas para " + matched.name + ": " + current + "/" + max);
        checkAbsenceAlert(current, max);
    }

    private static void handleTarefaAddDirect(String subjNameOrAbbrev, String desc) throws IOException {
        List<Subject> subjects = FileManager.loadSubjects(planningPath);
        Subject matched = null;
        for (Subject s : subjects) {
            if (s.name.toLowerCase().contains(subjNameOrAbbrev.toLowerCase()) ||
                s.getSanitizedFolderName().toLowerCase().contains(subjNameOrAbbrev.toLowerCase())) {
                matched = s;
                break;
            }
        }

        if (matched == null) {
            System.out.println("Matéria não encontrada no planejamento: " + subjNameOrAbbrev);
            return;
        }

        String folderName = matched.getSanitizedFolderName();
        Path folderPath = rootDir.resolve(folderName);
        if (!Files.exists(folderPath)) {
            Files.createDirectories(folderPath);
        }

        FileManager.addTask(folderPath, matched.name, desc);
        System.out.println("✔ Tarefa adicionada à matéria " + matched.name + ": \"" + desc + "\"");
    }

    private static String getContextHeader() {
        try {
            List<Subject> subjects = FileManager.loadSubjects(planningPath);
            List<Subject> enrolled = new ArrayList<>();
            for (Subject s : subjects) {
                if (s.statusChar.equals("c")) {
                    enrolled.add(s);
                }
            }

            int currentPeriod = 1;
            int totalPendingTasks = 0;
            List<String> criticalAbsences = new ArrayList<>();

            Path jsonPath = getMateriasJsonPath();
            Path absencesPath = rootDir.resolve("Faculdade").resolve("faltas.json");
            List<SubjectJson> jsonList = Files.exists(jsonPath) ? FileManager.loadCurriculum(jsonPath) : new ArrayList<>();
            Map<String, Integer> absencesMap = Files.exists(absencesPath) ? FileManager.loadAbsences(absencesPath) : new HashMap<>();

            for (Subject s : enrolled) {
                Matcher m = Pattern.compile("(\\d+)").matcher(s.period);
                if (m.find()) {
                    int pVal = Integer.parseInt(m.group(1));
                    if (pVal > currentPeriod) {
                        currentPeriod = pVal;
                    }
                }

                String folderName = s.getSanitizedFolderName();
                Path folderPath = rootDir.resolve(folderName);
                if (Files.exists(folderPath)) {
                    List<String> tasks = FileManager.loadTasks(folderPath);
                    for (String task : tasks) {
                        if (task.trim().startsWith("- [ ]")) {
                            totalPendingTasks++;
                        }
                    }
                }

                SubjectJson sj = matchSubject(s.name, jsonList);
                if (sj != null) {
                    int currentAbsences = absencesMap.getOrDefault(sj.Id, 0);
                    int maxAbsences = (int) Math.ceil((sj.Ch * 0.25) / 2.0) - 1;
                    int remaining = maxAbsences - currentAbsences;
                    if (remaining <= 2) {
                        criticalAbsences.add(sj.Id + " (" + remaining + " rest.)");
                    }
                }
            }

            String alertStr = criticalAbsences.isEmpty() ? "Nenhuma" : String.join(", ", criticalAbsences);
            return String.format("📅 Período: %dº | 📝 %d tarefas pendentes | ⚠️ Alerta Faltas: %s",
                currentPeriod, totalPendingTasks, alertStr);
        } catch (Exception e) {
            return "📅 Cursar CLI";
        }
    }

    private static String cleanCourseName(String rawName) {
        if (rawName == null) return "";
        int dashIndex = rawName.indexOf(" - ");
        if (dashIndex != -1) {
            return rawName.substring(0, dashIndex).trim();
        }
        return rawName.trim();
    }

    private static void importFromCanvas(Scanner scanner) {
        System.out.print("\u001b[2J\u001b[H");
        System.out.println(ANSI_CYAN + "==================================================" + ANSI_RESET);
        System.out.println(ANSI_BOLD + ANSI_BLUE + "         📥 INTEGRAÇÃO CANVAS LMS" + ANSI_RESET);
        System.out.println(ANSI_CYAN + "==================================================" + ANSI_RESET);

        String token = System.getenv("CANVAS_TOKEN");
        Path configPath = getConfigFilePath();
        Map<String, String> config = null;
        try {
            config = FileManager.loadConfig(configPath);
        } catch (IOException e) {
            System.out.println(ANSI_RED + "❌ Falha ao carregar Faculdade/config.json: " + e.getMessage() + ANSI_RESET);
            pressEnterToContinue(scanner);
            return;
        }

        if (token == null || token.trim().isEmpty()) {
            token = config.get("canvas_token");
        }

        if (token == null || token.trim().isEmpty() || token.equals("11748~EKPzTKrrf8FnreKWT7CY4JX6wEMU6Nty3Gunf22DFuHnLRm6JY8xXntDHfvCDFkP")) {
            System.out.println(ANSI_BOLD + ANSI_RED + "❌ Erro: Token do Canvas não encontrado." + ANSI_RESET);
            System.out.println("Por favor, configure a variável de ambiente " + ANSI_BOLD + "CANVAS_TOKEN" + ANSI_RESET);
            System.out.println("ou atualize o token no arquivo: " + ANSI_YELLOW + "Faculdade/config.json" + ANSI_RESET);
            System.out.println("----------------------------------------");
            System.out.print("Deseja inserir o novo token agora? (S/n): ");
            String ans = scanner.nextLine().trim().toLowerCase();
            if (ans.isEmpty() || ans.equals("s") || ans.equals("sim")) {
                System.out.print("Digite o token: ");
                String newToken = scanner.nextLine().trim();
                if (!newToken.isEmpty()) {
                    config.put("canvas_token", newToken);
                    try {
                        FileManager.saveConfig(configPath, config);
                        token = newToken;
                        System.out.println(ANSI_GREEN + "✔ Token atualizado com sucesso em Faculdade/config.json!" + ANSI_RESET);
                    } catch (IOException io) {
                        System.out.println(ANSI_RED + "❌ Erro ao salvar: " + io.getMessage() + ANSI_RESET);
                        pressEnterToContinue(scanner);
                        return;
                    }
                } else {
                    return;
                }
            } else {
                return;
            }
        }

        String canvasUrl = config.get("canvas_url");
        if (canvasUrl == null || canvasUrl.trim().isEmpty()) {
            canvasUrl = "https://pucminas.instructure.com";
        }

        List<Subject> enrolledList;
        try {
            List<Subject> subjects = FileManager.loadSubjects(planningPath);
            enrolledList = new ArrayList<>();
            for (Subject s : subjects) {
                if (s.statusChar.equals("c")) {
                    enrolledList.add(s);
                }
            }
        } catch (IOException e) {
            System.out.println(ANSI_RED + "❌ Erro ao carregar matérias locais." + ANSI_RESET);
            pressEnterToContinue(scanner);
            return;
        }

        if (enrolledList.isEmpty()) {
            System.out.println(ANSI_YELLOW + "⏳ Nenhuma matéria cursando atualmente no planejamento." + ANSI_RESET);
            pressEnterToContinue(scanner);
            return;
        }

        System.out.println("\n📡 Conectando ao Canvas para listar cursos e verificar atividades...");
        List<Map<String, Object>> canvasCourses = null;
        try {
            List<Map<String, Object>> allCourses = CanvasManager.getCourses(canvasUrl, token);
            canvasCourses = new ArrayList<>();
            for (Map<String, Object> c : allCourses) {
                String courseId = String.valueOf(c.get("id"));
                String rawName = (String) c.get("name");
                String displayName = cleanCourseName(rawName);
                System.out.print("🔍 Verificando \"" + displayName + "\"... ");
                try {
                    List<Map<String, Object>> assignments = CanvasManager.getAssignments(canvasUrl, token, courseId);
                    if (assignments != null && !assignments.isEmpty()) {
                        Subject localSub = null;
                        for (Subject s : enrolledList) {
                            Path folder = rootDir.resolve(s.getSanitizedFolderName());
                            String linkedId = FileManager.getCanvasCourseId(folder);
                            if (courseId.equals(linkedId)) {
                                localSub = s;
                                break;
                            }
                        }

                        List<String> importedIds = new ArrayList<>();
                        if (localSub != null) {
                            Path taskFile = rootDir.resolve(localSub.getSanitizedFolderName()).resolve("Tarefas.md");
                            if (Files.exists(taskFile)) {
                                List<String> lines = Files.readAllLines(taskFile);
                                Pattern canvasIdPattern = Pattern.compile("<!--\\s*canvas_id:\\s*(\\S+)\\s*-->");
                                for (String line : lines) {
                                    Matcher m = canvasIdPattern.matcher(line);
                                    if (m.find()) {
                                        importedIds.add(m.group(1));
                                    }
                                }
                            }
                        }

                        int nonImportedCount = 0;
                        for (Map<String, Object> a : assignments) {
                            String id = String.valueOf(a.get("id"));
                            if (!importedIds.contains(id)) {
                                nonImportedCount++;
                            }
                        }

                        if (nonImportedCount > 0) {
                            canvasCourses.add(c);
                            System.out.println(ANSI_GREEN + "Tem novas atividades (" + nonImportedCount + ")" + ANSI_RESET);
                        } else {
                            System.out.println(ANSI_YELLOW + "Sem novas atividades (todas " + assignments.size() + " já importadas)" + ANSI_RESET);
                        }
                    } else {
                        System.out.println(ANSI_YELLOW + "Sem atividades" + ANSI_RESET);
                    }
                } catch (Exception e) {
                    System.out.println(ANSI_RED + "Erro ao verificar" + ANSI_RESET);
                }
            }
        } catch (Exception e) {
            System.out.println(ANSI_RED + "❌ Falha ao conectar: " + e.getMessage() + ANSI_RESET);
            pressEnterToContinue(scanner);
            return;
        }

        if (canvasCourses.isEmpty()) {
            System.out.println(ANSI_YELLOW + "⚠ Nenhum curso ativo com atividades encontrado no Canvas." + ANSI_RESET);
            pressEnterToContinue(scanner);
            return;
        }

        List<String> courseOptions = new ArrayList<>();
        for (Map<String, Object> c : canvasCourses) {
            String rawName = (String) c.get("name");
            courseOptions.add(cleanCourseName(rawName));
        }

        int courseChoice = InteractiveMenu.select("Selecione o curso do Canvas para gerenciar:", courseOptions);
        if (courseChoice == -1) return;

        Map<String, Object> selectedCourse = canvasCourses.get(courseChoice);
        String courseId = String.valueOf(selectedCourse.get("id"));
        String selectedCourseName = cleanCourseName((String) selectedCourse.get("name"));

        Subject localSubject = null;
        for (Subject s : enrolledList) {
            Path folder = rootDir.resolve(s.getSanitizedFolderName());
            String linkedId = FileManager.getCanvasCourseId(folder);
            if (courseId.equals(linkedId)) {
                localSubject = s;
                break;
            }
        }

        if (localSubject == null) {
            List<String> localOptions = new ArrayList<>();
            for (Subject s : enrolledList) {
                localOptions.add("(" + s.period + ") " + s.name);
            }
            int localChoice = InteractiveMenu.select("Vincular curso '" + selectedCourseName + "' a qual matéria local?", localOptions);
            if (localChoice == -1) return;
            localSubject = enrolledList.get(localChoice);
            Path folder = rootDir.resolve(localSubject.getSanitizedFolderName());
            FileManager.saveCanvasCourseId(folder, courseId);
            System.out.println(ANSI_GREEN + "✔ Vínculo salvo localmente na pasta da matéria!" + ANSI_RESET);
        }

        Path subjectFolder = rootDir.resolve(localSubject.getSanitizedFolderName());

        System.out.println("\n📡 Buscando avaliações e tarefas no Canvas para '" + selectedCourseName + "'...");
        List<Map<String, Object>> assignments;
        try {
            assignments = CanvasManager.getAssignments(canvasUrl, token, courseId);
        } catch (Exception e) {
            System.out.println(ANSI_RED + "❌ Falha ao buscar tarefas: " + e.getMessage() + ANSI_RESET);
            System.out.println("Caso queira refazer o vínculo, delete o arquivo '.canvas_meta.json' na pasta da matéria.");
            pressEnterToContinue(scanner);
            return;
        }

        if (assignments.isEmpty()) {
            System.out.println(ANSI_YELLOW + "✔ Nenhuma tarefa cadastrada no Canvas para este curso." + ANSI_RESET);
            pressEnterToContinue(scanner);
            return;
        }

        // Sincroniza o status de entrega das tarefas já importadas
        try {
            Path taskFile = subjectFolder.resolve("Tarefas.md");
            if (Files.exists(taskFile)) {
                List<String> lines = new ArrayList<>(Files.readAllLines(taskFile));
                boolean localModified = false;
                
                // Mapeia os assignments por ID
                Map<String, Map<String, Object>> assignMap = new HashMap<>();
                for (Map<String, Object> a : assignments) {
                    assignMap.put(String.valueOf(a.get("id")), a);
                }
                
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    String trimmed = line.trim();
                    if (!(trimmed.startsWith("- [ ]") || trimmed.startsWith("- [x]") || trimmed.startsWith("- [X]"))) {
                        continue;
                    }
                    
                    // Verifica se é uma tarefa importada do Canvas
                    Pattern canvasIdPattern = Pattern.compile("<!--\\s*canvas_id:\\s*(\\S+)\\s*-->");
                    Matcher m = canvasIdPattern.matcher(line);
                    if (m.find()) {
                        String canvasId = m.group(1);
                        if (assignMap.containsKey(canvasId)) {
                            Map<String, Object> a = assignMap.get(canvasId);
                            
                            // Verifica se foi entregue no Canvas
                            boolean isSubmitted = false;
                            Object submissionObj = a.get("submission");
                            if (submissionObj instanceof Map) {
                                Map<?, ?> subMap = (Map<?, ?>) submissionObj;
                                Object state = subMap.get("workflow_state");
                                if ("submitted".equals(state) || "graded".equals(state)) {
                                    isSubmitted = true;
                                }
                            }
                            
                            boolean isLocalCompleted = trimmed.startsWith("- [x]") || trimmed.startsWith("- [X]");
                            if (isSubmitted && !isLocalCompleted) {
                                // Foi entregue no Canvas mas está pendente localmente -> Marca como concluída localmente!
                                String cleanTitle = line.replaceFirst("^\\s*-\\s*\\[[ xX]\\]\\s*", "");
                                cleanTitle = cleanTitle.replaceAll("<!--\\s*google_task_id:\\s*\\S+\\s*-->", "");
                                cleanTitle = cleanTitle.replaceAll("<!--\\s*canvas_id:\\s*\\S+\\s*-->", "");
                                cleanTitle = cleanTitle.replaceAll("<!--\\s*google_task_notes:\\s*.*?\\s*-->", "");
                                cleanTitle = cleanTitle.trim();
                                
                                System.out.println(ANSI_GREEN + "🎉 Entrega detectada no Canvas: \"" + cleanTitle + "\" -> Marcada como Concluída!" + ANSI_RESET);
                                String updatedLine = line.replaceFirst("\\[[ xX]\\]", "[x]");
                                lines.set(i, updatedLine);
                                localModified = true;
                            }
                        }
                    }
                }
                
                if (localModified) {
                    Files.write(taskFile, lines);
                }
            }
        } catch (Exception e) {
            System.out.println(ANSI_YELLOW + "⚠️ Não foi possível sincronizar o status de entrega do Canvas: " + e.getMessage() + ANSI_RESET);
        }

        List<String> importedIds = new ArrayList<>();
        try {
            Path taskFile = subjectFolder.resolve("Tarefas.md");
            if (Files.exists(taskFile)) {
                List<String> lines = Files.readAllLines(taskFile);
                Pattern canvasIdPattern = Pattern.compile("<!--\\s*canvas_id:\\s*(\\S+)\\s*-->");
                for (String line : lines) {
                    Matcher m = canvasIdPattern.matcher(line);
                    if (m.find()) {
                        importedIds.add(m.group(1));
                    }
                }
            }
        } catch (Exception e) {}

        List<Map<String, Object>> assignmentsToDisplay = new ArrayList<>();
        for (Map<String, Object> a : assignments) {
            String id = String.valueOf(a.get("id"));
            if (!importedIds.contains(id)) {
                assignmentsToDisplay.add(a);
            }
        }

        if (assignmentsToDisplay.isEmpty()) {
            System.out.println(ANSI_YELLOW + "✔ Todas as tarefas do Canvas para este curso já foram importadas!" + ANSI_RESET);
        }

        List<String> assignOptions = new ArrayList<>();
        assignOptions.add("➕ [Criar Tarefa Manual]");
        for (Map<String, Object> a : assignmentsToDisplay) {
            String name = (String) a.get("name");
            String due = (String) a.get("due_at");
            String formattedDue = CanvasManager.formatCanvasDate(due);
            assignOptions.add(name + " (" + formattedDue + ")");
        }

        List<Integer> choices = InteractiveMenu.selectMultiple("Selecione as tarefas para importar/criar:", assignOptions);
        if (choices.isEmpty()) return;

        int countImported = 0;
        for (int choice : choices) {
            if (choice == 0) {
                System.out.print("\nDigite a descrição da tarefa manual: ");
                String desc = scanner.nextLine().trim();
                if (!desc.isEmpty()) {
                    try {
                        FileManager.addTask(subjectFolder, localSubject.name, desc);
                        System.out.println(ANSI_GREEN + "✔ Tarefa manual adicionada com sucesso!" + ANSI_RESET);
                        countImported++;
                    } catch (IOException io) {
                        System.out.println(ANSI_RED + "❌ Falha ao adicionar: " + io.getMessage() + ANSI_RESET);
                    }
                }
            } else {
                Map<String, Object> selectedAssign = assignmentsToDisplay.get(choice - 1);
                String id = String.valueOf(selectedAssign.get("id"));
                String name = (String) selectedAssign.get("name");
                String due = (String) selectedAssign.get("due_at");
                String formattedDue = CanvasManager.formatCanvasDate(due);
                String description = (String) selectedAssign.get("description");

                try {
                    FileManager.addOrUpdateCanvasTask(subjectFolder, localSubject.name, name, formattedDue, id, description);
                    System.out.println(ANSI_GREEN + "✔ Importada: " + name + ANSI_RESET);
                    countImported++;
                } catch (IOException io) {
                    System.out.println(ANSI_RED + "❌ Erro ao salvar tarefa: " + io.getMessage() + ANSI_RESET);
                }
            }
        }

        if (countImported > 0) {
            System.out.println(ANSI_BOLD + ANSI_GREEN + "\n✔ Processo concluído para " + countImported + " tarefa(s)!" + ANSI_RESET);
        }

        System.out.println(ANSI_CYAN + "\n📊 Sincronizando notas do Canvas para '" + localSubject.name + "'..." + ANSI_RESET);
        syncGradesFromCanvasSilently(localSubject, subjectFolder);
        System.out.println(ANSI_GREEN + "✔ Sincronização de notas concluída (verifique 'Notas.md' e 'notas.json')!" + ANSI_RESET);
        pressEnterToContinue(scanner);
    }

    private static void syncCanvasSubmissions(Scanner scanner) {
        System.out.print("\u001b[2J\u001b[H");
        System.out.println(ANSI_CYAN + "==================================================" + ANSI_RESET);
        System.out.println(ANSI_BOLD + ANSI_BLUE + "       🔄 SINCRONIZAÇÃO DE ENTREGAS DO CANVAS" + ANSI_RESET);
        System.out.println(ANSI_CYAN + "==================================================" + ANSI_RESET);

        Path configPath = getConfigFilePath();
        Map<String, String> config = null;
        try {
            config = FileManager.loadConfig(configPath);
        } catch (IOException e) {
            System.out.println(ANSI_RED + "❌ Falha ao carregar Faculdade/config.json: " + e.getMessage() + ANSI_RESET);
            pressEnterToContinue(scanner);
            return;
        }

        String canvasUrl = config.get("canvas_url");
        String token = config.get("canvas_token");
        if (canvasUrl == null || canvasUrl.isEmpty() || token == null || token.isEmpty()) {
            System.out.println(ANSI_RED + "❌ Credenciais do Canvas não configuradas." + ANSI_RESET);
            System.out.println("Use a opção de importação primeiro para configurar.");
            pressEnterToContinue(scanner);
            return;
        }

        List<Subject> enrolledList;
        try {
            List<Subject> subjects = FileManager.loadSubjects(planningPath);
            enrolledList = new ArrayList<>();
            for (Subject s : subjects) {
                if (s.statusChar.equals("c")) {
                    enrolledList.add(s);
                }
            }
        } catch (IOException e) {
            System.out.println(ANSI_RED + "❌ Erro ao carregar matérias locais: " + e.getMessage() + ANSI_RESET);
            pressEnterToContinue(scanner);
            return;
        }

        if (enrolledList.isEmpty()) {
            System.out.println(ANSI_YELLOW + "⏳ Nenhuma matéria cursando atualmente." + ANSI_RESET);
            pressEnterToContinue(scanner);
            return;
        }

        System.out.println("📡 Conectando ao Canvas...");
        int updatedCount = 0;

        for (Subject s : enrolledList) {
            String cleanName = s.getSanitizedFolderName();
            Path subjectFolder = rootDir.resolve(cleanName);
            String courseId = FileManager.getCanvasCourseId(subjectFolder);

            if (courseId == null || courseId.isEmpty()) {
                System.out.println("  • \"" + s.name + "\": não vinculada ao Canvas. Pulando...");
                continue;
            }

            System.out.println("  • Buscando tarefas e entregas de \"" + s.name + "\"... ");
            try {
                List<Map<String, Object>> assignments = CanvasManager.getAssignments(canvasUrl, token, courseId);
                if (assignments.isEmpty()) {
                    continue;
                }

                Path taskFile = subjectFolder.resolve("Tarefas.md");
                if (!Files.exists(taskFile)) {
                    continue;
                }

                List<String> lines = new ArrayList<>(Files.readAllLines(taskFile));
                boolean localModified = false;

                Map<String, Map<String, Object>> assignMap = new HashMap<>();
                for (Map<String, Object> a : assignments) {
                    assignMap.put(String.valueOf(a.get("id")), a);
                }

                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    String trimmed = line.trim();
                    if (!(trimmed.startsWith("- [ ]") || trimmed.startsWith("- [x]") || trimmed.startsWith("- [X]"))) {
                        continue;
                    }

                    Pattern canvasIdPattern = Pattern.compile("<!--\\s*canvas_id:\\s*(\\S+)\\s*-->");
                    Matcher m = canvasIdPattern.matcher(line);
                    if (m.find()) {
                        String canvasId = m.group(1);
                        if (assignMap.containsKey(canvasId)) {
                            Map<String, Object> a = assignMap.get(canvasId);

                            boolean isSubmitted = false;
                            Object submissionObj = a.get("submission");
                            if (submissionObj instanceof Map) {
                                Map<?, ?> subMap = (Map<?, ?>) submissionObj;
                                Object state = subMap.get("workflow_state");
                                if ("submitted".equals(state) || "graded".equals(state)) {
                                    isSubmitted = true;
                                }
                            }

                            boolean isLocalCompleted = trimmed.startsWith("- [x]") || trimmed.startsWith("- [X]");
                            String updatedLine = line;
                            boolean lineChanged = false;

                            if (isSubmitted && !isLocalCompleted) {
                                String cleanTitle = line.replaceFirst("^\\s*-\\s*\\[[ xX]\\]\\s*", "");
                                cleanTitle = cleanTitle.replaceAll("<!--\\s*google_task_id:\\s*\\S+\\s*-->", "");
                                cleanTitle = cleanTitle.replaceAll("<!--\\s*canvas_id:\\s*\\S+\\s*-->", "");
                                cleanTitle = cleanTitle.replaceAll("<!--\\s*google_task_notes:\\s*.*?\\s*-->", "");
                                cleanTitle = cleanTitle.trim();

                                System.out.println("    " + ANSI_GREEN + "🎉 Entrega detectada: \"" + cleanTitle + "\" -> Marcada como Concluída!" + ANSI_RESET);
                                updatedLine = updatedLine.replaceFirst("\\[[ xX]\\]", "[x]");
                                lineChanged = true;
                                updatedCount++;
                            }

                            // Sincroniza o prazo (deadline) do Canvas se estiver desatualizado
                            String due = (String) a.get("due_at");
                            String canvasDeadline = CanvasManager.formatCanvasDate(due);
                            int firstCommentIdx = updatedLine.indexOf("<!--");
                            String visiblePart = firstCommentIdx != -1 ? updatedLine.substring(0, firstCommentIdx).trim() : updatedLine.trim();
                            int canvasPrefixIdx = updatedLine.indexOf("[Canvas]");
                            if (canvasPrefixIdx != -1) {
                                String statusPrefix = updatedLine.substring(0, canvasPrefixIdx).trim();
                                String assignmentName = (String) a.get("name");
                                String newVisiblePart = statusPrefix + " [Canvas] " + assignmentName + " (" + canvasDeadline + ")";
                                
                                String currentDeadline = "";
                                int lastOpenParen = visiblePart.lastIndexOf("(");
                                int lastCloseParen = visiblePart.lastIndexOf(")");
                                if (lastOpenParen != -1 && lastCloseParen > lastOpenParen) {
                                    currentDeadline = visiblePart.substring(lastOpenParen + 1, lastCloseParen).trim();
                                }
                                
                                if (!currentDeadline.equals(canvasDeadline)) {
                                    System.out.println("    " + ANSI_GREEN + "🔄 Prazo atualizado: \"" + assignmentName + "\" (" + currentDeadline + " ➔ " + canvasDeadline + ")" + ANSI_RESET);
                                    String commentsPart = firstCommentIdx != -1 ? updatedLine.substring(firstCommentIdx) : "";
                                    updatedLine = newVisiblePart + (commentsPart.isEmpty() ? "" : " " + commentsPart);
                                    lineChanged = true;
                                    updatedCount++;
                                }
                            }

                            // Sincroniza a descrição do Canvas se ela estiver ausente ou desatualizada localmente (atualizando modelo antigo)
                            Object descriptionObj = a.get("description");
                            String canvasNotes = "";
                            if (descriptionObj instanceof String) {
                                String cleanDesc = FileManager.cleanHtml((String) descriptionObj);
                                canvasNotes = cleanDesc.replace("\r\n", "\\n").replace("\n", "\\n").replace("\r", "\\n");
                            }

                            Pattern notesPattern = Pattern.compile("<!--\\s*google_task_notes:\\s*(.*?)\\s*-->");
                            Matcher notesMatcher = notesPattern.matcher(updatedLine);
                            String localNotes = "";
                            if (notesMatcher.find()) {
                                localNotes = notesMatcher.group(1);
                            }

                            if (!localNotes.equals(canvasNotes) && !canvasNotes.isEmpty()) {
                                System.out.println("    📝 Sincronizando notas/descrição de: \"" + a.get("name") + "\"");
                                String tempLine = updatedLine.replaceAll("<!--\\s*google_task_notes:\\s*.*?\\s*-->", "").trim();
                                if (tempLine.contains("<!-- google_task_id:")) {
                                    updatedLine = tempLine.replace("<!-- google_task_id:", "<!-- google_task_notes: " + canvasNotes + " --> <!-- google_task_id:");
                                } else {
                                    updatedLine = tempLine + " <!-- google_task_notes: " + canvasNotes + " -->";
                                }
                                lineChanged = true;
                            }

                            if (lineChanged) {
                                lines.set(i, updatedLine);
                                localModified = true;
                            }
                        }
                    }
                }

                if (localModified) {
                    Files.write(taskFile, lines);
                }

                // Sincroniza as notas do Canvas para esta matéria também!
                syncGradesFromCanvasSilently(s, subjectFolder);

            } catch (Exception e) {
                System.out.println("    " + ANSI_RED + "❌ Erro ao sincronizar: " + e.getMessage() + ANSI_RESET);
            }
        }

        System.out.println("\n" + ANSI_BOLD + ANSI_GREEN + "✔ Verificação concluída! " + updatedCount + " tarefa(s) atualizada(s) para Concluída." + ANSI_RESET);
        pressEnterToContinue(scanner);
    }

    private static void configureGoogleTasks(Scanner scanner) {
        System.out.print("\u001b[2J\u001b[H");
        System.out.println(ANSI_CYAN + "==================================================" + ANSI_RESET);
        System.out.println(ANSI_BOLD + ANSI_BLUE + "        ⚙️ CONFIGURAÇÃO DO GOOGLE TASKS" + ANSI_RESET);
        System.out.println(ANSI_CYAN + "==================================================" + ANSI_RESET);

        Path configPath = getConfigFilePath();
        Map<String, String> config = null;
        try {
            config = FileManager.loadConfig(configPath);
        } catch (IOException e) {
            System.out.println(ANSI_RED + "❌ Falha ao carregar Faculdade/config.json: " + e.getMessage() + ANSI_RESET);
            pressEnterToContinue(scanner);
            return;
        }

        String curClientId = config.get("google_client_id");
        String curClientSecret = config.get("google_client_secret");

        System.out.println("Client ID atual: " + (curClientId == null ? ANSI_YELLOW + "Não configurado" + ANSI_RESET : ANSI_GREEN + curClientId + ANSI_RESET));
        System.out.println("Client Secret atual: " + (curClientSecret == null ? ANSI_YELLOW + "Não configurado" + ANSI_RESET : ANSI_GREEN + "••••••••••••••••" + ANSI_RESET));
        System.out.println("--------------------------------------------------");

        System.out.print("Deseja configurar/alterar as credenciais? (s/N): ");
        String ans = scanner.nextLine().trim().toLowerCase();
        if (ans.equals("s") || ans.equals("sim")) {
            System.out.print("Digite o Google Client ID: ");
            String clientId = scanner.nextLine().trim();
            System.out.print("Digite o Google Client Secret: ");
            String clientSecret = scanner.nextLine().trim();

            if (!clientId.isEmpty() && !clientSecret.isEmpty()) {
                config.put("google_client_id", clientId);
                config.put("google_client_secret", clientSecret);
                try {
                    FileManager.saveConfig(configPath, config);
                    System.out.println(ANSI_GREEN + "✔ Credenciais salvas com sucesso!" + ANSI_RESET);
                } catch (IOException e) {
                    System.out.println(ANSI_RED + "❌ Erro ao salvar credenciais: " + e.getMessage() + ANSI_RESET);
                    pressEnterToContinue(scanner);
                    return;
                }
            } else {
                System.out.println(ANSI_YELLOW + "Configuração cancelada: Client ID e Client Secret não podem ser vazios." + ANSI_RESET);
                pressEnterToContinue(scanner);
                return;
            }
        }

        curClientId = config.get("google_client_id");
        curClientSecret = config.get("google_client_secret");

        if (curClientId == null || curClientSecret == null) {
            System.out.println(ANSI_RED + "❌ Credenciais não configuradas. Não é possível prosseguir com o vínculo." + ANSI_RESET);
            pressEnterToContinue(scanner);
            return;
        }

        System.out.print("\nDeseja realizar a vinculação/autorização com a conta do Google agora? (S/n): ");
        String linkAns = scanner.nextLine().trim().toLowerCase();
        if (linkAns.isEmpty() || linkAns.equals("s") || linkAns.equals("sim")) {
            try {
                String code = GoogleTasksManager.getAuthorizationCode(curClientId);
                if (code == null || code.isEmpty()) {
                    System.out.println(ANSI_RED + "❌ Nenhum código de autorização obtido." + ANSI_RESET);
                    pressEnterToContinue(scanner);
                    return;
                }

                System.out.println("\n📡 Obtendo tokens de acesso do Google...");
                Map<String, Object> tokens = GoogleTasksManager.exchangeCodeForTokens(curClientId, curClientSecret, code);

                String access = (String) tokens.get("access_token");
                String refresh = (String) tokens.get("refresh_token");
                long expiresIn = ((Number) tokens.get("expires_in")).longValue();

                config.put("google_access_token", access);
                if (refresh != null) {
                    config.put("google_refresh_token", refresh);
                }
                config.put("google_token_expiry", String.valueOf((System.currentTimeMillis() / 1000L) + expiresIn));

                FileManager.saveConfig(configPath, config);
                System.out.println(ANSI_BOLD + ANSI_GREEN + "✔ Google Tasks configurado e conectado com sucesso!" + ANSI_RESET);

            } catch (Exception e) {
                System.out.println(ANSI_RED + "❌ Erro ao conectar com o Google Tasks: " + e.getMessage() + ANSI_RESET);
            }
        }

        pressEnterToContinue(scanner);
    }

    private static void syncGoogleTasks(Scanner scanner) {
        System.out.print("\u001b[2J\u001b[H");
        System.out.println(ANSI_CYAN + "==================================================" + ANSI_RESET);
        System.out.println(ANSI_BOLD + ANSI_BLUE + "         🔄 SINCRONIZAÇÃO GOOGLE TASKS" + ANSI_RESET);
        System.out.println(ANSI_CYAN + "==================================================" + ANSI_RESET);

        Path configPath = getConfigFilePath();
        Map<String, String> config = null;
        try {
            config = FileManager.loadConfig(configPath);
        } catch (IOException e) {
            System.out.println(ANSI_RED + "❌ Falha ao carregar Faculdade/config.json: " + e.getMessage() + ANSI_RESET);
            pressEnterToContinue(scanner);
            return;
        }

        String googleClientId = config.get("google_client_id");
        if (googleClientId == null || googleClientId.isEmpty()) {
            System.out.println(ANSI_RED + "❌ Google Tasks não configurado." + ANSI_RESET);
            System.out.println("Use a opção " + ANSI_BOLD + "⚙️ Configurar Google Tasks" + ANSI_RESET + " primeiro.");
            pressEnterToContinue(scanner);
            return;
        }

        List<Subject> enrolledList;
        try {
            List<Subject> subjects = FileManager.loadSubjects(planningPath);
            enrolledList = new ArrayList<>();
            for (Subject s : subjects) {
                if (s.statusChar.equals("c")) {
                    enrolledList.add(s);
                }
            }
        } catch (IOException e) {
            System.out.println(ANSI_RED + "❌ Erro ao carregar matérias locais: " + e.getMessage() + ANSI_RESET);
            pressEnterToContinue(scanner);
            return;
        }

        if (enrolledList.isEmpty()) {
            System.out.println(ANSI_YELLOW + "⏳ Nenhuma matéria cursando atualmente." + ANSI_RESET);
            pressEnterToContinue(scanner);
            return;
        }

        System.out.println("📡 Conectando ao Google Tasks...");
        try {
            String accessToken = GoogleTasksManager.getAccessToken(config, configPath);

            // Busca ou cria a lista global "Faculdade"
            String taskListId = config.get("google_tasks_list_id");
            if (taskListId == null || taskListId.isEmpty()) {
                System.out.println("🔍 Buscando lista 'Faculdade' no Google Tasks...");
                List<Map<String, Object>> lists = GoogleTasksManager.getTaskLists(accessToken);
                for (Map<String, Object> list : lists) {
                    if ("Faculdade".equalsIgnoreCase((String) list.get("title"))) {
                        taskListId = (String) list.get("id");
                        break;
                    }
                }
                if (taskListId == null) {
                    System.out.println("➕ Criando nova lista 'Faculdade' no Google Tasks...");
                    taskListId = GoogleTasksManager.createTaskList(accessToken, "Faculdade");
                }
                config.put("google_tasks_list_id", taskListId);
                FileManager.saveConfig(configPath, config);
            }

            // Busca todas as tarefas da lista global "Faculdade"
            List<Map<String, Object>> googleTasks = GoogleTasksManager.getTasks(accessToken, taskListId);

            for (Subject s : enrolledList) {
                String cleanName = s.getSanitizedFolderName();
                System.out.println("\n🔄 Sincronizando \"" + cleanName + "\"... ");
                Path folder = rootDir.resolve(cleanName);
                if (!Files.exists(folder)) {
                    Files.createDirectories(folder);
                }
                try {
                    syncGoogleTasksForSubject(accessToken, taskListId, googleTasks, s, folder);
                    syncGradesFromCanvasSilently(s, folder);
                    System.out.println(ANSI_GREEN + "✔ Concluído!" + ANSI_RESET);
                } catch (Exception e) {
                    System.out.println(ANSI_RED + "❌ Falha ao sincronizar: " + e.getMessage() + ANSI_RESET);
                }
            }

            System.out.println("\n" + ANSI_BOLD + ANSI_GREEN + "✔ Sincronização geral do Google Tasks concluída!" + ANSI_RESET);

        } catch (Exception e) {
            System.out.println(ANSI_RED + "❌ Falha na sincronização: " + e.getMessage() + ANSI_RESET);
        }

        pressEnterToContinue(scanner);
    }

    public static String parseDueDate(String text) {
        if (text == null) return null;
        Pattern p = Pattern.compile("(\\d{2})/(\\d{2})(?:/(\\d{4}))?");
        Matcher m = p.matcher(text);
        if (m.find()) {
            String day = m.group(1);
            String month = m.group(2);
            String yearStr = m.group(3);
            int year = (yearStr != null) ? Integer.parseInt(yearStr) : java.time.LocalDate.now().getYear();
            return String.format("%04d-%02d-%02dT00:00:00.000Z", year, Integer.parseInt(month), Integer.parseInt(day));
        }
        return null;
    }

    public static String formatDueDateToLocal(String dueAt) {
        if (dueAt == null || dueAt.trim().isEmpty() || dueAt.equals("null")) {
            return "";
        }
        try {
            Pattern p = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})");
            Matcher m = p.matcher(dueAt);
            if (m.find()) {
                String month = m.group(2);
                String day = m.group(3);
                return " (Prazo: " + day + "/" + month + ")";
            }
        } catch (Exception e) {}
        return "";
    }

    @SuppressWarnings("unchecked")
    private static void syncGoogleTasksForSubject(String accessToken, String taskListId, List<Map<String, Object>> allGoogleTasks, Subject subject, Path subjectFolder) throws Exception {
        // 1. Carrega metadados do Google Tasks para essa matéria (apenas synced_task_ids)
        Map<String, Object> meta = FileManager.loadGoogleTasksMeta(subjectFolder);
        String oldListId = (String) meta.get("google_task_list_id");
        List<String> syncedTaskIds = (List<String>) meta.get("synced_task_ids");
        if (syncedTaskIds == null) {
            syncedTaskIds = new ArrayList<>();
        }

        boolean migrating = false;
        if (oldListId != null && !oldListId.isEmpty() && !oldListId.equals("null")) {
            migrating = true;
            syncedTaskIds.clear();
            FileManager.saveGoogleTasksMeta(subjectFolder, null, syncedTaskIds);
        }

        // 2. Lê as tarefas locais do arquivo Tarefas.md
        Path taskFile = subjectFolder.resolve("Tarefas.md");
        if (!Files.exists(taskFile)) {
            List<String> initialLines = new ArrayList<>();
            initialLines.add("# Tasks - " + subject.name);
            initialLines.add("");
            Files.write(taskFile, initialLines);
        }

        List<String> localLines = new ArrayList<>(Files.readAllLines(taskFile));
        long localLastModified = Files.getLastModifiedTime(taskFile).toMillis();

        // 3. Coleta tarefas locais e seus IDs
        Pattern idPattern = Pattern.compile("<!--\\s*google_task_id:\\s*(\\S+)\\s*-->");
        List<String> localIds = new ArrayList<>();

        for (String line : localLines) {
            Matcher m = idPattern.matcher(line);
            if (m.find()) {
                localIds.add(m.group(1));
            }
        }

        // 4. Filtra tarefas do Google que pertencem a esta matéria
        List<Map<String, Object>> subjectGoogleTasks = new ArrayList<>();
        Map<String, Map<String, Object>> googleMap = new HashMap<>();

        for (Map<String, Object> gt : allGoogleTasks) {
            String id = (String) gt.get("id");
            String title = (String) gt.get("title");
            boolean matches = false;

            if (localIds.contains(id)) {
                matches = true;
            } else if (syncedTaskIds.contains(id)) {
                matches = true;
            } else if (title != null && title.trim().startsWith("[" + subject.name + "]")) {
                matches = true;
            }

            if (matches) {
                subjectGoogleTasks.add(gt);
                googleMap.put(id, gt);
            }
        }

        // 5. DETECÇÃO DE EXCLUSÃO LOCAL:
        List<String> idsToDeleteFromGoogle = new ArrayList<>();
        for (String syncedId : syncedTaskIds) {
            if (!localIds.contains(syncedId)) {
                if (googleMap.containsKey(syncedId)) {
                    idsToDeleteFromGoogle.add(syncedId);
                }
            }
        }
        for (String id : idsToDeleteFromGoogle) {
            System.out.println("  • " + ANSI_YELLOW + "Deletando no Google (removida localmente): " + ANSI_RESET + String.valueOf(googleMap.get(id).get("title")));
            try {
                GoogleTasksManager.deleteTask(accessToken, taskListId, id);
            } catch (Exception e) {
                // ignorar
            }
            googleMap.remove(id);
        }
        syncedTaskIds.removeAll(idsToDeleteFromGoogle);

        // 6. SINCRONIZAÇÃO DAS TAREFAS
        boolean localModified = false;
        List<String> newSyncedTaskIds = new ArrayList<>(syncedTaskIds);

        // A. Processa cada linha do arquivo local
        for (int i = 0; i < localLines.size(); i++) {
            String line = localLines.get(i);
            String trimmed = line.trim();
            if (!(trimmed.startsWith("- [ ]") || trimmed.startsWith("- [x]") || trimmed.startsWith("- [X]"))) {
                continue;
            }

            boolean isLocalCompleted = trimmed.startsWith("- [x]") || trimmed.startsWith("- [X]");

            // Extrai a descrição limpa
            String cleanTitle = line.replaceFirst("^\\s*-\\s*\\[[ xX]\\]\\s*", "");
            cleanTitle = cleanTitle.replaceAll("<!--\\s*google_task_id:\\s*\\S+\\s*-->", "");
            cleanTitle = cleanTitle.replaceAll("<!--\\s*canvas_id:\\s*\\S+\\s*-->", "");
            cleanTitle = cleanTitle.replaceAll("<!--\\s*google_task_notes:\\s*.*?\\s*-->", "");
            cleanTitle = cleanTitle.trim();

            if (cleanTitle.toLowerCase().matches(".*\\bada\\b.*")) {
                Matcher mId = idPattern.matcher(line);
                if (mId.find()) {
                    String googleTaskId = mId.group(1);
                    try {
                        GoogleTasksManager.deleteTask(accessToken, taskListId, googleTaskId);
                    } catch (Exception e) {}
                    String updatedLine = line.replaceAll("<!--\\s*google_task_id:\\s*\\S+\\s*-->", "").trim();
                    localLines.set(i, updatedLine);
                    localModified = true;
                }
                continue;
            }

            // Extrai notas se houver comentário de notes local
            String notes = "";
            Pattern notesPattern = Pattern.compile("<!--\\s*google_task_notes:\\s*(.*?)\\s*-->");
            Matcher notesMatcher = notesPattern.matcher(line);
            if (notesMatcher.find()) {
                notes = notesMatcher.group(1).replace("\\n", "\n");
            }

            // Extrai a data de vencimento da linha local
            String due = parseDueDate(line);

            Matcher m = idPattern.matcher(line);
            if (m.find()) {
                String googleTaskId = m.group(1);

                if (googleMap.containsKey(googleTaskId)) {
                    Map<String, Object> gt = googleMap.get(googleTaskId);
                    boolean isGoogleCompleted = "completed".equals(gt.get("status"));
                    String googleTitle = (String) gt.get("title");
                    String googleNotes = (String) gt.get("notes");
                    String googleDue = (String) gt.get("due");

                    String formattedTitle = "[" + subject.name + "] " + cleanTitle;
                    boolean titleMismatch = googleTitle == null || !googleTitle.equals(formattedTitle);

                    String cleanGoogleNotes = googleNotes == null ? "" : googleNotes.trim();
                    String cleanLocalNotes = notes == null ? "" : notes.trim();
                    boolean notesMismatch = !cleanGoogleNotes.equals(cleanLocalNotes);

                    String googleDateOnly = "";
                    if (googleDue != null && googleDue.length() >= 10) {
                        googleDateOnly = googleDue.substring(0, 10);
                    }
                    String localDateOnly = "";
                    if (due != null && due.length() >= 10) {
                        localDateOnly = due.substring(0, 10);
                    }
                    boolean dueMismatch = !googleDateOnly.equals(localDateOnly);

                    if (isLocalCompleted != isGoogleCompleted || titleMismatch || notesMismatch || dueMismatch) {
                        // Conflito de status ou divergência de modelo (título, notas, prazo)!
                        String updatedStr = (String) gt.get("updated");
                        long googleUpdatedTime = 0;
                        if (updatedStr != null) {
                            try {
                                googleUpdatedTime = java.time.Instant.parse(updatedStr).toEpochMilli();
                            } catch (Exception e) {}
                        }

                        if (googleUpdatedTime > localLastModified && !titleMismatch && !notesMismatch) {
                            // Google é mais recente -> Atualiza local
                            System.out.println("  • Atualizando localmente: \"" + cleanTitle + "\" -> " + (isGoogleCompleted ? "Concluída" : "Pendente"));
                            String statusChar = isGoogleCompleted ? "x" : " ";
                            String updatedLine = line.replaceFirst("\\[[ xX]\\]", "[" + statusChar + "]");

                            if (googleNotes != null && !googleNotes.isEmpty()) {
                                String escapedGoogleNotes = googleNotes.replace("\r\n", "\\n").replace("\n", "\\n").replace("\r", "\\n");
                                updatedLine = updatedLine.replaceAll("<!--\\s*google_task_notes:\\s*.*?\\s*-->", "");
                                updatedLine = updatedLine.trim() + " <!-- google_task_notes: " + escapedGoogleNotes + " -->";
                            }

                            localLines.set(i, updatedLine);
                            localModified = true;
                        } else {
                            // Local é mais recente ou divergência gerada no sistema -> Atualiza Google Tasks
                            System.out.println("  • Atualizando no Google Tasks: \"" + cleanTitle + "\" (Sincronizando notas/prazos)");
                            GoogleTasksManager.updateTask(accessToken, taskListId, googleTaskId, formattedTitle, isLocalCompleted, notes.isEmpty() ? null : notes, due);
                        }
                    }
                } else {
                    if (syncedTaskIds.contains(googleTaskId) && !migrating) {
                        // Google Task ID existe localmente e já foi sincronizado antes com esta lista, mas não existe no Google Tasks -> Exclui local
                        System.out.println("  • Removendo localmente (excluída no Google): \"" + cleanTitle + "\"");
                        localLines.remove(i);
                        i--;
                        localModified = true;
                        newSyncedTaskIds.remove(googleTaskId);
                    } else {
                        // Não estava sincronizado com esta lista ainda ou migração de lista.
                        // Remove o ID do Google antigo/inválido para recriar na nova lista.
                        String updatedLine = line.replaceAll("<!--\\s*google_task_id:\\s*\\S+\\s*-->", "").trim();
                        localLines.set(i, updatedLine);
                        localModified = true;
                        i--; // Força reprocessamento da linha para criar no Google Tasks
                    }
                }
            } else {
                // Tarefa local sem ID do Google -> Criar ou Vincular no Google Tasks
                String formattedTitle = "[" + subject.name + "] " + cleanTitle;

                Map<String, Object> existingGoogleTask = null;
                for (Map<String, Object> gt : subjectGoogleTasks) {
                    String title = (String) gt.get("title");
                    if (title != null && title.trim().equalsIgnoreCase(formattedTitle)) {
                        String id = (String) gt.get("id");
                        if (!localIds.contains(id)) {
                            existingGoogleTask = gt;
                            break;
                        }
                    }
                }

                String newId;
                if (existingGoogleTask != null) {
                    newId = (String) existingGoogleTask.get("id");
                    System.out.println("  • Vinculando ao Google Tasks existente: \"" + cleanTitle + "\"");
                    boolean isGoogleCompleted = "completed".equals(existingGoogleTask.get("status"));
                    if (isLocalCompleted != isGoogleCompleted) {
                        GoogleTasksManager.updateTask(accessToken, taskListId, newId, formattedTitle, isLocalCompleted, notes.isEmpty() ? null : notes, due);
                    }
                } else {
                    System.out.println("  • Enviando ao Google Tasks: \"" + cleanTitle + "\"");
                    Map<String, Object> newTask = GoogleTasksManager.createTask(accessToken, taskListId, formattedTitle, isLocalCompleted, notes, due);
                    newId = (String) newTask.get("id");
                }

                String updatedLine = line.trim() + " <!-- google_task_id: " + newId + " -->";
                int leadingSpaces = line.length() - line.stripLeading().length();
                if (leadingSpaces > 0) {
                    updatedLine = " ".repeat(leadingSpaces) + updatedLine;
                }

                localLines.set(i, updatedLine);
                localModified = true;
                newSyncedTaskIds.add(newId);
                localIds.add(newId);
            }
        }

        // B. Processa tarefas no Google que não existem no arquivo local
        for (Map<String, Object> gt : subjectGoogleTasks) {
            String googleTaskId = (String) gt.get("id");
            if (!localIds.contains(googleTaskId) && !syncedTaskIds.contains(googleTaskId)) {
                String googleTitle = (String) gt.get("title");
                String googleNotes = (String) gt.get("notes");
                String googleDue = (String) gt.get("due");

                // Remove o prefixo da matéria do título para salvar localmente
                String localTitle = googleTitle;
                if (googleTitle != null && googleTitle.startsWith("[" + subject.name + "]")) {
                    localTitle = googleTitle.substring(("[" + subject.name + "]").length()).trim();
                }

                if (localTitle != null && localTitle.toLowerCase().matches(".*\\bada\\b.*")) {
                    try {
                        GoogleTasksManager.deleteTask(accessToken, taskListId, googleTaskId);
                    } catch (Exception e) {}
                    continue;
                }

                boolean isCompleted = "completed".equals(gt.get("status"));
                String statusChar = isCompleted ? "x" : " ";

                // Adiciona o prazo se houver
                String dueSuffix = formatDueDateToLocal(googleDue);

                // Adiciona as notas se houver
                String notesComment = "";
                if (googleNotes != null && !googleNotes.isEmpty()) {
                    String escapedGoogleNotes = googleNotes.replace("\r\n", "\\n").replace("\n", "\\n").replace("\r", "\\n");
                    notesComment = " <!-- google_task_notes: " + escapedGoogleNotes + " -->";
                }

                System.out.println("  • Puxando do Google Tasks: \"" + localTitle + "\"");
                localLines.add("- [" + statusChar + "] " + localTitle + dueSuffix + notesComment + " <!-- google_task_id: " + googleTaskId + " -->");
                localModified = true;
                newSyncedTaskIds.add(googleTaskId);
            }
        }

        // 7. Salva arquivos locais
        if (localModified) {
            Files.write(taskFile, localLines);
        }
        FileManager.saveGoogleTasksMeta(subjectFolder, null, newSyncedTaskIds);
    }
    private static void importCanvasFiles(Scanner scanner) {
        try {
            List<Subject> subjects = FileManager.loadSubjects(planningPath);
            List<Subject> enrolledList = new ArrayList<>();
            for (Subject s : subjects) {
                if (s.statusChar.equals("c")) {
                    Path folder = rootDir.resolve(s.getSanitizedFolderName());
                    String linkedId = FileManager.getCanvasCourseId(folder);
                    if (linkedId != null && !linkedId.trim().isEmpty()) {
                        enrolledList.add(s);
                    }
                }
            }

            if (enrolledList.isEmpty()) {
                System.out.println("\n" + ANSI_YELLOW + "⏳ Nenhuma matéria cursando possui vínculo ativo com o Canvas." + ANSI_RESET);
                System.out.println("Use a opção de vincular matérias ou importar tarefas primeiro.");
                pressEnterToContinue(scanner);
                return;
            }

            List<String> options = new ArrayList<>();
            for (Subject s : enrolledList) {
                options.add("(" + s.period + ") " + s.name);
            }
            int choice = InteractiveMenu.select("       📂 SELECIONE A MATÉRIA PARA IMPORTAR ARQUIVOS", options);
            if (choice == -1) {
                return;
            }
            Subject selected = enrolledList.get(choice);
            runSubjectFileImportMenu(scanner, selected);

        } catch (Exception e) {
            System.err.println("Erro ao carregar matérias: " + e.getMessage());
            pressEnterToContinue(scanner);
        }
    }

    private static void runSubjectFileImportMenu(Scanner scanner, Subject subject) {
        String token = System.getenv("CANVAS_TOKEN");
        Path configPath = getConfigFilePath();
        Map<String, String> config = null;
        try {
            config = FileManager.loadConfig(configPath);
        } catch (IOException e) {
            System.out.println(ANSI_RED + "❌ Falha ao carregar Faculdade/config.json: " + e.getMessage() + ANSI_RESET);
            pressEnterToContinue(scanner);
            return;
        }

        if (token == null || token.trim().isEmpty()) {
            token = config.get("canvas_token");
        }

        String canvasUrl = config.get("canvas_url");
        if (canvasUrl == null || canvasUrl.trim().isEmpty()) {
            canvasUrl = "https://pucminas.instructure.com";
        }

        if (token == null || token.trim().isEmpty() || token.equals("11748~EKPzTKrrf8FnreKWT7CY4JX6wEMU6Nty3Gunf22DFuHnLRm6JY8xXntDHfvCDFkP")) {
            System.out.println(ANSI_BOLD + ANSI_RED + "❌ Erro: Token do Canvas não encontrado." + ANSI_RESET);
            System.out.println("Configure o token no Faculdade/config.json ou na variável CANVAS_TOKEN.");
            pressEnterToContinue(scanner);
            return;
        }

        String courseName = subject.getSanitizedFolderName();
        Path subjectFolder = rootDir.resolve(courseName);
        String courseId = FileManager.getCanvasCourseId(subjectFolder);
        if (courseId == null || courseId.isEmpty()) {
            System.out.println(ANSI_RED + "\n❌ Esta matéria não está vinculada a nenhum curso do Canvas." + ANSI_RESET);
            System.out.println("Use a opção de importar tarefas primeiro para vincular.");
            pressEnterToContinue(scanner);
            return;
        }

        List<String> options = Arrays.asList(
            "📄 Baixar arquivo individual",
            "📁 Baixar pasta de arquivos",
            "⬅ Voltar"
        );

        while (true) {
            int choice = InteractiveMenu.select("📂 IMPORTAR DO CANVAS - " + subject.name.toUpperCase(), options);
            if (choice == -1 || choice == 2) {
                break;
            }
            try {
                if (choice == 0) {
                    downloadIndividualFileFlow(scanner, canvasUrl, token, courseId, subject, subjectFolder);
                } else if (choice == 1) {
                    downloadFolderFlow(scanner, canvasUrl, token, courseId, subject, subjectFolder);
                }
            } catch (Exception e) {
                System.out.println(ANSI_RED + "\n❌ Erro durante o download: " + e.getMessage() + ANSI_RESET);
                pressEnterToContinue(scanner);
            }
        }
    }

    private static Path selectLocalDestinationFolder(Scanner scanner, Path subjectFolder) {
        Path currentLocalPath = subjectFolder;
        Stack<Path> localPathHistory = new Stack<>();

        while (true) {
            List<Path> localDirs = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(currentLocalPath)) {
                for (Path entry : stream) {
                    if (Files.isDirectory(entry)) {
                        localDirs.add(entry);
                    }
                }
            } catch (IOException e) {
                // ignore
            }

            localDirs.sort(Comparator.comparing(p -> p.getFileName().toString().toLowerCase()));

            List<String> options = new ArrayList<>();
            options.add("💾 Salvar nesta pasta");
            options.add("➕ Criar nova pasta e salvar aqui");

            boolean hasBack = !localPathHistory.isEmpty();
            if (hasBack) {
                options.add("⬅ Voltar para pasta anterior");
            }

            int specialCount = options.size();

            for (Path dir : localDirs) {
                options.add("📁 " + dir.getFileName().toString());
            }

            String relativeDest = rootDir.relativize(currentLocalPath).toString();
            if (relativeDest.isEmpty()) relativeDest = ".";
            String menuTitle = "Escolha a pasta de destino local (Navegando: /" + relativeDest + "):";
            
            int choice = InteractiveMenu.select(menuTitle, options);
            if (choice == -1) {
                return null;
            }

            if (choice == 0) {
                return currentLocalPath;
            } else if (choice == 1) {
                System.out.print("\nDigite o nome da pasta a ser criada localmente: ");
                String folderNameInput = scanner.nextLine().trim();
                if (!folderNameInput.isEmpty()) {
                    Path newPath = currentLocalPath.resolve(sanitizeFileName(folderNameInput));
                    try {
                        Files.createDirectories(newPath);
                        return newPath;
                    } catch (IOException e) {
                        System.out.println(ANSI_RED + "❌ Falha ao criar pasta: " + e.getMessage() + ANSI_RESET);
                        pressEnterToContinue(scanner);
                    }
                }
            } else if (hasBack && choice == 2) {
                currentLocalPath = localPathHistory.pop();
            } else {
                int dirIdx = choice - specialCount;
                if (dirIdx >= 0 && dirIdx < localDirs.size()) {
                    localPathHistory.push(currentLocalPath);
                    currentLocalPath = localDirs.get(dirIdx);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void downloadIndividualFileFlow(Scanner scanner, String canvasUrl, String token, String courseId, Subject subject, Path subjectFolder) throws Exception {
        System.out.println("📡 Carregando estrutura de arquivos e pastas do Canvas (este processo pode demorar alguns segundos)...");
        List<Map<String, Object>> allFolders = CanvasManager.getCourseFolders(canvasUrl, token, courseId);
        List<Map<String, Object>> allFiles = CanvasManager.getCourseFiles(canvasUrl, token, courseId);

        // Constrói mapas em memória
        Map<String, Map<String, Object>> folderMap = new HashMap<>();
        Map<String, List<Map<String, Object>>> subfolderMap = new HashMap<>();
        Map<String, List<Map<String, Object>>> fileMap = new HashMap<>();

        for (Map<String, Object> f : allFolders) {
            String id = String.valueOf(f.get("id"));
            folderMap.put(id, f);
        }
        for (Map<String, Object> f : allFolders) {
            Object parentIdObj = f.get("parent_folder_id");
            if (parentIdObj != null) {
                String parentId = String.valueOf(parentIdObj);
                if (!parentId.equals("null")) {
                    subfolderMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(f);
                }
            }
        }
        for (Map<String, Object> f : allFiles) {
            Object folderIdObj = f.get("folder_id");
            if (folderIdObj != null) {
                String folderId = String.valueOf(folderIdObj);
                fileMap.computeIfAbsent(folderId, k -> new ArrayList<>()).add(f);
            }
        }

        // Localiza a pasta raiz
        Map<String, Object> rootFolder = null;
        for (Map<String, Object> f : allFolders) {
            Object p = f.get("parent_folder_id");
            if (p == null || String.valueOf(p).equals("null")) {
                rootFolder = f;
                break;
            }
        }
        if (rootFolder == null) {
            rootFolder = CanvasManager.getRootFolder(canvasUrl, token, courseId);
        }

        Map<String, Object> currentFolder = rootFolder;
        Stack<Map<String, Object>> folderHistory = new Stack<>();

        List<Map<String, Object>> selectedFiles = new ArrayList<>();

        while (true) {
            String folderId = String.valueOf(currentFolder.get("id"));
            List<Map<String, Object>> subfolders = subfolderMap.getOrDefault(folderId, new ArrayList<>());
            List<Map<String, Object>> files = fileMap.getOrDefault(folderId, new ArrayList<>());

            subfolders.sort(Comparator.comparing(f -> String.valueOf(f.get("name")).toLowerCase()));
            files.sort(Comparator.comparing(f -> String.valueOf(f.get("display_name")).toLowerCase()));

            List<String> options = new ArrayList<>();
            boolean hasFiles = !files.isEmpty();
            if (hasFiles) {
                options.add("📥 Selecionar múltiplos arquivos para baixar");
            }

            boolean hasBack = !folderHistory.isEmpty();
            if (hasBack) {
                options.add("⬅ Voltar para pasta anterior");
            }

            int specialCount = options.size();

            for (Map<String, Object> sub : subfolders) {
                options.add("📁 " + sub.get("name"));
            }

            int subfolderCount = subfolders.size();

            for (Map<String, Object> f : files) {
                long size = 0;
                Object sizeObj = f.get("size");
                if (sizeObj instanceof Number) {
                    size = ((Number) sizeObj).longValue();
                }
                options.add("📄 " + f.get("display_name") + " (" + formatFileSize(size) + ")");
            }

            String menuTitle = "Navegando Canvas: /" + (currentFolder.get("full_name") != null ? currentFolder.get("full_name") : currentFolder.get("name")) + "\nSelecione um arquivo para baixar ou pasta para navegar:";
            int choice = InteractiveMenu.select(menuTitle, options);
            if (choice == -1) {
                return;
            }

            int currentChoiceIdx = 0;
            boolean clickedMulti = false;
            if (hasFiles) {
                if (choice == currentChoiceIdx) {
                    clickedMulti = true;
                }
                currentChoiceIdx++;
            }
            boolean clickedBack = false;
            if (hasBack) {
                if (choice == currentChoiceIdx) {
                    clickedBack = true;
                }
                currentChoiceIdx++;
            }

            if (clickedMulti) {
                List<String> multiOptions = new ArrayList<>();
                for (Map<String, Object> f : files) {
                    long size = 0;
                    Object sizeObj = f.get("size");
                    if (sizeObj instanceof Number) {
                        size = ((Number) sizeObj).longValue();
                    }
                    multiOptions.add(f.get("display_name") + " (" + formatFileSize(size) + ")");
                }
                List<Integer> selectedIndexes = InteractiveMenu.selectMultiple("Selecione os arquivos para baixar:", multiOptions);
                if (selectedIndexes != null && !selectedIndexes.isEmpty()) {
                    for (int idx : selectedIndexes) {
                        selectedFiles.add(files.get(idx));
                    }
                    break;
                }
            } else if (clickedBack) {
                currentFolder = folderHistory.pop();
            } else {
                int subIdx = choice - specialCount;
                if (subIdx >= 0 && subIdx < subfolderCount) {
                    folderHistory.push(currentFolder);
                    currentFolder = subfolders.get(subIdx);
                } else if (subIdx >= subfolderCount) {
                    int fileIdx = subIdx - subfolderCount;
                    if (fileIdx >= 0 && fileIdx < files.size()) {
                        selectedFiles.add(files.get(fileIdx));
                        break;
                    }
                }
            }
        }

        if (selectedFiles.isEmpty()) return;

        Path targetPath = selectLocalDestinationFolder(scanner, subjectFolder);
        if (targetPath == null) return;

        System.out.println();
        int successCount = 0;
        for (Map<String, Object> f : selectedFiles) {
            String displayName = (String) f.get("display_name");
            String url = (String) f.get("url");
            if (url == null || url.isEmpty()) {
                System.out.println(ANSI_RED + "❌ O arquivo '" + displayName + "' não possui link de download válido." + ANSI_RESET);
                continue;
            }

            Path destFile = targetPath.resolve(sanitizeFileName(displayName));
            System.out.println("📥 Baixando: " + displayName + " -> " + rootDir.relativize(destFile));
            try {
                CanvasManager.downloadFile(url, token, destFile);
                successCount++;
            } catch (Exception e) {
                System.out.println(ANSI_RED + "  ❌ Falha no download: " + e.getMessage() + ANSI_RESET);
            }
        }

        System.out.println(ANSI_GREEN + "\n✔ Concluído! " + successCount + " de " + selectedFiles.size() + " arquivos baixados com sucesso." + ANSI_RESET);
        pressEnterToContinue(scanner);
    }

    @SuppressWarnings("unchecked")
    private static void downloadFolderFlow(Scanner scanner, String canvasUrl, String token, String courseId, Subject subject, Path subjectFolder) throws Exception {
        System.out.println("📡 Carregando estrutura de arquivos e pastas do Canvas (este processo pode demorar alguns segundos)...");
        List<Map<String, Object>> allFolders = CanvasManager.getCourseFolders(canvasUrl, token, courseId);
        List<Map<String, Object>> allFiles = CanvasManager.getCourseFiles(canvasUrl, token, courseId);

        // Constrói mapas em memória
        Map<String, Map<String, Object>> folderMap = new HashMap<>();
        Map<String, List<Map<String, Object>>> subfolderMap = new HashMap<>();
        Map<String, List<Map<String, Object>>> fileMap = new HashMap<>();

        for (Map<String, Object> f : allFolders) {
            String id = String.valueOf(f.get("id"));
            folderMap.put(id, f);
        }
        for (Map<String, Object> f : allFolders) {
            Object parentIdObj = f.get("parent_folder_id");
            if (parentIdObj != null) {
                String parentId = String.valueOf(parentIdObj);
                if (!parentId.equals("null")) {
                    subfolderMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(f);
                }
            }
        }
        for (Map<String, Object> f : allFiles) {
            Object folderIdObj = f.get("folder_id");
            if (folderIdObj != null) {
                String folderId = String.valueOf(folderIdObj);
                fileMap.computeIfAbsent(folderId, k -> new ArrayList<>()).add(f);
            }
        }

        // Localiza a pasta raiz
        Map<String, Object> rootFolder = null;
        for (Map<String, Object> f : allFolders) {
            Object p = f.get("parent_folder_id");
            if (p == null || String.valueOf(p).equals("null")) {
                rootFolder = f;
                break;
            }
        }
        if (rootFolder == null) {
            rootFolder = CanvasManager.getRootFolder(canvasUrl, token, courseId);
        }

        Map<String, Object> currentFolder = rootFolder;
        Stack<Map<String, Object>> folderHistory = new Stack<>();

        String selectedFolderId = null;
        String selectedFolderName = null;

        while (true) {
            String folderId = String.valueOf(currentFolder.get("id"));
            List<Map<String, Object>> subfolders = subfolderMap.getOrDefault(folderId, new ArrayList<>());

            subfolders.sort(Comparator.comparing(f -> String.valueOf(f.get("name")).toLowerCase()));

            List<String> options = new ArrayList<>();
            options.add("💾 Baixar esta pasta inteira");

            boolean hasBack = !folderHistory.isEmpty();
            if (hasBack) {
                options.add("⬅ Voltar para pasta anterior");
            }

            int specialCount = options.size();

            for (Map<String, Object> sub : subfolders) {
                options.add("📁 " + sub.get("name"));
            }

            int subfolderCount = subfolders.size();

            String menuTitle = "Navegando Canvas: /" + (currentFolder.get("full_name") != null ? currentFolder.get("full_name") : currentFolder.get("name")) + "\nSelecione uma pasta para navegar ou escolha uma ação:";
            int choice = InteractiveMenu.select(menuTitle, options);
            if (choice == -1) {
                return;
            }

            if (choice == 0) {
                selectedFolderId = folderId;
                selectedFolderName = (String) currentFolder.get("name");
                if (selectedFolderName == null || selectedFolderName.equals("course_files")) {
                    selectedFolderName = subject.getSanitizedFolderName();
                }
                break;
            } else if (hasBack && choice == 1) {
                currentFolder = folderHistory.pop();
            } else {
                int subfolderIndex = choice - specialCount;
                if (subfolderIndex >= 0 && subfolderIndex < subfolderCount) {
                    folderHistory.push(currentFolder);
                    currentFolder = subfolders.get(subfolderIndex);
                }
            }
        }

        if (selectedFolderId == null) return;

        Path targetPath = selectLocalDestinationFolder(scanner, subjectFolder);
        if (targetPath == null) return;

        Path downloadDestPath = targetPath;
        String rootId = String.valueOf(rootFolder.get("id"));
        if (!selectedFolderId.equals(rootId)) {
            String sanitizedCanvasName = sanitizeFileName(selectedFolderName);
            boolean match = false;
            String tName = targetPath.getFileName().toString().toLowerCase();
            String cName = sanitizedCanvasName.toLowerCase();
            if (tName.equals(cName) || tName.contains(cName) || cName.contains(tName)) {
                match = true;
            }
            if (!match) {
                downloadDestPath = targetPath.resolve(sanitizedCanvasName);
                try {
                    Files.createDirectories(downloadDestPath);
                } catch (IOException e) {
                    downloadDestPath = targetPath;
                }
            }
        }

        System.out.println("\n📡 Iniciando download recursivo da pasta...");
        downloadCanvasFolderRecursivelyOffline(token, selectedFolderId, downloadDestPath, subfolderMap, fileMap);
        System.out.println(ANSI_GREEN + "\n✔ Download da pasta inteira concluído!" + ANSI_RESET);
        pressEnterToContinue(scanner);
    }

    private static void downloadCanvasFolderRecursivelyOffline(String token, String folderId, Path localPath, Map<String, List<Map<String, Object>>> subfolderMap, Map<String, List<Map<String, Object>>> fileMap) throws Exception {
        List<Map<String, Object>> files = fileMap.getOrDefault(folderId, new ArrayList<>());
        for (Map<String, Object> f : files) {
            String displayName = (String) f.get("display_name");
            String url = (String) f.get("url");
            if (url == null || url.isEmpty()) continue;

            Path destFile = localPath.resolve(sanitizeFileName(displayName));
            System.out.println("  • Baixando arquivo: " + displayName + " -> " + rootDir.relativize(destFile));
            try {
                CanvasManager.downloadFile(url, token, destFile);
            } catch (Exception e) {
                System.out.println(ANSI_RED + "    ❌ Erro ao baixar " + displayName + ": " + e.getMessage() + ANSI_RESET);
            }
        }

        List<Map<String, Object>> subfolders = subfolderMap.getOrDefault(folderId, new ArrayList<>());
        for (Map<String, Object> sub : subfolders) {
            String subName = (String) sub.get("name");
            String subId = String.valueOf(sub.get("id"));
            Path subLocalPath = localPath.resolve(sanitizeFileName(subName));

            System.out.println("📂 Entrando na pasta: " + subName);
            downloadCanvasFolderRecursivelyOffline(token, subId, subLocalPath, subfolderMap, fileMap);
        }
    }

    private static String sanitizeFileName(String name) {
        if (name == null) return "arquivo";
        return name.replaceAll("[\\\\/:*?\"<>|]", " ").trim();
    }

    private static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format(Locale.US, "%.1f %cB", bytes / Math.pow(1024, exp), pre);
    }
    private static void syncCanvasCoursesMenu(Scanner scanner) {
        String token = System.getenv("CANVAS_TOKEN");
        Path configPath = getConfigFilePath();
        Map<String, String> config = null;
        try {
            config = FileManager.loadConfig(configPath);
        } catch (IOException e) {
            System.out.println(ANSI_RED + "❌ Falha ao carregar Faculdade/config.json: " + e.getMessage() + ANSI_RESET);
            pressEnterToContinue(scanner);
            return;
        }

        if (token == null || token.trim().isEmpty()) {
            token = config.get("canvas_token");
        }

        String canvasUrl = config.get("canvas_url");
        if (canvasUrl == null || canvasUrl.trim().isEmpty()) {
            canvasUrl = "https://pucminas.instructure.com";
        }

        if (token == null || token.trim().isEmpty() || token.equals("11748~EKPzTKrrf8FnreKWT7CY4JX6wEMU6Nty3Gunf22DFuHnLRm6JY8xXntDHfvCDFkP")) {
            System.out.println(ANSI_BOLD + ANSI_RED + "❌ Erro: Token do Canvas não encontrado." + ANSI_RESET);
            System.out.println("Configure o token no Faculdade/config.json ou na variável CANVAS_TOKEN.");
            pressEnterToContinue(scanner);
            return;
        }

        List<String> options = Arrays.asList(
            "🔗 Sincronização Assistida (Automática)",
            "🔗 Sincronização Manual",
            "⬅ Voltar"
        );

        while (true) {
            int choice = InteractiveMenu.select("🔗 VINCULAR MATÉRIAS AO CANVAS", options);
            if (choice == -1 || choice == 2) {
                break;
            }
            if (choice == 0) {
                runCanvasCoursesAssistedSync(scanner, canvasUrl, token);
                pressEnterToContinue(scanner);
            } else if (choice == 1) {
                runCanvasCoursesManualSync(scanner, canvasUrl, token);
                pressEnterToContinue(scanner);
            }
        }
    }

    private static void runCanvasCoursesAssistedSync(Scanner scanner, String canvasUrl, String token) {
        System.out.println("📡 Conectando ao Canvas para listar cursos...");
        List<Map<String, Object>> canvasCourses;
        List<Subject> enrolledList;
        try {
            canvasCourses = CanvasManager.getCourses(canvasUrl, token);
            List<Subject> subjects = FileManager.loadSubjects(planningPath);
            enrolledList = new ArrayList<>();
            for (Subject s : subjects) {
                if (s.statusChar.equals("c")) {
                    enrolledList.add(s);
                }
            }
        } catch (Exception e) {
            System.out.println(ANSI_RED + "❌ Erro de conexão ou carregamento: " + e.getMessage() + ANSI_RESET);
            return;
        }

        if (enrolledList.isEmpty()) {
            System.out.println(ANSI_YELLOW + "⏳ Nenhuma matéria cursando no momento localmente." + ANSI_RESET);
            return;
        }

        System.out.println("\n🔍 Executando pareamento assistido...");
        int linkedCount = 0;

        for (Subject s : enrolledList) {
            Path folder = rootDir.resolve(s.getSanitizedFolderName());
            String existingId = FileManager.getCanvasCourseId(folder);
            if (existingId != null && !existingId.isEmpty()) {
                System.out.println("  • " + s.name + " -> " + ANSI_GREEN + "Já está vinculada (ID: " + existingId + ")" + ANSI_RESET);
                continue;
            }

            Map<String, Object> bestMatch = null;
            String cleanLocalName = s.name.toLowerCase().trim();

            String acronym = null;
            Pattern acPat = Pattern.compile("\\(([^\\)]+)\\)");
            Matcher acMat = acPat.matcher(s.name);
            if (acMat.find()) {
                acronym = acMat.group(1).trim().toLowerCase();
            }

            for (Map<String, Object> cc : canvasCourses) {
                String ccName = (String) cc.get("name");
                if (ccName == null) continue;
                String ccClean = cleanCourseName(ccName).toLowerCase().trim();

                if (ccClean.equals(cleanLocalName)) {
                    bestMatch = cc;
                    break;
                }
                if (acronym != null) {
                    if (ccName.toLowerCase().matches(".*\\b" + Pattern.quote(acronym) + "\\b.*")) {
                        bestMatch = cc;
                        break;
                    }
                }
                if (ccClean.length() > 4 && cleanLocalName.contains(ccClean)) {
                    bestMatch = cc;
                } else if (cleanLocalName.length() > 4 && ccClean.contains(cleanLocalName)) {
                    bestMatch = cc;
                }
            }

            if (bestMatch != null) {
                String courseId = String.valueOf(bestMatch.get("id"));
                String courseName = (String) bestMatch.get("name");
                FileManager.saveCanvasCourseId(folder, courseId);
                System.out.println("  • " + s.name + " -> " + ANSI_GREEN + "Vinculada ao curso '" + cleanCourseName(courseName) + "' (ID: " + courseId + ")" + ANSI_RESET);
                linkedCount++;
            } else {
                System.out.println("  • " + s.name + " -> " + ANSI_YELLOW + "Não foi possível parear automaticamente." + ANSI_RESET);
            }
        }

        System.out.println("\n✔ Sincronização concluída! " + linkedCount + " matéria(s) vinculada(s) com sucesso.");
    }

    private static void runCanvasCoursesManualSync(Scanner scanner, String canvasUrl, String token) {
        List<Subject> enrolledList;
        List<Map<String, Object>> canvasCourses;
        try {
            List<Subject> subjects = FileManager.loadSubjects(planningPath);
            enrolledList = new ArrayList<>();
            for (Subject s : subjects) {
                if (s.statusChar.equals("c")) {
                    enrolledList.add(s);
                }
            }
            System.out.println("📡 Conectando ao Canvas para buscar cursos...");
            canvasCourses = CanvasManager.getCourses(canvasUrl, token);
        } catch (Exception e) {
            System.out.println(ANSI_RED + "❌ Erro de conexão ou carregamento: " + e.getMessage() + ANSI_RESET);
            return;
        }

        if (enrolledList.isEmpty()) {
            System.out.println(ANSI_YELLOW + "⏳ Nenhuma matéria cursando no momento localmente." + ANSI_RESET);
            return;
        }

        List<String> localOptions = new ArrayList<>();
        for (Subject s : enrolledList) {
            Path folder = rootDir.resolve(s.getSanitizedFolderName());
            String existingId = FileManager.getCanvasCourseId(folder);
            String status = (existingId != null && !existingId.isEmpty()) ? " [Vinculada: " + existingId + "]" : " [Sem Vínculo]";
            localOptions.add("(" + s.period + ") " + s.name + status);
        }

        int localChoice = InteractiveMenu.select("Selecione a matéria local para vincular:", localOptions);
        if (localChoice == -1) return;

        Subject selectedSubject = enrolledList.get(localChoice);
        Path folder = rootDir.resolve(selectedSubject.getSanitizedFolderName());

        List<String> canvasOptions = new ArrayList<>();
        for (Map<String, Object> cc : canvasCourses) {
            canvasOptions.add(String.valueOf(cc.get("id")) + " - " + cleanCourseName((String) cc.get("name")));
        }

        int canvasChoice = InteractiveMenu.select("Selecione o curso correspondente do Canvas:", canvasOptions);
        if (canvasChoice == -1) return;

        Map<String, Object> selectedCourse = canvasCourses.get(canvasChoice);
        String courseId = String.valueOf(selectedCourse.get("id"));
        String courseName = (String) selectedCourse.get("name");

        FileManager.saveCanvasCourseId(folder, courseId);
        System.out.println(ANSI_GREEN + "\n✔ Vínculo salvo com sucesso!" + ANSI_RESET);
        System.out.println("Matéria: " + selectedSubject.name + " <---> Curso Canvas: " + cleanCourseName(courseName) + " (ID: " + courseId + ")");
    }

    public static Path getConfigFilePath() {
        Path configJsonDir = rootDir.resolve("config").resolve("json").resolve("config.json");
        if (Files.exists(configJsonDir)) {
            return configJsonDir;
        }
        Path configDirFile = rootDir.resolve("config").resolve("config.json");
        if (Files.exists(configDirFile)) {
            return configDirFile;
        }
        Path faculdadeConfig = rootDir.resolve("Faculdade").resolve("config.json");
        if (Files.exists(faculdadeConfig)) {
            return faculdadeConfig;
        }
        return configJsonDir;
    }

    public static Path getMateriasJsonPath() {
        Path configJsonMaterias = rootDir.resolve("config").resolve("json").resolve("materias.json");
        if (Files.exists(configJsonMaterias)) {
            return configJsonMaterias;
        }
        Path configMaterias = rootDir.resolve("config").resolve("materias.json");
        if (Files.exists(configMaterias)) {
            return configMaterias;
        }
        Path rootMaterias = rootDir.resolve("materias.json");
        if (Files.exists(rootMaterias)) {
            return rootMaterias;
        }
        return configJsonMaterias;
    }

    private static void prepareCleanZipExport(Scanner scanner) {
        System.out.print("\u001b[2J\u001b[H");
        System.out.println(ANSI_CYAN + "==================================================" + ANSI_RESET);
        System.out.println(ANSI_BOLD + ANSI_BLUE + "📦 GERAR ARQUIVO .ZIP LIMPO DO SISTEMA" + ANSI_RESET);
        System.out.println(ANSI_CYAN + "==================================================" + ANSI_RESET);
        System.out.println("\nEste gerador criará um arquivo '.zip' limpo contendo APENAS:");
        System.out.println("  ✔ O executável 'Cursar.java'");
        System.out.println("  ✔ Toda a pasta do sistema 'config/' (códigos e matérias padrão)");
        System.out.println("  ✔ Scripts de execução ('Makefile')");
        System.out.println("\n❌ NÃO serão incluídas:");
        System.out.println("  • Nenhuma credencial ou token (Canvas / Google)");
        System.out.println("  • Nenhuma das suas pastas de matérias ou anotações pessoais");
        System.out.println("  • Nenhum histórico de faltas ou notas pessoais");
        System.out.println("--------------------------------------------------");
        System.out.print("Deseja gerar o arquivo 'Cursar_Sistema_Base.zip' agora? (S/n): ");

        String ans = scanner.nextLine().trim().toLowerCase();
        if (!(ans.isEmpty() || ans.equals("s") || ans.equals("sim"))) {
            System.out.println("\nOperação cancelada.");
            pressEnterToContinue(scanner);
            return;
        }

        Path zipOutputPath = rootDir.resolve("Cursar_Sistema_Base.zip");

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipOutputPath))) {
            // 1. Cursar.java
            addFileToZip(zos, rootDir.resolve("Cursar.java"), "Cursar.java");

            // 2. Scripts auxiliares e gitignore
            Path runBat = rootDir.resolve("run.bat");
            if (Files.exists(runBat)) addFileToZip(zos, runBat, "run.bat");

            Path makefile = rootDir.resolve("Makefile");
            if (Files.exists(makefile)) addFileToZip(zos, makefile, "Makefile");

            Path gitignore = rootDir.resolve(".gitignore");
            if (Files.exists(gitignore)) addFileToZip(zos, gitignore, ".gitignore");

            // 3. Pasta config/
            Path configDir = rootDir.resolve("config");
            if (Files.exists(configDir) && Files.isDirectory(configDir)) {
                try (var stream = Files.walk(configDir)) {
                    stream.forEach(path -> {
                        if (Files.isRegularFile(path)) {
                            String fileName = path.getFileName().toString();
                            if (!fileName.endsWith(".class")) {
                                String relativePath = rootDir.relativize(path).toString().replace('\\', '/');
                                try {
                                    // Skip any root materias.json or config.json inside config/
                                    if (relativePath.equals("config/materias.json") || relativePath.equals("config/config.json")) {
                                        return;
                                    }
                                    if (fileName.equals("config.json")) {
                                        byte[] cleanConfigBytes = ("{\n  \"canvas_url\": \"https://pucminas.instructure.com\",\n  \"canvas_token\": \"\"\n}\n")
                                                .getBytes(java.nio.charset.StandardCharsets.UTF_8);
                                        zos.putNextEntry(new ZipEntry(relativePath));
                                        zos.write(cleanConfigBytes);
                                        zos.closeEntry();
                                    } else {
                                        addFileToZip(zos, path, relativePath);
                                    }
                                } catch (IOException e) {
                                    System.err.println("Erro ao adicionar " + relativePath + ": " + e.getMessage());
                                }
                            }
                        }
                    });
                }
            }

            System.out.println("\n" + ANSI_BOLD + ANSI_GREEN + "🎉 Arquivo '.zip' limpo gerado com sucesso!" + ANSI_RESET);
            System.out.println("📍 Arquivo: " + ANSI_YELLOW + zipOutputPath.toAbsolutePath() + ANSI_RESET);
            System.out.println("\nEste arquivo contém APENAS o sistema base, sem suas matérias, anotações ou senhas!");
        } catch (Exception e) {
            System.out.println(ANSI_RED + "❌ Erro ao gerar o arquivo .zip: " + e.getMessage() + ANSI_RESET);
        }
        pressEnterToContinue(scanner);
    }

    private static void addFileToZip(ZipOutputStream zos, Path file, String zipPathStr) throws IOException {
        zos.putNextEntry(new ZipEntry(zipPathStr));
        Files.copy(file, zos);
        zos.closeEntry();
    }

    public static void runZipExport(String[] args) {
        rootDir = Paths.get(System.getProperty("user.dir"));
        if (!Files.exists(rootDir.resolve("Faculdade")) && !Files.exists(rootDir.resolve("Notes"))) {
            String userHome = System.getProperty("user.home");
            rootDir = Paths.get(userHome, "Repositorios", "PedroAnotacoes");
        }
        Scanner scanner = new Scanner(System.in);
        prepareCleanZipExport(scanner);
    }
}

