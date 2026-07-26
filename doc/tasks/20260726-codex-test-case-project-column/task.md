# 测试管理项目列分类

## Task Goal

- 在系统管理的测试管理列表中增加 `项目` 列。
- 将当前测试项分类到三个项目：`智能排产`、`文控`、`批记录`。
- 保持现有测试管理列表模板和数据契约，不引入 fallback、mock 或默认成功路径。

## Milestones

- [x] 梳理测试管理页面、API、后端持久化和当前测试项来源。
- [x] 先补最小 RED 静态/契约验证，证明 `项目` 列和分类缺失。
- [x] 实现 `项目` 字段展示、接口传递和当前项分类。
- [x] 运行针对性 GREEN/回归验证，并记录剩余阻塞。

## Expected Verification

- 前端测试管理静态契约能验证 `项目` 列存在并使用标准列表模板。
- 后端或数据静态契约能验证当前测试项具备三类项目归属。
- 相关 TypeScript / Java / SQL 变更通过最小范围检查；若存在无关历史失败，必须明确隔离记录。

## Current Status

- ready_for_closeout

## 经验门禁

- 命中 `docs/e2e-rules.md#codex-runner-自动测试门禁`：本任务只做静态合同与后端契约验证，未启动真实 Runner，因此不需要 Runner token/租户写入前置。
- 命中 `docs/frontend-development.md#前端静态契约隔离门禁`：前端使用 `system-codex-test-management-static.spec.js` 做聚焦合同验证。
- 命中 `docs/backend-development.md#2026-07-25-maven-reactor-兄弟模块验证门禁`：后端目标 JUnit 使用 `-pl yudao-module-system -am` 构建依赖模块。
- 新增经验 `docs/powershell-memory.md#powershell-maven--d-参数引号门禁`：PowerShell 中 Maven `-D` 参数需整体加引号。

## Remaining Closeout Blocker

- 当前分支任务开始前已存在 `ahead 3` 和多处非本任务脏改动；为避免把并行任务改动混入提交，本次未执行 baseline commit、任务 commit、cleanup apply 或 push。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，计划从数据契约和列表展示根因补齐项目字段。
- `是否存在临时补丁或绕过`：否。
