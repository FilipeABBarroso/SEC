CREATE TYPE statusOptions AS ENUM(
    'Pending',
    'Completed'
);

CREATE TABLE IF NOT EXISTS Balance(
    publicKey bytea PRIMARY KEY,
    balance integer
);

CREATE TABLE IF NOT EXISTS Transactions(
    publicSender bytea,
    publicReceiver bytea,
    amount integer,
    status statusOptions NOT NULL,
    id serial PRIMARY KEY,
    nonce integer,
    FOREIGN KEY (publicSender)
        REFERENCES Balance (publicKey),
    FOREIGN KEY (publicReceiver)
        REFERENCES Balance (publicKey)
);