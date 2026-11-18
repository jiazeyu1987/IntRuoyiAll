<template>
  <div v-loading="loading" class="edhr-detail-surface">
    <el-alert v-if="error" type="error" :closable="false" :title="error" />
    <template v-else-if="detail">
      <el-alert v-if="userNamesError" type="error" :closable="false" :title="userNamesError" class="mb-12px">
        <el-button size="default" link type="primary" @click="loadUserNames">重试</el-button>
      </el-alert>
      <dl class="edhr-detail-summary">
        <div>
          <dt>偏差编号</dt>
          <dd>{{ detail.deviationCode }}</dd>
        </div>
        <div>
          <dt>等级</dt>
          <dd>{{ detail.level === "CRITICAL" ? "重大（关键）" : "普通" }}</dd>
        </div>
        <div>
          <dt>状态</dt>
          <dd>{{ detail.status === "OPEN" ? "未处理" : "已处理" }}</dd>
        </div>
      </dl>
      <el-alert v-if="detail.status === 'OPEN' && detail.receiverId == null" title="此历史偏差缺少调查接收人，须由有效偏差管理员明确补录后办理。" type="warning" :closable="false" class="mb-12px">
        <DeviationResponsibilityRecovery v-if="canAssignReceiver" mode="assign" :deviation-id="props.id" :deviation-version="detail.version" :disabled="loading || handlingSaving" @changed="load" />
      </el-alert>
      <el-tabs v-model="detailTab" class="deviation-detail-tabs">
        <el-tab-pane label="关联业务" name="business">
          <section class="edhr-detail-section">
            <dl class="edhr-detail-fields">
              <div v-if="detail.activeOrderId">
                <dt>生产订单</dt>
                <dd>{{ detail.workOrderCode || '—' }}</dd>
              </div>
              <div>
                <dt>关联批记录</dt>
                <dd>
                  <el-button size="default" v-if="detail.batchExecutionId" link type="primary" @click="openBatch">{{
                    detail.batchExecutionCode || detail.batchExecutionId
                  }}</el-button
                  ><span v-else>—</span>
                </dd>
              </div>
              <div>
                <dt>批号</dt>
                <dd>{{ detail.batchCode || "—" }}</dd>
              </div>
              <div>
                <dt>产品名称</dt>
                <dd>{{ detail.productName || "—" }}</dd>
              </div>
              <div>
                <dt>产品规格</dt>
                <dd>{{ detail.productSpecification || "—" }}</dd>
              </div>
              <div>
                <dt>设备或系统</dt>
                <dd>{{ detail.equipmentOrSystem || "—" }}</dd>
              </div>
            </dl>
          </section>
        </el-tab-pane>
        <el-tab-pane label="发现与接收" name="discovery">
          <section class="edhr-detail-section">
            <dl class="edhr-detail-fields">
              <div>
                <dt>发现部门</dt>
                <dd>{{ detail.discoveryDepartmentName || "—" }}</dd>
              </div>
              <div>
                <dt>发现人</dt>
                <dd>{{ detail.discovererName || "—" }}</dd>
              </div>
              <div>
                <dt>发现时间</dt>
                <dd>{{ formatTime(detail.discoveredAt) }}</dd>
              </div>
              <div>
                <dt>发现地点</dt>
                <dd>{{ detail.discoveryLocation || "—" }}</dd>
              </div>
              <div>
                <dt>报告时间</dt>
                <dd>{{ formatTime(detail.reportedAt) }}</dd>
              </div>
              <div>
                <dt>接收人</dt>
                <dd>{{ detail.receiverName || "—" }}</dd>
              </div>
            </dl>
          </section>
        </el-tab-pane>
        <el-tab-pane label="偏差说明" name="description">
          <section class="edhr-detail-section">
            <dl class="edhr-detail-fields edhr-detail-fields--soft">
              <div>
                <dt>偏差类别</dt>
                <dd>
                  <BusinessEvidenceDetails
                    title="类别"
                    field="categoryCodesJson"
                    :source="detail.categoryCodesJson"
                  />
                </dd>
              </div>
              <div>
                <dt>偏差描述</dt>
                <dd>{{ detail.description || "—" }}</dd>
              </div>
              <div>
                <dt>紧急措施</dt>
                <dd>{{ detail.emergencyAction || "—" }}</dd>
              </div>
              <div>
                <dt>等级依据</dt>
                <dd>{{ detail.levelBasis || "—" }}</dd>
              </div>
            </dl>
          </section>
        </el-tab-pane>
        <el-tab-pane label="处置与签署" name="disposition">
          <section class="edhr-detail-section">
            <dl class="edhr-detail-fields">
              <div>
                <dt>关闭方式</dt>
                <dd>
                  {{
                    detail.closeReason === "TRANSFERRED_TO_NCR"
                      ? "转不合格审批关闭"
                      : detail.closeReason === "NORMAL_COMPLETED"
                        ? "常规闭环"
                        : "—"
                  }}
                </dd>
              </div>
              <div>
                <dt>关联不合格评审</dt>
                <dd>
                  <el-button
                    size="default"
                    v-if="detail.nonconformanceReviewId"
                    link
                    type="primary"
                    @click="openReview"
                    >{{
                      detail.nonconformanceReviewCode || `#${detail.nonconformanceReviewId}`
                    }}</el-button
                  ><span v-else>—</span>
                </dd>
              </div>
              <div>
                <dt>评审处置</dt>
                <dd>
                  {{ resolveNcrDisposition(detail.nonconformanceDisposition || detail.nonconformanceReviewStatus) }}
                </dd>
              </div>
              <div>
                <dt>发起电子签名</dt>
                <dd>
                  {{ detail.initiatorSignatureId ? "已签署" : "待签署" }}
                </dd>
              </div>
              <div>
                <dt>发起时间</dt>
                <dd>{{ formatTime(detail.initiatedAt) }}</dd>
              </div>
            </dl>
          </section>
          <section
            v-if="props.readonly && detail.closeReason === 'TRANSFERRED_TO_NCR' && detail.nonconformanceReviewId"
            class="ncr-disposition-evidence"
            data-deviation-ncr-disposition-evidence
          >
            <el-divider content-position="left">关联不合格审批处置</el-divider>
            <el-alert v-if="ncrReviewError" type="error" :closable="false" :title="ncrReviewError">
              <el-button size="default" link type="primary" @click="loadNcrReviewDetail">重试</el-button>
            </el-alert>
            <el-alert v-else-if="ncrReviewLoading" type="info" :closable="false" title="正在读取不合格审批处置记录" />
            <el-alert v-else-if="ncrSignatureEvidenceError" type="error" :closable="false" :title="ncrSignatureEvidenceError" />
            <div v-else-if="ncrReviewDetail?.reviewStatus === 'pending_review'">
              <section class="edhr-detail-section">
                <h3>处理记录</h3>
                <dl class="edhr-detail-fields">
                  <div>
                    <dt>评审状态</dt>
                    <dd>待QA处置</dd>
                  </div>
                  <div>
                    <dt>不合格原因</dt>
                    <dd>{{ ncrReviewDetail.nonconformanceReason || "—" }}</dd>
                  </div>
                </dl>
              </section>
            </div>
            <div v-else-if="ncrReviewDetail && ncrSignatureEvidence">
              <section class="edhr-detail-section">
                <h3>处理记录</h3>
                <dl class="edhr-detail-fields">
                  <div>
                    <dt>评审状态</dt>
                    <dd>已处置</dd>
                  </div>
                  <div>
                    <dt>不合格原因</dt>
                    <dd>{{ ncrReviewDetail.nonconformanceReason || "—" }}</dd>
                  </div>
                  <div>
                    <dt>处置方式</dt>
                    <dd>{{ resolveNcrDisposition(ncrReviewDetail.disposition) }}</dd>
                  </div>
                  <div>
                    <dt>评审实际意见</dt>
                    <dd>{{ ncrReviewDetail.reviewOpinion || "—" }}</dd>
                  </div>
                  <div>
                    <dt>QA处置签署人</dt>
                    <dd>{{ resolveUserName(ncrReviewDetail.qaUserId) }}</dd>
                  </div>
                  <div>
                    <dt>处置签署时间</dt>
                    <dd>{{ formatTime(ncrSignatureEvidence.signedAt) }}</dd>
                  </div>
                </dl>
              </section>
            </div>
          </section>
          <div v-if="detail.status === 'OPEN' && detail.level === 'CRITICAL' && !props.readonly" class="mt-12px">
            <el-button
              size="default"
              v-hasPermi="['mes:pro-edhr-nonconformance-review:deviation-create']"

              :disabled="detail.canTransferToNcr !== true"
              @click="openNcrCreateDialog"
            >
              发起不合格审批
            </el-button>
            <el-button size="default" link type="primary" @click="refreshTransferEligibility">刷新转审状态</el-button>
            <el-alert v-if="detail.canTransferToNcr === false" class="mt-8px" type="warning" :closable="false" :title="detail.transferToNcrBlockedReason" />
          </div>
          <el-divider />
          <el-alert v-if="handlingError" type="error" :closable="false" :title="handlingError">
            <el-button size="default" link type="primary" @click="loadHandling">重试</el-button>
          </el-alert>
          <el-empty v-else-if="detail.status === 'OPEN' && !handling" description="尚未填写处理内容">
            <el-button size="default" v-if="canEditHandling" @click="startHandling">开始处理</el-button>
          </el-empty>
          <el-card v-else-if="handling" v-loading="handlingLoading" shadow="never">
            <template #header>调查处理</template>
            <div>
              <section class="edhr-detail-section">
                <h3>处理记录</h3>
                <dl class="edhr-detail-fields">
                  <div>
                    <dt>内容版本</dt>
                    <dd>{{ handling.contentVersion || "—" }}</dd>
                  </div>
                  <div>
                    <dt>处理结论</dt>
                    <dd>{{ handling.handlingConclusion === 'CLOSED_LOOP' ? '闭环完成' : '—' }}</dd>
                  </div>
                  <div>
                    <dt>验证结果</dt>
                    <dd>{{ handling.verificationResult === 'PASS' ? '通过' : handling.verificationResult === 'FAIL' ? '不通过' : '—' }}</dd>
                  </div>
                  <div>
                    <dt>关联评审</dt>
                    <dd>{{ handling.nonconformanceReviewCode || "—" }}</dd>
                  </div>
                </dl>
              </section>
            </div>
            <div v-if="props.readonly || !canEditHandling || detail.status !== 'OPEN'" class="handling-readonly">
              <section class="edhr-detail-section">
                <h3>处理记录</h3>
                <dl class="edhr-detail-fields">
                  <div>
                    <dt>调查开始</dt>
                    <dd>{{ formatHandlingDateTime(handling.investigationStartedAt) }}</dd>
                  </div>
                  <div>
                    <dt>计划完成</dt>
                    <dd>{{ formatHandlingDateTime(handling.plannedCompletedAt) }}</dd>
                  </div>
                  <div>
                    <dt>实际完成</dt>
                    <dd>{{ formatHandlingDateTime(handling.completedAt) }}</dd>
                  </div>
                  <div>
                    <dt>调查成员</dt>
                    <dd>
                      <el-alert v-if="investigationMembers.error" type="error" :closable="false" :title="investigationMembers.error" />
                      <span v-else>{{ investigationMembers.entries.map(member => member.name ?? resolveUserName(member.id)).join('、') || '—' }}</span>
                    </dd>
                  </div>
                  <div>
                    <dt>根因分析</dt>
                    <dd>{{ handling.rootCauseAnalysis || "—" }}</dd>
                  </div>
                  <div>
                    <dt>影响范围</dt>
                    <dd>{{ handling.impactScope || "—" }}</dd>
                  </div>
                  <div>
                    <dt>风险评估</dt>
                    <dd>{{ handling.riskAssessment || "—" }}</dd>
                  </div>
                  <div>
                    <dt>产品处置</dt>
                    <dd>{{ handling.productDisposition || "—" }}</dd>
                  </div>
                  <div>
                    <dt>整改责任人</dt>
                    <dd>{{ resolveUserName(handling.correctiveOwnerId) }}</dd>
                  </div>
                  <div>
                    <dt>整改期限</dt>
                    <dd>{{ formatHandlingDateTime(handling.correctiveDueAt) }}</dd>
                  </div>
                  <div>
                    <dt>CAPA</dt>
                    <dd>
                      {{ handling.capaRequired ? "是" : "否"
                      }}{{ handling.capaCode ? ` / ${handling.capaCode}` : "" }}
                    </dd>
                  </div>
                  <div>
                    <dt>CAPA附件</dt>
                    <dd>
                      <BusinessEvidenceDetails title="附件信息" :source="handling.capaAttachmentsJson" />
                    </dd>
                  </div>
                  <div>
                    <dt>验证内容</dt>
                    <dd>{{ handling.verificationContent || "—" }}</dd>
                  </div>
                </dl>
              </section>
            </div>
            <el-form v-if="detail.status === 'OPEN' && !props.readonly && canEditHandling" :model="handlingForm" label-width="110px" class="handling-form">
              <el-form-item v-if="handlingForm.expectedContentVersion > 0" label="修订原因" required><el-input v-model="handlingForm.revisionReason" type="textarea" maxlength="500" show-word-limit placeholder="请说明本次修改原因" /></el-form-item>
              <el-form-item label="根因分析"><el-input v-model="handlingForm.rootCauseAnalysis" type="textarea" :rows="2" /></el-form-item>
              <el-form-item label="影响范围"><el-input v-model="handlingForm.impactScope" type="textarea" :rows="2" /></el-form-item>
              <el-form-item label="风险评估"><el-input v-model="handlingForm.riskAssessment" type="textarea" :rows="2" /></el-form-item>
              <el-form-item label="产品处置"><el-input v-model="handlingForm.productDisposition" type="textarea" :rows="2" /></el-form-item>
              <el-form-item label="调查开始"><el-date-picker v-model="handlingForm.investigationStartedAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
              <el-form-item label="计划完成"><el-date-picker v-model="handlingForm.plannedCompletedAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>

              <el-form-item label="调查成员"><el-input v-model="handlingForm.investigationMembersJson" type="textarea" :rows="2" /></el-form-item>
              <el-form-item label="整改责任人"><UserSelectV2 v-model="handlingForm.correctiveOwnerId" placeholder="请选择纠正措施责任人" @change="value => handlingForm.correctiveOwnerName = value?.nickname || ''" /></el-form-item>
              <el-form-item label="整改期限"><el-date-picker v-model="handlingForm.correctiveDueAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
              <el-form-item label="CAPA"><el-switch v-model="handlingForm.capaRequired" /><el-input v-if="handlingForm.capaRequired" v-model="handlingForm.capaCode" class="ml-8px" placeholder="CAPA编号" /></el-form-item>
              <el-form-item label="处理结论"><el-select v-model="handlingForm.handlingConclusion"><el-option label="闭环完成" value="CLOSED_LOOP" /></el-select></el-form-item>
              <el-form-item v-for="node in signNodeOptions" :key="node" :label="`${nodeLabels[node]}责任人`">
                <DeviationReviewerSelect v-model="handlingForm.reviewerAssignments[node]" :node="node" :placeholder="`请选择${nodeLabels[node]}责任人`" />
              </el-form-item>
            </el-form>
            <el-table v-if="handling.id" :data="assignedResponsibilities" size="small" border class="mb-12px">
              <el-table-column prop="label" label="签署职责" min-width="160" />
              <el-table-column prop="owner" label="指定责任人" min-width="120" />
              <el-table-column prop="status" label="当前版本状态" min-width="140" />
            </el-table>
            <el-divider v-if="handling.signatureEvidence?.length" />
            <div v-if="handling.signatureEvidence?.length" class="signature-evidence">
              <div class="signature-evidence__title">当前版本电子签名证据</div>
              <el-table :data="handling.signatureEvidence" size="small" border>
                <el-table-column label="签名节点" width="150"><template #default="scope">{{ nodeLabels[scope.row.node] }}</template></el-table-column>
                <el-table-column label="签名人" min-width="160">
                  <template #default="scope"><span data-deviation-signature-actor-name>{{ resolveUserName(scope.row.actorId) }}</span></template>
                </el-table-column>
                <el-table-column prop="meaningLabel" label="签名含义" min-width="140" />
                <el-table-column label="服务器签名时间" min-width="220"><template #default="scope">{{ formatTime(scope.row.signedAt) }} {{ scope.row.timeZone ? `(${scope.row.timeZone})` : '' }}</template></el-table-column>
              </el-table>
            </div>
            <el-divider v-if="handling.signatureHistory?.length" />
            <div v-if="handling.signatureHistory?.length" class="signature-history">
              <div class="signature-evidence__title">历史电子签名</div>
              <el-table :data="handling.signatureHistory" size="small" border>
                <el-table-column label="节点" width="140"><template #default="scope">{{ nodeLabels[scope.row.node] }}</template></el-table-column>
                <el-table-column label="签名人" min-width="150"><template #default="scope">{{ resolveUserName(scope.row.actorId) }}</template></el-table-column>
                <el-table-column prop="validityStatus" label="有效性" width="150" />
                <el-table-column prop="meaningLabel" label="签名含义" min-width="140" />
                <el-table-column label="签署时间" min-width="220"><template #default="scope">{{ formatTime(scope.row.signedAt) }} {{ scope.row.timeZone ? `(${scope.row.timeZone})` : '' }}</template></el-table-column>
              </el-table>
            </div>
            <el-divider v-if="handling.revisionHistory?.length" />
            <div v-if="handling.revisionHistory?.length" class="revision-history">
              <div class="signature-evidence__title">处理修订历史</div>
              <el-timeline>
                <el-timeline-item v-for="revision in handling.revisionHistory" :key="revision.auditId" :timestamp="formatTime(revision.occurredAt)">
                  <div>修订原因：{{ revision.revisionReason || '—' }}</div>
                  <div>版本：{{ revision.beforeContentVersion || '—' }} → {{ revision.afterContentVersion || '—' }}</div>
                  <div>操作人：{{ resolveUserName(revision.actorUserId) }}</div>
                </el-timeline-item>
              </el-timeline>
            </div>
          </el-card>
          <el-alert v-else type="info" :closable="false" :title="closedMessage" />
        </el-tab-pane>
        <el-tab-pane label="整改办理" name="corrective" lazy>
          <el-alert v-if="handlingError" type="error" :closable="false" :title="handlingError">
            <el-button size="default" link type="primary" @click="loadHandling">重试</el-button>
          </el-alert>
          <DeviationCorrectivePanel v-else-if="handling?.id" :record="handling" :owner-name="resolveUserName(handling.correctiveOwnerId)" :closed="detail.status === 'CLOSED'" :readonly="props.readonly || detail.status !== 'OPEN'" @changed="loadHandling" />
          <el-empty v-else description="暂无整改办理记录" />
        </el-tab-pane>
      </el-tabs>
      <Teleport :to="props.actionsTarget" :disabled="!props.actionsTarget">
        <div v-if="detailTab === 'disposition' && handling && detail.status === 'OPEN' && !props.readonly" class="handling-card__actions">
          <el-button size="default" v-if="canEditHandling" :loading="handlingSaving" @click="saveHandling">保存草稿</el-button>
          <el-button size="default" v-if="canEditHandling && handling.id" :loading="handlingSaving" @click="submitInvestigation">提交调查</el-button>
          <DeviationResponsibilityRecovery v-if="canWithdrawInvestigation" mode="withdraw" :deviation-id="props.id" :content-version="handling.contentVersion" :content-hash="handling.contentHash" :disabled="handlingSaving || formSnapshot() !== savedFormSnapshot" @changed="load" />
          <DeviationReviewReturn v-if="myReturnNodes.length && handling.id" :deviation-id="props.id" :content-version="handling.contentVersion!" :content-hash="handling.contentHash!" :nodes="myReturnNodes" :disabled="handlingSaving || formSnapshot() !== savedFormSnapshot" @returned="loadHandling" />
          <el-button size="default" v-if="mySignNodes.length" :disabled="!handling.id" @click="openSignDialog(mySignNodes[0])">签署本人节点</el-button>
          <el-button size="default" v-if="canCloseHandling" :loading="closing" :disabled="!handling.id" @click="closeHandling">常规关闭</el-button>
        </div>
      </Teleport>
    </template>
    <el-dialog v-model="signVisible" title="电子签名" width="480px" append-to-body>
      <el-form :model="signForm" label-width="100px">
        <el-form-item label="签名节点"><el-select v-model="signForm.node"><el-option v-for="node in mySignNodes" :key="node" :label="nodeLabels[node]" :value="node" /></el-select></el-form-item>
        <el-form-item label="密码"><SignatureCredentialInput v-model="signForm.password" /></el-form-item>
        <el-form-item label="签名意见"><el-input v-model="signForm.comment" type="textarea" /></el-form-item>
      </el-form>
      <template #footer><el-button size="default" @click="signVisible = false">取消</el-button><el-button size="default" :loading="signing" @click="signHandling">确认签名</el-button></template>
    </el-dialog>
    <el-dialog v-model="ncrCreateVisible" title="发起不合格审批" width="520px" destroy-on-close append-to-body>
      <el-alert v-if="ncrCreateError" :title="ncrCreateError" type="error" :closable="false" show-icon class="mb-12px" />
      <el-form :model="ncrCreateForm" label-width="110px">
        <el-form-item label="QA评审责任人" required><QaReviewerSelect v-model="ncrCreateForm.qaReviewerUserId" placeholder="请选择具备QA处置权限的责任人" /></el-form-item>
        <el-form-item label="不合格原因" required>
          <el-input v-model="ncrCreateForm.nonconformanceReason" type="textarea" :rows="4" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="ncrCreateForm.remark" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
        <el-form-item label="签名凭据" required>
          <SignatureCredentialInput v-model="ncrCreateForm.signaturePassword" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button size="default" @click="ncrCreateVisible = false">取消</el-button>
        <el-button size="default" :loading="ncrCreateLoading" @click="submitNcrCreate">签名并发起</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import DeviationCorrectivePanel from './components/DeviationCorrectivePanel.vue'
