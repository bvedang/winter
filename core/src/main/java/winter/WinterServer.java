package winter;

import io.undertow.Undertow;

public final class WinterServer implements AutoCloseable {

    private final Undertow server;

    WinterServer(Undertow server) {
        this.server = server;
    }

    @Override
    public void close() {
        server.stop();
    }
}
