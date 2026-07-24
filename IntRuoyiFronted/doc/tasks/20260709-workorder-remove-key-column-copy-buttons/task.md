# 20260709 生产工单删除每列复制按钮

## 任务目标

- 生产工单列表中，删除截图所示每列右侧的复制按钮。
- 覆盖字段：工单编号、产品编码、产品名称、规格型号、计划数量。
- 保留工单编号点击查看详情能力，保留列顺序、列宽、分页、显示字段和现有接口逻辑。

## 里程碑

- [x] M1 建立任务文档、BDD 场景、设计约束与经验门禁。
- [x] M2 编写删除复制按钮静态契约，先得到 RED。
- [x] M3 移除生产工单关键列复制按钮和无用复制处理函数。
- [x] M4 更新旧静态契约并运行相关验证。
- [x] M5 更新任务记录并按仓库状态决定是否提交。

## 预期验证

- `node tests/e2e/workorder-remove-key-column-copy-buttons-static.spec.js`
- `node tests/e2e/workorder-key-columns-static.spec.js`
- `node tests/e2e/workorder-code-copy-button-static.spec.js`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260709-workorder-remove-key-column-copy-buttons/frontend-feature-evidence.md`

## 经验门禁

- PowerShell / Windows shell / 中文编码：已读取 `docs/powershell-memory.md`；中文文件读写使用 UTF-8 aware 路径或 `apply_patch`。
- 前端页面 / 表格 / 样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`；本次只删除用户指定复制按钮，不做无关重构。
- 前端功能交付：已读取 `frontend-feature-delivery` 与 `frontend-contract.md`；按 BDD + TDD 记录证据。
- 本任务不涉及真实 E2E、服务器写入、数据库写入、发布、备份、恢复或 worktree 合并；无需执行高风险 `experience-preflight`。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，直接移除复制按钮渲染和不再使用的复制处理函数。
- 是否存在临时补丁或绕过：否。

## Current Status

completed

## 当前状态

- Status: completed
- 已完成：定位生产工单列表关键列复制按钮位于 `src/views/mes/pro/workorder/index.vue`。
- 已完成：静态契约 RED 证明工单编号列仍渲染复制按钮，随后移除五个关键列复制按钮和剪贴板 handler。
- 已完成：新旧静态契约和前端 evidence 校验均通过。

## 最终验证结果

- RED: `node tests/e2e/workorder-remove-key-column-copy-buttons-static.spec.js` -> FAIL，工单编号列仍渲染 `work-order-key-copy` 复制按钮。
- GREEN: `node tests/e2e/workorder-remove-key-column-copy-buttons-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/workorder-key-columns-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/workorder-code-copy-button-static.spec.js` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260709-workorder-remove-key-column-copy-buttons/frontend-feature-evidence.md` -> PASS。

## 提交状态

- Git commit -> BLOCKED，当前前端仓存在大量既有脏改；为避免混入非本任务 hunk，未创建提交。

## Cleanup Keep

- `doc/tasks/20260709-workorder-remove-key-column-copy-buttons/frontend-feature-evidence.md`
