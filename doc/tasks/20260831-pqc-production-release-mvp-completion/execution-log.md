# Execution Log

## User Intent

- 在 worktree 中完成文档要求的开发和验证，验证成功后融合进 `int_main`。
- 最新 MVP 口径：五个状态页签；待放行行操作为 `放行`、`不合格审查`；不再提供单独驳回操作；确认放行后完成电子签名提交、状态刷新和批记录入口。

## BDD

- BDD: PQC 查看五类生产放行记录 -> Given PQC 负责人进入生产放行页面; When 切换任一状态页签; Then 系统按权威状态返回该页签记录并显示对应数量和业务状态。
- BDD: PQC 签字放行 -> Given 待放行申请属于当前 PQC 候选且批次未冻结; When 输入当前账号电子签名密码并确认放行; Then 后端校验签名、完成正式放行事务、页面将记录移出待放行并提供查看批记录入口。
- BDD: 签名失败不放行 -> Given 待放行申请存在; When 电子签名密码为空或错误; Then 系统明确拒绝且申请保持待放行。
- BDD: 发起不合格审查 -> Given 待放行申请尚未创建批次执行但包含正式工单身份; When 点击不合格审查; Then 进入统一不合格评审入口并冻结工单，后续由 QA 处置为让步放行、返工或作废。
- BDD: 待放行页面不提供拒绝 -> Given PQC 负责人查看待放行记录; When 查看行操作; Then 只显示放行和不合格审查，不显示旧拒绝动作。

## Evidence

- M0：当前生产放行审批位于通用工作待办页面，动作仍为“通过/拒绝”；放行请求没有签名字段；现有放行追溯页面不是 PQC 专用五状态列表。
- M0：现有统一不合格评审已覆盖 PQC 放行来源、QA 让步放行/返工/作废和追溯，可直接复用。
- M0：现有批次详情已有电子签名密码放行模式，可复用正式签名校验服务，不新增第二套签名体系。
- RED: `node src\test\js\mes-pqc-production-release-mvp-completion-static.spec.cjs` -> FAIL，预期原因：后端尚无 PQC 专用分页接口、签名字段和申请级不合格评审支持。
- RED: `node tests\e2e\pqc-production-release-mvp-completion-static.spec.js` -> FAIL，预期原因：前端尚无五状态常量、专用页面和签名放行合同。

## Current Milestone

M5 - ready for branch commit and int_main integration.

## GREEN And Regression

- GREEN: `node src\test\js\mes-pqc-production-release-mvp-completion-static.spec.cjs` -> PASS。
- GREEN: `node tests\e2e\pqc-production-release-mvp-completion-static.spec.js` -> PASS。
- GREEN: `mvn -pl yudao-module-mes -Dtest=MesPqcReleaseBatchExecutionServiceTest,MesProEdhrNonconformanceReviewApplicationScopeTest -DforkCount=0 test` -> PASS，13 tests。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `mvn -pl yudao-server -am -DskipTests package` -> PASS，30 reactor modules。
- GREEN: release migration policy gate -> PASS，包含两条 20260831 迁移。
- GREEN: 既有不合格评审前后端静态合同、SP-2 生产放行合同、工作待办静态合同 -> PASS。
- GREEN: Playwright 真实页面 -> PASS；从真实动态菜单进入 `/mes/production-release/pqc`，五个页签全部可见，分页接口业务码为 0，页面无 pageerror，业务写请求 0。

## Runtime Findings

- 首次本地迁移执行命令因 `sh -lc` 参数被 PowerShell 拆分，命令返回 0 但 schema 未变化；改为将完整 shell 命令作为单一参数传入，并回读 `batch_execution_id` 已可空、生产放行菜单已成为正式页面。
- 首次真实列表请求因历史申请全部缺 PQC 待办 ID，调用 `selectByIds(empty)` 生成 `IN ()`；补测试和空集合门禁后，真实页面返回空列表而非系统异常。
- 真实页面唯一失败资源为外部头像地址连接拒绝，属于非目标链路，不影响菜单、目标接口或页面渲染。

## Not Executed

- 当前 `芋道源码` 租户待放行行数为 0，因此未执行电子签名放行和不合格评审写入。未通过 SQL、API-only 或 mock 造业务样本；对应写入行为由后端事务测试和前端合同覆盖。
- 两个既有非目标静态测试在当前基线失败：工作任务上下文测试要求另一 API 的 `workTaskId`，批次详情放行测试要求旧“质量拒收”按钮；本任务未修改对应业务链路。

## Closeout Preview

- `task-closeout-cleanup --mode preview` 正确保留三份任务记录并建议删除 migration policy JSON；生成 JSON 已删除。
- 自动 apply 被阻塞：主工作区存在并行未提交改动，且 cleanup 脚本无法从任务文档自动识别全部生产代码归属。按用户明确要求改为精确提交当前分支、核对与主工作区脏文件无交集后执行 `ff-only` 融合；不覆盖或提交并行改动。
