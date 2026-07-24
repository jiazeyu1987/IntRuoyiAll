# 展厅旧 product 编号到当前 INT 编号映射

## 任务目标
- 在展厅产品列表中补充旧底表编号 product_XXX 与当前 INT-* 产品编号的对应关系。
- 后续导入桌面 展厅讲解软件产品资料更新底表.xlsx 时，可以用 product_XXX 精确定位当前 INT-* 产品。
- 映射必须失败快：找不到、重复或冲突时导入失败并返回明确清单，不做猜测匹配。

## 经验门禁
- PowerShell/中文文件读写：已读取 docs/powershell-memory.md，中文文本必须显式 UTF-8。
- 后端/API变更：使用 BDD + 严格 TDD，先写失败测试再实现。
- 数据库/schema变更：如需新增字段/表，先核真实 schema 与迁移路径，并记录回滚风险。

## 里程碑
1. 定位产品列表、导入解析、产品持久化结构 - 已完成
2. 设计旧编号映射字段/契约与失败规则 - 已完成
3. 编写 RED 测试覆盖 product_XXX -> INT-* 导入 - 已完成
4. 最小实现并回归导入/列表接口 - 已完成
5. 归档验证并提交 - 已完成

## 预期验证
- 产品列表能看到或返回旧 product_XXX 映射。
- 导入旧底表时，product_XXX 能精确映射到当前 INT-* 产品。
- 未配置映射、重复映射、product_XXX 不存在时导入失败并给出清单。

## 设计约束检查
- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，采用显式映射，不猜测旧编号到新编号。
- 是否存在临时补丁或绕过：否。

## 当前状态
- 2026-07-05 18:32:29 +08:00：任务创建，开始定位现有产品导入/列表代码。
- 2026-07-05 19:30:28 +08:00：已完成旧编号字段、列表展示、旧底表导入映射与缺失映射失败规则；后端回归通过。
- 2026-07-05 19:34:00 +08:00：任务清理预览通过，无需删除临时产物；任务完成。
- 2026-07-05 20:36:30 +08:00：根据新增要求补强导入门禁：旧底表 `product_*` 不允许命中旧主产品，必须映射到当前 `INT-*`，保证导入后唯一 INT 数量与唯一 product 数量一致。

## 已完成工作
- 产品持久化模型新增 `legacyProductCode`，数据库新增 `legacy_product_code` 与租户内唯一索引。
- 产品列表、详情、草稿保存接口透出并保存旧底表编号，前端列表增加“旧产品编号”列。
- 桌面旧底表导入时，`product_XXX` 仅通过显式旧编号映射定位当前 `INT-*` 产品；缺失、重复或映射到非 `INT-*` 时失败快。
- 导出产品 Excel 保持原列顺序，不把内部映射字段写入旧底表列，避免破坏现有导入导出契约。

## 最终验证
- `mvn -pl yudao-module-showroom -Dtest=ShowroomProductExcelImportExportIntegrationTest#importBaseWorkbookShouldResolveLegacyProductCodeToCurrentIntProduct+importBaseWorkbookShouldFailWhenLegacyProductCodeHasNoMapping+exportProductExcelShouldIgnorePaginationAndExcludeMediaColumns test` -> PASS。
- `mvn -pl yudao-module-showroom -Dtest=ShowroomProductExcelImportExportIntegrationTest#importBaseWorkbookShouldRejectOldProductCodeWhenItDoesNotResolveToCurrentIntProduct+importBaseWorkbookShouldResolveLegacyProductCodeToCurrentIntProduct+importBaseWorkbookShouldFailWhenLegacyProductCodeHasNoMapping+exportProductExcelShouldIgnorePaginationAndExcludeMediaColumns test` -> PASS。
- `pnpm ts:check`（`NODE_OPTIONS=--max-old-space-size=8192`）-> PASS。

## 结论
- 已完成，可以做到：先在展厅产品列表维护 `product_XXX -> INT-*` 映射，后续导入桌面 `展厅讲解软件产品资料更新底表.xlsx` 时按该映射更新当前 `INT-*` 产品。
- 已补强：旧底表导入时唯一 `product_*` 必须解析到唯一当前 `INT-*`，不能直接命中旧 `product_*` 主产品导致导入后 `INT-*` 数量少于 `product_*` 数量。

## 名称一致映射收口（2026-07-05）

### 目标
- 按桌面 `展厅讲解软件产品资料更新底表.xlsx` 中的 `product_*` 名称，在当前 `INT-*` 产品中建立旧编号对应关系。
- 强约束：只允许 `product_*` 名称与 `INT-*` 当前产品名称一致的映射写入 `legacy_product_code`，不得按同编号 X 猜测。

