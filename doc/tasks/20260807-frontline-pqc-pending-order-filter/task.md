# 过滤无待检 PQC 工单并优化空状态提示

## Task Goal

修复一线 PQC 页面仍展示已无待执行 PQC 任务工单的问题：后端 active order 读模型只返回存在 `PENDING` PQC 任务的工单；前端在无可执行工单或已选工单失效时显示清晰业务空态，不再暴露 `routeProcessId=null, processId=null` 这类调试信息。

## Milestones

- [x] 记录 BDD 场景和回归证据契约。
- [x] 增加后端回归测试，先复现无 `PENDING` 任务工单仍进入列表的问题。
- [x] 实现后端待检工单过滤，保持正式任务状态链路。
- [x] 增加前端静态契约，覆盖无待检工单空态和旧选择清理。
- [x] 实现前端空状态文案优化。
- [x] 运行定向后端、前端验证并记录 RED/GREEN/REGRESSION。

## Expected Verification

- 后端：`mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- 前端：目标静态契约 `node tests/e2e/frontline-pqc-pending-order-empty-state-static.spec.js`
- 前端类型：视改动范围运行 `pnpm ts:check`，若存在无关历史阻塞需在 `execution-log.md` 记录首个阻塞点。
- 文档/格式：`git diff --check`

## Current Status

completed

实现、定向验证、经验沉淀和 task-closeout-cleanup preview/apply 均已完成。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，计划通过后端正式待检任务状态过滤工单，并由前端表达真实空态。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- PQC 前置门禁：PQC 页面必须按活跃订单冻结路线和正式待执行任务校验，不得用首个工序完整性、空列表成功、API-only 或前端隐藏替代正式任务状态。
- 前端空态/错误门禁：请求失败必须暴露真实错误；无数据空态和 API 错误必须分层，不得吞异常或默认成功。
- Maven PowerShell 门禁：PowerShell 下 Maven `-D...` 参数必须整体加双引号，并保留 `-pl yudao-module-mes -am` 构建 reactor 依赖。

## Final Verification

- 后端目标回归：`MesFrontlinePqcContextServiceTest` 27 tests, failures 0, errors 0。
- 前端目标静态契约：`frontline-pqc-pending-order-empty-state-static.spec.js` PASS。
- 前端相邻回归：`mes-frontline-pqc-order-picker-summary-static.spec.cjs` PASS。
- 前端类型检查：`pnpm ts:check` PASS。
- 格式检查：`git diff --check` PASS，仅出现 Git CRLF 提示。
- 收尾清理：`task_closeout.py --mode preview` 与 `--mode apply` PASS，删除项为空。
