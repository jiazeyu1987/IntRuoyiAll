<template>
  <div ref="pageContainer" class="craft-library-page">
    <ContentWrap class="craft-library">
      <el-alert
        v-if="error"
        :title="error"
        type="error"
        :closable="false"
        show-icon
        class="mb-12px"
      />
      <el-tabs v-model="activeTab" class="craft-library__tabs">
        <el-tab-pane label="工艺库" name="crafts">
          <UnifiedListTemplate
            :table-key="CRAFT_TABLE_KEY"
            single-line-toolbar
            class="craft-library__list"
            :query-model="craftQuery"
            :filter-definitions="[]"
            :quick-filter-state="{}"
            :operator-options="[]"
            :show-quick-filter="false"
            show-multi-filter
            :multi-filter-definitions="craftFilterDefinitions"
            :multi-filter-state="craftFilter.state"
            :columns="craftColumnControl.columns.value"
            :column-saving="craftColumnControl.saving.value"
            v-model:sort-state="craftSort"
            :total="filteredCrafts.length"
            v-model:page="craftQuery.pageNo"
            v-model:limit="craftQuery.pageSize"
            @update:multi-filter-state="craftFilter.updateState"
            @multi-filter-query="craftFilter.applyMultiFilter"
            @multi-filter-reset="craftFilter.resetMultiFilter"
            @multi-filter-remove="craftFilter.removeConditionAndApply"
            @column-change="craftColumnControl.saveConfig"
            @sort-change="craftQuery.pageNo = 1"
            :filter-rows="filteredCrafts"
          >
            <template #actions>
              <el-button
                v-hasPermi="['mes:pro-craft:create']"
                type="primary"
                plain
                @click="openEditor()"
              >
                <Icon icon="ep:plus" class="mr-5px" />新增工艺
              </el-button>
              <el-button :loading="loading" @click="load"
                ><Icon icon="ep:refresh" class="mr-5px" />刷新</el-button
              >
            </template>
            <template #table="{ resolveRows, sortColumnAttrs, handleSortChange }">
              <el-table
                v-loading="loading"
                :data="resolveRows(pagedCrafts)"
                height="100%"
                scrollbar-always-on
                border
                stripe
                show-overflow-tooltip
                row-key="id"
                data-user-table-column-explicit
                :data-user-table-key="CRAFT_TABLE_KEY"
                @header-dragend="craftColumnControl.handleHeaderDragend"
                @sort-change="handleSortChange"
              >
                <el-table-column
                  v-for="column in visibleCraftDataColumns"
                  :key="column.key"
                  :prop="column.key"
                  :label="column.label"
                  :width="column.width"
                  :min-width="column.minWidth"
                  align="center"
                  v-bind="sortColumnAttrs(column.key)"
                />
                <el-table-column
                  label="操作"
                  prop="operation"
                  :width="craftColumnControl.getColumnWidthString('operation')"
                  align="center"
                  fixed="right"
                >
                  <template #default="{ row }">
                    <el-button link type="primary" @click="openEditor(row, true)">查看</el-button>
                    <el-button
                      v-hasPermi="['mes:pro-craft:update']"
                      link
                      type="primary"
                      @click="openEditor(row)"
                      >修改</el-button
                    >
                  </template>
                </el-table-column>
              </el-table>
            </template>
          </UnifiedListTemplate>
        </el-tab-pane>
        <el-tab-pane label="工艺修改记录" name="changes" lazy>
          <UnifiedListTemplate
            :table-key="CHANGE_TABLE_KEY"
            single-line-toolbar
            class="craft-library__list"
            :query-model="changeQuery"
            :filter-definitions="[]"
            :quick-filter-state="{}"
            :operator-options="[]"
            :show-quick-filter="false"
            show-multi-filter
            :multi-filter-definitions="changeFilterDefinitions"
            :multi-filter-state="changeFilter.state"
            :columns="changeColumnControl.columns.value"
            :column-saving="changeColumnControl.saving.value"
            v-model:sort-state="changeSort"
            :total="filteredChanges.length"
            v-model:page="changeQuery.pageNo"
            v-model:limit="changeQuery.pageSize"
            @update:multi-filter-state="changeFilter.updateState"
            @multi-filter-query="changeFilter.applyMultiFilter"
            @multi-filter-reset="changeFilter.resetMultiFilter"
            @multi-filter-remove="changeFilter.removeConditionAndApply"
            @column-change="changeColumnControl.saveConfig"
            @sort-change="changeQuery.pageNo = 1"
            :filter-rows="filteredChanges"
          >
            <template #actions>
              <el-button :loading="loading" @click="load"
                ><Icon icon="ep:refresh" class="mr-5px" />刷新</el-button
              >
            </template>
            <template #table="{ resolveRows, sortColumnAttrs, handleSortChange }">
              <el-table
                v-loading="loading"
                :data="resolveRows(pagedChanges)"
                height="100%"
                scrollbar-always-on
                border
                stripe
                show-overflow-tooltip
                row-key="id"
                data-user-table-column-explicit
                :data-user-table-key="CHANGE_TABLE_KEY"
                @header-dragend="changeColumnControl.handleHeaderDragend"
                @sort-change="handleSortChange"
              >
                <el-table-column
                  v-for="column in visibleChangeDataColumns"
                  :key="column.key"
                  :prop="column.key"
                  :label="column.label"
                  :width="column.width"
                  :min-width="column.minWidth"
                  :formatter="column.key === 'createTime' ? dateFormatter : undefined"
                  align="center"
                  v-bind="sortColumnAttrs(column.key)"
                />
                <el-table-column
                  label="操作"
                  prop="operation"
                  :width="changeColumnControl.getColumnWidthString('operation')"
                  align="center"
                  fixed="right"
                >
                  <template #default="{ row }">
                    <el-button link type="primary" @click="showHistory(row)">查看变更</el-button>
                    <el-button
                      v-if="row.status === 'PENDING'"
                      v-hasPermi="['mes:pro-craft:update']"
                      link
                      type="danger"
                      :disabled="loading"
                      @click="cancelChange(row)"
                      >作废整次修改</el-button
                    >
                  </template>
                </el-table-column>
              </el-table>
            </template>
          </UnifiedListTemplate>
        </el-tab-pane>
      </el-tabs>
    </ContentWrap>
  </div>
  <Dialog
    v-model="editorVisible"
    :title="readOnly ? '查看工艺' : form.id ? '修改工艺' : '新增工艺'"
    width="92%"
  >
    <el-form label-width="90px" :disabled="readOnly || saving">
      <el-form-item label="工艺编号"
        ><el-input :model-value="editorCode || '保存后自动生成 GY- 编号'" disabled
      /></el-form-item>
      <el-form-item label="工艺名称" required
        ><el-input v-model="form.name" maxlength="128"
      /></el-form-item>
      <el-form-item label="所属工序" required>
        <el-select
          v-model="form.processId"
          filterable
          :disabled="!!form.id"
          placeholder="请选择工序"
        >
          <el-option
            v-for="p in processes"
            :key="p.id"
            :label="p.code + ' ' + p.name"
            :value="p.id!"
          />
        </el-select>
      </el-form-item>
      <el-button v-if="!readOnly" class="mb-12px" @click="addParameter">新增参数</el-button>
      <el-table :data="form.parameters" border max-height="440">
        <el-table-column label="参数编码" width="145"
          ><template #default="{ row }"><el-input v-model="row.parameterCode" /></template
        ></el-table-column>
        <el-table-column label="参数名称" width="140"
          ><template #default="{ row }"><el-input v-model="row.parameterName" /></template
        ></el-table-column>
        <el-table-column label="类型" width="130"
          ><template #default="{ row }">
            <el-select v-model="row.valueType" @change="resetTypeFields(row)"
              ><el-option
                v-for="t in parameterTypes"
                :key="t.value"
                :label="t.label"
                :value="t.value"
            /></el-select> </template
        ></el-table-column>
        <el-table-column label="单位" width="90"
          ><template #default="{ row }"><el-input v-model="row.unit" /></template
        ></el-table-column>
        <el-table-column label="标准说明" min-width="170"
          ><template #default="{ row }"
            ><el-input v-model="row.standardText" type="textarea" /></template
        ></el-table-column>
        <el-table-column label="下限" width="120"
          ><template #default="{ row }"
            ><el-input-number
              v-if="numeric(row)"
              v-model="row.lowerLimit"
              controls-position="right"
              class="!w-100px" /></template
        ></el-table-column>
        <el-table-column label="上限" width="120"
          ><template #default="{ row }"
            ><el-input-number
              v-if="numeric(row)"
              v-model="row.upperLimit"
              controls-position="right"
              class="!w-100px" /></template
        ></el-table-column>
        <el-table-column label="默认值" width="140"
          ><template #default="{ row }">
            <el-select v-if="row.valueType === 'BOOLEAN'" v-model="row.defaultValue"
              ><el-option label="是" :value="1" /><el-option label="否" :value="0"
            /></el-select>
            <el-input-number
              v-else-if="numeric(row)"
              v-model="row.defaultValue"
              controls-position="right"
              class="!w-120px"
            />
            <el-input v-else-if="row.valueType === 'SELECT'" v-model="row.defaultText" /> </template
        ></el-table-column>
        <el-table-column label="下拉选项" width="150"
          ><template #default="{ row }">
            <el-input
              v-if="row.valueType === 'SELECT'"
              :model-value="optionsText(row)"
              type="textarea"
              placeholder="每行一个选项"
              @update:model-value="setOptions(row, $event)"
            /> </template
        ></el-table-column>
        <el-table-column label="小数位数" width="120"
          ><template #default="{ row }"
            ><el-input-number
              v-if="row.valueType === 'DECIMAL'"
              v-model="row.decimalScale"
              :min="0"
              :max="6"
              controls-position="right"
              class="!w-100px" /></template
        ></el-table-column>
        <el-table-column label="顺序" width="110"
          ><template #default="{ row }"
            ><el-input-number
              v-model="row.sort"
              :min="0"
              controls-position="right"
              class="!w-90px" /></template
        ></el-table-column>
        <el-table-column v-if="!readOnly" label="操作" width="75"
          ><template #default="{ $index }"
            ><el-button link type="danger" @click="form.parameters.splice($index, 1)"
              >删除</el-button
            ></template
          ></el-table-column
        >
      </el-table>
      <el-form-item v-if="!readOnly" label="修改原因" required class="mt-16px"
        ><el-input v-model="form.reason" type="textarea" maxlength="512"
      /></el-form-item>
    </el-form>
    <el-alert
      v-if="editorError"
      :title="editorError"
      type="error"
      :closable="false"
      class="mt-12px"
    />
    <template #footer>
      <el-button :disabled="saving" @click="discardEditor">{{
        readOnly ? '关闭' : '取消本次修改'
      }}</el-button>
      <el-button v-if="!readOnly" type="primary" :loading="saving" @click="saveOrPreview">{{
        form.id ? '预览全部影响' : '保存工艺'
      }}</el-button>
    </template>
  </Dialog>
  <Dialog
    v-model="impactVisible"
    title="确认工艺修改及全部流程升级"
    width="1000px"
    :close-on-click-modal="false"
  >
    <el-alert
      title="确认后全部关联流程统一升级；已有生产订单继续使用原参数快照。"
      type="info"
      :closable="false"
      show-icon
    />
    <template v-if="impact">
      <el-alert
        v-for="(blocker, index) in impact.blockers"
        :key="index"
        :title="blocker"
        type="error"
        :closable="false"
        class="mt-8px"
      />
      <template>
        <p>工艺名称：{{ impact.beforeName }} → {{ impact.afterName }}</p>
        <el-table :data="parameterDiff" class="mt-12px" max-height="260">
          <el-table-column label="参数" prop="name" /><el-table-column
            label="修改前"
            prop="before"
          /><el-table-column label="修改后" prop="after" />
        </el-table>
        <p>受影响工艺流程：{{ impact.routes?.length || 0 }} 个</p>
        <el-table :data="impact.routes" max-height="260"
          ><el-table-column label="流程编号" prop="routeCode" /><el-table-column
            label="流程名称"
            prop="routeName" /><el-table-column label="当前版本" prop="versionNo"
        /></el-table>
      </template>
    </template>
    <el-alert
      v-if="impactError"
      :title="impactError"
      type="error"
      :closable="false"
      class="mt-12px"
    />
    <template #footer>
      <el-button :disabled="saving" @click="discardImpact">取消本次修改</el-button>
      <el-button
        type="primary"
        :loading="saving"
        :disabled="!impact || !!impact.blockers.length"
        @click="confirmAll"
        >确认修改并全部升级</el-button
      >
    </template>
  </Dialog>
  <Dialog v-model="historyVisible" title="工艺变更内容" width="900px">
    <el-descriptions v-if="history" :column="1" border
      ><el-descriptions-item label="状态">{{ statusLabel(history.status) }}</el-descriptions-item
      ><el-descriptions-item label="原因">{{
        history.reason
      }}</el-descriptions-item></el-descriptions
    >
    <template v-if="history?.changeType === 'INITIALIZE'">
      <p>初始化来源</p>
      <el-table :data="historySources" max-height="400"
        ><el-table-column label="工艺流程" prop="routeName" /><el-table-column
          label="工序"
          prop="processName" /><el-table-column label="节点编号" prop="routeProcessId"
      /></el-table>
      <p>新增工艺：{{ historyCraftCodes }}</p>
    </template>
    <template v-else>
      <el-descriptions :column="1" border class="mt-12px"
        ><el-descriptions-item label="工艺编号">{{ historyAfter?.code }}</el-descriptions-item
        ><el-descriptions-item label="名称变化"
          >{{ historyBefore?.name || '无' }} → {{ historyAfter?.name }}</el-descriptions-item
        ></el-descriptions
      >
      <el-table :data="historyDiff" max-height="400"
        ><el-table-column label="参数" prop="name" /><el-table-column
          label="修改前"
          prop="before" /><el-table-column label="修改后" prop="after"
      /></el-table>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import { useElementBounding, useWindowSize } from '@vueuse/core'
