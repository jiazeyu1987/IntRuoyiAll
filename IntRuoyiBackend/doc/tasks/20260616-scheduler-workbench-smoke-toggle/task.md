# 排产员工作台冒烟测试启停按钮后端

## 任务目标

- 在 MES 排产员工作台后端增加冒烟测试状态查询、启动、停止能力。
- 进程控制支持 Windows 与 Linux，启动当前前端已有 `e2e:mes:smart-scheduling-smoke` 真实脚本。
- 缺少必需配置、工作目录、脚本或命令时 fail fast。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
- 适用强制门禁：
  - 真实 E2E 默认本机 `http://localhost:8081` 与测试租户 `测试租户/aoteman`。
  - 长链路 E2E 前必须记录 `GREEN: experience-preflight -> PASS`；前置缺失时记录 `BLOCKER` 并停止。
  - 不访问测试服或正式服，除非用户明确授权。

## 上一任务检查

- 最近相关后端任务：`ruoyi-vue-pro/doc/tasks/20260611-kingdee-production-order-create/task.md`
- 状态：COMPLETED。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。启动目录、脚本名、npm 可执行命令、进程启动/停止失败均直接抛出业务错误。
- `是否从根因和长期维护角度解决`：是。新增正式服务封装进程控制，控制器只负责权限与 API。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- BDD: 空闲时启动冒烟测试 -> Given 当前没有冒烟测试进程 / When 调用启动接口 / Then 后端按当前操作系统启动 npm 脚本并返回运行状态。
- BDD: 运行时拒绝重复启动 -> Given 冒烟测试进程仍在运行 / When 再次调用启动接口 / Then 后端 fail fast 返回“冒烟测试正在运行”。
- BDD: 运行时停止冒烟测试 -> Given 冒烟测试进程仍在运行 / When 调用停止接口 / Then 后端终止该进程和子进程并返回停止状态。
- BDD: 配置缺失时不伪造成功 -> Given 冒烟测试工作目录或脚本配置缺失 / When 调用启动接口 / Then 后端返回明确错误，不启动任何替代命令。

## 里程碑

1. M1：记录任务与后端接口边界。`DONE`
2. M2：RED：新增服务单测，证明当前缺少状态与启停服务。`DONE`
3. M3：GREEN：实现 VO、服务、控制器、跨平台命令与进程树停止。`DONE`
4. M4：REGRESSION：运行 MES 模块目标单测。`DONE`

## 预期验证

- `mvn -pl yudao-module-mes -Dtest=MesProSchedulerWorkbenchSmokeTestServiceImplTest test`

## 当前状态

- 状态：COMPLETED。
- 收尾：`task-closeout-cleanup --mode preview` 通过，delete/blocked/warnings 均为 `<none>`。
