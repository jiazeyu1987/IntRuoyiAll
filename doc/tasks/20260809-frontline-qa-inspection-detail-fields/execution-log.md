# 执行日志

## 用户意图

- 截图红框改为显示“抽样方案”。
- 截图黄框改为显示“检验器具及设备”。
- 数据均来自 QA 的检验项目列表。

## BDD 场景

BDD: 检验方法详情展示 QA 项目的抽样方案 -> Given 当前一线检验项目来自已发布 QA 检验项目列表且包含正式抽样方案 When 用户打开该项目的检验详情 Then 弹窗顶部显示“抽样方案”及该项目的抽样方案内容，不再重复显示检验方法。

BDD: 检验方法详情展示 QA 项目的检验器具及设备 -> Given 当前一线检验项目来自已发布 QA 检验项目列表且包含正式检验器具及设备 When 用户打开该项目的检验详情 Then 弹窗右侧显示“检验器具及设备”及该项目对应内容，不再显示检验项目、结果类型、单位和来源四张旧卡片。

## 命令意图与证据

- 已读取 `docs/task-closeout-rules.md`、`docs/frontend-development.md`、前端功能交付技能及其 evidence contract。
- 已核对 `docs/experience-index.md` 中与 QA/PQC 检验项目详情、用户可见正式字段和静态合同边界相关的入口。
- RED: `node tests/e2e/frontline-pqc-sampling-equipment-dialog-static.spec.cjs` -> FAIL, expected reason: QA 保存项目契约缺少必填 `inspectionTool`。
- RED: `python -X utf8 -m pytest script/tests/test_mes_qa_inspection_item_display_fields_sql.py -q` -> FAIL, expected reason: 正式 schema migration 尚不存在。
- RED: `node tests/e2e/frontline-pqc-sampling-equipment-dialog-static.spec.cjs` -> FAIL, expected reason: QA 服务边界尚未显式拒绝空白 `inspectionTool`/`samplingPlanText`。
- GREEN: `node tests/e2e/frontline-pqc-sampling-equipment-dialog-static.spec.cjs` -> PASS。
- GREEN: `node tests/e2e/frontline-pqc-fact-dialog-static.spec.cjs` -> PASS。
- GREEN: `node tests/e2e/pqc-tab-method-display-static.spec.cjs` -> PASS。
- GREEN: `node tests/e2e/frontline-pqc-qa-process-standard-method-source-static.spec.cjs` -> PASS。
- GREEN: `node tests/e2e/pqc-item-equipment-standard-method-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/qa-regulation-applicable-types-derived-static.spec.js` -> PASS。
- GREEN: `python -X utf8 -m pytest script/tests/test_mes_qa_inspection_item_display_fields_sql.py -q` -> PASS, 2 passed。
- GREEN: `python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql` -> PASS, migrationCount=456，包含 `20260809_mes_qa_inspection_item_display_fields`。
- GREEN: `mvn -pl yudao-module-mes -am "-DskipTests" compile` -> PASS，24/24 Reactor modules success。
- GREEN: `mvn -pl yudao-module-mes "-DskipTests" compile` -> PASS，确认最终生产源码为 up to date。
- `git diff --check -- <task-owned paths>` -> PASS，仅有仓库换行转换 warning，无空白错误。
- `pnpm ts:check` -> BLOCKED，失败位于并发修改的 `BatchRecordTestPage.vue:1244`：`orderAllocation` 缺失；该文件当前有 686 行新增/29 行删除，不属于本任务，未修改或回退。
- `mvn -pl yudao-module-mes "-Dtest=MesQaInspectionRegulationServiceTest,MesFrontlinePqcContextServiceTest" test` -> BLOCKED at testCompile：`MesFrontlinePqcContextServiceTest:157` 仍向当前构造器传入已移除的 `DccProjectCodeMapper`；该测试文件在本任务开始前已有并发改动，未擅自修订并行构造契约。
- 真实 E2E schema preflight -> BLOCKED：`8081` 前端 HTTP 200、`48081` 后端 health `UP`，但本地 `ruoyi-vue-pro.mes_qa_inspection_regulation_item` 未返回 `inspection_tool`/`sampling_plan_text` 两列。未应用迁移、未改写 `芋道源码/admin` 基线数据、未用 mock/API-only 替代。
- GREEN: frontend/backend/database evidence validator -> 全部 PASS。
- `project-experience-consolidation`：已将“展示原文独立持久化、历史数据不可由结构化字段反推”的通用门禁合并到 `docs/backend-development.md`，并更新 `docs/experience-index.md`；未新建长期经验文件。
- `task_closeout.py --mode preview` -> PASS，仅计划删除本任务的 3 份中间 evidence 文件，保留 `task.md`、`execution-log.md`、`verification-report.md`、源码、测试和 migration。
- `task_closeout.py --mode apply` -> PASS，按预览删除 3 份中间 evidence 文件，无 blocked/warning；未处理任何并行任务产物。
- 收尾复核：任务目录仅保留 3 份核心记录；聚焦前端合同 PASS；migration pytest 2 passed；任务范围 `git diff --check` PASS（仅换行转换 warning）。

