# G34 真实页面验收账号前置审查

状态：SOURCE_REVIEWED_RUNTIME_ACTORS_NOT_PROVEN。2026-10-04。此报告只读正式源码及已有安全收据；未连接数据库、未启动服务、未运行浏览器、未执行 Maven 或 Git。仅新增本报告。主任务、迁移、审批与 E2E 状态均由 Root 管理。

## 范围与依据

沿用整合目录现有 backend task，未重建任务。工作区 `.git` 指向主仓库 worktree，其 HEAD 文件为 `refs/heads/codex/20261001-dcc-integration`；本委派禁止 Git，故未自行运行 Git。已读根 AGENTS、`docs/task-closeout-rules.md`、`docs/login-access.md`、`docs/e2e-rules.md`、`docs/dcc-parallel-delivery/shared-contract.md` 及最终需求 `docs/product/dcc-final-requirements.html` v1.5。登录访问文档里的旧 8081/48081 不是本任务运行入口；本任务只允许 Root 核对登记槽位后使用整合端口。

文档验证 Given 正式 Controller/service/DO/schema 与业务合同，When 提取各操作的账号资格和只读核验列，Then 每项区分菜单权限、正式业务身份、实际 BPM 任务、签名资格及尚未证明的运行事实；SQL 不输出凭据、联系方式、签名图片正文或存储配置。

当前 Root `g34-current-runtime-receipt.json` 证明：源测试库身份已只读核对，Java 进程 0，监听列表空；它没有账号/角色/项目的采样结果。因此没有任何非 admin 测试账号、文控账号、部门负责人或项目负责人被本报告认定为实际可用。AGENTS 给定的 admin 是默认测试登录身份，不自动满足以下所有职责。

最新方向先完成主流程架构。本报告现有资格矩阵只作为源码依据，不新增负向账号、异常测试矩阵或账号配置任务。主线最小账号集合按实际配置收敛：申请人、冻结审核人、现有项目批准账号、矩阵部门 leader/实际会签人/批准人、正式文控、目标项目 leader；同一个账号可兼任时也必须各自满足正式事实并分别完成节点，不能跳步。

对 admin 的主线结论：源码明确允许它承担项目/产品批准，因为该 service 检查 username；它只有在真实 reviewer 配置指向它时才能审核项目。上传、检出、升版另需正式项目与分类授权；文件指派/批准另需实际部门 leader/路线候选/任务身份与签名条件。培训和下发还需精确 `doc_control` 角色，超管 permission 不能替代此角色。引用/取消只有目标 project_leader_user_id 确实等于 admin 时允许。除项目批准的 username 条件外，本报告尚无实际库证据证明 admin 符合其它职责。

## 各真实操作的正式资格

