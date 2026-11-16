package cn.iocoder.yudao.module.mes.service.pro.testdata;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.PRO_PROCESS_POOL_SIMULATION_COPY_CLEANUP_BLOCKED;
import static cn.iocoder.yudao.module.mes.service.pro.testdata.MesLocalTestRunContracts.*;

import cn.hutool.core.util.IdUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.dcc.dal.mysql.projectcode.DccProjectProductCreateRequestMapper;
import cn.iocoder.yudao.module.dcc.service.projectcode.access.DccProjectAccessService;
import cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.DccProjectProductActorSupport;
import cn.iocoder.yudao.module.dcc.service.projectcode.productcreate.DccProjectReviewerRoleService;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.*;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.testdata.MesLocalTestRunManifestDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.*;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.*;
import cn.iocoder.yudao.module.mes.dal.mysql.md.unitmeasure.MesMdUnitMeasureMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.testdata.*;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.*;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesFixedTestOrderModeGuard;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesLocalTestRunSourceGuard;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Explicit test input boundary. Inserts initial upstream inputs only, never ERP writes or
 * downstream evidence.
 */
@Service
@RequiredArgsConstructor
public class MesLocalTestRunPreparationService {
  public static final String SOURCE = "LOCAL_TEST_RUN", STATUS = "LOCAL_TEST";
  private final MesFixedTestOrderModeGuard mode;
  private final MesLocalTestRunManifestMapper manifests;
  private final MesLocalTestRunDefinitionMapper definitions;
  private final DccProjectAccessService projectAccess;
  private final DccProjectProductCreateRequestMapper requests;
  private final DccProjectProductActorSupport actors;
  private final DccProjectReviewerRoleService reviewerRoles;
  private final PermissionApi permissions;
  private final MesLocalTestRunSourceGuard sourceGuard;
  private final MesMdItemMapper items;
  private final MesMdUnitMeasureMapper units;
  private final MesMdProductBomMapper productBoms;
  private final MesProWorkOrderMapper orders;
  private final MesProWorkOrderBomMapper orderBoms;
  private final MesKingdeeProductionMaterialListMapper materials;
  private final ErpKingdeeProductionPickListMapper picks;
  private final ErpKingdeeProductionPickListItemMapper pickItems;
  private final ErpKingdeeProductionReplenishmentListMapper replenishments;
  private final ErpKingdeeProductionReplenishmentListItemMapper replenishmentItems;

  public List<ApprovedProduct> approvedProducts(Long actor) {
    mode.requireEnabled(actor);
    actors.requireReadableAccount(actor);
    Set<Long> readable = new HashSet<>(projectAccess.listReadableProjectIds(actor));
    var candidates =
        definitions.selectApproved(tenant()).stream()
            .filter(c -> readable.contains(c.projectId()))
            .filter(c -> readableRequest(actor, c.requestId()))
            .toList();
    Set<Long> requestIds = new HashSet<>();
    for (var candidate : candidates) {
      if (!requestIds.add(candidate.requestId())) {
        throw blocked("批准申请对应多个MES产品身份，请核对工艺路线项目和产品绑定：申请ID=" + candidate.requestId());
      }
    }
    return candidates;
  }

  private boolean readableRequest(Long actor, Long requestId) {
    var q = requests.selectById(requestId);
    if (q == null || !Objects.equals(q.getTenantId(), tenant())) return false;
    return Objects.equals(actor, q.getApplicantUserId())
        || Objects.equals(actor, q.getConfiguredReviewerUserId())
        || Objects.equals(actor, q.getReviewerUserId())
        || Objects.equals(actor, q.getApproverUserId())
        || reviewerRoles.canReview(actor, q)
        || actors.isApprover(actor)
        || permissions.hasAnyRolesOrSuperAdmin(actor, "doc_control", "approval_admin");
  }

