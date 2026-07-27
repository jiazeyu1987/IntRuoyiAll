# Execution Log

## Intent

用户要求按“表单模板必须按批记录表单的 3 个按钮行为对齐”进行文档设计。

## Evidence Reviewed

- `IntRuoyiFronted/src/views/form-center/template/index.vue`：表单模板预览区按钮当前绑定 `openSelectedTemplate`、`editSelectedTemplate`、`openSelectedTemplateFill`。
- `IntRuoyiFronted/src/views/mes/pro/batchrecordformlist/index.vue`：批记录表单按钮当前绑定 `openDesigner(reportId, 'preview')`、`openDesigner(reportId, 'edit')`、`openSimulate(row)`。
- `IntRuoyiFronted/src/api/mes/pro/batchrecordreport/index.ts`：批记录设计器路径、编辑路径、单元格规则均以 `reportId` 为核心参数。
- `IntRuoyiBackend/yudao-module-bpm/.../FormCenterTemplateRespVO.java`：表单模板响应当前没有 `reportId` 字段。
- `IntRuoyiBackend/yudao-module-bpm/.../FormCenterRuntimeServiceImpl.java`：模板池从 `bpm_form_template_version` 转换为响应对象。
- `IntRuoyiBackend/yudao-module-mes/.../MesProBatchRecordReportServiceImpl.java`：批记录分页已支持 `reportId` 精确过滤。

## BDD Scenarios

- `BDD: 表单模板打开按钮对齐批记录打开 -> Given 表单模板行已绑定批记录 reportId / When 用户点击表单模板预览区“打开” / Then 前端进入批记录表单同源预览路径并请求 designer-path，不再打开 TemplateViewDialog。`
- `BDD: 表单模板编辑按钮对齐批记录编辑 -> Given 表单模板行已绑定批记录 reportId / When 用户点击“编辑” / Then 前端进入批记录设计器编辑路径并请求 edit-path，不再打开本页规则编辑弹窗或保存 form-center jimu-schema。`
- `BDD: 表单模板填写按钮对齐批记录填写 -> Given 表单模板行已绑定批记录 reportId / When 用户点击“填写” / Then 前端跳转批记录模板模拟填写页，并携带 reportId/reportName/batchRecordName/returnTo。`
- `BDD: 缺少 reportId 必须 fail fast -> Given 表单模板行没有稳定批记录 reportId / When 用户点击打开、编辑或填写 / Then 页面显示明确阻塞原因，不调用旧弹窗、不猜测报表、不返回默认成功。`

## RED / GREEN Notes

