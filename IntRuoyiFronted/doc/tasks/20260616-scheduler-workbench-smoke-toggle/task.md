# 排产员工作台冒烟测试启停按钮前端

## 任务目标

- 在 `src/views/mes/pro/scheduler-workbench/index.vue` 增加冒烟测试启停按钮。
- 按后端运行状态显示“开始冒烟测试”或“结束冒烟测试”，并展示运行中、已停止或失败摘要。
- 前端不直接启动本地进程，只调用正式后端 API。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- 适用强制门禁：
  - 本机前端默认入口 `http://localhost:8081/login?redirect=/index`。
  - 长链路 E2E 前先完成最小登录路径验证。
  - 页面样式遵循 IntPP 操作台风格，按钮使用紧凑蓝色主操作，错误必须显式暴露。

## 上一任务检查

- 当前前端仓存在未跟踪任务 `20260615-frontend-build-babel-helper-missing`，其 `task.md` 标记为进行中，但该任务不属于本轮需求且无已跟踪源代码变更。
- 本轮不修改该任务文件，不把其未完成状态作为本需求的验证结果。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。API 请求失败由页面消息暴露，不静默切换为本地假状态。
- `是否从根因和长期维护角度解决`：是。复用 `schedulerWorkbench` API 模块和当前工作台工具条结构。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- BDD: 工作台空闲时显示开始按钮 -> Given 后端状态为 `IDLE` / When 用户打开排产员工作台 / Then 页面展示“开始冒烟测试”按钮和空闲状态。
- BDD: 工作台运行时显示结束按钮 -> Given 后端状态为 `RUNNING` / When 用户打开排产员工作台 / Then 页面展示“结束冒烟测试”按钮和运行信息。
- BDD: 启停失败显式提示 -> Given 后端启动或停止接口失败 / When 用户点击按钮 / Then 页面显示后端错误，不伪造成功状态。

## 里程碑

1. M1：记录任务与前端入口。`DONE`
2. M2：RED：新增静态契约测试，证明当前缺少按钮/API。`DONE`
3. M3：GREEN：补 API 类型、按钮、状态展示和错误处理。`DONE`
4. M4：REGRESSION：运行静态契约测试与类型检查。`DONE`

## 预期验证

- `node tests/e2e/mes-scheduler-workbench-smoke-toggle-static.spec.js`
- `pnpm ts:check`

## 当前状态

- 状态：COMPLETED。
- 收尾：`task-closeout-cleanup --mode preview` 通过，delete/blocked/warnings 均为 `<none>`。
