package winter.compiler;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class RouteCompilerTest {

    @Test
    void invalidateForcesReload(@TempDir Path tempDir) throws Exception {
        Path route = tempDir.resolve("index.java");
        Files.writeString(
            route,
            """
            import winter.Ctx;
            public class Route {
              public Object get(Ctx ctx) { return "v1"; }
            }
            """
        );

        var compiler = new RouteCompiler();
        RouteHandle first = compiler.load(route);
        compiler.invalidate(route);
        RouteHandle second = compiler.load(route);

        assertNotSame(first, second);
    }
}
