-- ============================================================================
-- UNIVERSAL TRACKER - DATABASE SCHEMA
-- Version: 1.0.0
-- Description: Metadata-driven personal tracking platform
-- Pattern: EAV (Entity-Attribute-Value) with typed value columns
-- ============================================================================

-- Enable UUID generation
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================================
-- ENUMS
-- ============================================================================

CREATE TYPE field_type AS ENUM (
    'text',
    'number',
    'currency',
    'date',
    'datetime',
    'time',
    'dropdown',
    'multi_select',
    'checkbox',
    'rating',
    'progress',
    'formula',
    'url',
    'email',
    'phone',
    'image',
    'color',
    'duration'
);

CREATE TYPE validation_rule_type AS ENUM (
    'required',
    'min',
    'max',
    'min_length',
    'max_length',
    'regex',
    'unique',
    'custom'
);

CREATE TYPE view_type AS ENUM (
    'table',
    'calendar',
    'chart',
    'kanban',
    'summary',
    'timeline'
);

CREATE TYPE automation_trigger AS ENUM (
    'on_entry_created',
    'on_entry_updated',
    'on_entry_deleted',
    'on_field_value_changed',
    'on_schedule',
    'on_threshold_reached'
);

CREATE TYPE automation_action AS ENUM (
    'set_field_value',
    'create_entry',
    'send_notification',
    'update_status',
    'run_formula',
    'webhook'
);

CREATE TYPE entry_status AS ENUM (
    'active',
    'archived',
    'flagged',
    'draft'
);


-- ============================================================================
-- CORE TABLES
-- ============================================================================

CREATE TABLE domain (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL,
    name            VARCHAR(100) NOT NULL,
    slug            VARCHAR(100) NOT NULL,
    icon            VARCHAR(50),
    color           VARCHAR(7),
    description     TEXT,
    sort_order      INT DEFAULT 0,
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW(),

    CONSTRAINT uq_domain_slug_per_user UNIQUE (user_id, slug)
);

CREATE INDEX idx_domain_user ON domain(user_id);
CREATE INDEX idx_domain_active ON domain(user_id, is_active);


CREATE TABLE tracker (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    domain_id               UUID NOT NULL REFERENCES domain(id) ON DELETE CASCADE,
    user_id                 UUID NOT NULL,
    name                    VARCHAR(100) NOT NULL,
    slug                    VARCHAR(100) NOT NULL,
    icon                    VARCHAR(50),
    description             TEXT,
    entry_name_singular     VARCHAR(50) DEFAULT 'Entry',
    entry_name_plural       VARCHAR(50) DEFAULT 'Entries',
    default_date_field      VARCHAR(100),
    summary_config          JSONB DEFAULT '{}',
    sort_order              INT DEFAULT 0,
    is_active               BOOLEAN DEFAULT TRUE,
    created_at              TIMESTAMPTZ DEFAULT NOW(),
    updated_at              TIMESTAMPTZ DEFAULT NOW(),

    CONSTRAINT uq_tracker_slug_per_domain UNIQUE (domain_id, slug)
);

CREATE INDEX idx_tracker_domain ON tracker(domain_id);
CREATE INDEX idx_tracker_user ON tracker(user_id);
CREATE INDEX idx_tracker_active ON tracker(domain_id, is_active);


CREATE TABLE field_definition (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tracker_id          UUID NOT NULL REFERENCES tracker(id) ON DELETE CASCADE,
    name                VARCHAR(100) NOT NULL,
    slug                VARCHAR(100) NOT NULL,
    field_type          field_type NOT NULL,
    is_required         BOOLEAN DEFAULT FALSE,
    is_unique           BOOLEAN DEFAULT FALSE,
    is_filterable       BOOLEAN DEFAULT TRUE,
    is_summable         BOOLEAN DEFAULT FALSE,
    is_primary_display  BOOLEAN DEFAULT FALSE,
    default_value       JSONB,
    display_format      VARCHAR(100),
    placeholder         VARCHAR(200),
    help_text           TEXT,
    currency_code       VARCHAR(3),
    min_value           DECIMAL(15,4),
    max_value           DECIMAL(15,4),
    sort_order          INT DEFAULT 0,
    is_active           BOOLEAN DEFAULT TRUE,
    conditional_logic   JSONB,
    formula_expression  TEXT,
    created_at          TIMESTAMPTZ DEFAULT NOW(),
    updated_at          TIMESTAMPTZ DEFAULT NOW(),

    CONSTRAINT uq_field_slug_per_tracker UNIQUE (tracker_id, slug)
);

CREATE INDEX idx_field_def_tracker ON field_definition(tracker_id);
CREATE INDEX idx_field_def_active ON field_definition(tracker_id, is_active, sort_order);
CREATE INDEX idx_field_def_type ON field_definition(tracker_id, field_type);


