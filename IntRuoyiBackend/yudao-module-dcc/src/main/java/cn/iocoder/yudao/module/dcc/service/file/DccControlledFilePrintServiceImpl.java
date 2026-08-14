package cn.iocoder.yudao.module.dcc.service.file;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePrintCreateReqVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePrintHtmlRespVO;
import cn.iocoder.yudao.module.dcc.controller.admin.file.vo.DccControlledFilePrintRecordRespVO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFileMasterDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccControlledFilePrintRecordDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFileMasterMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccControlledFilePrintRecordMapper;
import cn.iocoder.yudao.module.dcc.enums.DccControlledFileStatusEnum;
import cn.iocoder.yudao.module.dcc.enums.DccFileCategoryPermissionActionEnum;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_NOT_EXISTS;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PRINT_NOT_ALLOWED;
import static cn.iocoder.yudao.module.dcc.enums.ErrorCodeConstants.CONTROLLED_FILE_PRINT_REQUIRED_FIELD_MISSING;

/**
 * DCC controlled file print service implementation.
 */
@Service
@Validated
public class DccControlledFilePrintServiceImpl implements DccControlledFilePrintService {

    private static final String APPROVAL_STATUS_DIRECT_PRINTED = "DIRECT_PRINTED";
    private static final DateTimeFormatter PRINT_NO_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter PRINT_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private DccControlledFileMapper controlledFileMapper;
    @Resource
    private DccControlledFileMasterMapper controlledFileMasterMapper;
    @Resource
    private DccControlledFilePrintRecordMapper printRecordMapper;
    @Resource
    private DccControlledFileCategoryPermissionSupport permissionSupport;
    @Resource
    private AdminUserApi adminUserApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DccControlledFilePrintRecordRespVO createPrintRecord(Long userId, Long controlledFileId,
                                                                DccControlledFilePrintCreateReqVO reqVO) {
        requirePrintFields(reqVO);
        DccControlledFileDO file = validatePrintableFile(userId, controlledFileId);
        LocalDateTime printTime = LocalDateTime.now();
        DccControlledFilePrintRecordDO record = DccControlledFilePrintRecordDO.builder()
                .controlledFileId(file.getId())
                .fileNumber(file.getFileNumber())
                .versionNo(file.getVersionNo())
                .printNo(buildPrintNo(printTime))
                .purpose(StrUtil.trim(reqVO.getPurpose()))
                .copies(reqVO.getCopies())
                .receivingDepartment(StrUtil.trim(reqVO.getReceivingDepartment()))
                .useLocation(StrUtil.trim(reqVO.getUseLocation()))
                .printUserId(userId)
                .printUserName(resolveUserName(userId))
                .printTime(printTime)
                .approvalStatus(APPROVAL_STATUS_DIRECT_PRINTED)
                .build();
        printRecordMapper.insert(record);
        return toRespVO(record);
    }

    @Override
    public List<DccControlledFilePrintRecordRespVO> getPrintRecords(Long userId, Long controlledFileId) {
        validatePrintableFile(userId, controlledFileId);
        return convertList(printRecordMapper.selectListByControlledFileId(controlledFileId), this::toRespVO);
    }

