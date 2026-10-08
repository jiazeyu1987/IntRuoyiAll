<template>
  <Form ref="formRef" :labelWidth="200" :rules="rules" :schema="schema">
    <template #sex="form">
      <el-radio-group v-model="form['sex']">
        <el-radio :value="1">{{ t('profile.user.man') }}</el-radio>
        <el-radio :value="2">{{ t('profile.user.woman') }}</el-radio>
      </el-radio-group>
    </template>
  </Form>
  <div style="text-align: center">
    <XButton :title="t('common.save')" type="primary" @click="submit()" />
    <XButton :title="t('common.reset')" type="danger" @click="init()" />
  </div>
</template>
<script lang="ts" setup>
import type { FormRules } from 'element-plus'
import { FormSchema } from '@/types/form'
import type { FormExpose } from '@/components/Form'
import {
  getUserProfile,
  updateUserProfile,
  UserProfileUpdateReqVO
} from '@/api/system/user/profile'
import { useUserStore } from '@/store/modules/user'

defineOptions({ name: 'BasicInfo' })

const { t } = useI18n()
const message = useMessage() // 消息弹窗
const userStore = useUserStore()

// 定义事件
const emit = defineEmits<{
  (e: 'success'): void
}>()
const originalContact = reactive<{ mobile?: string | null; email?: string | null }>({})
const normalizeContact = (value: string | null | undefined) => value?.trim() || ''
const validateContactClear = (
  field: 'mobile' | 'email',
  value: string,
  callback: (error?: Error) => void
) => {
  if (normalizeContact(originalContact[field]) && !value) {
    callback(new Error('当前修改入口不支持清空，请保留或填写有效值'))
    return
  }
  callback()
}

// 表单校验
const rules = reactive<FormRules>({
  nickname: [{ required: true, message: t('profile.rules.nickname'), trigger: 'blur' }],
  email: [
    {
      transform: normalizeContact,
      validator: (_rule, value, callback) => validateContactClear('email', value, callback),
      trigger: ['blur', 'change']
    },
    {
      transform: normalizeContact,
      type: 'email',
      message: t('profile.rules.truemail'),
      trigger: ['blur', 'change']
    },
    { transform: normalizeContact, max: 50, message: '邮箱长度不能超过 50 个字符', trigger: ['blur', 'change'] }
  ],
  mobile: [
    {
      transform: normalizeContact,
      validator: (_rule, value, callback) => validateContactClear('mobile', value, callback),
      trigger: ['blur', 'change']
    },
    {
      transform: normalizeContact,
      pattern: /^1[3-9]\d{9}$/,
      message: t('profile.rules.truephone'),
      trigger: 'blur'
    }
  ]
})
const schema = reactive<FormSchema[]>([
  {
    field: 'nickname',
    label: t('profile.user.nickname'),
    component: 'Input'
  },
  {
    field: 'mobile',
    label: t('profile.user.mobile'),
    component: 'Input'
  },
  {
    field: 'email',
    label: t('profile.user.email'),
    component: 'Input'
  },
  {
    field: 'sex',
    label: t('profile.user.sex'),
    component: 'InputNumber',
    value: 0
  }
])
const formRef = ref<FormExpose>() // 表单 Ref

// 监听 userStore 中头像的变化，同步更新表单数据
watch(
  () => userStore.getUser.avatar,
  (newAvatar) => {
    if (newAvatar && formRef.value) {
      // 直接更新表单模型中的头像字段
      const formModel = formRef.value.formModel
      if (formModel) {
        formModel.avatar = newAvatar
      }
    }
  }
)

const submit = async () => {
  const form = unref(formRef)
  const elForm = form?.getElFormRef()
  if (!form || !elForm) return
  form.formModel.mobile = normalizeContact(form.formModel.mobile)
  form.formModel.email = normalizeContact(form.formModel.email)
  await elForm.validate(async (valid) => {
    if (!valid) return
    const model = form.formModel
    const data: UserProfileUpdateReqVO = {
      nickname: model.nickname,
      sex: model.sex,
      avatar: model.avatar
    }
    if (model.mobile) data.mobile = model.mobile
    if (model.email) data.email = model.email
    await updateUserProfile(data)
    message.success(t('common.updateSuccess'))
    const profile = await init()
    await userStore.setUserNicknameAction(profile.nickname)
    // 发送成功事件
    emit('success')
  })
}

const init = async () => {
  const res = await getUserProfile()
  originalContact.mobile = res.mobile
  originalContact.email = res.email
  unref(formRef)?.setValues(res)
  return res
}

onMounted(async () => {
  await init()
})
</script>
