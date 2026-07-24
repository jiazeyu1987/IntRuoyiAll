package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecordreport;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecordreport.MesProBatchRecordDefinitionDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface MesProBatchRecordDefinitionMapper extends BaseMapperX<MesProBatchRecordDefinitionDO> {

    default MesProBatchRecordDefinitionDO selectByNameAndRouteKey(String batchRecordName, String routeKey) {
        return selectOne(new LambdaQueryWrapperX<MesProBatchRecordDefinitionDO>()
                .eq(MesProBatchRecordDefinitionDO::getBatchRecordName, batchRecordName)
                .eq(MesProBatchRecordDefinitionDO::getRouteKey, routeKey));
    }

    default List<MesProBatchRecordDefinitionDO> selectListByBatchRecordName(String batchRecordName) {
        return selectList(new LambdaQueryWrapperX<MesProBatchRecordDefinitionDO>()
                .eq(MesProBatchRecordDefinitionDO::getBatchRecordName, batchRecordName)
                .orderByAsc(MesProBatchRecordDefinitionDO::getId));
    }

    @Select("SELECT * FROM mes_pro_batch_record_definition WHERE id = #{id} FOR UPDATE")
    MesProBatchRecordDefinitionDO selectByIdForUpdate(@Param("id") Long id);

    @Update("UPDATE mes_pro_batch_record_definition "
            + "SET current_version_id = #{targetVersionId}, update_time = CURRENT_TIMESTAMP "
            + "WHERE id = #{definitionId} AND "
            + "((current_version_id IS NULL AND #{expectedCurrentVersionId} IS NULL) "
            + "OR current_version_id = #{expectedCurrentVersionId})")
    int updateCurrentVersionIfMatch(@Param("definitionId") Long definitionId,
                                    @Param("expectedCurrentVersionId") Long expectedCurrentVersionId,
                                    @Param("targetVersionId") Long targetVersionId);

    @Delete("DELETE FROM mes_pro_batch_record_definition WHERE id = #{id}")
    int deleteHardById(@Param("id") Long id);
}
