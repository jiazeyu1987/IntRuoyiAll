# DCC G06 升版多附件闭环

## Current Status

blocked — 全软件验收已由Root最终接收：有效47份后端class报告合计1008/1008、0F/E/skip，11865记录输入0漂移；F02 38/38、T04类型与B01构建通过，2429前端生产输入0漂移。仅Git提交/推送收尾未授权，不标工程completed；Root统一清理/报告。生产/tests保持冻结，无软件阻塞。

## 最终软件验收回执（M14补齐）

已只读核验Root final-verification.json、final-source-check.json、M14正式verification/receipt：M14 exit0、23reactor SUCCESS，G48声明18与独立父类27合计45/45，0F/E/skip；结合M12其余45类963PASS，最终47份有效class报告1008/1008。M12→M14仅G12测试fixture补明确required来源，无生产差异。此前M12全局45ERROR已关闭，不再作为待修项。

F02最终主申请行为38/38、T04完整类型exit0、B01完整Vite test build exit0均已由Root接收；后端11865/前端2429输入各0漂移。本线程仅更新记录和证据，未执行任何套件或改生产/tests。未要求的E2E/部署不列软件失败或关闭门禁；仅Git收尾未授权，工程不completed。最终摘要见final-software-acceptance.json。

## M12 最终软件接收

已实际读取Root m12-verification.json、reactor receipt、source-drift及9份G06相关fresh XML并核其SHA。A34 6/6、附件37/37、上传8/8、Query272/272、旧5DB211/211，全部0F/E/skip。M12整体1008项中963PASS、45ERROR、0FAIL/skip；唯一45ERROR为G12 G48 BeforeEach分类fixture缺required approvalSourceMode，非G06失败，Root仅M13复验该项。11865记录输入、0漂移，Root更正数字已准确保存。

Root明确接收本线程全部软件域，不再改生产/tests、不重跑已过套件。m12-software-acceptance.json归档本轮正式证据及全局边界；项目整体/工程Git仍由Root统一收口，无新Git/E2E/服务/实库授权。下方阶段内旧in_progress/等待联合仅保留历史，不覆盖本状态。

第一阶段材料已交付。2026-10-15 Root A08授权4个生产文件和2个专属测试；M02已回收，允许私有javac/JUnit，不写共享target，不运行Maven。Query/G07、Revision/Workflow/API/G01、browser/G08仍禁止写；不得标完整升版完成。

## A08 范围与新增BDD

- 授权生产：DccControlledFileUploadServiceImpl.java仅CHECKIN+ATTACHMENT；DccControlledFileAttachmentService.java/Impl.java仅新增工作稿绑定与正式版冻结事实API；DccControlledFileCheckinReqVO.java仅多附件票字段。
- 授权测试：service/upload/DccCheckinAttachmentUploadContextTest.java、service/file/DccRevisionAttachmentsDatabaseTest.java；使用自有H2内存schema与真实Mapper/票据/附件服务/事务，不改共享schema。
- BDD: CHECKIN双附件上传 → Given 合法租户/检出人/项目类别和目录；When 每票独立会话上传ATTACHMENT；Then 两张正式AVAILABLE票据且归精确base版本；同槽异内容冲突。
- BDD: 工作稿绑定 → Given 同Master新工作稿、合法有效检出和两张AVAILABLE普通票据；When 在业务事务绑定；Then 两行实际落库并两票BOUND；任意票/行失败全部回滚，权限失败零写。
- BDD: 正式版冻结 → Given 所选稿两普通附件及历史培训；When 有权限的申请人/OWNER在候选事务冻结准确清单；Then 新目标保存两行和独立已验证字节，上传事实保留、历史训练不复制，票据不重绑；晚失败回滚后完整重试。
- BDD: 清单重放 → Given 正式目标已有冻结清单；When 相同完整清单重放；Then 零新增；异清单/不完整事实明确拒绝，不猜第一份或当前版本。

A08当前结果：RED 21项4PASS/17FAIL；补版本门禁RED 24项23PASS/1FAIL；最终GREEN 24/24、相邻REGRESSION 54/54，0skip，实际source drift=[]。已实现服务未被Query/Revision/Workflow或页面接线，phase提交Root review，不标总体完成。

## Root A08 review补强及独立组件授权

