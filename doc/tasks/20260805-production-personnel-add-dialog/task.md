# 生产人员档案新增人员弹框

## Task Goal

在班组长工作台的生产人员档案页签中，将截图红框内“搜索选择正式工 / 手动录入临时工”入口从页面内联区块迁移到“新增人员”按钮点击后的弹框中；页面黄框位置新增“新增人员”按钮。

## Milestones

- [x] M0：读取前端、E2E、任务收尾、编码和技能规则，保存进入本任务前的脏工作区基线。
- [x] M1：补充生产人员档案弹框行为的 BDD 场景和 RED 静态合同。
- [x] M2：在 `TeamLeaderWorkbenchPage.vue` 中实现新增人员按钮、弹框和内容迁移。
- [x] M3：运行目标静态合同、类型检查或相邻验证，并记录 GREEN/回归证据。
- [x] M4：任务收尾，cleanup preview/apply、收尾记录提交和 `origin/int_main` 推送均通过。

## Expected Verification

- `node tests/e2e/production-personnel-add-dialog-static.spec.cjs`
- `node tests/e2e/production-personnel-management-static.spec.cjs`
- `node tests/e2e/production-personnel-audit-inline-static.spec.cjs`
- `pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260805-production-personnel-add-dialog/frontend-feature-evidence.md`

## Current Status

completed

- 已完成新增人员按钮和弹框迁移，人员相关静态合同全部 PASS。
- `pnpm ts:check` 最终复跑 PASS，并发任务此前造成的同文件类型检查阻塞已解除。
- 实现提交：`172c55077 feat: move production personnel creation into dialog`。
- 收尾提交：`74a3fdb61 docs: close production personnel dialog task`。
- `task-closeout-cleanup` preview/apply 均 PASS，已删除临时 `frontend-feature-evidence.md`，保留三份核心任务文档。
- 实现与收尾提交已推送到 `origin/int_main`；最终状态记录推送后本地分支不再领先远端。
- 工作区仍存在大量并发任务残余，均未纳入本任务提交。
- `frontend-feature-delivery` evidence validator 已 PASS。

## 适用经验门禁

- 前端静态契约隔离门禁：新增任务专用静态合同覆盖当前弹框行为，避免被既有大契约或历史失败阻塞。
- Windows 换行与脚本行为同步：静态合同读取 Vue SFC 时按 UTF-8 文本和结构性断言，不依赖脆弱换行。
- 技能证据文件清理前归档门禁：`frontend-feature-evidence.md` 通过 validator 后，将关键 PASS 结论复制到 `execution-log.md` / `verification-report.md`。
- 共享分支并发基线提交门禁：记录非本任务基线/并发残余，后续只选择性暂存本任务文件。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，将新增人员表单从页面主内容迁移到显式弹框入口，不改变接口契约。
- `是否存在临时补丁或绕过`：否。
