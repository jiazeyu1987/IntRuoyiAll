# 项目经验索引

任务启动顺序固定为：先创建或识别 `doc/tasks/<task-id>/` 并写入最小 `task.md`（目标、里程碑、预期验证、当前状态），再读本文件并只打开命中的经验文档，随后把适用门禁补入任务文档；不要全量阅读所有经验。唯一例外：PowerShell / Windows shell 任务可先执行只读 UTF-8 bootstrap 读取适用规则，bootstrap 完成后必须立即创建或更新任务文档。

## 经验文档写入规范（强制）

### 归档原则

- 先搜索并更新已有经验文档；不得把同类经验拆成多个重复入口。
- 可在行动前检查的问题，必须优先写入 preflight、checklist、memory 或正式流程文档；只有不能转成前置门禁的事后分析才写入 error-prevention。
- 一次性任务状态、临时数据、当前完成情况和长篇执行流水保留在 `doc/tasks/<task-id>/`，不得混入长期经验。
- 没有合适归宿且确需新建长期经验文档时，必须先取得用户明确授权。

### 门禁摘要模板

每条可执行经验必须先提供以下固定摘要，字段不得缺失：

```markdown
### <经验标题>

- Trigger: <哪些任务、关键词或现象会命中本经验>
- Preflight check: <执行风险动作前必须运行的精确检查>
- Blocker: <出现哪些结果必须立即停止>
- Verification: <证明通过的命令、字段、状态、日志或证据路径>
- Forbidden action: <禁止采用的绕过、降级、手工修补或跳过动作>
- Evidence: <来源任务、提交、releaseTag、operation id、文件或问题记录>
```

### 操作流程扩展模板

涉及构建、发布、服务器、数据库、真实 E2E、worktree、备份恢复或其他长链路操作时，在门禁摘要后按执行顺序补充：

1. `阶段 N：<阶段名称>`
2. `必查项`
3. `推荐命令`
4. `Fail Fast`
5. `必须记录`

每个问题必须记录：现象、阶段、影响、原因判断、处理动作、结果、是否可前置检查、是否可自动化、下次如何避免。

### 写入完成条件

- 在本索引增加可命中的任务关键词、错误文本、命令名、配置键或环境名路由。
- 若经验会阻塞实际流程，同步更新对应 checklist 或主流程入口。
- 在当前任务 `task.md` 摘取本次适用门禁，在 `execution-log.md` 记录验证证据。
- 使用 `rg` 验证新关键词能从索引定位到目标文档。
- 验证 UTF-8 编码和 `git diff --check`，只提交本任务产生的文件。
- 不得用模糊描述替代精确命令、字段、路径和 fail-fast 条件。

## 路由

- 经验沉淀 / 新增经验 / 更新经验 / 经验文件格式 / 前置经验 / 经验门禁模板：先执行本文件“经验文档写入规范（强制）”，再按下方关键词选择目标文档。
- 测试服发布 / 仅测试服 / current HEAD test release / publish-test / 运行态验收 / release-info 版本变更说明：`D:\ProjectPackage\Int\IntRuoyiMaintance\docs\test-release-preflight.md`
- publish-test preview 崩溃 / release package candidate scan / dead RUNNING operation / runtime-control JVM 退出：`docs/release-build-preflight-lessons.md#2026-07-11-runtime-control-发布候选扫描与-dead-operation-门禁`。
- PowerShell host:port 插值 / SSH IO pending / bash `$()` / Playwright 浏览器缓存 / release-info mojibake：`docs/powershell-preflight-lessons.md`，标题“2026-07-11 PowerShell/SSH/Playwright 发布验收门禁”。
- 仅测试服运行态验收 / release-info / 运行控制台版本变更说明 / Playwright 版本对话框：`docs/test-release-preflight.md#2026-07-11-仅测试服发布运行态验收新增门禁`。
- 维护控制台临时 worktree 构建 / pnpm approve-builds / ERR_PNPM_IGNORED_BUILDS：`D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-build-preflight-lessons.md`
- pnpm approve-builds 空结果但 install 仍因 ERR_PNPM_IGNORED_BUILDS 失败：`D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-build-preflight-lessons.md`

