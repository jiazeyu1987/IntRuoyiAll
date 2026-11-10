package cn.iocoder.yudao.module.bpm.approval.service;

/** Business modules provide their formal summary while BPM retains process ownership and navigation. */
public interface ApprovalTaskSummaryEnricher {
    void enrich(ApprovalTaskSummary summary, String processDefinitionKey);
}
