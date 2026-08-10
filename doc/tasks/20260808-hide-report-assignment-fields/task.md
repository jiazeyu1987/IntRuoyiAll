# 隐藏报工分配弹窗内部字段

## Task Goal

生产组长“分配报工”弹窗中，截图红框区域不再展示：分配说明、复核签名 ID、签名员工 ID、签名快照，以及 FIFO 辅助提示文案；保留正式活跃订单分配表、FIFO 自动分配、新增分配行和确认分配动作。

## Milestones

- [x] M1：定位现有弹窗、接口和相邻合同，记录 BDD 与 RED 证据。
- [x] M2：调整弹窗展示与提交链路，避免隐藏后仍依赖用户手填内部字段。
- [x] M3：运行定向静态合同、相邻合同、类型检查或记录阻塞。
- [x] M4：更新验证报告与任务状态。

## Expected Verification

- `node tests/e2e/team-leader-report-allocation-dialog-hide-static.spec.cjs`
- `node tests/e2e/team-leader-workbench-static.spec.cjs`
- `node tests/e2e/mes-process-pool-team-leader-static.spec.js`
- `mvn -pl yudao-module-mes -am "-Dtest=MesP0TeamLeaderReviewSignatureSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`（如遇无关测试编译阻塞，记录首个阻塞摘要）
- `pnpm ts:check`（如遇无关历史阻塞，记录首个阻塞摘要）
- `git diff --check`（仅核对本任务触碰文件）

## Current Status

blocked - implementation and focused frontend verification are complete; full completion is blocked by unrelated existing frontend type and backend test-compile failures in the shared workspace.

## Applicable Experience Gates

- 前端静态契约隔离门禁：本次用任务专用静态合同覆盖截图目标区域，先 RED 再实现，避免被既有大合同或无关历史问题遮蔽。
- 业务运行记录用户可读展示门禁：面向业务人员的弹窗不得直出用户 ID、签名 ID、签名快照 JSON 等内部传输/审计字段；身份和签名证据应由正式链路承载。
- 前端写入成功与列表刷新失败分层门禁：确认分配写入成功后，若列表刷新失败，不能误报为分配失败或允许重复提交。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；目标是移除业务弹窗内部字段展示，并同步调整依赖边界。
- `是否存在临时补丁或绕过`：否。