| 操作/待验证角色 | 精确入口权限 | 业务身份与必要事实 | 正式代码锚点 |
|---|---|---|---|
| 项目/产品申请人 | `dcc:project-code:create`；页面读取通常还需 `dcc:project-code:query` | 本次选定项目负责人必须为启用正式账号；提交前必须有当前租户的有效 reviewer 配置。申请人不自动成为审核人 | `DccProjectProductCreateController#create`；`DccProjectProductCreateServiceImpl#create`；`DccProjectReviewerConfigurationService#requireConfigured` |
| 项目/产品审核人 | `dcc:project-code:update`；待办读取 `dcc:project-code:query` | 当前 actor 必须等于本次申请冻结的 `configuredReviewerUserId`，且账号仍为当前租户启用账号；改当前配置不替代历史身份 | `DccProjectProductCreateController#reviewApprove`；`DccProjectProductCreateServiceImpl#review`；`DccProjectReviewerConfigurationService#assertFrozenReviewer` |
| 项目/产品批准人 | `dcc:project-code:update` | 当前正式实现另要求账号 `username == admin`。这是代码现状，不能据此把所有文件审批交给 admin；项目批准独立规则的业务待定部分由 Root 保留 | `DccProjectProductCreateServiceImpl#approve`、`#requireAdmin` |
| 审核人配置维护者 | `dcc:project-code:update` **且精确角色 `doc_control`** | reviewer 目标须同租户、启用、身份完整；配置维护本身不是审核或批准 | `DccProjectProductCreateController` reviewer-config PUT；`DccProjectReviewerConfigurationService#save` |
| 上传/INITIAL 申请人 | `dcc:controlled-file:submit`；浏览入口 `dcc:controlled-file:query` | 项目 `EDIT` 或 `OWNER` 规则、分类 `UPLOAD` 规则；启用项目、真实项目文件夹、类型/分类映射、目录绑定及项目属性。不能借文件菜单权限绕过项目或分类范围 | `DccControlledFileController` /submit 与 /working mapping；`DccControlledFileWorkflowServiceImpl#prepareSubmitContext`、`#validateSourceUploadContext` |
| 部门负责人指派会签 | `dcc:controlled-file:review` | 必须为该部门本次义务冻结的 `leaderUserId`，且实际 MATRIX_REVIEW 任务允许此 actor；负责人来自 `system_dept.leader_user_id`，不是部门内任意 OWNER。真实岗位、签名授权、可验证签名图片及本人签名凭据均需满足；每部门义务独立 | `DccWorkflowLifecycleController#assign`；`DccWorkflowSignoffAssignmentService#assignmentContext`、`#assign`；`DccControlledFileApprovalRouteAssigneeResolver#resolveDepartmentLeaders` |
| 实际会签人 | `dcc:controlled-file:review` | 负责人本人或同部门启用账号；非空系统岗位；指派时正式 readiness 通过。本次义务必须已经保存指派签名、assignedTime、精确 taskId，且 actor 等于冻结的 assigneeUserId；指派签名不能代替实际会签签名 | `DccWorkflowSignoffAssignmentService#assign`、`#requireAssigned`；`DccControlledFileWorkflowServiceImpl#validateTaskAction`；独立作废走 `#reviewObsolete` |
| 文件矩阵批准人 | `dcc:controlled-file:approve` | 由**本分类、本动作**审批路线解析并冻结的 USER / DEPT leader / POSITION 任命账号；实际 MATRIX_APPROVAL task/BPM 与文件一致。正式岗位、签名授权和有效图片均需满足。源码未规定名为“批准人”的固定角色，不得造角色或把 admin 当默认候选 | `DccControlledFileApprovalRouteAssigneeResolver#resolveApprovers`、`#resolveAssignmentUsers`；`DccControlledFileRouteReadinessService`；`DccControlledFileWorkflowServiceImpl#validateTaskAction` |
| 批准弹框选择的文件负责人 | 由上述真实批准人办理，不新增节点 | 被选人必须为当前租户启用正式账号、真实 ID 与完整用户名/昵称；选择与同文件/版本/流程/批准任务的正式签名绑定。不因此自动授予项目、正文或菜单权限。作废批准不选择文件负责人 | `DccApprovalFileOwnerSelectionService#prepare`、`#signedReason`、`#bind` |
| 线下培训文件上传文控 | `dcc:controlled-file:approve` **且 `doc_control`**；分类 `APPROVE` | 文件本次需要培训、PENDING_APPLICANT_TRAINING_RECORD、尚无培训记录/受控正文；票据 actor/category/purpose/TRAINING_RECORD/session 必须匹配本次真实文件版本与 BPM 轮次。文控上传即完成，不要求逐人在线培训 | `DccControlledFileWorkflowController#uploadTrainingRecord`；`DccControlledFileWorkflowServiceImpl#validateTrainingRecordUpload`、`#uploadTrainingRecord`、`#requireNativeTrainingContext` |
| 文控审核办理人 | `dcc:controlled-file:review`（DOC_CONTROL_REVIEW） | 该节点路线冻结的候选及实际 task 办理人，正式岗位与签名条件；源码这一节点按正式路线/权限验证，不能自行新增某个文控角色名来替代路线。若另有 DOC_CONTROL_APPROVAL 旧流程节点，其正式权限为 `approve`，不能与新流程混用 | `DccControlledFileWorkflowServiceImpl#validateTaskAction`、`#validateStagePermission`、`#prepareDocControlArtifacts`；`DccControlledFileRouteReadinessService#requiredPermission` |
| 文控下发、按日期工作台提醒 | `dcc:controlled-file:distribute` **且 `doc_control`**；分类 `DISTRIBUTE` | 真实受控文件 ACTIVE 或 CONTROLLED_PENDING_EFFECTIVE，尚未下发；部门和接收人按真实启用目录校验。受控/下发日期不替代预设生效日期。非文控不能靠管理员权限代替精确角色检查 | `DccWorkflowLifecycleController#pending`、`#distribute`；`DccWorkflowDistributionService#recipientOptions`、`#distribute` |
| 关联编辑者 | Controller 为 `isAuthenticated()`；公共浏览需 `query`，详情 relation-permissions 需 query/review/approve 之一 | 源文件名称可见、任务/分配硬范围、源项目 EDIT/OWNER、源分类 UPLOAD；目标名称可见及当前受控候选。名称权限不授予正文权限 | `DccFileRelationsController#replace`；`DccControlledFileQueryServiceImpl#assertRelationEditable`、`#assertCanMutateControlledFile` |
| 引用/取消引用者 | Controller 为 `isAuthenticated()`；公共项目/选择器需其正式读取菜单权限 | **唯一目标项目负责人账号** `dcc_project_code.project_leader_user_id == actor`，启用项目/账号、同租户、真实目标 folder 属于该 project。新引用还需源文件名称可见。OWNER/EDIT 规则、另一项目负责人或超级管理员身份不替代这一事实 | `DccProjectReferenceController#create`、`#cancel`；`DccProjectReferenceService#create`、`#cancel`；`DccProjectReferenceAuthorityImpl`；`DccProjectLeaderService#assertProjectLeader` |
| 检出 actor A 与用于互斥验证的 actor B | `dcc:controlled-file:query`；真实检入上传票据另涉及 `submit` | 两人须都先具备检出资格，才有意义地验证锁互斥。名称可见、精确 Master/File、项目 EDIT/OWNER 和分类 UPLOAD；非原 requester 对受控基线检出另要求项目 OWNER，且只能最新受控基线。actor B 不能因权限先失败冒充“检出锁阻止”。工作小版本仅其 requester 按正式规则检出；检入/撤销须真实持锁 actor | `DccControlledFileController` checkout/checkin/cancel；`DccControlledFileQueryServiceImpl#requireCheckoutAccessibleControlledFile`、`#canCheckoutLockedVersion`、`#doCheckoutControlledFile`、`#assertCanMutateControlledFile` |
| 局部/换版申请人 | 正式候选提交 `dcc:controlled-file:submit` | 项目 EDIT/OWNER 与分类 UPLOAD；换版另需项目 OWNER；合法较早工作小版本正文来自实际所选 File。OWNER 仍不是引用的唯一项目负责人合同 | `DccControlledFileRevisionServiceImpl`；`DccControlledFileWorkflowServiceImpl#requireWorkingApplication`、`#validateWorkingIterationSubmission` |
| 作废申请人及签名节点 | 申请入口 `dcc:controlled-file:query`；分类 `OBSOLETE`；指派/会签 review，批准 approve | 所选受控文件与本次独立 OBSOLETE BPM 精确关联；审批路线按 OBSOLETE 动作解析，不能借上传矩阵；批准后结束。作废后20年占用事实是终态核验，不能用账号菜单代替 | `DccControlledFileController#obsolete`；`DccControlledFileObsoleteServiceImpl#obsoleteControlledFile`；`DccWorkflowSignoffAssignmentService#requireObsoleteReview` |

