<template>
  <DccSignaturePage embedded single-tab="records" initial-tab="records" />
</template>

<script lang="ts" setup>
import DccSignaturePage from '../../dcc/controlled-file/signatures/index.vue'

defineOptions({ name: 'DccSignatureRecordsPane' })
</script>