- PowerShell 命令编排 / 中文编码 / 终端输出预检：先读权威共同规则 `E:\IntRuoyi\docs\powershell-memory.md`，维护仓专项增量再读 `D:\ProjectPackage\Int\IntRuoyiMaintance\docs\powershell-preflight-lessons.md`
- Keywords: PowerShell 字面管道字符, TypeScript 类型联合, command string pipe, apply_patch ACL, 文件长度变 0, PowerShell 写入超时截断, Set-Content 写源码, 反引号 r n 字面量, import 追加非法字符 -> `docs/powershell-memory.md#powershell-命令文本管道字符门禁`
- Keywords: PowerShell Maven -D, -Dsurefire.failIfNoSpecifiedTests, -Dtest, Unknown lifecycle phase, 目标 JUnit 加引号 -> `docs/powershell-memory.md#powershell-maven--d-参数引号门禁`
- Keywords: 同文件并行改动, mixed hunks, selective staging, git apply --cached, 选择性暂存, 本任务 hunks, 并行改动未混入 -> `docs/powershell-memory.md#同文件并行改动选择性暂存门禁`
- 构建发布耗时 / 真实 E2E 发布预检：`D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-build-preflight-lessons.md`
- 已提交 git 版本发布 / clean worktree 发布输入 / 发布前置门禁：`D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-build-preflight-lessons.md`
- GitHub 推送 / git push / GH001 / Large files detected / pre-receive hook declined / 历史大文件 / Git LFS / 100 MB：`D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-build-preflight-lessons.md#2026-07-24-github-推送前历史大文件门禁`
- Windows 发布 worktree 长路径 / Filename too long / 维护仓历史 evidence 检出失败：`D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-build-preflight-lessons.md`
- build-release 目标主机参数 / TestServerHost / BackupServerHost / ProdServerHost / runtime env 生成：`D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-build-preflight-lessons.md`
- publish-int-ruoyi NasConfigPath / NAS JSON / runtime-control.local.yaml 误传 / ConvertFrom-Json maintenance：`D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-build-preflight-lessons.md`
- 展厅图片 smoke gate / 远端 MySQL stdout 中文路径 / `infra_file.path` / `HEX(path)` / `EscapeDataString` / `showrooms[].products`：`D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-build-preflight-lessons.md#2026-07-20-展厅图片-smoke-gate-中文路径解码门禁`
- 不带数据发布 / code-only / SkipDatabaseSync / SkipMinioSync / manifest 无数据目录验证：`D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-build-preflight-lessons.md`
- release-migration dependsOn 缺失 / SQL 元数据依赖不存在 / migration policy gate：`D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-build-preflight-lessons.md`
- DCC 分类 lifecycle_stage / NOT NULL 迁移 / required SQL 空值 / deleted=1 历史归档行：`D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-build-preflight-lessons.md`
- 测试服远端 MySQL 查询 / ruoyi-vue-pro 业务库 / 容器内 MYSQL_ROOT_PASSWORD / SSH 多层引号：`D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-build-preflight-lessons.md`
- runtime-control.local.yaml / repo-root / frontend-root / 临时发布 worktree / 控制台健康检查：`D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-build-preflight-lessons.md`
- 发布 / 备份 / 恢复 / 服务器容量：`E:\IntRuoyi\docs\release-backup-restore.md`
- 备份计划任务 / IntRuoyi Backup Scheduled / NextRunTime N/A / schtasks 查询失败 / Task To Run 旧路径 / 定时备份恢复判断：`E:\IntRuoyi\docs\release-backup-restore.md#正式服备份计划任务状态门禁`
- 正式服 backup-now + 备份服 rehearsal / production source -> backup repository / same backupId / TargetEnvironment prod backup mismatch / backup-source NAS 未就绪 / INTBK-2003 / MySQL 恢复 dump 不在受保护 BackupPackage / INTBK-3002 / source temp dump / MinIO 对象备份 / INTBK-4001 / Invalid JSON primitive: Unable / Docker pull status lines / mc --json / DCC manifest / INTBK-6001 / dcc_object_inventory_missing / 对象 inventory 覆盖率 / checksums / Get-FileHash 缺失 / INTBK-6003 / rehearsalStatus verified PASSED 冲突 / backendHealth frontendHttp200 loginReachable fileDownloadSample / rehearsal bucket runtime 隔离：`E:\IntRuoyi\docs\release-backup-restore.md#正式源备份到备份服隔离演练仓库门禁`
- 服务器访问 / 重启 / 远端联调：`E:\IntRuoyi\docs\server-access.md`
- 前端页面 / 表格 / 样式：`D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- Keywords: Vite import-analysis Failed to resolve import / controlled-file/logs/index.vue / 前端源码 logs 目录被 .gitignore logs/ 忽略 / git check-ignore -> `docs/frontend-development.md#前端源码目录与-gitignore-门禁`
- Keywords: 既有大契约先失败, 最小静态契约, 专用 static.spec.js, 当前需求 RED/GREEN, 无关 ts:check blocker -> `docs/frontend-development.md#前端静态契约隔离门禁`
- Keywords: 保存系统异常重复提示, axios 自动错误提示, ignoreErrorMessage, 子组件 rethrow 前 toast, 父组件统一 toast, save error single toast, RouteFlowGraphDesigner, RouteFormContent -> `docs/frontend-development.md#前端保存链路重复错误提示门禁`
- Keywords: 草稿保存后仍可修改, 保存草稿不提交发布, 保存后不可继续编辑, 立即提交发布弹窗, submit-publish, DRAFT 被保存推进审批, RouteEditPage handleSaved, promptRouteVersionSubmit -> `docs/frontend-development.md#前端草稿保存与提交发布解耦门禁`
- Keywords: 聚合字段新增子项, 默认分类, 表单槽位, route-flow-graph-designer__node-form-count-badge, createEmptyRecordBinding, formSlotType MAIN, 新增后数字不变 -> `docs/frontend-development.md#前端聚合新增默认分类门禁`
- eDHR 批次详情 / 动态表单 / 损耗单 / 工艺路线绑定 / 填写人 / `fillableUsers` / `routeBindingId` / 配置页有值但详情接口为空：`E:\IntRuoyi\docs\backend-development.md#edhr-详情回填门禁`
- Keywords: 切换填写人, 协助填写人, 填写人快照, `assistSwitchTasks`, `candidateUserSnapshot`, `getEdhrBatchExecution`, `taskId`, active 执行记录, `batchExecutionId + taskId`, 弹窗打开耗时过长 -> `docs/backend-development.md#切换填写人快照读取边界`
- Keywords: eDHR 批次任务配置来源, routeSnapshotJson, batchUseConfigs, 当前 BATCH 工序配置, 陈旧绑定, legacy flat batchRecordReportId, 发布快照不得通用 fallback, 草稿 BATCH 快照显式保存, batchRecordBindingSnapshotExplicit, flow-config/batch-record/save, 表单槽位读回为空, 草稿保存系统异常 -> `docs/backend-development.md#edhr-批次任务配置来源门禁`
- Keywords: eDHR 批记录版本治理规则, CELL_RULE_RECONCILED, 1040750243, 批记录模板未确认填写规则, Jimu 当前 JSON, openOrCreateByContext -> `docs/backend-development.md#edhr-批记录版本治理规则运行态门禁`
- Keywords: eDHR 批记录单元格链接预填落库, PRODUCTION_WORK_ORDER.batchCode, 生产批号目标格为空, /batch-record-cell-link/prefill, cell_values_json=[], hydrateDraftState, 字段审计链, 创建打开执行记录自动落库 -> `docs/backend-development.md#批记录单元格链接预填落库边界`
- Keywords: eDHR 批记录 Word 表格解析, packed 物料矩阵, 括号续行, 生产自检, 合格标准, 检验方法, 截图位置错位, fixture 缺失, 表单名特例禁止 -> `docs/backend-development.md#edhr-批记录-word-表格解析门禁`
- Keywords: Jimu fillForm 组件类型, componentFlag input-textarea, componentFlag input-text, componentFlag signature, 多行文本, 普通文本, 日期单元格, 电子签名, 签名日期宽空白格, 记录人/日期, 操作人/日期, 复核人/日期, edhrSignature -> `docs/backend-development.md#jimu-fillform-组件类型语义优先边界`
- Keywords: 子表集合替换, 软删除唯一键, 逻辑删除占用唯一键, deleteByCaseId, system_codex_test_checkpoint, updateCase_allowsRepeatedCheckpointReplacement -> `docs/backend-development.md#2026-07-25-子表集合替换软删除唯一键门禁`
- Keywords: Windows CRLF 静态合同, static.spec.js 目标 worktree 自身失败, 真实 E2E 脚本与页面行为同步, 废弃弹窗流程, 字段审计保存直连 API -> `docs/e2e-rules.md#静态合同与真实-e2e-同步门禁`
- Keywords: worktree E2E 成对 URL, EDHR_RELEASE_DOSSIER_E2E_BASE_URL, EDHR_RELEASE_DOSSIER_E2E_BACKEND_URL, 同一 runtime slot, 48081 旧 jar, 8086 48086 -> `docs/e2e-rules.md#worktree-隔离运行态-url-门禁`
- Keywords: worktree 前端 vite 缺失, Command "vite" not found, node_modules .bin vite, pnpm install --frozen-lockfile, 不复制 node_modules -> `docs/worktree-memory.md#worktree-前端依赖启动门禁`
- Keywords: worktree 端口段, slot 1..19, slot >= 20, reserve-worktree-slot, 原子槽位, 重复活动槽位, 基准端口碰撞, int_main 误判 int_main_d -> `docs/worktree-memory.md#worktree-端口段与原子槽位门禁`
- Keywords: 多 worktree 融合, 批量 merge worktree, dirty worktree 独立提交, merge-base --is-ancestor, 聚焦组合回归, 宽回归失败归因, 合并后删除 worktree -> `docs/worktree-memory.md#多-worktree-批量融合门禁`
- Keywords: schema-backed E2E, source_type, source_field_code, sourceFields, Unknown column, 字段矩阵可见但不可选, is-source-selectable, 单元格链接生产工单字段, 只读 E2E 无 MES 写请求 -> `docs/e2e-rules.md#schema-backed-e2e-迁移与字段可选态门禁`
- Keywords: eDHR 单据填写人显示值, 损耗单卡片, fillableUsers displayName, candidateSourceNames 格式, 页面填写人断言 -> `docs/e2e-rules.md#edhr-单据填写人显示值门禁`
- Keywords: eDHR 终态批次个人待办, edhr-work-task/my-page, edhr-work-task/stats, workTaskId, 当前 eDHR 批次状态不允许该操作, TODO 作废批次, VOIDED, CLOSED, ARCHIVED, REJECTED, openTask 终态保护 -> `docs/e2e-rules.md#edhr-终态批次个人待办门禁`
- Keywords: eDHR 路线表单跳过口径, 损耗单打开填写, 查看表单, routeFormReadonly, 无 OPEN_FORM 只读查看, 必填路线表单不允许跳过, requiredPolicy OPTIONAL, requiredFlag 误判可跳过, canSkipOptionalTask, isOptionalTask, SKIP allowedActions, task/preview, shouldLoadTaskPreview, formCenterInstanceId -> `docs/e2e-rules.md#edhr-路线表单跳过口径门禁`
- Keywords: eDHR 右侧红框元信息, primary-fill-meta, primaryFormFillMetaItems, showPrimaryFormFillMeta, resolvePrimaryFormFillersText, 填写人 提交时间 红框, batchRecordFormNames, selected-field-detail, resolveRecordBindingSlotType, 过程检验记录误入批记录表单 -> `docs/e2e-rules.md#edhr-右侧红框元信息隐藏门禁`
- Keywords: 表单模板升版审批, Form template upgrade requires BPM approval, FORM_TEMPLATE_UPGRADE, FORM_TEMPLATE_OBSOLETE, form-template-upgrade-v1, 业务审批策略 DIRECT 降级, BPM_REQUIRED executor, bpm_business_approval_policy published 策略 -> `docs/backend-development.md#业务审批策略强制-bpm-门禁`
- 项目错误预防短记忆：`D:\ProjectPackage\Int\IntRuoyiMaintance\docs\agent-memory\project-error-prevention.md`