角色和 permission 不完全等价：`PermissionServiceImpl#hasAnyPermissions` 还使用动态权益、永久角色、超级管理员和临时角色；`#hasAnyRoles` 则检验启用角色的精确 code（没有超级管理员旁路）。因此“静态 role_menu 无某 permission”不足以断言实际拒绝；“超管有 permission”也不足以推出有 `doc_control` 或项目负责人资格。正式授权投影必须在目标账号真实页面 fresh 登录后只读核对，不能调用登录 API 执行业务或复用他人 token。

## BPM 与电子签名前置

实际待办中心入口 `BpmTaskController` todo/list-by-process-instance 需 `bpm:task:query`；DCC 详情读取实际审批资料还可触发 `bpm:process-instance:query`。真正签名业务动作走 DCC 公共批准/驳回入口及正式 service，不能用 BPM 通用 approve 代替负责人选择或 DCC 签名。若验收的是通用 BPM 操作，其 Controller 另需 `bpm:task:update`，不要无依据为所有 DCC actor 加这项权限。

`BpmTaskServiceImpl#validateTask` 检查任务存在及非空 assignee 与 actor；DCC 在其上校验 tenant、file、stage、process、冻结候选或部门义务。仅取得一个 taskId、在待办里看到文件名称或候选名单含 actor，均不是完整办理资格证明。