### 当前结果
- 本地库已写入并验证通过：每个租户 {'1': 143, '122': 143} 条映射。
- 总写入映射行：286。
- 已接受映射类型：{'MATCH_FULL_NAME_UNIQUE': 246, 'MATCH_CN_UNIQUE': 32, 'MATCH_FULL_NAME_GROUP_ORDERED': 8}。
- 剩余未映射 product 行/租户：{'1': 13, '122': 13}。
- 剩余原因分类：{'UNMAPPED_NO_EXACT_CN': 18, 'UNMAPPED_NAME_CARDINALITY_CONFLICT': 8}。

### 设计约束检查
- 是否引入 fallback/降级/吞异常：否。未按编号猜测，未把名称不一致数据强行映射。
- 是否从根因和长期维护角度解决：是。导入逻辑改为批量预构建名称一致映射，显式已有 legacy 映射优先；同名等量组按编号排序只在名称完全一致且两边数量一致时生效。
- 是否存在临时补丁或绕过：否。剩余名称不一致项以阻塞清单输出，不静默处理。

### 产物
- 映射明细：`doc/tasks/20260705-showroom-legacy-product-code-mapping/product-int-name-cardinality-mapping.csv`
- 未映射阻塞清单：`doc/tasks/20260705-showroom-legacy-product-code-mapping/product-int-name-unmapped-blockers.csv`
- 本地库验证：`doc/tasks/20260705-showroom-legacy-product-code-mapping/product-int-name-current-db-verification.json`
- SQL 回填脚本：`sql/mysql/20260705_showroom_legacy_product_code_name_backfill.sql`

### 当前状态
- 部分完成：名称一致且可证明一一对应的 `product_* -> INT-*` 已完成。
- 未完成：每租户 13 个 `product_*` 因名称不一致或同名数量冲突未自动映射，需业务确认是否改 INT 名称、改底表名称，或提供人工映射表。

## 剩余未映射人工确认模板（2026-07-05）

### 当前进展
- 已生成人工确认模板，用于处理严格名称规则下不能自动映射的剩余行。
- 唯一未映射 product 行数：24；按租户统计：{'1': 12, '122': 12}。
- 原因分类：{'UNMAPPED_NO_EXACT_CN': 18, 'UNMAPPED_NAME_CARDINALITY_CONFLICT': 6}。

### 模板产物
- Excel 模板：`doc/tasks/20260705-showroom-legacy-product-code-mapping/product-int-name-manual-decision-template.xlsx`
- CSV 模板：`doc/tasks/20260705-showroom-legacy-product-code-mapping/product-int-name-manual-decision-template.csv`
- 只读 SQL 注释模板：`doc/tasks/20260705-showroom-legacy-product-code-mapping/20260705_showroom_legacy_product_code_manual_decision_template.sql`

### 门禁结论
- 不自动写入剩余项：这些行要么名称不完全一致，要么同名候选数量无法证明唯一对应。
- 若继续完成全量目标，需要业务在模板的 `manual_decision_*` 列确认：目标 INT 编号、采用哪一侧名称作为最终一致名称、备注。

## 剩余未映射人工确认模板 v2（2026-07-05）

### 当前进展
- 已生成增强版人工确认模板，补充同编号候选、未占用候选、候选占用状态、底表重复编码诊断。
- 唯一未映射 product 行数：24；按租户统计：{'1': 12, '122': 12}。
- 决策分类：{'同 X 候选名称不一致，需确认改名方向后映射': 16, '多个未占用同名候选，必须人工选 INT': 4, '先处理底表重复 product_code，再决定映射': 2, '同名候选已被其它 product 占用，必须人工调整占用关系': 2}。
- 底表重复 product 编码：{'product_081': 2}。

### v2 产物
- Excel 模板：`doc/tasks/20260705-showroom-legacy-product-code-mapping/product-int-name-manual-decision-template-v2.xlsx`
- CSV 模板：`doc/tasks/20260705-showroom-legacy-product-code-mapping/product-int-name-manual-decision-template-v2.csv`
- JSON 摘要：`doc/tasks/20260705-showroom-legacy-product-code-mapping/product-int-name-manual-decision-template-v2.json`

### 继续门槛
- 这些剩余项仍未写库；必须先在 `manual_decision_*` 列确认目标 INT、最终一致名称和处理动作。
- 对名称不一致项，需要先确认是改底表名称、改 INT 当前产品名，还是业务指定不映射。
- 对重复 `product_081`，需要先消除或明确重复编码的业务含义，再生成正式回填 SQL。

## 人工确认 SQL 生成器（2026-07-05）

### 当前进展
- 已新增 `script/showroom_generate_manual_legacy_mapping_sql.py`。
- 生成器只读取人工确认表，不直接写数据库。
- 未填写 `manual_decision_*` 时阻塞：status=BLOCKED，blocker_count=24。
- 只有确认了目标 INT、最终一致名称和动作后，才会生成正式 SQL。

### 动作门禁
- `MAP_ONLY_NAMES_ALREADY_EQUAL`：候选 INT 当前名称必须已经等于 product 名称。
- `RENAME_INT_TO_PRODUCT_NAME_AND_MAP`：生成 SQL 会先把 INT 当前版本名称改成 product 名称，再写 `legacy_product_code`。
- 任一行最终中文名不等于 product 中文名时直接失败，不生成 SQL。

