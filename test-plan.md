# eDHR 偏差管理测试计划

## Purpose and Scope

按28项AC定义真实可观察行为和后续严格TDD。本轮只校验文档，下面目标测试文件尚待开发新增；不声称已经运行产品测试。

## Evidence Reviewed

PRD、frontend-interaction、接口/数据/权限设计、当前MES服务及现有前端package脚本。项目要求E2E只在用户当轮明确授权后用Playwright走真实页面。

## BDD Scenarios

### Feature Scenarios

- BDD: T01 合法发起与租户权限 -> Given 有/无create权和同/异租户批记录, When 发起并签名, Then 仅合法请求成功，其他业务零写入。
- BDD: T02 上市放行边界 -> Given 批记录已上市放行, When 从选择器或后端请求新增偏差, Then 明确拒绝且原记录不变。
- BDD: T03 编号边界与并发 -> Given 同租户不同月份、跨时区边界及流水9999, When 并发正式提交, Then 每月序列正确、10000扩位且不重复，成功号码不复用。
- BDD: T04 推送PQC前QA发起 -> Given 尚未推送PQC但有正式批记录和开放关键偏差, When QA从批记录签署发起NCR, Then 成功且不要求PQC申请ID；内部身份来自正式关系。
- BDD: T05 创建与放行串行化 -> Given 创建偏差和上市放行同时请求、或创建响应丢失, When 交错提交并用同幂等键重试, Then 放行先完成则创建拒绝；创建先完成则放行被阻断；重试只有一条。
- BDD: T06 发起字段与两级分类 -> Given Word发起字段和用户两档等级, When 录入并签名提交, Then 字段完整，只有普通/重大（关键），无主要等级自动映射。
- BDD: T07 唯一处理与重新验证 -> Given 一条OPEN偏差处理后验证不合格, When 修改原处理并重新验证, Then 处理ID不变、修订可查，不产生第二份处理单。
- BDD: T08 常规完整签名矩阵 -> Given 任一编制/验证/部门负责人/QA/质量签名缺失或验证不合格或结论持续跟踪, When 尝试常规关闭；再补齐必要签名与闭环结论, Then 缺项一直OPEN，全满足才CLOSED，QA/质量两种先后都成功。
- BDD: T09 关键管理者签名例外 -> Given 普通和关键分别走正常处理，另一关键走转NCR, When 执行相应关闭动作, Then 仅关键常规流程要求管理者签名，转审不要求偏差批准。
- BDD: T10 正文修改使旧签名失效 -> Given 处理正文已有有效签名, When 携带原因修改后再关闭, Then 审计前后值/失效证据完整，重签前不能关闭。
- BDD: T11 同账号多角色同版签名 -> Given 一人获多个签署角色且处理编制已签, When 首次签验证与部门结论再分别签QA/质量，修订验证结论并重放旧版本请求, Then 正常追加不废止编制/同版并列批准，结论修订使依赖它的后续签名失效，每节点独立事实且过期请求拒绝。
- BDD: T12 三Tab服务端标准列表 -> Given 同租户多个发起人、OPEN/CLOSED混合且超过一页, When 筛选排序分页切Tab重置, Then 标准模板结果/total准确，query权可见所有发起人。
- BDD: T13 详情唯一处理和转审原因 -> Given 无处理、常规关闭、转审关闭三类偏差, When 打开详情, Then 真实显示0..1处理及签名/关联，不造验证合格。
- BDD: T14 宿主与只读隔离 -> Given 正式顶层、两个追溯抽屉和内嵌逐工序预览, When 打开相关页面, Then 指定入口可见偏差，内嵌不重复，追溯无写动作。
- BDD: T15 空错无权与竞态请求 -> Given 空集合、服务失败、无query权及两个批次请求乱序, When 读取偏差, Then 各态分离，无前批缓存污染，无错误转空态。
- BDD: T16 只有订单ID的正式入口 -> Given 入口仅传activeOrderId且后端有明确正式批记录关系, When 进入活跃表单详情, Then 后端回传batchExecutionId成功查询；真实缺失/歧义才报错。
- BDD: T17 编辑与导航交互 -> Given 未保存输入、签名取消、版本冲突、响应不明, When 离开/重试/点击关联编号并返回, Then 不丢输入或覆盖别人，保留来源筛选页码，幂等无重复。
- BDD: T18 QA关键入口与旧NCR回归 -> Given 无QA权/普通偏差，另有原PQC检验不合格来源, When 尝试从偏差转审与原NCR创建, Then 偏差入口拒绝不合法者，原独立来源保持原合同。
- BDD: T19 同批多选与整体拒绝 -> Given 多个开放关键偏差及混入普通/跨批/已闭/空集合, When 一次创建NCR, Then 合法集一审多偏差；非法集整体拒绝，未选项不变。
- BDD: T20 未处理完成直接转审 -> Given 关键偏差尚无处理或仅部分处理、无验证批准, When QA签署发起NCR, Then 直接成功自动关闭，缺节点标未执行不伪造通过。
- BDD: T21 双阻断来源四动作 -> Given OPEN偏差，或CLOSED转审偏差关联pending NCR, When 逐一执行四项提交/放行, Then 每项服务端拒绝并带编号，关闭偏差后无解锁窗口。
- BDD: T22 三种处置与残余门禁 -> Given QA/PQC处置权用户、待审NCR和其他未闭偏差/外部冻结, When 分别让步、返工、作废, Then 正式处置正确，其余阻断保留，返工按正式路径，作废不复产。
- BDD: T23 转审失败与重放 -> Given 签名/建审/冻结/关联/关闭任一步失败或成功响应丢失, When 事务失败或同键重试, Then 无部分成功；重试返回同评审，异载荷键冲突拒绝。
- BDD: T24 不自锁与待审冲突 -> Given 同批有pending NCR又有新关键偏差, When 再发起、处理原偏差并处置当前NCR, Then 第二审拒绝且新偏差不关闭；处理/签署/评审处置不被门禁封死。
- BDD: T25 同批追溯一致 -> Given 同批多偏差和正式关联NCR有最终处置, When 管理、现场详情、两个追溯入口读取, Then 内容/关联一致且不混入其他批次/租户。
- BDD: T26 只读与关闭原因历史 -> Given 常规或转审关闭，批次已上市放行, When 尝试修改、重开或新增关联, Then 服务端拒绝，原内容签名/原因保留；在线验收不假称PDF完成。
- BDD: T27 签名展示真实性 -> Given 当前有效/旧版失效/未签节点, When 读取详情和追溯, Then 显示真实中文姓名、时间/时区、意图/证据，未签不造签名。
- BDD: T28 可复验测试证据 -> Given 各实现阶段和测试前置满足或缺失, When 执行该阶段命令, Then 测试数量及预期失败/成功原因明确；未授权E2E不运行、不标PASS。

