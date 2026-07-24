<template>
  <DccSignaturePage embedded single-tab="authorizations" initial-tab="authorizations" />
</template>

<script lang="ts" setup>
import DccSignaturePage from '../../dcc/controlled-file/signatures/index.vue'

defineOptions({ name: 'DccSignatureAuthorizationsPane' })
</script>
