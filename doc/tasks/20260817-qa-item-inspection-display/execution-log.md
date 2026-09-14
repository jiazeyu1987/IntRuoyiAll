# Execution Log

## User Intent

- 用户确认本次仅修复 QA 配置页显示，不修改一线 PQC 页面。
- 同一 QA 工序下不同检验项目必须独立显示首检和巡检配置，例如“组装Ⅰ / 外观”首检 13 件，“组装Ⅰ / 撤压”首检 5 件，巡检比例也分别读取。
- 修复当前不满足项：项目级独立显示、结构化字段、项目级预览、未配置空态和禁止前端默认内容冒充后端配置。

## BDD Scenarios

- BDD: 同工序不同检验项目独立显示首检巡检 -> Given 后端返回同属“组装Ⅰ”的“外观”和“撤压”两个检验项目且首检数量、巡检比例不同, When QA 配置页加载该规程, Then 每个项目行必须直接显示自己的启用状态、首检固定数量和巡检比例，不得使用工序级统一值或重新解析抽样文字代替结构化字段。
- BDD: 未配置项目保持正式空态 -> Given 所选 DCC 项目代码没有后端 QA 草稿或发布版, When QA 配置页完成加载, Then 页面必须明确显示“未配置”，检验项目和任务预览为空，不得显示前端默认首检或巡检任务。
- BDD: QA 页预览按项目展开 -> Given QA 草稿包含多个工序和多个检验项目, When QA 页面生成预览, Then 每一行必须包含 QA 工序、检验项目、检验类型和该项目自己的数量或比例。
- BDD: 末检适用性保持项目级统一 -> Given 当前 DCC 项目切换末检适用性, When QA 页面显示和保存检验项目, Then 末检适用性仍按项目统一，首检和巡检仍按检验项目独立。

## Command Intent

- 只读检查当前 QA 页面、正式 API 和后端项目级字段，定位不满足项。
- 下一步新增聚焦静态合同并执行 RED，不运行写入型业务操作。

## Experience Gate

- 已读取 `docs/experience-index.md` 对应 QA 抽样门禁和 `docs/backend-development.md` 中匹配章节。
- 旧门禁的“同一工序内部唯一值”已被本轮用户明确要求替代为“同一工序下每个检验项目独立值”；比例单位和原文独立持久化规则继续有效。

## Milestone Updates

- M1 completed：已确认后端具备检验项目级 `firstInspectionQuantity` 与 `patrolInspectionRatio`，并用聚焦合同冻结页面缺失结构化规则层、项目级控件、未配置空态和项目级预览。
- M2 completed：QA 项目行已直接映射后端结构化首检/巡检字段，新增项目级开关与数量/比例控件，保存载荷不再解析抽样文字。
- M3 completed：未配置状态明确显示，空配置不生成默认预览；QA 页预览按工序、检验项目和检验类型展开。
- M4 completed：聚焦合同、四项相邻 QA 合同、类型检查、格式检查、真实页面只读验证和两项技能证据校验均已完成。
- M5 completed：已把“首检/巡检按检验项目独立，末检按项目统一”沉淀到既有长期经验文档和索引；task-closeout-cleanup preview 无阻塞，apply 仅删除本任务 Playwright 临时产物和中间 evidence 文件，保留三份正式任务记录。

## Verification Evidence

- BASELINE: `node tests\e2e\qa-regulation-applicable-types-derived-static.spec.js` -> FAIL，既有合同仍定位已删除的旧保存函数，不能作为本任务 RED；本任务新增独立聚焦合同。
- RED: `node tests\e2e\qa-regulation-item-inspection-display-static.spec.cjs` -> FAIL，expected reason：缺少 `qaRegulationItemInspection.ts`，证明当前没有以后端结构化字段为来源的检验项目级规则层。
- GREEN: `node tests\e2e\qa-regulation-item-inspection-display-static.spec.cjs` -> PASS。
- GREEN: `node tests\e2e\qa-regulation-applicable-types-derived-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\qa-regulation-applicable-types-default-visible-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\qa-regulation-final-inspection-switch-static.spec.cjs` -> PASS。
- GREEN: `node tests\e2e\qa-regulation-dcc-backend-persistence-static.spec.cjs` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。重启前已完整结束；重启中断的是之后发起的重复确认，不改变首次通过证据。
- RETRY GREEN: `pnpm ts:check` -> PASS。2026-08-18 重试执行，聚焦合同、三项相邻合同和类型检查均通过。
- GREEN: `pnpm exec prettier --check <task-owned frontend files>` -> PASS。
- GREEN: `git diff --check` -> PASS，仅报告仓库既有 CRLF 转换警告，无空白错误。
- GREEN: bug regression 与 frontend feature evidence validator -> PASS。
- Playwright: 真实登录 QA 页面并进入“检验项目”页签，确认“首检”“巡检”“原抽样方案”列和“当前 DCC 项目未配置 QA 规程”空态；未发起保存或发布请求。
- RETRY GREEN: `QA_REGULATION_E2E_BASE_URL=http://127.0.0.1:8084 QA_REGULATION_E2E_DCC_PROJECT_CODE_ID=147 node tests\e2e\qa-regulation-item-inspection-display-real.e2e.cjs` -> PASS。使用独立 worktree 8084/48084 成对运行态，真实页面显示“组装Ⅰ / 外观”首检 13 件、“组装Ⅰ / 撤压”首检 5 件；未配置 DCC 项目显示空态；`writeRequests=[]`、`badTargetResponses=[]`、`consoleErrors=[]`、`pageErrors=[]`。
- Experience: 已更新 `docs/backend-development.md` 对应 QA 门禁和 `docs/experience-index.md` 索引，删除“同工序唯一”旧口径。
- CLOSEOUT: `task_closeout.py --mode preview` -> PASS，删除范围仅限本任务中间产物。
- CLOSEOUT: `task_closeout.py --mode apply` -> PASS，保留 `task.md`、`execution-log.md`、`verification-report.md`；本轮在独立 worktree 内继续验证，未执行 worktree 合并、移除或 Git 提交。

## Blockers

- 无。此前本地后端读取所选 DCC 项目超时导致已有配置实值未能完成页面验证；2026-08-18 使用独立 worktree 标准分支前端脚本重启 8084 并指向 48084 后，真实页面只读验证已通过。

## 2026-08-18 int_main Integration After Restart

- SAFETY: `codex/qa-item-inspection-display-20260818` 与 `int_main` 已分叉，直接合并会影响大量非本任务文件并包含删除风险；本轮只集成该提交中 task-owned QA 配置页、QA 静态/真实验证脚本和任务记录。
- GREEN: `node tests\e2e\qa-regulation-item-inspection-display-static.spec.cjs` -> PASS。
- GREEN: `node tests\e2e\qa-regulation-final-inspection-switch-static.spec.cjs` -> PASS。
- GREEN: `node tests\e2e\qa-regulation-applicable-types-derived-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\qa-regulation-applicable-types-default-visible-static.spec.js` -> PASS。
- GREEN: `git diff --cached --check` -> PASS。
- GREEN: `scripts\preflight\branch-runtime-port-guard.ps1` -> PASS，`int_main` 端口仍为前端 8081、后端 48081。
- NOTE: 首次从仓库根目录执行 `qa-regulation-applicable-types-default-visible-static.spec.js` 因脚本相对路径依赖前端目录而失败；已在 `IntRuoyiFronted` 目录重跑并通过。
