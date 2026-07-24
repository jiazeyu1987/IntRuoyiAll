package cn.iocoder.yudao.module.mes.dal.mysql.md.autocode;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.autocode.MesMdAutoCodeRecordDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;

/**
 * MES 编码生成记录 Mapper
 *
 * @author 瑛泰源码
 */
@Mapper
public interface MesMdAutoCodeRecordMapper extends BaseMapperX<MesMdAutoCodeRecordDO> {

    default MesMdAutoCodeRecordDO selectByResult(String result) {
        return selectOne(MesMdAutoCodeRecordDO::getResult, result);
    }

    default MesMdAutoCodeRecordDO selectLatestSerialRecord(Long ruleId, String inputChar, boolean filterByInputChar,
                                                           LocalDateTime cycleStartTime, LocalDateTime cycleEndTime) {
        LambdaQueryWrapperX<MesMdAutoCodeRecordDO> query = new LambdaQueryWrapperX<MesMdAutoCodeRecordDO>()
                .eq(MesMdAutoCodeRecordDO::getRuleId, ruleId);
        query.isNotNull(MesMdAutoCodeRecordDO::getSerialNo);
        query.orderByDesc(MesMdAutoCodeRecordDO::getSerialNo).last("LIMIT 1");
        if (filterByInputChar) {
            if (inputChar == null) {
                query.isNull(MesMdAutoCodeRecordDO::getInputChar);
            } else {
                query.eq(MesMdAutoCodeRecordDO::getInputChar, inputChar);
            }
        }
        if (cycleStartTime != null) {
            query.ge(MesMdAutoCodeRecordDO::getCreateTime, cycleStartTime);
        }
        if (cycleEndTime != null) {
            query.lt(MesMdAutoCodeRecordDO::getCreateTime, cycleEndTime);
        }
        return selectOne(query);
    }

}
