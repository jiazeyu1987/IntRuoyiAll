# C00 契约与 Schema 基线

## Task Goal

交付一线 PQC DCC-QA 链路的最小数据库契约基线：路线-DCC正式关系、活跃订单 QA 锁定快照、PQC task 规则身份与正式提交闭环约束。C00 只负责 schema、SQL证据脚本和静态 schema 测试，不实现业务 service。

## Milestones

- M0 启动门禁：读取 AGENTS、后端/数据库/PowerShell/收尾规则、C00设计包和共享接口合同。
- M1 BDD/RED：记录 Given/When/Then，并在实现前运行指定 RED 命令。
- M2 Schema增量：新增后继 SQL migration、preflight/backfill/postflight/rollback dry-run 脚本和静态 schema 断言。
- M3 GREEN/回归：运行 C00 GREEN、回归命令和 SQL dry-run 静态证据检查。
- M4 交付主管复审：记录 changed paths、验证证据、风险和阻塞；不提交、不合并、不清理 worktree。

## Expected Verification

- RED：`mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesQaPqcSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- GREEN：同 RED 命令。
- Regression：`mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesQaInspectionRegulationServiceTest,MesQaPqcSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- SQL evidence：检查任务自有 preflight/backfill/postflight/rollback dry-run SQL 包含 release metadata、输入清单 hash、影响行数、阻塞清单和执行顺序。

## Design Constraints Check

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，建立正式 schema 契约，阻止后续 Agent 重复推算或创建平行关系模型。
- 是否存在临时补丁或绕过：否。

## Applicable Experience Gates

- 运行态迁移漂移系统异常门禁：C00 只能交付正式 migration/schema 契约和同一合同的 RED/GREEN 证据；不得以业务 fallback、默认 ID、手工单列补丁或遗漏生成列/索引来掩盖 schema 缺口。
- PowerShell Maven -D 参数引号门禁：所有 Maven `-D...` 参数必须整体加双引号，并保留 `-pl yudao-module-mes -am`。
- Maven 单模块陈旧依赖门禁：验证必须使用 reactor `-am`，不能把上游陈旧依赖或未进入 Surefire 的编译失败记录为业务 RED/GREEN。
- Maven 静态源码合同工作目录门禁：静态 schema 测试读取 SQL 时必须兼容仓库根和模块根两种 `user.dir`。
- Windows Maven 增量输出删除卡住门禁：若 Maven 长时间无输出且无 surefire 报告，只能诊断并停止当前任务启动的 Maven PID，不得宣称通过或叠加命令。

## Current Status

ready_for_supervisor_review

## Completed Work

- Added C00 schema migration and four task-owned dry-run evidence scripts under `IntRuoyiBackend/sql/mysql/`.
- Added C00 static schema assertions to `MesQaPqcSchemaTest`.
- Recorded BDD, RED, GREEN, regression, SQL static checks, and database-schema evidence.
