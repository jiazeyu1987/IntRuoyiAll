package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordTemplateDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MesProBatchRecordTemplateMapper extends BaseMapperX<MesProBatchRecordTemplateDO> {

    default MesProBatchRecordTemplateDO selectLatestByProcessId(Long processId) {
        if (processId == null) {
            return null;
        }
        List<MesProBatchRecordTemplateDO> list = selectList(new LambdaQueryWrapperX<MesProBatchRecordTemplateDO>()
                .eq(MesProBatchRecordTemplateDO::getProcessId, processId)
                .orderByDesc(MesProBatchRecordTemplateDO::getId));
        return list.isEmpty() ? null : list.get(0);
    }
}
