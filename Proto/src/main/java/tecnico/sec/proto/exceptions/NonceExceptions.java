package tecnico.sec.proto.exceptions;

import io.grpc.Status;

public abstract class NonceExceptions extends BaseException{

    public static class NonceNotFoundException extends NonceExceptions {
        @Override
        public Exception toResponseException() {
            return Status.NOT_FOUND.withDescription("Nonce not found").asException();
        }
    }
}