### Failure Scenarios

拒绝权限/租户错误、缺关联、已上市放行、空或混合偏差集、签名错误、内容版本冲突、重复幂等键异载荷、待审重复创建；任一失败不留部分业务成功。T01/T02/T08/T10/T14/T18/T19/T23/T24为必测负例。

### Boundary Scenarios

T03月切、时区、9999扩位、不同租户；T05创建/放行竞态；T11同版多签与过期版本；T14请求乱序；T20无处理直接转审；T21阻断接管无窗口；T22残余冻结。

## Acceptance Mapping

| Test ID | Acceptance ID | 目标 |
| --- | --- | --- |
| T01 | P1-AC1 | 合法发起与租户权限 |
| T02 | P1-AC2 | 上市放行边界 |
| T03 | P1-AC3 | 编号边界与并发 |
| T04 | P1-AC4 | 推送PQC前QA发起 |
| T05 | P1-AC5 | 创建与放行串行化 |
| T06 | P2-AC1 | 发起字段与两级分类 |
| T07 | P2-AC2 | 唯一处理与重新验证 |
| T08 | P2-AC3 | 常规完整签名矩阵 |
| T09 | P2-AC4 | 关键管理者签名例外 |
| T10 | P2-AC5 | 正文修改使旧签名失效 |
| T11 | P2-AC6 | 同账号多角色同版签名 |
| T12 | P3-AC1 | 三Tab服务端标准列表 |
| T13 | P3-AC2 | 详情唯一处理和转审原因 |
| T14 | P3-AC3 | 宿主与只读隔离 |
| T15 | P3-AC4 | 空错无权与竞态请求 |
| T16 | P3-AC5 | 只有订单ID的正式入口 |
| T17 | P3-AC6 | 编辑与导航交互 |
| T18 | P4-AC1 | QA关键入口与旧NCR回归 |
| T19 | P4-AC2 | 同批多选与整体拒绝 |
| T20 | P4-AC3 | 未处理完成直接转审 |
| T21 | P4-AC4 | 双阻断来源四动作 |
| T22 | P4-AC5 | 三种处置与残余门禁 |
| T23 | P4-AC6 | 转审失败与重放 |
| T24 | P4-AC7 | 不自锁与待审冲突 |
| T25 | P5-AC1 | 同批追溯一致 |
| T26 | P5-AC2 | 只读与关闭原因历史 |
| T27 | P5-AC3 | 签名展示真实性 |
| T28 | P6-AC1 | 可复验测试证据 |

