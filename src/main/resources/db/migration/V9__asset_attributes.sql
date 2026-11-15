-- Free key-value properties of an asset (size, colour …), in the order the user keeps them.
alter table inventory_item add column attributes jsonb not null default '[]';
