package domain;

public class NoSuchBankAccountException extends Exception{
    public NoSuchBankAccountException(String str){
        super(str);
    }
}