import {
  CraftApi,
  type Craft,
  type CraftParameter,
  type CraftSave,
  type CraftImpact,
  type CraftChange
} from '@/api/mes/pro/craft'
import { ProProcessApi, type ProProcessVO } from '@/api/mes/pro/process'
import { dateFormatter } from '@/utils/formatTime'
import UnifiedListTemplate from '@/components/UnifiedListTemplate/index.vue'
import {
  useUserTableColumns,
  type UserTableColumnDefinition
} from '@/hooks/web/useUserTableColumns'
import {
  useTableMultiFilter,
  type ListMultiFilterDefinition,
  type ListMultiFilterCondition
} from '@/hooks/web/useTableMultiFilter'
defineOptions({ name: 'MesProCraft' })
const pageContainer = ref<HTMLElement>()
const { top: pageTop } = useElementBounding(pageContainer)
const { height: viewportHeight } = useWindowSize()
const pageHeight = computed(() => `${Math.max(440, viewportHeight.value - pageTop.value - 2)}px`)
const message = useMessage()
const crafts = ref<Craft[]>([]),
  processes = ref<ProProcessVO[]>([]),
  changes = ref<CraftChange[]>([])
const loading = ref(false),
  saving = ref(false)
const error = ref(''),
  editorError = ref(''),
  impactError = ref('')
