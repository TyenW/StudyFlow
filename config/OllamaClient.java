package config;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
import java.util.ArrayList;

public class OllamaClient {
    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static boolean isOllamaAvailable() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:11434/"))
                    .GET()
                    .timeout(Duration.ofSeconds(2))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    public static String getAvailableModel() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:11434/api/tags"))
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String body = response.body();
                List<String> models = new ArrayList<>();
                Pattern pattern = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
                Matcher matcher = pattern.matcher(body);
                while (matcher.find()) {
                    models.add(matcher.group(1));
                }
                
                if (!models.isEmpty()) {
                    // Prefer deepseek-r1 models (e.g. deepseek-r1:1.5b)
                    for (String m : models) {
                        if (m.startsWith("deepseek-r1") || m.contains("deepseek")) {
                            return m;
                        }
                    }
                    // Else prefer other common models
                    for (String m : models) {
                        String lower = m.toLowerCase();
                        if (lower.startsWith("llama") || lower.startsWith("qwen") || lower.startsWith("mistral")) {
                            return m;
                        }
                    }
                    // Return first model available
                    return models.get(0);
                }
            }
        } catch (Exception e) {
            // ignore and fallback
        }
        return "deepseek-r1:1.5b"; // Default fallback
    }

    public static String askOllama(String model, String systemPrompt, String userPrompt) throws Exception {
        String escapedUserPrompt = escapeJson(userPrompt);
        String escapedSystemPrompt = escapeJson(systemPrompt);

        String jsonBody = String.format(
            "{\"model\": \"%s\", \"prompt\": \"%s\", \"system\": \"%s\", \"stream\": false}",
            model, escapedUserPrompt, escapedSystemPrompt
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OLLAMA_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .timeout(Duration.ofMinutes(5)) // Aumentado para 5 minutos para textos longos
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("Erro do Ollama: HTTP " + response.statusCode());
        }

        return extractResponseFromJson(response.body());
    }

    // Método nativo para extrair a resposta sem usar dependências externas como Gson/Jackson
    private static String extractResponseFromJson(String json) {
        // Busca o conteúdo dentro do campo "response":"..."
        Pattern pattern = Pattern.compile("\"response\"\\s*:\\s*\"(.*?)\",\\s*\"done\"", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(json);
        
        if (matcher.find()) {
            String rawResponse = matcher.group(1);
            // Desfaz o escape das quebras de linha enviadas pelo JSON
            return rawResponse.replace("\\n", "\n")
                              .replace("\\\"", "\"")
                              .replace("\\t", "\t")
                              .replace("\\\\", "\\");
        }
        return "[Erro] Não foi possível extrair a resposta da IA.";
    }

    private static String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\b", "\\b")
                  .replace("\f", "\\f")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}
