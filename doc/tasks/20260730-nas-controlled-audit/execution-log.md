# Execution Log

## User Intent

在 NAS 管理页面增加“统计未受控文件”按钮。固定扫描 `1. QMS documents`、`2.DHF`、`3.DMR`，异步执行，子文件夹没有访问权限时跳过并记录，根目录或 NAS 连接失败时任务失败，最终下载包含汇总和明细的 Excel 文件。

## Preflight

- 工作区：`E:\IntRuoyi`，共享分支 `int_main`。
- 后端：`E:\IntRuoyi\IntRuoyiBackend`。
- 前端：`E:\IntRuoyi\IntRuoyiFronted`。
- 已发现工作区存在其他任务的既有文档改动和本地分支领先远端提交；本任务不得回滚、覆盖或混入这些改动。
- 已读取任务收尾、后端、前端、数据库、E2E、PowerShell 编码/编排规则及本任务使用的交付技能。
- `docs/experience-index.md` 存在；任务启动后只读取与 DCC/NAS、Excel、数据库迁移、异步任务和前端静态合同直接相关的经验。
- 本任务尚未执行构建、测试、远端写入或 NAS 修改。

## BDD Scenarios

BDD: 点击按钮创建异步统计任务 -> Given 用户拥有受控文件查询权限和 NAS 查询权限，When 点击“统计未受控文件”并确认，Then 后端创建任务并返回任务编号，页面进入轮询状态。

BDD: 子目录无权限时跳过并继续 -> Given 三个根目录可访问且某个子目录返回 `ACCESS_DENIED`，When 扫描该目录树，Then 跳过该子目录及其子树，记录路径、原因、时间，并继续扫描同级可访问目录。

BDD: 根目录或 NAS 连接失败 -> Given NAS 连接、共享、固定根目录或根目录访问失败，When 创建的任务开始扫描，Then 任务状态为失败并保留真实失败原因，不生成报告或伪造统计结果。

BDD: 精确路径对应一个 ACTIVE 受控文件 -> Given 一个可访问 NAS 文件的标准化相对路径精确对应一个当前 `ACTIVE` 受控文件，When 统计匹配，Then 文件状态为 `CONTROLLED`。

BDD: 精确路径没有受控文件 -> Given 一个可访问 NAS 文件的标准化相对路径没有当前 `ACTIVE` 受控文件，When 统计匹配，Then 文件状态为 `NOT_CONTROLLED` 并进入未受控明细。

BDD: 精确路径对应多个受控记录 -> Given 一个标准化 NAS 路径对应多个受控记录，When 统计匹配，Then 文件状态为 `AMBIGUOUS`，进入待确认明细且不计入已受控或未受控。

BDD: 无权限子树不计入未受控 -> Given 文件位于被跳过的无权限子目录内，When 生成汇总，Then 该文件不计入文件总数、未受控数量或待确认数量，并以跳过目录记录体现未知范围。

BDD: 来源映射缺失 -> Given 当前 `ACTIVE` 受控文件有正式 NAS 来源映射但扫描不到对应 NAS 文件，When 统计完成，Then 进入来源缺失明细并计入来源缺失数量。

BDD: 报告汇总与明细一致 -> Given 扫描成功并生成报告，When 打开 Excel 各工作表，Then 汇总中的未受控、待确认、来源缺失和跳过目录数量分别与对应明细工作表行数一致。

BDD: NAS 转移来源同事务落库 -> Given NAS 转移成功创建受控文件，When 事务提交，Then 受控文件和精确来源映射同时提交；任一写入失败时整体失败，不产生孤立来源映射。

## RED / GREEN / REGRESSION

RED: `mvn -pl yudao-module-infra -Dtest=NasRecursiveScanServiceTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, PowerShell 将 `-Dsurefire.failIfNoSpecifiedTests=false` 解析为非法 lifecycle phase `.failIfNoSpecifiedTests=false`，按 Maven `-D` 参数门禁改用引号重跑。

RED: `mvn -pl yudao-module-dcc -Dtest=DccNasControlAuditControllerTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, PowerShell 将 `-Dsurefire.failIfNoSpecifiedTests=false` 解析为非法 lifecycle phase `.failIfNoSpecifiedTests=false`，按 Maven `-D` 参数门禁改用引号重跑。

RED: `node E:\IntRuoyi\IntRuoyiFronted\tests\e2e\nas-control-audit-static.spec.js` -> FAIL, 首个失败原因：NAS 管理页缺少独立的“统计未受控文件”按钮。

RED: `mvn -pl yudao-module-infra "-Dtest=NasRecursiveScanServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 首个失败原因：缺少 `NasRecursiveScanServiceImpl` 与 `NasRecursiveScanHandler`。

RED: `mvn -pl yudao-module-dcc "-Dtest=DccNasControlAuditControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 首个失败原因：缺少 `DccNasControlAuditTaskRespVO`、`DccNasControlAuditService` 与 `DccNasControlAuditController`。

