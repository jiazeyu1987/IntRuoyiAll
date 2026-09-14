# Bug Regression Evidence

## Bug

用户在 DCC 文件上传页签选择文件上传时，页面提示 `Current user cannot access this controlled file`。

## Expected

文件上传页应只展示当前用户具备类别级 `UPLOAD` 权限的文件类别；如果旧表单状态残留无权限类别，应在前端表单校验阶段明确拦截。后端上传预览/提交仍必须对无类别 `UPLOAD` 权限请求 fail-fast，不得放宽权限、吞异常或默认成功。

## Reproduction

- 静态复现：上传页原逻辑只按 `category.active && Boolean(category.directoryId)` 计算可上传类别，缺少 `canUpload` 过滤。
- 后端复现：无类别 `UPLOAD` 权限时直接调用 `upload-preview`，`DccControlledFileCategoryPermissionSupport` 正确拒绝，用户在文件选择后才看到权限错误。

## Root Cause

类别列表接口没有向前端投影当前用户对每个文件类别的 `UPLOAD` 权限，上传页也没有基于类别级上传权限过滤下拉选项。后端错误不是误判，而是用户选择了本不应可选的无上传权限类别后触发的正式权限保护。

## RED

- RED: `node tests\e2e\dcc-upload-category-permission-static.spec.js` -> FAIL，预期原因：类别 API 类型缺少 `canUpload`，上传页未过滤 `canUpload=false` 类别。
- RED: `mvn.cmd -pl yudao-module-dcc "-Dtest=DccFileCategoryControllerConfigPackageContractTest#getCategoryList_projectsCurrentUserUploadPermission,DccFileCategoryControllerConfigPackageContractTest#getCategoryList_withoutLoginUserDoesNotGrantUploadProjection" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，预期原因：`DccFileCategoryRespVO#getCanUpload()` 缺失。

## GREEN

- GREEN: `node tests\e2e\dcc-upload-category-permission-static.spec.js` -> PASS。
- GREEN: `mvn.cmd -pl yudao-module-dcc "-Dtest=DccControlledFileUploadApiTest#uploadPreviewFile_withoutCategoryUploadPermission_deniesBeforePolicyOrStorage,DccFileCategoryControllerConfigPackageContractTest#getCategoryList_projectsCurrentUserUploadPermission,DccFileCategoryControllerConfigPackageContractTest#getCategoryList_withoutLoginUserDoesNotGrantUploadProjection" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，3 tests / 0 failures / 0 errors。
- GREEN: `pnpm exec eslint src\api\dcc\controlledFile\fileCategories.ts src\views\dcc\controlled-file\upload\index.vue src\views\dcc\controlled-file\external-review\index.vue tests\e2e\dcc-upload-category-permission-static.spec.js --format stylish` -> PASS。

## Verification

- 后端类别列表投影：`DccFileCategoryControllerConfigPackageContractTest#getCategoryList_projectsCurrentUserUploadPermission` 覆盖有登录用户时按类别级 `UPLOAD` 权限返回 `canUpload`。
- 后端匿名/无登录保护：`DccFileCategoryControllerConfigPackageContractTest#getCategoryList_withoutLoginUserDoesNotGrantUploadProjection` 覆盖无登录用户不默认授权。
- 后端拒绝回归：`DccControlledFileUploadApiTest#uploadPreviewFile_withoutCategoryUploadPermission_deniesBeforePolicyOrStorage` 覆盖无类别 `UPLOAD` 权限仍在上传预览前拒绝。
- 前端静态合同：`dcc-upload-category-permission-static.spec.js` 覆盖 API 类型、上传页过滤 `canUpload=false`、旧选择校验报错。
- 只读真实页面冒烟：本机上传页渲染成功，类别接口返回 60 条并均包含 `canUpload`，检测到 1 条 `canUpload=false` 类别，DCC 写请求数为 0。

## Blockers

- 完整真实上传 E2E 未执行：该路径会创建 DCC 受控文件，当前没有明确写入型测试租户/账号授权、任务自有测试数据和清理策略。
- Git closeout 未完成：当前分支存在无关 ahead 提交和多个并行任务脏改动；不得把非本任务变更混入提交/推送。
