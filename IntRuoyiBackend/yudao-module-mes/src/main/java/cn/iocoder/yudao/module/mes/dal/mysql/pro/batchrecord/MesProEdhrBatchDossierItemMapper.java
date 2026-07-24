package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrBatchDossierItemDO;
import cn.iocoder.yudao.module.mes.enums.pro.MesProEdhrDossierConstants;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MesProEdhrBatchDossierItemMapper extends BaseMapperX<MesProEdhrBatchDossierItemDO> {

    default List<MesProEdhrBatchDossierItemDO> selectListByBatchExecutionId(Long batchExecutionId) {
        return selectList(new LambdaQueryWrapperX<MesProEdhrBatchDossierItemDO>()
                .eq(MesProEdhrBatchDossierItemDO::getBatchExecutionId, batchExecutionId)
                .orderByAsc(MesProEdhrBatchDossierItemDO::getItemType)
                .orderByAsc(MesProEdhrBatchDossierItemDO::getItemKey)
                .orderByAsc(MesProEdhrBatchDossierItemDO::getId));
    }

    default MesProEdhrBatchDossierItemDO selectRequiredFinalInspection(Long batchExecutionId) {
        return selectOne(new LambdaQueryWrapperX<MesProEdhrBatchDossierItemDO>()
                .eq(MesProEdhrBatchDossierItemDO::getBatchExecutionId, batchExecutionId)
                .eq(MesProEdhrBatchDossierItemDO::getItemType,
                        MesProEdhrDossierConstants.ITEM_TYPE_FINAL_INSPECTION)
                .eq(MesProEdhrBatchDossierItemDO::getItemKey,
                        MesProEdhrDossierConstants.ITEM_KEY_FINAL_INSPECTION)
                .eq(MesProEdhrBatchDossierItemDO::getRequiredFlag, Boolean.TRUE));
    }

    default int updateFinalInspectionFromOqc(MesProEdhrBatchDossierItemDO item) {
        return update(new LambdaUpdateWrapper<MesProEdhrBatchDossierItemDO>()
                .eq(MesProEdhrBatchDossierItemDO::getId, item.getId())
                .set(MesProEdhrBatchDossierItemDO::getItemStatus, item.getItemStatus())
                .set(MesProEdhrBatchDossierItemDO::getSourceDocType, item.getSourceDocType())
                .set(MesProEdhrBatchDossierItemDO::getSourceDocId, item.getSourceDocId())
                .set(MesProEdhrBatchDossierItemDO::getSourceDocCode, item.getSourceDocCode())
                .set(MesProEdhrBatchDossierItemDO::getSourceDocStatus, item.getSourceDocStatus())
                .set(MesProEdhrBatchDossierItemDO::getSourceDocResult, item.getSourceDocResult())
                .set(MesProEdhrBatchDossierItemDO::getSourceDocHash, item.getSourceDocHash())
                .set(MesProEdhrBatchDossierItemDO::getCompletedAt, item.getCompletedAt())
                .set(MesProEdhrBatchDossierItemDO::getVerifiedAt, item.getVerifiedAt())
                .set(MesProEdhrBatchDossierItemDO::getBlockerCode, item.getBlockerCode())
                .set(MesProEdhrBatchDossierItemDO::getBlockerMessage, item.getBlockerMessage()));
    }
}
