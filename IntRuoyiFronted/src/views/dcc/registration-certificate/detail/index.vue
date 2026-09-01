<template>
  <ContentWrap data-testid="registration-certificate-detail-page">
    <el-alert v-if="invalidRoute" type="error" show-icon :closable="false" title="注册证编号无效，无法加载详情" />
    <el-skeleton v-else-if="loading" :rows="8" animated />
    <el-empty v-else-if="!detail" description="未加载到注册证详情" />
    <div v-else class="registration-certificate-detail">
      <div class="detail-title">
        <div>
          <h2>{{ detail.certificateNo }}</h2>
          <p>{{ detail.ownerCompanyName }} / {{ detail.productName }}</p>
        </div>
        <el-tag :type="getRegistrationCertificateStatusTagType(detail.status)" effect="dark">
          {{ detail.status === 'OLD' ? '已失效，失效日期 ' + formatRegistrationCertificateDate(detail.expiryDate) : formatRegistrationCertificateStatus(detail.status) }}
        </el-tag>
        <el-tag :type="getRegistrationCertificateReminderTagType(detail.reminderColor)">
          提醒：{{ formatRegistrationCertificateReminder(detail.visualState) }}
        </el-tag>
      </div>

      <el-descriptions :column="detailDescriptionColumns" border>
        <el-descriptions-item label="版本号">第 {{ detail.versionNo }} 版</el-descriptions-item>
        <el-descriptions-item label="分类">{{ displayText(detail.classification) }}</el-descriptions-item>
        <el-descriptions-item label="首次获证日">{{ formatRegistrationCertificateDate(detail.firstObtainedDate) }}</el-descriptions-item>
        <el-descriptions-item label="批准日">{{ formatRegistrationCertificateDate(detail.approvalDate) }}</el-descriptions-item>
        <el-descriptions-item label="生效日">{{ formatRegistrationCertificateDate(detail.effectiveDate) }}</el-descriptions-item>
        <el-descriptions-item label="有效期至">{{ formatRegistrationCertificateDate(detail.expiryDate) }}</el-descriptions-item>
        <el-descriptions-item label="项目代码">{{ displayText(detail.projectCode) }}</el-descriptions-item>
        <el-descriptions-item label="上传人">
          {{ displayOperationName(detail.uploadOperatorName) }}
        </el-descriptions-item>
        <el-descriptions-item label="上传时间">
          {{ formatDateTimeValue(detail.uploadedAt, '缺少正式记录') }}
        </el-descriptions-item>
        <el-descriptions-item label="上传审批人">
          {{ displayOperationName(detail.uploadApproverName) }}
        </el-descriptions-item>
        <el-descriptions-item label="上传审批时间">
          {{ formatDateTimeValue(detail.uploadApprovedAt, '缺少正式记录') }}
        </el-descriptions-item>
        <el-descriptions-item label="注册证文件">
          <div
            v-if="detail.registrationFileId && detail.registrationFileName"
            class="detail-attachment"
            data-testid="registration-certificate-detail-attachment"
          >
            <span class="detail-attachment__identity">
              <Icon icon="lucide:paperclip" :size="16" />
              <span class="detail-attachment__name">{{ detail.registrationFileName }}</span>
            </span>
            <span class="detail-attachment__actions">
              <el-button
                link
                type="primary"
                data-testid="registration-certificate-detail-attachment-preview"
                @click="openAttachmentPreview(detail.registrationFileId, detail.registrationFileName)"
              >
                <Icon icon="lucide:eye" />在线查看
              </el-button>
              <el-button
                v-hasPermi="['dcc:registration-certificate:access-request:create']"
                link
                type="primary"
                data-testid="registration-certificate-detail-attachment-download"
                :loading="isAttachmentDownloading(detail.registrationFileId)"
                @click="downloadAttachment(detail.registrationFileId)"
              >
                <Icon icon="lucide:download" />下载
              </el-button>
              <el-button
                v-hasPermi="['dcc:registration-certificate:access-request:create']"
                link
                data-testid="registration-certificate-detail-attachment-request-download"
                @click="openDownloadRequest(detail.registrationFileId)"
              >
                <Icon icon="lucide:file-key-2" />申请下载
              </el-button>
            </span>
          </div>
          <span v-else>未提供</span>
        </el-descriptions-item>
        <el-descriptions-item label="注册人">{{ displayText(detail.registrantName) }}</el-descriptions-item>
        <el-descriptions-item label="生产方式">
          自产：{{ detail.selfProduction ? '是' : '否' }} / 委托：{{ detail.entrustedProduction ? '是' : '否' }}
        </el-descriptions-item>
        <el-descriptions-item label="型号规格">{{ displayText(detail.modelSpecification) }}</el-descriptions-item>
        <el-descriptions-item label="结构组成">{{ displayText(detail.structureComposition) }}</el-descriptions-item>
        <el-descriptions-item label="适用范围">{{ displayText(detail.intendedUse) }}</el-descriptions-item>
        <el-descriptions-item label="技术要求">{{ displayText(detail.technicalRequirements) }}</el-descriptions-item>
        <el-descriptions-item label="住所">{{ displayText(detail.residenceAddress) }}</el-descriptions-item>
        <el-descriptions-item label="生产地址">{{ displayText(detail.productionAddress) }}</el-descriptions-item>
      </el-descriptions>

      <el-card class="detail-card" shadow="never">
        <template #header>备注</template>
        <div class="detail-remark">
          {{ displayText(detail.remark) }}
        </div>
      </el-card>

      <el-card class="detail-card" shadow="never">
        <template #header>受托生产企业</template>
        <div class="detail-enterprise-names">
          {{ formatEntrustedEnterpriseNames(detail.entrustedEnterprisesJson) }}
        </div>
      </el-card>

      <RegistrationCertificateActionPanel
        v-if="viewMode === 'access-request'"
        initial-action="access"
        read-only
        :certificate-id="detail.certificateId"
        :version-id="detail.versionId"
        :project-code-id="detail.projectCodeId"
        :business-file-id="detail.registrationFileId"
        :downloadable-files="downloadableFiles"
        :initial-access-request-type="routeDownloadFileId ? 'DOWNLOAD_FILE' : undefined"
        :initial-download-business-file-id="routeDownloadFileId"
      />

      <el-alert
        v-if="attachmentActionError"
        type="error"
        show-icon
        :closable="false"
        :title="attachmentActionError"
      />

      <el-card
        class="detail-card"
        data-testid="registration-certificate-renewal-history"
        shadow="never"
      >
        <template #header>
          <div class="detail-card__header">
            <span>延续记录</span>
            <el-tag type="info">{{ renewalHistory.length }} 次</el-tag>
          </div>
        </template>
        <el-empty v-if="renewalHistory.length === 0" description="暂无延续记录" />
        <div v-else class="renewal-history">
          <section
            v-for="item in renewalHistory"
            :key="String(item.targetVersionId)"
            class="renewal-history__item"
          >
            <div class="renewal-history__heading">
              <div class="renewal-history__version">
                <strong v-if="item.versionNo">第 {{ item.versionNo }} 版</strong>
                <strong v-else class="renewal-history__missing">版本信息缺失</strong>
                <span>记录时间 {{ formatDateTimeValue(item.occurredAt) }}</span>
              </div>
              <el-tag :type="getCategoryChangedTagType(item.categoryChanged)">
                {{ formatCategoryChangedStatus(item.categoryChanged) }}
              </el-tag>
            </div>

            <div class="renewal-history__section-label">操作记录</div>
            <dl class="renewal-history__audit">
              <div>
                <dt>延续操作人</dt>
                <dd>{{ displayOperationName(item.renewalOperatorName) }}</dd>
              </div>
              <div>
                <dt>延续操作时间</dt>
                <dd>{{ formatDateTimeValue(item.renewalOperatedAt, '缺少正式记录') }}</dd>
              </div>
              <div>
                <dt>延续审批人</dt>
                <dd>{{ displayOperationName(item.renewalApproverName) }}</dd>
              </div>
              <div>
                <dt>延续审批时间</dt>
                <dd>{{ formatDateTimeValue(item.renewalApprovedAt, '缺少正式记录') }}</dd>
              </div>
            </dl>

            <div class="renewal-history__section-label">延续参数</div>
            <dl class="renewal-history__parameters">
              <div>
                <dt>批准日期</dt>
                <dd>{{ formatRegistrationCertificateDate(item.approvalDate) }}</dd>
              </div>
              <div>
                <dt>生效日期</dt>
                <dd>{{ formatRegistrationCertificateDate(item.effectiveDate) }}</dd>
              </div>
              <div>
                <dt>有效期至</dt>
                <dd>{{ formatRegistrationCertificateDate(item.expiryDate) }}</dd>
              </div>
              <div>
                <dt>类别是否变更</dt>
                <dd>{{ formatBooleanChoice(item.categoryChanged) }}</dd>
              </div>
              <template v-if="item.categoryChanged">
                <div>
                  <dt>变更后注册证号</dt>
                  <dd>{{ displayText(item.certificateNo) }}</dd>
                </div>
                <div>
                  <dt>变更后类别</dt>
                  <dd>{{ displayText(item.classification) }}</dd>
                </div>
              </template>
            </dl>

            <div class="renewal-history__file">
              <Icon icon="lucide:file-text" />
              <span class="renewal-history__file-label">延续注册证文件</span>
              <span v-if="item.originalFileName" class="renewal-history__file-name">
                {{ item.originalFileName }}
              </span>
              <el-tag v-if="item.fileStatus === 'BOUND'" type="success">已归档</el-tag>
              <el-tag v-else-if="item.fileStatus === 'VOIDED'" type="info">已作废</el-tag>
              <el-tag v-else type="danger">缺少正式文件</el-tag>
              <span
                v-if="item.fileStatus === 'BOUND' && item.businessFileId && item.originalFileName"
                class="renewal-history__file-actions"
              >
                <el-button
                  link
                  type="primary"
                  data-testid="registration-certificate-renewal-attachment-preview"
                  @click="openAttachmentPreview(item.businessFileId, item.originalFileName)"
                >
                  <Icon icon="lucide:eye" />在线查看
                </el-button>
                <el-button
                  v-hasPermi="['dcc:registration-certificate:access-request:create']"
                  link
                  type="primary"
                  data-testid="registration-certificate-renewal-attachment-download"
                  :loading="isAttachmentDownloading(item.businessFileId)"
                  @click="downloadAttachment(item.businessFileId)"
                >
                  <Icon icon="lucide:download" />下载
                </el-button>
                <el-button
                  v-hasPermi="['dcc:registration-certificate:access-request:create']"
                  link
                  @click="openDownloadRequest(item.businessFileId)"
                >
                  <Icon icon="lucide:file-key-2" />申请下载
                </el-button>
              </span>
            </div>
          </section>
        </div>
      </el-card>

      <el-card class="detail-card" shadow="never">
        <template #header>历史记录</template>
        <el-table :data="otherHistory">
          <el-table-column label="事件" prop="eventType" min-width="150" />
          <el-table-column label="对象" prop="itemType" min-width="150" />
          <el-table-column label="操作人" prop="actorId" width="120" />
          <el-table-column label="变更前" prop="beforeValueJson" min-width="220" show-overflow-tooltip />
          <el-table-column label="变更后" prop="afterValueJson" min-width="220" show-overflow-tooltip />
          <el-table-column label="批件文件" width="120">
            <template #default="{ row }">
              <el-tag v-if="row.businessFileId" type="success">可申请下载</el-tag>
              <span v-else>-</span>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <Dialog
      v-model="previewDialogVisible"
      :title="selectedPreviewTitle || '注册证附件在线查看'"
      width="min(1120px, 94vw)"
      destroy-on-close
    >
      <ProtectedPdfViewer
        v-if="selectedPreviewSource"
        :preview-source="selectedPreviewSource"
        title="附件在线查看"
      />
      <el-empty v-else description="暂无可在线查看的附件" />
    </Dialog>
  </ContentWrap>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { formatDateTimeValue } from '@/utils/formatTime'
