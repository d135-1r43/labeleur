-- Who works on whose data. Every record, setting and print job stays keyed by
-- user_id; that key is now the account's, not necessarily the person's.
-- A user without a membership works on their own data (key = their subject),
-- as before. Inviting someone turns the account into an organisation: the
-- inviter gets a row as its founder (owner_id = their own subject), everyone
-- who accepts gets one pointing at the same owner_id, and all of them read and
-- write the rows keyed by it.
create table membership (
    user_id   varchar(64) primary key,
    owner_id  varchar(64) not null,
    email     varchar(320),
    name      varchar(300),
    joined_at timestamptz not null
);
create index membership_owner on membership (owner_id);

-- An open invitation to an organisation. The token is the secret in the link;
-- accepting also needs the invitee to be signed in with the invited email.
create table invitation (
    token      varchar(64) primary key,
    owner_id   varchar(64) not null,
    email      varchar(320) not null,
    invited_by varchar(300) not null,
    created_at timestamptz not null,
    expires_at timestamptz not null,
    unique (owner_id, email)
);
