# Execution Log

## Intent

用户要求 PQC 新增人员下拉不再显示全体员工，改为只显示拥有 PQC 权限的人；用户说明当前应有约 30 个这样的用户。

## BDD

- BDD: PQC 添加人员只显示 PQC 权限用户 -> Given 当前组长打开 PQC 人员管理新增弹窗 / When 输入关键字远程搜索候选人 / Then 后端只返回拥有 PQC 权限的启用用户，不返回无 PQC 权限的全公司用户。
- BDD: PQC 添加人员空关键字显示完整权限池 -> Given 本地 `pqc_permission` 角色当前绑定约 30 名启用用户 / When 当前组长打开新增弹窗并触发空关键字候选加载 / Then 后端从该角色分配池返回完整启用候选，不受 20 条候选上限截断。
- BDD: 空下拉点击自动加载候选 -> Given 新增 PQC 检验员弹窗打开且搜索框为空 / When 用户点击或聚焦下拉框 / Then 前端自动请求空关键字候选并展示符合 PQC 权限的启用用户供选择。
- BDD: 其它 PQC 组长已选人员红色禁选 -> Given 候选用户已存在其它 PQC 组长的启用员工 scope / When 当前组长打开新增候选下拉 / Then 该候选仍显示但为红色禁用状态，不能提交关联。
- BDD: PQC 正式员工关联校验复用同一权限范围 -> Given 请求关联一个无 PQC 权限的系统用户 / When 提交 PQC formal link / Then 后端业务拒绝该用户不是 PQC 权限候选，不写入 scope 关系，不返回默认成功。
- BDD: 空下拉候选接口不因缺少 keyword 系统异常 -> Given 新增 PQC 检验员弹窗点击空下拉触发候选加载 / When 前端请求未携带或携带空 `keyword` / Then 后端按空关键字查询 PQC 权限候选，不抛参数绑定异常，不返回“系统异常”。

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

## 2026-08-06 Continue Verification Update

- GREEN: `node tests\e2e\pqc-leader-personnel-company-wide-candidates-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\production-leader-active-order-pool-tab-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\mes-process-pool-team-leader-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesPqcLeaderPersonnelServiceTest,MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`Tests run: 25, Failures: 0, Errors: 0, Skipped: 0`。
- GREEN: `git diff --check -- <本任务相关路径>` -> PASS。
- GREEN: `git diff --check` -> PASS。
- GREEN: `rg -n "^(<<<<<<<|=======|>>>>>>>)" -g "!**/target/**" -g "!**/target_corrupt*/**" IntRuoyiBackend IntRuoyiFronted docs doc` -> 无匹配，conflict markers 已清零。
- 合并修复：去除 `MesProcessPoolTeamLeaderController` 重复 `toActiveOrderCandidateRespVO`，并补齐异常上报 `abnormalReasonCode` 前后端类型/BO/持久化链路，使既有班组长合同与当前实现一致。
- CLOSEOUT BLOCKER: `git status --short --branch --untracked-files=all` -> 当前工作区仍有其它任务代码/文档改动和未跟踪任务目录；本任务未提交/未推送，避免混入非本任务改动。

## 2026-08-06 Empty Dropdown System Exception Fix

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderControllerTest#pqcFormalCandidateEndpointAcceptsMissingKeywordForEmptyDropdown" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，预期失败原因：`searchPqcFormalEmployeeCandidates` 的 `@RequestParam("keyword")` 默认 `required=true`，空下拉请求缺省 `keyword` 时会在进入服务前参数绑定失败。
- Root cause: PQC 新增人员弹窗空点击是正式空关键字查询场景，但后端 Controller 把 `keyword` 建模为必填参数；请求封装或交互路径未带该参数时不会进入 `MesPqcLeaderPersonnelServiceImpl` 的空关键字权限池逻辑，前端收到异常后显示“系统异常”。
- Fix: 将 PQC 候选接口参数改为 `@RequestParam(value = "keyword", required = false)`，缺省值按 `null` 传入现有服务层，由 `normalizeText` 作为空关键字处理；未引入全员 fallback、默认成功或前端本地过滤。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderControllerTest#pqcFormalCandidateEndpointAcceptsMissingKeywordForEmptyDropdown" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`。
- GREEN: `node tests\e2e\pqc-leader-personnel-company-wide-candidates-static.spec.js` -> PASS，静态合同新增 `keyword required=false` 断言。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesPqcLeaderPersonnelServiceTest,MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`Tests run: 26, Failures: 0, Errors: 0, Skipped: 0`。
- GREEN: `pnpm ts:check` -> PASS。
- REGRESSION: `git diff --check` -> PASS，仅有 Git 换行提示，无空白错误。
- CLOSEOUT BLOCKER: `git status --short --branch --untracked-files=all` -> 当前共享工作区仍有其它任务代码/文档改动和未跟踪任务目录；本任务未提交/未推送，避免混入非本任务改动。
- EXPERIENCE: project-experience-consolidation -> 已合并到 `docs/frontend-development.md#复合输入控件交互保留门禁`，并更新 `docs/experience-index.md` 关键词；`rg -n "空下拉|keyword required=false|RequestParam required=false|20260806-pqc-personnel-permission-candidates" docs/frontend-development.md docs/experience-index.md` -> PASS。

