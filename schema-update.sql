-- Database Migration Script for NovaNest Category Schema Extension
-- Run this script against your MySQL database if Hibernate ddl-auto=update is disabled.

ALTER TABLE categories ADD COLUMN category_image VARCHAR(512) NULL;
ALTER TABLE categories ADD COLUMN description TEXT NULL;
ALTER TABLE categories ADD COLUMN banner_image VARCHAR(512) NULL;
ALTER TABLE categories ADD COLUMN display_order INT NULL;
ALTER TABLE categories ADD COLUMN visibility BOOLEAN NULL;

ALTER TABLE users ADD COLUMN address VARCHAR(512) NULL;
ALTER TABLE users ADD COLUMN status VARCHAR(50) NULL;
ALTER TABLE users ADD COLUMN profile_image VARCHAR(512) NULL;

ALTER TABLE products ADD COLUMN barcode VARCHAR(100) NULL;
ALTER TABLE products ADD COLUMN supplier VARCHAR(200) NULL;

