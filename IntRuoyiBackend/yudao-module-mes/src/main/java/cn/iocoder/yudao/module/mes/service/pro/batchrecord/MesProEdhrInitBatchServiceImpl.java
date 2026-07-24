package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrInitBatchCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrInitBatchPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrInitBatchPrecheckRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrInitBatchRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrInitIssuePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrInitIssueRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrInitManifestRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.batchrecord.vo.MesProEdhrInitManifestUploadReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrInitBatchDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrInitIssueDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrInitManifestDO;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrInitBatchMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrInitIssueMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrInitManifestMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrInitBatchErrorCodeConstants.PRO_EDHR_INIT_BATCH_MANIFEST_INVALID;
import static cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrInitBatchErrorCodeConstants.PRO_EDHR_INIT_BATCH_NOT_EXISTS;

@Service
public class MesProEdhrInitBatchServiceImpl implements MesProEdhrInitBatchService {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PRECHECK_FAILED = "PRECHECK_FAILED";
    public static final String STATUS_PRECHECK_PASSED = "PRECHECK_PASSED";

    private static final String MANIFEST_STATUS_UPLOADED = "UPLOADED";

    public static final String ISSUE_LEVEL_BLOCKER = "BLOCKER";
    public static final String ISSUE_STATUS_OPEN = "OPEN";
    public static final String ISSUE_STATUS_SUPERSEDED = "SUPERSEDED";

    public static final String ISSUE_CODE_MISSING_MANIFEST = "MISSING_MANIFEST";
    private static final String ISSUE_CODE_INVALID_MANIFEST_JSON = "INVALID_MANIFEST_JSON";
    private static final String ISSUE_CODE_CUSTOMER_FIELD_DICTIONARY_MISSING = "CUSTOMER_FIELD_DICTIONARY_MISSING";
    private static final String ISSUE_CODE_TENANT_AUTHORIZATION_MISSING = "TENANT_AUTHORIZATION_MISSING";
    private static final String ISSUE_CODE_BACKUP_RESTORE_EVIDENCE_MISSING = "BACKUP_RESTORE_EVIDENCE_MISSING";
    private static final String ISSUE_CODE_CONTROLLED_FILE_LIST_MISSING = "CONTROLLED_FILE_LIST_MISSING";
    private static final String ISSUE_CODE_FILE_CHECKSUM_MISSING = "FILE_CHECKSUM_MISSING";

    @Resource
    private MesProEdhrInitBatchMapper initBatchMapper;
    @Resource
    private MesProEdhrInitManifestMapper initManifestMapper;
    @Resource
    private MesProEdhrInitIssueMapper initIssueMapper;

    @Override
    public PageResult<MesProEdhrInitBatchRespVO> getPage(MesProEdhrInitBatchPageReqVO reqVO) {
        PageResult<MesProEdhrInitBatchRespVO> page =
                BeanUtils.toBean(initBatchMapper.selectPage(reqVO), MesProEdhrInitBatchRespVO.class);
        page.getList().forEach(this::fillLatestManifestHash);
        return page;
    }

