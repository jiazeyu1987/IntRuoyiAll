# E2E-22 详情与关联引用隔离

## Goal

独立验证E2E-22，作为24例之一交Root review；业务定义来自doc/tasks/20261015-dcc-six-thread-followup-review/e2e-test-cases.md。

## Current Status

blocked — 2026-10-20 原22-A/22-B/22-C在R6真实页面分项PASS：A2→A1→A2详情/属性/时间、原上传及后补固定正文/整改隔离、两版引用项目去重及全部4条引用正文。7项参数通过，runner16704 exit0，context/browser关闭且无OS残留。本轮无生产缺陷、无新增夹具或业务变更；受控阅读自然遥测保留。R5检出隔离PASS保留不重跑；既有样本和证据全部保留。工程待Root review，本轮禁止Git/证据清理，不标completed。详见acceptance-20261020-result.json与verification-report最新节。

## BDD

- R28原流程复验：Given 已完成审批使原APPROVAL_TASK正文资格结束；When Rel1原工作稿真实提交B/1并由S/L/C/A/D本人依次签名；Then 每次真实返回待办期间通过只读DOM观察器收集瞬态权限错误通知，并检查实际reading-session/stop请求及关闭失败控制台记录；旧轮次不重提。实际根因在阅读stop后端资格，早先task-action-readiness推测已撤回。观察器不读认证或改业务状态，签名密码期间关闭trace。

- 原Reader路径已放弃：Reader补类别UPLOAD后仍无入口，实际父组件还要求dcc:controlled-file:submit；不能把缺入口推断成禁止选择他人草稿。保持原编制人U及其已保存工作稿4257，不继续给Reader补一整套编制职责。Given U已有类别UPLOAD及submit、但本Source项目为AUTHOR，When 原项目OWNER只将本例P1内U提升为OWNER并回读，Then U获得现行REPLACEMENT资格而原Reader OWNER及其余项目/账号不变；撤回本轮临时Reader类别UPLOAD。授权来自持续目标下本例自有账号/权限准备，不改产品授权或原文件编制人。

- Given 不同版本有不同属性、时间、关联、引用及检出记录；同一项目有多个引用
- When 选择每个版本查看详情分区；打开关联的固定版本；查看被引用项目明细
- Then 名称/编号/状态/属性/备注/编制人/项目/文件夹/类型/检出人及时间与所选版本一致；生效方式、作者所选日期、实际生效及受控时间分开显示；关联/引用不自动追新版，按项目去重计数且明细准确；提交时、后补及整改记录可追溯，不混版。

### R22-01 前端最小行为RED（Root本轮唯一授权）

- Given 真实DetailSignoffAssignment加载Main当前任务、原上传Rel1 A/1和同项目已生效Rel2 A/1，SignoffAssignmentPanel仍使用本人签名字段；When 从既有项目文件选择器选择Rel2固定id，再点击真实“签名确认指派”handler；Then 正式assign-signoff请求携带supplementalRelatedControlledFileIds=[Rel2固定id]，不替换原Rel1，file/BPM/task/签名人与密码/意见不变。
- Given 同样的upload或revision任务；When 未选、取消未确认选择、或清空已选后签名；Then 请求名单分别为空、保持取消前名单、或为空，原关联不变；revision原整改安排仍由真实validate进入相同签名请求。
- Given 候选选择器属于Main的精确项目；When 查询与尝试选择其他项目文件；Then 仍使用relations目的及原项目身份，跨项目成员不能进入待签名名单或写请求。
- 授权仅新增IntRuoyiFronted/tests/unit/dcc-signoff-supplemental-relations.test.cjs及本例BDD/证据。Root独占测试执行；本线程不运行RED、不改任何生产Vue/TS/后端/权限/API。生产须Root取得实际RED后另行明确放行。

### R22-01 GREEN阶段最新授权

- Root已执行Node RED：e2e22-supplement-entry-red-process.json退出1，6项中1通过/5失败；未选的原upload/revision/obsolete签名合同通过，其他在真实parent缺选择控件失败，排除编译/import前置错误。
- Root现唯一放行生产文件DetailSignoffAssignment.vue；复用既有DccFileSelector及relation-arrangements slot，按精确项目准备候选，只有明确确认的后补名单进入原saveAssignment签名事务；取消保留已确认名单，确认空集合只清后补，原上传关联和revision整改不变。
- 保留当前任务/租户/项目身份验证、上下文切换清理和服务端二次校验；不修改SignoffAssignmentPanel.vue、workflow-actions.ts、后端、DTO、权限或关系API。Root独占Node GREEN/回归，本线程完成冻结后交Root执行；Main4249原L待办暂不操作。

