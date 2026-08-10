# PQC 弹框正式接收标准与检验方法来源修复

## Task Goal

修复一线 PQC 页面“接收标准 / 检验方法”卡片及弹框仍展示 V21 本地测试夹具默认首检规则的问题，使当前工序、当前检验类型、当前检验项目展示正式 QA 规程中维护的接收标准与检验方法，并阻止测试夹具文案继续作为正式运行态来源。

## Milestones

- [x] M1：确认正式 QA 工序表格字段、发布链路和当前夹具污染路径。
- [x] M2：以 BDD 和失败测试锁定夹具数据不得进入正式 PQC 弹框的行为。
- [x] M3：实现最小根因修复，保持工序、检验类型、检验项目和规程版本映射一致；按授权完成本地正式数据重建。
- [x] M4：完成定向测试、相关回归和真实页面验证。
- [x] M5：完成任务清理与经验沉淀。

## Expected Verification

- 后端回归测试证明正式 PQC 响应不再把本地测试夹具默认规则作为接收标准或检验方法。
- 前端静态合同证明卡片与弹框只展示后端正式 QA 字段，缺失时明确显示未配置。
- Maven 目标测试及前端相关静态测试通过。
- 在具备本地运行态前置时，使用 Playwright 只读验证当前工序弹框内容不再包含“本地测试夹具”“默认首检规则”等测试文案。
- `bug-regression-fix-loop` 证据校验通过。

## Applicable Experience Gates

- 命中 `docs/backend-development.md#MES PQC 项目级检验快照门禁`：接收标准和检验方法必须来自发布 QA 规程项目级快照，不得使用默认首检规则、判定值、测试夹具或旧字段冒充。
- 若正式 QA 数据缺失，必须阻塞或明确显示未配置，不得用夹具规则补齐。
- 当前任务只修改已确认的本地租户测试数据；发现并行任务修改同一规程或任务时必须停止后续写入，不得覆盖。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；将核对正式字段发布链路及夹具进入运行态的根因，而非继续增加显示别名。
- `是否存在临时补丁或绕过`：否。

## Cleanup Keep

- `doc/tasks/20260809-pqc-formal-standard-method-source/local-cleaning-formal-source-repair.sql`
- `doc/tasks/20260809-pqc-formal-standard-method-source/local-cleaning-formal-source-rollback.sql`
- `output/playwright/20260809-pqc-formal-standard-method-source/final-cleaning-standard-dialog.png`
- `output/playwright/20260809-pqc-formal-standard-method-source/final-cleaning-method-dialog.png`

## Cleanup Candidates

- `output/playwright/20260809-pqc-formal-standard-method-source/.playwright-cli`
- `output/playwright/20260809-pqc-formal-standard-method-source/cleaning-standard-dialog.png`
- `output/playwright/20260809-pqc-formal-standard-method-source/cleaning-method-dialog.png`
- `output/playwright/20260809-pqc-formal-standard-method-source/cleaning-standard-dialog-crop.png`
- `output/playwright/20260809-pqc-formal-standard-method-source/cleaning-method-dialog-crop.png`

## Current Status

completed：代码、正式本地数据修复、目标验证、任务清理和经验沉淀均已完成。

## Blocker

无。用户已明确授权处理唯一键冲突；旧夹具规程 41 已软删除，旧版本 53、旧项目和已取消任务 296-299 均保留审计。
