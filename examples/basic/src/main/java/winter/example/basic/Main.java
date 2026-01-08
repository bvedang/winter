package winter.example.basic;

import winter.Winter;
import winter.WinterConfig;

import java.nio.file.Path;

public final class Main {

    public static void main(String[] args) {
        var config = WinterConfig.of(Path.of("routes")).withHotReload(true);
        Winter.start(config, new CorsMiddleware());
        System.out.println("Winter running on http://" + config.host() + ":" + config.port());
    }
}
