package tecnico.sec.proto.exceptions;

import io.grpc.Status;

public abstract class IOExceptions extends BaseException{

    public static class IOException extends IOExceptions {
        @Override
        public Exception toResponseException() {
            return Status.FAILED_PRECONDITION.withDescription("Invalid Public Key Exception").asException();
        }
    }
}
