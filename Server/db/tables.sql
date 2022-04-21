CREATE TYPE statusOptions AS ENUM(
    'Pending',
    'Completed'
);

CREATE TABLE IF NOT EXISTS Balance(
    publicKey bytea PRIMARY KEY,
    balance integer,
    counter integer,
    FOREIGN KEY (counter)
        REFERENCES Transactions (id)
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

CREATE FUNCTION addTransaction(bytea, bytea, int, statusOptions, int, bytea)
    RETURNS INTEGER
AS '
    DECLARE
      publicKeySender ALIAS FOR $1;
      publicKeyReceiver ALIAS FOR $2;
      amount ALIAS FOR $2;
      status ALIAS FOR $2;
      nonce ALIAS FOR $2;
      signature ALIAS FOR $2;
      id INTEGER;
    BEGIN
       select nextval(''id_seq'') into id;
      INSERT INTO Transactions (id, publicKeySender, publicKeyReceiver, amount, status, nonce, signature) VALUES (id, publicKeySender, publicKeyReceiver, amount, status, nonce, signature);
    RETURN id;
    END;
 '
> LANGUAGE 'plpgsql';