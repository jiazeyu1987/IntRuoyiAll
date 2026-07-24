# DCC 项目代码关联文件 pageSize 超限修复

## Task Goal

修复 DCC 项目代码详情“关联文档”请求报错 `请求参数不正确:每页条数最大值为 200`，确保前端调用 `/dcc/project-codes/{id}/controlled-files/page` 时不会传超过后端上限的 `pageSize`。

## Milestones

1. [x] 创建任务文档并读取 PowerShell、前端、缺陷回归门禁。
2. [x] 补充 RED 静态回归测试，证明关联文件请求不得超过 200。
3. [x] 修复 `ProjectCodeTabPanel.vue` 关联文件请求分页参数。
4. [x] 运行 DCC 关联文档静态回归和 TypeScript 校验。
5. [x] 记录验证结果并提交前端 scoped 改动。

## Expected Verification

- 关联文件接口请求 `pageSize` 不超过后端最大值 200。
- 右侧文件表分页仍由 `associatedFilePage.pageSize` 控制。
- 三列分层展示仍保留：阶段、文件类型、文件列表。
- 切换页码时不重新刷新第一列/第二列分组数据。

## Experience Gates

- PowerShell：已读取根仓 `docs/powershell-memory.md`，中文文档读写使用 UTF-8。
- Frontend：已读取 `frontend-feature-delivery` 与 `frontend-contract.md`，不改后端接口契约。
- Bug 回归：已读取 `bug-regression-fix-loop` 与 `bug-contract.md`，先补失败回归测试再修复。
- Style：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`，本次仅改请求与状态逻辑，不做视觉重设计。

## Design Constraints

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，前端请求参数遵守后端分页契约。
- 是否存在临时补丁或绕过：否。

## Current Status

completed - 已修复关联文档导航请求 pageSize 超限问题，并通过静态回归与 TypeScript 校验。

## Root Cause

三列关联文档改造后，前端为了构建左侧阶段和中间文件类型导航，在 `getAssociatedFiles()` 中一次性用 `DCC_PROJECT_CODE_ASSOCIATED_NAVIGATION_PAGE_SIZE = 10000` 拉取关联文件。后端分页参数上限是 200，因此打开项目代码详情时会抛出 `请求参数不正确:每页条数最大值为 200`。

## Implementation

- 将 `DCC_PROJECT_CODE_ASSOCIATED_NAVIGATION_PAGE_SIZE` 调整为 200，遵守后端分页契约。
- `getAssociatedFiles()` 改为从第 1 页开始按 200 分页拉取，并根据 `total` 继续加载后续页，最终聚合为 `associatedNavigationFiles`。
- 右侧文件表继续使用本地 `associatedFilePage` 分页，切换右侧页码不重新请求后端，也不刷新左侧阶段和中间文件类型。

## Verification Result

- RED: `node tests/e2e/dcc-project-code-associated-three-column-static.spec.js` -> FAIL，静态契约检测到导航请求 pageSize 未固定为 200。
- GREEN: `node tests/e2e/dcc-project-code-associated-three-column-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/dcc-project-code-recognition-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/dcc-project-code-basic-data-static.spec.js` -> PASS。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