## TDD Sequence

1. P1：T01～T05，真实对象身份/权限/序列/竞态。数据库并发证据不能仅用纯mock替代。
2. P2：T06～T11，唯一处理与签名状态机。
3. P3：T12～T17，接口分页+组件行为+来源/只读合同。
4. P4：T18～T24，直接转审/事务/四项动作/处置及不自锁。
5. P5：T25～T27，同源追溯、只读、签名证据。
6. P6：T28，实际证据完整性和相关回归。

先新增可编译/可执行的测试，再跑RED，随后最小实现并用同命令GREEN。编译失败、脚本不存在、测试数为0不是有效RED/GREEN。单元测试可以使用依赖替身验证失败分支，但事务原子性、租户SQL和并发必须有实际数据库测试；不允许mock业务成功代替产品或E2E。

## RED Commands

以下命令均从仓库根目录执行，类名/新脚本是确定的实现目标，先落盘再运行。本轮不运行这些业务命令。

~~~powershell
mvn -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am '-Dtest=MesProEdhrDeviationServiceTest,MesProEdhrDeviationPermissionTest,MesProEdhrDeviationConcurrencyTest,MesProEdhrDeviationSignatureTest,MesProEdhrDeviationNcrIntegrationTest,MesProEdhrDeviationGateTest,MesProEdhrDeviationTraceTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
node IntRuoyiFronted/tests/e2e/edhr-deviation-management-static.spec.cjs
~~~

Maven中的failIfNoSpecifiedTests=false仅用于依赖模块无目标类的多模块构建；必须核对MES Surefire报告目标类确实执行且测试数大于0。不能把测试都没执行的退出码0当GREEN。

## Expected Failures

RED必须指出已运行用例的断言原因，例如部门未签仍关闭、同版签名互相失效、转审要求先验证、未闭偏差可上市放行。缺少测试类或不支持环境属于前置问题，不能冒充目标行为RED。

## GREEN Commands

执行同一RED命令，并记录对应类、用例数量、退出码和AC覆盖。随后执行相关既有NCR、签名、生产/PQC提交和放行回归；不只看源码字符串。

前端使用真实组件测试或现有可执行测试工具验证状态/请求/按钮行为；静态测试只证明路由、组件引用和字段合同。若测试基建缺失，先补齐可运行方案，不能以字符串检查作为唯一行为证据。

~~~powershell
pnpm --dir IntRuoyiFronted ts:check
pnpm --dir IntRuoyiFronted exec eslint src/views/mes/pro/edhr-deviation src/api/mes/pro/edhr/deviation.ts
~~~

新路径为开发目标；另将实际修改的共享Vue文件加入定向ESLint参数。不得用会自动修复全src的lint脚本改动其他任务资产。迁移策略用现有release-migration policy命令验证本任务迁移及完整依赖，首次/重复执行及schema实测需当前任务数据库授权。

