# E2E-17 同人跨部门参数判定与验收选项

## 判定

**当前正式输入不可达，原验收前提与人员模型冲突；不是已复现的生产去重缺陷。**

原用例 `doc/tasks/20261015-dcc-six-thread-followup-review/e2e-test-cases.md:43` 要求“其中一人属于多个所选部门”。当前用户仅有单值 `deptId`，首次生效下发按所选部门的启用直接成员冻结名单，不展开子部门、不以多岗位/多角色或部门负责人关系补齐归属。不同部门的合法直接成员集合互斥，无法通过正式页面让同一 userId 同时归属两个所选部门。

因此保留 `NOT_CONSTRUCTIBLE_WITH_CURRENT_FORMAL_ORG_MODEL`，该参数仍未验收，**不能直接改 PASS，也不能自动改为已批准的不适用**。本轮仅分析，不改生产、组织、数据或原用例定义；业务确认前没有可运行的同人跨部门成功 E2E。

## 本轮范围与最新基线

- 来源：`../20261016-dcc-24-e2e-coordination/recovery-dispatch-20261019.md`，Root本轮只给17该参数的文档/代码只读分析；服务尚未READY不影响此静态判定。
- 已读最新Root复核 `e2e16-24-acceptance.md:10/56/105`，及本例最新 `fixture-manifest.json`、`final-acceptance.json`。原4234/A1三人真实签收、未签数3→2→1→0、原正文/水印/通知与范围排除均已保存，Root已接受签收主链；本轮不重跑任何这些分支，不重签、不重建对象。
- 当前分支 `int_qms`；HEAD `f06833f06c868dc3a451eb63d01381a467ecfe92`。工作区存在并行改动，以下为所读文件当前内容，规范化源码指纹在配套JSON中；HEAD不能单独证明各文件内容。
- 本轮没有浏览器、API/DB、Maven/构建、服务、Git写或子Agent操作；只写本例分析与任务记录。既有测试仅阅读，未执行，不把测试方法存在写成通过。

## 正式链路证据

| 环节 | 已核代码锚点 | 含义 |
| --- | --- | --- |
| 页面输入 | `IntRuoyiFronted/src/views/system/user/UserForm.vue:17-29`，归属部门 `v-model=formData.deptId`，无multiple | 一个账号只选一个归属部门；多岗位不是多部门 |
| 持久模型 | `AdminUserDO.java:70`、`UserSaveReqVO.java:43`、`AdminUserRespDTO.java:40`，均为 `Long deptId` | UI、保存与目录DTO没有部门集合 |
| 正式目录调用 | `AdminUserApiImpl#getUserListByDeptIds` → `AdminUserServiceImpl#getUserListByDeptIds` → `AdminUserMapper#selectListByDeptIds` | 直接按 `AdminUserDO::getDeptId` 属于给定集合查询；不是按部门逐个拼接子树 |
| 容易混淆的另一入口 | `AdminUserApiImpl#getUserListBySubordinate`、`AdminUserServiceImpl#getDeptCondition` | 这些会扩子部门，但首次下发没有调用它们，不能借来构造父子部门重叠 |
| 首次名单来源 | `DccFirstEffectiveDistributionRosterSource#resolve/readDirectory:43/59/66/70` | 所选部门ID重复则ROSTER_REQUEST_INVALID；用户必须是所选直接部门的本租户账号；全局seen重复userId则ROSTER_DIRECTORY_INVALID，整个名册NotReady，不生成部分成功 |
| 冻结下发 | 同来源 `FIRST_EFFECTIVE_DISPATCH_V1`；`DccDistributionBatchService#processFromFormalSource` | 首次成功读取后冻结原名单；SENT重放返回原事实，冻结重试不重新取今日组织；先调岗再重试不构成同次双部门归属 |
| 下游义务 | `DccDistributionBatchService#validateFormalSnapshot`、`DccWorkflowDistributionService#dispatchActivatedDistribution` | 验证部门唯一及每部门内部userId唯一，然后逐部门建scope和recipient；不等同于跨部门按userId合并 |

设 `d(u)` 是账号u唯一归属部门，`M(A)={u | enabled(u) 且 d(u)=A}`。对不同A、B，`M(A)∩M(B)=∅`。选择父子部门时，只要取的是直接成员，结论相同；上下级关系不会令 `d(u)` 变成两个值。若目录返回同一userId的两个deptId，那是上游不一致数据，不是当前模型的合法双归属输入，正式来源会拒绝。

## 必须分开的三种“去重”

| 名称 | 当前行为 | 与原参数关系 |
| --- | --- | --- |
| 异常重复身份 | 正式目录返回重复账号或保存重复部门，拒绝整个来源，不静默丢掉其中一条 | 可作安全边界测试；不是跨部门成功合并 |
| 同一生效事件重放/重试 | 复用冻结义务、通知身份和原快照，不重新生成收件义务；已SENT返回原事实 | 可作幂等回归；不是同人多部门归属 |
| 合法同人跨部门合并 | 当前无合法输入来源，也无通用跨部门合并合同；下游按部门义务建行 | 原要求仍未验收，不能凭前两项通过关闭 |

当前唯一正式来源的全局seen守卫保证正常自动下发不会出现跨部门同人；下游通用快照结构仍是 `Department(departmentId,userIds)`，校验主要在每部门内部。不得笼统声称“所有未来来源已经支持跨部门去重”。将来引入不同名册来源时，应重新明确一人一份还是每部门一份义务，而非删掉seen守卫继续走现有循环。

## 选项A：按现有正式模型验收（建议业务明确采用）

