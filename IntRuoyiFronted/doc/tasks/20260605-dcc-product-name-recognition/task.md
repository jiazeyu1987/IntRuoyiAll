# 任务：DCC 文件产品名称识别按钮

## 任务目标

在 DCC 受控文件详情页“产品名称”行增加“识别”按钮。点击后调用后端识别接口，等待当前配置的 Codex CLI 识别当前文件产品名称，成功后刷新详情页并展示写入数据库后的产品名称。

## Previous Task Check

- 上一个前端任务 `doc/tasks/20260605-runtime-control-promote-backup-authenticated-dcc-readback/task.md` 已标记 `completed`。
- 本任务只修改 DCC 详情页、DCC API 类型/方法、前端测试和本任务目录。

## BDD 场景

- BDD: 产品名称行提供识别入口 -> Given DCC 详情页显示产品名称 / When 用户有文控角色且文件详情已加载 / Then 产品名称旁应显示“识别”按钮。
- BDD: 超管可见识别入口 -> Given 用户具备 `super_admin` 角色 / When 打开 DCC 详情页 / Then 产品名称旁同样显示“识别”按钮，后端仍按正式角色授权校验。
- BDD: 点击识别后刷新详情 -> Given 用户点击“识别”按钮 / When 后端返回识别出的产品名称 / Then 前端显示成功提示并调用 `reloadAll()` 刷新详情数据。
- BDD: 识别失败不伪造成功 -> Given 后端识别接口失败 / When 请求抛错 / Then 前端显示错误并保留当前详情，不写本地假值。

## 里程碑

- [x] M1：建立任务文档和验收标准。
- [x] M2：新增 RED 静态合同测试。
- [x] M3：实现 API 方法、详情页按钮和状态。
- [x] M4：运行前端验证并记录证据。
- [x] M5：收尾预览并提交。

## 预期验证

- `node scripts/dcc-controlled-file-product-name-recognition.test.mjs`
- `pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260605-dcc-product-name-recognition/frontend-feature-evidence.md`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。识别失败时展示错误，不在前端生成默认产品名。
- `是否从根因和长期维护角度解决`：是。前端只调用后端动作并刷新持久化结果，数据库写入由后端负责。
- `是否存在临时补丁或绕过`：否。不从文件名本地猜测，不绕过后端。

## 当前状态

completed

## Current Status

completed

## 当前证据

- RED：前端静态合同测试 3 项失败，原因是识别 API 类型/方法、详情页按钮和成功刷新处理尚未实现。
- GREEN：前端静态合同测试 3 项通过；`pnpm ts:check` 通过；frontend feature evidence 自检通过。
- REGRESSION：真实 Playwright 验证发现 `super_admin` 用户看不到“识别”按钮，当前进入权限修复闭环。
- GREEN：恢复后完成 `super_admin` 可见性修复；`node scripts/dcc-controlled-file-product-name-recognition.test.mjs` 与 `pnpm ts:check` 通过；Playwright 只读验证确认 `芋道源码/admin` 可见“识别”按钮且未执行写库点击。

## 阻塞记录

- BLOCKED：2026-06-05 用户将当前优先级切换为展厅产品附件发布与 Win7 默认软件打开方案，本轮不继续修改 DCC 产品名称识别任务源码。
- Impact：DCC 产品名称识别前端仍停留在 `super_admin` 可见性回归点，后续恢复该任务时需从该回归点继续。
- RESOLVED：2026-06-05 已恢复并修复 `super_admin` 可见性回归，当前任务完成。
