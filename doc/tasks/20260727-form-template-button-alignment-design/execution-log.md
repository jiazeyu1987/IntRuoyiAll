# Execution Log

## Intent

用户最初要求三个按钮按批记录表单行为对齐，随后明确纠正：实际表单模板与批记录表单没有直接关系，三个按钮提示“当前模板未绑定批记录表单”是缺陷。最终要求以本次纠正为准，三个按钮执行当前 FormCenter 模板自身操作。

## Evidence Reviewed

- `IntRuoyiFronted/src/views/form-center/template/index.vue`：表单模板预览区按钮当前绑定 `openSelectedTemplate`、`editSelectedTemplate`、`openSelectedTemplateFill`。
- `IntRuoyiFronted/src/views/mes/pro/batchrecordformlist/index.vue`：批记录表单按钮当前绑定 `openDesigner(reportId, 'preview')`、`openDesigner(reportId, 'edit')`、`openSimulate(row)`。
- `IntRuoyiFronted/src/api/mes/pro/batchrecordreport/index.ts`：批记录设计器路径、编辑路径、单元格规则均以 `reportId` 为核心参数。
- `IntRuoyiBackend/yudao-module-bpm/.../FormCenterTemplateRespVO.java`：表单模板响应当前没有 `reportId` 字段。
- `IntRuoyiBackend/yudao-module-bpm/.../FormCenterRuntimeServiceImpl.java`：模板池从 `bpm_form_template_version` 转换为响应对象。
- `IntRuoyiBackend/yudao-module-mes/.../MesProBatchRecordReportServiceImpl.java`：批记录分页已支持 `reportId` 精确过滤。

## Current BDD Scenarios

- `BDD: 未绑定批记录的表单模板可以打开 -> Given 当前表单模板存在且没有任何批记录绑定 / When 用户点击“打开” / Then 打开 TemplateViewDialog 展示当前模板，不显示批记录绑定错误、不跳转 MES 页面。`
- `BDD: 表单模板编辑使用自身规则工作区 -> Given 当前模板允许交互操作 / When 用户点击“编辑” / Then 通过 openSelectedTemplateAction('edit') 打开当前模板规则编辑工作区并使用 templateId/versionNo。`
- `BDD: 表单模板填写使用自身模拟工作区 -> Given 当前模板允许交互操作 / When 用户点击“填写” / Then 重置当前模板模拟值并打开 fillDialogVisible，不要求 reportId、不跳转批记录模拟填写路由。`
- `BDD: FormCenter 模板池不暴露批记录绑定契约 -> Given 表单模板与批记录表单无直接关系 / When 查询模板池 / Then VO、DO、运行态映射和前端类型均不包含 batchRecordBinding* 字段。`
- `BDD: 错误新增迁移停止进入发布 -> Given 批记录绑定迁移尚无发布引用 / When 完成本次纠偏 / Then 删除该迁移及旧专用测试，不执行已存在列的破坏性删除。`

## Superseded BDD Scenarios

- `BDD: 表单模板打开按钮对齐批记录打开 -> Given 表单模板行已绑定批记录 reportId / When 用户点击表单模板预览区“打开” / Then 前端进入批记录表单同源预览路径并请求 designer-path，不再打开 TemplateViewDialog。`
- `BDD: 表单模板编辑按钮对齐批记录编辑 -> Given 表单模板行已绑定批记录 reportId / When 用户点击“编辑” / Then 前端进入批记录设计器编辑路径并请求 edit-path，不再打开本页规则编辑弹窗或保存 form-center jimu-schema。`
- `BDD: 表单模板填写按钮对齐批记录填写 -> Given 表单模板行已绑定批记录 reportId / When 用户点击“填写” / Then 前端跳转批记录模板模拟填写页，并携带 reportId/reportName/batchRecordName/returnTo。`
- `BDD: 缺少 reportId 必须 fail fast -> Given 表单模板行没有稳定批记录 reportId / When 用户点击打开、编辑或填写 / Then 页面显示明确阻塞原因，不调用旧弹窗、不猜测报表、不返回默认成功。`

## RED / GREEN Notes

