<template>
  <ContentWrap title="偏差管理">
    <UnifiedListTemplate
      table-key="mes.pro.edhr.deviation.main"
      :query-model="query"
      :filter-definitions="filterDefinitions"
      :show-quick-filter-label="false"
      :quick-filter-state="quickFilter.state"
      :operator-options="[]"
      :columns="listColumnState"
      :show-column-settings="false"
      :total="total"
      v-model:page="query.pageNo"
      v-model:limit="query.pageSize"
      @pagination="load"
      @update:quick-filter-state="quickFilter.updateState"
      @quick-filter-query="quickFilter.applyQuickFilter"
      @sort-change="handleSortChange"
    >
      <template #actions>
        <el-tabs v-model="activeStatus" @tab-change="handleTabChange"><el-tab-pane label="全部" name="ALL" /><el-tab-pane label="未处理" name="OPEN" /><el-tab-pane label="已处理" name="CLOSED" /></el-tabs>
        <el-button v-hasPermi="['mes:pro-edhr-deviation:create']" type="primary" @click="openCreate">发起偏差</el-button>
        <el-button v-if="selectedCritical.length > 0" v-hasPermi="['mes:pro-edhr-nonconformance-review:deviation-create']" type="danger" @click="openBatchNcr">批量发起不合格审批（{{ selectedCritical.length }}）</el-button>
        <el-button @click="load">查询</el-button><el-button @click="reset">重置</el-button>
      </template>
      <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
        <el-table v-loading="loading" :data="rows" empty-text="没有偏差" row-key="id" @selection-change="handleSelectionChange" @sort-change="handleTemplateSortChange">
          <el-table-column type="selection" width="48" :selectable="isCriticalOpen" />
          <el-table-column prop="deviationCode" label="偏差编号" min-width="180" v-bind="sortColumnAttrs('deviationCode')" />
          <el-table-column prop="batchExecutionCode" label="关联批记录" min-width="180" />
          <el-table-column prop="batchCode" label="批号" min-width="140" />
          <el-table-column prop="level" label="等级" width="100" v-bind="sortColumnAttrs('level')"><template #default="{ row }"><el-tag :type="row.level === 'CRITICAL' ? 'danger' : 'warning'">{{ row.level === 'CRITICAL' ? '重大（关键）' : '普通' }}</el-tag></template></el-table-column>
          <el-table-column prop="status" label="状态" width="100" v-bind="sortColumnAttrs('status')"><template #default="{ row }">{{ row.status === 'OPEN' ? '未处理' : '已处理' }}</template></el-table-column>
          <el-table-column prop="initiatedAt" label="发起时间" min-width="180" v-bind="sortColumnAttrs('initiatedAt')"><template #default="{ row }">{{ formatTime(row.initiatedAt) }}</template></el-table-column>
          <el-table-column label="操作" width="100" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="openDetail(row.id)">详情</el-button></template></el-table-column>
        </el-table>
      </template>
    </UnifiedListTemplate>
    <el-drawer v-model="detailVisible" title="偏差详情" size="560px"><DeviationDetail v-if="detailId" :id="detailId" /></el-drawer>
    <el-dialog v-model="createVisible" title="发起偏差" width="680px" destroy-on-close>
      <el-alert v-if="createError" :title="createError" type="error" :closable="false" show-icon class="mb-12px" />
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="110px">
        <el-form-item label="批记录" prop="batchExecutionId"><el-select v-model="createForm.batchExecutionId" filterable remote clearable :remote-method="loadBatchOptions" :loading="batchLoading" placeholder="请选择正式批记录" style="width: 100%"><el-option v-for="item in batchOptions" :key="item.batchExecutionId" :label="`${item.batchExecutionCode || item.batchCode || item.batchExecutionId} / ${item.productName || '—'}`" :value="item.batchExecutionId" /></el-select></el-form-item>
        <el-form-item label="等级" prop="level"><el-radio-group v-model="createForm.level"><el-radio label="NORMAL">普通</el-radio><el-radio label="CRITICAL">重大（关键）</el-radio></el-radio-group></el-form-item>
        <el-form-item label="分类" prop="categoryCodes"><el-checkbox-group v-model="createForm.categoryCodes"><el-checkbox v-for="item in categoryOptions" :key="item.value" :label="item.value">{{ item.label }}</el-checkbox></el-checkbox-group></el-form-item>
        <el-form-item label="偏差描述" prop="description"><el-input v-model="createForm.description" type="textarea" :rows="3" maxlength="4000" show-word-limit /></el-form-item>
        <el-form-item label="等级依据" prop="levelBasis"><el-input v-model="createForm.levelBasis" type="textarea" :rows="2" maxlength="4000" show-word-limit /></el-form-item>
        <el-form-item label="发现位置"><el-input v-model="createForm.discoveryLocation" maxlength="255" /></el-form-item>
        <el-form-item label="紧急措施"><el-input v-model="createForm.emergencyAction" type="textarea" :rows="2" maxlength="4000" /></el-form-item>
        <el-form-item label="签名密码" prop="signaturePassword"><el-input v-model="createForm.signaturePassword" type="password" show-password autocomplete="new-password" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="createVisible = false">取消</el-button><el-button type="primary" :loading="createLoading" @click="submitCreate">签名并发起</el-button></template>
    </el-dialog>
    <el-dialog v-model="batchNcrVisible" title="批量发起不合格审批" width="520px" destroy-on-close>
      <el-alert type="info" :closable="false" :title="`将关联 ${selectedCritical.length} 条同一批记录的关键偏差`" class="mb-12px" />
      <el-form label-width="110px">
        <el-form-item label="不合格原因" required><el-input v-model="batchNcrForm.nonconformanceReason" type="textarea" :rows="4" maxlength="500" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="batchNcrForm.remark" type="textarea" :rows="2" maxlength="500" /></el-form-item>
        <el-form-item label="签名密码" required><el-input v-model="batchNcrForm.signaturePassword" type="password" show-password autocomplete="new-password" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="batchNcrVisible = false">取消</el-button><el-button type="danger" :loading="batchNcrLoading" @click="submitBatchNcr">签名并发起</el-button></template>
    </el-dialog>
  </ContentWrap>
