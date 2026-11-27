<template>
  <div class="tree-publish-preview">
    <el-alert v-for="blocker in blockers" :key="blocker.message" :title="blocker.message" type="error" :closable="false" class="mb-12px">
      <span v-if="blocker.names.length">{{ blocker.names.length }} 个子节点</span>
    </el-alert>
    <p v-if="matrixRows.length" class="tree-publish-preview__legend">红色为将覆盖的内容</p>
    <el-table v-if="preview.affected.length" :data="matrixRows" border max-height="440" class="tree-publish-preview__matrix"
      aria-label="子节点覆盖确认表" :empty-text="preview.blockers.length ? '存在冲突，暂无法计算覆盖内容' : '无子节点配置变更'">
      <el-table-column label="覆盖内容" fixed="left" width="160"><template #default="{ row }">{{ fieldLabel(row.field) }}</template></el-table-column>
      <el-table-column v-for="node in matrixNodes" :key="String(node.routeId)" :min-width="240">
        <template #header>
          <span>{{ node.name }}</span>
          <el-tag v-if="node.blocker" type="warning" size="small" class="ml-4px">无法同步</el-tag>
        </template>
        <template #default="{ row }">
          <span v-if="node.blocker" class="tree-publish-preview__blocked">无法同步</span>
          <template v-else-if="row.cells.has(String(node.routeId))">
            <details v-for="(change, index) in row.cells.get(String(node.routeId))" :key="index" class="tree-publish-preview__cell-change">
              <summary :aria-label="node.name + '：' + fieldLabel(change.field) + '，将覆盖'">
                <span class="tree-publish-preview__field">{{ fieldLabel(change.field) }}</span>
                <span class="tree-publish-preview__before">{{ summary(change.before) }}</span>
                <span aria-hidden="true"> → </span>
                <span class="tree-publish-preview__overwrite">{{ summary(change.after) }}</span>
              </summary>
              <div><strong>修改前</strong><ReadableData :source="JSON.stringify(change.before)" title="修改前配置" /></div>
              <div class="tree-publish-preview__overwrite"><strong>覆盖后</strong><ReadableData :source="JSON.stringify(change.after)" title="修改后配置" /></div>
            </details>
          </template>
          <span v-else class="tree-publish-preview__unchanged">保持原值</span>
        </template>
      </el-table-column>
    </el-table>
    <el-collapse v-if="parentChanges.length" class="mt-12px">
      <el-collapse-item :title="'父节点修改（' + parentChanges.length + ' 项）'">
        <div v-for="(change, index) in parentChanges" :key="index" class="tree-publish-preview__detail">
          <strong>{{ fieldLabel(change.field) }}</strong>
          <div class="tree-publish-preview__compare"><div><span>修改前</span><ReadableData :source="JSON.stringify(change.before)" title="修改前配置" /></div><div><span>修改后</span><ReadableData :source="JSON.stringify(change.after)" title="修改后配置" /></div></div>
        </div>
      </el-collapse-item>
    </el-collapse>
    <p v-if="!parentChanges.length && !matrixRows.length && !blockers.length && !preview.affected.length">无业务配置变更</p>
    <el-form-item v-if="!readOnly && !preview.blockers.length" label="变更原因" required class="mt-16px">
      <el-input :model-value="reason" @update:model-value="$emit('update:reason', $event)" type="textarea" placeholder="填写变更原因" aria-label="树形统一变更原因" />
    </el-form-item>
  </div>
