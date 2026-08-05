<template>
  <el-card class="profile-erp-table-sync" shadow="never">
    <template #header>
      <div class="profile-erp-table-sync__header">
        <div>
          <div class="profile-erp-table-sync__title">ERP表格自动同步</div>
          <div class="profile-erp-table-sync__subtitle">
            统一配置每天自动拉取哪些 ERP 表格、几点开始；支持 PRODUCT、BOM 等正式类型。
          </div>
        </div>
        <el-tag v-if="form.jobId" type="info">Job {{ form.jobId }}</el-tag>
      </div>
    </template>

    <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon />

    <el-form class="profile-erp-table-sync__form" label-width="120px" :disabled="loading">
      <el-form-item label="启用自动同步">
        <el-switch v-model="form.enabled" />
      </el-form-item>
      <el-form-item label="每日开始时间">
        <el-time-picker v-model="form.dailyStartTime" value-format="HH:mm:ss" format="HH:mm:ss" />
      </el-form-item>
      <el-form-item label="ERP 表格">
        <el-checkbox-group v-model="selectedSyncTypes" class="profile-erp-table-sync__checks">
          <el-checkbox v-for="item in syncTypes" :key="item.syncType" :label="item.syncType">
            {{ item.label }}
            <span class="profile-erp-table-sync__handler">{{ item.handlerName }}</span>
          </el-checkbox>
        </el-checkbox-group>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="saving" @click="handleSave">保存配置</el-button>
        <el-button type="success" :loading="running" @click="handleRunOnce">立即执行一次</el-button>
      </el-form-item>
    </el-form>

    <el-descriptions class="profile-erp-table-sync__summary" :column="3" border>
      <el-descriptions-item label="最近状态">{{ form.lastStatus || '未执行' }}</el-descriptions-item>
      <el-descriptions-item label="最近自动日期">{{ form.lastAutoRunDate || '-' }}</el-descriptions-item>
      <el-descriptions-item label="最近信息">{{ form.lastMessage || '-' }}</el-descriptions-item>
    </el-descriptions>

    <el-divider />

    <div class="profile-erp-table-sync__section-title">同步水位</div>
    <el-table v-loading="watermarkLoading" :data="watermarks" class="profile-erp-table-sync__table" border>
      <el-table-column prop="syncType" label="同步类型" width="180" />
      <el-table-column label="表格名称" min-width="160">
        <template #default="{ row }">{{ resolveTypeLabel(row.syncType) }}</template>
      </el-table-column>
      <el-table-column prop="lastSuccessTime" label="最近成功水位" min-width="180" />
    </el-table>

    <div class="profile-erp-table-sync__section-title profile-erp-table-sync__section-title--runs">
      最近执行记录
    </div>
    <el-table v-loading="runLoading" :data="runs" class="profile-erp-table-sync__table" border>
      <el-table-column prop="id" label="运行编号" width="100" />
      <el-table-column prop="syncType" label="同步类型" width="180" />
      <el-table-column label="表格名称" min-width="160">
        <template #default="{ row }">{{ resolveTypeLabel(row.syncType) }}</template>
      </el-table-column>
      <el-table-column prop="triggerType" label="触发" width="90" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">{{ formatRunStatus(row.status) }}</template>
      </el-table-column>
      <el-table-column prop="startedAt" label="开始时间" min-width="160" />
      <el-table-column label="数量" min-width="160">
        <template #default="{ row }">
          新增 {{ row.createdCount || 0 }} / 更新 {{ row.updatedCount || 0 }} / 跳过
          {{ row.skippedCount || 0 }}
        </template>
      </el-table-column>
      <el-table-column prop="failureMessage" label="failureMessage" min-width="220" show-overflow-tooltip />
    </el-table>
  </el-card>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import {
  ErpKingdeeTableAutoSyncApi,
  type ErpKingdeeTableAutoSyncPlanVO,
  type ErpKingdeeTableAutoSyncRunVO,
  type ErpKingdeeTableAutoSyncTypeVO,
  type ErpKingdeeTableAutoSyncWatermarkVO
} from '@/api/erp/kingdeeTableAutoSync'

const loading = ref(false)
const saving = ref(false)
const running = ref(false)
const runLoading = ref(false)
const watermarkLoading = ref(false)
const loadError = ref('')
const syncTypes = ref<ErpKingdeeTableAutoSyncTypeVO[]>([])
const runs = ref<ErpKingdeeTableAutoSyncRunVO[]>([])
const watermarks = ref<ErpKingdeeTableAutoSyncWatermarkVO[]>([])
const selectedSyncTypes = ref<string[]>([])
const form = reactive<ErpKingdeeTableAutoSyncPlanVO>({
  enabled: false,
  dailyStartTime: '02:00:00',
  items: []
})

