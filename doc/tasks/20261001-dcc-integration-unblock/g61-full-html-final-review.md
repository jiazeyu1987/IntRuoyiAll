# HTML v1.6 最终代码核对与修复验收

Status: ready_for_closeout — 27项已确认业务逐项源码核对完成，明确差异已修复，关键真实主流程通过。

业务依据为 `docs/product/dcc-final-requirements.html` 的12个流程及27项验收场景。四个 worktree 成果已合入本机 int_qms；当前修复也只提交到 int_qms。HTML 的规则保持不变，仅将已确认的上海时区、提前7天和每分钟检查口径同步到文档。

逐项代码核对及定向验证已覆盖已确认的业务方向。本报告将正式代码、定向测试和真实页面分开记录，不把尚未执行的负向排列写成真实页面PASS。

## 本轮修复

- 补齐上传、升版会签安排、关联冻结和只读浏览追溯的正式接线。
- 补齐文控线下培训待办、独立上传记录权限及正式通知；不增加在线培训或额外批准节点。
- 修复选择小版本、正文来源、工作稿显示和局部变更申请。
- 统一文控与共享受控内容的提前受控、生效、旧版作废事务；提供严格校验原证据的显式历史投影维护入口。
- 修复待生效受控文件的独立作废事务和操作面板入口。

## 27项核对

| 场景 | 源码与定向证据 | 本轮真实页面边界 |
|---|---|---|
| AC-01 | ProjectProductCreate + frozen ReviewerConfiguration + transactional USER/OWNER (G55 real positive) | G55 positive singleadmin and rejected/resubmitted requests; configuration-change/independentactor not re-run |
| AC-02 | ProjectAttributes beginDraft/save/freeze, defaultSource and actual immutable per submitted round (G55 real CE/FDA) | G55 passed CEdefault/FDA actual |
| AC-03 | Revision/Obsolete new draft from current project, old version actual kept (G56 lifecycle source review) | NOT_RUN_THIS_ACCEPTANCE_BRANCH; do not equate code/test evidence with real-page PASS |
| AC-04 | reservedDraft + immutable round snapshots, project edits affect future starts (G56 lifecycle source review) | NOT_RUN_THIS_ACCEPTANCE_BRANCH; do not equate code/test evidence with real-page PASS |
| AC-05 | ProjectAttributesFields and backend validated N/A/OTHER/transfer invariants | NOT_RUN_THIS_ACCEPTANCE_BRANCH; do not equate code/test evidence with real-page PASS |
| AC-06 | 正式上传两次确认和关联冻结在同一申请事务完成；非空关联先合法冻结再送审（G56已修）。 | G55 cancel/two-confirm PASS;G56 new linked +training real submit PASS r2/036 and frozenrelationrow1 aftercorrectsource |
| AC-07 | exact task-definition/department obligation, ASSIGN and APPROVE separate signatures; G55 six real signatures | G55 two departments sameadmin ASSIGN/APPROVE PASS, independentperson not re-run |
| AC-08 | native rejection cancels current round/rework predecessor and full new round, no inherited signature result; R01 initial revision read repaired | NOT_RUN_THIS_ACCEPTANCE_BRANCH; do not equate code/test evidence with real-page PASS |
| AC-09 | 正式上传/升版会签→批准→可选线下培训记录→文控审核→受控→下发；独立TRAINING_RECORD权限和真实待办通知（G57/G58）。 | G55无培训上传；G56关联+培训上传→文控审核→受控→下发；G61独立文控910328真实上传线下记录，培训待办1→0，随后审核并提前受控。未把上传组合外推为升版培训组合。 |
| AC-10 | Master lock + explicit checkout owner + actual checked-in iteration source and lifecycle invariants | G58真实检出、两次检入形成A/1-1和A/1-2并释锁；跨账号抢锁由定向事务测试证明，未称真实页面竞争通过。 |
| AC-11 | RevisionService exact selected WORKING source + PARTIAL/REPLACEMENT intent, single-letter1..9 rollover policy | G60真实选定A/1-1正文申请PARTIAL，形成A/2并受控；A/9进位和换版由正式版本策略测试证明，未称全部真实办理。 |
| AC-12 | 受控更新latest，生效才更新currentActive并自动作废旧版；native/shared同事务投影，分钟任务单注册恢复（G59/G60）。 | G60提前受控A/2时原A/1继续执行；2026-10-06上海零点自然分钟job44263生效2条，A/2 ACTIVE、A/1 OBSOLETE，native/shared/Master指针同向，原日期与历史签名保留。关联最新/冻结快照亦已真实对比。 |
| AC-13 | R01 exact first REVISION arrangement read repaired; signed arrangements + CONTROLLED event + exact outbox after commit | NOT_RUN_THIS_ACCEPTANCE_BRANCH; do not equate code/test evidence with real-page PASS |
| AC-14 | 当前关联取Master.latestControlled；审批快照固定当时版本；未受控初始来源明确展示（G56/G61实际对比）。 | G61真实当前关联显示目标A/2待生效；本版本审批关联快照保留A/1历史审批版本，选定ID不同且原快照不变。 |
| AC-15 | project/global browse exact selected ID binary permissions, readonly trace viewer entry and stage availability F03/R02 repairs | G55目录正文canvas、G56搜索/历史及只读浏览追溯实际通过；浏览入口无管理编辑权限扩张。 |
| AC-16 | project references exact official leader front+back, pinned chosen body, count distinct project | G56项目负责人实际引用至另一项目目录，引用数0→1且引用行标识正确；非负责人负向由正式权限测试证明。 |
| AC-17 | exact same target-project leader, second confirmation and only targeted reference removal/global color count | G56负责人二次确认取消引用，入口移除、计数1→0、源样式恢复；未把同账号证据外推不同负责人竞争。 |
| AC-18 | 模板直接权限保存；非空目录和使用中类型保护；正式项目文件夹维护入口已接入（G56）。 | G56真实创建、改名、删除任务目录；模板直接权限维护、非空删除及已使用类型保护有定向测试，未称全部排列真实办理。 |
| AC-19 | pendingDistribution formal dates, two-confirm distribution vs individual ack; no business rejection branch | G55 electronic distribution PASS, paper/individualreceipt not re-run |
| AC-20 | 独立作废两个审批节点，原文件历史不重写；正式部门义务在createInstance前冻结（G62），入口支持真正pending受控（G60/G61）；正常待办指派入口G63补齐。 | G63真实普通待办入口会签指派424→实际会签425→批准426，独立BPM00:43:40结束，只有MATRIX_REVIEW/APPROVAL，File/sharedOBSOLETE、原培训record和四签名保留。待生效入口已实际验证；完整批准发生在自然生效后，不冒待生效全链E2E。 |
| AC-21 | exact full original filename binary/case-sensitive name claim, extensions distinct and cross-project new chain conflict | NOT_RUN_THIS_ACCEPTANCE_BRANCH; do not equate code/test evidence with real-page PASS |
| AC-22 | stored revisionChangeType+baseline+choseniteration+reason actual intent, history own row | G60实际A/2详情显示局部变更、来源A/1及选定小版本；其它进位/换版的历史事实由策略/来源冻结测试证明。 |
| AC-23 | file business terminology controlled, shared currentActive label executing controlled version F01 repaired | G58真实工作稿/受控版本/当前执行版本分开显示，G60待生效状态实际显示，不根据B/1猜变更类型。 |
| AC-24 | name/number identity ownMaster exception plus 20year retention claims from actual obsolete time | G63真实作废NameClaim原完整name/number保留至2046-10-06；保留期NEW冲突拒绝由正式binary命名/NameClaim测试证明，未称实库另一次上传冲突通过。 |
| AC-25 | 预设生效日、受控时间、实际生效时间分别保存；上海分钟任务按期同事务切换native/shared与旧版作废，已自然跨日观察。 | G60提前受控A/2时原A/1继续执行；2026-10-06上海零点自然分钟job44263生效2条，A/2 ACTIVE、A/1 OBSOLETE，native/shared/Master指针同向，原日期与历史签名保留。关联最新/冻结快照亦已真实对比。 |
| AC-26 | 上海/提前7天/生效日排序；单分钟job5625按正式UI创建并独立恢复，实际日志及两版本自然激活已核验。 | 真实分钟job5625重启恢复；零点logger44263实际生效2条，下分钟44264实际0；正式上海7天排序配置和有限日期测试已证明，不以外层SUCCESS冒数据成功。 |
| AC-27 | retry formal target reservation sameA2, new independent file/BPM/source/snapshot/signature history and old active preserved | NOT_RUN_THIS_ACCEPTANCE_BRANCH; do not equate code/test evidence with real-page PASS |

