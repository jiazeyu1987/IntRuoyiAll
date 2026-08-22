# 流程修复 10：放行终态与追溯闭环

## 任务目标

- 只读审计放行完成后的最终状态、权限、并发、快照、审计和追溯出口。
- 设计多放行入口共用的唯一终态合同，并与流程修复 7、8、9、11 对接。
- 本任务只写开发文档，不修改生产代码、数据库、配置或运行环境。

## 里程碑

- [x] M1：读取项目规则、产品规则、开发规则和适用经验门禁。
- [x] M2：完成当前代码与相邻流程任务的只读审计。
- [x] M3：完成目标态、根因、修改边界、接口/数据/状态设计。
- [x] M4：完成 BDD、RED/GREEN/REGRESSION、迁移/回滚和 blocker 设计。
- [x] M5：完成文档结构与一致性验证。

## 预期验证

- 指定任务目录包含 `task.md`、`development-plan.md`、`test-plan.md`、`execution-log.md`、`verification-report.md`。
- 五份文档覆盖用户要求的全部设计主题，且状态、接口、幂等、追溯和跨线程合同互相一致。
- 实现验证使用 Maven 定向编译和合同测试；不启动服务、不运行写入型 E2E。

## 适用经验门禁

- 正式来源：完成节点前不得回填或创建批次，批记录只用工序正式绑定来源，三类回填成功后才创建/复用批次。
- 终态待办：`CLOSED`、`ARCHIVED`、`REJECTED`、`VOIDED` 对象不得保留可办入口。
- 放行资料：四份独立正式材料必须齐套；不得以旧“三份材料”表述、前端开关或模拟文件降级。
- E2E：后续实现仅可在任务自有测试环境执行真实多角色和签名凭据路径；本次只读文档任务未执行 E2E。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；以唯一状态所有者、统一终态命令、正式来源快照、条件唯一约束和可验证追溯为主线。
- 是否存在临时补丁或绕过：否。

## Current Status

ready_for_closeout：流程10专项实现、融合和主线程验证已完成；跨流程权威适配器、迁移、outbox 和全链路 E2E 仍 No-Go。

### 主流程统一冻结合同（2026-08-22）

流程10拥有唯一最终放行状态和 release manifest/签名审计。活跃订单关系仅在适用时要求；独立批次不得因无 activeOrderId 被拒绝。独立追溯显示 originType、独立凭证、工单/路线/批号、来源快照、适用事实、三类回填、四材料版本/hash、放行决定和审计链；不适用关系返回 `NOT_APPLICABLE`+原因码，应有关系缺失返回 `MISSING/BLOCKED`。

流程10专项实现、融合和主线程验证完成：代码以 `7f3547c17` fast-forward 融合到 `int_main`；主线程 Maven compile BUILD SUCCESS，流程10 focused suite 45/45 PASS，流程6/8/9/审批中心合同 suite 29/29 PASS，commit diff-check 与 runtime guard PASS。流程4/6/8权威凭证适配器、生产迁移/历史回填、outbox 投递和全链路真实 E2E 仍为 No-Go；本状态不宣称全链路完成。

## Cleanup Keep

- doc/tasks/20260821-flow-repair-10-final-release-state-and-trace/development-plan.md
- doc/tasks/20260821-flow-repair-10-final-release-state-and-trace/test-plan.md

## 修改边界

- 本专项实现已修改流程10生产代码、对应测试和迁移脚本，并以 task-owned commit `7f3547c17` 融合到 `int_main`；后续仅可由各领域 owner 补齐权威适配器、迁移/outbox 和真实 E2E。
- 禁止：直接改写 ERP 领料事实、伪造流程4/6/8凭证、默认成功、绕过权限/签名或运行写入型 E2E。
