import java.util.Map;
import winter.Ctx;

public class Route {

    public Object get(Ctx ctx) {
        return Map.of("category", ctx.param("category"), "id", ctx.param("id"));
    }
}
