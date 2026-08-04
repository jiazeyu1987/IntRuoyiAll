# 业务审批策略默认可开关审批视图

## Task Goal

将审批中心的业务审批策略列表默认视图改为“所有可以开关审批的业务”：用顶层执行器正向白名单展示文控、表单、工艺路线、批记录、批次执行等可开关审批业务，并允许页面筛选继续在该范围内过滤；不能只显示当前 `BPM_REQUIRED` 的开启状态，也不能把表单实例、路线附件、路线表单填写等明细策略默认混入。

## Milestones

- [x] M1: 建立任务记录并补充适用经验门禁。
- [x] M2: 为默认可开关审批视图补充后端和前端静态契约 RED。
- [x] M3: 修改业务审批策略页默认查询参数和后端分页筛选。
- [x] M4: 运行定向 GREEN 验证并记录结果。

## Expected Verification

- `node tests/e2e/bpm-business-approval-policy-static.spec.js`
- `mvn -pl yudao-module-bpm -Dtest=BusinessApprovalPolicyMapperTest,BusinessApprovalPolicyControllerContractTest test`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260804-bpm-policy-default-bpm-required/frontend-feature-evidence.md`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260804-bpm-policy-default-bpm-required/backend-api-evidence.md`

## Current Status

ready_for_closeout

- 实现和定向验证已完成；提交/推送仍受进入本任务前已存在的无关脏改动阻塞，未混入处理。

## 适用经验门禁

- 业务审批策略按配置执行门禁：不得把 `DIRECT` 当作异常或隐藏后端错误；本任务只调整默认列表筛选口径，不改变策略执行语义、切换流程或数据库数据。
- 业务审批策略默认视图门禁：`approvalSwitchScope=true` 必须用顶层可开关执行器正向白名单，不得只排除少量明细策略。
- 前端静态契约隔离门禁：本任务使用现有 `bpm-business-approval-policy-static.spec.js` 覆盖当前行为，避免依赖全量 `ts:check` 历史状态。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，直接调整列表默认查询状态并用静态契约锁定。
- `是否存在临时补丁或绕过`：否。

## Cleanup Keep

- doc/tasks/20260804-bpm-policy-default-bpm-required/frontend-feature-evidence.md
- doc/tasks/20260804-bpm-policy-default-bpm-required/backend-api-evidence.md
