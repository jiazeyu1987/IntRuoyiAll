<template>
  <div class="tree-publish-preview">
    <el-alert :title="'「' + preview.groupName + '」统一提交审核，涉及 ' + preview.affected.length + ' 个下属节点。审核通过后整批发布。'"
      type="info" :closable="false" />
    <el-alert v-for="blocker in preview.blockers" :key="blocker" :title="blocker" type="error" :closable="false" class="mt-8px" />
    <h4>父节点本次修改</h4>
    <el-collapse>
      <el-collapse-item v-for="(change, index) in displayChanges(preview.changes)" :key="index" :title="fieldLabel(change.field)">
        <div class="tree-publish-preview__compare"><div><strong>修改前</strong><pre>{{ format(change.before) }}</pre></div><div><strong>修改后</strong><pre>{{ format(change.after) }}</pre></div></div>
      </el-collapse-item>
    </el-collapse>
    <h4>下属流程最终差异</h4>
    <el-table :data="preview.affected" max-height="320">
      <el-table-column type="expand"><template #default="{ row }">
        <div v-for="(change, index) in displayChanges(row.changes || [])" :key="index" class="tree-publish-preview__detail">
          <strong>{{ fieldLabel(change.field) }}</strong>
          <div class="tree-publish-preview__compare"><pre>{{ format(change.before) }}</pre><pre>{{ format(change.after) }}</pre></div>
        </div>
        <p v-if="!displayChanges(row.changes || []).length">本次配置值相同，仍按整批要求发布新版本。</p>
      </template></el-table-column>
      <el-table-column prop="name" label="下属节点" min-width="160" />
      <el-table-column prop="versionNo" label="当前版本" width="95" />
      <el-table-column label="发布后工序" min-width="200"><template #default="{ row }">{{ row.afterProcesses?.join(' → ') || row.blocker }}</template></el-table-column>
    </el-table>
    <el-form-item v-if="!readOnly" label="变更原因" required class="mt-16px">
      <el-input :model-value="reason" @update:model-value="$emit('update:reason', $event)" type="textarea" placeholder="填写本次统一修改原因" aria-label="树形统一变更原因" />
    </el-form-item>
    <p>保留子流程未被本次修改覆盖的个性化内容；已有生产记录使用其冻结版本。</p>
  </div>
</template>
<script setup lang="ts">
import type { TreeChange, TreePreview } from '@/api/mes/pro/route/tree'
defineProps<{ preview: TreePreview; reason: string; readOnly?: boolean }>()
defineEmits<{ 'update:reason': [value: string] }>()
const labels: Record<string, string> = {
  configSnapshots: '工艺配置', flowGraph: '工序关系', nodes: '工序节点', edges: '工序连线',
  layouts: '工序布局', productionProcessConfigs: '生产工序配置', scheduleConfigs: '排产配置',
  batchUseConfigs: '批记录配置', scheduleUseConfigs: '工序开始配置', routeName: '名称', name: '名称',
  lossCoefficient: '损耗系数', routeStartProductionLeaders: '生产组长配置',
  batchRecordAttachmentOwners: '批记录附件责任配置', status: '启用状态', description: '说明',
  code: '流程编码', remark: '备注', executionMode: '执行方式',
  overagePercent: '允许超量比例（%）', productionQuantityFactor: '生产系数',
  parameterRules: '设备参数', deviceSelectionGroups: '设备选择组', lossReasons: '损耗原因',
  processName: '工序名称', processCode: '工序编码', sort: '顺序', lowerLimit: '下限', upperLimit: '上限',
  defaultValue: '默认值', parameterName: '参数名称', parameterCode: '参数编码', unit: '单位',
  deviceGroupName: '设备组名称', deviceGroupKey: '设备组标识', selectionMode: '设备选择方式',
  deviceIds: '设备', formBindings: '表单槽位', formTemplateName: '表单名称', bindingName: '绑定名称',
  reasonName: '原因名称', reasonCode: '原因编码', enabled: '启用', boundaryEdges: '开始与结束连线',
  relationType: '流转关系', boundaryType: '连接类型', x: '横向位置', y: '纵向位置', width: '宽度', height: '高度'
}
const internalFields = new Set(['id', 'routeId', 'routeVersionId', 'routeFlowConfigId', 'routeProcessId', 'clientRouteProcessId',
  'sourceRouteProcessId', 'targetRouteProcessId', 'routeProcessSort', 'sourceSort', 'targetSort',
  'graphVersion', 'configVersion', 'createTime', 'updateTime'])
// 图编辑器的临时操作和校验结果不属于审核人维护的配置；正式工序、连线仍逐项展示。
const graphMetadata = new Set(['cyclePaths', 'invalidEdgeIds', 'invalidRouteProcessIds',
  'routeProcessCreates', 'routeProcessDeletes', 'routeProcessUpdates', 'routeProcessIdMap',
  'valid', 'validationMessages', 'validationStatus'])
const objectValue = (value: unknown): value is Record<string, unknown> => !!value && typeof value === 'object' && !Array.isArray(value)
const displayChanges = (changes: TreeChange[]): TreeChange[] => changes.flatMap(change => {
  if (internalFields.has(change.field.split('.').at(-1)!)) return []
  if (change.field.startsWith('configSnapshots.flowGraph.') && graphMetadata.has(change.field.split('.')[2])) return []
  const compare = (before: unknown, after: unknown, field: string): TreeChange[] => {
    if (JSON.stringify(before) === JSON.stringify(after)) return []
    if (objectValue(before) && objectValue(after)) {
      return [...new Set([...Object.keys(before), ...Object.keys(after)])]
        .filter(key => !internalFields.has(key))
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
          const label = item.processName || item.parameterName || item.parameterCode || item.deviceGroupName || item.reasonName || item.bindingName || '配置项'
          return compare(old.get(id), next.get(id), `${field}.${label}`)
        })
      }
    }
    return [{ field, before, after }]
  }
  return compare(change.before, change.after, change.field)
})
const fieldLabel = (path: string) => path.split('.').map(key => labels[key] || key).join(' / ')
const format = (value: unknown): string => {
  const readable = (item: unknown): unknown => Array.isArray(item) ? item.map(readable) : objectValue(item)
    ? Object.fromEntries(Object.entries(item).filter(([key]) => !internalFields.has(key)).map(([key, val]) => [labels[key] || key, readable(val)])) : item
  return value == null ? '未设置' : typeof value === 'object' ? JSON.stringify(readable(value), null, 2) : String(value)
}
</script>
<style scoped>
:global(.route-tree-publish-message) { width: min(1100px, 94vw); max-width: 1100px; }
.tree-publish-preview { max-height: 70vh; overflow: auto; }
.tree-publish-preview__compare { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.tree-publish-preview pre { white-space: pre-wrap; overflow-wrap: anywhere; max-height: 260px; overflow: auto; font-size: 12px; background: var(--el-fill-color-light); padding: 12px; }
.tree-publish-preview__detail { padding: 12px; }
</style>
