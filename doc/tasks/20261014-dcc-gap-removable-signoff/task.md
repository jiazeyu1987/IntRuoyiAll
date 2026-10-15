# G04 会签部门全部可增删

## Current Status
blocked — 原G04已接收，A32/R09实现与260项开发回归通过，已ready_for_closeout并做cleanup preview；4生产源/所属测试正式申请归还停写。仅Git工程收尾与Root集成接收待处理，非软件测试失败。

## 目标
本次申请可移除公共矩阵原必选会签部门，也可增加部门；至少保留一个。最终选入的每个部门均须完成并通过。公共矩阵及批准、文控规则保持原事实。

## 里程碑
1. 核对 int_qms、规则、当前实现与相邻测试，保存改前 SHA256。
2. 记录 BDD，新增真实行为测试并记录 RED。
3. 在授权三个生产文件内修复并记录 GREEN/REGRESSION。
4. 提供 review-ready、changed-files、integration-notes、verification-report，申请 Root 复核/归还。
5. A32协助G03收口CUSTOM真实上传入口：只写upload/index.vue、submitter.ts、approval-matrix.ts及其专属/受影响测试；原已接收BE/会签helper等全部冻结。
6. R09追加回授signoff-departments.ts，仅resolvedUploadApprovers原PUBLIC批准组guard接受登记V1/V2，派生版本仍原DCC_PUBLIC_MATRIX_NATIVE_V1；真实helper RED/GREEN，全部原组与资格校验保留，不走旧fallback，不接受未知合同。与A32一并归还，不重复BE套件。

## A32 BDD及预期验证
- Given正式类型来源APPLICATION_CUSTOM，When在真实CustomEditor/page handler选择两账号和会签部门，Then实际route-preview承载唯一配置/部门；首次非空会签复制下发一次，后续部门独立增删，至少一个才可提交。
- GivenCUSTOM账号或属性/部门/关联已编辑，When切项目/类型或来源模式，Then必须确认；取消保留完整原值与票据，确认后清原关联/配置/来源票据并重读正式来源。
- GivenCUSTOM未完整配置或人员预检未就绪，When保存草稿，Then用SAVE阶段实际null/部分选择、真实server token保存无任务，不要求PublicRouteID/baseline。恢复已保存配置不得刷新PUBLIC默认；保存后file/reservedRound/context合同由G03正式API/DetailPanel消费。
- Given完整CUSTOM预检及全部源/附件上传成功，When提交确认，Then列编制/主管/批准/文控/本次矩阵/属性/附件/关联/培训/下发全员事实；取消零提交/任务且保填写；晚变化使旧配置票据失效。
- GivenPREVIEW/RESERVED阶段、context/实际部门/两账号/主管proof任一不一致或票据缺失，WhenSAVE/SUBMIT构造，Then失败且零写请求；客户端校验不冒服务器HMAC/人员资格。
- Node执行实际Vue template/handlers与真实state/payload helper RED→GREEN及相邻PUBLIC/DELAYED/null/可选说明/Long身份回归；不全类型/Maven/build/E2E/服务/Git/实库，不增依赖。

## A32 设计约束检查
- 三文件改前hash与G03 handback.json完全一致；baseline见a32-upload-before.json。
- 公共workflow.ts/applicationRead.ts/backend/DetailApplicationPanel.vue仍G03，必要字段/parser差异仅写本任务integration-notes转Root。
- 原BE Derivation/Resolver/G05权限及signoff helper均不改；只在三授权FE文件及所属测试内完成本阶段。

## 预期验证
- 独立 Node 单测执行实际 TypeScript helper：删除 A/保留 B、B+C、精确空提示、批准规则不弱化、公共数据不变。
- 任务私有 javac/JUnit 输出编译当前生产源码及当前测试（只读现有依赖 classpath，不写共享 target）：派生/解析行为和非法身份回归。
- 本任务专属完成证据回归已执行：本次派生B/C生成冻结名单，调用当前DccFrozenApprovalSignatures.requireComplete；缺C、拒绝、错轮次/版本、同负责人重复B均拒绝，B/C指派和通过证据完整才允许。仅隔离合同，不冒称真实签名办理。
- Maven/全类型/大构建由 Root 发唯一验证票后才运行。本轮不做 E2E 或运行验收。

## BDD
- Given 公共矩阵 A/B 且 A 标必选；When 本次删 A、保留 B 并做正式路线解析；Then 本次会签仅有 B，A 不产生本次部门义务，公共配置事实不变。
- Given 本次会签为空；When 预检；Then 阻止，显示“请至少选择一个会签部门”，不能恢复默认名单。
- Given 本次保留 B 并增加 C；When 解析路线；Then B/C 都进入本次义务，采用 ALL/100/requireAllApprovals=true，不可任一通过；同一负责人也保留两份部门身份。
- Given A/B 原配置与批准/文控原组；When 增删本次会签；Then 原配置、原批准规则和文控规则不变，原会签组保留来源事实并如实标记 applied。
- Given 类型正式 A=●、B=◎（或全部为 ◎/历史▲非必选）；When 首次预检不传本次名单；Then 带出全部已配置默认会签部门，独立复制为下发初始名单；不能因optional非空阻断。R02真实RED/修复/GREEN已通过。

## 设计约束检查
- 用户最新 G04 授权覆盖旧 Owner 表及旧“会签名单必须完整保留矩阵”的规则，只针对本次部门选择。
- 仅写 DccPublicMatrixNativeDerivation.java、DccControlledFileApprovalRouteAssigneeResolver.java、upload/signoff-departments.ts 及其专属测试；不写 upload/index.vue、Workflow、共享 fixture。
- 禁止子 Agent、Git 提交推送/切分支、worktree、E2E、服务重启、远端、真实库写入。
- 不增加 fallback、默认原因、自动选会签人或公共配置写入。不改变租户、签名、权限、版本、事务或重放身份。
- null 默认来源和显式空选择保持可区分；null 取正式类型全部默认会签部门，显式[]拒绝；非法、重复或不精确身份仍拒绝。

## Cleanup Keep
- doc/tasks/20261014-dcc-gap-removable-signoff/task.md
- doc/tasks/20261014-dcc-gap-removable-signoff/execution-log.md
- doc/tasks/20261014-dcc-gap-removable-signoff/review-ready.md
- doc/tasks/20261014-dcc-gap-removable-signoff/changed-files.md
- doc/tasks/20261014-dcc-gap-removable-signoff/integration-notes.md
- doc/tasks/20261014-dcc-gap-removable-signoff/verification-report.md
- doc/tasks/20261014-dcc-gap-removable-signoff/baseline.json
- doc/tasks/20261014-dcc-gap-removable-signoff/run-private-tests.ps1
- doc/tasks/20261014-dcc-gap-removable-signoff/G04TestLauncher.java
- doc/tasks/20261014-dcc-gap-removable-signoff/default-preview-review.md
- doc/tasks/20261014-dcc-gap-removable-signoff/final-source-manifest.json
- doc/tasks/20261014-dcc-gap-removable-signoff/private-verification-manifest.json
- doc/tasks/20261014-dcc-gap-removable-signoff/a32-upload-before.json
- doc/tasks/20261014-dcc-gap-removable-signoff/a32-upload-after.json
- doc/tasks/20261014-dcc-gap-removable-signoff/a32-r09-verification.json
