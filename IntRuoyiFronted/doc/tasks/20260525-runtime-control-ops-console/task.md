# 任务：运行控制台增加运维按钮和日志弹窗

## 任务目标

- 在 `基础设施 / 监控中心 / 运行控制台` 增加按钮入口：发布测试服、提升正式服、立即备份、回滚版本、恢复数据、查看日志。
- 前端必须展示权限态、加载态、错误态、操作审计结果和实时日志弹窗。
- 高风险动作必须要求原因；正式环境相关动作必须要求输入 `PROD`，不得前端静默替用户确认。

## 非目标

- 不重做运行控制台整体视觉风格。
- 不新增 mock 数据或前端假成功。
- 不绕过后端权限、确认和脚本白名单。

## 前置任务检查

- 新 worktree 分支：`task/20260525-runtime-control-ops-console`。
- 最近前端任务 `20260525-login-default-yudao-restore` 状态为 `completed`。
- 已阅读 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`，本页面继续采用密集运维控制台风格。

## 里程碑

- [x] M1：创建独立 worktree，建立任务记录和 BDD 场景。
- [x] M2：补齐前端 RED 静态/交互契约测试，证明当前页面缺少运维按钮和日志弹窗。
- [x] M3：实现 API client、按钮区、确认弹窗、操作审计刷新和日志弹窗。
- [x] M4：运行前端静态/类型测试，记录 GREEN。
- [x] M5：用 Playwright 走真实页面路径验证按钮、确认、权限/错误和日志弹窗。
- [x] M6：执行 closeout 预览，提交本任务前端改动。

## BDD 场景

- BDD: 运维按钮可见且权限受控 -> Given 运维人员打开运行控制台, When 页面加载成功, Then 可看到发布测试服、提升正式服、立即备份、回滚版本、恢复数据、查看日志入口，且无权限时按钮不可执行。
- BDD: 高风险动作需要确认 -> Given 运维人员点击提升正式服、回滚版本或恢复数据, When 未填写原因或 PROD 确认, Then 前端阻止提交并展示明确提示；When 填写完整, Then 调用后端运维动作 API。
- BDD: 操作审计可追踪 -> Given 运维动作提交后, When 后端返回 operationId, Then 最近操作表显示动作、状态、摘要和日志入口。
- BDD: 日志弹窗可刷新 -> Given 运维动作存在日志路径, When 点击查看日志, Then 弹窗展示后端返回的 tail 内容，可手动刷新并显示读取错误。

## 预期验证

- RED：`node tests/e2e/runtime-control-ops-static.spec.js` 先失败，证明当前页面缺少按钮和日志弹窗。
- GREEN：同一静态契约通过。
- GREEN：`pnpm ts:check` 通过或记录既有阻塞。
- GREEN：Playwright 访问 `http://localhost:8081/infra/monitors/runtime-control`，验证按钮、确认弹窗和日志弹窗。
- GREEN：`python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260525-runtime-control-ops-console/frontend-feature-evidence.md` 通过。

## 当前状态

- 状态：completed
- 已完成：
  - 已创建独立前端 worktree。
  - 已建立任务文档和 BDD 场景。
  - 已记录前端 RED：缺少运维动作 API、按钮和日志弹窗契约。
  - 已实现运维动作按钮、确认弹窗、操作审计日志入口和日志自动刷新弹窗。
  - 已补齐最近操作时间格式化，避免页面直接展示毫秒时间戳。
  - 已通过 Playwright 真实页面路径验证按钮权限态、PROD 确认拦截、最近操作刷新和日志弹窗读取。
  - 已释放本任务前端 dev server 后重跑 `pnpm ts:check`，全量类型检查通过；随后重新启动 worktree 前端并完成真实页面验证。
- 阻塞与影响：
  - 无功能阻塞。
  - closeout 预览已执行；自动清理/合并曾因前端 `master` 主分支 worktree 未检出而阻塞，本任务未执行自动删除或自动合并。

## 最终验证

- GREEN: `node tests\e2e\runtime-control-static.spec.js` -> PASS.
- GREEN: `node tests\e2e\runtime-control-ops-static.spec.js` -> PASS.
- GREEN: `pnpm ts:check` with `NODE_OPTIONS=--max-old-space-size=10240` -> PASS.
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260525-runtime-control-ops-console\frontend-feature-evidence.md` -> PASS.
- GREEN: Playwright real page path -> PASS, enabled operation buttons, PROD guard, operation audit table and log dialog verified on `http://127.0.0.1:8087`.
