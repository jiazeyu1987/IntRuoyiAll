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
          <el-tag :type="getMissingMarkerTagType(detail.hasRegistrationFile)">
            {{ formatMissingMarker(detail.hasRegistrationFile) }}
          </el-tag>
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

      <el-card class="detail-card" shadow="never">
        <template #header>历史记录</template>
        <el-table :data="history" row-key="eventType">
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
  formatMissingMarker,
  formatRegistrationCertificateDate,
  formatRegistrationCertificateReminder,
  formatRegistrationCertificateStatus,
  getMissingMarkerTagType,
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

.detail-enterprise-names {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}

.detail-remark {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