`DccControlledFileRouteReadinessService` 在创建路线时核对：系统岗位非空、review/approve 阶段权限、电子签名授权和有效签名图片。签名授权 `DccElectronicSignatureAuthorizationServiceImpl` 要求启用且没有当前有效锁；ENABLED 或有效的过期锁语义以正式方法为准。图片 metadata 的 active 状态/行数不能证明图片有效，`DccElectronicSignatureImageServiceImpl#requireActiveSnapshot` 会读取并验证真实存储图片；应由真实路线预检/签名界面确认，不把只读 SQL 行存在写成 PASS。

## 最小安全只读 SQL 建议（Root 执行，本 Agent 未执行）

以下查询仅提供现有 schema 支持的字段。先核对目标库 UUID/tenant，以及实际 `information_schema.COLUMNS`，缺列则停止，不用相似字段兼容。静态依据是 `sql/mysql/ruoyi-vue-pro.sql` **完整 CREATE TABLE 边界**、DCC 正式 DO、`20260930_dcc_b_project_attributes_folders.sql`、`20261003_dcc_project_reviewer_configuration.sql`。已有 G21 BPM SQL/G25 collection 并不采集这套账号和角色授权，不能当作实际账号凭据。

先只读核 schema，并核对 SESSION 时区；下面时间规则应在 Root 已验证的业务时区连接下读取。

```sql
SELECT DATABASE() AS database_name, @@server_uuid AS server_uuid,
       @@session.time_zone AS session_time_zone;
SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA=DATABASE()
  AND TABLE_NAME IN ('system_users','system_role','system_user_role',
      'system_role_menu','system_menu','system_dept','dcc_project_code',
      'dcc_project_access_rule','dcc_project_reviewer_config',
      'dcc_file_category_permission_rule','dcc_category_approval_route',
      'dcc_category_approval_route_node','dcc_position_assignment',
      'dcc_electronic_signature_authorization','dcc_electronic_signature_image',
      'dcc_controlled_file_task_assignee_snapshot')
  AND COLUMN_NAME IN ('id','user_id','username','status','dept_id','post_ids',
      'tenant_id','deleted','role_id','code','name','menu_id','permission',
      'parent_id','path','component','leader_user_id','project_leader_user_id',
      'dcc_project_code_id','subject_type','subject_id','access_level','active',
      'valid_from','expire_time','reviewer_user_id','version_no','category_id',
      'action_type','scope_type','route_id','stage_code','stage_order',
      'candidate_source_type','candidate_source_id','candidate_source_ids',
      'approve_method','require_all_approvals','effective_time','position_id',
      'assignment_type','system_post_id','electronic_signature_enabled',
      'authorization_state','locked_until','image_status','controlled_file_id',
      'process_instance_id','bpm_task_id','obligation_id','department_id',
      'assignee_user_id','assignment_signature_id','assigned_time')
ORDER BY TABLE_NAME, ORDINAL_POSITION;
```

