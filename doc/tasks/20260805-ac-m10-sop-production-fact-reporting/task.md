# AC-M10 SOP 生产事实报工修复

## Task Goal

修复岗位矩阵 `AC-M10 | 生产员工 | 按 SOP 生产` 的代码级缺口：生产员工未选择订单/任务时仍应能基于正式工序与 SOP 进入工序事实报工；缺少工序、缺少 SOP 或越权工序时必须后端 fail fast，不能用默认模板、空列表成功或前端隐藏替代正式阻塞。

## Milestones

- [x] 创建任务记录并锁定 BDD/TDD 验收口径
- [x] 定位一线生产入口、SOP/工序来源和现有失败路径
- [x] 补充 RED 回归并复用后端阻塞回归：无订单 SOP 入口、缺 SOP、越权工序
- [x] 实施前端最小修复，不引入 fallback、默认成功或吞异常
- [x] 运行定向验证并记录剩余阻塞

## Expected Verification

- `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineRuntimeConfigServiceTest,MesFrontlineTemplateResolverTest,MesFrontlineSubmitAuthorizationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `node tests/e2e/role-matrix-ac-m10-sop-production-static.spec.cjs`
- `node tests/e2e/frontline-formal-submit-static.spec.cjs`

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；要求使用正式工序、SOP/模板绑定和授权来源。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- `docs/backend-development.md#MES 一线设备账号权限门禁`：权限角色授权必须走标准权限解析，不能用岗位/工作站失败作为 fallback。
- `docs/backend-development.md#eDHR 批次任务配置来源门禁`：正式配置缺失必须 fail fast，不得用默认 MAIN、空绑定或发布快照泛化兜底。
- `docs/frontend-development.md#前端静态契约隔离门禁`：若全量前端检查受无关历史问题阻塞，使用任务专用静态契约记录 RED/GREEN。
- `docs/powershell-memory.md#PowerShell Maven -D 参数引号门禁`：Maven `-D` 参数必须整体加双引号。

## Current Status

completed

AC-M10 定向修复、验证和 cleanup preview/apply 已完成；实现与任务初始记录已由并发基线提交 `057fba5b9` 纳入本地历史，本收尾记录单独提交并推送。
