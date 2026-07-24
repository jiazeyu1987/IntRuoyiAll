# 执行日志：运行控制台发布范围选项前端

BDD: 发布动作默认只发代码 -> Given 运维人员打开发布测试服或提升正式服弹窗, When 弹窗出现, Then 发布范围默认选中 `只发代码`。

BDD: 发布动作可选择带数据 -> Given 运维人员打开发布弹窗, When 选择 `带数据发布`, Then 前端提交 `publishScope=with-data` 并显示覆盖数据库和文件对象的风险提示。

BDD: 非发布动作不显示发布范围 -> Given 运维人员打开备份、回滚或恢复数据弹窗, When 弹窗出现, Then 不显示发布范围控件。

BDD: 操作审计展示发布范围 -> Given 最近操作包含 `parameters.publishScope`, When 表格渲染, Then 范围列显示 `只发代码` 或 `带数据发布`。

RED: `node tests\e2e\runtime-control-ops-static.spec.js` -> FAIL, expected missing `publishScope` request type, publish scope radio options, default `code-only`, risk hint and table formatter.

GREEN: `node tests\e2e\runtime-control-ops-static.spec.js` -> PASS, runtime control operation buttons and log dialog contracts are wired.

GREEN: `pnpm ts:check` -> PASS.

GREEN: Playwright real page verification -> PASS, `发布测试服` and `提升正式服` dialogs show publish scope, default `只发代码`, with-data risk hint is visible, and `提升正式服` without `PROD` sends 0 `/infra/runtime-control/actions` requests.

GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260525-runtime-control-publish-scope\frontend-feature-evidence.md` -> PASS.

GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260525-runtime-control-publish-scope --mode preview` -> BLOCKED for apply gates, preview completed and listed only `frontend-feature-evidence.md` as task artifact delete candidate; no checked-out worktree for main branch `master` was found.