## Milestones

1. [x] 用例参数拆分、精确页面入口及角色/数据要求；已形成 prepared.json。
2. [x] 可复跑Playwright脚本、语法与结构检查。
3. [ ] Root放行后真实前端执行及任务自有数据收尾。
4. [ ] Root review脚本/trace/可见页面断言，逐分支给出实际结论。

## Expected Verification

- 只操作真实页面，不使用API/DB写入、mock或默认成功；每个参数化分支单独记录。
- 运行命令、实际退出码、环境/账号标签、版本/申请、页面断言、trace/截图、清理残留齐全。
- 静态检查与准备状态不得记E2E PASS；缺前置具体BLOCKED，业务失败具体FAIL。

## 设计约束检查

- 默认仅本目录测试资产；Root在实际RED后唯一授权DetailSignoffAssignment.vue单文件修复及原测试纯渲染注册。其余生产源码/共享.env不改；不重启服务、不跑共享Maven/构建、不切分支、不操作远端、不提交推送。
- 当前分支int_qms，端口8061/48061；浏览器启动前读取Root dispatch.json中本例gate和环境身份。
- 账号与凭据仅用已授权来源，不记录密码/token/cookie、不猜密码；缺独立角色就报告，不用admin替代。
- 自有数据前缀DCC241016C22，不改其它用例对象或公共矩阵；更广泛数据准备先交Root排期。
- 本目录由本例线程维护，Root独占协调目录。准备完成结束当前turn，不循环等待。

## Cleanup Keep

- doc/tasks/20261016-dcc-e2e-22/fixtures/rel1-checkin

- doc/tasks/20261016-dcc-e2e-22/task.md
- doc/tasks/20261016-dcc-e2e-22/execution-log.md
- doc/tasks/20261016-dcc-e2e-22/verification-report.md
- doc/tasks/20261016-dcc-e2e-22/prepared.json
- doc/tasks/20261016-dcc-e2e-22/scripts/e2e-22-detail-reference-isolation.cjs
- doc/tasks/20261016-dcc-e2e-22/scripts/sanitize-trace.py
- doc/tasks/20261016-dcc-e2e-22/scripts/fixture-ui.cjs
- doc/tasks/20261016-dcc-e2e-22/fixture-manifest.json
- doc/tasks/20261016-dcc-e2e-22/e2e-result.json
- doc/tasks/20261016-dcc-e2e-22/runtime-pause.json
- doc/tasks/20261016-dcc-e2e-22/runner-checkpoint.json
- doc/tasks/20261016-dcc-e2e-22/pause-business-result.json



- doc/tasks/20261016-dcc-e2e-22/scripts/business-steps.cjs
- doc/tasks/20261016-dcc-e2e-22/scripts/setup-actors.cjs
- doc/tasks/20261016-dcc-e2e-22/defect-supplement-entry.md
- doc/tasks/20261016-dcc-e2e-22/supplement-source-fingerprints.json



- doc/tasks/20261016-dcc-e2e-22/r22-01-red-handoff.json
- doc/tasks/20261016-dcc-e2e-22/r22-01-green-freeze.json
- doc/tasks/20261016-dcc-e2e-22/r22-01-test-only-source-check.json


## 2026-10-19 检出投影定向修复BDD与范围

- Given Main同一Master含旧版4249/A1与当前版4260/A2，Master活动锁baseIterationId仅指A2；When读取A1、A2详情及各自版本历史；Then Master互斥checkedOut保持活动锁事实，只有精确A2显示检出人/姓名/时间/原因；A1上述版本事实均为空，页面显示未检出，不能借用A2事实。
- Given 返回对象曾带旧检出字段；When读取无锁或锁属于其他版本的投影；Then清空旧actor/name/time/reason，保留逻辑锁布尔语义，不因复用对象残留串版。
- Given另一账号尝试检出同Master其他合法版本；When活动锁仍存在；Then既有selectActiveByMasterId写锁拒绝重复检出；本次不改变Master互斥、检入/撤销所有权及版本分配。
- 生产独占窄点：DccControlledFileQueryServiceImpl的两个fillCheckoutProjection；直接测试在DccControlledFileQueryServiceTest。先测试diff交Root执行RED，Root确认前不改生产；后端Maven/构建/启动由Root串行。
- UI仅复验检出隔离：新独立U context，确认原锁已撤销，在原4260/A2重新检出一次，真实详情分别断言A2=U/本次时间、4249/A1=未检出/未检出，再回A2断言仍保留本次锁，最后真实UI撤销并回读。已通过审批/属性/关联/引用/整改参数不再整体运行。
- 授权来源：协调目录recovery-dispatch-20261019.md及本轮Root派发。无Git提交推送、API/DB业务动作、新线程、服务操作或全量作业。

