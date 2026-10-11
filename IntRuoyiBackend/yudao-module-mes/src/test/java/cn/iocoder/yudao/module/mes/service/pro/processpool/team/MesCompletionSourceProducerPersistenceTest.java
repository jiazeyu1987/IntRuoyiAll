package cn.iocoder.yudao.module.mes.service.pro.processpool.team;

import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccElectronicSignatureAuthorizationAuditDO;
import cn.iocoder.yudao.module.dcc.dal.dataobject.file.DccElectronicSignatureAuthorizationDO;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccElectronicSignatureAuthorizationAuditMapper;
import cn.iocoder.yudao.module.dcc.dal.mysql.file.DccElectronicSignatureAuthorizationMapper;
import cn.iocoder.yudao.module.dcc.service.file.DccElectronicSignatureAuthorizationAuditServiceImpl;
import cn.iocoder.yudao.module.dcc.service.file.DccElectronicSignatureAuthorizationServiceImpl;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionPickListDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionPickListItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionReplenishmentListDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.production.kingdee.ErpKingdeeProductionReplenishmentListItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionPickListItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionPickListMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionReplenishmentListItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.production.kingdee.ErpKingdeeProductionReplenishmentListMapper;
import cn.iocoder.yudao.module.infra.api.config.ConfigApiImpl;
import cn.iocoder.yudao.module.infra.dal.dataobject.config.ConfigDO;
import cn.iocoder.yudao.module.infra.dal.mysql.config.ConfigMapper;
import cn.iocoder.yudao.module.infra.service.config.ConfigServiceImpl;
import cn.iocoder.yudao.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesIndependentBatchPrerequisiteReceiptDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProBatchRecordExecutionSignatureDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrMaterialGateReceiptDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrOperationAuditEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.feedback.MesProFeedbackDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.handoff.MesActiveOrderHandoffDeliveryDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.handoff.MesActiveOrderHandoffTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.process.MesProProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventRevisionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolEventRevisionDiffDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.MesProProcessPoolPqcRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionPieceDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderCompletionBackfillDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolActiveOrderTransferTraceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolOrderProcessCompletionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolReportAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolSubmissionReviewDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamEmployeeProfileDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.processpool.team.MesProcessPoolTeamLeaderScopeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteProcessDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.route.MesProRouteVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.productissue.MesWmProductIssueDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.productissue.MesWmProductIssueDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.transfer.MesWmTransferDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.transfer.MesWmTransferDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.transfer.MesWmTransferLineDO;
import cn.iocoder.yudao.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesEdhrSourceCreationBindingMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesEdhrReleaseSubmissionMaterialAssociationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesEdhrManagedHandoffMaterialAssociationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesIndependentBatchPrerequisiteReceiptMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProBatchRecordExecutionSignatureMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrMaterialGateReceiptMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrOperationAuditEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.batchrecord.MesProEdhrWorkTaskAssignmentRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.feedback.MesProFeedbackMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.handoff.MesActiveOrderHandoffDeliveryMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.handoff.MesActiveOrderHandoffTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.process.MesProProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventRevisionDiffMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolEventRevisionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.MesProProcessPoolPqcRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionPieceDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.pqc.MesPqcProcessInspectionAggregateDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderCompletionBackfillMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderCompletionReceiptMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderPickListBindingMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderProcessSnapshotMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolActiveOrderTransferTraceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolOrderProcessCompletionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolReportAllocationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolSubmissionReviewMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamEmployeeProfileMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.processpool.team.MesProcessPoolTeamLeaderScopeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.route.MesProRouteVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.productissue.MesWmProductIssueDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.productissue.MesWmProductIssueMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.transfer.MesWmTransferDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.transfer.MesWmTransferLineMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.transfer.MesWmTransferMapper;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesBatchRecordSignatureSubjectAdapter;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesEdhrSourceCreationBindingStore;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesEdhrSourceCreationProtocolRegistry;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesEdhrSourceCreationVerificationService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesEdhrSourceOwnerIntegrity;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProBatchRecordExecutionSignatureService;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrCandidateResolver;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProEdhrOperationAuditServiceImpl;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.MesProductionSignatureEvidenceService;
import cn.iocoder.yudao.module.mes.service.pro.frontline.ActiveOrderSnapshotResolver;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlineProcessMaterialServiceImpl;
import cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffAudit;
import cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffDeliveryService;
import cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffDeliveryTransactionService;
import cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffOwnerResolver;
import cn.iocoder.yudao.module.mes.service.pro.handoff.MesActiveOrderHandoffService;
import cn.iocoder.yudao.module.mes.service.pro.handoff.MesPqcHandoffAssignmentService;
import cn.iocoder.yudao.module.mes.service.pro.handoff.MesQaHandoffAssignmentService;
import cn.iocoder.yudao.module.mes.service.pro.handoff.MesSignedReturnCorrectionResolver;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesActiveOrderTransferTraceServiceImpl;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesCorrectionSignatureEvidenceReader;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesFormalProductionPickListSourceResolver;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesPqcProcessInspectionAggregationServiceImpl;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesProductionSubmissionReadBinding;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesSubmissionSignatureIdentityReader;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderCompletionBackfillPortImpl;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderCompletionFlow6ReceiptPortImpl;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderCompletionProgressPortImpl;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderCompletionServiceImpl;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderPickListCompletionSourceService;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderActiveOrderReleaseLossSourceReaderImpl;
import cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderScopeServiceImpl;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.MesReleaseAffectedStateCollector;
import cn.iocoder.yudao.module.mes.service.pro.productionrelease.notification.MesReleaseTaskNotificationPlatformSender;
import cn.iocoder.yudao.module.signature.dal.dataobject.ElectronicSignatureRecordDO;
import cn.iocoder.yudao.module.signature.dal.mysql.ElectronicSignatureRecordMapper;
import cn.iocoder.yudao.module.signature.service.ElectronicSignatureQueryServiceImpl;
import cn.iocoder.yudao.module.signature.service.ElectronicSignatureVerifiedFactService;
import cn.iocoder.yudao.module.signature.service.ElectronicSignatureServiceImpl;
import cn.iocoder.yudao.module.system.api.dept.DeptApiImpl;
import cn.iocoder.yudao.module.system.api.notify.NotifyMessageSendApiImpl;
import cn.iocoder.yudao.module.system.api.permission.PermissionApiImpl;
import cn.iocoder.yudao.module.system.api.user.AdminUserApiImpl;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.PostDO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.UserPostDO;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditEventDO;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditEventRelationDO;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditLedgerSequenceDO;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditPolicyActivationDO;
import cn.iocoder.yudao.module.system.dal.dataobject.gxpaudit.GxpAuditPolicyOperationDO;
import cn.iocoder.yudao.module.system.dal.dataobject.notify.NotifyMessageDO;
import cn.iocoder.yudao.module.system.dal.dataobject.notify.NotifyTemplateDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.MenuDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleCategoryDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleMenuDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.SystemEntitlementAuditEventDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.SystemEntitlementClaimDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.SystemEntitlementGrantDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.SystemEntitlementPolicyDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.TemporaryRoleGrantAuditDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.TemporaryRoleGrantDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.UserRoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.tenant.TenantDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserPasswordHistoryDO;
import cn.iocoder.yudao.module.system.dal.mysql.dept.DeptMapper;
import cn.iocoder.yudao.module.system.dal.mysql.dept.PostMapper;
import cn.iocoder.yudao.module.system.dal.mysql.dept.UserPostMapper;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditEventMapper;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditEventRelationMapper;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditLedgerSequenceMapper;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditPolicyActivationMapper;
import cn.iocoder.yudao.module.system.dal.mysql.gxpaudit.GxpAuditPolicyOperationMapper;
import cn.iocoder.yudao.module.system.dal.mysql.notify.NotifyMessageMapper;
import cn.iocoder.yudao.module.system.dal.mysql.notify.NotifyTemplateMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.MenuMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.ApprovalAuthorityRevisionMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.ApprovalAuthorityTenantScopeMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.PermissionCommandReceiptMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.RoleCategoryMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.RoleMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.RoleMenuMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.SystemEntitlementAuditEventMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.SystemEntitlementClaimMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.SystemEntitlementGrantMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.SystemEntitlementPolicyMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.TemporaryRoleGrantAuditMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.TemporaryRoleGrantMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.UserRoleMapper;
import cn.iocoder.yudao.module.system.dal.mysql.tenant.TenantMapper;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserPasswordHistoryMapper;
import cn.iocoder.yudao.module.system.service.dept.DeptServiceImpl;
import cn.iocoder.yudao.module.system.service.dept.PostServiceImpl;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpAuditServiceImpl;
import cn.iocoder.yudao.module.system.service.gxpaudit.GxpCreationAuditVerificationService;
import cn.iocoder.yudao.module.system.service.gxpaudit.SignatureCreationVerificationService;
import cn.iocoder.yudao.module.system.service.notify.NotifyMessageServiceImpl;
import cn.iocoder.yudao.module.system.service.notify.NotifySendServiceImpl;
import cn.iocoder.yudao.module.system.service.notify.NotifyTemplateServiceImpl;
import cn.iocoder.yudao.module.system.service.permission.MenuServiceImpl;
import cn.iocoder.yudao.module.system.service.permission.ApprovalAuthorityService;
import cn.iocoder.yudao.module.system.service.permission.GlobalApprovalAuthorityCoordinator;
import cn.iocoder.yudao.module.system.service.permission.PermissionCommandProtocol;
import cn.iocoder.yudao.module.system.service.permission.PermissionServiceImpl;
import cn.iocoder.yudao.module.system.service.permission.RoleCategoryServiceImpl;
import cn.iocoder.yudao.module.system.service.permission.RoleServiceImpl;
import cn.iocoder.yudao.module.system.service.permission.SystemEntitlementServiceImpl;
import cn.iocoder.yudao.module.system.service.permission.TemporaryRoleGrantServiceImpl;
import cn.iocoder.yudao.module.system.service.user.AdminUserServiceImpl;

