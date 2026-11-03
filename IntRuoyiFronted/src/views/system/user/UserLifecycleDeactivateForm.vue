<template>
  <Dialog v-model="dialogVisible" title="登记离职/转岗停用" width="520px">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :disabled="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="118px"
    >
      <el-form-item label="用户账号">
        <el-input :model-value="targetUserText" disabled />
      </el-form-item>
      <el-alert :title="supervisorImpact" type="warning" :closable="false" class="mb-16px" />
      <el-form-item label="单据类型" prop="documentType">
        <el-radio-group v-model="formData.documentType">
          <el-radio-button label="RESIGNATION">离职单</el-radio-button>
          <el-radio-button label="TRANSFER">转岗单</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="离职/转岗单号" prop="documentNo">
        <el-input v-model="formData.documentNo" maxlength="64" placeholder="请输入离职/转岗单号" />
      </el-form-item>
      <el-form-item label="单据时间" prop="documentTime">
        <el-date-picker
          v-model="formData.documentTime"
          type="datetime"
          value-format="YYYY-MM-DDTHH:mm:ss"
          format="YYYY-MM-DD HH:mm:ss"
          placeholder="请选择单据时间"
          class="system-user-lifecycle-date"
        />
      </el-form-item>
      <el-form-item label="生效时间" prop="effectiveTime">
        <el-date-picker
          v-model="formData.effectiveTime"
          type="datetime"
          value-format="YYYY-MM-DDTHH:mm:ss"
          format="YYYY-MM-DD HH:mm:ss"
          placeholder="请选择账号停用生效时间"
          class="system-user-lifecycle-date"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>

<script lang="ts" setup>
import { FormRules } from 'element-plus'
import * as UserApi from '@/api/system/user'

defineOptions({ name: 'SystemUserLifecycleDeactivateForm' })

const message = useMessage()

const dialogVisible = ref(false)
const formLoading = ref(false)
const formRef = ref()
const targetUserText = ref('')
const supervisorImpact = ref('')
const targetUser = ref<UserApi.UserVO>()
const formData = ref<UserApi.UserLifecycleDeactivateReqVO>({
  id: 0,
  documentType: 'RESIGNATION',
  documentNo: '',
  documentTime: '',
  effectiveTime: ''
})

const formRules = reactive<FormRules>({
  documentType: [{ required: true, message: '单据类型不能为空', trigger: 'change' }],
  documentNo: [
    { required: true, message: '离职/转岗单号不能为空', trigger: 'blur' },
    { max: 64, message: '离职/转岗单号不能超过 64 个字符', trigger: 'blur' }
  ],
  documentTime: [{ required: true, message: '单据时间不能为空', trigger: 'change' }],
  effectiveTime: [{ required: true, message: '生效时间不能为空', trigger: 'change' }]
})

const resetForm = (user: UserApi.UserVO) => {
  formData.value = {
    id: user.id,
    documentType: 'RESIGNATION',
    documentNo: '',
    documentTime: '',
    effectiveTime: ''
  }
  targetUserText.value = user.nickname ? `${user.username}（${user.nickname}）` : user.username
  formRef.value?.clearValidate()
}

const open = async (user: UserApi.UserVO) => {
  if (formLoading.value) return
  if (user.lifecycleDeactivatedTime) {
    message.warning('该账号已按离职/转岗单联动停用')
    return
  }
  const affected = await UserApi.getSupervisorImpact([user.id])
  resetForm(user)
  targetUser.value = user
  supervisorImpact.value = affected.length
    ? '到生效时间才解除主管配置：' +
      affected.map((dept) => dept.name).join('、') +
      '。登记后将自动打开现有待办交接确认。'
    : '到生效时间停用账号。登记后将自动打开现有待办交接确认。'
  dialogVisible.value = true
}

defineExpose({ open })

const emit = defineEmits(['success', 'handover'])
const submitForm = async () => {
  if (!formRef.value || formLoading.value) return
  formLoading.value = true
  const target = targetUser.value
  try {
    const valid = await formRef.value.validate()
    if (!valid || !dialogVisible.value || targetUser.value !== target) return
    const payload = { ...formData.value }
    try {
      await message.confirm(
        `目标账号：${targetUserText.value}\n单据类型：${payload.documentType === 'RESIGNATION' ? '离职单' : '转岗单'}\n单号：${payload.documentNo}\n生效时间：${payload.effectiveTime.replace('T', ' ')}\n到期立即或由定时任务停用账号并解除主管配置；启用账号停用时现有登录会话失效。\n${supervisorImpact.value}\n待办不会自动转交，登记成功后另行确认交接；取消交接不会撤销本次登记。`,
        '确认登记离职/转岗停用'
      )
    } catch (cause) {
      if (cause === 'cancel' || cause === 'close') return
      throw cause
    }
    if (!dialogVisible.value) return
    await UserApi.recordUserLifecycleDeactivation(payload)
    message.success('离职/转岗联动停用已登记')
    dialogVisible.value = false
    emit('handover', target)
    emit('success')
  } finally {
    formLoading.value = false
  }
}
</script>

<style scoped>
.system-user-lifecycle-date {
  width: 100%;
}
</style>
