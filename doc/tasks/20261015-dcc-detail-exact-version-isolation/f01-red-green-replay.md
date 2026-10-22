# F01 最小 RED / GREEN 恢复点

## Current Gate

Root M01 真实 RED 已执行并回执：`explicitDailyDetailClosesSameMasterRequesterWorkingPreview` 的前置 generic 实际授予断言通过，显式 DAILY 响应中 WORKING canPreview 仍为 true，目标 assertFalse 实际 FAIL。该结果为行为复现，已不再是预期或装配错误。

Root M01 正式 GREEN 回执已到：三个冻结生产文件按原始字节SHA恢复0mismatch，-am定向十类Maven exit0、freshXML125/125PASS、0F/E/skip；本类6项全部通过（SelectedDetailService7、HTTP3亦通过）。F01软件验证已关闭，不再保留待Maven/缺依赖阻塞。本线程未重跑或改写源码/原GREEN指纹。

正式证据：`doc/tasks/20261015-dcc-six-thread-followup-review/backend-red-summary.json` 与 `backend-green-summary.json`。整体RED11中3条实际断言失败及4条真实旧权限拒绝均由Root保存，不混写为本类全部失败。

## Behavioral Test

`DccSelectedDailyDetailProjectionTest#explicitDailyDetailClosesSameMasterRequesterWorkingPreview`

该方法使用实际 SelectedDetailReader、Query、版本链/动作投影与真实 Guard；仅持久化和外部依赖为 mock。fixture 有本人 ACTOR=99、同 Master ACTIVE=9007199254740993 / WORKING=9007199254740995、有正式 published/source PDF。先证明 generic 详情中本人 WORKING 的 canPreview/PREVIEW 实际成立，再检查显式 DAILY reader 必须关闭该选项，ACTIVE 根行仍可读，最后再次验证 generic 未被改变。

同类另有 `dailyWorkingIsDeniedByRealGuardWithoutPollutingLaterGenericRead` 和参数化 `legalNonDailyContextKeepsItsAuthorizedPreview`（AUTHORING、APPROVAL_TASK、APPROVAL_HISTORY、VERSION_HISTORY 四项）。没有在生产方法上打桩，不以手工构造 response 代替真实投影。

## Minimal Red Reversion

只在 `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccSelectedDetailReadService.java` 的 `authorizedProjection` 内把 **GREEN 一行**恢复为 **RED 一行**。不要回写整个 Query、Controller 或旧文件，也不要更改 reading hunk。

RED（改前真实源码）：

```java
        var result=query.getControlledFile(actor,file.getId());
```

GREEN（候选实现）：

```java
        var result=query.getControlledFile(actor,file.getId(),context);
```

RED 预期：前置 generic WORKING grant 两断言先通过，`assertFalse(history.getCanPreview(), "DAILY selected detail must not expose the WORKING preview entry")` 失败。若先因 fixture/编译错误停止，记录为装配问题并交回修正，不能当作行为 RED。

Root 定向命令（本线程未执行）：

```powershell
mvn -q -pl yudao-module-dcc -am "-Dtest=DccSelectedDailyDetailProjectionTest#explicitDailyDetailClosesSameMasterRequesterWorkingPreview" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

## Green Restoration And Scope

恢复上面的 GREEN 一行，再由 Root 执行 `DccSelectedDailyDetailProjectionTest` 整类；需要时连同已适配三参调用的 `DccSelectedDetailReadServiceTest` 与 HTTP3 检查。

Query 新增三参 `getControlledFile(userId,id,context)`：先 `context.requireFile` 与真实 Guard，再调用未改动的两参 generic 方法；仅 DAILY 结果复用 `restrictDailyBodyProjection` 收窄根行及各自历史项的 PREVIEW。整个投影不绑定根 context ThreadLocal，避免遍历同链其他 ID 时错配。

两参 generic 方法、`restrictDailyBodyProjection` 和 `assertPreviewReadingAccess` 原实现均不修改。最终源码与保护区 UTF-8/LF SHA256 见 `handoff-fingerprints.json`（生成后交还）。