账号与精确角色的最小候选清单（不读取用户密码、手机号、邮箱或其它个人字段）：

```sql
SELECT CAST(u.id AS CHAR) AS user_id, u.username, u.status,
       CAST(u.dept_id AS CHAR) AS dept_id,
       CASE WHEN u.post_ids IS NULL THEN 0
            WHEN JSON_VALID(u.post_ids) THEN JSON_LENGTH(u.post_ids)
            ELSE NULL END AS configured_post_count
FROM system_users u
WHERE u.tenant_id=1 AND u.deleted=b'0'
ORDER BY u.id;

SELECT CAST(u.id AS CHAR) AS user_id, u.username,
       CAST(r.id AS CHAR) AS role_id, r.code AS role_code, r.status AS role_status
FROM system_users u
JOIN system_user_role ur ON ur.user_id=u.id AND ur.tenant_id=u.tenant_id
JOIN system_role r ON r.id=ur.role_id AND r.tenant_id=u.tenant_id
WHERE u.tenant_id=1 AND u.deleted=b'0' AND ur.deleted=b'0' AND r.deleted=b'0'
ORDER BY u.id,r.id;
```

按已观察的真实 actor IDs 缩小范围后，静态菜单许可清单可用下面准备语句的 `:actorId`，不可填猜测账号 ID。`system_menu` 是全局表，没有 tenant_id；其余 join 均保留同租户约束。此结果不是动态权益、临时角色、超管或 fresh 登录授权投影的替代。

```sql
SELECT CAST(u.id AS CHAR) AS user_id, u.username,
       CAST(r.id AS CHAR) AS role_id, r.code AS role_code,
       CAST(m.id AS CHAR) AS menu_id, m.permission
FROM system_users u
JOIN system_user_role ur ON ur.user_id=u.id AND ur.tenant_id=u.tenant_id
JOIN system_role r ON r.id=ur.role_id AND r.tenant_id=u.tenant_id
JOIN system_role_menu rm ON rm.role_id=r.id AND rm.tenant_id=u.tenant_id
JOIN system_menu m ON m.id=rm.menu_id
WHERE u.tenant_id=1 AND u.id=:actorId AND u.status=0 AND u.deleted=b'0'
  AND ur.deleted=b'0' AND r.status=0 AND r.deleted=b'0'
  AND rm.deleted=b'0' AND m.status=0 AND m.deleted=b'0'
  AND (m.permission LIKE 'dcc:%' OR m.permission IN
       ('bpm:task:query','bpm:process-instance:query','bpm:task:update'))
ORDER BY r.id,m.id;
```

实际配置和项目事实（`:projectId`、`:categoryId`、`:actorId` 均为正式页面/安全只读证据已取得的精确参数；这些是准备语句建议，不是可以忽略参数的 CLI 命令）：