- 2026-10-15 Root要求补强formal目标与精确selectedIteration/基线版本合同，源body/源票/附件票据同事务晚失败及实际ticket副作用回滚证据；原4生产/2测试范围保持。
- 新授权仅新增IntRuoyiFronted/src/views/dcc/controlled-file/browser/CheckinAttachmentsPanel.vue及其专属真实Vue测试，browser/index.vue/G01公共API继续禁止写。
- BDD: formal身份 → Given 目标同Master但selected/predecessor/sourceBaseline或formal version错误；When 冻结；Then 拒绝且零copy/附件写入；合法target保留既有Revision来源字段。
- BDD: 正文与附件晚失败 → Given 本次源body及源票、两附件票均正式创建；When 同一事务已绑定源body与全部票据后出现第二附件/晚阶段失败；Then body/所有票据/附件/副本全部回滚，完整重试成功。
- BDD: 独立Vue组件 → Given 正式CHECKIN source context；When 选择两普通附件；Then 通过原uploadPreview API逐项独立session上传，完整就绪才输出清单；失败/未完成/无context拒绝，清理/切版本迟到响应不串版；未接父页不得冒称用户检入链路已完成。

## A17 浏览父页接线授权

Root正式转交browser/index.vue整文件（G07归还）以接入多附件状态/提交/取消/迟到回包；保留G08版本helper和精确ID、G07正文actualversion读取context。Query和workflow.ts公共API仍禁止写。前端BDD追加：Given 原source已READY而普通附件至少一项UPLOADING/FAILED/保存失败；When 实际submitCheckin；Then 零checkin请求、无成功提示；Given 两附件READY；When 实际提交；Then 两票独立session随MINOR payload完整发送，backend接线未获授权时不宣称DB绑定已完成。新增父页专属真实handler测试允许。

当前结果：A15 formal/完整SOURCE事务补强后BE GREEN28/28、regression58/58；A17组件/父页及G07/G08相邻前端定向49/49，Vue SFC转换成功。两个旧fixture未获写权限，直接命令仍因缺新依赖FAIL，临时host补依赖的原24项PASS只作补充证据。Query当前未绑定实际请求附件，父页不会把缺失保存清单的正文响应冒称成功。公共manifest/hash及确认送审未完成，具体hunk在a17-integration-hunks.md。

## A18 两旧fixture正式接入

2026-10-15 Root明确转交IntRuoyiFronted/tests/unit/dcc-browser-checkin-minor-only.test.cjs及dcc-browser-checkin-training-default.test.cjs，仅补无附件ready组件依赖和当前上下文，保留全部原身份/培训/late/图纸断言，不删不弱。需直接canonical Node命令通过；stdin注入不再作为仓库PASS。Query/Revision/Workflow及公共workflow.ts仍不写。

BDD: 旧检入无附件场景 → Given 原正式source与空的已就绪普通附件组件；When 真实submitCheckin/handleCheckin及Vue弹框渲染在原fixture中执行；Then 原身份、培训、图纸、迟到响应、精确ID断言均通过且无需运行时外部注入；实际未就绪附件仍由本任务专属测试拒绝。修改前保存两fixture hash及AST assertion列表，修改后原assertion必须逐项相同。

A18当前canonical2文件24/24、合并8文件73/73，0skip；原76assertion及24测试名AST完全相同。原A17旧fixture失败已关闭，不用stdin充当仓库PASS。Root新增请求hash/committed BOUND精确重放/正式manifest稳定事实摘要需求已给方法hunk，公共接线仍待Root转交。整体仍in_progress。

## A20 Query正式接线与T01修复

2026-10-15 Root接收G07 Query后整文件转交G06，仅checkin完整请求hash、BOUND精确清单重放、同事务普通附件绑定与成功响应验证。正文上下文/C04读取时间/Iteration显示/所有无关读投影保护。修改前重采Query raw/blob/hash；Maven/fulltype仍无票。旧checkout DO与Mapper不含请求hash，需最小正式schema/DO字段Root授权，不挪用source SHA/ticket/旁表或client hash。

