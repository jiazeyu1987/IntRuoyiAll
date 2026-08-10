# Verification Report

## Summary

- 任务目标：QA 规程配置页下拉中，所有已配置 QA 规程的产品优先展示；不是固定某两个产品排第一、第二。
- 真实原因：原页面默认只加载第一页 50 条 DCC 项目代码，ID / 球囊扩张压力泵 / 112 不在候选集合内，排序逻辑无法处理它；同时已配置判断只看后台正式状态，没有纳入当前页面已按产品保存的草稿数据。
- 修复结果：下拉候选改为按页拉完整匹配集合，再以产品 ID 统一合并后台正式配置状态和页面草稿状态，最后执行“已配置在前”的稳定排序。

## RED

- node tests/e2e/qa-regulation-project-configured-dropdown-real.e2e.cjs -> FAIL。
- 失败证据：真实页面下拉中未找到 ID / 球囊扩张压力泵 / 112，说明问题根因是候选加载范围不足，而不是单纯排序样式问题。

## GREEN

- node tests/e2e/qa-regulation-project-configured-dropdown-static.spec.cjs -> PASS。
- node tests/e2e/qa-regulation-project-configured-dropdown-real.e2e.cjs -> PASS。
- node tests/e2e/qa-regulation-header-project-select-static.spec.cjs -> PASS。
- node tests/e2e/qa-regulation-project-last-copy-static.spec.cjs -> PASS。
- node tests/e2e/qa-regulation-product-specific-rules-static.spec.cjs -> PASS。
- pnpm ts:check -> PASS。
- pnpm install --frozen-lockfile --reporter append-only -> PASS，任务 worktree 依赖恢复成功且锁文件未改变。
- 最新 int_main 合入后，目标静态合同、3 个相邻 QA 合同和 pnpm ts:check 再次通过。
- git diff --check on task-owned files -> PASS。

## Integration And Closeout

- int_main 与任务分支最终提交一致：c7192146ddbcead775c1e66ba93c829db684cf8e。
- branch runtime port guard 在任务分支和 int_main 均通过。
- task-closeout-cleanup preview/apply 通过，未删除核心任务记录。
- 两个本任务 worktree 均已移除，slot 11 已释放。

## Real Page Evidence

- 环境：int_main 本机前端 http://127.0.0.1:8081，后端 http://127.0.0.1:48081。
- 身份标签：芋道源码/admin。
- 候选集合：真实页面加载 119 条 DCC 项目代码。
- 已配置组：IDI / 按压式球囊扩充压力泵 / 1 与 ID / 球囊扩张压力泵 / 112 位于下拉前两项。
- 排序断言：已配置组最后索引为 1，未配置组起始索引为 2；没有产品名固定置顶断言，断言对象是配置状态分组。
- 安全断言：真实页面验证无业务写请求、无控制台错误、无页面错误。

## Residual Risk

- 当前用户确认的“统一口径”包含页面草稿数据；未把 ID / 球囊扩张压力泵 / 112 写入后台正式 QA 规程表，因此后台 project-statuses 仍只返回 1 条正式已配置记录。
- 若后续要求只承认后台正式记录，需要先把对应产品草稿正式保存为 QA 规程记录，再将排序口径收窄为纯后台状态。
