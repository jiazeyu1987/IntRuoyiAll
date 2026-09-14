<template>
  <Dialog v-model="dialogVisible" title="登记离职/转岗停用" width="520px">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="118px"
    >
      <el-form-item label="用户账号">
        <el-input :model-value="targetUserText" disabled />
      </el-form-item>
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

const open = (user: UserApi.UserVO) => {
  if (user.lifecycleDeactivatedTime) {
    message.warning('该账号已按离职/转岗单联动停用')
    return
  }
  resetForm(user)
  dialogVisible.value = true
}

defineExpose({ open })

const emit = defineEmits(['success'])
const submitForm = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate()
  if (!valid) return
  formLoading.value = true
  try {
    await UserApi.recordUserLifecycleDeactivation(formData.value)
    message.success('离职/转岗联动停用已登记')
    dialogVisible.value = false
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
