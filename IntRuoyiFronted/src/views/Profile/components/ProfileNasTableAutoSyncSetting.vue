<template>
  <el-card class="profile-nas-table-sync" shadow="never">
    <template #header>
      <div class="profile-nas-table-sync__header">
        <div>
          <div class="profile-nas-table-sync__title">NAS表格自动同步</div>
          <div class="profile-nas-table-sync__subtitle">
            统一配置每天自动同步哪些 ERP 表数据、几点开始以及写入哪个 NAS 目录。
          </div>
        </div>
        <el-tag v-if="form.jobId" type="info">Job {{ form.jobId }}</el-tag>
      </div>
    </template>

    <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon />

    <el-form class="profile-nas-table-sync__form" label-width="120px" :disabled="loading">
      <el-form-item label="启用自动同步">
        <el-switch v-model="form.enabled" />
      </el-form-item>
      <el-form-item label="每日开始时间">
        <el-time-picker v-model="form.dailyStartTime" value-format="HH:mm:ss" format="HH:mm:ss" />
      </el-form-item>
      <el-form-item label="ERP 表数据">
        <el-checkbox-group v-model="selectedSyncTypes" class="profile-nas-table-sync__checks">
          <el-checkbox v-for="item in syncTypes" :key="item.syncType" :label="item.syncType">
            {{ item.label }}
          </el-checkbox>
        </el-checkbox-group>
      </el-form-item>
      <el-form-item label="NAS 目录">
        <el-input v-model="form.nasDirectory" clearable />
      </el-form-item>
      <el-form-item label="文件名规则">
        <el-input v-model="form.fileNamePattern" clearable />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="saving" @click="handleSave">保存配置</el-button>
        <el-button :loading="testing" @click="handleTestNasWrite">测试NAS写入</el-button>
        <el-button type="success" :loading="running" @click="handleRunOnce">立即执行一次</el-button>
      </el-form-item>
    </el-form>

    <el-divider />

    <div class="profile-nas-table-sync__log-title">最近执行日志</div>
    <el-table v-loading="runLoading" :data="runs" class="profile-nas-table-sync__table" border>
      <el-table-column prop="id" label="运行编号" width="100" />
      <el-table-column prop="triggerType" label="触发" width="90" />
      <el-table-column prop="status" label="状态" width="100" />
      <el-table-column prop="startedAt" label="开始时间" min-width="160" />
      <el-table-column prop="outputPath" label="NAS 输出路径" min-width="220" show-overflow-tooltip />
      <el-table-column label="表格" min-width="120">
        <template #default="{ row }">
          {{ row.successTableCount || 0 }}/{{ row.totalTableCount || 0 }}
        </template>
      </el-table-column>
      <el-table-column prop="failureMessage" label="失败原因" min-width="220" show-overflow-tooltip />
    </el-table>
  </el-card>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import {
  ErpNasTableSyncApi,
  type ErpNasTableSyncPlanVO,
  type ErpNasTableSyncRunVO,
  type ErpNasTableSyncTypeVO
} from '@/api/erp/nasTableSync'

const loading = ref(false)
const saving = ref(false)
const testing = ref(false)
const running = ref(false)
const runLoading = ref(false)
const loadError = ref('')
const syncTypes = ref<ErpNasTableSyncTypeVO[]>([])
const runs = ref<ErpNasTableSyncRunVO[]>([])
const selectedSyncTypes = ref<string[]>([])
const form = reactive<ErpNasTableSyncPlanVO>({
  enabled: false,
  dailyStartTime: '02:00:00',
  nasDirectory: '',
  fileNamePattern: 'ERP_NAS_TABLE_SYNC_{yyyyMMdd_HHmmss}.xlsx',
  items: []
})

const resolveErrorMessage = (error: unknown, defaultMessage: string) => {
  if (error instanceof Error && error.message.trim()) return error.message
  const responseMessage = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  return typeof responseMessage === 'string' && responseMessage.trim() ? responseMessage : defaultMessage
}

const applyPlan = (plan: ErpNasTableSyncPlanVO) => {
  form.id = plan.id
  form.enabled = plan.enabled === true
  form.dailyStartTime = plan.dailyStartTime || '02:00:00'
  form.cronExpression = plan.cronExpression
  form.nasDirectory = plan.nasDirectory || ''
  form.fileNamePattern = plan.fileNamePattern || 'ERP_NAS_TABLE_SYNC_{yyyyMMdd_HHmmss}.xlsx'
  form.jobId = plan.jobId
  form.lastRunId = plan.lastRunId
  form.lastStatus = plan.lastStatus
  form.items = plan.items || []
  selectedSyncTypes.value = form.items.filter((item) => item.enabled).map((item) => item.syncType)
}

const loadRuns = async () => {
  runLoading.value = true
  try {
    const page = await ErpNasTableSyncApi.getRunPage({ pageNo: 1, pageSize: 10 })
    runs.value = page.list || []
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '最近执行日志加载失败'))
  } finally {
    runLoading.value = false
  }
}

const loadData = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const [typeList, plan] = await Promise.all([
      ErpNasTableSyncApi.getSyncTypes(),
      ErpNasTableSyncApi.getPlan()
    ])
    syncTypes.value = typeList
    applyPlan(plan)
    await loadRuns()
  } catch (error) {
    loadError.value = resolveErrorMessage(error, 'NAS表格自动同步配置加载失败')
    ElMessage.error(loadError.value)
  } finally {
    loading.value = false
  }
}

const buildSavePayload = () => ({
  enabled: form.enabled,
  dailyStartTime: form.dailyStartTime,
  nasDirectory: form.nasDirectory,
  fileNamePattern: form.fileNamePattern,
  items: syncTypes.value.map((item, index) => ({
    syncType: item.syncType,
    enabled: selectedSyncTypes.value.includes(item.syncType),
    sortOrder: index * 10,
    sheetName: item.defaultSheetName
  }))
})

const handleSave = async () => {
  saving.value = true
  try {
    const plan = await ErpNasTableSyncApi.savePlan(buildSavePayload())
    applyPlan(plan)
    ElMessage.success('NAS表格自动同步配置已保存')
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, 'NAS表格自动同步配置保存失败'))
  } finally {
    saving.value = false
  }
}

const handleTestNasWrite = async () => {
  testing.value = true
  try {
    const result = await ErpNasTableSyncApi.testNasWrite(form.nasDirectory)
    ElMessage.success(`测试NAS写入成功：${result.outputPath}`)
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '测试NAS写入失败'))
  } finally {
    testing.value = false
  }
}

const handleRunOnce = async () => {
  running.value = true
  try {
    const result = await ErpNasTableSyncApi.runOnce()
    if (result.status === 'FAILED') {
      ElMessage.error(result.failureMessage || '立即执行一次失败')
    } else {
      ElMessage.success('立即执行一次已完成')
    }
    await loadRuns()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '立即执行一次失败'))
  } finally {
    running.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.profile-nas-table-sync {
  max-width: 1080px;
}

.profile-nas-table-sync__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.profile-nas-table-sync__title {
  color: #1f2d3d;
  font-size: 16px;
  font-weight: 600;
}

.profile-nas-table-sync__subtitle {
  margin-top: 4px;
  color: #6b778c;
  font-size: 13px;
}

.profile-nas-table-sync__form {
  margin-top: 16px;
}

.profile-nas-table-sync__checks {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 18px;
}

.profile-nas-table-sync__log-title {
  margin-bottom: 12px;
  color: #1f2d3d;
  font-weight: 600;
}

.profile-nas-table-sync__table {
  width: 100%;
}
</style>