CREATE TABLE field_option (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    field_definition_id     UUID NOT NULL REFERENCES field_definition(id) ON DELETE CASCADE,
    label                   VARCHAR(200) NOT NULL,
    value                   VARCHAR(200) NOT NULL,
    color                   VARCHAR(7),
    icon                    VARCHAR(50),
    sort_order              INT DEFAULT 0,
    is_default              BOOLEAN DEFAULT FALSE,
    is_active               BOOLEAN DEFAULT TRUE,
    created_at              TIMESTAMPTZ DEFAULT NOW(),

    CONSTRAINT uq_option_value_per_field UNIQUE (field_definition_id, value)
);

CREATE INDEX idx_field_option_field ON field_option(field_definition_id);
CREATE INDEX idx_field_option_active ON field_option(field_definition_id, is_active, sort_order);


CREATE TABLE validation_rule (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    field_definition_id     UUID NOT NULL REFERENCES field_definition(id) ON DELETE CASCADE,
    rule_type               validation_rule_type NOT NULL,
    rule_params             JSONB NOT NULL DEFAULT '{}',
    error_message           VARCHAR(500),
    priority                INT DEFAULT 0,
    is_active               BOOLEAN DEFAULT TRUE,
    created_at              TIMESTAMPTZ DEFAULT NOW(),

    CONSTRAINT uq_rule_type_per_field UNIQUE (field_definition_id, rule_type)
);

CREATE INDEX idx_validation_field ON validation_rule(field_definition_id);


-- ============================================================================
-- DATA TABLES
-- ============================================================================

CREATE TABLE entry (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tracker_id      UUID NOT NULL REFERENCES tracker(id) ON DELETE CASCADE,
    user_id         UUID NOT NULL,
    entry_date      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    status          entry_status DEFAULT 'active',
    tags            JSONB DEFAULT '[]',
    notes           TEXT,
    metadata        JSONB DEFAULT '{}',
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW(),
    deleted_at      TIMESTAMPTZ
);

