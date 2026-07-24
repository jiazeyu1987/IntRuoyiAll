# 任务：展厅产品管理增加全部发布按钮

## 任务目标

- 为 showroom 后端补齐 `全部发布` 批量接口，按当前筛选范围处理产品直发。

## 里程碑

- [x] M1：补齐批量发布后端契约与测试。
- [x] M2：实现 runtime 与 admin controller 批量发布链路。
- [x] M3：完成定向后端验证。

## 预期验证

- `mvn -pl yudao-module-showroom "-Dtest=ShowroomApiRuntimeBatchPublishTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`

## 当前状态

- 状态：已完成
- 主任务文档：`D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260522-showroom-product-batch-publish-button\task.md`

## 最终验证结果

- PASS: `mvn -pl yudao-module-showroom "-Dtest=ShowroomApiRuntimeBatchPublishTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-showroom-product-batch-publish-button --mode preview`
