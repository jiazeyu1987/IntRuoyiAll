# Bug Regression Evidence

## Bug

用户重新创建批次执行后，打开“切换填写人”提示“当前执行详情缺少填写人快照，不能切换填写人。”；期望新批次执行可以选择该执行创建时固定的填写人候选。

## Expected

批次执行创建后，执行详情必须携带可追溯的 `assistSwitchTasks` 填写人快照；前端切换填写人弹窗直接读取执行详情快照，不重新拉取全量批次详情，不用空列表或当前登录人兜底。

## Reproduction

`node yudao-module-mes\src\test\js\mes-edhr-assist-filler-switch-snapshot-static.spec.cjs`

## Root Cause

传统批记录执行创建链路未保存批次任务 `taskId`，active 执行查询也未按 `batchExecutionId + taskId` 隔离，重新创建批次执行时可能复用同工单/工序/记录/批号下的旧 active 执行详情，导致当前执行详情缺少对应填写人快照。

## RED/GREEN

- RED: `node yudao-module-mes\src\test\js\mes-edhr-assist-filler-switch-snapshot-static.spec.cjs` -> FAIL，缺少 `.taskId(reqVO.getTaskId())`、active 查询隔离和前端快照契约。
- GREEN: `node yudao-module-mes\src\test\js\mes-edhr-assist-filler-switch-snapshot-static.spec.cjs` -> PASS，后端写入并按批次任务隔离执行记录，前端读取 `execution.value?.assistSwitchTasks`。

## Verification

- BLOCKED: `mvn -pl yudao-module-mes -am "-DskipTests" compile` 最新运行被并行 cell-link 未跟踪源码阻断。
- PASS: `node tests\e2e\edhr-switch-filler-selectability-static.spec.js`
- PASS: `git diff --check` scoped to current task files.

## Blockers

无切换填写人功能静态合同阻塞；最终 Maven 编译被非本任务 cell-link 工作区改动阻断，未使用 mock、API-only 或 fallback。