const editorVisible = ref(false),
  impactVisible = ref(false),
  readOnly = ref(false)
const editorCode = ref(''),
  impact = ref<CraftImpact>(),
  historyVisible = ref(false),
  history = ref<CraftChange>()
const form = reactive<CraftSave>({ name: '', processId: undefined, parameters: [], reason: '' })
const CRAFT_TABLE_KEY = 'mes.pro.craft.main'
const CHANGE_TABLE_KEY = 'mes.pro.craft.changes'
const activeTab = ref('crafts')
const craftDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'code', label: '工艺编号', width: 140, sortable: 'custom' },
  { key: 'name', label: '工艺名称', minWidth: 220, sortable: 'custom' },
  { key: 'processCode', label: '工序编码', width: 150, sortable: 'custom' },
  { key: 'processName', label: '工序名称', minWidth: 180, sortable: 'custom' },
  { key: 'parameterCount', label: '参数数量', width: 110, sortable: 'custom' },
  { key: 'operation', label: '操作', width: 150, hideable: false, business: false }
]
const changeDefaultColumns: UserTableColumnDefinition[] = [
  { key: 'createTime', label: '时间', width: 180, sortable: 'custom' },
  { key: 'creator', label: '操作人', width: 110, sortable: 'custom' },
  { key: 'changeTypeLabel', label: '变更类型', width: 110, sortable: 'custom' },
  { key: 'statusLabel', label: '状态', minWidth: 180, sortable: 'custom' },
  { key: 'reason', label: '原因', minWidth: 240, sortable: 'custom' },
  { key: 'operation', label: '操作', width: 220, hideable: false, business: false }
]
const craftColumnControl = useUserTableColumns(CRAFT_TABLE_KEY, craftDefaultColumns)
const changeColumnControl = useUserTableColumns(CHANGE_TABLE_KEY, changeDefaultColumns)
const visibleCraftDataColumns = computed(() =>
  craftColumnControl.visibleColumns.value.filter((column) => column.business !== false)
)
const visibleChangeDataColumns = computed(() =>
  changeColumnControl.visibleColumns.value.filter((column) => column.business !== false)
)
const craftFilterDefinitions: ListMultiFilterDefinition[] = [
  { key: 'code', label: '工艺编号', type: 'text' },
  { key: 'name', label: '工艺名称', type: 'text' },
  { key: 'processCode', label: '工序编码', type: 'text' },
  { key: 'processName', label: '工序名称', type: 'text' }
]
const changeFilterDefinitions: ListMultiFilterDefinition[] = [
  { key: 'creator', label: '操作人', type: 'text' },
  {
    key: 'changeTypeLabel',
    label: '变更类型',
    type: 'select',
    options: [
      { label: '新增', value: '新增' },
      { label: '修改', value: '修改' },
      { label: '初始化', value: '初始化' }
    ]
  },
  {
    key: 'statusLabel',
    label: '状态',
    type: 'select',
    options: [
      { label: '准备中', value: '准备中' },
      { label: '等待全部流程审批', value: '等待全部流程审批' },
      { label: '统一生效中', value: '统一生效中' },
      { label: '已生效', value: '已生效' },
      { label: '已作废', value: '已作废' }
    ]
  },
  { key: 'reason', label: '原因', type: 'text' }
]
type LocalListQuery = {
  pageNo: number
  pageSize: number
  multiFilters?: ListMultiFilterCondition[]
}
type LocalListSort = { prop?: string; key?: string; order?: 'ascending' | 'descending' | null }
const craftQuery = reactive<LocalListQuery>({ pageNo: 1, pageSize: 20 })
const changeQuery = reactive<LocalListQuery>({ pageNo: 1, pageSize: 20 })
const craftSort = ref<LocalListSort>({})
const changeSort = ref<LocalListSort>({})
const craftFilter = useTableMultiFilter(CRAFT_TABLE_KEY, craftFilterDefinitions, craftQuery, () =>
  nextTick()
)
const changeFilter = useTableMultiFilter(
  CHANGE_TABLE_KEY,
  changeFilterDefinitions,
  changeQuery,
  () => nextTick()
)
// 两个接口均返回完整集合：先按已应用的条件筛选、全量排序，再分页。
const queryRows = <T extends object>(
  rows: T[],
  query: LocalListQuery,
  sort: LocalListSort
): T[] => {
  const result = rows.filter((row) =>
    (query.multiFilters || []).every((condition) => {
      const value = row[condition.key as keyof T]
      if (condition.operator === 'eq') return String(value ?? '') === String(condition.value)
      if (condition.operator === 'contains')
        return String(value ?? '')
          .toLocaleLowerCase()
          .includes(String(condition.value).toLocaleLowerCase())
      throw new Error(`不支持的筛选运算符：${condition.operator}`)
    })
  )
  if (sort.prop && sort.order) {
    const prop = sort.prop as keyof T
    const direction = sort.order === 'ascending' ? 1 : -1
    result.sort((a, b) => {
      const left = a[prop],
        right = b[prop]
      const compared =
        typeof left === 'number' && typeof right === 'number'
          ? left - right
          : String(left ?? '').localeCompare(String(right ?? ''), 'zh-CN', { numeric: true })
      return direction * compared
    })
  }
  return result
}
const processMap = computed(() => new Map(processes.value.map((process) => [process.id, process])))
const craftRows = computed(() =>
  crafts.value.map((craft) => ({
    ...craft,
    processCode: processMap.value.get(craft.processId)?.code,
    processName: processMap.value.get(craft.processId)?.name,
    parameterCount: (JSON.parse(craft.parametersJson) as CraftParameter[]).length
  }))
)
const filteredCrafts = computed(() => queryRows(craftRows.value, craftQuery, craftSort.value))
const pagedCrafts = computed(() =>
  filteredCrafts.value.slice(
    (craftQuery.pageNo - 1) * craftQuery.pageSize,
    craftQuery.pageNo * craftQuery.pageSize
  )
)
const filteredChanges = computed(() =>
  queryRows(
    changes.value.map((change) => ({
      ...change,
      changeTypeLabel: typeLabel(change.changeType),
      statusLabel: statusLabel(change.status)
    })),
    changeQuery,
    changeSort.value
  )
)
const pagedChanges = computed(() =>
  filteredChanges.value.slice(
    (changeQuery.pageNo - 1) * changeQuery.pageSize,
    changeQuery.pageNo * changeQuery.pageSize
  )
)
const parameterTypes = [
  { value: 'INTEGER', label: '整数' },
  { value: 'DECIMAL', label: '小数' },
  { value: 'BOOLEAN', label: '是否' },
  { value: 'SELECT', label: '下拉选项' },
  { value: 'TEXT_STANDARD', label: '文本标准' }
]
const numeric = (p: CraftParameter) => ['INTEGER', 'DECIMAL'].includes(p.valueType)
const optionsText = (p: CraftParameter) =>
  p.optionValuesJson ? (JSON.parse(p.optionValuesJson) as string[]).join('\n') : ''
