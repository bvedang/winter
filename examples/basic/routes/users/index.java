import java.util.List;
import java.util.Map;
import winter.Ctx;

public class Route {

    public Object get(Ctx ctx) {
        return List.of(
            Map.of("id", "1", "name", "Ada"),
            Map.of("id", "2", "name", "Linus")
        );
    }
}