import DeviationResponsibilityRecovery from './components/DeviationResponsibilityRecovery.vue'
import DeviationReviewerSelect from './components/DeviationReviewerSelect.vue'
import DeviationReviewReturn from './components/DeviationReviewReturn.vue'
import SignatureCredentialInput from '@/components/SignatureCredentialInput/SignatureCredentialInput.vue'
import UserSelectV2 from '@/views/system/user/components/UserSelectV2.vue'
import QaReviewerSelect from '@/views/mes/pro/edhr-nonconformance/QaReviewerSelect.vue'
import '@/views/mes/pro/edhr/shared/detail-surface.scss'
import BusinessEvidenceDetails from '../processpool/components/BusinessEvidenceDetails.vue'
import { submitDeviationInvestigation, closeDeviationHandling, getDeviation, getDeviationHandling, saveDeviationHandling, signDeviationHandling, type DeviationHandlingRespVO, type DeviationRespVO } from '@/api/mes/pro/edhr/deviation'
import { createCriticalDeviationReview, getNonconformanceReview, type EdhrNonconformanceReviewRespVO } from '@/api/mes/pro/edhr/nonconformanceReview'
import { ElMessage } from 'element-plus'
import { useMessage } from '@/hooks/web/useMessage'
import dayjs from 'dayjs'
import { useApprovalCenterInvalidationStore } from '@/store/modules/approvalCenterInvalidation'
import { createFormalActionGuard } from '@/store/modules/approvalCenterInvalidationState'
import { useUserStore } from '@/store/modules/user'
import { checkPermi } from '@/utils/permission'
import { getSelectionList } from '@/api/system/user'
import { parseExactIntegerJson } from '@/utils/exactIntegerJson'
const message = useMessage()
const approvalInvalidation = useApprovalCenterInvalidationStore()
const signatureUser = computed(() => useUserStore().getUser)
const props = withDefaults(defineProps<{
  id: number
  readonly?: boolean
  actionsTarget?: HTMLElement | null
  initialTab?: 'business' | 'corrective' | 'disposition'
}>(), { readonly: false })
const emit = defineEmits<{ (event: 'closed'): void }>()
const detailTab = ref(props.initialTab || 'business')
const loading = ref(false)
const detail = ref<DeviationRespVO>()
const handling = ref<DeviationHandlingRespVO>()
const ncrReviewDetail = ref<EdhrNonconformanceReviewRespVO>()
const ncrSignatureEvidence = ref<{ signatureId: number; actionType: string; signedAt: string; aggregateHash: string }>()
const error = ref('')
const handlingError = ref('')
const ncrReviewError = ref('')
const ncrSignatureEvidenceError = ref('')
const handlingLoading = ref(false)
const ncrReviewLoading = ref(false)
const handlingSaving = ref(false)
const signing = ref(false)
const closing = ref(false)
const signVisible = ref(false)
const ncrCreateVisible = ref(false)
const ncrCreateLoading = ref(false)
const ncrCreateError = ref('')
const createHandlingForm = () => ({ expectedContentVersion: 0, revisionReason: '', investigationStartedAt: '', plannedCompletedAt: '', completedAt: '', investigationMembersJson: '', rootCauseAnalysis: '', impactScope: '', riskAssessment: '', productDisposition: '', nonconformanceReviewCode: '', correctiveOwnerId: undefined as number | string | undefined, correctiveOwnerName: '', correctiveDueAt: '', reviewerAssignments: {} as Record<string, number | string | undefined>, capaRequired: false, capaCode: '', capaAttachmentsJson: '', handlingConclusion: 'CLOSED_LOOP', verificationResult: 'PASS', verificationContent: '' })
const handlingForm = reactive(createHandlingForm())
let savedFormSnapshot = ''
let loadSequence = 0
let transferEligibilitySequence = 0
let handlingSequence = 0
let reviewSequence = 0
const formSnapshot = () => JSON.stringify({ ...handlingForm, revisionReason: '' })
const resetHandling = () => {
  handling.value = undefined
  Object.assign(handlingForm, createHandlingForm())
  savedFormSnapshot = ''
}

