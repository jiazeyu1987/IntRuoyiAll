<template>
  <ContentWrap
    class="scheme-d-basic-data-page scheme-d-basic-data-page--mdm-company-scope"
    data-testid="mdm-company-scope-page"
  >
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
          @click="openCreateDialog"
          v-hasPermi="['mdm:company-scope:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" />
          新建
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
          row-key="id"
          @sort-change="handleTemplateSortChange"
        >
          <el-table-column
            v-if="isCompanyScopeColumnVisible('name')"
            label="公司名称"
            prop="name"
            :min-width="getCompanyScopeColumnMinWidthString('name', 200)"
            v-bind="sortColumnAttrs('name')"
          />
          <el-table-column
            v-if="isCompanyScopeColumnVisible('enterpriseCode')"
            label="公司编码"
            prop="enterpriseCode"
            :min-width="getCompanyScopeColumnMinWidthString('enterpriseCode', 160)"
            v-bind="sortColumnAttrs('enterpriseCode')"
          />
          <el-table-column
            v-if="isCompanyScopeColumnVisible('status')"
            label="状态"
            prop="status"
            :width="getCompanyScopeColumnWidthString('status', 100)"
            v-bind="sortColumnAttrs('status')"
          >
            <template #default="{ row }">
              <el-tag :type="row.status === 'ENABLE' ? 'success' : 'info'">
                {{ row.status === 'ENABLE' ? '启用' : '停用' }}
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
                @click="openEditDialog(row)"
                v-hasPermi="['mdm:company-scope:update']"
              >编辑</el-button>
              <el-button
                link
                :type="row.status === 'ENABLE' ? 'warning' : 'success'"
                @click="toggleCompanyStatus(row)"
                v-hasPermi="['mdm:company-scope:update']"
              >{{ row.status === 'ENABLE' ? '停用' : '启用' }}</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </UnifiedListTemplate>
  </ContentWrap>
  <el-dialog
    v-model="formVisible"
    :title="formType === 'create' ? '新建所属公司' : '编辑所属公司'"
    width="520px"
    @closed="resetForm"
  >
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="92px">
      <el-form-item label="公司名称" prop="name">
        <el-input v-model="formData.name" maxlength="255" placeholder="请输入公司名称" />
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="company-scope-dialog-footer">
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="formSubmitting" @click="submitForm">
          保存
        </el-button>
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
import {
  createEnabledCompany,
  getOwnedCompanyPage,
  updateOwnedCompany,
  updateOwnedCompanyStatus
} from '@/api/mdm/companyScope'
import type { MdmEnterpriseRespVO } from '@/api/mdm/enterprise'

defineOptions({ name: 'MdmCompanyScope' })

const message = useMessage()
const loading = ref(false)
const total = ref(0)
const list = ref<MdmEnterpriseRespVO[]>([])
const loadError = ref('')
const formVisible = ref(false)
const formSubmitting = ref(false)
const formType = ref<'create' | 'update'>('create')
const editingCompanyId = ref<number>()
const formRef = ref<FormInstance>()
const formData = reactive({ name: '' })
const formRules: FormRules = {
  name: [{ required: true, message: '请输入公司名称', trigger: 'blur' }]
}
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  keyword: undefined as string | undefined,
  status: undefined as string | undefined
})
const companyScopeQuickFilterDefinitions: TableQuickFilterDefinition[] = [
  {
    key: 'keyword', label: '关键词', type: 'text', queryParamKey: 'keyword',
    placeholder: '公司名称或编码'
  },
  {
    key: 'status', label: '状态', type: 'select', queryParamKey: 'status', options: [
      { label: '启用', value: 'ENABLE' },
      { label: '停用', value: 'DISABLE' }
    ]
  }
]
const companyScopeDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'name', label: '公司名称', minWidth: 200, sortable: false },
  { key: 'enterpriseCode', label: '公司编码', minWidth: 160, sortable: false },
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

const resolvePageErrorMessage = (error: unknown) => {
  const record = error as {
    message?: string
    msg?: string
    response?: { data?: { msg?: string; message?: string } }
  }
  return record?.response?.data?.msg || record?.response?.data?.message || record?.msg ||
    record?.message || '公司列表加载失败，请查看网络或后端错误后重试。'
}
const getList = async () => {
  loading.value = true
  loadError.value = ''
  try {
    const data = await getOwnedCompanyPage(queryParams)
    list.value = data.list
    total.value = data.total
  } catch (error) {
    loadError.value = resolvePageErrorMessage(error)
    list.value = []
    total.value = 0
    message.error(loadError.value)
  } finally {
    loading.value = false
  }
}
const companyScopeEmptyText = '当前暂无所属公司'
const resetForm = () => {
  editingCompanyId.value = undefined
  formData.name = ''
  formRef.value?.clearValidate()
}
const openCreateDialog = async () => {
  resetForm()
  formType.value = 'create'
  formVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}
const openEditDialog = async (row: MdmEnterpriseRespVO) => {
  resetForm()
  formType.value = 'update'
  editingCompanyId.value = row.id
  formData.name = row.name
  formVisible.value = true
  await nextTick()
  formRef.value?.clearValidate()
}
const submitForm = async () => {
  if (!(await formRef.value?.validate())) return
  formSubmitting.value = true
  try {
    if (formType.value === 'create') {
      await createEnabledCompany(formData.name.trim())
      message.success('公司已创建，公司编码已自动生成')
    } else {
      if (!editingCompanyId.value) throw new Error('公司编号缺失')
      await updateOwnedCompany(editingCompanyId.value, formData.name.trim())
      message.success('公司信息已更新')
    }
    formVisible.value = false
    await getList()
  } finally {
    formSubmitting.value = false
  }
}
const toggleCompanyStatus = async (row: MdmEnterpriseRespVO) => {
  const nextStatus = row.status === 'ENABLE' ? 'DISABLE' : 'ENABLE'
  await updateOwnedCompanyStatus(row.id, nextStatus)
  message.success(`公司已${nextStatus === 'ENABLE' ? '启用' : '停用'}`)
  await getList()
}
const companyScopeQuickFilter = useTableQuickFilter(
  'mdm.companyScope.main', companyScopeQuickFilterDefinitions, queryParams, getList
)
onMounted(() => { void getList() })
</script>

<style scoped>
.company-scope-dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.company-scope-load-error {
  margin: 12px;
  width: auto;
}
</style>
