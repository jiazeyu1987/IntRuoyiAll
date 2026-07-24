# 排产工单删除追溯按钮

## 任务目标

排产工单主列表行操作中删除「追溯」按钮，保留查看、调整、交期、冻结、完成/撤销等既有操作，不改后端追溯接口与弹框能力。

## 经验门禁

- PowerShell / Windows shell：已读取 `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`；涉及中文读写时使用 `python -X utf8` 或 `apply_patch`，不使用默认 `Get-Content` / `Set-Content` / 重定向处理中文。
- 前端页面 / 表格 / 样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`；本次只删除行内文字操作，不引入新布局和新视觉风格。
- 前端交付契约：已读取 `frontend-feature-delivery` 技能和 `references/frontend-contract.md`；本次补充静态契约验证和前端证据。
- 真实 E2E：本次不执行真实登录和写入链路；如后续需要真实 E2E，先读取 `docs/login-access.md` 并完成登录 preflight。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；仅删除前端行操作入口。
- `是否从根因和长期维护角度解决`：是；直接从排产工单主列表行操作模板移除入口，并用静态契约防止回归。
- `是否存在临时补丁或绕过`：否。

## 里程碑

- [x] M1：读取 PowerShell、经验索引、前端交付和统一前端样式门禁。
- [x] M2：补充 RED 静态契约，锁定排产工单行操作不得出现「追溯」按钮。
- [x] M3：最小删除排产工单行操作中的「追溯」按钮。
- [x] M4：运行 targeted 静态验证和前端证据校验。
- [x] M5：运行收尾清理预览，更新任务记录并提交本次直接改动。

## BDD 场景

- BDD: 排产工单行操作删除追溯入口 -> Given 用户打开排产工单列表 / When 查看每行操作列 / Then 行操作不再显示「追溯」按钮，也不绑定 `openOperationLogDialog(row)`。

## 预期验证

- `node tests/e2e/mes-schedule-order-row-actions-static.spec.js`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260703-schedule-order-remove-trace-action/frontend-feature-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260703-schedule-order-remove-trace-action --mode preview`

## 当前状态

- 状态：completed
- 当前里程碑：completed
- 已完成：行操作静态契约已 GREEN，非冻结行操作不再包含「追溯」按钮和 `openOperationLogDialog(row)` 绑定。
- 已完成：前端证据校验通过，收尾清理预览复验通过，`frontend-feature-evidence.md` 已列入保留项。
- 最终验证：`node tests/e2e/mes-schedule-order-row-actions-static.spec.js`、前端证据校验、收尾清理预览均通过。
- 阻塞：无。当前前端仓仍有其他历史脏改，本次提交只纳入本任务直接相关改动。

## Cleanup Keep

- doc/tasks/20260703-schedule-order-remove-trace-action/frontend-feature-evidence.md
