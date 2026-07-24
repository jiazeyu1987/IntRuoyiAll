# 任务：本地管理端 48081 接口拒连排查（后端）

- Task ID: `20260701-local-admin-api-48081-connection-refused`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-07-01`
- Current Status: `completed`

## Task Goal

核对本地后端实际监听端口、启动脚本和健康检查入口，确认 `48081` 是否为项目约定本地端口，并在需要时恢复对应本地运行态。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260701-showroom-product-excel-audio-keyword-roundtrip\task.md`
- 状态：`blocked`
- 处理说明：该任务已因当前本地联调入口不可用而暂停，不阻塞本轮后端运行态排查。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。优先核对后端真实监听端口与启动契约，不通过改前端去掩盖后端本地运行态缺失。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 本地后端监听契约可被健康检查验证 -> Given 本地后端按项目约定启动 / When 检查 48081 健康接口 / Then 返回可用状态而不是连接拒绝。`
- `BDD: 后端未启动或端口漂移时会被明确识别 -> Given 本地后端未监听 48081 / When 执行端口与配置核对 / Then 能明确指出缺失前置条件或错误端口。`

## Milestones

1. M1：建立后端任务台账并确认本地监听端口来源。`completed`
2. M2：补 RED 健康检查或端口断言，确认拒连根因。`completed`
3. M3：恢复本地运行态或修复端口契约，并完成 GREEN 验证。`completed`

## Expected Verification

- `Invoke-WebRequest http://127.0.0.1:48081/actuator/health -UseBasicParsing`
- `rg -n "48081|server.port|actuator/health" D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`

## Current Blockers

- 暂无。

## Final Verification Result

- 后端本地端口契约没有漂移：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-server\src\main\resources\application-local.yaml` 与 `script/deploy/restart-int-ruoyi-local.ps1` 均以 `48081` 作为本机标准端口。
- 当前后端运行正常：
  - `Get-NetTCPConnection -LocalPort 48081 -State Listen` 显示 Java 进程监听；
  - `Invoke-WebRequest http://127.0.0.1:48081/actuator/health` 返回 `{"status":"UP"}`；
  - `Invoke-WebRequest http://127.0.0.1:48081/admin-api/system/dict-data/simple-list` 返回 HTTP `200`。
- 本次报错的直接原因是后端刚在 `2026-07-01 13:46:38` 重启，而 `yudao-server.log` 显示直到 `2026-07-01 13:47:36.961` 才真正监听 `48081`，`2026-07-01 13:47:37.050` 才完成启动；前端若在此窗口访问 `http://localhost:48081/admin-api/...`，会看到 `ERR_CONNECTION_REFUSED`。
- `yudao-server.log` 在 `2026-07-01 13:55:52` 已成功处理 `system/dict-data/simple-list` 与 `system/auth/get-permission-info`，说明当前本地后端链路已恢复，无需改业务代码。
