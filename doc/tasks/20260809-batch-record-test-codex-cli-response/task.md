# 批记录测试 Codex CLI 回复修复

## Task Goal

修复“批记录测试”行级“测试”按钮未实际启动的问题。点击后必须通过正式 Codex Test 执行链路调用 Codex CLI，对当前代码是否满足行描述进行只读分析，并在当前页面展示 Codex CLI 的执行状态与回复内容。

## Milestones

- [x] M1：复现并确认未创建执行批次的根因，冻结 BDD 与 RED 证据。
- [x] M2：补齐正式执行与回复查询契约，实现最小修复。
- [x] M3：完成前后端定向测试、真实页面路径验证和回归检查。
- [x] M4：完成任务清理与最终记录。

## Expected Verification

- 前端静态回归：点击行级“测试”必须直接启动单项只读代码分析，并打开可见的运行结果界面。
- 后端回归：执行详情必须保留 Runner/Codex CLI 回写的摘要、检查点实际结果和失败原因。
- Runner 回归：`CODE_READONLY` 测试通过正式 Runner claim、Codex CLI、结构化回写链路执行，不得裸调用或假成功。
- 真实页面验证：通过本机 `8081/48081`、真实测试租户与真实页面按钮创建 executionId，并在页面看到终态回复；缺少账号、Runner 或 Codex CLI 前置时必须阻塞并记录。
- `git diff --check` 与任务证据校验通过。

## Applicable Experience Gates

- Codex Runner 自动测试门禁：必须使用后端执行批次、Runner 注册/领取/心跳、Codex CLI 和结构化回写；禁止 API-only 或直接裸调用 CLI 冒充页面闭环。
- Codex Runner 运行态重启与 CLI 自检门禁：正式执行前确认 Runner 在线心跳与 Codex CLI 短预算自检；CLI 失败时不得继续宣称测试已运行。
- Codex Runner `CODE_READONLY` 长任务与实时代码证据门禁：Runner 注册可见性必须脱离调用方重复读事务快照，迟到有效心跳只续租已注册在线会话，结果查询使用独立长请求预算；Windows 只读 shell ACL 不可用时，只能由 Runner 从白名单源码目录收集有界实时证据，不得降级权限或扫描生成目录。
- 前端错误必须明确暴露；不得吞异常、默认成功或在执行未创建时展示回复。
- 本次不引入 fallback、兼容分支、mock 回复或占位成功。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；统一复用正式 Codex Test 执行与结果模型，在页面消费结构化执行详情。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed

实现、验证与收尾均已完成：启动事务可见性、迟到心跳续租、结果查询独立超时、Codex CLI 只读结构化执行及白名单实时代码证据链路均已修复。真实页面 execution `127` 首次验证通过；运行态恢复后 execution `130` 再次返回“通过”，逐行“历史”弹框展示完整 Codex CLI 回复。五项前端静态回归、类型检查、后端 50 项 Codex Test 回归、证据校验、cleanup preview/apply 和经验索引验证均通过。
