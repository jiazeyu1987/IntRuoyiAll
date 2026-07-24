# 执行日志：DCC PDF 预览缩放改为即时 CSS 缩放

- BDD: PDF 大文件缩放不重新加载 -> Given 用户打开多页或大体积 PDF / When 点击放大或缩小 / Then 页面只更新前端缩放状态，不重新调用 pdf.js 渲染整份文件。
- BDD: PDF 首次加载仍正常渲染 -> Given 用户打开 PDF 预览 / When 文件首次加载 / Then pdf.js 仍按容器基础尺寸渲染页面 canvas 与水印。
- BDD: 图片缩放行为保持即时 -> Given 用户打开图片预览 / When 点击放大或缩小 / Then 图片继续通过 CSS transform 即时缩放。
- GREEN: experience-preflight -> PASS，已读取 PowerShell 记忆与前端交付契约。
- RED: node tests/e2e/dcc-common-file-preview-source.spec.js -> FAIL，生产代码仍包含 applyPdfZoomChange / rerenderCurrentPdf。
- GREEN: implementation -> PASS，PDF 首次按基础尺寸渲染 canvas，缩放与旋转改为 CSS transform，并按缩放/旋转后的尺寸预留页面视口。

- GREEN: node tests/e2e/dcc-common-file-preview-source.spec.js -> PASS?
- GREEN: node tests/e2e/dcc-controlled-file-protection.contract.test.js -> PASS?
- GREEN: pnpm ts:check -> PASS?
- GREEN: validate_frontend_feature.py -> PASS?
- GREEN: local Vite served source/style markers -> PASS?????????? applyPdfZoomChange / rerenderCurrentPdf????? PDF viewport wrapper?
