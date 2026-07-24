# 任务：展柜画布新增 Website 预览模式

## 任务目标

在展柜管理的画布编辑弹窗中保留现有“布局编辑”块视图，并新增“Website 预览”视图。第二种视图使用封面图和底部产品名条来预览卡片外观，位置和尺寸严格复用当前 `layoutX/layoutY/layoutWidth/layoutHeight`，帮助排版时直接看到接近 Website 的真实效果。

## Previous Task Check

- 上一个前端任务：`doc/tasks/20260607-dcc-preview-detail-panel/task.md`
- 状态：`blocked`
- 处理：当前线程已显式切换，上一任务阻塞已记录，不与本任务混提。

## BDD 场景

- BDD: 默认进入布局编辑模式 -> Given 用户打开展柜画布弹窗 / When 弹窗首次加载 / Then 默认显示现有文字块编辑视图，不改变现有编辑习惯。
- BDD: 切换 Website 预览模式 -> Given 展柜画布已加载产品块 / When 用户切换到 `Website 预览` / Then 每个块按当前布局坐标显示封面图和底部名称条，观感接近 Website。
- BDD: 缺封面时显式占位 -> Given 产品没有封面图 / When 用户切换到 `Website 预览` / Then 该卡片显示统一占位块和产品名，不回退成纯文字块。
- BDD: 预览切换不影响编辑与保存 -> Given 用户在任一预览模式下拖拽、拆分、删除、交换或拉伸产品块 / When 保存布局 / Then 保存 payload 与现有协议一致，不因预览模式变化而改变。

## 里程碑

- [x] M1：建立任务文档并确认前一任务状态。
- [ ] M2：补前端 RED 静态/行为测试。
- [ ] M3：实现画布预览模式切换与 Website 风格卡片渲染。
- [ ] M4：接入封面图字段和缺图占位逻辑。
- [ ] M5：运行静态检查和真实前端验收。
- [ ] M6：记录证据、收尾预览并提交本任务前端改动。
 - [x] M2：补前端 RED 静态/行为测试。
 - [x] M3：实现画布预览模式切换与 Website 风格卡片渲染。
 - [x] M4：接入封面图字段和缺图占位逻辑。
 - [x] M5：运行静态检查和真实前端验收。
 - [x] M6：记录证据、收尾预览并提交本任务前端改动。

## Expected Verification

- RED/GREEN：`node scripts/showroom-admin-hall-canvas-layout.test.mjs`
- GREEN：`pnpm ts:check`
- GREEN：本机 `http://localhost:8081` 打开展柜管理画布，验证两种模式切换、Website 预览样式、缺图占位和保存后重开保持一致。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：是，仅限用户明确要求的缺图占位显示；缺图时显式展示占位卡片，不伪装成真实封面，不回退成旧文字块。
- `是否从根因和长期维护角度解决`：是。通过扩展候选产品数据模型和画布块渲染模型，让管理端能消费正式封面字段并独立切换预览。
- `是否存在临时补丁或绕过`：否。不修改布局保存协议，不新增测试专用模式，不复用 Website 全量样式文件。

## Current Status

completed

## Cleanup Keep

- `doc/tasks/20260607-showroom-hall-canvas-website-preview/task.md`
- `doc/tasks/20260607-showroom-hall-canvas-website-preview/execution-log.md`
- `doc/tasks/20260607-showroom-hall-canvas-website-preview/frontend-feature-evidence.md`