import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.datasource.config.YudaoDataSourceAutoConfiguration;
import cn.iocoder.yudao.framework.mybatis.config.YudaoMybatisAutoConfiguration;
import cn.iocoder.yudao.framework.jackson.config.YudaoJacksonAutoConfiguration;
import cn.iocoder.yudao.framework.test.config.SqlInitializationTestConfiguration;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.config.TenantProperties;
import cn.iocoder.yudao.framework.tenant.core.db.TenantDatabaseInterceptor;
import cn.iocoder.yudao.framework.mybatis.core.util.MyBatisUtils;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.module.mes.service.pro.batchrecord.*;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlinePqcContextServiceImpl;
import cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlinePqcSubmitCommand;
import cn.iocoder.yudao.module.mes.service.pro.frontline.PqcResultValueValidator;
import cn.iocoder.yudao.module.signature.api.ElectronicSignatureQueryService;
import cn.iocoder.yudao.module.system.service.gxpaudit.*;
import com.alibaba.druid.spring.boot3.autoconfigure.DruidDataSourceAutoConfigure;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.github.yulichang.autoconfigure.MybatisPlusJoinAutoConfiguration;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.jdbc.init.DataSourceScriptDatabaseInitializer;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationProperties;
import org.springframework.boot.sql.init.DatabaseInitializationMode;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import jakarta.annotation.Resource;
import javax.sql.DataSource;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real isolated H2 source producer. Synthetic upstream prerequisites are not public submit acceptance. */
@SpringBootTest(classes = MesCompletionSourceProducerPersistenceTest.Configuration.class, properties = {
        "spring.main.allow-circular-references=true",
        "spring.datasource.url=jdbc:h2:mem:ac02_completion_a_historical_r2_positive;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.sql.init.continue-on-error=false",
        "spring.sql.init.encoding=UTF-8",
        "spring.sql.init.schema-locations=classpath:/sql/mes_edhr_completion_source_producer_tables.sql",
        "yudao.info.base-package=cn.iocoder.yudao.ac02.completion.joint.scan_none",
        "mybatis-plus.configuration.local-cache-scope=STATEMENT",
        "mybatis-plus.configuration.map-underscore-to-camel-case=true",
        "mybatis-plus.global-config.db-config.id-type=AUTO",
        "mybatis-plus.global-config.db-config.logic-delete-value=1",
        "mybatis-plus.global-config.db-config.logic-not-delete-value=0",
        "yudao.security.password-encoder-length=4",
        "spring.sql.init.mode=always",
        "spring.sql.init.data-locations=",
        "spring.datasource.password=",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration"
})
class MesCompletionSourceProducerPersistenceTest {
    private static final long TENANT = 76104L, LEADER = 7610401L, OPERATOR = 7610402L,
            PQC = 7610403L, PQC_LEADER = 7610404L, WORK_ORDER = 76104101L, ORDER = 76104102L;
    private static final String SYNTHETIC = "SYNTHETIC_UPSTREAM_PROTOCOL_PREREQUISITE_ONLY";
    @Resource private ApplicationContext context;
    @Resource private JdbcTemplate jdbc;
    @Resource private PlatformTransactionManager transactionManager;
    @Resource private PasswordEncoder encoder;
    @Resource private MesTeamLeaderActiveOrderCompletionService completion;
    @Resource private MesTeamLeaderActiveOrderCompletionFlow6ReceiptPort flow6;
    @Resource private MesPqcProcessInspectionAggregationService aggregation;
    @Resource private MesProBatchRecordExecutionSignatureService signatures;
    @Resource private ElectronicSignatureQueryService signatureQuery;
    @Resource private GxpAuditService audit;
    @Resource private GxpCreationAuditVerificationService auditReader;
    @Resource private MesEdhrSourceCreationVerificationService creationReader;
    @Resource private MesProductionSignatureEvidenceService productionEvidence;
    private final Map<String,List<Long>> owned = new LinkedHashMap<>();
    private String password;
    private Long productionEventId, pqcEventId, productionReviewId, pqcReviewId, allocationId, completionSourceId;
    private List<Long> pieceIds, aggregateIds;

