# G58 独立文控入口角色的真实UI配置路径

Status: prepared_for_root_ui — 2026-10-05只读页面/后端合同核验，Root实际办理；本Agent未创建角色、修改菜单/用户、访问DB/API/UI、运行测试/Maven或操作Git。Root已核新用户910328、账号g57dccdoc20261005没有角色；既有wenkong角色含日志权限，不能合法赋给这个普通新用户，保留真实拒绝不绕守卫。

## 最短正式步骤

1. 当前admin进入**系统管理 → 权限角色/角色管理**，点击**新增权限角色**。不要复制wenkong或管理员角色。
2. 弹框**新增权限角色**依次填：权限角色名称=`G58独立文控入口`；权限角色标识=`g58_dcc_training_entry`；显示顺序=`58`；所属分类选择当前实际已启用、合适的文控/任务配置分类；状态=`开启/启用`；备注=`G58独立文控线下培训验收入口，仅文件查询、审批入口及统一待办查询，不包含日志或管理员权限`。点击**确 定**。
3. 在新角色精确行点击**菜单权限**，核弹框名称与标识均为刚建的角色；填写变更原因=`G58真实分离账号验收，为任务用户提供文件查询、受控审批入口及统一待办查询；不授日志、管理角色或正文权限`。
4. **先关闭父子联动**（标签：`父子联动(选中父节点，自动选择子节点)`，状态应为“否”），保持全选/全不选=`否`。可以打开“全部展开”帮助定位，但不能开全选。此页面初始父子联动为true；关闭后`el-tree check-strictly=true`，选择父不会把日志/下载/其它审批子节点一起勾上。
5. 仅勾现行三个正式权限节点及其必要菜单祖先：`dcc:controlled-file:query`、`dcc:controlled-file:approve`、`bpm:task:query`。菜单树只显示name，不能仅靠近似文字猜permission；Root以当前正式菜单事实核节点ID/完整祖先路径，再使用下文locator单独勾checkbox。祖先通常是文控中心、审批中心/待办，祖先若自身带其它permission也必须核对，不任意加权限。点击**确 定**；重新打开此角色菜单权限核只有准确节点/祖先，日志/管理员/下载/内容相关叶未因联动被加入。
6. 回**系统管理 → 用户管理**，搜索准确`g57dccdoc20261005`行，点该行**分配角色**（按钮直接在行操作，不必找通用“更多”）。弹框核用户名称/昵称，然后“角色”多选中选择**原doc_control身份角色**与**新G58独立文控入口**，去掉先前拒绝的wenkong/其它角色；新role实际ID由页面/Root只读核，不猜编号。
7. 填变更原因=`G58独立文控验收：赋予原doc_control身份与任务自有最小入口角色，不含日志/admin角色；培训内容权限仅由独立TRAINING_RECORD规则控制`，点**确 定**。当前0角色→准确2角色才是成功范围；若出现正式拒绝，保留错误和实际状态，不加入高权限角色绕过。任务用户以后重新登录读取正式当前角色/菜单，不能在浏览器注入token或mock身份。

新角色编码的下划线**合法**：RoleForm只required，RoleSaveReqVO仅NotBlank/Size(max100)，RoleService只核重复与特殊超级管理员code。该code不在RoleCodeEnum的管理员枚举，不能把它改成doc_control（会冲突现身份角色），也不能用super_admin等特殊code。

## 精确叶节点定位（不猜当前DB标签）

| 必需permission | 现源码中可核名称/路径提示 | 真实UI定位原则 |
|---|---|---|
| dcc:controlled-file:query | 现DCC菜单“受控浏览”；历史6807，path controlled-file/browser。另有旧DCC审批任务同permission，不能把历史ID视为现库必然ID。 | 找Root已核的实际受控浏览节点，展开其必要祖先；父子联动关闭后只点该树node本身checkbox，不勾preview/download/print/log等子节点。 |
| dcc:controlled-file:approve | 当前菜单来源曾多次迁移，普通源SQL仅通过permission查既有批准节点，**没有稳定可证明现行菜单name/id的创建常量**。 | 必须先用Root已有只读实际permission→node事实定位；不能猜“审核/批准”哪个重名节点，也不能用wenkong整个角色代替。 |
| bpm:task:query | 正式归并后的“审批中心 → 待办”，历史1207；原按钮“流程任务的查询”1221。可能多个节点共享同permission，页面name须以当前事实。 | 选择Root已核任务query叶及必要审批中心/待办祖先。父子联动关闭，勿勾流程管理、process/model或task:update等旁叶。 |

Leaf点击建议使用真实已观察Element Plus DOM：先用弹框`权限角色菜单权限`限定scope，再以准确完整祖先path展开；目标`.el-tree-node__content`内的`.el-tree-node__label`文本必须exact相等，随后只点同content内`label.el-checkbox`。可以概念性使用`getByRole('treeitem').filter({has: exactLabel}).locator(':scope > .el-tree-node__content label.el-checkbox')`，但Element Plus可能祖孙treeitem一起匹配，Root应检查匹配Count及本节点label，不能first()盲选重名。原页面没有permission文本/搜索框，不用evaluate改数据模型或调用assignAPI代替checkbox动作。

