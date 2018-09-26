package eu.jamiehall.percolate.exception;

public class InvalidQueryException extends IllegalStateException {

    public InvalidQueryException(String s) {
        super(s);
    }

    public InvalidQueryException(String message, Throwable cause) {
        super(message, cause);
    }
}