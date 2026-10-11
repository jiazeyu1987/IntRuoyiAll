package cn.iocoder.yudao.module.bpm.dal.mysql.approval;

import cn.iocoder.yudao.module.bpm.dal.dataobject.approval.BpmApprovalProcessOwnershipDO;
import org.apache.ibatis.annotations.*;

@Mapper
@com.baomidou.mybatisplus.annotation.InterceptorIgnore(tenantLine="true")
public interface BpmApprovalProcessOwnershipMapper {
    @Select("SELECT * FROM bpm_approval_process_ownership WHERE tenant_id=#{tenant} AND process_instance_id=#{process}")
    BpmApprovalProcessOwnershipDO selectExact(@Param("tenant") Long tenant, @Param("process") String process);
    @Insert("INSERT INTO bpm_approval_process_ownership(tenant_id,process_instance_id,process_definition_id,process_definition_key,business_key,projection_code,authority_type,authority_id,authority_version,created_at,writer_code,evidence_digest) VALUES(#{tenantId},#{processInstanceId},#{processDefinitionId},#{processDefinitionKey},#{businessKey},#{projectionCode},#{authorityType},#{authorityId},#{authorityVersion},#{createdAt},#{writerCode},#{evidenceDigest})")
    @Options(useGeneratedKeys=true, keyProperty="id")
    int insert(BpmApprovalProcessOwnershipDO fact);
    @Insert("INSERT INTO bpm_approval_selected_candidate(tenant_id,process_instance_id,user_id,selection_kind) VALUES(#{tenant},#{process},#{user},'REGISTRATION_CERTIFICATE')")
    int insertSelectedCandidate(@Param("tenant") Long tenant, @Param("process") String process, @Param("user") Long user);
    @Select("SELECT user_id FROM bpm_approval_selected_candidate WHERE tenant_id=#{tenant} AND process_instance_id=#{process} AND selection_kind='REGISTRATION_CERTIFICATE' ORDER BY user_id")
    java.util.List<Long> selectSelectedCandidates(@Param("tenant") Long tenant,@Param("process") String process);
    @Select("SELECT r.id request_id,r.request_key,b.business_key,b.detail_json FROM dcc_registration_certificate_bpm_binding b JOIN dcc_registration_certificate_access_request r ON r.tenant_id=b.tenant_id AND r.id=b.request_id AND r.bpm_process_instance_id=b.bpm_process_instance_id WHERE b.tenant_id=#{tenant} AND b.bpm_process_instance_id=#{process} AND b.deleted=0 AND r.deleted=0")
    java.util.List<cn.iocoder.yudao.module.bpm.dal.dataobject.approval.BpmApprovalRegistrationAnchorDO> selectRegistrationAnchors(
            @Param("tenant") Long tenant,@Param("process") String process);
}
