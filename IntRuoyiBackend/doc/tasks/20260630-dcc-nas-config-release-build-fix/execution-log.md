# 20260630 DCC NAS 连接配置发布构建阻塞修复执行日志

GREEN: experience-preflight-20260630-dcc-nas-config-release-build-fix -> PASS，已读取 `D:\ProjectPackage\Int\IntRuoyiMaintance\docs\experience-index.md`、`D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-build-preflight-lessons.md` 与 `D:\ProjectPackage\Int\IntRuoyi\AGENTS.md` 命中门禁；本轮仅修复 clean backend 已提交 HEAD 的发布构建编译阻塞，不触发服务器写入、真实 E2E、数据库变更或发布动作。

BDD: clean release build must compile NAS config tests -> Given 后端已提交 HEAD 使用 6 参 `NasConnectionConfig` 契约 When 运行受影响模块测试编译或真实 `build-release` Then DCC 与同类受影响模块测试必须改为当前构造签名并通过编译，不得因旧 4 参调用阻塞发布构建。

RED: `mvn -pl yudao-module-dcc -Dtest=DccNasPermissionSnapshotCaptureServiceImplTest test` -> FAIL，`DccNasPermissionSnapshotCaptureServiceImplTest` 第 `90`、`479` 行仍调用旧 4 参 `NasConnectionConfig` 构造，与当前 6 参契约不匹配。

RED: `mvn -pl yudao-module-srm -Dtest=SrmNasLocatorServiceTest test` -> FAIL，除 `SrmNasLocatorServiceImpl` 第 `435` 行仍调用旧 4 参 `NasConnectionConfig` 外，`SrmSupplierPortalApprovalTaskAdapter` 第 `93` 行 `@Override isVisibleTo` 在当前编译类路径下也发生签名不匹配，说明修复范围至少需覆盖 DCC 测试、SRM NAS locator 同类调用与该适配器编译阻塞。

GREEN: `mvn -pl yudao-module-dcc -Dtest=DccNasPermissionSnapshotCaptureServiceImplTest test` -> PASS，9 tests，DCC 旧 4 参 `NasConnectionConfig` 调用已修复，定向测试通过。

GREEN: `mvn -pl yudao-module-srm "-Dtest=SrmNasLocatorServiceTest,SrmSupplierPortalApprovalTaskAdapterTest" test` -> PASS，17 tests；SRM NAS locator 的旧 4 参 `NasConnectionConfig` 调用已更新为当前 6 参契约，`SrmSupplierPortalApprovalTaskAdapter` 的编译阻塞也已消除。日志中的 `根目录读取失败` 与 `access denied` 为测试用例刻意验证失败分支的预期输出，不影响最终 PASS。
