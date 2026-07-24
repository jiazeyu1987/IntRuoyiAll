# Execution Log：NAS转移遇到已有V1.0时删除旧版再导入

BDD: replace existing V1.0 during NAS transfer only -> Given 目标类别下已存在同一文件的 `V1.0` 受控版本 When 用户再次通过 NAS 转移导入同一文件 Then 后端必须先删除旧的 `V1.0` 记录，再重新导入新的 `V1.0`

BDD: keep manual upload version rule unchanged -> Given 用户通过普通手工上传提交受控文件 When 当前链上已有同编号 `V1.0` Then 普通上传仍必须要求更高版本号，不得因为 NAS 转移需求而放宽

RED: repeated live transfer on `PD可编辑` -> FAIL before fix with `Controlled file version must be greater than the current chain version`

GREEN: `mvn --% -pl yudao-module-dcc -am -Dtest=DccControlledFileWorkflowServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS after adding NAS-transfer-only `V1.0` replacement logic

GREEN: `mvn --% -pl yudao-server -am -DskipTests package` -> PASS

GREEN: repeated live transfer on `selectedNasPaths=["1. QMS documents/PD可编辑"]`, `templateCategoryId=900298`, `effectiveDate=2026-05-22` -> PASS with `createdFileCount=4`, `failedFileCount=0`
