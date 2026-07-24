# 执行日志：运行控制台增加运维按钮和日志弹窗

BDD: 运维按钮可见且权限受控 -> Given 运维人员打开运行控制台, When 页面加载成功, Then 可看到发布测试服、提升正式服、立即备份、回滚版本、恢复数据、查看日志入口，且无权限时按钮不可执行。

BDD: 高风险动作需要确认 -> Given 运维人员点击提升正式服、回滚版本或恢复数据, When 未填写原因或 PROD 确认, Then 前端阻止提交并展示明确提示；When 填写完整, Then 调用后端运维动作 API。

BDD: 操作审计可追踪 -> Given 运维动作提交后, When 后端返回 operationId, Then 最近操作表显示动作、状态、摘要和日志入口。

BDD: 日志弹窗可刷新 -> Given 运维动作存在日志路径, When 点击查看日志, Then 弹窗展示后端返回的 tail 内容，可手动刷新并显示读取错误。

RED: `node tests\e2e\runtime-control-ops-static.spec.js` -> FAIL, expected missing `/infra/runtime-control/actions` API client and operation buttons/log dialog wiring.

RED: `node tests\e2e\runtime-control-ops-static.spec.js` -> FAIL, expected recent operation time formatter is missing and the table can display a raw millisecond timestamp.

GREEN: `node tests\e2e\runtime-control-ops-static.spec.js` -> PASS, operation action API client, buttons, confirmation dialog and log dialog contracts are wired.

GREEN: `node tests\e2e\runtime-control-static.spec.js` -> PASS, existing runtime-control frontend API and production guard contracts remain wired.

GREEN: `node tests\e2e\runtime-control-ops-static.spec.js` -> PASS, operation action buttons, log dialog contracts, and operation time formatter are wired.

GREEN: Playwright real page path `http://127.0.0.1:8087/infra/monitors/runtime-control` -> PASS, operation buttons render enabled for admin after local permission SQL, high-risk `提升正式服` requires reason and `PROD`, local restart refreshes the operation table, and `查看日志` opens a real log dialog.

BLOCKED: `pnpm ts:check` -> FAIL, Node heap out of memory in the current workspace; rerun with `NODE_OPTIONS=--max-old-space-size=8192` also fails with heap out of memory, so full TypeScript verification is not available in this session.

GREEN: `pnpm ts:check` with `NODE_OPTIONS=--max-old-space-size=10240` after stopping this task's previous frontend dev server -> PASS, full relaxed TypeScript verification completes.

GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260525-runtime-control-ops-console\frontend-feature-evidence.md` -> PASS, frontend evidence file contains required Feature, Acceptance, BDD, RED, GREEN, Verification and Blockers markers.

BLOCKED: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260525-runtime-control-ops-console --mode preview` -> BLOCKED, closeout preview found no checked-out worktree for frontend main branch `master`; no cleanup, merge, or commit was performed.

GREEN: Playwright real page path `http://127.0.0.1:8087/infra/monitors/runtime-control` -> PASS, current worktree frontend and backend show enabled 发布测试服/提升正式服/立即备份/回滚版本/恢复数据 buttons, invalid 提升正式服 submit sends 0 action POST requests, and 查看日志 opens operation `7e8e34eb-eb06-4556-b284-49840aa80c2c`.