const signForm = reactive({ node: 'PREPARER', password: '', comment: '' })
const ncrCreateForm = reactive({ qaReviewerUserId: undefined as string | number | undefined, nonconformanceReason: '', remark: '', signaturePassword: '', idempotencyKey: '' })
const ncrCreateGuard = createFormalActionGuard(() => JSON.stringify([approvalInvalidation.syncSession().key, router.currentRoute.value.fullPath, props.id]))
const resetNcrCreateContext = () => { ncrCreateGuard.invalidate(); ncrCreateLoading.value = false; ncrCreateForm.signaturePassword = '' }
watch(ncrCreateVisible, resetNcrCreateContext, { flush: 'sync' })
watch(() => props.id, resetNcrCreateContext, { flush: 'sync' })
watch(() => approvalInvalidation.syncSession().key, () => { loadSequence++; resetNcrCreateContext(); ncrCreateVisible.value = false }, { flush: 'sync' })
onBeforeUnmount(() => { loadSequence++; handlingSequence++; reviewSequence++; transferEligibilitySequence++; resetNcrCreateContext() })
const signNodeOptions = computed(() => {
  const nodes = ['PREPARER', 'VERIFIER', 'DEPARTMENT_OWNER', 'QA', 'QUALITY_OWNER']
  if (detail.value?.level === 'CRITICAL') nodes.push('MANAGEMENT_REP')
  return nodes
})
const nodeLabels: Record<string, string> = { PREPARER: '编制', VERIFIER: '核验', DEPARTMENT_OWNER: '部门负责人确认', QA: 'QA关闭确认', QUALITY_OWNER: '质量负责人批准', MANAGEMENT_REP: '管理者代表批准' }
const nodePermissions: Record<string, string> = { PREPARER: 'handle', VERIFIER: 'verify', DEPARTMENT_OWNER: 'department-confirm', QA: 'qa-close', QUALITY_OWNER: 'quality-approve', MANAGEMENT_REP: 'critical-management-approve' }
const canAssignReceiver = computed(() => !props.readonly && detail.value?.status === 'OPEN'
  && detail.value.receiverId == null && checkPermi(['mes:pro-edhr-deviation:handle']))