GREEN: `node E:\IntRuoyi\IntRuoyiFronted\tests\e2e\nas-control-audit-static.spec.js` -> PASS, 前端静态合同确认独立“统计未受控文件”按钮、确认提示、任务轮询、自动下载/重新下载、失败原因展示和 NAS 统计 API wrapper。

REGRESSION: `git diff --check -- IntRuoyiFronted/src/views/system/nas/index.vue IntRuoyiFronted/src/api/system/nas/index.ts IntRuoyiFronted/tests/e2e/nas-control-audit-static.spec.js IntRuoyiBackend/yudao-module-dcc/pom.xml IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileNasTransferServiceTest.java` -> PASS。

BLOCKED: `mvn -pl yudao-module-infra -am "-Dtest=NasRecursiveScanServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> TIMEOUT after 180s, 未生成本测试 surefire 报告；线程栈显示本任务 Maven 进程停在 Java 文件描述符关闭/Windows 文件系统操作，已只终止本任务启动的 Maven 进程。

BLOCKED: `mvn -pl yudao-module-dcc -am "-Dtest=DccNasControlAuditControllerTest,DccBaseSchemaTest,DccControlledFileNasTransferServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> TIMEOUT after 180s, 未生成本次目标测试 surefire 报告；线程栈显示主线程停在 `WinNTFileSystem.delete0` / `IncrementalBuildHelper.beforeRebuildExecution`，已只终止本任务启动的 Maven 进程。

BLOCKED: `mvn -pl yudao-module-dcc -am "-DskipTests" "-Dmaven.compiler.useIncrementalCompilation=false" compile` -> FAIL, 诊断性编译失败在共享框架模块 `yudao-spring-boot-starter-web`，首个失败为 `ChineseNameDesensitize.class` 类文件 0 字节截断，后续 Lombok 生成方法/日志符号缺失为同一共享构建目录不一致症状；该命令不作为标准 GREEN。

BLOCKED: `pnpm ts:check` -> TIMEOUT after 180s，已只终止本任务启动的 `vue-tsc` 进程；不得将该命令记录为通过。

## Milestone Updates

### M1 - 任务与边界

- Status: `completed`
- Completed: 创建任务文档，记录用户目标、BDD 场景、预期验证、设计约束，并补入适用经验门禁摘要。
- Verification: 定位 NAS 单连接入口 `NasBrowserService.executeInSession(...)`、DCC NAS 转移提交链路 `DccControlledFileNasTransferServiceImpl#processFileItem`、受控文件当前版本字段 `dcc_controlled_file_master.current_active_controlled_file_id`、NAS 管理页面 `src/views/system/nas/index.vue`。
- Blockers: 无。

### M2 - RED 合同测试

- Status: `completed`
- Completed: 新增 infra 递归扫描 RED 单元测试、DCC 控制器 RED 合同测试、前端 NAS 统计按钮 RED 静态合同。
- Verification: 见 RED 记录。
- Blockers: 无。

### M3-M6 - 功能实现

- Status: `completed`
- Completed: 已新增 NAS 来源映射、统计任务持久化、子目录无权限跳过记录、DCC 统计服务/控制器/Excel 报告、NAS 转移同事务来源映射、前端按钮/确认/轮询/下载/失败展示。
- Verification: 前端任务专用静态合同已 PASS；后端目标 JUnit 受本机 Maven/共享构建目录阻塞，尚未取得 PASS。
- Blockers: 本机同仓 `restart-int-ruoyi-local.ps1 -Component full` 仍有 Maven 进程占用同一后端构建目录，主线程卡在 `WinNTFileSystem.delete0 / IncrementalBuildHelper.beforeRebuildExecution`，`48081` 后端未监听。

### M7 - E2E 验证

- Status: `blocked`
- Completed: 已确认本机前端 `http://127.0.0.1:8081/` HTTP 200，`npx`、项目 Playwright 依赖和本机 Chrome 可用。
- Verification: `Invoke-RestMethod http://127.0.0.1:48081/actuator/health` -> FAIL，连接被拒绝；真实 NAS 管理页面 E2E 未执行。
- Blockers: 缺少可用后端运行态、登录前置和真实 NAS 只读授权证据；不得用静态合同、API-only 或前端空页面冒充真实 E2E。

## Evidence

- Backend API evidence: 待生成。
- Database schema evidence: 待生成。
- Frontend feature evidence: `node E:\IntRuoyi\IntRuoyiFronted\tests\e2e\nas-control-audit-static.spec.js` PASS；`pnpm ts:check` 超时未通过。
- QA evidence: 真实 E2E 当前 BLOCKED，阻塞原因为本机后端运行态缺失且同仓重启 Maven 卡住。
- Verification report: `verification-report.md` 已生成草稿并标记 BLOCKED。

## Closeout

- Current status: `blocked_on_runtime`
- Cleanup keep: `task.md`
- Cleanup keep: `execution-log.md`
- Cleanup keep: `verification-report.md`
