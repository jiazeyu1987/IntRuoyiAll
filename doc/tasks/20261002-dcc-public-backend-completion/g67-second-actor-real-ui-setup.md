# G67 第二真实测试账号的有限UI配置方案

Status: ready_for_closeout — 只读源码准备完成，实际配置由Root通过真实前端办理；本Agent未操作UI、DB/API、Git、Maven或生产。Root已确认当前tenant1用户910328/`g57dccdoc20261005`启用，dept=NULL，已有doc_control与G58入口角色；不把尚缺配置称永久认证阻塞。

## 最短顺序与准确入口

1. **Root管理员：系统管理→用户管理 `/system/user`**，按准确用户名搜索910328行→编辑。`UserForm.vue`17–24“归属部门”树选Root当前事实ID103（不是按近似名称first）；72–78“岗位”多选必须选择适用、正式启用的系统岗位。Root若无合适任务岗位，可通过正式岗位管理新建明确 `G67文控测试岗位` 后赋给这个测试用户；不复制管理员岗位冒真实职责。保持启用、用户名/昵称/已有角色，保存。只改这个用户，不改共享部门负责人。
2. **新申请会签部门选择103**，由103真实部门负责人接当前MATRIX_REVIEW任务并指派910328。`SignoffAssignmentService.context`59–68只列同义务department、启用、postIds非空的用户；assign116–123再次核同dept、非空岗位和正式readiness。旧100或910334义务不能因为用户改dept103而转给103成员。该用户不是103负责人无需改部门leader，接到已签名转派后才办理其真正会签任务。
3. **Root管理员：系统管理→角色管理 `/system/role`** 新建任务专属角色，建议名称`G67独立会签编制入口`、code=`g67_dcc_review_compile`、启用、原因说明。下划线合法（required/max100/重复检查），不是admin或electronic_signature_admin。该角色只赋现正式menu permission叶 `dcc:controlled-file:review`、`dcc:controlled-file:submit`、`dcc:controlled-file:query` 及必要祖先；已有G58的approve/bpm:task:query保留，不复制wenkong或任意log/admin组。
4. **同角色行“菜单权限”**：填原因，先关闭“父子联动”、不全选；按Root当前permission→node/祖先事实逐叶勾选。历史源码“文件上传”6806/submit、“受控浏览”6807/query只能辅助定位，review当前叶ID/重名标签必须Root核；别从近似文字猜、别勾整棵含log的树。用户910328行“分配角色”追加G67，保原doc_control/G58，不替换为超级权限角色。PermissionService会合法拒普通用户新赋admin/log角色，方案不绕这条守卫。
5. **Root合法签名管理员：电子签名→签名授权**，正式 `/signature-governance/authorizations`，或旧 `/dcc/controlled-file/signatures` 的“签名授权”页签。准确找910328行→“启用签名”开关→弹框“启用电子签名授权”核目标用户、填原因→确认。Controller45–57要求 `dcc:controlled-file:signature:manage` 与 `electronic_signature_admin`，这是操作管理员资格，**不给910328这两个管理权限/角色**。无authorization行是未授权，不是图片或token故障；必须真实UI启用、未锁定的ENABLED状态。被锁时由合法管理员在同入口按真实原因解锁，不改表。
6. **910328本人重新登录**（刷新正式部门/岗位/角色菜单），打开 `/signature-governance/my-signature`。`SignatureGovernanceMySignaturePane`是本人图片正式页，管理tab权限与它独立；该hidden/canTo静态页面可正式导航，不在浏览器改权限或注入token。填“变更原因”`G67测试账号910328独立电子签名验收，图片明确TEST ONLY`→“上传图片”选择本账号测试PNG/JPEG。206–207真实upload后自动enable，两请求成功才提示“已上传并启用”；若已上传但enable失败，按真实错误处理后点“启用图片”，不重写ACTIVE。本人my-image Controller69–98只取当前LoginUserId，不接目标userId，无签名管理员要求；Root用admin登录上传会属于admin，不能代替本人。
7. **Root管理员：DCC基础数据→项目代码/产品→项目271准确行“权限”**（button `dcc-project-code-access-rules-open`，组件ProjectCodeTabPanel）。保原启用USER OWNER admin1，点击“添加编制”→主体类型“用户”→910328/准确账号→级别`编制 EDIT`→启用、填写该行“变更原因”→“保存正式权限”。这是replace整个rules列表，必须追加而非覆盖旧OWNER；至少一条当前OWNER仍保留。只为检出/检入选择EDIT已足，若明确验换版才增加真实OWNER，不把OWNER当项目leader或引用资格。
8. **Root管理员：文件类别→技术调研报告908710→类别权限/维护权限**，保所有旧行，新增该测试用户的精确`UPLOAD`动作规则（主体用户910328/启用/原因）。菜单submit不等于类别UPLOAD；`Query.assertCanMutateControlledFile`1264–1274还核assignment scope＋project EDIT/OWNER＋category UPLOAD。controller checkout/checkin528–540本身要求query。用于检出应选当前正式受控版本，不能以EDIT代替他人的WORKING requester，也不新增scope:all/目录管理/log权限。会签REVIEW/APPROVE资格由真实任务/矩阵与menu共同确定，CategoryPermissionRulesTab199已将这两动作列为矩阵派生，不能在此伪造直接审批规则。

