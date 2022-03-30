package tecnico.sec.proto.exceptions;

import io.grpc.Status;

public abstract class TransactionsExceptions extends BaseException {

    public static class SenderPublicKeyNotFoundException extends TransactionsExceptions {
        @Override
        public Exception toResponseException() {
            return Status.NOT_FOUND.asException();
        }
    }

    public static class ReceiverPublicKeyNotFoundException extends TransactionsExceptions {
        @Override
        public Exception toResponseException() {
            return Status.NOT_FOUND.asException();
        }
    }

    public static class FailInsertTransactionException extends TransactionsExceptions {
        @Override
        public Exception toResponseException() {return Status.NOT_FOUND.asException();}
    }

    public static class TransactionIDNotFoundException extends TransactionsExceptions {
        @Override
        public Exception toResponseException() {
            return Status.NOT_FOUND.asException();
        }
    }
}
