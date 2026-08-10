# 20260810-pqc-management-desc-sort

## Task Goal

将 PQC 组长工作台的“PQC管理”列表改为按提交时间倒序排列，最近提交的记录排在最前面；排序必须来自正式后端分页读模型，不能只在前端当前页重排。

## Milestones

- [x] 确认 PQC 管理列表入口、后端分页接口与现有排序来源。
- [x] 先补充 RED 静态/后端测试，锁定 PQC 管理请求和后端分页倒序要求。
- [x] 实现最小正式排序逻辑，保持分页、权限、筛选与错误暴露不变。
- [x] 运行目标 GREEN 与相邻回归验证，并记录结果。
- [ ] 完成任务证据、清理与状态收尾。

## Expected Verification

- `node tests/e2e/pqc-leader-management-desc-sort-static.spec.cjs`
- `node tests/e2e/pqc-leader-management-default-submit-date-static.spec.cjs`
- 受影响后端测试或静态合同验证后端 SQL 按 `server_submit_time DESC, id DESC` 排序。
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260810-pqc-management-desc-sort/frontend-feature-evidence.md`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260810-pqc-management-desc-sort/backend-api-evidence.md`

## Current Status

ready_for_closeout

实现、目标验证和 evidence validator 已通过，准备运行 task-closeout-cleanup preview/apply。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；排序将在正式分页数据源层表达，避免仅前端当前页重排导致分页语义错误。
- `是否存在临时补丁或绕过`：否。

## Experience Gate Summary

- `docs/frontend-development.md#前端服务端分页排序链路门禁`：服务端分页列表不能只在前端当前页 `Array.sort`；本任务采用后端固定正式排序，并保留稳定排序键。
- `docs/backend-development.md#MES PQC组长人员范围与管理数据可见性门禁`：PQC管理读模型按当前登录组长的启用人员范围读取；本任务只调整已可见分页结果的提交时间顺序，不改租户、人员范围、角色或可见性。