  public Preflight preflight(Request r, Long actor) {
    mode.requireEnabled(actor);
    actors.requireReadableAccount(actor);
    validate(r);
    List<Check> checks = new ArrayList<>();
    checks.add(check("INPUT", "RUN、A/B数量批号和补料选择完整", "/mes/pro/work-order", true));
    var saved = manifests.selectByRunId(r.getRunId());
    if (!saved.isEmpty()) {
      projectAccess.assertProjectViewerOrAbove(actor, r.getProjectId());
      if (!readableRequest(actor, r.getRequestId())) throw blocked("无权读取所选批准申请");
      boolean match = saved.size() == 2;
      List<OrderResult> result = new ArrayList<>();
      for (var m : saved) {
        var input =
            r.getOrders().stream()
                .filter(o -> o.getSlot().equals(m.getSlot()))
                .findFirst()
                .orElse(null);
        if (input == null
            || !sameRequest(r, input, m)
            || !Objects.equals(m.getInputHash(), MesLocalTestRunInputHash.hash(m))) {
          match = false;
          continue;
        }
        var order = orders.selectById(m.getWorkOrderId());
        if (!sameOrder(m, order)) {
          match = false;
          continue;
        }
        sourceGuard.validate(order, false);
        result.add(result(m, order, true));
      }
      checks.add(
          check(
              "RUN_IDENTITY",
              match ? "同RUN原记录复用，保留实际状态" : "同RUN参数或持久化身份冲突，禁止覆盖",
              "/mes/pro/work-order",
              match));
      return response(checks, result);
    }
    projectAccess.assertProjectViewerOrAbove(actor, r.getProjectId());
    if (!readableRequest(actor, r.getRequestId())) throw blocked("无权读取所选批准申请");
    var exact =
        definitions.selectApproved(tenant()).stream()
            .filter(
                c ->
                    Objects.equals(c.requestId(), r.getRequestId())
                        && Objects.equals(c.projectId(), r.getProjectId())
                        && Objects.equals(c.catalogId(), r.getCatalogId())
                        && Objects.equals(c.relationId(), r.getRelationId()))
            .toList();
    boolean approved =
        exact.size() == 1 && Objects.equals(exact.get(0).mesProductId(), r.getMesProductId());
    checks.add(
        check(
            "APPROVED_PRODUCT",
            exact.size() > 1 ? "批准申请对应多个MES产品身份，请核对工艺路线项目和产品绑定" : "当前租户COMPLETED申请、目录、项目和正式关系精确一致",
            "/mdm/product-catalog",
            approved));
    var product = r.getMesProductId() == null ? null : items.selectById(r.getMesProductId());
    boolean productReady =
        approved
            && product != null
            && Objects.equals(product.getStatus(), 0)
            && Objects.equals(product.getCode(), exact.get(0).productCode())
            && Objects.equals(product.getName(), exact.get(0).productName());
    checks.add(
        check(
            "MES_PRODUCT", "工艺路线须显式绑定本项目和所选MES产品，且产品编码、名称与批准申请精确一致", "/mes/md/item", productReady));
    checks.add(
        check(
            "ROUTE",
            "须在工艺路线显式绑定本项目和产品并发布，当前应有唯一启用且生效的正式路线",
            "/mes/pro/route",
            productReady
                && definitions.countExactActiveRoute(
                        tenant(), r.getProjectId(), r.getMesProductId())
                    == 1));
    var boms =
        productReady
            ? productBoms.selectByItemId(product.getId()).stream()
                .toList()
            : List.<MesMdProductBomDO>of();
    boolean bomReady =
        !boms.isEmpty()
            && boms.stream().map(MesMdProductBomDO::getBomItemId).distinct().count() == boms.size();
    boolean unitReady = productReady && unitEnabled(product.getUnitMeasureId());
    for (var b : boms) {
      var child = items.selectById(b.getBomItemId());
      bomReady &=
          b.getId() != null
              && b.getQuantity() != null
              && b.getQuantity().signum() > 0
              && child != null
              && Objects.equals(child.getStatus(), 0);
      unitReady &= child != null && unitEnabled(child.getUnitMeasureId());
      for (var input : r.getOrders())
        if (b.getQuantity() != null) {
          bomReady &= fits(b.getQuantity().multiply(input.getQuantity()));
          if (Boolean.TRUE.equals(input.getIncludeReplenishment()))
            bomReady &= fits(b.getQuantity());
        }
    }
    checks.add(check("BOM", "启用BOM、物料及数量精度完整，无跨产品推断", "/mes/md/item", bomReady));
    checks.add(check("UNITS", "产品及BOM物料计量单位均启用", "/mes/md/unit-measure", unitReady));
    boolean clear = r.getOrders().stream().allMatch(o -> orders.selectByCode(code(r, o)) == null);
    checks.add(check("RUN_IDENTITY", "A/B工单编码未被其他来源使用", "/mes/pro/work-order", clear));
    return response(checks, List.of());
  }