## 里程碑状态

- M1：完成。任务目标、BDD、预期验证及无 fallback 约束已记录。
- M2：完成。确认截图目标为 `FrontlineFixedTemplatePanel.vue` 的检验方法详情弹窗；现有 QA 保存/发布契约仅保留方法、标准、数量比例和设备选项，未持久化 `inspectionTool`/`samplingPlanText` 原文，不能从现有字段无损反推。
- M3：完成。两个聚焦合同均在预期缺口处 RED。
- M4：完成。QA 保存/发布持久化、一线 PQC 响应和弹窗均使用 `inspectionTool`/`samplingPlanText` 正式原文字段；前端不再使用空字符串 fallback，历史缺口按精确字段路径 fail fast。
- M5：阻塞。先前静态/自动化验证完成后曾执行 cleanup；用户要求继续真实验收后任务已重新打开。本地 schema 与任务运行态前置已解除，但真实页面缺少已确认租户下可安全写入的完整业务数据链，尚不能完成 Playwright 验收和最终收尾。

## Blockers

- 真实 Playwright 验收被目标租户和业务数据范围阻塞：真实页面确认当前身份为“测试租户/瑛泰管理员”，该租户无 QA 规程、无活跃订单，且没有可用于 QA 的有效 DCC→MES 产品→路线→活跃订单链；截图本身无法唯一确认目标租户。继续写入前必须由用户明确确认是在测试租户创建完整任务自有链路，还是另行指定允许操作的租户与业务对象。

## 独立验证复核（2026-08-09）

- 前置：`Get-Command npx` -> PASS，解析到 `D:\Programs\npx.ps1`。
- 前置：`http://127.0.0.1:8081` -> HTTP 200；`http://127.0.0.1:48081/actuator/health` -> `UP`。
- 前置：查询 `mes_qa_inspection_regulation_item` 的目标列 -> 0 行；`inspection_tool`、`sampling_plan_text` 均未应用。
- GREEN: 6 个前端目标/相邻静态合同 -> 全部 PASS。
- GREEN: `pnpm ts:check` -> PASS；原并行类型阻塞本次已解除。
- GREEN: migration pytest -> PASS，2 passed。
- GREEN: release migration policy gate -> PASS，migrationCount=457，包含 `20260809_mes_qa_inspection_item_display_fields`。
- GREEN: 目标 JUnit -> PASS，49 tests，0 failures，0 errors，0 skipped，BUILD SUCCESS；原测试编译阻塞本次已解除。
- BLOCKED: 真实 Playwright -> 未启动。正式 schema 前置缺失，继续浏览器路径会触发已知运行态契约失败；未应用迁移、未修改业务数据、未降级为 mock/API-only。
- 独立门禁结论：BLOCKED。代码及自动化验证通过，但缺少真实页面验收证据。

## 本地迁移与真实页面续验（2026-08-09）

- 用户指令：继续。
- 数据库引擎：本机 Docker MySQL 8.0.39，数据库 `ruoyi-vue-pro`；不访问远端环境。
- RED: `information_schema.COLUMNS` 目标列查询 -> 0 行，预期原因：`inspection_tool`、`sampling_plan_text` 尚未应用。
- 迁移前置：`standard_lower_limit`、`standard_upper_limit`、`standard_unit`、`standard_precision`、`equipment_required` 和 `mes_qa_inspection_regulation_item_equipment` 均存在，正式依赖已满足。
- 数据安全：目标表迁移前 166 行；迁移只新增 nullable 字段，不执行 DML、不猜测或回填历史原文。
- 恢复计划：若迁移验证失败，先确认两列非空值计数为 0，再删除本迁移新增列；若任一列已有业务值则停止回滚并保留现场，不丢弃数据。
- GREEN: 正式迁移首次执行 -> PASS，退出码 0。
- GREEN: schema 复核 -> `inspection_tool`、`sampling_plan_text` 均为 `varchar(512) NULL`；历史行仍为 166，两列非空计数均为 0。
- GREEN: 正式迁移幂等重跑 -> PASS，两列均报告 already exists，退出码 0。
- GREEN: migration pytest -> PASS，2 passed。
- GREEN: release migration policy gate -> PASS，migrationCount=457，依赖 `20260803_mes_pqc_item_equipment_standard_snapshot`，riskLevel=low。
- 运行 Jar 前置：旧 `48081` Jar 的 `MesFrontlinePqcInspectionItem` 不含 `inspectionTool`/`samplingPlanText`，不能用于真实验收。
- 运行态更新：保留原 Jar，复制生成任务专用稳定 Jar，仅热替换本任务 8 组 MES class；目标 SHA-256=`E9EBFAB02CB93FE5840675B83C1D93ECEA29CB7318B4726FA74CDF24FBE509B9`，内嵌 MES Jar `compress_type=0`。
- 运行态 class 核对：保存 VO、一线 PQC record、响应 VO 均通过 `javap` 确认包含两个正式字段。
- 本地后端重启：停止归属明确且无活动连接的旧 PID 30464，启动任务 Jar PID 48788；`http://127.0.0.1:48081/actuator/health` -> `UP`。
- Playwright 真实路径：通过本机 `8081` 登录“测试租户/瑛泰管理员”，依次进入 `MES系统 > eDHR批记录 > QA`，页面 URL 为 `http://127.0.0.1:8081/mes/pro/process-pool/qa-regulation`。
- 页面前置结果：选择现有 IDI 项目后页面明确提示“当前 DCC 项目代码未绑定 MDM 产品，无法读取产品工艺路线绑定。”，未进入 QA 保存或发布写路径。
- 只读数据核对：测试租户 QA 规程数为 0、QA 项目数为 0、活跃订单数为 0；可见 DCC 项目没有可用于本任务的完整 DCC→MES 产品→路线→活跃订单链。现有 30 条规程/166 条项目均属于其它租户，不满足任务自有写入约束。
- 写入审计：本轮 QA 业务写请求数为 0；未修改 admin 基线或其它租户现有 QA 规程，未使用 SQL/API 造业务数据。
- Playwright 收尾：命名会话已关闭；任务输出目录、运行态构建临时目录以及本任务确切的两个含登录页内容的 `.playwright-cli` 文件均已删除并复核 `Test-Path=False`，未清理其它任务 artifact。
- 运行态复核：任务 Jar 曾健康运行并支撑上述真实页面取证；随后 PID 48788 与 `8081/48081` 监听均已停止，日志截至 18:45:24 无 JVM 致命退出记录。因业务数据授权仍阻塞，未擅自再次启动共享本地服务。
- `project-experience-consolidation`：复核 `docs/login-access.md` 的截图租户确认门禁，以及 `docs/e2e-rules.md` 的写入型任务自有数据和 Playwright 凭据快照清理门禁；现有长期文档已完整覆盖本轮经验，因此不重复修改或新建经验文档。

