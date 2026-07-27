# PRD: MES 全量测试回归清零

## Goal

在保留 eDHR 工作任务“通知全部有效候选人”正式行为的前提下，修复 `yudao-module-mes` 完整测试套件中的全部现有 failure 和 error，使未筛选、未降级的 MES 模块测试稳定通过，并恢复该任务的完整回归放行能力。

## Scope

- 修复 2026-07-27 基线完整运行中 41 个失败测试套件对应的正式根因。
- 获取并治理真实 Word/Excel fixture，使相关测试不再依赖个人桌面或固定盘符。
- 修复过时的前端目录、路由和共享组件静态契约。
- 修复 schema、迁移、Spring 测试上下文、Bean 注入、H2 数据隔离和唯一键问题。
- 修复 Mockito stubbing 漂移和新增服务依赖未装配问题，同时保持严格 Mockito 校验。
- 对齐 eDHR 候选/配置快照、批次执行、任务门禁、演练就绪和 legacy-process 契约。
- 对齐批记录解析、JSON 类型、布局、形状和路线识别的正式规则。
- 对齐自动排产、工单编码、路线版本、排产订单准入、风险和容量状态契约。
- 保持并重新验证原通知行为的目标 3 个测试和服务类 66 个测试。
- 以完整命令 `mvn -pl yudao-module-mes test` 作为最终验收。

## Non-Goals

- 不修复 `yudao-module-infra` 或其他 Maven 模块的独立失败，除非 MES 正式依赖契约明确要求且用户另行扩展范围。
- 不通过跳过、排除、禁用、删除测试或减少测试发现范围实现通过。
- 不通过放宽断言、删除业务校验、改成默认成功或接受错误结果实现通过。
- 不创建伪造、空白、裁剪或内容近似的 Word/Excel fixture。
- 不引入 fallback、兼容 shim、吞异常、mock success 或默认数据。
- 不修改与本任务无关的前端并发改动、其他任务文档、运行态、远端服务或业务数据。
- 不把“工序开始”“批记录表单”和“表单槽位 `formBindings`”混为同一数据来源。

## User or System Scenarios

### Scenario 1: 完整 MES 回归

Given 当前分支包含所有正式修复和真实测试依赖  
When 在 `E:\IntRuoyi\IntRuoyiBackend` 执行 `mvn -pl yudao-module-mes test`  
Then Maven 返回 `BUILD SUCCESS`，完整套件没有 failure 或 error，且没有通过筛选或排除隐藏测试

### Scenario 2: 缺少真实 fixture

Given 权威 Word 或 Excel fixture 尚未取得或完整性未确认  
When 准备修复依赖该文件的解析、导入或布局测试  
Then 任务明确阻塞并报告缺失文件，不生成替代文件，不返回模拟成功

### Scenario 3: eDHR 通知回归保护

Given 一个工作任务候选快照包含多个有效账号或重复账号  
When 创建并发送工作任务通知  
Then 每个有效候选账号每个任务收到一次通知，且目标 3 个测试与服务类 66 个测试继续通过

### Scenario 4: 测试契约与生产行为不一致

Given 某失败可能来自生产代码缺陷或陈旧测试期望  
When 分析该套件的首个失败  
Then 依据当前正式 schema、业务规则和服务契约确定根因，并以 RED/GREEN 证明修复，而不是直接放宽断言

### Scenario 5: 并发文件冲突

Given 共享工作区中另一任务已修改本任务计划触及的文件  
When 本任务准备编辑该文件  
Then 停止该文件的修改并协调所有权，不覆盖、回退或混入并发任务改动

## Functional Requirements

- **FR-01 通知行为保护：** 系统必须继续按每个工作任务自身的有效候选快照发送站内信，并在任务内按账号去重。
- **FR-02 Fixture 真实性：** Word/Excel 解析和导入测试必须使用经确认的真实原始 fixture；缺失时必须 fail fast。
- **FR-03 Fixture 可移植性：** 测试不得继续依赖 `C:\Users\BJB110\Desktop\...`、`D:\ocr2\resource\...` 等个人绝对路径。
- **FR-04 前端契约定位：** MES 静态契约必须定位当前 `IntRuoyiFronted` 工程及其正式路由/共享批记录组件，不得引用废弃的 `yudao-ui-admin-vue3` 路径。
- **FR-05 Schema 契约：** 运行时 schema、迁移和测试 schema 必须满足批记录、路线版本和当前服务所声明的正式字段及约束。
- **FR-06 Spring 测试装配：** 数据库与集成测试所需 Bean、配置和依赖必须完整注入；ApplicationContext 必须成功启动。
- **FR-07 数据隔离：** H2 测试数据必须在套件和用例间保持可重复、互不污染，不得触发非预期唯一键冲突。
- **FR-08 Mockito 严格性：** 测试 double 必须与当前调用参数和依赖关系一致；应删除无效 stubbing 或补齐准确 stubbing，不得改用 lenient 规避。
- **FR-09 路线服务依赖：** 路线版本、所有者权限、受控内容和路线版本解析相关测试必须装配当前正式依赖并验证真实调用契约。
- **FR-10 eDHR 快照契约：** 批次执行、工作任务、演练就绪和门禁测试必须分别使用正式候选快照、配置快照及逐工序批记录表单绑定，不得从其他链路推断。
- **FR-11 批记录解析契约：** JSON 类型、表格坐标、形状数量、路线候选治理和真实文档识别必须与正式解析输出一致。
- **FR-12 排产契约：** 自动排产、工单编码、路线版本、排产订单准入、风险识别、无默认配置和容量状态必须具有一致且可测试的正式行为。
- **FR-13 完整回归：** 最终测试必须运行整个 `yudao-module-mes` 测试集合，不使用用例筛选、排除或跳过参数。
- **FR-14 失败可见性：** 缺少 fixture、schema、Bean、测试数据或其他前置条件时必须输出明确失败，不得转换为默认值、空结果或成功状态。

