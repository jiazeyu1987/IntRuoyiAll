# 流程修复1：活跃订单加入时正式绑定领料单

## Task Goal

在生产组长把生产工单加入活跃订单池的同一业务动作中，由用户显式选择并由后端校验、持久化正式生产领料单及其明细快照。后续活跃订单完成、批次执行创建、资料上传、放行和追溯只能消费这条绑定关系，禁止再按工单号临时反查、按默认领料单猜测或用动态表单来源替代。

本轮在独立 worktree 实现活跃订单领料绑定及其下游来源消费；不启动服务、不运行写入型 E2E。共享 runtime contract 文件不属于本任务提交范围。

## Scope

- 活跃订单加入界面的领料单候选查询、选择、校验和提交合同。
- 后端加入请求、绑定聚合、领料单头/明细快照、唯一性、审核状态、领料单生产工单号匹配、并发和幂等规则。
- 批次执行与活跃订单领料单绑定的稳定追溯关系设计。
- 与流程修复6、7、9的字段、状态所有者、失败 blocker、幂等和追溯接口契约，并补充流程修复8、10、11的邻接说明。
- BDD、RED/GREEN/REGRESSION 计划、迁移和回滚边界。

## Out Of Scope

- 不实现流程修复6、7、8、9、10、11所属阶段；本轮只提供修复1绑定关系和下游来源字段契约。
- 不修复已有历史活跃订单数据，不直接更新任务、快照、报工或批次执行数据。
- 不改变其它流程修复的状态、材料门禁或独立入口语义。

## Milestones

1. [x] 读取项目规则、正式来源门禁、生产角色运营规则、前端和 E2E 规则。
2. [x] 审计当前加入活跃订单、领料单正式来源和批次执行代码事实。
3. [x] 形成目标态、根因、数据/接口/状态和跨线程合同。
4. [x] 形成 BDD、严格 TDD 计划、迁移/回滚边界和阻塞清单。
5. [x] 完成前后端、快照持久化和绑定来源消费实现。
6. [ ] 完成 Maven 定向测试和主工作树融合；环境缺少 Maven 时记录阻塞。

## Expected Verification

- 文档 UTF-8、必备章节、路径和约束扫描通过。
- `task.md`、`development-plan.md`、`test-plan.md`、`execution-log.md`、`verification-report.md` 齐全。
- 当前代码事实有源码路径和符号证据，且明确代码尚未符合目标态。
- 未来实现必须通过后端合同/单元测试、前端静态合同、数据库 schema 合同和真实浏览器路径；本轮不运行这些写入型验证。

## Current Status

ready_for_closeout

代码已在独立 worktree 实现；Maven 后端验证和主线 fast-forward 融合尚未完成，见 `verification-report.md`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。本设计要求缺少正式领料单、状态漂移、关系缺失和幂等冲突均 fail-fast。
- `是否从根因和长期维护角度解决`：是。根因是加入请求只携带工单号、绑定缺失且后续按工单反查；设计把绑定、快照和下游关系变成正式数据链路。
- `是否存在临时补丁或绕过`：否。不允许用 `formBindings`、默认 `MAIN`、当前最新 ERP 记录、空值或 API-only 结果掩盖来源缺失。

## Applicable Gates

- 活跃订单申请放行资料必须只使用正式来源：领料单必须在加入时绑定，放行阶段只读绑定快照和当前状态核验。
- Long ID HTTP JSON 按字符串传输；pickListBindingId、pickListId、sourceSnapshotHash、bindingVersion、batchPickListRelationId 及幂等键必须在实现前冻结。
- 活跃订单历史缺损只能通过正式重建或删除后重新新增修复，禁止直接 SQL 回填绑定。
- 三类配置链路保持独立：工序开始、批记录表单、表单槽位 `formBindings` 均不能充当领料单来源。

## References

- `docs/backend-development.md`：活跃订单申请放行资料必须只使用正式来源。
- `docs/product/production-role-system-operations.md`。
- `docs/frontend-development.md`、`docs/e2e-rules.md`。
- `docs/experience-index.md` 中正式领料单来源、幂等、批次执行和追溯门禁。
- `development-plan.md`：目标设计和分阶段实现边界。
- `test-plan.md`：BDD 和验证矩阵。
- `execution-log.md`：审计命令、事实和未执行验证记录。
