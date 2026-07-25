# 批记录模块 Codex 测试项补充

## Task Goal

仿照测试管理中“排产工单手动重排”测试项的组织方式，为批记录模块补充必要的 Codex Runner 测试项，覆盖批记录核心业务路径和风险点。

## Milestones

1. 定位现有“排产工单手动重排”测试项定义、数据结构和写入位置。
2. 梳理批记录模块需要补充的测试项清单，保持与现有测试管理数据契约一致。
3. 增加批记录测试项并执行针对性结构验证。
4. 记录验证证据、剩余风险和收尾状态。

## Expected Verification

- 结构验证：新增测试项字段完整、模块/分类/执行参数与现有测试项契约一致。
- 回归验证：相关静态测试或数据校验命令通过。
- 证据记录：`execution-log.md` 和 `verification-report.md` 记录 BDD、RED/GREEN、验证命令与结果。

## Current Status

in_progress

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，直接补充测试管理所需测试项，保持现有契约。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

### Codex Runner 目标测试项存在性门禁

- Trigger: 增加、调整或执行系统管理-测试管理中的 Codex Runner 测试项。
- Preflight check: 定位现有测试项数据源，确认目标测试项不是仅存在于自然语言描述或历史证据中。
- Blocker: 测试项搜索总数为 0、Runner 空领取、或新增项缺少可执行目标/断言字段时必须停止。
- Verification: 用结构化校验或静态测试证明测试项可被发现且字段完整。
- Forbidden action: 禁止把 Runner 空领取或未匹配测试项当作执行成功。
- Evidence: `docs/experience-index.md` 路由到 `docs/e2e-rules.md#codex-runner-目标测试项存在性门禁`。

### Codex Runner 自动测试门禁

- Trigger: 测试管理测试项涉及 Codex Runner、自然语言测试方法、检查点截图或 Playwright 实流。
- Preflight check: 确认可执行入口、目标菜单、测试方法和检查点断言都在测试项中明确。
- Blocker: 缺少真实前端路径、登录/租户前置条件、或检查点无法定位时阻塞。
- Verification: 记录静态合同验证、真实 E2E 或明确的阻塞原因。
- Forbidden action: 禁止使用 mock、API-only 路径或空 Runner 领取替代真实用户路径。
- Evidence: `docs/experience-index.md` 路由到 `docs/e2e-rules.md#codex-runner-自动测试门禁`。
