# 任务：运行控制台真实带数据提升正式服验证

## 任务目标

- 使用 Playwright 通过真实前端路径点击运行控制台的“提升正式服”按钮。
- 在弹窗中选择“带数据发布”，确认前端提交 `publishScope=with-data`。
- 发布完成后验证正式服 Website 根路径和 `/showroom` 能正常打开。

## 前序任务检查

- 前端上一任务 `20260525-runtime-control-real-promote-prod-flow` 状态为 `completed`，无阻塞。

## BDD 场景

- BDD: 带数据提升正式服成功 -> Given 运维人员在本机运行控制台打开“提升正式服”弹窗, When 选择“带数据发布”、填写原因并输入 `PROD` 后确认执行, Then 前端应提交 `promote-prod` 请求，参数包含 `publishScope=with-data`，并展示可查看日志。
- BDD: 带数据发布后 Website 正常打开 -> Given 带数据提升正式服完成, When 浏览器访问正式服 Website 根路径和 `/showroom`, Then 页面应成功加载且不出现前端运行错误。
- BDD: 操作日志可追溯 -> Given 带数据提升正式服动作完成, When 查看运行控制台操作日志, Then 最近操作应显示“提升正式服”“带数据发布”和成功状态。

## 里程碑

- [x] M1：建立任务文档并确认前序任务状态。
- [x] M2：扩展真实 E2E 支持带数据提升正式服。
- [x] M3：记录发布前页面和环境状态。
- [x] M4：用 Playwright 完整执行带数据提升正式服。
- [x] M5：验证正式服 Website、展厅和操作日志。

## 预期验证

- Playwright 真实前端操作“提升正式服”。
- 点击“带数据发布”单选项后提交。
- 请求参数为 `publishScope=with-data`。
- 正式服 Website 根路径和 `/showroom` 可通过真实浏览器打开。
- 最近操作表显示本次提升正式服成功。

## 当前状态

- 状态：completed
- 已完成：
  - 已扩展真实提升正式服 E2E，支持 `RUNTIME_CONTROL_REAL_PROMOTE_SCOPE=with-data`。
  - 已增加双重显式开关：`RUNTIME_CONTROL_ALLOW_REAL_PROMOTE_PROD=1` 与 `RUNTIME_CONTROL_ALLOW_REAL_PROMOTE_WITH_DATA=1`，避免误触正式服数据覆盖。
  - 已通过 Playwright 从真实前端路径选择“提升正式服”与“带数据发布”，填写原因并输入 `PROD`。
  - 已验证成功操作 `5806c1d8-ebd9-405e-85cf-f37b322397c2` 状态为 `succeeded`，审计参数为 `publishScope=with-data`。
  - 已验证正式服 Website 根路径和 `/showroom` 均可通过真实浏览器打开。
- 阻塞：暂无。