    @Override
    public MesProEdhrInitBatchRespVO get(Long id) {
        return toRespVO(validateBatchExists(id), true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrInitBatchRespVO create(MesProEdhrInitBatchCreateReqVO reqVO) {
        validateJsonObject(reqVO.getInitScopeJson(), "initScopeJson");
        MesProEdhrInitBatchDO batch = BeanUtils.toBean(reqVO, MesProEdhrInitBatchDO.class)
                .setStatus(STATUS_DRAFT)
                .setManifestCount(0)
                .setBlockingIssueCount(0)
                .setVersion(1);
        initBatchMapper.insert(batch);
        return toRespVO(batch, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrInitManifestRespVO uploadManifest(MesProEdhrInitManifestUploadReqVO reqVO) {
        MesProEdhrInitBatchDO batch = validateBatchExists(reqVO.getInitBatchId());
        String manifestHash = reqVO.getManifestHash();
        if (manifestHash.length() != 64) {
            throw exception(PRO_EDHR_INIT_BATCH_MANIFEST_INVALID, "manifestHash 必须为 64 位摘要");
        }
        MesProEdhrInitManifestDO existing = initManifestMapper.selectByBatchAndHash(batch.getId(), manifestHash);
        if (existing != null) {
            return BeanUtils.toBean(existing, MesProEdhrInitManifestRespVO.class);
        }

        MesProEdhrInitManifestDO manifest = BeanUtils.toBean(reqVO, MesProEdhrInitManifestDO.class)
                .setUploadStatus(MANIFEST_STATUS_UPLOADED)
                .setUploadedBy(SecurityFrameworkUtils.getLoginUserId())
                .setUploadedAt(LocalDateTime.now());
        initManifestMapper.insert(manifest);
        refreshManifestCount(batch.getId());
        return BeanUtils.toBean(manifest, MesProEdhrInitManifestRespVO.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MesProEdhrInitBatchPrecheckRespVO runPrecheck(Long id) {
        MesProEdhrInitBatchDO batch = validateBatchExists(id);
        initIssueMapper.closeOpenByBatchId(batch.getId());

        List<MesProEdhrInitManifestDO> manifests = initManifestMapper.selectListByBatchId(batch.getId());
        List<MesProEdhrInitIssueDO> issues = new ArrayList<>();
        if (manifests.isEmpty()) {
            issues.add(buildIssue(batch.getId(), null, ISSUE_CODE_MISSING_MANIFEST,
                    null, null, null, "manifestHash", null, null, null,
                    "初始化批次尚未上传 manifest，禁止进入导入。",
                    "上传商业化交付 manifest 后重新执行预检。", "{\"scope\":\"batch\"}"));
        } else {
            manifests.forEach(manifest -> collectManifestIssues(batch, manifest, issues));
        }

        if (!issues.isEmpty()) {
            initIssueMapper.insertBatch(issues);
        }

        LocalDateTime precheckAt = LocalDateTime.now();
        int blockingIssueCount = initIssueMapper.countOpenBlockers(batch.getId());
        String status = blockingIssueCount > 0 ? STATUS_PRECHECK_FAILED : STATUS_PRECHECK_PASSED;
        initBatchMapper.updateById(new MesProEdhrInitBatchDO()
                .setId(batch.getId())
                .setStatus(status)
                .setManifestCount(manifests.size())
                .setBlockingIssueCount(blockingIssueCount)
                .setLastPrecheckAt(precheckAt)
                .setVersion(batch.getVersion() == null ? 1 : batch.getVersion() + 1));

        List<MesProEdhrInitIssueDO> openIssues = initIssueMapper.selectOpenListByBatchId(batch.getId());
        return new MesProEdhrInitBatchPrecheckRespVO()
                .setInitBatchId(batch.getId())
                .setStatus(status)
                .setManifestCount(manifests.size())
                .setIssueCount(openIssues.size())
                .setBlockingIssueCount(blockingIssueCount)
                .setPrecheckAt(precheckAt)
                .setIssues(BeanUtils.toBean(openIssues, MesProEdhrInitIssueRespVO.class));
    }

    @Override
    public PageResult<MesProEdhrInitIssueRespVO> getIssuePage(MesProEdhrInitIssuePageReqVO reqVO) {
        validateBatchExists(reqVO.getInitBatchId());
        return BeanUtils.toBean(initIssueMapper.selectPage(reqVO), MesProEdhrInitIssueRespVO.class);
    }

    private MesProEdhrInitBatchDO validateBatchExists(Long id) {
        MesProEdhrInitBatchDO batch = id == null ? null : initBatchMapper.selectById(id);
        if (batch == null) {
            throw exception(PRO_EDHR_INIT_BATCH_NOT_EXISTS);
        }
        return batch;
    }

    private void validateJsonObject(String rawJson, String fieldName) {
        try {
            JSON.parseObject(rawJson);
        } catch (JSONException ex) {
            throw exception(PRO_EDHR_INIT_BATCH_MANIFEST_INVALID, fieldName + " 不是有效 JSON 对象");
        }
    }

    private void refreshManifestCount(Long initBatchId) {
        int manifestCount = initManifestMapper.selectListByBatchId(initBatchId).size();
        initBatchMapper.updateById(new MesProEdhrInitBatchDO()
                .setId(initBatchId)
                .setManifestCount(manifestCount));
    }

    private void collectManifestIssues(MesProEdhrInitBatchDO batch,
                                       MesProEdhrInitManifestDO manifest,
                                       List<MesProEdhrInitIssueDO> issues) {
        JSONObject manifestJson;
        try {
            manifestJson = JSON.parseObject(manifest.getManifestJson());
        } catch (JSONException ex) {
            issues.add(buildIssue(batch.getId(), manifest.getId(), ISSUE_CODE_INVALID_MANIFEST_JSON,
                    manifest.getPackageType(), manifest.getSourceFileName(), null, "manifestJson",
                    null, null, null,
                    "manifestJson 不是有效 JSON 对象，禁止进入导入。",
                    "重新导出商业化交付 manifest 后上传。", "{\"scope\":\"manifest\"}"));
            return;
        }

        Boolean customerFieldDictionaryConfirmed = manifestJson.getBoolean("customerFieldDictionaryConfirmed");
        if (!Boolean.TRUE.equals(customerFieldDictionaryConfirmed)) {
            issues.add(buildIssue(batch.getId(), manifest.getId(), ISSUE_CODE_CUSTOMER_FIELD_DICTIONARY_MISSING,
                    manifest.getPackageType(), manifest.getSourceFileName(), null, "customerFieldDictionaryConfirmed",
                    "FIELD_DICTIONARY", batch.getProjectCode(), null,
                    "客户字段字典尚未确认，禁止进入导入。",
                    "补齐客户字段字典确认记录并更新 manifest。", "{\"scope\":\"fieldDictionary\"}"));
        }

        Boolean tenantAuthorizationConfirmed = manifestJson.getBoolean("tenantAuthorizationConfirmed");
        if (!Boolean.TRUE.equals(tenantAuthorizationConfirmed)) {
            issues.add(buildIssue(batch.getId(), manifest.getId(), ISSUE_CODE_TENANT_AUTHORIZATION_MISSING,
                    manifest.getPackageType(), manifest.getSourceFileName(), null, "tenantAuthorizationConfirmed",
                    "TENANT_AUTHORIZATION", String.valueOf(batch.getTargetTenantId()), batch.getApprovalOwnerUserId(),
                    "目标租户授权未确认，禁止进入导入。",
                    "由审批负责人确认租户授权边界后重新预检。", "{\"scope\":\"tenantAuthorization\"}"));
        }

        Boolean backupRestoreEvidenceConfirmed = manifestJson.getBoolean("backupRestoreEvidenceConfirmed");
        if (!Boolean.TRUE.equals(backupRestoreEvidenceConfirmed)) {
            issues.add(buildIssue(batch.getId(), manifest.getId(), ISSUE_CODE_BACKUP_RESTORE_EVIDENCE_MISSING,
                    manifest.getPackageType(), manifest.getSourceFileName(), null, "backupRestoreEvidenceConfirmed",
                    "BACKUP_RESTORE", batch.getDataVersion(), batch.getOwnerUserId(),
                    "缺少备份和恢复演练证据，禁止进入导入。",
                    "补齐备份、恢复验证和回滚证据后重新预检。", "{\"scope\":\"backupRestore\"}"));
        }

        Object filesNode = manifestJson.get("files");
        if (!(filesNode instanceof JSONArray files) || files.isEmpty()) {
            issues.add(buildIssue(batch.getId(), manifest.getId(), ISSUE_CODE_CONTROLLED_FILE_LIST_MISSING,
                    manifest.getPackageType(), manifest.getSourceFileName(), null, "files",
                    "CONTROLLED_FILE", null, batch.getOwnerUserId(),
                    "manifest 缺少受控文件清单，禁止进入导入。",
                    "补齐文件清单、文件大小和摘要后重新预检。", "{\"scope\":\"controlledFiles\"}"));
            return;
        }

        for (int index = 0; index < files.size(); index++) {
            JSONObject file = files.getJSONObject(index);
            String sourceFileName = StrUtil.blankToDefault(file.getString("sourceFileName"),
                    StrUtil.blankToDefault(file.getString("fileName"), manifest.getSourceFileName()));
            String checksum = StrUtil.blankToDefault(file.getString("checksum"), file.getString("sha256"));
            if (StrUtil.isBlank(checksum)) {
                issues.add(buildIssue(batch.getId(), manifest.getId(), ISSUE_CODE_FILE_CHECKSUM_MISSING,
                        StrUtil.blankToDefault(file.getString("packageType"), manifest.getPackageType()),
                        sourceFileName, index + 1, "checksum",
                        StrUtil.blankToDefault(file.getString("objectType"), "CONTROLLED_FILE"),
                        file.getString("objectKey"), readLong(file, "responsibleUserId"),
                        file.getString("responsibleName"),
                        "受控文件缺少 checksum，禁止进入导入。",
                        "重新生成包含 checksum 的 manifest 后上传。", "{\"scope\":\"controlledFile\"}"));
            }
        }
    }

    private Long readLong(JSONObject object, String key) {
        Object value = object.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = Objects.toString(value, "");
        return StrUtil.isBlank(text) ? null : Long.valueOf(text);
    }

    private MesProEdhrInitIssueDO buildIssue(Long batchId,
                                             Long manifestId,
                                             String issueCode,
                                             String packageType,
                                             String sourceFileName,
                                             Integer sourceRowNo,
                                             String sourceFieldName,
                                             String objectType,
                                             String objectKey,
                                             Long responsibleUserId,
                                             String issueMessage,
                                             String remediationSuggestion,
                                             String impactScopeJson) {
        return buildIssue(batchId, manifestId, issueCode, packageType, sourceFileName, sourceRowNo,
                sourceFieldName, objectType, objectKey, responsibleUserId, null,
                issueMessage, remediationSuggestion, impactScopeJson);
    }

    private MesProEdhrInitIssueDO buildIssue(Long batchId,
                                             Long manifestId,
                                             String issueCode,
                                             String packageType,
                                             String sourceFileName,
                                             Integer sourceRowNo,
                                             String sourceFieldName,
                                             String objectType,
                                             String objectKey,
                                             Long responsibleUserId,
                                             String responsibleName,
                                             String issueMessage,
                                             String remediationSuggestion,
                                             String impactScopeJson) {
        return new MesProEdhrInitIssueDO()
                .setInitBatchId(batchId)
                .setInitManifestId(manifestId)
                .setIssueCode(issueCode)
                .setIssueLevel(ISSUE_LEVEL_BLOCKER)
                .setIssueStatus(ISSUE_STATUS_OPEN)
                .setPackageType(packageType)
                .setSourceFileName(sourceFileName)
                .setSourceRowNo(sourceRowNo)
                .setSourceFieldName(sourceFieldName)
                .setObjectType(objectType)
                .setObjectKey(objectKey)
                .setResponsibleUserId(responsibleUserId)
                .setResponsibleName(StrUtil.blankToDefault(responsibleName, "交付负责人"))
                .setIssueMessage(issueMessage)
                .setRemediationSuggestion(remediationSuggestion)
                .setImpactScopeJson(impactScopeJson);
    }

    private MesProEdhrInitBatchRespVO toRespVO(MesProEdhrInitBatchDO batch, boolean withManifests) {
        MesProEdhrInitBatchRespVO respVO = BeanUtils.toBean(batch, MesProEdhrInitBatchRespVO.class);
        if (withManifests) {
            List<MesProEdhrInitManifestRespVO> manifests =
                    BeanUtils.toBean(initManifestMapper.selectListByBatchId(batch.getId()), MesProEdhrInitManifestRespVO.class);
            respVO.setManifests(manifests);
            if (!manifests.isEmpty()) {
                respVO.setLatestManifestHash(manifests.get(manifests.size() - 1).getManifestHash());
            }
        }
        return respVO;
    }

    private void fillLatestManifestHash(MesProEdhrInitBatchRespVO respVO) {
        List<MesProEdhrInitManifestDO> manifests = initManifestMapper.selectListByBatchId(respVO.getId());
        if (!manifests.isEmpty()) {
            respVO.setLatestManifestHash(manifests.get(manifests.size() - 1).getManifestHash());
        }
    }
}
