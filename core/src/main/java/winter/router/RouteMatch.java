package winter.router;

import java.nio.file.Path;
import java.util.Map;

public record RouteMatch(Path file, Map<String, String> params) {}
