package winter;

public final class WinterException extends RuntimeException {

    WinterException(String message) {
        super(message);
    }

    WinterException(String message, Throwable cause) {
        super(message, cause);
    }
}
