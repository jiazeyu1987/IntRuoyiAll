# 订单异常上报字段收敛

## Task Goal

将生产组长工作台“订单异常上报”收敛为只需要订单号和异常说明；不再要求或提交工序ID、异常原因。

## Milestones

- [ ] 记录 BDD/TDD 与适用门禁
- [ ] 新增前端与后端 RED 合同，证明旧实现仍依赖工序ID或异常原因
- [ ] 更新前端表单、请求类型和后端接口/服务契约
- [ ] 运行目标 GREEN 验证并记录结果
- [ ] 更新验证报告与收尾状态

## Expected Verification

- `node tests/e2e/mes-process-pool-team-leader-static.spec.js`
- `mvn -pl yudao-module-mes -am "-Dtest=MesWorkOrderAbnormalReportServiceTest,MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260806-work-order-abnormal-minimal-report/frontend-feature-evidence.md`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260806-work-order-abnormal-minimal-report/backend-api-evidence.md`

## Current Status

in_progress

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，前端表单、前端请求类型、后端 VO、BO 和服务校验同步收敛。
- `是否存在临时补丁或绕过`：否。

## Experience Gate Summary

- `docs/experience-index.md` 已存在并读取。
- 命中 `docs/frontend-development.md#前端静态契约隔离门禁`：用当前需求专用静态合同隔离验证，不用全量历史失败冒充当前结论。
- 命中 `docs/powershell-memory.md#PowerShell Maven -D 参数引号门禁`：Maven `-D...` 参数必须整体加双引号。
- 命中 `docs/powershell-memory.md#脏工作区基线门禁`：当前工作区已有大量非本任务脏改动与 staged 改动，本任务不使用宽泛暂存或回滚；若进入提交需先按门禁处理基线授权。
