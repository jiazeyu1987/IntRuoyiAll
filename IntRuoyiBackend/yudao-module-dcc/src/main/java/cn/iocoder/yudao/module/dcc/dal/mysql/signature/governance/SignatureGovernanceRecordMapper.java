package cn.iocoder.yudao.module.dcc.dal.mysql.signature.governance;

import cn.iocoder.yudao.module.dcc.controller.admin.signature.governance.vo.SignatureGovernanceRecordPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.governance.vo.SignatureGovernanceRecordRespVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SignatureGovernanceRecordMapper {

    IPage<SignatureGovernanceRecordRespVO> selectSignatureRecordPage(
            IPage<SignatureGovernanceRecordRespVO> page,
            @Param("reqVO") SignatureGovernanceRecordPageReqVO reqVO);

    SignatureGovernanceRecordRespVO selectSignatureRecordByGlobalId(@Param("globalId") String globalId);

}
