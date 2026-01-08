import winter.Ctx;

import java.util.Map;

public class Route {

    public Object get(Ctx ctx) {
        return Map.of("message", "Hello from Winter");
    }
}