- `RED: node tests\e2e\form-template-batch-record-button-alignment-static.spec.js -> FAIL, FormTemplateListItemVO 缺少 batchRecordReportId 等显式绑定字段，三按钮仍使用旧弹窗/本页编辑/本页模拟填写。`
- `RED: python -m pytest script\tests\test_form_template_batch_record_binding_sql.py -> FAIL, 缺少 IntRuoyiBackend/sql/mysql/20260727_bpm_form_template_batch_record_binding.sql。`
- `RED: mvn.cmd -pl yudao-module-bpm "-Dtest=FormCenterTemplateBatchRecordBindingContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, FormCenterTemplateRespVO/FormTemplateVersionDO 缺少批记录绑定字段，runtime 未映射。`
- `GREEN: documentation-structure -> PASS, 任务级设计文档已覆盖前端、后端 API、数据模型、配置安全部署四个维度。`
- `GREEN: python -X utf8 section-check -> PASS, 任务级设计文档 UTF-8 可读，四份设计文档均包含系统设计必备章节。`
- `GREEN: git diff --check -- doc\tasks\20260727-form-template-button-alignment-design -> PASS, 任务文档无 diff 空白错误。`
- `GREEN: python -m pytest script\tests\test_form_template_batch_record_binding_sql.py -> PASS, SQL 迁移契约 3 项通过。`
- `GREEN: node tests\e2e\form-template-batch-record-button-alignment-static.spec.js -> PASS, 表单模板三按钮静态合同通过。`
- `GREEN: mvn.cmd -pl yudao-module-bpm "-Dtest=FormCenterTemplateBatchRecordBindingContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, BPM 合同测试 3 项通过。`
- `GREEN: pnpm ts:check -> PASS, 前端 relaxed TypeScript 检查通过。`
- `GREEN: docker information_schema columns -> PASS, 本地 Docker MySQL 的 bpm_form_template_version 已包含 7 个 batch_record_* 新增字段。`
- `GREEN: docker information_schema statistics -> PASS, 本地 Docker MySQL 已存在 idx_bpm_form_template_batch_record_report(tenant_id,batch_record_report_id,deleted)。`
- `REGRESSION: real-login template-pool probe -> FAIL, /mdm/form-center/template 页面入口可访问且 /form-center/template-pool 返回 code=0、rowCount=5，但当前 48081 原始 jar 响应首行不包含 batchRecord* 绑定字段，boundRowCount=0。`
- `REGRESSION: clean HEAD yudao-server package -> FAIL, 从 git archive HEAD 创建的干净快照执行 mvn.cmd -pl yudao-server -am "-DskipTests" package，在 yudao-module-mes 编译失败：MesProBatchRecordExecutionFieldAuditServiceImpl 缺少 currentUserId/goldenFingerMode。`
- `REGRESSION: BPM-only patched runtime jar -> FAIL, 仅替换 yudao-module-bpm 后启动失败，旧 CRM 模块仍引用 BpmProcessInstanceStatusEventListener。`
- `REGRESSION: clean-built-module patched runtime jar -> FAIL, 替换干净快照中已编译的 24 个模块后启动失败，旧 MES 模块仍引用 ApprovalTaskProvider。`
- `GREEN: restore original backend runtime -> PASS, 已恢复 E:\IntRuoyi\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar，48081 health=UP，PID=67500。`
- `GREEN: clean snapshot plus MES compile fix yudao-server package -> PASS, 临时构建快照仅补 MesProBatchRecordExecutionFieldAuditServiceImpl 中 currentUserId/goldenFingerMode 作用域编译修复后，mvn.cmd -pl yudao-server -am "-DskipTests" package 成功。`
- `GREEN: runtime jar startup -> PASS, 48081 已启动 E:\IntRuoyi\.runtime\form-template-button-alignment-20260727113740\yudao-server-exec-clean-snapshot-plus-mes-compile-fix.jar，SHA256=3F1A0FF05DF94A1D2E67C9A3F2B5F038BADD484F8EBB37FC00F063BC08C73487，health=UP，PID=10456。`
- `GREEN: real-login template-pool probe -> PASS, /mdm/form-center/template 页面入口可访问，/form-center/template-pool 返回新增 batchRecord* 字段。`
- `GREEN: real E2E form template 3 buttons -> PASS, 临时绑定模板行 id=29 到 reportId=45144f68db034fb9bbd01179c7cee59b 后，页面点击 打开/编辑/填写 分别进入 preview designer、edit designer、template-simulate 路由。`
- `GREEN: fixture restore -> PASS, bpm_form_template_version id=29 的 batch_record_report_id 与 batch_record_binding_status 已恢复为 NULL。`
- `GREEN: frontend/backend/database evidence validators -> PASS, 三份技能证据文档校验通过。`
- `GREEN: python -X utf8 docs read -> PASS, 任务目录 Markdown 均可 UTF-8 读取。`
- `GREEN: task-owned trailing whitespace scan -> PASS, 本任务新增/修改文件均无尾随空白。`
- `GREEN: project-experience-consolidation -> PASS, 已核对现有 docs/frontend-development.md、docs/e2e-rules.md、docs/local-runtime.md、docs/powershell-memory.md；本次无新增长期经验归档，现有门禁已覆盖无关 ts:check 阻塞和旧 jar 运行态核对。`
- `GREEN: temporary runtime cleanup -> PASS, PID 10456 已停止且无进程引用后，删除 E:\IntRuoyi\.runtime\form-template-button-alignment-20260727113740。`
- `GREEN: temporary build snapshot cleanup -> PASS, 路径解析为 D:\IntRuoyiWorktree\form-template-button-build-20260727、位于授权根目录、无 .git 元数据且未登记为 worktree，删除后路径不存在。`
- `GREEN: task-closeout-cleanup preview -> PASS, 保留任务目录 10 份设计/证据文档，delete/blocked/warnings 均为 none。`
- `GREEN: task-closeout-cleanup apply -> PASS, deleted_paths 为 none。`
- `REGRESSION: current worktree button alignment contract -> FAIL, node 聚焦断言返回 “FAIL buttons not aligned: 打开,编辑,填写”；当前工作区三按钮均不再进入批记录同源路径。`
- `BLOCKER: concurrent same-file behavior conflict -> IntRuoyiFronted/src/views/form-center/template/index.vue 与对应静态合同已被并行任务反向修改并暂存；未取得行为优先级确认前不得覆盖。`
- `USER CONFIRMATION: 2026-07-27 -> 用户明确回复“三个按钮按批记录表单执行”，解除同文件行为优先级冲突。`
- `RED: node tests\e2e\form-template-batch-record-button-alignment-static.spec.js -> FAIL, expected reason: 当前实现仍将 打开/编辑/填写 保留在 FormCenter 本页流程，未复用批记录路径。`
- `GREEN: node tests\e2e\form-template-batch-record-button-alignment-static.spec.js -> PASS, 三按钮恢复批记录设计器 preview/edit 和 template-simulate 路径。`
- `GREEN: pnpm ts:check -> PASS, 前端 relaxed TypeScript 检查通过。`
- `GREEN: python -m pytest script\tests\test_form_template_batch_record_binding_sql.py -> PASS, SQL 迁移契约 3 项通过。`
- `GREEN: mvn.cmd -pl yudao-module-bpm "-Dtest=FormCenterTemplateBatchRecordBindingContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, BPM 合同测试 3 项通过。`
- `GREEN: docs/frontend-development gate correction -> PASS, 项目级门禁已改回用户确认后的三按钮批记录对齐口径，避免后续再反向修复。`
- `GREEN: real E2E after user confirmation -> PASS, 本机 8081/48081 使用 芋道源码/admin 登录，临时绑定模板版本 id=29 到 reportId=2ef53e1302bd47bdba9ccbb87cd92032，真实点击 打开/编辑/填写 后分别进入批记录 preview designer、edit designer、template-simulate 路由。`
- `GREEN: fixture restore after confirmation -> PASS, bpm_form_template_version.id=29 的 batch_record_report_id 与 batch_record_binding_status 已恢复为 NULL。`
- `GREEN: project-experience-consolidation before commit -> PASS, 现有 docs/frontend-development.md 已承载表单模板三按钮与批记录绑定动作边界门禁，docs/experience-index.md 可按关键词定位，无需新建长期经验文档。`
- `GREEN: branch runtime port guard before implementation commit -> PASS, int_main 使用 frontend 8081 / backend 48081。`
- `GREEN: implementation commit -> PASS, commit 3f79f736251dab6be9d0413eea602a4ee1990fa6，仅包含 IntRuoyiFronted/src/views/form-center/template/index.vue、IntRuoyiFronted/tests/e2e/form-template-batch-record-button-alignment-static.spec.js、docs/frontend-development.md。`
- `GREEN: closeout commit -> PASS, commit 67631b4a，仅包含本任务 task.md、execution-log.md、frontend-feature-evidence.md、verification-report.md。`
- `GREEN: push origin/int_main -> PASS, origin/int_main 已包含 3f79f736 与 67631b4a，推送后分支不再 ahead。`
- `REGRESSION: local login/API response -> FAIL, 旧 48081 进程 health 虽为 UP，但多数请求线程阻塞在 Logback OutputStreamAppender 锁，登录和业务接口持续无响应。`
- `RED: backend restart without inherited runtime environment -> FAIL, DCC electronic signature evidence configuration is missing；按 fail-fast 处理，未修改配置或降级绕过。`
- `GREEN: backend restart with required inherited environment -> PASS, 48081 当前监听 PID=54560，命令行指向 E:\IntRuoyi\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar，登录预检和业务请求恢复。`
- `GREEN: fresh automated verification -> PASS, 表单模板三按钮静态合同、pnpm ts:check、SQL pytest 3 项、BPM Maven 合同 3 项、前端/后端/数据库 evidence validators 均通过。`
- `GREEN: fresh schema verification -> PASS, bpm_form_template_version 的 7 个 batch_record_* 字段及 idx_bpm_form_template_batch_record_report 复合索引均存在。`
- `GREEN: fresh real E2E form template 3 buttons -> PASS, 本机 8081/48081 使用 芋道源码/admin，模板行 id=29、模板 28/V2.0、reportId=2ef53e1302bd47bdba9ccbb87cd92032；真实点击 打开/编辑/填写 分别进入 preview designer、edit designer、template-simulate。`
- `GREEN: unbound template fail-fast -> PASS, 清空绑定后点击“打开”显示“当前模板未绑定批记录表单”且页面未跳转，不回退旧弹窗或猜测 reportId。`
- `GREEN: fixture restore all binding fields -> PASS, 模板版本 id=29 的 7 个批记录绑定字段均恢复为 NULL。`
- `BLOCKER: supplemental runtime cleanup -> 48081 共享后端 PID=54560 正在占用 backend-with-env.stdout.log/backend-with-env.stderr.log，codex-test-runner PID=53624 与后端存在活动连接；为避免中断并行任务，未停止进程，剩余两份运行日志待共享运行态空闲后删除。`
- `GREEN: project-experience-consolidation refresh -> PASS, 已将 health UP 但 API 因 OutputStreamAppender 锁挂起、长运行日志不得放入 task cleanup 目录等门禁合并到 docs/local-runtime.md，并更新现有经验索引。`
- `GREEN: supplemental cleanup preview -> PASS, 仅计划删除未锁定的 backend.stderr.log/backend.stdout.log，任务核心文档全部保留，blocked/warnings 均为空。`
- `GREEN: supplemental cleanup apply -> PASS, 已删除 backend.stderr.log/backend.stdout.log；一次性 form-template-buttons-real-e2e.mjs 已删除，未操作共享后端占用的两份空日志。`