    @BeforeEach
    void initializeRealPrerequisites() throws Exception {
        context.getBean("jsonUtils");
        assertEquals(1, context.getBeansOfType(DataSource.class).size());
        var tenantInterceptor = context.getBean(TenantLineInnerInterceptor.class);
        var interceptors = context.getBean(MybatisPlusInterceptor.class).getInterceptors();
        assertEquals(1, interceptors.stream().filter(TenantLineInnerInterceptor.class::isInstance).count());
        assertSame(tenantInterceptor, interceptors.get(0));
        assertEquals(1, context.getBeansOfType(DataSourceScriptDatabaseInitializer.class).size());
        var initialization = context.getBean(SqlInitializationProperties.class);
        assertEquals(List.of("classpath:/sql/mes_edhr_completion_source_producer_tables.sql"), initialization.getSchemaLocations());
        assertTrue(initialization.getDataLocations() == null || initialization.getDataLocations().isEmpty());
        assertEquals(DatabaseInitializationMode.ALWAYS, initialization.getMode());
        assertFalse(initialization.isContinueOnError());
        try (var connection = context.getBean(DataSource.class).getConnection()) {
            assertTrue(connection.getMetaData().getURL().startsWith("jdbc:h2:mem:ac02_completion_a_historical_r2_positive"));
        }
        for (String table : cleanupTables()) assertEquals(0L, count(table), "Preseed database must be empty: " + table);
        TenantContextHolder.setTenantId(TENANT);
        byte[] random = new byte[32]; new SecureRandom().nextBytes(random);
        password = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        tx(() -> {
            checkedSql("INSERT INTO gxp_audit_ledger_sequence (tenant_id,next_ledger_sequence) VALUES (?,1)", TENANT);
            checkedSql("INSERT INTO gxp_audit_policy_activation (tenant_id,policy_version,policy_hash,request_id,actor_id,approval_reference,activated_at_utc,effective_after_sequence,canonical_activation_json,activation_hash) VALUES (?,?,?,?,?,?,?,?,?,?)",
                    TENANT, "AC02_COMPLETION_PROTOCOL_TEST_ONLY_V1", DigestUtil.sha256Hex("AC02_TEST_ONLY"), "AC02_TEST_ONLY_ACTIVATION",
                    LEADER, "SYNTHETIC_TEST_ONLY_NOT_PRODUCTION_APPROVAL", LocalDateTime.now(), 0L, "{}", DigestUtil.sha256Hex("AC02_ACTIVATION_TEST_ONLY"));
            policy("signature.record.create", "SIGNATURE", "SIGNATURE_RECORD", "CREATE",
                    "cn.iocoder.yudao.module.signature.service.ElectronicSignatureServiceImpl#sign");
            policy("mes.production.submit", "MES", "PROCESS_POOL_EVENT", "SUBMIT",
                    "cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesProFrontlineFeedbackSubmitServiceImpl#submit");
            policy("mes.pqc.submit", "MES", "MES_PQC_INSPECTION_TASK", "SUBMIT",
                    "cn.iocoder.yudao.module.mes.service.pro.frontline.MesFrontlinePqcContextServiceImpl#submitPqcInspection");
            policy("mes.pqc.review.approve", "MES", "PQC_REVIEW", "APPROVE",
                    "cn.iocoder.yudao.module.mes.service.pro.processpool.team.MesTeamLeaderSubmissionReviewServiceImpl#reviewSubmission");
            policy("mes.active-order.complete", "MES", "ACTIVE_ORDER_COMPLETION", "COMPLETE",
                    MesTeamLeaderActiveOrderCompletionServiceImpl.class.getName() + "#complete");
            for (long id : List.of(LEADER, OPERATOR, PQC, PQC_LEADER)) {
                // Synthetic task-owned identity initialization; this H2 fixture does not
                // exercise the formal MySQL generation/version triggers.
                checkedSql("INSERT INTO system_users (id,tenant_id,username,canonical_username,nickname,password,password_update_time,password_credential_status,status,signature_credential_generation,signature_credential_version) VALUES (?,?,?,?,?,?,?,?,0,?,1)",
                        id, TENANT, "ac02_joint_" + id, "ac02_joint_" + id, "AC02_SYNTHETIC_" + id,
                        encoder.encode(password), LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS), "ACTIVE", java.util.UUID.randomUUID().toString());
                checkedSql("INSERT INTO dcc_electronic_signature_authorization (user_id,tenant_id,electronic_signature_enabled,authorization_state) VALUES (?,?,1,'ENABLED')", id, TENANT);
            }
            checkedSql("INSERT INTO system_menu(id,name,permission,type) VALUES (76104301,'AC02 synthetic review','mes:pro-process-pool-team-leader:review',3)");
            checkedSql("INSERT INTO system_entitlement_grant(id,tenant_id,subject_type,subject_id,resolved_user_id,permission_code,menu_id,policy_code,active_claim_count,status) VALUES (76104302,?,'USER',?,?,'mes:pro-process-pool-team-leader:review',76104301,'AC02_TEST_ONLY',1,'ACTIVE')", TENANT,LEADER,LEADER);
            insert(context.getBean(MesProWorkOrderMapper.class), initialRow10());
            insert(context.getBean(MesProcessPoolActiveOrderMapper.class), initialRow11());
            var snapshot = initialRow12();
            String config = JsonUtils.toJsonString(Map.of("outputMaterialIds", List.of(76104108L)));
            snapshot.setProductionConfigSnapshotJson(config);
            snapshot.setProductionConfigSnapshotSha256(DigestUtil.sha256Hex(config));
            insert(context.getBean(MesProcessPoolActiveOrderProcessSnapshotMapper.class), snapshot);
            insert(context.getBean(ErpKingdeeProductionPickListMapper.class), initialRow13());
            insert(context.getBean(ErpKingdeeProductionPickListItemMapper.class), initialRow14());
            insert(context.getBean(MesWmProductIssueMapper.class), initialRow15());
            insert(context.getBean(MesWmProductIssueDetailMapper.class), initialRow16());
        });
        assertEquals(LocalDateTime.of(2026,10,10,0,0), jdbc.queryForObject(
                "SELECT last_sync_time FROM erp_kingdee_production_pick_list WHERE id=76104401", LocalDateTime.class));
    }

    @Test
    void realCompleteProducesAllPartsAndStrictFlow6ReceiptAndReplay() {
        history(0);
        long ledgerBefore = ledger();
        var result = completion.complete(LEADER, command());
        assertNotNull(result.getCompletionReceiptId());
        var fact = flow6.requireVerifiedByReceiptId(result.getCompletionReceiptId(), TENANT);
        assertEquals(result.getCompletionReceiptId(), fact.receipt().getReceiptId());
        var frozenBindings = JsonUtils.parseTree(fact.receipt().getFormalSourceSnapshotJson()).path("pickListBindings");
        assertEquals(frozenBindings.size(), fact.receipt().getPickListSources().size());
        assertFalse(fact.receipt().getPickListSources().isEmpty());
        var frozenPick = fact.receipt().getPickListSources().get(0);
        assertEquals(76104401L, frozenPick.getPickListId());
        assertEquals(frozenBindings.get(0).path("id").asLong(), frozenPick.getPickListBindingId());
        assertEquals(frozenBindings.get(0).path("sourceSnapshotHash").asText(), frozenPick.getSourceSnapshotHash());
        assertNotEquals(command().getIdempotencyKey(), fact.transactionId());
        assertEquals(fact.transactionId(), auditReader.requireVerifiedEvent(TENANT, fact.creation().creationEventId()).columns().transactionId());
        var main = auditReader.requireVerifiedEvent(TENANT, fact.creation().creationEventId());
        var state = JsonUtils.parseTree(main.columns().afterStateJson());
        var parts = state.path("affectedRowAuditManifest").path("parts");
        assertTrue(parts.isArray() && parts.size() >= 2, "Actual 1024-row history must emit measured multiple fragments");
        long previous = ledgerBefore - 1L;
        long actualRowBytes = 0;
        for (var part : parts) {
            var event = auditReader.requireVerifiedEvent(TENANT, part.path("eventId").asLong());
            assertEquals(fact.transactionId(), event.columns().transactionId());
            assertEquals(part.path("eventHash").asText(), event.eventHash());
            assertTrue(event.columns().ledgerSequence() > previous);
            previous = event.columns().ledgerSequence();
            for (String raw : List.of(event.columns().beforeStateJson(), event.columns().afterStateJson())) {
                for (var rows : JsonUtils.parseTree(raw).path("affectedRows")) for (var row : rows)
                    actualRowBytes += row.toString().getBytes(StandardCharsets.UTF_8).length + 1;
            }
        }
        assertTrue(actualRowBytes > 512 * 1024);
        assertTrue(main.columns().ledgerSequence() > previous);
        assertEquals(3L, count("mes_pro_process_pool_active_order_transfer_trace"));
        assertNativeJsonOutputs(fact.receipt());
        assertSourceIdPhases(fact.receipt());
        long completedLedger = ledger();
        var replay = completion.complete(LEADER, command());
        assertEquals(result.getCompletionReceiptId(), replay.getCompletionReceiptId());
        assertEquals(completedLedger, ledger());
        assertEquals(fact.creation(), creationReader.requireCompletionCreation(TENANT, result.getCompletionReceiptId()));
    }

    @Test
    void missingOriginalPiecesRejectsHistoricalAggregationAndRollsBackTheWholeGenerator() {
        String before = watermark();
        assertThrows(RuntimeException.class, () -> history(2));
        assertEquals(before, watermark());
        assertEquals(0L,count("mes_pro_process_pool_active_order_completion_receipt"));
    }

    @Test
    void oneMissingAggregateIsRejectedByRealCompletionAndKeepsDamagedBaseline() {
        history(0);
        tx(() -> checkedSql("DELETE FROM mes_pqc_process_inspection_aggregate_detail WHERE id=?", aggregateIds.get(aggregateIds.size()-1)));
        String damaged = watermark();
        assertThrows(RuntimeException.class, () -> completion.complete(LEADER, command()));
        assertEquals(damaged, watermark());
        assertEquals(0L,count("mes_pro_process_pool_active_order_completion_receipt"));
    }

    @Test
    void oneMissingOriginalPieceFailsFixtureCensusBeforeFirstAggregationAndRollsBack() {
        String before=watermark();
        assertThrows(AssertionError.class,()->history(1));
        assertEquals(before,watermark());
        assertEquals(0L,count("mes_pqc_process_inspection_aggregate_detail"));
    }

    @Test
    void allMissingAggregatesRejectRealCompleteAndPreserveCommittedDamagedBaseline() {
        history(0);
        tx(()-> {for(Long id:aggregateIds) checkedSql("DELETE FROM mes_pqc_process_inspection_aggregate_detail WHERE id=?",id);});
        assertEquals(0L,count("mes_pqc_process_inspection_aggregate_detail"));
        String damaged=watermark();
        assertThrows(RuntimeException.class,()->completion.complete(LEADER,command()));
        assertEquals(damaged,watermark());
    }

    @Test
    void corruptedOriginalProductionRawRejectsRealEvidenceWithoutSuccessfulCompletion() {
        history(0);
        tx(()->checkedSql("UPDATE mes_pro_process_pool_event SET raw_payload=? WHERE id=?",
                JsonUtils.toJsonString(Map.of("source",SYNTHETIC,"tampered",true)),productionEventId));
        var changed=context.getBean(MesProProcessPoolEventMapper.class).selectById(productionEventId);
        String damaged=watermark();
        var invalid = assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> productionEvidence.isValidForEvent(changed));
        assertEquals(MesProductionSignatureEvidenceService.EVIDENCE_INVALID.getCode(), invalid.getCode());
        assertEquals(damaged,watermark());
        assertEquals(0L,count("mes_pro_process_pool_active_order_completion_receipt"));
    }

    @Test
    void disabledRealUserCannotCreateSignatureOrAudit() {
        login(OPERATOR);
        tx(()->checkedSql("UPDATE system_users SET status=1 WHERE id=?",OPERATOR));
        String disabled=watermark();
        assertThrows(RuntimeException.class,()->tx(()->signatures.recordProductionSubmitSignature(OPERATOR,password,SYNTHETIC,
                new MesProductionSubmitSignatureContext(ORDER,76104105L,76104106L,"AC02_PROTOCOL_PRODUCTION_001"))));
        assertEquals(disabled,watermark());
        assertEquals(0L,count("system_electronic_signature"));
    }

    @Test
    void missingCompletionPolicyRollsBackReceiptBackfillsTracesAndEveryPart() {
        history(0);
        tx(() -> checkedSql("UPDATE gxp_audit_policy_operation SET active=FALSE WHERE tenant_id=? AND operation_id='mes.active-order.complete'",TENANT));
        String before = watermark();
        assertThrows(RuntimeException.class, () -> completion.complete(LEADER, command()));
        assertEquals(before, watermark());
        assertEquals(0L,count("mes_pro_process_pool_active_order_completion_receipt"));
        assertEquals(0L,count("mes_pro_process_pool_active_order_completion_backfill"));
        assertEquals(0L,count("mes_pro_process_pool_active_order_transfer_trace"));
    }

    @Test
    void mainAuditFailureAfterRealFragmentsRollsBackEntireCompletion() {
        history(0);
        String before=watermark(); long ledgerBefore=ledger();
        CompletionMainFailure.observed.set(false);
        jdbc.execute("CREATE TRIGGER ac02_fail_completion_main BEFORE INSERT ON gxp_audit_event FOR EACH ROW CALL '"
                +CompletionMainFailure.class.getName()+"'");
        try {
            assertThrows(RuntimeException.class,()->completion.complete(LEADER,command()));
            assertTrue(CompletionMainFailure.observed.get(),"Failure must follow multiple real parts and persisted receipt/backfill/three traces");
        } finally {
            jdbc.execute("DROP TRIGGER ac02_fail_completion_main");
        }
        assertEquals(before,watermark()); assertEquals(ledgerBefore,ledger());
        assertEquals(0L,count("mes_pro_process_pool_active_order_completion_receipt"));
        assertEquals(0L,count("mes_pro_process_pool_active_order_completion_backfill"));
        assertEquals(0L,count("mes_pro_process_pool_active_order_transfer_trace"));
    }

    public static class CompletionMainFailure implements org.h2.api.Trigger {
        static final java.util.concurrent.atomic.AtomicBoolean observed=new java.util.concurrent.atomic.AtomicBoolean();
        private int operationColumn,keyColumn;
        @Override public void init(java.sql.Connection connection,String schema,String name,String table,
                boolean before,int type) throws java.sql.SQLException {
            try(var columns=connection.getMetaData().getColumns(null,schema,table,null)) {
                while(columns.next()) {
                    String column=columns.getString("COLUMN_NAME");int ordinal=columns.getInt("ORDINAL_POSITION")-1;
                    if("operation_id".equalsIgnoreCase(column)) operationColumn=ordinal;
                    if("idempotency_key".equalsIgnoreCase(column)) keyColumn=ordinal;
                }
            }
        }
        @Override public void fire(java.sql.Connection connection,Object[] oldRow,Object[] newRow) throws java.sql.SQLException {
            if(!"mes.active-order.complete".equals(newRow[operationColumn])
                    || String.valueOf(newRow[keyColumn]).contains(":ROWS:")) return;
            try(var statement=connection.createStatement();var rows=statement.executeQuery(
                    "SELECT (SELECT COUNT(*) FROM gxp_audit_event WHERE operation_id='mes.active-order.complete'),"
                    +"(SELECT COUNT(*) FROM mes_pro_process_pool_active_order_completion_receipt),"
                    +"(SELECT COUNT(*) FROM mes_pro_process_pool_active_order_completion_backfill),"
                    +"(SELECT COUNT(*) FROM mes_pro_process_pool_active_order_transfer_trace)")) {
                rows.next();observed.set(rows.getLong(1)>=2 && rows.getLong(2)==1
                        && rows.getLong(3)>0 && rows.getLong(4)==3);
            }
            throw new java.sql.SQLException("AC02 injected MAIN failure after actual PART writes");
        }
    }

    @Test
    void actualWrongPasswordNeverCreatesSignatureOrAudit() {
        login(OPERATOR);
        String before = watermark();
        assertThrows(RuntimeException.class, () -> tx(() -> signatures.recordProductionSubmitSignature(OPERATOR,
                password + "_wrong", SYNTHETIC, new MesProductionSubmitSignatureContext(ORDER,76104105L,76104106L,"AC02_PROTOCOL_PRODUCTION_001"))));
        assertEquals(before, watermark());
        assertEquals(0L,count("system_electronic_signature"));
    }

    @Test
    void strictReaderRejectsMissingActualGeneratedPart() {
        history(0);
        var result = completion.complete(LEADER, command());
        var proof = creationReader.requireCompletionCreation(TENANT,result.getCompletionReceiptId());
        var main = auditReader.requireVerifiedEvent(TENANT,proof.creationEventId());
        long partId = JsonUtils.parseTree(main.columns().afterStateJson()).path("affectedRowAuditManifest").path("parts").get(0).path("eventId").asLong();
        tx(() -> {
            int relations = jdbc.update("DELETE FROM gxp_audit_event_relation WHERE event_id=? AND tenant_id=?",partId,TENANT);
            assertTrue(relations > 0);
            checkedSql("DELETE FROM gxp_audit_event WHERE id=? AND tenant_id=?",partId,TENANT);
        });
        assertThrows(RuntimeException.class, () -> flow6.requireVerifiedByReceiptId(result.getCompletionReceiptId(), TENANT));
    }

    private void history(int removePieces) {
        tx(() -> {
            login(OPERATOR);
            var feedback = initialRow6(); feedback.setFeedbackTime(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
            insert(context.getBean(MesProFeedbackMapper.class), feedback);
            var productionContext = new MesProductionSubmitSignatureContext(ORDER,76104105L,76104106L,"AC02_PROTOCOL_PRODUCTION_001");
            // A approved synthetic upstream protocol; real configured writer, actual real signatures/audit.
            String rawProduction = JsonUtils.toJsonString(Map.of("activeOrderId", ORDER,
                    "signatureIdentityDomain", "SYSTEM_USER", "source", SYNTHETIC,
                    "materialDetails", List.of(Map.of("materialId",76104108L,"outputQuantity",new BigDecimal("1024.000000")))));
            Long productionSignature = signatures.recordProductionSubmitSignature(OPERATOR,password,SYNTHETIC,productionContext);
            var production = initialRow5(); production.setDeviceAccountId(OPERATOR);
            production.setFeedbackSourceId(feedback.getId()); production.setSignatureId(productionSignature);
            production.setServerSubmitTime(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
            production.setRawPayload(rawProduction);
            production.setSignatureSnapshot(productionEvidence.snapshotForSubmission(OPERATOR,productionSignature,productionContext));
            insert(context.getBean(MesProProcessPoolEventMapper.class),production); productionEventId=production.getId();
            appendProductionAudit(production);
            assertTrue(productionEvidence.isValidForEvent(production));
            var productionReview = review(production, LEADER, initialRow7()); productionReviewId=productionReview.getId();
            var allocation=initialRow8(); allocation.setEventId(productionEventId);allocation.setReviewId(productionReviewId);
            allocation.setConfirmedAt(productionReview.getReviewedAt());
            insert(context.getBean(MesProcessPoolReportAllocationMapper.class),allocation); allocationId=allocation.getId();
            // Re-read DECIMAL scale and timestamp from the actual mapper before reproducing original aggregate seed.
            allocation=context.getBean(MesProcessPoolReportAllocationMapper.class).selectById(allocationId);
            var source=initialRow9(); source.setLastEventId(productionEventId);source.setLastReviewId(productionReviewId);
            source.setCompletedAt(productionReview.getReviewedAt());
            source.setSourceEventIdsJson(JsonUtils.toJsonString(List.of(productionEventId)));
            source.setSourceAllocationIdsJson(JsonUtils.toJsonString(List.of(allocationId)));
            String aggregateHash=DigestUtil.sha256Hex("routeProcess:76104105\nprocess:76104106\nallocation:"+allocationId+"|"+productionEventId+"|"+allocation.getAllocatedQuantity()+"|"+allocation.getConfirmedAt()+"\n");
            source.setAggregateHash(aggregateHash);
            source.setBackfillIdempotencyKey("PROCESS_POOL_REPORT_BACKFILL_AGG:76104101:76104105:76104106:"+aggregateHash);
            insert(context.getBean(MesProcessPoolOrderProcessCompletionMapper.class),source);completionSourceId=source.getId();
            var task=initialRow0(); insert(context.getBean(MesPqcInspectionTaskMapper.class),task);
            var details=new ArrayList<MesPqcInspectionPieceDetailDO>(); pieceIds=new ArrayList<>();
            var validated=PqcResultValueValidator.validate("NUMERIC","1.0",new BigDecimal("0.0"),new BigDecimal("2.0"),1);
            for(int sample=1;sample<=1024;sample++) {
                var detail=initialRow3();detail.setSampleNo(sample);detail.setItemResult(validated.normalizedValue());
                detail.setMeasuredValue(validated.normalizedValue());detail.setJudgement(validated.judgement());
                insert(context.getBean(MesPqcInspectionPieceDetailMapper.class),detail);
                details.add(detail);pieceIds.add(detail.getId());
            }
            assertEquals(1024,new HashSet<>(pieceIds).size());
            assertEquals(1024L,count("mes_pqc_inspection_piece_detail"));
            for(var row:jdbc.queryForList("SELECT sample_no,selected_equipment_id,selected_equipment_code,selected_equipment_name,selected_equipment_number FROM mes_pqc_inspection_piece_detail ORDER BY sample_no")) {
                assertEquals(76104901L,((Number)row.get("selected_equipment_id")).longValue());
                assertEquals("AC02_A_EQUIP_001",row.get("selected_equipment_code"));
                assertEquals("AC02 A numeric inspection equipment",row.get("selected_equipment_name"));
                assertEquals("AC02-A-EQ-76104-001",row.get("selected_equipment_number"));
            }
            var command=new MesFrontlinePqcSubmitCommand();command.setRawPayload(Map.of("source",SYNTHETIC));
            command.setActiveOrderId(ORDER);command.setPqcTaskId(task.getId());command.setRegulationVersionId(76104110L);
            command.setWorkOrderId(WORK_ORDER);command.setRouteId(76104103L);command.setQaProcessId(76104111L);
            command.setProductionSubmitEventId(productionEventId);command.setInspectionType("FIRST");
            command.setBusinessDate(LocalDate.of(2026,10,10));command.setShiftCode("FIRST");command.setRoundNo(1);
            command.setActualInspectionQuantity(1024);command.setScrapQuantity(0);
            String pqcRaw=MesFrontlinePqcContextServiceImpl.buildPqcInspectionEventRawPayload(command,details,"SUCCESS");
            login(PQC);Long pqcSignature=signatures.recordPqcSubmitSignature(PQC,task.getId(),password,SYNTHETIC);
            var event=initialRow1();event.setDeviceAccountId(PQC);event.setSignatureId(pqcSignature);
            event.setServerSubmitTime(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));event.setRawPayload(pqcRaw);
            event.setSignatureSnapshot(JsonUtils.toJsonString(Map.of("signatureId",pqcSignature,"actorId",PQC,
                    "requestUserId",PQC,"actionType","PQC_SUBMIT","pqcTaskId",task.getId(),
                    "performedBy",Map.of("actorId",Long.toString(PQC),"actorType","SYSTEM_USER",
                            "displayName","AC02_SYNTHETIC_"+PQC,"username","ac02_joint_"+PQC))));
            insert(context.getBean(MesProProcessPoolEventMapper.class),event);pqcEventId=event.getId();
            task.setSubmittedEventId(pqcEventId);assertEquals(1,context.getBean(MesPqcInspectionTaskMapper.class).updateById(task));
            var record=initialRow2();record.setEventId(pqcEventId);record.setProductionSubmitEventId(productionEventId);
            record.setSignatureId(pqcSignature);record.setServerSubmitTime(event.getServerSubmitTime());record.setRawPayload(pqcRaw);
            insert(context.getBean(MesProProcessPoolPqcRecordMapper.class),record);
            appendOriginalPqcAudit(task,event);
            String beforeReview=JsonUtils.toJsonString(context.getBean(MesReleaseAffectedStateCollector.class)
                    .capturePqcSubmission(event.getId(),event.getFeedbackSourceId()));
            var pqcReview=review(event,PQC_LEADER,initialRow4());pqcReviewId=pqcReview.getId();
            if(removePieces==2) {
                for(Long id:pieceIds) checkedSql("DELETE FROM mes_pqc_inspection_piece_detail WHERE id=?",id);
                assertEquals(0L,count("mes_pqc_inspection_piece_detail"));
            }
            if(removePieces==1) {
                checkedSql("DELETE FROM mes_pqc_inspection_piece_detail WHERE id=?",pieceIds.get(1023));
                assertEquals(1023L,count("mes_pqc_inspection_piece_detail"));
                // Fixture exact sample census, not a claimed existing production partial-absence rejection.
                assertEquals(1024L,count("mes_pqc_inspection_piece_detail"),"Approved A fixture must contain every original sample before its first aggregation");
            }
            aggregation.aggregateApprovedPqcSubmission(pqcEventId,pqcReviewId);
            appendOriginalPqcReviewAudit(event,pqcReview,beforeReview);
            aggregateIds=jdbc.queryForList("SELECT id FROM mes_pqc_process_inspection_aggregate_detail ORDER BY sample_no",Long.class);
            assertEquals(1024,aggregateIds.size());assertEquals(1024,new HashSet<>(aggregateIds).size());
            var aggregateRows=jdbc.queryForList("SELECT source_piece_detail_id,sample_no,selected_equipment_id,selected_equipment_code,selected_equipment_name,selected_equipment_number,item_result,measured_value,judgement FROM mes_pqc_process_inspection_aggregate_detail ORDER BY sample_no");
            for(int n=0;n<1024;n++) {
                var row=aggregateRows.get(n);assertEquals(pieceIds.get(n),((Number)row.get("source_piece_detail_id")).longValue());
                assertEquals(n+1,((Number)row.get("sample_no")).intValue());assertEquals(76104901L,((Number)row.get("selected_equipment_id")).longValue());
                assertEquals("AC02_A_EQUIP_001",row.get("selected_equipment_code"));assertEquals("AC02 A numeric inspection equipment",row.get("selected_equipment_name"));
                assertEquals("AC02-A-EQ-76104-001",row.get("selected_equipment_number"));assertEquals("1",row.get("item_result"));
                assertEquals("1",row.get("measured_value"));assertEquals("SUCCESS",row.get("judgement"));
            }
        });
        login(LEADER);
    }

    private MesProcessPoolSubmissionReviewDO review(MesProProcessPoolEventDO event,long actor,MesProcessPoolSubmissionReviewDO row) {
        login(actor);
        var signatureContext=new MesTeamLeaderReviewSignatureContext(event.getId(),"APPROVED",
                MesProBatchRecordExecutionFieldAuditHasher.hashCellValues(event.getRawPayload()),null,null,null);
        Long id=signatures.recordTeamLeaderReviewSignature(actor,password,SYNTHETIC,signatureContext);
        var evidence=signatureQuery.getById(id);assertNotNull(evidence);
        assertEquals("VALID",signatureQuery.verifyEvidence(id).verificationStatus());
        row.setEventId(event.getId());row.setReviewSignatureId(id);row.setReviewedAt(evidence.signedAt().truncatedTo(ChronoUnit.SECONDS));
        row.setReviewSignatureSnapshotJson(JsonUtils.toJsonString(Map.of("signatureId",id,"actorId",actor,
                "actionType","TEAM_LEADER_REVIEW","processPoolEventId",event.getId(),"eventType",event.getEventType(),
                "leaderType",row.getLeaderType(),"reviewStatus","APPROVED","payloadHash",signatureContext.payloadHash())));
        insert(context.getBean(MesProcessPoolSubmissionReviewMapper.class),row);return row;
    }

    private void appendProductionAudit(MesProProcessPoolEventDO event) {
        String snapshot=JsonUtils.toJsonString(Map.of("source",SYNTHETIC,"activeOrderId",ORDER));
        String hash=DigestUtil.sha256Hex(snapshot),snapshotId="AC02_SYNTHETIC_SOURCE_"+event.getId();
        var after=new LinkedHashMap<String,Object>();after.put("profile","PRODUCTION");after.put("eventId",event.getId());
        after.put("feedbackId",event.getFeedbackSourceId());after.put("activeOrderId",ORDER);after.put("routeProcessId",event.getRouteProcessId());
        after.put("processId",event.getProcessId());after.put("actualEmployeeId",OPERATOR);after.put("signatureEmployeeId",OPERATOR);
        after.put("signatureId",event.getSignatureId());after.put("formalSourceSnapshot",Map.of("snapshotId",snapshotId,"snapshotHash",hash,"content",JsonUtils.parseTree(snapshot)));
        after.put("formalSourceSnapshotHash",hash);
        var result=audit.append(GxpAuditCommand.builder().eventSchemaVersion(2).operationId("mes.production.submit")
                .subjectId("MES_PROCESS_POOL_EVENT:"+event.getId()).subjectVersion(event.getId().toString())
                .reason(SYNTHETIC).reasonCode("MES_PRODUCTION_SUBMIT").reasonSource("SYSTEM")
                .performedBy(JsonUtils.toJsonString(Map.of("actorId",OPERATOR,"tenantId",TENANT,"actorType","SYSTEM_USER")))
                .beforeState(GxpAuditStateEnvelope.builder().state("ABSENT").canonicalJson("{\"state\":\"ABSENT\"}").build())
                .afterState(GxpAuditStateEnvelope.builder().state("PRESENT").objectVersion(event.getId().toString()).canonicalJson(JsonUtils.toJsonString(after)).build())
                .idempotencyKey("PRODUCTION_SUBMIT_EVENT:"+event.getId()).requestId("MES-PRODUCTION-SUBMIT:"+event.getId())
                .resultStatus("SUCCESS").sourceType("SERVICE_METHOD")
                .sourceLocator("cn.iocoder.yudao.module.mes.service.pro.feedback.frontline.MesProFrontlineFeedbackSubmitServiceImpl#submit")
                .signatureRecordId(event.getSignatureId().toString())
                .links(List.of(new GxpAuditRelation("SUBJECT","ACTIVE_ORDER",Long.toString(ORDER),null,null),
                        new GxpAuditRelation("SOURCE","FEEDBACK",event.getFeedbackSourceId().toString(),null,null),
                        new GxpAuditRelation("SOURCE","PROCESS_POOL_EVENT",event.getId().toString(),event.getId().toString(),null),
                        new GxpAuditRelation("SIGNATURE","SIGNATURE",event.getSignatureId().toString(),null,null)))
                .evidences(List.of(new GxpAuditEvidence("FORMAL_SOURCE_SNAPSHOT",snapshotId,null,hash,"PRODUCTION"))).build());
        assertNotNull(result);assertFalse(result.replayed());assertEquals(result.eventHash(),auditReader.requireVerifiedEvent(TENANT,result.eventId()).eventHash());
    }

    // Approved A synthetic prerequisite assembly, calling original persisted audit algorithms only.
    // These helpers do not claim public upstream submit/review action acceptance.
    private void appendOriginalPqcAudit(MesPqcInspectionTaskDO task,MesProProcessPoolEventDO event) {
        // Only the private persisted-source audit method is invoked under approved A.
        // Its three real mapper dependencies and real audit writer are supplied; nulls belong
        // solely to unused upstream context/submit paths, which this fixture does not accept.
        var writer = new MesFrontlinePqcContextServiceImpl(
                null, context.getBean(MesProProcessPoolEventMapper.class),
                null, null, null, null, null, null, null, null, null, null, null, null,
                null, context.getBean(MesPqcInspectionPieceDetailMapper.class),
                null, null, null, null, context.getBean(MesProProcessPoolPqcRecordMapper.class),
                null, null, audit);
        long before=ledger();ReflectionTestUtils.invokeMethod(writer,"appendPqcSubmitGxpAudit",task,event.getId());
        assertEquals(before+1,ledger());
    }
    private void appendOriginalPqcReviewAudit(MesProProcessPoolEventDO event,MesProcessPoolSubmissionReviewDO review,String before) {
        var writer=new MesTeamLeaderSubmissionReviewServiceImpl(context.getBean(MesTeamLeaderScopeService.class),
                context.getBean(MesProProcessPoolEventMapper.class),context.getBean(MesProcessPoolSubmissionReviewMapper.class),aggregation);
        ReflectionTestUtils.setField(writer,"pqcTaskMapper",context.getBean(MesPqcInspectionTaskMapper.class));
        ReflectionTestUtils.setField(writer,"affectedStateCollector",context.getBean(MesReleaseAffectedStateCollector.class));
        ReflectionTestUtils.setField(writer,"gxpAuditService",audit);
        long prior=ledger();ReflectionTestUtils.invokeMethod(writer,"appendPqcReviewGxpAudit",event,review,before);
        assertEquals(prior+1,ledger());
    }
    private String jdbcJson(Object value) {
        assertNotNull(value,"Native JSON must not silently become SQL NULL");
        if(value instanceof byte[] bytes) return new String(bytes,StandardCharsets.UTF_8);
        assertInstanceOf(String.class,value,"Independent JDBC JSON transport must be documented String or UTF8 bytes");
        return (String)value;
    }
    private void assertNativeJsonOutputs(MesFlow6CompletionBackfillReceipt receipt) {
        for(var row:jdbc.queryForList("SELECT id,source_ids_json,payload_json FROM mes_pro_process_pool_active_order_completion_backfill ORDER BY id")) {
            var actual=context.getBean(MesProcessPoolActiveOrderCompletionBackfillMapper.class).selectById(((Number)row.get("id")).longValue());
            String ids=jdbcJson(row.get("source_ids_json")),payload=jdbcJson(row.get("payload_json"));
            assertEquals(actual.getSourceIdsJson(),ids,"Native String source IDs must preserve exact persisted bytes");
            assertEquals(actual.getPayloadJson(),payload,"Native String payload must preserve exact persisted bytes");
            assertTrue(JsonUtils.parseTree(ids).isArray(),"Native source IDs must retain ARRAY root, not quoted text");
            assertTrue(JsonUtils.parseTree(payload).isObject(),"Native payload must retain OBJECT root");
        }
        for(var row:jdbc.queryForList("SELECT id,source_snapshot_json FROM mes_pro_process_pool_active_order_transfer_trace ORDER BY id")) {
            var actual=context.getBean(MesProcessPoolActiveOrderTransferTraceMapper.class).selectById(((Number)row.get("id")).longValue());
            String snapshot=jdbcJson(row.get("source_snapshot_json"));
            assertEquals(actual.getSourceSnapshotJson(),snapshot,"Trace snapshot must preserve exact String bytes");
            assertTrue(JsonUtils.parseTree(snapshot).isObject(),"Native trace String must retain OBJECT root");
        }
        var pqcRow=jdbc.queryForMap("SELECT id,raw_payload FROM mes_pro_process_pool_pqc_record WHERE event_id=?",pqcEventId);
        String pqcRaw=jdbcJson(pqcRow.get("raw_payload"));
        var record=context.getBean(MesProProcessPoolPqcRecordMapper.class).selectById(((Number)pqcRow.get("id")).longValue());
        assertEquals(record.getRawPayload(),pqcRaw,"Native PQC raw payload must preserve exact original writer bytes");
        assertTrue(JsonUtils.parseTree(pqcRaw).isObject(),"Native PQC raw must retain OBJECT root");
        assertEquals(DigestUtil.sha256Hex(record.getRawPayload()),DigestUtil.sha256Hex(pqcRaw));
        assertFalse(receipt.getFormalSourceSnapshotJson().isBlank());
    }

    private void assertSourceIdPhases(MesFlow6CompletionBackfillReceipt receipt) {
        var expected=new ArrayList<Long>();expected.addAll(java.util.stream.Stream.of(completionSourceId,allocationId).distinct().sorted().toList());
        var bindings=context.getBean(MesProcessPoolActiveOrderPickListBindingMapper.class).selectListByActiveOrderId(ORDER);
        for(var binding:bindings) { expected.add(binding.getId());context.getBean(MesProcessPoolActiveOrderPickListBindingItemMapper.class)
                .selectListByBindingId(binding.getId()).forEach(item->expected.add(item.getId())); }
        expected.add(76104851L);expected.add(76104852L);
        assertEquals(JsonUtils.toJsonString(expected),receipt.getBatchRecordSourceIdsJson());
        var encoded=JsonUtils.parseTree(receipt.getBatchRecordSourceIdsJson());
        for(int n=0;n<expected.size();n++) {
            long id=expected.get(n);var node=encoded.get(n);
            if(id<=-9007199254740991L || id>=9007199254740991L) {
                assertTrue(node.isTextual());assertEquals(Long.toString(id),node.textValue());
            } else {assertTrue(node.isIntegralNumber());assertEquals(id,node.longValue());}
        }
        var inspection=new ArrayList<Long>(aggregateIds);inspection.add(76104109L);
        assertEquals(JsonUtils.toJsonString(inspection.stream().distinct().sorted().toList()),receipt.getProcessInspectionSourceIdsJson());
        for(long value:List.of(Long.MIN_VALUE,-9007199254740992L,-9007199254740991L,-9007199254740990L,0L,9007199254740990L,9007199254740991L,9007199254740992L,Long.MAX_VALUE)) {
            var node=JsonUtils.parseTree(JsonUtils.toJsonString(value));
            if(value>-9007199254740991L&&value<9007199254740991L) assertTrue(node.isIntegralNumber());
            else {assertTrue(node.isTextual());assertEquals(Long.toString(value),node.textValue());}
        }
    }

    private MesTeamLeaderActiveOrderCompletionCommand command() {
        return new MesTeamLeaderActiveOrderCompletionCommand().setActiveOrderId(ORDER).setExpectedVersion(0)
                .setIdempotencyKey("AC02_JOINT_COMPLETE_001").setConfirmNoReplenishmentInfo(true);
    }
    private void policy(String operation,String domain,String subject,String action,String locator) {
        String reason=operation.equals("signature.record.create")?"USER_REQUIRED":
                operation.equals("mes.pqc.review.approve")?"SYSTEM_WITH_OPTIONAL_TEXT":"SYSTEM";
        String signature=operation.equals("mes.active-order.complete")?"NONE":
                operation.equals("signature.record.create")||operation.equals("mes.pqc.review.approve")?"REQUIRED":"PARENT";
        String state=operation.equals("signature.record.create")?"PRESENT_TO_PRESENT":"ABSENT_TO_PRESENT";
        checkedSql("INSERT INTO gxp_audit_policy_operation(tenant_id,policy_version,operation_id,source_type,source_locator,domain,subject_type,action_type,reason_policy,signature_policy,state_policy,retention_class,test_ids,owner,applicability,active) VALUES (?,'AC02_COMPLETION_PROTOCOL_TEST_ONLY_V1',?,'SERVICE_METHOD',?,?,?,?,?,?,?,'TEST_ONLY','AC02_REAL_SOURCE_PRODUCER','TASK_OWNED','GXP',TRUE)",TENANT,operation,locator,domain,subject,action,reason,signature,state);
    }
    private void login(long actor) {
        var user=new LoginUser();user.setId(actor);user.setTenantId(TENANT);user.setUserType(2);
        user.setInfo(Map.of("username","ac02_joint_"+actor,LoginUser.INFO_KEY_NICKNAME,"AC02_SYNTHETIC_"+actor));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user,null,List.of()));
    }
    private void tx(Runnable action) {new TransactionTemplate(transactionManager).executeWithoutResult(status->action.run());}
    private void checkedSql(String sql,Object...args) {assertEquals(1,jdbc.update(sql,args),"Exactly one task-owned row must be affected");}
    @SuppressWarnings({"unchecked","rawtypes"})
    private void insert(BaseMapper mapper,Object row) {
        assertEquals(1,mapper.insert(row));var bean=new BeanWrapperImpl(row);Long id=((Number)bean.getPropertyValue("id")).longValue();
        assertTrue(id>0);var persisted=mapper.selectById(id);assertNotNull(persisted);
        // These legacy BaseDOs omit the Java tenant property; the real tenant interceptor
        // still writes their formal tenant_id column. Assert that persisted column directly.
        if (row instanceof MesProFeedbackDO) {
            assertEquals(TENANT, jdbc.queryForObject("SELECT tenant_id FROM mes_pro_feedback WHERE id=?", Long.class, id));
        } else if (row instanceof MesWmProductIssueDO) {
            assertEquals(TENANT, jdbc.queryForObject("SELECT tenant_id FROM mes_wm_product_issue WHERE id=?", Long.class, id));
        } else if (row instanceof MesWmProductIssueDetailDO) {
            assertEquals(TENANT, jdbc.queryForObject("SELECT tenant_id FROM mes_wm_product_issue_detail WHERE id=?", Long.class, id));
        } else {
            assertEquals(TENANT,((Number)new BeanWrapperImpl(persisted).getPropertyValue("tenantId")).longValue());
        }
    }
    private long count(String table) { assertTrue(cleanupTables().contains(table));return jdbc.queryForObject("SELECT COUNT(*) FROM "+table,Long.class); }
    private long ledger() {return jdbc.queryForObject("SELECT next_ledger_sequence FROM gxp_audit_ledger_sequence WHERE tenant_id=?",Long.class,TENANT);}
    private String watermark() {
        var rows=new LinkedHashMap<String,Object>();for(String table:cleanupTables()) rows.put(table,jdbc.queryForList("SELECT * FROM "+table+" ORDER BY "+(table.equals("gxp_audit_ledger_sequence")?"tenant_id":"id")));
        return DigestUtil.sha256Hex(JsonUtils.toJsonString(rows));
    }
    private List<String> cleanupStatements() {
        try(var in=getClass().getResourceAsStream("/sql/mes_edhr_real_source_producer_clean.sql")) {
            assertNotNull(in);return new String(in.readAllBytes(),StandardCharsets.UTF_8).lines().filter(line->line.startsWith("DELETE FROM ")).toList();
        } catch(java.io.IOException error) {throw new IllegalStateException("Finite owned cleanup resource missing",error);}
    }
    private List<String> cleanupTables() {return cleanupStatements().stream().map(line->line.split(" ")[2]).toList();}
    @AfterEach
    void cleanupOnlyCapturedTaskRows() {
        try {
            // The isolated datasource starts empty; every persisted row was created by this fixture/real writer.
            for(String table:cleanupTables()) {
                String key=table.equals("gxp_audit_ledger_sequence")?"tenant_id":"id";
                List<Long> ids=jdbc.queryForList("SELECT "+key+" FROM "+table+" ORDER BY "+key,Long.class);
                for(var row:jdbc.queryForList("SELECT * FROM "+table)) if(row.containsKey("tenant_id"))
                    assertEquals(TENANT,((Number)row.get("tenant_id")).longValue(),"Cleanup refuses foreign tenant ownership");
                if(table.equals("gxp_audit_ledger_sequence")) assertEquals(List.of(TENANT),ids);
                assertEquals(ids.size(),new HashSet<>(ids).size());owned.put(table,List.copyOf(ids));
            }
            tx(()-> {for(String table:cleanupTables()) {
                List<Long> ids=owned.get(table);if(ids.isEmpty())continue;
                String key=table.equals("gxp_audit_ledger_sequence")?"tenant_id":"id";
                String placeholders=String.join(",",Collections.nCopies(ids.size(),"?"));
                assertEquals(ids.size(),jdbc.queryForList("SELECT "+key+" FROM "+table+" WHERE "+key+" IN ("+placeholders+")",Long.class,ids.toArray()).size());
                assertEquals(ids.size(),jdbc.update("DELETE FROM "+table+" WHERE "+key+" IN ("+placeholders+")",ids.toArray()));
                assertEquals(0L,count(table));
            }});
            for(String table:cleanupTables()) assertEquals(0L,count(table));
        } finally {password=null;SecurityContextHolder.clearContext();TenantContextHolder.clear();}
    }

    private MesPqcInspectionTaskDO initialRow0() {
        var row = new MesPqcInspectionTaskDO();
        row.setId(76104109L);
        row.setActiveOrderId(76104102L);
        row.setWorkOrderId(76104101L);
        row.setRouteId(76104103L);
        row.setRouteVersionId(76104104L);
        row.setRouteProcessId(76104105L);
        row.setProcessId(76104106L);
        row.setQaProcessId(76104111L);
        row.setQaItemCode("AC02_NUMERIC_01");
        row.setRegulationVersionId(76104110L);
        row.setQaRegulationVersionNo("AC02_QA_V1");
        row.setInspectionType("FIRST");
        row.setInspectionRuleKey("FIRST");
        row.setBusinessDate(LocalDate.parse("2026-10-10"));
        row.setShiftCode("FIRST");
        row.setRoundNo(1);
        row.setPlannedInspectionQuantity(1024);
        row.setActualInspectionQuantity(1024);
        row.setTaskStatus("SUBMITTED");
        row.setSimulated(false);
        row.setTenantId(76104L);
        row.setCreator("ac02_joint_fixture");
        row.setCreateTime(LocalDateTime.parse("2026-10-10T00:00:00"));
        row.setUpdater("ac02_joint_fixture");
        row.setUpdateTime(LocalDateTime.parse("2026-10-10T00:00:00"));
        row.setDeleted(false);
        return row;
    }
    private MesProProcessPoolEventDO initialRow1() {
        var row = new MesProProcessPoolEventDO();
        row.setPoolId(76104115L);
        row.setEventType("PQC_INSPECTION");
        row.setEventIdempotencyKey("AC02_PROTOCOL_PQC_001");
        row.setWorkOrderId(76104101L);
        row.setRouteId(76104103L);
        row.setQaProcessId(76104111L);
        row.setActualEmployeeId(7610403L);
        row.setDeviceAccountId(76104112L);
        row.setDeviceId(76104114L);
        row.setWorkstationId(76104113L);
        row.setTemplateType("PQC_INSPECTION");
        row.setFeedbackSourceType("MES_PQC_INSPECTION_TASK");
        row.setFeedbackSourceId(76104109L);
        row.setRecordbookSourceType("MES_PQC_INSPECTION_TASK");
        row.setRecordbookSourceId(76104109L);
        row.setSignatureUserId(7610403L);
        row.setSimulated(false);
        row.setReportManagementStatus("NOT_APPLICABLE");
        row.setReportOutputQuantity(new BigDecimal("0.000000"));
        row.setReportAllocatedQuantity(new BigDecimal("0.000000"));
        row.setReportUnallocatedQuantity(new BigDecimal("0.000000"));
        row.setReportReleaseStatus("NOT_APPLICABLE");
        row.setTenantId(76104L);
        row.setCreator("ac02_joint_fixture");
        row.setCreateTime(LocalDateTime.parse("2026-10-10T00:00:00"));
        row.setUpdater("ac02_joint_fixture");
        row.setUpdateTime(LocalDateTime.parse("2026-10-10T00:00:00"));
        row.setDeleted(false);
        return row;
    }
    private MesProProcessPoolPqcRecordDO initialRow2() {
        var row = new MesProProcessPoolPqcRecordDO();
        row.setPoolId(76104115L);
        row.setWorkOrderId(76104101L);
        row.setRouteId(76104103L);
        row.setQaProcessId(76104111L);
        row.setActualEmployeeId(7610403L);
        row.setSignatureUserId(7610403L);
        row.setInspectionResult("SUCCESS");
        row.setSimulated(false);
        row.setProcessInspectionAggregationStatus("PENDING");
        row.setTenantId(76104L);
        row.setCreator("ac02_joint_fixture");
        row.setCreateTime(LocalDateTime.parse("2026-10-10T00:00:00"));
        row.setUpdater("ac02_joint_fixture");
        row.setUpdateTime(LocalDateTime.parse("2026-10-10T00:00:00"));
        row.setDeleted(false);
        return row;
    }
    private MesPqcInspectionPieceDetailDO initialRow3() {
        var row = new MesPqcInspectionPieceDetailDO();
        row.setTaskId(76104109L);
        row.setItemCode("AC02_NUMERIC_01");
        row.setItemName("AC02 numeric item");
        row.setInspectionMethod("AC02_FIXED_NUMERIC");
        row.setStandardText("0.0 <= value <= 2.0");
        row.setSelectedEquipmentId(76104901L);
        row.setSelectedEquipmentCode("AC02_A_EQUIP_001");
        row.setSelectedEquipmentName("AC02 A numeric inspection equipment");
        row.setSelectedEquipmentNumber("AC02-A-EQ-76104-001");
        row.setStandardLowerLimit(new BigDecimal("0.0"));
        row.setStandardUpperLimit(new BigDecimal("2.0"));
        row.setStandardUnit("mm");
        row.setStandardPrecision(1);
        row.setResultType("NUMERIC");
        row.setSimulated(false);
        row.setTenantId(76104L);
        row.setCreator("ac02_joint_fixture");
        row.setCreateTime(LocalDateTime.parse("2026-10-10T00:00:00"));
        row.setUpdater("ac02_joint_fixture");
        row.setUpdateTime(LocalDateTime.parse("2026-10-10T00:00:00"));
        row.setDeleted(false);
        return row;
    }
    private MesProcessPoolSubmissionReviewDO initialRow4() {
        var row = new MesProcessPoolSubmissionReviewDO();
        row.setLeaderUserId(7610404L);
        row.setLeaderType("PQC");
        row.setReviewStatus("APPROVED");
        row.setReviewRemark("SYNTHETIC_UPSTREAM_PROTOCOL_PREREQUISITE_ONLY");
        row.setReviewSignatureUserId(7610404L);
        row.setReviewRound(0);
        row.setSimulated(false);
        row.setTenantId(76104L);
        row.setCreator("ac02_joint_fixture");
        row.setCreateTime(LocalDateTime.parse("2026-10-10T00:00:00"));
        row.setUpdater("ac02_joint_fixture");
        row.setUpdateTime(LocalDateTime.parse("2026-10-10T00:00:00"));
        row.setDeleted(false);
        return row;
    }
    private MesProProcessPoolEventDO initialRow5() {
        var row = new MesProProcessPoolEventDO();
        row.setPoolId(76104115L);
        row.setEventType("PRODUCTION_SUBMIT");
        row.setEventIdempotencyKey("AC02_PROTOCOL_PRODUCTION_001");
        row.setWorkOrderId(76104101L);
        row.setRouteId(76104103L);
        row.setRouteProcessId(76104105L);
        row.setProcessId(76104106L);
        row.setActualEmployeeId(7610402L);
        row.setDeviceAccountId(76104112L);
        row.setDeviceId(76104114L);
        row.setWorkstationId(76104113L);
        row.setTemplateType("PRODUCTION");
        row.setFeedbackSourceType("MES_PRO_FEEDBACK");
        row.setSignatureUserId(7610402L);
        row.setSimulated(false);
        row.setReportManagementStatus("PENDING_RELEASE");
        row.setReportOutputQuantity(new BigDecimal("1024.000000"));
        row.setReportAllocatedQuantity(new BigDecimal("1024.000000"));
        row.setReportUnallocatedQuantity(new BigDecimal("0.000000"));
        row.setReportReleaseStatus("NOT_RELEASED");
        row.setTenantId(76104L);
        row.setCreator("ac02_joint_fixture");
        row.setCreateTime(LocalDateTime.parse("2026-10-10T00:00:00"));
        row.setUpdater("ac02_joint_fixture");
        row.setUpdateTime(LocalDateTime.parse("2026-10-10T00:00:00"));
        row.setDeleted(false);
        return row;
    }
    private MesProFeedbackDO initialRow6() {
        var row = new MesProFeedbackDO();
        row.setCode("AC02_PROTOCOL_FEEDBACK_001");
        row.setType(1);
        row.setChannel("FRONTLINE");
        row.setWorkstationId(76104113L);
        row.setRouteId(76104103L);
        row.setProcessId(76104106L);
        row.setWorkOrderId(76104101L);
        row.setItemId(76104107L);
        row.setLotNumber("AC02-BATCH-76104-001");
        row.setScheduledQuantity(new BigDecimal("1024.00"));
        row.setFeedbackQuantity(new BigDecimal("1024.00"));
        row.setQualifiedQuantity(new BigDecimal("1024.00"));
        row.setUnqualifiedQuantity(new BigDecimal("0.00"));
        row.setUncheckQuantity(new BigDecimal("0.00"));
        row.setLaborScrapQuantity(new BigDecimal("0.00"));
        row.setMaterialScrapQuantity(new BigDecimal("0.00"));
        row.setOtherScrapQuantity(new BigDecimal("0.00"));
        row.setFeedbackUserId(7610402L);
        row.setApproveUserId(7610401L);
        row.setStatus(4);
        row.setRemark("SYNTHETIC_UPSTREAM_PROTOCOL_PREREQUISITE_ONLY");
        row.setCreator("ac02_joint_fixture");
        row.setCreateTime(LocalDateTime.parse("2026-10-10T00:00:00"));
        row.setUpdater("ac02_joint_fixture");
        row.setUpdateTime(LocalDateTime.parse("2026-10-10T00:00:00"));
        row.setDeleted(false);
        return row;
    }
    private MesProcessPoolSubmissionReviewDO initialRow7() {
        var row = new MesProcessPoolSubmissionReviewDO();
        row.setLeaderUserId(7610401L);
        row.setLeaderType("PRODUCTION");
        row.setReviewStatus("APPROVED");
        row.setReviewRemark("SYNTHETIC_UPSTREAM_PROTOCOL_PREREQUISITE_ONLY");
        row.setReviewSignatureUserId(7610401L);
        row.setReviewRound(0);
        row.setSimulated(false);
        row.setTenantId(76104L);
        row.setCreator("ac02_joint_fixture");
        row.setCreateTime(LocalDateTime.parse("2026-10-10T00:00:00"));
        row.setUpdater("ac02_joint_fixture");
        row.setUpdateTime(LocalDateTime.parse("2026-10-10T00:00:00"));
        row.setDeleted(false);
        return row;
    }
    private MesProcessPoolReportAllocationDO initialRow8() {
        var row = new MesProcessPoolReportAllocationDO();
        row.setLeaderUserId(7610401L);
        row.setActiveOrderId(76104102L);
        row.setWorkOrderId(76104101L);
        row.setRouteProcessId(76104105L);
        row.setProcessId(76104106L);
        row.setAllocatedQuantity(new BigDecimal("1024.000000"));
        row.setAllocationMode("MANUAL");
        row.setLifecycleStatus("CURRENT");
        row.setCreatedVersion(1);
        row.setSimulated(false);
        row.setTenantId(76104L);
        row.setCreator("ac02_joint_fixture");
        row.setCreateTime(LocalDateTime.parse("2026-10-10T00:00:00"));
        row.setUpdater("ac02_joint_fixture");
        row.setUpdateTime(LocalDateTime.parse("2026-10-10T00:00:00"));
        row.setDeleted(false);
        return row;
    }
    private MesProcessPoolOrderProcessCompletionDO initialRow9() {
        var row = new MesProcessPoolOrderProcessCompletionDO();
        row.setWorkOrderId(76104101L);
        row.setRouteProcessId(76104105L);
        row.setProcessId(76104106L);
        row.setTargetQuantity(new BigDecimal("1024.000000"));
        row.setConfirmedQuantity(new BigDecimal("1024.000000"));
        row.setCompletionStatus("COMPLETED");
        row.setBackfillStatus("NOT_REQUIRED");
        row.setTenantId(76104L);
        row.setCreator("ac02_joint_fixture");
        row.setCreateTime(LocalDateTime.parse("2026-10-10T00:00:00"));
        row.setUpdater("ac02_joint_fixture");
        row.setUpdateTime(LocalDateTime.parse("2026-10-10T00:00:00"));
        row.setDeleted(false);
        return row;
    }
    private MesProWorkOrderDO initialRow10() {
        var row = new MesProWorkOrderDO();
        row.setId(76104101L);
        row.setCode("AC02_JOINT_WORK_ORDER_76104");
        row.setName("AC02 protocol initial work order");
        row.setProductId(76104107L);
        row.setQuantity(new BigDecimal("1024.000000"));
        row.setQuantityProduced(new BigDecimal("0.000000"));
        row.setQuantityChanged(new BigDecimal("0.000000"));
        row.setQuantityScheduled(new BigDecimal("0.000000"));
        row.setBatchCode("AC02-BATCH-76104-001");
        row.setTemporaryFrozen(false);
        row.setRemark("SYNTHETIC_UPSTREAM_PROTOCOL_PREREQUISITE_ONLY");
        row.setTenantId(76104L);
        row.setCreator("ac02_joint_fixture");
        row.setCreateTime(LocalDateTime.parse("2026-10-10T00:00:00"));
        row.setUpdater("ac02_joint_fixture");
        row.setUpdateTime(LocalDateTime.parse("2026-10-10T00:00:00"));
        row.setDeleted(false);
        return row;
    }
    private MesProcessPoolActiveOrderDO initialRow11() {
        var row = new MesProcessPoolActiveOrderDO();
        row.setId(76104102L);
        row.setLeaderUserId(7610401L);
        row.setWorkOrderId(76104101L);
        row.setRouteId(76104103L);
        row.setRouteVersionId(76104104L);
        row.setErpFixedQuantitySnapshot(new BigDecimal("1024.000000"));
        row.setActiveStatus("ACTIVE");
        row.setBusinessStatus("ACTIVE");
        row.setJoinedAt(LocalDateTime.parse("2026-10-10T00:00:00"));
        row.setSortOrder(1L);
        row.setSimulated(false);
        row.setVersion(0);
        row.setTenantId(76104L);
        row.setCreator("ac02_joint_fixture");
        row.setCreateTime(LocalDateTime.parse("2026-10-10T00:00:00"));
        row.setUpdater("ac02_joint_fixture");
        row.setUpdateTime(LocalDateTime.parse("2026-10-10T00:00:00"));
        row.setDeleted(false);
        return row;
    }
    private MesProcessPoolActiveOrderProcessSnapshotDO initialRow12() {
        var row = new MesProcessPoolActiveOrderProcessSnapshotDO();
        row.setId(76104116L);
        row.setActiveOrderId(76104102L);
        row.setWorkOrderId(76104101L);
        row.setRouteId(76104103L);
        row.setRouteVersionId(76104104L);
        row.setRouteProcessId(76104105L);
        row.setProcessId(76104106L);
        row.setProcessCodeSnapshot("AC02_PROC_76104");
        row.setProcessNameSnapshot("AC02 initial protocol process");
        row.setErpFixedQuantitySnapshot(new BigDecimal("1024.000000"));
        row.setProductionQuantityFactorSnapshot(new BigDecimal("1.000000"));
        row.setPlannedQuantitySnapshot(new BigDecimal("1024.000000"));
        row.setParameterSnapshotState("MISSING_LEGACY");
        row.setSimulated(false);
        row.setTenantId(76104L);
        row.setCreator("ac02_joint_fixture");
        row.setCreateTime(LocalDateTime.parse("2026-10-10T00:00:00"));
        row.setUpdater("ac02_joint_fixture");
        row.setUpdateTime(LocalDateTime.parse("2026-10-10T00:00:00"));
        row.setDeleted(false);
        return row;
    }
    private ErpKingdeeProductionPickListDO initialRow13() {
        var row = new ErpKingdeeProductionPickListDO();
        row.setId(76104401L);
        row.setSourceFormId("PRD_PickMtrl");
        row.setSourceFid("AC02_SOURCE_FID_76104");
        row.setSourceBillNo("AC02_PICK_76104");
        row.setDocumentStatus("C");
        row.setDescription("SYNTHETIC_UPSTREAM_PROTOCOL_PREREQUISITE_ONLY");
        row.setLastSyncTime(LocalDateTime.parse("2026-10-10T00:00:00"));
        row.setTenantId(76104L);
        row.setCreator("ac02_joint_fixture");
        row.setCreateTime(LocalDateTime.parse("2026-10-10T00:00:00"));
        row.setUpdater("ac02_joint_fixture");
        row.setUpdateTime(LocalDateTime.parse("2026-10-10T00:00:00"));
        row.setDeleted(false);
        return row;
    }
    private ErpKingdeeProductionPickListItemDO initialRow14() {
        var row = new ErpKingdeeProductionPickListItemDO();
        row.setId(76104402L);
        row.setProductionPickListId(76104401L);
        row.setSourceFormId("PRD_PickMtrl");
        row.setSourceFid("AC02_SOURCE_FID_76104");
        row.setSourceEntryId("AC02_ENTRY_1");
        row.setSourceLineKey("AC02_SOURCE_FID_76104:AC02_ENTRY_1");
        row.setSourceBillNo("AC02_PICK_76104");
        row.setMaterialNumber("AC02_MATERIAL_76104108");
        row.setMaterialName("AC02 task initial material");
        row.setRequestedQuantity(new BigDecimal("1024.000000"));
        row.setActualQuantity(new BigDecimal("1024.000000"));
        row.setProductionOrderNo("AC02_JOINT_WORK_ORDER_76104");
        row.setLastSyncTime(LocalDateTime.parse("2026-10-10T00:00:00"));
        row.setTenantId(76104L);
        row.setCreator("ac02_joint_fixture");
        row.setCreateTime(LocalDateTime.parse("2026-10-10T00:00:00"));
        row.setUpdater("ac02_joint_fixture");
        row.setUpdateTime(LocalDateTime.parse("2026-10-10T00:00:00"));
        row.setDeleted(false);
        return row;
    }
    private MesWmProductIssueDO initialRow15() {
        var row = new MesWmProductIssueDO();
        row.setId(76104851L);
        row.setWorkOrderId(76104101L);
        row.setCode("AC02_COMPLETION_TEST_ONLY_ISSUE_001");
        row.setStatus(4);
        row.setDeleted(false);
        row.setIssueDate(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        row.setWorkstationId(76104113L);
        row.setRemark("SYNTHETIC_UPSTREAM_PROTOCOL_PREREQUISITE_ONLY");
        return row;
    }
    private MesWmProductIssueDetailDO initialRow16() {
        var row = new MesWmProductIssueDetailDO();
        row.setId(76104852L);
        row.setIssueId(76104851L);
        row.setLineId(76104853L);
        row.setMaterialStockId(76104854L);
        row.setItemId(76104108L);
        row.setBatchId(76104855L);
        row.setBatchCode("AC02_COMPLETION_TEST_ONLY_BATCH_001");
        row.setQuantity(new BigDecimal("1024.000000"));
        row.setDeleted(false);
        row.setRemark("SYNTHETIC_UPSTREAM_PROTOCOL_PREREQUISITE_ONLY");
        return row;
    }
    @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    @org.springframework.boot.context.properties.EnableConfigurationProperties(SqlInitializationProperties.class)
    @ImportAutoConfiguration({DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class,
            DruidDataSourceAutoConfigure.class, YudaoDataSourceAutoConfiguration.class, JacksonAutoConfiguration.class,
            YudaoJacksonAutoConfiguration.class, YudaoMybatisAutoConfiguration.class, MybatisPlusAutoConfiguration.class,
            MybatisPlusJoinAutoConfiguration.class})
    @Import({SpringUtil.class,
            MesTeamLeaderActiveOrderCompletionServiceImpl.class,
            MesProBatchRecordExecutionSignatureService.class,
            MesTeamLeaderActiveOrderCompletionProgressPortImpl.class,
            MesTeamLeaderActiveOrderCompletionBackfillPortImpl.class,
            MesTeamLeaderActiveOrderPickListCompletionSourceService.class,
            MesActiveOrderTransferTraceServiceImpl.class,
            MesPqcProcessInspectionAggregationServiceImpl.class,
            GxpAuditServiceImpl.class,
            MesReleaseAffectedStateCollector.class,
            MesActiveOrderHandoffService.class,
            AdminUserServiceImpl.class,
            ApprovalAuthorityService.class,
            GlobalApprovalAuthorityCoordinator.class,
            DeptServiceImpl.class,
            PostServiceImpl.class,
            PermissionServiceImpl.class,
            RoleServiceImpl.class,
            DccElectronicSignatureAuthorizationServiceImpl.class,
            AdminUserApiImpl.class,
            ElectronicSignatureServiceImpl.class,
            MesTeamLeaderActiveOrderReleaseLossSourceReaderImpl.class,
            MesFormalProductionPickListSourceResolver.class,
            MesActiveOrderHandoffOwnerResolver.class,
            MesQaHandoffAssignmentService.class,
            MesPqcHandoffAssignmentService.class,
            MesActiveOrderHandoffDeliveryService.class,
            MesActiveOrderHandoffAudit.class,
            MesSignedReturnCorrectionResolver.class,
            ConfigApiImpl.class,
            MenuServiceImpl.class,
            SystemEntitlementServiceImpl.class,
            TemporaryRoleGrantServiceImpl.class,
            PermissionCommandProtocol.class,
            RoleCategoryServiceImpl.class,
            DccElectronicSignatureAuthorizationAuditServiceImpl.class,
            MesProductionSignatureEvidenceService.class,
            MesFrontlineProcessMaterialServiceImpl.class,
            MesTeamLeaderScopeServiceImpl.class,
            PermissionApiImpl.class,
            MesSubmissionSignatureIdentityReader.class,
            ElectronicSignatureQueryServiceImpl.class,
            ElectronicSignatureVerifiedFactService.class,
            MesProEdhrCandidateResolver.class,
            MesProEdhrOperationAuditServiceImpl.class,
            MesActiveOrderHandoffDeliveryTransactionService.class,
            MesReleaseTaskNotificationPlatformSender.class,
            MesCorrectionSignatureEvidenceReader.class,
            ConfigServiceImpl.class,
            NotifySendServiceImpl.class,
            ActiveOrderSnapshotResolver.class,
            MesProductionSubmissionReadBinding.class,
            SignatureCreationVerificationService.class,
            DeptApiImpl.class,
            NotifyMessageSendApiImpl.class,
            NotifyTemplateServiceImpl.class,
            NotifyMessageServiceImpl.class,
            MesEdhrSourceCreationVerificationService.class,
            MesEdhrSourceOwnerIntegrity.class,
            MesEdhrSourceCreationBindingStore.class,
            MesEdhrSourceCreationProtocolRegistry.class,
            GxpCreationAuditVerificationService.class,
            MesBatchRecordSignatureSubjectAdapter.class,
            MesTeamLeaderActiveOrderCompletionFlow6ReceiptPortImpl.class})
    static class Configuration {
        // Register the actual strict Spring initializer directly: the conditional test
        // configuration can be parsed before datasource auto-configuration is registered.
        @Bean
        DataSourceScriptDatabaseInitializer dataSourceScriptDatabaseInitializer(
                DataSource dataSource, SqlInitializationProperties properties) {
            return new SqlInitializationTestConfiguration()
                    .dataSourceScriptDatabaseInitializer(dataSource, properties);
        }
        @Bean JdbcTemplate jdbcTemplate(DataSource dataSource) { return new JdbcTemplate(dataSource); }
        @Bean
        TenantLineInnerInterceptor tenantLineInnerInterceptor(MybatisPlusInterceptor interceptor) {
            if (interceptor.getInterceptors().stream().anyMatch(TenantLineInnerInterceptor.class::isInstance)) {
                throw new IllegalStateException("Duplicate tenant interceptor in isolated completion fixture");
            }
            var inner = new TenantLineInnerInterceptor(new TenantDatabaseInterceptor(new TenantProperties()));
            MyBatisUtils.addInterceptor(interceptor, inner, 0);
            return inner;
        }
        @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(4); }
        @Bean java.time.Clock approvalAuthorityClock() { return java.time.Clock.systemUTC(); }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<DccElectronicSignatureAuthorizationAuditMapper> dccElectronicSignatureAuthorizationAuditMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(DccElectronicSignatureAuthorizationAuditMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<DccElectronicSignatureAuthorizationMapper> dccElectronicSignatureAuthorizationMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(DccElectronicSignatureAuthorizationMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<ErpKingdeeProductionPickListItemMapper> erpKingdeeProductionPickListItemMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(ErpKingdeeProductionPickListItemMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<ErpKingdeeProductionPickListMapper> erpKingdeeProductionPickListMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(ErpKingdeeProductionPickListMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<ErpKingdeeProductionReplenishmentListItemMapper> erpKingdeeProductionReplenishmentListItemMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(ErpKingdeeProductionReplenishmentListItemMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<ErpKingdeeProductionReplenishmentListMapper> erpKingdeeProductionReplenishmentListMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(ErpKingdeeProductionReplenishmentListMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<ConfigMapper> configMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(ConfigMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<MesMdItemMapper> mesMdItemMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(MesMdItemMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<MesEdhrSourceCreationBindingMapper> mesEdhrSourceCreationBindingMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(MesEdhrSourceCreationBindingMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<MesEdhrReleaseSubmissionMaterialAssociationMapper> mesEdhrReleaseSubmissionMaterialAssociationMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(MesEdhrReleaseSubmissionMaterialAssociationMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<MesEdhrManagedHandoffMaterialAssociationMapper> mesEdhrManagedHandoffMaterialAssociationMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(MesEdhrManagedHandoffMaterialAssociationMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<ApprovalAuthorityRevisionMapper> approvalAuthorityRevisionMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(ApprovalAuthorityRevisionMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<ApprovalAuthorityTenantScopeMapper> approvalAuthorityTenantScopeMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(ApprovalAuthorityTenantScopeMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<MesIndependentBatchPrerequisiteReceiptMapper> mesIndependentBatchPrerequisiteReceiptMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(MesIndependentBatchPrerequisiteReceiptMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<MesProBatchRecordExecutionSignatureMapper> mesProBatchRecordExecutionSignatureMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(MesProBatchRecordExecutionSignatureMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<MesProEdhrMaterialGateReceiptMapper> mesProEdhrMaterialGateReceiptMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(MesProEdhrMaterialGateReceiptMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<MesProEdhrOperationAuditEventMapper> mesProEdhrOperationAuditEventMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(MesProEdhrOperationAuditEventMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<MesProEdhrWorkTaskAssignmentRuleMapper> mesProEdhrWorkTaskAssignmentRuleMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(MesProEdhrWorkTaskAssignmentRuleMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<MesProFeedbackMapper> mesProFeedbackMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(MesProFeedbackMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<MesActiveOrderHandoffDeliveryMapper> mesActiveOrderHandoffDeliveryMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(MesActiveOrderHandoffDeliveryMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<MesActiveOrderHandoffTaskMapper> mesActiveOrderHandoffTaskMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(MesActiveOrderHandoffTaskMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<MesProProcessMapper> mesProProcessMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(MesProProcessMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<MesProProcessPoolEventMapper> mesProProcessPoolEventMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(MesProProcessPoolEventMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<MesProProcessPoolEventRevisionDiffMapper> mesProProcessPoolEventRevisionDiffMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(MesProProcessPoolEventRevisionDiffMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<MesProProcessPoolEventRevisionMapper> mesProProcessPoolEventRevisionMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(MesProProcessPoolEventRevisionMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<MesProProcessPoolPqcRecordMapper> mesProProcessPoolPqcRecordMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(MesProProcessPoolPqcRecordMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<MesPqcInspectionPieceDetailMapper> mesPqcInspectionPieceDetailMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(MesPqcInspectionPieceDetailMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<MesPqcInspectionTaskMapper> mesPqcInspectionTaskMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(MesPqcInspectionTaskMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<MesPqcProcessInspectionAggregateDetailMapper> mesPqcProcessInspectionAggregateDetailMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(MesPqcProcessInspectionAggregateDetailMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<MesProcessPoolActiveOrderCompletionBackfillMapper> mesProcessPoolActiveOrderCompletionBackfillMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(MesProcessPoolActiveOrderCompletionBackfillMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<MesProcessPoolActiveOrderCompletionReceiptMapper> mesProcessPoolActiveOrderCompletionReceiptMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(MesProcessPoolActiveOrderCompletionReceiptMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<MesProcessPoolActiveOrderMapper> mesProcessPoolActiveOrderMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(MesProcessPoolActiveOrderMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<MesProcessPoolActiveOrderPickListBindingItemMapper> mesProcessPoolActiveOrderPickListBindingItemMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(MesProcessPoolActiveOrderPickListBindingItemMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<MesProcessPoolActiveOrderPickListBindingMapper> mesProcessPoolActiveOrderPickListBindingMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(MesProcessPoolActiveOrderPickListBindingMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<MesProcessPoolActiveOrderProcessSnapshotMapper> mesProcessPoolActiveOrderProcessSnapshotMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(MesProcessPoolActiveOrderProcessSnapshotMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<MesProcessPoolActiveOrderTransferTraceMapper> mesProcessPoolActiveOrderTransferTraceMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(MesProcessPoolActiveOrderTransferTraceMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<MesProcessPoolOrderProcessCompletionMapper> mesProcessPoolOrderProcessCompletionMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(MesProcessPoolOrderProcessCompletionMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<MesProcessPoolReportAllocationMapper> mesProcessPoolReportAllocationMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(MesProcessPoolReportAllocationMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<MesProcessPoolSubmissionReviewMapper> mesProcessPoolSubmissionReviewMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(MesProcessPoolSubmissionReviewMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<MesProcessPoolTeamEmployeeProfileMapper> mesProcessPoolTeamEmployeeProfileMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(MesProcessPoolTeamEmployeeProfileMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<MesProcessPoolTeamLeaderScopeMapper> mesProcessPoolTeamLeaderScopeMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(MesProcessPoolTeamLeaderScopeMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<MesProRouteMapper> mesProRouteMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(MesProRouteMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<MesProRouteProcessMapper> mesProRouteProcessMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(MesProRouteProcessMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<MesProRouteVersionMapper> mesProRouteVersionMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(MesProRouteVersionMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<MesProWorkOrderMapper> mesProWorkOrderMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(MesProWorkOrderMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<MesWmProductIssueDetailMapper> mesWmProductIssueDetailMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(MesWmProductIssueDetailMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<MesWmProductIssueMapper> mesWmProductIssueMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(MesWmProductIssueMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<MesWmTransferDetailMapper> mesWmTransferDetailMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(MesWmTransferDetailMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<MesWmTransferLineMapper> mesWmTransferLineMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(MesWmTransferLineMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<MesWmTransferMapper> mesWmTransferMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(MesWmTransferMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<ElectronicSignatureRecordMapper> electronicSignatureRecordMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(ElectronicSignatureRecordMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<DeptMapper> deptMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(DeptMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<PostMapper> postMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(PostMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<UserPostMapper> userPostMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(UserPostMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<GxpAuditEventMapper> gxpAuditEventMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(GxpAuditEventMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<GxpAuditEventRelationMapper> gxpAuditEventRelationMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(GxpAuditEventRelationMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<GxpAuditLedgerSequenceMapper> gxpAuditLedgerSequenceMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(GxpAuditLedgerSequenceMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<GxpAuditPolicyActivationMapper> gxpAuditPolicyActivationMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(GxpAuditPolicyActivationMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<GxpAuditPolicyOperationMapper> gxpAuditPolicyOperationMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(GxpAuditPolicyOperationMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<NotifyMessageMapper> notifyMessageMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(NotifyMessageMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<NotifyTemplateMapper> notifyTemplateMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(NotifyTemplateMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<MenuMapper> menuMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(MenuMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<PermissionCommandReceiptMapper> permissionCommandReceiptMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(PermissionCommandReceiptMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<RoleCategoryMapper> roleCategoryMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(RoleCategoryMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<RoleMapper> roleMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(RoleMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<RoleMenuMapper> roleMenuMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(RoleMenuMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<SystemEntitlementAuditEventMapper> systemEntitlementAuditEventMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(SystemEntitlementAuditEventMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<SystemEntitlementClaimMapper> systemEntitlementClaimMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(SystemEntitlementClaimMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<SystemEntitlementGrantMapper> systemEntitlementGrantMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(SystemEntitlementGrantMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<SystemEntitlementPolicyMapper> systemEntitlementPolicyMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(SystemEntitlementPolicyMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<TemporaryRoleGrantAuditMapper> temporaryRoleGrantAuditMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(TemporaryRoleGrantAuditMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<TemporaryRoleGrantMapper> temporaryRoleGrantMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(TemporaryRoleGrantMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<UserRoleMapper> userRoleMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(UserRoleMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<TenantMapper> tenantMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(TenantMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<AdminUserMapper> adminUserMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(AdminUserMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
        @Bean @DependsOn({"jsonUtils", "dataSourceScriptDatabaseInitializer"})
        MapperFactoryBean<AdminUserPasswordHistoryMapper> adminUserPasswordHistoryMapper(SqlSessionFactory factory) {
            var bean = new MapperFactoryBean<>(AdminUserPasswordHistoryMapper.class);
            bean.setSqlSessionFactory(factory);
            return bean;
        }
    }
}