</template>

<script setup lang="ts">
import { createDeviation, getDeviationBatchOptions, getDeviationPage, type DeviationBatchOptionRespVO, type DeviationCreateReqVO, type DeviationRespVO } from '@/api/mes/pro/edhr/deviation'
import type { FormInstance, FormRules } from 'element-plus'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { useTableQuickFilter, type TableQuickFilterDefinition } from '@/hooks/web/useTableQuickFilter'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import dayjs from 'dayjs'
import DeviationDetail from './DeviationDetail.vue'
import { createCriticalDeviationReview } from '@/api/mes/pro/edhr/nonconformanceReview'

const message = useMessage()
const loading = ref(false)
const rows = ref<DeviationRespVO[]>([])
const selectedCritical = ref<DeviationRespVO[]>([])
const batchNcrVisible = ref(false)
const batchNcrLoading = ref(false)
const batchNcrForm = reactive({ nonconformanceReason: '', remark: '', signaturePassword: '' })
const total = ref(0)
const activeStatus = ref<'ALL' | 'OPEN' | 'CLOSED'>('ALL')
const detailVisible = ref(false)
const detailId = ref<number>()
const query = reactive({ pageNo: 1, pageSize: 10, search: '', level: undefined as 'NORMAL' | 'CRITICAL' | undefined, initiatedAt: [] as string[], sortField: '', sortOrder: undefined as 'asc' | 'desc' | undefined })
const formatTime = (value?: string | number | null) => {
  if (value == null || value === '') return '--'
  const formatted = dayjs(value)
  return formatted.isValid() ? formatted.format('YYYY-MM-DD HH:mm:ss') : String(value)
}
const listColumns: UserTableColumnDefinition[] = [
  { key: 'deviationCode', label: '偏差编号', minWidth: 180 },
  { key: 'batchExecutionCode', label: '关联批记录', minWidth: 180 },
  { key: 'batchCode', label: '批号', minWidth: 140 },
  { key: 'level', label: '等级', width: 100 },
  { key: 'status', label: '状态', width: 100 },
  { key: 'initiatedAt', label: '发起时间', minWidth: 180 },
  { key: 'operation', label: '操作', width: 100, hideable: false, business: false }
]
const { columns: listColumnState } = useUserTableColumns('mes.pro.edhr.deviation.main', listColumns)
const filterDefinitions: TableQuickFilterDefinition[] = [
  { key: 'search', label: '关键词', type: 'text', queryParamKey: 'search', placeholder: '偏差编号、批号或批记录' },
  { key: 'level', label: '等级', type: 'select', queryParamKey: 'level', options: [{ label: '普通', value: 'NORMAL' }, { label: '重大（关键）', value: 'CRITICAL' }] },
  { key: 'initiatedAt', label: '发起时间', type: 'dateRange', queryParamKey: 'initiatedAt' }
]
let requestSerial = 0
const createVisible = ref(false)
const createLoading = ref(false)
const createError = ref('')
const createFormRef = ref<FormInstance>()
const batchLoading = ref(false)
const batchOptions = ref<DeviationBatchOptionRespVO[]>([])
const categoryOptions = [
  { label: '生产过程', value: 'PRODUCTION_PROCESS' }, { label: '生产公用系统', value: 'PRODUCTION_UTILITY' },
  { label: '检验', value: 'INSPECTION' }, { label: '验证确认', value: 'VALIDATION_CONFIRMATION' },
  { label: '物料管理', value: 'MATERIAL_MANAGEMENT' }, { label: '文件记录', value: 'DOCUMENT_RECORD' },
  { label: '产品放行', value: 'PRODUCT_RELEASE' }, { label: '其他系统', value: 'OTHER_SYSTEM' }
]
const createForm = reactive<DeviationCreateReqVO>({ batchExecutionId: 0, level: 'NORMAL', categoryCodes: [], description: '', levelBasis: '', discoveryLocation: '', emergencyAction: '', idempotencyKey: '', signaturePassword: '' })
const createRules: FormRules = {
  batchExecutionId: [{ required: true, message: '请选择正式批记录', trigger: 'change' }],
  level: [{ required: true, message: '请选择偏差等级', trigger: 'change' }],
  categoryCodes: [{ type: 'array', min: 1, required: true, message: '至少选择一个分类', trigger: 'change' }],
  description: [{ required: true, message: '请填写偏差描述', trigger: 'blur' }],
  levelBasis: [{ required: true, message: '请填写等级依据', trigger: 'blur' }],
  signaturePassword: [{ required: true, message: '请输入签名密码', trigger: 'blur' }]
}