const isInvestigationReceiver = computed(() => detail.value?.receiverId != null && signatureUser.value.id != null
  && String(detail.value.receiverId) === String(signatureUser.value.id))
const canEditHandling = computed(() => !props.readonly && detail.value?.status === 'OPEN'
  && (handling.value?.id ? handling.value.canEditInvestigation === true
    : isInvestigationReceiver.value && checkPermi(['mes:pro-edhr-deviation:handle'])))
const canWithdrawInvestigation = computed(() => !props.readonly && detail.value?.status === 'OPEN'
  && isInvestigationReceiver.value && handling.value?.investigationSubmitted === true
  && checkPermi(['mes:pro-edhr-deviation:handle']))
const mySignNodes = computed(() => signNodeOptions.value.filter(node =>
  handling.value?.reviewReady === true
  && String(handling.value?.reviewerAssignments?.[node] ?? '') === String(signatureUser.value.id ?? '')
  && !handling.value?.effectiveSignatureIds?.[node]
  && checkPermi([`mes:pro-edhr-deviation:${nodePermissions[node]}`])))
const canCloseHandling = computed(() => String(handling.value?.reviewerAssignments?.QA ?? '') === String(signatureUser.value.id ?? '')
  && handling.value?.reviewReady === true
  && checkPermi(['mes:pro-edhr-deviation:qa-close'])
  && signNodeOptions.value.every(node => Boolean(handling.value?.effectiveSignatureIds?.[node])))
