-- ============================================================
-- V2 – project_members tablosuna member_type kolonu ekleme
--
-- ProjectMember entity'sine sonradan eklenen MemberType enum
-- alanı için veritabanı şeması güncelleniyor.
-- Mevcut kayıtlar varsayılan değer olarak 'MEMBER' alır.
-- ============================================================

ALTER TABLE project_members
    ADD COLUMN IF NOT EXISTS member_type VARCHAR(50) NOT NULL DEFAULT 'MEMBER';

