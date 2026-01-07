import java.util.Map;
import winter.Ctx;
import winter.HttpError;

public class Route {

    public Object get(Ctx ctx) {
        throw new HttpError(418, Map.of("error", "I'm a teapot"));
    }
}
