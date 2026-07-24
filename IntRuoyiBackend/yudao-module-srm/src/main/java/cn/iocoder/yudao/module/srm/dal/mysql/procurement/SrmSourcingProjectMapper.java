package cn.iocoder.yudao.module.srm.dal.mysql.procurement;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.srm.controller.admin.nonbidding.vo.SrmNonBiddingProjectPageReqVO;
import cn.iocoder.yudao.module.srm.controller.admin.tender.vo.SrmTenderProjectPageReqVO;
import cn.iocoder.yudao.module.srm.dal.dataobject.procurement.SrmSourcingProjectDO;
import cn.iocoder.yudao.module.srm.enums.procurement.SrmProcurementMethodEnum;
import cn.iocoder.yudao.module.srm.enums.procurement.SrmSourcingProjectStatusEnum;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmSourcingProjectMapper extends BaseMapperX<SrmSourcingProjectDO> {

    default SrmSourcingProjectDO selectBySourcePlanId(Long sourcePlanId) {
        return selectOne(new LambdaQueryWrapperX<SrmSourcingProjectDO>()
                .eq(SrmSourcingProjectDO::getSourcePlanId, sourcePlanId)
                .last("LIMIT 1"));
    }

    default PageResult<SrmSourcingProjectDO> selectNonBiddingPage(SrmNonBiddingProjectPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SrmSourcingProjectDO>()
                .eq(SrmSourcingProjectDO::getProjectType, SrmProcurementMethodEnum.NON_BIDDING.getMethod())
                .likeIfPresent(SrmSourcingProjectDO::getProjectNo, reqVO.getProjectNo())
                .likeIfPresent(SrmSourcingProjectDO::getProjectTitle, reqVO.getProjectTitle())
                .eqIfPresent(SrmSourcingProjectDO::getProjectStatus, reqVO.getProjectStatus())
                .eqIfPresent(SrmSourcingProjectDO::getDealSupplierId, reqVO.getSupplierId())
                .orderByDesc(SrmSourcingProjectDO::getId));
    }

    default PageResult<SrmSourcingProjectDO> selectContractableNonBiddingPage(SrmNonBiddingProjectPageReqVO reqVO) {
        LambdaQueryWrapperX<SrmSourcingProjectDO> wrapper = new LambdaQueryWrapperX<SrmSourcingProjectDO>()
                .eq(SrmSourcingProjectDO::getProjectType, SrmProcurementMethodEnum.NON_BIDDING.getMethod())
                .eq(SrmSourcingProjectDO::getProjectStatus, SrmSourcingProjectStatusEnum.DEAL_CONFIRMED.getStatus());
        wrapper.isNull(SrmSourcingProjectDO::getContractId);
        return selectPage(reqVO, wrapper
                .likeIfPresent(SrmSourcingProjectDO::getProjectNo, reqVO.getProjectNo())
                .likeIfPresent(SrmSourcingProjectDO::getProjectTitle, reqVO.getProjectTitle())
                .eqIfPresent(SrmSourcingProjectDO::getDealSupplierId, reqVO.getSupplierId())
                .orderByDesc(SrmSourcingProjectDO::getId));
    }

    default PageResult<SrmSourcingProjectDO> selectTenderPage(SrmTenderProjectPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SrmSourcingProjectDO>()
                .eq(SrmSourcingProjectDO::getProjectType, SrmProcurementMethodEnum.TENDER.getMethod())
                .likeIfPresent(SrmSourcingProjectDO::getProjectNo, reqVO.getProjectNo())
                .likeIfPresent(SrmSourcingProjectDO::getProjectTitle, reqVO.getProjectTitle())
                .eqIfPresent(SrmSourcingProjectDO::getProjectStatus, reqVO.getProjectStatus())
                .eqIfPresent(SrmSourcingProjectDO::getDealSupplierId, reqVO.getSupplierId())
                .orderByDesc(SrmSourcingProjectDO::getId));
    }

    default void clearContractAndRestoreStatus(Long id, String projectStatus) {
        update(null, new LambdaUpdateWrapper<SrmSourcingProjectDO>()
                .eq(SrmSourcingProjectDO::getId, id)
                .set(SrmSourcingProjectDO::getContractId, null)
                .set(SrmSourcingProjectDO::getProjectStatus, projectStatus));
    }
}
