package cn.iocoder.yudao.module.mes.service.qa.regulation;

import cn.iocoder.yudao.module.mes.controller.admin.qa.regulation.vo.MesQaInspectionRegulationParseRespVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.QA_INSPECTION_REGULATION_WORD_IMPORT_INVALID;

@Service
@RequiredArgsConstructor
public class MesQaInspectionRegulationParseService {

    private static final int SCHEMA_VERSION = 1;
    private static final int MAX_BUSINESS_CODE_LENGTH = 64;

    private final MesQaInspectionRegulationWordParser parser;

    public MesQaInspectionRegulationParseRespVO parseWord(MultipartFile file) {
        String fileName = validateFile(file);
        MesQaInspectionRegulationWordParser.ParsedRegulation parsed;
        try {
            parsed = parser.parse(file.getBytes(), fileName);
        } catch (IOException ex) {
            throw exception(QA_INSPECTION_REGULATION_WORD_IMPORT_INVALID,
                    "读取上传文件失败：" + MesQaInspectionRegulationWordParser.normalizeText(ex.getMessage()));
        }
        return MesQaInspectionRegulationParseRespVO.builder()
                .schemaVersion(SCHEMA_VERSION)
                .sourceFileName(fileName)
                .regulationCode(parsed.regulationCode())
                .regulationName(parsed.regulationName())
                .versionNo(parsed.versionNo())
                .effectiveDate(parsed.effectiveDate().toString())
                .processes(groupItemsByProcess(parsed))
                .build();
    }

    private static List<MesQaInspectionRegulationParseRespVO.InspectionProcess> groupItemsByProcess(
            MesQaInspectionRegulationWordParser.ParsedRegulation parsed) {
        Map<String, ProcessGroup> processGroups = new LinkedHashMap<>();
        int globalItemIndex = 0;
        for (MesQaInspectionRegulationWordParser.ParsedItem parsedItem : parsed.items()) {
            globalItemIndex++;
            ProcessGroup group = processGroups.computeIfAbsent(parsedItem.processName(),
                    ignored -> new ProcessGroup(parsedItem.processName(), new ArrayList<>()));
            List<String> applicableInspectionTypes = new ArrayList<>();
            if (parsedItem.firstInspectionQuantity() != null) {
                applicableInspectionTypes.add("FIRST");
            }
            if (parsedItem.patrolInspectionRatio() != null) {
                applicableInspectionTypes.add("PATROL");
            }
            group.items().add(MesQaInspectionRegulationParseRespVO.InspectionItem.builder()
                    .itemSort(group.items().size() + 1)
                    .itemCode(generatedCode(parsed.regulationCode(), "I", globalItemIndex))
                    .itemName(parsedItem.itemName())
                    .inspectionMethod(parsedItem.inspectionMethod())
                    .inspectionTool(parsedItem.inspectionTool())
                    .samplingPlanText(parsedItem.samplingPlanText())
                    .standardText(parsedItem.standardText())
                    .resultType("BOOLEAN")
                    .applicableInspectionTypes(List.copyOf(applicableInspectionTypes))
                    .firstInspectionQuantity(parsedItem.firstInspectionQuantity())
                    .patrolInspectionRatio(parsedItem.patrolInspectionRatio())
                    .build());
        }

        List<MesQaInspectionRegulationParseRespVO.InspectionProcess> processes = new ArrayList<>();
        int processIndex = 0;
        for (ProcessGroup group : processGroups.values()) {
            processIndex++;
            processes.add(MesQaInspectionRegulationParseRespVO.InspectionProcess.builder()
                    .processCode(generatedCode(parsed.regulationCode(), "P", processIndex))
                    .processName(group.processName())
                    .sort(processIndex)
                    .items(List.copyOf(group.items()))
                    .build());
        }
        return List.copyOf(processes);
    }

    private static String validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw exception(QA_INSPECTION_REGULATION_WORD_IMPORT_INVALID, "上传文件不能为空");
        }
        String originalName = file.getOriginalFilename();
        String normalizedPath = originalName == null ? "" : originalName.replace('\\', '/');
        String fileName = normalizedPath.substring(normalizedPath.lastIndexOf('/') + 1).trim();
        if (fileName.isEmpty() || !fileName.toLowerCase().endsWith(".docx")) {
            throw exception(QA_INSPECTION_REGULATION_WORD_IMPORT_INVALID, "仅支持 .docx 文件");
        }
        return fileName;
    }

    private static String generatedCode(String regulationCode, String type, int index) {
        String code = regulationCode + "-" + type + String.format("%03d", index);
        if (code.length() > MAX_BUSINESS_CODE_LENGTH) {
            throw exception(QA_INSPECTION_REGULATION_WORD_IMPORT_INVALID,
                    "生成的业务编码超过 " + MAX_BUSINESS_CODE_LENGTH + " 个字符：" + code);
        }
        return code;
    }

    private record ProcessGroup(String processName,
                                List<MesQaInspectionRegulationParseRespVO.InspectionItem> items) {
    }
}