- `GREEN: change request validation -> PASS, docs/changes/20260727-form-template-buttons-independent-from-batch-record.md 已记录并通过校验。`
- `USER CORRECTION: 2026-07-27 -> 表单模板与批记录表单没有直接关系；三个按钮必须使用当前表单模板自身链路。`
- `RED: node tests\e2e\form-template-independent-button-actions-static.spec.js -> FAIL, “打开”仍调用 openSelectedTemplateDesigner('preview')，没有打开当前模板 TemplateViewDialog。`
- `RED: mvn.cmd -pl yudao-module-bpm "-Dtest=FormCenterTemplateIndependenceContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, 2 tests，BPM VO/DO 和 runtime 仍暴露并映射 batchRecordReportId 等字段。`
- `RED: python -X utf8 -m pytest script\tests\test_form_template_batch_record_independence.py -> FAIL, 2 tests，错误绑定迁移仍存在且 FormCenter 源码仍定义批记录绑定字段。`
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
- `GREEN: supplemental cleanup apply -> PASS, 已删除 backend.stderr.log/backend.stdout.log；一次性 form-template-buttons-real-e2e.mjs 已删除，未操作共享后端占用的两份运行日志。`
- `GREEN: supplemental evidence commit -> PASS, commit 3cf97ab2，仅包含本任务复验记录、一次性 E2E 脚本删除和本地运行态经验门禁。`
- `GREEN: supplemental evidence push -> PASS, origin/int_main 已包含 3cf97ab2，推送后分支不再 ahead。`

## Authoritative Correction Verification

以下记录覆盖此前“稳定 reportId 绑定”方案；此前实现、临时数据库绑定夹具和对应 E2E 仅作为已废弃历史证据保留，不代表最终行为。

- `GREEN: change request validator -> PASS, docs/changes/20260727-form-template-buttons-independent-from-batch-record.md 已确认最终范围。`
- `GREEN: node tests\e2e\form-template-independent-button-actions-static.spec.js -> PASS, 打开/编辑/填写使用当前模板查看、规则编辑和模拟填写工作区。`
- `GREEN: pnpm ts:check -> PASS, 前端 TypeScript 检查通过。`
- `GREEN: mvn.cmd -pl yudao-module-bpm "-Dtest=FormCenterTemplateIndependenceContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, 2 tests。`
- `GREEN: python -X utf8 -m pytest script\tests\test_form_template_batch_record_independence.py -> PASS, 2 tests。`
- `GREEN: local login preflight -> PASS, 入口 /mdm/form-center/template，身份标签 芋道源码/admin，本机 Chrome executablePath 可用。`
- `GREEN: real Playwright E2E -> PASS, 打开显示“查看表单模板”，编辑显示 .form-template-rules-dialog，填写显示 .form-template-fill-dialog。`
- `GREEN: real Playwright route boundary -> PASS, 三次点击 pathname 均保持 /mdm/form-center/template，未跳转 MES。`
- `GREEN: real Playwright error boundary -> PASS, 页面未出现“当前模板未绑定批记录表单”。`
- `GREEN: pnpm ts:check final rerun -> PASS, 退出码 0。`
- `GREEN: system design validator + self-test -> PASS。`
- `GREEN: frontend/backend/database/bug/change evidence validators -> PASS。`
- `GREEN: UTF-8 strict read -> PASS, 任务目录 11 个 Markdown 文件。`
- `GREEN: task-owned git diff --check -> PASS。`
- `GREEN: project-experience-consolidation -> PASS, 经验已合并到现有 docs/frontend-development.md，无需新建文档。`
- `GREEN: branch runtime port guard -> PASS, int_main frontend 8081 / backend 48081。`
- `GREEN: task-closeout-cleanup preview -> PASS, keep 11 files, delete/blocked/warnings none。`
- `GREEN: task-closeout-cleanup apply -> PASS, deleted_paths none。`
- `CONCURRENCY: implementation persistence -> 并行任务按脏工作区基线策略创建 698d6ba3，包含本任务实现、设计、测试以及其他并行改动；本任务未改写该提交历史。`
- `GREEN: supplemental closeout commit -> a6714535, 包含本任务 execution-log.md/verification-report.md 和并行收尾记录。`
- `GREEN: remote integration commit -> 97ecf51a, 共享 int_main 已整合 origin/int_main。`
- `GREEN: git push origin int_main -> PASS, 70a4b414..97ecf51a。`
- `REGRESSION: node tests\e2e\form-center-static.spec.js -> FAIL, 仅失败于无关策略路由 activeMenu 断言；本任务未修改该路由或断言。`

## Milestone Updates

- 变更纠偏完成：用户最终确认 FormCenter 模板与批记录表单没有直接关系。
- RED 完成：前端、BPM 和迁移独立性合同均稳定证明错误绑定实现。
- GREEN 完成：三个按钮恢复当前模板自身工作区，七个错误字段和错误迁移从代码/发布内容移除。
- 真实 E2E 完成：本机 Chrome 逐个点击三个按钮，弹窗、路由和错误边界均通过。
- 文档校准完成：四份设计文档、三份交付证据、缺陷证据和验证报告均改为最终独立行为。
- 项目经验门禁完成：`docs/frontend-development.md` 已记录“交互对齐不等于数据绑定”。
- Closeout 完成：cleanup、远端同步和最终任务记录均已完成。

## Blockers

- 当前功能无 blocker。
- 本地数据库冗余列的物理清理未获授权，需独立迁移审计；不影响本次按钮功能。
- 无关并行任务文件保持不动。

## Final Status

- `completed`
