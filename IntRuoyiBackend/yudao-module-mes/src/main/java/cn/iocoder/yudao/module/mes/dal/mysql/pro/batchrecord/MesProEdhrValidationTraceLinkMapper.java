package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrValidationTraceLinkDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MesProEdhrValidationTraceLinkMapper extends BaseMapperX<MesProEdhrValidationTraceLinkDO> {

    default List<MesProEdhrValidationTraceLinkDO> selectListByPackageId(Long packageId) {
        return selectList(new LambdaQueryWrapperX<MesProEdhrValidationTraceLinkDO>()
                .eq(MesProEdhrValidationTraceLinkDO::getPackageId, packageId)
                .orderByAsc(MesProEdhrValidationTraceLinkDO::getSourceItemId)
                .orderByAsc(MesProEdhrValidationTraceLinkDO::getTargetItemId));
    }
}
