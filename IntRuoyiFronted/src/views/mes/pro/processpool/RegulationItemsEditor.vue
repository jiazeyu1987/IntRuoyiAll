<template>
  <el-form :disabled="disabled" class="regulation-items-editor">
    <div class="regulation-items-editor__toolbar">
      <h3>工序检验方法与抽样方案</h3>
      <div class="regulation-items-editor__actions">
        <span>是否需要末检</span>
        <el-switch
          v-model="draft.finalInspectionApplicable"
          aria-label="通用规程是否需要末检"
          active-text="需要"
          inactive-text="不需要"
          @change="syncFinal"
        />
        <el-input
          v-if="!draft.finalInspectionApplicable"
          v-model="draft.finalInspectionNotApplicableReason"
          aria-label="末检不适用依据"
          placeholder="填写末检不适用的正式依据"
          class="regulation-items-editor__reason"
        />
        <UserTableColumnSettings
          :columns="columns"
          :saving="columnSaving"
          :show-reset="false"
          @change="saveColumnConfig"
        />
        <el-button type="primary" plain @click="addVisible = true">新增工序/检验项目</el-button>
      </div>
    </div>
    <el-table
      :data="pagedRows"
      stripe
      border
      size="small"
      row-key="item.itemCode"
      :max-height="tableHeight"
      @header-dragend="handleHeaderDragend"
    >
      <el-table-column
        v-if="isColumnVisible('processName')"
        label="工序"
        :min-width="getColumnMinWidthString('processName', 170)"
      >
        <template #default="{ row }"
          ><el-input v-model="row.process.processName" aria-label="通用规程工序名称"
        /></template>
      </el-table-column>
      <el-table-column
        v-if="isColumnVisible('processCode')"
        label="工序编码"
        :min-width="getColumnMinWidthString('processCode', 150)"
      >
        <template #default="{ row }"
          ><el-input v-model="row.process.processCode" aria-label="通用规程工序编码"
        /></template>
      </el-table-column>
      <el-table-column
        v-if="isColumnVisible('itemName')"
        label="检验项目"
        :min-width="getColumnMinWidthString('itemName', 170)"
      >
        <template #default="{ row }"
          ><el-input v-model="row.item.itemName" aria-label="通用规程检验项目"
        /></template>
      </el-table-column>
      <el-table-column v-if="isColumnVisible('types')" label="适用检验类型" min-width="180">
        <template #default="{ row }">
          <el-tag v-for="label in typeLabels(row.item)" :key="label" size="small" class="mr-4px">{{
            label
          }}</el-tag>
          <el-checkbox
            v-if="draft.finalInspectionApplicable"
            :model-value="hasType(row.item, 'FINAL')"
            aria-label="该项目适用末检"
            @change="(value) => toggleType(row.item, 'FINAL', Boolean(value))"
            >末检</el-checkbox
          >
        </template>
      </el-table-column>
      <el-table-column v-if="isColumnVisible('first')" label="首检" min-width="200">
        <template #default="{ row }">
          <el-switch
            :model-value="hasType(row.item, 'FIRST')"
            active-text="启用"
            inactive-text="不启用"
            aria-label="启用首检"
            @change="(value) => toggleType(row.item, 'FIRST', Boolean(value))"
          />
          <div v-if="hasType(row.item, 'FIRST')" class="regulation-items-editor__quantity">
            <el-input-number
              v-model="row.item.firstInspectionQuantity"
              :min="1"
              :precision="0"
              controls-position="right"
              aria-label="首检数量"
            />
            件
          </div>
        </template>
      </el-table-column>
      <el-table-column v-if="isColumnVisible('patrol')" label="巡检" min-width="225">
        <template #default="{ row }">
          <el-switch
            :model-value="hasType(row.item, 'PATROL')"
            active-text="启用"
            inactive-text="不启用"
            aria-label="启用巡检"
            @change="(value) => toggleType(row.item, 'PATROL', Boolean(value))"
          />
          <div v-if="hasType(row.item, 'PATROL')" class="regulation-items-editor__quantity">
            <SamplingRuleEditor
              v-model:inspection-level="row.item.inspectionLevel"
              v-model:aql="row.item.aql"
              :disabled="disabled"
            />
          </div>
        </template>
      </el-table-column>
      <el-table-column
        v-if="isColumnVisible('standard')"
        label="接受标准"
        :min-width="getColumnMinWidthString('standard', 280)"
      >
        <template #default="{ row }"
          ><el-input
            v-model="row.item.standardText"
            type="textarea"
            :rows="3"
            aria-label="接受标准"
        /></template>
      </el-table-column>
      <el-table-column
        v-if="isColumnVisible('method')"
        label="检验方法"
        :min-width="getColumnMinWidthString('method', 240)"
      >
        <template #default="{ row }"
          ><el-input
            v-model="row.item.inspectionMethod"
            type="textarea"
            :rows="3"
            aria-label="检验方法"
        /></template>
      </el-table-column>
      <el-table-column v-if="isColumnVisible('equipment')" label="检验器具及设备" min-width="300">
        <template #default="{ row }">
          <el-input v-model="row.item.inspectionTool" aria-label="检验器具及设备要求" />
          <el-select
            :model-value="row.item.equipmentOptions.map((e) => e.equipmentId)"
            multiple
            filterable
            aria-label="通用规程检验设备"
            placeholder="选择设备编号"
            @change="(ids) => selectEquipment(row.item, ids)"
          >
            <el-option
              v-for="equipment in equipmentOptions(row.item)"
              :key="equipment.id"
              :label="`${equipment.code} / ${equipment.name}`"
              :value="equipment.id"
            />
          </el-select>
          <template
            v-if="!row.item.equipmentOptions.length && isNonSensoryTool(row.item.inspectionTool)"
          >
            <el-input
              v-model="row.item.equipmentUnboundReason"
              aria-label="器具未绑定原因"
              type="textarea"
              placeholder="填写器具暂未绑定的具体原因"
            />
            <small>{{ EQUIPMENT_UNBOUND_NOTICE }}</small>
          </template>
        </template>
      </el-table-column>
      <el-table-column v-if="isColumnVisible('sampling')" label="原抽样方案" min-width="240">
        <template #default="{ row }"
          ><el-input
            v-model="row.item.samplingPlanText"
            aria-label="原抽样方案"
            type="textarea"
            :rows="3"
        /></template>
      </el-table-column>
      <el-table-column v-if="isColumnVisible('itemCode')" label="检验项目编码" min-width="180">
        <template #default="{ row }"
          ><el-input v-model="row.item.itemCode" aria-label="检验项目编码"
        /></template>
      </el-table-column>
      <el-table-column v-if="isColumnVisible('result')" label="结果类型及数值要求" min-width="220">
        <template #default="{ row }">
          <el-select v-model="row.item.resultType" aria-label="结果类型">
            <el-option label="合格 / 不合格" value="BOOLEAN" /><el-option
              label="数值"
              value="NUMERIC"
            /><el-option label="文本" value="TEXT" />
          </el-select>
          <template v-if="row.item.resultType === 'NUMERIC'">
            <el-input-number
              v-model="row.item.standardLowerLimit"
              aria-label="数值下限"
              controls-position="right"
            />
            <el-input-number
              v-model="row.item.standardUpperLimit"
              aria-label="数值上限"
              controls-position="right"
            />
            <el-input v-model="row.item.standardUnit" aria-label="数值单位" placeholder="单位" />
            <el-input-number
              v-model="row.item.standardPrecision"
              aria-label="数值精度"
              :min="0"
              :max="8"
              :precision="0"
              controls-position="right"
            />
          </template>
        </template>
      </el-table-column>
      <el-table-column v-if="isColumnVisible('critical')" label="关键项" width="90">
        <template #default="{ row }"
          ><el-switch v-model="row.item.critical" aria-label="关键检验项目"
        /></template>
      </el-table-column>
      <el-table-column v-if="isColumnVisible('failure')" label="不合格处理要求" min-width="260">
        <template #default="{ row }"
          ><el-input
            v-model="row.item.failureRule"
            aria-label="不合格处理要求"
            type="textarea"
            :rows="3"
        /></template>
      </el-table-column>
      <el-table-column v-if="isColumnVisible('source')" label="原文依据" min-width="320">
        <template #default="{ row }">
          <el-input
            v-model="row.item.sourceOriginalItem"
            aria-label="原文项目"
            placeholder="原文项目"
          />
          <el-input
            v-model="row.item.sourceOriginalExcerpt"
            aria-label="原文摘录"
            type="textarea"
            :rows="3"
          />
          <el-input
            v-model="row.item.sourceOriginalMethod"
            aria-label="原文方法"
            type="textarea"
            :rows="2"
          />
          <el-input v-model="row.item.sourceNote" aria-label="来源说明" placeholder="来源说明" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="85" fixed="right">
        <template #default="{ row }"
          ><el-button type="danger" link @click="removeItem(row.process, row.item)"
            >删除</el-button
          ></template
        >
      </el-table-column>
    </el-table>
    <el-pagination
      v-model:current-page="pageNo"
      v-model:page-size="pageSize"
      :total="rows.length"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next, jumper"
      class="mt-16px"
    />
    <el-dialog v-model="addVisible" title="新增工序/检验项目" width="520px">
      <el-form-item label="所属工序">
        <el-select v-model="addProcessCode" aria-label="新增检验项所属工序">
          <el-option label="新建工序" value="" />
          <el-option
            v-for="process in draft.processes"
            :key="process.processCode"
            :label="process.processName"
            :value="process.processCode"
          />
        </el-select>
      </el-form-item>
      <el-form-item v-if="!addProcessCode" label="工序名称"
        ><el-input v-model="addProcessName" aria-label="新增工序名称"
      /></el-form-item>
      <el-form-item label="检验项目"
        ><el-input v-model="addItemName" aria-label="新增检验项目名称"
      /></el-form-item>
      <template #footer
        ><el-button @click="addVisible = false">取消</el-button
        ><el-button type="primary" @click="addItem">添加</el-button></template
      >
    </el-dialog>
  </el-form>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useWindowSize } from '@vueuse/core'
