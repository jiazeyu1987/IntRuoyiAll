# Execution Log

- 任务由用户于 2026-09-18 明确授权执行 DCC 作废完整 E2E。
- 已读取 `AGENTS.md`、`docs/e2e-rules.md`、`docs/login-access.md`、`docs/local-runtime.md`、`docs/worktree-restrictions.md`、`docs/branch-runtime-ports.md` 和 `docs/task-closeout-rules.md`。
- 当前分支：`int_qms`。本任务使用前端 `http://127.0.0.1:8061`、后端 `http://127.0.0.1:48061`。
- 工作区在任务开始前已存在大量与 DCC 相关的未提交改动；本任务不修改、不回滚、不提交这些既有改动。
- BDD: 受控文件作废完整闭环 -> Given/When/Then 已写入 `task.md`。
- RED/GREEN：本任务不修改生产代码；将记录真实 Playwright 运行结果，不以静态合同或 API-only 结果替代。
- RED: 真实 Playwright 只读样本探针（`dcc-obsolete-real-sample-probe.cjs`）首次因本机未安装 Playwright Chromium 启动失败；改用已安装的 Chrome 可执行文件重跑。
- Playwright 登录：PASS，前端 `http://127.0.0.1:8061`，租户“芋道源码”，账号标签 `admin`。
- 浏览页自然请求：`GET /admin-api/dcc/controlled-files/browser-page?pageNo=1&pageSize=50&latestVersionOnly=true` 返回 HTTP 200、业务码 500；页面显示“系统异常”，`pageErrors` 记录“系统异常”。
- 真实业务写请求：0；未点击作废入口、未提交作废申请、未进入审批确认，未改变业务数据。
- 运行态阻断：`48061` PID 8836 使用 `IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar`；后端日志明确报 `Unknown column 'revision_code' in 'field list'`。
- 运行态 schema/代码不一致：当前 DCC 源码和 `20260906_dcc_new_file_lifecycle_p1.sql` 要求 `dcc_controlled_file.revision_code`，当前运行数据库缺少该列。
- 结论：完整作废 E2E 在业务写入前 `BLOCKED`。不得用 API、数据库写入、旧运行态或其它端口替代真实页面路径。
- 收尾：当前环境未提供 `task-closeout-cleanup` 脚本或工具，未执行 cleanup preview/apply；未将其写成 PASS。
- 最终状态：blocked，等待运行态 schema 与 Jar 对齐后重跑。

## 2026-09-18 23:xx - User authorization resumed

- 用户明确授权执行数据库正式迁移及重启当前任务所属的 `int_qms` 后端 `48061`。
- 业务动作仍必须全部由 Playwright 在真实前端页面完成；数据库仅用于正式迁移和只读核验。

## 2026-09-19 - Runtime schema migration and restart verification

- 只读前置核验：实际运行库为 Docker `int-ruoyi-mysql` 的 `ruoyi-vue-pro`；`dcc_controlled_file`、`dcc_controlled_file_master` 和 `system_tenant` 均存在；P1 目标字段 `revision_code`、`iteration_no`、`file_type_taxonomy_leaf_id`、`normalized_file_number` 缺失。
- 正式迁移：执行 `IntRuoyiBackend/sql/mysql/20260906_dcc_new_file_lifecycle_p1.sql`，退出码 `0`；未执行手工单列补丁。
- 迁移后只读核验：P1 字段与 `uk_dcc_new_logical_file_identity`、`idx_dcc_master_new_identity_lookup` 已存在；`dcc_controlled_file` 行数 `41572`、`dcc_controlled_file_master` 行数 `36657`，迁移未回填历史业务数据。
- Migration policy gate：使用工作区 Python 运行全量 `sql/mysql` gate，`status=passed`、`migrationCount=623`；证据为 `migration-policy-gate.json`。
- 重启第一次失败：旧 `target/yudao-server-exec.jar` 与当前源码不一致，启动先报 DCC 签名证据配置缺失；未关闭校验。
- 构建：`mvn -pl yudao-server -am clean package -DskipTests` 首次因无关 `yudao-module-bpm\\target` 文件锁失败；未删除无关目标目录。随后 `mvn -pl yudao-server -am package -DskipTests` 成功，重新生成当前源码对应 Jar；新 DCC 模块包不含旧 `DccDownloadEncryptionProperties` class。
- 重启第二次失败：为当前任务 `48061` 注入本地正式 DCC 签名配置后，后端继续在 MES 启动门禁失败：缺少 `MES_EDHR_INDEPENDENT_RECEIPT_ISSUER_SYSTEM`；对应外部 `D:/ProjectPackage/Int/IntPP/backend/.env` 不存在，进程/User/Machine 环境均未配置该项及其 signing secret。
- 严格阻断：不得猜测 MES issuer/secret、不得使用默认值、不得关闭 fail-fast、不得使用旧 Jar 或其它端口替代当前源码运行态；当前 `48061` 未监听，前端 `8061` 仍在运行，`int_main` 的 `48081` 未触碰。
- Playwright 完整作废流程：仍为未执行业务写入；既有真实页面探针登录 PASS、浏览页业务码 `500` 的失败证据保持有效。本轮没有 API/DB 代替作废申请、审批或最终断言。
- 最终状态：`blocked`，等待运行态提供合法 MES 独立凭证配置后，重新健康检查并从真实页面重跑完整作废闭环。

