# G49 / LD04 后端接线 BDD

状态：in_progress。主管理已移交唯一Maven/后端源码归属。只改projectcreate/native delegate/现DCC adapter/授权byId/VO/单新模板及对应有限tests；G48/Writer/product resolver/FE/system框架源冻结。实际DB/服务/Git/package/E2E由Root独占。

- Given当前租户真实启用申请人与冻结审核人、正式通知模板，When create/resubmit提交，Then原request/claim/Gxp与幂等system_notify_message同物理事务，审核人获得指准确request的消息；缺模板/无效收件人/空messageId/晚失败整个事务回滚。
- Given原PENDING_REVIEW且实际frozen reviewer，When审核通过，Then原意见/time/actor保存，准确当前租户admin且合法update资格获得批准消息/TODO；配置后改不替换老reviewer。审核/批准拒绝给原申请人原因通知，旧历史保留，重提新request独立键。
- Given混合文件与native请求待办及历史，When统一DCC provider分页/keyword/actor/globalView查询，Then来源独立身份、按合并排序窗口准确total与rows；native只PROCESS_IN_MODULE/false signature、无伪BPM/task/file身份，文件原路径不变。
- Given真实申请详情含COMPLETED/REJECTED及合法申请人/frozen reviewer/admin或既有受权管理查看者，When准确GET，Thentenant/账号/正式权限复核，全部Long原字符串、正确previous/next；无权限/foreigntenant拒绝，不pendingfallback。
- Givennative timeline请求准确stage sourceID和businessKey，When读取，Then核身份与真实访问授权，返回实际submission/review/approval/history事实；不能要求伪processId或写签名。

有效RED先针对旧源码实际缺通知/晚失败事务仍提交执行，再实施GREEN；新类缺失/fixture错误不当业务RED。最终记录实际CLIexit/日志/各XML及rawhash，不累计旧G48/FE执行，不以H2/离线账号端口声称真实MySQL/站内信页面E2E。

## 已执行阶段（等待最终闭合）

- 初单模块命令取旧m2 AdminDTO导致编译失败、第二命令PowerShell未quote dot参数失败，均未到业务，不计有效RED。
- 本树 `-o -pl yudao-module-dcc -am -Dtest=DccProjectApplicationNotificationTest -Dsurefire.failIfNoSpecifiedTests=false test`（PowerShell参数实际引号保存于日志入口）：2026-10-05 01:34:30 exit1；6fail/0error/0skip。旧source消息/native列表为0；模板缺失仍正常提交不抛错。原XML已另封 g49-backend-effective-red.xml，rawSHA57cb15865e43133a1808e6dd7a4570df73edd59d2943c6d5e38ee8316a3eb7b0。
- 首GREEN同源6/0 exit0 01:41:36；真实H2 request/officialSystemNotify/Gxp同事务成功。
- 首相关98/7含6HTTP fixture tenant缺失与1DONE sourceport未stub错误，准确记fixtures，不放松生产actor/requiredsourceguard。补实际DTOtenant1及明确空DONEsource后，100/7全0 exit0 01:51:40。
- Root独立审查指出update-only reviewer无法打开query-required详情；有限新case在改guard前执行有效RED1/1fail/0error（currentcreate仍成功未抛错）。新增资格update+query，申请人/拒绝收件人可读，approver沿原准确admin并需既有update+query；不自动grant。native权限只过滤其自己的源，不阻断无project-query但合法filetask来源。该修复后最终7组回归进行中。

原related5 fixture只mock新的notification/Actor端口以保原专属测试边界；新NotificationTest使用真实通知System服务与Gxp/H2覆盖事务，不用该mock宣称实际发送。新增mixedpage测试执行现正式DCC adapter与独立源宿主；仍离线而非BPM浏览器E2E。

## 最终有限验证结果

2026-10-05 01:57:53 本树 `-am` reactor退出0，7类102执行，fail/error/skip均0。专属notification15（真实request/officialnotify/Gxp事务、missingtemplate/消息实际INSERT后失败回滚、精确消息replay/冲突、当前approver、原申请/后继授权、readonlyController Long、nativeactor/keyword/timeline/query资格）；adapter27含混合source统一分页；原属性13/决策9/账本25/重试1/HTTP12。只按这一次实际7XML汇总，既有100和原G48/FE计数不叠加。原XMLbyte复制封存g49-backend-junit-archive，验证收据g49-backend-verification-receipt.json包含每项rawSHA。

新模板metadata依赖初写扩展名，正式policyparser实际RED missing migration；改为正式stem ID后完整二项闭包PASS，g49-notify-policy-red/green.json。SQL结构验证仅一新template INSERT，精确payload重复no-op，duplicate/漂移 SIGNAL，无旧UPDATE/DELETE/grant/预DROP。g49-notify-sql-static-receipt.json明确actualMySqlRuns=0，不能当MySQL首/重复已演练。

G48交付全部files当前rawSHA/bytes匹配未动；Response全部Long逐字段Shape.STRING，writeAttemptNo保持number。未Git/package/runtime/E2E/真实业务数据库动作。

## 最后 timeline 有限修复

Root要求原阶段结果独立于后续拒绝/完成。102执行前status已按stage事实固定，action尚用stage常量。新增唯一“审核通过、之后批准拒绝”的真实申请/消息/Gxp链断言：current源码实际 `DCC_PROJECT_PRODUCT_REVIEW` 而非 `APPROVE`，有效RED1/1fail/0error；XML独立封g49-timeline-effective-red.xml。只改delegate的action/actionLabel，node/storedactor/time保持。2026-10-05 02:07:41 本树相关两组43执行（新notification16＋adapter27）全0/exit0；两份XML另封g49-timeline-junit-archive。先前102/7 receipt与7XML不覆盖，43不与102叠加成全新全量结果。此后source/tests冻结，不再无因扩测试。
