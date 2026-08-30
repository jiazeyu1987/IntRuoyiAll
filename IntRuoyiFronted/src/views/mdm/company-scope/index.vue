<template>
  <ContentWrap
    class="scheme-d-basic-data-page scheme-d-basic-data-page--mdm-company-scope"
    data-testid="mdm-company-scope-page"
  >
    <div class="mb-16px flex items-center gap-8px">
      <span class="text-18px font-600 text-[var(--el-text-color-primary)]">基础数据 / 企业公司范围</span>
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
        </el-table>
      </template>
    </UnifiedListTemplate>
  </ContentWrap>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { dateFormatter2 } from '@/utils/formatTime'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import { useTableQuickFilter, type TableQuickFilterDefinition } from '@/hooks/web/useTableQuickFilter'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import * as MdmCompanyScopeApi from '@/api/mdm/companyScope'
import type {
  MdmCompanyScopePageReqVO,
  MdmCompanyScopeRespVO
} from '@/api/mdm/companyScope'

defineOptions({ name: 'MdmCompanyScope' })

const message = useMessage()

const loading = ref(false)
const total = ref(0)
const list = ref<MdmCompanyScopeRespVO[]>([])
const loadError = ref('')

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
  { key: 'updateTime', label: '更新时间', width: 180, sortable: false }
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
</style>
