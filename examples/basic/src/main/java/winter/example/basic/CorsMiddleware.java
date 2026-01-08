package winter.example.basic;

import winter.Ctx;
import winter.Res;
import winter.middleware.Middleware;

public final class CorsMiddleware implements Middleware {

    private static final String ALLOW_METHODS = "GET,POST,PUT,PATCH,DELETE,OPTIONS";
    private static final String DEFAULT_ALLOW_HEADERS = "content-type, authorization";

    @Override
    public Object before(Ctx ctx) {
        if (!"OPTIONS".equalsIgnoreCase(ctx.method())) return null;

        return baseCors(Res.of(204, null), ctx)
                .header("Access-Control-Allow-Methods", ALLOW_METHODS)
                .header("Access-Control-Allow-Headers", allowHeaders(ctx))
                .header("Access-Control-Max-Age", "86400");
    }

    @Override
    public Object after(Ctx ctx, Object result) {
        Res base;
        if (result instanceof Res res) base = res;
        else if (result == null) base = Res.of(204, null);
        else base = Res.of(200, result);

        return baseCors(base, ctx);
    }

    private static Res baseCors(Res res, Ctx ctx) {
        // Minimal/default policy for development: allow all origins.
        // If you want stricter behavior, make this check `Origin` and return 403.
        return res.header("Access-Control-Allow-Origin", "*").header("Vary", "Origin");
    }

    private static String allowHeaders(Ctx ctx) {
        String requested = ctx.header("Access-Control-Request-Headers");
        if (requested == null || requested.isBlank()) return DEFAULT_ALLOW_HEADERS;
        return requested;
    }
}
