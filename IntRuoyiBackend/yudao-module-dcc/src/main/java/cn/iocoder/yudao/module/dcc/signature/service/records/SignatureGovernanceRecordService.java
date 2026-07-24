package cn.iocoder.yudao.module.dcc.signature.service.records;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.governance.vo.SignatureGovernanceRecordPageReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.signature.governance.vo.SignatureGovernanceRecordRespVO;

public interface SignatureGovernanceRecordService {

    PageResult<SignatureGovernanceRecordRespVO> getPage(SignatureGovernanceRecordPageReqVO reqVO);

    SignatureGovernanceRecordPdfArtifact exportRecordPdf(String globalId);

}
