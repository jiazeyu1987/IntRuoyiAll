# G59 — 详情管理态显式校验与维护生命周期投影

Status: ready_for_closeout_for_Root_review。共享任务保持in_progress。Root真实PARTIAL申请因共享ref仍FINALIZING而拒，DCC原文件已ACTIVE；BE owner开发正式native生命周期同步和有预像的显式历史维护。Root正式稳定DTO已relay后实施；后端拼接及真实页面写入验收由Root负责。

Given当前正式管理详情且非viewer/readonly trace，当前用户具有doc_control和现query/update/category:manage权限，When用户显式点击更多中的“校验生命周期投影”，Then只请求正式只读preview，显示同tenant file/master/ref/preimage和原DCC受控、生效、BPM、签名、原件事实及拟维护共享投影；进入普通详情不自动读该preview，更不写。

Given正式preview允许维护并匹配当前file/actor/tenant上下文，When填写修复原因且明确二次确认，ThenPOST只使用正式preview的身份/预像与真实reason，原文件、历史、签名不改；取消、缺原因、无资格或过期上下文零写。失败在当前弹框可见；真实成功后结束该写入，若重读失败明确“已维护但刷新失败”，不诱导重复写入。

Given只读viewer/trace或菜单权限、角色、file/actor/tenant上下文改变，When按钮或异步preview/confirm/write响应运行，Then入口及handler均拒，旧响应不污染新文件，不借admin或旧缓存资格。Parent继承G57r2 readonly metadata gate/nativeSource，全局角色/正文许可不改。

预定唯一源码：detail/index.vue（入口/child binding），新增detail/DetailLifecycleProjectionRepairDialog.vue，新增api/dcc/controlledFile/lifecycleProjectionRepair.ts；新增专属实际SFC/wrapper行为测试。正式endpoint、JSON Longstring、preimage/hash/idempotency/receipt字段由BE后续源确定，不在此猜命名。当前query/update/category:manage全三菜单 gate加enabled doccontrol/file scope后端硬守；客户端gate不授资格，不新增QA签名/taskId/生命周期节点/表/SQL。

有效RED/GREEN/有限相关lint由本Agent，Root统一全types/build与技术审计配置及真实task自有文件页面维护验收。无实际UI/API/DB/服务/Git/Maven/全typesbuild；G58两个封存、Root workingnav与其余角色数据保持。

Root正式稳定合同已确认：GET /dcc/controlled-file/workflow-lifecycle/{fileId}/lifecycle-projection-repair-preview，POST同prefix/{fileId}/repair-lifecycle-projection；所有Long十进制STRING/null、时间ISOstring/null、hash64hex。Preview展示原file/master/ref/tenant/version及DCC/canonical/domain/BPM/原control/active/approved/published/effective/artifact/signature/latest/current事实；canRepair控制提交，expectedTargetStatus及expectedActions只读由server给。POST精确masterId/versionRefId/processInstanceId/expectedCanonicalStatus FINALIZING/sourceFactsHash/preimageHash/reason/idempotencyKey，fileID只path，无clientactor/time/资格。Resp正式REPAIRED|REPLAY、原身份/BPM/hash/key及auditEventId/repairedAt，校验匹配当前请求后才提示完成。共享新pending最终统一CONTROLLED_PENDING_EFFECTIVE，不客户端猜transition。

有效RED4项FAIL/exit1：未改生产时原Parent缺实际入口computed/handler、新正式wrapper及dialog尚缺，缺口日志effective-red完整保留。首次测试文件async箭头未括号导致语法准备失败（red.log），不计业务RED；修host语法后再跑有效RED，随后才实施3生产。

Parent入口及handler要求非viewer、正式管理态、doc_control、query/update/category:manage全部菜单；现checkPermi为OR，所以逐项单permission调用再every，不将数组OR当全权。仅点击才开启dialog/preview。Preview严格原身份、nullable事实/时间/hash/资格和服务器目标；POST载荷显式8字段白名单，不注入actor/time/statebool。回执严格绑定file/master/ref/BPM/hash/key和真实audit时间/ID，未匹配不报成功。

Dialog实际source资格/canRepair、原因、二确认、同preview/context/sequence先后校验，取消及actor/file/permission/现场tenant变化均零写；readContextKey在异步边界实时读tenant，避免只computed缓存。canRepairfalse真实renderer展示原证据而无维护按钮，失败弹框可见。成功先结束该写再通知Parent，同context详情刷新失败明确已维护但刷新失败，不自动重试或伪空成功。

最后5文件20项PASS、0fail/skip、exit0（final-frozen.log），其中新5项actual Parent AST/handler、真实API wrapper+payload/receipt验证、actualdialog handlers/cancel/late/liveTenant及全SFC Vue renderer证据/readonly按钮；另15项是原G57readonly/训练/路由回归，不累加。3生产ESLint --max-warnings 0实际exit0、无输出（lint-frozen.log）。Source/test冻结，指纹g59-lifecycle-projection-repair-frontend-fingerprints.json只替代Parent及新增2源/1test；G58两批资产和G57其他11原字节保持。Root最终全types/build、BE接口落地/技术审计规则和实际task自有页面维护仍待，离线结果不是实际维护成功。
