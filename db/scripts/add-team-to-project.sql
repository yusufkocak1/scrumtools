-- =============================================================================
-- Bir takımın tüm üyelerini bir projeye toplu ekler (idempotent).
--
-- Kullanım (sunucuda, repo kökünden):
--   docker exec -i scrumtools-postgres psql -U scrumtools -d scrumtools \
--     -v ON_ERROR_STOP=1 \
--     -v project_id='f6dfdfa1-62ed-402d-88ea-aefb54c076f3' \
--     -v team_name='development' \
--     -v admin_email='yusufkocak.326@gmail.com' \
--     < db/scripts/add-team-to-project.sql
--
-- Parametreler:
--   project_id  : hedef projenin UUID'si
--   team_name   : takım adı (büyük/küçük harf duyarsız, kısmi eşleşme)
--   admin_email : işlemi yapan kullanıcı; projede üye değilse Project Admin
--                 rolüyle eklenir (403 sorununu da bu düzeltir)
-- =============================================================================

BEGIN;
SET LOCAL search_path TO scrumtools, public;

SET LOCAL app.project_id  = :'project_id';
SET LOCAL app.team_name   = :'team_name';
SET LOCAL app.admin_email = :'admin_email';

DO $$
DECLARE
    v_project_id   uuid   := current_setting('app.project_id')::uuid;
    v_team_name    text   := current_setting('app.team_name');
    v_admin_email  text   := current_setting('app.admin_email');
    v_project      record;
    v_team         record;
    v_admin_id     uuid;
    v_admin_pm_id  uuid;
    v_admin_role   uuid;
    v_dev_role     uuid;
    v_pm_id        uuid;
    v_added        int := 0;
    v_skipped      int := 0;
    tm             record;
    v_user_id      uuid;
BEGIN
    -- ── Doğrulamalar ─────────────────────────────────────────────────────────
    SELECT id, name, organization_id INTO v_project
    FROM projects WHERE id = v_project_id;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Proje bulunamadı: %', v_project_id;
    END IF;

    SELECT id, team_name, organization_id INTO v_team
    FROM teams WHERE team_name ILIKE '%' || v_team_name || '%';
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Takım bulunamadı: %', v_team_name;
    END IF;
    IF (SELECT count(*) FROM teams WHERE team_name ILIKE '%' || v_team_name || '%') > 1 THEN
        RAISE EXCEPTION '"%" ile eşleşen birden fazla takım var, tam ad verin.', v_team_name;
    END IF;
    IF v_team.organization_id IS DISTINCT FROM v_project.organization_id THEN
        RAISE EXCEPTION 'Takım (%) ile proje (%) farklı organizasyonlarda.',
            v_team.team_name, v_project.name;
    END IF;

    SELECT id INTO v_admin_id FROM users WHERE lower(email) = lower(v_admin_email);
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Kullanıcı bulunamadı: %', v_admin_email;
    END IF;

    -- Seed'lenen varsayılan roller (RoleService.seedDefaultRoles: scope=PROJECT)
    SELECT id INTO v_admin_role FROM roles
    WHERE name = 'Project Admin' AND scope = 'PROJECT' AND is_default = true;
    SELECT id INTO v_dev_role FROM roles
    WHERE name = 'Developer' AND scope = 'PROJECT' AND is_default = true;

    -- ── 1) Operatörü projeye Project Admin olarak ekle / rolünü tamamla ─────
    SELECT id INTO v_admin_pm_id FROM project_members
    WHERE project_id = v_project_id AND user_id = v_admin_id;

    IF v_admin_pm_id IS NULL THEN
        INSERT INTO project_members (id, project_id, user_id, added_by_id, member_type, joined_at)
        VALUES (gen_random_uuid(), v_project_id, v_admin_id, v_admin_id, 'MEMBER', now())
        RETURNING id INTO v_admin_pm_id;
        RAISE NOTICE '% projeye üye olarak eklendi.', v_admin_email;
    END IF;

    IF v_admin_role IS NOT NULL THEN
        INSERT INTO project_member_roles (project_member_id, role_id)
        SELECT v_admin_pm_id, v_admin_role
        WHERE NOT EXISTS (
            SELECT 1 FROM project_member_roles
            WHERE project_member_id = v_admin_pm_id AND role_id = v_admin_role);
    END IF;

    -- ── 2) Takımı projeye bağla (release yönetimi teams.project_id ister) ───
    IF EXISTS (SELECT 1 FROM teams
               WHERE id = v_team.id AND project_id IS DISTINCT FROM v_project_id) THEN
        UPDATE teams SET project_id = v_project_id WHERE id = v_team.id;
        RAISE NOTICE 'Takım "%" projeye bağlandı: %', v_team.team_name, v_project.name;
    END IF;

    -- ── 3) Takım üyelerini projeye ekle ──────────────────────────────────────
    FOR tm IN
        SELECT email, display_name FROM team_members WHERE team_id = v_team.id
    LOOP
        SELECT id INTO v_user_id FROM users WHERE lower(email) = lower(tm.email);
        IF v_user_id IS NULL THEN
            v_skipped := v_skipped + 1;
            RAISE NOTICE 'ATLANDI (kayıtlı kullanıcı değil): % <%>', tm.display_name, tm.email;
            CONTINUE;
        END IF;

        IF EXISTS (SELECT 1 FROM project_members
                   WHERE project_id = v_project_id AND user_id = v_user_id) THEN
            CONTINUE;
        END IF;

        INSERT INTO project_members (id, project_id, user_id, added_by_id, member_type, joined_at)
        VALUES (gen_random_uuid(), v_project_id, v_user_id, v_admin_id, 'MEMBER', now())
        RETURNING id INTO v_pm_id;

        IF v_dev_role IS NOT NULL THEN
            INSERT INTO project_member_roles (project_member_id, role_id)
            VALUES (v_pm_id, v_dev_role);
        END IF;

        v_added := v_added + 1;
        RAISE NOTICE 'EKLENDİ: % <%>', tm.display_name, tm.email;
    END LOOP;

    RAISE NOTICE '--- Özet: % eklendi, % atlandı (takım: %, proje: %) ---',
        v_added, v_skipped, v_team.team_name, v_project.name;
END $$;

-- Son durum raporu
SELECT u.email, u.name,
       string_agg(r.name, ', ' ORDER BY r.name) AS roles,
       pm.member_type, pm.joined_at
FROM project_members pm
JOIN users u ON u.id = pm.user_id
LEFT JOIN project_member_roles pmr ON pmr.project_member_id = pm.id
LEFT JOIN roles r ON r.id = pmr.role_id
WHERE pm.project_id = :'project_id'::uuid
GROUP BY u.email, u.name, pm.member_type, pm.joined_at
ORDER BY pm.joined_at;

COMMIT;
