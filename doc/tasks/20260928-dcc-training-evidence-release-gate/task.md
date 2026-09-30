# DCC 三流程培训证据分发门禁

## 目标

当上传或升版选择培训时，只有 `trainingRecordFileId` 对应的 `FileDO` 确实存在，详情投影才标记培训证据可用，且手工分发门禁才允许通过。未选择培训时跳过此门禁；普通流程继续使用既有培训状态/确认规则。

## BDD

- BDD-01: Given 三流程文件处于待手工分发、`needTraining=true`、`trainingRecordFileId` 非空但对应 `FileDO` 不存在；When 查询详情；Then `trainingRecordAvailable=false` 且 `canManualRelease=false`。
- BDD-02: Given 三流程文件处于待手工分发、`needTraining=true` 且对应 `FileDO` 存在；When 查询详情；Then 培训证据投影可用且 `canManualRelease=true`。
- BDD-03: Given 三流程文件处于待手工分发、`needTraining=false`；When 查询详情；Then 培训证据不投影且 `canManualRelease=true`，无需查询培训文件。
- BDD-04: Given 普通受控文件处于待手工分发；When 查询详情；Then 手工分发仍按原培训状态确认规则判定，不使用三流程原生培训文件门禁。

## 里程碑

1. [x] 阅读仓库规则、三流程设计、上传绑定及详情投影代码。
2. [x] 新增缺失文件 RED 测试，并确认其只因当前 ID-only 门禁而失败。
3. [x] 复用单次 `FileDO` 投影结果实现最小修复；完整 GREEN 被工作区外部编译错误阻断，未冒充通过。
4. [x] 运行指定 QueryService 测试与 `git diff --check`，审阅范围和结果。

## 预期验证

- `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileQueryServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `git diff --check`
- 覆盖培训 ID 存在但文件记录缺失、文件记录存在、`needTraining=false`；普通流程逻辑保持原样。

## 设计约束检查

- 仅修改 `DccControlledFileQueryServiceImpl.java`、`DccControlledFileQueryServiceTest.java` 与本任务目录。
- 不访问或修改数据库，不改服务运行态，不改 Git 历史/索引。
- 不吞异常、不伪造培训证据、不做兼容回退。
- 同一次详情请求不得为培训文件重复查询 `FileDO`；已查得的文件存在性同时驱动显示投影和门禁。

## Current Status

ready_for_closeout