import {
  downloadRegistrationCertificateFile,
  getRegistrationCertificateDetail,
  getRegistrationCertificateHistory,
  type DccRegistrationCertificateDetailVO,
  type DccRegistrationCertificateHistoryItemVO
} from '@/api/dcc/registrationCertificate'
import {
  buildDccRegistrationCertificatePreviewSource,
  type OnlineFilePreviewSource
} from '@/api/common/filePreview'
import { downloadByData } from '@/utils/filt'
import { parsePositiveRouteQueryId } from '@/utils/routeQueryId'
import ProtectedPdfViewer from '@/views/dcc/controlled-file/view/index.vue'
import RegistrationCertificateActionPanel from '../workflow/ActionPanel.vue'
import {
  displayText,
  formatEntrustedEnterpriseNames,
  formatRegistrationCertificateDate,
  formatRegistrationCertificateReminder,
  formatRegistrationCertificateStatus,
  getRegistrationCertificateReminderTagType,
  getRegistrationCertificateStatusTagType,
  resolveRegistrationCertificateUserMessage
} from '../shared/state'

defineOptions({ name: 'DccRegistrationCertificateDetail' })

const route = useRoute()
const router = useRouter()
const certificateId = computed(() => parsePositiveRouteQueryId(route.params.id))
const detailVersionId = computed(() => parsePositiveRouteQueryId(route.query.versionId))
const routeDownloadFileId = computed(() => parsePositiveRouteQueryId(route.query.downloadFileId))
const viewMode = computed(() => {
  if (route.query.mode === 'access-request') return 'access-request'
  if (route.query.mode === 'old-detail') return 'old-detail'
  return 'current'
})
const invalidRoute = computed(() => !certificateId.value)
const loading = ref(false)
const detail = ref<DccRegistrationCertificateDetailVO>()
const history = ref<DccRegistrationCertificateHistoryItemVO[]>([])
const previewDialogVisible = ref(false)
const selectedPreviewSource = ref<OnlineFilePreviewSource | null>(null)
const selectedPreviewTitle = ref('')
const downloadingBusinessFileId = ref('')
const attachmentActionError = ref('')
const viewportWidth = ref(typeof window === 'undefined' ? 1024 : window.innerWidth)
const detailDescriptionColumns = computed(() => viewportWidth.value <= 720 ? 1 : 2)
const renewalHistory = computed(() => history.value
  .filter((item) => item.eventType === 'RENEWAL_UPLOADED')
  .slice()
  .reverse())
