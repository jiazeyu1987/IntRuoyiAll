# Verification Report

## Verdict

PASS_WITH_USER_AUTHORIZED_MAPPING_DEFERRAL

PQC 生产放行 MVP 的代码、迁移、构建、类型、单元/合同测试、真实页面入口、正式完工、待放行申请、不合格审查和 QA 让步均通过。14 张逐工序批记录当前缺少正式映射，PQC 签名事务按设计在创建批次前阻断。用户明确授权本次跳过映射配置，以静态逻辑检查通过作为该部分验收通过；生产代码没有绕过映射门禁。

## Static Logic Review

- 完工与申请：同一事务先生成并复核完工回执，再创建唯一 PQC 申请；失败整体回滚。
- 幂等与重试：申请复用稳定幂等键；已有完工回执使用原始版本和键重新校验正式来源。
- 批记录身份：逐工序 `batchRecordReportId` 从正式报表元数据解析定义/版本；冗余字段冲突时阻断，不使用 `formBindings` 替代。
- 映射与资料：只接受正式报工或领料来源；逐项核验提交、分配、复核签名、来源值、当前批次任务和字段审计。
- PQC 决策：先校验角色、候选、工单冻结状态、评审终态和签名密码，再创建批次、写三类资料、记录签名和推进申请。
- 错误处理：明确业务 blocker 返回结构化原因，前端不会误判为网络不确定，也不会锁死合法重试。
- Review verdict: PASS，未发现新的逻辑缺口。

## Verified Behavior

- 正式动态菜单入口为 `MES 系统 > eDHR批记录 > PQC生产放行`。
- 页面提供 `待放行`、`已放行`、`已作废`、`已返工`、`已让步放行` 五个页签。
- 待放行操作只保留 `放行`、`不合格审查`；通用工作待办改为进入专用页面，不再暴露旧拒绝动作。
- 直接放行必须校验当前 PQC 负责人电子签名并保存签名证据；签名失败不推进状态。
- 不合格审查可在尚未创建批次执行时按生产放行申请发起并冻结工单。
- QA 让步后回到待放行，PQC 签字完成后进入已让步放行；返工进入已返工；作废持续阻断工单和放行。
- 历史申请缺 PQC 待办 ID 时不会生成空 `IN ()` SQL，列表返回可用空态。

## Evidence

- 后端定向 JUnit：PASS，13 tests，0 failures，0 errors。
- 后端静态合同：PASS。
- 前端静态合同和 SP-2/工作待办/不合格评审相邻合同：PASS。
- `pnpm ts:check`：PASS。
- `mvn -pl yudao-module-mes -am -DskipTests compile`：PASS，24 modules。
- `mvn -pl yudao-server -am -DskipTests package`：PASS，30 modules。
- migration policy gate：PASS。
- `git diff --check`：PASS。
- Playwright：PASS，真实菜单、目标 URL、五页签和目标分页接口业务码均正确；截图为 `IntRuoyiFronted/output/playwright/pqc-production-release-mvp-completion/pqc-production-release.png`。

## Deferred Mapping Evidence

- 真实页面成功形成任务自有活跃订单 `348`、生产放行申请 `9`、PQC 待办 `2391` 和已让步评审 `7`。
- PQC 电子签名请求返回正式业务 blocker，未创建批次、未推进申请版本、未完成待办。
- 14 张批记录的放行可接受映射数均为 0；必须由模板/工艺负责人配置 `PROCESS_POOL_REPORT` 或 `PRODUCTION_PICK_LIST` 到正式目标单元格后复跑。
- 本任务未自动批准模板规则、未生成猜测映射、未直接修改 Jimu JSON，也未使用 SQL/API-only 推进申请。
- 当前申请是已让步待签名审计记录，系统没有正式取消入口；未直接删除该记录，作为用户授权静态验收后的本机残余风险保留。
- 用户已接受本次映射部分以静态逻辑检查替代动态验收；映射完成后需补跑完整签名、批次创建、资料落库与测试数据清理。
- Playwright 捕获一个外部头像 GET 连接拒绝；目标接口、页面脚本和业务操作不受影响。

## Runtime

- Worktree：`D:\IntRuoyiWorktree\pqc-production-release-mvp-completion-20260831`。
- Profile/slot：`int_main slot 56`。
- Frontend/backend：`8311/48311`。
- 本地验证库已幂等应用两条 20260831 迁移并回读目标状态。

## Integration

- 功能提交重放后为 `d9fe88557`。
- `int_main` 使用 `ff-only` 融合到 `d9fe88557`。
- 主工作区并行未提交改动未被暂存、覆盖或提交。
- 额外 worktree 和运行端口均已收口。
- M6 续验修复提交为 `f23ed1252`，在最新主线复验后再次通过 `ff-only` 融合到 `int_main`。
- 额外 worktree `D:\IntRuoyiWorktree\pqc-production-release-write-e2e-20260831` 已移除，slot 56 已释放，8311/48311 无监听。
- 最终结论：按用户明确授权的映射延期口径，本任务完成；映射可用后补跑真实 PQC 签名、批次创建、资料落库和本机测试证据清理。