BDD: 有序请求 → Given 源/图纸/两附件独立session正式上传；When Query同事务检入；Then 全票/附件/正文/checkout状态/hash一次保存且返回准确清单；晚失败所有回滚。BDD: committed重放 → Given checkout已CHECKED_IN且全票BOUND；When 相同完整payload重试；Then 前于AVAILABLE门禁精确核hash/全清单零写返回，遗漏/空[]/乱序/变hash失败；null未读取不能当[]。BDD: cleanupAll → Given 真实Element file-list含两附件；When 正式beforeRemove经cleanupAll执行；Then 使用实际file与file-list签名或同领域helper，共同清理失败保留项、保存中/late状态不绕过，不cast/optional/空集合压错。

## A21 正式checkout字段授权

Root核准CheckoutDO.checkinPayloadHash、CheckoutMapper.markCheckedIn新增server payloadHash实参及同CAS列写、唯一20261015_dcc_checkin_payload_hash.sql(nullableCHAR64守护DDL，无历史UPDATE)。本线程私有schema可改，共享create_tables.sql由G05，不并写。canonical精确tenant/actor/base/MINOR/source+diagramticket/session/trim说明/remark null语义/明确training/有序附件全含；committed先hash后全部BOUND比，旧null拒完整重放。后端私有编译JUnit，Maven/fulltype仍无票。

## A23 正式升版与延迟确认读投影

Root正式转交DccControlledFileRevisionServiceImpl.java整文件，仅所选附件冻结/稳定事实manifest进入creation payloadHash/真实重试；保持正式sourceproof及G03执行合同，不另猜配置。WorkflowImpl/SubmitIterationVO/publicWorkflowAPI/DetailApplicationPanel目前G03owner，不写，精确ID+serverhash+完整确认hunk交Root转她。Query额外授新PENDING_DELAYED_EFFECTIVE_CONFIRM精确办理正文来源分支和4个delayed保存字段只读映射，serverpermission仍G07Guard，日常不放行。

BDD: 真正Revision → Given 精确工作稿普通附件manifest；When 调用实际createRevision或createInitialCandidate；Then 正式target保存独立附件副本及原事实、candidateHash含有序ID和稳定事实；同键改变hash/事实拒绝、晚失败回滚可重试。BDD: 延迟确认读 → Given 所选版本保存4个delayed事实且新stage；When 合法exact审批context读正文/详情；Then 只读该版本source/diagram与已存4事实；日常/错误context仍Guard拒绝，不now/default填值。

## A24 既有六BE测试fixture转交

Root明确授a21-fixture-handoff.md六文件仅checkin fixture、markCheckedIn第8参数及真实BOUND事实调整，所有原G07正文/图纸/权限/late/锁/轮次断言保护。原SchemaOwner G05当前同步的新category14fixture作为只读输入。success精确hash期望，不anyhash/compat overload。新formal确认公共owner仍G03，G06不写公共VO/Workflow/API。

## M10 冻结交接及唯一资源定位修复

Root已收211/211、G06新45及相邻75 checkpoint，17份后端生产/测试冻结见m10-frozen-sources.json。Root M10正式-am main/testCompile PASS；11类539测试中523PASS、16ERROR、0FAIL/skip，全部16ERROR为G06新QueryFixture用仓库相对路径在Maven模块cwd找不到真实create_tables.sql。Root当轮仅授权修该test真实root定位及private runner显式root参数，生产继续冻结。BDD: Given Maven basedir精确模块或standalone明确dcc.repository.root；When 运行16个QueryFixture；Then 都只读当前正式schema资源，验证AGENTS/POMmarkers；缺失/相对/错误根明确失败，不复制/猜schema。原16业务断言完整保留，不重跑75/211。

## A34 最小custom继承hunk

Root M11正式66/66接收A34前软件checkpoint。现仅解除Query/Revision的custom三字段继承位置冻结：实际target INSERT/prepared/真实targetRound之后调用G03正式readInheritedCustomDraft，同原tenant/id/WORKING/nullBPM CAS保存3字段并同步本地对象，affected必须1；public null不改原通道。G03负责完整reject/checkin/candidate/resubmit组合，G06专属DccCustomDraftInheritanceIntegrationTest用真实ExecutionContract/CustomMatrix/initializer/round/Mapper/H2，不能mock CustomDraftInheritance记录冒验收。

