# Verification Report V2

## Scope

本报告覆盖“活跃订单放行资料生成”V2 文档修订的结构校验、UTF-8 校验、关键词覆盖和关键业务约束校验。本轮只修改设计文档，不修改生产代码、不修改数据库、不启动服务、不执行真实写入型 E2E。

## Commands

- `python -X utf8 C:\Users\BJB110\.codex\skills\product-requirements-docs\scripts\validate_product_requirements.py --root E:\IntRuoyi\doc\tasks\20260808-active-order-release-dossier-design` -> PASS，输出 `Product requirements docs validation passed.`
- `python -X utf8 C:\Users\BJB110\.codex\skills\system-design-docs\scripts\validate_system_design.py --root E:\IntRuoyi\doc\tasks\20260808-active-order-release-dossier-design` -> PASS，输出 `System design docs validation passed.`
- `python -X utf8 C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root E:\IntRuoyi\doc\tasks\20260808-active-order-release-dossier-design` -> PASS，输出 `BDD/TDD acceptance plan validation passed.`
- `python -X utf8 -c "from pathlib import Path; root=Path(r'E:\IntRuoyi\doc\tasks\20260808-active-order-release-dossier-design'); files=sorted(root.rglob('*.md')); [p.read_text(encoding='utf-8') for p in files]; print('UTF-8 read passed:', len(files), 'markdown files')"` -> PASS，输出 `UTF-8 read passed: 15 markdown files`。
- `rg -n "真实历史数据|历史表单|测试数据|QA 文件|批记录表单|签名时间|不直接改活跃订单进度|formBindings|默认 MAIN|生产负责人放行" doc\tasks\20260808-active-order-release-dossier-design` -> PASS，关键 V2 约束均可检索。
- `git diff --check -- doc/tasks/20260808-active-order-release-dossier-design` -> PASS，无空白错误。

## Document Inventory

- `docs/product/prd.md`
- `docs/product/user-flows.md`
- `docs/product/acceptance-criteria.md`
- `docs/system/frontend-design.md`
- `docs/system/backend-api-design.md`
- `docs/system/data-model.md`
- `docs/system/config-security-deployment.md`
- `docs/acceptance/bdd-scenarios.md`
- `docs/acceptance/tdd-plan.md`
- `docs/acceptance/e2e-plan.md`
- `docs/acceptance/test-data.md`
- `development-plan.md`

## Business Constraint Review

- V2 目标：第一版以跑通“真实历史数据 -> 申请放行 -> 生产负责人放行”为主，限制能不增加就不增加。
- 双 100% 来源：文档明确禁止直接改活跃订单进度，必须由生产组长/PQC 组长历史数据和历史表单支撑。
- 测试数据：`test-data.md` 已明确如何制造产品、路线、QA 文件、批记录表单、设备、生产历史、PQC 历史、活跃订单和签名数据。
- 批记录映射：文档要求生产历史数据符合批记录表单，填写人、审核人和签名时间来自真实提交/确认。
- 过程检验映射：文档要求 PQC 历史数据符合产品 QA 文件，填写人、审核人和签名时间来自一线 PQC 与 PQC 组长复核。
- 损耗单映射：文档要求损耗数据来自生产损耗历史，数量、原因、工序、产品和批号可追溯。
- 放行职责：生产组长申请只生成资料并推送待办，不直接放行；生产负责人负责最终签名放行。
- blocker 规则：缺 QA 文件、批记录表单、设备参数、历史表单、签名、损耗映射或负责人时必须阻塞，不生成假资料。

## Remaining Product Decisions

- 过程检验单和损耗单的正式承载类型仍需以当前系统实现确认。
- 正式损耗单字段映射仍需对照真实模板逐字段确认。
- 生产负责人来源优先复用 `RELEASE_APPROVE`，但需确认目标路线配置完整。
- 生产组长申请动作是否需要新增电子签名仍待业务决定；V2 第一版默认不新增，除非业务明确要求。

## Cleanup

- `task-closeout-cleanup --mode preview` -> PASS，无删除项、无阻塞、无警告。
- `task-closeout-cleanup --mode apply` -> PASS，无删除项、无阻塞、无警告。

## V2 Optimization Review

