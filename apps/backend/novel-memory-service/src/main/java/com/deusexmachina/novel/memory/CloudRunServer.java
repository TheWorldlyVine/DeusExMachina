package com.deusexmachina.novel.memory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.google.cloud.functions.HttpRequest;
import com.google.cloud.functions.HttpResponse;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Simple HTTP server wrapper to run the Cloud Function on Cloud Run
 */
public class CloudRunServer {
    private static final Logger logger = Logger.getLogger(CloudRunServer.class.getName());
    private static final int PORT = Integer.parseInt(System.getenv("PORT") != null ? System.getenv("PORT") : "8080");
    
    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        
        NovelMemoryFunction function = new NovelMemoryFunction();
        
        // Route all requests to the function
        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                CloudRunHttpRequest request = new CloudRunHttpRequest(exchange);
                CloudRunHttpResponse response = new CloudRunHttpResponse(exchange);
                
                try {
                    function.service(request, response);
                } catch (Exception e) {
                    logger.severe("Error handling request: " + e.getMessage());
                    e.printStackTrace();
                    if (!response.isCommitted()) {
                        exchange.sendResponseHeaders(500, -1);
                    }
                } finally {
                    response.close();
                }
            }
        });
        
        // Health check endpoint
        server.createContext("/health", exchange -> {
            String response = "{\"status\":\"healthy\",\"service\":\"novel-memory-service\"}";
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        });
        
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        
        logger.info("Novel Memory Service started on port " + PORT);
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down server...");
            server.stop(0);
        }));
    }
    
    // Adapter classes to bridge HttpExchange to Cloud Function interfaces
    static class CloudRunHttpRequest implements HttpRequest {
        private final HttpExchange exchange;
        private final Map<String, List<String>> queryParams;
        private String body;
        
        CloudRunHttpRequest(HttpExchange exchange) {
            this.exchange = exchange;
            this.queryParams = parseQuery(exchange.getRequestURI().getQuery());
        }
        
        @Override
        public String getMethod() {
            return exchange.getRequestMethod();
        }
        
        @Override
        public String getUri() {
            return exchange.getRequestURI().toString();
        }
        
        @Override
        public String getPath() {
            return exchange.getRequestURI().getPath();
        }
        
        @Override
        public Optional<String> getQuery() {
            return Optional.ofNullable(exchange.getRequestURI().getQuery());
        }
        
        @Override
        public Map<String, List<String>> getQueryParameters() {
            return queryParams;
        }
        
        @Override
        public Map<String, HttpPart> getParts() {
            return Collections.emptyMap(); // Not implemented for simplicity
        }
        
        @Override
        public Optional<String> getContentType() {
            return Optional.ofNullable(exchange.getRequestHeaders().getFirst("Content-Type"));
        }
        
        @Override
        public long getContentLength() {
            String contentLength = exchange.getRequestHeaders().getFirst("Content-Length");
            return contentLength != null ? Long.parseLong(contentLength) : -1;
        }
        
        @Override
        public Optional<String> getCharacterEncoding() {
            return Optional.of("UTF-8");
        }
        
        @Override
        public String getReader() throws IOException {
            if (body == null) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(exchange.getRequestBody(), "UTF-8"))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    body = sb.toString();
                }
            }
            return body;
        }
        
        @Override
        public InputStream getInputStream() {
            return exchange.getRequestBody();
        }
        
        @Override
        public Map<String, HttpHeaders> getHeaders() {
            Map<String, HttpHeaders> headers = new HashMap<>();
            exchange.getRequestHeaders().forEach((key, values) -> {
                headers.put(key, new HttpHeaders() {
                    @Override
                    public List<String> getValues() {
                        return values;
                    }
                });
            });
            return headers;
        }
        
        @Override
        public List<String> getHeaders(String name) {
            return exchange.getRequestHeaders().get(name);
        }
        
        @Override
        public Optional<String> getFirstHeader(String name) {
            return Optional.ofNullable(exchange.getRequestHeaders().getFirst(name));
        }
        
        private Map<String, List<String>> parseQuery(String query) {
            Map<String, List<String>> params = new HashMap<>();
            if (query != null && !query.isEmpty()) {
                for (String param : query.split("&")) {
                    String[] parts = param.split("=", 2);
                    String key = parts[0];
                    String value = parts.length > 1 ? parts[1] : "";
                    params.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
                }
            }
            return params;
        }
    }
    
    static class CloudRunHttpResponse implements HttpResponse {
        private final HttpExchange exchange;
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        private final PrintWriter writer = new PrintWriter(new OutputStreamWriter(buffer));
        private int statusCode = 200;
        private boolean committed = false;
        
        CloudRunHttpResponse(HttpExchange exchange) {
            this.exchange = exchange;
        }
        
        @Override
        public void setStatusCode(int code) {
            this.statusCode = code;
        }
        
        @Override
        public void setStatusCode(int code, String message) {
            this.statusCode = code;
        }
        
        @Override
        public void appendHeader(String header, String value) {
            exchange.getResponseHeaders().add(header, value);
        }
        
        @Override
        public void setContentType(String contentType) {
            exchange.getResponseHeaders().set("Content-Type", contentType);
        }
        
        @Override
        public Optional<String> getContentType() {
            return Optional.ofNullable(exchange.getResponseHeaders().getFirst("Content-Type"));
        }
        
        @Override
        public BufferedWriter getWriter() {
            return new BufferedWriter(writer);
        }
        
        @Override
        public OutputStream getOutputStream() throws IOException {
            return buffer;
        }
        
        boolean isCommitted() {
            return committed;
        }
        
        void close() throws IOException {
            if (!committed) {
                writer.flush();
                byte[] responseBytes = buffer.toByteArray();
                exchange.sendResponseHeaders(statusCode, responseBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
                committed = true;
            }
        }
    }
}