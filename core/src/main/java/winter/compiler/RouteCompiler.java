package winter.compiler;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

public final class RouteCompiler {

    private record Cached(long lastModifiedMillis, RouteHandle handle) {}

    private final Map<Path, Cached> cache = new ConcurrentHashMap<>();

    public RouteCompiler() {}

    public void invalidate(Path routeFile) {
        if (routeFile == null) return;
        var existing = cache.remove(routeFile);
        if (existing != null) closeQuietly(existing.handle);
    }

    public RouteHandle load(Path routeFile) {
        long lastModified;
        try {
            lastModified = Files.getLastModifiedTime(routeFile).toMillis();
        } catch (Exception exception) {
            throw new RuntimeException("Failed to stat route file: " + routeFile, exception);
        }

        return cache.compute(
                        routeFile,
                        (path, existing) -> {
                            if (existing != null && existing.lastModifiedMillis == lastModified)
                                return existing;

                            RouteHandle next = compile(routeFile);
                            if (existing != null) closeQuietly(existing.handle);
                            return new Cached(lastModified, next);
                        })
                .handle;
    }

    private RouteHandle compile(Path routeFile) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new RuntimeException("No Java compiler found. Run with a JDK (not a JRE).");
        }

        Path outputDir = cacheDirFor(routeFile);
        try {
            Files.createDirectories(outputDir);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to create route cache dir: " + outputDir, exception);
        }

        String sourceText;
        try {
            sourceText = Files.readString(routeFile, UTF_8);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to read route: " + routeFile, exception);
        }

        var diagnostics = new DiagnosticCollector<JavaFileObject>();

        try (var fileManager = compiler.getStandardFileManager(diagnostics, null, UTF_8)) {
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT, List.of(outputDir.toFile()));

            String classpath = System.getProperty("java.class.path", "");
            var options = List.of("-classpath", classpath);

            JavaFileObject routeSource =
                    new SimpleJavaFileObject(
                            URI.create("winter-route:///Route.java"), JavaFileObject.Kind.SOURCE) {
                        @Override
                        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                            return sourceText;
                        }
                    };

            boolean ok =
                    compiler.getTask(
                                    null,
                                    fileManager,
                                    diagnostics,
                                    options,
                                    null,
                                    List.of(routeSource))
                            .call();
            if (!ok) {
                throw new RuntimeException(
                        "Failed to compile route: "
                                + routeFile
                                + "\n"
                                + formatDiagnostics(diagnostics));
            }
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new RuntimeException("Failed to compile route: " + routeFile, exception);
        }

        try {
            URLClassLoader loader =
                    new URLClassLoader(
                            new URL[] {outputDir.toUri().toURL()}, getClass().getClassLoader());
            Class<?> routeClass = Class.forName("Route", true, loader);
            return new RouteHandle(routeClass, loader);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to load compiled route: " + routeFile, exception);
        }
    }

    private static void closeQuietly(RouteHandle handle) {
        try {
            handle.close();
        } catch (Exception exception) {
            System.err.println("Failed to close route classloader: " + exception.getMessage());
        }
    }

    private static Path cacheDirFor(Path routeFile) {
        String hash = sha1(routeFile.toAbsolutePath().normalize().toString());
        return Path.of(System.getProperty("java.io.tmpdir"), "winter-route-cache", hash);
    }

    private static String sha1(String input) {
        try {
            var digest = MessageDigest.getInstance("SHA-1").digest(input.getBytes(UTF_8));
            var out = new StringBuilder(digest.length * 2);
            for (byte b : digest) out.append(String.format("%02x", b));
            return out.toString();
        } catch (Exception exception) {
            throw new RuntimeException("Failed to hash route path", exception);
        }
    }

    private static String formatDiagnostics(DiagnosticCollector<JavaFileObject> diagnostics) {
        var out = new StringBuilder();
        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
            out.append("Line ")
                    .append(diagnostic.getLineNumber())
                    .append(": ")
                    .append(diagnostic.getMessage(null))
                    .append('\n');
        }
        return out.toString();
    }
}
