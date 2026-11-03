<template>
  <ContentWrap data-testid="registration-certificate-history-page">
    <el-alert v-if="invalidRoute" type="error" show-icon :closable="false" title="注册证编号无效，无法加载历史记录" />
    <CertificateHistoryTable v-else v-loading="loading" :rows="history" />
  </ContentWrap>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import CertificateHistoryTable from './CertificateHistoryTable.vue'
import { useRoute } from 'vue-router'
import {
  getRegistrationCertificateHistory,
  type DccRegistrationCertificateHistoryItemVO
} from '@/api/dcc/registrationCertificate'
import { parsePositiveRouteQueryId } from '@/utils/routeQueryId'

defineOptions({ name: 'DccRegistrationCertificateHistory' })

const route = useRoute()
const certificateId = computed(() => parsePositiveRouteQueryId(route.params.id || route.query.id))
const invalidRoute = computed(() => !certificateId.value)
const loading = ref(false)
const history = ref<DccRegistrationCertificateHistoryItemVO[]>([])

const loadHistory = async () => {
  if (!certificateId.value) return
  loading.value = true
  try {
    history.value = await getRegistrationCertificateHistory(certificateId.value)
  } finally {
    loading.value = false
  }
}

onMounted(loadHistory)
</script>

<style scoped>
.history-json {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
