-- Proje üyelerinin birden fazla role sahip olabilmesi için join tablosu
CREATE TABLE project_member_roles (
    project_member_id UUID NOT NULL REFERENCES project_members(id) ON DELETE CASCADE,
    role_id           UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (project_member_id, role_id)
);

-- Mevcut tek-rol verisini yeni tabloya taşı
INSERT INTO project_member_roles (project_member_id, role_id)
SELECT id, role_id FROM project_members WHERE role_id IS NOT NULL;

-- Eski tek-rol sütununu kaldır
ALTER TABLE project_members DROP COLUMN role_id;