- 过度设计收敛：后端首版以 `MesTeamLeaderActiveOrderReleaseApplicationService` 为唯一申请编排入口，不预先拆多个独立 service。
- 数据模型收敛：第一版只新增申请记录表；字段映射快照表保持后续可选，不作为首版前置。
- 现有系统复用：负责人来源默认复用 `RELEASE_APPROVE`，批次执行复用 eDHR，字段来源优先复用现有字段审计、PQC 汇集和操作审计。
- 双 100% 校验收敛：首版只核验正式来源可追溯，不重建完整进度计算体系。
- 测试数据补强：新增 `Executable Fixture Contract`，允许页面优先或正式领域服务 fixture，但必须在生产组长/PQC 组长历史列表和历史表单中只读验证可见。

## Optimization Cleanup

- `task-closeout-cleanup --mode preview` -> PASS，无删除项、无阻塞、无警告。
- `task-closeout-cleanup --mode apply` -> PASS，无删除项、无阻塞、无警告。

## Result

PASS。V2 文档包已通过结构、UTF-8、关键词覆盖和空白检查，可作为下一步实现与真实测试数据准备的输入。

## V3 Multi-Agent Plan Review

### Scope

本轮 V3 追加覆盖“6 个子 agent 可独立开发和验证，最后由 1 个主 agent 集成方案和集成测试”的开发组织设计。本轮仍只修改设计文档，不修改生产代码、不修改数据库、不启动服务、不执行真实写入型 E2E。

### Added Document

- `v3-agent-development-plan.md`

### V3 Review Findings

- 子 agent 拆分：A1 前端入口、A2 后端申请编排、A3 生产到批记录、A4 PQC 到过程检验、A5 损耗与完成性、A6 测试数据与真实 E2E。
- 独立性：每个子 agent 均包含开发任务、非范围、独立验收任务和验收通过标准。
- 主 agent：文档明确主 agent 负责 M0 契约冻结、范围冲突复核、跨 agent 运行时契约复验、系统级真实数据测试和最终完成门禁。
- 集成方案：文档包含 8 步集成顺序和 IT-V3-01 至 IT-V3-12 集成测试用例。
- 测试数据：文档明确 fixture 必须制造生产组长/PQC 组长报工历史、历史表单、QA 文件、批记录绑定、设备参数、签名和活跃订单自然双 100%。
- 复用现有系统：文档要求第一版仍以一个申请编排入口为核心，复用 eDHR 批次执行、正式工序批记录绑定、PQC 汇集、`RELEASE_APPROVE` 和现有放行待办。
- 过度设计控制：V3 拆分是开发协作拆分，不是运行时拆 6 套业务主流程。

### V3 Commands

- `python -X utf8 -c "... required=['A1 子 Agent', ...] ..."` -> PASS，输出 `V3 keyword coverage PASS`。
- `python -X utf8 -c "... read_text(encoding='utf-8') ..."` -> PASS，输出 `UTF-8 read passed: 16 markdown files`。
- `python -X utf8 C:\Users\BJB110\.codex\skills\product-requirements-docs\scripts\validate_product_requirements.py --root E:\IntRuoyi\doc\tasks\20260808-active-order-release-dossier-design` -> PASS。
- `python -X utf8 C:\Users\BJB110\.codex\skills\system-design-docs\scripts\validate_system_design.py --root E:\IntRuoyi\doc\tasks\20260808-active-order-release-dossier-design` -> PASS。
- `python -X utf8 C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root E:\IntRuoyi\doc\tasks\20260808-active-order-release-dossier-design` -> PASS。
- `git diff --check -- doc/tasks/20260808-active-order-release-dossier-design` -> PASS。
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260808-active-order-release-dossier-design --mode preview` -> PASS，无删除项、无阻塞、无警告。
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260808-active-order-release-dossier-design --mode apply` -> PASS，无删除项、无阻塞、无警告。

### V3 Result

PASS。V3 已把实现拆成 6 个可独立开发和验证的子 agent，并补齐主 agent 集成方案、系统级集成测试、真实 fixture 造数要求和最终完成门禁。根据该文档开发，可以达到“真实历史数据 -> 生成正式批记录/过程检验单/损耗单 -> 推送生产负责人放行”的目标；实际编码阶段仍必须按 V3 的 M0 契约冻结、子 agent 独立验收和主 agent 真实 E2E 门禁执行。

## V3 Independent Review Against Current Code

### Review Scope

本轮按用户要求复核 V3 是否存在潜在问题、是否过度设计、是否复用现有系统、测试数据是否可执行、6 个 agent 拆分是否合理、接口和集成设计是否正确，以及按文档开发能否达到“真实历史数据 -> 正式批记录/过程检验单/损耗单 -> 生产负责人放行”的目标。

