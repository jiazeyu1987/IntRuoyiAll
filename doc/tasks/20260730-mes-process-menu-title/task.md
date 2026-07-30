# 20260730 MES 工序菜单标题恢复

## Task Goal

- 将生产管理下 `/mes/pro/mes-process` 入口从“标准模板列表”恢复为“MES工序”。
- 同步页面标题、动态菜单 SQL/运行态菜单名、搜索入口和验证脚本，确保前端真实路径能看到“MES工序”，且不再显示“标准模板列表”。

## Milestones

1. 创建任务记录并确认动态菜单重命名门禁。
2. 写出先失败的聚焦静态契约，证明旧标题仍存在。
3. 修改正式标题、菜单 SQL/运行态数据和搜索验证。
4. 运行静态合同、类型检查或相邻契约，并执行真实 Playwright E2E。
5. 记录验证报告、清理状态和最终结论。

## Expected Verification

- `node tests/e2e/mes-pro-mes-process-readonly-static.spec.js`
- 必要时运行相邻静态契约。
- 必要时运行 `pnpm ts:check`。
- Playwright 使用本机 `http://127.0.0.1:8081` 登录 `芋道源码/admin`，真实页面验证：
  - 菜单/页面显示“MES工序”。
  - 搜索 `mes工序` 能命中并进入 `/mes/pro/mes-process`。
  - 页面和侧边栏不显示“标准模板列表”。
  - `/admin-api/mes/pro/route-resource/page` 返回业务码 `0`。
  - 无 `系统异常`，无 MES 写请求。

## Applicable Gates

- 动态菜单页签重命名门禁：必须同时覆盖 `system_menu.name` 正式来源、页面标题、搜索入口和真实登录态菜单。
- 中文菜单名称 ASCII 安全迁移门禁：若写入中文菜单名 SQL 或运行库数据，必须使用 UTF-8/HEX 可核验路径并精确定位目标行。
- 前端静态契约隔离门禁：若全量检查有无关失败，使用聚焦契约证明当前行为 RED/GREEN。
- 官方登录前置与 admin-only 全量验证门禁：真实 E2E 使用 `芋道源码/admin`，仅做只读验证，不写入业务数据。

## Current Status

in_progress

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是同步正式动态菜单名、页面标题和搜索验证，而不是只改截图文案。
- `是否存在临时补丁或绕过`：否。
