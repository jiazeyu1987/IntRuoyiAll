# 任务：芋道源码租户模拟报工归属与排产进度核验

## 任务目标

在用户本次明确授权前提下，于本机 `http://localhost:8081` 的 `芋道源码/admin` 身份下进入报工页签，执行一轮真实“模拟报工 -> 选择归属”写入操作，并核验归属成功后排产工单进度是否正确更新。

## 当前状态

completed / 已完成

## Current Status

completed

## 上一相关任务检查

- 上一前端相关任务 `20260625-mes-feedback-attribution-row-fill-fix` 已完成，允许继续本任务。

## 经验门禁

- 来源：`docs/experience-index.md`
- 命中文档：
  - `docs/login-access.md`
- 适用强制门禁：
  - 本次用户已在当前任务明确授权对 `芋道源码/admin` 做报工及相关数据写入，允许以 `tenant-id=1` 和 `admin` 身份执行真实归属。
  - 长链路真实 E2E 前必须先验证“登录到目标页面”的最小路径。
  - 写入前需在执行日志记录 `GREEN: experience-preflight -> PASS`。
  - 发现登录失败、租户错误、菜单不可见或写入报错时必须 fail fast，并记录实际失败位置与影响。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。只走现有真实页面和真实接口，不增加 mock、绕过或备用写入路径。
- `是否从根因和长期维护角度解决`：是。本任务先复现真实链路并核验进度更新结果，若失败则定位真实根因。
- `是否存在临时补丁或绕过`：否。

## 用户授权记录

- 用户于当前线程明确授权：`本次授权你在芋道源码里对报工以及相关数据进行修改`。

## BDD 场景

- `BDD: 芋道源码租户可完成模拟报工归属 -> Given admin 已登录芋道源码租户报工页签 / When 用户执行一轮模拟报工并选择订单工序归属 / Then 页面归属成功且真实写入发生。`
- `BDD: 归属后排产工单进度正确更新 -> Given 某排产工单工序被成功归属正式报工 / When 归属成功后查看排产工单相关进度 / Then 已完成、剩余或相关进度字段与本次报工数量一致更新。`

## Milestones

1. M1：记录授权、门禁和执行日志前置。`DONE`
2. M2：用真实浏览器登录 `芋道源码/admin` 并进入报工页签。`DONE`
3. M3：执行一轮模拟报工归属并捕获页面/接口证据。`DONE`
4. M4：核验排产工单进度更新并记录最终结果。`DONE`

## Expected Verification

- Playwright 真实打开登录页并以 `芋道源码/admin` 登录成功。
- 真实模拟报工归属成功，页面或接口返回成功结果。
- 归属成功后，排产工单进度字段发生符合本次报工数量的更新。

## 根因与修复结果

- 根因：模拟报工归属会创建并提交正式报工，此时报工状态为 `APPROVING`；排产工单进度同步原先只统计 `FINISHED`，导致归属成功后工序 `reportedQuantity` 不更新。
- 修复：排产工单进度同步将已归属但尚在审批中的 `APPROVING` 与待检 `UNCHECK` 一并计入已报工数量，保持归属后排产工序进度立即可见。
- 约束：未引入 fallback、降级或异常吞并；仍然使用真实页面、真实接口和真实租户数据路径。

## 最终验证

- 后端定向回归：`mvn -pl yudao-module-mes -Dtest="MesProScheduleOrderProgressServiceTest,MesProScheduleOrderFourRiskContractTest" test` -> PASS，8 个测试通过。
- 本地运行包验证：生成仅替换排产进度同步 class 且保持 Spring Boot 内嵌 jar `ZIP_STORED` 的本机验证包，`http://127.0.0.1:48081/actuator/health` -> `UP`。
- 真实 E2E：`node doc/tasks/20260625-admin-feedback-attribution-progress-verification/verify-admin-feedback-attribution-progress.e2e.js` -> PASS。`芋道源码/admin` 下模拟报工并选择归属，`importRecordId=454`，`feedbackId=263`，本次归属数量 `99`，目标 `scheduleOrderId=13`、`scheduleOrderProcessId=296`，工序 `reportedQuantity: 0 -> 99`，`remainingQuantity: 99 -> 0`。

## Cleanup Keep

- `doc/tasks/20260625-admin-feedback-attribution-progress-verification/task.md`
- `doc/tasks/20260625-admin-feedback-attribution-progress-verification/execution-log.md`
