<template>
  <template v-if="releaseTransactionId && pending">
    <el-button
      v-hasPermi="['mes:pro-edhr-release:reject']"
      link
      type="danger"
      @click="open('reject')"
      >不予上市放行</el-button
    >
    <el-button
      v-hasPermi="['mes:pro-edhr-release:withdraw']"
      link
      type="warning"
      @click="open('withdraw')"
      >撤回申请</el-button
    >
  </template>
  <Dialog
    v-model="visible"
    :title="action === 'reject' ? '不予上市放行并转交QA' : '撤回上市放行申请'"
    width="580px"
  >
    <div v-loading="loading">
      <el-alert v-if="error" :title="error" type="error" :closable="false" />
      <el-alert
        :title="
          action === 'reject'
            ? '原上市审批将结束，批次冻结并转交指定QA进行正式质量处置。'
            : '仅原申请人可撤回尚未决定的申请。撤回后请在个人工作台补正并重新提交正式审查。'
        "
        type="warning"
        :closable="false"
      />
      <el-form label-position="top" @submit.prevent>
        <el-form-item label="原因" required
          ><el-input v-model="reason" type="textarea" :rows="3" maxlength="500"
        /></el-form-item>
        <template v-if="action === 'reject'">
          <el-form-item label="QA评审责任人" required
            ><UserSelectV2 v-model="qaReviewerUserId" placeholder="请选择具备QA处置权限的责任人"
          /></el-form-item>
          <el-form-item label="电子签名密码" required
            ><el-input v-model="password" type="password" show-password autocomplete="off"
          /></el-form-item>
        </template>
      </el-form>
    </div>
    <template #footer>
      <el-button :disabled="submitting" @click="visible = false">取消</el-button>
      <el-button
        type="danger"
        :loading="submitting"
        :disabled="
          loading ||
          !receipt ||
          !reason.trim() ||
          (action === 'reject' && (!qaReviewerUserId || !password))
        "
        @click="submit"
        >{{ action === 'reject' ? '签名确认不予放行' : '确认撤回' }}</el-button
      >
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import { ref, watch } from 'vue'
import { generateUUID } from '@/utils'
import UserSelectV2 from '@/views/system/user/components/UserSelectV2.vue'
import {
  getEdhrRelease,
  rejectEdhrRelease,
  withdrawEdhrRelease,
  type EdhrReleaseRowVO
} from '@/api/mes/pro/edhr/release'
const props = defineProps<{ releaseTransactionId?: string | number; pending: boolean }>()
const emit = defineEmits<{ changed: [] }>()
const visible = ref(false),
  loading = ref(false),
  submitting = ref(false),
  error = ref('')
const reason = ref(''),
  password = ref(''),
  qaReviewerUserId = ref<string | number>()
const action = ref<'reject' | 'withdraw'>('reject'),
  receipt = ref<EdhrReleaseRowVO>()
let generation = 0,
  key = ''
watch(
  () => props.releaseTransactionId,
  () => {
    generation++
    visible.value = false
    receipt.value = undefined
  }
)
watch(visible, (value) => {
  if (!value) {
    generation++
    password.value = ''
    receipt.value = undefined
  }
})
const open = async (next: 'reject' | 'withdraw') => {
  action.value = next
  visible.value = true
  loading.value = true
  error.value = ''
  reason.value = ''
  password.value = ''
  qaReviewerUserId.value = undefined
  receipt.value = undefined
  key = generateUUID()
  const token = ++generation
  try {
    const value = await getEdhrRelease(String(props.releaseTransactionId))
    if (token !== generation) return
    if (value.releaseStatus !== 'PENDING_APPROVAL' || !value.releaseApprovalWorkTaskId)
      throw new Error('当前申请不是正式待决定状态，请刷新列表')
    receipt.value = value
  } catch (failure) {
    if (token === generation)
      error.value = failure instanceof Error ? failure.message : String(failure)
  } finally {
    if (token === generation) loading.value = false
  }
}
const submit = async () => {
  if (submitting.value || !receipt.value || !props.releaseTransactionId) return
  submitting.value = true
  error.value = ''
  const token = generation
  const data = {
    releaseTransactionId: String(props.releaseTransactionId),
    workTaskId: String(receipt.value.releaseApprovalWorkTaskId),
    expectedVersion: receipt.value.version,
    idempotencyKey: key
  }
  try {
    if (action.value === 'reject')
      await rejectEdhrRelease(
        {
          ...data,
          rejectReason: reason.value.trim(),
          qaReviewerUserId: qaReviewerUserId.value,
          password: password.value
        },
        { ignoreErrorMessage: true }
      )
    else
      await withdrawEdhrRelease(
        { ...data, withdrawReason: reason.value.trim() },
        { ignoreErrorMessage: true }
      )
    if (token === generation) {
      visible.value = false
      emit('changed')
    }
  } catch (failure) {
    if (token === generation)
      error.value = failure instanceof Error ? failure.message : String(failure)
  } finally {
    password.value = ''
    submitting.value = false
  }
}
</script>
