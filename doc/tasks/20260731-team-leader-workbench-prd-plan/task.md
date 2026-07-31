# 生产组长工作台 PRD / 开发计划 / 测试计划

## Task Goal

基于用户已确认的生产组长职责和员工填报链路，形成可交付给研发与测试执行的三类文档：

- `prd.md`：生产组长工作台与员工填报配置联动的产品需求。
- `development-plan.md`：按 BDD + 严格 TDD 分阶段落地的开发计划。
- `test-plan.md`：覆盖单元、接口、前端静态合同和真实 Playwright E2E 的测试计划。

## Evidence Reviewed

- 用户需求：生产组长维护员工、设备、设备参数、活跃订单、工序关系、异常原因，并确认报工分配到活跃订单。
- 用户需求：员工填报页 `output/frontline-production-operator-1920.html` 和 `output/frontline-production-operator-1920-no-device.html` 的设备、参数、不良原因来自生产组长配置。
- 用户需求：订单某工序累计确认分配数量达到订单生产数量后，工序完成并自动回填该工序绑定的正式批记录表格。
- 用户澄清：设备状态是“报修”，不是“保修”。
- 用户澄清：报工分配需要支持 FIFO 自动分配，同时允许手动分配或调整。
- 当前前端入口：`IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue`。
- 当前接口模块：`IntRuoyiFronted/src/api/mes/pro/processpool/teamLeader.ts`。
- 当前员工填报组件：`IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue`。
- 项目规则：`docs/task-closeout-rules.md`、`docs/powershell-encoding.md`、`docs/e2e-rules.md`、`docs/engineering/technology-stack-routing.md`。
- 技能规则：`product-requirements-docs`、`bdd-tdd-acceptance-planner`。

## Milestones

- [x] M1：创建任务目录并记录目标、门禁、预期验证。
- [x] M2：写入 `prd.md`，定义产品范围、业务规则、状态流转和验收标准。
- [x] M3：写入 `development-plan.md`，定义按阶段推进的 BDD + TDD 开发顺序。
- [x] M4：写入 `test-plan.md`，定义 BDD 场景、RED/GREEN 命令、真实 E2E 路径和测试数据。
- [x] M5：执行文档结构与 UTF-8 读取验证，记录结果。

## Expected Verification

- 使用 UTF-8 方式读取任务目录下所有 Markdown 文档。
- 检查 `prd.md` 包含 PRD 结构要求的核心章节。
- 检查 `development-plan.md` 包含阶段、RED/GREEN、验证门禁。
- 检查 `test-plan.md` 包含 BDD、TDD、E2E、测试数据、阻塞项。
- 不运行产品代码测试；本任务只交付计划文档，不修改生产代码。

## Applicable Gate Summary

### 文档目录精确暂存门禁

- Trigger: 当前任务只新增 `doc/tasks/20260731-team-leader-workbench-prd-plan/` 文档，且工作区已有并行前端改动。
- Preflight check: 只写入本任务目录，不触碰现有源码与其它任务目录。
- Blocker: 如需提交，必须先确认现有 ahead 与脏工作区归属，不能用宽泛 `git add -A`。
- Verification: `git status --short --branch` 显示本任务仅新增当前任务目录文件。
- Forbidden action: 禁止提交或回滚并行任务源码改动。

### E2E 脚本入口存在性门禁

- Trigger: 测试计划列出真实 Playwright E2E。
- Preflight check: 实施阶段必须核对 `IntRuoyiFronted/package.json` scripts、目标 spec 文件、真实路由、权限、测试租户和账号。
- Blocker: 缺少脚本、页面入口、权限、测试账号、浏览器或运行态时，E2E 标记 BLOCKED，不得用 API-only 或静态合同冒充真实 E2E。
- Verification: 真实 E2E 证据必须包含前后端 URL、租户/账号标签、页面断言、接口核验和任务自有数据清理。
- Forbidden action: 禁止 mock 数据、API-only、直连历史 URL 或静态合同替代真实用户路径。

### PowerShell / UTF-8 文档写入门禁

- Trigger: 写入中文 Markdown 任务文档。
- Preflight check: 使用 `apply_patch` 写入，验证时使用 `python -X utf8` 或等效 UTF-8 读取。
- Blocker: 发现乱码、编码漂移、文件读写失败时停止并报告。
- Verification: 记录 UTF-8 读取命令与结构校验结果。
- Forbidden action: 禁止默认 `Set-Content`、`Out-File`、`>`、`>>` 写中文文档。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。文档明确缺少正式数据链路时必须 fail fast。
- `是否从根因和长期维护角度解决`：是。方案围绕班组配置、员工填报、组长确认、订单工序完成、正式批记录回填的根链路设计。
- `是否存在临时补丁或绕过`：否。文档禁止使用 `formBindings`、默认 `MAIN`、空值、mock 或前端文案替代正式批记录来源。

## Current Status

ready_for_closeout

## Closeout Notes

- 本任务已完成文档写入与结构验证。
- 当前工作区存在任务开始前已有的 ahead 与源码改动，未执行提交或推送；如需要收尾提交，必须先确认并行改动归属和提交边界。
