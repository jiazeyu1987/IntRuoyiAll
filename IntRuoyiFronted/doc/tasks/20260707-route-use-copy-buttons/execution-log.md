# 执行日志

## BDD

- BDD: 路线编码可复制且配置入口不变 -> Given 用户进入工艺用途路线列表；When 点击某行“路线编码”旁的复制按钮；Then 当前行路线编码复制到剪切板，且点击路线编码文本仍打开用途配置。
- BDD: 路线名称可复制且详情入口不变 -> Given 用户进入工艺用途路线列表；When 点击某行“路线名称”旁的复制按钮；Then 当前行路线名称复制到剪切板，且点击路线名称文本仍打开源工艺路线详情。

## 经验门禁

- GREEN: powershell-memory -> PASS，第一条 PowerShell 命令前已读取 `docs/powershell-memory.md`，后续中文读写均显式 UTF-8。
- GREEN: experience-index -> PASS，已读取 `docs/experience-index.md`，本任务命中 PowerShell 编码门禁和前端页面样式门禁。

## TDD 证据

- RED: node tests/e2e/mes-route-use-copy-buttons-static.spec.js -> FAIL，当前 `RouteUsePage.vue` 未引入 `useClipboard`，路线编码和路线名称列没有复制按钮。
- GREEN: node tests/e2e/mes-route-use-copy-buttons-static.spec.js -> PASS，路线编码和路线名称列均提供行内复制按钮，复制方法分别写入 `row.code` 和 `row.name`。
- GREEN: node tests/e2e/mes-route-use-source-route-detail-link-static.spec.js -> PASS，路线编码仍打开用途配置，路线名称仍打开源工艺路线详情。
- GREEN: pnpm.cmd exec eslint src/views/mes/pro/route-use/RouteUsePage.vue tests/e2e/mes-route-use-copy-buttons-static.spec.js -> PASS，本次目标页面和新增静态测试无 ESLint 问题。
- BLOCKER: NODE_OPTIONS=--max-old-space-size=8192 pnpm.cmd ts:check -> FAIL，未触碰文件 `src/views/mes/pro/edhr-batch/BatchExecutionDetailPage.vue(130,59)` 引用 `EdhrBatchExecutionReviewExecutionRespVO.closedAt`，当前类型定义不存在该属性；本任务未修改该文件，按脏工作区隔离原则不在本任务内修复。
- UPDATE: 按用户反馈对齐生产工单复制按钮样式，将“复制”文字改为 `ep:copy-document` 图标按钮，增加 `aria-label`，并让文本区占满剩余宽度，使复制按钮在列内右侧对齐。

## 收尾

- 状态：completed
- GREEN: node tests/e2e/mes-route-use-copy-buttons-static.spec.js -> PASS，路线编码和路线名称图标复制按钮契约通过。
- GREEN: node tests/e2e/mes-route-use-source-route-detail-link-static.spec.js -> PASS，路线编码用途配置入口与路线名称详情入口未回归。
- GREEN: pnpm.cmd exec eslint src/views/mes/pro/route-use/RouteUsePage.vue tests/e2e/mes-route-use-copy-buttons-static.spec.js -> PASS，本次目标页面和新增静态测试无 ESLint 问题。
- 说明：全量类型检查仍受未触碰 eDHR 文件既有 `closedAt` 类型错误影响，本任务按无关脏改隔离原则不处理。