</template>
<script setup lang="ts">
import ReadableData from '@/components/BusinessData/ReadableData.vue'
import type { TreeChange, TreePreview } from '@/api/mes/pro/route/tree'
import { computed } from 'vue'
import { buildRouteTreePublishMatrix } from './routeTreePublishMatrix'
const props = defineProps<{ preview: TreePreview; reason: string; readOnly?: boolean }>()
defineEmits<{ 'update:reason': [value: string] }>()
const labels: Record<string, string> = {
  configSnapshots: '工艺配置', flowGraph: '工序结构', nodes: '工序节点', edges: '工序连线',
  layouts: '工序布局', productionProcessConfigs: '生产工序配置', scheduleConfigs: '排产配置',
  batchUseConfigs: '批记录配置', scheduleUseConfigs: '工序开始配置', routeName: '名称', name: '名称',
  lossCoefficient: '损耗系数', routeStartProductionLeaders: '生产组长配置',
  batchRecordAttachmentOwners: '批记录附件责任配置', status: '启用状态', description: '说明',
  code: '流程编码', remark: '备注', executionMode: '执行方式',
  overagePercent: '允许超量比例（%）', productionQuantityFactor: '生产系数',
  parameterRules: '设备参数', deviceSelectionGroups: '设备选择组', lossReasons: '损耗原因',
  processName: '工序名称', processCode: '工序编码', sort: '顺序', lowerLimit: '下限', upperLimit: '上限',
  keyFlag: '关键工序', checkFlag: '检验工序', routeProcessWorkstationId: '工位',
  defaultValue: '默认值', parameterName: '参数名称', parameterCode: '参数编码', unit: '单位',
  deviceGroupName: '设备组名称', deviceGroupKey: '设备组标识', selectionMode: '设备选择方式',
  deviceIds: '设备', formBindings: '表单槽位', formTemplateName: '表单名称', bindingName: '绑定名称',
  reasonName: '原因名称', reasonCode: '原因编码', enabled: '启用', boundaryEdges: '开始与结束连线',
  relationType: '流转关系', boundaryType: '连接类型', x: '横向位置', y: '纵向位置', width: '宽度', height: '高度'
}
const internalFields = new Set(['id', 'routeId', 'routeVersionId', 'routeFlowConfigId', 'routeProcessId', 'clientRouteProcessId',
  'sourceRouteProcessId', 'targetRouteProcessId', 'routeProcessSort', 'sourceSort', 'targetSort',
  'graphVersion', 'configVersion', 'createTime', 'updateTime', 'creator', 'updater', 'deleted', 'tenantId'])
// 图编辑器的临时操作和校验结果不属于审核人维护的配置；正式工序、连线仍逐项展示。
const graphMetadata = new Set(['cyclePaths', 'invalidEdgeIds', 'invalidRouteProcessIds',
  'routeProcessCreates', 'routeProcessDeletes', 'routeProcessUpdates', 'routeProcessIdMap',
  'valid', 'validationMessages', 'validationStatus'])
const objectValue = (value: unknown): value is Record<string, unknown> => !!value && typeof value === 'object' && !Array.isArray(value)
const clean = (value: unknown): unknown => Array.isArray(value) ? value.map(clean) : objectValue(value)
  ? Object.fromEntries(Object.keys(value).filter(key => !internalFields.has(key)).sort().map(key => [key, clean(value[key])])) : value
const processLabels = computed(() => {
  const result = new Map<string, string>()
  const visit = (value: unknown) => {
    if (Array.isArray(value)) { value.forEach(visit); return }
    if (!objectValue(value)) return
    if (value.processName || value.processCode) {
      const label = String(value.processName || value.processCode)
      for (const key of ['processId', 'routeProcessId']) if (value[key] != null) result.set(String(value[key]), label)
    }
    Object.values(value).forEach(visit)
  }
  const changes = [...props.preview.changes, ...props.preview.affected.flatMap(node => node.changes || [])]
  changes.forEach(change => { visit(change.before); visit(change.after) })
  return result
})
const displayChanges = (changes: TreeChange[]): TreeChange[] => changes.flatMap(change => {
  if (internalFields.has(change.field.split('.').at(-1)!)) return []
  if (change.field === 'configSnapshots.flowGraph.layouts') return []
  if (change.field.startsWith('configSnapshots.flowGraph.') && graphMetadata.has(change.field.split('.')[2])) return []
  const compare = (before: unknown, after: unknown, field: string): TreeChange[] => {
    if (JSON.stringify(clean(before)) === JSON.stringify(clean(after))) return []
    if (objectValue(before) && objectValue(after)) {
      return [...new Set([...Object.keys(before), ...Object.keys(after)])]
        .filter(key => !internalFields.has(key) && !(field.startsWith('configSnapshots.flowGraph.nodes.')
          && ['processName', 'processCode'].includes(key) && (before[key] == null || after[key] == null)))
        .flatMap(key => compare(before[key], after[key], `${field}.${key}`))
    }
    if (Array.isArray(before) && Array.isArray(after)) {
      const entries = [...before, ...after]
      const identity = ['processId', 'routeProcessId', 'bindingKey', 'formBindingKey', 'deviceGroupKey', 'parameterCode', 'id']
        .find(key => entries.length && entries.every(item => objectValue(item) && item[key] != null))
      if (identity) {
        const key = (item: Record<string, unknown>) => `${item.deviceId ?? ''}:${String(item[identity])}`
        const old = new Map(before.map(item => [key(item), item]))
        const next = new Map(after.map(item => [key(item), item]))
        return [...new Set([...old.keys(), ...next.keys()])].flatMap(id => {
          const item = next.get(id) || old.get(id)
          const label = item.processName || processLabels.value.get(String(item.processId ?? item.routeProcessId))
            || item.parameterName || item.parameterCode || item.deviceGroupName || item.reasonName || item.bindingName || '配置项'
          return compare(old.get(id), next.get(id), `${field}.${label}`)
        })
      }
    }
    return [{ field, before: clean(before), after: clean(after) }]
  }
  return compare(change.before, change.after, change.field)
})
const parentChanges = computed(() => displayChanges(props.preview.changes))
const matrixNodes = computed(() => props.preview.affected
  .map(row => ({ ...row, changes: displayChanges(row.changes || []) })))
