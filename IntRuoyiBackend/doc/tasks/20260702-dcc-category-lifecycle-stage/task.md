# 任务：文控类别生命周期阶段列

- Task ID: `20260702-dcc-category-lifecycle-stage`
- Created: 2026-07-02
- Current Status: completed

## 任务目标

在文控中心的文控权限页签下，为“类别列表”增加固定 6 阶段字段：`01 plan 策划`、`02 input 输入`、`03 output 输出`、`04 verification 验证`、`05 validation 确认`、`06 transfer 转移`。后端持久化 `lifecycle_stage`，接口使用 `lifecycleStage`，新增/修改必须校验枚举，不允许缺失、非法值或静默默认。

## 里程碑

1. 建立任务台账、经验门禁、BDD/TDD 证据。completed
2. 补后端与 SQL RED 测试，固化阶段字段、校验和迁移回填契约。completed
3. 实现数据库、后端模型、服务校验和导入阶段解析。completed
4. 运行后端定向测试与 SQL/schema 回归。completed
5. 记录验证结果并收尾。completed

## Expected Verification

- `mvn -pl yudao-module-dcc "-Dtest=DccFileCategoryAdminServiceImplTest,DccBaseSchemaTest" test`
- `python -X utf8 -m pytest script/tests/test_dcc_category_lifecycle_stage_sql.py -q`

## 经验门禁

- 命中 `docs/powershell-memory.md`：PowerShell 命令、中文文本、SQL 与测试输出必须显式 UTF-8，不使用 `&&`。
- 命中 `database-schema-delivery`：新增字段必须非破坏性迁移，历史数据必须明确回填，未知类别必须 fail fast。
- 命中 `backend-api-delivery`：新增/修改接口必须校验阶段枚举，缺失或非法值返回明确错误，不吞异常、不默认成功。
- 命中 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：前端阶段列和筛选控件保持现有紧凑运营台风格。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，新增正式字段、枚举校验和可追溯迁移回填。
- 是否存在临时补丁或绕过：否。

## BDD 场景

- `BDD: 类别列表显示阶段 -> Given 文控类别存在生命周期阶段 / When 管理员查询类别列表 / Then 每行返回稳定 lifecycleStage，前端展示对应 01-06 阶段标签。`
- `BDD: 阶段筛选 -> Given 类别列表包含不同阶段 / When 管理员选择 02 input 输入 / Then 列表只显示 INPUT 阶段类别。`
- `BDD: 新增修改必须选择阶段 -> Given 管理员新增或编辑类别 / When 阶段为空或非法 / Then 后端拒绝保存并提示阶段无效。`
- `BDD: 历史类别明确回填 -> Given 运行时库已有 DCC_FVM_DHF/DMR/OTHER 类别 / When 执行迁移 / Then 已知类别按确认映射写入阶段，未知类别阻断迁移并列出风险。`

## Current Blockers

- 全量本地后端重启脚本默认打包被无关 MES 测试源码编译失败阻塞；本任务已使用 maven.test.skip=true 生产打包并通过真实 E2E。

## Final Verification Result

- `mvn -pl yudao-module-dcc "-Dtest=DccFileCategoryAdminServiceImplTest,DccFileCategoryMapperTest,DccAdminFullConfigPackageServiceTest,DccBaseSchemaTest#mysqlSchemaShouldSupportDccCategoryLifecycleStage" "-DskipITs" test` -> `PASS`，29 个用例通过。
- `python -X utf8 -m pytest script/tests/test_dcc_category_lifecycle_stage_sql.py -q` -> `PASS`，2 个用例通过。
- `mvn -pl yudao-server -am -Dmaven.test.skip=true package` + `restart-ruoyi-local-component.ps1 -Component backend -SkipBuild` -> `PASS`，本地后端已启动最新生产包用于真实 E2E。