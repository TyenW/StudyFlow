package config;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.*;

public class ObsidianManager {

    private static Path vaultDir;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static void run(String[] args) {
        vaultDir = Paths.get(System.getProperty("user.dir"));
        // Check if vaultDir contains Faculdade or Notes, if not try user.home/Repositorios/PedroAnotacoes
        if (!Files.exists(vaultDir.resolve("Faculdade")) && !Files.exists(vaultDir.resolve("Notes"))) {
            String userHome = System.getProperty("user.home");
            vaultDir = Paths.get(userHome, "Repositorios", "PedroAnotacoes");
        }

        Scanner scanner = new Scanner(System.in);
        List<String> menuOptions = Arrays.asList(
            "🔍 Auditoria de Notas Incompletas (Sugerir Tags/Datas/Aliases)",
            "🏷️ Gerenciar Tags Globalmente",
            "🎭 Gerenciar Apelidos (Aliases) Globalmente",
            "🔗 Vincular/Desvincular Tags e Apelidos por Página",
            "🛠️ Diagnóstico do Segundo Cérebro (Links Quebrados & Órfãos)",
            "🧹 Limpar Anexos Órfãos (Lixeira de Imagens/PDFs)",
            "🧠 IA Acadêmica (Ollama)",
            "⚡ Links Automáticos (Wikilinks no Cofre)",
            "💬 Conversar com o Cofre (RAG Simplificado)",
            "🔄 Sincronizar Backlinks (Índice Reverso)",
            "📊 Gerar Grafo Interativo de Notas",
            "🔙 Voltar ao Menu Principal"
        );

        while (true) {
            String title = "\n==========================================\n" +
                           "🧠 GERENCIADOR OBSIDIAN (SEGUNDO CÉREBRO)  \n" +
                           "==========================================\n" +
                           "Cofre: " + vaultDir.toAbsolutePath() + "\n";
            int choice = InteractiveMenu.select(title, menuOptions);
            if (choice == -1 || choice == 11) {
                break;
            }

            try {
                switch (choice) {
                    case 0:
                        auditMetadata(scanner);
                        break;
                    case 1:
                        manageTagsGlobal(scanner);
                        break;
                    case 2:
                        manageAliasesGlobal(scanner);
                        break;
                    case 3:
                        managePageMetadata(scanner);
                        break;
                    case 4:
                        auditHealth(scanner);
                        break;
                    case 5:
                        cleanOrphanAttachments(scanner);
                        break;
                    case 6:
                        runAiMenu(scanner);
                        break;
                    case 7:
                        runAutoLinker(scanner);
                        break;
                    case 8:
                        conversarComCofre(scanner);
                        break;
                    case 9:
                        sincronizarBacklinks(scanner);
                        break;
                    case 10:
                        generateGraph();
                        break;
                }
            } catch (Exception e) {
                System.out.println("\n❌ Ocorreu um erro: " + e.getMessage());
                e.printStackTrace();
                System.out.print("\nPressione ENTER para continuar...");
                scanner.nextLine();
            }
        }
    }

    private static List<Path> scanVault() throws IOException {
        List<Path> mdFiles = new ArrayList<>();
        Files.walk(vaultDir).forEach(path -> {
            String name = path.getFileName().toString();
            // Skip hidden folders like .git or .obsidian
            if (Files.isRegularFile(path) && (name.endsWith(".md") || name.endsWith(".markdown"))) {
                boolean inHidden = false;
                for (Path p : vaultDir.relativize(path)) {
                    if (p.toString().startsWith(".")) {
                        inHidden = true;
                        break;
                    }
                }
                if (!inHidden) {
                    mdFiles.add(path);
                }
            }
        });
        return mdFiles;
    }

    private static Map<Path, FrontmatterParser.NoteMetadata> loadAllMetadata(List<Path> files) {
        Map<Path, FrontmatterParser.NoteMetadata> map = new HashMap<>();
        for (Path file : files) {
            try {
                map.put(file, FrontmatterParser.parse(file));
            } catch (Exception e) {
                System.err.println("Erro ao ler metadados de " + file + ": " + e.getMessage());
            }
        }
        return map;
    }

