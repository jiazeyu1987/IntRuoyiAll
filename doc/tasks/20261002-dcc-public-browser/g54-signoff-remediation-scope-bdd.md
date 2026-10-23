# G54 — 会签指派只在真实升版流程办理关联整改

Status: ready_for_closeout_for_Root_review。Root真实NEW文件2054545668044084026已经创建，正式MATRIX_REVIEW页面指派0POST被不适用arrangement GET 403和“整改安排尚未加载”挡住。HTML关联整改安排仅升版，后端权限核心保持。当前真正生产接线在DetailSignoffAssignment.vue；父detail/index仅挂载，上传source不改。

Given真实会签context当前task对应dcc-controlled-file-upload或dcc-controlled-file-obsolete，When加载指派和签名，Then仅依赖准确file/BPM/task/canAssign/部门候选事实，不读/渲染/要求关联整改arrangements；正式assign payload不含relationArrangements属性，也不伪造空数组或吞403。

Given真实当前task为dcc-controlled-file-revision，When加载或签名，Then保持历史关联快照/实际整改负责人/保存安排读取以及表单ready/ref/校验；其失败准确可见且0POST，成功同签名command携合法安排。Unknown/missing scope不能默认nonrevision，必须准确拒绝。

原源码已读：signoff-context record/API没有applicationType或processDefinitionKey，fileRespVO未含changeType且文件原processkey不能代表作废独立BPM。Root已协调BE新增context.processDefinitionKey，由actual task processDefinitionId准确当前tenant/round定义读取。FE API DTO/wrapper与child同步校验only upload/revision/obsolete三正式key，不猜file/路由、无额外roundGET；BE实际运行由其Owner/Root验证，不冒新字段已上线。

有效RED→GREEN执行生产child watch/save handler和真实SignoffAssignmentPanel.submit→实际workflow helper→childsave→正式API wrapper的离线transport；复用当前相关detail/workflow有限回归。Root负责共享types/build、BE实际适用save修复、真实账号/页面/运行；本Agent不Git/DB/runtime/API/browser/Maven。

结果：初有效RED4项2PASS/2FAIL，扩正式API缺scope合同后RED5项2PASS/3FAIL/exit1；失败分别为旧NEW/作废不适用GET、缺/未知key仍ready、旧API缺key未拒；原revision有数据/错误两项已PASS。GREEN5全PASS/exit0。最终6文件50执行PASS/0fail/0skip/exit0（含原detail/workflow相关重叠案例，不与RED/GREEN累计），两个生产文件ESLint --max-warnings 0 exit0、日志空。已有detail-integration专属revision fixture只补准确key，原断言保持。

实现仅DetailSignoffAssignment.vue与applicationRead.ts DTO/wrapper：scope有效后NEW/obsolete不发任何history/person/arrangements请求，actualcontext才使ready；revision保真实依赖和失败。child最后nonrevision payload删除 helper生成relationArrangements空属性，不传[]/null、不吞GET错误；revision仍表单.validate，同command保存。helper/SignoffPanel/writer/index/upload不改。实际SFC script/compiler及handlers、真Panel.submit/helper/wrapper经显式Vue/network宿主执行，非真实E2E。Source/test冻结，RootBE/types/build/运行待验；后端save适用边界另Owner修，不能用前端PASS冒实际POST已成功。
