<template>
  <el-form ref="formRef" :model="password" :rules="rules" :label-width="200">
    <el-form-item :label="t('profile.password.oldPassword')" prop="oldPassword">
      <InputPassword v-model="password.oldPassword" :disabled="submitting" />
    </el-form-item>
    <el-form-item :label="t('profile.password.newPassword')" prop="newPassword">
      <InputPassword v-model="password.newPassword" :disabled="submitting" strength />
    </el-form-item>
    <el-form-item :label="t('profile.password.confirmPassword')" prop="confirmPassword">
      <InputPassword v-model="password.confirmPassword" :disabled="submitting" strength />
    </el-form-item>
    <el-form-item>
      <XButton
        :title="t('common.save')"
        type="primary"
        :loading="submitting"
        :disabled="submitting"
        @click="submit(formRef)"
      />
      <XButton
        :title="t('common.reset')"
        type="danger"
        :disabled="submitting"
        @click="reset(formRef)"
      />
    </el-form-item>
  </el-form>
</template>
<script lang="ts" setup>
import type { FormInstance, FormRules } from 'element-plus'

import { InputPassword } from '@/components/InputPassword'
import { updateUserPassword } from '@/api/system/user/profile'
import { systemPasswordRule } from '@/views/system/user/systemPasswordPolicy'
import { isNavigationFailure } from 'vue-router'
import { useUserStore } from '@/store/modules/user'

defineOptions({ name: 'ResetPwd' })

const { t } = useI18n()
const message = useMessage()
const userStore = useUserStore()
const { push } = useRouter()
const formRef = ref<FormInstance>()
const submitting = ref(false)
const password = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 表单校验
const equalToPassword = (_rule, value, callback) => {
  if (password.newPassword !== value) {
    callback(new Error(t('profile.password.diffPwd')))
  } else {
    callback()
  }
}

const rules = reactive<FormRules>({
  oldPassword: [{ required: true, message: t('profile.password.oldPwdMsg'), trigger: 'blur' }],
  newPassword: [
    { required: true, message: t('profile.password.newPwdMsg'), trigger: 'blur' },
    systemPasswordRule
  ],
  confirmPassword: [
    { required: true, message: t('profile.password.cfPwdMsg'), trigger: 'blur' },
    { required: true, validator: equalToPassword, trigger: 'blur' }
  ]
})

const submit = async (formEl: FormInstance | undefined) => {
  if (!formEl || submitting.value) return
  submitting.value = true
  try {
    await formEl.validate(async (valid) => {
      if (valid) {
        try {
          await updateUserPassword(password.oldPassword, password.newPassword)
        } catch {
          message.error('密码修改失败，请检查输入后重试')
          return
        }
        message.success('密码已修改，请重新登录')
        userStore.clearSession()
        try {
          const failure = await push({ path: '/login' })
          if (isNavigationFailure(failure)) {
            message.error('密码已修改，请重新登录')
          }
        } catch {
          message.error('密码已修改，请重新登录')
        }
      }
    })
  } finally {
    submitting.value = false
  }
}

const reset = (formEl: FormInstance | undefined) => {
  if (!formEl || submitting.value) return
  formEl.resetFields()
}
</script>
