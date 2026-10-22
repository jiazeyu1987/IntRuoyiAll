# 01 / 03 / 05 流程定义阻塞续修

状态 in_progress：共享流程部署/资源隔离、精确映射及账号ID传输修复已完成，01/03/05真实路线预检全部GREEN。完整业务用例由三个原线程继续，不将前置通过等同整例PASS。

## 精确根因与原始RED

运行PID43284选择的tenant1上传/升版定义均是数据库版本5，但实际canonical digest为PUBLIC_MATRIX_NATIVE_V1。代码新上传/升版要求PUBLIC_MATRIX_NATIVE_V2，因此01/03/05均被EXECUTION_CONTRACT_UNREGISTERED_DEFINITION拒绝。实际部署XML与旧版本4相同，不能按UI版本号推断图身份。

workflow-definition-before.json与原始XML冻结精确定义，workflow-definition-digests.json通过当前Flowable序列化器与生产digest方法复核。正式config/dcc-workflow-v6两图恰好匹配已登记V2，不改catalog或历史签名字节。

## 当前验证和部署

- 后端核心3类10项PASS；历史继承/最新定义选择/精确启动3类59项PASS；前端模型导入5项PASS。JUnit与命令回执保存在本协调目录。
- C05通过真实前端分别导入、保存、发布两图，发布为upload 49065d37-c970-11f1-aae9-b082e25ec548、revision 59c2e12a-c970-11f1-aae9-b082e25ec548，均v6。
- 备份及冻结：workflow-deployment-backup.json。5表完整只读SQL备份位于C:/IntRuoyiBackups/20261016-dcc-workflow-v6/20261016-223602/；另冻结10旧资源SHA和97旧实例定义绑定。
- 发布后Root发现旧seed模型编辑源ID共用旧v4部署resource，正常addModelEditorSource更新导致两份旧资源变化。Root在发布前未识别该共享引用，未把此轮历史校验记PASS；已立即停止文件业务。C05通过页面导入原备份XML仅保存、零再次发布，完成精确恢复。
- 恢复后workflow-deployment-verified.json核对10旧资源SHA、97历史实例绑定、模型metadata均保持。workflow-definition-digests-after.json再次确认两个实际新部署canonical命中V2。当前模型编辑源暂为恢复后的旧图，须等待隔离修复后重新导入V2，仅保存，不重复发布。

## 正在修复的资源隔离

E2E15唯一修改BpmModelServiceImpl及专属测试，Root独占Maven。真实私有Flowable引擎复现旧seed共享引用，实际RED为旧资源bytes/digest被更新、编辑源仍为旧resource；证据bpm-isolation-red.log、-junit.xml。修复采用同一Flowable命令事务分离属于部署资源的编辑源，再写入；缺失资源明确报错。3项真实引擎隔离/晚失败回滚/普通更新及25项相关BPM回归全部PASS。

workflow-package-process.json完整package成功；workflow-runtime-copy.json核对内嵌BpmModelServiceImpl与当前测试class字节一致。新PID34096/48061，Jar SHA e537370a8e6e4cf425ccd292d5856feb791e99f6124d9e19cad55697ac42f7ce，healthUP，23:18:37—23:18:43官方真实登录PASS。

C05在新后端通过真实页面将两模型编辑源重新保存为批准V6，0发布，原批准/发布/恢复记录均保留。Root执行workflow-deployment-verify.py --require-editor-isolation与workflow-digest-inspect.py --after均PASS：两个编辑源为独立且无deployment归属的新resource、内容等于批准XML；两个v6定义ID及部署不变、每key仍恰好6版本；10旧资源/97历史绑定及metadata保持。证据workflow-deployment-verified.json、workflow-definition-digests-after.json及C05/resave-v6-result.json。

## 精确映射与账号前置

新V2 upload到新V2 revision需新增唯一精确ID/digest映射，原V1映射完整保留。workflow-definition-transition-v6.json已从实际部署核对结果生成，并在新PID34096启动时显式加载，共两行，不改旧registry；启动证据runtime-start-workflowv6.json。相同V2 revision到下一轮revision复用原定义，不创建自环猜测。

05原预检已越过定义错误，新异常NATIVE_SUPERVISOR_ENABLED_ACCOUNT_REQUIRED实际也可来自申请人U。源代码先核对U账号、租户、启用状态及岗位，再核对主管；C05确认U缺岗位，已通过真实页面仅绑定本例既有岗位，单次保存并回读。S启用/岗位及精确直属主管关系已核对。01、03已通知按自己的真实账号准备资格，待新包统一放行后复验，不重建已有fixture。

所有续发线程显式gpt-6.1-sol/high。未提交/推送、未直接API/SQL修复历史bytes、未操作远端。完整01/03/05结果仍待原用例继续验证。

## 恢复后真实页面与新增前端修复

01已真实复验原定义错误解除，批准人/会签/默认下发可带出；其余预检明确列出S2/L2/A2岗位、阶段权限、签名授权/有效图片缺口。原线程按授权继续补本例真实账号资格，不将该准备缺口归因为流程仍旧。

05申请人已补本例岗位后，真实预检暴露“审批矩阵身份缺失或精度损失”。Root核对默认Long序列化器及custom canonical合同，发现安全ID返回JSON数字，而customValue严格只收字符串。已增加真实状态机回归，取得5项中唯一合法数字ID失败RED；修复仅对安全正整数转为精确字符串，与已有会签部门传输规则一致，不改变公共矩阵身份校验、canonical原字节、digest/HMAC或不安全数字拒绝。保存/提交选择也输出规范身份。22项状态机/真实Vue编辑组件/共享合同回归全部PASS，证据custom-actor-id-red-target.log与custom-actor-id-green.log。

05在原U/原项目/原其他类型的实际页面复验显示“审批路线已就绪，共4个节点”，root-workflow-resume-result.json记录PASS_ROUTE_READINESS_READY_FOR_COMPLETE_C05、preflightPassed=true，三项原错误均未复现，未上传/保存草稿/提交申请。完整05由原线程继续。03经历readonly文件夹下拉被脚本fill的定位错误，已要求修正实际下拉操作；随后也遇到同一已修的账号ID传输校验，正用新context复验，不把未达到预检算旧定义仍失败。

## 三例前置最终核验

| 用例 | 真实页面结果 | 原阻塞 | 当前边界 |
| --- | --- | --- | --- |
| 01 | 审批路线已就绪，共4个节点；ROUTE_READINESS_PASS_FULL_CASE_PENDING | oldDefinitionErrorCleared=true；本例岗位/阶段权限/本人签名图片已补齐并回读 | 原10分支完整执行继续 |
| 03 | 审批路线已就绪，共4个节点；READY_FOR_BUSINESS | previousDefinitionErrorCleared=true；readonly下拉脚本与账号ID误判已解除 | 源件/双附件及后续升版业务继续 |
| 05 | 审批路线已就绪，共4个节点；PASS_ROUTE_READINESS_READY_FOR_COMPLETE_C05 | 定义/native账号/自定义矩阵ID错误均未复现 | 四个完整自定义矩阵业务分支继续 |

证据均在各自原目录root-workflow-resume-result.json。Root已核对03真实截图identity-fix-artifacts/root-route-readiness.png和05/01回传字段；实际新运行PID34096及SHA见workflow-isolation-runtime-proof.json。01最终checkedAt=2026-10-16T15:54:10.933Z，03=15:46:56.362Z，05最终页面=15:40:10.352Z。03/05采用APPLICATION_CUSTOM，01采用正式公共矩阵，覆盖两类实际页面。