const matrixRows = computed(() => buildRouteTreePublishMatrix(matrixNodes.value))
const blockers = computed(() => {
  const groups = new Map<string, string[]>()
  for (const raw of props.preview.blockers) {
    const row = props.preview.affected.find(item => item.blocker && raw === `${item.name}：${item.blocker}`)
    const detail = row?.blocker || raw
    const missingProcess = /^下属流程缺少待修改工序或绑定：flowGraph\.nodes (\d+)$/.exec(detail)
    const processName = missingProcess && processLabels.value.get(missingProcess[1])
    const message = missingProcess ? `缺少对应工序${processName ? `「${processName}」` : ''}，无法同步`
      : /^下属流程缺少待修改工序或绑定：/.test(detail) ? '缺少对应配置或绑定，无法同步'
      : detail.startsWith('配置不完整：') && detail.includes('reasonCode=PROCESS_ID_INVALID')
        ? '工序配置标识无效，无法同步' : detail
    const names = groups.get(message) || []
    if (row && !names.includes(row.name)) names.push(row.name)
    groups.set(message, names)
  }
  return [...groups].map(([message, names]) => ({ message, names }))
})
const fieldLabel = (path: string) => path.split('.').filter(key => !['configSnapshots', 'nodes'].includes(key))
  .map(key => processLabels.value.get(key) || labels[key] || key).join(' / ')
const summary = (value: unknown): string => {
  if (value == null || value === '') return '未设置'
  if (typeof value === 'boolean') return value ? '是' : '否'
  if (Array.isArray(value)) return value.length ? `共 ${value.length} 项配置（展开查看明细）` : '无配置项'
  if (objectValue(value)) {
    const entries = Object.entries(value)
    const highlights = entries.filter(([, item]) => item != null && typeof item !== 'object')
      .slice(0, 2).map(([key, item]) => `${labels[key] || key}：${summary(item)}`)
    return `共 ${entries.length} 个配置字段${highlights.length ? `；${highlights.join('；')}` : '（展开查看明细）'}`
  }
  const text = String(value)
  return text.length > 70 ? `${text.slice(0, 70)}…` : text
}

</script>
<style scoped>
:global(.route-tree-publish-message) { width: min(1400px, 94vw); max-width: 1400px; }
:global(.route-tree-publish-message .el-message-box__message) { flex: 1; min-width: 0; }
.tree-publish-preview { max-height: 70vh; overflow: auto; }
.tree-publish-preview__compare { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); gap: 16px; }
.tree-publish-preview pre { white-space: pre-wrap; overflow-wrap: anywhere; max-height: 260px; overflow: auto; font-size: 12px; background: var(--el-fill-color-light); padding: 12px; }
.tree-publish-preview__detail { padding: 12px; }
.tree-publish-preview__legend { margin: 0 0 12px; color: var(--el-text-color-secondary); font-size: 12px; }
.tree-publish-preview__matrix :deep(.cell) { overflow-wrap: anywhere; }
.tree-publish-preview__cell-change + .tree-publish-preview__cell-change { margin-top: 12px; }
.tree-publish-preview__cell-change summary { cursor: pointer; }
.tree-publish-preview__field { display: block; font-size: 12px; }
.tree-publish-preview__before { color: var(--el-text-color-secondary); }
.tree-publish-preview__overwrite { color: var(--el-color-danger); font-weight: 600; }
.tree-publish-preview__unchanged { color: var(--el-text-color-placeholder); }
.tree-publish-preview__blocked { color: var(--el-color-warning-dark-2); }
</style>
