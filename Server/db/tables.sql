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
    senderTransactionId integer,
    receiverTransactionId integer,
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
            BEGIN
                SELECT MAX(senderTransactionId) FROM Transactions INTO id_val;
                id_val:=id_val + 1;
            EXCEPTION
                WHEN NO_DATA_FOUND THEN
                    id_val:=1;
            END;
            INSERT INTO Transactions (senderTransactionId, publicKeySender, publicKeyReceiver, amount, status, nonce, signature) VALUES (id_val, $1, $2, $3, $4, $5, $6);
            UPDATE Balance set lastTransactionId = id_val where publicKey=$2;
        RETURN id_val;
        END;'
    LANGUAGE plpgsql;

CREATE FUNCTION update_db(bytea, bytea, int, statusOptions, int, bytea, int, int)
AS'
        DECLARE
            senderTransactionId_val integer;
            receiverTransactionId_val integer;
        BEGIN
            SELECT lastTransactionId FROM Balance WHERE publicKey=$1 INTO senderTransactionId_val;
            SELECT lastTransactionId FROM Balance WHERE publicKey=$2 INTO receiverTransactionId_val;
            IF senderTransactionId_val < $7 THEN
                UPDATE Balance set lastTransactionId = id_val where publicKey=$1;
            END IF;
            IF receiverTransactionId_val < $8 THEN
                UPDATE Balance set lastTransactionId = id_val where publicKey=$2;
            END IF;
            INSERT INTO Transactions (senderTransactionId, publicKeySender, publicKeyReceiver, amount, status, nonce, signature, senderTransactionId, receiverTransactionId) VALUES (id_val, $1, $2, $3, $4, $5, $6, $7, $8);

        END;'
    LANGUAGE plpgsql;
