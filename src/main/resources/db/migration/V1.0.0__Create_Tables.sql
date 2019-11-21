CREATE TABLE product (
  id BIGSERIAL NOT NULL PRIMARY KEY ,
  sku VARCHAR(255) NOT NULL UNIQUE,
  attributes JSONB
 );

CREATE INDEX product_attributes_idx ON product USING GIN (attributes jsonb_ops);