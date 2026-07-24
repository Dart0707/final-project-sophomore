package myexceptions;

public class NullValueException extends Exception {

    public NullValueException() {
        super();
    }

    public NullValueException(String message) {
        super(message);
    }
}
