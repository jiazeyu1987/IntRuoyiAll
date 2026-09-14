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
- INFO: 经验门禁 -> `docs/experience-index.md` 命中 DCC / frontend / Element Plus / 未分类相关门禁；本任务仅处理 DCC 目录管理页展示缩进，不改后端目录 `parent_id`、正式 `UNCLASSIFIED` seed 或上传类别链路。
- RED: `node tests/e2e/dcc-directory-folder-icon-inline-static.spec.js` -> FAIL, expected reason: 缺少 `:deep(.dcc-directory-name-column .el-table__placeholder) { display: none; }`，根级叶子目录仍保留 Element Plus 占位缩进。
- GREEN: `node tests/e2e/dcc-directory-folder-icon-inline-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/dcc-directory-folder-border-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/dcc-directory-lazy-loading-static.spec.js` -> PASS。
- GREEN: `git diff --check -- IntRuoyiFronted/src/views/dcc/controlled-file/directories/index.vue IntRuoyiFronted/tests/e2e/dcc-directory-folder-icon-inline-static.spec.js doc/tasks/20260804-dcc-directory-root-leaf-indent` -> PASS，只有 CRLF warning，无 whitespace error。
- GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260804-dcc-directory-root-leaf-indent/bug-regression-evidence.md` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260804-dcc-directory-root-leaf-indent/frontend-feature-evidence.md` -> PASS。
- INFO: `pnpm ts:check` -> NOT COMPLETED，运行数分钟无输出和无退出；已按命令行精确匹配停止本次任务-owned `vue-tsc` 进程，未将该命令记为通过。
- BLOCKER: closeout-commit-push -> 当前仓库仍有其它任务无关未提交文件和本地 `int_main...origin/int_main [ahead 1]`，本任务不处理或回退这些无关变更。

## Verification

- 聚焦静态契约和相邻目录页回归通过；完整 TypeScript 检查未完成，提交/推送因无关脏工作区阻塞。
