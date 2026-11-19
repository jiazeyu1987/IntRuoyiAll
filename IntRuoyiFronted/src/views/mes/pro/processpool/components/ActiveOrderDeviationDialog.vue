<template>
  <el-dialog :model-value="true" title="发起订单偏差" width="680px" :close-on-click-modal="false" :close-on-press-escape="!submitting" :show-close="!submitting" @close="emit('close')">
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon class="mb-12px" />
    <el-skeleton v-if="loading" :rows="4" animated />
    <el-form v-else ref="formRef" :model="form" :rules="rules" :disabled="submitting || !batchesLoaded" label-width="110px">
      <el-alert v-if="batchesLoaded && !batches.length" title="偏差将关联当前生产订单；正式批记录在完工后生成。" type="info" :closable="false" class="mb-12px" />
      <el-form-item label="生产订单"><el-input :model-value="orderCode" disabled /></el-form-item>
      <el-form-item v-if="batches.length" label="批记录" prop="batchExecutionId">
        <el-select v-model="form.batchExecutionId" placeholder="请选择本订单正式批记录" style="width:100%" :disabled="batches.length === 1">
          <el-option v-for="batch in batches" :key="batch.batchExecutionId" :label="batch.batchExecutionCode || batch.batchCode || String(batch.batchExecutionId)" :value="Number(batch.batchExecutionId)" />
        </el-select>
      </el-form-item>
        <el-form-item label="等级" prop="level"><el-radio-group v-model="form.level"><el-radio label="NORMAL">普通</el-radio><el-radio label="CRITICAL">重大</el-radio></el-radio-group></el-form-item>
        <el-form-item label="分类" prop="categoryCodes"><el-checkbox-group v-model="form.categoryCodes"><el-checkbox v-for="item in categoryOptions" :key="item.value" :label="item.value">{{ item.label }}</el-checkbox></el-checkbox-group></el-form-item>
        <el-form-item label="偏差描述" prop="description"><el-input v-model="form.description" type="textarea" :rows="3" maxlength="4000" show-word-limit /></el-form-item>
        <el-form-item label="等级依据" prop="levelBasis"><el-input v-model="form.levelBasis" type="textarea" :rows="2" maxlength="4000" show-word-limit /></el-form-item>
        <el-form-item label="发现位置"><el-input v-model="form.discoveryLocation" maxlength="255" /></el-form-item>
        <el-form-item label="紧急措施"><el-input v-model="form.emergencyAction" type="textarea" :rows="2" maxlength="4000" /></el-form-item>
        <el-form-item label="接收调查人员" prop="receiverId"><DeviationReceiverSelect v-model="form.receiverId" @change="value => form.receiverName = value?.nickname || ''" /></el-form-item>
        <el-form-item label="签名凭据" prop="signaturePassword"><SignatureCredentialInput v-model="form.signaturePassword" /></el-form-item>

    </el-form>
    <template #footer>
      <el-button :disabled="submitting" @click="emit('close')">取消</el-button>
      <el-button v-if="!loading && !batchesLoaded" @click="loadBatches">重新加载</el-button>
      <el-button type="primary" :loading="submitting" :disabled="loading || !batchesLoaded" @click="submit">签名并发起</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import { createSecureUuid } from '@/utils/uuid'
import SignatureCredentialInput from '@/components/SignatureCredentialInput/SignatureCredentialInput.vue'
import DeviationReceiverSelect from '@/views/mes/pro/edhr-deviation/components/DeviationReceiverSelect.vue'
import { createActiveOrderDeviation, getActiveOrderDeviationCreationBatches, type DeviationBatchOptionRespVO, type DeviationCreateReqVO } from '@/api/mes/pro/edhr/deviation'
import { useApprovalCenterInvalidationStore } from '@/store/modules/approvalCenterInvalidation'

