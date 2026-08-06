package config;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GoogleTasksManager {

    private static final String REDIRECT_URI = "http://localhost:8080";

    public static String getAuthorizationCode(String clientId) throws IOException {
        int port = 8080;
        String authUrl = "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + java.net.URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                + "&redirect_uri=" + java.net.URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8)
                + "&response_type=code"
                + "&scope=https://www.googleapis.com/auth/tasks"
                + "&access_type=offline"
                + "&prompt=consent";

        System.out.println("\n==================================================");
        System.out.println("          🔑 AUTORIZAÇÃO GOOGLE TASKS");
        System.out.println("==================================================");
        System.out.println("Abra o seguinte link no seu navegador para autorizar:");
        System.out.println(authUrl);
        System.out.println("==================================================");

        // Tentar abrir o navegador automaticamente
        try {
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().browse(new URI(authUrl));
            }
        } catch (Exception e) {
            // Ignorar falhas silenciosamente
        }

        final String[] codeContainer = new String[1];
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setSoTimeout(60000); // Timeout de 60 segundos
            System.out.println("\nAguardando autorização no navegador...");

            try (Socket socket = serverSocket.accept();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                 OutputStream writer = socket.getOutputStream()) {

                String line = reader.readLine();
                if (line != null && line.startsWith("GET")) {
                    Pattern p = Pattern.compile("code=([^&\\s]+)");
                    Matcher m = p.matcher(line);
                    if (m.find()) {
                        codeContainer[0] = m.group(1);
                    }
                }

                String html = "<html><body style='font-family:sans-serif; text-align:center; padding-top:50px;'>"
                        + "<h1>Autorizado com sucesso!</h1>"
                        + "<p>Você já pode fechar esta aba e voltar para o terminal do Cursar CLI.</p>"
                        + "</body></html>";

                writer.write(("HTTP/1.1 200 OK\r\n"
                        + "Content-Type: text/html; charset=UTF-8\r\n"
                        + "Content-Length: " + html.getBytes(StandardCharsets.UTF_8).length + "\r\n"
                        + "Connection: close\r\n\r\n"
                        + html).getBytes(StandardCharsets.UTF_8));
                writer.flush();
            }
        } catch (SocketTimeoutException e) {
            System.out.println("⚠️ Tempo limite de 60s excedido.");
        } catch (IOException e) {
            System.out.println("⚠️ Não foi possível rodar o servidor local na porta " + port + ": " + e.getMessage());
        }

        if (codeContainer[0] == null) {
            System.out.print("\nCaso a página não tenha aberto automaticamente, faça a autorização no link acima,\n"
                    + "copie o código 'code' da URL resultante e cole aqui: ");
            Scanner sc = new Scanner(System.in);
            String input = sc.nextLine().trim();
            if (!input.isEmpty()) {
                codeContainer[0] = input;
            }
        }

        return codeContainer[0];
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> exchangeCodeForTokens(String clientId, String clientSecret, String code) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        String body = "code=" + java.net.URLEncoder.encode(code, StandardCharsets.UTF_8)
                + "&client_id=" + java.net.URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                + "&client_secret=" + java.net.URLEncoder.encode(clientSecret, StandardCharsets.UTF_8)
                + "&redirect_uri=" + java.net.URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8)
                + "&grant_type=authorization_code";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://oauth2.googleapis.com/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Falha ao obter tokens (HTTP " + response.statusCode() + "): " + response.body());
        }

        Object parsed = JsonParser.parse(response.body());
        if (parsed instanceof Map) {
            return (Map<String, Object>) parsed;
        }
        throw new IOException("Resposta de token inválida do Google.");
    }

    @SuppressWarnings("unchecked")
    public static String getAccessToken(Map<String, String> config, Path configPath) throws Exception {
        String accessToken = config.get("google_access_token");
        String refreshToken = config.get("google_refresh_token");
        String expiryStr = config.get("google_token_expiry");
        String clientId = config.get("google_client_id");
        String clientSecret = config.get("google_client_secret");

        if (accessToken == null || accessToken.isEmpty() || refreshToken == null || refreshToken.isEmpty()) {
            throw new IllegalStateException("Google Tasks não autorizado. Use a opção de configuração primeiro.");
        }

        long expiry = 0;
        if (expiryStr != null) {
            expiry = Long.parseLong(expiryStr);
        }

        // Renova se estiver expirado ou perto de expirar (menos de 60 segundos restantes)
        if (System.currentTimeMillis() / 1000L >= expiry - 60) {
            HttpClient client = HttpClient.newHttpClient();
            String body = "client_id=" + java.net.URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                    + "&client_secret=" + java.net.URLEncoder.encode(clientSecret, StandardCharsets.UTF_8)
                    + "&refresh_token=" + java.net.URLEncoder.encode(refreshToken, StandardCharsets.UTF_8)
                    + "&grant_type=refresh_token";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://oauth2.googleapis.com/token"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IOException("Erro ao renovar token (HTTP " + response.statusCode() + "): " + response.body());
            }

            Object parsed = JsonParser.parse(response.body());
            if (parsed instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) parsed;
                String newAccess = (String) map.get("access_token");
                long expiresIn = ((Number) map.get("expires_in")).longValue();

                config.put("google_access_token", newAccess);
                config.put("google_token_expiry", String.valueOf((System.currentTimeMillis() / 1000L) + expiresIn));
                FileManager.saveConfig(configPath, config);
                return newAccess;
            } else {
                throw new IOException("Resposta de renovação de token inválida.");
            }
        }

        return accessToken;
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> getTaskLists(String accessToken) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://tasks.googleapis.com/tasks/v1/users/@me/lists?maxResults=100"))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Erro ao listar listas de tarefas (HTTP " + response.statusCode() + "): " + response.body());
        }

        Object parsed = JsonParser.parse(response.body());
        List<Map<String, Object>> results = new ArrayList<>();
        if (parsed instanceof Map) {
            Object items = ((Map<String, Object>) parsed).get("items");
            if (items instanceof List) {
                for (Object item : (List<?>) items) {
                    if (item instanceof Map) {
                        results.add((Map<String, Object>) item);
                    }
                }
            }
        }
        return results;
    }

    @SuppressWarnings("unchecked")
    public static String createTaskList(String accessToken, String title) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        String jsonBody = "{\"title\":\"" + escapeJson(title) + "\"}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://tasks.googleapis.com/tasks/v1/users/@me/lists"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Erro ao criar lista de tarefas (HTTP " + response.statusCode() + "): " + response.body());
        }

        Object parsed = JsonParser.parse(response.body());
        if (parsed instanceof Map) {
            return (String) ((Map<String, Object>) parsed).get("id");
        }
        throw new IOException("Resposta inválida ao criar lista de tarefas.");
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> getTasks(String accessToken, String taskListId) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://tasks.googleapis.com/tasks/v1/lists/" + taskListId + "/tasks?showCompleted=true&showHidden=true&maxResults=100"))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Erro ao listar tarefas (HTTP " + response.statusCode() + "): " + response.body());
        }

        Object parsed = JsonParser.parse(response.body());
        List<Map<String, Object>> results = new ArrayList<>();
        if (parsed instanceof Map) {
            Object items = ((Map<String, Object>) parsed).get("items");
            if (items instanceof List) {
                for (Object item : (List<?>) items) {
                    if (item instanceof Map) {
                        results.add((Map<String, Object>) item);
                    }
                }
            }
        }
        return results;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> createTask(String accessToken, String taskListId, String title, boolean completed, String notes, String due) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        String status = completed ? "completed" : "needsAction";
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"title\":\"").append(escapeJson(title)).append("\"");
        sb.append(",\"status\":\"").append(status).append("\"");
        if (notes != null && !notes.isEmpty()) {
            sb.append(",\"notes\":\"").append(escapeJson(notes)).append("\"");
        }
        if (due != null && !due.isEmpty()) {
            sb.append(",\"due\":\"").append(due).append("\"");
        }
        sb.append("}");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://tasks.googleapis.com/tasks/v1/lists/" + taskListId + "/tasks"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(sb.toString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Erro ao criar tarefa (HTTP " + response.statusCode() + "): " + response.body());
        }

        Object parsed = JsonParser.parse(response.body());
        if (parsed instanceof Map) {
            return (Map<String, Object>) parsed;
        }
        throw new IOException("Resposta inválida ao criar tarefa.");
    }

    @SuppressWarnings("unchecked")
    public static void updateTask(String accessToken, String taskListId, String taskId, String title, boolean completed, String notes, String due) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        String status = completed ? "completed" : "needsAction";
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"status\":\"").append(status).append("\"");
        if (title != null) {
            sb.append(",\"title\":\"").append(escapeJson(title)).append("\"");
        }
        if (notes != null) {
            sb.append(",\"notes\":\"").append(escapeJson(notes)).append("\"");
        }
        if (due != null) {
            sb.append(",\"due\":\"").append(escapeJson(due)).append("\"");
        }
        sb.append("}");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://tasks.googleapis.com/tasks/v1/lists/" + taskListId + "/tasks/" + taskId))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(sb.toString(), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Erro ao atualizar status da tarefa (HTTP " + response.statusCode() + "): " + response.body());
        }
    }

    public static void deleteTask(String accessToken, String taskListId, String taskId) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://tasks.googleapis.com/tasks/v1/lists/" + taskListId + "/tasks/" + taskId))
                .header("Authorization", "Bearer " + accessToken)
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 204 && response.statusCode() != 200) {
            throw new IOException("Erro ao deletar tarefa (HTTP " + response.statusCode() + "): " + response.body());
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
            } else if (ch < 32) {
                String hex = Integer.toHexString(ch);
                sb.append("\\u0000".substring(0, 6 - hex.length())).append(hex);
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }
}
