# 任务：DCC 受控预览返回来源页

## 任务目标

将 DCC 受控文件预览页左上角按钮改为“返回”，点击后返回进入预览页之前的来源页面，而不是固定回详情页。支持至少 `受控浏览`、`我的文件`、`审批任务`、`受控文件详情` 四类入口，并保留来源页 query。

## Previous Task Check

- 上一个前端任务：`doc/tasks/20260607-dcc-onlyoffice-anonymous-name-prompt/task.md`
- 状态：`completed`
- 处理：上一任务已完成；本任务只修改 DCC 预览路由构造、返回逻辑、静态测试和任务记录。

## BDD 场景

- BDD: 受控浏览进入预览后返回浏览页 -> Given 用户从受控浏览页打开预览 / When 点击预览页左上角“返回” / Then 页面返回受控浏览，并保留当时目录/分类/状态 query。
- BDD: 我的文件进入预览后返回我的文件 -> Given 用户从我的文件页打开预览 / When 点击“返回” / Then 页面返回我的文件页。
- BDD: 审批任务进入预览后返回审批任务 -> Given 用户从审批任务页打开预览 / When 点击“返回” / Then 页面返回审批任务页。
- BDD: 详情进入预览后返回详情 -> Given 用户从受控文件详情打开预览 / When 点击“返回” / Then 页面返回当前文件详情页。
- BDD: 缺少或非法来源时回详情 -> Given 用户直接访问预览 URL 或 `returnTo` 非法 / When 点击“返回” / Then 页面回退到当前文件详情页，不跳外部地址。

## Milestones

- [x] M1：建立任务文档并确认上一前端任务已完成。
- [x] M2：新增 RED 静态契约测试。
- [x] M3：实现预览 `returnTo` 路由参数和返回逻辑。
- [x] M4：运行静态、类型和真实 E2E 验证。
- [x] M5：更新证据并完成收尾。

## Expected Verification

- `node scripts/dcc-controlled-file-preview-detail-panel.test.mjs`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`
- 真实 Playwright：从 `browser`、`mine`、`approval-tasks`、`detail` 四类入口进入预览后点击“返回”，应回到对应来源页。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。非法来源只回当前详情页，不静默跳外部或未知页面。
- `是否从根因和长期维护角度解决`：是。统一把来源 `route.fullPath` 编进预览 URL，由预览页统一解析回跳。
- `是否存在临时补丁或绕过`：否。

## 当前状态

completed

## 当前证据

- RED：`node scripts/dcc-controlled-file-preview-detail-panel.test.mjs` -> FAIL，预览 URL 未传 `returnTo`，返回按钮仍按旧逻辑回详情页。
- GREEN：`node scripts/dcc-controlled-file-preview-detail-panel.test.mjs` -> PASS，8 tests。
- GREEN：`$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- GREEN：`node doc/tasks/20260607-dcc-preview-return-to-origin/verify-preview-return-to-origin.e2e.mjs` -> PASS，浏览页回到 `/dcc/controlled-file/browser?directoryId=906200&categoryId=906101&status=ACTIVE`，我的文件回到 `/dcc/controlled-file/mine`，审批任务回到 `/dcc/controlled-file/approval-tasks`，详情页回到 `/dcc/controlled-file/detail/2054545668044046252`。
