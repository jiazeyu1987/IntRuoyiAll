# G49 / LD04 前端冻结交付独立复核

结论：限定主流程源码接线复核**无新增 open P1**。准确原申请读取、专用通知导航、原意见办理/重提和文件任务导航隔离均按 Root 已确认的正式合同接通。后端授权 GET、native待办与消息事务仍是后续实施范围，不能据此宣称全 LD04 已完成。

本 reviewer 只读取源码/清单/现日志并写本报告，没有编辑生产或测试、执行测试/build/Maven、调用 HTTP/DB/浏览器、启停服务或执行 Git 写操作。当前日期 2026-10-05，分支 `int_qms`。

## 冻结清单与真实日志来源

`doc/tasks/20261002-dcc-public-browser/g49-project-application-entry-fingerprints.json` raw SHA-256 为 `6af4e1bcff5424b8437f4d5d8fb2bce2460048a6a1c89803639f5ec3c20ef738`。已重新计算 assets 7项（生产5、测试2）、BDD文档、未改G48manifest及upload6资产、rawLogs5项的 bytes/SHA；全匹配。

最终现日志 `g49-project-application-entry-final.log` raw SHA `f73a31e6bd0b0ee193598e5e8c0273aab620efef6d2ffcff9509f66cd1e34a89`，内容明确22执行、22pass、fail/cancelled/skipped/todo均0。其8文件含旧confirmation/resubmit/reasons/通知与文件入口合同，不与此前执行重复累计。原有效RED7、初GREEN7和一次旧SFC fixture import问题保留其独立日志；不是本reviewer重跑。

| 生产文件（相对 IntRuoyiFronted/src） | 原始 SHA-256 |
| --- | --- |
| views/dcc/controlled-file/basic-data/components/ProductCatalogTabPanel.vue | e2870627e936e85d8cfbab14b1ee524d224c424d0ff125a24340d3474855b7f8 |
| api/dcc/controlledFile/projectProductRequests.ts | 9ee50a97893e08ed46f4f009d4f188556d4987bf57f54baf68aa0ec03d5d0fb3 |
| utils/notifyMessageNavigation.ts | 26b2135398453fc2b0c995c1b42335d6f25f735d074d5efc92d7f5d2d84cf27a |
| views/system/notify/my/MyNotifyMessageDetail.vue | d6b31ea1c3684ceaa7b59b44aec5adb88de294cca8b4ac608e9928378855d65d |
| views/approval-center/index.vue | 79da229f01e99b95785afa63a90bbcc217698fcc8b5e447dab400c4b91f9baff |

## 有限主线核对与依据

1. **准确 GET 与 Long**：`projectProductRequests.ts.dccProjectProductRequestIdentity / getDccProjectProductRequest` 只接正整数准确 Long范围字符串或safe number；新GET `/dcc/project-product-requests/<原id>` 不Number化。响应必须为对象，且实际result.id为字符串并精确等于请求，错误本地传播。RespVO所有Long声明含previous/resubmitted/user/generated/relation为string/null；writeAttemptNo仍number。未从路由猜 actor/status/审批资格。
2. **原申请详情与授权分界**：`ProductCatalogTabPanel.loadProjectProductRequests:1124` 在准确入口只调用byId，普通手动records才读pending。GET失败清空该记录并在原弹框显示错误，不退回pending或其他申请；COMPLETED/REJECTED不因pending过滤丢失原申请。自动expanded row按精确requestId显示原冻结属性/目录及真实驳回与前后继资料。路由query仅定位，后端仍须核当前租户及真实查看资格。
3. **路由、迟到与关闭**：`consumeProjectProductRequestRoute:1179` 仅准确 `/mdm/product-catalog` 和 requestId/requestOpen=records/from=approval-center|notification 三query合同；非法id/数组/额外字段准确错误，不接文件handling。load序号和exactId均一致才应用；close/离页/unmount使read序号失效、清理原记录和loading。已存在离线晚A→B与关闭期间回执回归证明对应handler边界，未替代浏览器Element事件验证。
4. **专用消息目标**：`notifyMessageNavigation.ts.resolveDccProjectProductTarget:190` 要求 `notifyTargetType=DCC_PROJECT_PRODUCT_REQUEST`、原Longstring notifyTargetId和actionUrl。严格同origin、准确productcatalog路径、无hash/userinfo、仅三种query各恰好一次、requestId与targetId一致、requestOpen=records/from=notification。专用marker非法时返回无目标，不落BPM/showroom/文件fallback。导航只推固定path与已核query；`MyNotifyMessageDetail` 新按钮调用现共享helper，先关闭旧消息弹框。原因/标题沿Vue文字，不作为路由或任意HTML。
5. **原意见/重提与文件任务保持**：现 `handleProjectProductAction` 仍从实际row.id调用原review/approve意见wrapper，并按原exact GET刷新；不引入密码、签名图片或uniform quick-review。`editRejectedProjectProduct / restoreRejectedProjectProductForm` 仍校验原申请人、REJECTED、无已重提后继；深复制原属性/身份，正式resubmit绑定原rejectedId生成新后继，不覆盖旧申请。批准资格仍由后端现合法规则校验，待办/路由marker不新增权限。审批中心本批只加REVIEW/APPROVAL中文source label；现file detail专用handling/task导航分支未扩到 `/mdm/product-catalog`。

