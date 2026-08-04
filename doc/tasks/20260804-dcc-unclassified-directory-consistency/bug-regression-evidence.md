# Bug Regression Evidence

## Bug Summary

DCC 受控文件上传主路径已经支持未绑定类别自动落位未分类目录，但 NAS 转移、本地文件夹导入和元数据维护仍有独立的“类别必须绑定目录”阻塞逻辑。

## Expected Behavior

未绑定目录的文件类别应统一解析到正式唯一启用的 `UNCLASSIFIED / 未分类` 目录；若正式目录缺失或不唯一，应 fail fast。

## Reproduction

- RED: `mvn.cmd -f IntRuoyiBackend\pom.xml -pl yudao-module-dcc -am "-Dtest=DccControlledFileNasTransferServiceTest#transfer_unboundSelectedCategoryUsesUnclassifiedDirectory+DccControlledFileNasTransferServiceTest#processWaitingTasks_unboundSelectedCategoryUsesUnclassifiedDirectory+DccControlledFileMetadataUpdateServiceTest#updateMetadata_unboundCategoryUsesUnclassifiedDirectory" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL,旧实现要求类别先绑定目录。

## Root Cause

- NAS 转移、本地文件夹导入和元数据编辑各自读取类别目录绑定，并在 `selectActiveByCategoryId(...) == null` 时直接抛出“请先绑定目录”类错误。
- 上传主链路已经建立正式 `UNCLASSIFIED / 未分类` 目录解析，但上述入口没有复用 `DccUploadDirectoryResolver`，导致同一业务语义下体验不一致。

## Regression Tests

- `DccControlledFileNasTransferServiceTest#transfer_unboundSelectedCategoryUsesUnclassifiedDirectory`
- `DccControlledFileNasTransferServiceTest#processWaitingTasks_unboundSelectedCategoryUsesUnclassifiedDirectory`
- `DccControlledFileMetadataUpdateServiceTest#updateMetadata_unboundCategoryUsesUnclassifiedDirectory`
- `scripts/system-nas-management.test.mjs`
- `scripts/dcc-controlled-file-metadata-edit.test.mjs`

## RED

- RED: `mvn.cmd -f IntRuoyiBackend\pom.xml -pl yudao-module-dcc -am "-Dtest=DccControlledFileNasTransferServiceTest#transfer_unboundSelectedCategoryUsesUnclassifiedDirectory+DccControlledFileNasTransferServiceTest#processWaitingTasks_unboundSelectedCategoryUsesUnclassifiedDirectory+DccControlledFileMetadataUpdateServiceTest#updateMetadata_unboundCategoryUsesUnclassifiedDirectory" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，预期失败原因为旧目录绑定阻塞。

## GREEN

- GREEN: `mvn.cmd -f IntRuoyiBackend\pom.xml -pl yudao-module-dcc -am "-Dtest=DccControlledFileNasTransferServiceTest#transfer_unboundSelectedCategoryUsesUnclassifiedDirectory+processWaitingTasks_unboundSelectedCategoryUsesUnclassifiedDirectory,DccControlledFileMetadataUpdateServiceTest#updateMetadata_unboundCategoryUsesUnclassifiedDirectory" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，3 tests。
- GREEN: `mvn.cmd -f IntRuoyiBackend\pom.xml -pl yudao-module-dcc -am "-Dtest=DccControlledFileNasTransferServiceTest,DccControlledFileMetadataUpdateServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，61 tests。
- GREEN: 前端聚焦静态合同和相邻上传合同全部 PASS，详见 `verification-report.md`。
- GREEN: 上传页和 NAS 转移弹窗真实只读 E2E PASS，详见 `verification-report.md`。

## Verification

- 后端确认未绑定类别走正式唯一启用 `UNCLASSIFIED / 未分类` 目录；缺失或不唯一仍 fail fast。
- 前端确认旧阻塞文案和阻塞函数不在运行时源码中残留。
- 扩大扫描确认系统内同类运行时阻塞已收敛；配置维护页保留“绑定目录”不是提交阻塞。

## Risk And Scope

- Scope: DCC NAS 转移、本地文件夹导入、受控文件元数据维护。
- Non-goal: 审批路线、培训规则、上传策略等管理配置页仍保留文件类别选择。

## Blockers

- 元数据编辑真实页面 E2E 在当前本机账号无 `doc_control` 角色时无法进入弹窗；已记录为前置阻塞，不以 API-only 替代。
- `pnpm ts:check` 仍受既有 LocalDateTime 类型合同错误阻塞，未归属本任务。
