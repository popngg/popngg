package gg.popn.http.renewal;
import org.springframework.http.HttpStatus;
public class RenewalException extends RuntimeException {
    private final HttpStatus status; private final String code;
    public RenewalException(HttpStatus status, String code, String message) { super(message); this.status=status; this.code=code; }
    public HttpStatus status(){ return status; } public String code(){ return code; }
}
