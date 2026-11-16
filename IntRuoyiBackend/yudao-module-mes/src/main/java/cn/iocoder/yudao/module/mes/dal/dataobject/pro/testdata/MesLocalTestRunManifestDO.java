package cn.iocoder.yudao.module.mes.dal.dataobject.pro.testdata;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;
import lombok.*;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("mes_local_test_run_manifest")
public class MesLocalTestRunManifestDO extends TenantBaseDO {
  @TableId private Long id;
  private String source;
  private String runId;
  private String slot;
  private Long workOrderId;
  private Long requestId;
  private Long projectId;
  private Long catalogId;
  private Long relationId;
  private Long mesProductId;
  private BigDecimal quantity;
  private String batchCode;
  private Boolean includeReplenishment;
  private Long pickHeaderId;
  private String pickItemIdsJson;
  private Long replenishmentHeaderId;
  private String replenishmentItemIdsJson;
  private String materialItemIdsJson;
  private String materialSnapshotHash;
  private String inputHash;
  private String pickSnapshotHash;
  private String replenishmentSnapshotHash;
}