    @Override
    public DccControlledFilePrintHtmlRespVO getPrintHtml(Long userId, Long controlledFileId, Long printRecordId) {
        validatePrintableFile(userId, controlledFileId);
        DccControlledFilePrintRecordDO record = printRecordMapper.selectByIdAndControlledFileId(printRecordId,
                controlledFileId);
        if (record == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        DccControlledFilePrintHtmlRespVO respVO = new DccControlledFilePrintHtmlRespVO();
        respVO.setPrintRecordId(record.getId());
        respVO.setPrintNo(record.getPrintNo());
        respVO.setHtml(buildControlledPrintHtml(record));
        return respVO;
    }

    private DccControlledFileDO validatePrintableFile(Long userId, Long controlledFileId) {
        DccControlledFileDO file = controlledFileMapper.selectById(controlledFileId);
        if (file == null) {
            throw exception(CONTROLLED_FILE_NOT_EXISTS);
        }
        if (!DccControlledFileStatusEnum.ACTIVE.getStatus().equals(file.getStatus())) {
            throw exception(CONTROLLED_FILE_PRINT_NOT_ALLOWED);
        }
        DccControlledFileMasterDO master = file.getMasterId() == null ? null
                : controlledFileMasterMapper.selectById(file.getMasterId());
        if (master == null || !Objects.equals(master.getCurrentActiveControlledFileId(), file.getId())) {
            throw exception(CONTROLLED_FILE_PRINT_NOT_ALLOWED);
        }
        if (!permissionSupport.hasCategoryPermission(file.getCategoryId(), userId,
                DccFileCategoryPermissionActionEnum.PRINT)) {
            throw exception(CONTROLLED_FILE_PRINT_NOT_ALLOWED);
        }
        return file;
    }

    private void requirePrintFields(DccControlledFilePrintCreateReqVO reqVO) {
        if (reqVO == null
                || StrUtil.isBlank(reqVO.getPurpose())
                || reqVO.getCopies() == null
                || reqVO.getCopies() <= 0
                || StrUtil.isBlank(reqVO.getReceivingDepartment())
                || StrUtil.isBlank(reqVO.getUseLocation())) {
            throw exception(CONTROLLED_FILE_PRINT_REQUIRED_FIELD_MISSING);
        }
    }

    private String buildPrintNo(LocalDateTime printTime) {
        return "DCCP-" + PRINT_NO_TIME_FORMATTER.format(printTime) + "-"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private List<String> buildControlledPrintCopyNumbers(DccControlledFilePrintRecordDO record) {
        int copies = Math.max(1, record.getCopies() == null ? 1 : record.getCopies());
        int width = Math.max(2, String.valueOf(copies).length());
        List<String> copyNumbers = new ArrayList<>(copies);
        for (int i = 1; i <= copies; i++) {
            copyNumbers.add(record.getPrintNo() + "-" + StrUtil.padPre(String.valueOf(i), width, '0'));
        }
        return copyNumbers;
    }

    private DccControlledFilePrintRecordRespVO toRespVO(DccControlledFilePrintRecordDO record) {
        return BeanUtils.toBean(record, DccControlledFilePrintRecordRespVO.class);
    }

    private String resolveUserName(Long userId) {
        AdminUserRespDTO user = userId == null ? null : adminUserApi.getUser(userId);
        if (user == null) {
            return userId == null ? "-" : "用户#" + userId;
        }
        String nickname = StrUtil.trim(user.getNickname());
        String username = StrUtil.trim(user.getUsername());
        if (StrUtil.isNotBlank(nickname) && StrUtil.isNotBlank(username)) {
            return nickname + " (" + username + ")";
        }
        return StrUtil.blankToDefault(nickname, StrUtil.blankToDefault(username, "用户#" + userId));
    }

    private String buildControlledPrintHtml(DccControlledFilePrintRecordDO record) {
        String printTimeText = record.getPrintTime() == null ? "-" : PRINT_TIME_FORMATTER.format(record.getPrintTime());
        String copyNumberText = String.join("、", buildControlledPrintCopyNumbers(record));
        return """
                <!doctype html>
                <html lang="zh-CN">
                <head>
                  <meta charset="UTF-8" />
                  <title>DCC受控打印-%s</title>
                  <style>
                    body { font-family: "Microsoft YaHei", Arial, sans-serif; margin: 32px; color: #111827; }
                    .watermark { position: fixed; inset: 0; z-index: -1; color: rgba(22, 119, 255, 0.10); font-size: 54px; font-weight: 700; transform: rotate(-28deg); display: flex; align-items: center; justify-content: center; }
                    h1 { margin: 0 0 16px; font-size: 24px; }
                    .meta { width: 100%%; border-collapse: collapse; margin-top: 16px; }
                    .meta th, .meta td { border: 1px solid #cbd5e1; padding: 10px 12px; text-align: left; }
                    .meta th { width: 160px; background: #f8fafc; }
                    .copy-numbers { line-height: 1.8; word-break: break-all; }
                    .notice { margin-top: 18px; padding: 12px; border: 1px solid #93c5fd; background: #eff6ff; color: #1d4ed8; }
                  </style>
                </head>
                <body>
                  <div class="watermark">受控打印 %s</div>
                  <h1>DCC 受控打印件</h1>
                  <table class="meta">
                    <tbody>
                      <tr><th>打印编号</th><td>%s</td></tr>
                      <tr><th>文件编号</th><td>%s</td></tr>
                      <tr><th>版本</th><td>%s</td></tr>
                      <tr><th>打印人</th><td>%s</td></tr>
                      <tr><th>打印时间</th><td>%s</td></tr>
                      <tr><th>份数</th><td>%s</td></tr>
                      <tr><th>副本编号</th><td><div class="copy-numbers">%s</div></td></tr>
                      <tr><th>打印用途</th><td>%s</td></tr>
                      <tr><th>接收部门</th><td>%s</td></tr>
                      <tr><th>使用位置</th><td>%s</td></tr>
                      <tr><th>审批/打印状态</th><td>%s</td></tr>
                      <tr><th>审批策略</th><td>直接受控打印（当前文件类别无需打印审批）</td></tr>
                    </tbody>
                  </table>
                  <div class="notice">本打印件来自当前有效受控版本，按直接受控打印策略生成，仅限登记用途和使用位置使用，打印记录和每份副本编号均可追溯。</div>
                </body>
                </html>
                """.formatted(
                html(record.getPrintNo()),
                html(record.getPrintNo()),
                html(record.getPrintNo()),
                html(record.getFileNumber()),
                html(record.getVersionNo()),
                html(record.getPrintUserName()),
                html(printTimeText),
                html(record.getCopies()),
                html(copyNumberText),
                html(record.getPurpose()),
                html(record.getReceivingDepartment()),
                html(record.getUseLocation()),
                html(record.getApprovalStatus()));
    }

    private String html(Object value) {
        return HtmlUtils.htmlEscape(value == null ? "-" : String.valueOf(value));
    }
}
