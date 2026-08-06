# Verification Report

## Summary

一线生产员工弹窗的数据源已与生产组长人员管理的生产人员档案来源对齐。后端运行配置现在按当前生产组长 scope 查询启用的生产人员档案，员工切换校验也继续使用同一运行配置，避免出现“弹窗能看到但切换被拒绝”的前后端不一致。

## Changed Files

- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlineRuntimeConfigServiceImpl.java`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlineRuntimeConfigServiceTest.java`
- `IntRuoyiFronted/tests/e2e/edhr-frontline-production-employee-options-match-leader-personnel-static.spec.cjs`

## RED Evidence

- `node tests\e2e\edhr-frontline-production-employee-options-match-leader-personnel-static.spec.cjs` -> FAIL: 旧运行配置服务未从 leader 人员档案列表构造员工弹窗候选。
- `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineRuntimeConfigServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL: `expected: <2> but was: <1>`，旧逻辑只返回工序绑定员工。

## GREEN Evidence

- `node tests\e2e\edhr-frontline-production-employee-options-match-leader-personnel-static.spec.cjs` -> PASS。
- `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlineRuntimeConfigServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 3, Failures: 0, Errors: 0。

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

## Closeout Notes

- 任务实现和验证完成。
- 当前仓库存在大量非本任务脏改动；本任务未执行混合提交或推送，避免把无关任务文件纳入本次交付。
