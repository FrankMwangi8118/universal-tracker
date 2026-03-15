# Universal Tracker — API Reference

## Base URL
```
http://<server-ip>:8080
```

## Authentication
Every request requires:
```
X-User-Id: <uuid>
```
Example: `X-User-Id: a1000000-0000-0000-0000-000000000001`

Returns `400` if missing or not a valid UUID.

---

## Response Format

### Success
```json
{ "success": true, "data": { ... }, "meta": null }
```

### Success (paginated)
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

### Error
```json
{
  "error": "Human readable message",
  "code": "RESOURCE_NOT_FOUND",
  "details": { "field_name": "error detail" },
  "timestamp": "2026-03-14T10:00:00Z"
}
```

**Error codes:**
| Code | HTTP Status | When |
|------|------------|------|
| `VALIDATION_ERROR` | 400 | Invalid request body fields |
| `RESOURCE_NOT_FOUND` | 404 | ID/slug does not exist |
| `DUPLICATE_RESOURCE` | 409 | Unique constraint violation |
| `BUSINESS_RULE_VIOLATION` | 422 | Logic error (e.g. wrong ownership) |
| `METHOD_NOT_ALLOWED` | 405 | Wrong HTTP method |

---

## Health

### GET /api/health
Check if the server is running.

**Response:**
```json
{ "success": true, "data": "OK", "meta": null }
```

---

## Domains

A domain is a top-level grouping (e.g. "Finance", "Health", "Work").

---

### POST /api/domains
Create a new domain.

**Request:**
```json
{
  "name": "Finance",          // required, max 100 chars
  "icon": "💰",               // optional
  "color": "#22c55e",         // optional, hex color
  "description": "..."        // optional
}
```

