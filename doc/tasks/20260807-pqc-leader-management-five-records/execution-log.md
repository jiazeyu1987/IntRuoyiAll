# Execution Log

## User Intent

- 用户要求给 PQC 组长的 `PQC管理` 增加 5 条符合从一线 PQC 提交条件的数据。
- 本次范围限定为本机 `int_main` 测试环境，不操作远程测试服、生产服或备用服。

## BDD Scenarios

- BDD: 一线 PQC 提交进入组长管理 -> Given 本机测试租户存在可登录的 PQC 人员、目标 PQC 组长、发布 QA 规程和可提交活跃订单 / When PQC 人员通过真实前端完成 5 次正式检验提交 / Then PQC 组长在 `PQC管理` 中可看到 5 条对应数据，且每条的任务、事件、记录和结构化检验项目完整。
- BDD: 缺正式提交前置时停止 -> Given 缺测试账号、人员范围、发布 QA 规程、活跃订单、工序或正式 schema / When 尝试创建数据 / Then 停止写入并报告精确缺口，不创建孤立数据、mock 成功或前端假行。
- BDD: 任务标识防止重复写入 -> Given `CODX-PQC-20260807` 已存在任一正式提交记录 / When 再次执行本任务 / Then 在新增前停止，不重复创建 5 条数据。
- BDD: 并发写入不覆盖共享状态 -> Given 其它任务可能修改同一工序池或活跃订单 / When 选择本任务提交对象 / Then 必须使用无冲突对象或等待冲突解除，不覆盖其它任务的汇总和测试数据。

## Command And Evidence Log

- 2026-08-07: 已读取数据库、登录、PowerShell 编码、任务收尾规则，以及 `database-schema-delivery` 技能和数据库证据合同。
- 2026-08-07: 已读取 `docs/experience-index.md`，匹配 `docs/backend-development.md#MES-PQC-项目级检验快照门禁`。
- 2026-08-07: 已核对历史任务 `doc/tasks/20260806-pqc-management-list-test-data/`，确认 PQC 管理正式读模型依赖 `mes_pro_process_pool_event`、`mes_pro_process_pool_pqc_record`、`mes_pqc_inspection_task` 和结构化项目/逐件明细；历史对象只作证据，本轮必须重新核对真实运行库。
- 2026-08-07: Git 预检显示根仓库 `int_main` 已领先 `origin/int_main` 2 个提交，并存在其它任务改动；本任务不回滚、清理或覆盖并发任务文件。

## RED / GREEN / REGRESSION

- RED: 待执行。
- GREEN: 待执行。
- REGRESSION: 待执行。

## Data Safety

- 任务标识：`CODX-PQC-20260807`。
- 写入范围：仅本机测试租户中通过真实一线 PQC 页面产生的 5 条正式提交及系统自动形成的关联数据。
- 禁止范围：远程环境、无关租户、无关业务记录、权限扩大、mock、API-only 写入、直接 SQL 伪造正式提交。
- 并发策略：写入前检查目标工序池/活跃订单是否被其它任务占用；发现共享目标冲突时停止，不强停其它任务。
- 回滚口径：按本任务提交产生的 5 个 PQC task/event/record 主键和 `CODX-PQC-20260807` 标识精确清理关联明细；本任务目标为保留数据，验证通过后不主动回滚。

