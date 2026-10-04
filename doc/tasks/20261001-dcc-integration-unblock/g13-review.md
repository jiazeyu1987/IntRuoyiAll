# G13 批准负责人初步实现与独立 Review

2026-10-03。主目录 int_qms，整合分支 codex/20261001-dcc-integration，共同HEAD a801dc8b91579241e221d129ab34343997673f40。沿用已有任务，用户已明确负责人由批准人在批准弹框中选择，不新增流程节点。前轮引用明细与后台条件有实际进度，不累计全目标阻塞。

## 当前实现范围

Root在主任务登记临时唯一写入和BDD后，新增批准负责人服务并接公开Workflow.approveTask。只在Native UPLOAD/REVISION的MATRIX_APPROVAL通过时必选；用正式AdminUserApi验证启用同租户账号，不默认申请人或额外授予OWNER权限。其它节点和独立作废携带此字段拒绝。

负责人ID/用户名/昵称作为规范JSON事实加入既有批准签名reason，进入实际签名证据/HMAC；另保存版本上的ID/姓名快照/签名ID/批准task/BPM/时间。绑定重新校验签名文件、实际BPM、版本、actor、APPROVE/批准meaning、有效evidence hash、reason和时间；MANDATORY事务只在公开批准事务内写。超500字signed reason明确拒绝，避免正式signature.comment容量截断，不扩大索引列或兼容旧库。

新正式候选从所选正文复制时清所有上轮owner字段，详情/版本历史投影取冻结字段，旧行NULL如实未记录。独立前向迁移只nullable加七列，information_schema守护、不改历史行；H2fixture同步。真实MySQL未执行。

前端正式批准弹框挂ApprovalFileOwnerPicker，正式simple-list只取启用账号，精确字符串Long且不选默认值。可选字段仅适用上传/升版实际MATRIX_APPROVAL且本版native BPM匹配；驳回/会签/文控/作废不显示不发。提交前核对意见、文件版本及负责人账号；cancel零写，文件/task/BPM/route/字段变更拒绝提交；历史显示本版冻结姓名/签名。该初步实现仍需独立父handler/路由组合Review，不能凭模板出现判完整。

## 实际验证和失败边界

- 独立backend行为RED3：原owner service不存在；后续服务GREEN3，原Workflow164中一例新依赖fixture未接，未计业务RED。修正公开Native approval真实fixture为正式所选账号和签名，不靠mock owner成功。
- 后端新增公开Workflow三负向（缺选择/外租户/签名拒绝），H2三项实际快照commit、晚外层失败回滚signature+owner、账号改名历史不变。隔离signed row在H2测试里是明确签名端口fixture，不冒称完整真实签名内核或Flowable审批。
- 一次编译失败缺verifyNoInteractions静态import；H2初始化漏submitter_id导致3fixture errors均修正并保留日志，不作为有效业务RED。原regression命令有不存在的selectedIteration类名，421实际4类不能称五类；修正正式类后最终g13-owner-backend-final.log **465/6类、0failure/error/skip，BUILD SUCCESS**，main-final-compile通过，session62865 exit0。
- 前端有效RED3（缺负责人校验/载荷/模板），GREEN6包含实际picker启用Long、权限错误、卸载晚响应。当前14文件组合 **160 PASS/0fail/skip**，四生产文件lint0/0，正式types/build session14185 exit0。初次types失败将validation接口直接赋Record，已仅映射实际owner错误字段；未放宽tsconfig。
- 迁移包g13-migration-package.json扩至 **18根/44依赖，policy passed**，prepared_not_executed，databaseConnected=false。

## 独立子 Agent 正式 Review

backend_closure与detail_closure已通过正式followup续派，独立只读review后端/FE及自己的必要测试，Root停止对应生产写入。他们不运行Maven/type/build、服务/DB/Git，不以已有completed状态代表当前review终态。

后端Reviewer已报两个需要核对的实际边界：同file/BPM退回批准后重新选择是否被“首次owner不可覆盖”错误阻断；统一审批中心adapter.review没有owner输入，需核对其正式页面是否仅“进入模块办理”而非支持quick approve。这些当前是待复现风险，不称已修。其它记录仍须审查（历史签名真正包含选择事实、长意见、同号重提、多批准人、权限及Long）。收到具体测试/代码发现后Root重新分派修复，不先关闭W08。

## 尚未完成的目标

真实测试环境MySQL23306/Redis26379与整合8067/48067无监听；共享依赖恢复、提醒业务配置和迁移执行尚未获具体决定。Flowable/Quartz及其它后台启动副作用仍需实际运行方案核验。未启动共享依赖、服务、实库、E2E、提交或合入；无发布。负责人初步离线实现不是整套完成，goal active。
