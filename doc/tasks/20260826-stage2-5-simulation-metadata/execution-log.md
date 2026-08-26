# Execution Log

## BDD

BDD: Stage2.5 simulation facts are attributable -> Given a Stage2.5 run has a simulationRunId; When production and PQC facts are generated; Then every fact carries stageCode 2.5 and the same simulationRunId.

## RED

- mes-active-order-stage2-5-static.spec.cjs -> FAIL，正式模拟服务调用缺少 simulation stage metadata 参数。

## GREEN / REGRESSION

- 正式模拟入口改为传入 simulationStage=2.5 和当前 simulationRunId。
- Stage2.5 前端 API、活跃订单按钮和真实批次详情跳转已纳入静态合同。
- Stage2.5 static contract：PASS。
- Java receipt handoff contract：PASS。
- Stage4/Stage6 adjacent frontend static contracts：PASS。
- MES compile：BUILD SUCCESS。

## Integration

- implementation commit: ab69fa640c7155a7dcf51b73a335b5fe3186d8e1。
- int_main moved from 534d27949 to ab69fa640 with old-HEAD protection。
- Main worktree Stage2.5 backend and frontend files were synchronized without cleaning or committing unrelated dirty overlay。
- Main worktree Stage2.5 static contract: PASS。

## Blockers

Pending.
