package cn.iocoder.yudao.module.srm.dal.mysql.contract;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.srm.controller.admin.contract.vo.SrmProcurementContractPageReqVO;
import cn.iocoder.yudao.module.srm.dal.dataobject.contract.SrmProcurementContractDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SrmProcurementContractMapper extends BaseMapperX<SrmProcurementContractDO> {

    default SrmProcurementContractDO selectEffectiveBySource(String sourceType, Long sourceId) {
        return selectOne(new LambdaQueryWrapperX<SrmProcurementContractDO>()
                .eq(SrmProcurementContractDO::getSourceType, sourceType)
                .eq(SrmProcurementContractDO::getSourceId, sourceId)
                .eq(SrmProcurementContractDO::getContractStatus, "EFFECTIVE")
                .last("LIMIT 1"));
    }

    default List<SrmProcurementContractDO> selectListBySource(String sourceType, Long sourceId) {
        return selectList(new LambdaQueryWrapperX<SrmProcurementContractDO>()
                .eq(SrmProcurementContractDO::getSourceType, sourceType)
                .eq(SrmProcurementContractDO::getSourceId, sourceId)
                .orderByDesc(SrmProcurementContractDO::getId));
    }

    default PageResult<SrmProcurementContractDO> selectPage(SrmProcurementContractPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SrmProcurementContractDO>()
                .likeIfPresent(SrmProcurementContractDO::getContractNo, reqVO.getContractNo())
                .likeIfPresent(SrmProcurementContractDO::getContractTitle, reqVO.getContractTitle())
                .eqIfPresent(SrmProcurementContractDO::getSourceType, reqVO.getSourceType())
                .eqIfPresent(SrmProcurementContractDO::getSupplierId, reqVO.getSupplierId())
                .eqIfPresent(SrmProcurementContractDO::getContractStatus, reqVO.getContractStatus())
                .orderByDesc(SrmProcurementContractDO::getId));
    }
}
