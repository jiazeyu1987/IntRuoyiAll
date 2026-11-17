<template>
  <Dialog v-model="visible" title="影响确认" width="1040px" :close-on-click-modal="false">
    <div v-if="draft" v-loading="loading" class="shared-change">
      <el-form label-width="92px" :disabled="loading">
        <el-form-item v-if="draft.action === 'REPLACE'" label="替换为" required>
          <el-select v-model="draft.targetId" filterable class="w-full" @change="targetChanged">
            <el-option v-for="option in targets" :key="option.id" :value="option.id" :label="[option.code, option.name, option.productName].filter(Boolean).join(' / ')" />
          </el-select>
        </el-form-item>
        <el-form-item label="变更原因" required><el-input v-model="draft.reason" maxlength="1000" show-word-limit /></el-form-item>
      </el-form>
      <el-alert v-if="error" type="error" :closable="false" :title="error" />
      <template v-if="impact">
        <el-table :data="differences" border size="small" max-height="190px">
          <el-table-column prop="label" label="修改内容" width="130" />
          <el-table-column prop="before" label="修改前" min-width="260" show-overflow-tooltip />
          <el-table-column prop="after" label="修改后" min-width="260" show-overflow-tooltip />
        </el-table>
        <div v-if="parameterGroups.length" class="shared-change__schemes">
          <div v-for="group in parameterGroups" :key="group.key" class="shared-change__scheme">
            <div><strong>{{ group.processName }}</strong><span class="shared-change__muted">{{ group.members.length }} 处引用</span></div>
            <div class="shared-change__muted">{{ group.summary }}</div>
            <el-select v-model="group.selected" filterable placeholder="选择替换后工艺" @change="needsPreview = true">
              <el-option v-if="draft.subjectType === 'DEVICE'" :value="0" label="无工艺参数" />
              <el-option v-for="craft in group.options" :key="craft.id" :value="craft.id" :label="craft.code + ' / ' + craft.name + ' / ' + parameterSummary(craft.parametersJson)" />
            </el-select>
          </div>
        </div>
        <el-alert v-if="impact.blockers.length" type="error" :closable="false" :title="impact.blockers.join('；')" />
        <div class="shared-change__count">受影响路线 {{ impact.routes.length }} 条</div>
        <el-table :data="pagedRoutes" border stripe max-height="330px">
          <el-table-column prop="routeCode" label="路线编码" min-width="120" />
          <el-table-column prop="routeName" label="路线名称" min-width="160" />
          <el-table-column label="版本" width="130"><template #default="{ row }">{{ row.versionNo }} → {{ row.nextVersionNo }}</template></el-table-column>
          <el-table-column label="车间 / 工序" min-width="240"><template #default="{ row }"><div v-for="node in row.nodes" :key="node.routeProcessId">{{ node.workshopName || '未关联车间' }} / {{ node.processName || node.processId }}</div></template></el-table-column>
        </el-table>
        <el-pagination v-if="impact.routes.length > 20" v-model:current-page="page" :page-size="20" :total="impact.routes.length" layout="total, prev, pager, next" />
      </template>
    </div>
    <template #footer>
      <el-button :disabled="loading" @click="visible = false">取消</el-button>
      <el-button v-if="needsPreview" type="primary" :loading="loading" :disabled="!selectionsComplete" @click="refreshPreview">预览影响</el-button>
      <el-button v-else type="primary" :loading="loading" :disabled="!canSubmit" @click="submit">确认并提交</el-button>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import { SharedChangeApi, type SharedChangeRequest, type SharedImpact, type SharedSubject } from '@/api/mes/pro/sharedChange'
