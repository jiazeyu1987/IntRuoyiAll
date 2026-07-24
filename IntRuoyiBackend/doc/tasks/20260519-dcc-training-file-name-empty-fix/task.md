# 任务：修复 DCC 培训执行文件名称为空

## 目标

修复 `DCC培训 -> 培训执行` 列表中 `文件名称` 列为空的问题，确保 training API 返回的是上传时填写的文件名称。

## 前置任务检查

- 已检查上一条后端任务文档 `doc/tasks/20260519-dcc-training-execution-file-name-field/task.md`
- 状态：已完成
- 结论：允许开始当前任务

## 里程碑

- [x] M1：记录 bug 现象、预期行为与 RED 回归测试
- [x] M2：最小修复 training fileName 取值逻辑
- [x] M3：运行目标后端测试并更新 bug evidence
- [x] M4：执行 closeout 预览并准备仅提交本任务文件

## 范围

- `yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccTrainingTaskServiceImpl.java`
- `yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccTrainingTaskServiceTest.java`
- `doc/tasks/20260519-dcc-training-file-name-empty-fix/**`

## 非范围

- 不修改前端列表结构
- 不修改数据库 schema
- 不更改 training 分配、状态机、权限

## 预期验证

- `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc "-Dtest=DccTrainingTaskServiceTest" -Dsurefire.failIfNoSpecifiedTests=false test`
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260519-dcc-training-file-name-empty-fix\bug-regression-evidence.md`

## 当前状态

已完成。目标测试、bug evidence 校验与 closeout 预览均已通过，待在后端仓库提交本任务文件。

## Cleanup Keep

- `doc/tasks/20260519-dcc-training-file-name-empty-fix/bug-regression-evidence.md`
