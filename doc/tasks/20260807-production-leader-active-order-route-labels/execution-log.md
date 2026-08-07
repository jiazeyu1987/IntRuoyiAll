# 执行日志

## User Intent And Scope

- 用户要求：删除截图红框中的“状态”列；黄框中的路线和版本改为显示路线名称与版本数字，不显示 ID。
- 页面范围：本机生产组长 `/mes/pro/process-pool/production-leader` 的“活跃订单池”页签。
- 非目标：不修改活跃订单业务状态、加入/移出行为、其它页签、其它角色工作台或业务数据。

## BDD

- BDD: 活跃订单显示业务路线信息 -> Given 活跃订单关联正式工艺路线和路线版本，When 生产组长打开“活跃订单池”，Then 表格显示“路线名称”和“版本号”，每行显示正式路线名称及版本数字，且不显示路线 ID、路线版本 ID 和状态列。
- BDD: 正式显示字段缺失时失败 -> Given 活跃订单读模型无法解析正式路线或路线版本，When 后端生成列表响应，Then 明确失败或暴露契约缺失，不由前端猜测、硬编码或回退显示内部 ID。

## RED / GREEN Evidence

- RED: `node tests\e2e\production-leader-active-order-route-labels-static.spec.js` -> FAIL，首个失败断言为前端活跃订单契约缺少 `routeName/routeVersionNo`，符合预期。
- RED: `mvn -pl yudao-module-mes ... test` -> FAIL，测试编译明确找不到新读模型 `MesTeamLeaderActiveOrderRow`，符合预期。
- RED ENV: 首次 Maven reactor 因残留 `target_corrupt_m4_20260802_1327` 触发增量 `StaleSourceScanner` 长时间扫描；停止本任务 PID `68812` 后，以 `-Dmaven.compiler.useIncrementalCompilation=false` 重跑相同测试范围并取得上述业务 RED。
- GREEN: pending - 后端接口、前端页面、聚焦测试和真实页面验证待完成。

## Command Intent

- READ: 已读取项目 `AGENTS.md`、`docs/frontend-development.md`、`docs/task-closeout-rules.md`、`docs/e2e-rules.md`、`docs/login-access.md`、`docs/local-runtime.md`、`docs/experience-index.md`。
- SKILL: 使用 `frontend-feature-delivery` 与 `backend-api-delivery`，已读取各自 `SKILL.md` 和证据合同。
- SAFETY: 仅操作本机源代码和只读页面验证；不访问远端、不修改现有活跃订单数据。

## Milestone Status

- M1 completed：确认入口、目标表格、现有 TypeScript VO 和后端活跃订单响应类位置。
- M2 completed：前端静态合同和后端 JUnit 已锁定成功字段、缺失路线及版本错配失败行为。
- M3 in_progress：实现正式批量读模型、接口响应字段和表格列调整。

## Blockers

- 暂无；如后端不存在正式路线/版本来源则必须停止，不允许前端 ID 回退。
