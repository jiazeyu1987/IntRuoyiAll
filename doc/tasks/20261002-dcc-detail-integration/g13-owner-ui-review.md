# G13 批准文件负责人前端只读 Review

2026-10-03；目标分支 codex/20261001-dcc-integration，沿用20261002-dcc-detail-integration。范围为Root本批detail/index.vue、ApprovalFileOwnerPicker.vue、approval-actions.ts、workflow.ts和dcc-approval-file-owner.test.cjs。只读这些生产/测试文件；仅本报告和既有任务文档写入。没有运行全量types/build、Maven、服务、实库、E2E或Git。Root最终FE验证进程14185由Root管理。

确认需求：Native上传/升版MATRIX_APPROVAL通过时，批准人在原批准弹框选择同租户正式启用负责人；与原批准签名同次保存，不新增节点、负责人不获得其它权限；独立作废/会签/文控/外来不适用。历史显示当时冻结账号及签名/BPM，不从当前人员目录替代。

## 本轮结论

主体字段、正式账号来源、签名载荷和冻结展示有真实接线。现有定向测试6项负责人+11项旧详情共17/17 PASS，Picker真实SFC script/template compile PASS。但公开submitActionDialog仍有两个可复现异步窗口，不能仅凭这17项PASS关闭完整批准业务。

## 实际复现的缺口

### G13-FE-01 [P1] 旧提交点击可在readiness等待后改成提交新任务

源码：detail/index.vue:submitActionDialog（约6734）在入口只检查当前approvalTodoTask.id，随后await refreshTaskActionReadiness；文件/route/BPM/task/mode/owner的确认指纹在这个await之后（約6804）才捕获。refreshTaskActionReadiness（约5487）虽有requestSeq取消旧读取，但submitActionDialog不检查其返回值，继续读取共享taskActionReadiness.ready及当前task/form。openActionDialog又把submitting=false，允许在任务变化后打开新弹框。

只读实际复现：使用TypeScript AST提取实际生产submitActionDialog与refreshTaskActionReadiness声明，执行真实approval-actions.ts载荷构造；仅transport/readiness/confirm用可控Promise，未改生产或测试文件。顺序：A弹框点击提交，A-readiness挂起；切到B route/BPM/task并重新打开B弹框、填写B意见/密码/owner；B自身自动readiness先通过；A-readiness返回，被requestSeq判旧并返回false；A提交调用仍继续，弹出B确认并向签名transport发送task-B/owner-B。实验断言readyCalls=[task-A,task-B]、签名1次且taskId=task-B/fileOwnerUserId=8，输出REPRODUCED。

这是旧点击承担新任务写操作。后端仍会校验B资格/密码/签名，不能以服务器守卫代替用户在B明确点击提交。

最小修复：submit入口捕获真正clicked context（file/route/BPM/task/模式）和表单指纹，after readiness必须检查返回true及context仍一致；不能读取新的共享ready为旧调用续行。payload使用本次点击冻结的file/task/owner/password/reason，在二次确认后再次校验。建议专属公开handler行为测试覆盖A挂起→B打开并就绪→A返回，签名transport必须0次，B需自己点击。

### G13-FE-02 [P2] 旧签名响应会关闭新任务弹框并把结果提示在新上下文

源码：submitActionDialog await submitDccApprovalAction成功后，立即message.success、closeActionDialog(true)、reloadAll，没有检验请求file/task/BPM/route仍属于当前界面；finally也无请求归属直接submitting=false。

只读实际复现：提取实际公开handler与真实approval-actions模型，task-A合法选负责人确认后进入挂起签名transport；改route/BPM/task为B并打开B弹框；A返回成功。实验断言closeActionDialog收到当前task-B并关闭B，success共1次，输出REPRODUCED。这个实验确认UI串上下文，不声称后台误写B：transport发送的仍是A，问题是A成功改变B页面状态。

最小修复：沿用同一个clicked context/请求generation；响应返回后只有相同上下文才message/关闭/刷新，并且仅所属调用能释放submitting。旧写成功若当前已不同上下文，不要伪称B成功，不关闭/覆盖B，可保留全局可追溯反馈或返回原上下文核对（由Root选具体业务文案）。补实际handler晚成功及晚失败测试：A response不能清B输入/fieldErrors/owner，也不能把B submitting置false。

## 已实际验证的边界

