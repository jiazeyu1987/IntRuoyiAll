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

      <el-descriptions :column="2" border>
        <el-descriptions-item label="版本号">V{{ detail.versionNo }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{ displayText(detail.classification) }}</el-descriptions-item>
        <el-descriptions-item label="首次获证日">{{ formatRegistrationCertificateDate(detail.firstObtainedDate) }}</el-descriptions-item>
        <el-descriptions-item label="批准日">{{ formatRegistrationCertificateDate(detail.approvalDate) }}</el-descriptions-item>
        <el-descriptions-item label="生效日">{{ formatRegistrationCertificateDate(detail.effectiveDate) }}</el-descriptions-item>
        <el-descriptions-item label="有效期至">{{ formatRegistrationCertificateDate(detail.expiryDate) }}</el-descriptions-item>
        <el-descriptions-item label="项目代码">{{ displayText(detail.projectCode) }}</el-descriptions-item>
        <el-descriptions-item label="注册证文件">
          <div
            v-if="detail.registrationFileId && detail.registrationFileName"
            class="detail-attachment"
            data-testid="registration-certificate-detail-attachment"
          >
            <Icon icon="lucide:paperclip" :size="16" />
            <span class="detail-attachment__name">{{ detail.registrationFileName }}</span>
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
                <strong v-if="item.versionNo">V{{ item.versionNo }}</strong>
                <strong v-else class="renewal-history__missing">版本信息缺失</strong>
                <span>提交时间 {{ formatDateTimeValue(item.occurredAt) }}</span>
              </div>
              <el-tag :type="item.categoryChanged ? 'warning' : 'info'">
                {{ item.categoryChanged ? '类别已变更' : '类别未变更' }}
              </el-tag>
            </div>

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
              <Icon icon="ep:document" />
              <span class="renewal-history__file-label">延续注册证文件</span>
              <span v-if="item.originalFileName" class="renewal-history__file-name">
                {{ item.originalFileName }}
              </span>
              <el-tag v-if="item.fileStatus === 'BOUND'" type="success">已归档</el-tag>
              <el-tag v-else-if="item.fileStatus === 'VOIDED'" type="info">已作废</el-tag>
              <el-tag v-else type="danger">缺少正式文件</el-tag>
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
  </ContentWrap>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { formatDateTimeValue } from '@/utils/formatTime'
import {
  getRegistrationCertificateDetail,
  getRegistrationCertificateHistory,
  type DccRegistrationCertificateDetailVO,
  type DccRegistrationCertificateHistoryItemVO
} from '@/api/dcc/registrationCertificate'
import { parsePositiveRouteQueryId } from '@/utils/routeQueryId'
import RegistrationCertificateActionPanel from '../workflow/ActionPanel.vue'
import {
  displayText,
  formatEntrustedEnterpriseNames,
  formatRegistrationCertificateDate,
  formatRegistrationCertificateReminder,
  formatRegistrationCertificateStatus,
  getRegistrationCertificateReminderTagType,
  getRegistrationCertificateStatusTagType
} from '../shared/state'

defineOptions({ name: 'DccRegistrationCertificateDetail' })

const route = useRoute()
const certificateId = computed(() => parsePositiveRouteQueryId(route.params.id))
const detailVersionId = computed(() => parsePositiveRouteQueryId(route.query.versionId))
const viewMode = computed(() => {
  if (route.query.mode === 'access-request') return 'access-request'
  if (route.query.mode === 'old-detail') return 'old-detail'
  return 'current'
})
const invalidRoute = computed(() => !certificateId.value)
const loading = ref(false)
const detail = ref<DccRegistrationCertificateDetailVO>()
const history = ref<DccRegistrationCertificateHistoryItemVO[]>([])
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
    if (!item.businessFileId || !item.fileKind) {
      return
    }
    if (item.fileKind === 'CHANGE_APPROVAL') {
      changeApprovalFileIndex += 1
    }
    files.push({
      businessFileId: item.businessFileId,
      fileKind: item.fileKind,
      label: item.fileKind === 'CHANGE_APPROVAL'
        ? `变更批件文件 ${changeApprovalFileIndex}`
        : `业务文件 ${files.length + 1}`
    })
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

onMounted(loadDetail)
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

.renewal-history__parameters {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px 24px;
  margin: 0;
}

.renewal-history__parameters div {
  min-width: 0;
}

.renewal-history__parameters dt,
.renewal-history__parameters dd {
  margin: 0;
}

.renewal-history__parameters dd {
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

  .renewal-history__parameters {
    grid-template-columns: 1fr;
  }
}
</style>
