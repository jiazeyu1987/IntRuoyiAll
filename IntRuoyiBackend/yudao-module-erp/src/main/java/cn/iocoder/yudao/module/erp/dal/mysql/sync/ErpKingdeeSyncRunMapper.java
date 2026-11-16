package cn.iocoder.yudao.module.erp.dal.mysql.sync;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.sync.vo.ErpKingdeeSyncRunPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sync.ErpKingdeeSyncRunDO;
import cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncRunStatusEnum;
import org.apache.ibatis.annotations.Mapper;
import static cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.*;

@Mapper
public interface ErpKingdeeSyncRunMapper extends BaseMapperX<ErpKingdeeSyncRunDO> {

    default ErpKingdeeSyncRunDO selectRunningBySyncType(String syncType) {
        return selectOne(new LambdaQueryWrapperX<ErpKingdeeSyncRunDO>()
                .eq(ErpKingdeeSyncRunDO::getSyncType, syncType)
                .in(ErpKingdeeSyncRunDO::getStatus, 10, 40));
    }

    default int addProductionMaterialListProgress(Long runId, int created, int updated, int skipped) {
        return update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ErpKingdeeSyncRunDO>()
                .eq(ErpKingdeeSyncRunDO::getId, runId)
                .eq(ErpKingdeeSyncRunDO::getSyncType,
                        cn.iocoder.yudao.module.erp.enums.sync.ErpKingdeeSyncTypeEnum.PRODUCTION_MATERIAL_LIST.getType())
                .eq(ErpKingdeeSyncRunDO::getStatus, ErpKingdeeSyncRunStatusEnum.RUNNING.getStatus())
                .setSql("created_count = created_count + {0}", created)
                .setSql("updated_count = updated_count + {0}", updated)
                .setSql("skipped_count = skipped_count + {0}", skipped));
    }

    default PageResult<ErpKingdeeSyncRunDO> selectPage(ErpKingdeeSyncRunPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpKingdeeSyncRunDO> query = new LambdaQueryWrapperX<ErpKingdeeSyncRunDO>()
                .eqIfPresent(ErpKingdeeSyncRunDO::getSyncType, reqVO.getSyncType())
                .betweenIfPresent(ErpKingdeeSyncRunDO::getStartedAt, reqVO.getStartedAt())
                .orderByDesc(ErpKingdeeSyncRunDO::getId);
        if (Integer.valueOf(10).equals(reqVO.getStatus())) query.in(ErpKingdeeSyncRunDO::getStatus, 10, 40);
        else query.eqIfPresent(ErpKingdeeSyncRunDO::getStatus, reqVO.getStatus());
        return selectPage(reqVO, apply(reqVO, query, listFilterFields()));
    }


    static java.util.Map<String, cn.iocoder.yudao.framework.mybatis.core.util.ListFilterQueryUtils.Field> listFilterFields() {
        return java.util.Map.ofEntries(
                java.util.Map.entry("id", number("erp_kingdee_sync_run.id")),
                java.util.Map.entry("syncType", text("erp_kingdee_sync_run.sync_type")),
                java.util.Map.entry("triggerType", text("erp_kingdee_sync_run.trigger_type")),
                java.util.Map.entry("status", number("erp_kingdee_sync_run.status")),
                java.util.Map.entry("windowStartTime", dateTime("erp_kingdee_sync_run.window_start_time")),
                java.util.Map.entry("windowEndTime", dateTime("erp_kingdee_sync_run.window_end_time")),
                java.util.Map.entry("startedAt", dateTime("erp_kingdee_sync_run.started_at")),
                java.util.Map.entry("endedAt", dateTime("erp_kingdee_sync_run.ended_at")),
                java.util.Map.entry("createdCount", number("erp_kingdee_sync_run.created_count")),
                java.util.Map.entry("updatedCount", number("erp_kingdee_sync_run.updated_count")),
                java.util.Map.entry("skippedCount", number("erp_kingdee_sync_run.skipped_count")),
                java.util.Map.entry("failedCount", number("erp_kingdee_sync_run.failed_count")),
                java.util.Map.entry("failureMessage", text("erp_kingdee_sync_run.failure_message")),
                java.util.Map.entry("createTime", dateTime("erp_kingdee_sync_run.create_time")));
    }
}
