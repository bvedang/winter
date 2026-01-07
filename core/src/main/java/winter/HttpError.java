package winter;

public final class HttpError extends RuntimeException {

    private final int status;
    private final Object body;

    public HttpError(int status, Object body) {
        super(String.valueOf(body));
        this.status = status;
        this.body = body;
    }

    public int status() {
        return status;
    }

    public Object body() {
        return body;
    }
}