BDD: Given 已保存custom源和实际V6 definition/source-round；When 调用真正Query检入；Then 新target三字段保持本次值且canonical的file/round/requester为新target；public不引入custom。BDD: Given 已真实准备custom WORKING；When 真正Revision生成initial candidate；Then 初始化实际candidate round并由typed服务重建canonical，旧source事实不改。BDD: Given CAS写入晚失败；When 当前创建事务回滚后重试；Then target/prepared/attributes/三字段无半保存，重试完整，既有manifest/正文/权限守卫保持。

A34结果：首次private fixture未建附件表先环境失败，补仅本测试私有形状后真实3FAIL（缺字段、缺targetRound、CAS约束未触发）→3PASS；补exact-file约束取得新增RED（clone旧canonical在INSERT即错），INSERT前清空三字段后5PASS；加同范围公共Legacy保持用例最终6/6、javac0/JUnit0、drift=[]。新类直接BaseDbUnitTest，不继承业务tests；Root冻结消息后无生产/test变更，精确六方法与SHA见a34-frozen-sources.json和review-ready.md。

## 任务目标

检入及升版支持本次新增多个普通附件；源文件与全部附件上传、保存成功才可送审；确认框展示完整附件清单；附件随本次申请及目标版本保存；历史清单与历史培训记录隔离；晚失败事务回滚后能够完整重试。

## 授权与基线

- 本轮唯一写范围：doc/tasks/20261014-dcc-gap-revision-attachments/。初始 SPEC_ONLY。
- 2026-10-14 实际执行 git branch --show-current，输出 int_qms；没有切换分支。
- 已读根 AGENTS.md、docs/task-closeout-rules.md、backend-development.md、frontend-development.md 中适用规则，以及协调任务 requirements.md、coordination.md。
- 未运行 Maven、全类型/大构建、E2E、服务、真实业务数据库写入、远端、Git 提交推送、worktree或子 Agent。
- 工作区存在并行改动；不将其算作本任务实现，不回滚、不清理、不改写。
- 自有记录改前状态：任务目录不存在，task.md/execution-log.md 为新建，无改前 hash；只读生产源码指纹及锚点将记录于 source-baseline.json。

## 里程碑

1. 核对分支、规则及阶段权限，建立任务记录。
2. 复核检入→工作版本→申请→确认→送审、上传升版与详情附件/培训读取链路。
3. 阅读相邻测试，区分真实行为测试、静态合同和未覆盖场景。
4. 冻结最小端到端合同、BDD、真实 RED/GREEN/REGRESSION 方案及逐文件交接需求。
5. 第一阶段提交 review-ready.md、changed-files.md、integration-notes.md、verification-report.md；等待 Root 核准文件转交与唯一验证票。

## BDD

- BDD: 双附件检入及送审 → Given 本次选择两个普通附件并上传成功；When 检入新工作版本且确认送审；Then 两个附件均归本次申请和新版本，确认框完整列出，源文件身份独立。
- BDD: 双附件大版本升版 → Given 有权限的 OWNER 选择合法来源与两个新普通附件；When 创建目标大版本并确认提交；Then 本次新版本和申请只归属本次清单，不复制历史培训附件。
- BDD: 失败/未完成阻断 → Given 任一源文件或本次附件 uploading/failed/保存失败/票据无效；When 检入或送审；Then 明确指出失败项并拒绝，无申请推进或部分正式绑定。
- BDD: 历史隔离 → Given 历史申请有普通附件及培训附件，新版本另有普通附件；When 查看各版本/申请；Then 每个清单只呈现其自身正式事实，历史培训不变成本次普通附件。
- BDD: 取消确认 → Given 全部内容已填写；When 取消确认送审；Then 保留填写且不创建审批、不签名、不送审。
- BDD: 晚失败与重试 → Given 第二附件绑定或申请持久化/送审在晚阶段失败；When 重试相同正式幂等键与完整载荷；Then 初次全部数据库事实回滚，重试完整成功且无重复行；同键改清单明确冲突。

## 预期验证

- SPEC_ONLY：源码与相邻测试只读复核，指纹/锚点记录；自有 Markdown/JSON 结构验证。
- 生产授权后：正式服务行为及隔离数据库事务测试先 RED，再 GREEN/REGRESSION；FE 调用真实生产函数的状态/提交/确认行为单测。
- 后端 Maven、全类型与大构建仅 Root 唯一验证票允许；未获票不得运行。真实页面验收需要额外当轮 E2E 授权。
- 第一阶段不能标 G06 实现完成；工程 completed 不允许，本轮未授权 Git 收尾。

