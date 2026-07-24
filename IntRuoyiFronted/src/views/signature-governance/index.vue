<template>
  <ContentWrap>
    <div class="signature-governance">
      <el-alert
        v-if="routeTabError"
        :closable="false"
        show-icon
        type="error"
        :title="routeTabError"
      />

      <SignatureGovernanceRecordsPane v-if="activeTab === 'signature-records'" />
      <SignatureGovernanceMySignaturePane v-if="activeTab === 'my-signature'" />
      <DccSignatureAuthorizationsPane v-if="activeTab === 'authorizations' && canViewAuthorizations" />
      <RetentionGovernanceListPane v-if="activeTab === 'retention'" />
      <PeriodicReviewGovernanceListPane v-if="activeTab === 'periodic-review'" />
      <CsvPackageGovernanceListPane v-if="activeTab === 'csv-package'" />
      <PolicyGovernanceListPane v-if="activeTab === 'policy'" />
    </div>
  </ContentWrap>
</template>

<script lang="ts" setup>
import DccSignatureAuthorizationsPane from './components/DccSignatureAuthorizationsPane.vue'
import CsvPackageGovernanceListPane from './components/CsvPackageGovernanceListPane.vue'
import PeriodicReviewGovernanceListPane from './components/PeriodicReviewGovernanceListPane.vue'
import PolicyGovernanceListPane from './components/PolicyGovernanceListPane.vue'
import RetentionGovernanceListPane from './components/RetentionGovernanceListPane.vue'
import SignatureGovernanceMySignaturePane from './components/SignatureGovernanceMySignaturePane.vue'
import SignatureGovernanceRecordsPane from './components/SignatureGovernanceRecordsPane.vue'
import { useUserStore } from '@/store/modules/user'

defineOptions({ name: 'SignatureGovernanceWorkbench' })

type ActiveTab =
  | 'signature-records'
  | 'my-signature'
  | 'authorizations'
  | 'retention'
  | 'periodic-review'
  | 'csv-package'
  | 'policy'

const route = useRoute()
const userStore = useUserStore()
const SIGNATURE_MANAGE_PERMISSION = 'dcc:controlled-file:signature:manage'
const SIGNATURE_ADMIN_ROLE = 'electronic_signature_admin'
const ALL_PERMISSION = '*:*:*'

const signatureTabRoutes: Record<ActiveTab, string> = {
  'signature-records': '/signature-governance/signature-records',
  'my-signature': '/signature-governance/my-signature',
  authorizations: '/signature-governance/authorizations',
  retention: '/signature-governance/retention',
  'periodic-review': '/signature-governance/periodic-review',
  'csv-package': '/signature-governance/csv-package',
  policy: '/signature-governance/policy'
}

const routeAliases: Record<string, ActiveTab> = {
  '/signature-governance': 'signature-records'
}

const resolveActiveTab = (path: string): ActiveTab => {
  const normalizedPath = path.replace(/\/+$/, '') || '/signature-governance'
  const matchedTab = (Object.entries(signatureTabRoutes) as Array<[ActiveTab, string]>)
    .find(([, routePath]) => routePath === normalizedPath)?.[0]
  return matchedTab || routeAliases[normalizedPath] || 'signature-records'
}

const activeTab = computed<ActiveTab>(() => resolveActiveTab(route.path))
const hasSignatureManagePermission = computed(
  () =>
    userStore.getPermissions.has(ALL_PERMISSION) ||
    userStore.getPermissions.has(SIGNATURE_MANAGE_PERMISSION)
)
const canViewAuthorizations = computed(
  () => userStore.getRoles.includes(SIGNATURE_ADMIN_ROLE) && hasSignatureManagePermission.value
)
const routeTabError = computed(() => {
  const normalizedPath = route.path.replace(/\/+$/, '') || '/signature-governance'
  const knownRoutes = new Set([...Object.values(signatureTabRoutes), ...Object.keys(routeAliases)])
  if (normalizedPath === signatureTabRoutes.authorizations && !canViewAuthorizations.value) {
    return '当前账号没有电子签名管理员权限'
  }
  return knownRoutes.has(normalizedPath) ? '' : '未识别的电子签名治理入口'
})
</script>

<style scoped>
.signature-governance {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>
