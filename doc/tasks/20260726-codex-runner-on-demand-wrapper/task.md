# 20260726 Codex Runner 按需包装层

## Task Goal

将“测试管理”执行链路从依赖常驻在线 Runner，调整为正式的按需 Runner 包装层：点击执行时由受控 Runner 启动/注册/领取任务，再调用 Codex CLI，并结构化回写测试方法项、目标项、失败原因和运行监控状态。

## Milestones

- [x] 梳理现有测试管理、Runner、监控接口和前端执行入口
- [x] 设计并记录按需 Runner 生命周期与失败行为
- [x] 先补后端/前端 RED 回归测试
- [x] 实现后端按需 Runner 启动与状态契约
- [x] 实现前端执行入口、运行监控页签和错误展示联动
- [x] 完成 GREEN/REGRESSION 验证与证据归档

## Expected Verification

- Maven 定向测试覆盖 Runner 按需启动、未配置启动器 fail-fast、运行状态回写失败原因
- 前端静态合同覆盖执行入口调用按需启动接口、运行监控页签显示任务状态、红色目标可查看失败原因
- Runner 脚本语法检查通过
- 不用 mock、默认成功或静默降级替代真实 Runner 可用性判断

## Current Status

ready_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；缺少 Codex CLI、token、启动脚本或后端连接时必须 fail-fast 并暴露原因。
- `是否从根因和长期维护角度解决`：是；目标是建立按需 Runner 生命周期与结构化监控，而不是重启旧 Runner 进程。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

### Codex Runner 自动测试门禁

- Trigger: 修改“系统管理 > 测试管理”、Codex Runner、自然语言测试方法、由 Codex 调用 Playwright 的自动测试流程。
- Preflight check: 真实执行前确认本机前后端入口、测试租户、Runner token、Codex CLI、Playwright 浏览器、Runner 本地凭据映射；不得把进程存在当作在线证明。
- Blocker: Runner token 与后端运行态不一致、注册失败、heartbeat 过期、并行执行包含 `parallelSafe=false`、失败检查点没有差异描述时必须停止。
- Verification: 记录 Runner 注册/领取/执行期心跳/空闲心跳/回写命令、页面执行入口、检查点结果、失败原因和最终 UI 状态。
- Forbidden action: 禁止用 API-only、mock 截图、默认成功、Runner 离线跳过或顺序执行降级当作通过。
- Evidence: `docs/e2e-rules.md#codex-runner-自动测试门禁`

### Codex Runner 目标测试项存在性门禁

- Trigger: 用户指定运行测试管理中的某个测试项，或要求 Runner 领取并执行单个自然语言测试项。
- Preflight check: 点击执行前按真实测试管理页面可见业务名称搜索目标项，页面未命中再只读核对 `system_codex_test_case`。
- Blocker: 目标不存在、被删除、禁用、租户不匹配时必须停止。
- Verification: 证据包含页面搜索总数、目标租户/用户标签、是否创建 executionId。
- Forbidden action: 禁止模糊关键词误选其它测试项、API-only 启动替代页面行级执行点击、缺少方法和目标时临时造数。
- Evidence: `docs/e2e-rules.md#codex-runner-目标测试项存在性门禁`
