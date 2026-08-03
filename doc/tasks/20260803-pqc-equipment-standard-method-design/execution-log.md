# Execution Log: PQC 检验设备、接收标准与检验方法闭环修改文档

## User Intent

用户确认 PQC 填写页需要按“检验项目”补齐检验设备、设备编号、接收标准、检验方法，并要求 PQC 组长与 QA 业务同步修改；本轮要求根据当前系统整理修改文档，按照 TDD + BDD 方式组织，并在整理后进行 review 优化。

## Commands And Evidence

- BDD: PQC item-level equipment and standard closed loop -> Given 当前 PQC 填写、组长审核、QA 审核围绕同一检验任务流转, When 每个检验项目配置不同设备、编号、接收标准和检验方法, Then 填写、审核、放行和历史追溯必须展示并校验同一份项目级快照。
- RED: 文档阶段 -> FAIL, 当前尚未形成覆盖填写端、组长端、QA 端、历史追溯和测试计划的修改文档。
- GREEN: 文档阶段 -> PASS, 已输出 `pqc-equipment-standard-method-modification.md`、`pqc-equipment-standard-method-bdd-tdd-plan.md` 和 `verification-report.md`，覆盖 PQC 填写、PQC 组长、QA、历史追溯、异常阻塞、权限、数据快照和 BDD/TDD 计划。
- BDD: Worktree document verification and merge -> Given 用户要求在 `20260803_pqcc` worktree 中进行文档开发验证, When 文档结构验证通过并提交分支, Then 将验证通过的文档改动融合进 `int_main`，且不混入主工作区已有无关脏改动。
- 命令意图：读取 `product-requirements-docs`、`bdd-tdd-acceptance-planner`、`independent-verification-gate` 技能说明和结构模板，用于约束文档结构、BDD/TDD 计划和独立复审。
- 命令意图：读取 PQC 前端入口、填写组件、payload 白名单、组长工作台、前端 API 类型、后端提交 VO/Command、QA 规程 DO、PQC 逐件明细 DO、PQC 提交服务和现有验收文档，作为当前系统证据。
- 命令意图：运行文档结构扫描 `rg -n "^## ...|^### BDD:|^\\| T0|RED:|GREEN:|PASS for documentation" doc/tasks/20260803-pqc-equipment-standard-method-design`，确认主文档、BDD/TDD 计划和复审报告包含必要结构与标记。
- 命令意图：运行 `git -C E:\IntRuoyi status --short --branch -- doc/tasks/20260803-pqc-equipment-standard-method-design`，确认本任务目录为新增未跟踪文件，当前分支 `int_main...origin/int_main [ahead 4]`。
- 命令意图：读取 `docs/worktree-restrictions.md` 和 `docs/powershell-memory.md`，确认 worktree 创建、路径、Git 编排和合并门禁。
- 命令意图：运行 `git -C E:\IntRuoyi worktree list`，确认 `D:\IntRuoyiWorktree\20260803_pqcc` 尚不存在。
- 命令意图：解析目标路径 `D:\IntRuoyiWorktree\20260803_pqcc`，确认其为 `D:\IntRuoyiWorktree\` 子路径。
- 命令意图：运行 `git -C E:\IntRuoyi worktree add D:\IntRuoyiWorktree\20260803_pqcc -b codex/20260803_pqcc int_main`，创建任务 worktree 和分支。
- 命令意图：将任务文档从主工作区复制到 `D:\IntRuoyiWorktree\20260803_pqcc\doc\tasks\20260803-pqc-equipment-standard-method-design`，后续仅在该 worktree 继续验证和提交。

## Milestone Updates

- 2026-08-03: 建立任务目录与初始任务文档。
- 2026-08-03: 完成当前系统证据盘点，确认 PQC 填写端已有规程项目和方法/标准文本，但缺项目级检验设备、设备编号、数值上下限、按钮弹窗和审核端结构化展示。
- 2026-08-03: 输出主修改文档，明确第一版范围、非目标、业务规则、数据/API/前端/审核/QA/trace 修改方案和产品阻塞。
- 2026-08-03: 输出 BDD/TDD 计划，覆盖 12 个 BDD 场景、10 条 TDD 序列、RED/GREEN 命令、真实 E2E 前置和测试数据要求。
- 2026-08-03: 完成独立 review，修正为“PQC 检验项目级事实闭环”口径，并把设备主数据来源、上下限 schema、组长结构化读模型和 QA 页面权限列为实施阻塞。
- 2026-08-03: 创建 `D:\IntRuoyiWorktree\20260803_pqcc` worktree，分支为 `codex/20260803_pqcc`，并迁入本任务文档准备验证。

## Verification Evidence

- PASS: `pqc-equipment-standard-method-modification.md` 覆盖当前证据、缺口分析、第一版范围、非目标、功能需求、业务规则、状态流转、边界场景、验收标准和产品阻塞。
- PASS: `pqc-equipment-standard-method-bdd-tdd-plan.md` 覆盖 Feature/Failure/Boundary BDD 场景、TDD 表、RED/GREEN 命令、测试数据、清理方式和阻塞条件。
- PASS: `verification-report.md` 记录 requirement-to-artifact checklist、review findings、已优化项和剩余实施阻塞。
- PASS: 文档结构扫描命中主文档必要章节、BDD 标记、TDD `T01`-`T09`、RED/GREEN 模板和 review PASS 结论。
- PENDING: worktree 内文档结构验证、`git diff --check`、提交和融合进 `int_main`。

## Blockers

- Implementation Blocker: 当前系统证据尚未确认检验设备与设备编号的唯一正式主数据来源，后续实现前必须确认。
- Implementation Blocker: 当前 QA 规程项目 schema 未发现数值上下限、单位和区间规则字段，后续实现前必须补正式 schema 或确认已有来源。
- Implementation Blocker: PQC 组长详情需要结构化项目明细读模型，不能继续依赖 raw payload 固定四项。
- Main Worktree Risk: 主工作区 `E:\IntRuoyi` 有大量既有无关脏改动且 `int_main` 领先 `origin` 4 个提交；本任务必须通过 `D:\IntRuoyiWorktree\20260803_pqcc` 隔离提交，融合时不得混入这些无关改动。
