# G12 文件清单（阶段性）

## 已修改
- IntRuoyiFronted/src/views/dcc/controlled-file/upload/index.vue：选填提示、冻结阶段仅校验长度。
- IntRuoyiFronted/src/views/dcc/controlled-file/upload/submitter.ts：空说明序列化缺省，保留真实非空说明及长度校验。
- IntRuoyiFronted/src/api/dcc/controlledFile/workflow.ts：仅A02原因锚点改可选string/trim后500字检查。
- IntRuoyiFronted/tests/unit/dcc-upload-actual-attributes.test.cjs：仅A02旧原因断言更新，保留缺文件夹及超长拒绝。
- IntRuoyiBackend/config/gxp-audit-policy.yaml：仅新增upload-bind OPTIONAL_TEXT条目。
- IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/projectcode/G48StorageProjectionTest.java：新增g12*方法、自有fixture与唯一参数项目名；共享基类未改。
- A37追加的唯一fixture hunk：`realActorDirectory()` 的 `dcc_file_category` INSERT 显式写入 `approval_source_mode='PUBLIC_MATRIX'`；不代表新的产品代码或产品RED。
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccPublicUploadPlacementService.java：正常NEW可选真实说明，trim后500字上限，其它权限/归属校验保留。
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/projectcode/folder/DccProjectFilePlacementService.java：独立upload-bind可选说明，严格bind不变，server context必需且重放前验证。

## 已新增
- IntRuoyiFronted/tests/unit/dcc-upload-g12-optional-folder-reason.test.cjs：11 个实际模块/页面函数行为测试。
- IntRuoyiFronted/tests/unit/dcc-upload-g12-api-optional-folder-reason.test.cjs：Root 公共 wrapper 接入的真实函数行为测试，独立 RED 交接。
- doc/tasks/20261014-dcc-gap-optional-folder-reason/ 下 G12 自有记录。

## 软件已接收/文件正式归还
Root按A07正式接收前端57/57、后端g12*13/13 GREEN+当前派生storage专属5/5回归及最小diff。前端已A04交G01；两服务/G48测试归还Root并冻结，policy归还后由G01仅新增confirmation条目且保留strict/upload-bind。G12不再写共享文件。交还hash见a02-frontend-release-hashes.json与m02-release-hashes.json，最终索引m02-final-receipt.json；不将并行后续改动归为G12证据。

## 未写保留文件
除A02精确锚点外的公共Workflow/API、所有全局审计与共享测试基类未修改。A04后三个前端生产归G01，G12不再写。无Git提交推送、真实库/服务/E2E操作。

## A37 冻结归还
- 改前 G48StorageProjectionTest SHA256：FFD67BF735452C5FC3AF04D590FFD2E0C500A26F55FCCD1CD4C37E7476216FAA。
- 改后 SHA256：97A99DC4E34F987EBA42B3B2E8F7D06F76158F248DF5431D7EE234CD9AF02354。
- Root最终M14按正确fixture执行G48声明18+原父类27=45/45，0F/E/skip；合并M12有效47份class报告1008/1008，全软件验收已正式接收。本线程不跑Maven或旧M02，不改生产/schema/负向缺值用例；仅Git收尾未授权，Root统一清理/报告。
