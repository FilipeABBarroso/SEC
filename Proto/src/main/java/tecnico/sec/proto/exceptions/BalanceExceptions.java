package tecnico.sec.proto.exceptions;

import io.grpc.Status;

public abstract class BalanceExceptions extends BaseException{

    public static class PublicKeyNotFoundException extends BalanceExceptions {
        @Override
        public Exception toResponseException() {return Status.NOT_FOUND.withDescription("Public Key not found").asException();}
    }

    public static class PublicKeyAlreadyExistException extends BalanceExceptions {
        @Override
        public Exception toResponseException() {return Status.ALREADY_EXISTS.withDescription("Public Key already exists").asException();}
    }
}
