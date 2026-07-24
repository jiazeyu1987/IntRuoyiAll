package cn.iocoder.yudao.module.srm.dal.mysql.framework;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.srm.dal.dataobject.framework.SrmFrameworkAgreementLineDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SrmFrameworkAgreementLineMapper extends BaseMapperX<SrmFrameworkAgreementLineDO> {

    default List<SrmFrameworkAgreementLineDO> selectListByAgreementId(Long agreementId) {
        return selectList(new LambdaQueryWrapperX<SrmFrameworkAgreementLineDO>()
                .eq(SrmFrameworkAgreementLineDO::getAgreementId, agreementId)
                .orderByAsc(SrmFrameworkAgreementLineDO::getId));
    }
}