## Refactor Checks

只对实际改动扩大回归；不新增fallback/假成功。验证旧PQC不合格来源、原租户边界、只读追溯和签名证据未退化。生产代码与对应测试一并交付；禁止修改测试取消仍然需要的四项限制。

## E2E

### User Paths / Browser or Client Steps

本轮未获E2E执行授权，仅规划。之后按当轮授权使用真实页面、真实测试租户及任务自有数据：

1. 由管理员在正式角色页面授予所需权限；使用无权/有权和多角色账号验收，口令由运行环境注入，不写任务文档。
2. 准备未推送PQC的真实批记录，从批记录详情发起普通偏差、保存唯一处理、编制签名、验证合格、部门签名、QA和质量两种先后签署。
3. 另一个关键偏差走常规流程，实证管理者代表缺签不能关闭；同一处理记录验证失败后修改、重签和重验。
4. 同批两条关键偏差在尚未处理完成时由QA一次发起NCR，验收发起签名、自动关闭、关联与未选偏差保留。
5. 分别从真实一线生产、一线PQC、PQC生产放行和上市放行入口尝试指定动作，核对未闭偏差或待审NCR的拒绝。不能只看禁用按钮截屏。
6. 三组独立可处置数据分别走让步、返工、作废，核验实际后续允许/禁止动作以及其他偏差仍阻断；作废数据不能再拿来测试让步。
7. 管理列表筛选/返回，正式顶层Tab、历史追溯抽屉、批次详情追溯抽屉逐个读取一致内容。正常空集合、真实无权和错误态分别验收。

### API Verification

被验收业务动作全部由Playwright在前端完成。遵守当前仓库更严格规则，不直接fetch/apiGet接口；从页面实际响应和UI回读核验。单元/集成测试中的服务调用不算E2E。

### Console and Log Checks

记录实际页面请求业务码、错误区文案、状态前后变化和正式对象身份；保存任务拥有的trace及必要截图。无异常日志或按钮出现不能单独证明业务PASS。

## Test Data

### Required Test Data

普通/关键、同批多关键、另一批记录、另一租户、已上市放行、未推送PQC、pending NCR、其他未闭偏差/外部冻结、三种处置独立样本；账号包括无权、query、create、handle、verify、department-confirm、QA、质量、管理者、PQC处置，覆盖一人多角色。

### Data Ownership

单元/数据库集成用隔离测试库；真实E2E用批准租户中的任务自有记录并标识taskId，不修改他人批次、不删除固定测试租户或主数据。

### Reset Procedure

通过正式前端允许的动作收口任务数据，保留签名与审计事实。不可直接SQL删记录掩盖失败。不能清理的已签数据保留任务标识并记录，不扩大删除范围。

## Evidence Log Template

~~~text
BDD: Txx 场景 -> Given 前置, When 动作, Then 结果
RED: 实际命令 -> FAIL, 目标用例及预期失败原因
GREEN: 同一命令 -> PASS, 测试数量与报告位置
REGRESSION: 实际受影响范围/命令 -> PASS或具体失败
AC: Pn-ACm -> 实际证据路径与本轮结果
~~~

文档审查是DOC验证，不写作上述业务RED/GREEN；未执行写NOT_RUN，不复用历史PASS冒充新版本证据。

## Failure Paths

四项受限动作逐项覆盖开放偏差/待审NCR两种来源及服务失败；NCR五步骤故障逐点回滚；签名缺失/身份失败/正文版本变化；创建与放行交错；列表响应乱序；跨租户/跨批身份；正常入口缺来源。所有负例须明确允许继续的处理/处置路径以防自锁。

## Open Questions

主流程已收敛。具体运行测试库、目标分支runtime、签名授权和真实数据由实施预检确认；不因本文件存在而默认可用。PDF导出、NCR事后追加和关闭后重开不在当前验收。

## Test Blockers

未取得当轮E2E或数据库写入授权时不执行对应验证。缺正式来源、可用签名授权或当前schema需报告确切前置与影响；不能生成mock成功值或换到其他任务数据。