## 本批最小资格与边界

| 用途 | 正式必需事实 | 不应混为一项 |
|---|---|---|
| 部门负责人指派第二人 | 当前真实103义务、103leader签名；第二人启用＋dept103＋postIds非空；MATRIX_REVIEW menu review＋ENABLED签名授权＋有效本人图片 | doc_control、menu approve或project EDIT都不能代部门匹配/实际义务。 |
| 第二人实际会签 | 正式任务已转派给910328、同tenant/round、密码签名和意见；readiness.requireReadyParticipants检查review/auth/image | 审计必须910328真实目录身份，不能用admin签名或只观察任务API200称签名完成。 |
| 自己图片 | 本人认证；PNG/JPEG、0<size<=2MiB、ImageIO可解码且宽高>0；启用reason非空、image.userId=current；真实stored bytes SHA核验，ACTIVE/active1 | 上传只是UPLOADED/active0；VALID是真实校验结果，不填DB verified字段。 |
| 检出/检入 | query菜单＋project271 EDIT/OWNER＋908710 UPLOAD＋当前精确版本和互斥checkout记录/actor | 项目文本leader不是正式编辑rule；已有VIEW/培训记录权限不能代UPLOAD；没有额外“checkout管理员”。 |
| 正文预览 | 新PENDING真正分配任务可由Query2235–2240的当前task资格读审批正文；受控正文仍核现VIEW矩阵/目录授权/正式分发或requester | 不自动给910328所有正文权限或把project EDIT当全类别VIEW。若此次只验checkout/会签，按正式当前资格，不为“看不到旧正文”授目录管理员。 |

“我的签名”页面只负责本人图片，不加载统一records列表；那一列表另要求 `signature-governance:policy:query`。本次不用为上传图片默认加治理policy/manage/retention权限。若Root实际导航出现正式权限错误，核当前菜单/route事实再处理，不能声称认证能力永久不可用，也不调用my-image API替代UI。

## 测试图片素材

本次在DCC测试resources、任务目录及当前runtime保护目录未找到**明确属于910328**的签名PNG；旧admin图片及未标身份的一像素测试图不适合复用给第二人。建议Root准备纯测试PNG（此处只建议，不声称文件已存在）：白底黑字、640×180，内容 `G67 TEST ONLY` / `g57dccdoc20261005` / `User 910328 – local acceptance`；建议文件名 `g67-test-signature-910328.png`，实际可解码、<=2MiB。它明确代表这个任务测试账号，不画/复制别人的手写签名，不当正式个人签章。由910328自己的真实页面upload+activate，记录实际imageId/hash与本人userId即可；该操作不是QA批准/生产身份创建。

## Root结束时实际核验

两个浏览器会话分别真实登录admin与910328；新103会签指派完成后，910328待办出现exact task→意见/自己的密码签名→原任务完成，新signature actor910328/image绑定自己，admin指派签名仍独立。checkout竞争仅在task-owned当前受控链上，持锁人是谁由真实页面显示；另一人拒后不自动转派/偷释锁。配置成功提示不是最终PASS，Root只读核用户dept/post/role、授权及ACTIVE图片、projectrule/categoryrule，并核任务与签名。不把同admin两个会话称第二人。

读时关键pin：SignatureAuthorizationController `4c83674508741052102e9cf2762929a3ddb9e2ad5e9bcfcf1818a6fb0a57cff9`；MySignaturePane `43c5adbeb581cafd9ca85a5c58a5873581887faf67c649d14170d42e48931180`。前端role按钮与安全守卫沿G58已审正式入口，无新平台、无seed/SQL/API代业务；本准备不宣称账号、图片或多用户验收已完成。