```sql
SELECT CAST(c.reviewer_user_id AS CHAR) AS reviewer_user_id,
       u.username,u.status,c.version_no
FROM dcc_project_reviewer_config c
LEFT JOIN system_users u ON u.id=c.reviewer_user_id AND u.tenant_id=c.tenant_id
  AND u.deleted=b'0'
WHERE c.tenant_id=1;

SELECT CAST(p.id AS CHAR) AS project_id,p.status,
       CAST(p.project_leader_user_id AS CHAR) AS leader_user_id,
       u.username,u.status AS leader_status
FROM dcc_project_code p
LEFT JOIN system_users u ON u.id=p.project_leader_user_id AND u.tenant_id=p.tenant_id
  AND u.deleted=b'0'
WHERE p.tenant_id=1 AND p.deleted=b'0' AND p.id=:projectId;

SELECT CAST(id AS CHAR) AS rule_id,subject_type,
       CAST(subject_id AS CHAR) AS subject_id,access_level,active,
       CASE WHEN valid_from IS NULL OR valid_from<=NOW() THEN 1 ELSE 0 END AS started,
       CASE WHEN expire_time IS NULL OR expire_time>NOW() THEN 1 ELSE 0 END AS not_expired
FROM dcc_project_access_rule
WHERE tenant_id=1 AND deleted=b'0' AND dcc_project_code_id=:projectId
ORDER BY id;

SELECT CAST(id AS CHAR) AS rule_id,action_type,subject_type,
       CAST(subject_id AS CHAR) AS subject_id,scope_type,active
FROM dcc_file_category_permission_rule
WHERE tenant_id=1 AND deleted=b'0' AND category_id=:categoryId
ORDER BY id;

SELECT CAST(r.id AS CHAR) AS route_id,r.action_type,r.version_no,r.active,
       CAST(n.id AS CHAR) AS node_id,n.stage_code,n.stage_order,
       n.candidate_source_type,CAST(n.candidate_source_id AS CHAR) AS candidate_source_id,
       n.candidate_source_ids,n.approve_method,n.require_all_approvals
FROM dcc_category_approval_route r
JOIN dcc_category_approval_route_node n ON n.route_id=r.id AND n.tenant_id=r.tenant_id
WHERE r.tenant_id=1 AND r.category_id=:categoryId AND r.deleted=b'0'
  AND n.deleted=b'0' AND r.active=b'1'
ORDER BY r.action_type,r.version_no DESC,n.stage_order,n.id;

SELECT CAST(d.id AS CHAR) AS department_id,CAST(d.leader_user_id AS CHAR) AS leader_user_id,
       u.username,u.status AS leader_status,d.status AS department_status
FROM system_dept d
LEFT JOIN system_users u ON u.id=d.leader_user_id AND u.tenant_id=d.tenant_id
  AND u.deleted=b'0'
WHERE d.tenant_id=1 AND d.deleted=b'0'
ORDER BY d.id;

SELECT CAST(user_id AS CHAR) AS user_id,electronic_signature_enabled,
       authorization_state,
       CASE WHEN locked_until IS NOT NULL AND locked_until>NOW() THEN 1 ELSE 0 END AS locked_now
FROM dcc_electronic_signature_authorization
WHERE tenant_id=1 AND deleted=b'0' AND user_id=:actorId;
SELECT CAST(user_id AS CHAR) AS user_id,COUNT(*) AS active_image_count
FROM dcc_electronic_signature_image
WHERE tenant_id=1 AND deleted=b'0' AND user_id=:actorId AND active=b'1'
GROUP BY user_id;
```

分类 scope_type、签名 tenant_id、项目 access valid_from/expire_time 属于扩展 schema；如实际 schema 没有相应列，停止该查询并交 Root 核对已应用迁移。表 DO 的 BaseDO 不表示数据库无 tenant 列，不能去掉 tenant 条件。POSITION 候选还要按实际 `dcc_position_assignment` USER/POST 任命，以及特殊 uploader-derived position 正式解析；报告不提供将所有 POSITION 一律展开成系统岗位的错误 SQL。

业务提交后的部门义务可只读核以下身份列，不能更改 assignee 或签名字段：

```sql
SELECT CAST(id AS CHAR) AS obligation_row_id,
       CAST(controlled_file_id AS CHAR) AS controlled_file_id,
       process_instance_id,bpm_task_id,obligation_id,stage_code,
       CAST(department_id AS CHAR) AS department_id,
       CAST(leader_user_id AS CHAR) AS leader_user_id,
       CAST(assignee_user_id AS CHAR) AS assignee_user_id,
       CASE WHEN assignment_signature_id IS NULL THEN 0 ELSE 1 END AS has_assignment_signature,
       CASE WHEN assigned_time IS NULL THEN 0 ELSE 1 END AS has_assigned_time
FROM dcc_controlled_file_task_assignee_snapshot
WHERE tenant_id=1 AND deleted=b'0' AND controlled_file_id=:controlledFileId
ORDER BY stage_no,id;
```

