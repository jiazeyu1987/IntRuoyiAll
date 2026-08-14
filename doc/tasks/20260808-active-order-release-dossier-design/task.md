# 活跃订单放行资料自动生成设计任务

## Task Goal

为“生产组长活跃订单达到生产进度 100% 且检验进度 100% 后，手动申请生成正式放行资料并推送生产负责人放行”的能力设计完整 PRD、开发文档和测试计划。

## Milestones

- [x] 创建任务目录并识别本任务为文档设计任务。
- [x] 输出产品需求文档、用户流程和验收标准。
- [x] 输出前端设计、后端 API 设计、数据模型和配置安全部署设计。
- [x] 输出 BDD/TDD、E2E 和测试数据计划。
- [x] 执行结构、编码和关键业务约束校验。
- [x] 输出 V3 多 agent 开发方案、6 个子 agent 独立验收任务和主 agent 集成测试方案。
- [x] 执行 V3 关键词、结构、UTF-8、validator 和空白校验。
- [x] 基于当前代码实现证据独立审查 V3 的可实现性、过度设计、现有系统复用、测试数据、agent 拆分、接口契约和集成方案。
- [x] 修正 V3 文档包：统一接口契约、blocker DTO、现有系统复用边界、A1/A2/A3 分工、A3/A4/A5 writer 集成顺序、fixture manifest 和测试门禁。
- [x] 输出最终版 V4 开发方案，作为后续实现的唯一开发入口。

## Expected Verification

- 任务目录包含 `task.md`、`execution-log.md`、`verification-report.md`。
- PRD 文档覆盖范围、用户、业务规则、状态流转、异常和阻塞项。
- 系统设计覆盖前端入口、后端编排、接口契约、数据一致性、权限、事务、幂等、审计和观测。
- 测试计划覆盖 BDD 场景、严格 TDD 顺序、真实 E2E 路径、测试数据和失败门禁。
- UTF-8 方式读取所有生成的中文 Markdown 文件成功。

## Current Status

completed

V2 优化、结构 validator、UTF-8、关键词覆盖、空白检查和 task-closeout-cleanup preview/apply 均已通过。2026-08-08 追加 V3：拆分为 6 个可独立开发和验证的子 agent，并由 1 个主 agent 执行集成验收与系统级测试。V3 独立审查结论为有条件通过；最终版 V4 已输出为后续实现唯一入口。V4 保留 6-agent 协作，但进一步压缩为最小可交付：M0 契约冻结、A1/A2/A3 复用现有能力、A4/A5 补真实 writer、A6 负责真实 fixture 和 E2E，最终只按三类 writer、签名证据、负责人待办和真实页面放行判断完成。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。本设计要求正式数据缺失时阻塞，禁止用默认值、旧字段、表单槽位或模拟数据冒充正式资料。
- `是否从根因和长期维护角度解决`：是。设计目标是复用现有 eDHR 批次执行、工序批记录、PQC 汇总、放行审批任务等正式链路，而不是新增平行主流程。
- `是否存在临时补丁或绕过`：否。

## V2 Revision Focus

- 第一版目标改为“能基于真实历史数据走到生产负责人放行”，不额外增加非必要限制。
- 双 100% 不允许凭空造字段，必须由生产组长报工历史、生产历史表单、PQC 检验历史和 PQC 历史表单支撑。
- 测试数据必须符合对应产品、路线、工序、QA 文件、批记录表单、设备和参数约束。
- 表单填写人、审核人、签名时间必须来自一线生产/PQC 提交、生产组长/PQC 组长审核和签名动作。
- 开发方案必须明确活跃订单、历史数据、批记录、过程检验单、损耗单、批次执行和放行待办之间的映射关系。

## Applicable Gates

### PowerShell 与 UTF-8 文档写入门禁

