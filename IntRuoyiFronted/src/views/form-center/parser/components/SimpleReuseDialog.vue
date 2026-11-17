<template>
  <el-dialog v-model="visible" title="工序与工艺复用" width="min(960px, 95vw)" :close-on-click-modal="false">
    <div class="reuse-list">
      <section v-for="row in rows" :key="row.processIndex" class="reuse-row">
        <div class="reuse-heading">
          <strong>{{ row.processIndex + 1 }}. {{ row.name }}</strong>
          <el-tag size="small" :type="choice(row).processId ? 'info' : 'success'">
            {{ choice(row).processId ? '复用工序' : '新建工序及工艺' }}
          </el-tag>
        </div>
        <el-select :model-value="choice(row).processId || 0" :aria-label="`${row.name}工序选择`"
          class="reuse-select" @change="selectProcess(row, $event)">
          <el-option :value="0" label="新建独立工序及工艺" />
          <el-option v-if="choice(row).processId && !selected(row)" :value="choice(row).processId!" label="原工序已变化，请重新选择" disabled />
          <el-option v-for="candidate in row.candidates" :key="candidate.processId" :value="candidate.processId"
            :label="candidateLabel(candidate)" />
        </el-select>
        <template v-if="selected(row)">
          <div class="reuse-context">
            <span>使用车间：{{ workshopLabel(selected(row)!) }}</span>
            <span v-if="selected(row)!.productName">产品／项目：{{ selected(row)!.productName }}</span>
          </div>
          <div v-for="group in selected(row)!.groups" :key="group.groupIndex" class="reuse-group">
            <div>
              <strong>参数组 {{ group.groupIndex + 1 }}</strong>
              <div class="reuse-parameters">
                <span v-for="parameter in group.parameters" :key="parameter.parameterName">
                  {{ parameter.parameterName }}：{{ parameter.standardText }}
                  <template v-if="parameter.unit">（{{ parameter.unit }}）</template>
                </span>
              </div>
            </div>
            <el-select :model-value="choice(row).craftSelections.find(c => c.groupIndex === group.groupIndex)?.craftId || 0"
              :aria-label="`${row.name}参数组${group.groupIndex + 1}工艺选择`"
              @change="selectCraft(row, group.groupIndex, $event)">
              <el-option :value="0" :label="group.crafts.length ? '新建独立工艺' : '参数不同，新建工艺'" />
              <el-option v-if="staleCraft(row, group.groupIndex)" :value="staleCraft(row, group.groupIndex)!" label="原工艺已变化，请重新选择" disabled />
              <el-option v-for="craft in group.crafts" :key="craft.id" :value="craft.id"
                :label="`${craft.code} · ${craft.name}（参数一致）`" />
            </el-select>
          </div>
        </template>
      </section>
    </div>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :disabled="invalidSelection" @click="confirm">确认并预览</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { SimpleParserRequest, SimpleParserReuseMatch, SimpleParserReuseCandidate } from '@/api/mes/pro/batchrecordreport/simpleParser'
type Selection = SimpleParserRequest['processSelections'][number]
const emit = defineEmits<{ confirm: [selections: Selection[]] }>()
const visible = ref(false)
const rows = ref<SimpleParserReuseMatch[]>([])
const selections = ref<Selection[]>([])
const choice = (row: SimpleParserReuseMatch) => selections.value.find(s => s.processIndex === row.processIndex)!
const selected = (row: SimpleParserReuseMatch) => row.candidates.find(c => c.processId === choice(row).processId)
function staleCraft(row: SimpleParserReuseMatch, groupIndex: number) {
  const id = choice(row).craftSelections.find(c => c.groupIndex === groupIndex)?.craftId
  return id && !selected(row)?.groups.find(g => g.groupIndex === groupIndex)?.crafts.some(c => c.id === id) ? id : undefined
}
const invalidSelection = computed(() => rows.value.some(row =>
  (choice(row).processId && !selected(row)) || choice(row).craftSelections.some(c => staleCraft(row, c.groupIndex))))
const workshopLabel = (candidate: SimpleParserReuseCandidate) => candidate.workshops.map(w => w.name).join('、') || '暂无使用车间'
const candidateLabel = (candidate: SimpleParserReuseCandidate) =>
  `${workshopLabel(candidate)} · ${candidate.code} · ${candidate.name}${candidate.currentWorkshop ? '（当前车间）' : ''}`
function selectCraft(row: SimpleParserReuseMatch, groupIndex: number, craftId: number) {
  const selection = choice(row)
  selection.craftSelections = selection.craftSelections.filter(c => c.groupIndex !== groupIndex)
  if (craftId) selection.craftSelections.push({ groupIndex, craftId })
}
function selectProcess(row: SimpleParserReuseMatch, processId: number) {
  const selection = choice(row)
  selection.processId = processId || undefined
  selection.craftSelections = []
  const candidate = selected(row)
  // 用户选择工序后，唯一完全匹配的工艺在确认清单中明确选中；多候选不猜测。
  for (const group of candidate?.groups || []) {
    if (group.crafts.length === 1) selection.craftSelections.push({ groupIndex: group.groupIndex, craftId: group.crafts[0].id })
  }
}
function open(matches: SimpleParserReuseMatch[], current: Selection[]) {
  rows.value = matches.filter(row => row.candidates.length > 0 || current.some(c => c.processIndex === row.processIndex && c.processId))
  selections.value = current.map(item => ({ ...item, craftSelections: item.craftSelections.map(c => ({ ...c })) }))
  visible.value = true
}
function confirm() {
  if (invalidSelection.value) return
  visible.value = false
  emit('confirm', selections.value)
}
defineExpose({ open })
</script>

<style scoped>
.reuse-list { max-height: 65vh; overflow-y: auto; }
.reuse-row { padding: 16px 0; border-bottom: 1px solid var(--el-border-color-lighter); }
.reuse-row:first-child { padding-top: 0; }
.reuse-heading { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 12px; }
.reuse-select { width: 100%; }
.reuse-context { display: flex; flex-wrap: wrap; gap: 8px 24px; margin-top: 8px; color: var(--el-text-color-secondary); font-size: 13px; }
.reuse-group { display: grid; grid-template-columns: 1fr minmax(230px, 40%); align-items: center; gap: 16px; margin-top: 16px; }
.reuse-parameters { display: flex; flex-wrap: wrap; gap: 4px 16px; margin-top: 4px; font-size: 13px; color: var(--el-text-color-regular); }
@media (max-width: 640px) { .reuse-group { grid-template-columns: 1fr; gap: 8px; } }
</style>
