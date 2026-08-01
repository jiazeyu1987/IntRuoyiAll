# 20260802 DCC 上传升版 E2E Execution Log

## User Intent

用户要求使用 5 个非 admin 账号，通过真实 E2E 验证 DCC 文控上传 + 升版完整业务链路。账号密码仅用于本机执行，不记录明文。

## Gate Setup

- 已按任务要求创建任务目录：`doc/tasks/20260802-dcc-upload-revision-e2e/`
- 已读取 `docs/experience-index.md`，命中 DCC 上传类别权限、Element Plus 上传控件、int_main 运行态 URL、Playwright 浏览器、OnlyOffice 容器链路门禁，并同步到 `task.md`。
- BDD: V1 上传发布 -> Given 上传账号具有目标分类上传权限 When 上传账号通过前端提交 V1.0 新文档并四个审批账号依次审批 Then V1.0 成为该文件编号的当前生效版本。
- BDD: V2 升版发布 -> Given 同一文件编号已有 V1.0 当前生效版本 When 上传账号通过前端选择升版并提交 V2.0 且四个审批账号依次审批 Then 当前生效版本切换到 V2.0 且 V2.0 标记为升版。

## Command Intent Log

- 运行态预检：本机前端 `http://127.0.0.1:8081/` HTTP 200，本机后端 `http://127.0.0.1:48081/actuator/health` UP，OnlyOffice 容器可访问后端 health，本机 Chrome 和 `npx` 可用。
- 权限定位：审批中心 `POST /approval-center/tasks/review` 要求 `bpm:task:update`；四个审批账号原角色能查待办但缺更新权限。
- 权限补齐：按用户授权给非 admin 的 `审批中心入口` 角色 `910295` 增加 `bpm:task:update` 菜单绑定，新增 `system_role_menu.id=912074`，未使用 admin 账号作为业务角色。
- Playwright E2E：多次通过真实上传页创建 V1.0 任务自有文档；直接审批中心 BPM 行审批仍 403。
- DCC 正式入口核对：改脚本尝试进入 DCC 详情审批卡片；发现 `DccControlledFileDetail` 无 `viewer=1` 被路由守卫跳回受控浏览，有 `viewer=1` 时仅只读 viewer，不渲染审批卡片。
- 只读 DB 核验：当前任务自有文件编号前缀下共有 4 条 V1.0 残留，均停留在 `PENDING_DOC_CONTROL_REVIEW`，当前 Flowable 任务分配给首审账号 `zhaohaichen`。

## Milestone Status

- M1 运行态和数据前置：completed
- M2 V1.0 上传提交：completed
- M3 V1.0 审批发布：blocked
- M4 V2.0 升版提交：blocked
- M5 V2.0 审批发布：blocked
- M6 只读核验与报告：completed_blocked

## Verification Evidence

- `node --check doc/tasks/20260802-dcc-upload-revision-e2e/dcc-upload-revision-e2e.cjs`：PASS。
- 登录权限复查：`zhaohaichen` 登录后的权限响应包含 `bpm:task:query`、`bpm:task:update`、`dcc:controlled-file:review`、`dcc:controlled-file:approve`。
- 真实上传结果：最新运行 `CODX-DCC-REV-20260802-20260801174426` 创建 V1.0，`controlled_file_id=2054545668044070241`，状态 `PENDING_DOC_CONTROL_REVIEW`。
- 前端入口诊断：登录 `zhaohaichen` 后访问 `/dcc/controlled-file/detail/2054545668044070241` 实际落到 `/dcc/controlled-file/browser`，页面显示受控浏览而非审批详情。
- 代码证据：`IntRuoyiFronted/src/router/modules/remaining.ts` 的 `DccControlledFileDetail.beforeEnter` 在缺少 `viewer=1` 时返回 `DccControlledFileBrowser`；`IntRuoyiFronted/src/views/dcc/controlled-file/detail/index.vue` 的审批阶段和 `approvalTodoTask` 卡片位于 `viewerMode` 的 `v-else` 分支。
- QA 报告校验：`python C:\Users\BJB110\.codex\skills\quality-assurance-test-suite\scripts\validate_quality_assurance.py --evidence doc/tasks/20260802-dcc-upload-revision-e2e/verification-report.md` -> PASS。
- 敏感信息扫描：按本任务约定的密码、授权头和刷新令牌关键字扫描任务目录 -> 无命中。
- 经验沉淀：新增 `docs/e2e-rules.md#dcc-文控审批处理入口门禁`，并在 `docs/experience-index.md` 添加关键词路由；`git diff --check -- ...` -> PASS，仅提示 Git 换行转换 warning。

## Blockers

- BLOCKED: DCC 文控审批缺少可完成电子签名审批的真实前端入口。审批中心 DCC 行只能进入只读 viewer；非 viewer 详情被路由守卫跳回受控浏览。按项目 E2E 规则，不能用 API-only 替代 V1/V2 审批页面路径，因此完整上传 + 升版链路未通过。
- Residue: 本任务创建的 `CODX-DCC-REV-20260802-*` 四条 V1.0 测试文档均处于待文控审核状态，未静默删除或直接 SQL 清理。
