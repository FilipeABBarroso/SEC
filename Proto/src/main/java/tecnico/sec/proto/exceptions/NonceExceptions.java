package tecnico.sec.proto.exceptions;

import io.grpc.Status;

public abstract class NonceExceptions extends BaseException{

    public static class NonceNotFoundException extends NonceExceptions {
        @Override
        public Exception toResponseException() {
            return Status.NOT_FOUND.withDescription("Nonce not found").asException();
        }
    }

    public static class FailInsertNonceException extends NonceExceptions {
        @Override
        public Exception toResponseException() {
            return Status.INTERNAL.withDescription("Fail inserting nonce").asException();
        }
    }

    public static class PublicKeyNotFoundException extends TransactionsExceptions {
        @Override
        public Exception toResponseException() {
            return Status.NOT_FOUND.withDescription("Public key not found in nonce").asException();
        }
    }
}
