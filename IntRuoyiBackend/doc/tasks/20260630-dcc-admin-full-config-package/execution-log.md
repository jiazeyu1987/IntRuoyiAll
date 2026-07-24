# Execution Log：文控管理员全量数据包（后端）

BDD: 后端可导出文控中心单个全量包 -> Given 当前租户存在文控目录、文控权限、分发规则、培训规则与审批岗位配置 / When 调用文控中心全量包导出接口 / Then 返回一个包含所有受管配置与引用键的 JSON 数据包。
BDD: 后端可导入文控中心单个全量包 -> Given 用户持有源租户导出的全量包 / When 在目标租户调用导入接口 / Then 系统按依赖顺序覆盖目录、类别、矩阵、规则与审批岗位配置。
BDD: 导入按业务键覆盖并刷新 managed scope -> Given 目标租户已存在同 code 的目录、类别或审批岗位，以及历史包外旧配置 / When 导入新全量包 / Then 同业务键配置被覆盖，仅上一次由文控管理员全量包接管的 scope 会按本次包内容刷新，包外历史配置不会拖垮整次导入。
BDD: managed scope 导出只返回包管理范围 -> Given 目标租户存在历史测试类别或目录，但当前 managed scope 已被文控管理员全量包刷新 / When 再次导出全量包 / Then 导出结果只包含 managed scope 内的目录、类别与审批岗位，业务内容与源包一致。
BDD: 导入失败时显式阻塞缺失引用 -> Given 数据包中的规则引用了目标包内不存在的目录、类别或审批岗位 / When 导入执行 / Then 后端 fail fast 报错并回滚，不写入部分成功数据。
ANALYSIS: export-contract -> 聚合包将同时输出结构化 JSON 对象与可直接导入的稳定字段，不允许只输出 source id 关系。
ANALYSIS: import-order -> 审批岗位 -> 目录 -> 目录授权 -> 类别 -> 类别目录绑定 -> 审批矩阵/查看矩阵/类别权限/分发/培训规则；下游引用未解析时立刻失败。
ANALYSIS: managed-scope-root-cause -> 真实测试租户存在大量包外历史类别/目录仍被 `dcc_controlled_file_master` 等业务数据引用，当前 cleanup 把“整个租户配置”误判为 package owned scope，导致导入在清理阶段被整体阻塞。
ANALYSIS: managed-scope-strategy -> 新增 tenant-scoped managed scope 持久化，仅记录文控管理员全量包上一次接管的目录 path、类别 code、审批岗位 code；导出只按该 scope 出包，导入只清理上一轮 scope 中但本轮不再出现且安全可删的项。
ANALYSIS: tenant-isolation -> DCC 配置表虽然多为 `BaseDO`，但当前工程对已注册实体统一挂了 tenant interceptor；本次导入导出优先走现有 MyBatis Plus mapper CRUD，避免原生 SQL 遗漏 `tenant_id`。
ANALYSIS: implementation-plan -> 聚合包将导出稳定业务键与可回放请求形状；导入时先按业务键解析系统用户/部门/岗位/角色与 DCC 岗位，再复用 `replace*` / `save*` 现有管理员服务落库。
RED: mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc "-Dtest=DccAdminFullConfigPackageServiceTest,DccFileCategoryControllerConfigPackageContractTest" -Dsurefire.failIfNoSpecifiedTests=false test -> FAIL, PowerShell 把 `-Dsurefire.failIfNoSpecifiedTests=false` 解析成生命周期参数 `.failIfNoSpecifiedTests=false`，命令本身需改写后重跑。
GREEN: mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc "-Dtest=DccAdminFullConfigPackageServiceTest,DccFileCategoryControllerConfigPackageContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, `DccFileCategoryControllerConfigPackageContractTest` 2 条与 `DccAdminFullConfigPackageServiceTest` 3 条全部通过。
GREEN: experience-preflight -> PASS, 已补读 `docs/login-access.md` 并核验本机后端健康接口 `http://127.0.0.1:48081/actuator/health` 可达，允许进入本机真实登录最小路径验证。
BLOCKER: real-import-cleanup -> `java.lang.IllegalArgumentException: DCC admin full config package cleanup blocked by referenced category: CODEX_DCC_LOCAL_20260525`，当前 cleanup 合同仍按全租户范围扫描，必须先完成 managed scope 重构后再复跑真实导入。
RED: `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc "-Dtest=DccAdminFullConfigPackageServiceTest,DccFileCategoryControllerConfigPackageContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, 新增 managed scope 回归测试暴露 cleanup 仍按全租户范围扫描，且导出会把包外历史类别再次打回配置包。
GREEN: `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc "-Dtest=DccAdminFullConfigPackageServiceTest,DccFileCategoryControllerConfigPackageContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, `12` 条定向测试通过；managed scope 现已只清上一轮接管范围，并保证目标租户再导出只返回当前 managed scope 业务内容。
