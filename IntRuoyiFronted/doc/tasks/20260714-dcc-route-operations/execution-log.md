# Execution Log

BDD: 流程路线主列表新增入口 -> Given 用户打开 DCC 流程路线列表 / When 点击顶部“新增路线” / Then 系统打开路线表单并允许选择文件类别后配置节点。

BDD: 流程路线主列表行级修改 -> Given 主列表存在一条审批路线 / When 用户点击该行“修改” / Then 系统打开路线表单并带入当前行版本数据，文件类别不可被误改。

BDD: 流程路线主列表行级删除 -> Given 主列表存在一条审批路线 / When 用户确认删除该行 / Then 系统只删除当前路线版本及其节点，并刷新列表；不存在路线必须显式失败。

BDD: 无 fallback 路线预览 -> Given 删除后类别无启用路线 / When 用户查询或预览该类别路线 / Then 系统展示后端真实错误，不自动恢复旧版本或返回默认成功。

STATUS: task-start -> PASS，已建立前端任务文档并记录经验门禁。

RED: pnpm e2e:dcc:route-operations:static -> FAIL，缺少 `deleteApprovalRoute` 前端 API 和路线新增/修改/删除操作入口。

GREEN: pnpm e2e:dcc:route-operations:static -> PASS，主列表新增“新增路线”按钮、行级“修改/删除”操作和删除 API 契约均通过。

GREEN: pnpm e2e:dcc:route-summary:static -> PASS，审批路线主列表节点列契约保持通过。

GREEN: pnpm e2e:dcc:routes-list-display:static -> PASS，审批路线分页列表契约保持通过。

GREEN: pnpm e2e:dcc:routes-node-columns:static -> PASS，节点列真实 `nodes` 数据解析契约保持通过。

BLOCKER: pnpm build:local -> TIMEOUT，5 分钟未返回；改用项目已有 relaxed 类型检查验证本轮前端修改。

BLOCKER: pnpm exec vue-tsc --noEmit --skipLibCheck -> FAIL，默认堆 OOM；提高 `NODE_OPTIONS=--max-old-space-size=8192` 后严格类型检查仍被既有无关 TS 错误阻塞，错误位于 approval-center、dcc browser/logs、mes edhr/scheduleorder、signature-governance 等非本任务文件。

GREEN: node node_modules/vue-tsc/bin/vue-tsc.js --noEmit -p tsconfig.relaxed.json -> PASS，使用项目已有 relaxed 配置完成本轮前端类型验证。

GREEN: frontend-feature-evidence-validator -> PASS，`frontend-feature-evidence.md` 证据完整。

GREEN: git diff --check -> PASS，本任务前端文件无空白错误。

GREEN: 2026-07-14 recheck pnpm e2e:dcc:route-operations:static -> PASS，操作面板静态契约通过。

GREEN: 2026-07-14 recheck pnpm e2e:dcc:route-summary:static -> PASS，路线摘要/节点列契约通过。

GREEN: 2026-07-14 recheck pnpm e2e:dcc:routes-list-display:static -> PASS，列表展示契约通过。

GREEN: 2026-07-14 recheck pnpm e2e:dcc:routes-node-columns:static -> PASS，节点列契约通过。

GREEN: 2026-07-14 recheck node node_modules/vue-tsc/bin/vue-tsc.js --noEmit -p tsconfig.relaxed.json -> PASS，`NODE_OPTIONS=--max-old-space-size=8192`。

GREEN: 2026-07-14 recheck frontend-feature-evidence-validator -> PASS。

GREEN: 2026-07-14 recheck git diff --check -> PASS，本任务前端文件无空白错误。

GREEN: task-closeout-cleanup preview -> PASS，保留 `task.md`、`execution-log.md`、`verification-report.md`，待清理 `frontend-feature-evidence.md`，无阻塞、无警告。

GREEN: task-closeout-cleanup apply -> PASS，已清理 `frontend-feature-evidence.md`；当前主工作区 `int_main` 非 linked worktree，无需合并或删除 worktree。
