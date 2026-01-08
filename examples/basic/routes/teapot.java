import winter.Ctx;
import winter.HttpError;

import java.util.Map;

public class Route {

    public Object get(Ctx ctx) {
        throw new HttpError(418, Map.of("error", "I'm a teapot"));
    }
}
