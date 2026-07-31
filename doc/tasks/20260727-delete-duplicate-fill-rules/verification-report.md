# Verification Report

## Status

PASS

## Passed

- Guarded transaction normalized one retained rule, deleted exactly 86 rows, and committed with one remaining row.
- Before-image contains 87 inserts; SHA-256 is
  `FCB40150DCA3216DA66746213689EDEDD08799B2F51F4A378AD560E3E035AA60`.
- Latest stable database state contains one enabled `FILL` rule for tenant `1`,
  report `1d05410f1d3140c5b8aa6786887ae69c`, and version `130`.
- Final business fields are `scope_key=ALL`, `candidate_source_type=ROLE`,
  `candidate_source_ids=910405`, with no enabled `CODX_VFC_ASSIST_*` rows.
- Role `910405` is “压力泵生产1”; enabled members are 王歆 and 任丹.
- Temporary procedure `codex_repair_fill_rules_20260727` does not remain.
- Mapper version scope explicitly filters `batch_record_version_id` when version ID is non-null.
- Login-state `get-by-report` returned `code=0`, `fillRuleStatus=CONFIGURED`,
  `ROLE / 910405`, and candidate users 王歆、任丹.
- Real page read-only verification displayed `已配置 王歆、任丹` for
  “球囊扩张压力泵 / 粗洗工序生产记录” and produced zero MES write requests.

## Concurrent E2E Observation

- `edhr-visual-fill-config-real-flow.e2e.js` can temporarily recreate 87 cell-scoped rules.
- After the observed run completed, its restore phase returned the target to one formal rule.
- A later run completed and restored the target to one formal rule again.
- The concurrent task was not terminated.

## Runtime Recovery

- Tenant resolution initially timed out, and a later direct login was refused while another task restarted the backend.
- After `48081` recovered with health `UP`, the login-state API and real page verification both passed.

## Final Database Result

- Current count: `1`.
- Latest retained physical ID: `3479` (the write-type E2E restore can recreate the physical row).
- Version limit: `batch_record_version_id=130`.
- Formal rule: `ALL / ROLE / 910405`.
