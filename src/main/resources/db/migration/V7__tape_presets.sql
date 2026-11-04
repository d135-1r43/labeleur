-- Cut lengths and text directions a user keeps for continuous tape, offered on every label type.
create table tape_preset (
    user_id    varchar(64) not null,
    id         varchar(20) not null,
    length_mm  integer not null,
    read_along boolean not null,
    primary key (user_id, id)
);
