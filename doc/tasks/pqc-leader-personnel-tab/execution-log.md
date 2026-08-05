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

## Completed Work

- 新增 PQC personnel service、BO/VO、Mapper 精确查询和四个 Controller API。
- PQC 人员关系写入正式 `leader_type=PQC + scope_type=EMPLOYEE` scope。
- 新增 `人员管理 / PQC管理 / 看板` tab、标准列表、新增弹窗和启禁用操作。

## Remaining Blocker

- 必须用已验证的不可变 Jar 替换确认归属的旧 PID `60192`，并完成健康检查和认证态目标接口验证。
