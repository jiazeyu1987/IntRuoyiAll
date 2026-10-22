# 检出人/时间串版：定向RED交接

2026-10-19，本轮只处理Query检出事实展示。Root已执行有效RED（4项/3失败/0错误，EXACT通过）并明确授权两个overload生产GREEN；最小实现已冻结，实际GREEN待Root统一执行，本线程不跑Maven或UI。

## 根因与边界

最新Root接管manifest里，原Main4249/A1、4260/A2均已完成各自业务，只有A2被检出，却在两个只读详情同时显示DCC241016C22U、2026-10-19 12:39:25。旧verify-main-version-facts只采集这些字段，没有隔离断言，不能作为本参数通过证据。mainCheckout明确active=false/restoredThroughUi=true，原锁已于12:42真实页面撤销，不重放旧撤销。

直接根因：DccControlledFileQueryServiceImpl的fillCheckoutProjection(DccControlledFileRespVO,DccControlledFileDO)通过readLogicalCheckout取Master锁，随后不比较baseIterationId就复制by/name/time/reason。buildVersionHistory也把同一logicalCheckout传入每个历史行的另一个fillCheckoutProjection重载。当前直接测试logicalCheckoutIsVisibleFromFormalAndOtherHistoryVersions恰好断言所有历史行同一检出时间，是需纠正的旧合同。

Master锁是逻辑文件互斥，不应改变。保留response.checkedOut=(lock!=null)，前端canCheckoutVersion已经用该布尔阻止同Master新检出。版本事实checkedOutBy/Name/Time/Reason必须只属于lock.baseIterationId精确匹配的所选file.id/history.id。其他版本和无锁投影显式清空这些字段，防止Bean拷贝或对象复用残留；写侧checkout/checkin/cancel、数据库唯一约束和selectActiveByMasterId不改。精确版本比较使用Java Long，不转double/前端数字。

## BDD与直接测试

1. 同Master的A1与被检出工作版同时读取：公共getControlledFile返回A1的逻辑锁为true、版本actor/name/time/reason为空，历史精确锁基版有真实四字段，其他版本无四字段。
2. 直接调用两个真实投影重载，EXACT保持完整事实，OTHER_VERSION/NO_LOCK清空预置旧字段；两个相邻高位Long身份9007199254740993/9007199254740995不混淆。
3. 只读投影不得取消或检入锁；现有revisionOptionsKeepCheckoutActorTimeAndBlockBothFormalActions以及Master并发写入测试作为相邻回归，不改其逻辑锁范围。

测试文件：IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileQueryServiceTest.java。

精确新增/替换diff：checkout-isolation-red-test.diff（仅本轮检出测试块，已排除原工作区其他改动）。生产源指纹和命令：checkout-isolation-red-handoff.json。

Root在IntRuoyiBackend执行：

```powershell
mvn -pl yudao-module-dcc -am '-Dtest=DccControlledFileQueryServiceTest#logicalCheckoutKeepsMasterMutexButOnlyExactVersionShowsCheckoutFacts+checkoutProjectionClearsBorrowedFactsAndKeepsExactLongIdentity' -Dsurefire.failIfNoSpecifiedTests=false test
```

RED预期：公共A1投影错误带88L；OTHER_VERSION保留/覆盖了其他版本actor；NO_LOCK保留预设旧字段。EXACT控制保持通过。实际Root结果：4项/3失败/0错误，EXACT控制通过，旧版借actor88及无锁残留77精准失败；过程/xml见协调目录e2e22-checkout-isolation-red-process.json和同名.xml。

## 待Root确认RED后的最小GREEN

仅DccControlledFileQueryServiceImpl两个既有fillCheckoutProjection：设置逻辑锁布尔后，先显式清空版本actor/name/time/reason；无锁或baseIterationId不等当前file.id/history响应id时返回；精确匹配才填原四字段。调用方、API字段、Master锁、服务端所有权校验和前端无需新增接口。生产改动已按上述边界实施：精确diff见checkout-isolation-green.diff（6增/2删），冻结指纹和Root命令见checkout-isolation-green-freeze.json。Root实际GREEN/回归未返回，不能宣称通过。

## 定向UI复验准备

自有scripts/business-steps.cjs新增verify-main-checkout-isolation，不重跑原主链。checkout-main-current成功后从真实行解析本次精确检出时间；分别打开原A2和A1的真实只读记录，强断言A2=U/本次时间、A1=未检出/未检出，再返回A2确认本次锁仍在；最后撤销本次新锁并断言两版本未检出。每次断言保留实际值、期望值、精确fileId和截图。旧verify-main-version-facts在锁活动时也增加真正actor/time断言，不再仅记录。

本轮Root已恢复前端8061/PID20976和后端48061/PID25124；reboot1019-runtime-proof.json记录120105包hash ba0a8ead6e8e85cf29a8e339bbbc61a93ec30b3bb2adaf876d2736ff15f006b3、32份原文件/0待办保持、health/真实登录通过及5627暂停。此包newFixesIncluded=false；只有Root后续明确包含本修复的新包并放行后，才能记录该修复UI GREEN。

继承已通过：MainA2审批、属性/时间、整改；A1引用2项目3目录、A2引用1项目；R28/29/30原UI。没有创建项目/文件、重签、重放旧撤销、API/DB业务动作、新线程、服务或Git写入。


## 最新实际GREEN
Root已执行5tests/0failures/0errors、exit0（四项原RED参数+revisionOptions守卫）；证据为协调目录e2e22-checkout-isolation-green-process.json及同名XML/log。此前章节的待执行状态为历史阶段，现以本节与green-freeze记录为准。源码冻结，等待Root统一回归/新包明确放行，UI尚未重跑。
