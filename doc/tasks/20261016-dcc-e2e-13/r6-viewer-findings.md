# R6 正文前置缺口与日常入口最终交接

状态：BLOCKED_TEST_PRECONDITION_PROJECT_READ_MEMBERSHIP_MISSING。取代此前“内部谓词待定位”和“日常脚本前置未完成”；原O910463、文件2054545668044084222、A/1、UPLOAD/BPM fb0c1653-ca6f-11f1-9342-b082e25ec548、签名860保持。

## 确定的项目阅读前置缺口

真实页面自然打开TRAINING_APPLICATION viewer，metadata业务code1080000012，页面显示Current user cannot access this controlled file；版本身份0、canvas0、preview/preview-metadata请求0。两次日志均为DccControlledFileDetailAuthorizationGuard.bodyDenied:196；日志只有首帧，未宣称取得完整调用栈。

本轮获准SELECT/SHOW核对该调用链：原文件tenant1、未删除、project302；O账号tenant1、启用且未删除。项目302启用且未删除，其全部访问规则只有id145：USER910457（原U）、OWNER、active1、无起止限制、tenant1、未删除。没有其他用户/部门/角色/岗位规则可匹配O；O没有项目USER规则、分配记录或临时角色授予，静态相关权限只有dcc:controlled-file:query。

代码链：DccSelectedDetailReadService#read → authorizedProjection → DccControlledFileDetailAuthorizationGuard#requireBodyRead。TRAINING_APPLICATION先requireReadableProject，再精确轮次和实时培训职责；requireReadableProject要求listReadableProjectIds(O)包含302。DccProjectAccessServiceImpl只从匹配的正式VIEW/EDIT/OWNER规则产生可读项目，再与硬范围求交。项目302唯一规则属于U，O必然缺少此正文前置资格。该结论来自全部正式规则和实际代码，不是从页面空态推断。

此前真实关联分区PASS保留，名称准入及硬file scope路径已通过；真实登录、文件tenant与自然上下文已核对。没有另行调用正文Guard逐项探针，未把这些事实夸大成完整正文Guard通过。正式轮次、历史/运行流程的file/BPM/definition/tenant一致，O/签名860/native等待和TRAINING receiveTask已有只读佐证；正文在项目门禁即可拒绝，后续完整bound resolver及二进制读取未验收。原始部署资源SHA不能与BPMN重序列化契约digest直接比较。

按最新指令，现行合同要求项目阅读资格，原O缺少资格，归类测试前置/数据缺口。没有已确证产品缺陷需生产RED/GREEN；不构造应放行RED，不改权限/Guard、不换对象、不追加日志或扩大诊断。旧323/37/14和新增单类14GREEN保留，未重跑。全部只读SQL/原始输出和结论索引见runs/20261020-repair-r6/predicate-readonly-summary.json；viewer-failure-proof.json保留此前未知谓词时的历史证据，其未知结论由本交接取代。

## 日常入口真实页面结果

BDD：Given 原O且默认项目模式；When 点击真实“受控浏览 → 存储目录 → 全域”，正式TableMultiFilter选择“文件编号/包含”，输入DCC241016C13R18ADJUST并查询；Then 完成全域过滤并记录目标入口状态，不手拼URL/context或调用API。

verify-original-O-r6-daily.cjs已修正可见入口。daily-O-1792427733934实际exit1：页面已正确过滤，但脚本的自然请求matcher误把quickFilter当整体JSON。保留为脚本失败，不记业务FAIL。仅修matcher匹配自然参数值后，daily-O-1792427825375实际exit0。

通过轮次DOM和截图人工视觉确认：存储目录已选、范围全域、文件编号输入及筛选标签正确、目标行0，显示“无权限或无匹配受控文件”。自然query为quickFilter.fieldKey=fileNumber、quickFilter.operator=contains、quickFilter.value=DCC241016C13R18ADJUST、latestVersionOnly=true；仅记录HTTP200，不以响应业务数据作PASS依据。

结论为DAILY_UI_ENTRY_UNAVAILABLE_NOT_DIRECT_BODY_ENDPOINT_TEST：日常真实入口不可用。没有执行DAILY正文端点，且O缺少项目阅读资格，不能据此独立证明待培训状态门禁。证据位于daily-O-1792427825375：result.json、process-receipt.json、evidence/01-visible-storage-entry.png、07-daily-original-boundary.png、06-exit-state.png和original-O-r6-daily.trace.zip。

## 结束状态

四轮R6浏览器均关闭。新增两轮日常补跑页面异常0、登录后写请求0、培训上传写请求0（登录单列），两包trace脱敏与ZIP校验PASS。本轮仅自有脚本/证据及获准DB只读，无直接API、DB写、账号授权、生产修改、Maven、构建/服务、Git提交推送、子Agent或跨线程消息。

Maven窗口RELEASED，本轮未获取/运行，Root可复用；没有新Java修复需出包。任务blocked，保留原资产，正文未通过，不标记ready_for_closeout/completed。按监督提醒到此结束，不扩展诊断或重跑既有PASS。
