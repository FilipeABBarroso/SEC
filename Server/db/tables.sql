CREATE TYPE statusOptions AS ENUM(
    'Pending',
    'Completed'
);

CREATE TABLE IF NOT EXISTS Balance(
    publicKey bytea PRIMARY KEY,
    balance integer
);

CREATE TABLE IF NOT EXISTS Nonce(
    publicKey bytea PRIMARY KEY,
    nonce integer,
    FOREIGN KEY (publicKey)
        REFERENCES Balance (publicKey)
);

CREATE TABLE IF NOT EXISTS Transactions(
    publicKeySender bytea,
    publicKeyReceiver bytea,
    amount integer,
    status statusOptions NOT NULL,
    nonce integer,
    signature bytea,
    id serial PRIMARY KEY,
    FOREIGN KEY (publicKeySender)
        REFERENCES Balance (publicKey),
    FOREIGN KEY (publicKeyReceiver)
        REFERENCES Balance (publicKey)
);