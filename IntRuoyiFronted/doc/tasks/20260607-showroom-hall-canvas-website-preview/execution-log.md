# 执行日志：展柜画布新增 Website 预览模式

- CHECK: 上一前端任务状态 -> PASS，`doc/tasks/20260607-dcc-preview-detail-panel/task.md` 已记录为 `blocked`，当前线程可切换到新问题。
- BDD: 默认进入布局编辑模式 -> Given 用户打开展柜画布弹窗 / When 弹窗首次加载 / Then 默认显示现有文字块编辑视图，不改变现有编辑习惯。
- BDD: 切换 Website 预览模式 -> Given 展柜画布已加载产品块 / When 用户切换到 `Website 预览` / Then 每个块按当前布局坐标显示封面图和底部名称条，观感接近 Website。
- BDD: 缺封面时显式占位 -> Given 产品没有封面图 / When 用户切换到 `Website 预览` / Then 该卡片显示统一占位块和产品名，不回退成纯文字块。
- BDD: 预览切换不影响编辑与保存 -> Given 用户在任一预览模式下拖拽、拆分、删除、交换或拉伸产品块 / When 保存布局 / Then 保存 payload 与现有协议一致，不因预览模式变化而改变。
- RED: `node scripts/showroom-admin-hall-canvas-layout.test.mjs` 变更前契约 -> FAIL，expected reason：画布弹窗不存在 `Website 预览` 模式切换，也没有 `previewImageUrl` 相关卡片渲染断言。
- GREEN: `node scripts/showroom-admin-hall-canvas-layout.test.mjs` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- CHECK: 真实页面 `http://127.0.0.1:8081` -> PASS，登录 `测试租户/aoteman` 后进入 `展柜管理 -> 画布布局`，可切换到 `Website 预览`，实际 `blockCount=23`、`websitePreviewBlocks=23`、`labelCount=23`、`placeholderCount=23`。
- CHECK: 当前测试租户真实数据未返回可用封面 -> PASS，Website 预览按设计显示显式占位卡片，不回退旧文字块。
- GREEN: Playwright 截图验证 -> PASS，已生成 `artifacts/hall-canvas-editor-mode.png`、`artifacts/hall-canvas-website-preview-mode.png` 和 `artifacts/hall-canvas-preview-e2e-summary.json`。
