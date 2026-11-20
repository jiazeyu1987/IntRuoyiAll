<template>
  <div class="controlled-detail-approval" v-loading="loading" data-testid="dcc-controlled-detail-approval">
    <section class="controlled-detail-card">
      <div class="approval-heading">
        <h3>审批记录</h3>
        <div v-if="groups.length > 1 && !contextError" class="approval-filters">
          <label>版本
            <el-select v-model="versionFilter" clearable placeholder="全部版本" aria-label="版本筛选">
              <el-option v-for="version in versions" :key="version" :label="getDccVersionDisplayText(version)" :value="version" />
            </el-select>
          </label>
          <label>申请类型
            <el-select v-model="typeFilter" clearable placeholder="全部类型" aria-label="申请类型筛选">
              <el-option v-for="type in types" :key="type.value" :label="type.label" :value="type.value" />
            </el-select>
          </label>
        </div>
      </div>
      <el-alert v-if="error" :title="error" type="error" :closable="false" />
      <el-table
v-if="!contextError" :data="records" :span-method="spanGroup" :row-class-name="rowClass"
        row-key="key" border empty-text="暂无匹配的审批或电子签名记录" data-user-table-column-explicit>
        <el-table-column label="办理事项" min-width="290">
          <template #default="{ row }">
            <template v-if="row.group">
              <button
                type="button" class="approval-group-title" :aria-expanded="expandedGroups.includes(row.group.key)"
                @click="toggleGroup(row.group.key)">
                <span aria-hidden="true" class="approval-group-arrow">{{ expandedGroups.includes(row.group.key) ? '⌄' : '›' }}</span>
                <strong>{{ getDccVersionDisplayText(row.group.version) }}</strong>
                <span>{{ row.group.applicationLabel }}</span>
                <span v-if="row.group.submittedAt != null" class="approval-group-time">提交：{{ formatControlledFileDateTime(row.group.submittedAt) }}</span>
                <span v-if="duplicateGroup(row.group)" class="approval-secondary">本版本第 {{ groupOrdinal(row.group) }} 次申请</span>
              </button>
              <el-alert v-if="row.group.error" :title="row.group.error" type="error" :closable="false" />
              <div v-if="expandedGroups.includes(row.group.key) && !row.group.rows.length" class="approval-secondary">暂无可查办理记录</div>
            </template>
            <template v-else>
              <button
v-if="row.details.length" type="button" class="approval-expand" :aria-expanded="expandedRows.includes(row.key)"
                @click="toggleRow(row.key)">
                {{ row.item }} <span aria-hidden="true">{{ expandedRows.includes(row.key) ? '⌄' : '›' }}</span>
              </button>
              <div v-else class="approval-item">{{ row.item }}</div>
              <div v-if="expandedRows.includes(row.key)">
                <div v-for="detail in row.details" :key="detail" class="approval-secondary">{{ detail }}</div>
              </div>
            </template>
          </template>
        </el-table-column>
        <el-table-column label="办理人" min-width="170" prop="actor" />
        <el-table-column label="记录时间" min-width="175">
          <template #default="{ row }"><span class="approval-time">{{ formatControlledFileDateTime(row.time) }}</span></template>
        </el-table-column>
        <el-table-column label="处理结果" min-width="110" prop="result" />
        <el-table-column label="审批意见" min-width="170">
          <template #default="{ row }">{{ opinion(row.comment) }}</template>
        </el-table-column>
        <el-table-column label="电子签名" min-width="110" prop="signature" />
      </el-table>
    </section>
  </div>
</template>
<script setup lang="ts">
import { computed, ref, watch, onBeforeUnmount } from 'vue'
import { getControlledFile, type ControlledFileVO } from '@/api/dcc/controlledFile/workflow'
import { getControlledFileApplicationRounds, getControlledFileApplicationEvidence } from '@/api/dcc/controlledFile/applicationRead'
import type { DccApplicationType } from '@/api/dcc/controlledFile/applicationRead'
import { serializeDccBodyReadContext, type DccBodyReadContext } from '@/api/dcc/controlledFile/bodyReadContext'
import { loadControlledApprovalGroups, filterControlledApprovalGroups, type ControlledApprovalGroup } from './controlled-detail-approval'
import { formatControlledFileDateTime } from './presentation'
import { getDccSignatureOpinionLabel } from '../shared/signature-evidence'
import { getDccVersionDisplayText } from '../shared/version-display'

const props = defineProps<{
  file: ControlledFileVO
  bpmRound?: string | null
  applicationType?: DccApplicationType
  userNames?: Map<number, string>
  readAuthority?: () => string
  bodyReadContext?: DccBodyReadContext
  contextKey?: string
  contextError?: string
}>()
const groups = ref<ControlledApprovalGroup[]>([])
const loading = ref(false), error = ref(''), versionFilter = ref(''), typeFilter = ref('')
const expandedRows = ref<string[]>([])
const expandedGroups = ref<string[]>([])
const versions = computed(() => [...new Set(groups.value.map(group => group.version))])
const types = computed(() => [...new Set(groups.value.map(group => group.applicationLabel))]
  .map(label => ({ value: label, label })))
