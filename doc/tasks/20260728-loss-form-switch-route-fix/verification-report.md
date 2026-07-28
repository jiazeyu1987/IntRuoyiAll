# Verification Report

## Summary

已修复切换填写人选择动态路线表单槽位损耗单时误走批记录路线解析的问题。后端现在对动态路线表单任务从冻结 FormCenter 模板版本读取 `edhrAssistRows`，按所选填写人的正式责任范围过滤后返回给前端。

## Commands

- RED: 旧 HEAD 源码静态断言 -> FAIL as expected，旧服务缺少动态路线表单辅助行分流。
- GREEN: `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-assist-filler-switch-snapshot-static.spec.cjs` -> PASS。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#openTask_dynamicRouteFormFillerSwitchUsesTemplateAssistRowsWithoutExecutionRoute" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 1。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProEdhrBatchExecutionServiceTest#openTask_dynamicRouteFormFillerSwitchUsesTemplateAssistRowsWithoutExecutionRoute+openTask_returnsDynamicRouteFormContextWithoutBatchReportExecution+previewTask_returnsDynamicRouteFormTemplatePreviewWithoutBatchReportSource" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 2。
- GREEN: `mvn -pl yudao-module-mes -am "-DskipTests" compile` -> PASS，BUILD SUCCESS。

## Files

- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceImpl.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecord/MesProEdhrBatchExecutionServiceTest.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/js/mes-edhr-assist-filler-switch-snapshot-static.spec.cjs`
- `doc/tasks/20260728-loss-form-switch-route-fix/*`

## Remaining Work

- 提交 worktree 变更。
- 执行经验沉淀、cleanup preview/apply、branch runtime guard。
- 融合回 `int_main` 并处理主工作区当前 ahead/behind 与既有脏改动边界。
