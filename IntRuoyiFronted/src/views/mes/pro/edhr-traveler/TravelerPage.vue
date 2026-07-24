<template>
  <ContentWrap>
    <div class="edhr-traveler-page">
      <el-tabs v-model="activeTab" class="edhr-traveler-page__tabs" @tab-change="handleTabChange">
        <el-tab-pane label="流转单实例" name="traveler" />
        <el-tab-pane label="模板维护" name="template" />
      </el-tabs>

      <section v-show="activeTab === 'traveler'" class="edhr-traveler-page__section">
        <el-form :inline="true" :model="travelerQuery" class="edhr-traveler-page__toolbar">
          <el-form-item label="流转单编码">
            <el-input v-model="travelerQuery.travelerCode" clearable class="!w-170px" />
          </el-form-item>
          <el-form-item label="批次执行">
            <el-input v-model="travelerQuery.batchExecutionCode" clearable class="!w-170px" />
          </el-form-item>
          <el-form-item label="工单">
            <el-input v-model="travelerQuery.workOrderCode" clearable class="!w-160px" />
          </el-form-item>
          <el-form-item label="批次">
            <el-input v-model="travelerQuery.batchCode" clearable class="!w-170px" />
          </el-form-item>
          <el-form-item label="SN">
            <el-input v-model="travelerQuery.serialNo" clearable class="!w-150px" />
          </el-form-item>
          <el-form-item label="工序">
            <el-input v-model="travelerQuery.processName" clearable class="!w-150px" />
          </el-form-item>
          <el-form-item label="打印状态">
            <el-select v-model="travelerQuery.printStatus" clearable class="!w-140px">
              <el-option label="未打印" value="NOT_PRINTED" />
              <el-option label="已排队" value="QUEUED" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleTravelerQuery">查询</el-button>
            <el-button @click="resetTravelerQuery">重置</el-button>
            <el-button
              v-hasPermi="['mes:pro-edhr-traveler:generate']"
              type="success"
              @click="openGenerateDialog"
            >
              生成流转单
            </el-button>
          </el-form-item>
        </el-form>

        <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon />

        <el-table
          v-loading="travelerLoading"
          :data="travelerList"
          stripe
          :show-overflow-tooltip="true"
          empty-text="暂无流转单"
        >
          <el-table-column label="流转单编码" prop="travelerCode" min-width="180" />
          <el-table-column label="模板" min-width="170">
            <template #default="{ row }">
              <div class="edhr-traveler-page__strong">{{ row.templateCode || '--' }}</div>
              <div class="edhr-traveler-page__muted">版本：{{ row.templateVersion || '--' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="生产上下文" min-width="280">
            <template #default="{ row }">
              <div class="edhr-traveler-page__strong">{{ row.workOrderCode || '--' }}</div>
              <div class="edhr-traveler-page__muted">批次：{{ row.batchCode || '--' }}</div>
              <div class="edhr-traveler-page__muted">批次执行：{{ row.batchExecutionCode || row.batchExecutionId || '--' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="SN / 粒度" min-width="160">
            <template #default="{ row }">
              <div>{{ row.serialNo || '--' }}</div>
              <el-tag size="small" :type="row.scopeType === 'SN_LEVEL' ? 'warning' : 'info'">
                {{ resolveScopeTypeLabel(row.scopeType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="工序" min-width="180">
            <template #default="{ row }">
              <div class="edhr-traveler-page__strong">{{ row.processName || '--' }}</div>
              <div class="edhr-traveler-page__muted">{{ row.processCode || '--' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag type="success">{{ resolveTravelerStatusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="打印状态" width="110">
            <template #default="{ row }">
              <el-tag :type="row.printStatus === 'QUEUED' ? 'warning' : 'info'">
                {{ resolvePrintStatusLabel(row.printStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="生成时间" prop="generatedAt" width="180" />
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDetailDrawer(row)">详情</el-button>
              <el-button link type="primary" @click="openEventDrawer(row)">事件</el-button>
            </template>
          </el-table-column>
        </el-table>
        <Pagination
          :total="travelerTotal"
          v-model:page="travelerQuery.pageNo"
          v-model:limit="travelerQuery.pageSize"
          @pagination="loadTravelerList"
        />
      </section>

      <section v-show="activeTab === 'template'" class="edhr-traveler-page__section">
        <el-form :inline="true" :model="templateQuery" class="edhr-traveler-page__toolbar">
          <el-form-item label="模板编码">
            <el-input v-model="templateQuery.templateCode" clearable class="!w-170px" />
          </el-form-item>
          <el-form-item label="模板名称">
            <el-input v-model="templateQuery.templateName" clearable class="!w-170px" />
          </el-form-item>
          <el-form-item label="模板状态">
            <el-select v-model="templateQuery.status" clearable class="!w-130px">
              <el-option label="草稿" value="DRAFT" />
              <el-option label="已启用" value="ACTIVE" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleTemplateQuery">查询</el-button>
            <el-button @click="resetTemplateQuery">重置</el-button>
            <el-button
              v-hasPermi="['mes:pro-edhr-traveler-template:create']"
              type="success"
              @click="openTemplateDialog"
            >
              创建模板
            </el-button>
          </el-form-item>
        </el-form>

        <el-alert v-if="templateError" :title="templateError" type="error" :closable="false" show-icon />

        <el-table
          v-loading="templateLoading"
          :data="templateList"
          stripe
          :show-overflow-tooltip="true"
          empty-text="暂无流转单模板"
        >
          <el-table-column label="模板编码" prop="templateCode" min-width="170" />
          <el-table-column label="模板名称" prop="templateName" min-width="180" />
          <el-table-column label="模板版本" prop="templateVersion" width="120" />
          <el-table-column label="适用范围" min-width="260">
            <template #default="{ row }">
              <div>产品：{{ row.applicableProductCode || '通用' }}</div>
              <div class="edhr-traveler-page__muted">路线：{{ row.applicableRouteCode || row.applicableRouteId || '通用' }}</div>
              <div class="edhr-traveler-page__muted">工序：{{ row.applicableProcessName || row.applicableProcessCode || row.applicableProcessId || '通用' }}</div>
            </template>
          </el-table-column>
          <el-table-column label="模板状态" width="110">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
                {{ resolveTemplateStatusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="启用时间" prop="activeAt" width="180" />
          <el-table-column label="操作" width="110" fixed="right">
            <template #default="{ row }">
              <el-button
                v-hasPermi="['mes:pro-edhr-traveler-template:activate']"
                link
                type="primary"
                :disabled="row.status === 'ACTIVE'"
                @click="activateTemplate(row)"
              >
                启用
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <Pagination
          :total="templateTotal"
          v-model:page="templateQuery.pageNo"
          v-model:limit="templateQuery.pageSize"
          @pagination="loadTemplateList"
        />
      </section>
    </div>

    <Dialog title="创建模板" v-model="templateDialogVisible" width="720px">
      <el-alert v-if="templateError" :title="templateError" type="error" :closable="false" show-icon />
      <el-form ref="templateFormRef" :model="templateForm" :rules="templateRules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="模板编码" prop="templateCode">
              <el-input v-model="templateForm.templateCode" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模板版本" prop="templateVersion">
              <el-input v-model="templateForm.templateVersion" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="模板名称" prop="templateName">
              <el-input v-model="templateForm.templateName" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="产品编码">
              <el-input v-model="templateForm.applicableProductCode" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="路线ID">
              <el-input-number v-model="templateForm.applicableRouteId" :min="1" :controls="false" class="!w-100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="路线编码">
              <el-input v-model="templateForm.applicableRouteCode" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="工序ID">
              <el-input-number v-model="templateForm.applicableProcessId" :min="1" :controls="false" class="!w-100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="工序编码">
              <el-input v-model="templateForm.applicableProcessCode" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="工序名称">
              <el-input v-model="templateForm.applicableProcessName" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="templateForm.remark" type="textarea" :rows="2" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="templateDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="templateSubmitting" @click="submitTemplate">保存</el-button>
      </template>
    </Dialog>

    <Dialog title="生成流转单" v-model="generateDialogVisible" width="620px">
      <el-alert v-if="generateError" :title="generateError" type="error" :closable="false" show-icon />
      <el-form ref="generateFormRef" :model="generateForm" :rules="generateRules" label-width="120px">
        <el-form-item label="模板ID" prop="templateId">
          <el-select v-model="generateForm.templateId" filterable class="!w-100%">
            <el-option
              v-for="item in activeTemplateOptions"
              :key="item.id"
              :label="`${item.templateCode || item.id} / ${item.templateName || '--'}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="批次执行ID" prop="batchExecutionId">
          <el-input-number v-model="generateForm.batchExecutionId" :min="1" :controls="false" class="!w-100%" />
        </el-form-item>
        <el-form-item label="路线工序ID" prop="routeProcessId">
          <el-input-number v-model="generateForm.routeProcessId" :min="1" :controls="false" class="!w-100%" />
        </el-form-item>
        <el-form-item label="SN">
          <el-input v-model="generateForm.serialNo" clearable />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="generateForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="generateDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="generateSubmitting" @click="submitGenerate">生成</el-button>
      </template>
    </Dialog>

    <el-drawer v-model="detailDrawerVisible" title="流转单详情" size="640px">
      <el-descriptions v-if="currentTraveler" :column="1" border>
        <el-descriptions-item label="流转单编码">{{ currentTraveler.travelerCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="模板">{{ currentTraveler.templateCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="工单">{{ currentTraveler.workOrderCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="批次">{{ currentTraveler.batchCode || '--' }}</el-descriptions-item>
        <el-descriptions-item label="SN">{{ currentTraveler.serialNo || '--' }}</el-descriptions-item>
        <el-descriptions-item label="工序">{{ currentTraveler.processName || '--' }}</el-descriptions-item>
        <el-descriptions-item label="业务键">{{ currentTraveler.businessKeyHash || '--' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>

    <el-drawer v-model="eventDrawerVisible" title="流转单事件" size="760px">
      <el-table
        v-loading="eventLoading"
        :data="eventList"
        stripe
        :show-overflow-tooltip="true"
        empty-text="暂无事件"
      >
        <el-table-column label="事件" prop="eventType" min-width="160" />
        <el-table-column label="结果" prop="resultStatus" width="120" />
        <el-table-column label="失败原因" prop="failureReason" min-width="220" />
        <el-table-column label="操作人" min-width="130">
          <template #default="{ row }">{{ row.operatorUsername || row.operatorUserId || '--' }}</template>
        </el-table-column>
        <el-table-column label="发生时间" prop="occurredAt" width="180" />
      </el-table>
      <Pagination
        :total="eventTotal"
        v-model:page="eventQuery.pageNo"
        v-model:limit="eventQuery.pageSize"
        @pagination="loadEventList"
      />
    </el-drawer>
  </ContentWrap>
</template>

<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import {
  activateEdhrTravelerTemplate,
  createEdhrTravelerTemplate,
  generateEdhrTraveler,
  getEdhrTravelerEventPage,
  getEdhrTravelerPage,
  getEdhrTravelerTemplatePage,
  type EdhrTravelerEventRespVO,
  type EdhrTravelerGenerateReqVO,
  type EdhrTravelerRespVO,
  type EdhrTravelerTemplateCreateReqVO,
  type EdhrTravelerTemplateRespVO
} from '@/api/mes/pro/edhr/traveler'

const message = useMessage()

const activeTab = ref<'traveler' | 'template'>('traveler')
const loadError = ref('')
const templateError = ref('')
const generateError = ref('')

const travelerLoading = ref(false)
const travelerList = ref<EdhrTravelerRespVO[]>([])
const travelerTotal = ref(0)
const travelerQuery = reactive({
  pageNo: 1,
  pageSize: 10,
  travelerCode: undefined as string | undefined,
  batchExecutionCode: undefined as string | undefined,
  workOrderCode: undefined as string | undefined,
  batchCode: undefined as string | undefined,
  serialNo: undefined as string | undefined,
  processName: undefined as string | undefined,
  printStatus: undefined as string | undefined
})

const templateLoading = ref(false)
const templateList = ref<EdhrTravelerTemplateRespVO[]>([])
const templateTotal = ref(0)
const templateQuery = reactive({
  pageNo: 1,
  pageSize: 10,
  templateCode: undefined as string | undefined,
  templateName: undefined as string | undefined,
  status: undefined as string | undefined
})

const eventLoading = ref(false)
const eventDrawerVisible = ref(false)
const eventList = ref<EdhrTravelerEventRespVO[]>([])
const eventTotal = ref(0)
const eventQuery = reactive({
  pageNo: 1,
  pageSize: 10,
  travelerId: undefined as number | undefined
})

const detailDrawerVisible = ref(false)
const currentTraveler = ref<EdhrTravelerRespVO>()

const templateDialogVisible = ref(false)
const templateSubmitting = ref(false)
const templateFormRef = ref<FormInstance>()
const templateForm = reactive<EdhrTravelerTemplateCreateReqVO>({
  templateCode: '',
  templateName: '',
  templateVersion: '',
  applicableProductCode: undefined,
  applicableRouteId: undefined,
  applicableRouteCode: undefined,
  applicableProcessId: undefined,
  applicableProcessCode: undefined,
  applicableProcessName: undefined,
  remark: undefined
})
const templateRules: FormRules = {
  templateCode: [{ required: true, message: '模板编码不能为空', trigger: 'blur' }],
  templateName: [{ required: true, message: '模板名称不能为空', trigger: 'blur' }],
  templateVersion: [{ required: true, message: '模板版本不能为空', trigger: 'blur' }]
}

const generateDialogVisible = ref(false)
const generateSubmitting = ref(false)
const generateFormRef = ref<FormInstance>()
const generateForm = reactive<EdhrTravelerGenerateReqVO>({
  templateId: undefined as unknown as number,
  batchExecutionId: undefined as unknown as number,
  routeProcessId: undefined as unknown as number,
  serialNo: undefined,
  remark: undefined
})
const generateRules: FormRules = {
  templateId: [{ required: true, message: '模板ID不能为空', trigger: 'change' }],
  batchExecutionId: [{ required: true, message: '批次执行ID不能为空', trigger: 'blur' }],
  routeProcessId: [{ required: true, message: '路线工序ID不能为空', trigger: 'blur' }]
}

const activeTemplateOptions = computed(() =>
  templateList.value.filter((item) => item.status === 'ACTIVE')
)

const resolveErrorMessage = (error: unknown, defaultMessage: string) => {
  const responseMessage = (error as { response?: { data?: { msg?: string; message?: string } } })?.response?.data
  if (responseMessage?.msg?.trim()) return responseMessage.msg
  if (responseMessage?.message?.trim()) return responseMessage.message
  if (error instanceof Error && error.message.trim()) return error.message
  return defaultMessage
}

const normalizeQuery = <T extends Record<string, unknown>>(query: T): T => {
  const normalized: Record<string, unknown> = { ...query }
  Object.keys(normalized).forEach((key) => {
    if (normalized[key] === '') normalized[key] = undefined
  })
  return normalized as T
}

const loadTravelerList = async () => {
  travelerLoading.value = true
  loadError.value = ''
  try {
    const data = await getEdhrTravelerPage(normalizeQuery(travelerQuery))
    travelerList.value = data.list || []
    travelerTotal.value = data.total || 0
  } catch (error) {
    loadError.value = resolveErrorMessage(error, '流转单列表加载失败。')
    message.error(resolveErrorMessage(error, '流转单列表加载失败。'))
  } finally {
    travelerLoading.value = false
  }
}

const loadTemplateList = async () => {
  templateLoading.value = true
  templateError.value = ''
  try {
    const data = await getEdhrTravelerTemplatePage(normalizeQuery(templateQuery))
    templateList.value = data.list || []
    templateTotal.value = data.total || 0
  } catch (error) {
    templateError.value = resolveErrorMessage(error, '模板列表加载失败。')
    message.error(resolveErrorMessage(error, '模板列表加载失败。'))
  } finally {
    templateLoading.value = false
  }
}

const loadEventList = async () => {
  if (!eventQuery.travelerId) return
  eventLoading.value = true
  try {
    const data = await getEdhrTravelerEventPage(eventQuery)
    eventList.value = data.list || []
    eventTotal.value = data.total || 0
  } catch (error) {
    message.error(resolveErrorMessage(error, '流转单事件加载失败。'))
  } finally {
    eventLoading.value = false
  }
}

const handleTravelerQuery = () => {
  travelerQuery.pageNo = 1
  loadTravelerList()
}

const resetTravelerQuery = () => {
  Object.assign(travelerQuery, {
    pageNo: 1,
    pageSize: travelerQuery.pageSize,
    travelerCode: undefined,
    batchExecutionCode: undefined,
    workOrderCode: undefined,
    batchCode: undefined,
    serialNo: undefined,
    processName: undefined,
    printStatus: undefined
  })
  loadTravelerList()
}

const handleTemplateQuery = () => {
  templateQuery.pageNo = 1
  loadTemplateList()
}

const resetTemplateQuery = () => {
  Object.assign(templateQuery, {
    pageNo: 1,
    pageSize: templateQuery.pageSize,
    templateCode: undefined,
    templateName: undefined,
    status: undefined
  })
  loadTemplateList()
}

const openTemplateDialog = () => {
  templateError.value = ''
  Object.assign(templateForm, {
    templateCode: '',
    templateName: '',
    templateVersion: '',
    applicableProductCode: undefined,
    applicableRouteId: undefined,
    applicableRouteCode: undefined,
    applicableProcessId: undefined,
    applicableProcessCode: undefined,
    applicableProcessName: undefined,
    remark: undefined
  })
  templateDialogVisible.value = true
}

const submitTemplate = async () => {
  const valid = await templateFormRef.value?.validate().then(() => true).catch(() => false)
  if (!valid) {
    templateError.value = '请补齐模板必填项。'
    return
  }
  templateSubmitting.value = true
  templateError.value = ''
  try {
    await createEdhrTravelerTemplate({ ...templateForm })
    message.success('模板创建成功')
    templateDialogVisible.value = false
    await loadTemplateList()
  } catch (error) {
    templateError.value = resolveErrorMessage(error, '模板创建失败。')
    message.error(resolveErrorMessage(error, '模板创建失败。'))
  } finally {
    templateSubmitting.value = false
  }
}

const activateTemplate = async (row: EdhrTravelerTemplateRespVO) => {
  if (!row.id) {
    message.error('模板ID缺失，无法启用。')
    return
  }
  try {
    await activateEdhrTravelerTemplate({ id: row.id })
    message.success('模板已启用')
    await loadTemplateList()
  } catch (error) {
    templateError.value = resolveErrorMessage(error, '模板启用失败。')
    message.error(resolveErrorMessage(error, '模板启用失败。'))
  }
}

const openGenerateDialog = async () => {
  generateError.value = ''
  if (!templateList.value.length) {
    await loadTemplateList()
  }
  Object.assign(generateForm, {
    templateId: activeTemplateOptions.value[0]?.id,
    batchExecutionId: undefined,
    routeProcessId: undefined,
    serialNo: undefined,
    requestId: `traveler-${Date.now()}`,
    remark: undefined
  })
  generateDialogVisible.value = true
}

const submitGenerate = async () => {
  const valid = await generateFormRef.value?.validate().then(() => true).catch(() => false)
  if (!valid) {
    generateError.value = '请补齐生成必填项。'
    return
  }
  generateSubmitting.value = true
  generateError.value = ''
  try {
    await generateEdhrTraveler({ ...generateForm })
    message.success('流转单已生成')
    generateDialogVisible.value = false
    await loadTravelerList()
  } catch (error) {
    generateError.value = resolveErrorMessage(error, '流转单生成失败。')
    message.error(resolveErrorMessage(error, '流转单生成失败。'))
  } finally {
    generateSubmitting.value = false
  }
}

const openDetailDrawer = (row: EdhrTravelerRespVO) => {
  currentTraveler.value = row
  detailDrawerVisible.value = true
}

const openEventDrawer = async (row: EdhrTravelerRespVO) => {
  if (!row.id) {
    message.error('流转单ID缺失，无法查看事件。')
    return
  }
  currentTraveler.value = row
  eventQuery.pageNo = 1
  eventQuery.travelerId = row.id
  eventDrawerVisible.value = true
  await loadEventList()
}

const handleTabChange = () => {
  if (activeTab.value === 'template' && !templateList.value.length) {
    loadTemplateList()
  }
}

const resolveTemplateStatusLabel = (status?: string) => {
  if (status === 'ACTIVE') return '已启用'
  if (status === 'DRAFT') return '草稿'
  return status || '--'
}

const resolveTravelerStatusLabel = (status?: string) => {
  if (status === 'GENERATED') return '已生成'
  return status || '--'
}

const resolvePrintStatusLabel = (status?: string) => {
  if (status === 'NOT_PRINTED') return '未打印'
  if (status === 'QUEUED') return '已排队'
  return status || '--'
}

const resolveScopeTypeLabel = (scopeType?: string) => {
  if (scopeType === 'SN_LEVEL') return 'SN级'
  if (scopeType === 'BATCH_LEVEL') return '批次级'
  return '--'
}

onMounted(() => {
  loadTravelerList()
  loadTemplateList()
})
</script>

<style scoped>
.edhr-traveler-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.edhr-traveler-page__tabs {
  --el-tabs-header-height: 40px;
}

.edhr-traveler-page__section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.edhr-traveler-page__toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  gap: 0 8px;
}

.edhr-traveler-page__strong {
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.edhr-traveler-page__muted {
  margin-top: 2px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