## Root 真实验收要先取得的证据

1. 从真实用户/角色/菜单页面零写入核对已存在账号；按本表确认原 applicant、每部门 leader、被指派会签人、矩阵批准人、培训/下发文控及目标项目唯一负责人。不能因为角色名相似或 admin 可登录就替代。
2. 每个实际 actor 使用自己的已获授权凭据经 Playwright fresh 登录。缺凭据、账号角色或权限时记录具体前置缺口；不重置密码、不建角色、不临时加管理员、不复用 token。需要任务自有 fixture 或配置变更时先由 Root 核对用户授权边界。
3. 在正式页面取得真实 project/folder/category/File/Master/version/BPM/task，页面自然进入待办和签名弹框；候选与独立义务匹配。Long ID 在收据与前端处理为字符串。
4. 当前先运行成功主线，第二 actor 和负责人拒绝场景属于后续完整验收条件，不在本报告中新增账号或权限配置；上表留存正式检出资格供后续精确判断，不能以权限失败冒充互斥证明。
5. 图片、签名、菜单可见性、正文权限与最终工作流进展都由真实页面验证；上述 SQL 仅只读佐证，不承担申请、指派、签名、上传培训、下发或数据准备。

## 限制与结构验证

没有实际账号清单、角色绑定、菜单授权投影、岗位真实性、签名图片字节、当前部门 leader、目标项目 leader、矩阵 route 或实际任务受理证据，故所有实际账号 readiness 仍为 NOT_PROVEN。没有把静态源码检查写为真实 E2E PASS。

曾一次读取仓库基线 SQL 的固定行段越过 CREATE TABLE 边界，工具输出包含不在允许清单内的 seed 字段；已停止该读取方式并向 Root 报告，后续使用完整 CREATE TABLE 正则边界与列名清单。未复制这些值至任何新报告；没有实际 DB 读取或凭据使用。

本报告结构验证：角色矩阵覆盖上传、指派、实际会签、批准选负责人、培训、文控审核/下发、目标 leader 引用/取消、第二 actor 检出、升版及作废；SQL 全为 SELECT，参数未赋猜测值，所有 ID 输出转 CHAR 或保存正式字符串。待 Root Review 后将本报告加入现有任务 Cleanup Keep；本报告不改 task 状态。

## 源码指纹

以下为实际读取文件的 SHA-256（原始 bytes）；表结构基线仅查完整 CREATE TABLE 块，未复制 seed 内容。源码变更后重新核对本报告结论。

