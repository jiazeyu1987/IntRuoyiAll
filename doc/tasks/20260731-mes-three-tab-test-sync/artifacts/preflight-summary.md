# Three Tab Sync Preflight Summary

- Generated: `2026-07-31T00:26:45.089516+00:00`
- Source whitelist rows: `2989`
- Target current whitelist rows: `1096`
- Blockers: `13`

## Blockers
- `schema`: target mes_pro_route_version.route_snapshot_json is not MEDIUMTEXT
- `schema`: target mes_pro_schedule_order.promise_date is not nullable
- `schema`: target mes_pro_batch_record_report.form_definition_id is missing
- `schema`: target mes_pro_batch_record_report.form_version_id is missing
- `schema`: source route snapshots exceed target TEXT capacity
- `dependency`: target dependency check failed for form_template_version_id
- `dependency`: target dependency check failed for permission_scope_id
- `dependency`: target dependency check failed for item_id
- `dependency`: target dependency check failed for work_order_id
- `dependency`: target dependency check failed for calendar_rule_id
- `dependency`: target dependency check failed for workstation_id
- `dependency`: target dependency check failed for user_id
- `external_reference`: target has active non-whitelist references to rows that would be replaced