## 留给 BE 的准确合同

新授权 `GET /dcc/project-product-requests/{id}` 返回现原申请RespVO全部Long的真实字符串，并包含准确next/previous关系；不能借旧pending缺失证明原申请不存在，也不能只核query marker。待办sourceType为 `DCC_PROJECT_PRODUCT_REVIEW` / `DCC_PROJECT_PRODUCT_APPROVAL`，仅PROCESS_IN_MODULE、requiresSignature=false；detailRoute=`/mdm/product-catalog`，detailQuery={requestId原字符串,requestOpen:records,from:approval-center}。

通知固定 `notifyTargetType=DCC_PROJECT_PRODUCT_REQUEST`、`notifyTargetId=<原requestId字符串>`、actionUrl=`/mdm/product-catalog?requestId=<同id>&requestOpen=records&from=notification`。原G49已核唯一DCC provider组合delegate/同步正式幂等数据库消息的方案保留；不新建第二provider、BPM/fileId假身份、签名/批准配置或并行工作台。

Root统一types/build当前另行运行，本报告不抢跑或重跑。现22项为实际离线SFC/TS/helper/renderer/static合同测试日志，network/Element/lifecycle明确使用测试宿主；尚无本批真实账号浏览器办理、实际新GET、native待办或通知事务的运行证明。源接通与实际业务全链验收分开表达；本报告无新增有限源码修复要求。

## r2 有限类型修正追加

Root统一类型检查发现原候选数组推断导致TS2677；FE owner仅将原四类候选集合显式声明 `Array<NotifyMessageTarget | null>`，仍按原filter收集，项目专用分支/目标/导航未改。r2 manifest保存独立交付，旧seal不重写；notifyMessageNavigation.ts当前SHA `e184920a9ca69d47949c6bc09622ed2ce6ac061f3139e259758f217f2f8e63c2`（9770bytes），其余生产4/测试2仍原精确hash。本段只核这一类型差异，不能将它当旧7资产无解释漂移，也不重跑22项。后续Root统一types/build结果单列。

r2 manifest rawSHA为 `affe52dbce9a1396a376e650bdca5e9a64124f7f0800335f89376f21b210a792`。之后Root要求native keyfields使用项目语义，FE r3仅为两个project sourceTypes显示真实businessCode“项目代码”与真实中文currentNodeName；不制造文件版本/类别tags。r3中心sourceSHA `faf9ac9b92c3bc27c4d2b6f418a79cec68f43498432d9ba07d8dc802c22a5045`，r3 manifest rawSHA `535ead21cdfd2aae95d21837ba8e57c910cc37a8696132513fe3c90393b1fc3b`。这些新seal明确supersede对应旧源，原报告初版匹配事实保留；本reviewer未运行其26项或Roottypes/build，BE业务投影依该已确认合同提供原projectCode和中文节点。
