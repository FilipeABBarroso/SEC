package tecnico.sec.proto.exceptions;

import io.grpc.Status;

public abstract class DataBaseExceptions extends BaseException{
    public static class GeneralDatabaseError extends BalanceExceptions {
        @Override
        public Exception toResponseException() {return Status.INTERNAL.asException();}
    }
}
