# G57 作废、工作小版、升版返工最短UI路线核验

状态：只读完成。没有新确定主线源码缺口；以下为当前源码可到达路径，实际库与浏览器由Root核验。本owner不执行SQL/API/浏览器/Maven/Git，也不改源或测试。

## 1. 独立作废：从当前受控文件办理，批准即结束

进入项目目录，选择项目/存储文件夹，选准确当前受控文件，打开“管理”详情；在操作面板点“风险操作”→“作废当前版本”。弹框“作废申请”显示原完整文件名、编号、版本，填写“作废原因”，核对/调整“会签部门”，批准人从正式矩阵带出，核对三类本次属性和“本次默认来源（只读）”。点击“核对并提交作废申请”，再确认“确认提交作废申请”。没有培训选择、培训记录、文控审核或下发字段。

真实新待办从统一审批中心DCC TODO“办理”进入该原文件详情，但必须使用新作废BPM/task（不能文件原UPLOAD BPM）。部门负责人在“本部门会签指派”选真实会签负责人，填说明、真实密码签名；NEW/OBSOLETE不加载关联整改安排。负责人依当前真实UserTask填写通过/驳回意见、本人密码点击“确认签名”，批准人同样真实签名批准。OBSOLTE不选择文件负责人。最后刷新原详情：状态OBSOLETE、实际作废时间/原因及本次作废申请、签名历史可查；Root可另只读核BPM结束和20年保留截止/占名占号。

源码锚点：detail/index.vue:153–168风险操作；DetailObsoleteApplication.vue:1–54表单、160–197二确认及返回原file context；ObsoleteService.obsoleteControlledFile:105创建FormActionInstance和绑定新BPM/属性轮次，未insert新File；Workflow.approveTask/rejectTask当前task分支调用reviewObsolete；Signoff.reviewObsolete真实签名/正式task；v4 seed:116–123 MATRIX_APPROVAL→EndEvent；DccControlledFileObsoleteFormEffectExecutor.execute核PENDING_EFFECT与真实BPM后applyApproved；ObsoleteService.applyApproved:180–224更新原File/保留、名称编号不释放。

新位置bool不会额外阻断独立作废：这条申请不新建candidate File，而是原受控File上的独立FormAction/BPM。原File真实placement仍在，详情读取同一个id；作废effects没有清除placement或改storageDirectory。合法legacy原File无placement且无derived leaf时false，不会借Master猜位置；本次作废同样可保持。若原derived目录有mapping但placement丢失会明确错误，是本来真实破损，不能当legacy成功。新Query投影只详情metadata，作废权限还是原正式动作/类别/身份门禁。

## 2. 工作小版本：先检出/检入，再单独送审

对任务原文件精确管理详情点“检出 / 检入”，新helper依据服务端hasProjectStorageMapping及真实file/projectFolder进入现受控浏览版本操作，避免调用内部leaf的物理GET。列表“版本”选准确File小版本，点“检出”，弹框“检出文件”填“本次修改原因”→“检出”。只有实际检出人看到“检入”/“撤销检出”，同时显示谁已检出。

点“检入”打开“检入新版本”。选实际修改后的同完整原名源文件（若图纸则本次配套PDF）；填写必需“修改说明”和实际“检入备注”，培训可明确选。源或备注至少一项真实变化，失败申请修正必须新源文件。点击“检入并生成工作小版本”，等待票据上传完成及列表真实刷新；这一步生成A/1-1等WORKING而不建正式审批。

需较早正文时重复检出/检入形成两个合法working版本，然后在管理详情“发起局部 / 换版变更”（working详情按钮名“准备本次申请”）准备本次申请。“实际变更类型”选“局部变更”或“换版变更”，“正文小版本”选较早合法小版本（可先“预览所选正文”），“目标受控版本”由服务端带出，填“预设生效日期”、会签部门、培训、属性与“变更说明”，点“提交正式变更”后二次确认。无受控baseline的首次working申请显示“核对并提交初始申请”。提交后跟随服务器返回新candidate id安全导航，再真实审批。

