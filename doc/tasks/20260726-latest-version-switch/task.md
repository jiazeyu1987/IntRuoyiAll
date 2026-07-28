# 20260726 Latest Version Switch

## Task Goal

将截图所示列表工具栏中的“批量删除”按钮改为“最新版本”开关；开启后只显示最新版本的表单。

## Milestones

- [x] 定位列表页面、筛选状态和接口参数契约。
- [x] 先补充最小静态契约测试，覆盖开关替换按钮和最新版本筛选行为。
- [x] 实现开关 UI 与筛选逻辑，不引入 fallback、吞异常或 mock 成功。
- [x] 运行相关前端和后端验证并记录结果。
- [x] 修复“最新版本”开关仍显示同产品旧定义旧版本的回归。
- [ ] 完成任务证据、清理和收尾记录。

## Expected Verification

- 任务专用静态契约先 RED 后 GREEN。
- 受影响前端范围测试或静态检查通过；若全量检查被既有问题阻塞，记录首个无关 blocker。
- 人工核对代码路径：开关选择后查询参数仅请求最新版本表单，关闭后恢复默认列表。

## Current Status

blocked_pending_git_closeout

## Design Constraints Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，计划复用页面既有查询状态和接口参数契约。
- `是否存在临时补丁或绕过`：否。

## Initial Workspace Note

- 开始时 `E:\IntRuoyi` 已存在大量未提交改动且分支领先 `origin/int_main`，本任务只修改当前需求相关文件和 `doc/tasks/20260726-latest-version-switch/` 下任务记录。

## Experience Gates

### 前端静态契约隔离门禁

- Trigger: 当前需求需要 RED/GREEN，但全量检查可能先失败在无关历史问题上。
- Preflight check: 新增任务专用最小静态契约 `IntRuoyiFronted/tests/e2e/batch-record-form-latest-version-switch-static.spec.js`。
- Blocker: 若任务专用契约不能稳定 RED/GREEN，不得宣称完成。
- Verification: 任务专用静态契约、相邻工具栏契约和 `pnpm ts:check` 已通过。
- Forbidden action: 禁止修改无关大契约或用无关 blocker 代替当前需求验证。
- Evidence: `docs/frontend-development.md#前端静态契约隔离门禁`。

### Maven Reactor 兄弟模块验证门禁

- Trigger: MES 模块依赖 sibling module，目标测试需确认依赖产物一致。
- Preflight check: 先用 `-pl yudao-module-mes -am` 复验；若无关 sibling testCompile 阻塞，记录 blocker 后隔离构建必要主产物。
- Blocker: 不能证明失败点与当前任务无关时，不得宣称当前任务通过。
- Verification: `mvn -pl yudao-module-bpm -am '-Dmaven.test.skip=true' install` 后，MES 目标 DB 测试通过。
- Forbidden action: 禁止把 sibling 测试编译问题误判为本任务失败，或跳过 MES 目标测试。
- Evidence: `docs/backend-development.md#2026-07-25 Maven Reactor 兄弟模块验证门禁`。

## Cleanup Keep

- doc/tasks/20260726-latest-version-switch/frontend-feature-evidence.md
- doc/tasks/20260726-latest-version-switch/bug-regression-evidence.md