## 任务门禁

- 在 `task.md` 只摘命中文档的门禁，写入 `## 经验门禁`。
- 涉及服务器写入、发布、恢复、大文件上传、真实 E2E、worktree 合并或清理等高风险动作前，`execution-log.md` 必须先记录 `GREEN: experience-preflight -> PASS` 或 `BLOCKER: experience-preflight -> <原因>`。
- 没有 `experience-preflight` 记录时，不执行长链路或高风险动作。

- 发布 mark-tested / mark-release-tested / selectedRecoverySetCandidateId / recovery set 候选：见 `docs/release-agent-checklist.md`，标题“4. mark-tested 放行规则”；自由文本参数引号问题见 `docs/release-build-preflight-lessons.md#2026-07-06-mark-tested-本地启动参数引号门禁`。
- 发布验证 / 正式服端口 / docker compose ps / 48081 / 8081：见 `docs/release-build-preflight-lessons.md#2026-07-04-正式服验证必须读取-docker-compose-端口映射`。
- 发布构建后 sourceRepos 校验：完整契约入口是发布包根目录 `manifest.json`，不是兼容概要 `release-manifest.json`；详见 `docs/release-build-preflight-lessons.md#2026-07-05-发布-manifest-sourcerepos-校验入口`。
- 发布部署动作参数契约：`publish-test/promote-prod/promote-backup` 不传 `publishScope`，必须传 `targetEnvironment` 与 `releaseTag`；详见 `docs/release-build-preflight-lessons.md#2026-07-05-deploy-action-preview-参数契约`。
- Keywords: publish-test preview msg=publishScope, deploy action publishScope, includeOnlyOffice includeShowroomBuildPackage, r260719i preview 400 -> `docs/release-build-preflight-lessons.md#2026-07-05-deploy-action-preview-参数契约`
- required SQL 菜单硬编码 ID / system_menu 占用 / 900301 / 900302 / 权限漂移：`D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-build-preflight-lessons.md`
- 发布脚本自由文本参数 / Start-Process ArgumentList / mark-tested TestConclusion 引号门禁：见 `docs/release-build-preflight-lessons.md` 的 “2026-07-06 mark-tested 本地启动参数引号门禁”。
- `docs/release-build-preflight-lessons.md`：测试服 HEAD 发布前置经验，覆盖 clean worktree、pnpm 版本、SQL metadata、Smart Release 输入、大镜像分块上传、runtime-control API、UTF-8/SSH 输出校验和发布后 fail-fast 条件。（2026-07-09 任务 `20260709-head-test-server-release` 更新）

