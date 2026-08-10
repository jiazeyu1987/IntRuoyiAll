# 一线生产移除设备/工作站上下文不一致限制

## Task Goal

按用户最新要求移除一线生产正式提交中的“提交设备/工作站上下文与授权工序不一致”限制，不再因 submittedDeviceId/submittedWorkstationId 与授权候选 expectedDeviceId/expectedWorkstationId 不一致抛出该错误。

## Milestones

- [x] M1: 读取后端/API 规则并确认当前仍保留工位比较。
- [x] M2: 新增 RED 测试覆盖设备和工位都不一致时不应抛上下文不一致。
- [x] M3: 移除授权服务中的设备/工作站上下文不一致判断。
- [x] M4: 运行定向 Maven 测试和证据校验。
- [x] M5: 更新文档、经验索引和收尾记录。

## Expected Verification

- `MesFrontlineSubmitAuthorizationServiceImpl` 不再引用或抛出 `PRO_FRONTLINE_SUBMIT_DEVICE_CONTEXT_MISMATCH`。
- 同路线、路线工序、MES 工序、实际员工、签名员工和模板合法时，不因提交设备/工作站与授权候选设备/工作站不同而失败。
- 仍保留请求必填、签名员工一致、授权工序存在、实际员工属于团队、模板一致等校验。

## Current Status

completed

已移除一线生产正式提交授权中的设备/工作站上下文候选比较，定向授权测试、相邻提交回归、backend API 证据校验和 cleanup apply 均已通过。

## Applicable Gate Summary

- `docs/backend-development.md#一线运行态-route-start-生产组长来源必须独立于班组设备绑定`：提交授权不得把 submittedDeviceId/submittedWorkstationId 与 route-start 或 post-binding 候选 expectedDeviceId/expectedWorkstationId 互相比对来阻断提交。
- `docs/backend-development.md#一线生产正式提交必须单事务落链并按唯一组长归属可见`：正式提交阶段不执行设备参数校验，不因设备缺失、设备参数缺失/异常或候选上下文不一致阻断；仍保留路线、路线工序、MES 工序、实际员工、签名员工和模板校验。
- `docs/experience-index.md` 已补充 `提交设备/工作站上下文不一致限制去掉`、`PRO_FRONTLINE_SUBMIT_DEVICE_CONTEXT_MISMATCH 不抛`、`submittedWorkstationId expectedWorkstationId 不比较` 等关键词。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；本次是用户明确要求移除特定业务限制，不新增 fallback 或异常吞噬。
- `是否从根因和长期维护角度解决`：是；删除会产生该错误的授权比较，避免不同上下文字段继续误杀一线提交。
- `是否存在临时补丁或绕过`：否。

## Cleanup Keep

- doc/tasks/20260808-frontline-submit-remove-context-mismatch-limit/task.md
- doc/tasks/20260808-frontline-submit-remove-context-mismatch-limit/execution-log.md
- doc/tasks/20260808-frontline-submit-remove-context-mismatch-limit/verification-report.md
