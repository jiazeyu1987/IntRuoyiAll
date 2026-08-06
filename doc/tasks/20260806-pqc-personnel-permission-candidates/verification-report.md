# Verification Report

## Summary

- PASS: PQC 新增人员候选范围已从全公司用户改为拥有 `pqc_permission` 的启用用户。
- PASS: 空关键字候选从 `pqc_permission` 角色分配池加载完整启用用户，不再返回空列表，也不再受 20 条候选上限截断。
- PASS: PQC 候选查询和提交关联使用同一后端权限口径；无权限用户在写入 scope 前业务拒绝。
- PASS: 前端仍调用 `/mes/pro/process-pool/team-leader/pqc-personnel/formal-candidates` 远程搜索，不新增前端本地过滤。
- PASS: `pnpm ts:check` 本轮通过。

## Verification

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesPqcLeaderPersonnelServiceTest,MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，缺少候选服务方法、权限注入构造器和新错误码。
- RED: `node tests\e2e\pqc-leader-personnel-company-wide-candidates-static.spec.js` -> FAIL，PQC 候选端点仍使用全公司正式员工候选服务。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesPqcLeaderPersonnelServiceTest,MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，初版 `Tests run: 21, Failures: 0, Errors: 0, Skipped: 0`。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesPqcLeaderPersonnelServiceTest,MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，角色池取数修正后 `Tests run: 22, Failures: 0, Errors: 0, Skipped: 0`。
- GREEN: `node tests\e2e\pqc-leader-personnel-company-wide-candidates-static.spec.js` -> PASS。
- PASS: `git diff --check -- <本任务相关路径>` -> PASS；仅存在 CRLF 工作区提示，无空白错误。
- PASS: `pnpm ts:check` -> PASS。

## Scope Notes

- 权限口径：使用本地已有 `PQC权限角色` 的正式角色编码 `pqc_permission`，避免通用菜单权限 `mes:pro-edhr-batch-execution:query` 或 `mes:pro-process-pool-team-leader:query` 复用导致混入非 PQC 用户。
- 候选入口：后端先按 `pqc_permission` 解析角色 ID，再取角色分配用户池并加载用户信息；空关键字返回完整启用候选，输入关键字仅在该权限池内筛选。
- 提交校验：`linkFormalInspector` 先校验用户有效，再校验 PQC 权限角色，再检查重复 scope，最后写入 `mes_pro_process_pool_team_leader_scope`。

## Closeout Blockers

- 当前共享工作区已有大量非本任务改动，未执行提交和推送，避免混入其它任务文件。
