# Codex 自动测试管理设计任务

## Task Goal

为“系统管理 > 测试管理”能力完成 BDD + TDD 驱动的文档设计，覆盖权限角色、测试项管理、自然语言测试方法、Codex Runner + Playwright 真实执行、检查点结果、失败截图临时存储、顺序/并行执行和租户级执行上下文。

## Milestones

- [x] 创建任务目录并记录任务目标。
- [x] 核对项目规则、技术栈、权限/菜单/前端/后端/E2E 相关证据。
- [x] 输出系统设计文档：前端、后端 API、数据模型、配置安全部署。
- [x] 输出验收设计文档：BDD 场景、TDD 计划、E2E 计划、测试数据。
- [x] 进行文档结构验证并记录结果。

## Expected Verification

- 文档包含可执行的 Given / When / Then 场景。
- 每个核心行为都有 RED/GREEN 的严格 TDD 路径。
- 系统设计明确权限、租户、自然语言执行协议、Runner 回写协议、失败截图临时目录和并行安全边界。
- 文档不引入 mock 成功、静默降级、隐式 fallback 或 API-only 替代真实 Playwright 路径。

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，当前任务先进行可实现的系统级文档设计，避免直接堆临时页面或脚本。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- Trigger: 权限菜单、角色赋权、真实 Playwright 和 Windows PowerShell 文档设计。
- Preflight check: 菜单和角色迁移以稳定 `permission`、`role.code`、`username + tenant_id` 查询真实 ID；真实 Playwright 前确认模块、浏览器 executable path、测试租户和账号映射。
- Blocker: Runner、Codex CLI、Playwright、浏览器、测试租户凭据或前端入口缺失；菜单/角色 schema 未核对；截图临时目录不可读写。
- Verification: 菜单动态权限响应、后端 `@PreAuthorize`、前端 `v-hasPermi`、Runner 结果 JSON、截图 artifact、真实页面路径和结构校验结果。
- Forbidden action: 不得使用 API-only 代替真实页面路径；不得硬编码角色或用户绑定 ID；不得把 Runner 缺失、账号缺失或截图缺失标记为成功。
- Evidence: `docs/experience-index.md`，`docs/powershell-preflight-lessons.md`，`docs/release-build-preflight-lessons.md` 中权限菜单兼容门禁，`docs/e2e-rules.md`。

## Deliverables

- `docs/system/frontend-design.md`
- `docs/system/backend-api-design.md`
- `docs/system/data-model.md`
- `docs/system/config-security-deployment.md`
- `docs/acceptance/bdd-scenarios.md`
- `docs/acceptance/tdd-plan.md`
- `docs/acceptance/e2e-plan.md`
- `docs/acceptance/test-data.md`

## Final Verification

- 系统设计结构校验通过。
- BDD/TDD 验收设计结构校验通过。
- UTF-8 回读、弱占位扫描和 `git diff --check` 通过。
- `task-closeout-cleanup` preview/apply 通过，无删除项、无阻塞项、无额外 worktree 操作。
- 未修改生产代码、数据库、运行环境或测试租户；实现阶段仍需按 TDD 计划执行 RED/GREEN 和真实 Playwright 验收。
