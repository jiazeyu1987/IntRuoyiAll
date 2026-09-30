package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileNameClaimDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileNameClaimMapper;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Locale;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_NAME_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING;

/**
 * Owns the minimum tenant-wide name reservation used by DCC uploads.
 */
@Service
@Validated
public class DccControlledFileNameClaimService {

    @Resource
    private DccControlledFileNameClaimMapper claimMapper;

    public void claim(Long tenantId, String fileName, Long masterId) {
        if (tenantId == null || masterId == null || StrUtil.isBlank(fileName)) {
            throw exception(CONTROLLED_FILE_SUBMIT_REQUIRED_METADATA_MISSING);
        }
        String normalizedName = normalize(fileName);
        DccControlledFileNameClaimDO existing = claimMapper.selectActiveByName(tenantId, normalizedName);
        if (existing != null) {
            if (masterId.equals(existing.getMasterId())) {
                return;
            }
            throw exception(CONTROLLED_FILE_NAME_EXISTS);
        }
        DccControlledFileNameClaimDO claim = DccControlledFileNameClaimDO.builder()
                .tenantId(tenantId)
                .normalizedName(normalizedName)
                .masterId(masterId)
                .build();
        try {
            if (claimMapper.insert(claim) != 1) {
                throw exception(CONTROLLED_FILE_NAME_EXISTS);
            }
        } catch (DuplicateKeyException ex) {
            throw exception(CONTROLLED_FILE_NAME_EXISTS);
        }
    }

    public void release(Long tenantId, Long masterId) {
        if (tenantId == null || masterId == null) {
            return;
        }
        claimMapper.releaseByMasterId(tenantId, masterId);
    }

    static String normalize(String fileName) {
        return fileName.trim().toLowerCase(Locale.ROOT);
    }
}
