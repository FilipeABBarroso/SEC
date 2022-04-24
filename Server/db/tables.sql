CREATE TYPE statusOptions AS ENUM(
    'Pending',
    'Completed'
);

CREATE TABLE IF NOT EXISTS Balance(
    publicKey bytea PRIMARY KEY,
    balance integer,
    lastTransactionId integer
);

CREATE TABLE IF NOT EXISTS Nonce(
    publicKey bytea PRIMARY KEY,
    nonce bigint,
    zeros int,
    FOREIGN KEY (publicKey)
        REFERENCES Balance (publicKey)
);

CREATE TABLE IF NOT EXISTS Transactions(
    publicKeySender bytea,
    publicKeyReceiver bytea,
    amount integer,
    status statusOptions NOT NULL,
    nonce bigint,
    signature bytea,
    id serial PRIMARY KEY,
    senderTransactionId integer,
    receiverTransactionId integer,
    FOREIGN KEY (publicKeySender)
        REFERENCES Balance (publicKey),
    FOREIGN KEY (publicKeyReceiver)
        REFERENCES Balance (publicKey)
);
