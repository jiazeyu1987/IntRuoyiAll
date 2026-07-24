﻿# EDHR 收尾按钮文案精简

## 任务目标

将批次详情“收尾/放行归档”区域的可见按钮文案精简为每个不超过 4 个汉字，同时保留现有权限、状态判断、点击方法和业务流程。

## 经验门禁

- PowerShell / Windows shell：已读取根仓 `docs/powershell-memory.md`，命令输出显式 UTF-8，不使用 `&&`。
- 前端页面 / 表格 / 样式：已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`，本次仅做按钮短文案，不重构布局、不改变交互结构。
- 前端文案：已读取 `clear-frontend-copy` 与 `copy-standards.md`，按钮采用简短、正式、清晰中文。
- 前端特性：已读取 `frontend-feature-delivery` 与 `frontend-contract.md`，保留现有 API、权限、路由和状态边界。
- BDD/TDD：先记录 Given/When/Then 和 RED/GREEN 证据；静态测试锁定收尾按钮文案长度。
- 禁止 fallback：不新增降级、兜底、mock 或静默吞错。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，通过统一按钮文案长度约束降低收尾区操作拥挤度，并用静态测试防回退。
- 是否存在临时补丁或绕过：否。

## BDD 场景

BDD: 收尾按钮短文案 -> Given 用户打开批次详情收尾/放行归档区域 / When 页面展示关闭、归档、放行、拒收、重开和追溯类操作 / Then 每个可见按钮文案不超过 4 个汉字，且原点击方法、权限和禁用条件保持不变。

## 里程碑

- [x] M1：创建任务文档并记录 BDD、门禁和设计约束。
- [x] M2：新增 RED 静态测试，证明当前收尾按钮存在超过 4 字文案。
- [x] M3：精简收尾区按钮文案，不改变业务逻辑。
- [x] M4：运行静态测试和必要语法检查，记录 GREEN 证据。
- [x] M5：收尾清理预览并按范围提交或报告提交阻塞。

## 预期验证

- `node tests/e2e/edhr-closing-actions-compact-copy-static.spec.js` 先 RED 后 GREEN。
- 所有收尾区按钮文案长度不超过 4 个汉字。
- 收尾区按钮仍保留原 `@click`、`:disabled`、`:loading`、`type` 和 `v-hasPermi` 绑定。

## 当前状态

completed

## 实现结果

- 收尾区按钮文案已统一压缩到 4 字以内：`生成归档`、`下载`、`放行检查`、`质量拒收`、`变更记录`、`操作审计` 等。
- 保留原有 `@click`、`:disabled`、`:loading`、`type` 和 `v-hasPermi` 绑定，不改变权限、状态判断或业务流程。
- 新增静态测试 `tests/e2e/edhr-closing-actions-compact-copy-static.spec.js`，锁定收尾区按钮文案长度和原操作绑定。

## 最终验证

- RED: `node tests/e2e/edhr-closing-actions-compact-copy-static.spec.js` -> FAIL，识别出 6 个超过 4 字按钮。
- GREEN: `node tests/e2e/edhr-closing-actions-compact-copy-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-batch-detail-review-fusion-static.spec.js` -> PASS。
- GREEN: `node --check tests/e2e/edhr-closing-actions-compact-copy-static.spec.js` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260707-edhr-closing-button-copy-compact --mode preview` -> PASS，保留 `task.md` 与 `execution-log.md`，无删除项、无阻塞。
