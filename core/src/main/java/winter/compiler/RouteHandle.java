package winter.compiler;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;
import winter.Ctx;

public final class RouteHandle implements AutoCloseable {

    private final Class<?> routeClass;
    private final URLClassLoader classLoader;

    RouteHandle(Class<?> routeClass, URLClassLoader classLoader) {
        this.routeClass = routeClass;
        this.classLoader = classLoader;
    }

    public Set<String> allowedMethods() {
        var allowed = new HashSet<String>();
        for (Method method : routeClass.getDeclaredMethods()) {
            if (method.getParameterCount() != 1) continue;
            if (method.getParameterTypes()[0] != Ctx.class) continue;

            String name = method.getName();
            if (!isVerbName(name)) continue;
            allowed.add(name.toUpperCase());
        }

        allowed.add("OPTIONS");
        if (allowed.contains("GET")) allowed.add("HEAD");
        return Set.copyOf(allowed);
    }

    public Object invoke(String verb, Ctx ctx) throws Exception {
        Method method = routeClass.getDeclaredMethod(verb, Ctx.class);

        Object instance = routeClass.getDeclaredConstructor().newInstance();
        try {
            return method.invoke(instance, ctx);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof Exception asException) throw asException;
            if (cause instanceof Error asError) throw asError;
            throw new RuntimeException(cause);
        }
    }

    private static boolean isVerbName(String name) {
        return switch (name) {
            case "get", "post", "put", "patch", "delete", "head" -> true;
            default -> false;
        };
    }

    @Override
    public void close() throws IOException {
        classLoader.close();
    }
}
