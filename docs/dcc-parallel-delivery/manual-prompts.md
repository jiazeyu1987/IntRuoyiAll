# 四个worktree手动派发提示词

2026-09-30。四个worktree已经创建，基线a801dc8b91579241e221d129ab34343997673f40。用户在Codex桌面分别选择下列目录，再发送对应提示词；不要另建worktree或让四线程写主目录。

| 模块 | 工作目录 | 分支 | 前端/后端预约端口 |
|---|---|---|---|
| A | C:/IntRuoyi/20260930-dcc-a | codex/20260930-dcc-a | 8063/48063 |
| B | C:/IntRuoyi/20260930-dcc-b | codex/20260930-dcc-b | 8064/48064 |
| C | C:/IntRuoyi/20260930-dcc-c | codex/20260930-dcc-c | 8065/48065 |
| D | C:/IntRuoyi/20260930-dcc-d | codex/20260930-dcc-d | 8066/48066 |

当前未启动服务。各worker的AGENTS.md与需求、任务书为主管理预置文档层，不要还原或当作本模块实现。implementation-contract.md（IC-1）已同步到每个worker，覆盖旧任务包中“G0基线未定、只设计”的过期说明。共同基线已经建立；跨模块尚未提供的实际接口仍需Review接入，不能伪造依赖。

前端node_modules为指向主工作区已有依赖的junction，允许使用既有工具做检查，不要安装、升级或清理共享依赖。公共页面与模型整合由主管理负责，模块线程不能跨worker改文件。

## A：审批与生命周期

```text
你负责DCC模块A，实际开发和定向验证，不只输出计划。

工作目录：C:/IntRuoyi/20260930-dcc-a
分支：codex/20260930-dcc-a
共同HEAD：a801dc8b91579241e221d129ab34343997673f40
预约端口：8063/48063；本次不启动服务。
先核对目录、分支和HEAD。不一致准确报告，不自动切换或覆盖。只修改此worktree。

先读AGENTS.md及docs对应开发/测试/数据库/task-closeout规则；再读docs/product/dcc-final-requirements.html v1.3、docs/dcc-parallel-delivery/task-a-workflow.md、ownership.md、verification-plan.md、implementation-contract.md(IC-1)。IC-1覆盖旧G0只设计文案。

实现范围：
1. 上传/升版：会签→批准→培训(如需)→文控审核→受控→下发；作废：会签→批准，通过即作废并结束。
2. 部门负责人先指派并签名，再由实际会签人签名，可指派自己；同人多部门不得省略义务。
3. 区分受控日期与编制预设生效日期。新版提前受控时旧版继续执行，新版生效时同事务自动作废旧版。
4. 文控按生效日期先后收到处置提醒；受控后才下发。下发无业务驳回分支，不代表可伪造技术成功。
5. 受控事件与生效事件分开；关联整改在受控成功后通知，驳回/失败不通知。

修改归属：Workflow/Finalization/Obsolete、路线、BPM新模型、批准效果与监听器、签名指派及独立生命周期组件。按IC-1可添加Master.latestControlledFileId；currentActiveControlledFileId只表示已生效执行版；File.controlledTime/activatedTime及CONTROLLED_PENDING_EFFECTIVE状态。只改A字段，不改B/C字段。公共上传、详情、浏览大页面由主管理整合，你提供独立组件和接入说明。

先建doc/tasks/20260930-dcc-a-workflow，写Given/When/Then，严格真实RED→GREEN→回归。覆盖培训分支、作废批准终止、两次签名、文控驳回、未来生效、并发/重放切换、失败旧版保护、权限/租户/错误签名。运行任务书定向测试并核对实际测试数量。隔离H2/内存Flowable测试可以运行，不能连接业务库。

不启用子Agent或外部app-server；不提交、推送、合并、写业务数据库、部署BPM、发布、启动/重启服务或真实E2E。保留天数、提醒提前量、培训标准未定时不猜测，提供正式配置与缺值错误。跨模块接口未到位先交精确接口说明，继续独立部分，不写fallback或假依赖。

阶段交付写入自身任务目录的verification-report.md和integration-notes.md：改动文件、字段接口、RED/GREEN命令/退出码/数量、回归、未运行/阻塞项和主管理接入点。最终回复报告绝对路径，等待主管理Review；未完成集成不能宣称全流程已验收。
```

