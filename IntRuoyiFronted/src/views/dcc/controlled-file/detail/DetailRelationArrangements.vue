<template>
  <section class="relation-arrangements">
    <el-table v-if="relations.length" :data="relations" row-key="masterId">
      <el-table-column v-if="remediable" label="整改" width="68">
        <template #default="{ row }"><el-checkbox :model-value="Boolean(selected(row.masterId))" :aria-label="`${row.fileName}需要整改`" :disabled="disabled" @update:model-value="value => toggle(row.masterId, Boolean(value))" /></template>
      </el-table-column>
      <el-table-column label="关联文件" min-width="220">
        <template #default="{ row }">
          <div>{{ row.fileName }}</div>
          <div class="relation-meta"><span>{{ row.fileNumber || '文件编号未记录' }}</span><el-tag type="info">审阅版本 {{ getDccVersionDisplayText(row.versionNo) }}</el-tag><el-tag :type="row.supplemental ? 'success' : 'info'">{{ row.supplemental ? '本次补充' : '原有关联' }}</el-tag></div>
        </template>
      </el-table-column>
      <el-table-column v-if="remediable" label="负责人" min-width="180">
        <template #default="{ row }">
          <div class="required-control"><span v-if="selected(row.masterId)" class="required-mark" aria-hidden="true">*</span>
            <UserSelectV2 v-if="selected(row.masterId)" v-model="selected(row.masterId)!.assigneeUserId" :disabled="disabled" clearable :user-options="users.map(user => ({ ...user, nickname: user.name }))" />
            <el-input v-else disabled placeholder="—" :aria-label="`${row.fileName}整改负责人`" />
          </div>
          <p v-if="rowErrors[row.masterId]?.assignee && !users.some(user => user.id === selected(row.masterId)?.assigneeUserId)" class="row-error" role="alert">{{ rowErrors[row.masterId].assignee }}</p>
        </template>
      </el-table-column>
      <el-table-column v-if="remediable" label="整改期限" min-width="235">
        <template #default="{ row }">
          <div class="required-control"><span v-if="selected(row.masterId)" class="required-mark" aria-hidden="true">*</span>
            <el-date-picker v-if="selected(row.masterId)" v-model="selected(row.masterId)!.dueAt" :disabled="disabled" type="datetime" @change="clearDueError(row.masterId)" value-format="YYYY-MM-DD HH:mm:ss" :aria-label="`${row.fileName}整改期限`" />
            <el-input v-else disabled placeholder="—" :aria-label="`${row.fileName}整改期限`" />
          </div>
          <p v-if="rowErrors[row.masterId]?.dueAt" class="row-error" role="alert">{{ rowErrors[row.masterId].dueAt }}</p>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90">
        <template #default="{ row }"><el-button v-if="row.supplemental" link type="danger" :disabled="disabled" :aria-label="`移除 ${row.fileName}`" @click="emit('remove', row.masterId)">移除</el-button><span v-else class="immutable-relation">不可移除</span></template>
      </el-table-column>
    </el-table>
  </section>
