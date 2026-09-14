# 20260728 DCC 上传页签受控文件访问权限修复

## Task Goal

修复用户在文件上传页签上传文件时出现 `Current user cannot access this controlled file` 的问题，确保用户只能选择具备类别级 `UPLOAD` 权限的文件类别，后端上传预览/提交仍保留正式权限拦截。

## Milestones

- [x] 建立缺陷复现与预期行为记录。
- [x] 定位上传页签触发受控文件访问校验的根因。
- [x] 先补 RED 回归测试，再实施最小正式修复。
- [x] 运行目标 GREEN 与相关回归验证。
- [x] 完成证据、风险与收尾记录。

## Expected Verification

- 后端类别列表返回当前用户类别级 `canUpload` 投影。
- 前端上传页与外来文件评审页不展示 `canUpload=false` 的类别，并拦截旧选择。
- 上传预览服务仍在用户缺少类别级 `UPLOAD` 权限时 fail-fast 拒绝。
- 受影响模块的目标测试通过。
- 不引入 fallback、降级、吞异常、mock 成功或默认成功。

## Current Status

ready_for_closeout

## Root Cause

上传页签原本只按 `active + directoryId` 展示文件类别，没有消费当前用户在类别维度的 `UPLOAD` 权限。用户选择了无上传权限的类别后，后端 `upload-preview` 正确通过 `DccControlledFileCategoryPermissionSupport` fail-fast 拒绝，于是页面在文件选择后才出现 `Current user cannot access this controlled file`。本次修复保留后端拒绝逻辑，在类别列表增加 `canUpload` 投影，并让前端在选择前过滤和校验无上传权限类别。

## Verification Evidence

- `node tests\e2e\dcc-upload-category-permission-static.spec.js` -> PASS。
- `pnpm exec eslint src\api\dcc\controlledFile\fileCategories.ts src\views\dcc\controlled-file\upload\index.vue src\views\dcc\controlled-file\external-review\index.vue tests\e2e\dcc-upload-category-permission-static.spec.js --format stylish` -> PASS。
- `mvn.cmd -pl yudao-module-dcc "-Dtest=DccControlledFileUploadApiTest#uploadPreviewFile_withoutCategoryUploadPermission_deniesBeforePolicyOrStorage,DccFileCategoryControllerConfigPackageContractTest#getCategoryList_projectsCurrentUserUploadPermission,DccFileCategoryControllerConfigPackageContractTest#getCategoryList_withoutLoginUserDoesNotGrantUploadProjection" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，3 tests / 0 failures / 0 errors。
- 本机只读页面冒烟：`http://127.0.0.1:8081/dcc/controlled-file/upload` 渲染成功，类别接口返回 60 条且均包含 `canUpload`，检测到 1 条 `canUpload=false` 类别，DCC 写请求数为 0；未执行真实上传写入。

## Remaining Closeout Blockers

- `task-closeout-cleanup` preview/apply 已执行，保留 `task.md`、`execution-log.md`、`verification-report.md`、`bug-regression-evidence.md`，删除项为 none。
- 完整真实上传 E2E 会创建 DCC 受控文件，需要明确的写入型测试租户/账号授权、任务自有测试数据和清理策略；当前仅执行只读页面冒烟，未用 API-only 或 mock 替代。
- 当前 `int_main` 分支存在无关 ahead 提交和多个并行任务脏改动；为避免把非本任务变更混入提交/推送，本任务只能停在 `ready_for_closeout`，不得标记 `completed`。

## 经验门禁

- Trigger: DCC 受控文件上传、`upload-preview`、`Current user cannot access this controlled file`、类别权限、文件类别下拉。
- Preflight check: 先区分菜单权限 `dcc:controlled-file:submit/query` 与类别级 `UPLOAD` 权限；上传页不得展示当前用户无 `UPLOAD` 权限的类别，后端上传预览/提交仍必须 fail-fast 拦截无权限类别。
- Blocker: 缺少类别级上传权限投影、前端仅靠上传接口报错、或为消除报错放宽后端 `UPLOAD` 权限校验时必须停止。
- Verification: 后端类别列表投影 `canUpload`，前端静态契约验证上传页过滤 `canUpload=false`，并运行上传服务原有权限拒绝测试确保后端拦截仍保留。
- Forbidden action: 禁止绕过 `DccControlledFileCategoryPermissionSupport`、禁止把无权限类别当可上传类别展示、禁止吞掉 `CONTROLLED_FILE_ACCESS_DENIED` 或改成默认上传成功。
- Evidence: 本任务 `doc/tasks/20260728-dcc-upload-controlled-file-access/`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，正式修复为后端权限投影 + 前端选择前拦截，同时保留后端上传权限 fail-fast。
- `是否存在临时补丁或绕过`：否。

## Cleanup Keep

- doc/tasks/20260728-dcc-upload-controlled-file-access/bug-regression-evidence.md