### Evidence Checked

- V3 文档：`v3-agent-development-plan.md`。
- V2 后端接口设计：`docs/system/backend-api-design.md`。
- V2 测试数据计划：`docs/acceptance/test-data.md`。
- V2 数据模型：`docs/system/data-model.md`。
- 当前后端接口：`MesProcessPoolTeamLeaderController.applyActiveOrderRelease(...)`。
- 当前后端编排：`MesTeamLeaderActiveOrderReleaseApplicationServiceImpl.apply(...)`。
- 当前批记录回填能力：`MesTeamLeaderBatchRecordBackfillServiceImpl.backfillCompletedProcess(...)`。
- 当前前端入口/API：`TeamLeaderWorkbenchPage.vue`、`teamLeader.ts`。
- 当前静态合同：`mes-team-leader-active-order-release-application-static.spec.cjs`。

### Verdict

CONDITIONAL PASS。V3 的业务方向、6-agent 协作框架、测试数据原则和主 agent 集成门禁可以支撑目标；但 V3 不能直接作为并行开发冻结版。正式开发前必须先做 M0 契约修正，否则 6 个 agent 容易按不同字段、重复实现已有能力，最终集成无法稳定到放行。

### Blocking Corrections Before Development

- 接口请求字段不一致：V3 写的是可选 `clientRequestId`，当前后端/前端/V2 设计均使用必填 `idempotencyKey`。首版建议沿用 `idempotencyKey`，除非同步修改后端 VO、前端类型、静态合同和测试。
- 响应结构不一致：V3 写 `sourceSnapshotHash` 顶层字段和 `generatedDocuments[]`，当前实现是 `dossierSummary.sourceSnapshotHash`，且没有 `generatedDocuments[]`。首版应冻结当前结构，或在同一个 M0 变更中统一后端、前端和测试。
- Blocker DTO 不一致：V3 写 `code/message/actionHint/fieldCode/cellKey`，当前 VO 是 `blockerType/reason/suggestion/objectCode`，字段级定位不足。首版可先沿用当前 DTO 并补字段级可选字段，不能让不同 agent 各自定义 blocker。
- A1/A2/A3 不是从零开发：当前已存在申请按钮、前端 API wrapper、申请接口、申请记录、eDHR 批次创建、`submitForApproval` 和批记录回填服务。V3 应改成“复用 + gap hardening”，避免重复造入口和申请编排。
- 当前 apply 编排尚未达到 V3 成功路径：现有逻辑只做来源预检、打开批次、release precheck、提交负责人待办；未在 apply 中明确调用批记录、过程检验单、损耗单真实写入端口，且 `signatureEvidenceCount` 当前为 0。
- 损耗单成功路径仍是硬缺口：当前代码在发现 `LOSS_REPORT` 绑定时返回 `LOSS_REPORT_SOURCE_REQUIRED`，说明正式损耗单来源映射/写入尚未完成。按 V3 成功路径验收前必须补 A5。
- 过程检验单仍需写入端口：当前代码能检查 `PROCESS_INSPECTION` 绑定和 PQC 汇集明细，但还未证明把汇集明细按 QA 约束写入正式过程检验单。
- 集成顺序需要收紧：应为“来源校验 -> 创建/复用批次执行 -> A3/A4/A5 写正式资料 -> 完成性检查 -> release precheck -> `submitForApproval`”。当前代码先 `precheck` 再提交，若 precheck 依赖资料完成性，会在未写资料时失败或形成空资料待办。

### Overdesign Review

- 不属于过度设计：禁止直接改双 100%、禁止伪造签名、禁止用 `formBindings`/默认 `MAIN` 替代批记录、要求真实 fixture 和页面路径，这些是 MES/eDHR 数据完整性门禁，不是多余限制。
- 可收敛项：`PRECHECKING/GENERATING` 可作为运行时瞬态状态，不必首版强制持久化；`generatedDocuments[]` 可延后到只读复核接口或 `dossierSummary` 能满足前端展示后再加；字段级 `cellKey` 可先作为可选增强字段，不阻塞首版按钮到放行。
- 不建议新增字段映射快照表作为首版前置；继续采用 V2 的原则，优先复用批记录字段审计、PQC 汇集明细、损耗来源记录、操作审计和申请摘要。

### Existing System Reuse Review

