package cn.iocoder.yudao.module.mes.service.pro.testdata;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.testdata.MesLocalTestRunManifestDO;
import java.util.TreeMap;

/**
 * Stable immutable input identity; independent of mutable live definitions and production state.
 */
public final class MesLocalTestRunInputHash {
  private MesLocalTestRunInputHash() {}

  public static String hash(MesLocalTestRunManifestDO m) {
    var v = new TreeMap<String, Object>();
    v.put("tenantId", m.getTenantId());
    v.put("runId", m.getRunId());
    v.put("slot", m.getSlot());
    v.put("workOrderId", m.getWorkOrderId());
    v.put("requestId", m.getRequestId());
    v.put("projectId", m.getProjectId());
    v.put("catalogId", m.getCatalogId());
    v.put("relationId", m.getRelationId());
    v.put("mesProductId", m.getMesProductId());
    v.put("quantity", m.getQuantity().stripTrailingZeros().toPlainString());
    v.put("batchCode", m.getBatchCode());
    v.put("includeReplenishment", m.getIncludeReplenishment());
    v.put("source", m.getSource());
    return DigestUtil.sha256Hex(JsonUtils.toJsonString(v));
  }
}
