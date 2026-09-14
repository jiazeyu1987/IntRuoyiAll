# Bug Regression Evidence

## Bug Summary

DCC 受控文件目录管理页中，正式根级 `UNCLASSIFIED / 未分类` 目录没有父目录，但页面显示时被 Element Plus 树表叶子占位符额外缩进，看起来像“质量管理”的子目录。

## Expected Behavior

根级叶子目录和根级父目录的文件夹图标必须左对齐；真实子目录仍保留树层级缩进。

## Reproduction Command

- `node tests/e2e/dcc-directory-folder-icon-inline-static.spec.js`

## Root Cause

目录页已隐藏 Element Plus 默认展开图标 `.el-table__expand-icon`，并使用自定义文件夹图标承接展开视觉；但叶子节点仍保留 `.el-table__placeholder` 横向占位，导致无子级的根级目录比有子级的根级目录多出一段缩进。

## Regression Test

在 `dcc-directory-folder-icon-inline-static.spec.js` 中补充断言，要求目录名称列同时隐藏 `.el-table__placeholder`。

## RED

- RED: `node tests/e2e/dcc-directory-folder-icon-inline-static.spec.js` -> FAIL，缺少隐藏 `.el-table__placeholder` 的样式。
- `node tests/e2e/dcc-directory-folder-icon-inline-static.spec.js` -> FAIL，缺少隐藏 `.el-table__placeholder` 的样式。

## GREEN

- GREEN: `node tests/e2e/dcc-directory-folder-icon-inline-static.spec.js` -> PASS。
- `node tests/e2e/dcc-directory-folder-icon-inline-static.spec.js` -> PASS。
- `node tests/e2e/dcc-directory-folder-border-static.spec.js` -> PASS。
- `node tests/e2e/dcc-directory-lazy-loading-static.spec.js` -> PASS。

## Verification

- Verification: 聚焦静态契约和相邻目录页回归通过；`pnpm ts:check` 未完成，不作为通过证据。

## Risk And Regression Scope

风险低。修复只作用于 DCC 目录名称列的 Element Plus 树表占位符，保留 `.el-table__indent` 树层级缩进，不改变后端目录数据、懒加载接口、权限或目录父子关系。

## Blockers And Follow-up

- `pnpm ts:check` 长时间未退出，未记为通过。
- 仓库存在大量预先已有无关脏改和本地 ahead 状态，本任务未提交或推送。
