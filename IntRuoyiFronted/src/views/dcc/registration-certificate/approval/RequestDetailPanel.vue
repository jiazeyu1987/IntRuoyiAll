<template>
  <el-card shadow="never" v-loading="loading" data-testid="registration-approval-submission">
    <template #header>本次注册证申请材料</template>
    <el-alert v-if="error" type="error" :title="error" :closable="false" show-icon />
    <template v-else-if="detail">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="申请状态">{{ statusLabels[detail.status] || detail.status }}</el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ formatDateTimeValue(detail.requestedAt) }}</el-descriptions-item>
        <el-descriptions-item label="所属公司">{{ detail.submission?.ownerCompanyName || '未填写' }}</el-descriptions-item>
        <el-descriptions-item label="申请说明">{{ detail.purpose || '未填写' }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.rejectReason" label="驳回原因">{{ detail.rejectReason }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.withdrawReason" label="撤回原因">{{ detail.withdrawReason }}</el-descriptions-item>
      </el-descriptions>
      <el-form v-if="facts" class="mt-16px" label-width="140px"><FactsFields :model-value="facts" readonly /></el-form>
      <el-descriptions v-if="draft" :column="2" border>
        <el-descriptions-item v-for="(label, key) in draftLabels" :key="key" :label="label">{{ display(draft[key]) }}</el-descriptions-item>
      </el-descriptions>
      <el-table v-if="detail.changes.length" :data="detail.changes" border class="mt-16px">
        <el-table-column label="变更事项" width="130"><template #default="{row}">{{ changeLabels[row.itemType] || row.itemType }}</template></el-table-column>
        <el-table-column label="变更前"><template #default="{row}"><div class="value-text">{{ describeChange(row.beforeValueJson) }}</div></template></el-table-column>
        <el-table-column label="变更后"><template #default="{row}"><div class="value-text">{{ describeChange(row.afterValueJson) }}</div></template></el-table-column>
      </el-table>
      <el-table :data="detail.files" border class="mt-16px" empty-text="本次申请没有文件材料">
        <el-table-column label="材料类型" width="160"><template #default="{row}">{{ fileLabels[row.fileKind] || row.fileKind }}</template></el-table-column>
        <el-table-column prop="originalName" label="文件名" />
        <el-table-column label="查看" width="90"><template #default="{row}"><el-button link type="primary" @click="preview(row)">在线查看</el-button></template></el-table-column>
      </el-table>
    </template>
  </el-card>
  <el-dialog v-model="previewOpen" :title="previewName" width="90%" destroy-on-close append-to-body>
    <ProtectedPdfViewer v-if="previewSource" :preview-source="previewSource" />
  </el-dialog>
</template>
<script setup lang="ts">
import request from '@/config/axios'
import FactsFields from '../shared/FactsFields.vue'
import { parseFacts, factLabels, factOptions, type CertificateType } from '../shared/facts'
import { formatDateTimeValue } from '@/utils/formatTime'
import ProtectedPdfViewer from '@/views/dcc/controlled-file/view/index.vue'
import { buildDccRegistrationCertificateApprovalPreviewSource, type OnlineFilePreviewSource } from '@/api/common/filePreview'

const props = defineProps<{ requestId: number; processInstanceId: string }>()
const loading = ref(false)
const error = ref('')
const detail = ref<any>()
const draft = computed(() => detail.value?.submission?.draft)
const facts = computed(() => {
  const json = draft.value?.factsJson || detail.value?.submission?.factsJson
  return json ? parseFacts(json) : undefined
})
const statusLabels: Record<string,string> = {SUBMITTED:'已提交',BPM_BOUND:'审批中',APPROVED:'已批准',REJECTED:'已驳回',WITHDRAWN:'已撤回'}
const draftLabels: Record<string,string> = {productName:'产品中文名称',registrantName:'注册人',modelSpecification:'型号规格',structureComposition:'结构组成',intendedUse:'适用范围',technicalRequirements:'技术要求',residenceAddress:'住所',productionAddress:'生产地址',selfProduction:'自产',entrustedProduction:'委托生产',entrustedEnterpriseIds:'受托企业编号',entrustedEnterprisesJson:'受托企业'}
const changeLabels: Record<string,string> = {CERTIFICATE_INFORMATION:'注册证信息',PRODUCTION_RELATION:'生产关系',PRODUCT_NAME:'产品名称',REGISTRANT_NAME:'注册人',MODEL_SPECIFICATION:'型号规格',STRUCTURE_COMPOSITION:'结构组成',INTENDED_USE:'适用范围',TECHNICAL_REQUIREMENTS:'技术要求',RESIDENCE_ADDRESS:'住所',PRODUCTION_ADDRESS:'生产地址',OTHER_CONTENT:'其他',CERTIFICATE_FILE:'注册证文件',CERTIFICATE_ATTACHMENT:'附件',FDA_PRODUCT_DESCRIPTION:'FDA产品描述'}
const fileLabels: Record<string,string> = {REGISTRATION_CERTIFICATE:'注册证',CHANGE_APPROVAL:'变更批件',CERTIFICATE_ATTACHMENT:'附件',FDA_PRODUCT_DESCRIPTION:'FDA产品描述',RENEWAL_ACCEPTANCE_RECEIPT:'续证受理回执',RENEWAL_SUPPLEMENT_NOTICE:'续证补充通知'}
const nestedLabels: Record<string,string> = {draft:'本次完整信息',factsJson:'注册证信息',ceCertificates:'证书Rev.记录',revision:'Rev.',issueDate:'签发日期'}
const display = (value: any, key = '', type: CertificateType = facts.value?.certificateType || 'DOMESTIC'): string => {
  if (key === 'factsJson' && typeof value === 'string') return display(parseFacts(value))
  if (value == null || value === '') return '未填写'
  if (Array.isArray(value)) return value.map(item => display(item, key, type)).join('；') || '无'
  if (typeof value === 'object') {
    const certificateType = value.certificateType || type
    return Object.entries(value).map(([field, item]) => `${factLabels[field] || draftLabels[field] || nestedLabels[field] || field}：${display(item, field, certificateType)}`).join('\n')
  }
  if (key === 'certificateType') return value === 'DOMESTIC' ? '国内' : String(value)
  const option = factOptions(key, type).find(option => option.value === value)
  return option?.label || (value === true ? '是' : value === false ? '否' : String(value))
}
const describeChange = (value: string) => {
  if (!value) return '未填写'
  let parsed: any
  try { parsed = JSON.parse(value) } catch { return value }
  if (parsed && typeof parsed === 'object' && Object.hasOwn(parsed, 'value')) parsed = parsed.value
  if (typeof parsed === 'string' && /^[\[{]/.test(parsed.trim())) {
    try { parsed = JSON.parse(parsed) } catch { return parsed }
  }
  return display(parsed)
}
const previewOpen = ref(false)
const previewName = ref('')
const previewSource = shallowRef<OnlineFilePreviewSource>()
const preview = (file: any) => {
  previewSource.value = buildDccRegistrationCertificateApprovalPreviewSource(file.id, props.requestId, props.processInstanceId)
  previewName.value = file.originalName
  previewOpen.value = true
}
let sequence = 0
watch(() => [props.requestId, props.processInstanceId], async () => {
  const current = ++sequence
  loading.value = true
  error.value = ''
  detail.value = undefined
  previewOpen.value = false
  previewSource.value = undefined
  previewName.value = ''
  try {
    const result = await request.get({url:'/dcc/registration-certificates/approval-detail',params:{requestId:props.requestId,processInstanceId:props.processInstanceId}})
    if (current === sequence) detail.value = result
  } catch (e: any) {
    if (current === sequence) error.value = e.message || '申请材料加载失败'
  } finally { if (current === sequence) loading.value = false }
}, {immediate:true})
</script>
<style scoped>.value-text {white-space:pre-wrap;overflow-wrap:anywhere}</style>