**Response: 201**
```json
{
  "id": "3f1b2c4d-...",
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

### GET /api/domains
List all domains for the current user, ordered by `sort_order`.

**Response: 200**
```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "name": "Finance",
      "slug": "finance",
      "icon": "💰",
      "color": "#22c55e",
      "sort_order": 0,
      "is_active": true,
      "tracker_count": 3
    }
  ]
}
```

---

### GET /api/domains/{slug}
Get a single domain by its slug.

**Response: 200** — same as create response.

---

### PUT /api/domains/{id}
Update a domain. All fields are optional (partial update).

**Request:**
```json
{
  "name": "Personal Finance",
  "icon": "💵",
  "color": "#16a34a",
  "description": "Updated description",
  "sort_order": 1,
  "is_active": true
}
```

**Response: 200** — full domain object.

---

### DELETE /api/domains/{id}
Delete a domain. Cascades to all trackers, fields, and entries inside it.

**Response: 204** (no body)

---

### PUT /api/domains/reorder
Set sort order for all domains.

**Request:** Array of domain UUIDs in desired order.
```json
["uuid-1", "uuid-2", "uuid-3"]
```

**Response: 200** (no body)

---

## Trackers

A tracker is a configurable database inside a domain (e.g. "Expenses", "Workouts").

---

### POST /api/trackers
Create a tracker.

**Request:**
```json
{
  "domain_id": "uuid",                    // required
  "name": "Expenses",                     // required, max 100 chars
  "icon": "💳",                           // optional
  "description": "Daily expenses",        // optional
  "entry_name_singular": "Expense",       // optional, default "Entry"
  "entry_name_plural": "Expenses",        // optional, default "Entries"
  "default_date_field": "date"            // optional, field slug used as primary date
}
```

**Response: 201**
```json
{
  "id": "uuid",
  "domain_id": "uuid",
  "name": "Expenses",
  "slug": "expenses",
  "icon": "💳",
  "description": "Daily expenses",
  "entry_name_singular": "Expense",
  "entry_name_plural": "Expenses",
  "default_date_field": null,
  "summary_config": {},
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

### GET /api/trackers
List all trackers for the current user across all domains.

**Response: 200** — array of tracker objects (same shape as above).

---

### GET /api/domains/{domainSlug}/trackers
List trackers inside a specific domain.

**Response: 200** — array of tracker objects.

---

### GET /api/trackers/{id}
Get a single tracker by ID.

**Response: 200** — full tracker object.

---

### PUT /api/trackers/{id}
Update a tracker. All fields optional.

**Request:**
```json
{
  "name": "Monthly Expenses",
  "icon": "📊",
  "description": "Updated",
  "entry_name_singular": "Expense",
  "entry_name_plural": "Expenses",
  "is_active": true
}
```

**Response: 200** — full tracker object.

---

### DELETE /api/trackers/{id}
Delete a tracker. Cascades to fields, entries, views, automations.

**Response: 204** (no body)

---

### PUT /api/trackers/{id}/reorder
Reorder trackers within a domain.

**Request:** Array of tracker UUIDs in desired order.
```json
["uuid-1", "uuid-2"]
```

**Response: 200** (no body)

---

## Field Definitions

Fields define the schema (columns) of a tracker.

### Field Types
| Type | Description | Value format |
|------|-------------|-------------|
| `TEXT` | Free text | `"string"` |
| `NUMBER` | Decimal number | `123.45` |
| `CURRENCY` | Money with currency code | `1500.00` |
| `DATE` | Date only | `"2026-03-14T00:00:00Z"` |
| `DATETIME` | Date + time | `"2026-03-14T14:30:00Z"` |
| `TIME` | Time only | `"2026-03-14T14:30:00Z"` |
| `DROPDOWN` | Single option selection | `"option_value"` |
| `MULTI_SELECT` | Multiple option selection | `["val1","val2"]` |
| `CHECKBOX` | Boolean | `true` / `false` |
| `RATING` | 1–5 stars (configurable max) | `4` |
| `PROGRESS` | 0–100 percent | `75` |
| `FORMULA` | Computed from other fields | read-only |
| `URL` | Web URL (validated) | `"https://..."` |
| `EMAIL` | Email address (validated) | `"x@example.com"` |
| `PHONE` | Phone number | `"+254700000000"` |
| `IMAGE` | Image URL | `"https://..."` |
| `COLOR` | Hex color (validated) | `"#ff5733"` |
| `DURATION` | Duration in seconds | `3600` |

---

### POST /api/trackers/{trackerId}/fields
Create a field on a tracker.

**Request:**
```json
{
  "name": "Amount",               // required
  "field_type": "CURRENCY",       // required — see types above
  "is_required": true,            // optional, default false
  "is_unique": false,             // optional, default false
  "is_filterable": true,          // optional, default true
  "is_summable": true,            // optional — show in totals
  "is_primary_display": true,     // optional — shown in entry list header
  "currency_code": "KES",         // for CURRENCY type
  "min_value": 0,                 // for NUMBER/CURRENCY/RATING/PROGRESS
  "max_value": 1000000,           // for NUMBER/CURRENCY/RATING/PROGRESS
  "placeholder": "Enter amount",  // optional hint text
  "help_text": "In Kenyan shillings",
  "options": [                    // required for DROPDOWN / MULTI_SELECT
    { "label": "Food", "value": "food", "color": "#f97316" },
    { "label": "Transport", "value": "transport", "color": "#3b82f6" }
  ]
}
```

**Response: 201**
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
  "placeholder": null,
  "help_text": null,
  "sort_order": 0,
  "is_active": true,
  "options": [],
  "created_at": "2026-03-14T10:00:00Z",
  "updated_at": "2026-03-14T10:00:00Z"
}
```

---

### GET /api/trackers/{trackerId}/fields
List all active fields on a tracker, ordered by `sort_order`.

**Response: 200** — array of field objects (includes `options` array).

---

### GET /api/fields/{id}
Get a single field with all its options.

**Response: 200** — full field object.

---

### PUT /api/fields/{id}
Update a field. All fields optional.

**Request:** Same shape as create, all optional. Pass `options` to replace all options.

**Response: 200** — full field object.

---

### DELETE /api/fields/{id}
Delete a field and all its values in existing entries.

**Response: 204** (no body)

---

### PUT /api/trackers/{trackerId}/fields/reorder
Set sort order for fields.

**Request:** Array of field UUIDs in desired order.
```json
["uuid-1", "uuid-2", "uuid-3"]
```

**Response: 200** (no body)

---

### POST /api/fields/{id}/duplicate
Duplicate a field (copies config and options, excludes `is_primary_display` and `is_unique`).

**Response: 201** — new field object.

---

## Field Options

Options belong to `DROPDOWN` or `MULTI_SELECT` fields only.

---

### POST /api/fields/{fieldId}/options
Add an option to a field.

**Request:**
```json
{
  "label": "Food",        // required, display text
  "value": "food",        // required, stored value (unique per field)
  "color": "#f97316",     // optional, hex color
  "icon": "🍔",           // optional
  "is_default": false     // optional
}
```

**Response: 201**
```json
{
  "id": "uuid",
  "label": "Food",
  "value": "food",
  "color": "#f97316",
  "icon": "🍔",
  "sort_order": 0,
  "is_default": false,
  "is_active": true
}
```

---

### PUT /api/fields/{fieldId}/options/{optionId}
Update an option. All fields optional.

**Response: 200** — full option object.

---

### DELETE /api/fields/{fieldId}/options/{optionId}
Delete an option.

**Response: 204** (no body)

---

### PUT /api/fields/{fieldId}/options/reorder
Set sort order for options.

**Request:** Array of option UUIDs in desired order.

**Response: 200** (no body)

---

## Validation Rules

Rules are enforced on entry create and update.

### Rule Types
| Type | `rule_params` shape | Description |
|------|---------------------|-------------|
| `REQUIRED` | `{}` | Field must have a value |
| `MIN` | `{"min": 0}` | Minimum numeric value |
| `MAX` | `{"max": 100}` | Maximum numeric value |
| `MIN_LENGTH` | `{"min_length": 5}` | Minimum text length |
| `MAX_LENGTH` | `{"max_length": 200}` | Maximum text length |
| `REGEX` | `{"pattern": "^[A-Z]"}` | Must match regex |
| `UNIQUE` | `{}` | Value must be unique across all entries |
| `CUSTOM` | any object | Stored for client-side use |

---

### POST /api/fields/{fieldId}/rules
Add a validation rule.

**Request:**
```json
{
  "rule_type": "MIN",
  "rule_params": { "min": 1 },
  "error_message": "Amount must be at least 1",
  "priority": 1,
  "is_active": true
}
```

**Response: 201**
```json
{
  "id": "uuid",
  "field_definition_id": "uuid",
  "rule_type": "MIN",
  "rule_params": { "min": 1 },
  "error_message": "Amount must be at least 1",
  "priority": 1,
  "is_active": true,
  "created_at": "2026-03-14T10:00:00Z"
}
```

---

### GET /api/fields/{fieldId}/rules
List all rules for a field.

**Response: 200** — array of rule objects.

---

### PUT /api/rules/{id}
Update a rule. All fields optional.

**Response: 200** — full rule object.

---

### DELETE /api/rules/{id}
Delete a rule.

**Response: 204** (no body)

---

## Entries

An entry is a single logged record in a tracker.

---

### POST /api/trackers/{trackerId}/entries
Create an entry.

**Request:**
```json
{
  "entry_date": "2026-03-14T08:00:00Z",   // optional, defaults to now
  "tags": ["march", "recurring"],          // optional
  "notes": "Lunch at Java House",          // optional
  "fields": {
    "amount": 1500,
    "category": "food",
    "date": "2026-03-14T00:00:00Z",
    "paid": true,
    "notes": "Lunch"
  }
}
```

Keys in `fields` are **field slugs**. Values are raw (numbers, strings, booleans, ISO dates, arrays).

If any required field is missing or validation fails, a `400` is returned with details for every failing field:
```json
{
  "error": "Validation failed",
  "code": "VALIDATION_ERROR",
  "details": {
    "amount": "Amount must be at least 1",
    "category": "Field is required"
  }
}
```

**Response: 201**
```json
{
  "id": "uuid",
  "tracker_id": "uuid",
  "entry_date": "2026-03-14T08:00:00Z",
  "status": "active",
  "tags": ["march", "recurring"],
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
    },
    {
      "field_id": "uuid",
      "field_name": "Paid",
      "field_slug": "paid",
      "field_type": "CHECKBOX",
      "raw_value": true,
      "display_value": "Yes"
    }
  ],
  "created_at": "2026-03-14T08:00:00Z",
  "updated_at": "2026-03-14T08:00:00Z"
}
```

---

### GET /api/trackers/{trackerId}/entries
List entries with filtering and pagination.

**Query params:**
| Param | Type | Description |
|-------|------|-------------|
| `page` | int | 0-based (default `0`) |
| `size` | int | Page size (default `20`) |
| `status` | string | `active` / `archived` / `flagged` / `draft` |
| `from` | ISO 8601 | `entry_date` >= from |
| `to` | ISO 8601 | `entry_date` <= to |
| `field[slug]` | string | Filter by field value e.g. `field[category]=food` |

**Response: 200** — paginated array of entry list items (lighter than full entry — no nested field objects, just primary display field values).

---

### GET /api/entries/{id}
Get a single entry with all field values.

**Response: 200** — full entry object (see create response).

---

### PUT /api/entries/{id}
Update an entry. All fields optional.

**Request:**
```json
{
  "entry_date": "2026-03-15T08:00:00Z",
  "status": "archived",
  "tags": ["march"],
  "notes": "Updated note",
  "fields": {
    "amount": 2000,
    "paid": true
  }
}
```

Only the fields you include in `fields` are updated. Others are untouched.

**Response: 200** — full entry object.

---

### DELETE /api/entries/{id}
Soft delete an entry (sets `deleted_at`, hidden from normal list).

**Response: 204** (no body)

---

### POST /api/entries/{id}/restore
Restore a soft-deleted entry.

**Response: 200** — full entry object.

---

### DELETE /api/entries/{id}/permanent
Hard delete an entry. Cannot be undone.

**Response: 204** (no body)

---

### POST /api/trackers/{trackerId}/entries/bulk
Create multiple entries in one request.

**Request:** Array of create entry request objects.
```json
[
  { "entry_date": "2026-03-14T08:00:00Z", "fields": { "amount": 500, "category": "food" } },
  { "entry_date": "2026-03-14T09:00:00Z", "fields": { "amount": 250, "category": "transport" } }
]
```

**Response: 201** — array of full entry objects.

---

### DELETE /api/entries/bulk
Soft delete multiple entries.

**Request:**
```json
{ "ids": ["uuid-1", "uuid-2", "uuid-3"] }
```

**Response: 204** (no body)

---

## Views

Saved view configurations — how entries are displayed (table, chart, calendar, etc).

---

### POST /api/trackers/{trackerId}/views
Create a view.

**Request:**
```json
{
  "name": "All Expenses",
  "view_type": "TABLE",
  "columns": [
    { "field": "amount", "width": 120 },
    { "field": "category", "width": 150 },
    { "field": "date", "width": 120 },
    { "field": "paid", "width": 80 }
  ],
  "sort_rules": [
    { "field": "date", "direction": "desc" }
  ],
  "filter_rules": [
    { "field": "status", "operator": "eq", "value": "active" }
  ],
  "aggregations": [
    { "field": "amount", "fn": "SUM", "label": "Total Spent" }
  ],
  "is_default": true
}
```

**View types:** `TABLE` `CALENDAR` `CHART` `KANBAN` `SUMMARY` `TIMELINE`

**Response: 201**
```json
{
  "id": "uuid",
  "tracker_id": "uuid",
  "name": "All Expenses",
  "view_type": "TABLE",
  "columns": [ ... ],
  "sort_rules": [ ... ],
  "filter_rules": [ ... ],
  "group_by": null,
  "aggregations": [ ... ],
  "chart_config": null,
  "is_default": true,
  "sort_order": 0,
  "created_at": "2026-03-14T10:00:00Z",
  "updated_at": "2026-03-14T10:00:00Z"
}
```

---

### GET /api/trackers/{trackerId}/views
List all views for a tracker.

**Response: 200** — array of view objects.

---

### GET /api/views/{id}
Get a single view.

**Response: 200** — full view object.

---

### PUT /api/views/{id}
Update a view. All fields optional.

**Response: 200** — full view object.

---

### DELETE /api/views/{id}
Delete a view.

**Response: 204** (no body)

---

### PUT /api/views/{id}/default
Set this view as the default for its tracker (unsets previous default).

**Response: 200** — full view object.

---

### POST /api/views/{id}/duplicate
Duplicate a view.

**Response: 201** — new view object with name suffixed " (copy)".

---

## Aggregation

Run aggregate calculations on tracker entries.

---

### GET /api/trackers/{trackerId}/aggregate

**Query params:**
| Param | Required | Description |
|-------|----------|-------------|
| `field` | yes | Field slug to aggregate |
| `fn` | no | `SUM` `AVG` `COUNT` `MIN` `MAX` (default: `SUM`) |
| `group_by` | no | Field slug to group results by |
| `date_bucket` | no | `day` `week` `month` `year` — groups by time period |
| `from` | no | ISO 8601 start filter |
| `to` | no | ISO 8601 end filter |

**Scalar response (no group_by / date_bucket):**
```json
{
  "type": "scalar",
  "fn": "SUM",
  "field": "amount",
  "value": 6550.0
}
```

**Grouped response (group_by set):**
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

**Time series response (date_bucket set):**
```json
{
  "type": "time_series",
  "fn": "SUM",
  "field": "amount",
  "date_bucket": "month",
  "rows": [
    { "bucket": "2026-01", "value": 12000.0 },
    { "bucket": "2026-02", "value": 9800.0 },
    { "bucket": "2026-03", "value": 6550.0 }
  ]
}
```

---

## Dashboard

### GET /api/trackers/{trackerId}/dashboard
Returns summary cards and trend data for the tracker.

**Response: 200**
```json
{
  "tracker_id": "uuid",
  "cards": [
    {
      "label": "Total Spent",
      "value": 6550.0,
      "formatted": "KES 6,550",
      "trend": {
        "direction": "up",
        "change_pct": 12.5
      }
    }
  ],
  "generated_at": "2026-03-14T10:00:00Z"
}
```

---

## Automations

Automations run actions automatically on entry events.

---

### POST /api/trackers/{trackerId}/automations
Create an automation.

**Request:**
```json
{
  "name": "Flag large expenses",
  "description": "Auto-flags entries over KES 3000",
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

**Trigger events:**
| Value | When |
|-------|------|
| `ON_ENTRY_CREATED` | After an entry is created |
| `ON_ENTRY_UPDATED` | After an entry is updated |
| `ON_ENTRY_DELETED` | After an entry is soft-deleted |

**Condition operators:** `eq` `neq` `gt` `lt` `gte` `lte` `contains` `not_null`

**Action types and their `action_params`:**

`UPDATE_STATUS`:
```json
{ "status": "flagged" }
```

`SET_FIELD_VALUE`:
```json
{ "field": "paid", "value": true }
```

`CREATE_ENTRY`:
```json
{ "fields": { "amount": 0, "category": "other" } }
```

`SEND_NOTIFICATION`:
```json
{ "channel": "in_app", "message": "Large expense logged" }
```

`RUN_FORMULA`:
```json
{ "expression": "{price} * {quantity}", "target_field": "total" }
```

**Response: 201**
```json
{
  "id": "uuid",
  "tracker_id": "uuid",
  "name": "Flag large expenses",
  "description": "Auto-flags entries over KES 3000",
  "trigger_event": "ON_ENTRY_CREATED",
  "trigger_config": {},
  "conditions": [
    { "field": "amount", "operator": "gt", "value": 3000 }
  ],
  "action_type": "UPDATE_STATUS",
  "action_params": { "status": "flagged" },
  "is_active": true,
  "last_triggered": null,
  "run_count": 0,
  "created_at": "2026-03-14T10:00:00Z",
  "updated_at": "2026-03-14T10:00:00Z"
}
```

---

### GET /api/trackers/{trackerId}/automations
List all automations for a tracker.

**Response: 200** — array of automation objects.

---

### GET /api/automations/{id}
Get a single automation.

**Response: 200** — full automation object.

---

### PUT /api/automations/{id}
Update an automation. All fields optional.

**Response: 200** — full automation object.

---

### DELETE /api/automations/{id}
Delete an automation.

**Response: 204** (no body)

---

### PUT /api/automations/{id}/toggle
Enable or disable an automation (flips `is_active`).

**Response: 200** — automation object with updated `is_active`.

---

### GET /api/automations/{id}/logs
Get execution history for an automation.

**Query params:** `page` (default 0), `size` (default 20)

**Response: 200** (paginated)
```json
{
  "success": true,
  "data": [
    {
      "id": "uuid",
      "automation_id": "uuid",
      "entry_id": "uuid",
      "trigger_data": {
        "trigger": "on_entry_created",
        "entry_id": "uuid",
        "tracker_id": "uuid"
      },
      "action_result": { "status_set_to": "flagged" },
      "status": "success",
      "error_message": null,
      "executed_at": "2026-03-14T10:00:05Z"
    }
  ],
  "meta": { "page": 0, "size": 20, "total_elements": 1, "total_pages": 1 }
}
```

---

## Swagger UI

Interactive API explorer: `http://<server-ip>:8080/swagger-ui.html`

Click **Authorize** and enter your UUID in the `X-User-Id` field to test all endpoints directly in the browser.
