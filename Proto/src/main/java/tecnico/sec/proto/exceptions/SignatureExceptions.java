package tecnico.sec.proto.exceptions;

import io.grpc.Status;

public abstract class SignatureExceptions extends BaseException{

    public static class SignatureDoNotMatchException extends SignatureExceptions{
        @Override
        public Exception toResponseException() {
            return Status.PERMISSION_DENIED.withDescription("Signature Do Not Match").asException();
        }
    }

    public static class CanNotSignException extends SignatureExceptions{
        @Override
        public Exception toResponseException() {
            return Status.ABORTED.withDescription("Something went wrong while signing").asException();
        }
    }
}