<template>
  <el-dialog v-model="visible" title="工序与工艺复用" width="min(1400px, calc(100vw - 24px))"
    class="simple-reuse-dialog" :close-on-click-modal="false">
    <div class="reuse-list">
      <section v-for="row in rows" :key="row.processIndex" class="reuse-row">
        <div class="reuse-heading">
          <div class="reuse-title">
            <span class="reuse-number">{{ row.processIndex + 1 }}</span>
            <h3>{{ row.name }}</h3>
          </div>
          <el-tag class="reuse-status" :type="choice(row).processId ? 'info' : 'success'">
            {{ choice(row).processId ? '复用工序' : '新建工序及工艺' }}
          </el-tag>
        </div>
        <el-select :model-value="choice(row).processId || 0" :aria-label="`${row.name}工序选择`"
          class="reuse-select" @change="selectProcess(row, $event)">
          <el-option v-if="choice(row).processId && !selected(row)" :value="choice(row).processId!" label="原工序已变化，请重新选择" disabled />
          <el-option v-for="candidate in row.candidates" :key="candidate.processId" :value="candidate.processId"
            :label="candidateLabel(candidate)" />
          <el-option :value="0" label="新建独立工序及工艺" />
        </el-select>
        <template v-if="selected(row)">
          <div class="reuse-context">
            <span>使用车间：{{ workshopLabel(selected(row)!) }}</span>
            <span v-if="selected(row)!.productName">产品／项目：{{ selected(row)!.productName }}</span>
          </div>
          <div v-for="group in selected(row)!.groups" :key="group.groupIndex" class="reuse-group">
            <div class="reuse-group-content">
              <h4>参数组 {{ group.groupIndex + 1 }}</h4>
              <dl class="reuse-parameters">
                <div v-for="parameter in group.parameters" :key="parameter.parameterName" class="reuse-parameter">
                  <dt>{{ parameter.parameterName }}</dt>
                  <dd>{{ parameterValue(parameter) }}</dd>
                </div>
              </dl>
            </div>
            <div class="reuse-craft-choice">
              <span class="reuse-craft-label">工艺处理方式</span>
              <el-select :model-value="choice(row).craftSelections.find(c => c.groupIndex === group.groupIndex)?.craftId || 0"
                :aria-label="`${row.name}参数组${group.groupIndex + 1}工艺选择`"
                @change="selectCraft(row, group.groupIndex, $event)">
                <el-option :value="0" :label="group.crafts.length ? '新建独立工艺' : '参数不同，新建工艺'" />
                <el-option v-if="staleCraft(row, group.groupIndex)" :value="staleCraft(row, group.groupIndex)!" label="原工艺已变化，请重新选择" disabled />
                <el-option v-for="craft in group.crafts" :key="craft.id" :value="craft.id"
                  :label="`${craft.code} · ${craft.name}（参数一致）`" />
              </el-select>
            </div>
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
function parameterValue(parameter: { standardText: string; unit?: string }) {
  const text = parameter.standardText.trim()
  const unit = parameter.unit?.trim()
  return unit && !text.toLowerCase().endsWith(unit.toLowerCase()) ? `${text} ${unit}` : text
}
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
function open(matches: SimpleParserReuseMatch[], current: Selection[], newProcessIndices: number[] = []) {
  rows.value = matches.filter(row => row.candidates.length > 0 || current.some(c => c.processIndex === row.processIndex && c.processId))
  selections.value = current.map(item => ({ ...item, craftSelections: item.craftSelections.map(c => ({ ...c })) }))
  // 优先复用当前车间的同名工序；保留用户已明确选择的复用或独立新建。
  const usedIds = new Set(current.flatMap(item => item.processId ? [item.processId] : []))
  for (const row of rows.value) {
    if (choice(row).processId || newProcessIndices.includes(row.processIndex)) continue
    const available = row.candidates.filter(candidate => !usedIds.has(candidate.processId))
    const candidate = available.find(candidate => candidate.currentWorkshop) || available[0]
    if (!candidate) continue
    selectProcess(row, candidate.processId)
    usedIds.add(candidate.processId)
  }
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
:global(.el-dialog.simple-reuse-dialog) {
  --el-dialog-padding-primary: 0;
  display: flex;
  flex-direction: column;
  max-height: calc(100dvh - 32px);
  margin: 16px auto;
  padding: 0;
  overflow: hidden;
  border-radius: 14px;
  color: var(--el-text-color-primary);
  font-family: 'Microsoft YaHei', 'PingFang SC', sans-serif;
  box-shadow: 0 16px 48px #1d293926;
}
:global(.simple-reuse-dialog .el-dialog__header) {
  margin: 0;
  padding: 22px 64px 20px 28px;
  border-bottom: 1px solid var(--el-border-color);
}
:global(.simple-reuse-dialog .el-dialog__title) { color: #1d2935; font-size: 28px; font-weight: 700; line-height: 1.3; }
:global(.simple-reuse-dialog .el-dialog__headerbtn) { top: 16px; right: 16px; width: 40px; height: 40px; font-size: 24px; }
:global(.simple-reuse-dialog .el-dialog__body) { min-height: 0; padding: 0; overflow: hidden; }
:global(.simple-reuse-dialog .el-dialog__footer) {
  padding: 16px 26px;
  border-top: 1px solid var(--el-border-color);
}
:global(.simple-reuse-dialog .el-dialog__footer .el-button) { min-width: 104px; height: 44px; font-size: 16px; border-radius: 6px; }
:global(.simple-reuse-dialog .el-dialog__footer .el-button + .el-button) { min-width: 156px; margin-left: 16px; }
.reuse-list {
  max-height: calc(100dvh - 190px);
  padding: 20px 26px;
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-gutter: stable;
  background: var(--el-fill-color-light);
}
.reuse-row { padding: 16px 24px 20px; border: 1px solid var(--el-border-color); border-radius: 10px; background: var(--el-bg-color); }
.reuse-row + .reuse-row { margin-top: 18px; }
.reuse-heading, .reuse-title { display: flex; align-items: center; gap: 14px; }
.reuse-heading { justify-content: space-between; margin-bottom: 12px; }
.reuse-title { min-width: 0; }
.reuse-title h3 { margin: 0; color: #1d2935; font-size: 22px; font-weight: 700; line-height: 1.4; overflow-wrap: anywhere; }
.reuse-number { display: grid; place-items: center; min-width: 32px; height: 32px; padding: 0 4px; border-radius: 6px; background: #c7f1f0; color: #009c9b; font-size: 23px; font-weight: 700; }
.reuse-status { flex-shrink: 0; font-size: 14px; border-radius: 6px; }
.reuse-select, .reuse-craft-choice .el-select { width: 100%; }
.reuse-row :deep(.el-select__wrapper) { min-height: 40px; font-size: 16px; border-radius: 6px; }
.reuse-row :deep(.el-select__selected-item) { color: #1d2935; }
.reuse-context { display: flex; flex-wrap: wrap; gap: 6px 40px; margin-top: 10px; color: var(--el-text-color-secondary); font-size: 15px; }
.reuse-group { display: grid; grid-template-columns: minmax(0, 2fr) minmax(260px, 1fr); align-items: stretch; gap: 28px; margin-top: 16px; padding-top: 14px; border-top: 1px solid var(--el-border-color); }
.reuse-group-content { min-width: 0; }
.reuse-group h4 { margin: 0 0 8px; color: #1d2935; font-size: 18px; line-height: 1.4; }
.reuse-parameters { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 14px 20px; margin: 0; }
.reuse-parameter { min-width: 0; }
.reuse-parameter dt { margin-bottom: 6px; color: var(--el-text-color-secondary); font-size: 15px; overflow-wrap: anywhere; }
.reuse-parameter dd { margin: 0; color: #1d2935; font-size: 18px; line-height: 1.5; overflow-wrap: anywhere; }
.reuse-craft-choice { display: flex; flex-direction: column; justify-content: space-between; gap: 12px; padding-left: 28px; border-left: 1px solid var(--el-border-color); }
.reuse-craft-label { color: var(--el-text-color-secondary); font-size: 15px; }
@media (max-width: 1000px) {
  .reuse-row { padding: 16px; }
  .reuse-group { grid-template-columns: minmax(0, 1.5fr) minmax(230px, 1fr); gap: 20px; }
  .reuse-parameters { grid-template-columns: repeat(3, minmax(0, 1fr)); }
  .reuse-craft-choice { padding-left: 20px; justify-content: flex-start; }
}
@media (max-width: 640px) {
  :global(.simple-reuse-dialog .el-dialog__header) { padding: 18px 52px 16px 16px; }
  :global(.simple-reuse-dialog .el-dialog__title) { font-size: 21px; }
  :global(.simple-reuse-dialog .el-dialog__footer) { padding: 12px 16px; }
  .reuse-list { padding: 14px 12px; }
  .reuse-title { gap: 8px; }
  .reuse-title h3 { font-size: 18px; }
  .reuse-group { grid-template-columns: minmax(0, 1fr); gap: 16px; }
  .reuse-parameters { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .reuse-craft-choice { padding: 0; border: 0; gap: 8px; }
}
</style>
