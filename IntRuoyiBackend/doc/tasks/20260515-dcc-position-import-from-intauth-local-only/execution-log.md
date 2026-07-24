# Execution Log: DCC 审批岗位从 IntAuth 一次性导入并改为本地只读

BDD: 审批岗位列表只读取本地表 -> Given 本地 `dcc_approval_position` 已经存在导入后的 IntAuth 岗位映射 / When 管理员请求 `GET /dcc/approval-positions` / Then 列表接口只返回本地激活岗位且不在运行时调用 IntAuth。
BDD: 管理员显式导入 IntAuth 审批岗位 -> Given IntAuth 内部岗位接口可返回当前岗位列表 / When 管理员请求 `POST /dcc/approval-positions/import-intauth` / Then 系统把缺失岗位导入本地、复用同名本地岗位并保留岗位分配数据。
BDD: IntAuth 岗位导入配置缺失时失败 -> Given `yudao.dcc.int-auth` 配置缺失 / When 管理员请求岗位导入 / Then 后端返回明确的 IntAuth 岗位导入配置错误且不写入本地岗位。

## TDD / Verification Evidence

- M1: Completed. Previous backend task `20260515-dcc-runtime-seed-garbled-names` is completed and does not block this change.
- M2: Completed. This task document and execution log were created before production code changes.
- RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc -Dtest=DccApprovalPositionAdminServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, test compile stopped at missing symbol `DccApprovalPositionImportResult` and missing method `importPositionsFromIntAuth()`, proving the explicit import contract and local-only list path did not exist yet.
- GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc -Dtest=DccApprovalPositionAdminServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS, 7 focused approval-position tests green after adding the explicit import result, explicit import endpoint, and local-only list behavior.
- GREEN: normal list path verification -> PASS, `getPositionList_readsImportedLocalTableWithoutCallingIntAuth` now asserts `verifyNoInteractions(intAuthPositionClient)`.
- Blocked broader follow-up: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc -Dtest=DccApprovalPositionAdminServiceImplTest,DccIntAuthPositionClientImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> BLOCKED by unrelated dirty directory-import test files in the same repo, not by this approval-position change.
