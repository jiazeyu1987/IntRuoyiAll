# 展厅产品导入字段语义调整任务

## 任务目标

在展厅-产品管理导入链路中支持验收 Excel `D:\ProjectPackage\Int\IntRuoyi\resource\产品资料修改版.xlsx` 的 `产品列表` 工作表。该文件不是新增字段模板，而是把原产品基础信息导入表头替换为新的业务表头；实现必须按新表头直接导入，不能依赖旧表头标签或兼容 fallback。

本任务的业务语义调整为：

- 旧语义/待替换：`core_selling_points` 曾承载“核心卖点”；新语义：`core_selling_points` 承载 `在售国家`。
- 旧语义/待替换：`pipeline_layout` 曾承载“管线布局”；新语义：`pipeline_layout` 承载 `BU`。
- 底层字段继续复用 `core_selling_points`、`core_selling_points_en`、`pipeline_layout`、`pipeline_layout_en`，不新增数据库列，不新增兼容字段，不做 fallback。
- 导入、导出、模板、接口、发布 JSON、管理前端与 Website 展示必须统一使用新业务标签。

## Worker 边界

- 文档阶段角色：文档 worker 子 agent 只写任务文档，不修改生产代码、测试代码、配置或资源文件。
- 实现阶段角色：主 agent 负责后端实现与 reviewer 复核；前端/Website worker 子 agent 只修改管理前端与 Website 切片。
- reviewer 负责 Gate 1/2/3 判定；worker 不自行决定放行。
- Gate 3 已通过；收尾提交阶段只允许暂存本任务直接产生的文件。

## 验收 Excel 证据

只读检查 `产品资料修改版.xlsx` 得到工作表：

- `产品列表`
- `奖项`
- `原材料`

`产品列表` 第 1 行表头：

`展品编码 | 产品名-中文 | 产品名-英文 | 展柜名称 | 持证公司 | 在售/在研 | BU | 在售国家 | 适应症 | 型号规格 | 注册证信息 | 奖项 | 原材料表单`

本任务必须保证 `产品列表` 新表头整体替换进入导入契约；实现 worker 应按严格 TDD 将这些表头纳入新 Excel 契约，而不是兼容旧表头。

## 产品列表表头对应关系

| 验收 Excel 表头 | 既有字段/属性 | 原导入表头 | 处理要求 |
| --- | --- | --- | --- |
| `展品编码` | `productCode` | `产品编码` | 作为产品唯一匹配键；缺失或匹配不到产品必须失败可见。 |
| `产品名-中文` | `nameCn` | `中文名称` | 写入中文产品名；不得依赖原表头。 |
| `产品名-英文` | `nameEn` | `英文名称` | 写入英文产品名；不得依赖原表头。 |
| `展柜名称` | 展柜/产品映射（若当前代码已支持） | 无 | 当前产品基础信息导入范围外；除非实现选择校验既有展柜映射，否则不得静默使用它作为产品定位或字段 fallback。 |
| `持证公司` | 既有所属公司展示/输入列，当前底层为 `owner_company_id` | `所属公司` | 必须确定性解析或校验；若实现继续沿用 currentDetail 的 `owner_company_id`，必须记录为 deliberate no-fallback，并增加测试证明 Excel 公司名与当前产品所属公司不一致时失败可见，不得静默忽略。 |
| `在售/在研` | `lifecycleStage` | `生命周期` | 解析 `已注册 -> REGISTERED`、`研发中 -> R_AND_D`；未知值失败可见。 |
| `BU` | `pipelineLayout` / `pipeline_layout` | 旧语义标签 | 写入 `pipeline_layout`，最终业务标签为 `BU`。 |
| `在售国家` | `coreSellingPoints` / `core_selling_points` | 旧语义标签 | 写入 `core_selling_points`，最终业务标签为 `在售国家`。 |
| `适应症` | `indicationContent` / `indication_content` | `适应症` | 写入 `indication_content`。 |
| `型号规格` | `modelSpecification` / `model_specification` | `型号规格` | 写入 `model_specification`。 |
| `注册证信息` | `registrationCertificate` / `registration_certificate` | `注册证` | 写入 `registration_certificate`。 |
| `奖项` | 无当前产品基础信息字段 | 无 | 当前实现范围外，除非代码已有支持；不得静默宣称已导入，需在导入契约或响应中明确非导入范围/失败策略。 |
| `原材料表单` | 无当前产品基础信息字段 | 无 | 当前实现范围外，除非代码已有支持；不得静默宣称已导入，需在导入契约或响应中明确非导入范围/失败策略。 |