    private static void auditMetadata(Scanner scanner) throws IOException {
        System.out.println("\n🔍 Iniciando auditoria de metadados...");
        List<Path> files = scanVault();
        Map<Path, FrontmatterParser.NoteMetadata> metaMap = loadAllMetadata(files);

        List<Path> incompleteFiles = new ArrayList<>();
        for (Path f : files) {
            FrontmatterParser.NoteMetadata meta = metaMap.get(f);
            if (meta == null || meta.criado_em.isEmpty() || meta.atualizado_em.isEmpty() || meta.tags.isEmpty()) {
                incompleteFiles.add(f);
            }
        }

        if (incompleteFiles.isEmpty()) {
            System.out.println("✨ Todos os arquivos possuem metadados completos (datas e tags)!");
            System.out.print("\nPressione ENTER para continuar...");
            scanner.nextLine();
            return;
        }

        System.out.println("\nForam encontradas " + incompleteFiles.size() + " notas incompletas.");
        System.out.println("Deseja iniciar a triagem interativa? (s/n)");
        String start = scanner.nextLine().trim().toLowerCase();
        if (!start.equals("s")) return;

        for (Path file : incompleteFiles) {
            System.out.println("\n------------------------------------------");
            System.out.println("Nota: " + vaultDir.relativize(file));
            FrontmatterParser.NoteMetadata meta = metaMap.get(file);
            if (meta == null) meta = new FrontmatterParser.NoteMetadata();

            // Preview first few lines
            String content = meta.content;
            String[] lines = content.split("\n");
            System.out.println("Preview:");
            for (int i = 0; i < Math.min(lines.length, 5); i++) {
                System.out.println("  " + lines[i]);
            }

            // Suggestions
            String suggestedCreated = meta.criado_em;
            if (suggestedCreated.isEmpty()) {
                try {
                    BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
                    suggestedCreated = DATE_FORMATTER.format(LocalDate.ofInstant(attrs.creationTime().toInstant(), java.time.ZoneId.systemDefault()));
                } catch (Exception e) {
                    suggestedCreated = DATE_FORMATTER.format(LocalDate.now());
                }
            }

            String suggestedUpdated = meta.atualizado_em;
            if (suggestedUpdated.isEmpty()) {
                try {
                    suggestedUpdated = DATE_FORMATTER.format(LocalDate.ofInstant(Files.getLastModifiedTime(file).toInstant(), java.time.ZoneId.systemDefault()));
                } catch (Exception e) {
                    suggestedUpdated = DATE_FORMATTER.format(LocalDate.now());
                }
            }

            List<String> suggestedTags = new ArrayList<>();
            // Tag based on folder slugified
            if (file.getParent() != null) {
                String folderName = file.getParent().getFileName().toString();
                String slugified = slugify(folderName);
                if (!slugified.isEmpty() && !slugified.equals("1-notas-de-aula") && !slugified.equals("notes")) {
                    suggestedTags.add(slugified);
                }
            }
            // Tag suggestions based on simple TF-IDF / keyword extraction
            List<String> keywords = extractKeywords(content, 3);
            suggestedTags.addAll(keywords);

            List<String> suggestedAliases = new ArrayList<>();
            String filename = getBaseName(file);
            // Alias suggestion from parentheses
            if (filename.contains("(") && filename.contains(")")) {
                int startP = filename.indexOf("(");
                int endP = filename.indexOf(")");
                String inside = filename.substring(startP + 1, endP).trim();
                String outside = filename.substring(0, startP).trim();
                suggestedAliases.add(inside);
                suggestedAliases.add(outside);
            }
            // Alias suggestion from first H1 header
            Pattern h1Pattern = Pattern.compile("^#\\s+(.+)$", Pattern.MULTILINE);
            Matcher h1Matcher = h1Pattern.matcher(content);
            if (h1Matcher.find()) {
                suggestedAliases.add(h1Matcher.group(1).trim());
            }

            System.out.println("\nSugestões do Sistema:");
            System.out.println("📅 Criado em: " + suggestedCreated);
            System.out.println("📅 Atualizado em: " + suggestedUpdated);
            System.out.println("🏷️ Tags: " + suggestedTags);
            System.out.println("🎭 Apelidos (Aliases): " + suggestedAliases);

            System.out.println("\nEscolha uma opção:");
            System.out.println("[1] Aceitar sugestões completas");
            System.out.println("[2] Editar manualmente");
            System.out.println("[3] Pular");
            String choice = scanner.nextLine().trim();

            if (choice.equals("1")) {
                meta.criado_em = suggestedCreated;
                meta.atualizado_em = suggestedUpdated;
                for (String t : suggestedTags) {
                    if (!meta.tags.contains(t)) meta.tags.add(t);
                }
                for (String a : suggestedAliases) {
                    if (!meta.aliases.contains(a)) meta.aliases.add(a);
                }
                FrontmatterParser.save(file, meta);
                System.out.println("✅ Metadados salvos!");
            } else if (choice.equals("2")) {
                System.out.print("Digite a data de criação (YYYY-MM-DD) [" + suggestedCreated + "]: ");
                String cInput = scanner.nextLine().trim();
                meta.criado_em = cInput.isEmpty() ? suggestedCreated : cInput;

                System.out.print("Digite a data de atualização (YYYY-MM-DD) [" + suggestedUpdated + "]: ");
                String uInput = scanner.nextLine().trim();
                meta.atualizado_em = uInput.isEmpty() ? suggestedUpdated : uInput;

                System.out.print("Digite as tags separadas por vírgula (ex: heapsort, java) [" + String.join(", ", suggestedTags) + "]: ");
                String tagsInput = scanner.nextLine().trim();
                if (!tagsInput.isEmpty()) {
                    meta.tags.clear();
                    for (String t : tagsInput.split(",")) {
                        meta.tags.add(t.trim().toLowerCase());
                    }
                } else {
                    meta.tags = suggestedTags;
                }

                System.out.print("Digite os aliases separados por vírgula [" + String.join(", ", suggestedAliases) + "]: ");
                String aliasesInput = scanner.nextLine().trim();
                if (!aliasesInput.isEmpty()) {
                    meta.aliases.clear();
                    for (String a : aliasesInput.split(",")) {
                        meta.aliases.add(a.trim());
                    }
                } else {
                    meta.aliases = suggestedAliases;
                }

                FrontmatterParser.save(file, meta);
                System.out.println("✅ Metadados editados e salvos!");
            } else {
                System.out.println("⏭️ Pulado.");
            }
        }

        System.out.println("\nTriagem concluída!");
        System.out.print("\nPressione ENTER para retornar...");
        scanner.nextLine();
    }

