# G67 关联整改通知及七天提醒入口

Status: ready_for_closeout_for_Root_review。共享任务保持in_progress。Root最新授权收敛为既有通知helper+消息详情安全来源入口，不新增my-tasks工作台队列/后端DTO/marker/审批；实际UI、DB及runtime由Root独占。

通知源是DccRelationPlatformNotificationSender，正式templateCode=dcc_relation_remediation，params包含sourceControlledFileId、relatedMasterId、fileNumber、versionNo、dueAt和detailUrl，原地址严格为/dcc/controlled-file/detail/{sourceID}?viewer=1&from=notification。通用resolver原不识别该模板，detailUrl隐藏且消息内容不可点；详情业务信息原显示3身份/期限的英文key，并非事实未发送。

BDD：Given本人正式收到该准确templateCode且source/relatedMaster为Longstring、source编号/版本、秒精度合法期限、同源exactsource只读地址都一致，When消息详情“查看来源受控申请”或正文链接点击，Then先关闭弹框，再打开正式source只读详情，相关Master与期限中文显示；不扩大正文权限、不将源文档当整改对象。Given未知template/missingtuple/外站/地址不同ID/重复或管理query，Then无可用导航，不用任意detailUrl或fallbackBPM。SOURCE打开仍由原server权限决定。

接收人路径：原站内信/顶部消息→关联整改通知详情→核来源编号/版本、关联文件Master身份、整改期限→安全来源详情。相关文件操作应在原项目目录/选择器定位实际relatedMaster后按原权限升版，不以本通知手造related文件ID或授管理权。现my-tasks仅后端有正式recipient接口、前端无caller；Root此次明确不纳新队列，没有“整改完成批准”新规则。

提醒路径：文控工作台“待文控下发”→默认全未下发列表→勾“仅显示到期、逾期及按配置临期事项”→刷新。PendingWorkflowDistributionList验证真实ID/控制日期/版本/日期和service分类，按server日期/id升序，坏序/重复报错，已下发行过滤，未知count/error不记0；不客户端改日期造提醒。日期策略Asia/Shanghai+7天，today2026-10-06 through10-13 inclusive：10/13已控未生效=UPCOMING（临期）；10/14=FUTURE被提醒过滤；今天DUE、过去OVERDUE。旧A3已下发不再出现，排序需其他正式未下发事项。Root新B1未来10/13受控可同时验证当前关联切换/旧执行A3保和7天提醒，无改时钟。

TDD有限源为utils/notifyMessageNavigation.ts（typedcode/tuple/sameorigin地址）、system/notify/my/MyNotifyMessageDetail.vue（专用按钮/中文字段），专属实际parser/原handler/按钮与原消息/培训回归。先现senderpayload旧resolver/原handler有效RED→GREEN+2lint，一次全types/build由Root最后统一或授权此Agent执行；不新API/权限/globalwidget/角色/source数据。

已实施仅2生产：正式templateCode精确分支，source与relatedMaster Longstring<=JavaMAX、source编号/版本非空、合法秒deadline，detailUrl同origin/无凭据hash/唯一viewer1+fromnotification且pathsource相同；坏tuple在该专用分支不转其它BPM/任意URL。真实来源只读route继承原server权限，不将relatedMaster变文件ID。消息中文展示来源文件身份、关联Master身份及整改期限，专用按钮和原内容导航先关闭当前弹框再跳。没有sender/DTO/原workbench修改。

有效现payload原源码RED3=1PASS/2FAIL、exit1（red.log）：旧resolver无target、原消息无handler/按钮。最后3文件17PASS、0fail/skip、exit0（final.log），新3 actualexistingpayloadparser/foreign+tuple rejection/原模板按钮Vue renderer和真实handler、原offline6/project8回归；不叠加历史。2生产ESLint --max-warnings0 exit0无输出。Root授权单次完整types75657、单build84592实际均exit0/Build successful，独立taskoutDir g67-build-local-output不改共享dist/服务，仅既有Vite CJS弃用/Browserslist过期提示、无依赖修改。源码/测试在验证期间freeze不写，ParentG63、submitterG64r2、workbenchG59全原pins保持。Root真实收件点击和提醒仍待，不冒离线/原H2结果当真实通知UI成功。

最终g67-relation-remediation-notification-fingerprints.json封2生产/1test及原RED/GREEN/lint/全types/build日志。构建输出是任务临时产物，Root最终cleanup，不发布运行服务；原G57通知source旧pin由此阶段明确替代，旧seals保留。
