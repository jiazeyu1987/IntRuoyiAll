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

待补充。每个里程碑必须记录实际命令、退出码、首个失败原因和通过证据，不以静态阅读代替测试。

## Milestone Updates

### M1 - 任务与边界

- Status: `in_progress`
- Completed: 创建任务文档，记录用户目标、BDD 场景、预期验证和设计约束。
- Verification: 待补充代码边界、schema 和经验文档核对结果。
- Blockers: 无。

## Evidence

- Backend API evidence: 待生成。
- Database schema evidence: 待生成。
- Frontend feature evidence: 待生成。
- QA evidence: 待生成。
- Verification report: 待生成。

## Closeout

- Current status: `in_progress`
- Cleanup keep: `task.md`
- Cleanup keep: `execution-log.md`
- Cleanup keep: `verification-report.md`
