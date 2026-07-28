# Request Analysis

## User Goal

在已完成“eDHR 工作任务通知全部有效候选人”行为修复的基础上，将任务范围扩大为：逐项修复 `mvn -pl yudao-module-mes test` 暴露的全部 MES 测试失败，直到完整、未筛选的 MES 测试套件通过。

最终结果必须同时满足：

- 原通知行为继续通过：3 个目标用例通过，`MesProEdhrWorkTaskServiceImplTest` 66 个用例全部通过。
- 完整 MES 测试命令成功结束，不遗留 failure 或 error。
- 不跳过、不排除失败测试，不放宽断言，不使用 fallback、mock success、默认成功或伪造 fixture 隐藏问题。

2026-07-28 范围变更：用户明确确认 Sheet1 Excel 真实样本覆盖“不需要覆盖这个”。因此缺失 `球囊扩张导管工序(1).xlsx` 不再作为本任务最终验收前置；处理方式是删除该真实样本覆盖入口并保留 Sheet1 parser 合成 fail-fast/契约测试，禁止用跳过、排除、伪 fixture 或合成 workbook 冒充真实样本。

## Current System

- 原通知缺陷已修复并完成定向验证：目标 3/3 通过，同类服务测试 66/66 通过。
- 2026-07-27 完整执行 `mvn -pl yudao-module-mes test` 的基线结果为：2509 tests、58 failures、78 errors、31 skipped，共 41 个失败测试套件。
- 已确认的失败簇包括：
  - 本机硬编码 Word fixture 缺失：`C:\Users\BJB110\Desktop\2\2\RE-PP-ID-01（A 1）球囊扩张压力泵生产记录(1).doc`。
  - 本机硬编码 Excel fixture 缺失：`D:\ocr2\resource\球囊扩张导管工序(1).xlsx`。
  - 前端契约测试仍读取旧目录 `E:\IntRuoyi\yudao-ui-admin-vue3`，而当前前端根目录为 `E:\IntRuoyi\IntRuoyiFronted`。
  - 运行时 schema 契约缺少 `mes_pro_batch_record_execution.batch_record_definition_id`。
  - 多个数据库测试因 Spring Bean 注入或 ApplicationContext 创建失败而报错。
  - `MesProBatchRecordReportServiceImplDbTest` 存在 H2 唯一键冲突。
  - Mockito 严格模式暴露无效或参数不匹配的 stubbing。
  - 工艺路线服务测试缺少新增依赖 mock，出现 `routeOwnerPermissionService`、`platformAdapter`、`routeVersionMapper` 等空引用。
  - eDHR 批次执行、任务门禁、候选/配置快照和演练就绪测试与当前正式行为不一致。
  - 批记录解析、JSON 类型、表格布局、形状规则和路线识别断言漂移。
  - 自动排产、排产订单准入、容量缺失状态和工单编码契约漂移。
- 当前根仓库分支为 `int_main`，相对 `origin/int_main` ahead 1；共享工作区存在多个并发任务的未提交修改和未跟踪目录。
- 当前任务目录中的既有证据文档也处于修改状态，后续工作不得覆盖或回退这些并发变更。

## Constraints

- 必须从 `E:\IntRuoyi\IntRuoyiBackend` 执行最终验收命令：`mvn -pl yudao-module-mes test`。
- 不得使用 `-DskipTests`、`-Dtest=...`、Surefire 排除、测试 profile 排除、`@Disabled`、条件假设或其他方式绕开完整失败面。
- 不得通过放宽断言、删除有效断言、改成宽泛匹配或接受错误结果来制造绿色。
- 不得为缺失 Word/Excel fixture 创建伪文件、空文件、简化替代文件或 mock 内容。
- 不得引入 fallback、兼容分支、吞异常、默认值或默认成功。
- 修复必须区分生产代码缺陷、测试装配缺陷、测试数据缺陷和过时契约；不能统一按“改测试”处理。
- 所有行为修复应遵循 BDD 和严格 TDD，并保留 RED、GREEN 和完整回归证据。
- 共享工作区中的无关并发改动不属于本任务；不得修改、回退、提交或清理。
- 若本任务必须修改的文件同时被并发任务修改，必须先阻塞并协调，不能覆盖、拼接猜测或强行提交。