角色创建输入可以按原`getByPlaceholder`精确定位：`请输入权限角色名称`、`请输入权限角色标识`、`请输入显示顺序`、`请选择角色分类`；授权原因是`请输入本次菜单权限变更原因`，用户角色原因是`请输入本次用户角色变更原因`。角色多选Option只显示实际角色name，需要以Root核过code/id的对应名称来选原doc_control；别选近似“文控”但含日志的wenkong。

## 正式守卫与业务边界

`PermissionServiceImpl#validateAssignableUserRoles`324–355：当前用户未有restricted role时，target含任何RoleCodeEnum管理员code或任何菜单permission匹配`(^|[:\\-])log([:\\-]|$)`会整体拒绝。这是普通新用户的正式规则，不是接口偶发失败；wenkong内的dcc:controlled-file:log:query会命中，目标组合任何一个restricted就不能赋。

新入口role只有上述三permission和必要空权限祖先，原doc_control角色的categorymanage/print身份权限按Root已核事实保留，不宣称两角色并集只剩三个权限。角色菜单权限不等于项目OWNER/assignment、类别正文VIEW或新TRAINING_RECORD。新培训功能的类别TRAINING_RECORD规则由Root后续在正式页面按新合同配置；不配置正文VIEW/APPROVE类别规则去冒充该独立能力，不默认将管理员/审核人变文控。

此计划只是可审的真实UI动作路径。Root实际菜单名称/ID/ancestor必须观察核定，赋予后由普通账号登录、统一待办/准确文件培训入口验证才算实际通过；创建角色、页面成功提示或DB userroles数量不能单独证明培训能力可用。

## 源码锚点与读时指纹

- RoleForm.vue：required字段/创建/分类列表，SHA `af2a952fa95e903c7220a3eb2593064f593d78170dc2b3ea0c5bfc5270fb6147`。
- RoleAssignMenuForm.vue：24–54树与check-strictly；submit合并getCheckedKeys+getHalfCheckedKeys及原因/幂等键，SHA `74410c1cb6c137c26de77bfca83ea9a28d0f25013d5ebfca1b30bdb885a5407c`。
- UserAssignRoleForm.vue：准确row用户多选/原getUserRoleList/正式assignUserRole，SHA `7e4ec0e81e73d3c186e272a24f99ed1703a823a7e30ab008e82673570f244075`。
- PermissionServiceImpl：正式普通账号restricted角色守卫，SHA `334dbfc88e3d8cbf6f51fea275e267b28e7874c3761b0d7f1ae488b75034aae6`。

未写任何Source/test或新schema，没有seed/SQL/API/自动授role。任务真实角色与菜单资料由Root办理/只读佐证；当前准备不称G58身份配置或独立培训真实验收已完成。

## Root后继实际菜单事实：精确叶定位

Root当前只读真实菜单确认：1221=`流程任务的查询`/`bpm:task:query`/parent1207；6807=`受控浏览`/`dcc:controlled-file:query`/parent6800；6814=`受控浏览`/相同query/parent6800；6817=`DCC Approve`/`dcc:controlled-file:approve`/parent6814。这里使用的是Root本次真实事实，替代上表历史提示；本Agent没有访问库。6817的必要祖先6814本身已有同query权限，不能称它是空权限祖先，但它没有增加第四个业务permission；全路径更高祖先由Root当前事实再核。

实际menu plan（仅可审配置描述，不可拿它调用API代替页面）：

```json
{
  "roleName": "G58独立文控入口",
  "roleCode": "g58_dcc_training_entry",
  "coupledParentChildSelection": false,
  "exactLeaves": [
    {"id":"1221","name":"流程任务的查询","permission":"bpm:task:query","parentId":"1207"},
    {"id":"6807","name":"受控浏览","permission":"dcc:controlled-file:query","parentId":"6800"},
    {"id":"6817","name":"DCC Approve","permission":"dcc:controlled-file:approve","parentId":"6814"}
  ],
  "requiredDirectParents": [
    {"id":"1207","requireActualAncestorPathReview":true},
    {"id":"6800","requireActualAncestorPathReview":true},
    {"id":"6814","name":"受控浏览","permission":"dcc:controlled-file:query"}
  ],
  "userId":"910328",
  "userName":"g57dccdoc20261005",
  "assignAlongsideExistingRoleCode":"doc_control",
  "excludedRoleCode":"wenkong",
  "allowedPermissionSet":["dcc:controlled-file:query","dcc:controlled-file:approve","bpm:task:query"],
  "actualUiExecutedByThisAgent":false
}
```

勾选策略：父子联动关闭、非全选后，先定位唯一`DCC Approve`叶，校核它位于Root实际6814那一个受控浏览父节点；另6807与6814同名，不能用“受控浏览”文本first()。展开6817所在parent可证明6814路径，6807须用当前真实路径/DOM node身份独立区分。1221标签`流程任务的查询`也在Root实际1207待办路径下勾本节点；祖先checkbox逐个选，不联选其它叶。最终重新打开核节点并由Root只读核permission集合恰为上述三种，禁止log/admin等意外第四权限。若6807与6814菜单呈现重复不可准确区分，先暂停这一步核实际treepath，不盲选，也不因同query权限就选整棵含日志菜单。
