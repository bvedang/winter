import java.util.Map;
import winter.Ctx;

public class Route {

    public Object get(Ctx ctx) {
        return Map.of("id", ctx.param("id"));
    }
}
