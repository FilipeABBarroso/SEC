package tecnico.sec.proto.exceptions;

import io.grpc.Status;

public abstract class SignatureExceptions extends BaseException{

    public static class SignatureDoNotMatchException extends SignatureExceptions{
        @Override
        public Exception toResponseException() {
            return Status.FAILED_PRECONDITION.withDescription("Signature Do Not Match Exception").asException();
        }
    }

    public static class CanNotSignException extends SignatureExceptions{
        @Override
        public Exception toResponseException() {
            return Status.FAILED_PRECONDITION.withDescription("Something went wrong while signing Exception").asException();
        }
    }
}