    private static void manageTagsGlobal(Scanner scanner) throws IOException {
        List<Path> files = scanVault();
        Map<Path, FrontmatterParser.NoteMetadata> metaMap = loadAllMetadata(files);

        // Map tags to files
        Map<String, List<Path>> tagToFiles = new TreeMap<>();
        for (Path file : files) {
            FrontmatterParser.NoteMetadata meta = metaMap.get(file);
            if (meta != null) {
                for (String t : meta.tags) {
                    tagToFiles.computeIfAbsent(t.toLowerCase(), k -> new ArrayList<>()).add(file);
                }
            }
        }

        System.out.println("\n🏷️ TAGS ENCONTRADAS NO COFRE:");
        if (tagToFiles.isEmpty()) {
            System.out.println("Nenhuma tag cadastrada.");
            System.out.print("\nPressione ENTER para voltar...");
            scanner.nextLine();
            return;
        }

        int idx = 0;
        List<String> tagList = new ArrayList<>(tagToFiles.keySet());
        for (String tag : tagList) {
            System.out.println(String.format("  [%d] #%s (%d notas)", idx++, tag, tagToFiles.get(tag).size()));
        }

        System.out.println("\nEscolha uma opção:");
        System.out.println("[1] Renomear Tag globalmente");
        System.out.println("[2] Excluir Tag globalmente");
        System.out.println("[3] Voltar");
        String opt = scanner.nextLine().trim();

        if (opt.equals("1")) {
            System.out.print("Selecione o número da tag a renomear: ");
            try {
                int tagIdx = Integer.parseInt(scanner.nextLine().trim());
                if (tagIdx >= 0 && tagIdx < tagList.size()) {
                    String oldTag = tagList.get(tagIdx);
                    System.out.print("Digite o novo nome para #" + oldTag + " (sem espaços, minúscula): ");
                    String newTag = slugify(scanner.nextLine().trim());
                    if (!newTag.isEmpty()) {
                        int count = 0;
                        for (Path file : tagToFiles.get(oldTag)) {
                            FrontmatterParser.NoteMetadata meta = metaMap.get(file);
                            if (meta != null) {
                                meta.tags.remove(oldTag);
                                if (!meta.tags.contains(newTag)) meta.tags.add(newTag);
                                FrontmatterParser.save(file, meta);
                                count++;
                            }
                        }
                        System.out.println("✅ Tag renomeada em " + count + " arquivos!");
                    }
                }
            } catch (Exception e) {
                System.out.println("Opção inválida.");
            }
        } else if (opt.equals("2")) {
            System.out.print("Selecione o número da tag a excluir: ");
            try {
                int tagIdx = Integer.parseInt(scanner.nextLine().trim());
                if (tagIdx >= 0 && tagIdx < tagList.size()) {
                    String targetTag = tagList.get(tagIdx);
                    System.out.println("⚠️ Tem certeza que deseja remover #" + targetTag + " de todas as notas? (s/n)");
                    if (scanner.nextLine().trim().equalsIgnoreCase("s")) {
                        int count = 0;
                        for (Path file : tagToFiles.get(targetTag)) {
                            FrontmatterParser.NoteMetadata meta = metaMap.get(file);
                            if (meta != null) {
                                meta.tags.remove(targetTag);
                                FrontmatterParser.save(file, meta);
                                count++;
                            }
                        }
                        System.out.println("✅ Tag excluída de " + count + " arquivos!");
                    }
                }
            } catch (Exception e) {
                System.out.println("Opção inválida.");
            }
        }
    }

    private static void manageAliasesGlobal(Scanner scanner) throws IOException {
        List<Path> files = scanVault();
        Map<Path, FrontmatterParser.NoteMetadata> metaMap = loadAllMetadata(files);

        Map<String, List<Path>> aliasToFiles = new TreeMap<>();
        for (Path file : files) {
            FrontmatterParser.NoteMetadata meta = metaMap.get(file);
            if (meta != null) {
                for (String a : meta.aliases) {
                    aliasToFiles.computeIfAbsent(a, k -> new ArrayList<>()).add(file);
                }
            }
        }

        System.out.println("\n🎭 APELIDOS (ALIASES) ENCONTRADOS NO COFRE:");
        if (aliasToFiles.isEmpty()) {
            System.out.println("Nenhum apelido cadastrado.");
            System.out.print("\nPressione ENTER para voltar...");
            scanner.nextLine();
            return;
        }

        int idx = 0;
        List<String> aliasList = new ArrayList<>(aliasToFiles.keySet());
        for (String alias : aliasList) {
            List<Path> list = aliasToFiles.get(alias);
            System.out.println(String.format("  [%d] \"%s\" -> Notas: %s", idx++, alias, list.stream().map(ObsidianManager::getBaseName).collect(java.util.stream.Collectors.toList())));
        }

        System.out.println("\nEscolha uma opção:");
        System.out.println("[1] Excluir Apelido globalmente");
        System.out.println("[2] Voltar");
        String opt = scanner.nextLine().trim();

        if (opt.equals("1")) {
            System.out.print("Selecione o número do apelido a excluir: ");
            try {
                int aliasIdx = Integer.parseInt(scanner.nextLine().trim());
                if (aliasIdx >= 0 && aliasIdx < aliasList.size()) {
                    String targetAlias = aliasList.get(aliasIdx);
                    int count = 0;
                    for (Path file : aliasToFiles.get(targetAlias)) {
                        FrontmatterParser.NoteMetadata meta = metaMap.get(file);
                        if (meta != null) {
                            meta.aliases.remove(targetAlias);
                            FrontmatterParser.save(file, meta);
                            count++;
                        }
                    }
                    System.out.println("✅ Apelido removido de " + count + " arquivos!");
                }
            } catch (Exception e) {
                System.out.println("Opção inválida.");
            }
        }
    }

