package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrValidationRequirementItemPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrValidationRequirementItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MesProEdhrValidationRequirementItemMapper extends BaseMapperX<MesProEdhrValidationRequirementItemDO> {

    default PageResult<MesProEdhrValidationRequirementItemDO> selectPage(MesProEdhrValidationRequirementItemPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<MesProEdhrValidationRequirementItemDO>()
                .eq(MesProEdhrValidationRequirementItemDO::getPackageId, reqVO.getPackageId())
                .eqIfPresent(MesProEdhrValidationRequirementItemDO::getItemType, reqVO.getItemType())
                .eqIfPresent(MesProEdhrValidationRequirementItemDO::getItemStatus, reqVO.getItemStatus())
                .likeIfPresent(MesProEdhrValidationRequirementItemDO::getItemCode, reqVO.getItemCode())
                .orderByAsc(MesProEdhrValidationRequirementItemDO::getSort)
                .orderByAsc(MesProEdhrValidationRequirementItemDO::getId));
    }

    default List<MesProEdhrValidationRequirementItemDO> selectListByPackageId(Long packageId) {
        return selectList(new LambdaQueryWrapperX<MesProEdhrValidationRequirementItemDO>()
                .eq(MesProEdhrValidationRequirementItemDO::getPackageId, packageId)
                .orderByAsc(MesProEdhrValidationRequirementItemDO::getSort)
                .orderByAsc(MesProEdhrValidationRequirementItemDO::getId));
    }
}
