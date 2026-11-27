<template>
  <section class="controlled-detail-version" data-testid="dcc-detail-version-panel">
    <div class="controlled-detail-version__top">
      <section class="controlled-detail-section">
        <h3>受控与下发</h3>
        <dl class="controlled-detail-facts controlled-detail-facts--events">
          <div v-for="event in events" :key="event.time">
            <dt>{{ event.time }}</dt><dd>{{ event.labels.join('、') }}</dd>
          </div>
        </dl>
        <p v-if="!events.length" class="controlled-detail-muted">暂无受控、启用、发布和下发时间记录</p>
        <p class="controlled-detail-note" :class="{ 'controlled-detail-note--pending': ['pending', 'unknown'].includes(predecessorFact.kind) }">
          <template v-if="predecessorFact.kind === 'obsolete'">旧版已作废：{{ predecessorFact.versions.map(row => getDccVersionDisplayText(row.versionNo)).join('、') }}</template>
          <template v-else-if="predecessorFact.kind === 'initial'">{{ file.effectiveTime ? '初始版本，首次生效，无旧版作废事项。' : '初始版本，尚未记录实际生效，无旧版作废事项。' }}</template>
          <template v-else-if="predecessorFact.kind === 'pending'">{{ file.activatedTime ? '当前可见历史不足以核验旧版作废，请由文控人员查看作废归档。' : '新版尚未日常启用，旧版作废情况待核验。' }}</template>
          <template v-else>未查到由本次生效产生的作废记录，初始版本及前版信息尚未确认。</template>
        </p>
        <p v-if="file.finalizationError" class="controlled-detail-note controlled-detail-note--pending">受控处理待核验：{{ file.finalizationError }}</p>
      </section>
      <section class="controlled-detail-section">
        <h3>发布件与盖章件</h3>
        <dl class="controlled-detail-facts controlled-detail-facts--labels">
          <div><dt>发布件</dt><dd><el-tag :type="file.publishedArtifactAvailable === true ? 'success' : 'info'">{{ artifactText(file.publishedArtifactAvailable) }}</el-tag></dd></div>
          <div><dt>盖章件</dt><dd><el-tag :type="file.stampedArtifactAvailable === true ? 'success' : 'info'">{{ artifactText(file.stampedArtifactAvailable) }}</el-tag></dd></div>
        </dl>
        <el-button v-if="hasArtifacts" link type="primary" :disabled="!canViewArtifacts || file.canPreview !== true" @click="viewArtifacts">查看盖章 / 发布文件</el-button>
        <p v-if="hasArtifacts && (!canViewArtifacts || file.canPreview !== true)" class="controlled-detail-muted">{{ file.previewUnavailableReason || '当前权限不可预览' }}</p>
      </section>
    </div>
    <section class="controlled-detail-section">
      <div class="controlled-detail-section__heading">
        <h3>版本历史</h3>
        <div>
          <el-button v-if="canPartialRevision" @click="emit('revision', 'PARTIAL')">发起局部变更</el-button>
          <el-button v-if="canReplacementRevision" @click="emit('revision', 'REPLACEMENT')">发起换版变更</el-button>
        </div>
      </div>
      <el-table data-user-table-column-explicit :data="formalVersions" empty-text="暂无获授权的正式版本历史" row-key="id">
        <el-table-column label="版本" min-width="130"><template #default="{ row }"><el-button link type="primary" @click="emit('open-version', row.id)">{{ getDccVersionDisplayText(row.versionNo) }}</el-button></template></el-table-column>
        <el-table-column label="变更类型" min-width="140"><template #default="{ row }">{{ revisionChangeLabel(row.revisionChangeType) }}</template></el-table-column>
        <el-table-column prop="changeDescription" label="变更说明" min-width="240"><template #default="{ row }">{{ row.changeDescription || '—' }}</template></el-table-column>
        <el-table-column label="提交人" min-width="160"><template #default="{ row }">{{ getControlledDetailPersonText(userNames.get(row.submitterId ?? row.requesterId)) || '未记录姓名' }}</template></el-table-column>
      </el-table>
    </section>
  </section>
</template>
<script setup lang="ts">
import { computed } from 'vue'
import type { ControlledFileVO } from '@/api/dcc/controlledFile/workflow'
import { getDccVersionDisplayText } from '../shared/version-display'
import { revisionChangeLabel } from '../revision/revision-model'
import { formatControlledFileDateTime } from './presentation'
import { getControlledDetailFormalVersions, getControlledDetailPredecessorFact, getControlledDetailTimeEvents } from './controlled-detail-version'
import { getControlledDetailPersonText } from './controlled-detail-people'

const props = withDefaults(defineProps<{
  file: ControlledFileVO
  userNames: Map<number, string>
  canPartialRevision?: boolean
  canReplacementRevision?: boolean
  canViewArtifacts?: boolean
}>(), { canPartialRevision: false, canReplacementRevision: false, canViewArtifacts: false })
const emit = defineEmits<{
  revision: [intent: 'PARTIAL' | 'REPLACEMENT']
  'view-artifacts': []
  'open-version': [id: number]
}>()
const events = computed(() => getControlledDetailTimeEvents(props.file, formatControlledFileDateTime))
const formalVersions = computed(() => getControlledDetailFormalVersions(props.file.versionHistory))
const predecessorFact = computed(() => getControlledDetailPredecessorFact(props.file))
const hasArtifacts = computed(() => props.file.publishedArtifactAvailable === true || props.file.stampedArtifactAvailable === true)
const artifactText = (available?: boolean) => available === true ? '可用' : available === false ? '尚未生成或不可用' : '未返回可用状态'
const viewArtifacts = () => {
  if (props.canViewArtifacts && props.file.canPreview === true && hasArtifacts.value) emit('view-artifacts')
}
</script>
<style scoped>
@import './controlled-detail-panels.css';
.controlled-detail-version { display: grid; gap: 14px; min-width: 0; }
.controlled-detail-version__top { display: grid; grid-template-columns: minmax(0, 1.4fr) minmax(0, 1fr); gap: 14px; align-items: stretch; }
@media (max-width: 900px) { .controlled-detail-version__top { grid-template-columns: minmax(0, 1fr); } }
</style>
