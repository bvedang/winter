package winter.router;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class FileRouterTest {

    @Test
    void rootIndexResolves(@TempDir Path tempDir) throws Exception {
        Path routes = tempDir.resolve("routes");
        Files.createDirectories(routes);
        Files.writeString(routes.resolve("index.java"), "");

        var router = new FileRouter(routes);
        RouteMatch match = router.match("/");

        assertNotNull(match);
        assertEquals(routes.resolve("index.java").normalize(), match.file());
        assertEquals(Map.of(), match.params());
    }

    @Test
    void staticDirIndexResolves(@TempDir Path tempDir) throws Exception {
        Path routes = tempDir.resolve("routes");
        Files.createDirectories(routes.resolve("users"));
        Files.writeString(routes.resolve("users/index.java"), "");

        var router = new FileRouter(routes);
        RouteMatch match = router.match("/users");

        assertNotNull(match);
        assertEquals(
            routes.resolve("users/index.java").normalize(),
            match.file()
        );
        assertEquals(Map.of(), match.params());
    }

    @Test
    void exactLeafFileResolves(@TempDir Path tempDir) throws Exception {
        Path routes = tempDir.resolve("routes");
        Files.createDirectories(routes);
        Files.writeString(routes.resolve("teapot.java"), "");

        var router = new FileRouter(routes);
        RouteMatch match = router.match("/teapot");

        assertNotNull(match);
        assertEquals(routes.resolve("teapot.java").normalize(), match.file());
        assertEquals(Map.of(), match.params());
    }

    @Test
    void nestedDynamicDirResolves(@TempDir Path tempDir) throws Exception {
        Path routes = tempDir.resolve("routes");
        Files.createDirectories(routes.resolve("users/[id]/posts"));
        Files.writeString(routes.resolve("users/[id]/posts/index.java"), "");

        var router = new FileRouter(routes);
        RouteMatch match = router.match("/users/123/posts");

        assertNotNull(match);
        assertEquals(
            routes.resolve("users/[id]/posts/index.java").normalize(),
            match.file()
        );
        assertEquals(Map.of("id", "123"), match.params());
    }

    @Test
    void nestedDynamicDirThenDynamicFileResolves(@TempDir Path tempDir)
        throws Exception {
        Path routes = tempDir.resolve("routes");
        Files.createDirectories(routes.resolve("[category]"));
        Files.writeString(routes.resolve("[category]/[id].java"), "");

        var router = new FileRouter(routes);
        RouteMatch match = router.match("/books/123");

        assertNotNull(match);
        assertEquals(
            routes.resolve("[category]/[id].java").normalize(),
            match.file()
        );
        assertEquals(Map.of("category", "books", "id", "123"), match.params());
    }

    @Test
    void staticBeatsDynamicAtSameLevel(@TempDir Path tempDir) throws Exception {
        Path routes = tempDir.resolve("routes");
        Files.createDirectories(routes.resolve("users"));
        Files.createDirectories(routes.resolve("[category]"));
        Files.writeString(routes.resolve("users/index.java"), "");
        Files.writeString(routes.resolve("[category]/index.java"), "");

        var router = new FileRouter(routes);
        RouteMatch match = router.match("/users");

        assertNotNull(match);
        assertEquals(
            routes.resolve("users/index.java").normalize(),
            match.file()
        );
        assertEquals(Map.of(), match.params());
    }

    @Test
    void dynamicDirBeatsDynamicFile(@TempDir Path tempDir) throws Exception {
        Path routes = tempDir.resolve("routes");
        Files.createDirectories(routes.resolve("[x]"));
        Files.writeString(routes.resolve("[x]/index.java"), "");
        Files.writeString(routes.resolve("[id].java"), "");

        var router = new FileRouter(routes);
        RouteMatch match = router.match("/foo");

        assertNotNull(match);
        assertEquals(
            routes.resolve("[x]/index.java").normalize(),
            match.file()
        );
        assertEquals(Map.of("x", "foo"), match.params());
    }

    @Test
    void ambiguityMultipleDynamicDirsThrows(@TempDir Path tempDir)
        throws Exception {
        Path routes = tempDir.resolve("routes");
        Files.createDirectories(routes.resolve("[id]"));
        Files.createDirectories(routes.resolve("[slug]"));
        Files.writeString(routes.resolve("[id]/index.java"), "");
        Files.writeString(routes.resolve("[slug]/index.java"), "");

        var router = new FileRouter(routes);
        assertThrows(IllegalStateException.class, () ->
            router.match("/anything")
        );
    }

    @Test
    void ambiguityMultipleDynamicFilesThrows(@TempDir Path tempDir)
        throws Exception {
        Path routes = tempDir.resolve("routes");
        Files.createDirectories(routes);
        Files.writeString(routes.resolve("[id].java"), "");
        Files.writeString(routes.resolve("[slug].java"), "");

        var router = new FileRouter(routes);
        assertThrows(IllegalStateException.class, () ->
            router.match("/anything")
        );
    }

    @Test
    void duplicateParamNameThrows(@TempDir Path tempDir) throws Exception {
        Path routes = tempDir.resolve("routes");
        Files.createDirectories(routes.resolve("[id]"));
        Files.writeString(routes.resolve("[id]/[id].java"), "");

        var router = new FileRouter(routes);
        assertThrows(IllegalStateException.class, () -> router.match("/a/b"));
    }

    @Test
    void traversalSegmentsReturnNull(@TempDir Path tempDir) throws Exception {
        Path routes = tempDir.resolve("routes");
        Files.createDirectories(routes);
        Files.writeString(routes.resolve("index.java"), "");

        var router = new FileRouter(routes);
        assertNull(router.match("/../secrets"));
        assertNull(router.match("/a/../b"));
        assertNull(router.match("/./secrets"));
    }

    @Test
    void normalizesTrailingSlashAndQueryString(@TempDir Path tempDir)
        throws Exception {
        Path routes = tempDir.resolve("routes");
        Files.createDirectories(routes.resolve("users"));
        Files.writeString(routes.resolve("users/index.java"), "");

        var router = new FileRouter(routes);
        assertNotNull(router.match("/users/"));
        assertNotNull(router.match("/users?x=y"));
        assertNotNull(router.match("/users/?x=y"));
    }

    @Test
    void unsafeBackslashSegmentReturnsNull(@TempDir Path tempDir)
        throws Exception {
        Path routes = tempDir.resolve("routes");
        Files.createDirectories(routes);
        Files.writeString(routes.resolve("index.java"), "");

        var router = new FileRouter(routes);
        assertNull(router.match("/a\\b"));
    }

    @Test
    void staticDirWithoutIndexDoesNotMatch(@TempDir Path tempDir)
        throws Exception {
        Path routes = tempDir.resolve("routes");
        Files.createDirectories(routes.resolve("users"));

        var router = new FileRouter(routes);
        assertNull(router.match("/users"));
    }

    @Test
    void exactFileOnlyMatchesLeaf(@TempDir Path tempDir) throws Exception {
        Path routes = tempDir.resolve("routes");
        Files.createDirectories(routes);
        Files.writeString(routes.resolve("users.java"), "");

        var router = new FileRouter(routes);
        assertNull(router.match("/users/123"));
    }

    @Test
    void dynamicFileOnlyMatchesLeaf(@TempDir Path tempDir) throws Exception {
        Path routes = tempDir.resolve("routes");
        Files.createDirectories(routes);
        Files.writeString(routes.resolve("[id].java"), "");

        var router = new FileRouter(routes);
        assertNull(router.match("/a/b"));
    }

    @Test
    void dynamicDirRequiresIndexForLeaf(@TempDir Path tempDir)
        throws Exception {
        Path routes = tempDir.resolve("routes");
        Files.createDirectories(routes.resolve("[id]"));
        // no [id]/index.java

        var router = new FileRouter(routes);
        assertNull(router.match("/anything"));
    }
}
