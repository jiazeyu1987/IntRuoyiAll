# 修复测试管理访问系统异常

## Task Goal

定位并修复访问「测试管理」时提示系统异常的问题，补充回归验证，避免通过 fallback、吞异常或默认成功掩盖根因。

## Milestones

- [x] 建立复现路径和预期行为
- [x] 定位前端/后端/API 根因
- [x] 先补充失败回归测试或可复现验证证据
- [x] 实施最小正式修复
- [x] 运行定向回归验证并记录结果
- [ ] 收尾清理、经验沉淀、提交与推送

## Expected Verification

- 访问「系统管理 / 测试管理」不再出现系统异常。
- 定向回归测试覆盖本次根因。
- 相关前端或后端检查通过。
- 不引入 fallback、吞异常、mock 成功或默认成功。

## Current Status

ready_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；本地数据库应用缺失迁移 `20260726_system_codex_test_case_project.sql`，补齐当前代码必需的 `system_codex_test_case.project` 字段。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- Codex Runner 自动测试门禁：测试管理页面真实执行前需确认本机前端/后端入口、目标租户、测试管理员账号和页面执行入口；不得用 API-only、mock 或 Runner 离线跳过替代真实页面验证。
- 数据库 Schema 核对门禁：涉及测试管理表结构时先核对 `system_codex_test_case` / `system_codex_test_execution_case` 字段；缺字段不得用前端隐藏、后端默认值或吞异常掩盖。

## Closeout Status

- 实现、验证、经验沉淀和 cleanup preview/apply 已完成，当前保持 `ready_for_closeout`。
- Git 收尾暂未完成：当前分支已有非本任务 ahead 提交和非本任务 dirty/untracked 文件，需单独确认后才能按项目规则提交、清理和推送。
- 当前非本任务 dirty/untracked 证据包括 MES 路线相关文件、`.runtime/codex-test-runner/codex-runner.pid`、`IntRuoyiFronted/scripts/start-codex-test-runner.ps1`、以及 `doc/tasks/20260726-route-flow-add-form-click-count/`。

## Cleanup Keep

- doc/tasks/fix-test-management-system-exception-20260726/bug-regression-evidence.md