- build-release API 预览 targetEnvironment 参数契约：`D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-build-preflight-lessons.md#2026-07-09-build-release-预览请求不得携带-targetenvironment`

- build-release 三环境预览 ProdServerHost 门禁：`D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-build-preflight-lessons.md#2026-07-09-build-release-三环境预览必须含-prodserverhost`
- 2026-07-09：build-release 前必须执行全量迁移元数据策略门禁，`type` 只允许 schema/data/menu/config/permission/seed，修复 SQL 元数据需同步补 `script/tests` 覆盖；`20260714_dcc_personal_file_decommission.sql` / `Release migration metadata missing` 也命中该门禁；详见 `docs/release-build-preflight-lessons.md#2026-07-09-build-release-迁移元数据必须先过全量策略门禁`。
- 2026-07-09：publish-test required SQL 若出现 MySQL 1137 Can't reopen table，先冻结远端 operation/migration/env 证据，再拆分或落表修复 SQL 并补 script/tests，最后重新构建新的 releaseTag；详见 `docs/release-build-preflight-lessons.md`，标题“2026-07-09 publish-test SQL 不得复用同一派生/临时表别名触发 MySQL 1137”。
- 2026-07-09：publish-test required SQL 不得依赖测试服手工业务数据；出现 Missing <业务对象> 时先冻结 operation/migration/业务表快照，再通过迁移自给或 dependsOn 修复并重新构建新 releaseTag；详见 docs/release-build-preflight-lessons.md#2026-07-09-publish-test-required-sql-前置业务数据必须由迁移自给或显式依赖。


- Keywords: code-only required SQL, type=data, publishScope=code-only, SkipDatabaseSync, SkipMinioSync, preflight-plan APPLY data migration
  - Read: `docs/release-build-preflight-lessons.md`
  - Gate: code-only 发布前确认 data required SQL 不进入远端 MySQL 执行队列；如会执行，阻塞并修复发布脚本后重建 releaseTag。


- Keywords: IntRuoyi code-only component, Component intruoyi, Website dirty sourceRepos, website package directory, manifest sourceRepos dirty
  - Read: `docs/release-build-preflight-lessons.md`
  - Gate: IntRuoyi 后端/前端发布 build-release 必须显式 `-Component intruoyi`；manifest 不得包含 Website 仓或 website 包目录。


