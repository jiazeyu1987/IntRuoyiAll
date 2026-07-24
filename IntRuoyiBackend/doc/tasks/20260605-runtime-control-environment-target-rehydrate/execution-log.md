# 执行日志：20260605-runtime-control-environment-target-rehydrate

BDD: 局部 host 覆盖不应丢失默认 target -> Given `application-local.yaml` 只配置 test/prod/backup 的 host / When Runtime Control 属性绑定完成 / Then `intruoyi-frontend`、`intruoyi-backend`、`intruoyi-full`、`website-frontend` 仍然存在。

BDD: 覆盖后的远端 URL 应与当前 host 一致 -> Given test/prod/backup 的 host 被覆盖 / When 读取 Runtime Control 目标 URL / Then 远端状态 URL 必须使用覆盖后的 host 重新生成。

RED: `mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest#runtimeControlPropertiesShouldRehydrateDefaultTargetsAfterHostOnlyOverride test` -> FAIL，`RuntimeControlProperties` 不存在绑定后标准化入口，无法在局部 host 覆盖后回填默认 target。

RED: `mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest#runtimeControlPropertiesShouldRehydrateDefaultTargetsAfterHostOnlyOverride,RuntimeControlServiceImplTest#runtimeControlPropertiesShouldKeepProductionAccessDisabledAfterHostOnlyOverride" test` -> FAIL，host-only 覆盖下正式环境禁用原因未继承默认配置，说明绑定后合并还不完整。

GREEN: `mvn -pl yudao-module-infra "-Dtest=RuntimeControlServiceImplTest#runtimeControlPropertiesShouldRehydrateDefaultTargetsAfterHostOnlyOverride,RuntimeControlServiceImplTest#runtimeControlPropertiesShouldKeepProductionAccessDisabledAfterHostOnlyOverride" test` -> PASS。

GREEN: `mvn -pl yudao-module-infra -Dtest=RuntimeControlServiceImplTest test` -> PASS，46 tests passed。
