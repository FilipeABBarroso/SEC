package tecnico.sec.proto.exceptions;

public abstract class BaseException extends Exception{
    public abstract Exception toResponseException();
}