## 2026-09-19 - Continue recheck after user request

- 用户要求继续后重新复核运行态：`8061` 前端仍在监听，`48061` 后端未监听，`48081` 当前也未监听；本任务未停止或重启 `int_main`。
- 重新复核本机环境：`MES_EDHR_INDEPENDENT_RECEIPT_ISSUER_SYSTEM` 和 `MES_EDHR_INDEPENDENT_RECEIPT_SIGNING_SECRET` 在 Process/User/Machine 环境均不存在。
- 重新搜索当前仓库相关配置：仅 `IntRuoyiBackend/yudao-server/src/main/resources/application-local.yaml` 声明两个无默认值占位符，未发现合法本地取值来源。
- 阻断维持：不能猜测 MES issuer/secret、不能写默认值、不能关闭 fail-fast、不能用旧 Jar 或 API/DB 替代真实前端作废流程。

## 2026-09-19 - User requested full void-flow rerun

- 用户再次明确要求完整走一遍作废流程；沿用本任务目录，不新建语义重复任务。
- 重新核对端口：`8061` 前端监听，`48061` 后端监听；本轮不触碰 `8081/48081`。
- 当前状态恢复为 `in_progress`，下一步通过真实 Playwright 页面登录并从 DCC 受控文件入口继续。
- 新增一次性真实页面脚本 `dcc-void-real-ui.e2e.cjs`，显式从前端 `node_modules/playwright` 加载 Playwright；脚本仅通过页面操作完成业务动作，接口只做自然请求监听和证据记录。
- 首次命令路径写错、第二次脚本依赖解析失败，均未启动业务浏览器或执行 DCC 动作；修正后真实运行。
- 真实 Playwright：登录 `http://127.0.0.1:8061`、租户“芋道源码”、账号标签 `admin` PASS。
- 真实页面进入 DCC 受控浏览页后未出现可作废候选；页面截图显示目录树仍在加载，列表空态为“无权限或无匹配当前有效文件”。结构化证据 `dcc-void-real-ui-2026-09-19T07-41-19-477Z.json`，截图/trace 位于 `artifacts-2026-09-19T07-41-19-477Z/`。
- 复跑仓库只读探针 `tests/e2e/dcc-obsolete-real-sample-probe.cjs`，同一租户/账号/端口下等待 `/dcc/controlled-files/browser-page` 超时；既有探针证据仍显示该接口业务码 `500`、页面错误“系统异常”。
- 后端日志定位：当前 `48061` 运行态的 `/admin-api/dcc/controlled-files/browser-page` 查询缺少正式表 `dcc_publication_followup_batch`，报 `Table 'ruoyi-vue-pro.dcc_publication_followup_batch' doesn't exist`；更早还出现 `dcc_controlled_file_related_file` 缺表。
- 业务写入：本轮未点击作废、未提交作废申请、未审批；除登录外未产生 DCC 写请求。不得用 API/DB 或共享数据补齐候选。
- 最终状态：`blocked`，等待运行库正式 DCC schema 补齐并能通过真实受控浏览页展示可作废文件后重跑。

## 2026-09-19 - Authorized continuation for schema drift repair

