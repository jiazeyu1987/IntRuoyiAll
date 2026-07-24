# 20260612 工艺用途路线页签前端

## 任务目标

新增 `工艺排产路线` 和 `工艺批记录路线` 两个前端页签。页面只维护用途配置，不提供原始工艺路线或原始工序的新增、删除、修改入口。

## 里程碑

1. M1 审计：确认工艺路线列表、用途配置 API、电子批记录报表选择和菜单测试结构。
2. M2 RED：新增前端静态契约测试。
3. M3 GREEN：实现共享用途路线页面与两个包装页。
4. M4 REGRESSION：运行静态测试和类型检查。
5. M5 E2E/收尾：记录真实路径验证或阻塞，运行 cleanup 预览。

## 预期验证

- `node tests/e2e/mes-process-use-route-tabs-static.spec.js`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`
- Playwright 真实路径：本机 `http://localhost:8081`、测试租户 `aoteman`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；加载或保存失败展示后端错误。
- `是否从根因和长期维护角度解决`：是；共享用途路线组件，按 useType 切换行为。
- `是否存在临时补丁或绕过`：否。

## 当前状态

- 状态：COMPLETED。
- 已完成：共享用途路线组件、工艺排产路线包装页、工艺批记录路线包装页、静态契约测试、类型检查和真实 Playwright 路径验证。

## Current Status

completed
