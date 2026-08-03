# PQC 项目级设备、接收标准与检验方法 BDD/TDD 计划

## Purpose and Scope

本文档把 PQC 项目级设备、设备编号、接收标准、检验方法、PQC 组长复核、QA 快照追溯转换为可执行 BDD 场景和严格 TDD 序列。本文档不写生产代码，只定义后续实施必须先 RED、再 GREEN、再回归的验证路径。

## Evidence Reviewed

- PQC 填写入口：`IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchPqcFillPage.vue`
- PQC 填写主体：`IntRuoyiFronted/src/views/mes/pro/feedback/FrontlineFixedTemplatePanel.vue`
- PQC payload 白名单：`IntRuoyiFronted/src/views/mes/pro/feedback/frontlineTemplate.ts`
- PQC API 类型：`IntRuoyiFronted/src/api/mes/pro/feedback/index.ts`
- PQC 组长页面：`IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue`
- PQC 提交请求/命令：`MesFrontlinePqcSubmitReqVO.java`、`MesFrontlinePqcSubmitCommand.java`
- QA 规程项目：`MesQaInspectionRegulationItemDO.java`
- PQC 逐件明细：`MesPqcInspectionPieceDetailDO.java`
- PQC 提交服务：`MesFrontlinePqcContextServiceImpl.java`
- 现有验收计划：`docs/acceptance/production-execution-main-loop/*`

## Feature Scenarios

### BDD: PQC 员工按项目选择检验设备和设备编号

Given PQC 员工选择活跃生产工单、路线工序、PQC 任务和 QA 规程发布版本  
And 当前规程版本中每个检验项目都配置了允许设备和设备编号范围  
When PQC 员工打开 PQC 填写页  
Then 页面按检验项目展示检验设备选择、设备编号选择、“接收标准”按钮和“检验方法”按钮  
And 设备编号选项只来自当前项目所选检验设备  
And 不同检验项目可以显示不同设备和编号范围

### BDD: PQC 员工查看项目接收标准

Given PQC 员工正在填写一个数值型检验项目  
And QA 规程发布版本为该项目配置了标准文本、下限、上限、单位和结果类型  
When PQC 员工点击“接收标准”  
Then 系统弹出该项目的接收标准  
And 弹窗展示规程版本、标准文本、下限、上限、单位和结果类型  
And 弹窗内容来自任务绑定的规程版本快照

### BDD: PQC 员工查看项目检验方法

Given PQC 员工正在填写任一 PQC 检验项目  
And QA 规程发布版本为该项目配置了检验方法  
When PQC 员工点击“检验方法”  
Then 系统弹出该项目的检验方法  
And 弹窗展示项目名称、规程版本和检验方法全文  
And 不使用前端硬编码文案替代规程方法

### BDD: PQC 提交冻结项目级快照

Given PQC 员工已为每个检验项目选择合法设备和设备编号  
And 已填写所有样本值并完成电子签名  
When PQC 员工提交检验结果  
Then 后端保存 PQC 任务终态和项目/逐件明细  
And 每个项目冻结设备、设备编号、接收标准、检验方法、上下限、单位、结果类型、实测值和判定  
And 工序池 PQC 事件或其关联读模型可以追溯这些项目级快照

### BDD: PQC 组长按真实项目复核

Given PQC 员工已经提交带项目级快照的 PQC 结果  
When PQC 组长打开 PQC 组长看板和提交详情  
Then 列表摘要按真实检验项目展示失败项目和设备摘要  
And 详情展示每个项目的设备、编号、方法、标准、样本值和判定  
And PQC 组长复核只保存复核结论、签名、时间和备注，不改原始项目明细

### BDD: QA 规程升版不改变历史 PQC 快照

Given QA 已发布 V1 规程并产生一条 PQC 提交  
And QA 后续发布 V2 规程并修改某项目标准、方法或设备范围  
When QA、PQC 组长或追溯用户查看 V1 提交历史  
Then 系统仍显示 V1 提交时冻结的设备、编号、接收标准和检验方法  
And 新建 PQC 任务才使用 V2 规程快照

## Failure Scenarios

### BDD: 缺项目级设备绑定时阻塞填写或提交

Given PQC 任务绑定的规程项目缺少正式检验设备来源  
When PQC 员工打开或提交该项目  
Then 系统显示“检验项目缺少设备配置”或等价明确错误  
And 本次 PQC 提交不得成功  
And 不使用工位默认设备、生产设备、固定字符串或第一台设备作为替代

### BDD: 设备编号不属于当前设备时拒绝提交

Given 某检验项目允许设备 A 和设备 B  
And 设备编号 A-01 只属于设备 A  
When 客户端提交设备 B 搭配设备编号 A-01  
Then 后端拒绝提交并返回项目级错误  
And 不写入 PQC 任务终态、逐件明细终态或工序池 PQC 事件

### BDD: 数值型项目缺上下限时阻塞

