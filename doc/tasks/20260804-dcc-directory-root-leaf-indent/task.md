# 任务：DCC 根级未分类目录缩进修复

## Task Goal

修复 DCC 受控文件目录管理页中根级叶子目录（例如正式 `UNCLASSIFIED / 未分类`）被 Element Plus 树表占位符错误缩进的问题。根级目录无论是否有子目录，都必须与其它根级目录左侧图标对齐；真正子目录仍保留树层级缩进。

## Milestones

1. [in_progress] 建立任务记录、BDD 场景和 RED 静态契约。
2. [pending] 修复目录名称列的根级叶子占位符缩进。
3. [pending] 运行聚焦静态契约和相邻目录页回归。
4. [pending] 记录验证结果与收尾状态。

## Expected Verification

- `node tests/e2e/dcc-directory-folder-icon-inline-static.spec.js`
- `node tests/e2e/dcc-directory-folder-border-static.spec.js`
- `node tests/e2e/dcc-directory-lazy-loading-static.spec.js`

## Current Status

in_progress：正在补充根级叶子目录不缩进的静态契约和最小 CSS 修复。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，直接移除 Element Plus 树表叶子占位符对自定义目录图标的额外横向占位。
- `是否存在临时补丁或绕过`：否。
