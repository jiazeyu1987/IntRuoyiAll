# Bug Regression Evidence

## Bug

受控文件提交页选择“文件分类”后，继续选择文件类别或触发目录加载时可能报错 `Controlled file category does not exist`。

2026-08-03 复现补充：截图显示用户仅选择“文件分类”taxonomy 后，“文件类别”尚未选择且下拉为空，此时右上角仍出现 `Controlled file category does not exist`。因此除 stale `categoryId` 外，辅助历史文件名称预加载也必须从文件分类切换事件中移除。

## Expected

前端必须区分“文件分类”`fileTypeTaxonomyId` 和 DCC 正式“文件类别”`categoryId`。文件类别只能来自当前 taxonomy 叶子节点唯一绑定的可上传正式类别；文件分类切换后必须清空旧类别和目录上下文，不得把 stale 或跨分类 ID 发送给后端。

文件分类或 DCC 项目切换时不得主动调用历史文件名称候选接口；历史文件名称是可选辅助信息，只能在用户聚焦/查询“文件名称”时按需加载，避免分类选择动作弹出无关的 category 错误。

2026-08-03 追加期望：文件类别由文件分类叶子节点自动显示，不允许用户填写；如果唯一解析出的正式类别未绑定提交目录，系统自动落位到正式 `UNCLASSIFIED / 未分类` 目录，不再提示提交人去维护目录绑定。

## Reproduction

静态复现命令：`node tests/e2e/dcc-upload-category-taxonomy-binding-static.spec.js`。修复前契约缺少 taxonomy 分支过滤和切换清理逻辑，能够证明旧页面存在跨链路类别 ID 风险。

## Root Cause

上传页同时维护 taxonomy 分类和 DCC 类别。后端 `getUploadDirectoryTree(categoryId)` 只接受 DCC 正式类别 ID，并会 fail fast 抛出 `Controlled file category does not exist`。旧前端未把类别下拉严格收敛到当前 taxonomy 分支，也未在 taxonomy 切换时统一清理旧 `categoryId` 和目录上下文，导致可能提交不再有效的类别 ID。

## RED

- RED: `node tests/e2e/dcc-upload-category-taxonomy-binding-static.spec.js` -> FAIL, expected reason: 缺少 `selectedFileTypeTaxonomyCategoryIds`、`availableCategories` taxonomy 过滤、以及 `handleFileTypeTaxonomyChange()` 的类别/目录/预览清理。
- RED: `node tests/e2e/dcc-upload-name-version-autofill-static.spec.js` -> FAIL, expected reason: 旧契约仍要求 `handleFileTypeTaxonomyChange()` 主动刷新历史文件名称选项，无法证明截图中的文件分类切换不会触发辅助接口 toast。
- RED: `node tests/e2e/dcc-upload-category-permission-static.spec.js` -> FAIL, expected reason: 旧 `availableCategories` 仍排除未绑定 `directoryId` 的类别，无法进入后端未分类目录落位流程。
- RED: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_dcc_unclassified_upload_directory_seed_sql.py -q` -> FAIL, expected reason: 缺少 `20260803_dcc_unclassified_upload_directory_seed.sql`。
- RED: `mvn -pl yudao-module-dcc -am "-Dtest=..." test` -> FAIL, expected reason: 缺少 `FILE_CATEGORY_UNCLASSIFIED_DIRECTORY_NOT_EXISTS` 和 `DccControlledFileUploadDirectoryTreeRespVO.defaultUnclassified`。

## GREEN

- GREEN: `node tests/e2e/dcc-upload-category-taxonomy-binding-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/dcc-upload-category-permission-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/dcc-upload-project-taxonomy-revision-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/dcc-upload-name-version-autofill-static.spec.js` -> PASS。
- GREEN: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_dcc_unclassified_upload_directory_seed_sql.py -q` -> PASS。
- GREEN: `python -X utf8 IntRuoyiBackend\script\release\run-release-migration-policy-gate.py --sql-root IntRuoyiBackend\sql\mysql --sql-file IntRuoyiBackend\sql\mysql\20260513_dcc_base_schema.sql --sql-file IntRuoyiBackend\sql\mysql\20260803_dcc_unclassified_upload_directory_seed.sql --output doc\tasks\20260803-controlled-file-category-missing\migration-policy-gate-unclassified.json` -> PASS。
- GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccControlledFileQueryServiceTest#getUploadDirectoryTree_categoryWithoutBindingReturnsUnclassifiedDirectory,DccControlledFileWorkflowServiceImplTest#submitControlledFile_categoryWithoutDirectoryBindingUsesUnclassifiedDirectory" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 2 tests.
- GREEN: heartbeat Maven `DccControlledFileQueryServiceTest#getUploadDirectoryTree_categoryWithoutBindingAndUnclassifiedMissingFailsFast` -> PASS.
- GREEN: heartbeat Maven `DccControlledFileWorkflowServiceImplTest#submitControlledFile_bindingMissingAndUnclassifiedDirectoryMissing_throwsNotExists` -> PASS.

## Verification

定向静态契约覆盖 taxonomy-bound 类别过滤、文件分类切换清理、上传类别权限投影、未绑定目录自动落位、项目 taxonomy 修订联动和上传文件名/版本自动填充相邻回归。后端单测覆盖目录树返回未分类、提交使用未分类，以及未分类目录缺失 fail-fast。`git diff --check` 对本任务路径通过。

## Blockers

Real Playwright E2E 未运行，因为未确认本地前后端运行态、登录账号、测试租户和可写测试数据。`pnpm ts:check` 失败在无关详情页缺少 `pagedRouteSnapshotRows`、`distributionStatusRows`、`pagedDistributionStatusRows`；全量迁移门禁失败在无关历史 SQL `20260730_mes_process_pool_team_leader.sql` 缺少 release metadata。当前主工作区存在非本任务脏改动且分支已领先 `origin/int_main`，无法安全完成独立提交、推送和 `completed` 状态。
