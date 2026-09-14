# Bug Regression Evidence

## Bug Summary

DCC 上传页选择已配置审批矩阵的技术调研报告类别时，预检仍显示审批岗位 0 个、会签/签核岗位 0 个，并提示“审批链路不完整”。

## Expected Behavior

`/dcc/file-categories` 返回文件类别列表时，应为已配置当前有效审批矩阵的类别投影 `signoffPositionIds` 与 `approvalPositionIds`。前端上传预检基于这两个字段判断审批链路完整性，不应把已配置类别误判为未配置。

## Reproduction

- BDD: DCC 文件类别列表投影当前有效审批矩阵 -> Given 文件类别存在当前有效审批路线且路线包含 MATRIX_REVIEW 会签岗位与 MATRIX_APPROVAL 批准岗位 When 上传页调用 `/dcc/file-categories` Then 响应中的该类别包含 `signoffPositionIds` 和 `approvalPositionIds`，前端预检不应误判为审批链路不完整。
- RED: `mvn -pl yudao-module-dcc -am "-Dtest=DccFileCategoryControllerConfigPackageContractTest#getCategoryList_projectsActiveApprovalMatrixPositionIds" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: 服务契约与实现尚不存在。

## Root Cause

`DccFileCategoryRespVO` 已定义 `signoffPositionIds` / `approvalPositionIds`，但 `DccFileCategoryController#getCategoryList()` 只从类别主表映射基础字段，并额外设置 `directoryId` 与 `canUpload`，没有读取 DCC 分类审批矩阵的当前有效路线节点。因此前端收到空数组或 null 后显示审批链路不完整。

## Fix

- 在 `DccCategoryApprovalMatrixAdminService` 增加只读投影方法，按类别批量读取最新 active 审批路线。
- 从路线节点的 `MATRIX_REVIEW` / `MATRIX_APPROVAL` 阶段提取 DCC 岗位 ID，保序去重后返回。
- 在 `DccFileCategoryController#getCategoryList()` 回填 `signoffPositionIds` / `approvalPositionIds`，未配置有效路线时保持空数组。

## GREEN

- GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccFileCategoryControllerConfigPackageContractTest#getCategoryList_projectsActiveApprovalMatrixPositionIds,DccCategoryApprovalMatrixAdminServiceImplTest#getActiveMatrixPositionIdsByCategoryIds_readsLatestActiveRoutePositionNodes" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.compiler.useIncrementalCompilation=false" test` -> PASS, Tests run: 2, Failures: 0, Errors: 0, Skipped: 0.
- GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccFileCategoryControllerConfigPackageContractTest#getCategoryList_projectsActiveApprovalMatrixPositionIds,DccCategoryApprovalMatrixAdminServiceImplTest#getActiveMatrixPositionIdsByCategoryIds_readsLatestActiveRoutePositionNodes" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 2, Failures: 0, Errors: 0, Skipped: 0.
- GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccFileCategoryControllerConfigPackageContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 5, Failures: 0, Errors: 0, Skipped: 0.

## Verification

目标用例和控制器相邻契约均已通过，证明类别列表接口会返回当前有效审批矩阵岗位投影，同时保留目录绑定、上传权限、导入和导出契约。

真实前端路径 E2E 已通过：`node tests\e2e\dcc-upload-approval-chain-projection-real.e2e.js` 选择 `/dcc/controlled-file/upload` 的“技术调研报告”，运行态返回审批岗位 2 个、会签/签核岗位 5 个，页面预检显示“审批人链路已具备”，且 `dccWriteRequests=[]`。

## Risk And Regression Scope

- Scope is limited to DCC file category list response projection and approval matrix read model.
- No fallback, default approver, BPM shortcut, frontend bypass, or hardcoded category-specific behavior was introduced.
- Categories without active approval matrix still return empty signoff/approval arrays so the existing upload precheck continues to fail fast.

## Blockers And Follow-Up

- Commit and push are not completed because the workspace already had unrelated dirty files and `int_main` was ahead of `origin/int_main` before this task. This task did not stage, commit, revert, or mix unrelated changes.