</template>
<script setup lang="ts">
import UserSelectV2 from '@/views/system/user/components/UserSelectV2.vue'
import { ref, watch } from 'vue'
import { getDccVersionDisplayText } from '../shared/version-display'
import type { ArrangementCommand, ArrangementForm } from '../relations/arrangement-form'
import { isWorkflowId } from '../workflow/workflow-actions'
const props = withDefaults(defineProps<{
  contextKey: string
  relations: Array<{ masterId: string; fileName: string; fileNumber?: string; versionNo: string; supplemental?: boolean }>
  remediable?: boolean
  users: Array<{ id: string; name: string }>
  saved: ArrangementCommand[]
  disabled: boolean
}>(), { remediable: true })
const emit = defineEmits<{ remove: [masterId: string]; 'count-change': [count: number] }>()
const rowErrors = ref<Record<string, { assignee?: string; dueAt?: string }>>({})
const rows = ref<ArrangementForm[]>([])
watch(
  () => [props.contextKey, props.saved],
  () => {
    rowErrors.value = {}
    rows.value = props.saved.map((row) => ({ ...row }))
  },
  { immediate: true }
)
watch(() => props.relations.map(file => file.masterId), ids => {
  rows.value = rows.value.filter(row => ids.includes(row.relatedMasterId))
})
watch(rows, () => {
  emit('count-change', rows.value.length)
}, { deep: true, immediate: true })
const clearDueError = (id: string) => { if (rowErrors.value[id]) rowErrors.value[id].dueAt = undefined }
const selected = (id: string) => rows.value.find((row) => row.relatedMasterId === id)
const toggle = (id: string, checked: boolean) => {
  if (props.disabled || !props.remediable) return
  if (checked && !selected(id)) rows.value.push({ relatedMasterId: id })
  else if (!checked) rows.value = rows.value.filter((row) => row.relatedMasterId !== id)
  delete rowErrors.value[id]
}
const validate = (): ArrangementCommand[] => {
  if (!props.remediable) return []
  rowErrors.value = {}
  for (const row of rows.value) {
    const errors: { assignee?: string; dueAt?: string } = {}
    if (!row.assigneeUserId || !props.users.some(user => user.id === row.assigneeUserId)) errors.assignee = '请选择有效整改负责人'
    if (!row.dueAt) errors.dueAt = '请选择整改期限'
    if (errors.assignee || errors.dueAt) rowErrors.value[row.relatedMasterId] = errors
  }
  if (Object.keys(rowErrors.value).length) throw new Error('请填写对应行的整改负责人和整改期限')
  const masters = new Set<string>()
  return rows.value.map((row) => {
    if (
      !isWorkflowId(row.relatedMasterId) ||
      masters.has(row.relatedMasterId) ||
      !props.relations.some((relation) => relation.masterId === row.relatedMasterId)
    )
      throw new Error('整改关联文件不属于本次申请')
    masters.add(row.relatedMasterId)
    if (!row.assigneeUserId || !props.users.some((user) => user.id === row.assigneeUserId))
      throw new Error('请选择有效整改负责人')
    const matched = /^(\d{4})-(\d{2})-(\d{2}) (\d{2}):(\d{2}):(\d{2})$/.exec(row.dueAt || '')
    if (!matched) { rowErrors.value[row.relatedMasterId] = { dueAt: '请选择精确到秒的整改期限' }; throw new Error('请选择精确到秒的整改期限') }
    const [year, month, day, hour, minute, second] = matched.slice(1).map(Number)
    const date = new Date(0)
    date.setUTCFullYear(year, month - 1, day)
    date.setUTCHours(hour, minute, second, 0)
    if (
      year < 1 ||
      date.getUTCFullYear() !== year ||
      date.getUTCMonth() !== month - 1 ||
      date.getUTCDate() !== day ||
      date.getUTCHours() !== hour ||
      date.getUTCMinutes() !== minute ||
      date.getUTCSeconds() !== second
    )
      { rowErrors.value[row.relatedMasterId] = { dueAt: '整改期限不是有效日期' }; throw new Error('整改期限不是有效日期') }
    return {
      relatedMasterId: row.relatedMasterId,
      assigneeUserId: row.assigneeUserId,
      dueAt: row.dueAt!
    }
  })
}
defineExpose({ validate })
</script>

<style scoped>
.relation-meta { display: flex; align-items: center; flex-wrap: wrap; gap: 6px; margin-top: 6px; color: var(--el-text-color-secondary); font-size: 12px; }
.required-control { display: flex; align-items: center; gap: 6px; }
.required-control :deep(.el-date-editor) { width: 100%; }
.required-mark, .row-error { color: var(--el-color-danger); }
.row-error { margin: 4px 0 0; font-size: 12px; }
.immutable-relation { font-size: 12px; color: var(--el-text-color-secondary); }
</style>