import { ElMessage } from 'element-plus'
import UserTableColumnSettings from '@/components/UserTableColumnSettings/index.vue'
import SamplingRuleEditor from '@/views/mes/qc/template/SamplingRuleEditor.vue'
import {
  useUserTableColumns,
  type UserTableColumnDefinition
} from '@/hooks/web/useUserTableColumns'
import {
  EQUIPMENT_UNBOUND_NOTICE,
  isNonSensoryTool,
  type Draft
} from '@/views/form-center/parser/simplePqc'
import type { QaInspectionRegulationItemVO } from '@/api/mes/qc/template'
import type { DvMachineryVO } from '@/api/mes/dv/machinery'

const draft = defineModel<Draft>('draft', { required: true })
const props = defineProps<{ disabled: boolean; machinery: DvMachineryVO[] }>()
type Item = QaInspectionRegulationItemVO
type Process = Draft['processes'][number]
type InspectionType = Item['applicableInspectionTypes'][number]
const pageNo = ref(1)
const pageSize = ref(20)
const addVisible = ref(false)
const addProcessCode = ref('')
const addProcessName = ref('')
const addItemName = ref('')
const { height } = useWindowSize()
const tableHeight = computed(() => Math.max(260, height.value - 390))
const rows = computed(() =>
  draft.value.processes.flatMap((process) => process.items.map((item) => ({ process, item })))
)
const pagedRows = computed(() =>
  rows.value.slice((pageNo.value - 1) * pageSize.value, pageNo.value * pageSize.value)
)
watch(
  () => draft.value.regulationId,
  () => {
    pageNo.value = 1
  }
)
watch(
  () => rows.value.length,
  (total) => {
    pageNo.value = Math.min(pageNo.value, Math.max(1, Math.ceil(total / pageSize.value)))
  }
)
const defaultColumns: UserTableColumnDefinition[] = [
  { key: 'processName', label: '工序', minWidth: 170 },
  { key: 'processCode', label: '工序编码', minWidth: 150 },
  { key: 'itemName', label: '检验项目', minWidth: 170 },
  { key: 'types', label: '适用检验类型', minWidth: 180 },
  { key: 'first', label: '首检', minWidth: 200 },
  { key: 'patrol', label: '巡检', minWidth: 225 },
  { key: 'standard', label: '接受标准', minWidth: 280 },
  { key: 'method', label: '检验方法', minWidth: 240 },
  { key: 'equipment', label: '检验器具及设备', minWidth: 300 },
  { key: 'sampling', label: '原抽样方案', minWidth: 240 },
  { key: 'itemCode', label: '检验项目编码', minWidth: 180, visible: false },
  { key: 'result', label: '结果类型及数值要求', minWidth: 220, visible: false },
  { key: 'critical', label: '关键项', width: 90, visible: false },
  { key: 'failure', label: '不合格处理要求', minWidth: 260, visible: false },
  { key: 'source', label: '原文依据', minWidth: 320, visible: false }
]
const {
  columns,
  saving: columnSaving,
  isColumnVisible,
  getColumnMinWidthString,
  handleHeaderDragend,
  saveConfig: saveColumnConfig
} = useUserTableColumns('mes.qa.commonRegulation.editor.items', defaultColumns)
const hasType = (item: Item, type: InspectionType) => item.applicableInspectionTypes.includes(type)
function typeLabels(item: Item) {
  const labels: string[] = []
  if (hasType(item, 'FIRST')) labels.push('首检')
  if (hasType(item, 'PATROL'))
    draft.value.inspectionTypeRules
      .filter((rule) => rule.inspectionType === 'PATROL' && rule.required)
      .forEach((rule) => labels.push(rule.label))
  if (hasType(item, 'FINAL')) labels.push('末检')
  return labels
}
function toggleType(item: Item, type: InspectionType, enabled: boolean) {
  if (props.disabled) return
  item.applicableInspectionTypes = enabled
    ? [...new Set([...item.applicableInspectionTypes, type])]
    : item.applicableInspectionTypes.filter((value) => value !== type)
}
function syncFinal() {
  const rule = draft.value.inspectionTypeRules.find((rule) => rule.key === 'FINAL')
  if (!rule) throw new Error('规程缺少末检规则')
  rule.required = draft.value.finalInspectionApplicable
  if (!rule.required)
    draft.value.processes.forEach((process) =>
      process.items.forEach((item) => toggleType(item, 'FINAL', false))
    )
  else draft.value.finalInspectionNotApplicableReason = undefined
}
function equipmentOptions(item: Item) {
  const options = props.machinery.map((equipment) => ({
    id: equipment.id,
    code: equipment.code,
    name: equipment.name
  }))
  item.equipmentOptions.forEach((equipment) => {
    if (!options.some((option) => option.id === equipment.equipmentId))
      options.push({
        id: equipment.equipmentId,
        code: equipment.equipmentCode,
        name: equipment.equipmentName
      })
  })
  return options
}
function selectEquipment(item: Item, ids: number[]) {
  if (props.disabled) return
  item.equipmentOptions = ids.map((id, index) => {
    const existing = item.equipmentOptions.find((equipment) => equipment.equipmentId === id)
    if (existing) return { ...existing, defaultFlag: index === 0, sort: index + 1 }
    const equipment = props.machinery.find((equipment) => equipment.id === id)
    if (!equipment?.code) throw new Error('设备编号不存在，请刷新设备目录')
    return {
      equipmentId: id,
      equipmentCode: equipment.code,
      equipmentName: equipment.name,
      equipmentNumber: equipment.code,
      defaultFlag: index === 0,
      sort: index + 1
    }
  })
  if (ids.length) item.equipmentUnboundReason = undefined
}
function removeItem(process: Process, item: Item) {
  if (props.disabled) return
  process.items.splice(process.items.indexOf(item), 1)
  if (!process.items.length) draft.value.processes.splice(draft.value.processes.indexOf(process), 1)
}
function addItem() {
  if (props.disabled) return
  if (!addItemName.value.trim() || (!addProcessCode.value && !addProcessName.value.trim())) {
    ElMessage.warning('请填写工序名称和检验项目')
    return
  }
  let process = draft.value.processes.find(
    (process) => process.processCode === addProcessCode.value
  )
  if (!process) {
    process = {
      processCode: `${draft.value.regulationCode}-P-${crypto.randomUUID().slice(0, 8)}`,
      processName: addProcessName.value.trim(),
      sort: draft.value.processes.length + 1,
      items: []
    }
    draft.value.processes.push(process)
  }
  process.items.push({
    itemCode: crypto.randomUUID(),
    itemName: addItemName.value.trim(),
    itemSort: process.items.length + 1,
    inspectionMethod: '',
    inspectionTool: '',
    samplingPlanText: '',
    standardText: '',
    resultType: 'BOOLEAN',
    applicableInspectionTypes: [],
    equipmentOptions: []
  })
  addVisible.value = false
  addItemName.value = ''
  pageNo.value = Math.ceil(rows.value.length / pageSize.value)
}
</script>

<style scoped>
.regulation-items-editor__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 20px;
}
.regulation-items-editor__toolbar h3 {
  margin: 0;
  font-size: 16px;
}
.regulation-items-editor__actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 13px;
}
.regulation-items-editor__reason {
  width: 280px;
}
.regulation-items-editor__quantity {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
}
.regulation-items-editor__quantity .el-input-number {
  width: 120px;
}
.regulation-items-editor :deep(.el-table__header th) {
  background: #f6f8fb;
  color: #172033;
}
.regulation-items-editor :deep(.el-table .cell) {
  padding: 10px;
}
.regulation-items-editor :deep(.el-table .el-select) {
  width: 100%;
  margin-top: 8px;
}
.regulation-items-editor small {
  display: block;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
}
</style>