const otherHistory = computed(() => history.value
  .filter((item) => item.eventType !== 'RENEWAL_UPLOADED'))

const formatBooleanChoice = (value?: boolean) => {
  if (value === true) return '是'
  if (value === false) return '否'
  return '缺少正式记录'
}

const displayOperationName = (value?: string) => {
  if (typeof value !== 'string' || !value.trim()) return '缺少正式记录'
  return value.trim()
}

const formatCategoryChangedStatus = (value?: boolean) => {
  if (value === true) return '类别已变更'
  if (value === false) return '类别未变更'
  return '类别变更记录缺失'
}

const getCategoryChangedTagType = (value?: boolean) => {
  if (value === true) return 'warning'
  if (value === false) return 'info'
  return 'danger'
}

const openAttachmentPreview = (businessFileId: number | string, fileName: string) => {
  attachmentActionError.value = ''
  selectedPreviewSource.value = buildDccRegistrationCertificatePreviewSource(businessFileId)
  selectedPreviewTitle.value = fileName
  previewDialogVisible.value = true
}

const isAttachmentDownloading = (businessFileId: number | string) =>
  downloadingBusinessFileId.value === String(businessFileId)

const resolveAttachmentDownloadError = (error: unknown) => {
  const message = String((error as { message?: string })?.message || '').trim()
  const messages: Record<string, string> = {
    '注册证访问授权范围不合法':
      '当前附件尚未获得下载授权，请先申请下载。',
    '注册证访问授权已过期': '附件下载授权已过期，请重新申请。',
    '注册证访问授权已撤销': '附件下载授权已撤销，请重新申请。',
    '注册证下载授权已使用': '本次下载授权已使用，请重新申请。'
  }
  return messages[message] || resolveRegistrationCertificateUserMessage(
    error,
    '注册证附件下载失败，请确认下载授权后重试。'
  )
}

