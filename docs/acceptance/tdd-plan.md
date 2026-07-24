# Codex 测试管理 TDD 计划

## Purpose and Scope

本文档定义实现测试管理功能时必须遵循的严格 TDD 路径。实现阶段应先提交失败测试，确认失败原因与当前缺失能力一致，再实现最小生产代码，最后运行回归命令。当前文档设计阶段不修改生产代码。

## Evidence Reviewed

- `docs/acceptance/bdd-scenarios.md`：行为场景来源。
- `docs/system/backend-api-design.md`：后端 API 和 Runner 协议。
- `docs/system/frontend-design.md`：前端页面和权限展示。
- `docs/system/data-model.md`：表结构和状态模型。
- `IntRuoyiBackend/pom.xml`：Java 17、Maven 多模块。
- `IntRuoyiFronted/package.json`：pnpm、TypeScript、Playwright 和现有 E2E 命令模式。

## TDD Sequence

1. 后端权限与菜单合同测试：先验证 `测试管理员` 角色、菜单权限、admin 赋权和非授权拒绝。
2. 后端测试项 CRUD 服务测试：先验证自然语言方法、检查点校验、历史快照和运行中删除限制。
3. 后端执行编排测试：先验证租户校验、Runner 在线校验、顺序与并行规则、并行安全拒绝。
4. Runner 协议 Controller 测试：先验证注册、领取、心跳、artifact 上传、checkpoint 回写和结构非法拒绝。
5. 前端静态合同测试：先验证路由、API wrapper、权限按钮、检查点编辑器和红绿结果渲染。
6. 前端 TypeScript 检查：确保页面、接口类型和组件 props 正确。
7. Playwright 真实路径 E2E：验证登录、进入菜单、创建测试项、选择租户、执行、查看结果和失败截图。

## RED Commands

- `mvn -pl yudao-module-system -am -Dtest=CodexTestPermissionMenuContractTest test`
  - 预期失败：测试类不存在，或菜单/角色/权限种子尚未实现。
- `mvn -pl yudao-module-system -am -Dtest=CodexTestCaseServiceImplTest test`
  - 预期失败：测试项服务、检查点校验和快照逻辑尚未实现。
- `mvn -pl yudao-module-system -am -Dtest=CodexTestExecutionServiceImplTest test`
  - 预期失败：执行编排、Runner 在线校验和并行安全规则尚未实现。
- `mvn -pl yudao-module-system -am -Dtest=CodexTestRunnerControllerTest test`
  - 预期失败：Runner 注册、领取、回写和 artifact 接口尚未实现。
- `pnpm test e2e:system:codex-test-management:static`
  - 预期失败：前端静态合同脚本或页面路由/API wrapper 尚未实现。
- `pnpm ts:check`
  - 预期失败：新增页面类型和 API 类型未实现前无法通过相关引用。
- `pnpm test e2e:system:codex-test-management:real`
  - 预期失败：真实页面路径、Runner 和测试租户凭据未就绪。

## Expected Failures

- 权限测试应先失败在缺少 `system:codex-test:*` 菜单权限，而不是失败在测试环境不可启动。
- CRUD 测试应先失败在缺少 `CodexTestCaseService` 或缺少表结构。
- 执行编排测试应先失败在 Runner 在线校验未实现，而不是默认创建成功执行。
- Runner 回写测试应先失败在接口不存在或结构校验缺失。
- 前端静态测试应先失败在缺少路由、API wrapper、结果图标和截图入口。
- 真实 E2E 应先失败在功能入口缺失或 Runner 前置条件缺失；不得改成 API-only 通过。

## GREEN Commands

- `mvn -pl yudao-module-system -am -Dtest=CodexTestPermissionMenuContractTest,CodexTestCaseServiceImplTest,CodexTestExecutionServiceImplTest,CodexTestRunnerControllerTest test`
- `pnpm test e2e:system:codex-test-management:static`
- `pnpm ts:check`
- `pnpm test e2e:system:codex-test-management:real`
- `python -X utf8 C:/Users/BJB110/.codex/skills/system-design-docs/scripts/validate_system_design.py --root E:/IntRuoyi`
- `python -X utf8 C:/Users/BJB110/.codex/skills/bdd-tdd-acceptance-planner/scripts/validate_acceptance_plan.py --root E:/IntRuoyi`

## Refactor Checks

- 移除重复的状态判断，把执行状态流转集中在服务层方法。
- 保持 Controller 只做权限、参数和响应转换，不在 Controller 中拼接 Runner 执行逻辑。
- Runner 协议 DTO 与页面 VO 分离，避免把内部 token、临时路径或凭据相关字段暴露给前端。
- 前端检查点编辑器保持纯表单组件，执行状态展示保持只读组件。
- 不引入静默 fallback；Runner 缺失、凭据缺失、截图缺失都走明确错误状态。

## Evidence Log Template

- BDD: `<场景名>` -> Given/When/Then 摘要。
- RED: `<命令>` -> FAIL, `<预期失败原因>`。
- GREEN: `<命令>` -> PASS。
- REGRESSION: `<命令>` -> PASS, 覆盖权限、租户、Runner、截图和真实 Playwright 路径。
- BLOCKER: `<前置条件>` -> `<缺失内容和影响>`。

## Test Blockers

- 缺少真实数据库 schema 核对时，不能编写或执行菜单/角色/权限迁移。
- 缺少 Runner token、Codex CLI、Playwright 或浏览器时，Runner 真实执行测试阻塞。
- 缺少测试租户账号凭据映射时，真实 E2E 阻塞。
- 当前工作区已有大量非本任务改动；实现阶段提交必须只包含本任务拥有文件。