- 负责人确认cancel：实际submitActionDialog实验transport0次，owner值保留，submitting最终false；未触发伪成功。
- 二次确认已经打开后改负责人：实际公开handler检测指纹变化，transport0次，明确“已变化”错误。
- 二次确认期间route/BPM/task变化：实际公开handlertransport0次。上述有效保护不覆盖FE-01等待readiness前的窗口，也不覆盖FE-02transport后的窗口。
- 非负责人适用approve和reject：实际approval-actions构造的transport载荷不含fileOwnerUserId，password仍为当前输入字段（未替换成所选负责人密码），reason按原规则trim。
- 精确Long：现有测试真实模型发送9007199254740993字符串，unsafe number被拒绝且零追加请求；referenceIdentity/max Long验证不按Number转ID。
- Picker不选择默认账号；目录读失败/不精确ID不emit，卸载后的账号响应不写users。现有Picker使用正式/system/user/simple-list，UserController实际只getUserListByStatus(ENABLE)，无扩大账号权限的新接口。
- 父页requiresFileOwnerSelection限定approve、非外来、MATRIX_APPROVAL、Native上传/升版definition并实际approval BPM与file.native一致；作废/会签/文控因此不携带负责人字段。公开后端approveTaskWithProcessDefinitionKey再次validateTaskAction并拒绝不适用节点携带owner，防前端绕字段。本报告没有运行后台代码，不把此源码检查写成权限运行PASS。
- 详情/历史展示读取fileOwnerNicknameSnapshot/fileOwnerUsernameSnapshot/fileOwnerUserId以及负责人批准signature/BPM字段；没有调用当前users目录替代历史名字。字段由后台prepare/bind保存，签名reason加入规范[ownerId,username,nickname]事实，并核验同file/version/BPM/task/actor/signature；这是只读源码确认，不是实际签名存储/事务验收。

## 未被当前前端测试实际证明的内容

- 账号在目录加载后被停用/跨租户的最终签名拒绝，正确依赖DccApprovalFileOwnerSelectionService.prepare在正式submit时重查启用/tenant。前端静态候选不具停用实时性；本轮只读未运行Maven/实库，需Root引用对应后台定向证据和未来页面负向验收，不能靠Picker存在声称此项PASS。
- 无权限账号的签名不会推进，必须由backend validateTaskAction/签名验证实际测试证明；本轮只读helpertransport使用受控替身并未冒充真实授权接口。
- 历史在当前账号改名/停用后仍冻名，以及失败回滚不覆盖签名，前端只读字段已接但需要后台真实数据/页面验收。
- 当前6项新测试只覆盖helper载荷、Picker与静态入口，未执行真正公开submitActionDialog取消/确认窗口/readiness/transport异步生命周期；本报告的只读实际提取实验补出了两项缺口。Root应把这些实验升级为长期专属父handler测试先RED再修。

## 证据及指纹

命令：node --test tests/unit/dcc-approval-file-owner.test.cjs tests/unit/dcc-detail-integration.test.cjs ->17/17 PASS；OwnerPicker真实compiler-sfc script/template PASS。上述两个Promise窗口实验通过真实production handler AST提取及approval-actions转译在stdin内执行，所有断言通过并输出REPRODUCED。无临时review脚本写到源码或测试目录；首次实验工具语法/等待时间问题已纠正，最终证据只采用明确confirmationStarted后操作及真实readiness两个handler组合。

LF UTF-8 SHA256：
- detail/index.vue e102fc2174b39bac243a6dc7073b0109ece8f3839a3cf4698e7946ab33eada9f
- ApprovalFileOwnerPicker.vue 51c0928a3dbc92fc83fe5e28f0950d2aec39ee9e07ce8377d7dd4bbb949abfec
- approval-actions.ts dff6853e1005a6df439dc9b9f3f28be4c64983b5fd5d3dc77661bc3011e223e7
- workflow.ts 6e11d776ba79bec04fcb8771d8342585f47203e33dd860ffb1f6154f115ad8aa
- dcc-approval-file-owner.test.cjs 5bbd287696d6d822db79b4045a7818de00de3826f5486ae1c689787b5d1a4d73

Root当前可能在修复/格式化；若指纹变化，以上结论以方法行为重新复核，不能机械按旧line或单字符串关闭。

## G14修复更新

本报告G13-FE-01/P1与G13-FE-02/P2已由Root正式授权detail Owner修复，不删除原RED依据。detail/index.vue入口捕获文件/task/BPM/route/actor/dialogGeneration/表单快照；readiness true/确认后同代同上下文才发冻结payload，晚签名响应只更新原弹框；旧成功明确原记录已提交核对，旧失败不覆盖新状态，ABA/新busy受保护。异步cleanup guard与保存后刷新失败明确提示一起覆盖。

新真实父handler测试RED5FAIL/1PASS→GREEN10/10；受影响44/44组合、3静态合同、index真实SFC编译、index lint0/0。本修复没有改workflow.ts/approval-actions.ts/Picker/后台，只detail/index.vue与新专属测试/记录。最终指纹见execution-log G14；尚无真实UI/type/build/Maven证明，总体门禁由Root收口。
