# 验证报告

## 结论

PASS。`int_main:48081` 已运行当前完整后端源码快照构建的 Jar；生产组长设备列表正式路由已加载，登录态 API 与真实页面回归均通过。

## 关键证据

- 目标 JUnit：35 项通过，0 失败、0 错误、0 跳过。
- 完整构建：30 个 Maven reactor 模块全部 SUCCESS。
- 运行 Jar：`backend-latest-20260807-1919-team-device-list.jar`，SHA-256=`8F8C8443C1F2B66613899C79FED5E97631DE7A6848A147EA5830445121982691`。
- 运行态：PID `2396`，`GET /actuator/health` 返回 `UP`，命令行确认加载上述 Jar。
- 登录态 API：`芋道源码/admin` 登录业务码 `0`；设备列表 HTTP `200`、业务码 `0`、数组响应。
- Playwright：生产组长工作台标题可见；“请求地址不存在”、设备列表加载错误和匹配控制台错误均为 `0`。
- 收尾：cleanup preview/apply 完成，任务专属构建快照与临时证据已删除，仅保留三份核心任务记录；运行 Jar 位于 `output/runtime/int_main`，未被清理。

## 风险与边界

- 构建来源是用户要求的当前完整后端工作区快照，包含快照时已有的全部未提交后端改动；本任务未修改这些源码。
- 未执行 Git stage、commit、merge、push 或 worktree 操作。
- 旧 Jar 保留，可按原参数回滚；本次未触发回滚。
