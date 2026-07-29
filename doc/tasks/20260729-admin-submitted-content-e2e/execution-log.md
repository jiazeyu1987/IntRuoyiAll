# Execution Log

## 2026-07-29

- User intent: 对“批记录管理员在批次执行主区域查看其他账号提交后的当前内容”执行 E2E 验证。
- Read gates: `docs/task-closeout-rules.md`, `docs/e2e-rules.md`, `docs/login-access.md`, `docs/local-runtime.md`, `docs/worktree-restrictions.md`, `docs/powershell-encoding.md`, Playwright skill.
- Workspace state before task-owned edits: `int_main...origin/int_main` with unrelated dirty files in frontend tests and prior task docs; this task will not stage or modify unrelated files.
- BDD: 管理员看到已提交主区域内容 -> Given 填写账号在批次执行中提交了批记录单元格内容 / When 批记录管理员打开同一批次执行并查看主区域 / Then 主区域显示提交后的单元格值，不读取草稿，不触发写请求。
- BDD: 主区域不使用草稿 -> Given 同一执行记录存在草稿和已提交版本差异 / When 管理员打开主区域查看 / Then 显示已提交版本，草稿内容不应覆盖已提交内容。
- RED: `node doc/tasks/20260729-admin-submitted-content-e2e/admin-submitted-content-real.e2e.js` -> FAIL, MySQL `TO_BASE64(cell_values_json)` 输出被换行拆分，脚本无法解析已提交样本；这是验证脚本输入编码问题，不是产品页面断言失败。
- GREEN: `node --check doc/tasks/20260729-admin-submitted-content-e2e/admin-submitted-content-real.e2e.js` -> PASS，验证脚本改用 `HEX(cell_values_json)` 后语法通过。
- GREEN: `node tests/e2e/edhr-batch-admin-preview-runtime-fix-static.spec.js` -> PASS，主区域静态合同确认只读取已提交 execution review，不读取 `task/preview`。
- GREEN: `node scripts/preflight/login-preflight.mjs --base-url http://127.0.0.1:8081 ... --target-path /index` -> PASS，`芋道源码/admin` 真实登录通过，密码未写入日志。
- GREEN: `EDHR_ADMIN_SUBMITTED_VERIFY_MODE=current-unsubmitted node doc/tasks/20260729-admin-submitted-content-e2e/admin-submitted-content-real.e2e.js` -> PASS，目标 `workOrderCode=881MO090935`、`batchExecutionId=900000000909`、`taskId=7206`、`executionId=1589`、`executionStatus=0`；页面显示“暂无已提交批记录内容”，未渲染只读原表，未请求 `/task/preview`，未产生 MES 写请求。证据：`doc/tasks/20260729-admin-submitted-content-e2e/admin-submitted-content-e2e-output/admin-current-unsubmitted-main-area.json`。
- RED: `EDHR_ADMIN_SUBMITTED_VERIFY_MODE=submitted-content node doc/tasks/20260729-admin-submitted-content-e2e/admin-submitted-content-real.e2e.js` -> FAIL，历史已提交样本 `900000000709` 进入 `review-timeline` 后返回业务失败：`eDHR 批次执行缺少工艺流程批记录配置流程配置或默认批记录`。
- BLOCKED: 本地库没有当前可用于“其他账号已提交后管理员主区域显示内容”的有效样本；执行写入型闭环需要已确认的测试租户、非 admin 填写账号和签名密码，当前环境未提供。
- Experience consolidation: 已将“管理员主区域已提交内容 E2E 必须先确认当前 `review-timeline` 成功且样本是已提交态，草稿 cell_values_json 不可作为显示通过”的复用门禁合并到 `docs/e2e-rules.md#eDHR 管理员主区域已提交内容门禁`，并在 `docs/experience-index.md` 增加关键词路由。
- GREEN: UTF-8 文档读取检查 -> PASS，覆盖 task/report/log 与更新后的 `docs/e2e-rules.md`、`docs/experience-index.md`。
- GREEN: `git diff --check -- docs/e2e-rules.md docs/experience-index.md doc/tasks/20260729-admin-submitted-content-e2e` -> PASS，仅有 CRLF 工作区提示，无空白错误。
- Closeout: 当前任务状态保持 `blocked`；未提交/推送，因为完整提交后显示 E2E 缺少正式可写样本，且工作区仍存在非本任务并发脏改动。
- User provided write-test credential source: `测试租户/auteman`，password received in chat and intentionally not recorded in task files.
- RED: `node scripts/preflight/login-preflight.mjs --base-url http://127.0.0.1:8081 --tenant 测试租户 --username auteman --password [REDACTED] --target-path /index --timeout 90000` -> FAIL，真实登录接口返回 `登录失败，账号密码不正确`。
- Supporting read-only DB check: `system_tenant.name='测试租户'` exists and enabled as tenant `122`; `system_users` in tenant `122` lists enabled users such as `admin/limin/...` but no username `auteman` was found.
- BLOCKED remains: cannot execute the multi-user/write submission E2E until a valid enabled test-tenant account with required fill/sign permissions is available.