  @Transactional(rollbackFor = Exception.class)
  public Result prepare(Request r, Long actor) {
    mode.requireEnabled(actor);
    validate(r);
    if (manifests.lockTenant(tenant()) == null) throw blocked("当前租户不存在");
    var check = preflight(r, actor);
    if (!check.ready())
      throw blocked(
          check.missing().stream()
              .map(Check::message)
              .reduce((a, b) -> a + "；" + b)
              .orElse("预检失败"));
    if (!check.orders().isEmpty()) return new Result(SOURCE, r.getRunId(), check.orders());
    var product = r.getMesProductId() == null ? null : items.selectById(r.getMesProductId());
    var bom =
        productBoms.selectByItemId(product.getId()).stream()
            .sorted(Comparator.comparing(MesMdProductBomDO::getId))
            .toList();
    List<OrderResult> results = new ArrayList<>();
    for (var input :
        r.getOrders().stream().sorted(Comparator.comparing(OrderInput::getSlot)).toList()) {
      var m = manifest(r, input);
      long orderId = IdUtil.getSnowflakeNextId();
      m.setId(IdUtil.getSnowflakeNextId());
      m.setWorkOrderId(orderId);
      var order =
          MesProWorkOrderDO.builder()
              .id(orderId)
              .code(code(r, input))
              .name("本地虚拟测试输入 " + r.getRunId() + "/" + input.getSlot())
              .type(1)
              .orderSourceType(2)
              .orderSourceCode(marker(m))
              .productId(r.getMesProductId())
              .quantity(input.getQuantity())
              .batchCode(input.getBatchCode())
              .quantityProduced(BigDecimal.ZERO)
              .quantityChanged(BigDecimal.ZERO)
              .quantityScheduled(BigDecimal.ZERO)
              .parentId(0L)
              .status(1)
              .temporaryFrozen(false)
              .requestDate(LocalDateTime.now())
              .remark("LOCAL_TEST_RUN：本地虚拟上游已确认输入；不代表ERP同步、ERP审核、生产或检验完成")
              .build();
      order.setTenantId(tenant());
      must(orders.insert(order));
      if (!sameOrder(m, orders.selectById(orderId))) throw blocked("工单数量或批号持久化精度不匹配，整批回滚");
      createSources(m, order, bom, product);
      m.setInputHash(MesLocalTestRunInputHash.hash(m));
      must(manifests.insert(m));
      sourceGuard.validate(orders.selectById(orderId), false);
      results.add(result(m, orders.selectById(orderId), false));
    }
    return new Result(SOURCE, r.getRunId(), List.copyOf(results));
  }

