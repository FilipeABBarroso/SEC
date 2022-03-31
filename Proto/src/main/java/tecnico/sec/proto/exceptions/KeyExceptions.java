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

    public static class KeyStoreException extends KeyExceptions{
        @Override
        public Exception toResponseException() {
            return Status.INTERNAL.asException();
        }
    }

    public static class PasswordMismatchException extends KeyExceptions{
        @Override
        public Exception toResponseException() {
            return Status.PERMISSION_DENIED.asException();
        }
    }

    public static class GeneralKeyStoreErrorException extends KeyExceptions{
        @Override
        public Exception toResponseException() {
            return Status.INTERNAL.asException();
        }
    }
}
