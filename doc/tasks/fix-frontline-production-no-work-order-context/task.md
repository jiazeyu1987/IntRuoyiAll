# 任务：一线生产取消工单匹配上下文

## Task Goal

按用户最新口径修正一线生产正式提交链路：一线生产不需要匹配任何工单，选择员工/运行态加载不得因 activeOrder、workOrder 或 task 缺失而失败。

## Milestones

- [x] M1：定位运行态和正式提交中工单/任务上下文依赖。
- [x] M2：补充 RED 回归测试，证明无活跃订单/无工单时一线运行态仍可加载。
- [x] M3：实施最小修复，不引入 fallback、默认成功或吞异常。
- [ ] M4：运行目标 GREEN 与相邻正式提交/员工切换回归。
- [x] M5：整理验证报告和证据文件。

## Expected Verification

- 后端回归覆盖一线生产运行态不再查询或要求 activeOrder/workOrder/task/recordbook。
- 相邻员工切换测试通过，选择员工不再触发工单上下文错误。
- 正式提交服务相邻测试通过或记录需要按新业务口径进一步调整的明确阻塞。
- `git diff --check` 通过。

## Applicable Experience Gates

- 一线生产正式提交门禁：正式签名主体是选择员工，不是登录账号；缺少真正必需的员工/工序/签名信息必须 fail fast。
- 本任务口径覆盖旧门禁中的工单匹配要求：一线生产不需要匹配任何工单。

## Current Status

blocked - 实现与核心验证已完成，但相邻 JUnit `MesP0FrontlineSubmitIdempotencyTest,MesFrontlineEmployeeSwitchServiceTest` 仍被同模块并发 Maven 编译占用共享 `target` 阻塞；为避免破坏其他任务产物，未清理或强杀进程。

已按用户口径“一线生产不需要匹配任何工单”移除正式一线生产的 activeOrder/workOrder/task/recordbook 前置要求。核心后端回归、前端静态合同、迁移策略门禁和 scoped diff 检查已通过；剩余阻塞仅为并发构建导致的相邻 JUnit 补跑条件未满足。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。目标是移除不应存在的工单匹配前置条件，而非以默认工单绕过。
- `是否存在临时补丁或绕过`：否。
