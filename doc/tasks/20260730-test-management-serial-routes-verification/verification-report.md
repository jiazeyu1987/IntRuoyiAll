# Verification Report

## Verdict

FAIL：测试管理下的 3 条正式串行路线在当前测试租户中都不能完整跑完。

## Scope

- 入口：`http://127.0.0.1:8081`
- 后端：`http://127.0.0.1:48081`
- 租户/用户标签：`芋道源码/admin`
- 真实路径：登录 -> `系统管理 > 测试管理` -> 串行路线筛选 -> 逐行选择完整节点串 -> `顺序执行` -> `系统管理 > 测试记录` -> `查看结果`
- Runner：最终会话 `94`

## Preflight

- 前端 HTTP `200`，后端 health `UP`。
- 3 条正式节点串存在且节点连续：
  - `工艺路线节点闭环`：4 个节点
  - `批记录节点闭环`：6 个节点
  - `智能排产节点闭环`：4 个节点
- 每个节点均为 `ENABLE`、`SEQUENTIAL`、`parallelSafe=false`，包含测试方法和检查点。
- Runner 最终为 `ONLINE`，能力齐备。

## Results

| 串行路线 | 批次 | 结果 | 首节点结果 | 后续节点 |
| --- | ---: | --- | --- | --- |
| 工艺路线节点闭环 | 34 | FAIL | `工艺路线节点：基础维护` 的 Codex 子进程 `exit 1` | 3 个节点均 `BLOCKED`，未领取 |
| 批记录节点闭环 | 35 | FAIL | `批记录节点：解析` 的 Codex 子进程 `exit 1` | 5 个节点均 `BLOCKED`，未领取 |
| 智能排产节点闭环 | 36 | FAIL | `智能排产节点：工单入池` 在 `600000ms` 后超时 | 3 个节点均 `BLOCKED`，未领取 |

工艺路线和批记录首节点回写的 stderr 前段包含远程插件目录要求 ChatGPT 认证且不支持 API key 认证，以及 `plan_tool`、`rmcp_client`、`streamable_shell`、`view_image_tool` 等未知 feature key 警告。由于回写内容被截断，本报告不把这些警告断言为唯一根因，只确认 Codex 子进程实际以退出码 `1` 结束。

## Serial Semantics

- PASS：每条节点串均通过真实页面完整选择后发起顺序执行。
- PASS：只有首节点被 Runner 领取。
- PASS：首节点未通过后，所有后续节点自动标记 `BLOCKED`，没有被领取。
- PASS：批次终态后 Runner `currentRunningCount=0`，heartbeat age `0s < 60s`。
- PASS：批次 `34`、`35`、`36` 均在真实“测试记录”页面核对最终状态。

## Interrupted Attempt

首次批次 `33` 执行期间，本机后端运行态被并行任务重启。Runner session `93` 变为 `STALE`，批次悬挂在 `RUNNING`。本任务通过真实“测试记录”页面取消批次，批次及其 4 个节点均进入 `CANCELED`，未遗留活动执行项。

## Non-Blocking Observation

页面加载期间外部头像 URL 返回 HTTP `502`。测试管理、节点串、Runner、执行和记录接口均正常返回，该资源错误不属于 3 条串行路线失败原因。

## Final State

- 活动测试批次：无。
- Runner session `94`：`ONLINE`。
- Runner 当前运行计数：`0`。
- 结论：不能回答“都可以完整跑完”；当前 3 条路线均为失败。
