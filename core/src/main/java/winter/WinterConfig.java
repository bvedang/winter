package winter;

import java.nio.file.Path;

public record WinterConfig(
    Path routesDir,
    String host,
    int port,
    int maxBodyBytes,
    boolean exposeErrors
) {
    public static final int DEFAULT_MAX_BODY_BYTES = 1024 * 1024;

    public static WinterConfig of(Path routesDir) {
        return new WinterConfig(
            routesDir,
            "0.0.0.0",
            8080,
            DEFAULT_MAX_BODY_BYTES,
            false
        );
    }

    public WinterConfig withHost(String host) {
        return new WinterConfig(
            routesDir,
            host,
            port,
            maxBodyBytes,
            exposeErrors
        );
    }

    public WinterConfig withPort(int port) {
        return new WinterConfig(
            routesDir,
            host,
            port,
            maxBodyBytes,
            exposeErrors
        );
    }

    public WinterConfig withMaxBodyBytes(int maxBodyBytes) {
        return new WinterConfig(
            routesDir,
            host,
            port,
            maxBodyBytes,
            exposeErrors
        );
    }

    public WinterConfig withExposeErrors(boolean exposeErrors) {
        return new WinterConfig(
            routesDir,
            host,
            port,
            maxBodyBytes,
            exposeErrors
        );
    }
}