const load = async () => {
  const serial = ++requestSerial
  loading.value = true
  try {
    const data = await getDeviationPage({ ...query, initiatedAtStart: query.initiatedAt[0], initiatedAtEnd: query.initiatedAt[1], status: activeStatus.value === 'ALL' ? undefined : activeStatus.value })
    if (serial !== requestSerial) return
    rows.value = data.list || []
    total.value = data.total || 0
  } catch (error: any) {
    if (serial !== requestSerial) return
    rows.value = []
    const status = error?.response?.status
    message.error(status === 403 ? '当前账号没有偏差查询权限' : status === 401 ? '登录状态已失效，请重新登录' : '偏差查询失败，请重试')
  } finally { loading.value = false }
}
const quickFilter = useTableQuickFilter('mes.pro.edhr.deviation.main', filterDefinitions, query, load)
const handleSortChange = (state: { key?: string; prop?: string; order?: 'ascending' | 'descending' | null }) => {
  query.sortField = state.key || state.prop || ''
  query.sortOrder = state.order === 'ascending' ? 'asc' : state.order === 'descending' ? 'desc' : undefined
  query.pageNo = 1
  void load()
}
const handleTabChange = () => { query.pageNo = 1; load() }
const reset = () => { query.search = ''; query.level = undefined; query.initiatedAt = []; query.sortField = ''; query.sortOrder = undefined; query.pageNo = 1; quickFilter.resetQuickFilter(); void load() }
const openDetail = (id: number) => { detailId.value = id; detailVisible.value = true }
const isCriticalOpen = (row: DeviationRespVO) => row.level === 'CRITICAL' && row.status === 'OPEN'
const handleSelectionChange = (selection: DeviationRespVO[]) => {
  const eligible = selection.filter(isCriticalOpen)
  const batchIds = [...new Set(eligible.map(row => row.batchExecutionId))]
  selectedCritical.value = batchIds.length <= 1 ? eligible : []
}
const openBatchNcr = () => {
  if (!selectedCritical.value.length) return
  batchNcrForm.nonconformanceReason = ''
  batchNcrForm.remark = ''
  batchNcrForm.signaturePassword = ''
  batchNcrVisible.value = true
}
const submitBatchNcr = async () => {
  const reason = batchNcrForm.nonconformanceReason.trim()
  const password = batchNcrForm.signaturePassword.trim()
  if (!reason || !password || !selectedCritical.value.length) return
  const batchExecutionId = selectedCritical.value[0].batchExecutionId
  batchNcrLoading.value = true
  try {
    await createCriticalDeviationReview({
      batchExecutionId,
      deviationIds: selectedCritical.value.map(row => row.id),
      nonconformanceReason: reason,
      remark: batchNcrForm.remark.trim() || undefined,
      idempotencyKey: `EDHR_DEVIATION_NCR_BATCH_${batchExecutionId}_${Date.now()}`,
      signaturePassword: password
    })
    batchNcrVisible.value = false
    selectedCritical.value = []
    await load()
    message.success('不合格审批已发起，所选偏差已转审关闭')
  } catch (error: any) {
    message.error(error?.response?.data?.msg || '不合格审批发起失败，请重试')
  } finally { batchNcrLoading.value = false }
}
const loadBatchOptions = async (search = '') => {
  batchLoading.value = true
  try { const data = await getDeviationBatchOptions({ pageNo: 1, pageSize: 50, search }); batchOptions.value = data.list || [] } catch { batchOptions.value = [] } finally { batchLoading.value = false }
}
const openCreate = async () => {
  createError.value = ''
  createForm.idempotencyKey = `EDHR_DEVIATION_${Date.now()}_${Math.random().toString(36).slice(2)}`
  createForm.signaturePassword = ''
  createForm.categoryCodes = []
  createVisible.value = true
  await loadBatchOptions()
}
const submitCreate = async () => {
  if (!createFormRef.value || !(await createFormRef.value.validate().catch(() => false))) return
  createLoading.value = true
  createError.value = ''
  try {
    await createDeviation({ ...createForm })
    message.success('偏差已签名发起')
    createVisible.value = false
    await load()
  } catch (error: any) { createError.value = error?.response?.data?.msg || '偏差发起失败，请重试' } finally { createLoading.value = false }
}
onMounted(load)
</script>
