# Verification Report

## Summary

一线生产员工弹窗的数据源已与生产组长人员管理的生产人员档案来源对齐。后端运行配置现在按当前登录生产组长查询启用的生产人员档案，员工切换校验也继续使用同一运行配置，避免出现“弹窗能看到但切换被拒绝”的前后端不一致。

真实 E2E 已通过：生产组长人员管理启用人员 8 人、一线生产 runtime employees 8 人、员工弹窗候选 8 人，三者 hash 均为 `a7115b13b7357fb2a3691ec6f3b339a11d45f162c6bc8b81e8f9946ad9378e40`。

## Changed Files

- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlineRuntimeConfigServiceImpl.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlineRuntimeConfigServiceTest.java`
- `IntRuoyiFronted/tests/e2e/edhr-frontline-production-employee-options-match-leader-personnel-static.spec.cjs`
- `doc/tasks/20260806-frontline-production-employee-options-match-leader-personnel/frontline-production-employee-popup-real-e2e.cjs`
- `doc/tasks/20260806-frontline-production-employee-options-match-leader-personnel/restart-frontline-employee-runtime.ps1`

## RED Evidence

- `node tests\e2e\edhr-frontline-production-employee-options-match-leader-personnel-static.spec.cjs` -> FAIL: 旧运行配置服务未调用 `toEmployeeOptions(loginUserId)`，仍按设备/工序 scope leader 构造员工弹窗候选。
- `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineRuntimeConfigServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL: `expected: <8801> but was: <8802>`，设备 scope leader 与当前登录生产组长不一致时，旧逻辑返回了设备 scope 人员。

## GREEN Evidence

- `node tests\e2e\edhr-frontline-production-employee-options-match-leader-personnel-static.spec.cjs` -> PASS。
- `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineRuntimeConfigServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 4, Failures: 0, Errors: 0。

## Regression Evidence

- `node tests\e2e\edhr-frontline-fill-tabs-static.spec.cjs` -> PASS。
- `node tests\e2e\frontline-team-config-static.spec.cjs` -> PASS。
- `node tests\e2e\production-personnel-management-static.spec.cjs` -> PASS。
- `node tests\e2e\team-leader-workbench-static.spec.cjs` -> PASS。
- `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineEmployeeSwitchServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 4, Failures: 0, Errors: 0。
- `pnpm ts:check` -> PASS。
- `git diff --check -- <task-owned files>` -> PASS。
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260806-frontline-production-employee-options-match-leader-personnel\frontend-feature-evidence.md` -> PASS。
- `rg -n "一线生产员工弹窗|getFrontlineRuntimeConfig employees|工序员工绑定" docs\experience-index.md docs\backend-development.md` -> PASS。

## Real E2E Evidence

- `node --check doc\tasks\20260806-frontline-production-employee-options-match-leader-personnel\frontline-production-employee-popup-real-e2e.cjs` -> PASS。
- 旧运行 Jar 下真实 E2E -> FAIL：人员管理启用人员 8 个，一线生产 `粗洗工序` runtime/popup 仅返回 `刘悦悦`；证据文件 `doc\tasks\20260806-frontline-production-employee-options-match-leader-personnel\frontline-production-employee-popup-evidence.md`。
- 运行 Jar 拆检 -> FAIL：旧 Jar 内 `MesFrontlineRuntimeConfigServiceImpl` 仍为 `toEmployeeOptions(List<EmployeeBindingDO>)` / `selectBatchIds` 旧字节码。
- 运行态刷新 -> PASS：新 Jar `E:\IntRuoyi\output\runtime\int_main\backend-runtime-frontline-employee-options-20260806-162955.jar` 已替换目标 class，嵌套 MES Jar `compress_type=0`，48081 重启后 health `UP`。
- 当前运行态复查 -> PASS：48081 后续被并行 Jar `backend-runtime-frontline-employee-options-active-order-code-input-20260806-1638.jar` 接管；该 Jar 内目标 class 仍为 `toEmployeeOptions(Set)` / `employeeProfileMapper.selectList` 新字节码。
- 新 Jar 复跑真实 E2E -> BLOCKED：`TeamLeaderWorkbenchPage.vue` 含未解决冲突标记，Vite 报 `Attribute name cannot contain U+0022...`，生产组长真实页面无法加载。
- 本轮源码补强 -> PASS：`MesFrontlineRuntimeConfigServiceImpl` 改为 `toEmployeeOptions(loginUserId)`，并按 `.eq(MesProcessPoolTeamEmployeeProfileDO::getLeaderUserId, leaderUserId)` 查询当前登录生产组长启用人员档案。
- 本轮运行态刷新 -> PASS：基于当前 48081 Jar 生成 `E:\IntRuoyi\output\runtime\int_main\backend-runtime-frontline-employee-options-login-leader-20260806-171928.jar`，仅替换目标 service class；嵌套 MES Jar `compress_type=0`，48081 从 PID `46572` 切换到 PID `45716`，health `UP`。
- 本轮真实 E2E -> PASS：`node doc\tasks\20260806-frontline-production-employee-options-match-leader-personnel\frontline-production-employee-popup-real-e2e.cjs` -> `PASS: frontline production employee popup matches enabled production personnel list; count=8`。
- 本轮真实 E2E 结果：人员管理启用人员、runtime employees、popup options 均为 `112`、`113`、`114`、`陈丽`、`方王魏`、`李业辉`、`李之音`、`王一林`；`pageErrors=[]`、`consoleErrors=[]`、`targetNetworkFailures=[]`、`targetHttpFailures=[]`。
- 本轮证据：`E:\IntRuoyi\output\playwright\20260806-frontline-production-employee-options-match-leader-personnel\frontline-production-employee-popup-result.json`、`production-personnel-list.png`、`frontline-production-employee-popup.png`。

## Closeout Notes

- 任务实现、静态合同、后端单测和真实页面 E2E 均通过；任务状态已更新为 `ready_for_closeout`。
- 当前仓库存在大量非本任务脏改动；本轮按用户要求完成 E2E 复验，尚未执行混合提交或推送，避免把无关任务文件纳入本次交付。
