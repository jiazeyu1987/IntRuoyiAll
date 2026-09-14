# Bug Regression Evidence

## Bug Summary

测试服受控浏览打开文件 `2054545668044071537` 的详情 viewer 时，`GET /admin-api/dcc/controlled-files/2054545668044071537/preview-metadata` 返回 `code=500, msg=系统异常`。后端日志显示根因是 `DccControlledPreviewAccessService.requireRequest` 对 `fileNumber` 强制非空校验，实际目标受控文件 `file_number` 为空。

## Expected Behavior

`preview-metadata` 应允许受控文件没有文件编号。只要用户具备预览权限且文件、版本、访问类型、目的、隐私模式和 token TTL 等必要字段有效，后端应生成 viewer token、水印追踪和访问日志；不得抛出 `fileNumber is required`。

## Reproduction

- 页面路径：`/dcc/controlled-file/detail/2054545668044071537?viewer=1&from=browser`
- 接口路径：`GET /admin-api/dcc/controlled-files/2054545668044071537/preview-metadata`
- 本地 RED 命令：`mvn -pl yudao-module-dcc -am "-Dtest=DccControlledPreviewAccessServiceTest#prepareAccess_allowsMissingFileNumberForPreviewMetadata" "-Dsurefire.failIfNoSpecifiedTests=false" test`

## Root Cause

`DccControlledFileQueryServiceImpl.getPreviewMetadata` 将 `file.getFileNumber()` 传入 `DccPreviewAccessRequest`。受控文件元数据契约允许 `fileNumber` 为空，但 `DccControlledPreviewAccessService.requireRequest` 和 `DccControlledFileAccessAuditService.requireWatermarkTrace` 均强制 `fileNumber` 非空，导致空编号文件在预览元数据生成阶段失败。

## Regression Test

- 新增 `DccControlledPreviewAccessServiceTest#prepareAccess_allowsMissingFileNumberForPreviewMetadata`。
- 用 `fileNumber = null` 走真实 `prepareAccess`、访问事件、水印追踪、访问日志和 viewer token 链路。
- 断言水印追踪与 payload 中 `fileNumber` 写为空串，且 viewer token 仍绑定正确文件上下文。

## RED:

`mvn -pl yudao-module-dcc -am "-Dtest=DccControlledPreviewAccessServiceTest#prepareAccess_allowsMissingFileNumberForPreviewMetadata" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，预期失败为 `IllegalArgumentException: fileNumber is required`。

## GREEN:

- `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledPreviewAccessServiceTest#prepareAccess_allowsMissingFileNumberForPreviewMetadata" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。
- `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledPreviewAccessServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，3 tests。
- `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileQueryServiceTest#getPreviewMetadata*" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，4 tests。
- `mvn -pl yudao-module-dcc -am "-Dtest=DccOnlineFilePreviewServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，3 tests。

## Risk And Scope

修复范围限定在 DCC 预览元数据生成链路。仍保留 tenantId、userId、fileId、versionId、accessType、purpose、ttlSeconds、privacyMode、访问权限和 viewer token 上下文校验；不隐藏异常、不改权限、不新增兼容降级。

## Verification

RED/GREEN 与相邻回归命令均已执行，结果见上方 `RED:` 与 `GREEN:` 记录。

## Blockers And Follow-Up

实现与目标验证已完成。当前根仓存在并行脏改动且分支已被其他任务推进到 ahead 状态，本任务未执行提交、推送或收尾清理。
