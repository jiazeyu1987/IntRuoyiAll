# Execution Log

## Intent

- 用户反馈上传技术调研报告时提示“审批链路不完整”，要求修复。
- 截图现象为上传预检卡片显示审批岗位 0 个、会签/签核岗位 0 个，请先补齐分类审批链路。

## BDD

- BDD: DCC 文件类别列表投影当前有效审批矩阵 -> Given 文件类别存在当前有效审批路线且路线包含 MATRIX_REVIEW 会签岗位与 MATRIX_APPROVAL 批准岗位 When 上传页调用 `/dcc/file-categories` Then 响应中的该类别包含 `signoffPositionIds` 和 `approvalPositionIds`，前端预检不应误判为审批链路不完整。

## Findings

- 前端上传页根据所选类别的 `approvalPositionIds` 和 `signoffPositionIds` 计算审批链路是否完整。
- `DccFileCategoryRespVO` 已定义上述字段，但 `DccFileCategoryController#getCategoryList()` 只设置 `directoryId` 与 `canUpload`，未投影审批矩阵路线节点。
- 当前修复不改前端兜底逻辑，后端补齐正式接口契约。

## Verification Evidence

- RED: `mvn -pl yudao-module-dcc -am "-Dtest=DccFileCategoryControllerConfigPackageContractTest#getCategoryList_projectsActiveApprovalMatrixPositionIds" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: 新增回归引用 `DccCategoryApprovalMatrixAdminService#getActiveMatrixPositionIdsByCategoryIds` 与 `MatrixPositionIds`，生产服务契约尚未实现。
- GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccFileCategoryControllerConfigPackageContractTest#getCategoryList_projectsActiveApprovalMatrixPositionIds,DccCategoryApprovalMatrixAdminServiceImplTest#getActiveMatrixPositionIdsByCategoryIds_readsLatestActiveRoutePositionNodes" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.compiler.useIncrementalCompilation=false" test` -> PASS, Tests run: 2, Failures: 0, Errors: 0, Skipped: 0.
- GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccFileCategoryControllerConfigPackageContractTest#getCategoryList_projectsActiveApprovalMatrixPositionIds,DccCategoryApprovalMatrixAdminServiceImplTest#getActiveMatrixPositionIdsByCategoryIds_readsLatestActiveRoutePositionNodes" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 2, Failures: 0, Errors: 0, Skipped: 0.
- GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccFileCategoryControllerConfigPackageContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 5, Failures: 0, Errors: 0, Skipped: 0.
- Evidence validator: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260804-dcc-upload-approval-chain-projection\bug-regression-evidence.md` -> PASS, Bug regression evidence is valid.
- Experience consolidation: updated `docs/frontend-development.md#DCC 上传类别权限投影门禁` with `审批链路不完整` / `approvalPositionIds` / `signoffPositionIds` gate; `rg -n "审批链路不完整|approvalPositionIds|20260804-dcc-upload-approval-chain-projection" docs\frontend-development.md` -> PASS.
- E2E RED: `node tests\e2e\dcc-upload-approval-chain-projection-real.e2e.js` -> FAIL, expected reason: 48081 仍运行旧 `backend-runtime-control-20260804-dcc-nas-uncontrolled-import.jar`，真实 `/dcc/file-categories` 未返回 `approvalPositionIds`。
- Runtime refresh: `powershell -NoProfile -ExecutionPolicy Bypass -File doc\tasks\20260804-dcc-upload-approval-chain-projection\patch-runtime-jar.ps1` -> PASS, generated `output\runtime\int_main\backend-runtime-control-20260804-dcc-upload-approval-chain-projection-20260804-114202.jar`, SHA256 `95868975041F498D41328074EDB4F6794949C2C76B52B920F3D771C318083622`, nested `yudao-module-dcc` stored uncompressed.
- Runtime restart: `powershell -NoProfile -ExecutionPolicy Bypass -File doc\tasks\20260804-dcc-upload-approval-chain-projection\restart-runtime-jar.ps1 -NewJar E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260804-dcc-upload-approval-chain-projection-20260804-114202.jar -OldPid 14800` -> PASS, old PID 14800 stopped after ownership check, new PID 72116 health `UP`.
- E2E GREEN: `node tests\e2e\dcc-upload-approval-chain-projection-real.e2e.js` -> PASS, evidence `output\playwright\20260804-dcc-upload-approval-chain-projection\dcc-upload-approval-chain-projection-real-evidence.json`; runtime category `技术调研报告` returned approval positions 2, signoff positions 5, UI preflight showed `审批人链路已具备`, DCC write requests `[]`.

## Completed Work

- Added `DccCategoryApprovalMatrixAdminService.MatrixPositionIds` and `getActiveMatrixPositionIdsByCategoryIds(List<Long>)`.
- Implemented active-route projection from latest active `dcc_category_approval_route` and stage 2/3 `dcc_category_approval_route_node` rows.
- Updated `DccFileCategoryController#getCategoryList()` to return `signoffPositionIds` / `approvalPositionIds` while preserving `directoryId` and `canUpload`.
- Added controller and service regression tests for the “技术调研报告 / INTAUTH-26” approval-chain projection.
- Added focused Playwright E2E `tests/e2e/dcc-upload-approval-chain-projection-real.e2e.js` to validate the real upload page preflight against local runtime.
- Refreshed local 48081 runtime from copied old Jar by replacing only the task-owned DCC class files required for the category projection.

## Blockers

- 当前工作区已有大量无关脏改动且 `int_main` ahead 8；本任务只触碰 DCC 类别接口、当前任务文档和当前回归测试，不混入其它改动。
- 提交/推送未执行，避免在未授权情况下把无关脏改动或基线提交混入当前修复。
