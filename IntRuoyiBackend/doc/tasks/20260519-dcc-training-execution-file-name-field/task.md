# 任务：DCC 培训执行补充文件名称字段

## 目标

为 DCC 培训执行 / 我的培训相关 training API 返回补充 `fileName` 字段，供前端新增 `文件名称` 列时使用，保持现有 `title`、`fileNumber`、`versionNo` 等字段继续可用。

## 前置任务检查

- 已检查上一条后端任务文档 `doc/tasks/20260519-system-user-company-cascade-delete/task.md`
- 处理结果：已按用户改为当前需求而显式标记为 blocked
- 结论：允许开始当前任务

## 里程碑

- [x] M1：记录 BDD 场景并补 training API RED 测试
- [x] M2：最小化扩展 training 返回 VO 和 service 赋值
- [x] M3：运行目标后端测试并更新 evidence
- [x] M4：执行 closeout 预览并准备仅提交本任务后端文件

## 范围

- `yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/training/vo/DccTrainingExecutionRespVO.java`
- `yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/training/vo/DccTrainingTaskRespVO.java`
- `yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccTrainingTaskServiceImpl.java`
- `yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccTrainingTaskServiceTest.java`
- `doc/tasks/20260519-dcc-training-execution-file-name-field/**`

## 非范围

- 不修改 training 数据生成规则
- 不改分页、权限、状态机
- 不改数据库 schema

## 预期验证

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc "-Dtest=DccTrainingTaskServiceTest" -Dsurefire.failIfNoSpecifiedTests=false test`

## 当前状态

已完成。后端验证、evidence 校验与 closeout 预览均已通过，待在后端仓库提交本任务文件。

## Cleanup Keep

- `doc/tasks/20260519-dcc-training-execution-file-name-field/backend-api-evidence.md`
