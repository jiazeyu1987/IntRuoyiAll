# 排产工单主操作移入标题栏右侧

## 任务目标

将排产工单页顶部筛选行右侧的主操作按钮组移动到页面标题栏右侧，匹配截图中红框区域；保留筛选、表格、分页、权限与后端查询契约不变。

## 经验门禁

- PowerShell / Windows shell：已读取 `docs/powershell-memory.md`；中文读写使用 UTF-8 明确路径或 `apply_patch`。
- 前端页面 / 表格 / 样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`，沿用紧凑操作台风格。
- 前端功能交付：已读取 `frontend-feature-delivery` 及 evidence contract，本次仅调整用户可见布局，不改 API 契约。
- BDD + 严格 TDD：先记录可观察行为与静态契约，再做最小布局实现和验证。
- No fallback：不新增降级、兼容兜底、mock 或静默吞错。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；将页面级主操作放入 `ContentWrap` 标题栏插槽，避免占用筛选工具条位置。
- 是否存在临时补丁或绕过：否。

## 里程碑

1. 定位排产工单页与统一列表模板现有工具栏结构：已完成。
2. 将主操作和显示字段移动到标题栏右侧：已完成。
3. 更新静态布局契约测试：已完成。
4. 运行验证并记录完成结果：已完成。

## 预期验证

- `node tests/e2e/mes-pro-schedule-order-toolbar-layout-static.spec.js` 通过。
- `node tests/e2e/unified-list-template-static.spec.js` 通过。
- `node tests/e2e/mes-pro-schedule-order-pool-static.spec.js` 通过。

## 当前状态

已完成；静态契约验证通过。

## Current Status

completed

## 完成记录

- `src/views/mes/pro/scheduleorder/index.vue`：将“同步工单 / 导出 / 手动重排 / 批量操作 / 显示字段”移入 `ContentWrap` 的 `header` 插槽，显示在“排产工单”标题栏右侧。
- `src/components/UnifiedListTemplate/index.vue`：增加 `showColumnSettings` 开关，排产工单主列表隐藏筛选行默认显示字段入口，避免重复。
- `src/views/mes/pro/scheduleorder/index.vue`：保留 `UnifiedListTemplate` 承载快速筛选、表格和分页，不改变后端查询参数与表格列配置数据契约。
- `tests/e2e/mes-pro-schedule-order-toolbar-layout-static.spec.js`、`tests/e2e/unified-list-template-static.spec.js`：更新静态契约，禁止主操作继续占用统一列表筛选行右侧动作插槽。

## 最终验证

- GREEN: `node tests/e2e/mes-pro-schedule-order-toolbar-layout-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/unified-list-template-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/mes-pro-schedule-order-pool-static.spec.js` -> PASS。

## 提交状态

暂未提交；前端仓已有大量前置未提交改动，且本任务目标文件位于既有脏改之上，无法安全只提交本轮 hunk。
