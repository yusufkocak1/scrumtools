-- ============================================================================
-- V7: Docs Module — Space, Page, Version, Permission, Attachment
-- ============================================================================

-- 1. Doc Spaces
CREATE TABLE doc_spaces (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_doc_spaces_project ON doc_spaces(project_id);

-- 2. Doc Pages
CREATE TABLE doc_pages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    space_id UUID NOT NULL REFERENCES doc_spaces(id) ON DELETE CASCADE,
    parent_page_id UUID REFERENCES doc_pages(id) ON DELETE SET NULL,
    title VARCHAR(500) NOT NULL,
    slug VARCHAR(500) NOT NULL,
    content TEXT DEFAULT '',
    sort_order INT NOT NULL DEFAULT 0,
    created_by UUID NOT NULL REFERENCES users(id),
    updated_by UUID REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_doc_pages_space ON doc_pages(space_id);
CREATE INDEX idx_doc_pages_parent ON doc_pages(parent_page_id);
CREATE UNIQUE INDEX idx_doc_pages_slug_space ON doc_pages(space_id, slug);

-- 3. Doc Page Versions (snapshot on each save)
CREATE TABLE doc_page_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    page_id UUID NOT NULL REFERENCES doc_pages(id) ON DELETE CASCADE,
    version_number INT NOT NULL,
    title VARCHAR(500) NOT NULL,
    content TEXT NOT NULL,
    change_summary VARCHAR(500),
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_doc_page_versions_page ON doc_page_versions(page_id);
CREATE UNIQUE INDEX idx_doc_page_versions_unique ON doc_page_versions(page_id, version_number);

-- 4. Doc Permissions (space-level or page-level)
CREATE TABLE doc_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    space_id UUID REFERENCES doc_spaces(id) ON DELETE CASCADE,
    page_id UUID REFERENCES doc_pages(id) ON DELETE CASCADE,
    access_level VARCHAR(20) NOT NULL, -- READ, WRITE, ADMIN
    target_type VARCHAR(30) NOT NULL,  -- USER, TEAM, ORGANIZATION, PROJECT_MEMBERS
    target_id UUID NOT NULL,
    granted_by UUID NOT NULL REFERENCES users(id),
    can_delegate BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT chk_doc_perm_target CHECK (space_id IS NOT NULL OR page_id IS NOT NULL)
);

CREATE INDEX idx_doc_permissions_space ON doc_permissions(space_id);
CREATE INDEX idx_doc_permissions_page ON doc_permissions(page_id);
CREATE INDEX idx_doc_permissions_target ON doc_permissions(target_type, target_id);

-- 5. Doc Page Attachments (MinIO)
CREATE TABLE doc_page_attachments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    page_id UUID NOT NULL REFERENCES doc_pages(id) ON DELETE CASCADE,
    file_name VARCHAR(500) NOT NULL,
    object_key VARCHAR(1000) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(255),
    uploaded_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_doc_page_attachments_page ON doc_page_attachments(page_id);

