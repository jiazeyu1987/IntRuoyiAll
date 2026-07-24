<template>
  <ContentWrap>
    <div class="portal-hero">
      <div>
        <p class="portal-hero__eyebrow">SRM Supplier Portal</p>
        <h1 class="portal-hero__title">供应商注册与资料提交</h1>
        <p class="portal-hero__desc">
          注册账号后，请完整填写企业、资质与付款资料。资料提交并审核通过后，系统会自动生成准入建档基础信息。
        </p>
      </div>
      <el-tag :type="statusTagType" effect="dark" size="large">
        {{ formData.applicationStatusLabel || '未提交' }}
      </el-tag>
    </div>

    <el-alert
      v-if="formData.auditRemark"
      class="mb-16px"
      :title="`最新审核意见：${formData.auditRemark}`"
      type="info"
      :closable="false"
    />

    <el-form ref="formRef" :model="formData" :rules="rules" label-width="132px">
      <el-row :gutter="20">
        <el-col :xs="24" :md="12">
          <el-form-item label="企业名称" prop="companyName">
            <el-input v-model="formData.companyName" placeholder="请输入企业名称" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-form-item label="统一社会信用代码" prop="unifiedSocialCreditCode">
            <el-input v-model="formData.unifiedSocialCreditCode" placeholder="请输入统一社会信用代码" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="8">
          <el-form-item label="联系人" prop="contactName">
            <el-input v-model="formData.contactName" placeholder="请输入联系人" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="8">
          <el-form-item label="联系电话" prop="contactPhone">
            <el-input v-model="formData.contactPhone" placeholder="请输入联系电话" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="8">
          <el-form-item label="联系邮箱" prop="contactEmail">
            <el-input v-model="formData.contactEmail" placeholder="请输入联系邮箱" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-form-item label="资质附件 URL" prop="qualificationAttachmentUrls">
            <el-input
              v-model="formData.qualificationAttachmentUrls"
              type="textarea"
              :rows="4"
              placeholder="请输入资质附件 URL，多个地址请换行填写"
            />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-form-item label="资质到期日" prop="qualificationExpireDate">
            <el-date-picker
              v-model="formData.qualificationExpireDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="请选择资质到期日"
              class="w-full"
            />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="8">
          <el-form-item label="开户行" prop="bankName">
            <el-input v-model="formData.bankName" placeholder="请输入开户行" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="8">
          <el-form-item label="银行账号" prop="bankAccount">
            <el-input v-model="formData.bankAccount" placeholder="请输入银行账号" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="8">
          <el-form-item label="开户地址" prop="bankAddress">
            <el-input v-model="formData.bankAddress" placeholder="请输入开户地址" />
          </el-form-item>
        </el-col>
      </el-row>

      <div class="portal-actions">
        <el-button :loading="loading" @click="handleSaveDraft">保存草稿</el-button>
        <el-button type="primary" :loading="loading" @click="handleSubmit">提交审核</el-button>
      </div>
    </el-form>
  </ContentWrap>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { ContentWrap } from '@/components/ContentWrap'
import {
  SrmSupplierPortalApi,
  type SrmSupplierPortalApplicationVO
} from '@/api/srm/supplier-portal'

defineOptions({ name: 'SrmSupplierPortalApplication' })

const formRef = ref<FormInstance>()
const loading = ref(false)
const formData = reactive<SrmSupplierPortalApplicationVO>({
  companyName: '',
  unifiedSocialCreditCode: '',
  contactName: '',
  contactPhone: '',
  contactEmail: '',
  qualificationAttachmentUrls: '',
  qualificationExpireDate: '',
  bankName: '',
  bankAccount: '',
  bankAddress: '',
  applicationStatus: 'DRAFT',
  applicationStatusLabel: '草稿'
})

const rules: FormRules = {
  companyName: [{ required: true, message: '请输入企业名称', trigger: 'blur' }],
  unifiedSocialCreditCode: [{ required: true, message: '请输入统一社会信用代码', trigger: 'blur' }],
  contactName: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  contactPhone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
  contactEmail: [
    { required: true, message: '请输入联系邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  qualificationAttachmentUrls: [{ required: true, message: '请输入资质附件 URL', trigger: 'blur' }],
  qualificationExpireDate: [{ required: true, message: '请选择资质到期日', trigger: 'change' }],
  bankName: [{ required: true, message: '请输入开户行', trigger: 'blur' }],
  bankAccount: [{ required: true, message: '请输入银行账号', trigger: 'blur' }]
}

const statusTagType = computed(() => {
  switch (formData.applicationStatus) {
    case 'APPROVED':
      return 'success'
    case 'REJECTED':
      return 'danger'
    case 'SUBMITTED':
      return 'warning'
    default:
      return 'info'
  }
})

const load = async () => {
  const data = await SrmSupplierPortalApi.getMyApplication()
  if (data) {
    Object.assign(formData, data)
  }
}

const handleSaveDraft = async () => {
  loading.value = true
  try {
    const data = await SrmSupplierPortalApi.saveDraft(formData)
    Object.assign(formData, data)
    ElMessage.success('草稿已保存')
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  loading.value = true
  try {
    const data = await SrmSupplierPortalApi.submit(formData)
    Object.assign(formData, data)
    ElMessage.success('资料已提交，等待内部审核')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped lang="scss">
.portal-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  padding: 24px;
  margin-bottom: 20px;
  border-radius: 20px;
  background:
    linear-gradient(135deg, rgba(12, 90, 166, 0.12), rgba(18, 167, 145, 0.1)),
    #fff;
}

.portal-hero__eyebrow {
  margin: 0 0 8px;
  font-size: 12px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: #0c5aa6;
}

.portal-hero__title {
  margin: 0 0 10px;
  font-size: 28px;
  line-height: 1.2;
  color: #10233a;
}

.portal-hero__desc {
  margin: 0;
  max-width: 760px;
  color: #5c6b7a;
  line-height: 1.7;
}

.portal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

@media (max-width: 768px) {
  .portal-hero {
    flex-direction: column;
  }

  .portal-actions {
    justify-content: stretch;
  }
}
</style>
