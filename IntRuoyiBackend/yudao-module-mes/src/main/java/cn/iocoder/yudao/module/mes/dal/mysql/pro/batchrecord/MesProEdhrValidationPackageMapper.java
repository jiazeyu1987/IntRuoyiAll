package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrValidationPackagePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrValidationPackageDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MesProEdhrValidationPackageMapper extends BaseMapperX<MesProEdhrValidationPackageDO> {

    default PageResult<MesProEdhrValidationPackageDO> selectPage(MesProEdhrValidationPackagePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProEdhrValidationPackageDO>()
                .likeIfPresent(MesProEdhrValidationPackageDO::getPackageCode, reqVO.getPackageCode())
                .likeIfPresent(MesProEdhrValidationPackageDO::getPackageName, reqVO.getPackageName())
                .likeIfPresent(MesProEdhrValidationPackageDO::getCustomerProjectName, reqVO.getCustomerProjectName())
                .eqIfPresent(MesProEdhrValidationPackageDO::getValidationStatus, reqVO.getValidationStatus())
                .orderByDesc(MesProEdhrValidationPackageDO::getId));
    }
}