const downloadAttachment = async (businessFileId: number | string) => {
  attachmentActionError.value = ''
  downloadingBusinessFileId.value = String(businessFileId)
  try {
    const result = await downloadRegistrationCertificateFile(businessFileId)
    downloadByData(result.blob, result.fileName, result.blob.type || 'application/octet-stream')
  } catch (error) {
    attachmentActionError.value = resolveAttachmentDownloadError(error)
  } finally {
    downloadingBusinessFileId.value = ''
  }
}

const openDownloadRequest = async (businessFileId: number | string) => {
  attachmentActionError.value = ''
  await router.replace({
    query: {
      ...route.query,
      mode: 'access-request',
      downloadFileId: String(businessFileId)
    }
  })
  await nextTick()
  document.querySelector('[data-testid="registration-certificate-access-request-action"]')
    ?.scrollIntoView({ behavior: 'smooth', block: 'center' })
}

type DownloadableFileOption = {
  businessFileId: number | string
  fileKind: string
  label: string
}

const downloadableFiles = computed<DownloadableFileOption[]>(() => {
  if (!detail.value) {
    return []
  }
  const files: DownloadableFileOption[] = []
  if (detail.value.registrationFileId) {
    files.push({
      businessFileId: detail.value.registrationFileId,
      fileKind: 'REGISTRATION_CERTIFICATE',
      label: detail.value.status === 'OLD' ? '失效注册证文件' : '注册证文件'
    })
  }
  let changeApprovalFileIndex = 0
  history.value.forEach((item) => {
    if (!item.businessFileId) {
      return
    }
    if (item.fileKind === 'CHANGE_APPROVAL') {
      changeApprovalFileIndex += 1
      files.push({
        businessFileId: item.businessFileId,
        fileKind: item.fileKind,
        label: `变更批件文件 ${changeApprovalFileIndex}`
      })
      return
    }
    if (item.fileKind === 'REGISTRATION_CERTIFICATE' && item.fileStatus === 'BOUND') {
      files.push({
        businessFileId: item.businessFileId,
        fileKind: item.fileKind,
        label: item.versionNo ? `延续注册证文件（第 ${item.versionNo} 版）` : '延续注册证文件'
      })
    }
  })
  const seen = new Set<string>()
  return files.filter((file) => {
    const key = String(file.businessFileId)
    if (seen.has(key)) {
      return false
    }
    seen.add(key)
    return true
  })
})

