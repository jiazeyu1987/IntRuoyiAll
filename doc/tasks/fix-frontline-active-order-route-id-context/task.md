# 任务：一线运行态按正式任务解析活跃订单

## Task Goal

修复一线生产“选择员工”后提示 `一线提交身份上下文缺少必填字段：productionSubmitContext.activeOrder routeId=...` 的问题。后端运行态不得仅按“组长 + 路线”要求活跃订单唯一；当同一路线存在多个活跃订单时，必须继续按正式生产任务的路线、工序和工作站唯一解析目标工单。

## Milestones

- [x] M1：定位截图报错对应的后端运行态配置链路。
- [x] M2：补充 RED 后端回归测试，证明同一路线多个活跃订单时必须按正式任务唯一解析。
- [x] M3：实施最小修复，不引入 fallback、默认成功或吞异常。
- [x] M4：运行目标 GREEN 与相邻一线正式提交回归。
- [x] M5：整理验证报告、经验记录和收尾。

## Expected Verification

- 后端回归覆盖同一组长同一路线存在多个活跃订单，但只有一个工单任务匹配当前路线、工序和工作站时，运行态配置必须返回匹配工单。
- 后端回归覆盖缺少唯一正式任务时继续 fail fast，不使用默认工单或任取第一条活跃订单。
- `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineRuntimeConfigServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 通过。
- 相邻一线正式提交/员工切换测试通过或记录明确阻塞。
- `git diff --check` 通过。

## Applicable Experience Gates

- 一线生产正式提交门禁：正式上下文来自运行态 `productionSubmitContext`，不得用 URL query 或前端猜测补身份；缺正式上下文必须 fail fast。
- 前端选择弹框即时反馈门禁：员工选择可以即时关闭，但不得破坏正式运行态上下文、签名或提交状态；失败必须暴露正式错误。
- 前端写入/提交断言门禁：严格提交断言只能检查正式结构字段，不能把已有正式字段误判为缺失。

## Current Status

completed

实现、验证、经验沉淀和 task-closeout-cleanup apply 均已完成；未执行 Git 提交/推送。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。目标是修正正式 active order 路线身份映射/校验，不绕过提交断言。
- `是否存在临时补丁或绕过`：否。

## Cleanup Keep

- doc/tasks/fix-frontline-active-order-route-id-context/bug-regression-evidence.md