## 字段契约

| 层级 | `BU` 契约 | `在售国家` 契约 |
| --- | --- | --- |
| Excel `产品列表` 表头 | `BU` | `在售国家` |
| Java VO 属性 | `pipelineLayout` | `coreSellingPoints` |
| API fields key | `pipeline_layout` | `core_selling_points` |
| 英文字段 key | `pipeline_layout_en` | `core_selling_points_en` |
| 数据库存储列 | `pipeline_layout` / `pipeline_layout_en` | `core_selling_points` / `core_selling_points_en` |
| 中文显示标签 | `BU` | `在售国家` |
| 英文显示标签 | `BU` | `Countries on Sale` |
| 发布 JSON `fieldCode` | `pipeline_layout` | `core_selling_points` |
| Website `labelZh` | `BU` | `在售国家` |
| Website `labelEn` | `BU` | `Countries on Sale` |
| 管理前端基础信息 | `BU` textarea/input | `在售国家` textarea/input |
| 管理前端列表状态 | 如纳入完整度，显示 `BU` | 显示 `在售国家` / `在售国家(英)` |

## 影响面

- 后端导入/导出 VO：`ShowroomProductExcelVO` 的 Excel 表头需改为完整新契约；导入读取 `展品编码`、`产品名-中文`、`产品名-英文`、`持证公司`、`在售/在研`、`BU`、`在售国家`、`适应症`、`型号规格`、`注册证信息` 等替换后表头。
- 所属公司校验：`持证公司` 必须确定性解析；若实现继续沿用 currentDetail 的 `owner_company_id`，需把它作为 no-fallback 约束，并在 mismatch 时失败可见。
- 非导入范围：`展柜名称`、`奖项`、`原材料表单` 当前不属于产品基础信息导入，除非现有代码已支持对应映射；不能静默作为 fallback 或伪成功。
- Excel 模板/导出：`getProductImportTemplate` 与 `exportProductExcel` 必须输出新表头；不得继续输出旧业务标签。
- 基础信息字段：`ShowroomFieldDisplaySupport`、字段目录、详情返回与版本中心显示统一为 `BU`、`在售国家`。
- 列表状态/资料完整判断：管理前端 `ProductListTable.vue` 当前以 `core_selling_points` 判断内容状态，状态文案必须改成 `在售国家` 对应信息。
- 发布包与 legacy projection：`ShowroomReleaseConstants`、`ShowroomVersionCenterService`、legacy Website projection 保持 fieldCode 不变，但 label/value 语义必须按新业务标签输出。
- Website 前台展示：`bilingualPublicFields` 中 `core_selling_points` 的 `labelZh/labelEn/valueZh/valueEn` 必须按 `在售国家` 展示；`pipeline_layout` 如公开展示必须按 `BU` 展示。
- 管理前端文案：基础信息表单、字段 contracts、版本中心测试数据、批量补齐提示等旧语义文案必须清理或按产品决策重命名。
- Website mock/tests：`public/mock/showroom-display-website-config.json`、`src/showroom-website-config.mock.js`、`src/medical-kiosk.test.js`、`tests/kiosk-detail.spec.js` 需更新为新标签与示例值。
- AI/旁白链路风险：当前存在写入 `core_selling_points` 的批量内容生成/提示词逻辑。若该功能继续存在，必须先明确它是否应改为 `在售国家` 补齐；不能把卖点文案写入 `在售国家` 字段。

