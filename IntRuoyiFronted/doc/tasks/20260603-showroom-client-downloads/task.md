# 任务：公司信息页面增加客户端下载按钮

## 任务目标

在展厅-公司信息页面顶部操作区增加两个下载按钮：

- `下载安卓客户端`
- `下载电脑桌面端`

按钮必须指向后端展厅模块下载接口，不使用前端 public 目录或部署后手工复制文件。

## Previous Task Check

- 上一个前端任务：`doc/tasks/20260603-runtime-control-hide-foolproof-error/task.md`
- 状态：`completed`
- 当前前端仓库已有 unrelated dirty changes；本任务只触碰展厅公司信息页面、展厅 API 契约测试与任务文档。

## BDD 场景

- BDD: 公司信息操作区显示安卓下载按钮 -> Given 用户进入 `/showroom/company` / When 公司信息工作台加载 / Then 顶部操作区显示 `下载安卓客户端` 按钮并指向安卓下载接口。
- BDD: 公司信息操作区显示桌面端下载按钮 -> Given 用户进入 `/showroom/company` / When 公司信息工作台加载 / Then 顶部操作区显示 `下载电脑桌面端` 按钮并指向桌面端下载接口。

## Milestones

- [x] M1：建立前端任务文档并确认上一任务已完成。
- [x] M2：新增 RED 静态测试覆盖按钮文案和下载 URL。
- [x] M3：最小修改公司信息页面操作区与 API 常量。
- [x] M4：运行前端目标测试、类型检查或记录缺失前置条件。
- [x] M5：记录 GREEN 证据并完成收尾。

## Expected Verification

- RED：`node tests/e2e/showroom-company-client-downloads-static.spec.js` 先失败。
- GREEN：同一命令通过。
- GREEN：`pnpm ts:check` 或现有目标类型检查通过。

## 当前状态

completed

## 已完成工作

- `ShowroomAdminApi` 新增安卓与 Win7 桌面端下载 URL、版本 1.0 文件名常量和下载方法。
- `CompanyWorkbench.vue` 顶部操作区新增 `下载安卓客户端` 与 `下载电脑桌面端` 按钮，点击后保存后端返回的 Blob。

## 验证结果

- GREEN：`node tests\e2e\showroom-company-client-downloads-static.spec.js` -> PASS。
- GREEN：`$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。
- GREEN：frontend evidence validator -> PASS。
- GREEN：收尾清理预览 -> PASS，delete `<none>`、blocked `<none>`、warnings `<none>`。
- E2E：Playwright 登录本机 `/showroom/company` -> PASS，页面可见两个客户端下载按钮。

## 剩余阻塞

- 无。

## Cleanup Keep

- `doc/tasks/20260603-showroom-client-downloads/frontend-feature-evidence.md`
- `tests/e2e/showroom-company-client-downloads-static.spec.js`
