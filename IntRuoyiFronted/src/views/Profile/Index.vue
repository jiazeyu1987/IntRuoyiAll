<template>
  <div class="profile-page">
    <el-card
      class="profile-page__card"
      :class="{ 'profile-page__card--workbench': activeName === 'workbench' }"
      shadow="never"
    >
      <el-alert v-if="handoffLoadError" :title="handoffLoadError" type="error" :closable="false" show-icon>
        <el-button link type="primary" @click="refreshHandoffs">重新加载交接</el-button>
      </el-alert>
      <el-tabs v-model="activeName" class="profile-tabs" tab-position="top">
        <el-tab-pane name="workbench" lazy>
          <template #label>
            <span class="profile-workbench-tab">
              <span>个人工作台</span>
              <span
                v-if="profileWorkbenchTodoBadgeStore.getHasVisibleTodoBadge"
                class="personal-workbench-todo-badge"
                :aria-label="`个人工作台待处理数量 ${profileWorkbenchTodoBadgeStore.todoTotal}`"
              >
                {{ profileWorkbenchTodoBadgeStore.todoTotal }}
              </span>
            </span>
          </template>
          <ProfileWorkbench />
          <PqcReleaseCorrection v-if="route.query.releaseCorrectionTaskId" />
        </el-tab-pane>
        <el-tab-pane name="notifyMessage" lazy>
          <template #label>
            <el-badge
              class="profile-notify-message-tab__badge"
              :is-dot="hasUnreadNotifyMessage"
              :hidden="!hasUnreadNotifyMessage"
            >
              <span>我的站内信</span>
            </el-badge>
          </template>
          <MyNotifyMessageList
            v-if="activeName === 'notifyMessage'"
            class="profile-notify-message-tab"
            embedded
            :active="activeName === 'notifyMessage'"
            @read-status-change="handleUnreadNotifyMessageRefresh"
          />
        </el-tab-pane>
        <el-tab-pane v-if="hasHandoffs" label="交接列表" name="handoffs">
          <ActiveOrderHandoffPanel
            ref="handoffPanelRef"
            :initial-tasks="handoffTasks"
            @loaded="handleHandoffsLoaded"
          />
        </el-tab-pane>
        <el-tab-pane :label="t('profile.info.basicInfo')" name="basicInfo">
          <BasicInfo />
        </el-tab-pane>
        <el-tab-pane label="电子签名设置" name="signatureSettings" lazy>
          <SignatureCredentialSettings v-if="activeName === 'signatureSettings'" />
        </el-tab-pane>
        <el-tab-pane :label="t('profile.info.resetPwd')" name="resetPwd">
          <ResetPwd />
        </el-tab-pane>
        <el-tab-pane
          v-if="route.query.tab === 'config'"
          label="配置入口已迁移"
          name="configMigration"
        >
          <BusinessConfigMigrationNotice />
        </el-tab-pane>
        <el-tab-pane :label="t('profile.info.userSocial')" name="userSocial" v-if="isAdminUser">
          <UserSocial v-model:activeName="activeName" />
        </el-tab-pane>
      </el-tabs>
      <!-- 无列表数据时仍保留原交接通知的只读结果入口。 -->
      <ActiveOrderHandoffPanel
        v-if="canReadActiveOrderHandoffs && !hasHandoffs && route.query.handoffTaskId"
        result-only
        :initial-tasks="handoffTasks"
      />
    </el-card>
  </div>
</template>
<script lang="ts" setup>
import SignatureCredentialSettings from '@/views/signature-governance/components/SignatureCredentialSettings.vue'
import * as NotifyMessageApi from '@/api/system/notify/message'
import { useProfileWorkbenchTodoBadgeStore } from '@/store/modules/profileWorkbenchTodoBadge'
import { useUserStore } from '@/store/modules/user'
import BusinessConfigMigrationNotice from '@/views/system/config-migration/BusinessConfigMigrationNotice.vue'
import MyNotifyMessageList from '@/views/system/notify/my/components/MyNotifyMessageList.vue'
import { BasicInfo, ProfileWorkbench, ResetPwd, UserSocial } from './components'
import PqcReleaseCorrection from './components/PqcReleaseCorrection.vue'
import ActiveOrderHandoffPanel from '@/views/mes/pro/handoff/ActiveOrderHandoffPanel.vue'
import * as HandoffApi from '@/api/mes/pro/handoff'
import { hasPermission } from '@/directives/permission/hasPermi'

const { t } = useI18n()
const route = useRoute()
const userStore = useUserStore()
const profileWorkbenchTodoBadgeStore = useProfileWorkbenchTodoBadgeStore()
defineOptions({ name: 'Profile' })

const isAdminUser = computed(() => userStore.getRoles.includes('super_admin'))
const isSocialBindingCallback = () =>
  typeof route.query.code === 'string' ||
  typeof route.query.type === 'string' ||
  route.fullPath.includes('type%3D')

const resolveProfileActiveTab = () => {
  if (isAdminUser.value && isSocialBindingCallback()) {
    return 'userSocial'
  }
  if (route.query.tab === 'config') {
    return 'configMigration'
  }
  if (route.query.tab === 'notifyMessage') {
    return 'notifyMessage'
  }
  if (route.query.tab === 'signatureSettings') return 'signatureSettings'
  if (route.query.tab === 'resetPwd') {
    return 'resetPwd'
  }
  return 'workbench'
}

