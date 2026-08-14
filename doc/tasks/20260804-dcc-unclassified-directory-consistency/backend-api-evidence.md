# Backend API Evidence

## Scope

- `DccControlledFileNasTransferServiceImpl`
- `DccControlledFileMetadataUpdateServiceImpl`

## Contract

- 类别有绑定目录时沿用绑定目录子树校验。
- 类别没有绑定目录时解析唯一启用 `UNCLASSIFIED / 未分类` 目录。
- `UNCLASSIFIED` 缺失或不唯一时继续抛出正式错误。

## Validation

- 目录解析来源：`DccUploadDirectoryResolver.resolveUnclassifiedUploadDirectory(...)`。
- NAS 转移和本地文件夹导入：`SelectedCategoryContext` 保存最终绑定目录 ID 与 NAS 根父目录 ID。
- 元数据编辑：请求 `directoryId` 可为空；类别无绑定时用正式未分类目录作为最终目录，冲突校验基于最终目录 ID。
- 无新增 schema、迁移、外部服务或配置。

## BDD

- BDD: NAS 模板类别未绑定目录自动落位未分类 -> Given DCC 模板类别启用但没有目录绑定且系统存在唯一启用 `UNCLASSIFIED / 未分类` 目录, When 用户发起 NAS 转移或本地文件夹导入, Then 后端创建任务并把目录根定位到未分类目录，不要求类别先绑定目录。
- BDD: 元数据编辑类别未绑定目录自动落位未分类 -> Given active 受控文件选择未绑定目录的文件类别, When 保存元数据, Then 后端把最终目录保存为正式未分类目录。
- BDD: 正式未分类目录缺失继续 fail fast -> Given 类别未绑定目录且唯一启用 `UNCLASSIFIED` 不存在或不唯一, When 需要落位, Then 后端返回正式错误，不创建默认成功数据。

## RED

- RED: `mvn.cmd -f IntRuoyiBackend\pom.xml -pl yudao-module-dcc -am "-Dtest=DccControlledFileNasTransferServiceTest#transfer_unboundSelectedCategoryUsesUnclassifiedDirectory+DccControlledFileNasTransferServiceTest#processWaitingTasks_unboundSelectedCategoryUsesUnclassifiedDirectory+DccControlledFileMetadataUpdateServiceTest#updateMetadata_unboundCategoryUsesUnclassifiedDirectory" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，旧实现阻塞未绑定目录类别。

## GREEN

- GREEN: `mvn.cmd -f IntRuoyiBackend\pom.xml -pl yudao-module-dcc -am "-Dtest=DccControlledFileNasTransferServiceTest#transfer_unboundSelectedCategoryUsesUnclassifiedDirectory+processWaitingTasks_unboundSelectedCategoryUsesUnclassifiedDirectory,DccControlledFileMetadataUpdateServiceTest#updateMetadata_unboundCategoryUsesUnclassifiedDirectory" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，3 tests。
- GREEN: `mvn.cmd -f IntRuoyiBackend\pom.xml -pl yudao-module-dcc -am "-Dtest=DccControlledFileNasTransferServiceTest,DccControlledFileMetadataUpdateServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，61 tests,0 failures/errors。

## Verification

- 相邻测试类完整通过，覆盖 NAS 转移、本地文件夹导入、任务处理、元数据更新、绑定目录子树校验和冲突校验。
- 扩大源码扫描未发现旧“请先绑定目录”运行时阻塞残留。

## Observability

- 任务失败仍记录 NAS 转移任务失败消息；本修复没有吞异常或默认成功。
- 缺失/重复未分类目录继续由 resolver 抛正式错误。

## Blockers

- 无后端阻塞。
- 真实页面元数据编辑 E2E 受当前本机账号权限阻塞，不影响后端单测覆盖。
