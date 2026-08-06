package config;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class InteractiveMenu {
    // Arrow keys / input constants
    private static final int KEY_UP = 1000;
    private static final int KEY_DOWN = 1001;
    private static final int KEY_ENTER = 1002;
    private static final int KEY_ESCAPE = 1003;
    private static final int KEY_BACKSPACE = 1004;
    private static final int KEY_RIGHT = 1005;
    private static final int KEY_LEFT = 1006;

    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");
    private static boolean useJavaRawMode = false;
    private static Object rawModeInstance = null;

    private static boolean tryJavaRawMode(boolean raw) {
        try {
            Console console = System.console();
            if (console != null) {
                if (raw) {
                    java.lang.reflect.Method enterRawMode = Console.class.getMethod("enterRawMode");
                    rawModeInstance = enterRawMode.invoke(console);
                    useJavaRawMode = true;
                    return true;
                } else {
                    boolean wasActive = useJavaRawMode;
                    if (useJavaRawMode && rawModeInstance != null) {
                        if (rawModeInstance instanceof AutoCloseable) {
                            ((AutoCloseable) rawModeInstance).close();
                        } else {
                            java.lang.reflect.Method close = rawModeInstance.getClass().getMethod("close");
                            close.invoke(rawModeInstance);
                        }
                        rawModeInstance = null;
                        useJavaRawMode = false;
                    }
                    return wasActive;
                }
            }
        } catch (Exception e) {
            useJavaRawMode = false;
        }
        return false;
    }

    private static void runPowerShellScript(String script) {
        if (!IS_WINDOWS) return;
        try {
            byte[] bytes = script.getBytes(java.nio.charset.StandardCharsets.UTF_16LE);
            String b64 = java.util.Base64.getEncoder().encodeToString(bytes);
            ProcessBuilder pb = new ProcessBuilder("powershell", "-NoProfile", "-NonInteractive", "-EncodedCommand", b64);
            pb.inheritIO().start().waitFor();
        } catch (Exception e) {}
    }

    private static void setRawMode(boolean raw) {
        if (tryJavaRawMode(raw)) {
            return;
        }
        if (IS_WINDOWS) {
            String modeCmd = raw 
                ? "$mIn = $mIn -band -not 6 -bor 512; $mOut = $mOut -bor 4"
                : "$mIn = $mIn -bor 6 -band -not 512";
            
            String script = String.format(
                "$sig = '[DllImport(\"kernel32.dll\")] public static extern IntPtr GetStdHandle(int n); " +
                "[DllImport(\"kernel32.dll\")] public static extern bool GetConsoleMode(IntPtr h, out uint m); " +
                "[DllImport(\"kernel32.dll\")] public static extern bool SetConsoleMode(IntPtr h, uint m);'; " +
                "if (-not ([System.Management.Automation.PSTypeName]'Win.Win32').Type) { Add-Type -MemberDefinition $sig -Name Win32 -Namespace Win }; " +
                "$hIn = [Win.Win32]::GetStdHandle(-10); $mIn = [uint32]0; [Win.Win32]::GetConsoleMode($hIn, [ref]$mIn); " +
                "$hOut = [Win.Win32]::GetStdHandle(-11); $mOut = [uint32]0; [Win.Win32]::GetConsoleMode($hOut, [ref]$mOut); " +
                "%s; " +
                "[Win.Win32]::SetConsoleMode($hIn, $mIn); [Win.Win32]::SetConsoleMode($hOut, $mOut);",
                modeCmd
            );
            runPowerShellScript(script);
        } else {
            try {
                String command = raw ? "stty raw -echo </dev/tty" : "stty cooked echo </dev/tty";
                ProcessBuilder pb = new ProcessBuilder("sh", "-c", command);
                pb.inheritIO().start().waitFor();
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    private static int readKey() throws IOException {
        int c = System.in.read();
        if (c == 27) { // Escape sequence
            // Check if there are more bytes available immediately
            if (System.in.available() > 0) {
                int next1 = System.in.read();
                if (next1 == 91) {
                    if (System.in.available() > 0) {
                        int next2 = System.in.read();
                        if (next2 == 65) return KEY_UP;
                        if (next2 == 66) return KEY_DOWN;
                        if (next2 == 67) return KEY_RIGHT;
                        if (next2 == 68) return KEY_LEFT;
                    }
                }
            }
            return KEY_ESCAPE;
        }
        if (c == 10 || c == 13) return KEY_ENTER;
        if (c == 127 || c == 8) return KEY_BACKSPACE;
        return c;
    }

    private static void print(String str) {
        System.out.print(str.replace("\n", "\r\n"));
    }

    private static void println(String str) {
        System.out.print(str.replace("\n", "\r\n") + "\r\n");
    }

    private static void println() {
        System.out.print("\r\n");
    }

    public static int select(String title, List<String> options) {
        if (IS_WINDOWS) {
            return selectNumeric(title, options);
        }
        return selectInteractive(title, options);
    }

    public static List<Integer> selectMultiple(String title, List<String> options) {
        if (IS_WINDOWS) {
            return selectMultipleNumeric(title, options);
        }
        return selectMultipleInteractive(title, options);
    }

    private static int selectNumeric(String title, List<String> options) {
        Scanner scanner = new Scanner(System.in);
        String filterQuery = "";

        while (true) {
            List<Integer> filteredIndices = new ArrayList<>();
            List<String> filteredOptions = new ArrayList<>();
            String q = filterQuery.toLowerCase().trim();

            for (int i = 0; i < options.size(); i++) {
                String opt = options.get(i);
                if (q.isEmpty() || opt.toLowerCase().contains(q)) {
                    filteredIndices.add(i);
                    filteredOptions.add(opt);
                }
            }

            print("\u001b[2J\u001b[H");
            println("\u001b[36m==================================================\u001b[0m");
            println("\u001b[1m\u001b[34m" + title + "\u001b[0m");
            println("\u001b[36m==================================================\u001b[0m");

            if (!filterQuery.isEmpty()) {
                println("🔍 Filtrar: \u001b[93m" + filterQuery + "\u001b[0m (Digite 0 para limpar o filtro)");
                println("\u001b[36m--------------------------------------------------\u001b[0m");
            } else {
                println("Digite o \u001b[92mnúmero\u001b[0m da opção desejada.");
                println("Digite \u001b[91m0\u001b[0m ou \u001b[91mEsc\u001b[0m para voltar/sair, ou digite um texto para filtrar.");
                println("\u001b[36m--------------------------------------------------\u001b[0m");
            }

            if (filteredOptions.isEmpty()) {
                println("  \u001b[31mNenhum resultado encontrado.\u001b[0m");
            } else {
                for (int i = 0; i < filteredOptions.size(); i++) {
                    int num = i + 1;
                    println("  \u001b[92m[" + num + "]\u001b[0m " + filteredOptions.get(i));
                }
            }

            println("\u001b[36m==================================================\u001b[0m");
            print("Opção: ");

            if (!scanner.hasNextLine()) {
                return -1;
            }
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("esc") || input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("sair")) {
                return -1;
            }

            if (input.equals("0")) {
                if (!filterQuery.isEmpty()) {
                    filterQuery = "";
                    continue;
                }
                return -1;
            }

            try {
                int choiceNum = Integer.parseInt(input);
                if (choiceNum >= 1 && choiceNum <= filteredOptions.size()) {
                    return filteredIndices.get(choiceNum - 1);
                } else {
                    println("\u001b[31mOpção inválida! Tente novamente.\u001b[0m");
                }
            } catch (NumberFormatException e) {
                filterQuery = input;
            }
        }
    }

    private static List<Integer> selectMultipleNumeric(String title, List<String> options) {
        Scanner scanner = new Scanner(System.in);
        Set<Integer> selectedIndices = new LinkedHashSet<>();
        String filterQuery = "";

        while (true) {
            List<Integer> filteredIndices = new ArrayList<>();
            List<String> filteredOptions = new ArrayList<>();
            String q = filterQuery.toLowerCase().trim();

            for (int i = 0; i < options.size(); i++) {
                String opt = options.get(i);
                if (q.isEmpty() || opt.toLowerCase().contains(q)) {
                    filteredIndices.add(i);
                    filteredOptions.add(opt);
                }
            }

            print("\u001b[2J\u001b[H");
            println("\u001b[36m==================================================\u001b[0m");
            println("\u001b[1m\u001b[34m" + title + "\u001b[0m");
            println("\u001b[36m==================================================\u001b[0m");

            if (!filterQuery.isEmpty()) {
                println("🔍 Filtrar: \u001b[93m" + filterQuery + "\u001b[0m");
                println("\u001b[36m--------------------------------------------------\u001b[0m");
            } else {
                println("Digite os \u001b[92mnúmeros\u001b[0m para marcar/desmarcar (ex: 1 3 ou 1,3).");
                println("Digite \u001b[92m'todos'\u001b[0m para marcar todos, \u001b[92m'nenhum'\u001b[0m para desmarcar todos.");
                println("Pressione \u001b[92mENTER\u001b[0m sem digitar nada ou digite \u001b[92m'ok'\u001b[0m para confirmar a seleção.");
                println("Digite \u001b[91m'0'\u001b[0m ou \u001b[91m'esc'\u001b[0m para cancelar.");
                println("\u001b[36m--------------------------------------------------\u001b[0m");
            }

            if (filteredOptions.isEmpty()) {
                println("  \u001b[31mNenhum resultado encontrado.\u001b[0m");
            } else {
                for (int i = 0; i < filteredOptions.size(); i++) {
                    int originalIndex = filteredIndices.get(i);
                    boolean isSelected = selectedIndices.contains(originalIndex);
                    String checkMark = isSelected ? "[\u2714] " : "[ ] ";
                    String colorPrefix = isSelected ? "\u001b[92m" : "";
                    String colorSuffix = isSelected ? "\u001b[0m" : "";
                    int num = i + 1;
                    println("  \u001b[92m[" + num + "]\u001b[0m " + colorPrefix + checkMark + filteredOptions.get(i) + colorSuffix);
                }
            }

            println("\u001b[36m==================================================\u001b[0m");
            if (!selectedIndices.isEmpty()) {
                println("Selecionados: \u001b[92m" + selectedIndices.size() + " itens\u001b[0m");
                println("\u001b[36m==================================================\u001b[0m");
            }

            print("Sua escolha: ");
            if (!scanner.hasNextLine()) {
                return new ArrayList<>();
            }
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("ok") || input.isEmpty()) {
                return new ArrayList<>(selectedIndices);
            }

            if (input.equalsIgnoreCase("esc") || input.equals("0")) {
                return new ArrayList<>();
            }

            if (input.equalsIgnoreCase("todos") || input.equalsIgnoreCase("all")) {
                selectedIndices.addAll(filteredIndices);
                continue;
            }

            if (input.equalsIgnoreCase("nenhum") || input.equalsIgnoreCase("none")) {
                for (int idx : filteredIndices) {
                    selectedIndices.remove(idx);
                }
                continue;
            }

            String[] tokens = input.split("[,\\s;]+");
            boolean allNumeric = true;
            List<Integer> parsedNums = new ArrayList<>();
            for (String tok : tokens) {
                try {
                    int val = Integer.parseInt(tok);
                    parsedNums.add(val);
                } catch (NumberFormatException e) {
                    allNumeric = false;
                    break;
                }
            }

            if (allNumeric && !parsedNums.isEmpty()) {
                for (int num : parsedNums) {
                    if (num >= 1 && num <= filteredOptions.size()) {
                        int originalIndex = filteredIndices.get(num - 1);
                        if (selectedIndices.contains(originalIndex)) {
                            selectedIndices.remove(originalIndex);
                        } else {
                            selectedIndices.add(originalIndex);
                        }
                    }
                }
            } else {
                filterQuery = input;
            }
        }
    }

    private static int selectInteractive(String title, List<String> options) {
        setRawMode(true);
        Thread hook = new Thread(() -> setRawMode(false));
        Runtime.getRuntime().addShutdownHook(hook);

        int selectedIndex = 0;
        StringBuilder query = new StringBuilder();

        try {
            while (true) {
                List<Integer> filteredIndices = new ArrayList<>();
                List<String> filteredOptions = new ArrayList<>();
                String q = query.toString().toLowerCase().trim();

                for (int i = 0; i < options.size(); i++) {
                    String opt = options.get(i);
                    if (q.isEmpty() || opt.toLowerCase().contains(q)) {
                        filteredIndices.add(i);
                        filteredOptions.add(opt);
                    }
                }

                if (filteredOptions.isEmpty()) {
                    selectedIndex = -1;
                } else {
                    if (selectedIndex < 0) selectedIndex = 0;
                    if (selectedIndex >= filteredOptions.size()) {
                        selectedIndex = filteredOptions.size() - 1;
                    }
                }

                // Clear screen and draw menu (Using ANSI sequence: clear screen and move cursor to top-left)
                print("\u001b[2J\u001b[H");

                println("\u001b[36m==================================================\u001b[0m");
                println("\u001b[1m\u001b[34m" + title + "\u001b[0m");
                println("\u001b[36m==================================================\u001b[0m");
                
                if (query.length() > 0) {
                    println("🔍 Filtrar: \u001b[93m" + query.toString() + "\u001b[0m");
                    println("\u001b[36m--------------------------------------------------\u001b[0m");
                } else {
                    println("Use \u001b[92m↑/↓\u001b[0m para navegar, \u001b[92mENTER\u001b[0m para escolher, ou comece a digitar.");
                    println("Pressione \u001b[91mEsc\u001b[0m para voltar.");
                    println("\u001b[36m--------------------------------------------------\u001b[0m");
                }

                if (filteredOptions.isEmpty()) {
                    println("  \u001b[31mNenhum resultado encontrado.\u001b[0m");
                } else {
                    int maxVisible = 8;
                    int totalFiltered = filteredOptions.size();
                    int start = 0;
                    int end = totalFiltered;

                    if (totalFiltered > maxVisible) {
                        start = selectedIndex - (maxVisible / 2);
                        if (start < 0) {
                            start = 0;
                        }
                        end = start + maxVisible;
                        if (end > totalFiltered) {
                            end = totalFiltered;
                            start = end - maxVisible;
                        }
                    }

                    if (start > 0) {
                        println("   \u001b[90m▲ (+" + start + " itens acima)\u001b[0m");
                    } else {
                        println(); // keep spacing
                    }

                    for (int i = start; i < end; i++) {
                        if (i == selectedIndex) {
                            println(" \u001b[46m\u001b[30m➤ " + filteredOptions.get(i) + " \u001b[0m");
                        } else {
                            println("   " + filteredOptions.get(i));
                        }
                    }

                    int below = totalFiltered - end;
                    if (below > 0) {
                        println("   \u001b[90m▼ (+" + below + " itens abaixo)\u001b[0m");
                    } else {
                        println(); // keep spacing
                    }
                }
                println("\u001b[36m==================================================\u001b[0m");

                int key = readKey();
                if (key == KEY_UP) {
                    if (selectedIndex > 0) selectedIndex--;
                } else if (key == KEY_DOWN) {
                    if (selectedIndex < filteredOptions.size() - 1) selectedIndex++;
                } else if (key == KEY_ENTER) {
                    if (selectedIndex >= 0 && selectedIndex < filteredIndices.size()) {
                        return filteredIndices.get(selectedIndex);
                    }
                } else if (key == KEY_ESCAPE) {
                    return -1;
                } else if (key == KEY_BACKSPACE) {
                    if (query.length() > 0) {
                        query.setLength(query.length() - 1);
                        selectedIndex = 0;
                    }
                } else if (key >= 32 && key <= 126) {
                    query.append((char) key);
                    selectedIndex = 0;
                }
            }
        } catch (IOException e) {
            return -1;
        } finally {
            setRawMode(false);
            try {
                Runtime.getRuntime().removeShutdownHook(hook);
            } catch (Exception e) {}
        }
    }

    private static List<Integer> selectMultipleInteractive(String title, List<String> options) {
        setRawMode(true);
        Thread hook = new Thread(() -> setRawMode(false));
        Runtime.getRuntime().addShutdownHook(hook);

        int selectedIndex = 0;
        StringBuilder query = new StringBuilder();
        Set<Integer> selectedIndices = new LinkedHashSet<>();

        try {
            while (true) {
                List<Integer> filteredIndices = new ArrayList<>();
                List<String> filteredOptions = new ArrayList<>();
                String q = query.toString().toLowerCase().trim();

                for (int i = 0; i < options.size(); i++) {
                    String opt = options.get(i);
                    if (q.isEmpty() || opt.toLowerCase().contains(q)) {
                        filteredIndices.add(i);
                        filteredOptions.add(opt);
                    }
                }

                if (filteredOptions.isEmpty()) {
                    selectedIndex = -1;
                } else {
                     if (selectedIndex < 0) selectedIndex = 0;
                     if (selectedIndex >= filteredOptions.size()) {
                         selectedIndex = filteredOptions.size() - 1;
                     }
                }

                // Draw menu
                print("\u001b[2J\u001b[H");
                println("\u001b[36m==================================================\u001b[0m");
                println("\u001b[1m\u001b[34m" + title + "\u001b[0m");
                println("\u001b[36m==================================================\u001b[0m");
                 
                if (query.length() > 0) {
                    println("🔍 Filtrar: \u001b[93m" + query.toString() + "\u001b[0m");
                    println("\u001b[36m--------------------------------------------------\u001b[0m");
                } else {
                    println("Use \u001b[92m↑/↓\u001b[0m para navegar, \u001b[92mESPAÇO/→\u001b[0m para marcar/desmarcar.");
                    println("Pressione \u001b[92mENTER\u001b[0m para confirmar, ou \u001b[91mEsc\u001b[0m para voltar.");
                    println("\u001b[36m--------------------------------------------------\u001b[0m");
                }

                if (filteredOptions.isEmpty()) {
                    println("  \u001b[31mNenhum resultado encontrado.\u001b[0m");
                } else {
                    int maxVisible = 8;
                    int totalFiltered = filteredOptions.size();
                    int start = 0;
                    int end = totalFiltered;

                    if (totalFiltered > maxVisible) {
                        start = selectedIndex - (maxVisible / 2);
                        if (start < 0) start = 0;
                        end = start + maxVisible;
                        if (end > totalFiltered) {
                            end = totalFiltered;
                            start = end - maxVisible;
                        }
                    }

                    if (start > 0) {
                        println("   \u001b[90m▲ (+" + start + " itens acima)\u001b[0m");
                    } else {
                        println();
                    }

                    for (int i = start; i < end; i++) {
                        int originalIndex = filteredIndices.get(i);
                        boolean isSelected = selectedIndices.contains(originalIndex);
                        String checkMark = isSelected ? "[\u2714] " : "[ ] ";
                        String colorPrefix = isSelected ? "\u001b[92m" : "";
                        String colorSuffix = isSelected ? "\u001b[0m" : "";

                        if (i == selectedIndex) {
                            println(" \u001b[46m\u001b[30m➤ " + checkMark + filteredOptions.get(i) + " \u001b[0m");
                        } else {
                            println("   " + colorPrefix + checkMark + filteredOptions.get(i) + colorSuffix);
                        }
                    }

                    int below = totalFiltered - end;
                    if (below > 0) {
                        println("   \u001b[90m▼ (+" + below + " itens abaixo)\u001b[0m");
                    } else {
                        println();
                    }
                }
                println("\u001b[36m==================================================\u001b[0m");
                 
                if (!selectedIndices.isEmpty()) {
                    println("Selecionados: \u001b[92m" + selectedIndices.size() + " itens\u001b[0m");
                    println("\u001b[36m==================================================\u001b[0m");
                }

                int key = readKey();
                if (key == KEY_UP) {
                    if (selectedIndex > 0) selectedIndex--;
                } else if (key == KEY_DOWN) {
                    if (selectedIndex < filteredOptions.size() - 1) selectedIndex++;
                } else if (key == 32 || key == KEY_RIGHT) {
                    if (selectedIndex >= 0 && selectedIndex < filteredIndices.size()) {
                        int originalIndex = filteredIndices.get(selectedIndex);
                        if (selectedIndices.contains(originalIndex)) {
                            selectedIndices.remove(originalIndex);
                        } else {
                            selectedIndices.add(originalIndex);
                        }
                    }
                } else if (key == KEY_ENTER) {
                    return new ArrayList<>(selectedIndices);
                } else if (key == KEY_ESCAPE) {
                    return new ArrayList<>();
                } else if (key == KEY_BACKSPACE) {
                    if (query.length() > 0) {
                        query.setLength(query.length() - 1);
                        selectedIndex = 0;
                    }
                } else if (key > 32 && key <= 126) {
                    query.append((char) key);
                    selectedIndex = 0;
                }
            }
        } catch (IOException e) {
            return new ArrayList<>();
        } finally {
            setRawMode(false);
            try {
                Runtime.getRuntime().removeShutdownHook(hook);
            } catch (Exception e) {}
        }
    }

}
