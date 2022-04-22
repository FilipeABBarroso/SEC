CREATE TYPE statusOptions AS ENUM(
    'Pending',
    'Completed'
);

CREATE TABLE IF NOT EXISTS Balance(
    publicKey bytea PRIMARY KEY,
    balance integer,
    counter integer
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

CREATE FUNCTION add_transaction(bytea, bytea, int, statusOptions, int, bytea)
    RETURNS INTEGER
    AS'
        DECLARE
            id_val integer;
        BEGIN
            SELECT nextval(''transactions_id_seq'') INTO id_val;
            INSERT INTO Transactions (id, publicKeySender, publicKeyReceiver, amount, status, nonce, signature) VALUES (id_val, $1, $2, $3, $4, $5, $6);
            UPDATE Balance set counter = id_val where publicKey=$2;
        RETURN id_val;
        END;'
    LANGUAGE plpgsql;