const setOptions = (p: CraftParameter, text: string) => {
  p.optionValuesJson = JSON.stringify(
    text
      .split('\n')
      .map((s) => s.trim())
      .filter(Boolean)
  )
}
const resetTypeFields = (p: CraftParameter) => {
  p.lowerLimit = null
  p.upperLimit = null
  p.defaultValue = null
  p.defaultText = null
  p.optionValuesJson = null
  p.decimalScale = null
}
const typeLabel = (s: string) =>
  (({ CREATE: '新增', UPDATE: '修改', INITIALIZE: '初始化' }) as Record<string, string>)[s] || s
const statusLabel = (s: string) =>
  (
    ({
      PREPARING: '准备中',
      PENDING: '等待全部流程审批',
      APPLYING: '统一生效中',
      APPLIED: '已生效',
      CANCELLED: '已作废'
    }) as Record<string, string>
  )[s] || s
const explain = (e: unknown) =>
  e instanceof Error
    ? e.message
    : typeof e === 'object' && e && 'msg' in e
      ? String(e.msg)
      : String(e)
const parameterText = (p?: CraftParameter) =>
  p
    ? [
        p.parameterName,
        parameterTypes.find((t) => t.value === p.valueType)?.label,
        p.standardText,
        p.unit ? '单位 ' + p.unit : '',
        p.lowerLimit != null ? '下限 ' + p.lowerLimit : '',
        p.upperLimit != null ? '上限 ' + p.upperLimit : '',
        p.defaultValue != null
          ? '默认 ' + p.defaultValue
          : p.defaultText
            ? '默认 ' + p.defaultText
            : '',
        p.valueType === 'SELECT' ? '选项 ' + optionsText(p) : '',
        p.decimalScale != null ? '小数位 ' + p.decimalScale : '',
        p.sort != null ? '顺序 ' + p.sort : ''
      ]
        .filter(Boolean)
        .join('；')
    : '无'
