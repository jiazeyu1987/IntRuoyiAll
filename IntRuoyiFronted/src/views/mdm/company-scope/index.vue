<template>
  <ContentWrap
    class="scheme-d-basic-data-page scheme-d-basic-data-page--mdm-company-scope"
    data-testid="mdm-company-scope-page"
  >
    <div class="mb-16px flex items-center gap-8px">
      <span class="text-18px font-600 text-[var(--el-text-color-primary)]">基础数据 / 授权公司</span>
    </div>
    <el-alert
      v-if="loadError"
      class="company-scope-load-error"
      type="error"
      :closable="false"
      :title="loadError"
    />
    <UnifiedListTemplate
      table-key="mdm.companyScope.main"
      :query-model="queryParams"
      label-width="86px"
      query-form-test-id="mdm-company-scope-filter-form"
      :filter-definitions="companyScopeQuickFilterDefinitions"
      :quick-filter-state="companyScopeQuickFilter.state"
      :selected-filter-definition="companyScopeQuickFilter.selectedDefinition.value"
      :operator-options="companyScopeQuickFilter.operatorOptions.value"
      :columns="companyScopeColumns"
      :column-saving="companyScopeColumnSaving"
      :show-column-reset="false"
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @update:quick-filter-state="companyScopeQuickFilter.updateState"
      @quick-filter-query="companyScopeQuickFilter.applyQuickFilter"
      @column-change="saveCompanyScopeColumnConfig"
      @pagination="getList"
    >
      <template #actions>
        <el-button
          type="primary"
          @click="openScopeForm('create')"
          v-hasPermi="['mdm:company-scope:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" />
          新增授权公司
        </el-button>
      </template>
      <template #table="{ sortColumnAttrs, handleSortChange: handleTemplateSortChange }">
        <el-table
          v-loading="loading"
          data-user-table-column-explicit
          data-user-table-key="mdm.companyScope.main"
          :data="list"
          border
          :empty-text="companyScopeEmptyText"
          :show-overflow-tooltip="true"
          :row-key="getCompanyScopeRowKey"
          @sort-change="handleTemplateSortChange"
        >
          <el-table-column
            v-if="isCompanyScopeColumnVisible('scopeType')"
            label="范围类型"
            prop="scopeType"
            :width="getCompanyScopeColumnWidthString('scopeType', 100)"
            v-bind="sortColumnAttrs('scopeType')"
          >
            <template #default="{ row }">
              <el-tag :type="row.scopeType === MdmCompanyScopeApi.MDM_COMPANY_SCOPE_TYPE_USER ? 'primary' : 'warning'">
                {{ formatScopeType(row.scopeType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isCompanyScopeColumnVisible('principalName')"
            label="用户 / 角色"
            prop="principalName"
            :min-width="getCompanyScopeColumnMinWidthString('principalName', 160)"
            v-bind="sortColumnAttrs('principalName')"
          />
          <el-table-column
            v-if="isCompanyScopeColumnVisible('principalCode')"
            label="账号 / 编码"
            prop="principalCode"
            :min-width="getCompanyScopeColumnMinWidthString('principalCode', 160)"
            v-bind="sortColumnAttrs('principalCode')"
          />
          <el-table-column
            v-if="isCompanyScopeColumnVisible('companyName')"
            label="公司名称"
            prop="companyName"
            :min-width="getCompanyScopeColumnMinWidthString('companyName', 200)"
            v-bind="sortColumnAttrs('companyName')"
          />
          <el-table-column
            v-if="isCompanyScopeColumnVisible('companyCode')"
            label="公司编码"
            prop="companyCode"
            :min-width="getCompanyScopeColumnMinWidthString('companyCode', 160)"
            v-bind="sortColumnAttrs('companyCode')"
          />
          <el-table-column
            v-if="isCompanyScopeColumnVisible('status')"
            label="状态"
            prop="status"
            :width="getCompanyScopeColumnWidthString('status', 100)"
            v-bind="sortColumnAttrs('status')"
          >
            <template #default="{ row }">
              <el-tag :type="row.status === MdmCompanyScopeApi.MDM_COMPANY_SCOPE_STATUS_ENABLE ? 'success' : 'info'">
                {{ formatStatus(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isCompanyScopeColumnVisible('updateTime')"
            label="更新时间"
            prop="updateTime"
            :width="getCompanyScopeColumnWidthString('updateTime', 180)"
            :formatter="dateFormatter2"
            v-bind="sortColumnAttrs('updateTime')"
          />
          <el-table-column
            v-if="isCompanyScopeColumnVisible('actions')"
            label="操作"
            fixed="right"
            :width="getCompanyScopeColumnWidthString('actions', 150)"
          >
            <template #default="{ row }">
              <el-button
                link
                type="primary"
                @click="openScopeForm('update', row)"
                v-hasPermi="['mdm:company-scope:update']"
              >
                编辑
              </el-button>
              <el-button
                link
                type="danger"
                @click="handleDelete(row)"
                v-hasPermi="['mdm:company-scope:delete']"
              >
                删除授权公司
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </UnifiedListTemplate>
  </ContentWrap>

  <el-dialog
    v-model="formVisible"
    :title="formType === 'create' ? '新增授权公司' : '编辑授权公司'"
    width="640px"
  >
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="112px">
      <el-form-item label="授权类型" prop="scopeType">
        <el-radio-group
          v-model="formData.scopeType"
          :disabled="formType === 'update'"
          @change="handleScopeTypeChange"
        >
          <el-radio-button :label="MdmCompanyScopeApi.MDM_COMPANY_SCOPE_TYPE_USER">用户</el-radio-button>
          <el-radio-button :label="MdmCompanyScopeApi.MDM_COMPANY_SCOPE_TYPE_ROLE">角色</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item
        v-if="formData.scopeType === MdmCompanyScopeApi.MDM_COMPANY_SCOPE_TYPE_USER"
        label="授权用户"
        prop="principalId"
      >
        <UserSelectV2 v-model="formData.principalId" placeholder="请选择授权用户" />
      </el-form-item>
      <el-form-item v-else label="授权角色" prop="principalId">
        <RoleSelect v-model="formData.principalId" placeholder="请选择授权角色" />
      </el-form-item>
      <el-form-item label="授权公司" prop="companyId">
        <el-select
          v-model="formData.companyId"
          class="!w-1/1"
          filterable
          clearable
          :loading="ownedCompanyLoading"
          placeholder="请选择授权公司"
        >
          <el-option
            v-for="item in ownedCompanyOptions"
            :key="item.id"
            :label="formatOwnedCompanyOption(item)"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="formData.status">
          <el-radio-button :label="MdmCompanyScopeApi.MDM_COMPANY_SCOPE_STATUS_ENABLE">启用</el-radio-button>
          <el-radio-button :label="MdmCompanyScopeApi.MDM_COMPANY_SCOPE_STATUS_DISABLE">停用</el-radio-button>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="company-scope-dialog-footer">
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="formSubmitting" @click="submitScopeForm">保存</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { dateFormatter2 } from '@/utils/formatTime'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { useTableQuickFilter, type TableQuickFilterDefinition } from '@/hooks/web/useTableQuickFilter'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import UserSelectV2 from '@/views/system/user/components/UserSelectV2.vue'
import RoleSelect from '@/views/system/role/components/RoleSelect.vue'
import * as MdmEnterpriseApi from '@/api/mdm/enterprise'
import type { MdmEnterpriseSimpleRespVO } from '@/api/mdm/enterprise'
import * as MdmCompanyScopeApi from '@/api/mdm/companyScope'
import type {
  MdmCompanyScopePageReqVO,
  MdmCompanyScopeRespVO,
  MdmCompanyScopeSaveReqVO
} from '@/api/mdm/companyScope'

defineOptions({ name: 'MdmCompanyScope' })

const message = useMessage()

const loading = ref(false)
const total = ref(0)
const list = ref<MdmCompanyScopeRespVO[]>([])
const loadError = ref('')
const formVisible = ref(false)
const formSubmitting = ref(false)
const formType = ref<'create' | 'update'>('create')
const formRef = ref<FormInstance>()
const ownedCompanyLoading = ref(false)
const ownedCompanyOptions = ref<MdmEnterpriseSimpleRespVO[]>([])

type CompanyScopePageQuery = MdmCompanyScopePageReqVO & {
  pageNo: number
  pageSize: number
}

const queryParams = reactive<CompanyScopePageQuery>({
  pageNo: 1,
  pageSize: 10,
  keyword: undefined,
  scopeType: undefined,
  status: undefined
})

const companyScopeQuickFilterDefinitions: TableQuickFilterDefinition[] = [
  {
    key: 'keyword',
    label: '关键词',
    type: 'text',
    queryParamKey: 'keyword',
    placeholder: '用户、角色或公司'
  },
  {
    key: 'scopeType',
    label: '范围类型',
    type: 'select',
    queryParamKey: 'scopeType',
    options: [
      { label: '用户', value: MdmCompanyScopeApi.MDM_COMPANY_SCOPE_TYPE_USER },
      { label: '角色', value: MdmCompanyScopeApi.MDM_COMPANY_SCOPE_TYPE_ROLE }
    ]
  },
  {
    key: 'status',
    label: '状态',
    type: 'select',
    queryParamKey: 'status',
    options: [
      { label: '启用', value: MdmCompanyScopeApi.MDM_COMPANY_SCOPE_STATUS_ENABLE },
      { label: '停用', value: MdmCompanyScopeApi.MDM_COMPANY_SCOPE_STATUS_DISABLE }
    ]
  }
]

const companyScopeDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'scopeType', label: '范围类型', width: 100, sortable: false },
  { key: 'principalName', label: '用户 / 角色', minWidth: 160, sortable: false },
  { key: 'principalCode', label: '账号 / 编码', minWidth: 160, sortable: false },
  { key: 'companyName', label: '公司名称', minWidth: 200, sortable: false },
  { key: 'companyCode', label: '公司编码', minWidth: 160, sortable: false },
  { key: 'status', label: '状态', width: 100, sortable: false },
  { key: 'updateTime', label: '更新时间', width: 180, sortable: false },
  { key: 'actions', label: '操作', width: 150, sortable: false, hideable: false, business: false }
]

const {
  columns: companyScopeColumns,
  saving: companyScopeColumnSaving,
  isColumnVisible: isCompanyScopeColumnVisible,
  getColumnWidthString: getCompanyScopeColumnWidthString,
  getColumnMinWidthString: getCompanyScopeColumnMinWidthString,
  saveConfig: saveCompanyScopeColumnConfig
} = useUserTableColumns('mdm.companyScope.main', companyScopeDefaultColumns)

const formatScopeType = (scopeType?: string | null) =>
  scopeType === MdmCompanyScopeApi.MDM_COMPANY_SCOPE_TYPE_USER
    ? '用户'
    : scopeType === MdmCompanyScopeApi.MDM_COMPANY_SCOPE_TYPE_ROLE
      ? '角色'
      : scopeType || '-'

const formatStatus = (status?: string | null) =>
  status === MdmCompanyScopeApi.MDM_COMPANY_SCOPE_STATUS_ENABLE ? '启用' : '停用'

type CompanyScopeFormData = Omit<MdmCompanyScopeSaveReqVO, 'principalId' | 'companyId'> & {
  principalId?: number
  companyId?: number
}

const formData = reactive<CompanyScopeFormData>({
  id: undefined,
  scopeType: MdmCompanyScopeApi.MDM_COMPANY_SCOPE_TYPE_USER,
  principalId: undefined,
  companyId: undefined,
  status: MdmCompanyScopeApi.MDM_COMPANY_SCOPE_STATUS_ENABLE
})

const formRules: FormRules = {
  scopeType: [{ required: true, message: '请选择授权类型', trigger: 'change' }],
  principalId: [{ required: true, message: '请选择授权对象', trigger: 'change' }],
  companyId: [{ required: true, message: '请选择授权公司', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

const resolvePageErrorMessage = (error: unknown) => {
  const record = error as {
    message?: string
    msg?: string
    response?: { data?: { msg?: string; message?: string } }
  }
  return (
    record?.response?.data?.msg ||
    record?.response?.data?.message ||
    record?.msg ||
    record?.message ||
    '企业公司范围加载失败，请查看网络或后端错误后重试。'
  )
}

const getList = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const data = await MdmCompanyScopeApi.getCompanyScopePage(queryParams)
    list.value = data.list
    total.value = data.total
  } catch (error) {
    const errorMessage = resolvePageErrorMessage(error)
    loadError.value = errorMessage
    list.value = []
    total.value = 0
    message.error(errorMessage)
  } finally {
    loading.value = false
  }
}

const companyScopeEmptyText = '当前暂无企业公司范围配置'

const resetScopeForm = () => {
  Object.assign(formData, {
    id: undefined,
    scopeType: MdmCompanyScopeApi.MDM_COMPANY_SCOPE_TYPE_USER,
    principalId: undefined,
    companyId: undefined,
    status: MdmCompanyScopeApi.MDM_COMPANY_SCOPE_STATUS_ENABLE
  })
  formRef.value?.clearValidate()
}

const loadOwnedCompanies = async () => {
  ownedCompanyLoading.value = true
  try {
    ownedCompanyOptions.value = await MdmEnterpriseApi.getEnterpriseSimpleList({
      type: MdmEnterpriseApi.MDM_ENTERPRISE_TYPE_OWNED_COMPANY,
      status: MdmEnterpriseApi.MDM_ENTERPRISE_STATUS_ENABLE
    })
  } finally {
    ownedCompanyLoading.value = false
  }
}

const openScopeForm = async (type: 'create' | 'update', row?: MdmCompanyScopeRespVO) => {
  resetScopeForm()
  formType.value = type
  await loadOwnedCompanies()
  if (type === 'update' && row) {
    Object.assign(formData, {
      id: row.id,
      scopeType: row.scopeType,
      principalId: row.principalId,
      companyId: row.companyId,
      status: row.status
    })
  }
  formVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}

const handleScopeTypeChange = () => {
  formData.principalId = undefined
  formRef.value?.clearValidate('principalId')
}

const formatOwnedCompanyOption = (item: MdmEnterpriseSimpleRespVO) => {
  const parts = [item.name, item.enterpriseCode].filter(Boolean)
  return parts.join(' - ')
}

const getCompanyScopeRowKey = (row: MdmCompanyScopeRespVO) => `${row.scopeType}:${row.id}`

const buildScopePayload = (): MdmCompanyScopeSaveReqVO => {
  if (!formData.principalId || !formData.companyId) {
    throw new Error('授权公司表单未完整填写')
  }
  return {
    id: formData.id,
    scopeType: formData.scopeType,
    principalId: formData.principalId,
    companyId: formData.companyId,
    status: formData.status
  }
}

const submitScopeForm = async () => {
  if (!(await formRef.value?.validate())) {
    return
  }
  const payload = buildScopePayload()
  formSubmitting.value = true
  try {
    if (formType.value === 'create') {
      await MdmCompanyScopeApi.createCompanyScope(payload)
    } else {
      await MdmCompanyScopeApi.updateCompanyScope(payload)
    }
    message.success('授权公司已保存')
    formVisible.value = false
    await getList()
  } finally {
    formSubmitting.value = false
  }
}

const handleDelete = async (row: MdmCompanyScopeRespVO) => {
  try {
    await message.delConfirm(`确认删除授权公司“${row.companyName}”吗？`)
  } catch {
    return
  }
  loading.value = true
  try {
    await MdmCompanyScopeApi.deleteCompanyScope(row.scopeType, row.id)
    message.success('授权公司已删除')
    await getList()
  } finally {
    loading.value = false
  }
}

const companyScopeQuickFilter = useTableQuickFilter(
  'mdm.companyScope.main',
  companyScopeQuickFilterDefinitions,
  queryParams,
  getList
)

onMounted(() => {
  void getList()
})
</script>

<style scoped>
.company-scope-load-error {
  margin: 12px;
  width: auto;
}

.company-scope-dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>