    private static void managePageMetadata(Scanner scanner) throws IOException {
        List<Path> files = scanVault();
        if (files.isEmpty()) {
            System.out.println("Cofre vazio.");
            return;
        }

        System.out.println("\n=== SELECIONE A NOTA PARA EDITAR METADADOS ===");
        List<String> titles = files.stream().map(ObsidianManager::getBaseName).collect(java.util.stream.Collectors.toList());
        int choice = InteractiveMenu.select("Pesquise a nota abaixo:", titles);
        if (choice == -1) return;

        Path targetFile = files.get(choice);
        FrontmatterParser.NoteMetadata meta = FrontmatterParser.parse(targetFile);

        while (true) {
            System.out.println("\n==========================================");
            System.out.println("EDITANDO NOTA: " + getBaseName(targetFile));
            System.out.println("------------------------------------------");
            System.out.println("📅 Criado em: " + meta.criado_em);
            System.out.println("📅 Atualizado em: " + meta.atualizado_em);
            System.out.println("🏷️ Tags: " + meta.tags);
            System.out.println("🎭 Aliases: " + meta.aliases);
            System.out.println("==========================================");

            List<String> opts = Arrays.asList(
                "➕ Adicionar Tag",
                "➖ Remover Tag",
                "➕ Adicionar Apelido (Alias)",
                "➖ Remover Apelido (Alias)",
                "💾 Salvar e Voltar",
                "❌ Cancelar"
            );

            int opt = InteractiveMenu.select("Escolha a ação:", opts);
            if (opt == 4) {
                FrontmatterParser.save(targetFile, meta);
                System.out.println("✅ Metadados salvos!");
                break;
            } else if (opt == 5 || opt == -1) {
                break;
            }

            switch (opt) {
                case 0:
                    System.out.print("Digite a nova tag (sem espaços, minúscula): ");
                    String newTag = slugify(scanner.nextLine().trim());
                    if (!newTag.isEmpty() && !meta.tags.contains(newTag)) {
                        meta.tags.add(newTag);
                    }
                    break;
                case 1:
                    if (meta.tags.isEmpty()) {
                        System.out.println("Nenhuma tag para remover.");
                        break;
                    }
                    int tagSel = InteractiveMenu.select("Selecione a tag para remover:", meta.tags);
                    if (tagSel != -1) meta.tags.remove(tagSel);
                    break;
                case 2:
                    System.out.print("Digite o novo apelido (alias): ");
                    String newAlias = scanner.nextLine().trim();
                    if (!newAlias.isEmpty() && !meta.aliases.contains(newAlias)) {
                        meta.aliases.add(newAlias);
                    }
                    break;
                case 3:
                    if (meta.aliases.isEmpty()) {
                        System.out.println("Nenhum alias para remover.");
                        break;
                    }
                    int aliasSel = InteractiveMenu.select("Selecione o alias para remover:", meta.aliases);
                    if (aliasSel != -1) meta.aliases.remove(aliasSel);
                    break;
            }
        }
    }

    private static void auditHealth(Scanner scanner) throws IOException {
        System.out.println("\n🛠️ Rodando Diagnóstico do Segundo Cérebro...");
        List<Path> files = scanVault();
        Map<Path, FrontmatterParser.NoteMetadata> metaMap = loadAllMetadata(files);

        // Map targets
        Map<String, Path> linkTargets = new HashMap<>();
        for (Path f : files) {
            String name = getBaseName(f);
            linkTargets.put(name.toLowerCase(), f);
            FrontmatterParser.NoteMetadata meta = metaMap.get(f);
            if (meta != null) {
                for (String alias : meta.aliases) {
                    linkTargets.put(alias.toLowerCase(), f);
                }
            }
        }

        // 1. Detect dead links and orphans
        Map<Path, List<String>> fileToDeadLinks = new HashMap<>();
        Set<String> referencedNotes = new HashSet<>();

        Pattern linkPattern = Pattern.compile("\\[\\[(.*?)\\]\\]");

        for (Path file : files) {
            FrontmatterParser.NoteMetadata meta = metaMap.get(file);
            if (meta == null) continue;

            Matcher matcher = linkPattern.matcher(meta.content);
            while (matcher.find()) {
                String link = matcher.group(1).trim();
                if (link.contains("|")) {
                    link = link.substring(0, link.indexOf("|")).trim();
                }
                // Skip media extensions
                if (link.endsWith(".png") || link.endsWith(".jpg") || link.endsWith(".jpeg") || link.endsWith(".pdf") || link.endsWith(".webp") || link.endsWith(".gif")) {
                    continue;
                }

                String target = link.toLowerCase();
                if (!linkTargets.containsKey(target)) {
                    fileToDeadLinks.computeIfAbsent(file, k -> new ArrayList<>()).add(link);
                } else {
                    Path referencedPath = linkTargets.get(target);
                    referencedNotes.add(getBaseName(referencedPath).toLowerCase());
                }
            }
        }

        // Identify orphans (not referenced in text by anyone, and not daily notes or indices)
        List<Path> orphans = new ArrayList<>();
        for (Path f : files) {
            String name = getBaseName(f);
            if (!referencedNotes.contains(name.toLowerCase()) && !name.equalsIgnoreCase("Tarefas") && !name.toLowerCase().contains("planejamento")) {
                orphans.add(f);
            }
        }

        // Print results
        System.out.println("\n--- RESULTADO DA ANÁLISE ---");
        int totalDeadLinks = fileToDeadLinks.values().stream().mapToInt(List::size).sum();
        System.out.println("❌ Links Quebrados Encontrados: " + totalDeadLinks);
        System.out.println("🕸️ Notas Órfãs (Sem links de entrada): " + orphans.size());

        if (totalDeadLinks > 0) {
            System.out.println("\nDeseja ver os detalhes e resolver os Links Quebrados? (s/n)");
            if (scanner.nextLine().trim().equalsIgnoreCase("s")) {
                resolveDeadLinks(scanner, fileToDeadLinks, linkTargets);
            }
        }

        if (!orphans.isEmpty()) {
            System.out.println("\nNotas Órfãs detectadas:");
            for (Path f : orphans) {
                System.out.println("  - " + vaultDir.relativize(f));
            }
        }

        System.out.print("\nPressione ENTER para voltar...");
        scanner.nextLine();
    }

