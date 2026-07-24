# 任务：电子批记录表单预览滚动归位修复

## 任务目标

- 修复电子批记录表单预览区滚动条无法稳定操作的问题。
- 鼠标滚轮在表单预览区域内滚动时，应优先滚动当前报表预览容器，而不是页面最外层滚动条。
- 保持现有同源 JMReport 预览、宽度自适应和工具条隐藏能力不变。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260628-electronic-batch-record-remove-outer-card\task.md`
- 状态：`COMPLETED`
- 处理说明：上一任务仅调整页面最外层卡片壳；本次继续在同一页面修复预览滚动行为，不回退上一任务结果。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
  - `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- 适用强制门禁：
  - 保持运维台风格，不额外新增嵌套卡片或视觉绕路。
  - PowerShell 读取和命令记录统一按 UTF-8 处理。
  - 本轮仅做本机静态回归，不触发真实 Playwright 登录、服务器写入、发布、恢复或其他高风险动作，因此无需 `experience-preflight`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，将滚动责任收敛到报表预览壳层，避免外层容器和 iframe 之间的滚动竞争。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 预览滚轮优先作用于报表区域 -> Given 用户已打开电子批记录表单预览 / When 鼠标悬停在报表区域内滚动滚轮 / Then 页面优先滚动当前报表预览容器，而不是最外层页面。`
- `BDD: 预览滚动条可直接操作 -> Given 报表内容高度超过预览容器 / When 用户在右侧预览滚动条上拖动滚块 / Then 当前报表预览区域随之滚动，外层页面不抢占滚动。`

## 里程碑

1. M1：补任务文档、命令记录和滚动静态 RED 契约。
2. M2：调整预览容器与 iframe 壳层滚动职责。
3. M3：运行定向静态回归并回写结果。

## 最终验证结果

- `node tests/e2e/batch-record-preview-toolbar.spec.js` -> PASS
- `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js` -> PASS
- `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260628-electronic-batch-record-preview-scroll-fix\bug-regression-evidence.md` -> PASS

## 完成记录

- 右侧报表预览滚动职责已从外层预览 body 收敛到 `iframe-shell`。
- `iframe-shell` 新增 `overflow: auto` 和 `overscroll-behavior: contain`，滚轮不再轻易串到页面最外层。
- 外层 `batch-record-template-preview__body` 改为 `overflow: hidden`，避免与预览壳层竞争滚动。

## 预期验证

- `node tests/e2e/batch-record-preview-toolbar.spec.js`
- `node tests/e2e/electronic-batch-record-master-detail-layout-static.spec.js`
- `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260628-electronic-batch-record-preview-scroll-fix\bug-regression-evidence.md`

## Current Status

completed
