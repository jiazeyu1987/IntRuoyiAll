# Execution Log

## Intent

用户要求 PQC 新增人员下拉不再显示全体员工，改为只显示拥有 PQC 权限的人；用户说明当前应有约 30 个这样的用户。

## BDD

- BDD: PQC 添加人员只显示 PQC 权限用户 -> Given 当前组长打开 PQC 人员管理新增弹窗 / When 输入关键字远程搜索候选人 / Then 后端只返回拥有 PQC 权限的启用用户，不返回无 PQC 权限的全公司用户。
- BDD: PQC 添加人员空关键字显示完整权限池 -> Given 本地 `pqc_permission` 角色当前绑定约 30 名启用用户 / When 当前组长打开新增弹窗并触发空关键字候选加载 / Then 后端从该角色分配池返回完整启用候选，不受 20 条候选上限截断。
- BDD: 空下拉点击自动加载候选 -> Given 新增 PQC 检验员弹窗打开且搜索框为空 / When 用户点击或聚焦下拉框 / Then 前端自动请求空关键字候选并展示符合 PQC 权限的启用用户供选择。
- BDD: 其它 PQC 组长已选人员红色禁选 -> Given 候选用户已存在其它 PQC 组长的启用员工 scope / When 当前组长打开新增候选下拉 / Then 该候选仍显示但为红色禁用状态，不能提交关联。
- BDD: PQC 正式员工关联校验复用同一权限范围 -> Given 请求关联一个无 PQC 权限的系统用户 / When 提交 PQC formal link / Then 后端业务拒绝该用户不是 PQC 权限候选，不写入 scope 关系，不返回默认成功。

## Command Log

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesPqcLeaderPersonnelServiceTest,MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 预期失败原因：`MesPqcLeaderPersonnelService` 尚无 `searchFormalInspectorCandidates`、`MesPqcLeaderPersonnelServiceImpl` 构造器尚未注入 `PermissionApi`、缺少 `PRO_PROCESS_POOL_TEAM_PQC_PERSONNEL_PERMISSION_REQUIRED` 错误码。
- RED: `node tests\e2e\pqc-leader-personnel-company-wide-candidates-static.spec.js` -> FAIL, 预期失败原因：PQC 候选端点仍调用生产正式员工全公司候选服务。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesPqcLeaderPersonnelServiceTest,MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，初版 `Tests run: 21, Failures: 0, Errors: 0, Skipped: 0`。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesPqcLeaderPersonnelServiceTest,MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，角色池取数修正后 `Tests run: 22, Failures: 0, Errors: 0, Skipped: 0`。
- GREEN: `node tests\e2e\pqc-leader-personnel-company-wide-candidates-static.spec.js` -> PASS，PQC 候选合同通过。
- REGRESSION: `git diff --check -- <本任务相关路径>` -> PASS，仅提示 Git 将在下次触碰时把 LF 替换为 CRLF，无空白错误。
- GREEN: `pnpm ts:check` -> PASS。

## Completed Work

- 已建立任务记录。
- 定位 `PQC权限角色` 为本地正式角色编码 `pqc_permission`，对应既有数据任务记录显示已绑定 30 名用户。
- 后端新增 `RoleApi.getRoleByCode`，PQC 候选服务先解析 `pqc_permission` 正式角色，再通过 `PermissionApi.getUserRoleIdListByRoleIds` 获取角色分配用户池，最后加载用户信息并按启用状态/关键字过滤。
- 空关键字不再返回空列表、不再受 20 条上限截断；单测固定当前 30 人候选池应完整返回。
- `linkFormalInspector` 在写入 scope 前复用同一 PQC 权限角色校验；无权限用户返回业务错误，不写入关联。
- Controller `/pqc-personnel/formal-candidates` 改为委托 `pqcPersonnelService.searchFormalInspectorCandidates`，前端 API 路径和远程搜索入口保持不变。
- 后端单测覆盖候选过滤、无权限关联拒绝、重复关联仍在写库前业务拒绝；前端静态合同锁定不做本地过滤、不回退到生产候选服务。
- 经验沉淀检查：已检索 `docs/*memory*.md` 与 `docs/experience-index.md`，本次没有需要新增的长期经验文档；具体业务状态和验证证据保留在本任务文档。

## Remaining Blocker

- 收尾提交/推送未执行：当前共享工作区存在大量非本任务改动和未跟踪任务目录，不能将本次改动与其它任务混入同一提交。


## 2026-08-06 Empty Dropdown Occupancy Update

- RED: `node tests\e2e\pqc-leader-personnel-company-wide-candidates-static.spec.js` -> FAIL，预期失败原因：PQC formal link 尚未拒绝已被其它 PQC 组长启用 scope 占用的候选；后续静态断言继续暴露候选方法未直接标记占用字段、前端空点击加载/红色禁选未实现。
- GREEN: `node tests\e2e\pqc-leader-personnel-company-wide-candidates-static.spec.js` -> PASS，合同锁定后端占用字段、跨组长提交拒绝、前端空点击加载和红色禁选展示。
- BLOCKED: `mvn -pl yudao-module-mes -am "-Dtest=MesPqcLeaderPersonnelServiceTest,MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，阻塞原因不是本轮 PQC 候选逻辑，而是当前共享工作区已有未解决冲突标记：`MesProcessPoolTeamLeaderController.java` 与 `MesTeamLeaderActiveOrderServiceImpl.java` 编译时报 merge conflict marker 相关语法错误。
- BLOCKED: `pnpm ts:check` -> FAIL，`TeamLeaderWorkbenchPage.vue` 存在既有 merge conflict marker，Vue TS 编译直接报 TS1185。
- BLOCKED: `git diff --check -- <本任务相关路径>` -> FAIL，`teamLeader.ts`、`TeamLeaderWorkbenchPage.vue`、`MesProcessPoolTeamLeaderController.java` 等文件存在 leftover conflict marker；这些冲突覆盖活跃订单和列配置等非本任务内容，需先由对应任务/负责人完成冲突选择。

## Current Update Work

- 后端 `MesTeamFormalUserCandidateBO` / `MesTeamFormalUserCandidateRespVO` 增加 `disabled`、`disabledReason`、`occupiedByOtherPqcLeader`、`occupiedLeaderUserId` 字段。
- 后端候选查询从 PQC 权限角色池加载用户后，按 `mes_pro_process_pool_team_leader_scope` 的启用 PQC 员工 scope 标记“其它组长占用”。
- 后端 `linkFormalInspector` 在写入 scope 前新增跨 PQC 组长占用校验，命中时返回 `PRO_PROCESS_POOL_TEAM_PQC_PERSONNEL_OCCUPIED_BY_OTHER_LEADER`，不写库。
- 前端 PQC 新增人员 `el-select` 增加 `automatic-dropdown`、空 focus/visible-change 加载、候选 disabled 绑定、红色占用原因展示；即使前端被绕过，后端仍二次拒绝。