    private static void resolveDeadLinks(Scanner scanner, Map<Path, List<String>> fileToDeadLinks, Map<String, Path> linkTargets) throws IOException {
        for (Path file : fileToDeadLinks.keySet()) {
            List<String> deadLinks = fileToDeadLinks.get(file);
            System.out.println("\nNa nota: " + vaultDir.relativize(file));
            FrontmatterParser.NoteMetadata meta = FrontmatterParser.parse(file);

            for (String deadLink : deadLinks) {
                System.out.println("  Link quebrado: [[" + deadLink + "]]");
                System.out.println("  Escolha uma ação:");
                System.out.println("  [1] Criar esta nota vazia");
                System.out.println("  [2] Corrigir/Vincular para nota existente");
                System.out.println("  [3] Remover os colchetes (tornar texto normal)");
                System.out.println("  [4] Ignorar");

                String choice = scanner.nextLine().trim();
                if (choice.equals("1")) {
                    // Create in parent folder of current file
                    Path newFilePath = file.getParent().resolve(deadLink + ".md");
                    if (!Files.exists(newFilePath)) {
                        FrontmatterParser.NoteMetadata newMeta = new FrontmatterParser.NoteMetadata();
                        newMeta.criado_em = DATE_FORMATTER.format(LocalDate.now());
                        newMeta.atualizado_em = newMeta.criado_em;
                        newMeta.content = "# " + deadLink + "\n\nNota criada automaticamente para resolver link quebrado.";
                        FrontmatterParser.save(newFilePath, newMeta);
                        System.out.println("  ✅ Nota criada em: " + vaultDir.relativize(newFilePath));
                    }
                } else if (choice.equals("2")) {
                    List<String> suggestions = findFuzzyMatches(deadLink, linkTargets.keySet());
                    if (suggestions.isEmpty()) {
                        System.out.println("  Nenhuma sugestão encontrada.");
                    } else {
                        System.out.println("  Sugestões de correspondência:");
                        for (int i = 0; i < suggestions.size(); i++) {
                            System.out.println("    [" + i + "] [[" + linkTargets.get(suggestions.get(i)).getFileName().toString().replace(".md", "") + "]]");
                        }
                        System.out.print("  Escolha o índice para substituir (ou ENTER para ignorar): ");
                        String idxStr = scanner.nextLine().trim();
                        if (!idxStr.isEmpty()) {
                            try {
                                int idx = Integer.parseInt(idxStr);
                                if (idx >= 0 && idx < suggestions.size()) {
                                    String matchName = getBaseName(linkTargets.get(suggestions.get(idx)));
                                    meta.content = meta.content.replace("[[" + deadLink + "]]", "[[" + matchName + "]]");
                                    FrontmatterParser.save(file, meta);
                                    System.out.println("  ✅ Link atualizado!");
                                }
                            } catch (Exception e) {
                                System.out.println("  Índice inválido.");
                            }
                        }
                    }
                } else if (choice.equals("3")) {
                    meta.content = meta.content.replace("[[" + deadLink + "]]", deadLink);
                    FrontmatterParser.save(file, meta);
                    System.out.println("  ✅ Colchetes removidos!");
                }
            }
        }
    }

    private static List<String> findFuzzyMatches(String query, Set<String> keys) {
        List<String> matches = new ArrayList<>();
        String q = query.toLowerCase();
        for (String k : keys) {
            if (k.contains(q) || q.contains(k)) {
                matches.add(k);
            }
        }
        return matches;
    }

    private static void cleanOrphanAttachments(Scanner scanner) throws IOException {
        System.out.println("\n🧹 Procurando Anexos Órfãos (Imagens e PDFs sem uso)...");
        List<Path> files = scanVault();
        Map<Path, FrontmatterParser.NoteMetadata> metaMap = loadAllMetadata(files);

        // Find all media files
        List<Path> mediaFiles = new ArrayList<>();
        Files.walk(vaultDir).forEach(path -> {
            if (Files.isRegularFile(path)) {
                String name = path.getFileName().toString().toLowerCase();
                if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") ||
                    name.endsWith(".gif") || name.endsWith(".pdf") || name.endsWith(".webp") || name.endsWith(".svg")) {
                    boolean inHidden = false;
                    for (Path p : vaultDir.relativize(path)) {
                        if (p.toString().startsWith(".") || p.toString().startsWith("Lixeira_Anexos")) {
                            inHidden = true;
                            break;
                        }
                    }
                    if (!inHidden) {
                        mediaFiles.add(path);
                    }
                }
            }
        });

        if (mediaFiles.isEmpty()) {
            System.out.println("Nenhum arquivo de mídia encontrado no cofre.");
            System.out.print("\nPressione ENTER para voltar...");
            scanner.nextLine();
            return;
        }

        // Build set of referenced file names/paths in markdown contents
        Set<String> referencedMedia = new HashSet<>();
        // Search matches like ![[image.png]] or [[image.png]] or (image.png)
        Pattern mediaPattern = Pattern.compile("(?:\\[\\[|\\(|\\!\\b)([^\\|\\]\\)\\!]+?\\.(?:png|jpg|jpeg|gif|pdf|webp|svg))", Pattern.CASE_INSENSITIVE);

        for (Path md : files) {
            FrontmatterParser.NoteMetadata meta = metaMap.get(md);
            if (meta == null) continue;

            Matcher m = mediaPattern.matcher(meta.content);
            while (m.find()) {
                String match = m.group(1).trim();
                // Extract filename only (handle subpaths if any)
                int lastSlash = match.lastIndexOf('/');
                if (lastSlash != -1) match = match.substring(lastSlash + 1);
                int lastBackslash = match.lastIndexOf('\\');
                if (lastBackslash != -1) match = match.substring(lastBackslash + 1);
                
                referencedMedia.add(match.toLowerCase());
            }
        }

        // Find orphans
        List<Path> orphans = new ArrayList<>();
        for (Path media : mediaFiles) {
            String name = media.getFileName().toString().toLowerCase();
            if (!referencedMedia.contains(name)) {
                orphans.add(media);
            }
        }

