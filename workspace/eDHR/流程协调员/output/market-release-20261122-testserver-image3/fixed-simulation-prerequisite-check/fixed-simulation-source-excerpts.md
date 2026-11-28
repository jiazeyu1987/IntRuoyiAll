# 本地源码只读证据摘录

检查时间：2026-11-22 19:46（北京时间）。本文件仅为本地磁盘源码，不证明58部署版本或运行行数。
本地HEAD：3368249ef92ce90b7e401a2f916cf57db67c0c74；工位前端TeamLeaderWorkbenchPage.vue存在已有未提交改动，本任务未修改其内容。

## IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue:10564
SHA256（检查时整个文件）：22ae4488a50656ab50c7d2be74944638415f28e969898f14c58d2501809361e9
```
10564: const handleResetFixedSimulationActiveOrder = async () => {
10565:   if (!requireWritableHandoff()) return
10566:   if (activeOrderTestResetBlocked.value) return
10567:   activeOrderTestResetSubmitting.value = true
10568:   let writeStarted = false
10569:   let writeCompleted = false
10570:   try {
10571:     await ElMessageBox.confirm(
10572:       '将清理固定测试工单的生产、PQC及下游放行数据，并重新加入活跃订单。此操作不可恢复，是否继续？',
10573:       '重置固定测试订单',
10574:       { type: 'warning', confirmButtonText: '确认重置', cancelButtonText: '取消' }
10575:     )
10576:     activeOrderTestResetReceipt.value = undefined
10577:     writeStarted = true
10578:     if (!requireWritableHandoff()) return
10579:     const result = await resetFixedSimulationActiveOrder()
10580:     if (!result?.workOrderCode?.trim() || !/^[1-9]\d*$/.test(String(result.activeOrderId))) {
10581:       throw new Error('重置回执缺少正式工单号或活跃订单身份，请刷新核对后再操作')
10582:     }
10583:     writeCompleted = true
10584:     activeOrderWorkOrderKeyword.value = result.workOrderCode
10585:     activeOrderQuery.pageNo = 1
10586:     await loadActiveOrders()
10587:     if (!activeOrderOptions.value.some((order) =>
10588:       String(order.id) === String(result.activeOrderId) && order.workOrderCode === result.workOrderCode
10589:     )) {
10590:       throw new Error('刷新后未找到重置回执对应的活跃订单，请核对列表')
10591:     }
10592:     activeOrderTestResetReceipt.value = { state: 'success', message: '重置完成：' + result.workOrderCode }
10593:     ElMessage.success(
10594:       `本轮测试数据已清理，原工单已重新加入：${result.workOrderCode}（活跃订单 ${result.activeOrderId}）`
10595:     )
10596:   } catch (error) {
10597:     if (!writeStarted) {
10598:       if (error !== 'cancel' && error !== 'close') {
10599:         ElMessage.error(resolveErrorMessage(error, '无法确认重置，请重新打开确认窗口'))
10600:       }
10601:       return
10602:     }
10603:     const reason = resolveErrorMessage(error, '请稍后刷新核对')
10604:     // Known prerequisite failures roll back; transport failures remain uncertain.
10605:     const errorCode = Number((error as { code?: number | string })?.code)
10606:     if (!writeCompleted && [1040760394, 1040502000, 1040506111, 1040760324, 1040760414, 1076041001].includes(errorCode)) {
10607:       const guidance = errorCode === 1076041001 && reason.includes('生产接手缺少正式人员责任配置')
10608:         ? `${reason}。当前登录账号尚未配置正式生产人员，请使用已配置班组的生产组长账号重置，或先在人员管理中完成正式员工及系统账号绑定`
10609:         : reason
10610:       const rejectedMessage = `测试订单重置已拒绝，本次未完成重置：${guidance}`
10611:       activeOrderTestResetReceipt.value = { state: 'rejected', message: rejectedMessage }
10612:       ElMessage.error(rejectedMessage)
10613:       return
10614:     }
10615:     activeOrderTestResetReceipt.value = {
10616:       state: 'unknown',
10617:       message: `重置结果需要核对：${reason}；请刷新列表核对，不要再次重置`
10618:     }
```

