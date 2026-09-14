# Frontend Feature Evidence

## Feature Goal And Non-goals

目标：修复 DCC 目录管理页根级叶子目录误缩进，使 `UNCLASSIFIED / 未分类` 与“质量管理”等根目录左对齐。

非目标：不修改后端目录树、目录 seed、上传类别解析、权限、懒加载接口或真实业务数据。

## Requirements And Acceptance

- 根级叶子目录不得因为 Element Plus placeholder 看起来像子目录。
- 子目录仍必须通过正式树层级缩进表达父子关系。
- 目录页原有文件夹图标、边框颜色和懒加载行为必须保留。

## UI Entry Points And Owned Files

- 入口：DCC 受控文件目录管理页。
- 组件：`IntRuoyiFronted/src/views/dcc/controlled-file/directories/index.vue`。
- 测试：`IntRuoyiFronted/tests/e2e/dcc-directory-folder-icon-inline-static.spec.js`。

## API Contracts And Data States

- 根目录仍由 `/dcc/directories/children` 未传 `parentId` 返回。
- 子目录仍由 `/dcc/directories/children?parentId=<id>` 返回。
- 本任务不改变 `parentId`、`hasChildren`、`children` 或 `UNCLASSIFIED` 数据契约。

## BDD Scenarios

- BDD: root leaf directory alignment -> Given DCC 目录接口返回根级“质量管理”和根级“未分类” / When 目录表渲染 / Then 两个根级文件夹图标左对齐。
- Given DCC 目录接口返回根级“质量管理”和根级“未分类” / When 目录表渲染 / Then 两个根级文件夹图标左对齐。
- Given 展开某目录返回真实子目录 / When 子目录渲染 / Then 子目录仍有树层级缩进。

## RED

- RED: `node tests/e2e/dcc-directory-folder-icon-inline-static.spec.js` -> FAIL，缺少 `.el-table__placeholder` 隐藏样式。
- `node tests/e2e/dcc-directory-folder-icon-inline-static.spec.js` -> FAIL，缺少 `.el-table__placeholder` 隐藏样式。

## GREEN

- GREEN: `node tests/e2e/dcc-directory-folder-icon-inline-static.spec.js` -> PASS。
- `node tests/e2e/dcc-directory-folder-icon-inline-static.spec.js` -> PASS。
- `node tests/e2e/dcc-directory-folder-border-static.spec.js` -> PASS。
- `node tests/e2e/dcc-directory-lazy-loading-static.spec.js` -> PASS。

## Verification

- Verification: 聚焦静态契约和相邻目录页回归通过；`pnpm ts:check` 未完成，不作为通过证据。

## UX And State Checks

- 响应式/布局：只移除叶子占位符横向空白，不改变列宽和文本省略。
- 可访问性：保留行名区域 `role="button"`、Enter/Space 展开和非可展开目录禁用 tabindex。
- 加载/空态/错误：保留懒加载和子目录加载错误标签。
- 权限：不改 `v-hasPermi` 控制的操作按钮。

## Blockers And Follow-up

- `pnpm ts:check` 长时间未退出，已停止，未记为通过。
- 仓库存在大量预先已有无关脏改和 ahead 状态，未执行提交和推送。
