package winter;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;
import io.undertow.util.HttpString;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Ctx {

    private final HttpServerExchange exchange;
    private final Map<String, String> params;
    private final ObjectMapper objectMapper;
    private final int maxBodyBytes;
    private byte[] cachedBody;

    Ctx(
            HttpServerExchange exchange,
            Map<String, String> params,
            ObjectMapper objectMapper,
            int maxBodyBytes) {
        this.exchange = exchange;
        this.params = params;
        this.objectMapper = objectMapper;
        this.maxBodyBytes = maxBodyBytes;
    }

    public String param(String name) {
        return params.get(name);
    }

    public String query(String name) {
        Deque<String> values = exchange.getQueryParameters().get(name);
        if (values == null || values.isEmpty()) return null;
        return values.getFirst();
    }

    public List<String> queryAll(String name) {
        Deque<String> values = exchange.getQueryParameters().get(name);
        if (values == null || values.isEmpty()) return List.of();
        return List.copyOf(values);
    }

    public String header(String name) {
        HeaderValues values = exchange.getRequestHeaders().get(name);
        if (values == null || values.isEmpty()) return null;
        return values.getFirst();
    }

    public List<String> headers(String name) {
        HeaderValues values = exchange.getRequestHeaders().get(name);
        if (values == null || values.isEmpty()) return List.of();
        return List.copyOf(values);
    }

    public Map<String, List<String>> headers() {
        var out = new HashMap<String, List<String>>();
        for (HttpString headerName : exchange.getRequestHeaders().getHeaderNames()) {
            HeaderValues values = exchange.getRequestHeaders().get(headerName);
            if (values == null || values.isEmpty()) continue;
            out.put(headerName.toString(), new ArrayList<>(values));
        }
        return Map.copyOf(out);
    }

    public String cookie(String name) {
        var cookie = exchange.getRequestCookie(name);
        if (cookie == null) return null;
        return cookie.getValue();
    }

    public Map<String, String> cookies() {
        var out = new HashMap<String, String>();
        for (var cookie : exchange.requestCookies()) {
            out.put(cookie.getName(), cookie.getValue());
        }
        if (out.isEmpty()) return Map.of();
        return Map.copyOf(out);
    }

    public String method() {
        return exchange.getRequestMethod().toString();
    }

    public String path() {
        return exchange.getRequestPath();
    }

    public byte[] bodyBytes() {
        if (cachedBody != null) return cachedBody;
        long contentLength = exchange.getRequestContentLength();
        if (maxBodyBytes > 0 && contentLength > maxBodyBytes) {
            throw new HttpError(413, Map.of("error", "Payload Too Large"));
        }

        cachedBody = readUpTo(exchange.getInputStream(), maxBodyBytes);
        return cachedBody;
    }

    public String bodyText() {
        return new String(bodyBytes(), StandardCharsets.UTF_8);
    }

    public <T> T body(Class<T> type) {
        try {
            return objectMapper.readValue(bodyBytes(), type);
        } catch (IOException exception) {
            throw new HttpError(400, Map.of("error", "Bad Request", "message", "Invalid JSON"));
        }
    }

    private static byte[] readUpTo(InputStream inputStream, int maxBytes) {
        try {
            if (maxBytes <= 0) return inputStream.readAllBytes();

            var buffer = new byte[8192];
            int total = 0;
            var out = new ByteArrayOutputStream(Math.min(maxBytes, 8192));

            while (true) {
                int read = inputStream.read(buffer);
                if (read == -1) break;
                total += read;
                if (total > maxBytes) {
                    throw new HttpError(413, Map.of("error", "Payload Too Large"));
                }
                out.write(buffer, 0, read);
            }

            return out.toByteArray();
        } catch (IOException exception) {
            throw new WinterException("Failed to read request body", exception);
        }
    }
}
