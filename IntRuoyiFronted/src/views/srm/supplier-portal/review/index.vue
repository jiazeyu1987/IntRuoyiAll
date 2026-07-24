<template>
  <ContentWrap>
    <div class="review-hero">
      <div>
        <p class="review-hero__eyebrow">SRM Portal Review</p>
        <h1 class="review-hero__title">供应商门户审核台</h1>
        <p class="review-hero__desc">
          审核通过后将自动创建或更新 ERP 供应商主档，并同步生成准入基础档案；未通过审核的供应商不得进入准入链路。
        </p>
      </div>
      <div class="review-hero__badge">
        <span class="review-hero__badge-label">待审核</span>
        <strong>{{ pendingCount }}</strong>
      </div>
    </div>

    <el-form :inline="true" :model="queryParams" class="mb-12px">
      <el-form-item label="企业名称">
        <el-input v-model="queryParams.companyName" placeholder="请输入企业名称" clearable @keyup.enter="getList" />
      </el-form-item>
      <el-form-item label="联系人">
        <el-input v-model="queryParams.contactName" placeholder="请输入联系人" clearable @keyup.enter="getList" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.applicationStatus" placeholder="全部状态" clearable style="width: 160px">
          <el-option
            v-for="item in srmSupplierPortalStatusOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="getList">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="list" border>
      <el-table-column label="企业名称" prop="companyName" min-width="220" />
      <el-table-column label="统一社会信用代码" prop="unifiedSocialCreditCode" min-width="180" />
      <el-table-column label="联系人" prop="contactName" width="110" />
      <el-table-column label="联系电话" prop="contactPhone" width="130" />
      <el-table-column label="联系邮箱" prop="contactEmail" min-width="180" />
      <el-table-column label="资质到期日" prop="qualificationExpireDate" width="120" />
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.applicationStatus)">{{ row.applicationStatusLabel }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="提交人" prop="submitterName" width="120" />
      <el-table-column label="提交时间" prop="submittedTime" width="168" />
      <el-table-column label="审核意见" prop="auditRemark" min-width="220" show-overflow-tooltip />
      <el-table-column label="操作" fixed="right" width="220">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          <el-button
            v-if="row.applicationStatus === 'SUBMITTED'"
            link
            type="success"
            v-hasPermi="['srm:supplier-portal:audit']"
            @click="openAudit(row, 'approve')"
          >
            通过
          </el-button>
          <el-button
            v-if="row.applicationStatus === 'SUBMITTED'"
            link
            type="danger"
            v-hasPermi="['srm:supplier-portal:audit']"
            @click="openAudit(row, 'reject')"
          >
            驳回
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <Dialog v-model="detailVisible" title="门户申请详情" width="860px">
      <template v-if="activeRow">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="企业名称">{{ activeRow.companyName }}</el-descriptions-item>
          <el-descriptions-item label="统一社会信用代码">{{ activeRow.unifiedSocialCreditCode }}</el-descriptions-item>
          <el-descriptions-item label="联系人">{{ activeRow.contactName }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ activeRow.contactPhone }}</el-descriptions-item>
          <el-descriptions-item label="联系邮箱">{{ activeRow.contactEmail }}</el-descriptions-item>
          <el-descriptions-item label="资质到期日">{{ activeRow.qualificationExpireDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="开户行">{{ activeRow.bankName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="银行账号">{{ activeRow.bankAccount || '-' }}</el-descriptions-item>
          <el-descriptions-item label="开户地址" :span="2">{{ activeRow.bankAddress || '-' }}</el-descriptions-item>
          <el-descriptions-item label="资质附件 URL" :span="2">
            <pre class="detail-pre">{{ activeRow.qualificationAttachmentUrls || '-' }}</pre>
          </el-descriptions-item>
          <el-descriptions-item label="状态">{{ activeRow.applicationStatusLabel || '-' }}</el-descriptions-item>
          <el-descriptions-item label="审核意见">{{ activeRow.auditRemark || '-' }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </Dialog>

    <Dialog v-model="auditVisible" :title="auditMode === 'approve' ? '审核通过' : '审核驳回'" width="520px">
      <el-form ref="auditFormRef" :model="auditForm" :rules="auditRules" label-width="88px">
        <el-form-item label="审核意见" prop="auditRemark">
          <el-input
            v-model="auditForm.auditRemark"
            type="textarea"
            :rows="4"
            :placeholder="auditMode === 'approve' ? '可填写通过说明' : '请输入驳回原因'"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditVisible = false">取消</el-button>
        <el-button type="primary" :loading="auditLoading" @click="submitAudit">确认</el-button>
      </template>
    </Dialog>
  </ContentWrap>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { ContentWrap } from '@/components/ContentWrap'
import { Dialog } from '@/components/Dialog'
import {
  SrmSupplierPortalApi,
  srmSupplierPortalStatusOptions,
  type SrmSupplierPortalApplicationAuditReqVO,
  type SrmSupplierPortalApplicationPageReqVO,
  type SrmSupplierPortalApplicationVO
} from '@/api/srm/supplier-portal'

defineOptions({ name: 'SrmSupplierPortalReview' })

const route = useRoute()
const loading = ref(false)
const total = ref(0)
const list = ref<SrmSupplierPortalApplicationVO[]>([])
const detailVisible = ref(false)
const auditVisible = ref(false)
const auditLoading = ref(false)
const activeRow = ref<SrmSupplierPortalApplicationVO>()
const auditMode = ref<'approve' | 'reject'>('approve')
const auditFormRef = ref<FormInstance>()
const openedRouteApplicationId = ref<number>()

const queryParams = reactive<SrmSupplierPortalApplicationPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  id: undefined,
  companyName: '',
  contactName: '',
  applicationStatus: ''
})

const auditForm = reactive<SrmSupplierPortalApplicationAuditReqVO>({
  id: 0,
  auditRemark: ''
})

const auditRules = computed<FormRules>(() => ({
  auditRemark:
    auditMode.value === 'reject'
      ? [{ required: true, message: '请输入驳回原因', trigger: 'blur' }]
      : []
}))

const pendingCount = computed(
  () => list.value.filter((item) => item.applicationStatus === 'SUBMITTED').length
)

const routeApplicationId = computed(() => {
  const rawValue = Array.isArray(route.query.applicationId)
    ? route.query.applicationId[0]
    : route.query.applicationId
  if (typeof rawValue !== 'string' || rawValue.trim() === '') {
    return undefined
  }
  const applicationId = Number(rawValue)
  return Number.isSafeInteger(applicationId) && applicationId > 0 ? applicationId : undefined
})

const statusTagType = (status?: string) => {
  switch (status) {
    case 'APPROVED':
      return 'success'
    case 'REJECTED':
      return 'danger'
    case 'SUBMITTED':
      return 'warning'
    default:
      return 'info'
  }
}

const getList = async () => {
  loading.value = true
  try {
    applyRouteApplicationId()
    const data = await SrmSupplierPortalApi.getApplicationPage(queryParams)
    list.value = data.list
    total.value = data.total
    openRouteApplicationDetail()
  } finally {
    loading.value = false
  }
}

const applyRouteApplicationId = () => {
  const applicationId = routeApplicationId.value
  if (!applicationId) {
    return
  }
  queryParams.id = applicationId
  queryParams.pageNo = 1
}

const openRouteApplicationDetail = () => {
  const applicationId = routeApplicationId.value
  if (!applicationId || openedRouteApplicationId.value === applicationId) {
    return
  }
  const matchedApplication = list.value.find((item) => item.id === applicationId)
  if (!matchedApplication) {
    ElMessage.error(`未找到统一审批中心指定的 SRM 门户申请：${applicationId}`)
    return
  }
  openedRouteApplicationId.value = applicationId
  openDetail(matchedApplication)
}

const resetQuery = () => {
  queryParams.pageNo = 1
  queryParams.id = undefined
  queryParams.companyName = ''
  queryParams.contactName = ''
  queryParams.applicationStatus = ''
  getList()
}

const openDetail = (row: SrmSupplierPortalApplicationVO) => {
  activeRow.value = row
  detailVisible.value = true
}

const openAudit = (row: SrmSupplierPortalApplicationVO, mode: 'approve' | 'reject') => {
  activeRow.value = row
  auditMode.value = mode
  auditForm.id = row.id || 0
  auditForm.auditRemark = mode === 'approve' ? '资料审核通过' : ''
  auditVisible.value = true
}

const submitAudit = async () => {
  const valid = await auditFormRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  auditLoading.value = true
  try {
    if (auditMode.value === 'approve') {
      await SrmSupplierPortalApi.approve(auditForm)
      ElMessage.success('已审核通过')
    } else {
      await SrmSupplierPortalApi.reject(auditForm)
      ElMessage.success('已驳回申请')
    }
    auditVisible.value = false
    await getList()
  } finally {
    auditLoading.value = false
  }
}

onMounted(getList)
</script>

<style scoped lang="scss">
.review-hero {
  display: flex;
  justify-content: space-between;
  gap: 24px;
  padding: 24px;
  margin-bottom: 20px;
  border-radius: 20px;
  background:
    radial-gradient(circle at top left, rgba(13, 110, 163, 0.18), transparent 40%),
    linear-gradient(135deg, rgba(16, 35, 58, 0.04), rgba(6, 146, 104, 0.08)),
    #fff;
}

.review-hero__eyebrow {
  margin: 0 0 8px;
  font-size: 12px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: #0d6ea3;
}

.review-hero__title {
  margin: 0 0 12px;
  color: #10233a;
  font-size: 30px;
  line-height: 1.2;
}

.review-hero__desc {
  margin: 0;
  max-width: 760px;
  color: #556576;
  line-height: 1.7;
}

.review-hero__badge {
  min-width: 120px;
  padding: 18px 20px;
  border-radius: 18px;
  background: #10233a;
  color: #fff;
  text-align: center;
}

.review-hero__badge-label {
  display: block;
  margin-bottom: 6px;
  font-size: 12px;
  letter-spacing: 0.1em;
  color: rgba(255, 255, 255, 0.7);
}

.review-hero__badge strong {
  font-size: 30px;
}

.detail-pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  font-family: inherit;
}

@media (max-width: 768px) {
  .review-hero {
    flex-direction: column;
  }
}
</style>
