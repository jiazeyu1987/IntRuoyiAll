<template>
  <ContentWrap class="registration-history" data-testid="registration-certificate-history-page">
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
.registration-history {
  --el-color-primary: #008f86;
  --el-color-primary-light-3: #4db1aa;
  --el-color-primary-light-5: #80c7c2;
  --el-color-primary-light-7: #b3ddd9;
  --el-color-primary-light-8: #cce9e7;
  --el-color-primary-light-9: #e6f5f2;
  --el-color-primary-dark-2: #00726b;

  min-width: 0;
  max-width: 100%;
  border-color: #dbe3e7;
  border-radius: 6px;
}

.registration-history :deep(.el-table) {
  --el-table-border-color: #dbe3e7;
  --el-table-header-bg-color: #f5f8f9;
  --el-table-header-text-color: #52646e;
  --el-table-text-color: #24343d;

  font-size: 13px;
}

.registration-history :deep(.el-table__cell) {
  padding: 8px 0;
  vertical-align: top;
}

.registration-history :deep(th.el-table__cell) {
  font-size: 12px;
  font-weight: 600;
}

.registration-history :deep(.el-table .cell) {
  padding: 0 12px;
  line-height: 20px;
  overflow-wrap: anywhere;
}

.history-json {
  margin: 0;
  font-size: 12px;
  line-height: 20px;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