import type { Craft } from '@/api/mes/pro/craft'
const message = useMessage()
const visible = ref(false), loading = ref(false), error = ref(''), page = ref(1), needsPreview = ref(false)
const impact = ref<SharedImpact>(), draft = ref<SharedChangeRequest>()
const targets = ref<Array<{ id: number; code: string; name: string; productName?: string }>>([])
type Group = { key: string; processName: string; summary: string; selected?: number; options: Craft[]; members: Array<{ routeId: number; routeProcessId: number; deviceIds: number[] }> }
const parameterGroups = ref<Group[]>([])
const emit = defineEmits<{ submitted: [] }>()
const selectionsComplete = computed(() => !!draft.value && (draft.value.action !== 'REPLACE' || !!draft.value.targetId) && parameterGroups.value.every(g => g.selected !== undefined))
const canSubmit = computed(() => !!draft.value?.reason.trim() && !!impact.value && !impact.value.blockers.length && !impact.value.pendingSelectionCount && !needsPreview.value && selectionsComplete.value)
const pagedRoutes = computed(() => impact.value?.routes.slice((page.value - 1) * 20, page.value * 20) || [])
const labels: Record<string, string> = { code: '编码', name: '名称', attention: '工序说明', sopFileUrl: 'SOP文件', productName: '产品名称', status: '状态', remark: '备注', brand: '品牌', specification: '规格型号', workshopId: '车间编号', machineryTypeId: '设备类型编号', processName: '工序名称', manualShiftCapacity: '人工班产能', standardHourlyCapacity: '小时产能', parametersJson: '工艺参数' }
const display = (value: unknown) => value === null || value === undefined || value === '' ? '—' : String(value)
const differences = computed(() => !impact.value ? [] : Object.entries(labels).filter(([key]) => display(impact.value!.before[key]) !== display(impact.value!.after[key])).map(([key, label]) => ({ label, before: key === 'parametersJson' ? parameterSummary(String(impact.value!.before[key])) : display(impact.value!.before[key]), after: key === 'parametersJson' ? parameterSummary(String(impact.value!.after[key])) : display(impact.value!.after[key]) })))
function parameterSummary(json: string) {
  return (JSON.parse(json) as Array<{ parameterName: string; standardText: string }>).map(p => `${p.parameterName}：${p.standardText}`).join('；')
}
const errorText = (e: unknown) => e instanceof Error ? e.message : String(e)
const open = async (request: SharedChangeRequest) => {
  draft.value = JSON.parse(JSON.stringify(request)); impact.value = undefined; targets.value = []; parameterGroups.value = []; page.value = 1; error.value = ''; visible.value = true
  needsPreview.value = request.action === 'REPLACE'; loading.value = true
  try {
    if (request.action === 'REPLACE') targets.value = await SharedChangeApi.options(request.subjectType, request.sourceId)
    else impact.value = await SharedChangeApi.preview(request.subjectType, draft.value!)
  } catch (e) { error.value = errorText(e); throw e } finally { loading.value = false }
}
const openReplacement = async (type: SharedSubject, id: number) => {
  const source = await SharedChangeApi.source(type, id)
  await open({ subjectType: type, action: 'REPLACE', sourceId: id, expectedFingerprint: source.fingerprint, requestId: crypto.randomUUID(), reason: '', nodes: [] })
}
const targetChanged = async () => {
  if (!draft.value?.targetId) return
  loading.value = true; error.value = ''; draft.value.nodes = []; parameterGroups.value = []; needsPreview.value = true
  try {
    impact.value = await SharedChangeApi.preview(draft.value.subjectType, draft.value)
    await buildGroups()
    if (!parameterGroups.value.length) needsPreview.value = false
  } catch (e) { impact.value = undefined; error.value = errorText(e); throw e } finally { loading.value = false }
}
async function buildGroups() {
  if (!draft.value || !impact.value || draft.value.subjectType === 'CRAFT') return
  const groups = new Map<string, Group>(), craftCache = new Map<number, Craft[]>()
  for (const route of impact.value.routes) for (const node of route.nodes) {
    const config = node.productionConfig
    if (!config) continue
    const rules: any[] = config.parameterRules || []
    if (draft.value.subjectType === 'PROCESS' && !rules.length) continue
    const processId = draft.value.subjectType === 'PROCESS' ? draft.value.targetId! : node.processId
    if (!craftCache.has(processId)) craftCache.set(processId, await SharedChangeApi.crafts(draft.value.subjectType, processId))
    const relevantRules = draft.value.subjectType === 'DEVICE' ? rules.filter(rule => (node.affectedDeviceIds || []).includes(rule.deviceId)) : rules
    const schemes = relevantRules.length ? Object.values(relevantRules.reduce((result: Record<string, { deviceIds: number[]; summary: string; definitions: unknown[] }>, rule: any) => {
      const id = String(rule.deviceId); if (!result[id]) result[id] = { deviceIds: [rule.deviceId], summary: '', definitions: [] }
      result[id].definitions.push(Object.fromEntries(['parameterName', 'standardText', 'unit', 'valueType', 'lowerLimit', 'upperLimit', 'decimalScale', 'defaultValue', 'defaultText', 'optionValuesJson'].map(key => [key, rule[key] ?? null])))
      result[id].summary += `${rule.parameterName}：${rule.standardText}；`; return result
    }, {})) : draft.value.subjectType === 'DEVICE' ? [{ deviceIds: node.affectedDeviceIds || [], summary: '无工艺参数', definitions: [] }] : []
    for (const scheme of schemes as Array<{deviceIds: number[]; summary: string; definitions: unknown[]}>) {
      const key = `${processId}:${JSON.stringify(scheme.definitions.map(d => JSON.stringify(d)).sort())}`
      if (!groups.has(key)) groups.set(key, { key, processName: node.processName, summary: scheme.summary, options: craftCache.get(processId)!, members: [] })
      groups.get(key)!.members.push({ routeId: route.routeId, routeProcessId: node.routeProcessId, deviceIds: scheme.deviceIds })
    }
  }
  parameterGroups.value = [...groups.values()]
}
function collectSelections() {
  const nodes = new Map<string, SharedChangeRequest['nodes'][number]>()
  for (const group of parameterGroups.value) for (const member of group.members) {
    const key = `${member.routeId}:${member.routeProcessId}`
    if (!nodes.has(key)) nodes.set(key, { routeId: member.routeId, routeProcessId: member.routeProcessId, crafts: [] })
    const node = nodes.get(key)!
    if (draft.value!.subjectType === 'DEVICE') { node.withoutParameters = group.selected === 0; node.deviceCraftId = group.selected || undefined }
    else {
      const existing = node.crafts.find(c => c.craftId === group.selected)
      if (existing) existing.deviceIds.push(...member.deviceIds)
      else node.crafts.push({ craftId: group.selected!, deviceIds: [...member.deviceIds] })
    }
  }
  return [...nodes.values()]
}
const refreshPreview = async () => {
  if (!draft.value || !selectionsComplete.value) return
  loading.value = true; error.value = ''
  try { draft.value.nodes = collectSelections(); impact.value = await SharedChangeApi.preview(draft.value.subjectType, draft.value); needsPreview.value = false } catch(e) { error.value = errorText(e); throw e } finally { loading.value = false }
}
const submit = async () => {
  if (!draft.value || !impact.value || !canSubmit.value) return
  loading.value = true; error.value = ''
  try { draft.value.impactToken = impact.value.impactToken; const result = await SharedChangeApi.submit(draft.value.subjectType, draft.value); visible.value = false; emit('submitted'); message.success(result.status === 'APPLIED' ? '已发布新版本' : '已提交审批') } catch(e) { error.value = errorText(e); throw e } finally { loading.value = false }
}
defineExpose({ open, openReplacement })
</script>
<style scoped>
.shared-change { display: flex; flex-direction: column; gap: 12px; max-height: 68vh; overflow-y: auto; }
.shared-change__schemes { display: grid; gap: 12px; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); }
.shared-change__scheme { padding: 12px; border: 1px solid var(--el-border-color); border-radius: 4px; display: flex; flex-direction: column; gap: 8px; }
.shared-change__muted { color: var(--el-text-color-secondary); font-size: 12px; margin-left: 8px; }
.shared-change__count { font-weight: 600; }
</style>
