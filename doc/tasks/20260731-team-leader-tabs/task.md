# 20260731 班组长页签拆分

## Task Goal

将现有“工序池班组长工作台”拆分为“生产组长”和“PQC 组长”两个页签：

- 生产组长页签保留当前提交看板、异常上报、班组维护功能。
- PQC 组长页签暂时显示明确的占位内容，不复用生产组长功能。
- 保留现有路由、查询权限和 API 契约。

## Milestones

- [ ] M1：补充 BDD 场景和聚焦静态契约，先得到 RED。
- [ ] M2：完成两个组长类型页签及 PQC 占位内容。
- [ ] M3：运行目标静态测试、类型检查和回归验证。
- [ ] M4：完成任务记录、收尾检查和最终状态更新。

## Expected Verification

- `node tests\e2e\mes-process-pool-team-leader-static.spec.js`
- `pnpm ts:check`
- `git diff --check`
- 前端功能证据校验脚本。

## Current Status

in_progress

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；PQC 页签是用户明确要求的占位范围，不作为生产功能降级。
- `是否从根因和长期维护角度解决`：是；用组长类型页签表达功能边界，避免继续让 PQC 组长误用生产组长操作。
- `是否存在临时补丁或绕过`：否；PQC 占位是明确的产品范围占位，不伪造数据、不调用生产组长接口。

## Applicable Experience Gates

- 前端静态契约必须先 RED，再实现后 GREEN。
- 不得通过隐藏错误、mock 数据或 API-only 验证替代真实页面结构。
- 本任务只修改班组长工作台和其聚焦静态测试，不触碰工作区已有的其他任务改动。
