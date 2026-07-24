# 后端与 API 证据

## 只读检查范围

- 后端 worktree：`D:\ProjectPackage\Int\IntRuoyi\worktrees\20260526-showroom-product-import-sales-country-bu\ruoyi-vue-pro`
- 模块：`yudao-module-showroom`
- 验收 Excel：`D:\ProjectPackage\Int\IntRuoyi\resource\产品资料修改版.xlsx`

## 实现前基线证据

- 以下证据来自文档 worker 的实现前只读检查，用于说明 RED 起点。
- `ShowroomProductExcelVO.java:36` 实现前 Excel 表头仍是旧语义/待替换标签，对应属性 `pipelineLayout`。
- `ShowroomProductExcelVO.java:42` 实现前 Excel 表头仍是旧语义/待替换标签，对应属性 `coreSellingPoints`。
- `ShowroomAdminController.java:253-303` 导出、模板、导入均使用 `ShowroomProductExcelVO`，工作表名为 `产品列表`。
- `ShowroomApiRuntime.java:1233-1256` 导出/导入字段映射已通过 `pipeline_layout` 与 `core_selling_points` 写入 fields，可复用底层字段。
- `ShowroomFieldDisplaySupport.java:54-84` 实现前字段显示元数据仍使用旧语义/待替换标签，需改为 `BU`、`在售国家`、`Countries on Sale`。
- `ShowroomReleaseConstants.java:44-53` 发布公共字段列表包含 `pipeline_layout` 与 `core_selling_points`，fieldCode 可保持不变。
- `ShowroomVersionCenterService.java:810-817` 发布/版本中心字段包包含 `pipeline_layout(_en)` 与 `core_selling_points(_en)`。
- `ShowroomVersionCenterService.java:854-856` 根据 fieldCode 读取中英文值，语义标签需由显示元数据和发布字段生成链路统一调整。
- `ShowroomProductNarrationCodexService.java:20,106,108,119,126` 实现前仍存在写入或引用旧语义的提示词/上下文，必须在实现前明确是否继续保留该功能。

## 实现后证据

- `ShowroomProductExcelVO` 已改为验收 Excel 新表头：`展品编码`、`产品名-中文`、`产品名-英文`、`展柜名称`、`持证公司`、`在售/在研`、`BU`、`在售国家`、`适应症`、`型号规格`、`注册证信息`、`奖项`、`原材料表单`。
- `ShowroomApiRuntime` 已将 `BU` 写入 `pipeline_layout`，将 `在售国家` 写入 `core_selling_points`；`展柜名称`、`奖项`、`原材料表单` 不写入产品 fields。
- `ShowroomApiRuntime` 已对 `持证公司` 做确定性校验；Excel 公司名与当前产品所属公司不一致时失败可见，不静默沿用。
- `ShowroomFieldDisplaySupport` 已将 `pipeline_layout(_en)` 标签改为 `BU`，将 `core_selling_points(_en)` 标签改为 `在售国家 / Countries on Sale`。
- `ShowroomAdminController` 后端批量入口已从 `/product/batch-generate-selling-points` 改为 `/product/batch-generate-sales-countries`。
- `ShowroomProductNarrationCodexService` 已把原卖点生成职责改为在售国家整理；缺少明确国家/地区时返回空内容并由调用方失败可见。
- 发布详情测试已验证 Website 发布字段 `pipeline_layout` 使用 `BU`，`core_selling_points` 使用 `在售国家 / Countries on Sale`，fieldCode 保持不变。
- `ShowroomApiRuntime.importProductExcel(...)` 已改为导入专用 text-only publish：普通手动发布仍生成音频，Excel 导入只沿用已发布讲解稿文本和音频引用来满足 Website 版本包契约，不调用 `narrationService.generateAudio(...)`，不调用封面生成，不调用 AI 补写在售国家。

## Excel 文件证据

`产品资料修改版.xlsx` 只读解析结果：

- 工作表：`产品列表`、`奖项`、`原材料`
- `产品列表` 表头：`展品编码`、`产品名-中文`、`产品名-英文`、`展柜名称`、`持证公司`、`在售/在研`、`BU`、`在售国家`、`适应症`、`型号规格`、`注册证信息`、`奖项`、`原材料表单`

## 产品列表完整表头映射要求