## 芋道源码租户授权续验（2026-08-09）

- 用户授权：明确授权在本机“芋道源码”租户进行真实验收。
- 数据边界：只创建带 `CODXQADETAIL20260809` 标识、可追踪且可清理的任务数据，或只读复用已有产品/路线/订单上下文；不覆盖或改写现有 QA 规程与其它基线记录。
- 验收路径：恢复 `8081/48081` 本机运行态，通过 Playwright 真实登录、QA 保存/发布和一线 PQC 详情弹窗完成验证；API/数据库仅用于只读前置与最终状态核验。

## 芋道源码真实发布前置结果（2026-08-09）

- 运行态核对：`8081` 与 `48081` 已由本机现有运行态恢复；当前后端 Jar 内嵌 MES class 经 `javap` 确认保存 VO、一线 PQC record、响应 VO 均包含 `inspectionTool` 与 `samplingPlanText`。
- Playwright 真实路径：登录“芋道源码”，进入 `MES系统 > eDHR批记录 > QA`，选择 IDI，成功加载路线 `RT000028-IDI / V1`。
- 来源字段证据：QA 检验项目列表真实显示列“检验器具及设备”和“抽样方案”；首条 `清洗 / 外观` 项目显示器具 `目测`，抽样方案 `GB/T 2828.1，I，AQL=0.4`。
- 写入准备：仅在页面内填写版本 `CODX-20260809`、规程编码 `CODXQADETAIL20260809`、规程名称 `CODX QA详情字段真实验收规程`，未通过其它入口预置数据。
- BLOCKED: 点击“发布规程” -> 页面在请求前提示“外观的正式工序‘组装螺杆八组件’未匹配激活路线版本中的任何路线工序”。网络记录无 QA POST，页面仍为 DRAFT。
- 正式映射缺口：QA 项目使用清洗、清洁、组装螺杆八组件、光固外套四组件、装配、整体粘结六个业务工序；RT000028-IDI / V1 的 14 个正式路线工序为粗洗、精洗、清洗、清洁、组装Ⅰ、光固Ⅰ、硅化Ⅰ、硅化Ⅱ、组装Ⅱ、检测、光固Ⅱ、单包装、中包装、大包装。源码仅有业务工序同名别名，没有可验证的非同名映射来源。
- 只读服务核对：活跃订单候选只校验已发布规程实际包含的工序，因此技术上可删减到单个同名项目；但发布会生成不可变版本，Controller/页面无删除、停用或作废入口。该路径会留下不可通过同一真实页面清理的裁剪规程，违反写入型 E2E 数据清理门禁，未执行。
- 禁止路径：未跨产品或路线借用旧规程，未改写现有 productId=924008 规程，未用 API/SQL 发布、补映射或清理，未删除 QA 默认项目以制造不完整发布成功。
- 写入审计：本轮 QA 保存/发布写请求数为 0；任务规程编码和 productId=14 在发布前均无规程记录。
- 独立验证结论：BLOCKED。M1-M4 保持完成，M5 需要业务方提供六个 QA 业务工序到当前正式路线工序的确认映射后才能继续。
- 最终数据库复核：`task_regulations=0`、`product14_regulations=0`；浏览器内填写没有形成草稿或发布记录。
- 会话与敏感产物清理：命名会话 `qa-detail-yudao-20260809` 已关闭并复核不存在；仅删除 `output/playwright/20260809-qa-inspection-detail-fields-yudao` 和 `output/runtime/int_main/inspection-20260809-qa-detail-current` 两个本任务目录，`Test-Path=False`。其它 Playwright 会话未关闭，`8081/48081` 非任务自有进程未停止。
- `project-experience-consolidation`：复核 `docs/e2e-rules.md` 的写入型任务自有数据清理门禁、`docs/backend-development.md` 的正式来源/禁止猜测门禁及 `docs/experience-index.md` 索引；现有规则已经覆盖本轮经验，未重复修改或新建长期经验文档。

