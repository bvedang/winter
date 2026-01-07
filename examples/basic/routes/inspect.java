import java.util.Map;
import winter.Ctx;

public class Route {

    public Object get(Ctx ctx) {
        return Map.of(
            "qFirst",
            ctx.query("q"),
            "qAll",
            ctx.queryAll("q"),
            "headerFirst",
            ctx.header("x-test"),
            "headerAll",
            ctx.headers("x-test"),
            "cookie",
            ctx.cookie("session")
        );
    }
}
