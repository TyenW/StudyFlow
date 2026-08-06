package config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Installer {

    private static final String PLANNING_FILE_NAME = "Planejamento  faculdade.md";
    private static final String DEFAULT_PLANNING_CONTENT = 
        "## ✅ 1º Período\n" +
        "- [ ] Algoritmos e Estruturas de Dados I (AED1)\n" +
        "- [ ] Cálculo I (CALC1)\n" +
        "- [ ] Introdução à Computação (INT COMP)\n" +
        "- [ ] Laboratório de Iniciação à Programação (LIP)\n" +
        "- [ ] Filosofia: Razão e Modernidade (FILO1)\n" +
        "- [ ] Trabalho Interdisciplinar I: Front-End (TI1)\n" +
        "- [ ] Desenvolvimento de Interfaces Web (DIW)\n\n" +
        "## ✅ 2º Período\n" +
        "- [ ] Algoritmos e Estruturas de Dados II (AED2)\n" +
        "- [ ] Arquitetura de Computadores I (AC1)\n" +
        "- [ ] Engenharia de Software I (ENG SOFT 1)\n" +
        "- [ ] Trabalho Interdisciplinar II: Back-End (TI2)\n" +
        "- [ ] Optativa I (OPT1)\n" +
        "- [ ] Cultura Religiosa: Fenômeno Religioso (RELI)\n\n" +
        "## ✅ 3º Período\n" +
        "- [ ] Algoritmos e Estruturas de Dados III (AED3)\n" +
        "- [ ] Arquitetura de Computadores II (AC2)*\n" +
        "- [ ] Cálculo II (CALC2) *\n" +
        "- [ ] Linguagens de Programação (LP)\n" +
        "- [ ] Banco de Dados (BD)*\n" +
        "- [ ] Trabalho Interdisciplinar III: Pesquisa Aplicada (TI3)*\n\n" +
        "## ✅ 4º Período\n" +
        "- [ ] Teoria dos Grafos e Computabilidade (GRAFOS)\n" +
        "- [ ] Estatística e Probabilidade (EST E PROB)\n" +
        "- [ ] Laboratório de Desenvolvimento para Dispositivos Móveis (LDDM)\n" +
        "- [ ] Inteligência Artificial (IA)\n" +
        "- [ ] Modelagem e Avaliação de Desempenho (MOD)\n" +
        "- [ ] Optativa II (OPT2)\n" +
        "- [ ] Trabalho Interdisciplinar IV: Aplicações Móveis (TI4)\n\n" +
        "## ✅ 5º Período\n" +
        "- [ ] Projeto e Análise de Algoritmos (PAA)\n" +
        "- [ ] Arquitetura de Computadores III (AC3)\n" +
        "- [ ] Engenharia de Software II (ENG SOFT 2)\n" +
        "- [ ] Redes de Computadores I (REDES1)\n" +
        "- [ ] Sistemas Operacionais (SO)\n" +
        "- [ ] Optativa III (OPT3)\n" +
        "- [ ] Trabalho Interdisciplinar V: Sistemas Computacionais (TI5)\n\n" +
        "## ✅ 6º Período\n" +
        "- [ ] Fundamentos Teóricos da Computação (FTC)\n" +
        "- [ ] Computação Paralela (COMP PAR)\n" +
        "- [ ] Processamento e Análise de Imagens (PAI)\n" +
        "- [ ] Computação Distribuída (COMP DIST)\n" +
        "- [ ] Geometria Analítica e Álgebra Linear (GAAL)\n" +
        "- [ ] Cultura Religiosa: Pessoa e Sociedade (RELI)\n" +
        "- [ ] Trabalho Interdisciplinar VI: Sistemas Paralelos e Distribuídos (TI6)\n\n" +
        "## ✅ 7º Período\n" +
        "- [ ] Trabalho de Conclusão de Curso I (TCC1)\n" +
        "- [ ] Compiladores (COMPI)\n" +
        "- [ ] Computação Gráfica (COMP GRAF)\n" +
        "- [ ] Redes de Computadores II (REDES2)\n" +
        "- [ ] Cibersergurança e Ethical Hacking (CIBER)\n" +
        "- [ ] Tópicos em Computação I (TOP1)\n" +
        "- [ ] Tópicos em Computação II (TOP2)\n" +
        "- [ ] Filosofia: Antropologia e Ética (FILO)\n\n" +
        "## ✅ 8º Período\n" +
        "- [ ] Trabalho de Conclusão de Curso II (TCC2)\n" +
        "- [ ] Computadores e Sociedade (COMP E SOC)\n" +
        "- [ ] Segurança e Auditoria de Sistemas (SEG E AUD)\n" +
        "- [ ] Otimização de Sistemas (TCS)\n" +
        "- [ ] Tópicos em Computação III (TOP3)\n" +
        "- [ ] Tópicos em Computação IV (TOP4)\n" +
        "- [ ] Optativa IV (OPT4)\n\n" +
        "[x] é feito\n" +
        "[ ] nao ta feito e nao estou fazendo \n" +
        "[c] cursando esse periodo\n";

    public static void checkAndRun(String[] args) {
        Path rootDir = Paths.get(System.getProperty("user.dir"));
        Path faculdadeDir = rootDir.resolve("Faculdade");
        Path planningPath = faculdadeDir.resolve(PLANNING_FILE_NAME);

        if (!Files.exists(faculdadeDir) || !Files.exists(planningPath)) {
            runSetup(rootDir, faculdadeDir, planningPath);
        }
    }

    private static void runSetup(Path rootDir, Path faculdadeDir, Path planningPath) {
        try {
            // 1. Create Directories & Default Planning File
            Files.createDirectories(faculdadeDir);
            Files.createDirectories(rootDir.resolve("config").resolve("json"));
            Files.write(planningPath, DEFAULT_PLANNING_CONTENT.getBytes(StandardCharsets.UTF_8));

            // Load subjects
            List<Subject> subjects = FileManager.loadSubjects(planningPath);
            List<String> planningLines = Files.readAllLines(planningPath);
            List<String> updatedLines = new ArrayList<>(planningLines);

            // Build options list for Interactive Menu
            List<String> subjectOptions = new ArrayList<>();
            for (Subject sub : subjects) {
                subjectOptions.add("[" + sub.period + "] " + sub.name);
            }

            // Step 1: Select COMPLETED subjects [x]
            String completedTitle = "🎓 PASSO 1: Selecione as disciplinas que você JÁ CONCLUIU";
            List<Integer> completedIndices = InteractiveMenu.selectMultiple(completedTitle, subjectOptions);
            Set<Integer> completedSet = new HashSet<>(completedIndices);

            // Build remaining list for CURSANDO subjects [c]
            List<Integer> remainingIndices = new ArrayList<>();
            List<String> remainingOptions = new ArrayList<>();
            for (int i = 0; i < subjects.size(); i++) {
                if (!completedSet.contains(i)) {
                    remainingIndices.add(i);
                    remainingOptions.add(subjectOptions.get(i));
                }
            }

            // Step 2: Select CURSANDO subjects [c]
            Set<Integer> cursandoSet = new HashSet<>();
            if (!remainingOptions.isEmpty()) {
                String cursandoTitle = "📚 PASSO 2: Selecione as disciplinas que você ESTÁ CURSANDO AGORA";
                List<Integer> cursandoSubIndices = InteractiveMenu.selectMultiple(cursandoTitle, remainingOptions);
                for (int subIdx : cursandoSubIndices) {
                    cursandoSet.add(remainingIndices.get(subIdx));
                }
            }

            // Update planning lines & create current subject folders
            List<Subject> currentSubjects = new ArrayList<>();
            int countCompleted = 0;
            int countCursando = 0;
            int countPending = 0;

            for (int i = 0; i < subjects.size(); i++) {
                Subject sub = subjects.get(i);
                String statusChar = " ";
                if (completedSet.contains(i)) {
                    statusChar = "x";
                    countCompleted++;
                } else if (cursandoSet.contains(i)) {
                    statusChar = "c";
                    countCursando++;
                    currentSubjects.add(sub);
                } else {
                    countPending++;
                }

                // Update line in planning file
                for (int l = 0; l < updatedLines.size(); l++) {
                    if (updatedLines.get(l).equals(sub.originalLine)) {
                        String newLine = sub.originalLine.replaceFirst("\\[[ xcdXCD]\\]", "[" + statusChar + "]");
                        updatedLines.set(l, newLine);
                        sub.originalLine = newLine;
                        sub.statusChar = statusChar;
                        break;
                    }
                }
            }

            Files.write(planningPath, updatedLines, StandardCharsets.UTF_8);

            // Create folders for currently taking subjects
            if (!currentSubjects.isEmpty()) {
                for (Subject sub : currentSubjects) {
                    Path folder = rootDir.resolve(sub.getSanitizedFolderName());
                    if (!Files.exists(folder)) {
                        Files.createDirectories(folder);
                        FolderManager.createSubjectSubfolders(folder, sub.name);
                    }
                }
            }

            // Interactive step: Canvas Integration
            List<String> canvasChoices = Arrays.asList(
                "❌ Pular integração com Canvas LMS por enquanto",
                "✅ Configurar integração com Canvas LMS agora"
            );
            int canvasOpt = InteractiveMenu.select("🌐 CONFIGURAÇÃO: Deseja conectar sua conta do Canvas LMS?", canvasChoices);

            String canvasUrl = "https://pucminas.instructure.com";
            String canvasToken = "";

            if (canvasOpt == 1) {
                Scanner sc = new Scanner(System.in);
                System.out.println("\n--- CONFIGURAÇÃO CANVAS LMS ---");
                System.out.print("URL do seu Canvas [" + canvasUrl + "]: ");
                String urlInput = sc.nextLine().trim();
                if (!urlInput.isEmpty()) {
                    canvasUrl = urlInput;
                }
                System.out.print("Cole o seu Token de Acesso do Canvas: ");
                canvasToken = sc.nextLine().trim();
            }

            // Interactive step: Google Tasks Integration
            List<String> tasksChoices = Arrays.asList(
                "❌ Pular integração com Google Tasks por enquanto",
                "✅ Configurar integração com Google Tasks agora"
            );
            int tasksOpt = InteractiveMenu.select("📱 CONFIGURAÇÃO: Deseja conectar sua conta do Google Tasks?", tasksChoices);

            String googleClientId = "";
            String googleClientSecret = "";

            if (tasksOpt == 1) {
                Scanner sc = new Scanner(System.in);
                System.out.println("\n--- CONFIGURAÇÃO GOOGLE TASKS ---");
                System.out.print("Digite o seu Google Client ID: ");
                googleClientId = sc.nextLine().trim();
                System.out.print("Digite o seu Google Client Secret: ");
                googleClientSecret = sc.nextLine().trim();
            }

            // Save config
            Path configPath = rootDir.resolve("config").resolve("json").resolve("config.json");
            Map<String, String> configMap = new HashMap<>();
            configMap.put("canvas_url", canvasUrl);
            configMap.put("canvas_token", canvasToken);
            if (!googleClientId.isEmpty() && !googleClientSecret.isEmpty()) {
                configMap.put("google_client_id", googleClientId);
                configMap.put("google_client_secret", googleClientSecret);
            }
            FileManager.saveConfig(configPath, configMap);

            // Interactive step: Git Repository Init
            List<String> gitChoices = Arrays.asList(
                "❌ Não inicializar Git agora",
                "✅ Inicializar repositório Git ('git init')"
            );
            int gitOpt = InteractiveMenu.select("🛠️ CONFIGURAÇÃO: Deseja inicializar o controle de versão Git?", gitChoices);

            if (gitOpt == 1) {
                try {
                    ProcessBuilder pb = new ProcessBuilder("git", "init");
                    pb.inheritIO().start().waitFor();
                } catch (Exception e) {}
            }

            // Completion Banner
            System.out.println("\n==================================================");
            System.out.println("🎉 INSTALAÇÃO E CONFIGURAÇÃO CONCLUÍDAS COM SUCESSO!");
            System.out.println("==================================================");
            System.out.println("  ✔ " + countCompleted + " Disciplinas Concluídas [x]");
            System.out.println("  ✔ " + countCursando + " Disciplinas Cursando Agora [c]");
            System.out.println("  ✔ " + countPending + " Disciplinas Pendentes [ ]");
            System.out.println("==================================================");
            System.out.println("Seu ambiente de estudos StudyFlow está pronto.");
            System.out.println("Pressione ENTER para abrir o menu principal...");
            try { new Scanner(System.in).nextLine(); } catch (Exception e) {}

        } catch (Exception e) {
            System.err.println("\n❌ Erro durante a instalação: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
