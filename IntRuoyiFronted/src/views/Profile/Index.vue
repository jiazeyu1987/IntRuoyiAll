<template>
  <div class="profile-page">
    <el-card class="profile-page__card" shadow="never">
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
        </el-tab-pane>
        <el-tab-pane name="notifyMessage">
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
            class="profile-notify-message-tab"
            embedded
            @read-status-change="handleUnreadNotifyMessageRefresh"
          />
        </el-tab-pane>
        <el-tab-pane :label="t('profile.info.basicInfo')" name="basicInfo">
          <BasicInfo />
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
    </el-card>
  </div>
</template>
<script lang="ts" setup>
import * as NotifyMessageApi from '@/api/system/notify/message'
import { useProfileWorkbenchTodoBadgeStore } from '@/store/modules/profileWorkbenchTodoBadge'
import { useUserStore } from '@/store/modules/user'
import BusinessConfigMigrationNotice from '@/views/system/config-migration/BusinessConfigMigrationNotice.vue'
import MyNotifyMessageList from '@/views/system/notify/my/components/MyNotifyMessageList.vue'
import { BasicInfo, ProfileWorkbench, ResetPwd, UserSocial } from './components'

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
  if (route.query.tab === 'resetPwd') {
    return 'resetPwd'
  }
  return 'workbench'
}

const activeName = ref(resolveProfileActiveTab())
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

watch(activeName, () => {
  ensureSocialTabVisible()
  refreshProfileWorkbenchTodoBadgeWhenVisible()
})

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
