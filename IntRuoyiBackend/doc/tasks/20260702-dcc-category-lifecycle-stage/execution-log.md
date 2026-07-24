# 执行日志：文控类别生命周期阶段列

BDD: 类别列表显示阶段 -> Given 文控类别存在生命周期阶段 / When 管理员查询类别列表 / Then 每行返回稳定 lifecycleStage，前端展示对应 01-06 阶段标签。

BDD: 阶段筛选 -> Given 类别列表包含不同阶段 / When 管理员选择 02 input 输入 / Then 列表只显示 INPUT 阶段类别。

BDD: 新增修改必须选择阶段 -> Given 管理员新增或编辑类别 / When 阶段为空或非法 / Then 后端拒绝保存并提示阶段无效。

BDD: 历史类别明确回填 -> Given 运行时库已有 DCC_FVM_DHF/DMR/OTHER 类别 / When 执行迁移 / Then 已知类别按确认映射写入阶段，未知类别阻断迁移并列出风险。

GREEN: experience-preflight -> PASS，已读取 `docs/powershell-memory.md`、`docs/experience-index.md`、`D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`；本阶段先执行本机源码、SQL 和测试改动，不操作服务器。

RED: `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileProjectCodeRecognitionServiceTest,DccBaseSchemaTest,DccFileCategoryAdminServiceImplTest,DccCategoryApprovalMatrixAdminServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，schema/识别规则回归暴露 runtime repair 缺少 recognition record `batch_task_id`、file type 迁移静态契约不兼容、目录别名匹配文本不完整。

GREEN: `python -X utf8 -m pytest script/tests/test_dcc_sql_scripts.py script/tests/test_dcc_category_lifecycle_stage_sql.py -q` -> PASS，8 passed。

GREEN: `python -X utf8 script/release/run-release-migration-policy-gate.py --sql-root sql/mysql` -> PASS。

GREEN: `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileProjectCodeRecognitionServiceTest,DccBaseSchemaTest,DccFileCategoryAdminServiceImplTest,DccCategoryApprovalMatrixAdminServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，76 tests。

RED: `mvn -pl yudao-module-dcc "-Dtest=DccFileCategoryAdminServiceImplTest#createCategory_withoutClientSource_persistsLocalSource" "-DskipITs" test` -> FAIL，前端新增类别不传内部 `source` 时，后端插入 `dcc_file_category` 触发 `NULL not allowed for column "source"`。

GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccFileCategoryAdminServiceImplTest#createCategory_withoutClientSource_persistsLocalSource" "-DskipITs" test` -> PASS，新增类别服务端补齐本地来源 `LOCAL`，阶段 `INPUT` 正常落库。

GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccFileCategoryAdminServiceImplTest,DccFileCategoryMapperTest,DccAdminFullConfigPackageServiceTest,DccBaseSchemaTest#mysqlSchemaShouldSupportDccCategoryLifecycleStage" "-DskipITs" test` -> PASS，29 tests。

GREEN: `python -X utf8 -m pytest script/tests/test_dcc_category_lifecycle_stage_sql.py -q` -> PASS，2 passed。

BLOCKER: `powershell -NoProfile -ExecutionPolicy Bypass -File script/deploy/restart-ruoyi-local-component.ps1 -Component backend` -> FAIL，重启脚本默认 `-DskipTests` 仍编译无关 MES 测试源码，当前 MES 测试缺失若干类导致打包失败；已改用 `mvn -pl yudao-server -am -Dmaven.test.skip=true package` 构建生产 jar，再以 `-SkipBuild` 启动本地后端完成本任务 E2E。
