package winter;

import java.util.HashMap;
import java.util.Map;

public record Res(int status, Map<String, String> headers, Object body) {
    public static Res of(int status, Object body) {
        return new Res(status, Map.of(), body);
    }

    public Res header(String name, String value) {
        var next = new HashMap<>(headers);
        next.put(name, value);
        return new Res(status, Map.copyOf(next), body);
    }
}
