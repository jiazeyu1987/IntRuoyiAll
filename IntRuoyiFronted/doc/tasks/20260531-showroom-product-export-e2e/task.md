# 任务：展厅产品管理导出 E2E 验证

## 目标

使用真实浏览器访问 `http://localhost:8081/showroom/product`，点击产品管理页面的“导出”按钮，验证是否产生 Excel 下载文件。

## 里程碑

1. 建立任务记录并确认前置条件：本地前端入口可访问、Playwright 可运行、登录方式明确。
2. 使用测试租户真实登录并进入展厅产品管理页面。
3. 点击“导出”按钮并捕获浏览器下载事件，检查文件名、扩展名和文件头。
4. 记录验证结果、下载证据和剩余阻塞。

## 预期验证

- Playwright 真实浏览器路径可进入 `http://localhost:8081/showroom/product`。
- 点击“导出”并确认后触发下载事件。
- 下载文件扩展名为 `.xlsx`，文件头符合 Office Open XML ZIP 容器特征。

## Current Status

completed

- status: completed
- previous frontend task status: `20260531-edhr-archive-version-sha-dialog` 已标记 completed，不阻塞本次验证。
- completed work:
  - 已使用 Playwright 真实浏览器登录测试租户 `测试租户/aoteman`。
  - 已进入 `http://localhost:8081/showroom/product` 并点击产品管理页面“导出”按钮。
  - 已在确认弹窗点击“确定”并捕获浏览器下载事件。
  - 已检查下载文件名、文件头和 ZIP 工作簿结构。
- final verification:
  - `node output\playwright\20260531-showroom-product-export-e2e\run-export-e2e.mjs` -> PASS。
  - 下载文件名：`产品资料修改版-补充产品资料.xlsx`。
  - 下载大小：`174022691` bytes。
  - 文件头：`50 4B 03 04 14 00 08 08`，符合 Office Open XML ZIP 容器。
  - ZIP 条目包含 `[Content_Types].xml`、`_rels/.rels`、`docProps/app.xml`、`xl/drawings/drawing1.xml`、`xl/media/image1.png` 等。
- remaining blockers: none.

## Cleanup Candidates

- `output/playwright/20260531-showroom-product-export-e2e/`