## 设计约束检查

- 正式票据只有一套；不新增绕过 ticket/权限/租户的平行 API。
- 明确上传操作者、租户、用途、申请/精确目标版本、文件类型及幂等载荷；不依赖名称或默认当前版推断。
- 检入工作稿 requester、正式大版本 OWNER、主管/矩阵/签名与版本策略保持现有正式边界。
- 普通附件、源文件、培训记录三条事实不混用；历史培训记录不复制至新申请。
- 不吞异常、不模拟成功、不造默认原因；DB 写入和票据绑定参与同一事务。
- 收到明确逐文件转交前不修改公共模型、接口、服务及前端页面。

## Cleanup Keep

- doc/tasks/20261014-dcc-gap-revision-attachments/task.md
- doc/tasks/20261014-dcc-gap-revision-attachments/execution-log.md
- doc/tasks/20261014-dcc-gap-revision-attachments/source-baseline.json
- doc/tasks/20261014-dcc-gap-revision-attachments/review-ready.md
- doc/tasks/20261014-dcc-gap-revision-attachments/changed-files.md
- doc/tasks/20261014-dcc-gap-revision-attachments/integration-notes.md
- doc/tasks/20261014-dcc-gap-revision-attachments/verification-report.md
- doc/tasks/20261014-dcc-gap-revision-attachments/a08-before.json
- doc/tasks/20261014-dcc-gap-revision-attachments/a08-after.json
- doc/tasks/20261014-dcc-gap-revision-attachments/G06TestLauncher.java
- doc/tasks/20261014-dcc-gap-revision-attachments/run-private-tests.ps1
- doc/tasks/20261014-dcc-gap-revision-attachments/red/receipt.json
- doc/tasks/20261014-dcc-gap-revision-attachments/red/compiled-sources.json
- doc/tasks/20261014-dcc-gap-revision-attachments/a08-version-guard-red-receipt.json
- doc/tasks/20261014-dcc-gap-revision-attachments/green/receipt.json
- doc/tasks/20261014-dcc-gap-revision-attachments/green/compiled-sources.json
- doc/tasks/20261014-dcc-gap-revision-attachments/regression/receipt.json
- doc/tasks/20261014-dcc-gap-revision-attachments/regression/compiled-sources.json
- doc/tasks/20261014-dcc-gap-revision-attachments/a15-formal-target-red-receipt.json
- doc/tasks/20261014-dcc-gap-revision-attachments/a17-before.json
- doc/tasks/20261014-dcc-gap-revision-attachments/a17-after.json
- doc/tasks/20261014-dcc-gap-revision-attachments/a17-integration-hunks.md
- doc/tasks/20261014-dcc-gap-revision-attachments/a18-before.json
- doc/tasks/20261014-dcc-gap-revision-attachments/a18-after.json
- doc/tasks/20261014-dcc-gap-revision-attachments/m10-frozen-sources.json
- doc/tasks/20261014-dcc-gap-revision-attachments/m10-path-before.json
- doc/tasks/20261014-dcc-gap-revision-attachments/m11-frozen-sources.json
- doc/tasks/20261014-dcc-gap-revision-attachments/a20-schema-request.md
- doc/tasks/20261014-dcc-gap-revision-attachments/a21-fixture-handoff.md
- doc/tasks/20261014-dcc-gap-revision-attachments/a23-g03-manifest-handoff.md
- doc/tasks/20261014-dcc-gap-revision-attachments/a34-before.json
- doc/tasks/20261014-dcc-gap-revision-attachments/a34-frozen-sources.json
- doc/tasks/20261014-dcc-gap-revision-attachments/a34-red/receipt.json
- doc/tasks/20261014-dcc-gap-revision-attachments/a34-red/compiled-sources.json
- doc/tasks/20261014-dcc-gap-revision-attachments/a34-green/receipt.json
- doc/tasks/20261014-dcc-gap-revision-attachments/a34-green/compiled-sources.json
- doc/tasks/20261014-dcc-gap-revision-attachments/a34-no-old-canonical-red-receipt.json
- doc/tasks/20261014-dcc-gap-revision-attachments/m12-software-acceptance.json
- doc/tasks/20261014-dcc-gap-revision-attachments/final-software-acceptance.json