不改组织或生产实现，沿用本例已有可构造分支PASS，业务确认原跨部门正例对本版本不适用；另将“去重”拆成异常输入拒绝、重放不重复两项技术边界。**这是验收范围变更建议，尚未获业务确认，不自动替代原参数。**

建议业务确认文字：

> 本版本下发按用户唯一归属部门的启用直接成员冻结，不包含部门子树或多岗位推导。E2E-17中“同一账号属于多个所选部门”的成功前提不适用于本版本；本例已通过的全员、范围、时序和逐人签收沿用既有证据。异常重复目录身份须拒绝，重复生效事件/冻结批次重放不得新增义务或重复发送，作为独立技术验收，不声称验证了跨部门成功合并。

可执行且不重跑已通过业务的验收步骤：

1. Root记录业务是否确认上述范围；未确认则保留当前NOT_CONSTRUCTIBLE，不改旧用例或最终PASS标志。
2. 需要额外技术证据时，由Root串行执行既有定向测试，测试中的重复DTO是明确的单元故障输入，仅证明拒绝，**不能当真实UI夹具或跨部门成功E2E**：
   - `DccFirstEffectiveDistributionRosterSourceTest#invalidDirectoryRowsNeverCreatePartialOrExpandedRoster` 的DUPLICATE/CHILD参数；重复账号返回ROSTER_DIRECTORY_INVALID，非所选直接部门不扩展。
   - 同类 `invalidSavedDepartmentIdentityCannotBeCoerced` 的DUPLICATE参数；重复所选部门先拒绝，不读目录。
   - `DccDistributionRosterPortTransactionTest#sentReplayReturnsOriginalFactWithoutProviderOrNewWrites` 与 `frozenBatchRetryNeverResolvesTodaysRosterAndReusesPlatformIdentity`；原义务/平台身份不增加，重试不换今日名单。
3. 上述故障拒绝/幂等测试本轮未运行；如Root实际执行，应记录命令、方法及结果为独立服务证据。无需创建新文件、移动原R1/R2/R3部门、重签或触发生效作业。它们不取代原跨部门E2E。

BDD：Given 当前单部门/直接成员规则、唯一原账号和所选部门；When 目录异常返回重复身份或同事件重放；Then 前者明确拒绝且不生成部分名单，后者复用原冻结义务而不重复发送。无论结果如何，都不能凭它把原跨部门正例写成PASS。

## 选项B：业务仍要求同人跨部门成功（单独需求，当前不实施）

必须先有**正式可维护的多部门人员来源**，并明确：

- 合法多归属是兼职部门、部门子树命中，还是其他正式人员关系；来源页面、有效期与首次冻结时点是什么。
- 去重单位是“每精确文件版本/BPM/人仅一条通知及一条签收，保留全部命中部门”，还是每部门各一份独立义务；两者不能混称。
- 部门报表如何引用同一签收事实、未签数按人还是义务计数，组织变更与重试如何保留冻结来源。

只有业务定案并另行授权后才评估数据模型、名册来源、冻结快照、通知幂等、签名绑定和逐人计数的变更及RED。当前禁止扩模型、复制同名账号冒充同人、手工伪造重复名册、API/SQL造数、借已签原文件回拨/补写。选项B不属于这轮只读分析与本例修复范围。

## 交Root结论

代码分析完成；不建议分配生产修复文件。最小待办是业务选择A的验收范围说明或提出B的独立需求。环境READY不改变此输入可达性，**本参数无需启动Playwright即可给出当前模型判定**；业务未明确前仍保留未验收，并继承原三人签收等全部既有PASS，不再整体运行。

## R2补查：已有业务定案与原E2E前提冲突

本轮进一步找到明确业务规范，而非只从源码推断：`doc/tasks/20261011-dcc-eight-thread-management/ic32-integration-review.md`记录2026-10-13用户两项定案，操作与行为第3条明确“首次生效下发从保存的直接部门获取当时启用成员，不读取下属部门”，冻结重试复用原义务；`doc/tasks/20261013-dcc-w07-ic32-production-roster/task.md`及`verification-report.md`落实同一规则。不能把旧文档中已答的名单来源问题重新报成未知，也不能借多角色/父子部门违背该直接成员定案。

因此最小待业务明确的是**原E2E表中后加“其中一人属于多个所选部门”的前提是否撤回/本版本不适用**，而不是重新决定当前直接成员来源。若仍要求它，必须明确覆盖上述已有规范并另行定义合法多归属及签收义务语义；未经确认仍不改PASS/N/A，也不新增模型。本轮仅补规范证据，原可构造分支仍不重跑。

## R5业务决策状态（2026-10-19）

本轮派单仅要求保留D17-01为业务决策项；未收到业务定案。因此状态仍为`DECISION_REQUIRED`，验收结果仍为`NOT_CONSTRUCTIBLE_WITH_CURRENT_FORMAL_ORG_MODEL`，`businessPass=false`、`notApplicableApproved=false`。本更新没有批准范围变更，也没有把未验收前提改成PASS或N/A。

- **A：按现有模型明确本版本不适用。** 业务需明确撤回/标记“同一人属于多个所选部门”的成功前提不适用于本版本；异常重复身份拒绝和重放幂等作为独立技术验收，不能替代原跨部门成功正例。已通过的三人逐人签收等真实UI证据继续沿用。
- **B：仍要求跨部门同人成功。** 业务需定义正式多归属来源、何种人员关系构成合法多归属，以及签收义务按人合并还是按部门分别产生；确认后另行评估实现和验收。本轮不扩展组织模型、不复制账号、不构造重复名册。

业务选择与批准人/依据尚未提供前，两项都只是待决策选项。R5未运行UI、Maven或上述异常拒绝/幂等测试；本轮不产生新业务或运行态证据。
