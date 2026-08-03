# PQC 检验设备、接收标准与检验方法闭环修改文档任务

## Task Goal

基于当前 IntRuoyi 系统，整理 PQC 填写、PQC 组长审核、QA 审核三端围绕“检验项目维度”的检验设备、设备编号、接收标准、检验方法的修改文档，并按 BDD + TDD 方式定义验收场景、测试优先级、RED/GREEN 证据模板和 review 优化结论。

## Milestones

- [x] 建立任务记录与经验门禁记录
- [x] 盘点当前系统中 PQC、检验项、检验规程、设备、组长审核、QA 审核相关现状证据
- [x] 输出修改文档，覆盖业务口径、页面交互、数据结构、接口、权限、审核闭环、历史追溯
- [x] 输出 BDD/TDD 验收与测试计划
- [x] 执行文档 review，优化遗漏、歧义、不可测试项和 no-fallback 风险
- [x] 完成结构校验并记录验证报告
- [ ] 在 `D:\IntRuoyiWorktree\20260803_pqcc` 执行文档开发验证
- [ ] 验证成功后提交 `codex/20260803_pqcc`
- [ ] 将验证通过的文档分支融合进 `int_main`

## Expected Verification

- 文档必须基于当前系统文件与已有任务记录证据，不凭空新增业务规则。
- 每个业务变更点必须能映射到至少一个 BDD 场景。
- 每个生产行为必须有对应 TDD RED/GREEN 计划或明确阻塞原因。
- 修改文档必须覆盖 PQC 填写、PQC 组长、QA、历史追溯、异常判定、权限和数据快照。
- Review 后必须记录优化项、已修正项、仍待产品确认的问题。

## Current Status

in_progress

已按用户目标迁入 `D:\IntRuoyiWorktree\20260803_pqcc` 的 `codex/20260803_pqcc` 分支，准备在独立 worktree 内执行文档结构验证、提交并融合进 `int_main`。本任务仍不修改生产代码、不启动运行态、不使用 API-only 冒充真实 E2E。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。本任务仅整理正式修改文档，不设计 fallback、mock 成功或静默降级。
- 是否从根因和长期维护角度解决：是。文档目标是从检验项目维度统一数据源、填写、审核、追溯与验证链路。
- 是否存在临时补丁或绕过：否。

## Experience Gate

- 命中 `docs/experience-index.md` 中“规划型 E2E / BDD/TDD 验收文档”门禁：本文档已补业务标记、BDD 场景、TDD RED/GREEN、真实 E2E 前置阻塞和 no-fallback 约束。
- 命中 `docs/frontend-development.md` 与 `docs/backend-development.md` 的测试优先规则：后续实现必须先写静态合同/JUnit RED，再做 GREEN。
- 命中 `docs/e2e-rules.md` 的脚本入口存在性门禁：真实 E2E 命令仅作为后续计划，若脚本、账号、租户、设备编号或页面入口缺失，必须记录 BLOCKED，不能用 API-only 冒充通过。
- 命中 `docs/worktree-restrictions.md`：目标 worktree 位于 `D:\IntRuoyiWorktree\20260803_pqcc`，属于允许根目录；本轮不启动前后端服务，因此不登记或占用运行端口。
- 本轮经验沉淀检查结论：未新增长期经验文档；本次内容属于 PQC 项目级修改方案与任务级验收计划，保留在 `doc/tasks/20260803-pqc-equipment-standard-method-design/`。