- 用户回复“授权继续”；本轮授权范围用于修复当前运行库正式 DCC schema 漂移并重跑真实页面 E2E，不授权造业务数据、API 替代业务动作、Git 提交/推送或远端操作。
- 已读取 `docs/database-rules.md`。
- 缺失表对应正式迁移文件已定位：`IntRuoyiBackend/sql/mysql/20260903_dcc_controlled_file_related_file.sql` 与 `IntRuoyiBackend/sql/mysql/20260907_dcc_publication_followup.sql`。
- 当前状态恢复为 `in_progress`，下一步执行迁移门禁、运行库 schema RED、正式迁移和真实页面复验。

## 2026-09-19 - Schema recheck and real precondition approval

- 运行库只读复核：`dcc_controlled_file_related_file`、`dcc_publication_followup_batch` 及其余 DCC 发布后续表均已存在；未执行额外业务数据写入。
- 全量 migration policy gate PASS，`migrationCount=623`；证据为 `migration-policy-gate-dcc-followup.json`。
- 真实 Playwright 登录 PASS：租户“芋道源码”，账号标签 `admin`，前端 `http://127.0.0.1:8061`，后端 `http://127.0.0.1:48061`。
- 为获得任务自有的可审批样本，使用文件 `2054545668044083986`（文件编号 `CODEX-E2E-OBSOLETE-20260919-FIX2`）的真实详情页完成三阶段外部评审审批：
  - “文控审核”页面点击“审核通过”，输入签名密码和审批意见，确认签名。
  - “会签批准”页面点击“批准通过”，输入签名密码和审批意见，确认签名。
  - “文控批准”页面填写接收结论、审批意见、评审结论，上传任务自有 `dcc-obsolete-external-review-output.docx`，确认签名。
- 上述动作均由 Playwright 真实页面完成；页面自然请求返回成功，未由脚本直接调用业务 API。数据库只读核验显示该文件最终状态为 `APPROVED`，`published_file_id`、`stamped_file_id`、`published_time` 均为空。
- 末级审批完成后的真实详情页显示“发布文件：缺失或尚未生成”“盖章文件：缺失或尚未生成”，当前有效版为 `-`，没有“风险操作”或“作废当前版本”入口。前端生命周期枚举也不接收 `APPROVED` 作为可管理状态，页面无法继续进入作废链路。
- 证据截图：`hidden-approval-detail.png`、`precondition-approval-after-submit.png`、`precondition-approval-step2.png`、`precondition-approval-step3.png`、`precondition-approval-step3-uploaded.png`、`precondition-final-state.png`、`precondition-active.png`。
- 外部评审输出文件：[dcc-obsolete-external-review-output.docx](dcc-obsolete-external-review-output.docx)。按文档技能尝试渲染，但环境未找到 LibreOffice/`soffice.exe`，因此文档渲染 QA 未通过且未冒充通过。
- 未执行作废原因填写、影响确认、作废申请提交、作废审批和作废终态核验；未产生作废业务写入。
- 代码归因：`DccExternalFileReviewStatusListener` 在外部评审流程 APPROVE 事件中将文件置为 `APPROVED` 并关闭外部评审；该流程没有将文件送入受控文件最终化监听器所要求的 `ACTIVE/READY_TO_PUBLISH` 路径。当前运行服务未提供可由页面触发的后置发布/生成入口。
- 最终状态：`blocked`。阻断点是前置文件无法成为页面可作废的当前有效版本，继续点击或绕过页面都会违反本任务 E2E 约束。

## 2026-09-19 - 修复后继续真实页面

- 修复内容：外部评审 APPROVE 事件改为进入 `READY_TO_PUBLISH`；发布生效前置不再要求发布件和盖章件已提前绑定，允许正式生效动作生成这些产物。
- RED/GREEN：定向 Maven 测试首轮 RED 后完成修复；定向测试 48/48 PASS；`yudao-server` 打包 PASS；当前源码 Jar 已重启到 `48061`，健康检查 PASS。
- 真实 Playwright 页面创建：PASS。租户“芋道源码”、账号标签 `admin`，通过 `/dcc/controlled-file/external-review` 页面填写来源、归属、原因、参与人、任务类别、提交目录、文件信息、DCC 项目、版本、生效日期，并通过页面上传任务自有 PDF 源文件和图纸 PDF。
- 页面提交结果：PASS。任务自有文件编号为 `CODEX-E2E-OBSOLETE-2026-09-19T11-28-53-590Z`，类别为“CODEX 作废 E2E 20260919”，页面提示“外来文件评审已提交”；trace 和截图保存在 `external-review-artifacts-2026-09-19T11-28-53-590Z`。
- 新阻断：该外部评审任务未出现在 `/approval-center/todo?moduleCode=DCC` 和 `/dcc/controlled-file/approval-tasks` 的当前可见待办列表；本轮未调用 API/DB 代替审批，也未继续作废动作。完整作废闭环仍为 `BLOCKED`，等待定位外部评审的正式页面审批入口。

