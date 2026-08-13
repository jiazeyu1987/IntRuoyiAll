# DF11 Frontend item and task projection

## Task Goal

Align the frontline PQC frontend API contract with the dedicated process response: activeOrderId request identity, full QA item fields, resultType union, inspectionRuleKey union, task status/options, production candidates, and stable task ordering. The page change is limited to removing the obsolete process-field-to-task fallback so the strict DTO remains authoritative; all other page behavior remains owned by INT12.

## Milestones

- [x] M1: Read AGENTS, frontend, encoding, closeout, skill, dev-plan, and test-plan rules.
- [x] M2: Record BDD scenarios and create DF11 RED static contract.
- [x] M3: Implement the frontend API type/request projection in owned files only.
- [x] M4: Run GREEN static contract, pnpm ts:check, git diff --check, frontend evidence validator, and forbidden scans.
- [x] M5: Update evidence and mark current status ready_for_closeout.

## Expected Verification

- `node tests/e2e/frontline-pqc-qa-process-contract-static.spec.cjs`
- `pnpm ts:check`
- `git diff --check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260812-frontline-pqc-dcc-qa-df11/frontend-feature-evidence.md`
- Forbidden scan for page/backend/schema/shared-doc changes, fallback/default-success patterns, route/formBinding replacements, and lost PATROL_AM/PATROL_PM identity.

## Applicable Experience Gates

- 前端静态契约隔离门禁：使用任务专用最小静态合同，锚点限定在 API 类型和请求 helper，不改无关大合同。
- 前端选择弹框即时反馈门禁 / PQC 待检工单门禁：活跃订单进程请求不得退回 workOrderId + routeId 推断，旧选择和待检状态由后续 INT12 页面集成处理。
- PQC 规则任务身份门禁：FIRST、PATROL_AM、PATROL_PM、FINAL 是正式 rule key；PATROL_AM 与 PATROL_PM 不得按 inspectionType=PATROL 合并。

## Design Constraints Check

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，按 DF11 API 契约补齐正式类型与请求 helper，并删除页面从工序顶层字段合成任务选项的旧 fallback。
- 是否存在临时补丁或绕过：否。

## Current Status

ready_for_closeout：2026-08-14 重启后复验确认冻结接口、完整 DTO、activeOrderId 唯一身份、旧 helper 删除、稳定排序、真实 consumer stale-response 隔离和页面旧任务合成 fallback 删除均已完成；静态合同、pnpm ts:check、diff check、证据校验和禁止项扫描通过。

## Verification Summary

- RED: node tests/e2e/frontline-pqc-qa-process-contract-static.spec.cjs -> FAIL，缺少正式 pqcProjection.ts。
- GREEN: node tests/e2e/frontline-pqc-qa-process-contract-static.spec.cjs -> PASS，覆盖完整 DTO、冻结 endpoint、旧 helper 删除、反序排序、AM/PM 身份和 stale-response 隔离。
- RED: node focused contract -> FAIL，检测到页面仍存在 getProcessPqcTaskSnapshot，从工序顶层字段合成缺失规则身份的任务选项。
- GREEN: node focused contract -> PASS，页面只使用后端正式 pqcTaskOptions，不再合成任务 fallback。
- GREEN: node focused contract -> PASS，picker row key、active equality、cache key、refresh retention 均只使用 activeOrderId。
- GREEN: node focused contract -> PASS，真实 selectFrontlinePqcActiveOrder consumer 的反序响应不会覆盖最新 activeOrderId 选择。
- REGRESSION: pnpm ts:check -> PASS。
- VALIDATOR: frontend-feature evidence validator -> PASS。
- VALIDATOR: bug-regression evidence validator -> PASS。
- STATIC: git diff --check 与新增行禁止项扫描 -> PASS。
- STATIC: 生产源码精确扫描确认无 getFrontlinePqcActiveOrderProcesses、旧 `/pqc/processes`、getProcessPqcTaskSnapshot、createFrontlinePqcProjectionLoader、FRONTLINE_PQC_RULE_KEY_ORDER、process-level flattened task 读取或 workOrderId+routeId 活跃订单身份。
- SCOPE: FrontlineFixedTemplatePanel.vue 仅删除旧任务合成 helper/调用，并改用 activeOrderId 选择身份；不改渲染、交互、加载、选择或提交合同。
