package cn.iocoder.yudao.module.mes.controller.admin.pro.processpool.team.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
public class MesProductionExecutionTraceRespVO {

    private Long processPoolEventId;
    private Boolean complete;
    private List<Section> sections = new ArrayList<>();
    private List<Blocker> blockers = new ArrayList<>();
    private List<CandidateEvent> candidateEvents = new ArrayList<>();
    private ClosureEvidence closureEvidence;
    private LocalDateTime lastUpdatedAt;

    @Data
    @Accessors(chain = true)
    public static class Section {

        private String sectionKey;
        private String status;
        private Map<String, Object> sourceIds = new LinkedHashMap<>();
        private List<Blocker> blockers = new ArrayList<>();
        private LocalDateTime lastUpdatedAt;
    }

    @Data
    @Accessors(chain = true)
    public static class Blocker {

        private String code;
        private String message;
        private String missingObjectType;
        private String resolution;
    }

    @Data
    @Accessors(chain = true)
    public static class CandidateEvent {

        private Long processPoolEventId;
        private String eventType;
        private Long actualEmployeeId;
        private LocalDateTime serverSubmitTime;
        private String status;
    }

    @Data
    @Accessors(chain = true)
    public static class ClosureEvidence {

        private Long processPoolEventId;
        private Boolean complete;
        private Map<String, EvidenceAnswer> answers = new LinkedHashMap<>();
        private List<SameSourceCheck> sameSourceChecks = new ArrayList<>();
        private List<Blocker> blockers = new ArrayList<>();
    }

    @Data
    @Accessors(chain = true)
    public static class EvidenceAnswer {

        private String answerKey;
        private Object value;
        private String section;
        private Boolean sameSource;
        private Map<String, Object> sourceIds = new LinkedHashMap<>();
        private List<ReadOnlyVerificationEntry> readOnlyVerificationEntries = new ArrayList<>();
        private List<Blocker> blockers = new ArrayList<>();
    }

    @Data
    @Accessors(chain = true)
    public static class SameSourceCheck {

        private String checkKey;
        private Boolean passed;
        private Map<String, Object> sourceIds = new LinkedHashMap<>();
        private String message;
    }

    @Data
    @Accessors(chain = true)
    public static class ReadOnlyVerificationEntry {

        private String verificationKey;
        private String method;
        private String path;
        private Map<String, Object> params = new LinkedHashMap<>();
    }
}