- Keywords: preflight-plan missing type, manifest requiredSql type map, code-only data SQL skip, requiredSqlTypeByMigrationId, RT000006 data migration
  - Read: `docs/release-build-preflight-lessons.md`
  - Gate: code-only required SQL 过滤必须从 manifest requiredSql 回查 migration type；preflight item 缺 type 不得导致 data SQL 执行。


- Keywords: Java native memory allocation failed, Maven build memory, hs_err_pid, build-release memory, insufficient memory JVM
  - Read: `docs/release-build-preflight-lessons.md`
  - Gate: build-release 前检查内存余量；JVM native memory 失败必须冻结 hs_err 与内存快照，降低构建内存压力后重建新 releaseTag。
- Keywords: prod-preflight-release, ProdDryRunEvidencePath, sanitized package workspace, Copy-Item, robocopy, shutil.copytree -> `docs/release-build-preflight-lessons.md`
- Keywords: worktree remove failed, residual worktree directory, Invalid argument, Directory not empty, runtime-backend.err.log, Vite, esbuild, Java process locks residual worktree, Git registration absent physical directory exists, 8084 48084 pici cleanup -> `E:/IntRuoyi/docs/worktree-memory.md`
- Keywords: frozen release baseline, build start worktree commit, source HEAD drift after build, 不追逐后续提交 -> `docs/release-build-preflight-lessons.md`
- Keywords: mes_pro_route_schedule_config conflict, route schedule config duplicate, 工序维度收敛, conflict guard -> `docs/release-build-preflight-lessons.md`
- Keywords: Manifest v1 legacy release-manifest frontend commit, schema-specific manifest validation -> `docs/release-build-preflight-lessons.md`
- Keywords: Manifest artifact sha256 prefix, sha256:<64hex>, 281 hash mismatch, hash prefix normalization -> `docs/release-build-preflight-lessons.md#2026-07-10-manifest-artifact-sha256-前缀门禁`
- Keywords: runtime-control Linux powershell.exe, 运维矩阵 ERROR unknown, release-status current tag null, release-info 版本变更说明 -> `docs/release-build-preflight-lessons.md`
- Keywords: PowerShell pipe Python Node 中文问号, subprocess text CRLF ssh bash, $LASTEXITCODE stderr, $Args automatic variable -> `docs/powershell-preflight-lessons.md`
- Keywords: PowerShell heredoc, python -c newline, U+FEFF BOM stdin, Playwright 中文正则, rg -- lookahead, preview 参数 flag 错位, -RequireTested, 脱敏误伤 ruoyi-vue-pro, -ProdServerHost -> `docs/powershell-preflight-lessons.md`，标题“2026-07-13 PowerShell 发布脚本承载、参数解析与证据脱敏门禁”
- Keywords: ssh -n bash -s, Windows text mode CRLF, binary stdin, remote MySQL heredoc, release lock empty query, migration failed count -> `docs/powershell-preflight-lessons.md#2026-07-13-ssh-stdinwindows-二进制-lf-与远端-sql-验收门禁`
- Keywords: concurrent publish, releaseTag drift, operation lock RUNNING, 并发发布, 目标版本漂移, .env compose image release-info 不一致 -> `docs/test-release-preflight.md#并发发布与目标版本漂移门禁`
- Keywords: publish-test preview BackupServerHost metadata, forbidden host token, ServerHost target gate, 172.30.30.59 误判 -> `docs/test-release-preflight.md`，标题“2026-07-12 publish-test 目标字段与版本说明验收门禁”
- Keywords: SmartReleaseBaselineManifestPath missing, enableSmartReleaseReport failed, smart release report-only baseline -> `docs/release-build-preflight-lessons.md#2026-07-12-smart-release-报告基线门禁`
- Keywords: Manifest v1 changeSet.component, legacy release-manifest component, schema-specific manifest validation -> `docs/release-build-preflight-lessons.md#2026-07-12-manifest-v1-与-legacy-schema-分离校验`
- Keywords: PowerShell $PID automatic variable, cleanup process self-match, r260712p worktree removal, command line guard -> `docs/powershell-preflight-lessons.md#2026-07-12-powershell-自动变量与进程守卫门禁`
- Keywords: migration policy gate --json unsupported, run-release-migration-policy-gate --output, frozen script parameter contract -> `docs/release-build-preflight-lessons.md#2026-07-12-冻结-worktree-脚本参数契约门禁`
- Keywords: build-release missing ProdServerHost, three environment host preview, TestServerHost ProdServerHost BackupServerHost -> `docs/release-build-preflight-lessons.md#2026-07-13-build-release-三环境-host-完整性门禁`
- Keywords: release worktree frontend node_modules missing, cross-env not recognized, yudao-ui-admin-vue3 frozen install -> `docs/release-build-preflight-lessons.md#2026-07-13-release-worktree-双前端依赖恢复门禁`
- Keywords: release log secret, mysql -p plaintext, 发布日志明文密码, request-command-log 脱敏, raw secret log -> `docs/release-build-preflight-lessons.md#2026-07-10-发布日志凭据脱敏门禁`
- Keywords: mes_pro_route_use_process_batch_record missing, mes_pro_route_use_process_batch_record_legacy_20260709, ERROR 1146, 20260708_mes_batch_record_version_phase_one, route flow legacy rename order -> `docs/release-build-preflight-lessons.md#2026-07-13-required-sql-兼容路线流迁移重命名顺序`
- Keywords: docker inspect crash, Exception 0xc0000005, local container env, raw docker env secrets, LocalMySqlContainer, LocalMinioContainer -> `docs/release-build-preflight-lessons.md#2026-07-13-build-release-本地-docker-inspect-与-env-脱敏门禁`
- Keywords: docker build no new log, Docker Desktop Linux engine 500, buildx _ping timeout, build-release BuildKit hang, Docker preflight passed then build hung, docker-buildx CPU, manifest absent after docker kill -> `docs/release-build-preflight-lessons.md#2026-07-19-build-release-docker-buildkit-卡顿诊断门禁`
- Keywords: infra_release_operation_lock updated_time, update_time, Unknown column updated_time, release lock schema -> `docs/release-build-preflight-lessons.md#2026-07-13-远端发布锁表查询必须先按真实-schema-校验`
- Keywords: mark-release-tested targetEnvironment, mark-tested targetEnvironment, selectedRecoverySetCandidateId restore candidate -> `docs/release-build-preflight-lessons.md#2026-07-13-mark-tested-payload-不得携带-targetenvironment`
- Keywords: runtime-control state-dir release worktree dirty, ?? runtime, operation state logs cache dirty sourceRepos -> `docs/release-build-preflight-lessons.md#2026-07-13-运行控制台-state-dir-不得污染-release-worktree`
- Keywords: runtime-control.local.yaml secret, release-status timeout, release-packages timeout, direct manifest lookup, publish-test log secret -> `docs/release-build-preflight-lessons.md#2026-07-13-测试服发布验收配置脱敏与-direct-manifest-lookup-门禁`
- Keywords: clean maintenance worktree local config absent, runtime-control.local.yaml missing, generated runtime dirty, maintenance worktree runtime folder -> `docs/release-build-preflight-lessons.md#2026-07-13-clean-maintenance-worktree-local-config-与-runtime-输出门禁`
- Keywords: UNC NAS manifest false negative, package lookup preview, historical failed migration, target release migration failure count -> `docs/release-build-preflight-lessons.md#2026-07-13-发布包可用性与历史-migration-失败分层门禁`
- Keywords: preview arguments RequireTested switch flag parser, SkipDatabaseSync SkipMinioSync flag, ServerHost preview gate false negative -> `docs/release-build-preflight-lessons.md#2026-07-13-preview-参数解析必须兼容-switch-flag`
- Keywords: AGENTS rule conflict, 规则优先级, previous task ownership, concurrent worktree ownership, 仅测试服完成定义 -> 先读全局 `C:\Users\BJB110\.codex\AGENTS.md` 的 `Rule Precedence and Task Ownership`，再读当前目录最近的项目 `AGENTS.md`
- Keywords: Invoke-WebRequest Content byte array, Invoke-RestMethod health status, process scan exclude current PID -> `docs/powershell-preflight-lessons.md`
- Keywords: restart-int-ruoyi-local, Missing int_main frontend path, yudao-ui-admin-vue3, IntRuoyiFronted, local backend restart E2E -> `docs/local-runtime.md#2026-07-24-本地重启脚本路径门禁`
- Keywords: local backend MySQL Access denied, dynamic-datasource create datasource named [master] error, Access denied for user 'root'@'localhost', 48081 未监听, 本地后端数据库凭据 -> `docs/local-runtime.md#2026-07-25-本地后端数据库凭据门禁`
- Keywords: D-Main local runtime, int_main_d, vite command not found, Java package runtime ignored, *.runtime不存在, git check-ignore runtime source -> `docs/local-runtime.md#2026-07-25-d-main-本地启动源码与依赖门禁`
- Keywords: isolated backend jar, dirty main workspace, int_main 48081, jar SHA256, local E2E reload, clean worktree build, 未登录 401, 登录态路由验证, route-not-found, schema 字段核对 -> `docs/local-runtime.md#2026-07-24-隔离构建-jar-加载门禁`
- Keywords: Element Plus el-table, 表格行复选框, 表头全选误点, indeterminate checkbox, Playwright body-wrapper row selection, 手动重排选中集合断言 -> `docs/e2e-rules.md#element-plus-表格选择门禁`
- Keywords: edhr-batch-execution-real-flow.e2e.js, int-ruoyi-mysql, 数据库夹具, LOCAL_DATABASE_FIXTURE, 芋道源码/admin, 真实 E2E 前置条件缺失, 覆盖历史 E2E 证据 -> `docs/e2e-rules.md#edhr-批次执行数据库夹具与证据文件门禁`
- Keywords: edhr-work-task-process-advance-real.e2e.js, FormCenter 动态表单夹具, batch_record_report_id 为空, form_binding_key, form_center_instance_id, 生产工单不存在, 当前工艺路线工序未配置默认批记录报表, eDHR 批次工序任务被阻塞, EDHR-ADV, 工作台处理按钮目标行 -> `docs/e2e-rules.md#edhr-工作任务-formcenter-动态表单夹具门禁`
- Keywords: eDHR 批次作废 BPM_REQUIRED, void-batch-execution approval-resolution, 作废弹窗审批策略, act_ru_task ASSIGNEE_, 审批中心 tasks/review, 作废后工作台待办闭环 -> `docs/e2e-rules.md#edhr-作废-bpm-审批真实-e2e-门禁`
- Keywords: 全局开关 E2E, 共享配置恢复, 系统级配置开关, Playwright finally restore, 恢复后复验, global switch cleanup -> `docs/e2e-rules.md#全局开关类-e2e-恢复门禁`
- Keywords: Element Plus el-select, Playwright 下拉选择, 租户下拉, 工单下拉, 工艺路线编码, ACTIVE route snapshot, draft independence E2E, el-popover, Popover 内下拉, teleported=false, click-outside, 复制弹层选择后误关闭 -> `docs/e2e-rules.md#element-plus-下拉选择门禁`
- Keywords: Element Plus el-select 多选标签, Element Plus el-input-number, 数字步进控件, el-select__tags-text, 选择框显示全, 目标项名称显示全, 填写人显示全, 三列弹窗表单, grid-template-columns, codex-test-checkpoint, collapse-tags-tooltip -> `docs/e2e-rules.md#element-plus-选择框显示门禁`
- Keywords: Codex Runner, 系统管理 测试管理, 自然语言测试方法, 检查点截图, runner token, parallelSafe, Codex 调用 Playwright, 没有在线 Codex Runner, 按需 Runner, Runner 包装层, 裸调用 codex, codex-test-result, taskkill.exe, Windows codex.cmd 子进程, heartbeat 过期 -> `docs/e2e-rules.md#codex-runner-自动测试门禁`
- Keywords: Codex Runner 目标测试项, 作废测试不存在, system_codex_test_case, 测试管理页面搜索总数 0, Runner 空领取不得当成功 -> `docs/e2e-rules.md#codex-runner-目标测试项存在性门禁`
- Keywords: 测试管理 系统异常, codex-test-case/page code 500, system_codex_test_case.project, 20260726_system_codex_test_case_project.sql, Codex 测试项分页缺字段 -> `docs/database-rules.md#测试管理-schema-迁移门禁`
- Keywords: 个人工作台 系统异常, 待办任务加载失败, 隐藏任务状态, profile-workbench-task-visibility, hidden-keys, system_profile_workbench_task_visibility, 20260727_system_profile_workbench_task_visibility.sql -> `docs/database-rules.md#个人工作台隐藏任务状态迁移门禁`
- Keywords: int_shedule local Docker dependencies, 23306, 26379, Docker MySQL Redis, branch local runtime dependency ports -> `docs/local-runtime.md#2026-07-25-分支本地运行复用-docker-依赖门禁`
- Keywords: task-closeout status unknown, Current Status completed, cleanup preview apply, worktree Directory not empty -> `docs/release-build-preflight-lessons.md` and `E:\IntRuoyi\docs\worktree-memory.md`
- Keywords: task-closeout runtime log lock, PermissionError WinError 32, runtime-control-main err log, long-running process log under task dir -> `docs/release-build-preflight-lessons.md#2026-07-13-task-closeout-长运行进程日志目录门禁`
- Keywords: Cleanup Keep, doc/tasks/**/*.cjs, cleanup keep 反引号, keep 路径内联说明, task_closeout preview delete, 验证脚本保留 -> `docs/task-closeout-rules.md#任务验证脚本保留门禁`
- Keywords: release-info CRLF, releaseInfoTagOk false, console-error-string-parser, Errors: 0, Warnings: 0, residual-release-worktree-root, r260713j physical root -> `docs/release-build-preflight-lessons.md#2026-07-13-release-info-与运行控制台验收解析门禁` and `docs/release-build-preflight-lessons.md#2026-07-13-release-worktree-物理根目录复核门禁`
- Keywords: ready_for_closeout, verification-report keep, cleanup apply, closeout state machine, completed after cleanup -> `docs/release-build-preflight-lessons.md#2026-07-10-发布任务-closeout-状态与清理契约` and `E:\IntRuoyi\docs\worktree-memory.md`
- Keywords: system_menu.id must be an integer literal, duplicate system_menu.id detected across release SQL history, INSERT INTO system_menu SELECT ON DUPLICATE KEY UPDATE VALUES, release SQL parser false positive, 20260714_signature_my_signature_admin_menu -> `docs/release-build-preflight-lessons.md#2026-07-16-release-preflight-菜单-sql-解析边界门禁`
- Keywords: vue/no-unused-vars, vite-plugin-eslint, pnpm build:test, ProcessWipTable, sortColumnAttrs, handleTemplateSortChange, slot unused vars -> `docs/release-build-preflight-lessons.md#2026-07-16-release-build-前端-eslint-slot-未使用变量门禁`
- Keywords: 20260718_system_entitlement_management, system_entitlement_policy, system entitlement migration metadata, missing release-migration metadata, system entitlement policy SQL -> `docs/release-build-preflight-lessons.md#2026-07-19-system-entitlement-迁移元数据门禁`
- Keywords: unknown release-migration metadata key, dependsOn missing migration, invalid type, schema,menu, .sql dependsOn suffix, 20260715_mes_schedule_capacity_mode_unification, 20260717_mes_route_process_workstation_binding, 20260717_mes_edhr_filler_minimal_permissions, 20260717_bpm_form_center, 20260718_mes_feedback_import_record_direct_progress -> `docs/release-build-preflight-lessons.md#2026-07-19-release-migration-结构化字段与-dependson-后缀门禁`
- Keywords: setReleaseActionLocked, getWorkTaskId, setBatchRecordVersionNo, withdrawVoidBatchExecution, updateApprovalFieldsToDraft, MesProBatchRecordParsedCell, isReviewedCellRule, getCellRuleSource, @Override 签名漂移, MES companion contract, yudao-module-mes compilation failure -> `docs/release-build-preflight-lessons.md#2026-07-19-build-release-mes-companion-contract-编译门禁`
- Keywords: withdrawVoidBatchExecution frontend export, edhr change API missing export, RollupError not exported, BatchExecutionListPage import change.ts -> `docs/release-build-preflight-lessons.md#2026-07-19-frontend-edhr-companion-api-export-门禁`
- Keywords: frontend build:test timeout, vite-plugin-progress EEXIST, node_modules .progress, .progress.json, dist cleanup, dist-test cleanup, residual node vite esbuild pnpm process -> `docs/release-build-preflight-lessons.md#2026-07-23-frontend-buildtest-vite-progress-cache-门禁`
- Keywords: ERROR 1267, Illegal mix of collations, utf8mb4_unicode_ci, utf8mb4_general_ci, 20260717_mes_edhr_filler_minimal_permissions, tmp_edhr_filler_required_permission, system_entitlement_policy no-op -> `docs/release-build-preflight-lessons.md#2026-07-19-publish-test-required-sql-collation-门禁`
- Keywords: ERROR 1644, Role id 910311 is already occupied, system_role.id, bpm_admin, 20260718_bpm_admin_role_assignment, electronic_signature_admin, LAST_INSERT_ID -> `docs/release-build-preflight-lessons.md#2026-07-19-publish-test-角色-id-硬编码占用门禁`
- Keywords: Missing enabled full-scope admin menu, tmp_audit_admin_expected_permission, tmp_audit_admin_expected_menu, 990226, dcc:project-code-assignment:audit:query, required SQL 权限菜单兼容 -> `docs/release-build-preflight-lessons.md#2026-07-22-publish-test-required-sql-权限菜单兼容门禁`
- Keywords: dcc master points to obsolete revision, OBSOLETE_CHAIN, 20260718_controlled_content_lifecycle, dcc_controlled_file_master, current_active_controlled_file_id, file_number, controlled content lifecycle -> `docs/release-build-preflight-lessons.md#2026-07-19-controlled-content-生命周期迁移-obsolete_chain-门禁`
- Keywords: controlled_content_version_ref.content_key, CAST master id AS CHAR collation, utf8mb4_0900_ai_ci, utf8mb4_unicode_ci, controlled content content_key ERROR 1267 -> `docs/release-build-preflight-lessons.md#2026-07-19-controlled-content-content_key-cast-collation-门禁`
- Keywords: ERROR 1406, Data too long for column menu_ids, system_tenant_package.menu_ids, 20260717_bpm_form_center, backup promote menu_ids length -> `docs/release-build-preflight-lessons.md#2026-07-19-backup-promote-tenant-package-menu_ids-长度门禁`
- Keywords: r260719k release failure prevention checklist, 三环境 code-only 发布复发防止, MES companion, frontend export, BuildKit, required SQL collation, menu_ids -> `docs/release-agent-checklist.md`，标题“2026-07-19 r260719k 发布失败复发防止清单”
- Keywords: test-only release, publish-test, test server runtime verification, release-info dialog, frozen release worktree, only test server -> `docs/test-release-preflight.md`
- Gate: 仅测试服发布任务优先读取 `docs/test-release-preflight.md`，再按失败点跳转 `release-build-preflight-lessons.md`、`powershell-preflight-lessons.md` 和 `E:\IntRuoyi\docs\worktree-memory.md`。
- Keywords: remote python3 absent, python3 command not found, jq command not found, backup runtime verification carrier, 远端验收工具缺失 -> `docs/powershell-preflight-lessons.md#2026-07-14-远端验收工具可用性门禁`
- Keywords: worktree remove Directory not empty, r260713v release worktree residual, git worktree registration absent physical directory exists -> `docs/release-build-preflight-lessons.md#2026-07-13-release-worktree-物理根目录复核门禁`
- Keywords: only test release ssh stdin CRLF, .env IMAGE_TAG remote verification, runtime console change notes, release lock APPLIED -> `docs/test-release-preflight.md#2026-07-13-仅测试服发布远端验收脚本承载门禁`
- Keywords: runtime console dialog source commits, version change notes visible, release-info source commit verification, page-visible releaseTag -> `docs/test-release-preflight.md#2026-07-13-运行控制台版本说明与-source-commit-分层验收门禁`
- Keywords: Python f-string literal braces, SSH verification carrier, bash SQL JSON braces SyntaxError -> `docs/powershell-preflight-lessons.md#2026-07-13-python-f-string-literal-braces-与远端验收脚本门禁`
- Keywords: login-preflight.mjs 缺失, admin-only 全量 E2E, 旧目标文案执行列表, 历史 execution 直连填写页, 默认密码清理, 动态预览批次任务 -> `docs/e2e-rules.md#官方登录前置与-admin-only-全量验证门禁`
- Keywords: stale blocked task, 提交前 blocked 状态复验, Maven compile 解除旧阻塞, pnpm ts:check 解除旧阻塞, 目标 JUnit 复验后提交 -> `docs/powershell-memory.md#提交前-stale-blocker-复验门禁`