## B：项目属性与基础配置

```text
你负责DCC模块B，实际开发和定向验证，不只输出计划。

工作目录：C:/IntRuoyi/20260930-dcc-b
分支：codex/20260930-dcc-b
共同HEAD：a801dc8b91579241e221d129ab34343997673f40
预约端口：8064/48064；本次不启动服务。
先核对目录、分支、HEAD，只修改本worktree，不自动切换或还原预置文档。

读AGENTS.md及对应docs规则、最终需求HTML v1.3，以及docs/dcc-parallel-delivery/task-b-project.md、ownership.md、verification-plan.md、implementation-contract.md(IC-1)。新基线已建立，IC-1覆盖旧G0只设计说明。

实现范围：
1. 项目创建/编辑三组默认属性：目标市场；是否注册人/是否生产方两个独立选择；文件转移及目标。
2. 提供上传、升版、作废共用值对象、默认值读取/校验服务和表单组件。新申请从项目默认初始化、用户可改；草稿回显实际值；项目修改不能覆盖在途/历史；作废属性独立保存、不回写原文件版本。
3. 项目负责人正式账号projectLeaderUserId及assertProjectLeader(userId,projectId)，不能用姓名、申请人、任意OWNER或admin权限代替。
4. 可复用文件夹模板库，项目创建选模板生成独立目录；编辑模板仅权限检查、不审批，修改模板不自动改已有项目。
5. 文件类型维护及正式矩阵映射，保护已使用目录与类型。产品创建审核人待讨论，不猜角色或默认批准。

修改归属：项目/产品/模板/类型服务、对象、controller、对应API与页面、独立属性组件。公共文件DO/VO、Workflow和上传/详情/浏览大页面不直接改，提交字段与integration-notes由主管理/A接入。市场多选、NA互斥、其他说明/转移目标条件必填按IC-1记录实现选择，保留MADSAP原名。

先建doc/tasks/20260930-dcc-b-project，BDD→真实RED→GREEN→回归。覆盖三个申请入口默认读取、手改只影响申请、历史冻结、切项目确认、属性非法/缺失、负责人授权、模板权限不审批、模板与既有目录独立、非空目录保护。运行任务书定向后端及独立组件测试，记录实际数量和退出码，不把字符串检查当真实保存回读。

不启用子Agent/外部app-server；不提交推送、不合并、写业务数据库、发布、启动/重启服务或真实E2E；不改主目录或其他worker。可运行隔离H2开发测试；依赖缺失准确报错，不假成功，不复制第二套共享模型绕开归属。

阶段交付verification-report.md和integration-notes.md：改动文件、属性/负责人/模板接口和组件、迁移依赖、RED/GREEN及回归、未运行/阻塞、主管理接入项。最终回复报告绝对路径供主管理Review，不宣称整套业务已验收。
```

## C：版本与名称编号占用

```text
你负责DCC模块C，实际开发和定向验证，不只输出计划。

工作目录：C:/IntRuoyi/20260930-dcc-c
分支：codex/20260930-dcc-c
共同HEAD：a801dc8b91579241e221d129ab34343997673f40
预约端口：8065/48065；本次不启动服务。
先核对目录、分支和HEAD，只修改本worktree，不自动切换或还原预置文档。

读AGENTS.md、对应docs规则、最终需求HTML v1.3和docs/dcc-parallel-delivery/task-c-version.md、ownership.md、verification-plan.md、implementation-contract.md(IC-1)。共同基线已建立，IC-1覆盖旧G0说明。

实现范围：
1. 检出互斥、检出人显示、本人检入/撤销及失败保护；检入只生成A/1-1、A/1-2等小版本。
2. 选小版本单独发起局部/换版。局部A/1→A/2、A/9→B/1且无A/10；换版A/3→B/1。
3. 保存revisionChangeType=INITIAL/PARTIAL/REPLACEMENT、来源受控版和选中小版。所有版本详情和历史显示实际变更类型，不能由版本号推断。
4. 同名按含后缀的完整源文件名区分大小写精确比较，跨项目判重。SOP.pdf与sop.pdf、SOP.PDF、SOP.docx均不同；不用模板标题或派生PDF名代替。
5. 作废保留期内原名称和编号继续占用，同一逻辑文件链升版可以沿用。提供正式占用/保留/释放服务，不硬编码期限，不误释放仍被当前版占用的身份。

修改归属：版本策略、QueryService、NameClaim及Mapper、独立Revision组件与测试。IC-1允许本worker公共FileDO/相关VO增加revisionChangeType和源完整名称身份，只改C字段，不改A生命周期字段。Finalization/Obsolete归A，你提供调用接口；公共大页面由主管理接入。

先建doc/tasks/20260930-dcc-c-version，BDD→真实RED→GREEN→回归。覆盖横杠小版、两种变更、9进位、同为B/1但来源不同、任意版本变更类型、并发检出/分配、上传失败/源件未变、送审冻结、精确名称及唯一约束、保留期与同链复用、错租户/无权限。按任务书运行实际定向测试，核对数量。MySQL字符索引提供迁移合同验证，未授权真实库验证记NOT RUN，不能以H2排序冒充。

不启用子Agent/外部app-server；不提交推送、不合并、写业务数据库、发布、启动/重启服务或真实E2E；不改主目录/其他worker、不修改共享node_modules。未定保留期和Z后规则不能猜；依赖不足交接口说明并继续独立部分，不造假数据或fallback。

交verification-report.md及integration-notes.md：文件、算法/字段/接口、迁移、RED/GREEN与回归命令/数量、未运行/阻塞、A及主管理接入项。最终回复报告绝对路径，等待主管理Review。
```