## 未识别批记录绑定工序继续显示变更（2026-08-09）

- 用户澄清：组装螺杆八组件、光固外套四组件等是 QA 业务工序，只是对应批记录表单尚未识别绑定，也应继续显示。
- 变更决策：ACCEPTED；证据为 `docs/changes/20260809-qa-unbound-process-visible.md`，change request validator 与 self-test 均 PASS。
- BDD: 未识别批记录绑定的 QA 业务工序继续显示并发布 -> Given QA 来源列表包含“组装螺杆八组件”等业务工序且激活路线版本没有同名路线工序或批记录绑定 When 用户保存或发布 QA 规程 Then 页面继续显示原业务工序和全部检验项目，未匹配项目归入页面已正式解析的 QA 质检工序载荷，不伪造批记录绑定摘要，也不因名称未匹配而阻断。
- BDD: 已识别路线工序保持正式分组 -> Given 清洗、清洁等 QA 业务工序可以唯一匹配激活路线版本工序 When 用户保存或发布 QA 规程 Then 这些项目继续按匹配到的正式 `routeProcessId/processId` 独立分组，不被并入未识别工序组。
- TDD 下一步：新增聚焦静态合同并先取得 RED，预期失败原因为当前 `resolveQaRegulationItemRouteProcesses` 在零匹配时直接抛错。
- RED: `node tests/e2e/qa-regulation-unbound-process-visible-static.spec.cjs` -> FAIL，expected reason: 页面尚无未识别批记录绑定业务工序的显式允许清单，零匹配仍直接抛错。
- GREEN: `node tests/e2e/qa-regulation-unbound-process-visible-static.spec.cjs` -> PASS。
- GREEN: `node tests/e2e/qa-regulation-process-scoped-publish-static.spec.cjs` -> PASS，已识别工序仍按正式路线工序身份分组。
- GREEN: `node tests/e2e/frontline-pqc-sampling-equipment-dialog-static.spec.cjs` -> PASS，目标弹窗仍直接读取 QA 抽样方案与检验器具及设备。
- GREEN: `node tests/e2e/qa-regulation-applicable-types-derived-static.spec.js` -> PASS。
- REGRESSION: IDI 完整项目与逐页截图静态合同 -> PASS；`role-matrix-qa-regulation-tab-static.spec.cjs` -> PASS。
- REGRESSION: `qa-regulation-route-checkflag-fallback-static.spec.cjs` -> FAIL，失败断言仍要求旧结构 `const [routeProcesses, scheduleConfigs, batchConfigs] = await Promise.all(...)`；当前正式代码早已改为 `currentRouteProcesses + resolveQaVersionRouteProcesses(...)`，失败不涉及本次未绑定工序分支，未修改该旧合同。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: 任务范围 `git diff --check` -> PASS，仅有 LF/CRLF warning。
- Playwright CLI 前置：`npx` 可用，`8081` HTTP 200，`48081` health `UP`。命名 CLI 会话在登录后异常退回 `about:blank` 并自行关闭，按 `docs/e2e-rules.md` 改用任务自有 Playwright 脚本承载同一真实页面路径，未降级为 API-only。
- GREEN: 真实只读 Playwright -> 登录“芋道源码”，进入 `/mes/pro/process-pool/qa-regulation`，选择 `IDI / 按压式球囊扩充压力泵 / 1`；页面可见“组装螺杆八组件”“光固外套四组件”以及“检验器具及设备”“抽样方案”列。
- 真实页面网络与错误证据：`qaWriteRequests=[]`、`consoleErrors=[]`、`pageErrors=[]`；未点击会立即写入不可变版本的“发布规程”，没有 QA 保存/发布写请求。
- 视觉复核：1440×1000 截图确认工序列实际显示未识别批记录绑定的业务工序，表格布局无本次变更造成的重叠。
- 当前剩余门禁：本次用户新增“未识别工序继续显示”行为已完成并通过真实只读页面；原任务的一线 PQC 弹窗完整真实路径仍缺可通过页面清理的已发布 QA 数据，不能用不可变任务规程或 API/SQL 清理绕过。
- GREEN: frontend feature evidence validator -> PASS；validator self-test -> PASS。
- 最终数据库复核：`task_regulations=0`、`product14_regulations=0`，本轮仍无 QA 草稿或发布记录。
- Playwright 收尾：任务命名 CLI 会话已关闭；仅删除 `output/playwright/20260809-qa-unbound-process-visible` 本任务目录并复核 `Test-Path=False`，其中敏感登录快照、临时只读脚本、result 与截图一并删除；未关闭其它会话或停止 `8081/48081`。
- `project-experience-consolidation`：将“QA 业务工序与批记录表单绑定是独立事实；显式允许的未绑定工序继续显示、由唯一正式 QA 质检工序承载且绑定摘要为空，仍禁止猜测路线映射”的长期门禁合并到 `docs/backend-development.md`，并更新 `docs/experience-index.md`；未新建长期经验文档。

