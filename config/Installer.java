package config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

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

        // If folder Faculté doesn't exist, we run the setup
        if (!Files.exists(faculdadeDir) || !Files.exists(planningPath)) {
            runSetup(rootDir, faculdadeDir, planningPath);
        }
    }

    private static void runSetup(Path rootDir, Path faculdadeDir, Path planningPath) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("\n==================================================");
        System.out.println("🚀 INSTALAÇÃO DO SISTEMA CURSAR & OBSIDIAN");
        System.out.println("==================================================");
        System.out.println("Parece que é a primeira vez que você roda este sistema neste diretório.");
        System.out.println("Vamos criar a estrutura básica de pastas e configurar as disciplinas.");
        System.out.println("Pressione ENTER para começar...");
        scanner.nextLine();

        try {
            // 1. Create Folders
            System.out.println("\n[1/4] Criando pastas do sistema...");
            Files.createDirectories(faculdadeDir);
            Files.createDirectories(rootDir.resolve("config").resolve("json"));
            System.out.println("✔ Pasta 'Faculdade/' criada!");
            System.out.println("✔ Pasta 'config/json/' criada!");

            // 2. Write Default Planning Grid
            System.out.println("\n[2/4] Criando arquivo de planejamento curricular...");
            Files.write(planningPath, DEFAULT_PLANNING_CONTENT.getBytes(StandardCharsets.UTF_8));
            System.out.println("✔ Arquivo 'Faculdade/" + PLANNING_FILE_NAME + "' inicializado!");

            // 3. Declare subject progress
            System.out.println("\n[3/4] Configuração do seu Progresso Acadêmico");
            System.out.println("Para cada disciplina da grade, digite:");
            System.out.println("  1 - Se você JÁ CONCLUIU (marcar com [x])");
            System.out.println("  2 - Se você ESTÁ CURSANDO agora (marcar com [c])");
            System.out.println("  Qualquer outra tecla (ou ENTER) - Se ainda é pendente (marcar com [ ])");
            System.out.println("--------------------------------------------------");

            List<Subject> subjects = FileManager.loadSubjects(planningPath);
            List<String> planningLines = Files.readAllLines(planningPath);
            List<String> updatedLines = new ArrayList<>(planningLines);

            List<Subject> currentSubjects = new ArrayList<>();

            for (Subject sub : subjects) {
                System.out.print(String.format("[%s] %s: ", sub.period, sub.name));
                String input = scanner.nextLine().trim();
                String statusChar = " ";
                if (input.equals("1")) {
                    statusChar = "x";
                } else if (input.equals("2")) {
                    statusChar = "c";
                }

                // Update line in planning file
                for (int i = 0; i < updatedLines.size(); i++) {
                    if (updatedLines.get(i).equals(sub.originalLine)) {
                        String newLine = sub.originalLine.replaceFirst("\\[[ xcd]\\]", "[" + statusChar + "]");
                        updatedLines.set(i, newLine);
                        sub.originalLine = newLine;
                        sub.statusChar = statusChar;
                        break;
                    }
                }

                if (statusChar.equals("c")) {
                    currentSubjects.add(sub);
                }
            }

            Files.write(planningPath, updatedLines, StandardCharsets.UTF_8);
            System.out.println("✔ Progresso das disciplinas salvo no arquivo de planejamento!");

            // Create folders for currently taking subjects
            if (!currentSubjects.isEmpty()) {
                System.out.println("\nCriando pastas para as matérias que você está cursando...");
                for (Subject sub : currentSubjects) {
                    Path folder = rootDir.resolve(sub.getSanitizedFolderName());
                    if (!Files.exists(folder)) {
                        Files.createDirectories(folder);
                        FolderManager.createSubjectSubfolders(folder, sub.name);
                        System.out.println("  ✔ Criada pasta: " + sub.getSanitizedFolderName());
                    }
                }
            }

            // 4. Setup configurations
            System.out.println("\n[4/4] Configuração de Integrações de API");
            System.out.print("Deseja configurar a integração com o Canvas LMS agora? (S/n): ");
            String canvasAns = scanner.nextLine().trim().toLowerCase();

            String canvasUrl = "https://pucminas.instructure.com";
            String canvasToken = "";

            if (canvasAns.isEmpty() || canvasAns.equals("s") || canvasAns.equals("sim")) {
                System.out.print("URL do seu Canvas [" + canvasUrl + "]: ");
                String urlInput = scanner.nextLine().trim();
                if (!urlInput.isEmpty()) {
                    canvasUrl = urlInput;
                }
                System.out.print("Cole o seu Token de Acesso do Canvas: ");
                canvasToken = scanner.nextLine().trim();
            }

            System.out.print("\nDeseja configurar a integração com o Google Tasks agora? (s/N): ");
            String tasksAns = scanner.nextLine().trim().toLowerCase();

            String googleClientId = "";
            String googleClientSecret = "";

            if (tasksAns.equals("s") || tasksAns.equals("sim")) {
                System.out.print("Digite o seu Google Client ID: ");
                googleClientId = scanner.nextLine().trim();
                System.out.print("Digite o seu Google Client Secret: ");
                googleClientSecret = scanner.nextLine().trim();
            }

            // Save configurations to config/json/config.json
            Path configPath = rootDir.resolve("config").resolve("json").resolve("config.json");
            java.util.Map<String, String> configMap = new java.util.HashMap<>();
            configMap.put("canvas_url", canvasUrl);
            configMap.put("canvas_token", canvasToken);
            if (!googleClientId.isEmpty() && !googleClientSecret.isEmpty()) {
                configMap.put("google_client_id", googleClientId);
                configMap.put("google_client_secret", googleClientSecret);
            }

            FileManager.saveConfig(configPath, configMap);
            System.out.println("✔ Configurações de API salvas com sucesso em: config/json/config.json");

            // Setup Git (optional)
            System.out.print("\nDeseja rodar 'git init' para começar o controle de versão neste cofre? (s/N): ");
            String gitAns = scanner.nextLine().trim().toLowerCase();
            if (gitAns.equals("s") || gitAns.equals("sim")) {
                try {
                    ProcessBuilder pb = new ProcessBuilder("git", "init");
                    pb.inheritIO().start().waitFor();
                    System.out.println("✔ Repositório Git inicializado!");
                } catch (Exception e) {
                    System.out.println("⚠ Não foi possível executar 'git init'. Verifique se o Git está instalado no seu terminal.");
                }
            }

            System.out.println("\n==================================================");
            System.out.println("🎉 INSTALAÇÃO CONCLUÍDA COM SUCESSO!");
            System.out.println("==================================================");
            System.out.println("Seu ambiente de estudos StudyFlow está pronto.");
            System.out.println("Abra o menu inicial para ver as opções.");
            System.out.println("Pressione ENTER para iniciar a aplicação...");
            scanner.nextLine();

        } catch (Exception e) {
            System.err.println("\n❌ Erro durante a instalação: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
