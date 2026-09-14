# PQC 检验设备、接收标准与检验方法闭环修改文档

## Purpose and Scope

本文档基于当前 IntRuoyi 系统证据，定义 PQC 填写页、PQC 组长复核、QA 规程/审核与历史追溯围绕“检验项目维度”的修改方案。目标是让每一个 PQC 检验项目都有正式来源的检验设备、设备编号、接收标准、检验方法、检测结果和判定结论，并在提交后冻结为可审计快照。

本次只整理修改方案、BDD/TDD 验收计划和 review 优化结论，不修改生产代码、不写数据库迁移、不运行真实写入型 E2E。

## Evidence Reviewed

- `IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchPqcFillPage.vue`：PQC 填写入口仅包装 `FrontlineFixedTemplatePanel mode="pqc"`。
- `IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue`：PQC 项目来自 `selectedProcess.inspectionItems`，当前以元信息字符串展示方法、标准、判定类型；缺少项目级检验设备、设备编号，以及独立的“接收标准”“检验方法”按钮和弹窗。
- `IntRuoyiFronted/src/views/mes/pro/feedback/frontlineTemplate.ts`：`PQC_SIMPLIFIED_FIELD_CODES = ['PQC_RESULT']`，PQC 简化模板当前 payload 白名单仅保留总体结果。
- `IntRuoyiFronted/src/api/mes/pro/feedback/index.ts`：`FrontlinePqcInspectionItemVO` 只有 `itemCode/itemName/inspectionMethod/standardText/resultType`；`FrontlinePqcInspectionSubmitReqVO` 没有项目级设备、设备编号、标准上下限字段。
- `IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue`：PQC 组长页存在 PQC tab，但提交内容解析使用固定四项 `length/appearance/seal/pressure`，不是按真实检验项目动态展示。
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/feedback/vo/frontline/MesFrontlinePqcSubmitReqVO.java`：PQC 提交请求不包含项目级设备或设备编号。
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlinePqcSubmitCommand.java`：PQC 提交命令不包含项目级设备或设备编号。
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlinePqcInspectionItem.java`：当前项目记录仅含方法、标准、结果类型。
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/qa/regulation/MesQaInspectionRegulationItemDO.java`：QA 规程项目当前含方法、标准文本、结果类型和检验数量配置，未发现数值上下限与项目设备绑定字段。
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/pro/processpool/pqc/MesPqcInspectionPieceDetailDO.java`：PQC 逐件明细已冻结方法、标准、结果类型、实测值、判定，但未冻结设备、设备编号、上下限。
- `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlinePqcContextServiceImpl.java`：提交时将方法和标准写入逐件明细；创建工序池 PQC 事件时 `deviceId(null)`、`workstationId(null)`。
- `IntRuoyiBackend/sql/mysql/20260802_mes_qa_inspection_regulation.sql` 与 `20260802_mes_pqc_inspection_task.sql`：现有迁移覆盖 QA 规程项目、PQC 任务和逐件明细，但不覆盖项目级设备、设备编号、数值上下限。
- `docs/acceptance/production-execution-main-loop/*`：既有验收文档要求 PQC 正式提交、PQC 组长复核、质量结果、签名和 trace 形成闭环，并禁止用默认设备、默认成功或 mock 成功替代正式来源。

## Product Summary

用户口径可以归纳为“PQC 检验项目级事实闭环”：

- 不同检验项目可以要求不同检验设备。
- 同一检验设备可以有不同设备编号可选。
- 接收标准来自检验规程，不由前端手填或当前页面推断。
- 数值型项目的接收标准需要支持参数上下限。
- 每个检验项目需要两个显性操作：“接收标准”和“检验方法”，点击后弹出对应内容。
- PQC 组长和 QA 相关业务必须同步改造，不能只改 PQC 填写页。

## Current System Summary

当前系统已经具备以下基础：

- PQC 填写入口、订单/工序/员工选择、首检/巡检/末检、逐件检验、签名编号和提交动作已经存在。
- PQC 检验项目已经从当前工序的 QA 规程快照中读取，前端可以获得项目编码、名称、检验方法、标准文本和结果类型。
- 后端提交时已经基于 QA 规程项目生成逐件明细，并把 `inspectionMethod`、`standardText` 和 `resultType` 冻结进 `mes_pqc_inspection_piece_detail`。
- PQC 组长工作台已经有 PQC tab 和复核入口。
- 生产执行主闭环验收文档已经要求 PQC 提交进入工序池事件、PQC 组长复核、电子签名和统一 trace。

当前主要缺口：

- PQC 检验项目没有正式项目级检验设备配置和设备编号选项。
- 当前 API、前端类型、提交请求、提交命令都不承载项目级设备选择。
- 数值型接收标准仍主要是 `standardText` 文本，缺正式上下限字段，无法可靠做后端数值判定。
- PQC 填写页把方法/标准压缩成小字元信息，不符合“按钮 + 弹窗”口径。
- PQC 组长页按固定四项解析 payload，不适配真实 QA 规程项目，也看不到项目级设备和标准方法快照。
- QA 规程发布模型缺项目级设备绑定、设备编号范围和数值上下限的正式来源。
- 历史追溯不能完整回答每个 PQC 项目“用什么设备、哪个编号、按什么方法、用哪个标准、实测多少、判定如何”。

## First Version Scope

第一版必须覆盖以下正式行为：

- PQC 填写页按检验项目逐项展示设备、设备编号、检测结果、判定状态，以及“接收标准”“检验方法”按钮。
- 检验设备选项和设备编号选项必须来自 QA 规程发布版本的项目级配置或经产品确认的正式主数据绑定。
- 设备编号选项必须随所选检验设备过滤，不能跨设备混选编号。
- 接收标准、检验方法、数值上下限、单位、结果类型必须来自当前 PQC 任务绑定的 `regulationVersionId`，提交时冻结到 PQC 明细。
- 数值型项目必须由后端按下限/上限判定合格或不合格；前端只做交互提示，不作为最终判定来源。
- 选择型项目按规程定义的结果类型和标准文本进行提交校验，至少支持合格/不合格。
- PQC 组长列表和详情必须按真实检验项目展示项目级设备、设备编号、接收标准、检验方法、实测值、判定和总体结果。
- PQC 组长复核只能基于已冻结的项目级明细，不得动态读取最新规程覆盖历史提交。
- QA 规程维护/发布必须能配置每个项目的检验方法、接收标准、上下限和设备/编号范围；发布后生成版本快照。
- QA 查看、审核或放行时必须看到与 PQC 提交时一致的项目级快照。
- 统一 trace 和历史详情必须显示项目级设备、编号、标准、方法和判定，并保留提交时的规程版本。

## Non-Goals

- 不把生产报工设备参数逻辑直接复用为 PQC 项目设备逻辑，除非后续正式设计确认两者共用同一设备主数据和权限模型。
- 不用前端文案、固定项目名、payload 四项字段、设备名称字符串或当前工位默认设备推断检验设备。
- 不用 `formBindings`、批记录表单、表单槽位或工序开始配置推断 PQC 检验设备或检验标准。
- 不为缺失设备配置、缺失上下限或缺失 QA 规程快照提供默认合格、默认设备、默认编号或空标准成功。
- 不在第一版处理仪器校准、计量有效期、设备维修状态联锁；这些可作为后续扩展，但第一版要预留字段或阻塞点。

## Functional Requirements

### FR-01 PQC 项目级设备配置

- QA 规程的每个检验项目必须能配置一个或多个允许的检验设备。
- 每个允许设备必须能解析到可选择的设备编号列表。
- 若业务需要“设备类型 -> 设备编号”两级选择，则规程项目必须保存设备类型/设备分类和允许设备范围；若业务需要指定设备，则规程项目必须保存设备 ID/编号。
- 同一 PQC 任务内，不同检验项目可以绑定不同设备范围。
- 同一检验项目如果允许多台设备，PQC 员工必须在提交前选择实际使用设备和设备编号。

### FR-02 PQC 填写页交互

- 每个检验项目卡片必须包含项目名称、结果录入入口、当前进度、设备选择、设备编号选择、判定状态，以及两个按钮：“接收标准”“检验方法”。
- 点击“接收标准”弹窗显示：项目名称、规程版本、标准文本、下限、上限、单位、结果类型、适用检验类型、版本生效信息。
- 点击“检验方法”弹窗显示：项目名称、规程版本、检验方法全文、所需检验设备说明、注意事项或空缺阻塞说明。
- 当前没有正式标准或方法时，弹窗不得用“未配置”继续放行；应显示明确阻塞原因，并阻止提交。
- 数值型项目的输入控件应显示单位、上下限提示和超限状态；最终判定仍由后端提交校验生成。

### FR-03 提交数据与后端校验

- PQC 提交请求需要新增 `itemResults` 或等价结构，按 `itemCode` 承载：
  - `itemCode`
  - `selectedEquipmentId`
  - `selectedEquipmentCode`
  - `selectedEquipmentName`
  - `selectedEquipmentNumber`
  - `sampleValues`
  - `clientItemJudgement` 可选，仅用于前端提示，不作为最终判定
- 后端必须按 `pqcTaskId + regulationVersionId + itemCode` 重新读取任务发布快照，校验项目存在、设备允许、编号属于设备、样本数满足检验数量。
- 后端必须从规程快照写入 `inspectionMethod`、`standardText`、`standardLowerLimit`、`standardUpperLimit`、`standardUnit`、`resultType`，不得信任客户端传来的方法和标准。
- 数值型样本必须转换为数值后按上下限判定；无法转换、缺上下限或缺单位规则时 fail fast。
- 选择型样本必须属于规程允许值；出现“不合格”或业务定义的失败值时，项目判定和总体 PQC 结果必须失败。
- 任一项目缺设备、缺设备编号、缺标准、缺方法、缺结果或缺规程快照时，本次 PQC 提交整体失败，不写入终态。

### FR-04 数据快照与追溯

- PQC 提交后必须冻结每个项目的设备、设备编号、检验方法、接收标准、上下限、单位、结果类型、实测值和判定。
- 当前 `mes_pqc_inspection_piece_detail` 可继续承载逐件样本快照，但需要补齐项目级设备和标准上下限字段，或新增项目级明细表后由逐件明细引用。
- 如果同一项目有多个样本，设备和标准快照必须对每个样本可追溯；可通过项目级明细聚合展示，不要求 UI 重复显示每个样本的相同设备信息。
- `mes_pro_process_pool_event` 单个 `deviceId` 字段不能代表多项目多设备事实；必须新增结构化 PQC 项目明细投影或设备摘要字段，避免把多设备压扁成一个默认设备。
- 历史提交详情和统一 trace 必须优先读取提交时冻结快照，不得因 QA 规程后续升版而改变历史显示。

### FR-05 PQC 组长业务修改

- PQC 组长列表的“提交内容”必须从固定四项解析改为按真实项目明细动态渲染。
- 组长详情必须展示每个项目：
  - 检验项目名称/编码
  - 检验设备和设备编号
  - 检验方法
  - 接收标准/上下限/单位
  - 样本值摘要
  - 项目判定
  - 总体 PQC 结果
- 组长复核时，应基于冻结明细进行“正确/不正确”判定和签名，不允许修改原 PQC 员工提交的设备、标准、方法或样本值。
- 如果 PQC 提交缺项目级明细，组长详情必须显示阻塞状态，不能展示“固定四项未填写”后继续复核通过。

### FR-06 QA 业务修改

- QA 规程项目维护必须新增检验设备/设备编号范围、接收标准、数值上下限、单位、结果类型和检验方法配置。
- QA 规程发布时必须校验每个启用项目的设备来源、编号范围、标准/上下限和方法完整性。
- QA 查看历史 PQC 或执行质量审核时，必须看到 PQC 提交时冻结的项目级快照。
- QA 规程升版后，新 PQC 任务使用新版本；旧 PQC 任务和已提交记录继续使用旧版本快照。
- QA 如果发现规程项目缺设备或缺上下限，应阻止发布或阻止生成可填写 PQC 任务，而不是让 PQC 填写端补猜。

### FR-07 权限与审计

- PQC 员工需要查看标准/方法、选择设备编号和提交 PQC 的权限。
- PQC 组长需要查看项目级 PQC 明细和复核权限。
- QA 需要维护规程项目设备/标准/方法、发布规程、查看历史快照的权限。
- 修改 QA 规程项目设备、编号范围、标准上下限或方法必须进入规程版本审计；已发布版本不可原地改写历史。

## Data Model Change Proposal

具体 schema 必须在实施前通过数据库规则核对真实表结构。第一版建议如下：

### QA 规程项目

在 `mes_qa_inspection_regulation_item` 增加或通过子表承载：

- `standard_lower_limit decimal(...) null`：数值下限。
- `standard_upper_limit decimal(...) null`：数值上限。
- `standard_unit varchar(...) null`：单位。
- `standard_precision int null`：显示/比较精度。
- `equipment_required bit/int not null`：是否必选设备。

新增子表 `mes_qa_inspection_regulation_item_equipment` 或等价正式结构：

- `regulation_version_id`
- `item_code`
- `equipment_type` 或 `equipment_category_id`
- `equipment_id`
- `equipment_code`
- `equipment_name`
- `equipment_number`
- `default_flag`
- `sort`

如果当前设备编号已经由 `mes_dv_machinery`、工装工具或设备台账承载，实施前必须明确唯一正式来源；不得新建重复主数据。

### PQC 提交快照

方案 A，最小改造：扩展 `mes_pqc_inspection_piece_detail`：

- `equipment_id`
- `equipment_code`
- `equipment_name`
- `equipment_number`
- `standard_lower_limit`
- `standard_upper_limit`
- `standard_unit`
- `standard_precision`

方案 B，长期更优：新增 `mes_pqc_inspection_item_detail`：

- `task_id`
- `item_code`
- `item_name`
- `equipment_id/code/name/number`
- `inspection_method`
- `standard_text`
- `standard_lower_limit/upper_limit/unit/precision`
- `result_type`
- `item_judgement`
- `sample_count`

再让 `mes_pqc_inspection_piece_detail` 增加 `item_detail_id`，只保存样本序号、实测值和样本判定。第一版若优先交付，可以采用方案 A；若要避免重复字段并支持 QA 历史审计，建议采用方案 B。

### 工序池事件与 trace

- 不建议把多个项目设备硬塞进 `mes_pro_process_pool_event.device_id`。
- 应新增 PQC 项目明细读取模型，或者在 PQC record/detail 中保存 `itemSnapshotsJson`，供组长详情、QA 详情和 trace 使用。
- 如果一个 PQC 提交只有一个唯一设备，可同步写入事件级 `deviceId` 作为摘要；如果存在多个设备，事件级设备必须显示“多设备”摘要并通过项目明细展开，不得选择第一台设备冒充完整事实。

## API Change Proposal

### PQC 上下文接口

`FrontlineDeviceRouteProcessVO.inspectionItems[]` 需要扩展：

- `equipmentRequired`
- `equipmentOptions[]`
- `equipmentOptions[].equipmentId/code/name`
- `equipmentOptions[].numbers[]`
- `standardLowerLimit`
- `standardUpperLimit`
- `standardUnit`
- `standardPrecision`
- `standardText`
- `inspectionMethod`
- `resultType`

### PQC 提交接口

`FrontlinePqcInspectionSubmitReqVO` 需要扩展：

- `itemResults[]`
- `itemResults[].itemCode`
- `itemResults[].selectedEquipmentId`
- `itemResults[].selectedEquipmentNumber`
- `itemResults[].sampleValues[]`

`rawPayload` 可以保留前端上下文快照，但不能作为后端判断设备、标准和方法的权威来源。

### 组长详情/QA 详情接口

详情响应需要新增 `pqcItemDetails[]`：

- `itemCode/itemName`
- `equipmentName/equipmentCode/equipmentNumber`
- `inspectionMethod`
- `standardText`
- `standardLowerLimit/standardUpperLimit/standardUnit`
- `resultType`
- `sampleValues`
- `itemJudgement`

列表摘要可以展示项目数量、失败项目、设备摘要和总体 PQC 结果。

## Frontend Change Proposal

### PQC 填写页

- `PqcInspectionItem` 类型新增设备选项和标准上下限字段。
- 检验项目卡片新增：
  - 检验设备选择控件
  - 设备编号选择控件
  - “接收标准”按钮
  - “检验方法”按钮
  - 项目判定状态标签
- 新增两个弹窗或一个通用弹窗：
  - `activePqcStandardItem`
  - `activePqcMethodItem`
- `buildPqcInspectionSubmitPayload` 改为构造项目级 `itemResults`。
- `buildPqcPieceValuesPayload` 保留样本值，但不再单独承担所有项目事实。
- `PQC_SIMPLIFIED_FIELD_CODES` 需要新增项目级字段或改为不再依赖简化模板白名单承载项目明细；具体以接口结构为准。

### PQC 组长页

- 移除固定四项 `PQC_SUBMISSION_CONTENT_DEFINITIONS` 对 PQC 正式明细的依赖。
- 列表摘要优先使用后端结构化 `pqcItemDetails` 或等价字段。
- 详情抽屉新增项目级表格，展示设备、编号、方法、标准、样本值和判定。
- 原始 payload 只作为审计附录，不作为组长判断的主展示。

### QA 相关页面

- QA 规程项目维护页增加设备/编号范围和上下限配置。
- QA 规程发布前展示完整性检查结果。
- QA 历史查看页使用冻结快照展示，不直接读取当前最新规程项目覆盖历史。

## Business Rules

- 项目级事实优先：PQC 的设备、编号、标准、方法、结果和判定都归属于检验项目，不归属于整个 PQC 页面。
- 规程版本优先：标准、方法、设备范围和上下限都来自 `regulationVersionId` 对应发布快照。
- 后端判定优先：前端可以提示超限，但最终合格/不合格由后端按发布规程判定。
- 快照不可变：提交后的项目标准、方法和设备快照不得因 QA 规程升版而变化。
- 缺正式来源即阻塞：缺设备绑定、缺设备编号范围、缺数值上下限、缺方法、缺标准、缺任务规程版本时，阻止填写或提交。
- 审核不改原始事实：PQC 组长和 QA 审核只增加复核/审核结论，不改 PQC 员工提交明细。

## States and Transitions

- QA 规程项目草稿：可编辑设备、编号范围、方法、标准、上下限。
- QA 规程发布：完整性检查通过后生成发布版本快照。
- PQC 任务生成：绑定 `regulationVersionId` 和该版本项目快照。
- PQC 填写中：员工选择项目设备/编号并填写样本值。
- PQC 提交校验失败：不写终态，返回项目级缺口或超限原因。
- PQC 已提交：冻结项目级快照和总体结果。
- PQC 组长待复核：组长查看冻结项目明细并签名复核。
- PQC 组长已复核：保存组长判定、签名、时间、备注；不改原提交。
- QA 审核/追溯：查看冻结快照和复核结论，必要时给出质量审核结论。

## Edge Cases

- 同一项目允许多设备：必须选择实际设备和编号。
- 同一设备有多个编号：编号必须按设备过滤。
- 一个 PQC 任务中多个项目使用不同设备：trace 展示多设备摘要并展开项目明细。
- 设备编号失效但任务已提交：历史继续显示提交快照；新任务不得选择失效编号。
- QA 规程升版后旧任务未提交：若任务已绑定旧 `regulationVersionId`，应继续使用旧版本，除非业务明确要求重新生成任务。
- 数值等于上下限：按规程定义的闭区间/开区间规则判定；当前系统尚未发现开闭区间字段，第一版默认闭区间前需要产品确认。
- `standardText` 与上下限不一致：发布前必须阻塞；不得以文本覆盖结构化上下限。
- 客户端篡改方法/标准：后端忽略客户端方法/标准，以规程快照为准。

## Acceptance Criteria

- PQC 填写页每个检验项目都能看到设备、设备编号、“接收标准”和“检验方法”入口。
- 设备编号随设备选择过滤。
- 点击“接收标准”显示规程版本、标准文本和上下限。
- 点击“检验方法”显示规程版本和检验方法。
- 缺设备、缺编号、缺标准、缺方法或缺上下限时，页面或接口明确阻塞。
- PQC 提交后，数据库或详情接口能按项目查到设备、编号、标准、方法、样本值和判定。
- 数值超出上下限时，后端判定项目失败，总体 PQC 失败。
- PQC 组长列表/详情按真实项目动态展示，不再依赖固定四项。
- PQC 组长复核保存签名和复核结论，不改原始项目明细。
- QA 规程升版后，历史 PQC 详情仍显示提交时版本快照。
- 所有测试计划都能映射到 BDD 场景和 RED/GREEN 命令或明确阻塞条件。

## Product Blockers

- 当前系统证据尚未确认“检验设备”和“设备编号”的唯一正式主数据来源：可能是设备台账、工装工具、工位设备绑定或其它 QA 专用设备表。实施前必须确认，不能从页面字符串或生产设备列表推断。
- 当前 QA 规程项目 schema 未发现数值上下限、单位和开闭区间字段。若数值上下限是用户必需范围，必须先补正式 schema 和发布快照。
- 当前 PQC 组长详情接口是否已有结构化逐件明细响应需要实施阶段继续核对；若没有，需要后端先补读模型，前端不能只解析 raw payload。
- QA 规程维护页面和 QA 审核页面的具体入口/权限未在本次文档任务中完全核对；实施前必须定位正式页面和权限码。

## Review Optimizations Applied

- 将原始“页面缺按钮”的需求提升为“PQC 检验项目级事实闭环”，避免只做前端展示。
- 明确 PQC 组长和 QA 必须同步改造，避免审核端看不到设备/标准/方法。
- 把设备、编号、标准、方法全部绑定到 `itemCode + regulationVersionId`，避免按页面、工序或 payload 推断。
- 把数值上下限列为正式结构化字段和阻塞项，避免只用 `standardText` 做不可测试规则。
- 明确历史提交使用冻结快照，不随 QA 规程升版变化。
- 明确禁止默认设备、默认编号、默认合格、固定四项解析和 raw payload 主展示。
