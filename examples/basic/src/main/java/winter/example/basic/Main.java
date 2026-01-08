package winter.example.basic;

import java.nio.file.Path;
import winter.Winter;
import winter.WinterConfig;

public final class Main {

    public static void main(String[] args) {
        var config = WinterConfig.of(Path.of("routes")).withHotReload(true);
        Winter.start(config, new CorsMiddleware());
        System.out.println("Winter running on http://" + config.host() + ":" + config.port());
    }
}
