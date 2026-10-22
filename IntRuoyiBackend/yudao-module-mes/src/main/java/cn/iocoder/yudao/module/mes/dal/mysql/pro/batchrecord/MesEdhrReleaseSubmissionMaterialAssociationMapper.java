package cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesEdhrReleaseSubmissionMaterialAssociationDO;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface MesEdhrReleaseSubmissionMaterialAssociationMapper extends BaseMapperX<MesEdhrReleaseSubmissionMaterialAssociationDO> {
    @Select("SELECT * FROM mes_edhr_release_submission_material_association WHERE tenant_id=#{tenant} AND release_transaction_id=#{releaseId} AND submission_key_digest=#{digest}")
    List<MesEdhrReleaseSubmissionMaterialAssociationDO> selectBySubmission(@Param("tenant") Long tenant,
            @Param("releaseId") Long releaseId, @Param("digest") String digest);
    @Select("SELECT * FROM mes_edhr_release_submission_material_association WHERE tenant_id=#{tenant} AND approval_work_task_id=#{taskId}")
    List<MesEdhrReleaseSubmissionMaterialAssociationDO> selectByTask(@Param("tenant") Long tenant, @Param("taskId") Long taskId);
}