const diff = (before: CraftParameter[], after: CraftParameter[]) =>
  [...new Set([...before, ...after].map((p) => p.parameterCode))].map((code) => {
    const a = before.find((p) => p.parameterCode === code),
      b = after.find((p) => p.parameterCode === code)
    return {
      name: (b?.parameterName || a?.parameterName || code) + '（' + code + '）',
      before: parameterText(a),
      after: parameterText(b)
    }
  })
const parameterDiff = computed(() => diff(impact.value?.before || [], impact.value?.after || []))
const historyBefore = computed<Craft | undefined>(() =>
  history.value?.changeType !== 'INITIALIZE' && history.value?.beforeJson
    ? JSON.parse(history.value.beforeJson)
    : undefined
)
const historyAfter = computed<Craft | undefined>(() =>
  history.value?.changeType !== 'INITIALIZE' && history.value?.afterJson
    ? JSON.parse(history.value.afterJson)
    : undefined
)
const historyDiff = computed(() =>
  diff(
    historyBefore.value ? JSON.parse(historyBefore.value.parametersJson) : [],
    historyAfter.value ? JSON.parse(historyAfter.value.parametersJson) : []
  )
)
const historySources = computed(() =>
  history.value?.changeType === 'INITIALIZE' && history.value.beforeJson
    ? JSON.parse(history.value.beforeJson)
    : []
)
const historyCraftCodes = computed(() =>
  history.value?.changeType === 'INITIALIZE' && history.value.afterJson
    ? (JSON.parse(history.value.afterJson) as number[])
        .map((id) => crafts.value.find((c) => c.id === id)?.code || '已作废工艺 #' + id)
        .join('、')
    : ''
)
async function load() {
  loading.value = true
  error.value = ''
  try {
    const [a, b, c] = await Promise.all([
      CraftApi.list(),
      ProProcessApi.getProcessSimpleList(),
      CraftApi.changes()
    ])
    crafts.value = a
    processes.value = b
    changes.value = c
  } catch (e) {
    error.value = explain(e)
  } finally {
    loading.value = false
  }
}
function openEditor(craft?: Craft, detail = false) {
  Object.assign(form, {
    id: craft?.id,
    name: craft?.name || '',
    processId: craft?.processId,
    parameters: craft ? JSON.parse(craft.parametersJson) : [],
    expectedFingerprint: craft?.fingerprint,
    impactToken: undefined,
    reason: ''
  })
  editorCode.value = craft?.code || ''
  readOnly.value = detail
  editorError.value = ''
  editorVisible.value = true
}
function addParameter() {
  form.parameters.push({
    parameterCode: '',
    parameterName: '',
    valueType: 'DECIMAL',
    standardText: '',
    sort: form.parameters.length + 1
  })
}
function discardEditor() {
  editorVisible.value = false
  form.parameters = []
}
function discardImpact() {
  impactVisible.value = false
  impact.value = undefined
  discardEditor()
}
async function saveOrPreview() {
  editorError.value = ''
  if (!form.name.trim() || !form.processId || !form.reason.trim() || !form.parameters.length) {
    editorError.value = '请填写工艺名称、所属工序、参数和修改原因'
    return
  }
  saving.value = true
  try {
    if (!form.id) {
      await CraftApi.create(form)
      editorVisible.value = false
      message.success('工艺已新增')
      await load()
    } else {
      impact.value = await CraftApi.preview(form)
      form.impactToken = impact.value.impactToken
      impactError.value = ''
      impactVisible.value = true
    }
  } catch (e) {
    editorError.value = explain(e)
  } finally {
    saving.value = false
  }
}
async function confirmAll() {
  if (!impact.value) return
  saving.value = true
  impactError.value = ''
  try {
    const result = await CraftApi.updateAll({ ...form, impactToken: impact.value.impactToken })
    impactVisible.value = false
    editorVisible.value = false
    message.success(
      result.status === 'APPLIED'
        ? '工艺及全部关联流程已统一生效'
        : '已提交，全部关联流程审批完成后统一生效'
    )
    await load()
  } catch (e) {
    impactError.value = explain(e)
  } finally {
    saving.value = false
  }
}
async function cancelChange(change: CraftChange) {
  await message.confirm('作废此次工艺修改及全部关联流程升级？原正式配置将保持不变。')
  loading.value = true
  error.value = ''
  try {
    await CraftApi.cancelChange(change.id)
    await load()
  } catch (e) {
    error.value = explain(e)
  } finally {
    loading.value = false
  }
}
function showHistory(change: CraftChange) {
  history.value = change
  historyVisible.value = true
}
onMounted(load)


</script>
<style scoped>
.craft-library {
  margin-bottom: 0;
}
.craft-library :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  height: v-bind(pageHeight);
  box-sizing: border-box;
  overflow: hidden;
}
.craft-library__tabs {
  display: flex;
  flex: 1;
  flex-direction: column;
  min-height: 0;
}
.craft-library__tabs :deep(.el-tabs__content),
.craft-library__tabs :deep(.el-tab-pane) {
  flex: 1;
  min-height: 0;
  height: 100%;
  overflow: hidden;
}
.craft-library__list {
  height: 100%;
  overflow: hidden;
}
.craft-library__list :deep(.unified-list-template__table-shell) {
  flex: 1 1 auto;
  min-height: 0;
  overflow: hidden;
}
.craft-library__list :deep(.unified-list-template__multi-filter) {
  min-width: 0;
}
.craft-library__list :deep(.unified-list-template__toolbar-actions) {
  max-width: 100%;
}

.craft-history {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
  max-height: 300px;
  overflow: auto;
  background: var(--el-fill-color-light);
  padding: 12px;
}
</style>
