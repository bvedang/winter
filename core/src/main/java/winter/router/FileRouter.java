package winter.router;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FileRouter {
    private final Path routesDir;

    public FileRouter(Path routesDir) {
        this.routesDir = routesDir.toAbsolutePath().normalize();
    }

    public RouteMatch match(String requestPath) {
        String path = normalizePath(requestPath);
        if (path.equals("/")) {
            Path file = routesDir.resolve("index.java");
            if (Files.isRegularFile(file)) return new RouteMatch(file, Map.of());
            return null;
        }

        var params = new HashMap<String, String>();
        Path current = routesDir;
        String[] segments = path.substring(1).split("/");

        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i];
            boolean last = i == segments.length - 1;

            if (!isSafeSegment(segment)) return null;

            Path exactDir = safeResolve(current, segment);
            if (exactDir == null) return null;
            if (Files.isDirectory(exactDir)) {
                current = exactDir;
                if (last) {
                    Path index = safeResolve(current, "index.java");
                    if (index == null) return null;
                    if (Files.isRegularFile(index))
                        return new RouteMatch(index, Map.copyOf(params));
                    return null;
                }
                continue;
            }

            if (last) {
                Path exactFile = safeResolve(current, segment + ".java");
                if (exactFile == null) return null;
                if (Files.isRegularFile(exactFile))
                    return new RouteMatch(exactFile, Map.copyOf(params));
            }

            Path dynamicDir = findSingleDynamicDir(current);
            if (dynamicDir != null) {
                String paramName = paramNameFromDir(dynamicDir.getFileName().toString());
                if (params.containsKey(paramName)) {
                    throw new IllegalStateException(
                            "Duplicate param name in route match: " + paramName);
                }
                params.put(paramName, segment);
                current = dynamicDir;
                if (last) {
                    Path index = safeResolve(current, "index.java");
                    if (index == null) return null;
                    if (Files.isRegularFile(index))
                        return new RouteMatch(index, Map.copyOf(params));
                    return null;
                }
                continue;
            }

            if (last) {
                Path dynamicFile = findSingleDynamicJavaFile(current);
                if (dynamicFile != null) {
                    String paramName = paramNameFromFile(dynamicFile.getFileName().toString());
                    if (params.containsKey(paramName)) {
                        throw new IllegalStateException(
                                "Duplicate param name in route match: " + paramName);
                    }
                    params.put(paramName, segment);
                    return new RouteMatch(dynamicFile, Map.copyOf(params));
                }
            }

            return null;
        }

        return null;
    }

    private boolean isWithinRoutes(Path path) {
        return path.startsWith(routesDir);
    }

    private Path safeResolve(Path base, String child) {
        Path resolved = base.resolve(child).normalize();
        if (!isWithinRoutes(resolved)) return null;
        return resolved;
    }

    private static boolean isSafeSegment(String segment) {
        if (segment == null || segment.isEmpty()) return false;
        if (segment.equals(".") || segment.equals("..")) return false;
        return segment.indexOf('/') < 0 && segment.indexOf('\\') < 0;
    }

    private static String normalizePath(String requestPath) {
        if (requestPath == null || requestPath.isBlank()) return "/";
        String p = requestPath;
        int q = p.indexOf('?');
        if (q >= 0) p = p.substring(0, q);
        if (!p.startsWith("/")) p = "/" + p;
        if (p.length() > 1 && p.endsWith("/")) p = p.substring(0, p.length() - 1);
        return p;
    }

    private static Path findSingleDynamicDir(Path directory) {
        List<Path> matches = new ArrayList<>();

        try (var stream = Files.list(directory)) {
            stream.filter(Files::isDirectory)
                    .filter(p -> isDynamicDirName(p.getFileName().toString()))
                    .forEach(matches::add);
        } catch (java.io.IOException exception) {
            System.err.println(
                    "Failed to list routes directory: "
                            + directory
                            + " ("
                            + exception.getMessage()
                            + ")");
            return null;
        } catch (SecurityException exception) {
            System.err.println(
                    "Permission denied listing routes directory: "
                            + directory
                            + " ("
                            + exception.getMessage()
                            + ")");
            return null;
        }

        if (matches.isEmpty()) return null;
        if (matches.size() > 1) {
            throw new IllegalStateException(
                    "Ambiguous dynamic directories under " + directory + ": " + matches);
        }
        return matches.getFirst();
    }

    private static Path findSingleDynamicJavaFile(Path directory) {
        List<Path> matches = new ArrayList<>();

        try (var stream = Files.list(directory)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> isDynamicFileName(p.getFileName().toString()))
                    .forEach(matches::add);
        } catch (java.io.IOException exception) {
            System.err.println(
                    "Failed to list routes directory: "
                            + directory
                            + " ("
                            + exception.getMessage()
                            + ")");
            return null;
        } catch (SecurityException exception) {
            System.err.println(
                    "Permission denied listing routes directory: "
                            + directory
                            + " ("
                            + exception.getMessage()
                            + ")");
            return null;
        }

        if (matches.isEmpty()) return null;
        if (matches.size() > 1) {
            throw new IllegalStateException(
                    "Ambiguous dynamic files under " + directory + ": " + matches);
        }
        return matches.getFirst();
    }

    private static boolean isDynamicDirName(String name) {
        return name.startsWith("[") && name.endsWith("]") && name.length() > 2;
    }

    private static boolean isDynamicFileName(String name) {
        return name.startsWith("[")
                && name.endsWith("].java")
                && name.length() > "[].java".length();
    }

    private static String paramNameFromDir(String dirName) {
        return dirName.substring(1, dirName.length() - 1);
    }

    private static String paramNameFromFile(String fileName) {
        return fileName.substring(1, fileName.length() - "].java".length());
    }
}