Given 某数值型检验项目缺少正式下限或上限  
When QA 发布规程或 PQC 员工提交检验结果  
Then 系统阻塞发布或提交  
And 不把 `standardText` 文本当作可解析上下限的 fallback

### BDD: 客户端篡改接收标准或检验方法无效

Given 客户端 raw payload 中传入了与规程快照不一致的标准或方法  
When PQC 员工提交检验结果  
Then 后端忽略客户端标准和方法  
And 以 `regulationVersionId + itemCode` 对应的发布快照写入明细  
And 如果客户端项目不存在或版本不匹配则提交失败

### BDD: PQC 组长详情缺项目级明细时阻塞复核

Given 某 PQC 提交没有正式项目级设备/标准/方法快照  
When PQC 组长打开详情准备复核  
Then 页面显示 PQC 明细缺失阻塞  
And 不使用固定 `length/appearance/seal/pressure` 或 raw payload 猜测明细  
And 不允许组长复核通过

## Boundary Scenarios

### BDD: 多项目多设备 trace 展开

Given 一个 PQC 提交包含两个检验项目  
And 两个项目选择了不同检验设备和设备编号  
When 用户打开统一 trace 或历史详情  
Then 摘要显示多设备或项目设备摘要  
And 明细逐项展示真实设备和编号  
And 不把第一台设备写成整条 PQC 事件的唯一设备事实

### BDD: 数值等于上下限的判定

Given 规程项目定义数值下限为 10、上限为 20 且区间规则为闭区间  
When PQC 员工提交样本值 10 和 20  
Then 后端判定样本合格  
And 如果后续产品要求开区间或半开区间，必须先补正式区间规则字段和测试

## TDD Sequence

| ID | 范围 | RED 命令 | 预期 RED 原因 | 最小 GREEN 目标 | 回归检查 |
| --- | --- | --- | --- | --- | --- |
| T01 | QA/PQC schema | `workdir=IntRuoyiBackend; mvn -pl yudao-module-mes -am "-Dtest=MesQaPqcSchemaTest#qaRegulationItemSchemaMustProvideEquipmentAndNumericStandardSnapshot" test` | QA 规程项目缺设备绑定、上下限、单位字段或子表 | schema/DO/SQL 增加正式字段或子表 | 复跑 `MesQaPqcSchemaTest` 全类 |
| T02 | PQC 上下文返回 | `workdir=IntRuoyiBackend; mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest#shouldResolveItemEquipmentOptionsAndStandardBoundsFromRegulationVersion" test` | PQC 上下文项目只返回方法/标准文本/结果类型 | 返回项目设备选项、编号范围、上下限、单位 | 复跑 PQC 上下文相邻测试 |
| T03 | PQC 提交校验 | `workdir=IntRuoyiBackend; mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest#shouldRejectPqcSubmitWhenItemEquipmentNumberIsNotAllowed" test` | 当前提交请求没有项目级设备/编号，无法校验 | 提交结构含项目设备/编号并后端校验 | 复跑 PQC 提交成功/失败测试 |
| T04 | 数值上下限判定 | `workdir=IntRuoyiBackend; mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest#shouldJudgeNumericPqcItemByRegulationBounds" test` | 当前仅按合格/不合格或总体结果判定 | 后端按上下限生成样本/项目判定和总体结果 | 复跑质量门禁测试 |
| T05 | 快照冻结 | `workdir=IntRuoyiBackend; mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest#shouldFreezeItemEquipmentMethodAndStandardWhenSubmittingPqc" test` | 逐件明细缺设备/编号/上下限快照 | 明细或项目明细冻结完整快照 | 复跑 `MesProcessPoolPqcEventTest` |
| T06 | 工序池/trace | `workdir=IntRuoyiBackend; mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolPqcEventTest#shouldExposePqcItemEquipmentSnapshotsForLeaderAndTrace" test` | 工序池 PQC 事件设备为空，读模型无项目设备 | PQC 事件关联读模型能返回项目快照 | 复跑生产执行主闭环相关测试 |
| T07 | 前端 PQC 填写静态合同 | `workdir=IntRuoyiFronted; node tests/e2e/pqc-item-equipment-standard-method-static.spec.js` | 页面缺设备/编号控件和标准/方法按钮 | PQC 项目卡片和弹窗结构存在，payload 构造项目级结果 | 复跑相邻 PQC 静态合同和 `pnpm ts:check` |
| T08 | 前端组长静态合同 | `workdir=IntRuoyiFronted; node tests/e2e/pqc-leader-item-snapshot-static.spec.js` | 组长页固定四项解析，缺动态项目表格 | 组长列表/详情按项目快照展示 | 复跑组长工作台静态合同 |
| T09 | QA 规程维护静态/后端合同 | `workdir=IntRuoyiFronted; node tests/e2e/qa-regulation-item-equipment-standard-static.spec.js` 和目标后端测试 | QA 规程维护未配置设备/上下限 | QA 发布前完整性校验和页面配置入口存在 | 复跑 QA 规程相关测试 |
| T10 | 真实 E2E | `workdir=IntRuoyiFronted; pnpm e2e:pqc-item-equipment-standard-method:real` | 脚本、账号、测试数据、设备编号或页面入口可能缺失 | Playwright 走真实 PQC 填写、组长复核、QA/trace 查看路径 | API 只做只读核验 |