- 已复用方向正确：V3 指定一个申请入口，不拆 6 套运行时主流程；放行待办复用 `RELEASE_APPROVE`；批次执行复用 eDHR；批记录来源要求使用逐工序正式绑定。
- 需要改写 agent 任务表述：A1 应是“前端入口硬化”，A2 应是“申请编排硬化与生成器集成”，A3 应是“复用 `MesTeamLeaderBatchRecordBackfillServiceImpl` 并接入 apply”，A4/A5 才是当前主要新开发缺口。
- 主 agent 集成必须检查 Spring 注入、构造器参数、VO/TS 类型、权限码和状态枚举；不能只检查文档关键词或子 agent 单测。

### Test Data Review

- 测试数据设计总体可执行且方向正确：不能直接造 100%，必须制造一线生产提交、生产组长确认、一线 PQC 提交、PQC 组长复核、PQC 汇集、损耗明细和对应活跃订单，再在历史列表/历史表单中验证可见。
- 需要补充可执行入口清单：A6 应在 M0 manifest 中固定使用哪些页面/API/领域服务造产品、路线、QA 文件、批记录表单、设备、人员签名、生产历史、PQC 历史和损耗明细。
- Fixture manifest 需要包含签名证据和值 hash：至少记录提交人、审核人、签名时间、来源表单 ID、来源事件 ID、PQC aggregate detail IDs、loss source IDs、批记录/过程检验/损耗目标对象 ID。
- 负向数据覆盖充分，但成功路径必须先做页面只读断言，再点击生产组长申请，不能用 API-only 直接证明资料生成。

### Agent Split Review

- 6 个 agent 拆分基本合理：A1 前端、A2 编排、A3 生产批记录、A4 PQC 过程检验、A5 损耗与完成性、A6 测试数据/E2E，边界符合业务链路。
- A5 组合“损耗单 + 完成性检查”可以接受，因为完成性是放行待办前最后硬门禁；但 A5 不应创建负责人待办，待办仍由 A2 在 A5 通过后调用现有放行服务。
- 最大风险不是拆分本身，而是共享契约未冻结导致 agent 间字段命名、状态枚举、生成器端口、source hash 算法和 blocker 结构不一致。

### Integration Review

- 集成目标合理：主 agent 需要先验 fixture 来源可见性，再跑生产组长页面申请，再跑生产负责人放行/驳回，最后只读复核三类正式资料和来源追溯。
- 集成门禁需要增加一条：`apply` 成功前必须能证明 A3/A4/A5 真实 writer 已执行，不能只有数量摘要或 release task。
- 集成门禁需要增加一条：成功 fixture 的 `signatureEvidenceCount` 必须大于 0，并且填写人/审核人/签名时间与 manifest 一致。
- 集成门禁需要增加一条：前后端 VO/TS 类型契约静态测试必须覆盖 `idempotencyKey`、blocker 字段、`dossierSummary.sourceSnapshotHash` 或最终选定的响应结构。

### Final Assessment

- 如果按当前 V3 原文直接并行开发：风险较高，主要会卡在接口契约不一致、重复开发已有 A1/A2/A3、A4/A5 写入端口缺失、当前 apply 未真实写正式资料、损耗单仍 blocker。
- 如果先按本审查完成 M0 契约修正：V3 可以指导 6 个子 agent 独立开发和验证，并通过主 agent 集成测试达到用户目标。
- 当前代码已经具备部分基础，但尚未实现完整“正式批记录 + 正式过程检验单 + 正式损耗单全部填写完成后推送负责人放行”的成功路径。


## V3 Correction Verification

### Correction Summary

- 接口契约已修正：V3 和后端/前端设计均以 `idempotencyKey` 为首版请求幂等字段，`sourceSnapshotHash` 位于 `dossierSummary.sourceSnapshotHash`，blocker 首版字段为 `blockerType/objectType/objectId/objectCode/reason/suggestion`。
- 过度设计已收敛：`PRECHECKING/GENERATING` 标注为首版可瞬态状态，`generatedDocuments[]` 不作为申请接口首版前置字段，字段级 `fieldCode/cellKey` 仅在可定位时作为可选增强。
- 现有系统复用已补强：A1/A2/A3 均改为复用现有能力并做 gap hardening，A3 明确复用 `MesTeamLeaderBatchRecordBackfillServiceImpl.backfillCompletedProcess(...)`。
- 集成设计已修正：A2 的正式顺序为来源校验、创建/复用批次执行、A3/A4/A5 写资料、完成性检查、release precheck、`submitForApproval`。
- 测试数据已补强：fixture manifest 补充 `batchRecordExecutionIds`、`processInspectionFormIds`、`lossReportFormIds`、`sourceFormIds`、`sourceEventIds`、`sourceValueHashes`、`signatureEvidenceCount`。
- 集成测试已补强：新增 IT-V3-13 接口契约一致性、IT-V3-14 三类 writer 执行证明、IT-V3-15 签名证据数量。

