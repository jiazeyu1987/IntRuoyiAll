package cn.iocoder.yudao.module.mes.dal.mysql.qa.regulation;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.qa.regulation.MesQaInspectionRegulationDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QA_INSPECTION_REGULATION_SNAPSHOT_INVALID;

@Mapper
public interface MesQaInspectionRegulationMapper extends BaseMapperX<MesQaInspectionRegulationDO> {

    default MesQaInspectionRegulationDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<MesQaInspectionRegulationDO>()
                .eq(MesQaInspectionRegulationDO::getId, id).last("FOR UPDATE"));
    }

    default MesQaInspectionRegulationDO selectByDccProjectCodeId(Long dccProjectCodeId) {
        return selectByDccProjectCodeId(dccProjectCodeId, MesQaInspectionRegulationDO.OWNER_MODULE_MES_QA);
    }

    default MesQaInspectionRegulationDO selectByDccProjectCodeId(Long dccProjectCodeId, String ownerModule) {
        return selectOne(new LambdaQueryWrapperX<MesQaInspectionRegulationDO>()
                .eq(MesQaInspectionRegulationDO::getDccProjectCodeId, dccProjectCodeId)
                .eq(MesQaInspectionRegulationDO::getOwnerModule, ownerModule));
    }

    default List<MesQaInspectionRegulationDO> selectListByDccProjectCodeIds(
            Collection<Long> dccProjectCodeIds) {
        if (dccProjectCodeIds == null || dccProjectCodeIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<MesQaInspectionRegulationDO>()
                .in(MesQaInspectionRegulationDO::getDccProjectCodeId, dccProjectCodeIds)
                .eq(MesQaInspectionRegulationDO::getOwnerModule,
                        MesQaInspectionRegulationDO.OWNER_MODULE_MES_QA)
                .orderByAsc(MesQaInspectionRegulationDO::getDccProjectCodeId)
                .orderByDesc(MesQaInspectionRegulationDO::getId));
    }

    default MesQaInspectionRegulationDO selectCommonByCode(String regulationCode) {
        List<MesQaInspectionRegulationDO> matches = selectList(new LambdaQueryWrapperX<MesQaInspectionRegulationDO>()
                .eq(MesQaInspectionRegulationDO::getOwnerModule,
                        MesQaInspectionRegulationDO.OWNER_MODULE_MES_QA_COMMON)
                .eq(MesQaInspectionRegulationDO::getRegulationCode, regulationCode));
        if (matches.size() > 1) {
            throw exception(QA_INSPECTION_REGULATION_SNAPSHOT_INVALID,
                    "文件编号 " + regulationCode + " 对应多份通用规程，请先确认重复记录，规程ID："
                            + matches.stream().map(MesQaInspectionRegulationDO::getId).toList());
        }
        return matches.isEmpty() ? null : matches.get(0);
    }

    default List<MesQaInspectionRegulationDO> selectCommonList() {
        return selectList(new LambdaQueryWrapperX<MesQaInspectionRegulationDO>()
                .eq(MesQaInspectionRegulationDO::getOwnerModule,
                        MesQaInspectionRegulationDO.OWNER_MODULE_MES_QA_COMMON)
                .orderByAsc(MesQaInspectionRegulationDO::getDccProjectCodeId)
                .orderByDesc(MesQaInspectionRegulationDO::getId));
    }
}