## IntRuoyiFronted/src/api/mes/pro/processpool/teamLeader.ts:1401
SHA256（检查时整个文件）：419da3489c1e1106de47b7869a1ffce19ebb88358c4b50c20fe231aab836adb5
```
1401: export const resetFixedSimulationActiveOrder = async () => {
1402:   return await request.post<TeamLeaderActiveOrderTestResetRespVO>({
1403:     url: '/mes/pro/process-pool/team-leader/active-order/simulation/test-reset',
1404:     // Formal writes may exceed the default 30s; do not retry automatically.
1405:     timeout: 150000,
1406:     ignoreErrorMessage: true
1407:   })
```

## IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/controller/admin/pro/processpool/team/MesProcessPoolTeamLeaderController.java:321
SHA256（检查时整个文件）：2f28a7a86042bead0b70752e2f827d477d856039716d1ced7a0f5926e1614a4e
```
321:     @PostMapping("/active-order/simulation/test-reset")
322:     @Operation(summary = "重置固定活跃订单测试数据并重新加入")
323:     @PreAuthorize("@ss.hasPermission('mes:pro-process-pool-team-leader:maintain')")
324:     public CommonResult<MesTeamLeaderActiveOrderTestResetRespVO> resetFixedSimulationActiveOrder() {
325:         MesTeamLeaderActiveOrderTestResetResult result = activeOrderService
326:                 .resetFixedSimulationActiveOrder(SecurityFrameworkUtils.getLoginUserId());
```

## IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesTeamLeaderActiveOrderServiceImpl.java:1019
SHA256（检查时整个文件）：a05a3c789a3480816337018116cfbea9136a7bb0f858c36c92120776e5c7baec
```
1019:     @Override
1020:     @Transactional(rollbackFor = Exception.class)
1021:     public MesTeamLeaderActiveOrderTestResetResult resetFixedSimulationActiveOrder(Long leaderUserId) {
1022:         fixedTestOrderModeGuard.requireEnabled(leaderUserId);
1023:         Long tenantId = TenantContextHolder.getTenantId();
1024:         if (tenantId == null || tenantId <= 0 || leaderUserId == null) {
1025:             throw exception(PRO_PROCESS_POOL_EVENT_CONTEXT_REQUIRED, "resetFixedSimulationActiveOrder");
1026:         }
1027:         gxpAuditService.acquireLedgerLock();
1028:         fixedTestOrderInitializer.createWorkOrderIfMissing(leaderUserId);
1029:         fixedTestOrderPickListInitializer.prepareForReset(FIXED_TEST_WORK_ORDER_CODE, leaderUserId);
1030:         fixedTestOrderInitializer.ensureTestRouteProductBinding(leaderUserId);
1031:         // Every tenant resets its own order without importing route or QA definitions.
1032:         MesProWorkOrderDO workOrder = workOrderMapper.selectByTenantIdAndCodeForUpdate(
1033:                 tenantId, FIXED_TEST_WORK_ORDER_CODE);
1034:         if (workOrder == null || workOrder.getId() == null) {
1035:             throw exception(PRO_WORK_ORDER_NOT_EXISTS, FIXED_TEST_WORK_ORDER_CODE);
1036:         }
1037:         if (workOrder.getTenantId() != null && !Objects.equals(workOrder.getTenantId(), tenantId)) {
1038:             throw new IllegalStateException("FIXED_ACTIVE_ORDER_TEST_RESET_TENANT_SCOPE_MISMATCH");
1039:         }
1040:         List<MesProcessPoolActiveOrderDO> activeOrders = activeOrderMapper.selectList(
1041:                 new LambdaQueryWrapperX<MesProcessPoolActiveOrderDO>()
1042:                         .eq(MesProcessPoolActiveOrderDO::getTenantId, tenantId)
1043:                         .eq(MesProcessPoolActiveOrderDO::getWorkOrderId, workOrder.getId())
1044:                         .orderByAsc(MesProcessPoolActiveOrderDO::getId)
1045:                         .last("FOR UPDATE"));
1046:         if (activeOrders.isEmpty()) {
1047:             fixedTestOrderInitializer.requireNoOrphanRuntime(tenantId, workOrder.getId());
1048:             workOrder.setStatus(MesProWorkOrderStatusEnum.CONFIRMED.getStatus())
1049:                     .setQuantity(FIXED_TEST_WORK_ORDER_QUANTITY)
1050:                     .setTemporaryFrozen(Boolean.FALSE).setQuantityProduced(BigDecimal.ZERO);
1051:             workOrderMapper.updateById(workOrder);
1052:             MesTeamLeaderActiveOrderAddResult added = addActiveOrder(MesTeamLeaderActiveOrderAddReqBO.builder()
1053:                     .leaderUserId(leaderUserId).workOrderId(workOrder.getId())
1054:                     .idempotencyKey("ACTIVE_ORDER_TEST_RESET:" + tenantId + ":" + workOrder.getId()).build());
1055:             if (added == null || added.getActiveOrderId() == null) throw new IllegalStateException("FIXED_ACTIVE_ORDER_TEST_RESET_ADD_FAILED");
1056:             TeamMaintenanceAuditSupport.insertAudit(auditMapper, leaderUserId, "RESET_FIXED_TEST_ORDER",
1057:                     "ACTIVE_ORDER", added.getActiveOrderId(), "workOrderId=" + workOrder.getId() + ",activeOrderIds=[]",
1058:                     "newActiveOrderId=" + added.getActiveOrderId() + ",firstActivation=true,retainedAuditEvidence=true");
1059:             return MesTeamLeaderActiveOrderTestResetResult.builder()
1060:                     .workOrderCode(FIXED_TEST_WORK_ORDER_CODE).workOrderId(workOrder.getId())
1061:                     .previousActiveOrderCount(0L).activeOrderId(added.getActiveOrderId()).action(added.getAction())
1062:                     .deletedEventCount(0L).deletedBatchExecutionCount(0L).deletedRecordExecutionCount(0L).build();
```

## IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesFixedTestOrderModeGuard.java:17
SHA256（检查时整个文件）：e845a8d7878717f1bf69283d8bf72f898dbd5c78fc50429398ee824a03943fee
```
17:     static final String TEST_MODE_KEY = "system.ui.test-mode-enabled";
18:     private final ConfigApi configApi;
19: 
20:     public void requireEnabled(Long operatorId) {
21:         Long tenantId = TenantContextHolder.getTenantId();
22:         var loginUser = SecurityFrameworkUtils.getLoginUser();
23:         if (operatorId == null || operatorId <= 0 || tenantId == null || tenantId <= 0
24:                 || TenantContextHolder.isIgnore() || loginUser == null
25:                 || !Objects.equals(operatorId, loginUser.getId())
26:                 || !Objects.equals(tenantId, loginUser.getTenantId())) {
27:             throw exception(PRO_PROCESS_POOL_SIMULATION_COPY_CLEANUP_BLOCKED, "重置测试订单需要登录用户及当前租户身份");
28:         }
29:         if (!"true".equals(configApi.getConfigValueByKey(TEST_MODE_KEY))) {
30:             throw exception(PRO_PROCESS_POOL_SIMULATION_COPY_CLEANUP_BLOCKED,
31:                     "系统未开启测试模式；system.ui.test-mode-enabled 必须明确配置为 true");
32:         }
```

## IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesFixedTestOrderInitializer.java:32
SHA256（检查时整个文件）：bc1fc9bfc589f510f15035ee481b9e4fe93dfaa69e65fbc9ba0aa9aa1cc6bb14
```
32:     static final String WORK_ORDER_CODE = "SIM-COPY-CODX-PQC-20260807-SP-WO-05-OPYAO451788352161891";
33:     // Business identity of the fixed test fixture; route IDs and published versions belong to each tenant.
34:     private static final String TEST_PROJECT_CODE = "IDI";
35:     private final JdbcTemplate jdbc;
36:     private final MesProRouteSnapshotCanonicalizer canonicalizer;
37:     private final MesProRouteControlledContentAdapter controlledContentAdapter;
38: 
39:     /** Creates only missing order data inside the reset transaction. */
40:     @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
41:     public void createWorkOrderIfMissing(Long actor) {
42:         createWorkOrderIfMissing(actor, MesFixedTestOrderDataSet.FIRST);
43:     }
44: 
45:     @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
46:     public void createWorkOrderIfMissing(Long actor, MesFixedTestOrderDataSet dataSet) {
47:         Long tenant = TenantContextHolder.getTenantId();
48:         if (tenant == null || tenant <= 0 || actor == null || actor <= 0 || TenantContextHolder.isIgnore())
49:             throw blocked("固定测试工单创建需要明确的当前租户和操作人");
50:         // Serialize first creation even when no work-order row exists yet.
51:         if (jdbc.queryForList("SELECT id FROM system_tenant WHERE id=? AND deleted=0 FOR UPDATE", Long.class, tenant).size() != 1)
52:             throw blocked("当前租户不存在，不能创建测试工单");
53:         List<Map<String, Object>> existing = jdbc.queryForList(
54:                 "SELECT * FROM mes_pro_work_order WHERE tenant_id=? AND deleted=0 AND code=? FOR UPDATE", tenant, dataSet.workOrderCode());
55:         if (existing.size() > 1) throw blocked("当前租户指定测试工单不唯一");
56:         if (!existing.isEmpty()) {
57:             if (dataSet == MesFixedTestOrderDataSet.SECOND
58:                     && !MARKER.equals(existing.get(0).get("remark")))
59:                 throw blocked("数据2生产订单号已被其他订单占用，保留原订单");
60:             return;
61:         }
62: 
63:         MesFixedTestOrderReferenceGraph graph = new MesFixedTestOrderReferenceGraph(jdbc, tenant, actor);
64:         Map<String, Object> source = one(graph.sourceRows("mes_pro_work_order", "code=?", WORK_ORDER_CODE), "固定测试工单");
65:         Map<String, Object> overrides = new LinkedHashMap<>();
66:         overrides.putAll(dataSet.workOrderOverrides());
67:         for (var reference : Map.of("product_id", "mes_md_item", "client_id", "mes_md_client", "vendor_id", "mes_md_vendor").entrySet())
68:             overrides.put(reference.getKey(), orderReference(graph, tenant, reference.getValue(), source.get(reference.getKey())));
69:         overrides.put("status", MesProWorkOrderStatusEnum.CONFIRMED.getStatus());
70:         overrides.put("quantity_produced", java.math.BigDecimal.ZERO);
71:         overrides.put("quantity_scheduled", java.math.BigDecimal.ZERO);
72:         overrides.put("temporary_frozen", false);
73:         Entry order = graph.plan("mes_pro_work_order", source.get("id"), source, overrides);
74:         requireEmpty(order, "parent_id");
75:         Object orderId = graph.id("mes_pro_work_order", source.get("id"));
76:         for (Map<String, Object> bom : graph.sourceRows("mes_pro_work_order_bom", "work_order_id=?", source.get("id"))) {
77:             Map<String, Object> bomReferences = new LinkedHashMap<>();
78:             bomReferences.put("work_order_id", orderId);
79:             bomReferences.put("item_id", orderReference(graph, tenant, "mes_md_item", bom.get("item_id")));
80:             graph.plan("mes_pro_work_order_bom", bom.get("id"), bom, bomReferences);
81:         }
82:         graph.insertAll();
```

## IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesFixedTestOrderInitializer.java:177
SHA256（检查时整个文件）：bc1fc9bfc589f510f15035ee481b9e4fe93dfaa69e65fbc9ba0aa9aa1cc6bb14
```
177:     @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
178:     public void initialize(Long actor) {
179:         Long tenant = TenantContextHolder.getTenantId();
180:         if (tenant == null || tenant <= 0 || actor == null || actor <= 0 || TenantContextHolder.isIgnore())
181:             throw blocked("固定测试订单初始化需要明确的当前租户和操作人");
182:         // The tenant row exists even before the fixed order. It serializes missing-row initialization.
183:         if (jdbc.queryForList("SELECT id FROM system_tenant WHERE id=? AND deleted=0 FOR UPDATE", Long.class, tenant).size() != 1)
184:             throw blocked("当前租户不存在，不能初始化测试订单");
185:         if (tenant == SOURCE_TENANT) return;
186:         MesFixedTestOrderReferenceGraph graph = new MesFixedTestOrderReferenceGraph(jdbc, tenant, actor);
187:         Map<String, Object> sourceOrder = one(graph.sourceRows("mes_pro_work_order", "code=?", WORK_ORDER_CODE), "固定测试工单");
188:         Entry order = graph.include("mes_pro_work_order", sourceOrder.get("id"));
```

## IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesFixedTestOrderInitializer.java:769
SHA256（检查时整个文件）：bc1fc9bfc589f510f15035ee481b9e4fe93dfaa69e65fbc9ba0aa9aa1cc6bb14
```
769:     static Map<String, Object> one(List<Map<String, Object>> rows, String name) {
770:         if (rows.size() != 1) throw blocked("参考数据缺失或不唯一：" + name);
771:         return rows.get(0);
772:     }
```

## IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesFixedTestOrderReferenceGraph.java:21
SHA256（检查时整个文件）：0cfbf2fbb12456d7d6b337521d4768e4d7a1f422726916134a06e98869064551
```
21: final class MesFixedTestOrderReferenceGraph {
22:     static final long SOURCE_TENANT = 1L;
23:     static final String MARKER = "[固定测试订单初始化，参考租户1；仅用于测试]";
```

## IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesFixedTestOrderReferenceGraph.java:157
SHA256（检查时整个文件）：0cfbf2fbb12456d7d6b337521d4768e4d7a1f422726916134a06e98869064551
```
157:     List<Map<String, Object>> sourceRows(String table, String predicate, Object... args) {
158:         requireSpec(table);
159:         List<Object> params = new ArrayList<>(); params.add(SOURCE_TENANT); params.addAll(Arrays.asList(args));
160:         return jdbc.queryForList("SELECT * FROM " + table + " WHERE tenant_id=? AND " + deletion(table) + "=0 AND " + predicate + " ORDER BY id", params.toArray());
161:     }
```

## IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesFixedTestOrderReferenceGraph.java:207
SHA256（检查时整个文件）：0cfbf2fbb12456d7d6b337521d4768e4d7a1f422726916134a06e98869064551
```
207:     private static String deletion(String table) { return table.startsWith("jimu_") ? "del_flag" : "deleted"; }
```

## IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesFixedTestOrderReferenceGraph.java:422
SHA256（检查时整个文件）：0cfbf2fbb12456d7d6b337521d4768e4d7a1f422726916134a06e98869064551
```
422:     static RuntimeException blocked(String detail) { return exception(PRO_PROCESS_POOL_SIMULATION_COPY_CLEANUP_BLOCKED, detail); }
```

## IntRuoyiBackend/yudao-module-mes-common/src/main/java/cn/iocoder/yudao/module/mes/enums/ErrorCodeConstants.java:1202
SHA256（检查时整个文件）：45e055a86951adc01be6173e66e4c92343dc0e5d00c631ef735de9d0f0a74c50
```
1202:     ErrorCode PRO_PROCESS_POOL_SIMULATION_COPY_CLEANUP_BLOCKED = new ErrorCode(1_040_760_394,
1203:             "测试模拟订单操作受限：{}");
```
