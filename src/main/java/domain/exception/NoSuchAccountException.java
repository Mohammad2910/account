package domain.exception;

public class NoSuchAccountException extends Exception{
    public NoSuchAccountException(String str){
        super(str);
    }
}
