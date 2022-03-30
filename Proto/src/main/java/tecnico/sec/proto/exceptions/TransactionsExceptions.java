package tecnico.sec.proto.exceptions;

import io.grpc.Status;

public abstract class TransactionsExceptions extends BaseException {

    public static class SenderPublicKeyNotFoundException extends TransactionsExceptions {
        @Override
        public Exception toResponseException() {
            return Status.NOT_FOUND.withDescription("Sender public key not found").asException();
        }
    }

    public static class ReceiverPublicKeyNotFoundException extends TransactionsExceptions {
        @Override
        public Exception toResponseException() {
            return Status.NOT_FOUND.withDescription("Receiver public key not found").asException();
        }
    }

    public static class FailInsertTransactionException extends TransactionsExceptions {
        @Override
        public Exception toResponseException() {return Status.NOT_FOUND.withDescription("Fail inserting transaction").asException();}
    }

    public static class TransactionIDNotFoundException extends TransactionsExceptions {
        @Override
        public Exception toResponseException() {
            return Status.NOT_FOUND.withDescription("Transaction id not found").asException();
        }
    }

    public static class TransactionPublicKeyReceiverDontMatchException extends TransactionsExceptions {
        @Override
        public Exception toResponseException() {
            return Status.INVALID_ARGUMENT.withDescription("Receiver public key don't match").asException();
        }
    }
}