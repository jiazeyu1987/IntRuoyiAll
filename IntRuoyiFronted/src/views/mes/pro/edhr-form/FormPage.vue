<template>
  <ContentWrap>
    <div class="edhr-form-page">
      <el-tabs v-model="activeTab" class="edhr-form-page__tabs" @tab-change="handleTabChange">
        <el-tab-pane label="独立表单实例" name="instance" />
        <el-tab-pane label="表单模板" name="template" />
      </el-tabs>

      <div v-if="activeTab === 'instance'" class="edhr-form-page__panel">
        <el-form :inline="true" :model="instanceQueryParams" class="edhr-form-page__toolbar">
          <el-form-item label="实例编码">
            <el-input v-model="instanceQueryParams.instanceCode" clearable class="!w-190px" />
          </el-form-item>
          <el-form-item label="模板编码">
            <el-input v-model="instanceQueryParams.templateCode" clearable class="!w-170px" />
          </el-form-item>
          <el-form-item label="表单状态">
            <el-select v-model="instanceQueryParams.status" clearable class="!w-140px">
              <el-option label="草稿" :value="EDHR_FORM_INSTANCE_STATUS_DRAFT" />
              <el-option label="已提交" :value="EDHR_FORM_INSTANCE_STATUS_SUBMITTED" />
            </el-select>
          </el-form-item>
          <el-form-item label="业务编码">
            <el-input v-model="instanceQueryParams.businessObjectCode" clearable class="!w-170px" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleInstanceQuery">查询</el-button>
            <el-button @click="resetInstanceQuery">重置</el-button>
            <el-button
              v-hasPermi="['mes:pro-edhr-form-instance:create']"
              type="primary"
              @click="openInstanceDialog"
            >
              <Icon icon="ep:plus" class="mr-5px" />
              创建实例
            </el-button>
          </el-form-item>
        </el-form>

        <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon />

        <div class="edhr-form-page__table">
          <el-table
            v-loading="instanceLoading"
            :data="instanceList"
            stripe
            :show-overflow-tooltip="true"
            empty-text="暂无独立表单实例"
          >
            <el-table-column label="实例编码" min-width="210">
              <template #default="{ row }">
                <el-button link type="primary" @click="openDetailDrawer(row)">
                  {{ row.instanceCode || row.id }}
                </el-button>
              </template>
            </el-table-column>
            <el-table-column label="模板编码" prop="templateCode" min-width="150" />
            <el-table-column label="模板名称" prop="templateName" min-width="170" />
            <el-table-column label="模板版本" prop="templateVersion" width="110" />
            <el-table-column label="业务范围" prop="businessScope" min-width="130" />
            <el-table-column label="业务编码" prop="businessObjectCode" min-width="150" />
            <el-table-column label="表单状态" width="110" align="center">
              <template #default="{ row }">
                <el-tag :type="resolveInstanceStatusType(row.status)">
                  {{ resolveInstanceStatusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="版本" prop="version" width="80" align="center" />
            <el-table-column label="最近更新时间" width="180">
              <template #default="{ row }">
                {{ formatDateTime(row.updateTime) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <div class="edhr-form-page__actions">
                  <el-button link type="primary" @click="openDetailDrawer(row)">详情</el-button>
                  <el-button link type="primary" @click="openEventDrawer(row)">事件</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
          <Pagination
            :total="instanceTotal"
            v-model:page="instanceQueryParams.pageNo"
            v-model:limit="instanceQueryParams.pageSize"
            @pagination="loadInstanceList"
          />
        </div>
      </div>

      <div v-else class="edhr-form-page__panel">
        <el-form :inline="true" :model="templateQueryParams" class="edhr-form-page__toolbar">
          <el-form-item label="模板编码">
            <el-input v-model="templateQueryParams.templateCode" clearable class="!w-180px" />
          </el-form-item>
          <el-form-item label="模板名称">
            <el-input v-model="templateQueryParams.templateName" clearable class="!w-180px" />
          </el-form-item>
          <el-form-item label="表单状态">
            <el-select v-model="templateQueryParams.status" clearable class="!w-140px">
              <el-option label="草稿" :value="EDHR_FORM_TEMPLATE_STATUS_DRAFT" />
              <el-option label="已启用" :value="EDHR_FORM_TEMPLATE_STATUS_ACTIVE" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleTemplateQuery">查询</el-button>
            <el-button @click="resetTemplateQuery">重置</el-button>
            <el-button
              v-hasPermi="['mes:pro-edhr-form-template:create']"
              type="primary"
              @click="openTemplateDialog"
            >
              <Icon icon="ep:plus" class="mr-5px" />
              创建模板
            </el-button>
          </el-form-item>
        </el-form>

        <el-alert v-if="loadError" :title="loadError" type="error" :closable="false" show-icon />

        <div class="edhr-form-page__table">
          <el-table
            v-loading="templateLoading"
            :data="templateList"
            stripe
            :show-overflow-tooltip="true"
            empty-text="暂无表单模板"
          >
            <el-table-column label="模板编码" prop="templateCode" min-width="160" />
            <el-table-column label="模板名称" prop="templateName" min-width="180" />
            <el-table-column label="模板版本" prop="templateVersion" width="110" />
            <el-table-column label="表单状态" width="110" align="center">
              <template #default="{ row }">
                <el-tag :type="resolveTemplateStatusType(row.status)">
                  {{ resolveTemplateStatusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="字段定义" min-width="260">
              <template #default="{ row }">
                <span>{{ summarizeFieldSchema(row.fieldSchemaJson) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="启用时间" width="180">
              <template #default="{ row }">
                {{ formatDateTime(row.activeAt) }}
              </template>
            </el-table-column>
            <el-table-column label="最近更新时间" width="180">
              <template #default="{ row }">
                {{ formatDateTime(row.updateTime) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <div class="edhr-form-page__actions">
                  <el-button
                    v-hasPermi="['mes:pro-edhr-form-template:activate']"
                    link
                    type="primary"
                    :disabled="row.status === EDHR_FORM_TEMPLATE_STATUS_ACTIVE"
                    @click="submitActivateTemplate(row)"
                  >
                    启用
                  </el-button>
                  <el-button link type="primary" @click="openEventDrawer(row)">事件</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
          <Pagination
            :total="templateTotal"
            v-model:page="templateQueryParams.pageNo"
            v-model:limit="templateQueryParams.pageSize"
            @pagination="loadTemplateList"
          />
        </div>
      </div>
    </div>
  </ContentWrap>

  <Dialog title="创建模板" v-model="templateDialogVisible" width="960px">
    <el-alert v-if="templateError"
      :title="templateError"
      type="error"
      :closable="false"
      show-icon
      class="edhr-form-page__dialog-alert"
    />
    <el-form label-width="96px">
      <el-row :gutter="16">
        <el-col :span="8">
          <el-form-item label="模板编码" required>
            <el-input v-model="templateForm.templateCode" maxlength="64" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="模板名称" required>
            <el-input v-model="templateForm.templateName" maxlength="128" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="模板版本" required>
            <el-input v-model="templateForm.templateVersion" maxlength="32" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="备注">
        <el-input v-model="templateForm.remark" type="textarea" :rows="2" maxlength="500" />
      </el-form-item>
      <el-form-item label="字段定义" required>
        <div class="edhr-form-page__field-editor">
          <el-table :data="templateFieldRows" border size="small" row-key="key">
            <el-table-column label="字段键" min-width="150">
              <template #default="{ row }">
                <el-input v-model="row.key" maxlength="64" />
              </template>
            </el-table-column>
            <el-table-column label="字段名称" min-width="150">
              <template #default="{ row }">
                <el-input v-model="row.label" maxlength="80" />
              </template>
            </el-table-column>
            <el-table-column label="类型" width="130">
              <template #default="{ row }">
                <el-select v-model="row.type">
                  <el-option label="文本" value="text" />
                  <el-option label="数字" value="number" />
                  <el-option label="枚举" value="enum" />
                  <el-option label="日期" value="date" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="必填" width="80" align="center">
              <template #default="{ row }">
                <el-checkbox v-model="row.required" />
              </template>
            </el-table-column>
            <el-table-column label="最小值" width="120">
              <template #default="{ row }">
                <el-input v-model="row.min" :disabled="row.type !== 'number'" />
              </template>
            </el-table-column>
            <el-table-column label="最大值" width="120">
              <template #default="{ row }">
                <el-input v-model="row.max" :disabled="row.type !== 'number'" />
              </template>
            </el-table-column>
            <el-table-column label="枚举选项" min-width="180">
              <template #default="{ row }">
                <el-input v-model="row.optionsText" :disabled="row.type !== 'enum'" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80" align="center">
              <template #default="{ $index }">
                <el-button link type="danger" @click="removeTemplateFieldRow($index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-button class="edhr-form-page__field-add" @click="addTemplateFieldRow">添加字段</el-button>
        </div>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="templateDialogVisible = false">取 消</el-button>
      <el-button type="primary" :loading="templateSubmitting" @click="submitTemplate">确 认</el-button>
    </template>
  </Dialog>

  <Dialog title="创建实例" v-model="instanceDialogVisible" width="620px">
    <el-alert v-if="instanceError"
      :title="instanceError"
      type="error"
      :closable="false"
      show-icon
      class="edhr-form-page__dialog-alert"
    />
    <el-form label-width="110px">
      <el-form-item label="表单模板" required>
        <el-select
          v-model="instanceForm.templateId"
          filterable
          clearable
          :loading="activeTemplateLoading"
          class="edhr-form-page__full"
        >
          <el-option
            v-for="template in activeTemplateOptions"
            :key="template.id"
            :label="resolveTemplateOptionLabel(template)"
            :value="template.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="业务范围">
        <el-input v-model="instanceForm.businessScope" maxlength="64" />
      </el-form-item>
      <el-form-item label="业务对象类型">
        <el-input v-model="instanceForm.businessObjectType" maxlength="64" />
      </el-form-item>
      <el-form-item label="业务对象ID">
        <el-input-number
          v-model="instanceForm.businessObjectId"
          :min="1"
          :controls="false"
          class="edhr-form-page__full"
        />
      </el-form-item>
      <el-form-item label="业务对象编码">
        <el-input v-model="instanceForm.businessObjectCode" maxlength="64" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="instanceForm.remark" type="textarea" :rows="3" maxlength="500" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="instanceDialogVisible = false">取 消</el-button>
      <el-button type="primary" :loading="instanceSubmitting" @click="submitCreateInstance">确 认</el-button>
    </template>
  </Dialog>

  <el-drawer
    v-model="detailDrawerVisible"
    title="独立表单实例详情"
    size="760px"
    destroy-on-close
  >
    <el-alert v-if="detailError"
      :title="detailError"
      type="error"
      :closable="false"
      show-icon
      class="edhr-form-page__dialog-alert"
    />
    <div v-loading="detailLoading" class="edhr-form-page__detail">
      <template v-if="detailInstance">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="实例编码">
            {{ detailInstance.instanceCode || '--' }}
          </el-descriptions-item>
          <el-descriptions-item label="表单状态">
            <el-tag :type="resolveInstanceStatusType(detailInstance.status)">
              {{ resolveInstanceStatusLabel(detailInstance.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="模板编码">
            {{ detailInstance.templateCode || '--' }}
          </el-descriptions-item>
          <el-descriptions-item label="模板版本">
            {{ detailInstance.templateVersion || '--' }}
          </el-descriptions-item>
          <el-descriptions-item label="业务编码">
            {{ detailInstance.businessObjectCode || '--' }}
          </el-descriptions-item>
          <el-descriptions-item label="提交时间">
            {{ formatDateTime(detailInstance.submittedAt) }}
          </el-descriptions-item>
        </el-descriptions>

        <el-form label-width="112px" class="edhr-form-page__value-form">
          <el-form-item
            v-for="field in detailFieldSchema"
            :key="field.key"
            :label="field.label"
            :required="field.required"
          >
            <el-input-number
              v-if="field.type === 'number'"
              v-model="detailValues[field.key]"
              :min="field.min"
              :max="field.max"
              :controls="false"
              :disabled="isDetailSubmitted"
              class="edhr-form-page__full"
            />
            <el-select
              v-else-if="field.type === 'enum'"
              v-model="detailValues[field.key]"
              :disabled="isDetailSubmitted"
              clearable
              class="edhr-form-page__full"
            >
              <el-option
                v-for="option in field.options || []"
                :key="option"
                :label="option"
                :value="option"
              />
            </el-select>
            <el-date-picker
              v-else-if="field.type === 'date'"
              v-model="detailValues[field.key]"
              type="date"
              value-format="YYYY-MM-DD"
              :disabled="isDetailSubmitted"
              class="edhr-form-page__full"
            />
            <el-input
              v-else
              v-model="detailValues[field.key]"
              :disabled="isDetailSubmitted"
              maxlength="1000"
            />
          </el-form-item>
          <el-form-item label="备注">
            <el-input
              v-model="detailRemark"
              type="textarea"
              :rows="3"
              :disabled="isDetailSubmitted"
              maxlength="500"
            />
          </el-form-item>
        </el-form>
      </template>
    </div>
    <template #footer>
      <div class="edhr-form-page__drawer-footer">
        <el-button @click="detailDrawerVisible = false">关 闭</el-button>
        <el-button
          v-hasPermi="['mes:pro-edhr-form-instance:save']"
          :disabled="isDetailSubmitted || !detailInstance"
          :loading="detailSubmitting"
          @click="submitSaveDraft"
        >
          保存草稿
        </el-button>
        <el-button
          v-hasPermi="['mes:pro-edhr-form-instance:submit']"
          type="primary"
          :disabled="isDetailSubmitted || !detailInstance"
          :loading="detailSubmitting"
          @click="submitForm"
        >
          提交表单
        </el-button>
      </div>
    </template>
  </el-drawer>

  <el-drawer v-model="eventDrawerVisible" title="事件" size="720px" destroy-on-close>
    <el-alert
      v-if="eventError"
      :title="eventError"
      type="error"
      :closable="false"
      show-icon
      class="edhr-form-page__dialog-alert"
    />
    <el-table v-loading="eventLoading" :data="eventList" stripe :show-overflow-tooltip="true">
      <el-table-column label="事件类型" prop="eventType" min-width="150" />
      <el-table-column label="结果" prop="resultStatus" width="110" />
      <el-table-column label="操作者" prop="operatorUsername" min-width="130" />
      <el-table-column label="发生时间" width="180">
        <template #default="{ row }">
          {{ formatDateTime(row.occurredAt) }}
        </template>
      </el-table-column>
      <el-table-column label="失败原因" prop="failureReason" min-width="180" />
    </el-table>
    <Pagination
      :total="eventTotal"
      v-model:page="eventQueryParams.pageNo"
      v-model:limit="eventQueryParams.pageSize"
      @pagination="loadEventList"
    />
  </el-drawer>
</template>

<script setup lang="ts">
import dayjs from 'dayjs'
import {
  EDHR_FORM_INSTANCE_STATUS_DRAFT,
  EDHR_FORM_INSTANCE_STATUS_SUBMITTED,
  EDHR_FORM_TEMPLATE_STATUS_ACTIVE,
  EDHR_FORM_TEMPLATE_STATUS_DRAFT,
  activateEdhrFormTemplate,
  createEdhrFormInstance,
  createEdhrFormTemplate,
  getEdhrFormEventPage,
  getEdhrFormInstance,
  getEdhrFormInstancePage,
  getEdhrFormTemplatePage,
  saveEdhrFormInstanceDraft,
  submitEdhrFormInstance,
  type EdhrFormEventRespVO,
  type EdhrFormFieldSpec,
  type EdhrFormFieldType,
  type EdhrFormInstanceRespVO,
  type EdhrFormTemplateRespVO
} from '@/api/mes/pro/edhr/form'

defineOptions({ name: 'MesProFeedbackEdhrForm' })

interface TemplateFieldRow {
  key: string
  label: string
  type: EdhrFormFieldType
  required: boolean
  min: string
  max: string
  optionsText: string
}

const message = useMessage()

const formatDateTime = (value?: string | number) => {
  if (value === undefined || value === null || value === '') {
    return '--'
  }
  const normalizedValue = typeof value === 'string' && /^\d{13}$/.test(value) ? Number(value) : value
  const dateValue = dayjs(normalizedValue)
  return dateValue.isValid() ? dateValue.format('YYYY-MM-DD HH:mm:ss') : String(value)
}

const activeTab = ref<'instance' | 'template'>('instance')
const loadError = ref('')
const templateError = ref('')
const instanceError = ref('')
const detailError = ref('')
const eventError = ref('')

const templateLoading = ref(false)
const instanceLoading = ref(false)
const activeTemplateLoading = ref(false)
const templateSubmitting = ref(false)
const instanceSubmitting = ref(false)
const detailLoading = ref(false)
const detailSubmitting = ref(false)
const eventLoading = ref(false)

const templateDialogVisible = ref(false)
const instanceDialogVisible = ref(false)
const detailDrawerVisible = ref(false)
const eventDrawerVisible = ref(false)

const templateList = ref<EdhrFormTemplateRespVO[]>([])
const instanceList = ref<EdhrFormInstanceRespVO[]>([])
const activeTemplateOptions = ref<EdhrFormTemplateRespVO[]>([])
const eventList = ref<EdhrFormEventRespVO[]>([])
const detailInstance = ref<EdhrFormInstanceRespVO>()
const detailFieldSchema = ref<EdhrFormFieldSpec[]>([])
const templateTotal = ref(0)
const instanceTotal = ref(0)
const eventTotal = ref(0)
const detailRemark = ref('')

const detailValues = reactive<Record<string, string | number | null | undefined>>({})

const templateQueryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  templateCode: '',
  templateName: '',
  status: undefined as 'DRAFT' | 'ACTIVE' | undefined
})

const instanceQueryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  instanceCode: '',
  templateCode: '',
  status: undefined as 'DRAFT' | 'SUBMITTED' | undefined,
  businessObjectCode: ''
})

const eventQueryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  instanceId: undefined as number | undefined,
  templateId: undefined as number | undefined
})

const templateForm = reactive({
  templateCode: '',
  templateName: '',
  templateVersion: 'V1.0',
  remark: ''
})

const instanceForm = reactive({
  templateId: undefined as number | undefined,
  businessScope: '',
  businessObjectType: '',
  businessObjectId: undefined as number | undefined,
  businessObjectCode: '',
  remark: ''
})

const buildDefaultTemplateRows = (): TemplateFieldRow[] => [
  {
    key: 'temperature',
    label: '温度',
    type: 'number',
    required: true,
    min: '0',
    max: '100',
    optionsText: ''
  },
  {
    key: 'inspectionResult',
    label: '结果',
    type: 'enum',
    required: true,
    min: '',
    max: '',
    optionsText: '合格,不合格'
  },
  {
    key: 'operatorName',
    label: '操作人',
    type: 'text',
    required: true,
    min: '',
    max: '',
    optionsText: ''
  }
]

const templateFieldRows = ref<TemplateFieldRow[]>(buildDefaultTemplateRows())

const isDetailSubmitted = computed(
  () => detailInstance.value?.status === EDHR_FORM_INSTANCE_STATUS_SUBMITTED
)

const resolveErrorMessage = (error: unknown, fallback: string) => {
  const responseMessage = (error as any)?.response?.data?.msg || (error as any)?.response?.data?.message
  if (typeof responseMessage === 'string' && responseMessage.trim()) return responseMessage
  if (error instanceof Error && error.message.trim()) return error.message
  return fallback
}

const parseOptionalNumber = (value: string, label: string) => {
  if (!value.trim()) return undefined
  const parsed = Number(value.trim())
  if (!Number.isFinite(parsed)) {
    throw new Error(`${label}必须为数字。`)
  }
  return parsed
}

const parseFieldSchema = (fieldSchemaJson?: string) => {
  if (!fieldSchemaJson?.trim()) return []
  const parsed = JSON.parse(fieldSchemaJson)
  if (!Array.isArray(parsed)) {
    throw new Error('字段定义必须为数组。')
  }
  return parsed as EdhrFormFieldSpec[]
}

const summarizeFieldSchema = (fieldSchemaJson?: string) => {
  try {
    const fields = parseFieldSchema(fieldSchemaJson)
    if (!fields.length) return '--'
    return fields.map((field) => `${field.label}:${field.type}`).join('；')
  } catch (error) {
    return resolveErrorMessage(error, '字段定义解析失败。')
  }
}

const buildTemplateQuery = () => ({
  pageNo: templateQueryParams.pageNo,
  pageSize: templateQueryParams.pageSize,
  templateCode: templateQueryParams.templateCode.trim() || undefined,
  templateName: templateQueryParams.templateName.trim() || undefined,
  status: templateQueryParams.status
})

const buildInstanceQuery = () => ({
  pageNo: instanceQueryParams.pageNo,
  pageSize: instanceQueryParams.pageSize,
  instanceCode: instanceQueryParams.instanceCode.trim() || undefined,
  templateCode: instanceQueryParams.templateCode.trim() || undefined,
  status: instanceQueryParams.status,
  businessObjectCode: instanceQueryParams.businessObjectCode.trim() || undefined
})

const loadTemplateList = async () => {
  templateLoading.value = true
  loadError.value = ''
  try {
    const data = await getEdhrFormTemplatePage(buildTemplateQuery())
    templateList.value = data.list || []
    templateTotal.value = data.total || 0
  } catch (error) {
    templateList.value = []
    templateTotal.value = 0
    loadError.value = resolveErrorMessage(error, '表单模板加载失败。')
  } finally {
    templateLoading.value = false
  }
}

const loadInstanceList = async () => {
  instanceLoading.value = true
  loadError.value = ''
  try {
    const data = await getEdhrFormInstancePage(buildInstanceQuery())
    instanceList.value = data.list || []
    instanceTotal.value = data.total || 0
  } catch (error) {
    instanceList.value = []
    instanceTotal.value = 0
    loadError.value = resolveErrorMessage(error, '独立表单实例加载失败。')
  } finally {
    instanceLoading.value = false
  }
}

const loadActiveTemplateOptions = async () => {
  activeTemplateLoading.value = true
  instanceError.value = ''
  try {
    const data = await getEdhrFormTemplatePage({
      pageNo: 1,
      pageSize: 100,
      status: EDHR_FORM_TEMPLATE_STATUS_ACTIVE
    })
    activeTemplateOptions.value = data.list || []
  } catch (error) {
    activeTemplateOptions.value = []
    instanceError.value = resolveErrorMessage(error, '已启用表单模板加载失败。')
  } finally {
    activeTemplateLoading.value = false
  }
}

const loadEventList = async () => {
  eventLoading.value = true
  eventError.value = ''
  try {
    const data = await getEdhrFormEventPage({
      pageNo: eventQueryParams.pageNo,
      pageSize: eventQueryParams.pageSize,
      instanceId: eventQueryParams.instanceId,
      templateId: eventQueryParams.templateId
    })
    eventList.value = data.list || []
    eventTotal.value = data.total || 0
  } catch (error) {
    eventList.value = []
    eventTotal.value = 0
    eventError.value = resolveErrorMessage(error, '事件加载失败。')
  } finally {
    eventLoading.value = false
  }
}

const handleTabChange = async (name: string | number) => {
  loadError.value = ''
  if (name === 'template') {
    await loadTemplateList()
  } else {
    await loadInstanceList()
  }
}

const handleTemplateQuery = () => {
  templateQueryParams.pageNo = 1
  loadTemplateList()
}

const resetTemplateQuery = () => {
  templateQueryParams.pageNo = 1
  templateQueryParams.pageSize = 10
  templateQueryParams.templateCode = ''
  templateQueryParams.templateName = ''
  templateQueryParams.status = undefined
  loadTemplateList()
}

const handleInstanceQuery = () => {
  instanceQueryParams.pageNo = 1
  loadInstanceList()
}

const resetInstanceQuery = () => {
  instanceQueryParams.pageNo = 1
  instanceQueryParams.pageSize = 10
  instanceQueryParams.instanceCode = ''
  instanceQueryParams.templateCode = ''
  instanceQueryParams.status = undefined
  instanceQueryParams.businessObjectCode = ''
  loadInstanceList()
}

const resetTemplateForm = () => {
  templateForm.templateCode = ''
  templateForm.templateName = ''
  templateForm.templateVersion = 'V1.0'
  templateForm.remark = ''
  templateFieldRows.value = buildDefaultTemplateRows()
}

const resetInstanceForm = () => {
  instanceForm.templateId = undefined
  instanceForm.businessScope = ''
  instanceForm.businessObjectType = ''
  instanceForm.businessObjectId = undefined
  instanceForm.businessObjectCode = ''
  instanceForm.remark = ''
}

const openTemplateDialog = () => {
  templateError.value = ''
  resetTemplateForm()
  templateDialogVisible.value = true
}

const openInstanceDialog = async () => {
  instanceError.value = ''
  resetInstanceForm()
  instanceDialogVisible.value = true
  await loadActiveTemplateOptions()
}

const addTemplateFieldRow = () => {
  templateFieldRows.value.push({
    key: '',
    label: '',
    type: 'text',
    required: false,
    min: '',
    max: '',
    optionsText: ''
  })
}

const removeTemplateFieldRow = (index: number) => {
  if (templateFieldRows.value.length === 1) {
    templateError.value = '至少保留一个字段定义。'
    return
  }
  templateFieldRows.value.splice(index, 1)
}

const buildFieldSchemaJson = () => {
  const usedKeys = new Set<string>()
  const fields = templateFieldRows.value.map((row, index) => {
    const key = row.key.trim()
    const label = row.label.trim()
    if (!key || !label || !row.type) {
      throw new Error(`第 ${index + 1} 行字段键、字段名称、类型必填。`)
    }
    if (usedKeys.has(key)) {
      throw new Error(`字段键 ${key} 重复。`)
    }
    usedKeys.add(key)
    const field: EdhrFormFieldSpec = {
      key,
      label,
      type: row.type,
      required: row.required
    }
    if (row.type === 'number') {
      field.min = parseOptionalNumber(row.min, `${label}最小值`)
      field.max = parseOptionalNumber(row.max, `${label}最大值`)
    }
    if (row.type === 'enum') {
      const options = row.optionsText
        .split(',')
        .map((option) => option.trim())
        .filter(Boolean)
      if (!options.length) {
        throw new Error(`${label}必须维护枚举选项。`)
      }
      field.options = options
    }
    return field
  })
  if (!fields.length) {
    throw new Error('字段定义不能为空。')
  }
  return JSON.stringify(fields)
}

const submitTemplate = async () => {
  templateSubmitting.value = true
  templateError.value = ''
  try {
    if (!templateForm.templateCode.trim()) throw new Error('模板编码不能为空。')
    if (!templateForm.templateName.trim()) throw new Error('模板名称不能为空。')
    if (!templateForm.templateVersion.trim()) throw new Error('模板版本不能为空。')
    await createEdhrFormTemplate({
      templateCode: templateForm.templateCode.trim(),
      templateName: templateForm.templateName.trim(),
      templateVersion: templateForm.templateVersion.trim(),
      fieldSchemaJson: buildFieldSchemaJson(),
      remark: templateForm.remark.trim() || undefined
    })
    templateDialogVisible.value = false
    message.success('表单模板已创建')
    await loadTemplateList()
  } catch (error) {
    templateError.value = resolveErrorMessage(error, '表单模板创建失败。')
    message.error(resolveErrorMessage(error, '表单模板创建失败。'))
  } finally {
    templateSubmitting.value = false
  }
}

const submitActivateTemplate = async (row: EdhrFormTemplateRespVO) => {
  templateError.value = ''
  try {
    await activateEdhrFormTemplate({ id: row.id })
    message.success('表单模板已启用')
    await loadTemplateList()
  } catch (error) {
    loadError.value = resolveErrorMessage(error, '表单模板启用失败。')
    message.error(resolveErrorMessage(error, '表单模板启用失败。'))
  }
}

const submitCreateInstance = async () => {
  instanceSubmitting.value = true
  instanceError.value = ''
  try {
    if (instanceForm.templateId == null) throw new Error('请选择已启用表单模板。')
    const result = await createEdhrFormInstance({
      templateId: instanceForm.templateId,
      businessScope: instanceForm.businessScope.trim() || undefined,
      businessObjectType: instanceForm.businessObjectType.trim() || undefined,
      businessObjectId: instanceForm.businessObjectId,
      businessObjectCode: instanceForm.businessObjectCode.trim() || undefined,
      remark: instanceForm.remark.trim() || undefined
    })
    instanceDialogVisible.value = false
    message.success('独立表单实例已创建')
    await loadInstanceList()
    await openDetailDrawer(result)
  } catch (error) {
    instanceError.value = resolveErrorMessage(error, '独立表单实例创建失败。')
    message.error(resolveErrorMessage(error, '独立表单实例创建失败。'))
  } finally {
    instanceSubmitting.value = false
  }
}

const clearDetailValues = () => {
  for (const key of Object.keys(detailValues)) {
    delete detailValues[key]
  }
}

const applyDetailInstance = (instance: EdhrFormInstanceRespVO) => {
  detailInstance.value = instance
  detailFieldSchema.value = parseFieldSchema(instance.fieldSchemaJson)
  detailRemark.value = instance.remark || ''
  clearDetailValues()
  for (const field of detailFieldSchema.value) {
    const value = instance.values?.[field.key]
    if (field.type === 'number' && value != null && String(value).trim()) {
      detailValues[field.key] = Number(value)
    } else {
      detailValues[field.key] = value == null ? undefined : String(value)
    }
  }
}

const openDetailDrawer = async (row: EdhrFormInstanceRespVO) => {
  detailDrawerVisible.value = true
  detailLoading.value = true
  detailError.value = ''
  detailInstance.value = undefined
  clearDetailValues()
  try {
    const data = await getEdhrFormInstance(row.id)
    applyDetailInstance(data)
  } catch (error) {
    detailError.value = resolveErrorMessage(error, '独立表单实例详情加载失败。')
  } finally {
    detailLoading.value = false
  }
}

const buildCurrentValues = () => {
  const values: Record<string, unknown> = {}
  for (const field of detailFieldSchema.value) {
    values[field.key] = detailValues[field.key]
  }
  return values
}

const submitSaveDraft = async () => {
  detailSubmitting.value = true
  detailError.value = ''
  try {
    if (!detailInstance.value?.id) throw new Error('请先打开有效表单实例。')
    const data = await saveEdhrFormInstanceDraft({
      id: detailInstance.value.id,
      values: buildCurrentValues(),
      remark: detailRemark.value.trim() || undefined
    })
    applyDetailInstance(data)
    message.success('草稿已保存')
    await loadInstanceList()
  } catch (error) {
    detailError.value = resolveErrorMessage(error, '表单草稿保存失败。')
    message.error(resolveErrorMessage(error, '表单草稿保存失败。'))
  } finally {
    detailSubmitting.value = false
  }
}

const submitForm = async () => {
  detailSubmitting.value = true
  detailError.value = ''
  try {
    if (!detailInstance.value?.id) throw new Error('请先打开有效表单实例。')
    const data = await submitEdhrFormInstance({
      id: detailInstance.value.id,
      values: buildCurrentValues(),
      remark: detailRemark.value.trim() || undefined
    })
    applyDetailInstance(data)
    message.success('表单已提交')
    await loadInstanceList()
  } catch (error) {
    detailError.value = resolveErrorMessage(error, '表单提交失败。')
    message.error(resolveErrorMessage(error, '表单提交失败。'))
  } finally {
    detailSubmitting.value = false
  }
}

const isFormInstanceRow = (
  row: EdhrFormInstanceRespVO | EdhrFormTemplateRespVO
): row is EdhrFormInstanceRespVO => {
  return 'instanceCode' in row || 'templateId' in row
}

const openEventDrawer = async (row: EdhrFormInstanceRespVO | EdhrFormTemplateRespVO) => {
  eventDrawerVisible.value = true
  eventQueryParams.pageNo = 1
  eventQueryParams.pageSize = 10
  if (isFormInstanceRow(row)) {
    eventQueryParams.instanceId = row.id
    eventQueryParams.templateId = row.templateId
  } else {
    eventQueryParams.instanceId = undefined
    eventQueryParams.templateId = row.id
  }
  await loadEventList()
}

const resolveTemplateOptionLabel = (template: EdhrFormTemplateRespVO) => {
  return [template.templateCode, template.templateName, template.templateVersion].filter(Boolean).join(' / ')
}

const resolveTemplateStatusLabel = (status?: string) => {
  if (status === EDHR_FORM_TEMPLATE_STATUS_DRAFT) return '草稿'
  if (status === EDHR_FORM_TEMPLATE_STATUS_ACTIVE) return '已启用'
  return status || '--'
}

const resolveTemplateStatusType = (status?: string) => {
  if (status === EDHR_FORM_TEMPLATE_STATUS_ACTIVE) return 'success'
  if (status === EDHR_FORM_TEMPLATE_STATUS_DRAFT) return 'info'
  return 'warning'
}

const resolveInstanceStatusLabel = (status?: string) => {
  if (status === EDHR_FORM_INSTANCE_STATUS_DRAFT) return '草稿'
  if (status === EDHR_FORM_INSTANCE_STATUS_SUBMITTED) return '已提交'
  return status || '--'
}

const resolveInstanceStatusType = (status?: string) => {
  if (status === EDHR_FORM_INSTANCE_STATUS_SUBMITTED) return 'success'
  if (status === EDHR_FORM_INSTANCE_STATUS_DRAFT) return 'info'
  return 'warning'
}

onMounted(loadInstanceList)
</script>

<style scoped>
.edhr-form-page {
  display: flex;
  flex-direction: column;
}

.edhr-form-page__tabs {
  border: 1px solid #dbe3ef;
  border-radius: 8px 8px 0 0;
  background: #ffffff;
  padding: 0 16px;
}

.edhr-form-page__panel,
.edhr-form-page__toolbar,
.edhr-form-page__table {
  background: #ffffff;
}

.edhr-form-page__panel {
  border: 1px solid #dbe3ef;
  border-top: 0;
  border-radius: 0 0 8px 8px;
}

.edhr-form-page__toolbar {
  padding: 16px 16px 0;
  border-bottom: 1px solid #edf1f6;
}

.edhr-form-page__table {
  padding: 16px;
}

.edhr-form-page__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.edhr-form-page__dialog-alert {
  margin-bottom: 12px;
}

.edhr-form-page__field-editor {
  width: 100%;
}

.edhr-form-page__field-add {
  margin-top: 10px;
}

.edhr-form-page__full {
  width: 100%;
}

.edhr-form-page__detail {
  min-height: 360px;
}

.edhr-form-page__value-form {
  margin-top: 18px;
}

.edhr-form-page__drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
