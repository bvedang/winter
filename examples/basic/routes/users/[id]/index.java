import winter.Ctx;

import java.util.Map;

public class Route {

    public Object get(Ctx ctx) {
        return Map.of("id", ctx.param("id"));
    }
}
