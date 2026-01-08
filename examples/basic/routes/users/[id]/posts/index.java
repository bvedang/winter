import winter.Ctx;

import java.util.Map;

public class Route {

    public Object get(Ctx ctx) {
        return Map.of("userId", ctx.param("id"), "posts", new String[] {"p1", "p2"});
    }
}
