package com.debjit.app;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * A tiny dependency-free HTTP service (JDK built-in server). It exists to give
 * the DevSecOps pipeline something real to build, scan, containerise, and
 * smoke-test — not to be a full application.
 */
public final class App {

    private App() {
    }

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/health", ex -> respond(ex, 200, "OK"));

        server.createContext("/", ex -> {
            if (ex.getRequestURI().getPath().equals("/")) {
                respond(ex, 200, "DevSecOps pipeline demo — built, scanned, and shipped.\n");
            } else {
                respond(ex, 404, "Not Found\n");
            }
        });

        // Reflects a message back, HTML-escaped — demonstrates safe output encoding.
        server.createContext("/echo", ex -> {
            String msg = queryParam(ex.getRequestURI(), "msg");
            respond(ex, 200, "<p>" + HtmlEscaper.escape(msg) + "</p>\n");
        });

        server.setExecutor(null);
        server.start();
        System.out.println("Listening on :" + port);
    }

    private static void respond(HttpExchange ex, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    static String queryParam(URI uri, String key) {
        String query = uri.getRawQuery();
        if (query == null || query.isBlank()) {
            return "";
        }
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && kv[0].equals(key)) {
                return java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return "";
    }

    // Exposed for completeness / potential reuse.
    static Map<String, String> emptyEnv() {
        return Map.of();
    }
}
