# Execution Log

## Intent

用户要求 PQC 组长和生产组长一样拥有 `人员管理` tab，支持通过 `新增` 关联当前 PQC 组长管理的 PQC 检验员，并在标准人员列表中展示。

## BDD

- BDD: PQC组长查看人员管理 -> Given PQC组长进入独立工作台 / When 页面加载模块 tab / Then 默认停留在 `人员管理`，tab 顺序为 `人员管理 / PQC管理 / 看板`，列表紧贴 tab 下方展示。
- BDD: PQC组长关联检验员 -> Given 当前 PQC 组长有可关联的下属正式员工 / When 在 `人员管理` 点击 `新增` 并选择员工确认 / Then 后端创建 `leader_type=PQC`、`scope_type=EMPLOYEE` 的 scope，列表刷新显示该 PQC 检验员。
- BDD: PQC组长维护检验员状态 -> Given 已关联 PQC 检验员 / When 禁用该人员 / Then 该 scope 明确置为禁用，PQC 负责员工范围不再包含该检验员。
- BDD: PQC重复关联失败 -> Given 同一 PQC 组长已经关联某检验员 / When 再次关联同一系统用户 / Then 在写库前返回业务错误，不静默成功、不依赖数据库异常。

## Command Log

- RED: `node tests/e2e/pqc-leader-personnel-tab-static.spec.js` -> FAIL，旧实现缺少默认 `人员管理` tab。
- RED: 后端目标 Maven首次运行 120 秒超时，未生成目标 Surefire 报告。
- GREEN: PQC人员管理静态合同、PQC相邻tab合同、生产组长相邻合同均 PASS。
- GREEN: `pnpm ts:check` -> PASS。
- REGRESSION: `git diff --check -- <task paths>` -> PASS。
- BLOCKER: 后端目标 Maven复跑 240 秒仍未到达 Surefire；确认 PID `56504` 属于本任务后仅停止该进程，未触碰其它 Java 进程。
- RED: 用户真实页面 `GET /admin-api/mes/pro/process-pool/team-leader/pqc-personnel/list` -> FAIL，返回 `请求地址不存在`。
- ROOT CAUSE: `48081` PID `60192` 运行 `output/runtime/int_main/backend-runtime-control-20260805-222248.jar`；健康检查虽为 `UP`，但内嵌 MES Jar 缺少 `MesPqcLeaderPersonnelService`、实现类和响应 VO，属于旧运行包未加载新增路由。
- SAFETY: 另一个 Java PID `5060` 属于 `D:\IntRuoyiWorktree\profile-erp-table-auto-sync`，与本任务无关，不停止、不修改。
- GREEN: 隔离 worktree 目标 Surefire -> PASS，`MesProcessPoolTeamLeaderControllerTest` 11 个、`MesPqcLeaderPersonnelServiceTest` 5 个、`MesTeamLeaderScopeServiceTest` 5 个，共 21 个测试，失败 0、错误 0。
- GREEN: 隔离构建 `yudao-server-exec.jar` SHA256 `9A424362D7A7A0986473AA395CF4D85E37BA4AF3868529EE3E6B7AD34469D9BA`；内嵌 MES Jar 包含 `MesPqcLeaderPersonnelService.class`、`MesPqcLeaderPersonnelServiceImpl.class`、`MesPqcLeaderPersonnelRespVO.class` 和新 Controller。
- RED CONFIRMATION: 旧运行 Jar SHA256 `4EA3E8BB6C585C738EB1F99AFE42C33827CB2908E275242819646213488F5A1F`，内嵌 MES Jar 缺少上述三个 PQC personnel 关键类。
- RUNTIME: 仅停止确认归属 `int_main:48081` 的旧 PID `60192`；新 PID `55784` 运行 `output/runtime/int_main/backend-runtime-control-pqc-personnel-4a2b24c39.jar`，health `UP`。
- GREEN: Playwright 真实页面 `/mes/pro/process-pool/pqc-leader` -> PASS；目标接口 HTTP `200`、业务码 `0`，tab 顺序正确，人员列表和新增按钮可见，`pageErrors=[]`。
- REGRESSION: PQC personnel、PQC tab、生产组长相邻 tab 三个前端静态合同复跑 PASS。
- TRANSIENT: 首次复跑 `pnpm ts:check` 时，并行任务 `20260805-production-leader-active-order-pool-tab` 正在写同一 Vue 文件，出现缺少 `activeOrder*` 定义的中间态错误。
- GREEN: 并行任务静态合同通过后按 stale-blocker 门禁复跑 `pnpm ts:check` -> PASS；未修改、覆盖或提交该并行任务代码。
- CLEANUP: 任务自有 detached verification worktree 已通过 `git worktree remove --force` 删除，路径不存在且 Git 登记已移除。
- VALIDATOR: bug-regression evidence -> PASS；validator self-test -> PASS。
- VALIDATOR: backend-api evidence -> PASS；validator self-test -> PASS。
- VALIDATOR: frontend-feature evidence -> PASS；validator self-test -> PASS。
- EXPERIENCE: `project-experience-consolidation` 核对后，现有 `docs/local-runtime.md` 和 `docs/worktree-memory.md` 已覆盖旧 Jar、认证态路由、隔离构建和不可变运行 Jar 门禁；本次不新增重复经验文档。
- CLOSEOUT: task-closeout preview -> keep 3、delete 6、blocked 0、warnings 0。
- CLOSEOUT: task-closeout apply -> PASS，删除 3 个临时 evidence 和 3 个一次性验证脚本，只保留 `task.md`、`execution-log.md`、`verification-report.md`。

## Completed Work

- 新增 PQC personnel service、BO/VO、Mapper 精确查询和四个 Controller API。
- PQC 人员关系写入正式 `leader_type=PQC + scope_type=EMPLOYEE` scope。
- 新增 `人员管理 / PQC管理 / 看板` tab、标准列表、新增弹窗和启禁用操作。

## Remaining Blocker

- 无。
