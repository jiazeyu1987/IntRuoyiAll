# 执行记录：DCC 根级未分类目录缩进修复

## User Intent

- 用户指出：正式 `UNCLASSIFIED / 未分类` 是根级目录，不在“质量管理”目录下，因此页面不应显示子级缩进。

## BDD / TDD

- BDD: 根级叶子目录与根级父目录对齐 -> Given DCC 目录接口返回两个根级目录“质量管理”和“未分类”，其中“未分类”没有子目录 / When 目录管理页渲染树表第一列 / Then “未分类”的文件夹图标必须与“质量管理”的文件夹图标左对齐，不得因为 Element Plus 叶子占位符产生额外缩进。
- BDD: 子目录仍保留层级缩进 -> Given 某目录展开后返回真实子目录 / When 子目录行渲染 / Then 子目录仍通过正式树层级缩进区分父子关系，不得把所有层级拍平。

## Evidence

- INFO: 技能 -> 已读取 `bug-regression-fix-loop`、`frontend-feature-delivery` 及其 evidence contract。
- INFO: 规则 -> 已读取 `docs/frontend-development.md`、`docs/powershell-encoding.md`、`docs/task-closeout-rules.md`。
- INFO: 工作区 -> 当前仓库已有大量无关脏改，本任务只修改 DCC 目录页、相邻静态契约和本任务文档，不回退其它文件。

## Verification

- pending
