# 生产报工提交参数与报工管理列表 BDD/TDD 设计

## Task Goal

按照用户确认后的方案，先完成 BDD + 严格 TDD 设计文档，不改生产代码。设计范围覆盖生产一线报工提交参数完整性、班组长报工管理列表字段拆分、设备参数超限红色提醒但允许提交、损耗数量与损耗原因明细合计一致、以及按当前工序读取班组长配置的损耗原因、设备和设备参数上下限。

## Milestones

- [x] 归一化用户需求，明确参数超限允许提交但数字标红。
- [x] 读取任务、编码、BDD/TDD 技能和 E2E 规划门禁。
- [x] 定位现有报工提交、班组长报工管理、损耗原因、设备参数规则和相关测试证据。
- [x] 编写 BDD 场景、TDD 顺序、E2E 路径和测试数据设计文档。
- [x] 运行结构化验收文档校验。
- [ ] 提交并推送本任务文档。

## Expected Verification

- `python C:\Users\BJB110\.codex\skills\bdd-tdd-acceptance-planner\scripts\validate_acceptance_plan.py --root <temp-root>`：对本任务四份验收设计文档按技能结构校验。
- `python -X utf8 -c "from pathlib import Path; ..."`：UTF-8 读取本任务中文 Markdown 文档。
- 后续实现阶段必须按 `bdd-scenarios.md`、`tdd-plan.md`、`e2e-plan.md` 和 `test-data.md` 中的 RED/GREEN 命令执行，不得用静态合同或 API-only 替代真实 E2E。

## Current Status

ready_for_closeout - BDD/TDD 设计文档已完成并通过结构化校验；最终提交/推送受阻于任务开始前和复核时均存在非本任务 Git 状态。复核时 `int_main` 显示 `ahead 1`，且仍有非本任务 tracked 改动，本任务目录保持未跟踪新增，不能安全混入提交。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；设计要求缺少正式配置、脚本入口、测试账号或测试数据时 fail fast。
- `是否从根因和长期维护角度解决`：是；设计要求从正式提交 payload、运行态配置来源、后端读模型和前端列表展示共同修正，不用展示文案或默认值掩盖数据缺口。
- `是否存在临时补丁或绕过`：否；文档仅设计 RED/GREEN gate，不修改生产逻辑。

## Applicable Gates

- BDD/TDD acceptance planner：每个行为都必须有 Given/When/Then，并映射 RED、GREEN、回归验证。
- 规划型 E2E 前置与业务 RED 分离门禁：脚本入口、测试文件、真实账号、真实数据和命令解析缺失时记录为前置 blocker，不能冒充业务 RED。
- PowerShell / UTF-8 门禁：中文任务文档使用 UTF-8 写入和读取校验。
- 脏工作区门禁：本任务新增文档不得混入既有已暂存/未提交改动。
