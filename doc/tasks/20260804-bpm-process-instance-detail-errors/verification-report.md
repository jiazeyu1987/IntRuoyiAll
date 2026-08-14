# Verification Report

## Scope

- 修复目标：BPM 流程实例详情页 BPMN 高亮 marker ID 与当前 BPMN XML 不一致导致的 3 个报错。
- 修改范围：`BpmProcessInstanceServiceImpl` 与对应单元回归测试。

## Completed Verification

- RED：新增 `getProcessInstanceBpmnModelView_shouldFilterMarkerIdsMissingFromCurrentBpmnModel` 后，后端仍返回缺失 marker ID，测试按预期失败。
- GREEN：聚焦方法级 Maven 回归通过，`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`。
- REGRESSION：后端测试类全量通过，`BpmProcessInstanceServiceImplTest` 共 3 个测试通过。
- FRONTEND GUARD：聚焦 Node 静态断言通过，确认流程图组件仍在 `canvas.addMarker/removeMarker` 前通过 `elementRegistry.get(activityId)` 校验，并保留可见 warning。

## Commands

- `mvn -pl yudao-module-bpm -am "-Dtest=BpmProcessInstanceServiceImplTest#getProcessInstanceBpmnModelView_shouldFilterMarkerIdsMissingFromCurrentBpmnModel" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `mvn -pl yudao-module-bpm -am "-Dtest=BpmProcessInstanceServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `node -e '<focused ProcessViewer marker guard assertions>'`

## Blocked Verification

- `node doc\tasks\20260804-bpm-process-instance-detail-errors\reproduce-bpm-detail-errors.cjs` 当前失败于 8081 连接拒绝。
- `Get-NetTCPConnection -LocalPort 8081,48081` 未返回监听进程。
- 主工作区存在大量非本任务脏改动，本地运行态规则禁止从脏主工作区重打运行 Jar 并声称本任务 E2E 通过。
- `node tests/e2e/dcc-revision-publish-ux-final-static.spec.cjs` 被无关 DCC 断言阻塞，不作为本 BPM 修复失败判定。

## Closeout Status

当前状态：`blocked_on_environment_closeout`。

代码修复和聚焦验证完成；真实 8081 页面复验、提交和推送需要先恢复安全的本地运行态或隔离本任务运行环境，并处理当前分支 ahead/dirty 的非本任务改动边界。