const visibleGroups = computed(() => props.contextError ? [] : filterControlledApprovalGroups(groups.value, versionFilter.value, typeFilter.value))
const records = computed(() => visibleGroups.value.flatMap(group => [
  { key: group.key, group },
  ...(expandedGroups.value.includes(group.key)
    ? [...group.rows].sort((a, b) => formatControlledFileDateTime(a.time).localeCompare(formatControlledFileDateTime(b.time))).map(row => ({ ...row, key: `${group.key}:${row.key}` }))
    : [])
]))
const duplicateGroup = (group: ControlledApprovalGroup) => groups.value.filter(item => item.version === group.version).length > 1
const groupOrdinal = (group: ControlledApprovalGroup) => groups.value.filter(item => item.version === group.version).findIndex(item => item.key === group.key) + 1
const toggleGroup = (key: string) => {
  expandedGroups.value = expandedGroups.value.includes(key) ? expandedGroups.value.filter(value => value !== key) : [...expandedGroups.value, key]
}
const toggleRow = (key: string) => {
  expandedRows.value = expandedRows.value.includes(key) ? expandedRows.value.filter(value => value !== key) : [...expandedRows.value, key]
}
const rowClass = ({ row }: { row: { key: string; group?: ControlledApprovalGroup } }) => row.group ? 'approval-group-row'
  : expandedRows.value.includes(row.key) ? 'approval-expanded-row' : ''
const spanGroup = ({ row, columnIndex }: { row: { group?: ControlledApprovalGroup }; columnIndex: number }) =>
  row.group ? columnIndex === 0 ? [1, 6] : [0, 0] : [1, 1]
const opinion = (value?: string | null) => {
  const text = getDccSignatureOpinionLabel(value)
  return !text || text === '未记录' ? '—' : text
}
let generation = 0
watch(() => [props.file, props.bpmRound, props.applicationType, props.contextKey, props.contextError,
  JSON.stringify(props.bodyReadContext), props.readAuthority?.()], async () => {
  const token = ++generation, authority = props.readAuthority?.()
  const current = () => token === generation && authority === props.readAuthority?.()
  groups.value = []
  error.value = ''
  expandedRows.value = []
  expandedGroups.value = []
  versionFilter.value = ''
  typeFilter.value = ''
  loading.value = true
  try {
    if (props.contextError) throw new Error(props.contextError)
    if (props.bodyReadContext) serializeDccBodyReadContext(props.file.id, props.bodyReadContext)
    const result = await loadControlledApprovalGroups(props.file, {
      detail: (id, context) => getControlledFile(id, { bodyReadContext: context }),
      rounds: (id, context) => getControlledFileApplicationRounds(id, context ? serializeDccBodyReadContext(id, context) : undefined),
      evidence: (id, type, round, context) => getControlledFileApplicationEvidence(id, type, round,
        context ? serializeDccBodyReadContext(id, context) : undefined)
    }, props.userNames, props.bodyReadContext)
    if (current()) groups.value = result
  } catch {
    if (current()) error.value = props.contextError || '审批记录读取失败，请重试。'
  } finally {
    if (current()) loading.value = false
  }
}, { immediate: true, flush: 'sync' })
onBeforeUnmount(() => { generation++ })
</script>
<style scoped>
.controlled-detail-approval { font-size: 14px; line-height: 22px; color: var(--el-text-color-primary); }
.controlled-detail-card { padding: 20px; border: 1px solid var(--el-border-color-light); border-radius: 6px; background: var(--el-bg-color); min-width: 0; }
.approval-heading { display: flex; align-items: center; justify-content: space-between; gap: 12px; flex-wrap: wrap; margin-bottom: 12px; }
h3 { margin: 0; font-size: 18px; line-height: 26px; font-weight: 600; }
.approval-filters, .approval-filters label { display: flex; align-items: center; gap: 8px; }
.approval-filters { gap: 16px; flex-wrap: wrap; }
.approval-filters :deep(.el-select) { width: 130px; }
.approval-item, .approval-expand { font-weight: 600; }
.approval-expand { appearance: none; background: none; border: 0; padding: 0; font: inherit; font-weight: 600; color: inherit; cursor: pointer; text-align: left; line-height: 22px; }
.approval-expand span { color: var(--el-color-primary); margin-left: 6px; }
.approval-expand:focus-visible, .approval-group-title:focus-visible { outline: 2px solid var(--el-color-primary); outline-offset: 2px; }
.approval-secondary { margin-top: 4px; color: var(--el-text-color-secondary); font-size: 13px; line-height: 20px; }
.approval-group-title { appearance: none; background: none; border: 0; padding: 0; width: 100%; font: inherit; color: inherit; cursor: pointer; text-align: left; display: flex; align-items: baseline; gap: 10px; flex-wrap: wrap; }
.approval-group-arrow { color: var(--el-color-primary); }
.approval-group-time { font-size: 13px; line-height: 20px; }
.approval-time, .approval-group-time { font-variant-numeric: tabular-nums; }
:deep(.el-table) { font-size: 14px; line-height: 22px; color: var(--el-text-color-primary); }
:deep(.el-table__cell) { vertical-align: top; }
:deep(.el-table .cell) { overflow-wrap: anywhere; padding: 0 12px; line-height: 22px; }
:deep(.el-table th.el-table__cell) { background: var(--el-fill-color-light); font-weight: 500; color: var(--el-text-color-primary); }
:deep(.el-table .approval-group-row) { --el-table-tr-bg-color: #e9f5f4; }
:deep(.el-table .approval-expanded-row) { --el-table-tr-bg-color: #f0f9f8; }
:deep(.approval-expanded-row td:first-child) { box-shadow: inset 2px 0 var(--el-color-primary); }
</style>
