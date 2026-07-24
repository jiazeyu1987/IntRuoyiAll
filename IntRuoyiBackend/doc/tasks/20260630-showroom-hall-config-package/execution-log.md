# Execution Log：展柜管理数据包导入导出（后端）

- `2026-06-30 任务创建`：建立后端任务文档，确认本次只在 `yudao-module-showroom` 内新增展柜配置包合同。
- `BDD: 后端可导出展柜配置 zip 包 -> Given 当前租户存在展柜、关键词、背景图、live 预览图和 live 中英文语音 / When 调用展柜配置包导出接口 / Then 后端返回单个 zip 包，其中 manifest 与资产内容完整可追溯。`
- `BDD: 导入按业务键完全替换展柜与关键词 owned scope -> Given 目标租户已有旧展柜和旧关键词 / When 导入新的展柜配置包 / Then hallCode 与关键词集合按包内容重建，包外旧数据在 owned scope 内被移除。`
- `BDD: 导入在缺 productCode 或 awardCode 引用时回滚 -> Given 配置包展柜映射引用目标租户不存在的产品或奖项 / When 导入执行 / Then 系统报出缺失业务键并整包回滚。`
- `BDD: 导入重建背景图、preview live 与 narration live -> Given 配置包中包含背景图、preview 资产和 hall live narration / When 导入成功 / Then 目标租户生成新的文件与新的 live 版本记录，但业务内容与源包一致。`
- `RED: mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-showroom -Dtest=ShowroomHallConfigPackageServiceTest,ShowroomHallConfigPackageControllerPermissionTest -Dsurefire.failIfNoSpecifiedTests=false test -> FAIL, 编译先被 ShowroomNativeImageGenerationService 对 CodexCli getter 的现有调用阻塞。`
- `RED: mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-showroom -am -Dtest=ShowroomHallConfigPackageServiceTest,ShowroomHallConfigPackageControllerPermissionTest -Dsurefire.failIfNoSpecifiedTests=false test -> FAIL, reactor 继续前进后又被 yudao-module-dcc 现有测试编译错误阻塞。`
- `2026-06-30 收敛实现`：已补 ShowroomNativeImageGenerationService 的反射兼容读取，避免当前快照缺失 getter 直接卡死 showroom 编译；同时修正 ShowroomHallConfigPackageServiceTest 的 Java 17 `getFirst()` 与 tenantId builder 问题，待下轮继续验证本任务 service/controller 合同。`
- `GREEN: mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-showroom -Dtest=ShowroomHallConfigPackageServiceTest,ShowroomHallConfigPackageControllerPermissionTest -Dsurefire.failIfNoSpecifiedTests=false test -> PASS，10 个定向测试全部通过，已覆盖导出 manifest/资产、完全替换、缺 productCode 或 awardCode 回滚、缺资产失败，以及 export/import 权限合同。`
- `GREEN: empty-package-replace-contract -> PASS，补充回归测试确认导入允许空关键词集合与空展柜集合，并能把目标租户旧关键词/旧展柜完全替换为空，符合“完全一致、可覆盖”语义。`
- `GREEN: mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-server -am -Dmaven.test.skip=true package -> PASS，最新 showroom 配置包实现已重新打进本机 `yudao-server-exec.jar`。`
- `GREEN: backend-runtime-reload -> PASS，按统一脚本重启本机 backend 后，`http://127.0.0.1:48081/actuator/health` 返回 `{"status":"UP"}`，新日志切换到 `backend-20260630-021955.out.log`。`
- `GREEN: real-import-error-contract -> PASS，真实导入阶段已不再返回笼统 `系统异常`；接口现在明确返回 `400 SHOWROOM_HALL_CONFIG_PACKAGE_REFERENCE_MISSING`，并携带缺失 `productCode` 清单。`
