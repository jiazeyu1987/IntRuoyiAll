# G09 本轮文件清单

## A20 最新差异（仅2文件，2026-10-15）

- IntRuoyiFronted/src/views/dcc/controlled-file/view/OnlyOfficeReadOnlyViewer.vue：新增typed error事件；真实onError增加当前精确URL条件并emit保留的message，原generation/卸载/只读/旋转不变。
- IntRuoyiFronted/tests/unit/dcc-g09-office-readonly.test.cjs：afteropened、切URL迟到/watch前窗口、同URL旧generation、卸载、实际父handler组合用例；原“无opened”断言按opened事件类型保持，允许新增正式error。
- 任务主报告、baseline-a20-sha256.json、final-a20-sha256.json更新；后端、Query/workflow/view父页、OfficeToken全部未写。
- 最新Office hash=9C01E796B673099EB68E5561C583AC73AEF051D61A39A4B69567D5DE13597199；测试hash=8155EEC4941394FBEC8FDBA823B0A57D3C685F5A9EF75218E0DDEEF245BE7E1A。旧final-c04对应两条是历史，不再作为当前源码匹配证据。

## C04 最新变更（2026-10-15，现已冻结待 Root review）

| 路径（后端前缀 IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/） | 变化 |
|---|---|
| controller/admin/file/vo/DccControlledFilePreviewMetadataRespVO.java | previewRegisteredAt String / Jackson READ_ONLY |
| service/audit/DccControlledFileAccessAuditService.java | 仅 PREVIEW 扩展原事件 insert/readback、完整身份与时间核对；既有带 process 检查保留 |
| service/preview/DccControlledPreviewAccessService.java | PREVIEW UTC 秒签发、当前 tenant 与 readback identity/time 核对；非 PREVIEW 时刻及流程不变 |
| service/filepreview/DccOnlineFilePreviewServiceImpl.java | null issuedAt 拒绝及 readonly UTC ISO offset 映射；原 business file 授权不变 |
| IntRuoyiFronted/src/views/dcc/controlled-file/view/OnlyOfficeReadOnlyViewer.vue | 可视旋转/复原、尺寸跟随、归零、DocEditor 100% 高度；原只读配置/URL/业务回执不变 |

测试新增 DccG09PreviewRegisteredAtTest.java、DccG09OnlinePreviewRegisteredAtTest.java、dcc-g09-registration-hunk.test.cjs；扩展 dcc-g09-office-readonly.test.cjs。相邻 DccControlledPreviewAccessServiceTest.java 仅 setUp 设置原请求 31L 的真实 tenant，上层 BaseDbUnitTest 既有清理保留；否则原测试在 tenant1 下发 tenant31 请求，被新增正式门禁正确拒绝。进场 fingerprint 单独保留 baseline-c04-adjacent-test.json。

私有复跑工具 run-c04-java.ps1 / G09TestLauncher.java、C04 两份报告和接入 hunk 已归档。C04 最终 SHA256 以 final-c04-sha256.json 为准；下方首轮 hash 仅记录 A05 历史交付。

未写 QueryServiceImpl、workflow.ts、view/index.vue、OfficeToken、vendor、schema 或端点；共享接入由 Root 已分派 G07/G01。

## 生产文件

| 路径 | 本轮差异 |
|---|---|
| IntRuoyiFronted/src/views/dcc/controlled-file/view/index.vue | PDF 搜索及页码工具、逐页 DOM refs、页码同步、真实索引状态、同步查询失效、加载世代隔离；直接 destroy 旧 PDF、画布缺失显性错误 |
| IntRuoyiFronted/src/views/dcc/controlled-file/view/OnlyOfficeReadOnlyViewer.vue | 仅 mountEditor 开始新增 resetZoom / 清空指针和 modifier，共 3 行；其余既有 opened/世代保护未冒认本任务成果 |
| IntRuoyiFronted/src/views/dcc/controlled-file/view/pdf-tools.ts | 新增只读 textContent 提取及标准化字面匹配 helper，取消与异常区分 |

授权的 `view/presentation.ts` 未修改；baseline 中 index/Office 已有的 opened / distribution 身份保护全部保留。相对 Git HEAD 的 diff 包含进场前既有改动，不等于本线程全部改动。

## 新增专属测试

- IntRuoyiFronted/tests/unit/dcc-g09-viewer-tools.test.cjs
- IntRuoyiFronted/tests/unit/dcc-g09-pdf-text.test.cjs
- IntRuoyiFronted/tests/unit/dcc-g09-office-readonly.test.cjs

## 任务资产

- 本目录 task.md、execution-log.md、baseline-sha256.json、verification-report.md、changed-files.md、integration-notes.md、review-ready.md。
- 测试均在正式 tests/unit 下；本任务目录没有临时测试脚本、截图或 stdout 产物。

## 最终生产 SHA256（原始字节）

| 文件 | SHA256 |
|---|---|
| view/index.vue | 610605E00ED1986F239D0B7E603647D5E64043F3D9494F4AD4AD054312910289 |
| view/OnlyOfficeReadOnlyViewer.vue | B44E7069E0218DADE6020A4082EEA1393FBFE8040E960285AE535086B8ED0CD7 |
| view/presentation.ts（未改） | 4514A00B20176E8BB3B261B8705803776F52FBF39134B6234CC5A37FADF6CB46 |
| view/pdf-tools.ts | A84B1D1BB4358EF2DE1974B42B5017411CD0EAC5967D0502EF3674DE34A53B50 |