  private void createSources(
      MesLocalTestRunManifestDO m,
      MesProWorkOrderDO order,
      List<MesMdProductBomDO> boms,
      MesMdItemDO product) {
    String fid = SOURCE + ":" + m.getId() + ":P", bill = order.getCode() + "-P";
    var header =
        ErpKingdeeProductionPickListDO.builder()
            .id(IdUtil.getSnowflakeNextId())
            .sourceFormId(SOURCE)
            .sourceFid(fid)
            .sourceBillNo(bill)
            .documentStatus(STATUS)
            .description("本地虚拟领料样本；非ERP审核证据")
            .billDate(LocalDateTime.now())
            .lastSyncTime(LocalDateTime.now())
            .build();
    header.setTenantId(tenant());
    must(picks.insert(header));
    m.setPickHeaderId(header.getId());
    ErpKingdeeProductionReplenishmentListDO rh = null;
    if (Boolean.TRUE.equals(m.getIncludeReplenishment())) {
      rh =
          ErpKingdeeProductionReplenishmentListDO.builder()
              .id(IdUtil.getSnowflakeNextId())
              .sourceFormId(SOURCE)
              .sourceFid(SOURCE + ":" + m.getId() + ":R")
              .sourceBillNo(order.getCode() + "-R")
              .documentStatus(STATUS)
              .description("本地虚拟补料样本；非ERP审核证据")
              .billDate(LocalDateTime.now())
              .lastSyncTime(LocalDateTime.now())
              .build();
      rh.setTenantId(tenant());
      must(replenishments.insert(rh));
      m.setReplenishmentHeaderId(rh.getId());
    }
    List<Long> pids = new ArrayList<>(), rids = new ArrayList<>(), mids = new ArrayList<>();
    int line = 0;
    for (var b : boms) {
      line++;
      var item = items.selectById(b.getBomItemId());
      var unit = units.selectById(item.getUnitMeasureId());
      BigDecimal quantity = b.getQuantity().multiply(m.getQuantity());
      var ob =
          MesProWorkOrderBomDO.builder()
              .id(IdUtil.getSnowflakeNextId())
              .workOrderId(order.getId())
              .itemId(item.getId())
              .quantity(quantity)
              .remark(SOURCE)
              .build();
      must(orderBoms.insert(ob));
      var material =
          MesKingdeeProductionMaterialListDO.builder()
              .id(IdUtil.getSnowflakeNextId())
              .sourceFormId(SOURCE)
              .sourceBillNo(order.getCode() + "-M")
              .sourceEntryId(ob.getId().toString())
              .productCode(product.getCode())
              .productionOrderNo(order.getCode())
              .productionOrderLineNo(1)
              .productionOrderStatus(STATUS)
              .childMaterialCode(item.getCode())
              .childMaterialName(item.getName())
              .childUnitName(unit.getName())
              .requiredQuantity(quantity)
              .workOrderId(order.getId())
              .workOrderCode(order.getCode())
              .workOrderBomId(ob.getId())
              .productId(product.getId())
              .childMaterialId(item.getId())
              .lastSyncTime(LocalDateTime.now())
              .build();
      must(materials.insert(material));
      mids.add(material.getId());
      var pi =
          ErpKingdeeProductionPickListItemDO.builder()
              .id(IdUtil.getSnowflakeNextId())
              .productionPickListId(header.getId())
              .sourceFormId(SOURCE)
              .sourceFid(fid)
              .sourceBillNo(bill)
              .sourceEntryId(ob.getId().toString())
              .sourceLineKey(fid + ":" + ob.getId())
              .materialNumber(item.getCode())
              .materialName(item.getName())
              .unitName(unit.getName())
              .requestedQuantity(quantity)
              .actualQuantity(quantity)
              .baseActualQuantity(quantity)
              .lotNumber(m.getBatchCode() + "-M" + line)
              .productionOrderNo(order.getCode())
              .productionOrderLineNo(1)
              .productionMaterialListNo(order.getCode() + "-M")
              .productionMaterialListLineNo(line)
              .lastSyncTime(LocalDateTime.now())
              .build();
      pi.setTenantId(tenant());
      must(pickItems.insert(pi));
      pids.add(pi.getId());
      if (rh != null) {
        BigDecimal extra = b.getQuantity();
        var ri =
            ErpKingdeeProductionReplenishmentListItemDO.builder()
                .id(IdUtil.getSnowflakeNextId())
                .productionReplenishmentListId(rh.getId())
                .sourceFormId(SOURCE)
                .sourceFid(rh.getSourceFid())
                .sourceBillNo(rh.getSourceBillNo())
                .sourceEntryId(ob.getId().toString())
                .sourceLineKey(rh.getSourceFid() + ":" + ob.getId())
                .materialNumber(item.getCode())
                .materialName(item.getName())
                .unitName(unit.getName())
                .requestedQuantity(extra)
                .actualQuantity(extra)
                .baseActualQuantity(extra)
                .lotNumber(m.getBatchCode() + "-R" + line)
                .productionOrderNo(order.getCode())
                .productionOrderLineNo(1)
                .productionMaterialListNo(order.getCode() + "-M")
                .productionMaterialListLineNo(line)
                .lastSyncTime(LocalDateTime.now())
                .build();
        ri.setTenantId(tenant());
        must(replenishmentItems.insert(ri));
        rids.add(ri.getId());
      }
    }
    m.setPickItemIdsJson(JsonUtils.toJsonString(pids));
    m.setReplenishmentItemIdsJson(JsonUtils.toJsonString(rids));
    m.setMaterialItemIdsJson(JsonUtils.toJsonString(mids));
    m.setMaterialSnapshotHash(
        MesLocalTestRunSourceGuard.materialSnapshotHash(
            materials.selectListByWorkOrderId(order.getId())));
    m.setPickSnapshotHash(
        MesLocalTestRunSourceGuard.pickSnapshotHash(
            picks.selectById(header.getId()),
            pickItems.selectListByPickListIds(List.of(header.getId()))));
    if (rh != null)
      m.setReplenishmentSnapshotHash(
          MesLocalTestRunSourceGuard.replenishmentSnapshotHash(
              replenishments.selectById(rh.getId()),
              replenishmentItems.selectListByReplenishmentListIds(List.of(rh.getId()))));
  }

  private boolean unitEnabled(Long id) {
    var u = id == null ? null : units.selectById(id);
    return u != null && Objects.equals(u.getStatus(), 0);
  }

