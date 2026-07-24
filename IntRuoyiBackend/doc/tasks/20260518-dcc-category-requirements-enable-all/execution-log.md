# Execution Log: DCC 文件类别要求分发和要求培训全量开启

BDD: 当前所有文件类别要求分发和要求培训必须统一开启 -> Given 运行库中存在历史 `distributionRequired=false` 或 `trainingRequired=false` 的 DCC 文件类别 / When 管理员触发本次修复后的真实类别写入路径 / Then 所有类别真实数据都必须持久化为 `distributionRequired=true` 和 `trainingRequired=true`。

BDD: 新导入或新创建的 DCC 文件类别不能再默认关闭要求开关 -> Given 系统后续通过 IntAuth 导入或新建 DCC 文件类别 / When 类别写入本地库 / Then `distributionRequired` 与 `trainingRequired` 必须默认保存为 `true`，避免再次出现列表与实际值偏差。

RED: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc -am -Dtest=DccFileCategoryAdminServiceImplTest,DccCategoryDistributionRuleAdminServiceImplTest,DccCategoryTrainingRuleAdminServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, `importCategoriesFromIntAuth_createsMissingLocalCategory` 与 `createCategory_missingRequirementFlags_defaultsToTrue` 两个断言都得到 `false`。

GREEN: `mvn --% -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-dcc -am -Dtest=DccFileCategoryAdminServiceImplTest,DccCategoryDistributionRuleAdminServiceImplTest,DccCategoryTrainingRuleAdminServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS
