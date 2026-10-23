# LD04/G49 项目及产品申请入口 — 前端有限BDD与接口合同

Status: ready_for_closeout_for_Root_review。2026-10-05，Root确认LD03统一类型及构建退出0后，授权本批有限前端实施。当前5生产/2测试冻结交付；共享task仍in_progress。下文保留设计和BDD，后端新接口/实际待办通知执行与全链E2E由Root继续，不以离线fixture称实际页面已通过。

业务操作：审核人打开统一审批中心待办或站内信 → 进入现有DCC产品目录 → 自动打开该原申请的“项目申请与审批”记录 → 沿原审核/批准意见入口办理。驳回通知指向原申请，原申请人沿现有“修改后重提”新建后继；旧申请快照、原因与历史不覆盖。项目申请不新增签名、文件BPM任务或另一套工作台。

## 已核正式入口与最小合同

依据后端任务g49-project-application-todo-plan.md和Root真实只读回执g49-route-template-readonly-preflight.json，正式页面为 `/mdm/product-catalog`，Controller base为 `/dcc/project-product-requests`。

| 合同 | 本批准确行为 |
| --- | --- |
| 页面query | `requestId=<原Long字符串>&requestOpen=records&from=approval-center`；通知使用`from=notification`。query只是定位来源，不能证明读/写权限；不带fileId/taskId/handling/processInstanceId |
| 新授权详情GET | `GET /dcc/project-product-requests/{id}`，沿现`DccProjectProductCreateRespVO`返回实际原申请及后继身份。后端核租户、账号、真实申请/审核/批准/既有查看权限。该接口尚待BE正式实现，FE不得从pending列表或假fixture冒接口已上线 |
| ID类型 | Root确认该RespVO全部Long字段返回原字符串，含id、申请/审核/批准/负责人/模板/物化及previousRequestId/resubmittedRequestId；TS响应声明按真实DTO保精度，禁止Number/parseInt转换。精确响应id须等于所求id；其他字段不从route猜值 |
| 通知目标 | 与BE对齐固定`notifyTargetType=DCC_PROJECT_PRODUCT_REQUEST`、`notifyTargetId=<真实requestId>`、`actionUrl`指向上述正式页面及notification query。专用resolver只接受当前origin、准确path、三个唯一白名单query和值，ID与targetId一致，拒hash和额外身份；不得落入通用BPM/showroom导航 |
| 审批来源 | `DCC_PROJECT_PRODUCT_REVIEW`显示“项目产品审核”；`DCC_PROJECT_PRODUCT_APPROVAL`显示“项目产品批准”。backend只给PROCESS_IN_MODULE，无quick APPROVE/REJECT、requiresSignature=false；现审批中心原module导航足够，不改通用签名/处理模型 |

## 唯一源码边界及行为

- `src/views/dcc/controlled-file/basic-data/components/ProductCatalogTabPanel.vue`：新增精确route消费，复用records弹框/原审核/批准/驳回重提。路由目标独立GET并显示唯一准确记录及展开快照；普通手动records仍读原pending。刷新或原动作成功后按同一精确GET更新路由目标，使COMPLETED/REJECTED仍可读，不能因pending排除COMPLETED而丢原申请。更换requestId、离页或关闭后旧响应不得覆盖新记录；读取失败在原弹框显示真实错误，不假显示空成功。
- `src/api/dcc/controlledFile/projectProductRequests.ts`：新增正式详情wrapper及所需Longstring声明/响应身份核验。沿现base与权限接口，不新增bootstrap、授权或申请写入方式；create/review/approve/resubmit继续原正式方法。
- `src/utils/notifyMessageNavigation.ts`：新增专用项目申请target/parser/navigate分支及“查看项目及产品申请”label，保现文件发布/BPM/showroom/eDHR。`MyNotifyMessageDetail.vue`仅补本目标明确按钮，消息列表和顶栏已有同helper caller无需改办理路径；不将reason/content拼HTML。
- `src/views/approval-center/index.vue`：只新增上述两个sourceType中文label。现`resolveDccApprovalDetailLocation`只有文件detail前缀加handling/task参数；项目route进入既有普通path/query分支，不扩quick-review或签名。

本人本批有限Owner仅上述前端及相应行为测试；BE独占controller/DTO/native todo/transactional通知，Root独占统一types/build/配置/实际页面。LD01产品身份、LD02文件选择、LD03目录映射不改，已冻结upload3源保持原精确hash。实际route消费及新GET wrapper沿Root确认合同实现，BE尚待正式接线；没有假HTTP接口。

## 有效RED→GREEN场景与结果

| Given | When | Then |
| --- | --- | --- |
| 正式当前审核待办带原Longstring requestId | 点击现PROCESS_IN_MODULE进入产品目录 | 实际SFC调用准确GET、自动records并展示该request快照；无pending代查或文件BPM参数 |
| 原申请已COMPLETED或REJECTED，pending不含它 | 从通知/已处理行打开及刷新 | 仍读精确原申请，实际状态/驳回原因/后继ID保留；原申请人现重提门禁保持，不重复重提旧已后继申请 |
| 页面从申请A切到B、A读取晚到 | B已显示或弹框关闭 | A不会重开弹框/替换B，真实读取错误可见而不是空成功 |
| 项目申请专用正式通知含Longstring ID | 现helper解析并导航 | 准确sameorigin白名单path/query与原ID；实际消息详情可显示明确按钮，原其他目标合同保持 |
| foreign origin、重复/额外query、ID不一致或无权详情 | 点击或读取 | 专用目标拒绝/准确接口错误，不换目标/自动授账号/伪造可办理状态 |
| 两个项目sourceType待办 | 原中心展示并打开模块 | 正确中文来源且PROCESS_IN_MODULE，无项目quick signature；文件既有handling与签名不变 |

RED应执行真实TS helper/wrapper、真实SFC route handler或Vue渲染子树及现中心导航函数，证明旧源缺精确routeGET/项目target/中文label；不能只字符串搜索代替行为。GREEN与受影响原records/重提/通知/中心导航定向回归，lint只当前Owner改动文件。不要新全量negative runner；Root随后统一types/build并走真实账号页面，离线fixture不冒E2E PASS。

## 当前核验边界

实际RED：专属7项全FAIL/exit1，证明旧源缺准确byId GET/route消费、专用通知target、中文sourceLabel及实际消息按钮。GREEN7全PASS/exit0；补一个实际原review/approve handler经原意见wrapper的准确Long路径和同一byId刷新案例，最终专属8项PASS。

关联回归最终8文件22执行PASS/0fail/0skip/exit0：本批entry、原confirmation、resubmit/reasons、原project approval、发布通知、文件中心入口和通知点击合同。tests/e2e中的相关脚本本次只Node离线执行，不浏览器E2E。初组合21/22PASS，唯一失败是旧reasons整SFC夹具缺G46既有confirmation import；一次补真实confirmation helper、正式人员/模板及新route/lifecycle宿主，原断言保留、未降级生产守卫。该初失败日志保存且不算本批RED。当前5生产文件ESLint --max-warnings 0退出0、0warning。

消息按钮由实际SFC AST编译/Vue renderer执行、触发实际导航handler及共享helper；中心使用实际原模块导航函数。wrapper/route handler执行正式源码，network/Element/lifecycle为明确离线宿主。前端读取失败显示实际错误，不改查pending；route晚响应/关闭防污染，readonly动作不代权限。没有本Agent真实HTTP/DB/browser/services/Maven/Git/fulltypes/build，Root统一剩余验证。本批fingerprints封5生产/2测试，旧G48seal及冻结upload源码/测试精确未改。

原方案只读事实：旧ProductCatalogTabPanel records只有`/pending`；原reviewer门禁取冻结id、原resubmit门禁取真实申请人/status/无后继；approve由BE既有资格核对。旧notify helper无项目申请target；列表/顶栏已调用共享helper；现中心非文件route保持query。这些为本批有效RED依据，现上述入口已实现并经有限离线验证；真实上线/权限/账号和E2E不以本记录冒称PASS。
