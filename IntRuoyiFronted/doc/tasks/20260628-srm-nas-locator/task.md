# 任务：SRM NAS定位 前端页面实现

## 任务目标

- 新增 `src/views/srm/nas-locator/index.vue` 与 `src/api/srm/nas-locator/index.ts`。
- 为 SRM `NAS定位` 提供状态区、搜索区、结果表格、刷新轮询与下载交互。
- 页面保持 IntPP 风格的紧凑运维工作台，不新增静态路由表项。

## 当前状态

COMPLETED

## Current Status

Completed

## 上一任务检查

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260628-schedule-route-missing-scheduling-content\task.md`
- 状态：`COMPLETED`
- 处理说明：上一前端任务已完成；本任务已闭环完成。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
  - `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- 适用强制门禁：
  - 页面布局必须遵循 IntPP 列表与工具栏样式，不做营销式视觉改造。
  - 真实 Playwright E2E 前必须先补 `GREEN: experience-preflight -> PASS` 并通过官方登录预检。
  - 前端不得吞掉后端真实错误；没有成功快照、刷新失败、下载失败都必须给用户明确反馈。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。前端不自行吞掉后端错误，也不在没有成功快照时渲染假空列表；readable-only 范围变化由正式后端接口承接，前端只如实展示状态和结果。
- `是否从根因和长期维护角度解决`：是。前端只消费正式后端快照接口与下载接口，补齐 3 秒轮询、状态区、可读列表和真实下载链路，不增加静态路由兜底。
- `是否存在临时补丁或绕过`：否。页面最终行为与后端正式范围保持一致。

## BDD 场景

- `BDD: 页面初始状态区展示共享范围和最近刷新摘要 -> Given 用户进入 /srm/nas-locator / When status 接口返回当前摘要 / Then 页面顶部应展示共享范围、最近成功刷新时间、目录数、文件数与最新任务状态。`
- `BDD: RUNNING 刷新期间页面自动轮询状态 -> Given 用户点击刷新且后端任务进入 RUNNING / When 页面处于运行中 / Then 前端应每 3 秒轮询一次 status，并在成功后自动重载当前列表。`
- `BDD: 用户搜索文件名时看到清晰结果表 -> Given 已有成功快照 / When 用户空关键字或输入文件名关键字搜索 / Then 页面按文件名、NAS目录、完整相对路径、修改时间、大小和下载列展示结果。`
- `BDD: 用户下载文件时收到真实附件 -> Given 某条搜索结果可下载 / When 用户点击下载 / Then 前端应调用正式下载接口并触发浏览器附件下载。`

## 里程碑

1. M1：补前端任务文档、执行日志和 evidence 骨架。`COMPLETED`
2. M2：先写静态 RED 测试，锁定 API、页面结构和权限片段。`COMPLETED`
3. M3：实现 API、页面、轮询和下载交互。`COMPLETED`
4. M4：跑静态回归与真实 Playwright E2E，回填证据。`COMPLETED`

## 预期验证

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\srm\nas-locator-static.spec.js`
- `node D:\ProjectPackage\Int\IntRuoyi\scripts\preflight\login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --password 111111 --target-path /srm/nas-locator --target-text NAS定位 --timeout 90000`
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\srm\nas-locator-real-flow.e2e.js`

## 当前结论

- 页面已交付状态区、搜索区、刷新按钮、下载按钮、结果表格和分页，路径列支持换行，文件名强调显示。
- `RUNNING` 时前端每 3 秒轮询一次 `status`，在刷新成功后会自动重载当前列表。
- 前端不会吞掉后端真实错误；没有成功快照、刷新失败、下载失败都会明确反馈。
- 真实 E2E 已通过，确认测试租户可从 `/srm/nas-locator` 进入页面、刷新成功、执行搜索并下载真实附件。
- 真实下载文件名已恢复可读，E2E 断言已改为读取整格文件名，不再因空格截断误判。

## 最终验证结果

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\srm\nas-locator-static.spec.js` -> PASS
- `node D:\ProjectPackage\Int\IntRuoyi\scripts\preflight\login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --password 111111 --target-path /srm/nas-locator --target-text NAS定位 --timeout 90000` -> PASS
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\srm\nas-locator-real-flow.e2e.js` -> PASS
- 真实截图与下载产物 -> PASS，已生成页面截图、刷新成功截图和真实附件文件

## 当前阻塞

- 无。当前范围内的前端页面、轮询、搜索和真实下载验收均已完成。
