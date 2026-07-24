# 任务：运行控制台发布范围选项前端

## 任务目标

- 在运行控制台 `发布测试服` 和 `提升正式服` 弹窗中增加“发布范围”选项。
- 默认选择 `只发代码`，可显式切换到 `带数据发布`。
- 操作审计表展示发布范围，帮助运维人员区分代码发布和带数据发布。

## 非目标

- 不重做运行控制台整体 UI。
- 不给备份、回滚、恢复数据动作增加发布范围。
- 不执行真实发布动作。

## 前置任务检查

- 当前 worktree 分支：`task/20260525-runtime-control-ops-console`。
- 前一任务 `20260525-runtime-control-ops-console` 状态为 `completed`。

## 里程碑

- [x] M1：建立任务文档和 BDD 场景。
- [x] M2：补齐前端 RED 静态契约测试。
- [x] M3：实现 API 类型、弹窗发布范围单选、默认值和风险提示。
- [x] M4：最近操作表增加范围展示。
- [x] M5：运行前端静态、类型和真实页面验证。
- [x] M6：closeout 预览并提交前端改动。

## BDD 场景

- BDD: 发布动作默认只发代码 -> Given 运维人员打开发布测试服或提升正式服弹窗, When 弹窗出现, Then 发布范围默认选中 `只发代码`。
- BDD: 发布动作可选择带数据 -> Given 运维人员打开发布弹窗, When 选择 `带数据发布`, Then 前端提交 `publishScope=with-data` 并显示覆盖数据库和文件对象的风险提示。
- BDD: 非发布动作不显示发布范围 -> Given 运维人员打开备份、回滚或恢复数据弹窗, When 弹窗出现, Then 不显示发布范围控件。
- BDD: 操作审计展示发布范围 -> Given 最近操作包含 `parameters.publishScope`, When 表格渲染, Then 范围列显示 `只发代码` 或 `带数据发布`。

## 预期验证

- RED：`node tests\e2e\runtime-control-ops-static.spec.js` 先失败，证明前端缺少 publishScope 类型和 UI 契约。
- GREEN：同一静态契约通过。
- GREEN：`pnpm ts:check` 通过。
- GREEN：Playwright 真实页面路径验证两个发布弹窗默认只发代码，提升正式服未输入 `PROD` 不发动作请求。
- GREEN：`python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260525-runtime-control-publish-scope\frontend-feature-evidence.md` 通过。

## 当前状态

- 状态：completed
- 已完成：
  - 已建立任务文档和 BDD 场景。
  - 已补齐前端静态契约 RED/GREEN 覆盖。
  - 已实现 API 类型、发布范围单选默认值与带数据风险提示。
  - 已在最近操作表增加范围展示。
  - 已完成类型检查和真实页面路径验证。
  - 已运行 task-closeout-cleanup 预览；预览因主 worktree 检测阻止 apply，未执行清理应用。
- 阻塞与影响：
  - task-closeout-cleanup apply 阶段未执行：未检测到 `master` 主 worktree；这只影响自动清理/合并 worktree，不影响本任务代码交付。