## 并发后端运行态字段校验回归（2026-08-09）

- 发现：最终复跑 `frontline-pqc-sampling-equipment-dialog-static.spec.cjs` 时，`MesFrontlinePqcContextServiceImpl` 已被并发业务改动重写；两个字段的构造映射仍存在，但 `toInspectionItem` 不再拒绝空白 `inspectionTool/samplingPlanText`。
- BDD: 一线运行态拒绝缺失 QA 展示原文 -> Given 已发布 QA 项目缺少检验器具及设备原文或抽样方案原文 When 一线 PQC 加载该项目 Then 服务按 `inspectionItem.inspectionTool` 或 `inspectionItem.samplingPlanText` 精确失败，不返回空白详情。
- RED: `node tests/e2e/frontline-pqc-sampling-equipment-dialog-static.spec.cjs` -> FAIL，expected reason: 当前并发版本只映射字段，缺少两条运行态 `StrUtil.isBlank` fail-fast 校验。
- 修复边界：只在当前 `toInspectionItem` 中补回两条字段校验，保留并发引入的 DCC 项目解析、QA productId 和任务选择逻辑，不回退其它代码。
- GREEN: `node tests/e2e/frontline-pqc-sampling-equipment-dialog-static.spec.cjs` -> PASS，运行态两个字段的映射与精确 fail-fast 合同恢复。
- BLOCKED: `mvn -pl yudao-module-mes "-Dtest=MesFrontlinePqcContextServiceTest" test` -> FAIL，39 tests 中 1 error；并发新增 `shouldListQaProcessWhenLegacyPublishedItemDisplayFieldsAreBlank` 明确断言历史空字段应返回 `null`，与原任务“历史缺正式原文必须按字段路径 fail fast”验收互斥。
- 冲突处理：未删除或改写并发测试，未用版本/日期/默认值条件兼容旧空字段，也未回退本任务精确校验；需要用户确认历史空字段的最终产品口径。

## 芋道源码 E2E 复验（2026-08-09）

