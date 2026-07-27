# Test Report

## Current Status

Not started. Independent test passes will be appended per task after executor evidence is available.

## Baseline

- Command: `mvn -pl yudao-module-mes test`
- Result: FAIL
- Summary: 2509 tests, 58 failures, 78 errors, 31 skipped, 41 failing suites.
- Final acceptance remains blocked by missing authoritative fixture and unresolved failure clusters.

## T2 Independent Verification

- Task ID: `20260727-edhr-notify-all-valid-candidates`
- Test case: `TC-03`
- Acceptance mapping: `AC-04`, `AC-05`, `AC-13`, `AC-14`, `AC-16`, `AC-17`
- Expected: 三个静态契约测试只对齐当前正式前端根目录 `IntRuoyiFronted` 和 `recognizeUploadedRoute` 正式精确参数序列；不得删除断言、缩小扫描范围或使用宽松匹配；定向测试与差异检查通过。
- Actual: 三个测试文件各仅有 1 行替换，共 3 insertions/3 deletions。两个菜单契约只将废弃根目录 `yudao-ui-admin-vue3` 替换为 `IntRuoyiFronted`，原 SQL、路由、文件存在性及禁止项断言均保留；版本迁移契约只在原精确字符串序列中加入正式参数 `null`，与生产代码调用 `oldVersion.getSourceVersionId(), null, productNames, true, List.of(), productNames` 一致。未发现断言删除、扫描范围缩小、正则放宽或模糊匹配。
- Result: `PASS`
- Test summary: `10 tests`, `0 failures`, `0 errors`, `0 skipped`, `BUILD SUCCESS`
- Test command: `mvn -pl yudao-module-mes "-Dtest=MesProEdhrFormFillLogMenuContractTest,MesProEdhrTemplateConfigMenuRemovalContractTest,MesProBatchRecordVersionPhaseTwoMigrationContractTest" test`
- Diff command: `git diff --check -- "yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/MesProEdhrFormFillLogMenuContractTest.java" "yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/MesProEdhrTemplateConfigMenuRemovalContractTest.java" "yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/MesProBatchRecordVersionPhaseTwoMigrationContractTest.java"`
- Diff result: exit code `0`，无空白错误；仅输出 Git 既有 LF/CRLF 转换提示。
- Unresolved issues: T2 范围内无未解决问题；任务整体仍受权威 fixture 缺失及其他 MES 完整回归失败簇阻塞，本次 PASS 不代表 TC-09 或完整任务通过。
