# 任务：排产工单列表工单编号显示修复

- Task ID: 20260702-schedule-order-workorder-code-display
- Created: 2026-07-02
- Current Status: completed

## Current Status

completed

## Task Goal

修复排产工单列表中工单编号不显示的问题：当排产工单记录存在 `workOrderId` 但历史冗余字段 `erpWorkOrderCode` 为空时，排产工单分页接口必须从关联生产工单补齐返回给前端的工单编码，确保列表“工单编码”列可见。

## Milestones

1. RED：补充后端回归测试，复现 `erpWorkOrderCode` 为空但 `workOrderId` 有效时响应缺少工单编码。completed
2. GREEN：在排产工单响应组装处从关联生产工单补齐显示字段，不改前端兜底。completed
3. REGRESSION：运行后端目标单测和前端排产工单静态契约。completed
4. CLOSEOUT：记录验证结果并提交本任务改动。completed

## Expected Verification

- `mvn -pl yudao-module-mes -Dtest=MesProScheduleOrderControllerTest#getScheduleOrderPage_backfillsWorkOrderCodeFromLinkedWorkOrderWhenScheduleCodeMissing test`
- `node tests/e2e/mes-schedule-order-workorder-link-static.spec.js`
- `node tests/e2e/mes-schedule-order-freeze-visibility-static.spec.js`
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260702-schedule-order-workorder-code-display/bug-regression-evidence.md`

## 经验门禁

- 已读取 `docs/experience-index.md`，本任务命中 PowerShell / Windows shell、前端页面 / 表格 / 样式。
- 已读取 `docs/powershell-memory.md`，PowerShell 命令设置 UTF-8 输入输出，中文文件读写使用显式 UTF-8 或 `apply_patch`。
- 已读取 `bug-regression-fix-loop` 契约，按 RED/GREEN 记录回归证据。
- 本任务不执行真实 E2E、服务器写入、数据库写入、发布、worktree 合并或清理，不触发高风险 experience-preflight。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。修复接口响应组装的正式数据补齐逻辑，不在前端做兜底显示。
- 是否从根因和长期维护角度解决：是。生产工单编号的权威来源是关联生产工单，排产工单列表响应应保证可展示该编号。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- Given 排产工单记录存在 `workOrderId` 但历史冗余字段 `erpWorkOrderCode` 为空, When 前端请求排产工单分页, Then 响应中的 `erpWorkOrderCode` 应使用关联生产工单 `code`，列表显示工单编号。
- Given 排产工单记录已经有 `erpWorkOrderCode`, When 前端请求排产工单分页, Then 响应保持原字段，不影响既有显示与查询。
- Given 排产员点击工单编码, When 排产工单存在生产工单 ID 和编码, Then 前端仍跳转到生产工单详情。

## Current Blockers

- 暂无。

## Final Verification Result

- `mvn -pl yudao-module-mes -Dtest=MesProScheduleOrderControllerTest#getScheduleOrderPage_backfillsWorkOrderCodeFromLinkedWorkOrderWhenScheduleCodeMissing test` -> PASS，Tests run: 1, Failures: 0, Errors: 0, Skipped: 0。
- `node tests/e2e/mes-schedule-order-workorder-link-static.spec.js` -> PASS。
- `node tests/e2e/mes-schedule-order-freeze-visibility-static.spec.js` -> PASS。
- `node tests/e2e/mes-pro-schedule-order-pool-static.spec.js` -> PASS。
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260702-schedule-order-workorder-code-display/bug-regression-evidence.md` -> PASS，Bug regression evidence is valid。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260702-schedule-order-workorder-code-display --mode preview` -> PASS，预览无阻塞；为保留本轮回归证据，未执行 apply 删除 `bug-regression-evidence.md`。