const resolveErrorMessage = (error: unknown, defaultMessage: string) => {
  if (error instanceof Error && error.message.trim()) return error.message
  const responseMessage = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  return typeof responseMessage === 'string' && responseMessage.trim() ? responseMessage : defaultMessage
}

const resolveTypeLabel = (syncType: string) =>
  syncTypes.value.find((item) => item.syncType === syncType)?.label || syncType

const formatRunStatus = (status: number | string) => {
  if (String(status) === '1') return '成功'
  if (String(status) === '2') return '失败'
  return String(status)
}

const applyPlan = (plan: ErpKingdeeTableAutoSyncPlanVO) => {
  form.id = plan.id
  form.enabled = plan.enabled === true
  form.dailyStartTime = plan.dailyStartTime || '02:00:00'
  form.cronExpression = plan.cronExpression
  form.jobId = plan.jobId
  form.lastAutoRunDate = plan.lastAutoRunDate
  form.lastRunTime = plan.lastRunTime
  form.lastStatus = plan.lastStatus
  form.lastMessage = plan.lastMessage
  form.items = plan.items || []
  selectedSyncTypes.value = form.items.filter((item) => item.enabled).map((item) => item.syncType)
}

const loadRuns = async () => {
  runLoading.value = true
  try {
    const page = await ErpKingdeeTableAutoSyncApi.getRunPage({ pageNo: 1, pageSize: 10 })
    runs.value = page.list || []
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '最近执行记录加载失败'))
  } finally {
    runLoading.value = false
  }
}

const loadWatermarks = async () => {
  watermarkLoading.value = true
  try {
    watermarks.value = await ErpKingdeeTableAutoSyncApi.getWatermarkList()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '同步水位加载失败'))
  } finally {
    watermarkLoading.value = false
  }
}

const loadData = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const [typeList, plan] = await Promise.all([
      ErpKingdeeTableAutoSyncApi.getSyncTypes(),
      ErpKingdeeTableAutoSyncApi.getPlan()
    ])
    syncTypes.value = typeList
    applyPlan(plan)
    await Promise.all([loadRuns(), loadWatermarks()])
  } catch (error) {
    loadError.value = resolveErrorMessage(error, 'ERP表格自动同步配置加载失败')
    ElMessage.error(loadError.value)
  } finally {
    loading.value = false
  }
}

const buildSavePayload = () => ({
  enabled: form.enabled,
  dailyStartTime: form.dailyStartTime,
  items: syncTypes.value.map((item, index) => ({
    syncType: item.syncType,
    enabled: selectedSyncTypes.value.includes(item.syncType),
    sortOrder: index * 10
  }))
})

const handleSave = async () => {
  saving.value = true
  try {
    const plan = await ErpKingdeeTableAutoSyncApi.savePlan(buildSavePayload())
    applyPlan(plan)
    ElMessage.success('ERP表格自动同步配置已保存')
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, 'ERP表格自动同步配置保存失败'))
  } finally {
    saving.value = false
  }
}

const handleRunOnce = async () => {
  running.value = true
  try {
    const result = await ErpKingdeeTableAutoSyncApi.runOnce()
    ElMessage.success(`立即执行一次已完成：${result.successSyncCount}/${result.totalSyncCount}`)
    await Promise.all([loadRuns(), loadWatermarks(), ErpKingdeeTableAutoSyncApi.getPlan().then(applyPlan)])
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '立即执行一次失败'))
  } finally {
    running.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.profile-erp-table-sync {
  max-width: 1080px;
}

.profile-erp-table-sync__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.profile-erp-table-sync__title {
  color: #1f2d3d;
  font-size: 16px;
  font-weight: 600;
}

.profile-erp-table-sync__subtitle {
  margin-top: 4px;
  color: #6b778c;
  font-size: 13px;
}

.profile-erp-table-sync__form {
  margin-top: 16px;
}

.profile-erp-table-sync__checks {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 18px;
}

.profile-erp-table-sync__handler {
  margin-left: 6px;
  color: #8a95a8;
  font-size: 12px;
}

.profile-erp-table-sync__summary {
  margin-top: 12px;
}

.profile-erp-table-sync__section-title {
  margin-bottom: 12px;
  color: #1f2d3d;
  font-weight: 600;
}

.profile-erp-table-sync__section-title--runs {
  margin-top: 18px;
}

.profile-erp-table-sync__table {
  width: 100%;
}
</style>
