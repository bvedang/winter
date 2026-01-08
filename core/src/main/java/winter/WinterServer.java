package winter;

import io.undertow.Undertow;

public final class WinterServer implements AutoCloseable {

    private final Undertow server;
    private final AutoCloseable routeWatcher;

    WinterServer(Undertow server, AutoCloseable routeWatcher) {
        this.server = server;
        this.routeWatcher = routeWatcher;
    }

    @Override
    public void close() {
        try {
            if (routeWatcher != null) routeWatcher.close();
        } catch (Exception ignored) {}
        server.stop();
    }
}
