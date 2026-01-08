package winter.reload;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import winter.compiler.RouteCompiler;

public final class RouteWatcher implements AutoCloseable {

    private final Path routesDir;
    private final RouteCompiler compiler;
    private final WatchService watchService;
    private final Thread thread;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Set<Path> registeredDirs = new HashSet<>();

    private RouteWatcher(
        Path routesDir,
        RouteCompiler compiler,
        WatchService watchService
    ) {
        this.routesDir = routesDir;
        this.compiler = compiler;
        this.watchService = watchService;
        this.thread = Thread.ofVirtual()
            .name("winter-route-watcher")
            .unstarted(this::run);
    }

    public static RouteWatcher start(Path routesDir, RouteCompiler compiler) {
        Objects.requireNonNull(routesDir, "routesDir");
        Objects.requireNonNull(compiler, "compiler");

        Path normalized = routesDir.toAbsolutePath().normalize();
        try {
            WatchService service = normalized.getFileSystem().newWatchService();
            var watcher = new RouteWatcher(normalized, compiler, service);
            watcher.registerDirTree(normalized);
            watcher.precompileAll();
            watcher.thread.start();
            return watcher;
        } catch (IOException exception) {
            throw new RuntimeException(
                "Failed to start route watcher for: " + normalized,
                exception
            );
        }
    }

    private void registerDirTree(Path root) throws IOException {
        Files.walkFileTree(
            root,
            new FileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(
                    Path dir,
                    BasicFileAttributes attrs
                ) throws IOException {
                    registerDir(dir);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(
                    Path file,
                    BasicFileAttributes attrs
                ) {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(
                    Path file,
                    IOException exc
                ) {
                    System.err.println(
                        "RouteWatcher: failed to visit " +
                            file +
                            " (" +
                            exc.getMessage() +
                            ")"
                    );
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(
                    Path dir,
                    IOException exc
                ) {
                    return FileVisitResult.CONTINUE;
                }
            }
        );
    }

    private void precompileAll() throws IOException {
        try (var stream = Files.walk(routesDir)) {
            stream
                .filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().endsWith(".java"))
                .forEach(this::recompileQuietly);
        }
    }

    private void registerDir(Path dir) throws IOException {
        if (!registeredDirs.add(dir)) return;
        dir.register(
            watchService,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_MODIFY,
            StandardWatchEventKinds.ENTRY_DELETE
        );
    }

    private void run() {
        while (running.get()) {
            WatchKey key;
            try {
                key = watchService.take();
            } catch (InterruptedException ignored) {
                return;
            } catch (Exception exception) {
                System.err.println(
                    "RouteWatcher: watchService failed (" +
                        exception.getMessage() +
                        ")"
                );
                return;
            }

            Path dir = (Path) key.watchable();
            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                if (kind == StandardWatchEventKinds.OVERFLOW) continue;

                @SuppressWarnings("unchecked")
                WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                Path child = dir.resolve(pathEvent.context()).normalize();
                if (!child.startsWith(routesDir)) continue;

                if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                    if (Files.isDirectory(child)) {
                        try {
                            registerDirTree(child);
                            precompileNewRoutesUnder(child);
                        } catch (IOException exception) {
                            System.err.println(
                                "RouteWatcher: failed to register new directory " +
                                    child +
                                    " (" +
                                    exception.getMessage() +
                                    ")"
                            );
                        }
                        continue;
                    }
                }

                if (!child.getFileName().toString().endsWith(".java")) continue;

                if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                    compiler.invalidate(child);
                    System.err.println(
                        "RouteWatcher: removed " + routesDir.relativize(child)
                    );
                    continue;
                }

                recompileQuietly(child);
            }

            boolean valid = key.reset();
            if (!valid) {
                System.err.println(
                    "RouteWatcher: watch key invalid for " + dir
                );
            }
        }
    }

    private void precompileNewRoutesUnder(Path dir) throws IOException {
        try (var stream = Files.walk(dir)) {
            stream
                .filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().endsWith(".java"))
                .forEach(this::recompileQuietly);
        }
    }

    private void recompileQuietly(Path routeFile) {
        compiler.invalidate(routeFile);
        if (!Files.isRegularFile(routeFile)) return;

        try {
            compiler.load(routeFile);
            System.err.println(
                "RouteWatcher: recompiled " + routesDir.relativize(routeFile)
            );
        } catch (Exception exception) {
            System.err.println(
                "RouteWatcher: compile error in " +
                    routesDir.relativize(routeFile)
            );
            System.err.println(exception.getMessage());
        }
    }

    @Override
    public void close() {
        running.set(false);
        thread.interrupt();
        try {
            watchService.close();
        } catch (IOException ignored) {}
    }
}
