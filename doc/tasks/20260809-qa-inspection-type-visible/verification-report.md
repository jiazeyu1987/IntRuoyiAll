# Verification Report

## Result

PASS：QA 规程“检验项目”表默认显示“适用检验类型”，历史用户的旧列配置通过统一升级后的 `mes.qa.regulation.items.processMethods.v2` 与新默认布局隔离。

## Automated Verification

- `node tests/e2e/qa-regulation-applicable-types-default-visible-static.spec.js` -> PASS。
- `node tests/e2e/qa-regulation-display-fields-titlebar-static.spec.js` -> PASS。
- `node tests/e2e/role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS。
- `node tests/e2e/qa-regulation-id-balloon-pressure-pump-pdf-items-static.spec.cjs` -> PASS。
- `pnpm ts:check` -> PASS。
- `git diff --check -- <task-owned tracked files>` -> PASS，无空白错误。
- `validate_frontend_feature.py --evidence .../frontend-feature-evidence.md` -> PASS。
- `validate_frontend_feature.py --self-test` -> PASS。
- 收尾清理 preview -> PASS；删除范围仅包含本任务临时证据、一次性浏览器脚本、结果和截图，保留正式源代码、正式回归测试及三份核心任务文档。

## Real Browser Verification

- 运行态：`int_main` 前端 `http://127.0.0.1:8081` HTTP 200；后端 `http://127.0.0.1:48081/actuator/health` 为 `UP`；两端进程均归属 `E:\IntRuoyi`。
- 浏览器：本机稳定 Chrome，通过项目已安装的 Playwright 启动。
- 身份与范围：`芋道源码/admin`，只读验证，不保存、不发布、不修改列设置。
- 路径：`/mes/pro/process-pool/qa-regulation`，精确选择 `ID / 球囊扩张压力泵 / 112`，进入“检验项目”。
- 页面结果：可见表头第 3 列为“适用检验类型”；首行显示“首检 + 3”；可见数据行 17。
- 安全与稳定性：后台写请求 0，目标请求失败 0，console error 0，pageerror 0。

## Scope Confirmation

- 未修改 QA 规程保存/发布 API、正式检验类型数据或 PQC 首检卡片生成规则。
- 保留“显示字段”个性化设置和 `applicableTypes` 多选编辑控件。
- 未修改或清理无关并发任务文件、运行态或数据。

## Blockers

- 无。

## Closeout

- `task-closeout-cleanup` preview/apply 均 PASS，无 blocked/warnings。
- 已删除本任务临时 `frontend-feature-evidence.md`、一次性 Playwright 脚本和 `output/playwright/20260809-qa-inspection-type-visible/`。
- 保留 `task.md`、`execution-log.md`、`verification-report.md`、生产代码和正式静态回归测试。
- 长期经验去重确认完成，现有前端默认列布局与 Element Plus 精确选择门禁已覆盖本次经验，无需新增长期文档。
- 未执行 Git 提交、合并或推送。