## 里程碑

| 里程碑 | 状态 | 说明 |
| --- | --- | --- |
| M1 文档 worker 输出任务文档 | Completed | 已创建 `task.md`、`execution-log.md`、`backend-api-evidence.md`、`frontend-feature-evidence.md`。 |
| M2 Reviewer Gate 1 文档放行 | Completed | 主 agent 已检查目标、字段契约、影响面、无 fallback 原则，并校正 `研发中 -> R_AND_D` 契约。 |
| M3 Reviewer Gate 2 TDD 放行 | Completed | 主 agent 已确认 BDD 场景、RED/GREEN 顺序、测试覆盖和真实数据/E2E路径。 |
| M4 实现 worker 代码与测试 | Completed | 已完成后端、管理前端、Website 代码与测试切片；目标 RED/GREEN 已记录在 `execution-log.md`。 |
| M5 Reviewer Gate 3 验收放行 | Completed | 目标自动化测试、管理端类型检查、真实测试租户 Playwright 导入和后端日志副作用检查均已通过。 |

## Gate 定义

- Gate 1：文档完整性。必须确认完整新旧表头映射、Excel 表头替换、影响面、非目标和不做 fallback 均明确。
- Gate 2：测试先行。必须先增加会失败的后端/前端/Website 测试，记录 RED 失败原因，再进入实现。
- Gate 3：实现验收。必须看到 GREEN、回归、真实 Excel 导入和 Website 展示证据；旧业务标签不得作为最终业务标签出现在目标界面或发布 JSON 中。

## 预期验证

- 后端：针对 `ShowroomProductExcelImportExportIntegrationTest` 增加只含替换后表头的 Excel/真实文件导入断言，验证不依赖旧表头标签，并覆盖 `展品编码`、产品中英文名、`持证公司` 校验、`在售/在研` 解析、`BU -> pipeline_layout`、`在售国家 -> core_selling_points`、`注册证信息 -> registration_certificate`。
- 后端：字段展示、版本中心、发布包、legacy projection 测试断言 `labelZh/labelEn` 使用新标签。
- 管理前端：字段 contracts、基础信息表单、列表状态列、版本中心脚本测试改为新标签。
- Website：mock 与 Vitest/Playwright 测试验证前台展示 `在售国家`、`Countries on Sale`。
- E2E：使用测试租户在 `http://localhost:8081` 走产品管理真实导入路径，接口仅用于最终校验。

## Cleanup Keep

- `doc/tasks/20260526-showroom-product-import-sales-country-bu/backend-api-evidence.md`
- `doc/tasks/20260526-showroom-product-import-sales-country-bu/frontend-feature-evidence.md`
- `doc/tasks/20260526-showroom-product-import-sales-country-bu/verification-report.md`

## 当前状态

代码实现、目标自动化验证和真实测试租户 Playwright 导入验证已完成，Reviewer Gate 3 已放行。

- 已通过：后端导入/导出、字段展示、发布投影、叙事批量入口回归；管理前端脚本测试、全仓类型检查与工具栏布局验证；Website Vitest 与 Playwright 产品详情验证。
- 已验证真实 Excel：`产品资料修改版.xlsx` 存在，`产品列表` 表头为本任务新契约，实际数据允许 `BU` 为空并提供 `在售国家=中国`。
- 已通过真实 E2E：从 `http://localhost:8081` 使用测试租户 `测试租户` / `aoteman` 导入 `产品资料修改版.xlsx`，返回 `totalRows=164`、`successCount=164`、`failureCount=0`。
- 已验证副作用：本轮真实导入后端日志未出现 `product-*-ruoxi.wav`、`Native memory allocation` 或 `SHOWROOM_AUDIO_GENERATION_FAILED`。
- 提交状态：本任务按混合仓库策略分别提交后，由最终响应回报各仓库 commit 结果。
