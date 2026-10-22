<template>
  <div>
    <p>仅勾选的关联文件在本次受控成功后生成整改任务。负责人和期限随本次会签安排冻结。</p>
    <el-table :data="relations" row-key="masterId">
      <el-table-column label="整改" width="64"><template #default="{ row }"><el-checkbox :disabled="readonly" :model-value="Boolean(arrangement(row.masterId))" @change="value => toggle(row.masterId, Boolean(value))" /></template></el-table-column>
      <el-table-column prop="fileName" label="关联文件" /><el-table-column prop="versionNo" label="受控版本" width="110" />
      <el-table-column label="整改负责人"><template #default="{ row }"><el-select v-if="arrangement(row.masterId)" :disabled="readonly" :model-value="arrangement(row.masterId)?.assigneeUserId" placeholder="请选择负责人" @update:model-value="value => change(row.masterId, { assigneeUserId: value })"><el-option v-for="user in assignees" :key="user.id" :label="user.name" :value="user.id" /></el-select></template></el-table-column>
      <el-table-column label="整改期限"><template #default="{ row }"><el-date-picker v-if="arrangement(row.masterId)" :disabled="readonly" :model-value="arrangement(row.masterId)?.dueAt" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="请选择期限" @update:model-value="value => change(row.masterId, { dueAt: value })" /></template></el-table-column>
    </el-table>
  </div>
</template>
<script setup lang="ts">
import type { FileCandidate } from './selector-state'
import { validateArrangementForm } from './arrangement-form'
import type { ArrangementForm } from './arrangement-form'
export type { ArrangementForm } from './arrangement-form'
const props = defineProps<{ modelValue: ArrangementForm[]; relations: FileCandidate[]; assignees: { id: string; name: string }[]; readonly?: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [rows: ArrangementForm[]] }>()
const arrangement = (masterId: string) => props.modelValue.find(row => row.relatedMasterId === masterId)
const toggle = (masterId: string, selected: boolean) => {
  if (props.readonly) return
  if (selected && !arrangement(masterId)) emit('update:modelValue', [...props.modelValue, { relatedMasterId: masterId }])
  if (!selected) emit('update:modelValue', props.modelValue.filter(row => row.relatedMasterId !== masterId))
}
const change = (masterId: string, fields: Partial<ArrangementForm>) => {
  if (props.readonly) return
  emit('update:modelValue', props.modelValue.map(row => row.relatedMasterId === masterId ? { ...row, ...fields } : { ...row }))
}
defineExpose({validate:()=>validateArrangementForm(props.modelValue,props.relations,props.assignees)})
</script>
