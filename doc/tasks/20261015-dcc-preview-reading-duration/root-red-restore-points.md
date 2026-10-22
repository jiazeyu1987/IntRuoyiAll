# Root M01 已执行完毕：以下恢复点仅作历史审计

Root 已实际按这些恢复点执行RED并恢复GREEN：RED11执行，3断言+4真实AuthorizationDenied；正式GREEN10类125/125、0F/E/skip，3生产原始字节恢复0mismatch。R01/R02/F01软件关闭，不再待执行。正式证据见 doc/tasks/20261015-dcc-six-thread-followup-review。不要为历史说明自行重复恢复或重跑。

---

# Root 统一 RED / GREEN 最小恢复点

当前 Java 实现均保持 GREEN 版本并已冻结；本线程未执行新后端测试，不等待 Root 先跑才实现，不把预期 RED/GREEN 写为已运行。实际边界以 Root 统一验证票为准。

本次相关文件实际为四个（两个生产类 + 两个所属测试类）：

- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/filepreview/DccOnlineFilePreviewReadingSessionService.java
- IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/filepreview/DccOnlineFilePreviewReadingSessionServiceTest.java
- IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/filepreview/DccPreviewReadingSessionController.java
- IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/filepreview/DccPreviewReadingSessionControllerTest.java

完整 UTF-8/LF SHA-256 见 review-handoff.json。本线程不再写上述四文件，等待 Root review。

## Duration 微秒边界

服务类 advance 方法，当前两行：

```java
Duration gap = Duration.between(session.getLastHeartbeatAt(), now);
long increment = gap.compareTo(Duration.ofSeconds(MAX_HEARTBEAT_GAP_SECONDS)) <= 0 ? gap.getSeconds() : 0;
```

仅为 RED 临时改回原两行，中间注释保留：

```java
long gap = Duration.between(session.getLastHeartbeatAt(), now).getSeconds();
long increment = gap <= MAX_HEARTBEAT_GAP_SECONDS ? gap : 0;
```

用例 clockMicrosecondLivenessBoundary 固定真实 service Clock，参数为 15000000/15000001/16000000 微秒，对 heartbeat/stop 各覆盖一次。旧行为预期在两条 15000001 微秒场景失败；这是静态预期，尚未执行。用例断言服务器累计和保存的精确 lastHeartbeatAt/endedAt，不靠字符串搜索判 PASS。

Root 命令示例（IntRuoyiBackend）：

```powershell
mvn -pl yudao-module-dcc -am "-Dtest=DccOnlineFilePreviewReadingSessionServiceTest#clockMicrosecondLivenessBoundary" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

随后恢复完整 Duration 比较再运行同用例及整个服务测试类。

## Controlled 方法安全

只改 Controller 的 startControlled、heartbeatControlled、stopControlled、summaryControlled 四个方法：

```java
// 当前 GREEN:
@PreAuthorize("isAuthenticated()")
// RED 临时恢复:
@PreAuthorize("@ss.hasPermission('dcc:controlled-file:query')")
```

统一 file-preview 的 start/heartbeat/stop/summary 四方法保持原 hasAnyPermissions 表达式，不能一并扩大。

方法安全测试通过实际 Spring AuthorizationManagerBeforeMethodInterceptor + CGLIB proxy；DefaultMethodSecurityExpressionHandler 已接 GenericApplicationContext 和 ss PermissionProbe，旧门禁是实际 query 权限拒绝，不能将缺 Bean/装配错误当产品 RED。

- actualMethodSecurityAllowsReviewOrApproveOnlyActorIntoAllFourControlledGuards：review/approve × APPROVAL_TASK/APPROVAL_HISTORY 四场景，均无 query 权限，实际调用四个 controlled 方法并验证精确 context/header token 传递。
- actualMethodSecurityRejectsMissingOrAnonymousActorBeforeAllControlledServices：无身份和匿名，两场景，实际四路在服务调用前拒绝。
- authenticatedControllerStillPropagatesDailyOrRevokedTaskGuardDenialWithZeroSessionWrites：实际 controller + reading service，Query 端口显式抛出既有正文拒绝，证明 DAILY/失效任务拒绝传播且零 session 锁/写；这是授权端口边界测试，真实 Query/Guard/BPM 的正负行为继续由其既有测试补位，不冒充真实数据库或 BPM 集成。
- onlyControlledRoutesChangeIdentityGateAndUnifiedRoutesKeepOriginalMenuPolicy：四路身份门禁和四路统一权限表达式精确合同。

Root 命令示例（IntRuoyiBackend）：

```powershell
mvn -pl yudao-module-dcc -am "-Dtest=DccPreviewReadingSessionControllerTest#actualMethodSecurityAllowsReviewOrApproveOnlyActorIntoAllFourControlledGuards+onlyControlledRoutesChangeIdentityGateAndUnifiedRoutesKeepOriginalMenuPolicy" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

随后恢复 controlled 四路 isAuthenticated，再运行整个 ControllerTest，并带真实 Query/Guard 的现有任务/历史/租户/停用用例做统一回归。方法安全基础设施若装配失败应记录测试 setup blocker，不计为业务 RED，也不能删掉拦截器退回裸 MockMvc 伪验证。

## 当前实际已运行范围

前端三专属文件 Node 25/25 PASS，当前 viewer/API/helper lint 0/0，实际 reading block + API/helper/source/context 定向 strict 检查 0 diagnostics。Java 新微秒/方法安全用例未由本线程编译或执行。没有 E2E、真实 DB、服务、Maven/shared target/full types/build、Git 提交推送、remote、subagents。
