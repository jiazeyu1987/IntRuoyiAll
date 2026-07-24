# 任务：DCC 受控预览右侧详情面板

## 任务目标

在 DCC 受控文件 `viewer=1` 预览页增加左右布局：左侧继续显示受控文件预览，右侧显示当前受控文件基础信息；文控角色 `doc_control` 可从右侧点击“修改”并复用现有基础信息弹窗保存。前端权限入口严格限定为 `doc_control`，不再把 `super_admin` 当作文控。

## Previous Task Check

- 上一个前端任务：`doc/tasks/20260607-runtime-console-current-release-refresh/task.md`
- 状态：`completed`
- 处理：上一任务已完成；本任务只修改 DCC 受控文件详情/预览、权限静态测试和任务记录。当前仓库存在上一任务未跟踪 `artifacts/`，本任务不得暂存或提交。

## BDD 场景

- BDD: 预览页显示右侧详情 -> Given 用户打开 `/dcc/controlled-file/detail/:id?viewer=1&from=detail` / When 受控预览加载 / Then 页面左侧显示文件预览，右侧显示文件类别、文件名称、产品名称、受控目录、培训要求等基础信息。
- BDD: 文控可在预览详情修改 -> Given 当前登录用户角色包含 `doc_control` / When 在预览页右侧详情点击“修改”并保存 / Then 前端调用现有 `PUT /dcc/controlled-files/{id}/metadata`，保存后刷新预览页右侧详情。
- BDD: 非文控和超管不可修改 -> Given 当前登录用户角色不包含 `doc_control`，即使包含 `super_admin` / When 打开详情页或预览页 / Then 不显示“修改基础信息”“修改”和产品名称“识别”入口。
- BDD: 保存失败明确暴露 -> Given 后端拒绝基础信息保存 / When 文控保存 / Then 弹窗保留错误，不关闭，不伪造成功。

## Milestones

- [x] M1：确认上一前端任务 completed，建立本任务文档。
- [x] M2：新增 RED 静态契约测试。
- [x] M3：抽出基础信息面板并实现预览左右布局。
- [x] M4：收紧前端文控入口权限。
- [x] M5：运行静态、类型和真实 Playwright 验证。
- [x] M6：记录证据、收尾预览并提交本任务前端改动。

## Expected Verification

- RED/GREEN：`node scripts/dcc-controlled-file-preview-detail-panel.test.mjs`
- GREEN：`node scripts/dcc-controlled-file-metadata-edit.test.mjs`
- GREEN：`pnpm ts:check`
- GREEN：真实 Playwright 本机 `http://localhost:8081`，测试租户 `aoteman` 打开 `2054545668044046252?viewer=1&from=detail`，验证右侧详情、修改保存和恢复。
- GREEN：真实 Playwright 本机 `芋道源码/admin` 验证右侧详情可见但无修改入口。
- GREEN：frontend feature evidence validator。
- GREEN：task-closeout-cleanup 预览。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。预览、详情或保存失败时沿用现有错误暴露，不伪造详情或保存成功。
- `是否从根因和长期维护角度解决`：是。抽出共享基础信息面板，普通详情页和预览详情共用字段展示，权限入口统一为 `doc_control`。
- `是否存在临时补丁或绕过`：否。不新增测试专用 UI，不临时切换账号或租户，不修改受保护远程环境。

## 当前状态

completed

## 当前证据

- RED：`node scripts/dcc-controlled-file-preview-detail-panel.test.mjs` -> FAIL，预览页缺少 split 布局/右侧详情面板/共享基础信息组件，浏览和详情页仍残留 `super_admin` 编辑入口。
- GREEN：`node scripts/dcc-controlled-file-preview-detail-panel.test.mjs` -> PASS，4 tests。
- GREEN：`node scripts/dcc-controlled-file-metadata-edit.test.mjs` -> PASS，4 tests。
- RED：`pnpm ts:check` -> FAIL，Node 默认 4GB 堆 OOM，未出现类型错误输出。
- GREEN：`$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- GREEN：`node doc/tasks/20260607-dcc-preview-detail-panel/verify-preview-detail-panel.e2e.mjs` -> PASS。测试租户 `aoteman` 原权限接口缺少 `doc_control`，脚本通过测试租户正式角色分配接口临时补齐、复登后完成右侧详情修改保存并恢复产品名称；`芋道源码/admin` 只读打开同页，右侧详情可见且修改按钮数量为 0。
- GREEN：测试租户角色残留清理复核 -> PASS，`aoteman` 当前角色为 `showroom_publicity,tenant_admin`。
- GREEN：frontend-feature evidence validator -> PASS。
- GREEN：task-closeout-cleanup preview -> PASS，delete none，blocked none。

## Cleanup Keep

- `doc/tasks/20260607-dcc-preview-detail-panel/task.md`
- `doc/tasks/20260607-dcc-preview-detail-panel/execution-log.md`
- `doc/tasks/20260607-dcc-preview-detail-panel/frontend-feature-evidence.md`
- `doc/tasks/20260607-dcc-preview-detail-panel/verify-preview-detail-panel.e2e.mjs`