## D：关联、引用与选择器

```text
你负责DCC模块D，实际开发独立服务/组件和定向测试，不只输出计划。

工作目录：C:/IntRuoyi/20260930-dcc-d
分支：codex/20260930-dcc-d
共同HEAD：a801dc8b91579241e221d129ab34343997673f40
预约端口：8066/48066；本次不启动服务。
先核对目录、分支和HEAD，只修改本worktree，不自动切换或还原预置文档。

读AGENTS.md、对应docs规则、最终需求HTML v1.3及docs/dcc-parallel-delivery/task-d-relations.md、ownership.md、verification-plan.md、implementation-contract.md(IC-1)。新基线已建立，IC-1覆盖旧G0只设计说明。

实现范围：
1. 统一选择器/关联窗口：顶部当前文件，左侧项目文件夹定位，右侧全局有权限项目搜索；分页、已选保留、去重及禁止自关联。
2. 跨项目关联稳定文件身份，当前关联取Master.latestControlledFileId并显示待生效；历史审批关系保留当时版，不能回退到currentActive执行版或覆盖历史。
3. 会签保存选定关联文件的整改负责人/期限；未选不生成任务。A受控成功事件才通知，审批中断/受控失败不通知，重放去重；不能改为生效日通知。
4. 引用/取消引用是独立项目文件夹关系，只有目标项目正式负责人可操作，普通成员/其他项目负责人/未任该负责人admin均拒绝。调用B的assertProjectLeader，不从姓名或任意OWNER猜。
5. 引用橙色加文字，数量按项目去重；取消二次确认，最后一个引用解除正确减数和恢复颜色，不删除源文件、历史或文件关联。

修改归属：RelatedFile/PublicationFollowup/ImpactAssessment及独立关系对象、controller、引用服务/API和选择器组件。公共Query由C、项目大页由B、上传详情浏览主页面由主管理接入，不直接改。引用跟版策略尚未确认时按IC-1保留选定版本身份，不把关联规则套给引用。

先建doc/tasks/20260930-dcc-d-relations，BDD→真实RED→GREEN→回归。覆盖跨项目/分页保留、当前最新与历史分离、名称与正文权限、失败零通知/事件去重、负责人权限无旁路、按项目计数、取消最后引用、源作废、保存失败/旧响应不串上下文。运行任务书定向后端和独立组件测试，记录真实数量/退出码，不把API或字符串检查当页面E2E。

A的latest字段/事件或B的负责人服务尚未到位时，交精确依赖清单并继续不依赖它们的部分，由主管理Review后同步；禁止自行读写其他worker生产文件、复制假实现或fallback。不要启用子Agent/外部app-server，不提交推送、不合并、写业务库、发布、启动/重启服务或真实E2E。

交verification-report.md和integration-notes.md：文件、关系/API/组件、迁移、RED/GREEN及回归、未运行/阻塞、跨模块和公共页面接入项。最终回复报告绝对路径供主管理Review，不宣称整个系统已验收。
```