const loadDetail = async () => {
  if (!certificateId.value) return
  loading.value = true
  try {
    detail.value = await getRegistrationCertificateDetail(certificateId.value, detailVersionId.value)
    history.value = await getRegistrationCertificateHistory(certificateId.value)
  } finally {
    loading.value = false
  }
}

const updateViewportWidth = () => {
  viewportWidth.value = window.innerWidth
}

onMounted(() => {
  window.addEventListener('resize', updateViewportWidth)
  void loadDetail()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateViewportWidth)
})
</script>

<style scoped>
.registration-certificate-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-title {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.detail-title h2 {
  margin: 0 0 6px;
}

.detail-title p {
  margin: 0;
  color: var(--el-text-color-secondary);
}

.detail-card {
  margin-top: 4px;
}

.detail-card__header,
.renewal-history__heading,
.renewal-history__file {
  display: flex;
  align-items: center;
}

.detail-card__header,
.renewal-history__heading {
  justify-content: space-between;
  gap: 12px;
}

.detail-card__header {
  font-weight: 600;
}

.renewal-history__item {
  padding: 4px 0 20px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.renewal-history__item + .renewal-history__item {
  padding-top: 20px;
}

.renewal-history__item:last-child {
  padding-bottom: 4px;
  border-bottom: 0;
}

.renewal-history__version {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.renewal-history__version strong {
  color: var(--el-text-color-primary);
  font-size: 16px;
}

.renewal-history__version span,
.renewal-history__section-label,
.renewal-history__parameters dt,
.renewal-history__audit dt,
.renewal-history__file-label {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.renewal-history__missing {
  color: var(--el-color-danger) !important;
}

.renewal-history__section-label {
  margin: 18px 0 8px;
}

.renewal-history__parameters,
.renewal-history__audit {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px 24px;
  margin: 0;
}

.renewal-history__parameters div,
.renewal-history__audit div {
  min-width: 0;
}

.renewal-history__parameters dt,
.renewal-history__parameters dd,
.renewal-history__audit dt,
.renewal-history__audit dd {
  margin: 0;
}

.renewal-history__parameters dd,
.renewal-history__audit dd {
  margin-top: 4px;
  color: var(--el-text-color-primary);
  line-height: 22px;
  overflow-wrap: anywhere;
}

.renewal-history__file {
  min-height: 36px;
  gap: 8px;
  padding: 10px 12px;
  margin-top: 16px;
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 4px;
}

.renewal-history__file-name {
  min-width: 0;
  color: var(--el-text-color-primary);
  overflow-wrap: anywhere;
}

.renewal-history__file-actions,
.detail-attachment__actions {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-left: auto;
}

.renewal-history__file-actions :deep(.el-button),
.detail-attachment__actions :deep(.el-button) {
  gap: 4px;
  margin-left: 0;
}

.detail-enterprise-names {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}

.detail-attachment {
  display: inline-flex;
  max-width: 100%;
  align-items: center;
  gap: 8px;
  color: var(--el-text-color-primary);
}

.detail-attachment__name {
  min-width: 0;
  overflow-wrap: anywhere;
}

.detail-attachment__identity {
  display: inline-flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
}

.detail-remark {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 720px) {
  .renewal-history__heading,
  .renewal-history__version,
  .renewal-history__file {
    align-items: flex-start;
    flex-direction: column;
  }

  .renewal-history__parameters,
  .renewal-history__audit {
    grid-template-columns: 1fr;
  }

  .renewal-history__file-actions,
  .detail-attachment__actions {
    width: 100%;
    flex-wrap: wrap;
    margin-left: 0;
  }

  .detail-attachment {
    display: flex;
    width: 100%;
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