const myReturnNodes = computed(() => [
  ...mySignNodes.value.map(node => ({ value: node, label: nodeLabels[node] })),
  ...(canCloseHandling.value ? [{ value: 'CLOSE', label: '正式关闭' }] : [])
])
const assignedResponsibilities = computed(() => signNodeOptions.value.map(node => ({
  label: nodeLabels[node],
  owner: handling.value?.reviewerAssignments?.[node] ? resolveUserName(handling.value.reviewerAssignments[node]) : '尚未指定',
  status: handling.value?.effectiveSignatureIds?.[node] ? '已签署' : handling.value?.reviewReady === true ? '待签署' : '等待处理记录完成'
})))
const router = useRouter()
const userNames = ref<Record<string, string>>({})
const userNamesLoading = ref(false)
const userNamesError = ref('')
let userNamesSequence = 0
const investigationMembers = computed(() => {
  const entries: Array<{ id?: string; name?: string }> = []
  const raw = handling.value?.investigationMembersJson
  if (!raw) return { entries, error: '' }
  try {
    const value = parseExactIntegerJson(raw)
    for (const member of Array.isArray(value) ? value : [value]) {
      if (typeof member === 'number' || (typeof member === 'string' && /^[1-9]\d*$/.test(member))) {
        entries.push({ id: String(member) })
      } else if (typeof member === 'string' && member.trim()) {
        entries.push({ name: member.trim() })
      } else if (member && typeof member === 'object' && !Array.isArray(member)) {
        const id = member.userId ?? member.id
        const name = member.nickname ?? member.name ?? member.displayName
        if (id != null) entries.push({ id: String(id) })
        else if (typeof name === 'string' && name.trim()) entries.push({ name: name.trim() })
        else throw new Error('调查成员缺少姓名或人员信息')
      } else throw new Error('调查成员格式无效')
    }
    return { entries, error: '' }
  } catch (cause) {
    return { entries: [], error: cause instanceof Error ? cause.message : '调查成员读取失败' }
  }
})
const detailUserIds = computed(() => [...new Set([
  ...investigationMembers.value.entries.map(member => member.id),
  ...Object.values(handling.value?.reviewerAssignments || {}),
  handling.value?.correctiveOwnerId,
  ncrReviewDetail.value?.qaUserId,
  ...(handling.value?.signatureEvidence || []).map(row => row.actorId),
  ...(handling.value?.signatureHistory || []).map(row => row.actorId),
  ...(handling.value?.revisionHistory || []).map(row => row.actorUserId)
].filter((id): id is number | string => id != null && id !== '').map(String))].sort())
const resolveUserName = (id?: number | string) => {
  if (id == null || id === '') return '—'
  if (userNamesLoading.value) return '加载中'
  return userNames.value[String(id)] || '姓名未提供'
}
const loadUserNames = async () => {
  const sequence = ++userNamesSequence
  const sessionKey = approvalInvalidation.syncSession().key
  const routeKey = router.currentRoute.value.fullPath
  const id = props.id
  const ids = [...detailUserIds.value]
  const isCurrent = () => sequence === userNamesSequence && id === props.id
    && approvalInvalidation.isCurrent(sessionKey) && routeKey === router.currentRoute.value.fullPath
  userNames.value = {}
  userNamesError.value = ''
  userNamesLoading.value = ids.length > 0
  if (!ids.length) return
  try {
    const users = await getSelectionList(ids)
    if (!isCurrent()) return
    userNames.value = Object.fromEntries(users.filter(user => user.nickname?.trim()).map(user => [String(user.id), user.nickname!.trim()]))
    if (ids.some(userId => !userNames.value[userId])) userNamesError.value = '部分人员缺少姓名，请核对人员资料'
  } catch (cause: any) {
    if (isCurrent()) userNamesError.value = cause?.response?.data?.msg || cause?.message || '人员姓名加载失败，请重试'
  } finally {
    if (isCurrent()) userNamesLoading.value = false
  }
}
watch(() => [props.id, approvalInvalidation.syncSession().key, detailUserIds.value.join(',')], loadUserNames)
onBeforeUnmount(() => userNamesSequence++)
const formatTime = (value?: string | number) => {
  if (value == null || value === '') return '—'
  const formatted = dayjs(value)
  return formatted.isValid() ? formatted.format('YYYY-MM-DD HH:mm:ss') : String(value)
}
const closedMessage = computed(() => detail.value?.closeReason === 'TRANSFERRED_TO_NCR'
  ? '已转不合格审批关闭'
  : detail.value?.closeReason === 'NORMAL_COMPLETED'
    ? '偏差已常规闭环'
    : '偏差已关闭')