## 2026-09-20 - Approval center projection fixed, real page approval resumed

- 修复内容：审批中心 DCC 适配器同时查询 `dcc-controlled-file-approval` 与 `dcc-external-file-review` 两类流程定义；外部评审待办在审批中心仅暴露“查看/流程”，处理动作回到 DCC 模块详情页完成。
- RED/GREEN：`DccApprovalTaskAdapterTest` 覆盖外部评审待办投影和双流程定义查询，18/18 PASS；`mvn -pl yudao-server -am -DskipTests package` PASS。
- 当前源码 Jar 已重启到任务所属 `48061`，health HTTP 200；未停止或重启 `int_main`。
- 真实 Playwright 页面复验：审批中心 DCC 待办已显示 `CODEX-E2E-OBSOLETE-2026-09-19T11-28-53-590Z`，行操作为“查看/流程”。
- 真实页面完成外部评审第 1 个“文控审核”节点：在 DCC 详情页点击“审核通过”，输入签名密码和审批意见后确认签名；页面提示“外来文件评审已签名通过”，文件进入“待会签审核”。
- 证据截图：`approval-center-after-fix.png`、`external-review-detail-after-fix.png`、`external-review-step1-dialog.png`、`external-review-step1-passed.png`。
- 当前状态：`in_progress`。下一步继续通过真实页面完成“审核会签”、后续批准节点、发布生效申请与作废完整闭环。

## 2026-09-20 - Full obsolete flow PASS

- 新任务自有样本：`CODEX-E2E-OBSOLETE-2026-09-19T18-52-52-266Z`，受控文件 ID `2054545668044083999`。
- 真实页面完成外部评审全链路：文控审核、审核会签、会签批准、文控批准均由 Playwright 在真实 DCC 详情页输入审批意见、签名密码并确认；末级上传任务自有 `dcc-obsolete-external-review-output.docx`。
- 真实页面完成生效申请：样本进入当前有效版本，详情页显示 ACTIVE V1.0 和生效时间。
- 真实页面提交作废申请：详情页“风险操作/作废当前版本”打开作废面板，表单中心创建实例 `FCI-1-1789847768616`，BPM 流程实例 `2a251b17-b464-11f1-8d4f-b082e25ec548`。
- 修复内容：`ActionFormPanel.vue` 允许 `requiresBpm=true` 且 `requiresForm=false` 创建表单中心实例；`DccApprovalTaskAdapter` 支持从作废 BPM 流程变量 `objectId` 映射受控文件，按 DCC 文件标题/编号过滤关键词，并将作废 review 分流到 BPM 任务服务。
- RED/GREEN：`DccApprovalTaskAdapterTest` 新增作废表单 BPM 映射、关键词过滤、作废 approve/reject 分流覆盖；最终 22/22 PASS。
- 构建：`mvn -pl yudao-server -am -DskipTests package` PASS；任务专属后端 `48061` 重启 health HTTP 200。未停止或重启 `int_main`/`48081`。
- 真实页面作废审批：审批中心 DCC 待办显示目标作废任务；Playwright 点击“审核”，填写意见和签名密码，提交 `/approval-center/tasks/review` 返回业务码 `0`、`data=true`。
- 终态验收：再次打开审批中心 DCC 待办，目标任务总数为 `0`；打开受控浏览 `status=OBSOLETE` + 目标文件编号，页面显示目标文件和“已作废/作废”状态；打开受控浏览 `status=ACTIVE` + 目标文件编号，表格有效数据行数为 `0`。
- 最终证据：`obsolete-approval-2026-09-19T22-22-32-104Z/result.json`，截图 `03-obsolete-browser.png`、`04-obsolete-removed-from-active.png`。
- 当前状态：`completed`。最终核对确认终态结果 JSON 为 PASS，任务文档和验证报告已更新，任务专属后端 `48061` health 仍为 UP。
