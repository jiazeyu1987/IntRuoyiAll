<template>
  <EdhrSignaturePage
    embedded
    :initial-execution-id="initialExecutionId"
    :initial-execution-code="initialExecutionCode"
  />
</template>

<script lang="ts" setup>
import EdhrSignaturePage from '../../mes/pro/edhr/SignaturePage.vue'
import { parsePositiveRouteQueryId } from '@/utils/routeQueryId'

defineOptions({ name: 'EdhrBatchSignatureRecordsPane' })

const route = useRoute()
const initialExecutionId = computed(() => {
  return parsePositiveRouteQueryId(route.query.executionId) || undefined
})
const initialExecutionCode = computed(() =>
  typeof route.query.executionCode === 'string' ? route.query.executionCode : ''
)
</script>
