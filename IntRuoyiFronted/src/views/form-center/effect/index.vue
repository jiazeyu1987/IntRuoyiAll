<template>
  <ContentWrap>
    <UnifiedListTemplate
      table-key="form.center.effect"
      :query-model="queryParams"
      label-width="88px"
      :filter-definitions="quickFilterDefinitions"
      :show-quick-filter-label="false"
      :quick-filter-state="quickFilter.state"
      :selected-filter-definition="quickFilter.selectedDefinition.value"
      :operator-options="quickFilter.operatorOptions.value"
      :columns="effectColumns"
      :column-saving="effectColumnSaving"
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @update:quick-filter-state="quickFilter.updateState"
      @quick-filter-query="handleQuery"
      @column-change="saveEffectColumnConfig"
      @column-reset="resetEffectColumnConfig"
      @pagination="getList"
    >
      <template #actions>
        <el-button @click="resetQuery">
          <Icon class="mr-5px" icon="ep:refresh" />
          重置
        </el-button>
        <el-button type="primary" @click="getList">
          <Icon class="mr-5px" icon="ep:search" />
          查询
        </el-button>
      </template>

      <template #table="{ sortColumnAttrs, handleSortChange }">
        <el-table
          v-loading="loading"
          :data="list"
          border
          :stripe="true"
          :show-overflow-tooltip="true"
          data-user-table-column-explicit
          data-user-table-key="form.center.effect"
          @header-dragend="handleEffectHeaderDragend"
          @sort-change="handleSortChange"
        >
          <el-table-column
            v-if="isEffectColumnVisible('instanceId')"
            align="center"
            label="实例ID"
            prop="instanceId"
            :width="getEffectColumnWidthString('instanceId', 120)"
            v-bind="sortColumnAttrs('instanceId')"
          />
          <el-table-column
            v-if="isEffectColumnVisible('executionCode')"
            align="center"
            label="执行编码"
            prop="executionCode"
            :min-width="getEffectColumnMinWidthString('executionCode', 180)"
            v-bind="sortColumnAttrs('executionCode')"
          />
          <el-table-column
            v-if="isEffectColumnVisible('status')"
            align="center"
            label="状态"
            prop="status"
            :width="getEffectColumnWidthString('status', 150)"
            v-bind="sortColumnAttrs('status')"
          >
            <template #default="{ row }">
              <el-tag :type="row.status === 'FAILED_PENDING' ? 'danger' : 'success'">
                {{ row.status === 'FAILED_PENDING' ? '生效失败待处理' : '已生效' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column
            v-if="isEffectColumnVisible('failureReason')"
            align="center"
            label="失败原因"
            prop="failureReason"
            :min-width="getEffectColumnMinWidthString('failureReason', 240)"
            v-bind="sortColumnAttrs('failureReason')"
          />
          <el-table-column
            v-if="isEffectColumnVisible('idempotencyKey')"
            align="center"
            label="幂等键"
            prop="idempotencyKey"
            :min-width="getEffectColumnMinWidthString('idempotencyKey', 180)"
            v-bind="sortColumnAttrs('idempotencyKey')"
          />
          <el-table-column
            v-if="isEffectColumnVisible('actions')"
            align="center"
            fixed="right"
            label="操作"
            prop="actions"
            :width="getEffectColumnWidthString('actions', 120)"
          >
            <template #default="{ row }">
              <el-button
                v-hasPermi="['form:effect:retry']"
                link
                type="primary"
                @click="handleRetry(row)"
              >
                重试
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </UnifiedListTemplate>
  </ContentWrap>
</template>

<script setup lang="ts">
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import {
  getPendingEffects,
  retryEffect,
  type FormEffectExecutionVO,
  type FormEffectPendingPageReqVO
} from '@/api/form-center/instance'
import { useUserTableColumns, type UserTableColumnDefinition } from '@/hooks/web/useUserTableColumns'
import { useTableQuickFilter, type TableQuickFilterDefinition } from '@/hooks/web/useTableQuickFilter'

defineOptions({ name: 'FormCenterEffectPending' })

const message = useMessage()
const loading = ref(false)
const total = ref(0)
const list = ref<FormEffectExecutionVO[]>([])
const queryParams = reactive<FormEffectPendingPageReqVO & { pageNo: number; pageSize: number }>({
  pageNo: 1,
  pageSize: 10,
  instanceId: undefined
})

const effectDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'instanceId', label: '实例ID', width: 120 },
  { key: 'executionCode', label: '执行编码', minWidth: 180 },
  { key: 'status', label: '状态', width: 150 },
  { key: 'failureReason', label: '失败原因', minWidth: 240 },
  { key: 'idempotencyKey', label: '幂等键', minWidth: 180 },
  { key: 'actions', label: '操作', width: 120, hideable: false, business: false }
]

const {
  columns: effectColumns,
  saving: effectColumnSaving,
  isColumnVisible: isEffectColumnVisible,
  getColumnWidthString: getEffectColumnWidthString,
  getColumnMinWidthString: getEffectColumnMinWidthString,
  handleHeaderDragend: handleEffectHeaderDragend,
  saveConfig: saveEffectColumnConfig,
  resetConfig: resetEffectColumnConfig
} = useUserTableColumns('form.center.effect', effectDefaultColumns)

const quickFilterDefinitions = computed<TableQuickFilterDefinition[]>(() => [
  {
    key: 'instanceId',
    label: '实例ID',
    type: 'text',
    queryParamKey: 'instanceId',
    placeholder: '请输入实例ID'
  }
])

const getList = async () => {
  loading.value = true
  try {
    const data = await getPendingEffects(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const quickFilter = useTableQuickFilter('form.center.effect', quickFilterDefinitions, queryParams, handleQuery)

const resetQuery = () => {
  queryParams.instanceId = undefined
  quickFilter.resetQuickFilter()
}

const handleRetry = async (row: FormEffectExecutionVO) => {
  await retryEffect(row.instanceId)
  message.success('已提交重试')
  await getList()
}

onMounted(getList)
</script>