const resolveNcrDisposition = (disposition?: string) => {
  if (disposition === 'pending_review') return '待评审'
  if (disposition === 'closed') return '已关闭'
  if (disposition === 'concession_release') return '让步放行'
  if (disposition === 'rework') return '返工'
  if (disposition === 'void') return '作废'
  return disposition || '—'
}
const readNcrSignatureEvidence = (review: EdhrNonconformanceReviewRespVO) => {
  if (!review.traceSnapshotJson) throw new Error('不合格评审未返回处置签名快照')
  const snapshot = JSON.parse(review.traceSnapshotJson) as {
    qaSignatureSnapshotJson?: {
      reviewId?: number
      signatureId?: number
      actionType?: string
      disposition?: string
      signedAt?: string
      aggregateHash?: string
    }
  }
  const evidence = snapshot.qaSignatureSnapshotJson
  if (!evidence
    || typeof evidence.signatureId !== 'number'
    || !Number.isSafeInteger(evidence.signatureId)
    || evidence.reviewId !== review.id
    || evidence.actionType !== 'QA_DISPOSITION'
    || evidence.disposition !== review.disposition
    || !evidence.signedAt
    || !evidence.aggregateHash) {
    throw new Error('不合格评审处置签名快照缺少有效签名证据')
  }
  return {
    signatureId: evidence.signatureId,
    actionType: evidence.actionType,
    signedAt: evidence.signedAt,
    aggregateHash: evidence.aggregateHash
  }
}
const openBatch = () => {
  if (detail.value?.batchExecutionId) {
    void router.push({
      path: '/mes/pro/feedback/edhr-batch-execution/active-order-detail',
      query: { batchExecutionId: String(detail.value.batchExecutionId), from: 'execution' }
    })
  }
}
const openReview = () => {
  if (detail.value?.nonconformanceReviewId) {
    void router.push({
      path: '/mes/pro/feedback/edhr-nonconformance-review',
      query: {
        reviewId: String(detail.value.nonconformanceReviewId),
        from: 'deviation',
        deviationId: String(detail.value.id),
        ...(detail.value.batchExecutionId != null ? { batchExecutionId: String(detail.value.batchExecutionId) } : { activeOrderId: String(detail.value.activeOrderId) })
      }
    })
  }
}
const refreshTransferEligibility = async () => {
  const sessionKey = approvalInvalidation.syncSession().key
  const routeKey = router.currentRoute.value.fullPath
  const id = props.id
  const sequence = loadSequence
  const refreshSequence = ++transferEligibilitySequence
  try {
    const latest = await getDeviation(id, { ignoreErrorMessage: true, isRequestContextCurrent: () => approvalInvalidation.isCurrent(sessionKey) && routeKey === router.currentRoute.value.fullPath && id === props.id && sequence === loadSequence && refreshSequence === transferEligibilitySequence })
    if (!approvalInvalidation.isCurrent(sessionKey) || routeKey !== router.currentRoute.value.fullPath || id !== props.id || sequence !== loadSequence || refreshSequence !== transferEligibilitySequence || !detail.value) return
    detail.value.canTransferToNcr = latest.canTransferToNcr
    detail.value.transferToNcrBlockedReason = latest.transferToNcrBlockedReason
  } catch (cause: any) {
    if (approvalInvalidation.isCurrent(sessionKey) && routeKey === router.currentRoute.value.fullPath && id === props.id && sequence === loadSequence && refreshSequence === transferEligibilitySequence) {
      ElMessage.error(cause?.response?.data?.msg || cause?.message || '转审状态读取失败，请重试')
    }
  }
}
const openNcrCreateDialog = () => {
  resetNcrCreateContext()
  if (props.readonly || !detail.value || detail.value.status !== 'OPEN' || detail.value.level !== 'CRITICAL') return
  if (detail.value.canTransferToNcr !== true) return
  ncrCreateError.value = ''
  ncrCreateForm.qaReviewerUserId = undefined
  ncrCreateForm.nonconformanceReason = ''
  ncrCreateForm.remark = ''
  ncrCreateForm.signaturePassword = ''
  ncrCreateForm.idempotencyKey = `EDHR_DEVIATION_NCR_${detail.value.id}_${Date.now()}_${Math.random().toString(36).slice(2)}`
  ncrCreateVisible.value = true
}
const submitNcrCreate = async () => {
  if (ncrCreateLoading.value) return
  if (props.readonly) return
  const current = detail.value
  let isCurrent = ncrCreateGuard.capture()
  const nonconformanceReason = ncrCreateForm.nonconformanceReason.trim()
  const signaturePassword = ncrCreateForm.signaturePassword.trim()
  if (!current || current.status !== 'OPEN' || current.level !== 'CRITICAL' || (!current.batchExecutionId && !current.activeOrderId)) {
    ncrCreateError.value = '当前偏差不满足发起不合格审批条件，请刷新后重试'
    return
  }
  if (current.canTransferToNcr !== true) {
    ncrCreateError.value = current.transferToNcrBlockedReason || '转审资格未就绪，请刷新转审状态'
    return
  }
  if (!ncrCreateForm.qaReviewerUserId) {
    ncrCreateError.value = '请选择QA评审责任人'
    return
  }
  if (!nonconformanceReason || !signaturePassword) {
    ncrCreateError.value = '不合格原因和签名凭据不能为空'
    return
  }
  ncrCreateLoading.value = true
  ncrCreateError.value = ''
  const payload = {
    qaReviewerUserId: ncrCreateForm.qaReviewerUserId,
    batchExecutionId: current.batchExecutionId, activeOrderId: current.batchExecutionId == null ? current.activeOrderId : undefined, deviationIds: [current.id], nonconformanceReason,
    remark: ncrCreateForm.remark.trim() || undefined, idempotencyKey: ncrCreateForm.idempotencyKey, signaturePassword
  }
  const submitSession = approvalInvalidation.syncSession()
  let writeSucceeded = false
  try {
    const receipt = await createCriticalDeviationReview(payload, { ignoreErrorMessage: true, isRequestContextCurrent: isCurrent , isSuccessReceiptContextCurrent: (() => { const receiptSessionKey = approvalInvalidation.syncSession().key; return () => approvalInvalidation.isCurrent(receiptSessionKey) })()})
    writeSucceeded = true
    if ((payload.batchExecutionId != null ? String(receipt.batchExecutionId) !== String(payload.batchExecutionId) : String(receipt.activeOrderId) !== String(payload.activeOrderId)) || !receipt.activeOrderId) throw new Error('转审已成功，但正式回执缺少本批次的活跃订单身份，请刷新核对')
    const event = approvalInvalidation.captureHandoffReceiptSuccess(submitSession, 'ACTIVE_ORDER', receipt.activeOrderId)
    approvalInvalidation.publishEdhrSuccess(event)
    if (!isCurrent()) return
    ncrCreateVisible.value = false
    ElMessage.success('不合格审批已发起，偏差已转审关闭')
    isCurrent = ncrCreateGuard.capture()
    await load()
    if (isCurrent() && error.value) throw new Error(error.value)
  } catch (error: any) {
    if (isCurrent()) ncrCreateError.value = writeSucceeded ? `不合格审批已发起，回执校验或详情刷新失败：${error?.message}` : error?.response?.data?.msg || error?.message || '不合格审批发起失败，请重试'
  } finally {
    if (isCurrent()) ncrCreateLoading.value = false
  }
}
const toHandlingDateTime = (value: string | number | null | undefined): string => {
  if (value === null || value === undefined || value === '') return ''
  const parsed = dayjs(value)
  if (!parsed.isValid()) throw new Error('偏差处理时间格式无效，请核对原始记录')
  return parsed.format('YYYY-MM-DDTHH:mm:ss')
}
const formatHandlingDateTime = (value: string | number | null | undefined) =>
  toHandlingDateTime(value).replace('T', ' ') || '—'
