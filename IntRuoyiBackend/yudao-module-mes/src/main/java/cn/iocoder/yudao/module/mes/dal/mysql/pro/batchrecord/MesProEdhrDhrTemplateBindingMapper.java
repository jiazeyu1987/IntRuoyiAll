package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrDhrTemplateBindingDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MesProEdhrDhrTemplateBindingMapper extends BaseMapperX<MesProEdhrDhrTemplateBindingDO> {

    default List<MesProEdhrDhrTemplateBindingDO> selectListByTemplateId(Long templateId) {
        return selectList(new LambdaQueryWrapperX<MesProEdhrDhrTemplateBindingDO>()
                .eq(MesProEdhrDhrTemplateBindingDO::getTemplateId, templateId)
                .orderByAsc(MesProEdhrDhrTemplateBindingDO::getBindingType));
    }
}
