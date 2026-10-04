<template>
  <section>
    <p>勾选关联文件，填写本次整改负责人和期限。安排随本次指派签名一并提交。</p>
    <el-table :data="relations" row-key="masterId">
      <el-table-column label="整改" width="68"
        ><template #default="{ row }"
          ><el-checkbox
            :model-value="Boolean(selected(row.masterId))"
            :disabled="disabled"
            @change="(value) => toggle(row.masterId, Boolean(value))" /></template
      ></el-table-column>
      <el-table-column label="关联文件" prop="fileName" /><el-table-column
        label="审阅版本"
        prop="versionNo"
        width="120"
      />
      <el-table-column label="负责人"
        ><template #default="{ row }"
          ><el-select
            v-if="selected(row.masterId)"
            v-model="selected(row.masterId)!.assigneeUserId"
            :disabled="disabled"
            filterable
            ><el-option
              v-for="user in users"
              :key="user.id"
              :label="user.name"
              :value="user.id" /></el-select></template
      ></el-table-column>
      <el-table-column label="整改期限"
        ><template #default="{ row }"
          ><el-date-picker
            v-if="selected(row.masterId)"
            v-model="selected(row.masterId)!.dueAt"
            :disabled="disabled"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss" /></template
      ></el-table-column>
    </el-table>
  </section>
</template>
<script setup lang="ts">
import { ref, watch } from 'vue'
import type { ArrangementCommand, ArrangementForm } from '../relations/arrangement-form'
import { isWorkflowId } from '../workflow/workflow-actions'
const props = defineProps<{
  contextKey: string
  relations: Array<{ masterId: string; fileName: string; versionNo: string }>
  users: Array<{ id: string; name: string }>
  saved: ArrangementCommand[]
  disabled: boolean
}>()
const rows = ref<ArrangementForm[]>([])
watch(
  () => [props.contextKey, props.saved],
  () => {
    rows.value = props.saved.map((row) => ({ ...row }))
  },
  { immediate: true }
)
const selected = (id: string) => rows.value.find((row) => row.relatedMasterId === id)
const toggle = (id: string, checked: boolean) => {
  if (props.disabled) return
  if (checked && !selected(id)) rows.value.push({ relatedMasterId: id })
  else if (!checked) rows.value = rows.value.filter((row) => row.relatedMasterId !== id)
}
const validate = (): ArrangementCommand[] => {
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
    if (!matched) throw new Error('请选择精确到秒的整改期限')
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
      throw new Error('整改期限不是有效日期')
    return {
      relatedMasterId: row.relatedMasterId,
      assigneeUserId: row.assigneeUserId,
      dueAt: row.dueAt!
    }
  })
}
defineExpose({ validate })
</script>