- 用户指令：在芋道源码里进行 E2E 验证。
- 技能与门禁：使用 `playwright` 操作真实前端页面，使用 `independent-verification-gate` 区分 QA 列表只读证据与一线 PQC 弹窗完整证据；静态合同和 API 查询均不得替代真实页面。
- 数据范围：允许登录“芋道源码”并只读复用现有 IDI 项目、路线和 QA 来源项目；如发布规程无法通过真实页面删除、停用或作废，则禁止创建不可清理的测试规程，禁止用 API、SQL 或 mock 发布/清理。
- 预期断言：项目选择项为 `IDI / 按压式球囊扩充压力泵 / 1`；QA 检验项目页可见“组装螺杆八组件”“光固外套四组件”“检验器具及设备”“抽样方案”，并核对真实来源值；目标 QA 写请求数为 0，目标链路错误、`consoleErrors` 与 `pageErrors` 均为空。
- 前置：`npx` 解析到 `D:\Programs\npx.ps1`；`8081` HTTP 200；`48081` health `UP`；Chrome `151.0.7922.76` 可启动；任务输出目录运行前不存在。
- Windows CLI 处置：本任务先前已确认命名 CLI 会话在登录后丢失，按 `docs/e2e-rules.md` 使用任务自有 Playwright 脚本承载同一真实页面路径，未降级为 API-only。
- 脚本边界修正：首次运行误把路线文本 `RT000028-IDI` 设为本轮硬断言；第二次误把输入框 `value` 当文本节点读取。修正后按表头定位首行单元格并读取真实输入值，不修改产品源码或业务数据。
- GREEN: QA 来源真实页面 -> 登录 `芋道源码/admin`，选择 `IDI / 按压式球囊扩充压力泵 / 1`；页面加载 `RT000028-IDI / V1`，真实显示“组装螺杆八组件”“光固外套四组件”“检验器具及设备”“抽样方案”。
- GREEN: QA 正式来源值 -> 首行“检验器具及设备”为 `目测`，首行“抽样方案”为 `GB/T 2828.1，I，AQL=0.4`；1920×1080 截图确认两列及值可见，布局无重叠。
- 一线 PQC 真实入口 -> `/mes/pro/feedback/edhr-batch-pqc-fill` 加载 7 个活跃订单，其中 5 个为“按压式球囊扩充压力泵”的 `CODX-AO5-20260807-*` 订单。
- BLOCKED: 抽查 `CODX-AO5-20260807-05`、`-04`、`-03`，对应工序接口均 HTTP 200，但真实页面一致显示 `设备账号上下文不完整或不一致：routeProjectItems routeId=980091，missingItemIds=[14]`；未渲染检验方法按钮，无法打开目标详情弹窗。三项一致结果排除单订单异常。
- 请求审计：`qaWriteRequests=[]`、业务数据写请求 `[]`、目标请求失败 `[]`、目标 HTTP 失败 `[]`、`consoleErrors=[]`、`pageErrors=[]`。首页切换到目标页时被浏览器取消的非目标请求均为 `net::ERR_ABORTED`，未影响 QA/PQC 目标接口和断言。
- 上下文 POST 说明：页面自动调用一次 `/mes/pro/feedback/frontline/device-account/pqc/switch-employee`；源码核对 `MesFrontlinePqcContextServiceImpl.switchPqcActualEmployee` 只校验上下文并构造返回对象，没有 Mapper/DAO 写入或事务持久化，因此单独记录为非持久化上下文解析，不冒充业务数据零请求。
- 独立门禁结论：BLOCKED。QA 来源页面行为通过；一线 PQC 完整详情弹窗缺正式 `routeProjectItems` 绑定前置，且后端历史空字段产品口径冲突仍存在，不能判定端到端完成。
- `project-experience-consolidation`：将“活跃订单可见不代表一线 PQC 上下文可用；必须验证路线产品项目绑定、不能用 HTTP 200 覆盖页面业务拒绝、非持久化 `pqc/switch-employee` POST 需单独记录”的长期门禁合并到 `docs/e2e-rules.md`，并更新 `docs/experience-index.md`；未新建长期经验文档。
- Playwright 收尾：任务自有浏览器已由脚本关闭；仅删除 `output/playwright/20260809-qa-detail-yudao-e2e`，其中临时脚本、result 和登录后截图一并清理，`Test-Path=False`。未关闭其它 Playwright 会话，未停止或重启 `8081/48081`。
