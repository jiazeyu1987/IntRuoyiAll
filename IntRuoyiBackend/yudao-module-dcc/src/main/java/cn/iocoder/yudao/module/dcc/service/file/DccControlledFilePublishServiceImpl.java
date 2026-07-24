package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.BusinessActionContextReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormInstanceCreateReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormInstanceRespVO;
import cn.iocoder.yudao.module.bpm.controller.admin.formcenter.vo.FormInstanceSubmitReqVO;
import cn.iocoder.yudao.module.bpm.formcenter.runtime.FormCenterRuntimeService;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePublishReqVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_NOT_EXISTS;

@Service
@Validated
public class DccControlledFilePublishServiceImpl implements DccControlledFilePublishService {

    @Resource
    private DccControlledFileMapper controlledFileMapper;
    @Resource
    private DccControlledFileFinalizationService finalizationService;
    @Resource
    private FormCenterRuntimeService formCenterRuntimeService;
    @Resource
    private DccControlledFileApprovalRouteAssigneeResolver approvalRouteAssigneeResolver;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FormInstanceRespVO publishControlledFile(Long userId, Long id, DccControlledFilePublishReqVO reqVO) {
        DccControlledFileDO file = requirePublishRequest(userId, id, reqVO);
        Map<String, Object> formData = buildPublishFormData(file, reqVO);
        FormInstanceCreateReqVO createReqVO = new FormInstanceCreateReqVO();
        createReqVO.setContext(buildPublishContext(file, reqVO));
        createReqVO.setIdempotencyKey(reqVO.getIdempotencyKey());
        createReqVO.setFormData(formData);
        FormInstanceRespVO draft = formCenterRuntimeService.createInstance(createReqVO, userId);

        FormInstanceSubmitReqVO submitReqVO = new FormInstanceSubmitReqVO();
        submitReqVO.setFormData(formData);
        Map<String, List<Long>> startUserSelectAssignees = reqVO.getStartUserSelectAssignees();
        if (startUserSelectAssignees == null || startUserSelectAssignees.isEmpty()) {
            startUserSelectAssignees = approvalRouteAssigneeResolver.resolveStartUserSelectAssignees(file, userId);
        }
        submitReqVO.setStartUserSelectAssignees(startUserSelectAssignees);
        return formCenterRuntimeService.submitInstance(draft.getId(), submitReqVO, userId);
    }

    private DccControlledFileDO requirePublishRequest(Long userId, Long id, DccControlledFilePublishReqVO reqVO) {
        if (reqVO == null || StrUtil.isBlank(reqVO.getReason())) {
            throw new IllegalArgumentException("DCC publish reason is required");
        }
        if (StrUtil.isBlank(reqVO.getIdempotencyKey())) {
            throw new IllegalArgumentException("DCC publish idempotencyKey is required");
        }
        DccControlledFileDO file = controlledFileMapper.selectById(id);
        if (file == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        finalizationService.precheckPublishControlledFile(userId, id);
        return file;
    }

    private BusinessActionContextReqVO buildPublishContext(DccControlledFileDO file,
            DccControlledFilePublishReqVO reqVO) {
        BusinessActionContextReqVO context = new BusinessActionContextReqVO();
        context.setDataDomain("DCC");
        context.setSystemCode("DCC");
        context.setObjectType("CONTROLLED_FILE");
        context.setObjectId(String.valueOf(file.getId()));
        context.setObjectVersion(file.getVersionNo());
        context.setActionCode("PUBLISH");
        context.setObjectState(file.getStatus());
        context.setProductCode(file.getProductCode());
        context.setCategoryCode(file.getCategoryId() == null ? null : String.valueOf(file.getCategoryId()));
        context.setReason(reqVO.getReason());
        return context;
    }

    private Map<String, Object> buildPublishFormData(DccControlledFileDO file, DccControlledFilePublishReqVO reqVO) {
        Map<String, Object> formData = new LinkedHashMap<>();
        formData.put("controlledFileId", file.getId());
        formData.put("reason", reqVO.getReason());
        return formData;
    }
}
