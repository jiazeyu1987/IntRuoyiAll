<template>
  <ContentWrap>
    <el-form :inline="true" :model="queryFormData" label-width="88px">
      <el-form-item label="ERP供应商">
        <el-select
          v-model="queryFormData.supplierId"
          filterable
          remote
          reserve-keyword
          class="!w-360px"
          placeholder="请输入供应商名称检索"
          :remote-method="handleSupplierSearch"
          :loading="supplierOptionsLoading"
        >
          <el-option
            v-for="item in supplierOptions"
            :key="item.id"
            :label="`${item.name} (#${item.id})`"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="loadProfile()">
          <Icon icon="ep:search" class="mr-5px" /> 查询档案
        </el-button>
        <el-button @click="resetProfile">
          <Icon icon="ep:refresh" class="mr-5px" /> 重置
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap v-if="profile">
    <div class="profile-hero">
      <div>
        <div class="profile-title">{{ profile.supplierName || '未命名供应商' }}</div>
        <div class="profile-subtitle">ERP 供应商编号 #{{ profile.supplierId }}</div>
      </div>
      <div class="profile-tags">
        <el-tag :type="resolveAccessTagType(profile.accessStatus)" size="large">
          {{ profile.accessStatusLabel || '未建档' }}
        </el-tag>
        <el-tag :type="resolveQualificationTagType(profile.qualificationStatusLabel)" size="large">
          {{ profile.qualificationStatusLabel || '未登记' }}
        </el-tag>
        <el-tag :type="resolveStageTagType(profile.trialOrderStatus)" size="large">
          {{ profile.onboardingStageSummary || '待建档' }}
        </el-tag>
      </div>
    </div>

    <div class="profile-grid">
      <div class="profile-card">
        <span class="profile-card__label">门户联系人</span>
        <strong class="profile-card__value">{{ profile.portalContactName || '-' }}</strong>
      </div>
      <div class="profile-card">
        <span class="profile-card__label">联系电话</span>
        <strong class="profile-card__value">{{ profile.portalContactPhone || '-' }}</strong>
      </div>
      <div class="profile-card">
        <span class="profile-card__label">准入概览</span>
        <strong class="profile-card__value">{{ profile.eligibilitySummary || '-' }}</strong>
      </div>
      <div class="profile-card profile-card--alert">
        <span class="profile-card__label">未处理高风险</span>
        <strong class="profile-card__value">{{ profile.openHighRiskCount || 0 }}</strong>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :xs="24" :lg="12">
        <div class="section-card">
          <div class="section-title">准入与资质</div>
          <div class="kv-list">
            <div class="kv-item"><span>资质到期日</span><strong>{{ profile.qualificationExpireDate || '-' }}</strong></div>
            <div class="kv-item"><span>启停状态</span><strong>{{ profile.enabled === false ? '停用' : profile.enabled === true ? '启用' : '-' }}</strong></div>
            <div class="kv-item"><span>准入备注</span><strong>{{ profile.accessRemark || '-' }}</strong></div>
            <div class="kv-item"><span>提交人</span><strong>{{ profile.submittedName || '-' }}</strong></div>
            <div class="kv-item"><span>提交时间</span><strong>{{ profile.submittedTime || '-' }}</strong></div>
            <div class="kv-item"><span>最终审核</span><strong>{{ profile.auditName || '-' }}</strong></div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :lg="12">
        <div class="section-card">
          <div class="section-title">阶段留痕</div>
          <div class="stage-list">
            <div class="stage-item">
              <div class="stage-item__head">
                <span>样品测试</span>
                <el-tag :type="resolveStageTagType(profile.sampleTestStatus)">
                  {{ profile.sampleTestStatusLabel || '-' }}
                </el-tag>
              </div>
              <div class="stage-item__meta">
                {{ profile.sampleAuditName || '-' }} / {{ profile.sampleAuditTime || '-' }}
              </div>
              <div class="stage-item__remark">{{ profile.sampleAuditRemark || '暂无样品审核意见' }}</div>
            </div>
            <div class="stage-item">
              <div class="stage-item__head">
                <span>小批试用</span>
                <el-tag :type="resolveStageTagType(profile.trialOrderStatus)">
                  {{ profile.trialOrderStatusLabel || '-' }}
                </el-tag>
              </div>
              <div class="stage-item__meta">
                {{ profile.trialAuditName || '-' }} / {{ profile.trialAuditTime || '-' }}
              </div>
              <div class="stage-item__remark">{{ profile.trialAuditRemark || '暂无试用审核意见' }}</div>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>
  </ContentWrap>

  <ContentWrap v-if="profile">
    <div class="section-title mb-12px">风险记录</div>
    <el-table :data="profile.riskList || []" :stripe="true" :show-overflow-tooltip="true" row-key="id">
      <el-table-column label="风险等级" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="resolveRiskLevelType(row.riskLevel)">{{ row.riskLevelLabel }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="处理状态" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="row.riskStatus === 'RESOLVED' ? 'success' : 'danger'">
            {{ row.riskStatusLabel }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="来源类型" prop="sourceTypeLabel" width="120" />
      <el-table-column label="来源编码" prop="sourceCode" min-width="140" />
      <el-table-column label="风险描述" prop="riskDescription" min-width="220" />
      <el-table-column label="风险备注" prop="riskRemark" min-width="180" />
      <el-table-column label="处理说明" prop="resolutionRemark" min-width="180" />
    </el-table>
  </ContentWrap>

  <ContentWrap v-else>
    <el-empty description="请选择供应商后查询统一档案" />
  </ContentWrap>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import {
  SrmSupplierAccessApi,
  type SrmSupplierProfileVO,
  type SrmSupplierReferenceVO
} from '@/api/srm/supplier-access'
import { srmSupplierRiskLevelOptions } from '@/api/srm/supplier-risk'

defineOptions({ name: 'SrmSupplierProfile' })

const message = useMessage()
const route = useRoute()
const router = useRouter()

const loading = ref(false)
const profile = ref<SrmSupplierProfileVO>()
const supplierOptions = ref<SrmSupplierReferenceVO[]>([])
const supplierOptionsLoading = ref(false)
const queryFormData = reactive({
  supplierId: undefined as number | undefined
})

const resolveErrorMessage = (error: unknown, defaultMessage: string) => {
  if (error instanceof Error && error.message) {
    return error.message
  }
  return defaultMessage
}

const resolveAccessTagType = (status?: string) => {
  if (status === 'APPROVED') {
    return 'success'
  }
  if (status === 'REJECTED') {
    return 'danger'
  }
  if (!status) {
    return 'info'
  }
  return 'warning'
}

const resolveQualificationTagType = (status?: string) => {
  if (status === '有效') {
    return 'success'
  }
  if (status === '已过期') {
    return 'danger'
  }
  if (status === '待更新') {
    return 'warning'
  }
  return 'info'
}

const resolveStageTagType = (status?: string) => {
  if (status === 'PASSED') {
    return 'success'
  }
  if (status === 'REJECTED') {
    return 'danger'
  }
  if (status === 'PENDING') {
    return 'warning'
  }
  return 'info'
}

const resolveRiskLevelType = (riskLevel?: string) => {
  const matched = srmSupplierRiskLevelOptions.find((item) => item.value === riskLevel)
  if (matched?.value === 'HIGH') {
    return 'danger'
  }
  if (matched?.value === 'MEDIUM') {
    return 'warning'
  }
  return 'success'
}

const upsertSupplierOptions = (items: SrmSupplierReferenceVO[]) => {
  const next = new Map<number, SrmSupplierReferenceVO>()
  supplierOptions.value.forEach((item) => next.set(item.id, item))
  items.forEach((item) => next.set(item.id, item))
  supplierOptions.value = Array.from(next.values())
}

const loadReferenceSuppliers = async (keyword?: string) => {
  supplierOptionsLoading.value = true
  try {
    const data = await SrmSupplierAccessApi.getReferenceSuppliers(keyword)
    upsertSupplierOptions(data || [])
  } catch (error) {
    message.error(resolveErrorMessage(error, '供应商引用列表加载失败，请检查后端接口。'))
    throw error
  } finally {
    supplierOptionsLoading.value = false
  }
}

const handleSupplierSearch = (keyword: string) => {
  loadReferenceSuppliers(keyword)
}

const syncRouteQuery = () => {
  router.replace({
    query: queryFormData.supplierId ? { supplierId: String(queryFormData.supplierId) } : {}
  })
}

const loadProfile = async (silent = false) => {
  if (!queryFormData.supplierId) {
    if (!silent) {
      message.error('请选择要查看的 ERP 供应商。')
    }
    return
  }
  loading.value = true
  try {
    profile.value = await SrmSupplierAccessApi.getSupplierProfile(queryFormData.supplierId)
    syncRouteQuery()
  } catch (error) {
    message.error(resolveErrorMessage(error, '供应商统一档案加载失败，请检查后端接口。'))
    throw error
  } finally {
    loading.value = false
  }
}

const resetProfile = () => {
  queryFormData.supplierId = undefined
  profile.value = undefined
  syncRouteQuery()
}

onMounted(async () => {
  await loadReferenceSuppliers('')
  const supplierId = Number(route.query.supplierId)
  if (Number.isFinite(supplierId) && supplierId > 0) {
    queryFormData.supplierId = supplierId
    await loadProfile(true)
  }
})
</script>

<style scoped lang="scss">
.profile-hero {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.profile-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.profile-subtitle {
  margin-top: 4px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.profile-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.profile-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.profile-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px 16px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 12px;
  background: linear-gradient(135deg, #ffffff 0%, #f7f9fc 100%);
}

.profile-card--alert {
  background: linear-gradient(135deg, #fff5f5 0%, #fff0f0 100%);
}

.profile-card__label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.profile-card__value {
  font-size: 22px;
  line-height: 1.1;
  color: var(--el-text-color-primary);
}

.section-card {
  height: 100%;
  padding: 18px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 14px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
}

.section-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--el-text-color-primary);
}

.kv-list {
  display: grid;
  gap: 10px;
  margin-top: 14px;
}

.kv-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 10px;
  border-bottom: 1px dashed var(--el-border-color-lighter);
}

.kv-item span {
  color: var(--el-text-color-secondary);
}

.kv-item strong {
  color: var(--el-text-color-primary);
  text-align: right;
}

.stage-list {
  display: grid;
  gap: 12px;
  margin-top: 14px;
}

.stage-item {
  padding: 14px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px;
  background: #fff;
}

.stage-item__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.stage-item__meta {
  margin-top: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.stage-item__remark {
  margin-top: 10px;
  color: var(--el-text-color-regular);
}

@media (max-width: 992px) {
  .profile-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .profile-hero {
    flex-direction: column;
  }

  .profile-grid {
    grid-template-columns: 1fr;
  }

  .kv-item {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