const applyHandling = (record?: DeviationHandlingRespVO) => {
  resetHandling()
  handling.value = record
  if (record) {
  if (!props.initialTab && (record.canSubmitCorrective || record.canVerifyCorrective)) detailTab.value = 'corrective'
  Object.assign(handlingForm, {
        expectedContentVersion: record.contentVersion || 0,
        investigationStartedAt: toHandlingDateTime(record.investigationStartedAt),
        plannedCompletedAt: toHandlingDateTime(record.plannedCompletedAt),
        completedAt: toHandlingDateTime(record.completedAt),
        investigationMembersJson: record.investigationMembersJson || '',
        rootCauseAnalysis: record.rootCauseAnalysis || '',
        impactScope: record.impactScope || '',
        riskAssessment: record.riskAssessment || '',
        productDisposition: record.productDisposition || '',
        nonconformanceReviewCode: record.nonconformanceReviewCode || '',
        correctiveOwnerId: record.correctiveOwnerId,
        correctiveOwnerName: record.correctiveOwnerName || '',
        reviewerAssignments: { ...(record.reviewerAssignments || {}) },
        correctiveDueAt: toHandlingDateTime(record.correctiveDueAt),
        capaRequired: record.capaRequired || false,
        capaCode: record.capaCode || '',
        capaAttachmentsJson: record.capaAttachmentsJson || '',
        handlingConclusion: record.handlingConclusion || 'CLOSED_LOOP',
        verificationResult: record.verificationResult || 'PASS',
        verificationContent: record.verificationContent || ''
      })
  }
  savedFormSnapshot = formSnapshot()
}
const loadHandling = async () => {
  if (!detail.value) return
  const sessionKey = approvalInvalidation.syncSession().key
  const routeKey = router.currentRoute.value.fullPath
  const id = props.id
  const sequence = ++handlingSequence
  const isCurrent = () => approvalInvalidation.isCurrent(sessionKey) && routeKey === router.currentRoute.value.fullPath && id === props.id && sequence === handlingSequence
  handlingLoading.value = true
  handlingError.value = ''
  try {
    const record = await getDeviationHandling(id, { ignoreErrorMessage: true, isRequestContextCurrent: isCurrent })
    if (!isCurrent()) return
    applyHandling(record)
  } catch {
    if (!isCurrent()) return
    resetHandling()
    handlingError.value = '偏差处理记录加载失败，请重试'
  } finally {
    if (isCurrent()) handlingLoading.value = false
  }
}
const loadNcrReviewDetail = async () => {
  const sessionKey = approvalInvalidation.syncSession().key
  const routeKey = router.currentRoute.value.fullPath
  const id = props.id
  const sequence = ++reviewSequence
  const isCurrent = () => approvalInvalidation.isCurrent(sessionKey) && routeKey === router.currentRoute.value.fullPath && id === props.id && sequence === reviewSequence
  const reviewId = detail.value?.nonconformanceReviewId
  if (!props.readonly || detail.value?.closeReason !== 'TRANSFERRED_TO_NCR' || !reviewId) {
    ncrReviewDetail.value = undefined
    ncrSignatureEvidence.value = undefined
    ncrReviewError.value = ''
    ncrSignatureEvidenceError.value = ''
    return
  }
  ncrReviewLoading.value = true
  ncrReviewError.value = ''
  ncrSignatureEvidenceError.value = ''
  ncrReviewDetail.value = undefined
  ncrSignatureEvidence.value = undefined
  try {
    const review = await getNonconformanceReview(reviewId, { ignoreErrorMessage: true, isRequestContextCurrent: isCurrent })
    if (!isCurrent()) return
    ncrReviewDetail.value = review
    if (review.reviewStatus !== 'pending_review') {
      try {
        ncrSignatureEvidence.value = readNcrSignatureEvidence(review)
      } catch (cause) {
        ncrSignatureEvidenceError.value = cause instanceof Error
          ? cause.message
          : '不合格评审处置签名证据无法解析'
      }
    }
  } catch (cause: any) {
    if (!isCurrent()) return
    ncrReviewError.value = cause?.response?.data?.msg || '关联不合格评审详情加载失败，请重试'
  } finally {
    if (isCurrent()) ncrReviewLoading.value = false
  }
}
const saveHandling = async () => {
  if (props.readonly) return
  if (handlingForm.expectedContentVersion > 0 && !handlingForm.revisionReason.trim()) {
    ElMessage.error('请填写修订原因后保存')
    return
  }
  const id = props.id
  const sequence = loadSequence
  handlingSaving.value = true
  try {
    const record = await saveDeviationHandling(id, {
      ...handlingForm,
      correctiveOwnerId: handlingForm.correctiveOwnerId,
      reviewerAssignments: { ...handlingForm.reviewerAssignments },
      investigationStartedAt: normalizeOptionalDateTime(handlingForm.investigationStartedAt),
      plannedCompletedAt: normalizeOptionalDateTime(handlingForm.plannedCompletedAt),
      completedAt: normalizeOptionalDateTime(handlingForm.completedAt),
      correctiveDueAt: normalizeOptionalDateTime(handlingForm.correctiveDueAt),
      investigationMembersJson: normalizeOptionalJson(handlingForm.investigationMembersJson),
      capaAttachmentsJson: normalizeOptionalJson(handlingForm.capaAttachmentsJson)
    })
    if (id !== props.id || sequence !== loadSequence) return
    applyHandling(record)
    ElMessage.success('处理记录已保存')
  } catch (error: any) {
    if (id === props.id && sequence === loadSequence) ElMessage.error(error?.response?.data?.msg || error?.message || '处理记录保存失败，请重试')
  } finally {
    if (id === props.id && sequence === loadSequence) handlingSaving.value = false
  }
}
const submitInvestigation = async () => {
  if (props.readonly || !canEditHandling.value || !handling.value?.id || handlingSaving.value) return
  if (formSnapshot() !== savedFormSnapshot) { ElMessage.error('请先保存草稿，再正式提交调查'); return }
  if (signNodeOptions.value.some(node => !handlingForm.reviewerAssignments[node])) { ElMessage.error('提交前请补全全部签署责任人'); return }
  const id = props.id
  const sequence = loadSequence
  handlingSaving.value = true
  try {
    const record = await submitDeviationInvestigation({ deviationId: id, expectedContentVersion: handling.value.contentVersion!, expectedContentHash: handling.value.contentHash! })
    if (id !== props.id || sequence !== loadSequence) return
    applyHandling(record)
    ElMessage.success('调查已提交，已派发后续办理任务')
  } catch (error: any) {
    if (id === props.id && sequence === loadSequence) ElMessage.error(error?.response?.data?.msg || error?.message || '调查提交失败')
  } finally { if (id === props.id && sequence === loadSequence) handlingSaving.value = false }
}
const canSignSavedHandling = () => {
  if (!handling.value?.id || !handling.value.contentVersion || !handling.value.contentHash || formSnapshot() !== savedFormSnapshot || handlingSaving.value) {
    ElMessage.error('请先保存当前处理内容，再审阅并签名')
    return false
  }
  return true
}
const openSignDialog = (node: string) => {
  if (props.readonly || !canSignSavedHandling()) return
  if (!mySignNodes.value.includes(node)) {
    ElMessage.error('当前账号不是该签署节点的指定责任人')
    return
  }
  signForm.node = node
  signForm.password = ''
  signForm.comment = ''
  signVisible.value = true
}
const startHandling = () => {
  if (props.readonly || !detail.value || detail.value.status !== 'OPEN') return
  resetHandling()
  handling.value = { id: 0, deviationId: props.id, contentVersion: 0, signatureEvidence: [], signatureHistory: [], revisionHistory: [] }
}
const normalizeOptionalJson = (value: string) => {
  const trimmed = value.trim()
  if (!trimmed) return null
  try {
    JSON.parse(trimmed)
    return trimmed
  } catch {
    return JSON.stringify(trimmed)
  }
}
const normalizeOptionalDateTime = (value: string | number | null | undefined) => toHandlingDateTime(value) || null
const signHandling = async () => {
  if (props.readonly || !canSignSavedHandling()) return
  if (!mySignNodes.value.includes(signForm.node)) {
    ElMessage.error('当前账号没有该节点的有效签署任务')
    return
  }
  const id = props.id
  const sequence = loadSequence
  const reviewed = handling.value!
  signing.value = true
  try {
    await signDeviationHandling({ deviationId: id, ...signForm, expectedContentVersion: reviewed.contentVersion!, expectedContentHash: reviewed.contentHash! })
    if (id !== props.id || sequence !== loadSequence) return
    signVisible.value = false
    await loadHandling()
    ElMessage.success('电子签名已记录')
  } catch (error: any) {
    if (id === props.id && sequence === loadSequence) ElMessage.error(error?.response?.data?.msg || error?.message || '电子签名失败，请重试')
  } finally {
    if (id === props.id && sequence === loadSequence) signing.value = false
  }
}
const closeHandling = async () => {
  if (props.readonly || !canSignSavedHandling()) return
  const reviewedVersion = handling.value!.contentVersion!
  const reviewedHash = handling.value!.contentHash!
  const id = props.id
  const sequence = loadSequence
  try {
    await message.confirm('确认按当前处理内容常规关闭该偏差吗？', '关闭确认')
  } catch (action) {
    if (action === 'cancel' || action === 'close') return
    throw action
  }
  if (id !== props.id || sequence !== loadSequence || !canSignSavedHandling()) return
  closing.value = true
  try {
    await closeDeviationHandling(id, reviewedVersion, reviewedHash)
    if (id !== props.id || sequence !== loadSequence) return
    emit('closed')
    await load()
    ElMessage.success('偏差已关闭')
  } catch (error: any) {
    if (id === props.id && sequence === loadSequence) ElMessage.error(error?.response?.data?.msg || error?.message || '偏差关闭失败，请重试')
  } finally {
    if (id === props.id && sequence === loadSequence) closing.value = false
  }
}
const load = async () => {
  const sessionKey = approvalInvalidation.syncSession().key
  const routeKey = router.currentRoute.value.fullPath
  const id = props.id
  const sequence = ++loadSequence
  handlingSequence++
  reviewSequence++
  detail.value = undefined
  resetHandling()
  signVisible.value = false
  ncrCreateVisible.value = false
  Object.assign(signForm, { node: 'PREPARER', password: '', comment: '' })
  ncrReviewDetail.value = undefined
  ncrSignatureEvidence.value = undefined
  handlingLoading.value = false
  handlingSaving.value = false
  signing.value = false
  closing.value = false
  ncrCreateLoading.value = false
  ncrCreateError.value = ''
  ncrReviewError.value = ''
  ncrSignatureEvidenceError.value = ''
  Object.assign(ncrCreateForm, { qaReviewerUserId: undefined, nonconformanceReason: '', remark: '', signaturePassword: '', idempotencyKey: '' })
  loading.value = true
  error.value = ''
  handlingError.value = ''
  try {
    const record = await getDeviation(id, { ignoreErrorMessage: true, isRequestContextCurrent: () => approvalInvalidation.isCurrent(sessionKey) && routeKey === router.currentRoute.value.fullPath && id === props.id && sequence === loadSequence })
    if (!approvalInvalidation.isCurrent(sessionKey) || routeKey !== router.currentRoute.value.fullPath || id !== props.id || sequence !== loadSequence) return
    detail.value = record
    await Promise.all([loadHandling(), loadNcrReviewDetail()])
  } catch {
    if (approvalInvalidation.isCurrent(sessionKey) && routeKey === router.currentRoute.value.fullPath && id === props.id && sequence === loadSequence) error.value = '偏差详情加载失败，请重试'
  } finally {
    if (approvalInvalidation.isCurrent(sessionKey) && routeKey === router.currentRoute.value.fullPath && id === props.id && sequence === loadSequence) loading.value = false
  }
}
watch(() => props.id, () => {
  detailTab.value = props.initialTab || 'business'
  return load()
}, { immediate: true })
</script>

<style scoped>
.handling-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  text-align: left;
}

.handling-card__actions :deep(.el-button) {
  margin-left: 0;
}

.deviation-detail-tabs :deep(.el-tabs__content) {
  max-height: max(260px, calc(100dvh - 310px));
  overflow-y: auto;
}

.deviation-detail-tabs :deep(.el-tab-pane) {
  min-width: 0;
  padding-right: 8px;
}

.deviation-detail-tabs .edhr-detail-section:last-child {
  margin-bottom: 0;
  border-bottom: 0;
}

.handling-readonly :deep(.el-descriptions__content) {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.ncr-disposition-evidence :deep(.el-descriptions__content) {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
</style>