## Non-Functional Requirements

- **NFR-01 可重复性：** 相同代码、真实 fixture 和测试环境连续执行完整命令应得到一致结果。
- **NFR-02 可移植性：** 测试资源定位不得依赖开发者用户名、桌面目录或非项目固定盘符。
- **NFR-03 可维护性：** 修复应解决共享根因，避免为单个测试增加临时分支或一次性绕过。
- **NFR-04 严格性：** 保持现有业务断言强度和 Mockito 严格模式；任何期望变更必须有正式契约证据。
- **NFR-05 隔离性：** 测试数据、Spring Context 和 H2 状态不得在测试类或重复运行间泄漏。
- **NFR-06 性能可执行性：** 完整运行允许覆盖基线约 38 分钟的耗时，不得用过短超时把未完成执行误判为测试结果。
- **NFR-07 并发安全：** 只修改任务拥有的文件；遇到共享文件冲突时必须阻塞协调。
- **NFR-08 证据完整性：** 每个失败簇需要保留 RED、GREEN 和相关回归证据，最终保留完整 Maven 汇总。

## Dependencies and Constraints

- 真实 Word fixture：`RE-PP-ID-01（A 1）球囊扩张压力泵生产记录(1).doc`。
- 真实 Excel fixture：`球囊扩张导管工序(1).xlsx`。
- Fixture 必须有可确认的来源、版本和完整性；在取得前，依赖它们的修复及最终验收为 blocked。
- 禁止自行制作、裁剪、转换或用相似文件替代上述 fixture。
- 技术环境为 Windows、Java 17、Maven，多模块后端根目录为 `E:\IntRuoyi\IntRuoyiBackend`。
- 当前共享分支为 `int_main`，工作区存在 unrelated concurrent changes，且分支相对 `origin/int_main` ahead 1。
- 实施前和每次编辑前必须核对目标文件并发状态；文件冲突时停止并协调，不得覆盖他人改动。
- 不得新增或扩大测试跳过、排除、禁用、条件假设或 Surefire 过滤。
- 不得降低断言、Mockito 严格性或异常可见性。
- 最终验收命令固定为：

```powershell
mvn -pl yudao-module-mes test
```

## Acceptance Criteria

- **AC-01：** 两个真实 fixture 均已从权威来源取得，版本和完整性已记录；未取得时任务保持 blocked，且仓库中不存在任务伪造的替代文件。
- **AC-02：** 所有依赖真实 Word fixture 的表格清单、结构校验、路线识别和打印探针测试通过。
- **AC-03：** 所有依赖真实 Excel fixture 的工艺路线、设备映射解析和导入测试通过。
- **AC-04：** 前端静态契约不再访问 `E:\IntRuoyi\yudao-ui-admin-vue3`，并能基于当前 `IntRuoyiFronted` 结构完成验证。
- **AC-05：** `MesBatchRecordBaseSchemaTest` 及相关 schema/迁移契约通过，正式字段和约束与运行时 schema 一致。
- **AC-06：** 基线中因 Spring Bean 注入或 ApplicationContext 创建失败的数据库/集成测试全部通过，且没有用 mock success 或删除上下文测试规避。
- **AC-07：** `MesProBatchRecordReportServiceImplDbTest` 不再发生 H2 唯一键冲突，并能重复独立执行通过。
- **AC-08：** Mockito stubbing 漂移和路线服务缺失依赖问题全部修复；严格模式下不再出现 `UnnecessaryStubbingException`、`PotentialStubbingProblem` 或依赖空引用。
- **AC-09：** eDHR 批次执行、任务门禁、候选/配置快照、演练就绪和 legacy-process 契约测试全部通过，且三类工艺路线配置来源保持独立。
- **AC-10：** 批记录 JSON 类型、布局坐标、形状规则、路线候选治理和解析断言全部通过；没有删除或放宽原有效业务断言。
- **AC-11：** 自动排产、工单编码、路线版本、排产订单准入、风险、无默认配置和容量状态测试全部通过。
- **AC-12：** 原通知目标 3 个测试通过，`MesProEdhrWorkTaskServiceImplTest` 66 个测试全部通过，通知收件人仍来自每个任务自身的有效候选快照。
- **AC-13：** 2026-07-27 基线中的 41 个失败测试套件均不再报告 failure 或 error；没有通过删除测试类、重命名逃避发现或减少测试扫描范围实现。
- **AC-14：** 本任务没有新增或扩大 `@Disabled`、条件跳过、Surefire excludes、测试 profile 排除或命令行测试筛选；基线 skipped 测试不得掩盖任何原失败套件。
- **AC-15：** 在 `E:\IntRuoyi\IntRuoyiBackend` 执行 `mvn -pl yudao-module-mes test` 返回退出码 0 和 `BUILD SUCCESS`，完整汇总为 0 failures、0 errors。
- **AC-16：** Git 差异审查确认只包含本任务拥有的修复与证据；任何并发文件冲突均已在修改前协调，没有覆盖、回退或提交无关改动。
- **AC-17：** 最终实现未引入 fallback、默认成功、吞异常、伪 fixture、宽松 mock 或放宽断言。
