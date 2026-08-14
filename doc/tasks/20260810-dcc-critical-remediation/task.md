# DCC 严重缺陷逐项修复与主线融合

## Task Goal

在独立 worktree 中逐项修复本次确认的 18 项 DCC 文件生命周期、审批路线、签名证据、分发访问、上传治理和 HTTP 契约问题；每项以 BDD + 严格 TDD 验证，完成系统级回归后融合进 `int_main`。

## Milestones

- [x] M1 建立受控 worktree、任务基线与监督状态
- [x] M2 完成需求分析、验收标准和依赖任务拆分
- [x] M3 修复文件生命周期、上传校验与幂等问题
- [x] M4 修复审批路线就绪校验、岗位提示和最终批准校验
- [x] M5 修复签名证据一致性、受控副本哈希绑定和分发访问
- [x] M6 修复 HTTP 状态与通用上传策略并完成系统回归
- [x] M7 独立审计、任务清理并融合到 `int_main`

## Expected Verification

- 每项缺陷均有 `BDD`、`RED`、`GREEN` 和相关回归证据。
- DCC 后端目标 JUnit、前端静态合同与类型检查通过。
- 涉及真实用户路径的场景使用 Playwright 和真实测试租户数据验证。
- 分支运行端口守卫、任务结构校验、独立完成度审计和 closeout preview/apply 通过。
- worktree 分支安全融合到 `int_main`，且不覆盖主工作区并发改动。

## 适用经验门禁

- 上传类别：预上传、提交、页面候选项必须统一使用同一份类别可用性与权限投影；类别不存在、停用或超出授权范围必须在预上传阶段拒绝。
- 预览：`canPreview` 必须同时受文件实际存在性和不可预览原因约束；后端文件不存在时前端不得继续展示可预览状态。
- DCC 审批 E2E：真实页面路径、真实候选人、发布前盖章 PDF、真实分发部门和指定审批账号不可替换为 API-only 或伪造路线。
- DCC 分发 E2E：必须分别证明目标文件、任务收件人、部门收件人可访问，且普通非收件人仍被拒绝。
- 脏主工作区融合：融合前计算 incoming 与主工作区 dirty 文件交集；交集非空时逐文件保护并复核，禁止覆盖或清理并发改动。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；以统一文件引用保护、原子状态转换、路线就绪检查和单一证据验证器为目标。
- 是否存在临时补丁或绕过：否。

## Current Status

completed：AC-01..AC-18 的实现、自动化验证、集成基线复验、task-closeout-cleanup、`ff-only` 融合、worktree 物理清理和 slot 释放均已完成；`int_main` 包含任务提交 `5932be504` 与验证记录提交 `688afee83`。真实 Playwright 完整发布成功路径仍受测试租户正式类别数据缺口阻断，阻断发生在零业务写入前并保留在 `verification-report.md`，未以改库、伪造分类或 API-only 绕过。按用户要求未 push。

## Cleanup Keep

- doc/tasks/20260810-dcc-critical-remediation/task.md
- doc/tasks/20260810-dcc-critical-remediation/execution-log.md
- doc/tasks/20260810-dcc-critical-remediation/verification-report.md