CREATE INDEX idx_entry_tracker ON entry(tracker_id);
CREATE INDEX idx_entry_user ON entry(user_id);
CREATE INDEX idx_entry_date ON entry(tracker_id, entry_date DESC);
CREATE INDEX idx_entry_status ON entry(tracker_id, status);
CREATE INDEX idx_entry_created ON entry(tracker_id, created_at DESC);
CREATE INDEX idx_entry_not_deleted ON entry(tracker_id, deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_entry_tags ON entry USING GIN(tags);


CREATE TABLE field_value (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    entry_id                UUID NOT NULL REFERENCES entry(id) ON DELETE CASCADE,
    field_definition_id     UUID NOT NULL REFERENCES field_definition(id) ON DELETE CASCADE,

    value_text              TEXT,
    value_number            DECIMAL(15,4),
    value_boolean           BOOLEAN,
    value_date              TIMESTAMPTZ,
    value_json              JSONB,

    created_at              TIMESTAMPTZ DEFAULT NOW(),
    updated_at              TIMESTAMPTZ DEFAULT NOW(),

    CONSTRAINT uq_value_per_entry_field UNIQUE (entry_id, field_definition_id)
);

CREATE INDEX idx_field_value_entry ON field_value(entry_id);
CREATE INDEX idx_field_value_field ON field_value(field_definition_id);
CREATE INDEX idx_field_value_number ON field_value(field_definition_id, value_number)
    WHERE value_number IS NOT NULL;
CREATE INDEX idx_field_value_date ON field_value(field_definition_id, value_date)
    WHERE value_date IS NOT NULL;
CREATE INDEX idx_field_value_text ON field_value(field_definition_id, value_text)
    WHERE value_text IS NOT NULL;
CREATE INDEX idx_field_value_json ON field_value USING GIN(value_json)
    WHERE value_json IS NOT NULL;


-- ============================================================================
-- VIEW & DISPLAY TABLES
-- ============================================================================

CREATE TABLE view_config (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tracker_id      UUID NOT NULL REFERENCES tracker(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    view_type       view_type NOT NULL,
    columns         JSONB DEFAULT '[]',
    sort_rules      JSONB DEFAULT '[]',
    filter_rules    JSONB DEFAULT '[]',
    group_by        JSONB,
    aggregations    JSONB DEFAULT '[]',
    chart_config    JSONB,
    is_default      BOOLEAN DEFAULT FALSE,
    sort_order      INT DEFAULT 0,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_view_tracker ON view_config(tracker_id);


-- ============================================================================
-- AUTOMATION TABLES
-- ============================================================================

CREATE TABLE automation (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tracker_id      UUID NOT NULL REFERENCES tracker(id) ON DELETE CASCADE,
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    trigger_event   automation_trigger NOT NULL,
    trigger_config  JSONB DEFAULT '{}',
    conditions      JSONB DEFAULT '[]',
    action_type     automation_action NOT NULL,
    action_params   JSONB DEFAULT '{}',
    is_active       BOOLEAN DEFAULT TRUE,
    last_triggered  TIMESTAMPTZ,
    run_count       INT DEFAULT 0,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_automation_tracker ON automation(tracker_id);
CREATE INDEX idx_automation_active ON automation(tracker_id, is_active);


CREATE TABLE automation_log (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    automation_id   UUID NOT NULL REFERENCES automation(id) ON DELETE CASCADE,
    entry_id        UUID REFERENCES entry(id) ON DELETE SET NULL,
    trigger_data    JSONB,
    action_result   JSONB,
    status          VARCHAR(20) DEFAULT 'success',
    error_message   TEXT,
    executed_at     TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_auto_log_automation ON automation_log(automation_id);
CREATE INDEX idx_auto_log_date ON automation_log(executed_at DESC);


-- ============================================================================
-- TEMPLATE TABLES
-- ============================================================================

CREATE TABLE template (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    author_id       UUID NOT NULL,
    name            VARCHAR(200) NOT NULL,
    slug            VARCHAR(200) NOT NULL UNIQUE,
    description     TEXT,
    scope           VARCHAR(20) NOT NULL DEFAULT 'tracker',
    schema_snapshot JSONB NOT NULL,
    preview_config  JSONB,
    tags            JSONB DEFAULT '[]',
    download_count  INT DEFAULT 0,
    rating          DECIMAL(2,1) DEFAULT 0.0,
    is_public       BOOLEAN DEFAULT FALSE,
    is_verified     BOOLEAN DEFAULT FALSE,
    version         VARCHAR(20) DEFAULT '1.0.0',
    published_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_template_author ON template(author_id);
CREATE INDEX idx_template_public ON template(is_public, published_at DESC);
CREATE INDEX idx_template_tags ON template USING GIN(tags);


-- ============================================================================
-- HELPER FUNCTIONS & TRIGGERS
-- ============================================================================

CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_domain_updated BEFORE UPDATE ON domain
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();
CREATE TRIGGER trg_tracker_updated BEFORE UPDATE ON tracker
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();
CREATE TRIGGER trg_field_def_updated BEFORE UPDATE ON field_definition
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();
CREATE TRIGGER trg_entry_updated BEFORE UPDATE ON entry
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();
CREATE TRIGGER trg_field_value_updated BEFORE UPDATE ON field_value
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();
CREATE TRIGGER trg_view_config_updated BEFORE UPDATE ON view_config
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();
CREATE TRIGGER trg_automation_updated BEFORE UPDATE ON automation
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();


-- ============================================================================
-- UTILITY VIEWS
-- ============================================================================

CREATE VIEW v_tracker_fields AS
SELECT
    fd.id,
    fd.tracker_id,
    fd.name,
    fd.slug,
    fd.field_type,
    fd.is_required,
    fd.sort_order,
    fd.is_active,
    t.name AS tracker_name,
    d.name AS domain_name,
    (SELECT COUNT(*) FROM field_option fo WHERE fo.field_definition_id = fd.id) AS options_count,
    (SELECT COUNT(*) FROM validation_rule vr WHERE vr.field_definition_id = fd.id) AS rules_count
FROM field_definition fd
JOIN tracker t ON t.id = fd.tracker_id
JOIN domain d ON d.id = t.domain_id
ORDER BY d.sort_order, t.sort_order, fd.sort_order;


CREATE VIEW v_entry_with_values AS
SELECT
    e.id AS entry_id,
    e.tracker_id,
    e.user_id,
    e.entry_date,
    e.status,
    e.tags,
    e.notes,
    e.created_at,
    fd.slug AS field_slug,
    fd.name AS field_name,
    fd.field_type,
    COALESCE(
        fv.value_text,
        CAST(fv.value_number AS TEXT),
        CAST(fv.value_boolean AS TEXT),
        CAST(fv.value_date AS TEXT),
        CAST(fv.value_json AS TEXT)
    ) AS display_value,
    fv.value_number,
    fv.value_text,
    fv.value_boolean,
    fv.value_date,
    fv.value_json
FROM entry e
JOIN field_value fv ON fv.entry_id = e.id
JOIN field_definition fd ON fd.id = fv.field_definition_id
WHERE e.deleted_at IS NULL
ORDER BY e.entry_date DESC, fd.sort_order;


CREATE VIEW v_tracker_summary AS
SELECT
    t.id AS tracker_id,
    t.name AS tracker_name,
    d.name AS domain_name,
    d.color AS domain_color,
    (SELECT COUNT(*) FROM entry e WHERE e.tracker_id = t.id AND e.deleted_at IS NULL) AS total_entries,
    (SELECT MAX(e.entry_date) FROM entry e WHERE e.tracker_id = t.id AND e.deleted_at IS NULL) AS last_entry_date,
    (SELECT COUNT(*) FROM entry e WHERE e.tracker_id = t.id AND e.deleted_at IS NULL
        AND e.entry_date >= DATE_TRUNC('week', NOW())) AS entries_this_week,
    (SELECT COUNT(*) FROM entry e WHERE e.tracker_id = t.id AND e.deleted_at IS NULL
        AND e.entry_date >= DATE_TRUNC('month', NOW())) AS entries_this_month
FROM tracker t
JOIN domain d ON d.id = t.domain_id
WHERE t.is_active = TRUE
ORDER BY d.sort_order, t.sort_order;
