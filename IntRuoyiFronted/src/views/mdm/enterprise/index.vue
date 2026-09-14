<template>
  <ContentWrap class="scheme-d-basic-data-page scheme-d-basic-data-page--mdm-enterprise">
    <div class="mb-16px flex items-center gap-8px">
      <span class="text-18px font-600 text-[var(--el-text-color-primary)]">基础数据 / 关联公司</span>
    </div>
    <UnifiedListTemplate
      table-key="mdm.enterprise.main"
      :query-model="queryParams"
      label-width="86px"
      :filter-definitions="enterpriseQuickFilterDefinitions"
      :quick-filter-state="enterpriseQuickFilter.state"
      :selected-filter-definition="enterpriseQuickFilter.selectedDefinition.value"
      :operator-options="enterpriseQuickFilter.operatorOptions.value"
      :columns="enterpriseColumns"
      :column-saving="enterpriseColumnSaving"
      :show-column-reset="false"
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @update:quick-filter-state="enterpriseQuickFilter.updateState"
      @quick-filter-query="handleQuery"
      @column-change="saveEnterpriseColumnConfig"
      @pagination="getList"
    >
      <template #actions>
        <el-button
          class="scheme-d-btn scheme-d-btn--success"
          type="primary"
          @click="openForm('create')"
          v-hasPermi="['mdm:enterprise:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" />
          新增
        </el-button>
      </template>
      <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
        <el-table
          v-loading="loading"
          class="mdm-enterprise-resizable-table"
          data-user-table-column-explicit
          data-user-table-key="mdm.enterprise.main"
          :data="list"
          border
          :allow-drag-last-column="true"
          :show-overflow-tooltip="true"
          @header-dragend="handleEnterpriseHeaderDragend"
          @sort-change="handleTemplateSortChange"
        >
          <el-table-column
            v-if="isEnterpriseColumnVisible('enterpriseCode')"
            label="公司编码"
            prop="enterpriseCode"
            :width="getEnterpriseColumnWidthString('enterpriseCode')"
            :min-width="getEnterpriseColumnMinWidthString('enterpriseCode', 160)"
            v-bind="sortColumnAttrs('enterpriseCode')"
          >
            <template #default="{ row }">
              <el-link :underline="false" type="primary" @click="openForm('update', row.id)">
                {{ row.enterpriseCode }}
              </el-link>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isEnterpriseColumnVisible('name')"
            label="公司名称"
            prop="name"
            :width="getEnterpriseColumnWidthString('name')"
            :min-width="getEnterpriseColumnMinWidthString('name', 240)"
            v-bind="sortColumnAttrs('name')"
          />
          <el-table-column
            v-if="isEnterpriseColumnVisible('type')"
            label="公司类型"
            prop="type"
            :width="getEnterpriseColumnWidthString('type', 130)"
            v-bind="sortColumnAttrs('type')"
          >
            <template #default="{ row }">
              <el-tag class="scheme-d-tag" :type="row.type === MdmEnterpriseApi.MDM_ENTERPRISE_TYPE_OWNED_COMPANY ? 'success' : 'warning'">
                {{ formatEnterpriseType(row.type) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isEnterpriseColumnVisible('status')"
            label="状态"
            prop="status"
            :width="getEnterpriseColumnWidthString('status', 100)"
            v-bind="sortColumnAttrs('status')"
          >
            <template #default="{ row }">
              <el-tag class="scheme-d-tag" :type="row.status === MdmEnterpriseApi.MDM_ENTERPRISE_STATUS_ENABLE ? 'success' : 'info'">
                {{ formatStatus(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isEnterpriseColumnVisible('updateTime')"
            label="更新时间"
            prop="updateTime"
            :width="getEnterpriseColumnWidthString('updateTime', 180)"
            :formatter="dateFormatter2"
            v-bind="sortColumnAttrs('updateTime')"
          />
          <el-table-column
            v-if="isEnterpriseColumnVisible('actions')"
            label="操作"
            prop="actions"
            fixed="right"
            :width="getEnterpriseColumnWidthString('actions', 230)"
          >
            <template #default="{ row }">
              <el-button
                link
                class="scheme-d-row-action scheme-d-row-action--primary"
                type="primary"
                @click="openForm('update', row.id)"
                v-hasPermi="['mdm:enterprise:update']"
              >
                编辑
              </el-button>
              <el-button
                link
                class="scheme-d-row-action"
                :class="row.status === MdmEnterpriseApi.MDM_ENTERPRISE_STATUS_ENABLE ? 'scheme-d-row-action--warning' : 'scheme-d-row-action--success'"
                :type="row.status === MdmEnterpriseApi.MDM_ENTERPRISE_STATUS_ENABLE ? 'warning' : 'success'"
                @click="handleStatusChange(row)"
                v-hasPermi="['mdm:enterprise:update']"
              >
                {{ row.status === MdmEnterpriseApi.MDM_ENTERPRISE_STATUS_ENABLE ? '停用' : '启用' }}
              </el-button>
              <el-button
                link
                class="scheme-d-row-action scheme-d-row-action--danger"
                type="danger"
                @click="handleDelete(row)"
                v-hasPermi="['mdm:enterprise:delete']"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </UnifiedListTemplate>
  </ContentWrap>

  <el-dialog
    v-model="formVisible"
    class="scheme-d-form-control"
    :title="formType === 'create' ? '新增关联公司' : '编辑关联公司'"
    width="620px"
  >
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="112px">
      <el-form-item label="公司编码" prop="enterpriseCode">
        <el-input v-model="formData.enterpriseCode" maxlength="64" />
      </el-form-item>
      <el-form-item label="公司名称" prop="name">
        <el-input v-model="formData.name" maxlength="255" />
      </el-form-item>
      <el-form-item label="公司类型" prop="type">
        <el-radio-group v-model="formData.type">
          <el-radio-button :label="MdmEnterpriseApi.MDM_ENTERPRISE_TYPE_OWNED_COMPANY">自有公司</el-radio-button>
          <el-radio-button :label="MdmEnterpriseApi.MDM_ENTERPRISE_TYPE_ENTRUSTED_PARTY">受托企业</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="formData.status">
          <el-radio-button :label="MdmEnterpriseApi.MDM_ENTERPRISE_STATUS_ENABLE">启用</el-radio-button>
          <el-radio-button :label="MdmEnterpriseApi.MDM_ENTERPRISE_STATUS_DISABLE">停用</el-radio-button>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="scheme-d-dialog-footer">
        <el-button class="scheme-d-btn scheme-d-btn--neutral" @click="formVisible = false">取消</el-button>
        <el-button
          class="scheme-d-btn scheme-d-btn--success"
          type="primary"
          :loading="formSubmitting"
          @click="submitForm"
        >
          保存
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { isSearchFormInputEmpty } from '@/utils/search'
import type { FormInstance, FormRules } from 'element-plus'
import { dateFormatter2 } from '@/utils/formatTime'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import {
  useTableQuickFilter,
  type TableQuickFilterDefinition
} from '@/hooks/web/useTableQuickFilter'
import { onActivated } from 'vue'
import { useRoute } from 'vue-router'
import * as MdmEnterpriseApi from '@/api/mdm/enterprise'
import type {
  MdmEnterpriseRespVO,
  MdmEnterpriseSaveReqVO
} from '@/api/mdm/enterprise'

defineOptions({ name: 'MdmEnterprise' })

const route = useRoute()
const ENTERPRISE_ROUTE_PATH = '/mdm/enterprise'

const isEnterpriseRoute = () => route.path === ENTERPRISE_ROUTE_PATH

const enterpriseQuickFilterDefinitions: TableQuickFilterDefinition[] = [
  {
    key: 'keyword',
    label: '关键词',
    type: 'text',
    queryParamKey: 'keyword',
    placeholder: '公司编码、公司名称'
  },
  {
    key: 'type',
    label: '公司类型',
    type: 'select',
    queryParamKey: 'type',
    options: [
      { label: '自有公司', value: MdmEnterpriseApi.MDM_ENTERPRISE_TYPE_OWNED_COMPANY },
      { label: '受托企业', value: MdmEnterpriseApi.MDM_ENTERPRISE_TYPE_ENTRUSTED_PARTY }
    ]
  },
  {
    key: 'status',
    label: '状态',
    type: 'select',
    queryParamKey: 'status',
    options: [
      { label: '启用', value: MdmEnterpriseApi.MDM_ENTERPRISE_STATUS_ENABLE },
      { label: '停用', value: MdmEnterpriseApi.MDM_ENTERPRISE_STATUS_DISABLE }
    ]
  }
]

const enterpriseDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'enterpriseCode', label: '公司编码', minWidth: 160 },
  { key: 'name', label: '公司名称', minWidth: 240 },
  { key: 'type', label: '公司类型', width: 130 },
  { key: 'status', label: '状态', width: 100 },
  { key: 'updateTime', label: '更新时间', width: 180 },
  { key: 'actions', label: '操作', width: 230, hideable: false, business: false }
]

const {
  columns: enterpriseColumns,
  saving: enterpriseColumnSaving,
  isColumnVisible: isEnterpriseColumnVisible,
  getColumnWidthString: getEnterpriseColumnWidthString,
  getColumnMinWidthString: getEnterpriseColumnMinWidthString,
  handleHeaderDragend: handleEnterpriseHeaderDragend,
  saveConfig: saveEnterpriseColumnConfig
} = useUserTableColumns('mdm.enterprise.main', enterpriseDefaultColumns)

const message = useMessage()

const loading = ref(false)
const total = ref(0)
const list = ref<MdmEnterpriseRespVO[]>([])
type MdmEnterprisePageQuery = MdmEnterpriseApi.MdmEnterprisePageReqVO & {
  pageNo: number
  pageSize: number
}

const queryParams = reactive<MdmEnterprisePageQuery>({
  pageNo: 1,
  pageSize: 10,
  keyword: undefined,
  enterpriseCode: undefined,
  name: undefined,
  type: undefined,
  status: undefined
})

const formVisible = ref(false)
const formSubmitting = ref(false)
const formType = ref<'create' | 'update'>('create')
const formRef = ref<FormInstance>()
const formData = reactive<MdmEnterpriseSaveReqVO>({
  enterpriseCode: '',
  name: '',
  type: MdmEnterpriseApi.MDM_ENTERPRISE_TYPE_OWNED_COMPANY,
  status: MdmEnterpriseApi.MDM_ENTERPRISE_STATUS_ENABLE
})
const formRules: FormRules = {
  enterpriseCode: [{ required: true, message: '请输入公司编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入公司名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择公司类型', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const formatStatus = (status: string) =>
  status === MdmEnterpriseApi.MDM_ENTERPRISE_STATUS_ENABLE ? '启用' : '停用'

const formatEnterpriseType = (type: string) =>
  ({
    [MdmEnterpriseApi.MDM_ENTERPRISE_TYPE_OWNED_COMPANY]: '自有公司',
    [MdmEnterpriseApi.MDM_ENTERPRISE_TYPE_ENTRUSTED_PARTY]: '受托企业'
  })[type] || type

const getList = async () => {
  loading.value = true
  try {
    const data = await MdmEnterpriseApi.getEnterprisePage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const enterpriseQuickFilter = useTableQuickFilter(
  'mdm.enterprise.main',
  enterpriseQuickFilterDefinitions,
  queryParams,
  getList
)
const enterpriseQuickFilterSearchForm = {
  fields: [{ prop: 'value' }]
}

const handleQuery = async (skipEmptyReset = false) => {
  if (
    skipEmptyReset !== true &&
    isSearchFormInputEmpty(enterpriseQuickFilterSearchForm, {
      value: enterpriseQuickFilter.state.value
    })
  ) {
    await enterpriseQuickFilter.resetQuickFilter()
    return
  }
  await enterpriseQuickFilter.applyQuickFilter()
}

const resetFormData = () => {
  Object.assign(formData, {
    id: undefined,
    enterpriseCode: '',
    name: '',
    type: MdmEnterpriseApi.MDM_ENTERPRISE_TYPE_OWNED_COMPANY,
    status: MdmEnterpriseApi.MDM_ENTERPRISE_STATUS_ENABLE
  })
}

const openForm = async (type: 'create' | 'update', id?: number) => {
  resetFormData()
  formType.value = type
  if (type === 'update' && id) {
    Object.assign(formData, await MdmEnterpriseApi.getEnterprise(id))
  }
  formVisible.value = true
}

const submitForm = async () => {
  if (!(await formRef.value?.validate())) {
    return
  }
  const payload: MdmEnterpriseSaveReqVO = {
    id: formData.id,
    enterpriseCode: formData.enterpriseCode.trim(),
    name: formData.name.trim(),
    type: formData.type,
    status: formData.status
  }
  formSubmitting.value = true
  try {
    if (formType.value === 'create') {
      await MdmEnterpriseApi.createEnterprise(payload)
    } else {
      await MdmEnterpriseApi.updateEnterprise(payload)
    }
    message.success('关联公司已保存')
    formVisible.value = false
    await getList()
  } finally {
    formSubmitting.value = false
  }
}

const handleStatusChange = async (row: MdmEnterpriseRespVO) => {
  const nextStatus =
    row.status === MdmEnterpriseApi.MDM_ENTERPRISE_STATUS_ENABLE
      ? MdmEnterpriseApi.MDM_ENTERPRISE_STATUS_DISABLE
      : MdmEnterpriseApi.MDM_ENTERPRISE_STATUS_ENABLE
  await MdmEnterpriseApi.updateEnterpriseStatus(row.id, nextStatus)
  message.success(`关联公司已${formatStatus(nextStatus)}`)
  await getList()
}

const handleDelete = async (row: MdmEnterpriseRespVO) => {
  try {
    await message.delConfirm(`确认删除关联公司“${row.name || row.enterpriseCode}”吗？`)
  } catch {
    return
  }
  loading.value = true
  try {
    await MdmEnterpriseApi.deleteEnterprise(row.id)
    message.success('关联公司已删除')
    await getList()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void getList()
})

let enterpriseInitialActivationHandled = false

onActivated(async () => {
  if (!isEnterpriseRoute()) {
    return
  }
  if (!enterpriseInitialActivationHandled) {
    enterpriseInitialActivationHandled = true
    return
  }
  await getList()
})

watch(
  () => route.path,
  async () => {
    if (!isEnterpriseRoute()) {
      return
    }
    await getList()
  }
)
</script>

<style scoped>
:deep(.mdm-enterprise-resizable-table .el-table__header-wrapper th.el-table__cell) {
  position: relative;
}

:deep(.mdm-enterprise-resizable-table .el-table__header-wrapper th.el-table__cell::after) {
  position: absolute;
  top: 0;
  right: 0;
  z-index: 2;
  width: 8px;
  height: 100%;
  content: '';
  cursor: col-resize;
  border-right: 2px solid transparent;
}

:deep(.mdm-enterprise-resizable-table .el-table__header-wrapper th.el-table__cell:hover::after) {
  border-right-color: #1677ff;
}
</style>