| 验收 Excel 表头 | 既有属性/字段 | 原导入表头 | fields key / 存储 | 要求 |
| --- | --- | --- | --- | --- |
| `展品编码` | `productCode` | `产品编码` | 产品主记录编码 | 作为现有产品匹配键；缺失、重复或不存在必须失败可见。 |
| `产品名-中文` | `nameCn` | `中文名称` | 产品主记录中文名 | 写入 `nameCn`，不得依赖原表头。 |
| `产品名-英文` | `nameEn` | `英文名称` | 产品主记录英文名 | 写入 `nameEn`，不得依赖原表头。 |
| `展柜名称` | 展柜/产品映射（若支持） | 无 | 非产品基础信息 fields | 当前产品基础信息导入范围外；除非实现明确校验既有展柜映射，否则不得静默使用为产品定位或 fallback。 |
| `持证公司` | 所属公司展示/输入列 | `所属公司` | `owner_company_id` | 必须确定性解析或校验；若继续从 currentDetail 保留 `owner_company_id`，必须新增 mismatch 失败测试，证明 Excel 公司名不会被静默忽略。 |
| `在售/在研` | `lifecycleStage` | `生命周期` | `lifecycle_stage` | 解析 `已注册 -> REGISTERED`、`研发中 -> R_AND_D`；未知值失败可见。 |
| `BU` | `pipelineLayout` | 旧语义标签 | `pipeline_layout` | 写入 `pipeline_layout`；最终中文/英文标签均为 `BU`。 |
| `在售国家` | `coreSellingPoints` | 旧语义标签 | `core_selling_points` | 写入 `core_selling_points`；最终标签为 `在售国家` / `Countries on Sale`。 |
| `适应症` | `indicationContent` | `适应症` | `indication_content` | 写入 `indication_content`。 |
| `型号规格` | `modelSpecification` | `型号规格` | `model_specification` | 写入 `model_specification`。 |
| `注册证信息` | `registrationCertificate` | `注册证` | `registration_certificate` | 写入 `registration_certificate`。 |
| `奖项` | 无当前产品基础信息字段 | 无 | 非产品基础信息 fields | 当前实现范围外，除非代码已有支持；不得静默宣称导入，需明确非导入范围或失败策略。 |
| `原材料表单` | 无当前产品基础信息字段 | 无 | 非产品基础信息 fields | 当前实现范围外，除非代码已有支持；不得静默宣称导入，需明确非导入范围或失败策略。 |

## No-Fallback 约束

- 实现目标是表头替换，不是旧表头兼容；RED 测试必须构造只含新表头的 `产品列表`，证明不依赖原导入表头标签。
- `持证公司` 不能被静默忽略。若当前导入保留 currentDetail 的 `owner_company_id`，必须把它作为 deliberate no-fallback 约束：Excel 公司名与当前所属公司必须一致，否则导入失败并报告产品编码和两侧公司名。
- `展柜名称` 不得作为产品编码匹配失败时的 fallback；如实现要校验展柜映射，必须使用既有映射规则并添加失败测试。
- `奖项`、`原材料表单` 不在当前产品基础信息导入范围时，导入结果必须明确它们未被导入或直接失败；不能以总成功数暗示已导入。

## 后端实现检查清单

- 更新 `ShowroomProductExcelVO` 表头为完整替换后 Excel 契约。
- 更新导入集成测试，直接或等价读取验收 Excel 的 `产品列表`，断言 `productCode`、`nameCn`、`nameEn`、`lifecycle_stage`、`pipeline_layout=BU 值`、`core_selling_points=在售国家值`、`indication_content`、`model_specification`、`registration_certificate`。
- 增加 `持证公司` mismatch 测试，证明不一致时失败可见。
- 增加 `在售/在研` 解析测试，覆盖 `已注册` 与 `研发中`。
- 明确 `展柜名称`、`奖项`、`原材料表单` 非导入范围或 fail-fast 行为，并用测试固定。
- 更新导出/模板测试，断言新表头存在，旧语义标签不作为最终业务标签输出。
- 更新 `ShowroomFieldDisplaySupport` 中中英文标签。
- 更新版本中心、发布包、legacy projection 相关测试数据和断言。
- 检查 `ShowroomProductNarrationCodexService` 与批量补齐流程：不能再将卖点文本生成到 `core_selling_points` 的新语义字段。
- 不新增数据库列，不新增字段别名，不新增旧表头兼容分支。

## 后端 RED/GREEN 证据模板

RED: `mvn -pl yudao-module-showroom -Dtest=ShowroomProductExcelImportExportIntegrationTest#importProductExcelShouldReadReplacementProductListHeaders test` -> FAIL, expected reason: 只含替换后表头的 Excel 尚未被 VO 识别，无法证明新文件导入不依赖旧表头标签。

RED: `mvn -pl yudao-module-showroom -Dtest=ShowroomProductExcelImportExportIntegrationTest#importProductExcelShouldFailOnOwnerCompanyMismatch test` -> FAIL, expected reason: `持证公司` 尚未被确定性解析或 mismatch 校验。

RED: `mvn -pl yudao-module-showroom -Dtest=ShowroomProductExcelImportExportIntegrationTest#importProductExcelShouldParseRegisteredAndInDevelopment test` -> FAIL, expected reason: `在售/在研` 新表头下的 `已注册`、`研发中` 解析尚未固定。

GREEN: `mvn -pl yudao-module-showroom -Dtest=ShowroomProductExcelImportExportIntegrationTest test` -> PASS

RED: `mvn -pl yudao-module-showroom -Dtest=ShowroomFoundationContractTest test` -> FAIL, expected reason: 字段显示元数据尚未输出新业务标签。

GREEN: `mvn -pl yudao-module-showroom -Dtest=ShowroomFoundationContractTest test` -> PASS

RED: `mvn -pl yudao-module-showroom -Dtest=ShowroomReleasePublisherServiceTest,ShowroomVersionCenterServiceTest test` -> FAIL, expected reason: 发布包/版本中心仍使用旧语义标签或测试值。

GREEN: `mvn -pl yudao-module-showroom -Dtest=ShowroomReleasePublisherServiceTest,ShowroomVersionCenterServiceTest test` -> PASS

## 后端当前状态

实现已完成，目标后端验证与真实测试租户导入均通过；Reviewer Gate 3 已放行。真实导入 `产品资料修改版.xlsx` 返回 164 行全部成功发布，后端日志未出现 `product-*-ruoxi.wav` 或 JVM native memory crash。