- Trigger: 写入中文 Markdown、任务文档或规则文档。
- Preflight check: 使用 `apply_patch` 或显式 UTF-8 API 写入；读取时使用 `Get-Content -Encoding utf8`、`python -X utf8`、Node UTF-8 API 或 `rg`。
- Blocker: 出现乱码、编码不明、命令退出码失败或需要隐式降级时停止。
- Verification: 写入后使用 `python -X utf8` 读取生成文档。
- Forbidden action: 禁止使用默认 `Set-Content`、`Add-Content`、`Out-File`、`>`、`>>` 写入中文。

### 规划型 E2E 与业务 RED 分离门禁

- Trigger: 仅编写 BDD/TDD/E2E 计划而非立刻实现生产行为。
- Preflight check: 文档必须明确真实用户路径、业务数据前置、API 验证边界和 RED 失败原因。
- Blocker: 缺少真实入口、测试账号、测试租户、正式样本数据或关键业务标记时，不能把结构校验当成业务通过。
- Verification: 结构校验只证明文档完整；后续实现仍需按测试计划执行 RED/GREEN 和真实 E2E。
- Forbidden action: 禁止用 API-only、静态合同或 mock 数据冒充真实 E2E。

### MES 批记录与表单槽位隔离门禁

- Trigger: 设计“批记录/批记录表单/过程检验单/损耗单”生成与回填。
- Preflight check: 批记录表单必须来自工序设置中的逐工序正式批记录绑定；过程检验和损耗来源必须独立定义，不得用 `formBindings` 自动替代。
- Blocker: 缺少正式工序批记录绑定、PQC 汇总明细、损耗来源映射、负责人来源或签字证据时，后端申请必须失败并返回可处理原因。
- Verification: 测试分别覆盖工序开始配置、逐工序批记录表单、表单槽位 `formBindings` 三条链路。
- Forbidden action: 禁止用默认 `MAIN` 槽位、特殊节点上传人、旧字段或空值推断正式批记录。

## Cleanup Keep

- doc/tasks/20260808-active-order-release-dossier-design/development-plan.md
- doc/tasks/20260808-active-order-release-dossier-design/docs/product/prd.md
- doc/tasks/20260808-active-order-release-dossier-design/docs/product/user-flows.md
- doc/tasks/20260808-active-order-release-dossier-design/docs/product/acceptance-criteria.md
- doc/tasks/20260808-active-order-release-dossier-design/docs/system/frontend-design.md
- doc/tasks/20260808-active-order-release-dossier-design/docs/system/backend-api-design.md
- doc/tasks/20260808-active-order-release-dossier-design/docs/system/data-model.md
- doc/tasks/20260808-active-order-release-dossier-design/docs/system/config-security-deployment.md
- doc/tasks/20260808-active-order-release-dossier-design/docs/acceptance/bdd-scenarios.md
- doc/tasks/20260808-active-order-release-dossier-design/docs/acceptance/tdd-plan.md
- doc/tasks/20260808-active-order-release-dossier-design/docs/acceptance/e2e-plan.md
- doc/tasks/20260808-active-order-release-dossier-design/docs/acceptance/test-data.md
- doc/tasks/20260808-active-order-release-dossier-design/v3-agent-development-plan.md
- doc/tasks/20260808-active-order-release-dossier-design/v4-final-agent-development-plan.md


## V2 Verification Evidence

- Product requirements validator -> PASS。
- System design validator -> PASS。
- BDD/TDD acceptance plan validator -> PASS。
- UTF-8 read -> PASS，15 个 Markdown 文件。
- V2 关键词覆盖 -> PASS，覆盖真实历史数据、历史表单、测试数据、QA 文件、批记录表单、签名时间、禁止直接改进度、禁止 `formBindings` / 默认 `MAIN`。
- `git diff --check -- doc/tasks/20260808-active-order-release-dossier-design` -> PASS。

- Cleanup preview/apply -> PASS，无删除项、无阻塞、无警告。


## V2 Optimization Focus

- 收敛后端服务拆分：第一版以一个申请编排服务为主，内部方法/小组件承载来源校验、映射和写入，稳定后再拆服务。
- 收敛数据模型：第一版只新增申请记录表；字段映射快照表保持可选，不作为首版前置。
- 收敛双 100% 校验：第一版验证进度背后的正式来源记录存在且可追溯，不重建完整进度计算体系。
- 补齐测试数据 fixture 合同：明确可用页面路径或正式领域服务 fixture 造数，并规定必须在生产组长/PQC 组长历史列表和历史表单中验证可见。


## V2 Optimization Verification Evidence

- Product requirements validator -> PASS。
- System design validator -> PASS。
- BDD/TDD acceptance plan validator -> PASS。
- UTF-8 read -> PASS，15 个 Markdown 文件。
- 优化关键词覆盖 -> PASS，覆盖 M0 实现前置收敛、第一版降复杂度边界、Executable Fixture Contract、正式领域服务 fixture、不重建完整进度计算体系、第一版只新增申请记录表、字段映射快照表不作为首版前置。
- `git diff --check -- doc/tasks/20260808-active-order-release-dossier-design` -> PASS。

- Optimization cleanup preview/apply -> PASS，无删除项、无阻塞、无警告。

## V3 Revision Focus

- 将实现拆成 6 个子 agent：前端入口、后端申请编排、生产到批记录、PQC 到过程检验、损耗与完成性、测试数据与真实 E2E。
- 每个子 agent 必须有独立开发范围、独立验收任务、输入/输出契约、验收证据和非范围边界。
- 主 agent 不直接代替子 agent 开发，负责冻结共享契约、检查范围冲突、集成 6 个结果、执行系统级真实数据测试和最终放行门禁。
- V3 仍保持 V2 原则：第一版以功能跑通为主，禁止直接改双 100%、禁止用 `formBindings` / 默认 `MAIN` 替代正式批记录、禁止制造签名或默认成功。

## V3 Verification Evidence

- V3 keyword coverage -> PASS，覆盖 A1-A6、主 Agent 集成方案、集成测试用例、测试数据制造设计、Fixture、真实 E2E、`formBindings` 和默认 MAIN。
- Product requirements validator -> PASS。
- System design validator -> PASS。
- BDD/TDD acceptance plan validator -> PASS。
- UTF-8 read -> PASS，16 个 Markdown 文件。
- `git diff --check -- doc/tasks/20260808-active-order-release-dossier-design` -> PASS。
- task-closeout-cleanup preview/apply -> PASS，保留 V2/V3 全部交付物，无删除项、无阻塞、无警告。


## V3 Correction Evidence

- 修正接口契约：首版不新增 `clientRequestId`，不要求申请接口必填 `generatedDocuments[]`，`sourceSnapshotHash` 放在 `dossierSummary.sourceSnapshotHash`。
- 修正 agent 分工：A1/A2/A3 改为现有能力复用与缺口硬化，A4/A5 为主要新开发缺口。
- 修正集成门禁：A2 必须在创建负责人待办前证明 A3/A4/A5 writer 已执行，且 `signatureEvidenceCount > 0`。
- 修正测试数据：fixture manifest 补充目标资料对象 ID、来源表单/事件、值 hash、签名主体和签名时间。
- 验证结果：product/system/acceptance validator、UTF-8 读取、Markdown 行尾空白、`git diff --check` 均通过。


## V4 Final Optimization Evidence

- `v4-final-agent-development-plan.md` 已作为最终开发入口。
- V4 明确最小可交付：三类正式资料 writer、完成性检查、`RELEASE_APPROVE` 待办、真实页面申请/放行和只读复核。
- V4 收敛首版非目标：不新增平行审批流、不新增第二套负责人配置、不强制 `generatedDocuments[]`、不持久化瞬态状态。
- V4 明确当前实现缺口：A4 过程检验单 writer、A5 损耗单 writer、A2 三类 writer 编排、A6 真实 fixture/E2E。
