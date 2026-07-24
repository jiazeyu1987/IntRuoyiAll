package cn.iocoder.yudao.module.srm.dal.mysql.framework;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.srm.controller.admin.framework.vo.SrmFrameworkAgreementPageReqVO;
import cn.iocoder.yudao.module.srm.dal.dataobject.framework.SrmFrameworkAgreementDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmFrameworkAgreementMapper extends BaseMapperX<SrmFrameworkAgreementDO> {

    default SrmFrameworkAgreementDO selectByFrameworkPlanId(Long frameworkPlanId) {
        return selectOne(new LambdaQueryWrapperX<SrmFrameworkAgreementDO>()
                .eq(SrmFrameworkAgreementDO::getFrameworkPlanId, frameworkPlanId)
                .last("LIMIT 1"));
    }

    default PageResult<SrmFrameworkAgreementDO> selectPage(SrmFrameworkAgreementPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<SrmFrameworkAgreementDO>()
                .likeIfPresent(SrmFrameworkAgreementDO::getAgreementNo, reqVO.getAgreementNo())
                .likeIfPresent(SrmFrameworkAgreementDO::getFrameworkPlanNo, reqVO.getFrameworkPlanNo())
                .likeIfPresent(SrmFrameworkAgreementDO::getSupplierName, reqVO.getSupplierName())
                .orderByDesc(SrmFrameworkAgreementDO::getId));
    }
}
