package winter;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.BlockingHandler;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;

import winter.compiler.RouteCompiler;
import winter.middleware.Middleware;
import winter.reload.RouteWatcher;
import winter.router.FileRouter;
import winter.router.RouteMatch;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Winter {

    private final WinterConfig config;
    private final ObjectMapper objectMapper;
    private final FileRouter router;
    private final RouteCompiler compiler;
    private final List<Middleware> middlewares;

    private Winter(WinterConfig config, List<Middleware> middlewares) {
        this.config = config;
        this.objectMapper = new ObjectMapper();
        this.router = new FileRouter(config.routesDir());
        this.compiler = new RouteCompiler();
        this.middlewares = List.copyOf(middlewares);
    }

    public static WinterServer start(WinterConfig config) {
        return start(config, new Middleware[0]);
    }

    public static WinterServer start(WinterConfig config, Middleware... middlewares) {
        var winter = new Winter(config, Arrays.asList(middlewares));

        Undertow server =
                Undertow.builder()
                        .addHttpListener(config.port(), config.host())
                        .setHandler(new BlockingHandler(winter.handler()))
                        .build();
        server.start();

        AutoCloseable watcher = null;
        if (config.hotReload()) {
            watcher = RouteWatcher.start(config.routesDir(), winter.compiler);
        }

        return new WinterServer(server, watcher);
    }

    private HttpHandler handler() {
        return exchange -> {
            RouteMatch match = router.match(exchange.getRequestPath());
            var params = match == null ? Map.<String, String>of() : match.params();
            var ctx = new Ctx(exchange, params, objectMapper, config.maxBodyBytes());

            Object result = execute(ctx, () -> dispatch(match, exchange, ctx));
            writeResult(exchange, result);
        };
    }

    @FunctionalInterface
    private interface TerminalHandler {
        Object handle() throws Exception;
    }

    private Object dispatch(RouteMatch match, HttpServerExchange exchange, Ctx ctx)
            throws Exception {
        if (match == null) {
            return Res.of(404, Map.of("error", "Not Found"));
        }

        var handle = compiler.load(match.file());
        var method = exchange.getRequestMethod().toString().toUpperCase();
        var verb = method.toLowerCase();

        if ("OPTIONS".equals(method)) {
            return Res.of(204, null).header("Allow", allowHeader(handle.allowedMethods()));
        }

        if ("HEAD".equals(method)) {
            try {
                return handle.invoke("head", ctx);
            } catch (NoSuchMethodException ignored) {
                try {
                    return handle.invoke("get", ctx);
                } catch (NoSuchMethodException exception) {
                    return Res.of(405, Map.of("error", "Method Not Allowed"))
                            .header("Allow", allowHeader(handle.allowedMethods()));
                }
            }
        }

        try {
            return handle.invoke(verb, ctx);
        } catch (NoSuchMethodException exception) {
            return Res.of(405, Map.of("error", "Method Not Allowed"))
                    .header("Allow", allowHeader(handle.allowedMethods()));
        }
    }

    private static String allowHeader(Set<String> allowed) {
        var out = new ArrayList<String>(allowed.size());
        addIfPresent(out, allowed, "GET");
        addIfPresent(out, allowed, "HEAD");
        addIfPresent(out, allowed, "POST");
        addIfPresent(out, allowed, "PUT");
        addIfPresent(out, allowed, "PATCH");
        addIfPresent(out, allowed, "DELETE");
        addIfPresent(out, allowed, "OPTIONS");
        for (String method : allowed) {
            if (!out.contains(method)) out.add(method);
        }
        return String.join(", ", out);
    }

    private static void addIfPresent(List<String> out, Set<String> allowed, String method) {
        if (allowed.contains(method)) out.add(method);
    }

    private Object execute(Ctx ctx, TerminalHandler terminal) {
        var executed = new ArrayList<Middleware>(middlewares.size());
        Object early = null;
        try {
            for (Middleware middleware : middlewares) {
                executed.add(middleware);
                early = middleware.before(ctx);
                if (early != null) break;
            }

            Object result = early != null ? early : terminal.handle();

            for (int i = executed.size() - 1; i >= 0; i--) {
                result = executed.get(i).after(ctx, result);
            }

            return result;
        } catch (Exception exception) {
            Object result = null;
            Exception current = exception;

            for (int i = executed.size() - 1; i >= 0; i--) {
                try {
                    result = executed.get(i).onError(ctx, current);
                    current = null;
                    break;
                } catch (Exception next) {
                    current = next;
                }
            }

            if (current != null) {
                result = defaultError(ctx, current);
            }

            for (int i = executed.size() - 1; i >= 0; i--) {
                try {
                    result = executed.get(i).after(ctx, result);
                } catch (Exception ignored) {
                    // Best-effort after hooks; response will fall back to what we have.
                }
            }

            return result;
        }
    }

    private Object defaultError(Ctx ctx, Exception exception) {
        if (exception instanceof HttpError error) return Res.of(error.status(), error.body());

        exception.printStackTrace(System.err);
        if (config.exposeErrors()) {
            return Res.of(
                    500,
                    Map.of(
                            "error",
                            "Internal Server Error",
                            "message",
                            String.valueOf(exception.getMessage())));
        }
        return Res.of(500, Map.of("error", "Internal Server Error"));
    }

    private void writeResult(HttpServerExchange exchange, Object result) {
        boolean head = exchange.getRequestMethod().equalToString("HEAD");

        if (result == null) {
            exchange.setStatusCode(204);
            return;
        }

        if (result instanceof Res res) {
            exchange.setStatusCode(res.status());
            res.headers()
                    .forEach(
                            (name, value) ->
                                    exchange.getResponseHeaders()
                                            .put(HttpString.tryFromString(name), value));
            writeResult(exchange, res.body());
            return;
        }

        if (result instanceof String text) {
            if (head) {
                byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
                exchange.setResponseContentLength(bytes.length);
                exchange.getResponseHeaders()
                        .put(Headers.CONTENT_TYPE, "text/plain; charset=utf-8");
                return;
            }
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain; charset=utf-8");
            exchange.getResponseSender().send(text, StandardCharsets.UTF_8);
            return;
        }

        int status = exchange.getStatusCode();
        if (status == 0) status = 200;
        writeJson(exchange, status, result, !head);
    }

    private void writeJson(HttpServerExchange exchange, int status, Object body, boolean sendBody) {
        exchange.setStatusCode(status);
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json; charset=utf-8");
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            exchange.setResponseContentLength(bytes.length);
            if (sendBody) {
                exchange.getResponseSender().send(new String(bytes, StandardCharsets.UTF_8));
            }
        } catch (Exception exception) {
            exchange.setStatusCode(500);
            exchange.getResponseHeaders()
                    .put(Headers.CONTENT_TYPE, "application/json; charset=utf-8");
            if (sendBody) {
                exchange.getResponseSender().send("{\"error\":\"Internal Server Error\"}");
            }
        }
    }
}