| 文件（仓库相对路径） | SHA-256 |
|---|---|
| docs/product/dcc-final-requirements.html | d6a62a4c4b9c1d191042fca4cfc0a66fee622622a1bdcecad48646bb3cd4f823 |
| docs/dcc-parallel-delivery/shared-contract.md | b33b8eb7fe52957a2623babcd0030ebcee4f8f048b81c27d2b3f4d534716a0c1 |
| IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/DccControlledFileController.java | c0ef1dcf1e184e1cb4ee37939abc7030ebc2d04d8834f30838e0719ad3549319 |
| IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/DccWorkflowLifecycleController.java | 05d00e38a7b12f6c1995d622842266c34b8578cb3c4cdee0ec4684c10d710f40 |
| IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/DccControlledFileWorkflowController.java | 9b76ba3d5bc82e1f185e041a28197b7cbce4c023c9c8a3e4a484443c6e0a4a2d |
| IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/projectcode/DccProjectProductCreateController.java | 06324ea122913383ae432c46458c165924807ad8f433ead606ab1184685cb67d |
| IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileWorkflowServiceImpl.java | 90ba908bc0191c28662a6e8967be7f10d6e61cfbdd874af59775d26f19f7f124 |
| IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileApprovalRouteAssigneeResolver.java | d2d4a8d0dc2126cd135d1131cf97dac17f7a219e7c6db09980ae9548cc27bc5c |
| IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileRouteReadinessService.java | 59a50cbc9cecabc206847d5dfd9997f1e6604dbdf5c6075b3f986d119b6b8ef1 |
| IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccWorkflowSignoffAssignmentService.java | 76563929db48508d58dd3524b584d79700a90bf4868744918a336228e4b46846 |
| IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccApprovalFileOwnerSelectionService.java | 35d32858db6905bb65af07ef0e932d38238a8761a9628e00df2ea3aed26ca4df |
| IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccWorkflowDistributionService.java | 193c7daf57d95b2cf4f9d3a73d9f100ea3081059cd114d14a14cb07a7499a8f5 |
| IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceImpl.java | e47d0a9d2bd30251417788345e9083442b22d86ba2e29d2b5913705487c4d337 |
| IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileRevisionServiceImpl.java | 074ab0e637417e5a3241e7207b5d5692ac00b57d996cfc39d3049ab215c00263 |
| IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileObsoleteServiceImpl.java | e648d32a79e1ded1ba1dee76153ceb8e289fa51bd13427d2d360fec558ce4c67 |
| IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileCategoryPermissionSupport.java | 768213fb53a394cbcf7cecd730c4c0d16f78886a8b56f9444293833c9eb92a9c |
| IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccElectronicSignatureAuthorizationServiceImpl.java | 6b7e80f13dba46a6fae62975c5eef9c5bfe0dd1e5cc9b8e25c71685f00906dce |
| IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccElectronicSignatureImageServiceImpl.java | a615eb2ec60269d0b860816c830a10e49ee94e2079828b814eadff561c4cbb7f |
| IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/projectcode/attributes/DccProjectLeaderService.java | 534c51ec04a11fde06ca34263eb1b04a0a5ce300328b8022ea21824f67ee8f91 |
| IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/projectcode/access/DccProjectAccessServiceImpl.java | 46007fa908119d3389e47ef6b4d6dc01e97e421fc2318a36bb4d550c606a6295 |
| IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/projectcode/productcreate/DccProjectReviewerConfigurationService.java | e1c74d6b2640b6a5b257b79ff9c4886fbb0e47e6248abafc091e7713b64a1c0d |
| IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/projectcode/productcreate/DccProjectProductCreateServiceImpl.java | 648857b1375995498f88bee42d9704399fcbadf0f11c1486f8b393ea4074a653 |
| IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/relations/DccProjectReferenceService.java | 54af3d934cef1b3849282359e22c618ce468de9e9d0ea1e612b9023653b4f224 |
| IntRuoyiBackend/yudao-module-system/src/main/java/cn/iocoder/yudao/module/system/service/permission/PermissionServiceImpl.java | 334dbfc88e3d8cbf6f51fea275e267b28e7874c3761b0d7f1ae488b75034aae6 |
| IntRuoyiBackend/yudao-module-bpm/src/main/java/cn/iocoder/yudao/module/bpm/service/task/BpmTaskServiceImpl.java | 97849a7b1b414e334ce4fba21d95efac9dc3ed428ab1ee9b832aca0c7aff2ec2 |
| IntRuoyiBackend/sql/mysql/20260930_dcc_b_project_attributes_folders.sql | 024ffedb8e7c7d326218f9022e2509c18295b59fb4563c0205d00a0f528a0b3c |
| IntRuoyiBackend/sql/mysql/20261003_dcc_project_reviewer_configuration.sql | 1fe36f1472cdbd5a9ca0e58497439cb7b64afbe3d6df0869938672b9726abdc4 |
