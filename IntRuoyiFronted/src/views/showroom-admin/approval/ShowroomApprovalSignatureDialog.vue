<template>
  <el-dialog
    v-model="dialogVisible"
    :title="title"
    width="520px"
    destroy-on-close
  >
    <el-form label-width="96px">
      <el-form-item label="登录密码">
        <el-input
          v-model="password"
          show-password
          type="password"
          placeholder="请输入当前登录密码"
        />
      </el-form-item>
      <el-form-item :label="mode === 'reject' ? '驳回原因' : '审批意见'">
        <el-input
          v-model="approvalComment"
          type="textarea"
          :rows="4"
          :placeholder="mode === 'reject' ? '请输入驳回原因' : '可填写审批意见（选填）'"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="loading" @click="handleConfirm">确认签名</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
defineOptions({ name: 'ShowroomApprovalSignatureDialog' })

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    title: string
    mode: 'approve' | 'reject'
    loading?: boolean
  }>(),
  {
    loading: false
  }
)

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  confirm: [payload: { password: string; comment: string }]
}>()

const password = ref('')
const approvalComment = ref('')

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

watch(
  () => props.modelValue,
  (visible) => {
    if (visible) {
      password.value = ''
      approvalComment.value = ''
    }
  }
)

const handleConfirm = () => {
  emit('confirm', {
    password: password.value.trim(),
    comment: approvalComment.value.trim()
  })
}
</script>
