package winter.integration;

import static org.junit.jupiter.api.Assertions.*;

import static java.net.http.HttpRequest.BodyPublishers;
import static java.net.http.HttpResponse.BodyHandlers;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import winter.Res;
import winter.Winter;
import winter.WinterConfig;
import winter.WinterServer;
import winter.middleware.Middleware;

import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.util.Map;

final class WinterIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void getIndexReturnsJson200(@TempDir Path tempDir) throws Exception {
        Path routes = tempDir.resolve("routes");
        Files.createDirectories(routes);
        writeRoute(
                routes.resolve("index.java"),
                """
            import winter.Ctx;
            import java.util.Map;
            public class Route {
              public Object get(Ctx ctx) { return Map.of("ok", true); }
            }
            """);

        try (var running = start(routes)) {
            var response =
                    running.client.send(
                            HttpRequest.newBuilder(running.base.resolve("/")).GET().build(),
                            BodyHandlers.ofString());

            assertEquals(200, response.statusCode());
            assertEquals(true, JSON.readValue(response.body(), Map.class).get("ok"));
        }
    }

    @Test
    void headFallsBackToGet(@TempDir Path tempDir) throws Exception {
        Path routes = tempDir.resolve("routes");
        Files.createDirectories(routes);
        writeRoute(
                routes.resolve("index.java"),
                """
            import winter.Ctx;
            public class Route {
              public Object get(Ctx ctx) { return "hello"; }
            }
            """);

        try (var running = start(routes)) {
            var response =
                    running.client.send(
                            HttpRequest.newBuilder(running.base.resolve("/"))
                                    .method("HEAD", BodyPublishers.noBody())
                                    .build(),
                            BodyHandlers.ofString());

            assertEquals(200, response.statusCode());
            assertTrue(response.body().isEmpty());
        }
    }

    @Test
    void optionsReturns204AndAllow(@TempDir Path tempDir) throws Exception {
        Path routes = tempDir.resolve("routes");
        Files.createDirectories(routes);
        writeRoute(
                routes.resolve("index.java"),
                """
            import winter.Ctx;
            public class Route {
              public Object get(Ctx ctx) { return "ok"; }
            }
            """);

        try (var running = start(routes)) {
            var response =
                    running.client.send(
                            HttpRequest.newBuilder(running.base.resolve("/"))
                                    .method("OPTIONS", BodyPublishers.noBody())
                                    .build(),
                            BodyHandlers.ofString());

            assertEquals(204, response.statusCode());
            String allow = response.headers().firstValue("Allow").orElse("");
            assertTrue(allow.contains("GET"));
            assertTrue(allow.contains("HEAD"));
            assertTrue(allow.contains("OPTIONS"));
        }
    }

    @Test
    void methodNotAllowedReturns405AndAllow(@TempDir Path tempDir) throws Exception {
        Path routes = tempDir.resolve("routes");
        Files.createDirectories(routes);
        Files.createDirectories(routes.resolve("users/[id]"));
        writeRoute(
                routes.resolve("users/[id]/index.java"),
                """
            import winter.Ctx;
            import java.util.Map;
            public class Route {
              public Object get(Ctx ctx) { return Map.of("id", ctx.param("id")); }
            }
            """);

        try (var running = start(routes)) {
            var response =
                    running.client.send(
                            HttpRequest.newBuilder(running.base.resolve("/users/123"))
                                    .PUT(BodyPublishers.noBody())
                                    .build(),
                            BodyHandlers.ofString());

            assertEquals(405, response.statusCode());
            String allow = response.headers().firstValue("Allow").orElse("");
            assertTrue(allow.contains("GET"));
            assertTrue(allow.contains("HEAD"));
            assertTrue(allow.contains("OPTIONS"));
        }
    }

    @Test
    void nestedDynamicRoutesWork(@TempDir Path tempDir) throws Exception {
        Path routes = tempDir.resolve("routes");
        Files.createDirectories(routes.resolve("users/[id]/posts"));
        writeRoute(
                routes.resolve("users/[id]/posts/index.java"),
                """
            import winter.Ctx;
            import java.util.Map;
            public class Route {
              public Object get(Ctx ctx) { return Map.of("userId", ctx.param("id")); }
            }
            """);

        try (var running = start(routes)) {
            var response =
                    running.client.send(
                            HttpRequest.newBuilder(running.base.resolve("/users/abc/posts"))
                                    .GET()
                                    .build(),
                            BodyHandlers.ofString());

            assertEquals(200, response.statusCode());
            assertEquals("abc", JSON.readValue(response.body(), Map.class).get("userId"));
        }
    }

    @Test
    void bodyLimitAndInvalidJsonHandled(@TempDir Path tempDir) throws Exception {
        Path routes = tempDir.resolve("routes");
        Files.createDirectories(routes);
        writeRoute(
                routes.resolve("len.java"),
                """
            import winter.Ctx;
            import java.util.Map;
            public class Route {
              public Object post(Ctx ctx) { return Map.of("len", ctx.bodyBytes().length); }
            }
            """);
        writeRoute(
                routes.resolve("parse.java"),
                """
            import winter.Ctx;
            public class Route {
              public Object post(Ctx ctx) { return ctx.body(java.util.Map.class); }
            }
            """);

        try (var running = start(routes, config -> config.withMaxBodyBytes(10))) {
            var tooBig =
                    running.client.send(
                            HttpRequest.newBuilder(running.base.resolve("/len"))
                                    .POST(BodyPublishers.ofString("01234567890"))
                                    .header("Content-Type", "text/plain")
                                    .build(),
                            BodyHandlers.ofString());
            assertEquals(413, tooBig.statusCode());

            var badJson =
                    running.client.send(
                            HttpRequest.newBuilder(running.base.resolve("/parse"))
                                    .POST(BodyPublishers.ofString("{"))
                                    .header("Content-Type", "application/json")
                                    .build(),
                            BodyHandlers.ofString());
            assertEquals(400, badJson.statusCode());
            assertEquals("Bad Request", JSON.readValue(badJson.body(), Map.class).get("error"));
        }
    }

    @Test
    void middlewareCanShortCircuitOptionsAndAddHeaders(@TempDir Path tempDir) throws Exception {
        Path routes = tempDir.resolve("routes");
        Files.createDirectories(routes);
        writeRoute(
                routes.resolve("index.java"),
                """
            import winter.Ctx;
            public class Route {
              public Object get(Ctx ctx) { return "ok"; }
            }
            """);

        Middleware middleware =
                new Middleware() {
                    @Override
                    public Object before(winter.Ctx ctx) {
                        if (!"OPTIONS".equalsIgnoreCase(ctx.method())) return null;
                        return Res.of(204, null).header("X-Preflight", "1");
                    }

                    @Override
                    public Object after(winter.Ctx ctx, Object result) {
                        if (result instanceof Res res) return res.header("X-MW", "1");
                        return Res.of(200, result).header("X-MW", "1");
                    }
                };

        try (var running = start(routes, middleware)) {
            var options =
                    running.client.send(
                            HttpRequest.newBuilder(running.base.resolve("/"))
                                    .method("OPTIONS", BodyPublishers.noBody())
                                    .build(),
                            BodyHandlers.ofString());
            assertEquals(204, options.statusCode());
            assertEquals("1", options.headers().firstValue("X-Preflight").orElse(null));
            assertEquals("1", options.headers().firstValue("X-MW").orElse(null));

            var get =
                    running.client.send(
                            HttpRequest.newBuilder(running.base.resolve("/")).GET().build(),
                            BodyHandlers.ofString());
            assertEquals(200, get.statusCode());
            assertEquals("1", get.headers().firstValue("X-MW").orElse(null));
        }
    }

    @Test
    void hotReloadUpdatesChangedRoute(@TempDir Path tempDir) throws Exception {
        Path routes = tempDir.resolve("routes");
        Files.createDirectories(routes);
        Path index = routes.resolve("index.java");

        writeRoute(
                index,
                """
            import winter.Ctx;
            import java.util.Map;
            public class Route {
              public Object get(Ctx ctx) { return Map.of("msg", "v1"); }
            }
            """);

        try (var running = start(routes, config -> config.withHotReload(true))) {
            assertEquals("v1", JSON.readValue(get(running, "/"), Map.class).get("msg"));

            writeRoute(
                    index,
                    """
                import winter.Ctx;
                import java.util.Map;
                public class Route {
                  public Object get(Ctx ctx) { return Map.of("msg", "v2"); }
                }
                """);
            Files.setLastModifiedTime(
                    index, FileTime.fromMillis(System.currentTimeMillis() + 2000));

            boolean updated = false;
            for (int i = 0; i < 50; i++) {
                String msg = (String) JSON.readValue(get(running, "/"), Map.class).get("msg");
                if ("v2".equals(msg)) {
                    updated = true;
                    break;
                }
                Thread.sleep(100);
            }

            assertTrue(updated, "Expected hot reload to serve updated route code");
        }
    }

    private static void writeRoute(Path file, String javaSource) throws Exception {
        Files.createDirectories(file.getParent());
        Files.writeString(file, javaSource);
    }

    private static Running start(Path routes, Middleware... middleware) {
        return start(routes, config -> config, middleware);
    }

    private static Running start(
            Path routes,
            java.util.function.UnaryOperator<WinterConfig> tweak,
            Middleware... middleware) {
        int port = freePort();
        WinterConfig config =
                tweak.apply(
                        WinterConfig.of(routes.toAbsolutePath())
                                .withHost("127.0.0.1")
                                .withPort(port));
        WinterServer server = Winter.start(config, middleware);
        var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        return new Running(server, URI.create("http://127.0.0.1:" + port), client);
    }

    private static int freePort() {
        try (var socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        } catch (Exception exception) {
            throw new RuntimeException("Failed to allocate free port", exception);
        }
    }

    private record Running(WinterServer server, URI base, HttpClient client)
            implements AutoCloseable {
        @Override
        public void close() {
            server.close();
        }
    }

    private static String get(Running running, String path) throws Exception {
        var response =
                running.client.send(
                        HttpRequest.newBuilder(running.base.resolve(path)).GET().build(),
                        BodyHandlers.ofString());
        assertEquals(
                200,
                response.statusCode(),
                "Unexpected status for GET " + path + ": " + response.body());
        return response.body();
    }
}