## 同号 X 名称与数量判定（2026-07-05）

### 判定口径
- 只按桌面 `C:\Users\BJB110\Desktop\展厅讲解软件产品资料更新底表.xlsx` 的 `product_XXX` 与当前展厅 `INT-X` 做同号 X 对比。
- 判定字段为中文名与英文名完全一致；数量判定为 `product_XXX` 数量与当前 `INT-*` 数量一致。

### 判定结果
- 不通过：`product_XXX` 与 `INT-X` 在同号 X 口径下数量不完全一致、名称也不完全一致。
- 每租户底表 product 数量：155；当前 INT 数量：163；数量不一致。
- 每租户同号 X 交集：153；其中中文名完全一致 22，中文名不一致或不可判定 131；中英文全名完全一致 19，全名不一致或不可判定 134。
- 底表存在但当前无同号 INT：`026`、`050`。
- 当前存在但底表无同号 product：`063`、`068`、`069`、`070`、`071`、`072`、`073`、`074`、`082`、`166`。

### 结论
- 不能按 `product_X -> INT-X` 直接批量绑定。
- 后续自动写入 `legacy_product_code` 仍只能使用“名称完全一致且一一对应”的规则；同号但名称不一致的项必须先业务确认是否改 INT 名称、改底表名称，或明确人工映射。

## 基础信息维护旧产品编号（2026-07-05）

### 目标
- 在展厅产品“基础信息”编辑弹窗里直接维护旧底表产品编号，支持人工把 `product_XXX` 保存到当前 `INT-*` 产品上。
- 保存后后续导入 `展厅讲解软件产品资料更新底表.xlsx` 时可使用该字段稳定解析旧编号。

### 当前实现
- 老的产品基础信息弹窗新增 `旧产品编号` 输入框，示例占位 `product_012`。
- 编辑产品时从产品详情 `legacyProductCode` 回填旧编号。
- 保存草稿/基础信息时 payload 带上裁剪后的 `legacyProductCode`。
- 新详情弹窗原有 `legacyProductCode` 展示与编辑链路保持不变。

### 设计约束检查
- 是否引入 fallback/降级/吞异常：否。只增加显式字段维护，不自动猜测编号。
- 是否从根因和长期维护角度解决：是。把人工确认后的旧编号持久化到产品基础数据字段，导入链路复用同一字段。
- 是否存在临时补丁或绕过：否。

### 验证
- `node tests/e2e/showroom-legacy-product-code-basic-info-static.spec.js` -> PASS。
- `pnpm ts:check`（`NODE_OPTIONS=--max-old-space-size=8192`）-> PASS。

## 基础底表导入旧编号精确匹配收口（2026-07-05 23:50）

### 新规则
- `/showroom/product/import-base-workbook` 导入 `产品列表` 时，`展品编码` 为 `product_*` 的行只按 `showroom_product.legacy_product_code` 精确匹配当前产品。
- 匹配到当前 `INT-*` 产品：更新该 `INT-*` 产品，保留当前产品编码，并把导入行的 `product_*` 保存为旧编号。
- 未匹配到任何 `legacy_product_code`：计入 `skippedProductCodes`，不创建新产品、不按名称猜测、不计入失败。
- 若 `product_*` 只命中旧主产品而没有当前 `INT-*` 目标映射：继续失败并暴露 `SHOWROOM_PRODUCT_LEGACY_CODE_INT_COUNT_MISMATCH`，避免旧产品被误更新。

### 已完成工作
- 禁用基础底表导入中的名称自动映射，不再按中英文名称或同名数量组把 `product_*` 推断绑定到 `INT-*`。
- 新增/调整集成测试覆盖：未映射旧编号跳过、名称相同但无旧编号跳过、重复同名组无旧编号跳过、显式旧编号仍可更新当前 INT 产品、旧主产品命中仍失败。
- 已验证桌面样本 `C:\Users\BJB110\Desktop\展厅讲解软件产品资料更新底表.xlsx` 可被当前 FastExcel 读取入口读出真实产品行。

### 设计约束检查
- 是否引入 fallback/降级/吞异常：否。跳过是本轮明确业务规则，不是隐藏失败；旧主产品误命中仍失败。
- 是否从根因和长期维护角度解决：是。导入匹配权威来源收敛为 `legacy_product_code`。
- 是否存在临时补丁或绕过：否。

### 当前状态
- 已完成：代码与定向回归均通过。
- 本轮不修改服务器、数据库或桌面样本文件。


## 发布契约修复记录

- 2026-07-06：人工确认 SQL 注释模板已移出 sql/mysql 可发布目录，保留为任务证据；可执行展厅旧编号 SQL 已补充 elease-migration 元数据，避免发布门禁误扫不可执行模板或缺少迁移契约。
