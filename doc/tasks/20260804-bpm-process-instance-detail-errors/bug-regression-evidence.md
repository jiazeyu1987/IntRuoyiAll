# Bug Regression Evidence

## Bug

使用本机 `芋道源码/admin` 访问 `http://localhost:8081/bpm/process-instance/detail?id=c1cd2ae6-8fbf-11f1-a00f-00155d2984a0` 时，流程图高亮链路收到 3 个当前 BPMN XML 中不存在的 marker ID：`flow_start_doc_control_review:success`、`startEvent:success`、`DOC_CONTROL_REVIEW:primary`。

## Expected

BPMN 模型视图响应只应返回当前 BPMN XML 中存在的任务节点和连线 ID；前端仍保留安全 marker helper 与可见 warning，不通过吞异常、隐藏流程图或禁用全部高亮来掩盖数据不一致。

## Reproduction

- 初始真实路径：登录本机前端并访问 `/bpm/process-instance/detail?id=c1cd2ae6-8fbf-11f1-a00f-00155d2984a0`，流程图 warning 暴露 3 个缺失 marker ID。
- 最新复验命令：`node doc\tasks\20260804-bpm-process-instance-detail-errors\reproduce-bpm-detail-errors.cjs`。
- 最新复验环境结果：失败于 `page.goto: net::ERR_CONNECTION_REFUSED at http://127.0.0.1:8081/login?...`，因为 8081 当前未监听。

## Root Cause

后端 `getProcessInstanceBpmnModelView` 将历史活动和任务状态直接转换为 BPMN marker ID 集合，但流程定义更新或任务/连线 ID 变化后，历史活动里会残留当前 BPMN XML 中不存在的 ID。前端虽有 `elementRegistry.get(activityId)` 安全校验和 warning，但后端仍把不存在的 ID 当作正式高亮目标返回，导致目标页面出现 3 个高亮不完整错误。

## RED:

`mvn -pl yudao-module-bpm -am "-Dtest=BpmProcessInstanceServiceImplTest#getProcessInstanceBpmnModelView_shouldFilterMarkerIdsMissingFromCurrentBpmnModel" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，`assertFalse` 证明缺失的任务或连线 ID 仍出现在响应集合中。

## GREEN:

- `mvn -pl yudao-module-bpm -am "-Dtest=BpmProcessInstanceServiceImplTest#getProcessInstanceBpmnModelView_shouldFilterMarkerIdsMissingFromCurrentBpmnModel" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，1 个测试通过。
- `mvn -pl yudao-module-bpm -am "-Dtest=BpmProcessInstanceServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，3 个测试通过。

## Verification

- 后端：`BpmProcessInstanceServiceImpl` 在返回前过滤 `unfinishedTaskActivityIds`、`finishedTaskActivityIds`、`finishedSequenceFlowActivityIds`、`rejectTaskActivityIds`，只保留 `BpmnModelUtils.getFlowElementById(bpmnModel, activityId) != null` 的 ID。
- 前端：聚焦 Node 静态断言通过，确认 `ProcessViewer.vue` 保留 `safeAddProcessMarker`、`safeRemoveProcessMarker`、`elementRegistry.get(activityId)` 校验、可见 warning 和 helper 调用链。
- 无关阻塞：`node tests/e2e/dcc-revision-publish-ux-final-static.spec.cjs` 失败在 DCC 版本历史 remark helper 断言，不属于本 BPMN marker 修复范围。

## Blockers

- 真实 8081 页面复验未完成：8081/48081 当前未监听。
- 本地运行态加载未执行：主工作区存在大量非本任务脏改动，按本地运行态规则不能从脏主工作区重打运行 Jar 冒充本任务 E2E。
- 提交/推送未执行：当前分支已 ahead 9 且存在大量非本任务改动，无法安全完成只含本任务的最终推送。
