package cn.iocoder.yudao.module.signature.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.signature.dal.dataobject.ElectronicSignatureRecordDO;
import cn.iocoder.yudao.module.signature.dal.dataobject.ElectronicSignatureSealDO;
import cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureRecordMapper;
import cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureSealMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.signature.enums.SignatureErrorCodeConstants.ESIGN_COMMAND_INVALID;

@Service
public class ElectronicSignatureSealService {

    private static final String STATUS_SEALED = "SEALED";

    @Resource
    private ElectronicSignatureRecordMapper signatureRecordMapper;
    @Resource
    private ElectronicSignatureSealMapper sealMapper;

    @Transactional(rollbackFor = Exception.class)
    public Long createDailySeal(LocalDate sealDate, String wormEvidenceId) {
        if (sealDate == null || StrUtil.isBlank(wormEvidenceId)) {
            throw exception(ESIGN_COMMAND_INVALID, "封存日期和 WORM 证据编号不能为空");
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        LocalDateTime startAt = sealDate.atStartOfDay();
        LocalDateTime endAt = sealDate.plusDays(1).atStartOfDay();
        List<ElectronicSignatureRecordDO> records = signatureRecordMapper.selectList(
                new LambdaQueryWrapper<ElectronicSignatureRecordDO>()
                        .eq(ElectronicSignatureRecordDO::getTenantId, tenantId)
                        .ge(ElectronicSignatureRecordDO::getSignedAt, startAt)
                        .lt(ElectronicSignatureRecordDO::getSignedAt, endAt)
                        .orderByAsc(ElectronicSignatureRecordDO::getId));
        if (CollUtil.isEmpty(records)) {
            throw exception(ESIGN_COMMAND_INVALID, "封存日期内不存在电子签名记录");
        }
        String previousSealHash = selectPreviousSealHash(tenantId, sealDate);
        ElectronicSignatureRecordDO first = records.get(0);
        ElectronicSignatureRecordDO last = records.get(records.size() - 1);
        String recordEvidenceChain = records.stream()
                .map(record -> record.getId() + ":" + record.getEvidenceHash())
                .collect(Collectors.joining(","));
        String sealHash = DigestUtil.sha256Hex(String.join("|", String.valueOf(tenantId), sealDate.toString(),
                String.valueOf(records.size()), String.valueOf(first.getId()), String.valueOf(last.getId()),
                StrUtil.nullToEmpty(previousSealHash), wormEvidenceId, recordEvidenceChain));

        ElectronicSignatureSealDO seal = ElectronicSignatureSealDO.builder()
                .sealDate(sealDate)
                .recordCount(records.size())
                .firstSignatureId(first.getId())
                .lastSignatureId(last.getId())
                .previousSealHash(previousSealHash)
                .sealHash(sealHash)
                .wormEvidenceId(wormEvidenceId)
                .status(STATUS_SEALED)
                .sealedAt(LocalDateTime.now())
                .build();
        seal.setTenantId(tenantId);
        sealMapper.insert(seal);
        return seal.getId();
    }

    private String selectPreviousSealHash(Long tenantId, LocalDate sealDate) {
        return sealMapper.selectList(new LambdaQueryWrapper<ElectronicSignatureSealDO>()
                        .eq(ElectronicSignatureSealDO::getTenantId, tenantId)
                        .lt(ElectronicSignatureSealDO::getSealDate, sealDate))
                .stream()
                .max(Comparator.comparing(ElectronicSignatureSealDO::getSealDate)
                        .thenComparing(ElectronicSignatureSealDO::getId))
                .map(ElectronicSignatureSealDO::getSealHash)
                .orElse(null);
    }

}
