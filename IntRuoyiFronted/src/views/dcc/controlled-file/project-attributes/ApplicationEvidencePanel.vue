<template>
  <ContentWrap v-loading="loading" data-testid="dcc-application-frozen-attributes">
    <div class="mb-12px font-600">{{ title }}</div>
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <template v-else-if="evidence?.recorded && evidence.actualAttributes && evidence.defaultSource">
      <ProjectAttributesFields :model-value="evidence.actualAttributes" readonly />
      <el-collapse>
        <el-collapse-item title="本次申请初始化时的项目默认值" name="default-source">
          <ProjectAttributesFields :model-value="evidence.defaultSource" readonly />
        </el-collapse-item>
      </el-collapse>
    </template>
    <el-alert
      v-else-if="evidence"
      :title="evidence.unavailableReason === 'NOT_FROZEN' ? '本次申请属性尚未提交冻结' : '历史申请属性未记录'"
      type="info"
      :closable="false"
    />
  </ContentWrap>
</template>
<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'
import { getControlledFileApplicationEvidence } from '@/api/dcc/controlledFile/applicationRead'
import type { ControlledFileApplicationEvidence, DccApplicationType } from '@/api/dcc/controlledFile/applicationRead'
import ProjectAttributesFields from './ProjectAttributesFields.vue'

const props = withDefaults(defineProps<{
  fileId: number | string
  applicationType: DccApplicationType
  bpmRound: string
  title?: string
}>(), { title: '本次申请属性' })
const evidence = ref<ControlledFileApplicationEvidence>()
const loading = ref(false)
const error = ref('')
let revision = 0
let mounted = true
onBeforeUnmount(() => { mounted = false; revision++ })
watch(() => [String(props.fileId), props.applicationType, props.bpmRound], async () => {
  const current = ++revision
  evidence.value = undefined; error.value = ''; loading.value = true
  try {
    const result = await getControlledFileApplicationEvidence(props.fileId, props.applicationType, props.bpmRound)
    if (mounted && current === revision) evidence.value = result
  } catch (cause) {
    if (mounted && current === revision) error.value = cause instanceof Error ? cause.message : String(cause)
  } finally {
    if (mounted && current === revision) loading.value = false
  }
}, { immediate: true })
</script>
