package cn.iocoder.yudao.module.mes.service.pro.batchrecord;

import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionOriginMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTraceLinkMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrBatchExecutionTaskMapper;
import cn.iocoder.yudao.module.bpm.dal.mysql.formcenter.FormActionInstanceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionFieldAuditItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolPqcRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrReleaseTransactionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderReleaseApplicationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrReverseTraceModels.Category;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MesProEdhrFormalReverseTraceAdapterConfiguration {

    @Bean
    MesProEdhrReverseTraceSourceAdapter fieldReverseTraceAdapter(MesProBatchRecordExecutionMapper executionMapper,
            MesProEdhrBatchExecutionOriginMapper originMapper, MesProEdhrBatchExecutionTraceLinkMapper traceLinkMapper,
            MesProEdhrBatchExecutionTaskMapper taskMapper, FormActionInstanceMapper formValueMapper,
            MesProBatchRecordExecutionFieldAuditItemMapper fieldAuditMapper,
            MesProProcessPoolEventMapper eventMapper,
            MesProProcessPoolPqcRecordMapper pqcRecordMapper,
            MesProcessPoolSubmissionReviewMapper reviewMapper,
            MesProcessPoolActiveOrderReleaseApplicationMapper releaseApplicationMapper,
            MesProEdhrReleaseTransactionMapper releaseTransactionMapper,
            MesProcessPoolActiveOrderCompletionReceiptMapper completionReceiptMapper) {
        return adapter(Category.FIELD, executionMapper, originMapper, traceLinkMapper, taskMapper, formValueMapper, fieldAuditMapper, eventMapper, pqcRecordMapper, reviewMapper, releaseApplicationMapper, releaseTransactionMapper, completionReceiptMapper);
    }

    @Bean
    MesProEdhrReverseTraceSourceAdapter parameterReverseTraceAdapter(MesProBatchRecordExecutionMapper executionMapper,
            MesProEdhrBatchExecutionOriginMapper originMapper, MesProEdhrBatchExecutionTraceLinkMapper traceLinkMapper,
            MesProEdhrBatchExecutionTaskMapper taskMapper, FormActionInstanceMapper formValueMapper,
            MesProBatchRecordExecutionFieldAuditItemMapper fieldAuditMapper,
            MesProProcessPoolEventMapper eventMapper,
            MesProProcessPoolPqcRecordMapper pqcRecordMapper,
            MesProcessPoolSubmissionReviewMapper reviewMapper,
            MesProcessPoolActiveOrderReleaseApplicationMapper releaseApplicationMapper,
            MesProEdhrReleaseTransactionMapper releaseTransactionMapper,
            MesProcessPoolActiveOrderCompletionReceiptMapper completionReceiptMapper) {
        return adapter(Category.PARAMETER, executionMapper, originMapper, traceLinkMapper, taskMapper, formValueMapper, fieldAuditMapper, eventMapper, pqcRecordMapper, reviewMapper, releaseApplicationMapper, releaseTransactionMapper, completionReceiptMapper);
    }

    @Bean
    MesProEdhrReverseTraceSourceAdapter equipmentReverseTraceAdapter(MesProBatchRecordExecutionMapper executionMapper,
            MesProEdhrBatchExecutionOriginMapper originMapper, MesProEdhrBatchExecutionTraceLinkMapper traceLinkMapper,
            MesProEdhrBatchExecutionTaskMapper taskMapper, FormActionInstanceMapper formValueMapper,
            MesProBatchRecordExecutionFieldAuditItemMapper fieldAuditMapper,
            MesProProcessPoolEventMapper eventMapper,
            MesProProcessPoolPqcRecordMapper pqcRecordMapper,
            MesProcessPoolSubmissionReviewMapper reviewMapper,
            MesProcessPoolActiveOrderReleaseApplicationMapper releaseApplicationMapper,
            MesProEdhrReleaseTransactionMapper releaseTransactionMapper,
            MesProcessPoolActiveOrderCompletionReceiptMapper completionReceiptMapper) {
        return adapter(Category.EQUIPMENT, executionMapper, originMapper, traceLinkMapper, taskMapper, formValueMapper, fieldAuditMapper, eventMapper, pqcRecordMapper, reviewMapper, releaseApplicationMapper, releaseTransactionMapper, completionReceiptMapper);
    }

    @Bean
    MesProEdhrReverseTraceSourceAdapter personReverseTraceAdapter(MesProBatchRecordExecutionMapper executionMapper,
            MesProEdhrBatchExecutionOriginMapper originMapper, MesProEdhrBatchExecutionTraceLinkMapper traceLinkMapper,
            MesProEdhrBatchExecutionTaskMapper taskMapper, FormActionInstanceMapper formValueMapper,
            MesProBatchRecordExecutionFieldAuditItemMapper fieldAuditMapper,
            MesProProcessPoolEventMapper eventMapper,
            MesProProcessPoolPqcRecordMapper pqcRecordMapper,
            MesProcessPoolSubmissionReviewMapper reviewMapper,
            MesProcessPoolActiveOrderReleaseApplicationMapper releaseApplicationMapper,
            MesProEdhrReleaseTransactionMapper releaseTransactionMapper,
            MesProcessPoolActiveOrderCompletionReceiptMapper completionReceiptMapper) {
        return adapter(Category.PERSON, executionMapper, originMapper, traceLinkMapper, taskMapper, formValueMapper, fieldAuditMapper, eventMapper, pqcRecordMapper, reviewMapper, releaseApplicationMapper, releaseTransactionMapper, completionReceiptMapper);
    }

    @Bean
    MesProEdhrReverseTraceSourceAdapter inspectionReverseTraceAdapter(MesProBatchRecordExecutionMapper executionMapper,
            MesProEdhrBatchExecutionOriginMapper originMapper, MesProEdhrBatchExecutionTraceLinkMapper traceLinkMapper,
            MesProEdhrBatchExecutionTaskMapper taskMapper, FormActionInstanceMapper formValueMapper,
            MesProBatchRecordExecutionFieldAuditItemMapper fieldAuditMapper,
            MesProProcessPoolEventMapper eventMapper,
            MesProProcessPoolPqcRecordMapper pqcRecordMapper,
            MesProcessPoolSubmissionReviewMapper reviewMapper,
            MesProcessPoolActiveOrderReleaseApplicationMapper releaseApplicationMapper,
            MesProEdhrReleaseTransactionMapper releaseTransactionMapper,
            MesProcessPoolActiveOrderCompletionReceiptMapper completionReceiptMapper) {
        return adapter(Category.INSPECTION, executionMapper, originMapper, traceLinkMapper, taskMapper, formValueMapper, fieldAuditMapper, eventMapper, pqcRecordMapper, reviewMapper, releaseApplicationMapper, releaseTransactionMapper, completionReceiptMapper);
    }

    @Bean
    MesProEdhrReverseTraceSourceAdapter materialReverseTraceAdapter(MesProBatchRecordExecutionMapper executionMapper,
            MesProEdhrBatchExecutionOriginMapper originMapper, MesProEdhrBatchExecutionTraceLinkMapper traceLinkMapper,
            MesProEdhrBatchExecutionTaskMapper taskMapper, FormActionInstanceMapper formValueMapper,
            MesProBatchRecordExecutionFieldAuditItemMapper fieldAuditMapper,
            MesProProcessPoolEventMapper eventMapper,
            MesProProcessPoolPqcRecordMapper pqcRecordMapper,
            MesProcessPoolSubmissionReviewMapper reviewMapper,
            MesProcessPoolActiveOrderReleaseApplicationMapper releaseApplicationMapper,
            MesProEdhrReleaseTransactionMapper releaseTransactionMapper,
            MesProcessPoolActiveOrderCompletionReceiptMapper completionReceiptMapper) {
        return adapter(Category.MATERIAL, executionMapper, originMapper, traceLinkMapper, taskMapper, formValueMapper, fieldAuditMapper, eventMapper, pqcRecordMapper, reviewMapper, releaseApplicationMapper, releaseTransactionMapper, completionReceiptMapper);
    }

    private MesProEdhrReverseTraceSourceAdapter adapter(Category category, MesProBatchRecordExecutionMapper executionMapper,
            MesProEdhrBatchExecutionOriginMapper originMapper, MesProEdhrBatchExecutionTraceLinkMapper traceLinkMapper,
            MesProEdhrBatchExecutionTaskMapper taskMapper, FormActionInstanceMapper formValueMapper,
            MesProBatchRecordExecutionFieldAuditItemMapper fieldAuditMapper,
            MesProProcessPoolEventMapper eventMapper,
            MesProProcessPoolPqcRecordMapper pqcRecordMapper,
            MesProcessPoolSubmissionReviewMapper reviewMapper,
            MesProcessPoolActiveOrderReleaseApplicationMapper releaseApplicationMapper,
            MesProEdhrReleaseTransactionMapper releaseTransactionMapper,
            MesProcessPoolActiveOrderCompletionReceiptMapper completionReceiptMapper) {
        return new MesProEdhrFormalReverseTraceAdapter(category, executionMapper, originMapper, traceLinkMapper, taskMapper, formValueMapper, fieldAuditMapper, eventMapper,
                pqcRecordMapper, reviewMapper, releaseApplicationMapper, releaseTransactionMapper, completionReceiptMapper);
    }
}
