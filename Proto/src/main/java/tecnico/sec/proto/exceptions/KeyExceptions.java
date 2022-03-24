package tecnico.sec.proto.exceptions;

import io.grpc.Status;

public abstract class KeyExceptions extends BaseException{
    public static class InvalidPublicKeyException extends KeyExceptions{
        @Override
        public Exception toResponseException() {
            return Status.FAILED_PRECONDITION.withDescription("Invalid Public Key Exception").asException();
        }
    }
}