## Milestone Updates

- 现状核对完成：确认三按钮当前不一致。
- 根因定位完成：表单模板响应缺少稳定 `reportId`，不能安全直接复用批记录按钮链路。
- 设计完成：要求后端先暴露正式映射，前端再复用批记录三按钮路由与接口。
- 实现完成：`bpm_form_template_version` 新增显式绑定摘要字段，模板池响应映射这些字段；表单模板 `打开 / 编辑 / 填写` 改为使用 `batchRecordReportId` 进入批记录表单设计器预览、编辑和模板模拟填写页。
- 文档校准完成：前端、后端 API、数据模型、配置安全部署设计文档已改为已落地方案，去除“推荐映射表/独立详情接口/实现前 blocker”的旧口径。
- 本地 schema 核对完成：Docker MySQL 已应用新增字段和索引；目标环境仍需走 release migration 流程。
- 前端回归完成：任务静态合同和全量 `pnpm ts:check` 均已通过。
- 运行态核对完成：48081 已加载包含新增模板池字段的完整 server jar，真实页面三按钮点击 E2E 通过，临时本地夹具已恢复。
- 临时产物清理完成：本任务专用运行态目录与临时构建快照均已删除，标准 cleanup preview/apply 通过。
- 冲突解除：用户明确确认三个按钮按批记录表单执行，已恢复 Vue 实现、静态合同和项目级门禁。
- 真实路径复验完成：三按钮按批记录表单行为执行，临时数据库夹具已恢复。
- 提交推送完成：实现与收尾记录分离提交，`origin/int_main` 已同步。
- 最新复验完成：自动化、schema、真实三按钮点击、未绑定失败行为和七字段夹具恢复均通过。
- 补充清理进行中：一次性 E2E 脚本已删除，运行态未锁定日志可清理；共享后端占用的两份运行日志暂不强删。

## Blockers

- 功能与验证无 blocker。仅补充收尾清理受共享 `int_main` 后端日志句柄阻塞；停止 PID `54560` 会中断 `codex-test-runner` 的活动连接，因此当前不执行。

## Final Status

- `ready_for_closeout`
