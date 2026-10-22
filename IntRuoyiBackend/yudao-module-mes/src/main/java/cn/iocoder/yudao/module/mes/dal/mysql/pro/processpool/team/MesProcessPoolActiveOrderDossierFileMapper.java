package cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDossierFileDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MesProcessPoolActiveOrderDossierFileMapper
        extends BaseMapperX<MesProcessPoolActiveOrderDossierFileDO> {

    default List<MesProcessPoolActiveOrderDossierFileDO> selectListByActiveOrderId(Long activeOrderId) {
        if (activeOrderId == null) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<MesProcessPoolActiveOrderDossierFileDO>()
                .eq(MesProcessPoolActiveOrderDossierFileDO::getActiveOrderId, activeOrderId)
                .orderByAsc(MesProcessPoolActiveOrderDossierFileDO::getCategoryKey)
                .orderByAsc(MesProcessPoolActiveOrderDossierFileDO::getId));
    }

    default List<MesProcessPoolActiveOrderDossierFileDO> selectListByFileId(Long fileId) {
        if (fileId == null) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<MesProcessPoolActiveOrderDossierFileDO>()
                .eq(MesProcessPoolActiveOrderDossierFileDO::getFileId, fileId)
                .orderByAsc(MesProcessPoolActiveOrderDossierFileDO::getId));
    }

    default List<MesProcessPoolActiveOrderDossierFileDO> selectListByActiveOrderIds(
            List<Long> activeOrderIds) {
        if (activeOrderIds == null || activeOrderIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<MesProcessPoolActiveOrderDossierFileDO>()
                .in(MesProcessPoolActiveOrderDossierFileDO::getActiveOrderId, activeOrderIds)
                .orderByAsc(MesProcessPoolActiveOrderDossierFileDO::getActiveOrderId)
                .orderByAsc(MesProcessPoolActiveOrderDossierFileDO::getId));
    }
}