  static MesLocalTestRunManifestDO manifest(Request r, OrderInput o) {
    var m = new MesLocalTestRunManifestDO();
    m.setTenantId(tenant());
    m.setSource(SOURCE);
    m.setRunId(r.getRunId());
    m.setSlot(o.getSlot());
    m.setRequestId(r.getRequestId());
    m.setProjectId(r.getProjectId());
    m.setCatalogId(r.getCatalogId());
    m.setRelationId(r.getRelationId());
    m.setMesProductId(r.getMesProductId());
    m.setQuantity(o.getQuantity());
    m.setBatchCode(o.getBatchCode());
    m.setIncludeReplenishment(o.getIncludeReplenishment());
    return m;
  }

  static boolean sameRequest(Request r, OrderInput o, MesLocalTestRunManifestDO saved) {
    var desired = manifest(r, o);
    desired.setWorkOrderId(saved.getWorkOrderId());
    return MesLocalTestRunInputHash.hash(desired).equals(saved.getInputHash());
  }

  static boolean sameOrder(MesLocalTestRunManifestDO m, MesProWorkOrderDO o) {
    return o != null
        && Objects.equals(m.getTenantId(), o.getTenantId())
        && Objects.equals(m.getWorkOrderId(), o.getId())
        && Objects.equals(m.getMesProductId(), o.getProductId())
        && Objects.equals(marker(m), o.getOrderSourceCode())
        && Objects.equals("LT-" + m.getRunId() + "-" + m.getSlot(), o.getCode())
        && Objects.equals(m.getBatchCode(), o.getBatchCode())
        && o.getQuantity() != null
        && m.getQuantity().compareTo(o.getQuantity()) == 0;
  }

  public static String marker(MesLocalTestRunManifestDO m) {
    return SOURCE + ":" + m.getId();
  }

  private static String code(Request r, OrderInput o) {
    return "LT-" + r.getRunId() + "-" + o.getSlot();
  }

  private static OrderResult result(
      MesLocalTestRunManifestDO m, MesProWorkOrderDO o, boolean reused) {
    return new OrderResult(m.getSlot(), m.getId(), o.getId(), o.getCode(), o.getStatus(), reused);
  }

  private static Check check(String c, String m, String p, boolean r) {
    return new Check(c, m, p, r);
  }

  private static Preflight response(List<Check> c, List<OrderResult> o) {
    var missing = c.stream().filter(x -> !x.ready()).toList();
    return new Preflight(missing.isEmpty(), List.copyOf(c), missing, List.copyOf(o));
  }

  private static Long tenant() {
    return TenantContextHolder.getRequiredTenantId();
  }

  private static RuntimeException blocked(String s) {
    return exception(PRO_PROCESS_POOL_SIMULATION_COPY_CLEANUP_BLOCKED, s);
  }

  private static void must(int count) {
    if (count != 1) throw blocked("本地测试输入写入失败，整批回滚");
  }

  private static boolean fits(BigDecimal q) {
    return q.signum() > 0 && q.stripTrailingZeros().scale() <= 6 && q.precision() - q.scale() <= 12;
  }

  static void validate(Request r) {
    if (r == null
        || !SOURCE.equals(r.getSource())
        || r.getRunId() == null
        || !r.getRunId().matches("[A-Za-z0-9][A-Za-z0-9_-]{2,39}")
        || r.getRequestId() == null
        || r.getProjectId() == null
        || r.getCatalogId() == null
        || r.getRelationId() == null
        || r.getOrders() == null
        || r.getOrders().size() != 2) throw blocked("请选择正式批准产品并填写RUN及A/B两项输入");
    Set<String> slots = new HashSet<>(), batches = new HashSet<>();
    for (var o : r.getOrders())
      if (o == null
          || o.getSlot() == null
          || !Set.of("A", "B").contains(o.getSlot())
          || !slots.add(o.getSlot())
          || o.getQuantity() == null
          || (!fits(o.getQuantity()) || o.getQuantity().stripTrailingZeros().scale() > 2)
          || o.getBatchCode() == null
          || o.getBatchCode().isBlank()
          || o.getBatchCode().length() > 64
          || !batches.add(o.getBatchCode())
          || o.getIncludeReplenishment() == null)
        throw blocked("A/B槽位必须唯一、数量为正且最多两位小数和12位整数、批号不同且不超过64字、明确选择补料");
  }
}
