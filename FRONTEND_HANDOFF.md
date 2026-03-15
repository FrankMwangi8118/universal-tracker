# Universal Tracker — Frontend Handoff

## Overview

Universal Tracker is a **metadata-driven personal tracking platform**. Users define their own data structures (trackers with typed fields) and log entries against them. Think of it like a personal Notion database or Airtable — but entirely API-driven.

The backend is a Spring Boot 4 REST API running on `http://localhost:8080`.

---

## Authentication

Every request **must** include a `X-User-Id` header with a valid UUID. There is no login/token system — this header is the identity.

```
X-User-Id: a1000000-0000-0000-0000-000000000001
```

If missing or invalid, the API returns `400 Bad Request`.

---

## Response Envelope

All responses follow this shape:

```json
{
  "success": true,
  "data": { ... },
  "meta": null
}
```

Paginated responses include `meta`:

```json
{
  "success": true,
  "data": [ ... ],
  "meta": {
    "page": 0,
    "size": 20,
    "total_elements": 54,
    "total_pages": 3
  }
}
```

Error responses:

```json
{
  "error": "Human-readable message",
  "code": "RESOURCE_NOT_FOUND | DUPLICATE_RESOURCE | VALIDATION_ERROR | BUSINESS_RULE_VIOLATION | METHOD_NOT_ALLOWED",
  "details": { "field": "error message" },
  "timestamp": "2026-03-14T15:41:25.025Z"
}
```

All fields use **snake_case**.

---

## Core Concepts

```
User
 └── Domain (e.g. "Finance", "Health", "Work")
      └── Tracker (e.g. "Expenses", "Workouts", "Tasks")
           ├── FieldDefinition (e.g. "Amount", "Category", "Date")
           │    ├── FieldOption (for DROPDOWN / MULTI_SELECT fields)
           │    └── ValidationRule (MIN, MAX, REQUIRED, REGEX, etc.)
           ├── Entry (one logged record)
           │    └── FieldValue (value for each field in this entry)
           ├── ViewConfig (saved table/chart/calendar views)
           └── Automation (trigger → condition → action)
```

---

## Field Types

