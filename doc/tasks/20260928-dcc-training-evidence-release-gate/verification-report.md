# 验证报告：DCC 三流程培训证据分发门禁

## 结论

补丁已完成：三流程 `needTraining=true` 的手工分发门禁现在复用详情培训证据投影的 `FileDO` 存在性结果；培训 ID 非空但 `FileDO` 缺失时不允许手工分发，真实文件存在时允许，`needTraining=false` 继续跳过该门禁，普通流程仍按原培训状态确认规则处理。

## Given / When / Then

- BDD-01：Given 上传/升版三流程处于待手工分发、培训已选、培训 ID 有值但 `FileDO` 不存在；When 查询详情；Then `trainingRecordAvailable=false`、`canManualRelease=false`，并且动作投影不包含 `MANUAL_RELEASE`。
- BDD-02：Given 上传/升版三流程处于待手工分发、培训已选且 `FileDO` 存在；When 查询详情；Then 投影文件名/可用标记正确，`canManualRelease=true`。
- BDD-03：Given 上传/升版三流程处于待手工分发、未选择培训；When 查询详情；Then 不投影培训证据且手工分发仍可用。
- BDD-04：Given 普通受控文件；When 查询详情；Then 继续使用既有 `trainingStatuses` 全员确认规则。

## RED / GREEN

- RED：完整定向 Maven 命令实际运行了 QueryServiceTest，`163 tests, 1 failure, 0 errors`；唯一失败是缺失 `FileDO` 场景的 `canManualRelease` 仍为 true，复现了问题。
- GREEN：补丁代码已写入并通过源码语义核对；但指定的 QueryServiceTest 最终没有获得 PASS，因为完整 Maven 编译先后被两个不属于本任务的工作区预存错误阻断：
  - `DccControlledFileLogQueryServiceImpl.java:392` 缺少 `Stream` 类型导入；
  - `DccControlledFileLogQueryServiceTest.java:491` 调用不存在的旧方法签名。
- 因此本报告不把 QueryServiceTest 标记为通过，也不把完整 Maven 骄傲地写成绿色。测试进程已确认终止，无后台测试进程残留。

## 文件范围

本任务允许范围内变更：

- `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java`
- `IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceTest.java`
- `doc/tasks/20260928-dcc-training-evidence-release-gate/task.md`
- `doc/tasks/20260928-dcc-training-evidence-release-gate/execution-log.md`
- `doc/tasks/20260928-dcc-training-evidence-release-gate/verification-report.md`

未修改数据库、服务运行态或 Git 历史；未修改超出范围的日志源码/测试文件。

## 静态验证

- `git diff --check`：收尾重跑结果见最终报告；本任务目标文件和文档无 whitespace error。
- 测试结果：QueryServiceTest 当前为阻断，详见 RED/GREEN。

## Current Status

ready_for_closeout
