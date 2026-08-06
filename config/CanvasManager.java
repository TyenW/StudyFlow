package config;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CanvasManager {

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> fetchJsonList(String initialUrl, String token) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        String url = initialUrl;
        List<Map<String, Object>> allResults = new ArrayList<>();

        while (url != null) {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 401) {
                throw new IOException("Token inválido ou expirado (HTTP 401).");
            } else if (response.statusCode() != 200) {
                throw new IOException("Erro na API do Canvas (Código: " + response.statusCode() + ").");
            }

            String body = response.body();
            Object parsed = JsonParser.parse(body);
            if (parsed instanceof List) {
                List<?> list = (List<?>) parsed;
                for (Object item : list) {
                    if (item instanceof Map) {
                        allResults.add((Map<String, Object>) item);
                    }
                }
            } else if (parsed instanceof Map) {
                allResults.add((Map<String, Object>) parsed);
            }

            url = null;
            Optional<String> linkHeader = response.headers().firstValue("Link");
            if (linkHeader.isPresent()) {
                String val = linkHeader.get();
                Pattern nextPat = Pattern.compile("<([^>]+)>;\\s*rel=\"next\"");
                Matcher m = nextPat.matcher(val);
                if (m.find()) {
                    url = m.group(1);
                }
            }
        }
        return allResults;
    }

    public static List<Map<String, Object>> getCourses(String canvasUrl, String token) throws IOException, InterruptedException {
        String baseUrl = canvasUrl.endsWith("/") ? canvasUrl : canvasUrl + "/";
        String url = baseUrl + "api/v1/courses?enrollment_state=active&per_page=100";
        return fetchJsonList(url, token);
    }

    public static List<Map<String, Object>> getAssignments(String canvasUrl, String token, String courseId) throws IOException, InterruptedException {
        String baseUrl = canvasUrl.endsWith("/") ? canvasUrl : canvasUrl + "/";
        String url = baseUrl + "api/v1/courses/" + courseId + "/assignments?per_page=100&include%5B%5D=submission";
        return fetchJsonList(url, token);
    }

    public static String formatCanvasDate(String dueAtUtc) {
        if (dueAtUtc == null || dueAtUtc.trim().isEmpty() || dueAtUtc.equals("null")) {
            return "[SEM PRAZO]";
        }
        try {
            ZonedDateTime utcTime = ZonedDateTime.parse(dueAtUtc);
            ZonedDateTime localTime = utcTime.withZoneSameInstant(ZoneId.systemDefault());
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm");
            return "Prazo: " + localTime.format(formatter);
        } catch (Exception e) {
            return "[SEM PRAZO]";
        }
    }

    public static Map<String, Object> getRootFolder(String canvasUrl, String token, String courseId) throws IOException, InterruptedException {
        String baseUrl = canvasUrl.endsWith("/") ? canvasUrl : canvasUrl + "/";
        String url = baseUrl + "api/v1/courses/" + courseId + "/folders/root";
        List<Map<String, Object>> res = fetchJsonList(url, token);
        if (!res.isEmpty()) {
            return res.get(0);
        }
        throw new IOException("Pasta raiz não encontrada para o curso " + courseId);
    }

    public static List<Map<String, Object>> getSubfolders(String canvasUrl, String token, String folderId) throws IOException, InterruptedException {
        String baseUrl = canvasUrl.endsWith("/") ? canvasUrl : canvasUrl + "/";
        String url = baseUrl + "api/v1/folders/" + folderId + "/folders?per_page=100";
        return fetchJsonList(url, token);
    }

    public static List<Map<String, Object>> getFolderFiles(String canvasUrl, String token, String folderId) throws IOException, InterruptedException {
        String baseUrl = canvasUrl.endsWith("/") ? canvasUrl : canvasUrl + "/";
        String url = baseUrl + "api/v1/folders/" + folderId + "/files?per_page=100";
        return fetchJsonList(url, token);
    }

    public static void downloadFile(String urlStr, String token, java.nio.file.Path destFile) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        String currentUrl = urlStr;
        
        while (true) {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(currentUrl))
                .GET();
            
            if (token != null && !currentUrl.contains("amazonaws.com") && !currentUrl.contains("s3")) {
                builder.header("Authorization", "Bearer " + token);
            }
            
            HttpRequest request = builder.build();
            HttpResponse<java.io.InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            
            int status = response.statusCode();
            if (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
                String loc = response.headers().firstValue("Location").orElse(null);
                if (loc != null) {
                    currentUrl = loc;
                    response.body().close();
                    continue;
                }
            }
            
            if (status != 200) {
                response.body().close();
                throw new IOException("Falha no download (Código HTTP: " + status + ")");
            }
            
            java.nio.file.Files.createDirectories(destFile.getParent());
            
            try (java.io.InputStream is = response.body();
                 java.io.OutputStream os = java.nio.file.Files.newOutputStream(destFile)) {
                is.transferTo(os);
            }
            break;
        }
    }

    public static List<Map<String, Object>> getCourseFolders(String canvasUrl, String token, String courseId) throws IOException, InterruptedException {
        String baseUrl = canvasUrl.endsWith("/") ? canvasUrl : canvasUrl + "/";
        String url = baseUrl + "api/v1/courses/" + courseId + "/folders?per_page=100";
        return fetchJsonList(url, token);
    }

    public static List<Map<String, Object>> getCourseFiles(String canvasUrl, String token, String courseId) throws IOException, InterruptedException {
        String baseUrl = canvasUrl.endsWith("/") ? canvasUrl : canvasUrl + "/";
        String url = baseUrl + "api/v1/courses/" + courseId + "/files?per_page=100";
        return fetchJsonList(url, token);
    }
}
