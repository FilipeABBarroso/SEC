package tecnico.sec.proto.exceptions;

import io.grpc.Status;

public abstract class KeyExceptions extends BaseException{
    public static class InvalidPublicKeyException extends KeyExceptions{
        @Override
        public Exception toResponseException() {
            return Status.ABORTED.withDescription("Invalid Public Key").asException();
        }
    }
    public static class NoSuchAlgorithmException extends KeyExceptions{
        @Override
        public Exception toResponseException() {
            return Status.INTERNAL.asException();
        }
    }
}