### Verification Commands

- `python -X utf8 C:/Users/BJB110/.codex/skills/product-requirements-docs/scripts/validate_product_requirements.py --root E:/IntRuoyi/doc/tasks/20260808-active-order-release-dossier-design` -> PASS。
- `python -X utf8 C:/Users/BJB110/.codex/skills/system-design-docs/scripts/validate_system_design.py --root E:/IntRuoyi/doc/tasks/20260808-active-order-release-dossier-design` -> PASS。
- `python -X utf8 C:/Users/BJB110/.codex/skills/bdd-tdd-acceptance-planner/scripts/validate_acceptance_plan.py --root E:/IntRuoyi/doc/tasks/20260808-active-order-release-dossier-design` -> PASS。
- UTF-8 read -> PASS，16 个 Markdown 文件。
- Markdown trailing whitespace check -> PASS。
- `git diff --check -- doc/tasks/20260808-active-order-release-dossier-design` -> PASS。

### Result

PASS。V3 修正版已消除上一轮审查指出的主要文档级阻塞：接口字段冲突、A1/A2/A3 重复开发倾向、writer 集成顺序不清、fixture manifest 签名证据不足、三类正式资料 writer 缺少集成验收门禁。按修正版开发，仍需在实现阶段补齐 A4/A5 真实 writer、A2 调用三类 writer 和真实 E2E。


## V4 Final Optimization Verification

### Scope

本轮输出最终版 V4，目标是把 V3 修正版收敛成可直接交给主 agent 和 6 个子 agent 执行的最小开发方案。

### Optimization Result

- V4 保留 6-agent 拆分，但不扩大运行时架构；后端仍以一个申请编排入口为核心。
- V4 明确 A1/A2/A3 复用现有系统，A4/A5 是主要新增 writer 缺口，A6 是成功路径唯一测试数据来源。
- V4 明确 M0 契约冻结，防止并行 agent 再次出现接口字段、DTO、writer 端口和 fixture manifest 漂移。
- V4 明确最终开发顺序：M0 -> A2 RED -> A3 接入 -> A4 -> A5 -> A2 串联 -> A1 对齐 -> A6 fixture/E2E -> 主 agent 集成验收。
- V4 明确最终验收矩阵 G-01 至 G-14，覆盖接口、来源、三类正式资料、签名证据、负责人待办、幂等、负向 blocker、禁止 `formBindings` 替代和禁止 API-only。

### Result

PASS。V4 是当前文档包的最终优化版本，可作为后续实现唯一开发入口。后续若按 V4 开发，必须以真实 writer、真实 fixture 和真实 E2E 为完成门禁；单个 agent 独立通过不能代表功能完成。


### V4 Verification Commands

- `python -X utf8 C:/Users/BJB110/.codex/skills/product-requirements-docs/scripts/validate_product_requirements.py --root E:/IntRuoyi/doc/tasks/20260808-active-order-release-dossier-design` -> PASS。
- `python -X utf8 C:/Users/BJB110/.codex/skills/system-design-docs/scripts/validate_system_design.py --root E:/IntRuoyi/doc/tasks/20260808-active-order-release-dossier-design` -> PASS。
- `python -X utf8 C:/Users/BJB110/.codex/skills/bdd-tdd-acceptance-planner/scripts/validate_acceptance_plan.py --root E:/IntRuoyi/doc/tasks/20260808-active-order-release-dossier-design` -> PASS。
- `node yudao-module-mes/src/test/js/mes-team-leader-active-order-release-application-static.spec.cjs` -> PASS。
- UTF-8 and whitespace scan -> PASS，17 个 Markdown 文件。
- `git diff --check -- doc/tasks/20260808-active-order-release-dossier-design` -> PASS。
- V4 key gate scan -> PASS，覆盖唯一开发入口、M0 契约冻结、三类 writer、签名证据、禁止 API-only 和批记录回填复用。