## 业务未定项和验证边界

当前不擅自新增Z之后版本字母规则、引用是否跟随最新受控版本、关联整改任务的后续完成政策及保留期满处置权限。这些事项没有替代已确认的主流程；20年内占用、关联始终最新受控均已按确认实现。

真实测试账号和数据仅限本机测试环境；培训记录为明确测试样本。所有被验收动作均通过Playwright真实前端完成，DB仅用于事后只读核验；没有用直接SQL审批、API上传或更改系统时钟代替验收。

详细独立源码审查见后端任务 `g60-final-html-code-direction-review.md`；本表引用已有证据，不重复跑与新增变更无关的全量测试。

## 最后真实结果

- 独立文控账号仅具线下培训记录职责，正式待办1→0并进入文控审核，真实受控成功。
- A/2提前受控时A/1仍执行；自然到预设日期，分钟任务令A/2生效、A/1自动作废，native/shared与Master指针同步。
- 当前关联A/2，原审批快照A/1，生效前后均分别核对。
- 同一受控文件正常待办作废：指派、会签、批准三独立签名，批准后流程结束；名称编号占用保留20年，原历史保留。

待生效作废入口已通过真实页面；其批准完成时已自然到生效日，因此待生效完整效果以真实H2/shared事务组合证明，不宣称其完整真实页面E2E。其它未走的多账号竞争、各节点文件驳回、A/9进位和换版、全部名称排列及纸件签收细分，表格已分别标明测试/页面边界。当前未发现已确认业务方向的明确未修主线缺口。

正式task-closeout-cleanup工具在当前工具目录及本地技能中不可用。已核对永久资产和217份原始证据精确归档，未伪造cleanup PASS或completed；主任务保留ready_for_closeout。

## G64后继真实验证（2026-10-06）

原“未发现主线缺口”仅为G63时点。G64实际会签驳回发现原生流程key仍被旧CAS写死，现已有效RED/GREEN120并实际修复。真实A/3在会签、批准、文控审核分别驳回三次，第四次仍申请A/3、重新全会签并于02:34:38受控生效、02:43:58下发；原三个失败File/BPM和签名都保留，旧A/2到新版生效才作废。

当前名称/历史占用/正式引用三组74项具体断言与原XML已补齐；当前原生Control/SharedCore/正式消息DB16項验证补位旧宿主依赖。真实跨项目原作废文件名称被拒，页面已准确说明20年占用，不提示换编号或先作废释放。

持续目标仍active，最后G66正逐项审核充分性；这批真实通过不能替代缺少的属性或版本边界证明，待补齐后统一更新最终表。