锚点：browser/index.vue:372版本select、429–454操作、1881checkoutprompt、1003–1090检入弹框、2102–2179绑定上传并提交；Query.checkin:690–745同原名/真实变化/nextWorking、placement和关联继承；DetailApplicationPanel:3/21–89及375–435确认提交；DccRevisionPanel:16–43 intent/正文/target/submit；RevisionService.freezeCandidate:262–312真实选中正文copy、WORKING候选、inherit placement/关联后提交。A/9进位与更早工作正文已在版本策略/选项，真实未走不等于未实现。

## 3. 失败升版重提：保留失败candidate，修正文后从baseline发起同目标

先按上一步A/1受控baseline+working正文选PARTIAL申请A/2。审批人从实际BPM/task详情点击“驳回”，填真实原因/密码→“确认签名”。若要验证回到申请人节点，使用页面已存在正式“退回”目标APPLICANT_REWORK而不是伪造REJECTED；两路径分别可验。

在浏览列表版本选择原失败正式candidate（例如A/2），由原申请人检出、上传实际修正文、检入生成该失败链的工作版本A/2-1，必要时再A/2-2。然后从仍为最新受控baseline A/1的管理详情“发起局部 / 换版变更”，选择刚刚保存的A/2-1或A/2-2正文和相同原intent PARTIAL，服务端目标仍A/2，填写本次日期/培训/部门/实际属性/说明再二确认。新candidate、新BPM、attempt递增；旧失败File/BPM/source/属性/签名在“版本历史”及“申请轮次”保留。

不要把“撤回流程→重新提交”按钮当成“升版被驳回后同目标重提”：它是旧withdrawn单独入口。正确同目标走当前revision options和所选失败后继WORKING，并由DccRevisionReworkPolicy判exactpredecessor。

锚点：Workflow.rejectTaskWithProcessDefinitionKey:1662真实签名→Bpm.reject→旧candidate REJECTED；returnTask:1768保真实退回；detail/index.vue:544只WORKING/ACTIVE/CONTROLLED_PENDING显示申请面板，失败candidate不能直接新提交；Query.getRevisionOptions:1119最新正式baseline、受控identity/working/reworkselection/锁；RevisionService.createRevision:150–216 requireAttempt、requireFailedProcess、formalTarget仍baseline分配；DccRevisionReworkPolicy:20–68 exactfailedlineage/目标号/attempt；Workflow:878–888关闭APPLICANT_REWORK predecessor但保旧事实。新candidate和checkin都真实inherit自有placement，因此当前位置投影有来源，不是新bool丢位置。

## Root当前可用数据与最少前置

- Root已实证源G53-DOC文件2054545668044084026受控/下发，本轮正检出、锁仍admin1；新包后先从当前真实列表读projectFolder与checkout事实，不再次检出或假设已检入。旧真实上传票据不要跨旧会话/BPM复用。
- Root实证本次含关联+training文件尾号4028已经ACTIVE16:46:51、下发16:57:02，四签名412/413/414/415，培训原件9198354931280。本owner不从后缀拼完整Fileid，Root用现真实DOM/只读回执取得精确File/Master/project/folder/BPM。4028适合独立作废；4026保留给工作小版/升版链，避免互相终止测试前置。
- 项目272/catalog615由申请10新建，是另一真实项目，可保交叉引用测试；别把引用位置作原文件placement或原文件权限。
- 根源流程已确认单admin兼任，真实独立文控角色发现入口尚须新包/FE后验收。独立身份签名需已有真实账号/签名资料，不能脚本生成或凭请求ID伪造。

结论只限这三条正向主链及新位置投影关联读：没有发现确定新代码缺口；不声称已完成实际作废/失败重提/两次工作小版或不同账号验收。后续Root遇到真实阻断再按明确来源最小修复。