## Unknowns

- 缺失 Word 与 Excel 文件的权威来源、文件版本、内容完整性、授权范围和校验值尚未确认。
- 获得真实 fixture 后，应采用何种项目内稳定位置及治理方式，需要根据现有测试资源规范确认；不能继续依赖个人桌面或固定盘符。
- 各契约漂移中，正式产品行为与测试期望哪一方过时，需要逐套件依据当前 schema、服务契约和业务规则判定。
- Spring 测试上下文失败的共同根因可能涉及测试配置、Bean 扫描、依赖新增或数据库初始化，需按首个根因展开，不能仅按表层注入异常逐个打补丁。
- 基线 31 个 skipped 是否全部为既有明确设计尚未核实；它们不得用于掩盖当前 41 个失败套件，也不得因本任务新增或扩大跳过范围。
- 共享分支 ahead 1 的提交归属和后续并发修改时序需要在实施前再次确认。

## Risks

- 伪造或替换真实生产文档 fixture 会让解析与布局测试失去业务真实性，并可能造成错误规则进入生产。
- 将所有失败视为测试过时并修改断言，可能掩盖 schema、调度、批记录和任务快照的真实回归。
- Spring Context 和 H2 数据隔离问题可能具有共同根因；逐用例规避会增加长期脆弱性。
- 解析和布局测试单次运行耗时较长，若只在最后执行全量测试，会显著延迟反馈。
- 排产、路线和 eDHR 快照修复可能触及共享核心服务，存在跨套件行为回归风险。
- 并发任务可能修改同一文件或推进共享分支；未做冲突检查会覆盖他人工作或使验证证据失效。
- 将 Mockito 改为 lenient、扩大 matcher 或删除验证虽然可能消除报错，但违反严格验证目标。

## Validation Surface

- 通知行为：目标 3 个测试及 `MesProEdhrWorkTaskServiceImplTest` 全部 66 个测试。
- Fixture 消费：Word 表格清单、路线识别、结构校验、打印探针；Excel 工艺路线和设备映射解析/导入。
- 前端静态契约：路由、菜单、共享批记录设计器引用和当前前端目录定位。
- Schema 与迁移契约：批记录基础 schema、版本重传、路线生成和历史链字段治理。
- Spring/数据库集成：Bean 注入、ApplicationContext、H2 数据初始化、唯一键和测试隔离。
- 工艺路线：版本复制、所有者权限、受控内容适配器、批记录绑定和显示字段。
- eDHR：批次执行、任务门禁、候选快照、配置快照、演练就绪和 legacy-process 防回退契约。
- 批记录解析与布局：JSON 字段类型、布局坐标、形状数量、候选路线治理和真实文档识别。
- 排产与排产订单：算法契约、路线版本解析、准入 stubbing、风险契约、无默认配置和容量状态。
- 完整回归：不带测试筛选或排除参数的 `mvn -pl yudao-module-mes test`。

## Blocking Prerequisites

- **保留范围内的真实 fixture 获取是硬阻塞项。** 必须取得并确认以下原始文件的权威副本、版本和完整性后，才能完成仍依赖它们的测试修复：
  - `RE-PP-ID-01（A 1）球囊扩张压力泵生产记录(1).doc`
- `球囊扩张导管工序(1).xlsx` 的 Sheet1 真实样本覆盖已被用户明确取消，不再阻塞最终全量验收；不得生成、转换、下载相似文件或构造最小内容替代，也不得用跳过参数掩盖。
- 实施前必须重新检查 Git 状态和目标文件并发修改情况。若目标文件与其他任务冲突，必须先协调文件所有权。
- 需要具备 Java 17、可用 Maven 依赖缓存/网络和足够的连续执行时间；基线完整运行约 38 分钟。
- 只有全部前置条件满足并且完整命令返回 `BUILD SUCCESS`，才可进入任务收尾。
