package domain.exception;

public class DuplicateBankAccountException extends Exception{

    public DuplicateBankAccountException (String str) {
        super(str);
    }
}