const props = defineProps<{ activeOrderId: number | string; orderCode?: string }>()
const emit = defineEmits<{ close: [] }>()
const route = useRoute()
const approvalInvalidation = useApprovalCenterInvalidationStore()
const message = useMessage()
const loading = ref(false)
const submitting = ref(false)
const error = ref('')
const batchesLoaded = ref(false)
const batches = ref<DeviationBatchOptionRespVO[]>([])
const formRef = ref<FormInstance>()
const form = reactive<DeviationCreateReqVO>({ batchExecutionId: undefined, level: 'NORMAL', categoryCodes: [], description: '', levelBasis: '', discoveryLocation: '', emergencyAction: '', idempotencyKey: `EDHR_ORDER_DEVIATION_${props.activeOrderId}_${createSecureUuid()}`, signaturePassword: '' })
const categoryOptions = [
  { label: '生产过程', value: 'PRODUCTION_PROCESS' }, { label: '生产公用系统', value: 'PRODUCTION_UTILITY' },
  { label: '检验', value: 'INSPECTION' }, { label: '验证确认', value: 'VALIDATION_CONFIRMATION' },
  { label: '物料管理', value: 'MATERIAL_MANAGEMENT' }, { label: '文件记录', value: 'DOCUMENT_RECORD' },
  { label: '产品放行', value: 'PRODUCT_RELEASE' }, { label: '其他系统', value: 'OTHER_SYSTEM' }
]
const rules: FormRules = {
  batchExecutionId: [{ validator: (_rule, value, callback) => batches.value.length && !value ? callback(new Error('请选择正式批记录')) : callback(), trigger: 'change' }],
  level: [{ required: true, message: '请选择偏差等级', trigger: 'change' }],
  categoryCodes: [{ type: 'array', min: 1, required: true, message: '至少选择一个分类', trigger: 'change' }],
  description: [{ required: true, message: '请填写偏差描述', trigger: 'blur' }],
  receiverId: [{ required: true, message: '请选择负责接收并调查偏差的人员', trigger: 'change' }],
  levelBasis: [{ required: true, message: '请填写等级依据', trigger: 'blur' }],
  signaturePassword: [{ required: true, message: '请输入所选签名方式的六位 PIN 码或密码', trigger: 'blur' }]
}

let generation = 0
const captureContext = () => {
  const current = ++generation
  const orderId = props.activeOrderId
  const sessionKey = approvalInvalidation.syncSession().key
  const routeKey = route.fullPath
  return () => current === generation && orderId === props.activeOrderId && routeKey === route.fullPath && approvalInvalidation.isCurrent(sessionKey)
}
const invalidate = () => { generation++; form.signaturePassword = '' }
onBeforeUnmount(invalidate)
onDeactivated(() => { invalidate(); emit('close') })
watch(() => [props.activeOrderId, route.fullPath, approvalInvalidation.syncSession().key], () => { invalidate(); emit('close') }, { flush: 'sync' })

const loadBatches = async () => {
  const isCurrent = captureContext()
  loading.value = true
  error.value = ''
  batches.value = []
  batchesLoaded.value = false
  form.batchExecutionId = undefined
  try {
    const result = await getActiveOrderDeviationCreationBatches(props.activeOrderId)
    if (!isCurrent()) return
    if (!Array.isArray(result)) throw new Error('订单关联批记录返回格式无效')
    batches.value = result
    batchesLoaded.value = true
    if (result.length === 1) form.batchExecutionId = Number(result[0].batchExecutionId)
  } catch (cause: any) {
    if (isCurrent()) error.value = cause?.response?.data?.msg || cause?.message || '订单批记录加载失败，请重试'
  } finally { if (isCurrent()) loading.value = false }
}

const submit = async () => {
  if (submitting.value || loading.value || !batchesLoaded.value || (batches.value.length > 0 && !batches.value.some(batch => String(batch.batchExecutionId) === String(form.batchExecutionId)))) return
  const isCurrent = captureContext()
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid || !isCurrent()) return
  submitting.value = true
  error.value = ''
  try {
    const result = await createActiveOrderDeviation(props.activeOrderId, { ...form, categoryCodes: [...form.categoryCodes] })
    if (!isCurrent()) return
    if (String(result.activeOrderId) !== String(props.activeOrderId) || (form.batchExecutionId != null ? String(result.batchExecutionId) !== String(form.batchExecutionId) : result.batchExecutionId != null) || !result.id || !result.deviationCode) {
      throw new Error('发起回执缺少当前订单偏差信息，请核对后再操作')
    }
    message.success(`偏差已发起：${result.deviationCode}，已交接调查责任人`)
    emit('close')
  } catch (cause: any) {
    if (isCurrent()) error.value = cause?.response?.data?.msg || cause?.message || '偏差发起失败，请重试'
  } finally {
    form.signaturePassword = ''
    if (isCurrent()) submitting.value = false
  }
}
onMounted(loadBatches)
</script>
