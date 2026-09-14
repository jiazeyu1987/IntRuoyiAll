# PQC 弹框正式来源修复验证报告

## Result

completed：代码、正式本地数据、目标测试、最终真实页面复验、任务清理和经验沉淀全部通过。

## Passed Evidence

- QA 发布逻辑按激活路线版本工序分组，`清洗/精洗` 仅发布到“精洗、清洗”。
- 前端定向静态合同全部通过，`pnpm ts:check` 通过。
- 后端 `MesQaInspectionRegulationServiceTest` 与 `MesFrontlinePqcContextServiceTest` 共 47 个测试通过。
- 正式规程 `53/MES_QA/PUBLISHED`、版本 `54/G/0/PUBLISHED`、三条项目和四条新待检任务后置核对通过；旧夹具审计数据保留。
- 最终 Playwright 已确认订单 `881MO090889` 仅显示 `1. 清洗工序`，首检 `0/5`、巡检 `0/113`，两个弹框显示正式标准、正式方法和“发布 QA 规程快照”来源。
- 页面正文断言确认包含订单、工序、正式标准、正式方法和正式来源，不包含“V21工序本地测试夹具补齐”或“默认首检规则”。
- 弹框截图：`output/playwright/20260809-pqc-formal-standard-method-source/final-cleaning-standard-dialog.png`、`final-cleaning-method-dialog.png`。
- 收尾清理 preview/apply 均无 blocker/warning；任务核心记录、回滚 SQL 与最终截图保留，临时浏览器快照、旧截图和容器临时 SQL 已删除。
- 长时间保持 Playwright CLI 会话期间，全局通知轮询 `/system/notify-message/get-unread-count` 曾超时/返回 500；目标 PQC 页面、正式规程字段和任务切换断言均已独立通过，该非目标接口不影响本次结果。

## Blocking Evidence

无。

## Remaining Verification

无。
