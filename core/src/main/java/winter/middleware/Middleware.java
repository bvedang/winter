package winter.middleware;

import winter.Ctx;

public interface Middleware {
    default Object before(Ctx ctx) throws Exception {
        return null;
    }

    default Object after(Ctx ctx, Object result) throws Exception {
        return result;
    }

    default Object onError(Ctx ctx, Exception exception) throws Exception {
        throw exception;
    }
}