- doc/tasks/20261016-dcc-e2e-22/checkout-version-isolation.md
- doc/tasks/20261016-dcc-e2e-22/checkout-isolation-root-baseline.json
- doc/tasks/20261016-dcc-e2e-22/checkout-isolation-red-test.diff
- doc/tasks/20261016-dcc-e2e-22/checkout-isolation-red-handoff.json


- doc/tasks/20261016-dcc-e2e-22/checkout-isolation-green.diff
- doc/tasks/20261016-dcc-e2e-22/checkout-isolation-green-freeze.json


## repair-20261019-r2：冻结签名展示BDD

- Given 正式已生效/历史详情没有加载实时任务列表，但该版本签名摘要含固定taskId、processInstanceId、versionNo、meaningCode及actionType；When显示该版本签核追溯职责；Then只按与所选BPM/版本匹配的冻结签名含义展示真实职责；缺值、跨轮次/版本或action矛盾保持“签名职责未记录”，不查当前组织或补实时任务权限。
- Given 关联阶段header只冻结departmentId，现有同轮次nodeRecords.assignmentTarget保留原task/signature/actor/stage及obligationDepartmentName；When显示原义务部门；Then只在精确file/BPM/task/signature/actor/stage与ASSIGN记录唯一匹配时使用该冻结原义务部门名；不使用签名人部门或现组织。确实缺该快照时仍“未记录”。
- 最小前端范围：native-approval-progress.ts的独立冻结职责显示函数、index.vue签核追溯调用、DetailRelationStageEvidence.vue显示以及其两处已有证据调用方。后端Query除已冻结检出overload不改；没有新增查询或后端快照字段。先轻量Node实际RED，再GREEN与相邻回归；真实页面仅原A1/A2签核/部门显示定向复验。

- 检出原因UI证据BDD：Given后端精确版本已提供checkedOutReason；When查看A2及A1只读记录；ThenA2显示本次真实检出原因，A1未检出，不显示旧残留原因。原只读面板此前没有原因字段，轻量真实模板RED已证实缺入口，补一个既有字段描述项以满足本轮actor/time/reason显式UI断言，不新接口。


- doc/tasks/20261016-dcc-e2e-22/repair-20261019-r2-handoff.json
- doc/tasks/20261016-dcc-e2e-22/frozen-signature-presentation-red.json
- doc/tasks/20261016-dcc-e2e-22/frozen-signature-presentation-green.json
- doc/tasks/20261016-dcc-e2e-22/frozen-signature-presentation-ui.json
- doc/tasks/20261016-dcc-e2e-22/frozen-signature-presentation-files.json
- doc/tasks/20261016-dcc-e2e-22/checkout-reason-ui-red.json


- doc/tasks/20261016-dcc-e2e-22/repair-20261019-r3-handoff.json


- doc/tasks/20261016-dcc-e2e-22/repair-20261019-r4-handoff.json

- doc/tasks/20261016-dcc-e2e-22/resume-20261020-preflight.md

- doc/tasks/20261016-dcc-e2e-22/resume-20261020-readonly-result.json

- doc/tasks/20261016-dcc-e2e-22/acceptance-20261020-plan.md

- doc/tasks/20261016-dcc-e2e-22/acceptance-20261020-result.json

- doc/tasks/20261016-dcc-e2e-22/scripts/acceptance-20261020.cjs

## 2026-10-20 R2 第四轮独立重跑

本轮 runId=rerun-20261020-r2-round4；22-A/B/C 在冻结 R2 运行态全部真实页面 PASS，包含新检出/撤销隔离，不计入旧 PASS。独立交接：e2e-artifacts/rerun-20261020-r2-round4/handoff.json；报告：同目录 verification-report.md。Reader/U runner26172/40412均exit0，自有Chrome无残留；工程blocked，Root交接后HOLD，不改写上轮历史结论。
