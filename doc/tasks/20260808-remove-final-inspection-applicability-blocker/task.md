# 去掉末检适用性缺失阻塞

## Task Goal

按用户明确要求，去掉活跃订单/PQC 放行链路中“QA 规程发布版本缺少末检适用性配置”的阻塞；历史发布版本 `finalInspectionApplicable=null` 时不再要求 FINAL 末检任务。

## Milestones

- [x] 记录 BDD、范围和预期验证。
- [x] 增加 RED 测试覆盖发布版本末检适用性为空但 FIRST/PATROL 已完整的通过场景。
- [x] 修改放行完整性校验，取消该 blocker。
- [x] 更新相关长期规则文档，避免仍把该字段缺失列为活跃订单/放行 blocker。
- [ ] 运行后端定向测试和 backend evidence 校验。

## Expected Verification

- `mvn -pl yudao-module-mes clean test "-Dtest=MesOrderReleaseCompletenessServiceTest,MesTeamLeaderActiveOrderServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false"`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260808-remove-final-inspection-applicability-blocker\backend-api-evidence.md`
- `git diff --check -- <task-owned files>`

## Current Status

blocked - 代码、测试和规则文档已更新，但标准 GREEN 验证被同模块并发 Maven 进程持续写入 `yudao-module-mes\target` 阻塞；按项目 Maven target 门禁，不能清理 target、不能强杀非本任务进程，也不能把静态检查或非隔离输出伪装成 JUnit 通过。

## Experience Gate Summary

- `docs/backend-development.md#PQC 末检适用性必须有发布规程依据`：原规则要求缺失适用性阻塞；本次按用户明确要求调整该规则。
- `docs/backend-development.md#零排产活跃订单必须使用发布态正式路线`：候选资格和新增写入仍必须使用发布态正式路线、发布快照 routeProcessId 和正式 QA 规程；本次只取消末检适用性为空的阻塞，不放宽路线/规程存在性。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：是。用户明确要求“去掉这个限制”；触发条件为历史 QA 规程发布版本 `finalInspectionApplicable=null`，风险是系统不再因缺少该字段要求 FINAL 末检任务；移除策略为后续若业务重新要求强制末检适用性，可恢复该校验和测试。
- `是否从根因和长期维护角度解决`：是。将活跃订单/PQC 放行链路的业务口径改为“只有明确 true 才要求 FINAL，false 或 null 均不要求 FINAL”，并同步测试与规则文档。
- `是否存在临时补丁或绕过`：否。