const activeName = ref(resolveProfileActiveTab())
const handoffTasks = ref<HandoffApi.HandoffTask[]>([])
const handoffLoadError = ref('')
const handoffPanelRef = ref<{ refreshLayout: () => Promise<void> }>()
const canReadActiveOrderHandoffs = computed(() =>
  hasPermission([
    'mes:pro-feedback:query',
    'mes:pro-process-pool-team-leader:query',
    'mes:pro-edhr-nonconformance-review:query',
    'mes:pro-edhr-work-task:query'
  ])
)
const hasHandoffs = computed(() => canReadActiveOrderHandoffs.value && handoffTasks.value.length > 0)
const handleHandoffsLoaded = (rows: HandoffApi.HandoffTask[]) => {
  handoffTasks.value = rows
  if (!rows.length && activeName.value === 'handoffs') activeName.value = 'notifyMessage'
}
let handoffLoadSequence = 0
const refreshHandoffs = async () => {
  const sequence = ++handoffLoadSequence
  handoffLoadError.value = ''
  if (!canReadActiveOrderHandoffs.value) {
    handleHandoffsLoaded([])
    return
  }
  try {
    const rows = await HandoffApi.myHandoffs()
    if (sequence === handoffLoadSequence) handleHandoffsLoaded(rows)
  } catch (error) {
    if (sequence === handoffLoadSequence)
      handoffLoadError.value = error instanceof Error ? error.message : String(error)
  }
}
watch(
  [() => userStore.getUser.id, canReadActiveOrderHandoffs],
  () => {
    handleHandoffsLoaded([])
    void refreshHandoffs()
  },
  { immediate: true }
)
onBeforeUnmount(() => {
  ++handoffLoadSequence
})
let handoffsDeactivated = false
onDeactivated(() => {
  handoffsDeactivated = true
})
onActivated(() => {
  if (!handoffsDeactivated) return
  handoffsDeactivated = false
  void refreshHandoffs()
})
const unreadNotifyMessageCount = ref(0)
const hasUnreadNotifyMessage = computed(() => unreadNotifyMessageCount.value > 0)

const refreshUnreadNotifyMessageCount = async () => {
  unreadNotifyMessageCount.value = await NotifyMessageApi.getUnreadNotifyMessageCount({
    ignoreErrorMessage: true
  })
}

const reportProfileNotifyMessageError = (error: unknown) => {
  console.error('个人中心站内信未读数量加载失败', error)
  unreadNotifyMessageCount.value = 0
}

const handleUnreadNotifyMessageRefresh = () => {
  void refreshUnreadNotifyMessageCount().catch(reportProfileNotifyMessageError)
}

const reportProfileWorkbenchTodoBadgeError = (error: unknown) => {
  const code = (error as { code?: unknown } | null)?.code
  console.error('个人工作台待处理数量加载失败', Number.isSafeInteger(code) ? code : 'WORKBENCH_REQUEST_FAILED')
}

const refreshProfileWorkbenchTodoBadge = () => {
  void profileWorkbenchTodoBadgeStore
    .ensureTodoTotalLoaded()
    .catch(reportProfileWorkbenchTodoBadgeError)
}

const refreshProfileWorkbenchTodoBadgeWhenVisible = () => {
  if (activeName.value === 'workbench') {
    refreshProfileWorkbenchTodoBadge()
  }
}

const ensureSocialTabVisible = () => {
  if (!isAdminUser.value && activeName.value === 'userSocial') {
    activeName.value = 'workbench'
  }
}

watch([() => route.fullPath, isAdminUser], () => {
  activeName.value = resolveProfileActiveTab()
  ensureSocialTabVisible()
})

watch(
  activeName,
  async () => {
    ensureSocialTabVisible()
    refreshProfileWorkbenchTodoBadgeWhenVisible()
    if (activeName.value === 'handoffs') await handoffPanelRef.value?.refreshLayout()
  },
  { flush: 'post' }
)

onMounted(() => {
  refreshUnreadNotifyMessageCount().catch(reportProfileNotifyMessageError)
  refreshProfileWorkbenchTodoBadgeWhenVisible()
})
</script>
<style scoped>
.profile-page {
  width: 100%;
}

.profile-page__card {
  width: 100%;
  min-height: 640px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
}

.profile-page__card--workbench {
  height: calc(100dvh - var(--top-tool-height) - var(--tags-view-height) - var(--app-content-padding));
}

.profile-page__card--workbench :deep(> .el-card__body),
.profile-page__card--workbench :deep(.profile-tabs) {
  display: flex;
  height: 100%;
  min-height: 0;
  flex-direction: column;
}

.profile-page__card--workbench :deep(.profile-tabs > .el-tabs__header) {
  flex-shrink: 0;
}

.profile-page__card--workbench :deep(.profile-tabs > .el-tabs__content) {
  flex: 1;
  min-height: 0;
}

.profile-page__card--workbench :deep(#pane-workbench) {
  height: 100%;
}

:deep(.profile-page__card > .el-card__body) {
  padding: 18px 24px 24px;
}

:deep(.profile-tabs > .el-tabs__content) {
  padding-top: 18px;
  min-height: 520px;
}

.profile-notify-message-tab__badge {
  line-height: 1;
}

.profile-workbench-tab {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  line-height: 1;
}

.personal-workbench-todo-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 24px;
  max-width: none;
  height: 18px;
  padding: 0 6px;
  overflow: visible;
  color: #1677ff;
  font-size: 12px;
  font-weight: 600;
  line-height: 18px;
  white-space: nowrap;
  background: #e8f2ff;
  border: 1px solid #c7ddff;
  border-radius: 9px;
  box-sizing: border-box;
}

:deep(.profile-notify-message-tab__badge .el-badge__content.is-dot) {
  top: 4px;
  right: -6px;
}
</style>