## RED Commands

实施时必须先新增或修改测试，使旧系统出现明确失败：

- `mvn -pl yudao-module-mes -am "-Dtest=MesQaPqcSchemaTest#qaRegulationItemSchemaMustProvideEquipmentAndNumericStandardSnapshot" test`
- `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest#shouldResolveItemEquipmentOptionsAndStandardBoundsFromRegulationVersion" test`
- `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest#shouldRejectPqcSubmitWhenItemEquipmentNumberIsNotAllowed" test`
- `mvn -pl yudao-module-mes -am "-Dtest=MesFrontlinePqcContextServiceTest#shouldJudgeNumericPqcItemByRegulationBounds" test`
- `node tests/e2e/pqc-item-equipment-standard-method-static.spec.js`
- `node tests/e2e/pqc-leader-item-snapshot-static.spec.js`

## Expected Failures

- schema 测试先失败在缺项目级设备、设备编号、上下限字段或子表。
- PQC 上下文测试先失败在 `inspectionItems` 没有设备选项、编号范围和标准上下限。
- PQC 提交测试先失败在请求/命令没有项目级设备字段，或后端没有非法编号校验。
- 数值判定测试先失败在后端不按上下限判断样本值。
- 前端静态合同先失败在缺“接收标准”“检验方法”按钮、缺弹窗和缺项目级提交结构。
- 组长静态合同先失败在固定四项解析仍存在，或详情不渲染项目快照表格。

## GREEN Commands

最小实现完成后必须得到明确 PASS：

- `mvn -pl yudao-module-mes -am "-Dtest=MesQaPqcSchemaTest,MesFrontlinePqcContextServiceTest,MesProcessPoolPqcEventTest" test`
- `node tests/e2e/pqc-item-equipment-standard-method-static.spec.js`
- `node tests/e2e/pqc-leader-item-snapshot-static.spec.js`
- `node tests/e2e/qa-regulation-item-equipment-standard-static.spec.js`
- `pnpm ts:check`

真实 E2E 只有在账号、租户、运行态、设备编号和测试数据齐备时运行：

- `pnpm e2e:pqc-item-equipment-standard-method:real`

如果脚本或真实数据缺失，记录为 `E2E BLOCKED`，不得改用 API-only 或静态合同冒充真实 E2E 通过。

## Refactor Checks

- 不保留固定 `length/appearance/seal/pressure` 作为 PQC 正式明细主路径。
- 不在前端 hardcode 标准、方法、设备编号。
- 不从 raw payload 解析方法/标准作为后端权威来源。
- 不在后端用 `deviceId(null)` 掩盖 PQC 项目设备事实；如果事件级字段无法表达多设备，必须提供项目级读模型。
- 不捕获并吞掉缺规程、缺设备或缺编号异常。

## Required Test Data

真实 E2E 需要任务自有或已确认测试数据：

- 测试租户。
- PQC 员工账号。
- PQC 组长账号。
- QA 规程维护/审核账号。
- 活跃生产工单和路线工序。
- PQC 任务绑定 QA 规程发布版本。
- 至少两个检验项目：
  - 项目 A：数值型，设备 A，编号 A-01/A-02，下限 10，上限 20。
  - 项目 B：选择型，设备 B，编号 B-01，标准文本和检验方法不同于项目 A。
- 电子签名测试能力。
- 可清理的任务标识，例如 `PQC-EQUIP-STD-<runId>`。

## Reset Procedure

- 清理任务自有 PQC 任务、项目明细、逐件明细、工序池 PQC 事件、组长复核记录和测试规程版本。
- 不清理共享设备主数据、共享员工、共享角色或非任务自有历史提交。
- 如果测试临时创建 QA 规程版本，必须按任务标识定位并清理或作废。

## Test Blockers

- 缺正式检验设备/设备编号主数据来源。
- 缺 QA 规程项目上下限 schema。
- 缺 PQC 组长结构化项目明细接口。
- 缺 QA 规程维护页面或权限码。
- 缺真实测试租户、PQC 员工、PQC 组长、QA 账号或电子签名能力。
- 缺本机前后端运行态和 Playwright 浏览器。

## Evidence Log Template

实施任务的 `execution-log.md` 必须使用以下标记：

- `BDD: PQC item-level equipment selection -> Given/When/Then`
- `BDD: PQC item-level standard and method dialogs -> Given/When/Then`
- `BDD: PQC leader item snapshot review -> Given/When/Then`
- `BDD: QA regulation version snapshot immutability -> Given/When/Then`
- `RED: <command> -> FAIL, <expected reason>`
- `GREEN: <command> -> PASS`
- `E2E BLOCKED: <command> -> BLOCKED, <missing precondition>`