## 2026-08-06 Admin Real E2E Verification

- RED: `芋道源码/admin` 真实页面 E2E -> FAIL，点击 PQC 新增人员空下拉时 `/pqc-personnel/formal-candidates?keyword=` 返回业务码 `500`、消息“系统异常”；后端日志显示 `NoSuchMethodError: RoleApi.getRoleByCode(String)`，说明 48081 运行 Jar 的 MES 与 system 模块版本不一致。
- RUNTIME: 停止完整 `mvn -pl yudao-server -am -DskipTests package`，原因是本轮 Maven 长时间卡在 Windows 文件属性读取且仓内存在损坏的旧 `target_corrupt_m4_20260802_1327` 目录；未删除非本任务目录，改用项目规则允许的内嵌模块热替换。
- RUNTIME: 以 `yudao-module-system-2026.04-SNAPSHOT.jar` 热替换运行 Jar 内 `BOOT-INF/lib/yudao-module-system-2026.04-SNAPSHOT.jar`，保持 nested jar `compress_type=0`；后端 `56964 -> 49580`，health `UP`。
- RED: `芋道源码/admin` 真实页面 E2E 补充缺省 `keyword` 接口核验 -> FAIL，页面 `keyword=` 已返回 30 个候选且无系统异常，但缺省 `keyword` 登录态接口仍返回 `请求参数缺失:keyword`；`javap -v target/classes` 确认源码编译 class 中 `searchPqcFormalEmployeeCandidates` 的 `@RequestParam required=false` 已存在，运行 Jar 的 MES Controller class 未同步。
- RUNTIME: 以当前运行 Jar 为底，热替换内嵌 MES jar 中 `MesProcessPoolTeamLeaderController.class`，并再次替换 system 模块，两个 nested jar 均验证 `compress_type=0`；后端 `49580 -> 42608`，health `UP`。
- RUNTIME: 前端 8081 Vite 在后端刷新后 HTTP 请求超时，确认 PID `54068` 是 `vite --mode env.local --strictPort` 后重启；新前端 HTTP `200`，后端 health `UP`。
- GREEN: `芋道源码/admin` 真实页面 E2E -> PASS，当前 URL `/mes/pro/process-pool/pqc-leader`，PQC tab 顺序 `人员管理/PQC管理/看板` 且默认 `人员管理`；点击新增人员空下拉后页面候选接口返回 `code=0,count=30`，可见选项数 30，无“系统异常/请求地址不存在”，无目标网络失败、无 pageerror、无写请求。
- GREEN: 登录态缺省 `keyword` 补充核验 -> PASS，`GET /admin-api/mes/pro/process-pool/team-leader/pqc-personnel/formal-candidates` 不带 query 返回 `code=0,count=30`。
- Evidence: `output/playwright/20260806-pqc-personnel-admin-e2e/result.json`，截图 `output/playwright/20260806-pqc-personnel-admin-e2e/pqc-personnel-dropdown.png`；本地当前端口 `8081` HTTP `200`，`48081` health `UP`。
- Scope note: 当前 `芋道源码/admin` 数据中 `occupiedCandidateCount=0`，因此真实 E2E 未观察到“其它 PQC 组长已选候选红色禁用”样本；该行为仍由前端静态合同、后端单测和提交校验覆盖。