        if (orphans.isEmpty()) {
            System.out.println("✨ Todos os anexos do cofre estão sendo referenciados por alguma nota!");
            System.out.print("\nPressione ENTER para voltar...");
            scanner.nextLine();
            return;
        }

        System.out.println("\nForam detectados " + orphans.size() + " anexos órfãos:");
        for (Path o : orphans) {
            System.out.println("  - " + vaultDir.relativize(o));
        }

        System.out.println("\nDeseja mover esses anexos para a pasta Lixeira_Anexos? (s/n)");
        if (scanner.nextLine().trim().equalsIgnoreCase("s")) {
            Path trashDir = vaultDir.resolve("Lixeira_Anexos");
            if (!Files.exists(trashDir)) {
                Files.createDirectories(trashDir);
            }

            int count = 0;
            for (Path o : orphans) {
                Path dest = trashDir.resolve(o.getFileName());
                // Avoid overwriting if file already exists in trash
                if (Files.exists(dest)) {
                    dest = trashDir.resolve(System.currentTimeMillis() + "_" + o.getFileName());
                }
                Files.move(o, dest);
                count++;
            }
            System.out.println("✅ " + count + " anexos órfãos movidos com sucesso para a pasta: Lixeira_Anexos/");
        }

        System.out.print("\nPressione ENTER para voltar...");
        scanner.nextLine();
    }

    private static void runAiMenu(Scanner scanner) throws Exception {
        List<String> aiOpts = Arrays.asList(
            "🏷️🎭 Sugerir Tags e Apelidos (Aliases) com IA",
            "🏷️ Auto-Tagueamento em Lote (Notas sem Tags)",
            "📝 Síntese Estruturada de Nota de Aula (Resumo Acadêmico)",
            "🔙 Voltar"
        );

        while (true) {
            int aiChoice = InteractiveMenu.select("🧠 IA ACADÊMICA (OLLAMA LOCAL)", aiOpts);
            if (aiChoice == -1 || aiChoice == 3) {
                break;
            }

            System.out.println("\n🚀 Esta funcionalidade com IA será implementada em breve!");
            System.out.print("Pressione ENTER para continuar...");
            scanner.nextLine();
        }
    }

    private static String truncateForTags(String content) {
        if (content == null) return "";
        if (content.length() > 4000) {
            return content.substring(0, 4000);
        }
        return content;
    }

    private static String removeThinkBlock(String response) {
        if (response == null) return "";
        int startThink = response.indexOf("<think>");
        int endThink = response.indexOf("</think>");
        if (startThink != -1 && endThink != -1) {
            String before = response.substring(0, startThink);
            String after = response.substring(endThink + "</think>".length());
            return (before + after).trim();
        } else if (endThink != -1) {
            return response.substring(endThink + "</think>".length()).trim();
        } else if (startThink != -1) {
            return response.substring(0, startThink).trim();
        }
        return response.trim();
    }

    private static void parseTagsAndAliases(String cleanResponse, List<String> tags, List<String> aliases) {
        String[] lines = cleanResponse.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.toUpperCase().startsWith("TAGS:")) {
                String val = trimmed.substring(5).trim();
                if (val.startsWith("[") && val.endsWith("]")) {
                    val = val.substring(1, val.length() - 1);
                }
                for (String part : val.split(",")) {
                    String t = slugify(part.trim());
                    if (!t.isEmpty() && !tags.contains(t)) {
                        tags.add(t);
                    }
                }
            } else if (trimmed.toUpperCase().startsWith("ALIASES:")) {
                String val = trimmed.substring(8).trim();
                if (val.startsWith("[") && val.endsWith("]")) {
                    val = val.substring(1, val.length() - 1);
                }
                for (String part : val.split(",")) {
                    String alias = part.trim();
                    if ((alias.startsWith("\"") && alias.endsWith("\"")) || (alias.startsWith("'") && alias.endsWith("'"))) {
                        alias = alias.substring(1, alias.length() - 1).trim();
                    }
                    if (!alias.isEmpty() && !aliases.contains(alias)) {
                        aliases.add(alias);
                    }
                }
            }
        }
        
        // Fallback: if no tags were extracted, run parseAiTags on the text
        if (tags.isEmpty()) {
            tags.addAll(parseAiTags(cleanResponse));
        }
    }

    private static List<String> parseAiTags(String response) {
        List<String> tags = new ArrayList<>();
        if (response == null) return tags;

        String clean = removeThinkBlock(response).trim();

        // 1. Remove markdown code blocks
        clean = clean.replaceAll("(?s)```[a-zA-Z]*", "").replaceAll("```", "").trim();

        // 2. Format check: lists (lines starting with -)
        if (clean.contains("\n-") || clean.startsWith("-")) {
            String[] lines = clean.split("\n");
            for (String line : lines) {
                String trimmedLine = line.trim();
                if (trimmedLine.startsWith("-")) {
                    String tag = slugify(trimmedLine.substring(1).trim());
                    if (!tag.isEmpty() && !tags.contains(tag)) {
                        tags.add(tag);
                    }
                }
            }
            if (!tags.isEmpty()) {
                return tags;
            }
        }

        // 3. Strip surrounding brackets
        if (clean.startsWith("[") && clean.endsWith("]")) {
            clean = clean.substring(1, clean.length() - 1);
        }

        // 4. Split by commas or newlines
        String separator = ",";
        if (!clean.contains(",") && clean.contains("\n")) {
            separator = "\n";
        }
        
        for (String p : clean.split(separator)) {
            String tag = p.trim();
            if ((tag.startsWith("\"") && tag.endsWith("\"")) || (tag.startsWith("'") && tag.endsWith("'"))) {
                tag = tag.substring(1, tag.length() - 1).trim();
            }
            tag = slugify(tag);
            if (!tag.isEmpty() && !tags.contains(tag) && tag.length() > 1) {
                tags.add(tag);
            }
        }
        return tags;
    }

    private static void runAutoLinker(Scanner scanner) throws IOException {
        System.out.println("\n⚡ Executando Links Automáticos (Wikilinks no cofre)...");
        List<Path> files = scanVault();
        Map<Path, FrontmatterParser.NoteMetadata> metaMap = loadAllMetadata(files);

        // Map titles & aliases to target note titles
        Map<String, String> termToTarget = new HashMap<>();
        for (Path f : files) {
            String name = getBaseName(f);
            termToTarget.put(name.toLowerCase(), name);
            FrontmatterParser.NoteMetadata meta = metaMap.get(f);
            if (meta != null) {
                for (String alias : meta.aliases) {
                    termToTarget.put(alias.toLowerCase(), name);
                }
            }
        }

        // Sort terms by length descending to match longer phrases first (e.g. "banco de dados" before "dados")
        List<String> terms = new ArrayList<>(termToTarget.keySet());
        terms.sort((a, b) -> Integer.compare(b.length(), a.length()));

        System.out.println("Deseja aplicar auto-link de termos em TODO o cofre? Isso pode alterar vários arquivos. (s/n)");
        if (!scanner.nextLine().trim().equalsIgnoreCase("s")) return;

        int filesChanged = 0;
        for (Path file : files) {
            FrontmatterParser.NoteMetadata meta = metaMap.get(file);
            if (meta == null) continue;

            String originalContent = meta.content;
            String filename = getBaseName(file);
            String currentTitleLower = filename.toLowerCase();

            // Linkify content while protecting code blocks, existing links, etc.
            String newContent = linkifyText(originalContent, terms, termToTarget, currentTitleLower);

            if (!newContent.equals(originalContent)) {
                meta.content = newContent;
                meta.atualizado_em = DATE_FORMATTER.format(LocalDate.now());
                FrontmatterParser.save(file, meta);
                filesChanged++;
            }
        }

        System.out.println("✅ Auto-link finalizado! " + filesChanged + " notas foram atualizadas com novas conexões.");
        System.out.print("\nPressione ENTER para voltar...");
        scanner.nextLine();
    }

    private static String linkifyText(String text, List<String> terms, Map<String, String> termToTarget, String currentTitleLower) {
        // Simple tokenizer: protect code blocks, images, HTML and existing links.
        // We replace them with placeholder tokens, apply link replacement, then restore them.
        List<String> placeholders = new ArrayList<>();
        
        // Regexes of blocks to protect
        Pattern codeBlockPattern = Pattern.compile("```[\\s\\S]*?```");
        Pattern inlineCodePattern = Pattern.compile("`[^`\\n]+?`");
        Pattern linkPattern = Pattern.compile("\\[\\[[^\\]]+?\\]\\]");
        Pattern imagePattern = Pattern.compile("\\!\\[.+?\\]\\(.+?\\)");
        Pattern mdLinkPattern = Pattern.compile("\\[.+?\\]\\(.+?\\)");

        String workingText = text;

        // Protection phase
        workingText = protectMatches(workingText, codeBlockPattern, placeholders);
        workingText = protectMatches(workingText, inlineCodePattern, placeholders);
        workingText = protectMatches(workingText, linkPattern, placeholders);
        workingText = protectMatches(workingText, imagePattern, placeholders);
        workingText = protectMatches(workingText, mdLinkPattern, placeholders);

        // Replace plain text matches of terms
        for (String term : terms) {
            if (term.length() < 3) continue; // Skip very short terms to avoid false links
            
            // Skip linking to self
            String target = termToTarget.get(term);
            if (target.toLowerCase().equals(currentTitleLower)) continue;

            // Regex matches term on word boundaries, case-insensitive
            // Portuguese characters support via boundary pattern
            String regex = "(?i)\\b" + Pattern.quote(term) + "\\b";
            Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(workingText);

            // We construct the wikilink preserving the matched case of the term in text
            // e.g. "Algoritmos" -> "[[Algoritmos]]" or if the target is "Projeto de Algoritmos" -> "[[Projeto de Algoritmos|Algoritmos]]"
            StringBuilder sb = new StringBuilder();
            while (m.find()) {
                String match = m.group();
                String replacement;
                if (target.equalsIgnoreCase(match)) {
                    replacement = "[[" + target + "]]";
                } else {
                    replacement = "[[" + target + "|" + match + "]]";
                }
                m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
            m.appendTail(sb);
            workingText = sb.toString();
        }

        // Restoration phase (in reverse order to handle nesting if any)
        for (int i = placeholders.size() - 1; i >= 0; i--) {
            workingText = workingText.replace("__PROTECTED_TOKEN_" + i + "__", placeholders.get(i));
        }

        return workingText;
    }

    private static String protectMatches(String text, Pattern pattern, List<String> placeholders) {
        Matcher matcher = pattern.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String match = matcher.group();
            String placeholder = "__PROTECTED_TOKEN_" + placeholders.size() + "__";
            placeholders.add(match);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(placeholder));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static void generateGraph() throws IOException {
        System.out.println("\n📊 Gerando Grafo Interativo...");
        List<Path> files = scanVault();
        Map<Path, FrontmatterParser.NoteMetadata> metaMap = loadAllMetadata(files);

        GraphGenerator.generate(vaultDir, files, metaMap);
        System.out.println("✨ Grafo gerado com sucesso em: " + vaultDir.resolve("segundo_cerebro_grafo.html").toAbsolutePath());
        System.out.println("Abra o arquivo acima em qualquer navegador de internet para visualizar suas conexões.");
        System.out.print("\nPressione ENTER para continuar...");
        new Scanner(System.in).nextLine();
    }

    private static List<String> extractKeywords(String text, int max) {
        List<String> keywords = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) return keywords;

        String clean = text.replaceAll("[^a-zA-ZáéíóúâêôãõçÁÉÍÓÚÂÊÔÃÕÇ\\s]", " ").toLowerCase();
        String[] words = clean.split("\\s+");

        Set<String> stopWords = new HashSet<>(Arrays.asList(
            "de", "do", "da", "em", "para", "o", "a", "que", "com", "um", "uma", "se", "por", 
            "as", "os", "como", "ao", "aos", "dos", "das", "ou", "mais", "mas", "na", "no", "nas", "nos",
            "uma", "num", "numa", "pelo", "pela", "pelos", "pelas", "sem", "sobre", "sob", "sua", "seu",
            "suas", "seus", "este", "esta", "estes", "estas", "esse", "essa", "esses", "essas", "aquilo"
        ));

        Map<String, Integer> freq = new HashMap<>();
        for (String w : words) {
            if (w.length() > 3 && !stopWords.contains(w)) {
                freq.put(w, freq.getOrDefault(w, 0) + 1);
            }
        }

        List<Map.Entry<String, Integer>> list = new ArrayList<>(freq.entrySet());
        list.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        for (int i = 0; i < Math.min(list.size(), max); i++) {
            keywords.add(list.get(i).getKey());
        }
        return keywords;
    }

    private static String slugify(String s) {
        if (s == null) return "";
        String clean = s.toLowerCase()
                .replaceAll("[áàâã]", "a")
                .replaceAll("[éèê]", "e")
                .replaceAll("[íìî]", "i")
                .replaceAll("[óòôõ]", "o")
                .replaceAll("[úùû]", "u")
                .replaceAll("ç", "c")
                .replaceAll("[^a-z0-9\\-\\s]", "")
                .replaceAll("\\s+", "-")
                .trim();
        return clean;
    }

    private static void conversarComCofre(Scanner scanner) throws Exception {
        System.out.println("\n🚀 Esta funcionalidade com IA (RAG) será implementada em breve!");
        System.out.print("Pressione ENTER para continuar...");
        scanner.nextLine();
    }

    private static void sincronizarBacklinks(Scanner scanner) throws IOException {
        System.out.println("\n🔄 Sincronizando Backlinks (Índice Reverso)...");
        List<Path> files = scanVault();
        Map<Path, FrontmatterParser.NoteMetadata> metaMap = loadAllMetadata(files);

        // Map targets (filenames and aliases) to target note paths
        Map<String, Path> linkTargets = new HashMap<>();
        for (Path f : files) {
            String name = getBaseName(f);
            linkTargets.put(name.toLowerCase(), f);
            FrontmatterParser.NoteMetadata meta = metaMap.get(f);
            if (meta != null) {
                for (String alias : meta.aliases) {
                    linkTargets.put(alias.toLowerCase(), f);
                }
            }
        }

        // Build backlinks map: Target Note Path -> Set of Source Note Paths
        Map<Path, Set<Path>> backlinksMap = new HashMap<>();
        Pattern linkPattern = Pattern.compile("\\[\\[(.*?)\\]\\]");

        for (Path file : files) {
            FrontmatterParser.NoteMetadata meta = metaMap.get(file);
            if (meta == null) continue;

            Matcher matcher = linkPattern.matcher(meta.content);
            while (matcher.find()) {
                String link = matcher.group(1).trim();
                if (link.contains("|")) {
                    link = link.substring(0, link.indexOf("|")).trim();
                }
                // Skip media extensions
                if (link.endsWith(".png") || link.endsWith(".jpg") || link.endsWith(".jpeg") || link.endsWith(".pdf") || link.endsWith(".webp") || link.endsWith(".gif")) {
                    continue;
                }

                String target = link.toLowerCase();
                if (linkTargets.containsKey(target)) {
                    Path targetPath = linkTargets.get(target);
                    // Avoid self-references
                    if (!targetPath.equals(file)) {
                        backlinksMap.computeIfAbsent(targetPath, k -> new LinkedHashSet<>()).add(file);
                    }
                }
            }
        }

        // Update each note's backlinks footer
        int count = 0;
        for (Path file : files) {
            FrontmatterParser.NoteMetadata meta = metaMap.get(file);
            if (meta == null) continue;

            String originalContent = meta.content;
            String cleanedContent = originalContent;

            // Remove existing backlinks section if it exists
            int backlinksIndex = cleanedContent.indexOf("### Referenciado Por:");
            if (backlinksIndex != -1) {
                cleanedContent = cleanedContent.substring(0, backlinksIndex).trim();
            }

            Set<Path> backlinks = backlinksMap.get(file);
            String newContent = cleanedContent;

            if (backlinks != null && !backlinks.isEmpty()) {
                StringBuilder footer = new StringBuilder();
                footer.append("\n\n### Referenciado Por:\n");
                for (Path bl : backlinks) {
                    footer.append("- [[").append(getBaseName(bl)).append("]]\n");
                }
                newContent = cleanedContent + footer.toString();
            }

            // Save file if content changed (either added backlinks, updated them, or removed them)
            if (!newContent.equals(originalContent)) {
                meta.content = newContent;
                meta.atualizado_em = DATE_FORMATTER.format(LocalDate.now());
                FrontmatterParser.save(file, meta);
                count++;
            }
        }

        System.out.println("✅ Sincronização concluída! " + count + " notas foram atualizadas com seus respectivos backlinks.");
        System.out.print("\nPressione ENTER para voltar...");
        scanner.nextLine();
    }

    private static String getBaseName(Path path) {
        String name = path.getFileName().toString();
        int idx = name.lastIndexOf('.');
        return idx == -1 ? name : name.substring(0, idx);
    }
}