| Type | Stored as | Notes |
|------|-----------|-------|
| `TEXT` | string | |
| `NUMBER` | decimal | |
| `CURRENCY` | decimal | has `currency_code` (e.g. "KES") |
| `DATE` | ISO 8601 string | |
| `DATETIME` | ISO 8601 string | |
| `TIME` | ISO 8601 string | |
| `DROPDOWN` | string (option value) | requires options |
| `MULTI_SELECT` | array of strings | requires options |
| `CHECKBOX` | boolean | |
| `RATING` | decimal (1–5 default) | |
| `PROGRESS` | decimal (0–100) | |
| `FORMULA` | decimal | computed server-side |
| `URL` | string | validated |
| `EMAIL` | string | validated |
| `PHONE` | string | validated |
| `IMAGE` | string (URL) | |
| `COLOR` | string (#hex) | validated |
| `DURATION` | decimal (seconds) | |

---

## API Reference

### Health

```
GET /api/health
```

---

### Domains

Domains are top-level groupings (like folders).

```
POST   /api/domains                    Create domain
GET    /api/domains                    List all domains for current user
GET    /api/domains/{slug}             Get domain by slug
PUT    /api/domains/{id}               Update domain
DELETE /api/domains/{id}               Delete domain (cascades to trackers)
PUT    /api/domains/reorder            Reorder domains (body: array of UUIDs)
```

**Create request:**
```json
{
  "name": "Finance",
  "icon": "💰",
  "color": "#22c55e",
  "description": "Personal finance tracking"
}
```

**Response:**
```json
{
  "id": "uuid",
  "name": "Finance",
  "slug": "finance",
  "icon": "💰",
  "color": "#22c55e",
  "description": "Personal finance tracking",
  "sort_order": 0,
  "is_active": true,
  "tracker_count": 0,
  "created_at": "2026-03-14T10:00:00Z",
  "updated_at": "2026-03-14T10:00:00Z"
}
```

---

### Trackers

Trackers are the actual databases (like a spreadsheet sheet).

```
POST   /api/trackers                          Create tracker (body must include domain_id)
GET    /api/trackers                          List all trackers for current user
GET    /api/domains/{domainSlug}/trackers     List trackers in a domain
GET    /api/trackers/{id}                     Get tracker
PUT    /api/trackers/{id}                     Update tracker
DELETE /api/trackers/{id}                     Delete tracker
PUT    /api/trackers/{id}/reorder             Reorder trackers within domain
```

**Create request:**
```json
{
  "domain_id": "uuid",
  "name": "Expenses",
  "icon": "💳",
  "description": "Daily expense tracking",
  "entry_name_singular": "Expense",
  "entry_name_plural": "Expenses"
}
```

**Response:**
```json
{
  "id": "uuid",
  "domain_id": "uuid",
  "name": "Expenses",
  "slug": "expenses",
  "icon": "💳",
  "description": "Daily expense tracking",
  "entry_name_singular": "Expense",
  "entry_name_plural": "Expenses",
  "field_count": 0,
  "entry_count": 0,
  "last_entry_at": null,
  "sort_order": 0,
  "is_active": true,
  "created_at": "2026-03-14T10:00:00Z",
  "updated_at": "2026-03-14T10:00:00Z"
}
```

---

### Field Definitions

Define the columns/schema of a tracker.

```
POST   /api/trackers/{trackerId}/fields       Create field
GET    /api/trackers/{trackerId}/fields       List fields
GET    /api/fields/{id}                       Get field
PUT    /api/fields/{id}                       Update field
DELETE /api/fields/{id}                       Delete field
PUT    /api/trackers/{trackerId}/fields/reorder  Reorder fields (body: array of UUIDs)
POST   /api/fields/{id}/duplicate            Duplicate field
```

**Create request:**
```json
{
  "name": "Amount",
  "field_type": "CURRENCY",
  "is_required": true,
  "is_primary_display": true,
  "is_summable": true,
  "currency_code": "KES"
}
```

For DROPDOWN / MULTI_SELECT, include `options` array:
```json
{
  "name": "Category",
  "field_type": "DROPDOWN",
  "is_required": true,
  "is_filterable": true,
  "options": [
    { "label": "Food", "value": "food", "color": "#f97316" },
    { "label": "Transport", "value": "transport", "color": "#3b82f6" }
  ]
}
```

**Response:**
```json
{
  "id": "uuid",
  "tracker_id": "uuid",
  "name": "Amount",
  "slug": "amount",
  "field_type": "CURRENCY",
  "is_required": true,
  "is_unique": false,
  "is_filterable": true,
  "is_summable": true,
  "is_primary_display": true,
  "currency_code": "KES",
  "min_value": null,
  "max_value": null,
  "sort_order": 0,
  "is_active": true,
  "options": [],
  "created_at": "2026-03-14T10:00:00Z",
  "updated_at": "2026-03-14T10:00:00Z"
}
```

---

### Field Options

For DROPDOWN and MULTI_SELECT fields only.

```
POST   /api/fields/{fieldId}/options                      Add option
PUT    /api/fields/{fieldId}/options/{optionId}           Update option
DELETE /api/fields/{fieldId}/options/{optionId}           Delete option
PUT    /api/fields/{fieldId}/options/reorder              Reorder (body: array of UUIDs)
```

**Create request:**
```json
{
  "label": "Food",
  "value": "food",
  "color": "#f97316",
  "icon": "🍔",
  "is_default": false
}
```

---

### Validation Rules

Rules applied to field values on entry create/update.

```
POST   /api/fields/{fieldId}/rules    Add rule
GET    /api/fields/{fieldId}/rules    List rules
PUT    /api/rules/{id}                Update rule
DELETE /api/rules/{id}                Delete rule
```

**Rule types:** `REQUIRED` `MIN` `MAX` `MIN_LENGTH` `MAX_LENGTH` `REGEX` `UNIQUE` `CUSTOM`

**Create request:**
```json
{
  "rule_type": "MIN",
  "rule_params": { "min": 1 },
  "error_message": "Amount must be at least 1",
  "priority": 1
}
```

---

### Entries

Entries are the actual logged data rows.

```
POST   /api/trackers/{trackerId}/entries           Create entry
GET    /api/trackers/{trackerId}/entries           List entries (paginated, filterable)
GET    /api/entries/{id}                           Get entry
PUT    /api/entries/{id}                           Update entry
DELETE /api/entries/{id}                           Soft delete entry
POST   /api/entries/{id}/restore                   Restore soft-deleted entry
DELETE /api/entries/{id}/permanent                 Hard delete entry
POST   /api/trackers/{trackerId}/entries/bulk      Bulk create
DELETE /api/entries/bulk                           Bulk soft delete (body: { "ids": [...] })
```

**Create request:**
```json
{
  "entry_date": "2026-03-14T08:00:00Z",
  "tags": ["march", "recurring"],
  "notes": "Optional free text note",
  "fields": {
    "amount": 1500,
    "category": "food",
    "date": "2026-03-14T00:00:00Z",
    "paid": true
  }
}
```

`fields` keys are **field slugs** (auto-generated from field name, lowercased + hyphenated).

**List query params:**
| Param | Type | Description |
|-------|------|-------------|
| `page` | int | 0-based page (default 0) |
| `size` | int | page size (default 20) |
| `status` | string | `active` `archived` `flagged` `draft` |
| `from` | ISO 8601 | filter entry_date >= from |
| `to` | ISO 8601 | filter entry_date <= to |
| `field[slug]` | string | filter by field value e.g. `field[category]=food` |

**Entry response:**
```json
{
  "id": "uuid",
  "tracker_id": "uuid",
  "entry_date": "2026-03-14T08:00:00Z",
  "status": "active",
  "tags": ["march"],
  "notes": "Lunch at Java House",
  "fields": [
    {
      "field_id": "uuid",
      "field_name": "Amount",
      "field_slug": "amount",
      "field_type": "CURRENCY",
      "raw_value": 1500,
      "display_value": "KES 1,500"
    },
    {
      "field_id": "uuid",
      "field_name": "Category",
      "field_slug": "category",
      "field_type": "DROPDOWN",
      "raw_value": "food",
      "display_value": "Food"
    }
  ],
  "created_at": "2026-03-14T08:00:00Z",
  "updated_at": "2026-03-14T08:00:00Z"
}
```

---

### Views

Saved view configurations (table, calendar, chart, kanban, summary, timeline).

```
POST   /api/trackers/{trackerId}/views    Create view
GET    /api/trackers/{trackerId}/views    List views
GET    /api/views/{id}                   Get view
PUT    /api/views/{id}                   Update view
DELETE /api/views/{id}                   Delete view
PUT    /api/views/{id}/default           Set as default view
POST   /api/views/{id}/duplicate         Duplicate view
```

**Create request:**
```json
{
  "name": "All Expenses",
  "view_type": "TABLE",
  "columns": [
    { "field": "amount", "width": 120 },
    { "field": "category", "width": 150 },
    { "field": "date", "width": 120 }
  ],
  "sort_rules": [{ "field": "date", "direction": "desc" }],
  "filter_rules": [],
  "is_default": true
}
```

**View types:** `TABLE` `CALENDAR` `CHART` `KANBAN` `SUMMARY` `TIMELINE`

---

### Aggregation

```
GET /api/trackers/{trackerId}/aggregate
```

Query params:
| Param | Required | Description |
|-------|----------|-------------|
| `field` | yes | field slug to aggregate |
| `fn` | no | `SUM` `AVG` `COUNT` `MIN` `MAX` (default: `SUM`) |
| `group_by` | no | field slug to group by |
| `date_bucket` | no | `day` `week` `month` `year` |
| `from` | no | ISO 8601 start date |
| `to` | no | ISO 8601 end date |

**Scalar response:**
```json
{
  "type": "scalar",
  "fn": "SUM",
  "field": "amount",
  "value": 6550.0
}
```

**Grouped response:**
```json
{
  "type": "grouped",
  "fn": "SUM",
  "field": "amount",
  "group_by": "category",
  "rows": [
    { "group": "food", "value": 1500.0 },
    { "group": "transport", "value": 250.0 },
    { "group": "utilities", "value": 4800.0 }
  ]
}
```

**Time series response:**
```json
{
  "type": "time_series",
  "fn": "SUM",
  "field": "amount",
  "date_bucket": "month",
  "rows": [
    { "bucket": "2026-03", "value": 6550.0 }
  ]
}
```

---

### Tracker Dashboard

```
GET /api/trackers/{trackerId}/dashboard
```

Returns summary cards and trend data based on the tracker's `summary_config`. If `summary_config` is empty, returns basic entry counts.

---

### Automations

Automations execute actions automatically when entry events occur.

```
POST   /api/trackers/{trackerId}/automations    Create automation
GET    /api/trackers/{trackerId}/automations    List automations
GET    /api/automations/{id}                   Get automation
PUT    /api/automations/{id}                   Update automation
DELETE /api/automations/{id}                   Delete automation
PUT    /api/automations/{id}/toggle            Enable / disable
GET    /api/automations/{id}/logs              Execution log (paginated)
```

**Triggers:** `ON_ENTRY_CREATED` `ON_ENTRY_UPDATED` `ON_ENTRY_DELETED` `ON_FIELD_VALUE_CHANGED` `ON_THRESHOLD_REACHED`

**Actions:** `SET_FIELD_VALUE` `UPDATE_STATUS` `CREATE_ENTRY` `SEND_NOTIFICATION` `RUN_FORMULA`

**Create request:**
```json
{
  "name": "Flag large expenses",
  "trigger_event": "ON_ENTRY_CREATED",
  "trigger_config": {},
  "conditions": [
    { "field": "amount", "operator": "gt", "value": 3000 }
  ],
  "action_type": "UPDATE_STATUS",
  "action_params": {
    "status": "flagged"
  }
}
```

**Condition operators:** `eq` `neq` `gt` `lt` `gte` `lte` `contains` `not_null`

**Action params by type:**

| Action | Required params |
|--------|----------------|
| `SET_FIELD_VALUE` | `field` (slug), `value` |
| `UPDATE_STATUS` | `status` (`active` / `archived` / `flagged` / `draft`) |
| `CREATE_ENTRY` | `fields` (object of slug → value) |
| `SEND_NOTIFICATION` | `message`, `channel` |
| `RUN_FORMULA` | `expression` (e.g. `"{price} * {qty}"`), `target_field` (slug) |

Automations run **asynchronously** — they do not block the entry creation response.

---

## Entry Statuses

| Value | Meaning |
|-------|---------|
| `active` | Normal, visible entry |
| `archived` | Hidden from default views |
| `flagged` | Marked for attention |
| `draft` | Work in progress |

---

## Slug Rules

Slugs are auto-generated from names. "Monthly Expenses" → `monthly-expenses`. They are unique per parent (per domain for trackers, per tracker for fields). You never need to send a slug — just use the `id` or the slug returned in the response.

---

## Base URL

```
http://localhost:8080
```

Swagger UI available at: `http://localhost:8080/swagger-ui.html`
API docs (OpenAPI JSON): `http://localhost:8080/v3/api-docs`

---

## What Is Not Yet Implemented

- **Templates** — save/load tracker configs as reusable templates
- **Export** — download entries as CSV or JSON
- **Global dashboard** — cross-tracker stats for the whole user account
- **User management** — no sign-up/login, auth is header-only for now
- **Webhooks** — automation webhook action is stubbed (no HTTP delivery)
- **Push notifications** — send_notification logs intent only, no delivery
