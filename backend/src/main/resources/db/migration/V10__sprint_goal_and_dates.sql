-- V10: Sprint tablosuna goal alanı ekle, tarih sütunlarını DATE tipine çevir
-- Aynı anda birden fazla sprint aktif olabilir (kısıtlama yok)

ALTER TABLE sprints ADD COLUMN IF NOT EXISTS goal TEXT;

-- start_date ve end_date sütunlarını DATE tipine dönüştür (mevcut string veriler varsa uyumlu olacak şekilde)
ALTER TABLE sprints ALTER COLUMN start_date TYPE DATE USING CASE WHEN start_date IS NOT NULL AND start_date != '' THEN start_date::DATE ELSE NULL END;
ALTER TABLE sprints ALTER COLUMN end_date   TYPE DATE USING CASE WHEN end_date   IS NOT NULL AND end_date   != '' THEN end_date::DATE   ELSE NULL END;

