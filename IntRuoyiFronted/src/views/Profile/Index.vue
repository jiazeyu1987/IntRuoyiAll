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
            @read-status-change="refreshUnreadNotifyMessageCount"
          />
        </el-tab-pane>
        <el-tab-pane :label="t('profile.info.basicInfo')" name="basicInfo">
          <BasicInfo />
        </el-tab-pane>
        <el-tab-pane :label="t('profile.info.resetPwd')" name="resetPwd">
          <ResetPwd />
        </el-tab-pane>
        <el-tab-pane label="配置" name="config" v-if="hasAnyProfileConfigPermission">
          <div class="profile-config-pane">
            <el-tabs v-model="activeConfigName" class="profile-config-tabs">
              <el-tab-pane v-if="hasGoldenFingerPermission" label="eDHR记录本" name="recordbook">
                <EdhrRecordbookGlobalSetting />
              </el-tab-pane>
              <el-tab-pane v-if="hasGoldenFingerPermission" label="放行资料要求" name="releaseDossier">
                <EdhrReleaseDossierRequirementSetting />
              </el-tab-pane>
              <el-tab-pane v-if="hasGoldenFingerPermission" label="ERP表格自动同步" name="erpTableSync">
                <ProfileErpTableAutoSyncSetting />
              </el-tab-pane>
              <el-tab-pane
                label="注册证配置"
                name="registrationCertificate"
                v-if="hasRegistrationCertificateConfigPermission"
              >
                <RegistrationCertificateConfig
                  :can-update="hasRegistrationCertificateConfigUpdatePermission"
                />
              </el-tab-pane>
            </el-tabs>
          </div>
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
import MyNotifyMessageList from '@/views/system/notify/my/components/MyNotifyMessageList.vue'
import {
  BasicInfo,
  EdhrReleaseDossierRequirementSetting,
  EdhrRecordbookGlobalSetting,
  ProfileErpTableAutoSyncSetting,
  ProfileWorkbench,
  RegistrationCertificateConfig,
  ResetPwd,
  UserSocial
} from './components'

const { t } = useI18n()
const route = useRoute()
const userStore = useUserStore()
const profileWorkbenchTodoBadgeStore = useProfileWorkbenchTodoBadgeStore()
defineOptions({ name: 'Profile' })

const isAdminUser = computed(() => userStore.getRoles.includes('super_admin'))
const GOLDEN_FINGER_PERMISSION = 'mes:pro-batch-record-execution:golden-finger'
const REGISTRATION_CERTIFICATE_CONFIG_QUERY_PERMISSION =
  'dcc:registration-certificate:config:query'
const REGISTRATION_CERTIFICATE_CONFIG_UPDATE_PERMISSION =
  'dcc:registration-certificate:config:update'
const hasGoldenFingerPermission = computed(() => userStore.permissions.has(GOLDEN_FINGER_PERMISSION))
const hasRegistrationCertificateConfigPermission = computed(() =>
  userStore.permissions.has(REGISTRATION_CERTIFICATE_CONFIG_QUERY_PERMISSION)
)
const hasRegistrationCertificateConfigUpdatePermission = computed(() =>
  userStore.permissions.has(REGISTRATION_CERTIFICATE_CONFIG_UPDATE_PERMISSION)
)
const hasAnyProfileConfigPermission = computed(
  () => hasGoldenFingerPermission.value || hasRegistrationCertificateConfigPermission.value
)

const isSocialBindingCallback = () =>
  typeof route.query.code === 'string' ||
  typeof route.query.type === 'string' ||
  route.fullPath.includes('type%3D')

const resolveProfileActiveTab = () => {
  if (isAdminUser.value && isSocialBindingCallback()) {
    return 'userSocial'
  }
  if (route.query.tab === 'config' && hasAnyProfileConfigPermission.value) {
    return 'config'
  }
  if (route.query.tab === 'notifyMessage') {
    return 'notifyMessage'
  }
  return 'workbench'
}

const activeName = ref(resolveProfileActiveTab())
const resolveFirstConfigName = () => {
  if (hasGoldenFingerPermission.value) return 'recordbook'
  if (hasRegistrationCertificateConfigPermission.value) return 'registrationCertificate'
  return ''
}
const resolveConfigActiveTab = () => {
  if (
    route.query.config === 'registrationCertificate' &&
    hasRegistrationCertificateConfigPermission.value
  ) {
    return 'registrationCertificate'
  }
  return resolveFirstConfigName()
}
const activeConfigName = ref(resolveConfigActiveTab())
const unreadNotifyMessageCount = ref(0)
const hasUnreadNotifyMessage = computed(() => unreadNotifyMessageCount.value > 0)

const refreshUnreadNotifyMessageCount = async () => {
  unreadNotifyMessageCount.value = await NotifyMessageApi.getUnreadNotifyMessageCount()
}

const reportProfileWorkbenchTodoBadgeError = (error: unknown) => {
  console.error('个人工作台待处理数量加载失败', error)
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
  if (!hasGoldenFingerPermission.value && activeName.value === 'config') {
    if (hasAnyProfileConfigPermission.value) {
      return
    }
    activeName.value = 'workbench'
  }
  const visibleConfigNames = new Set<string>()
  if (hasGoldenFingerPermission.value) {
    visibleConfigNames.add('recordbook')
    visibleConfigNames.add('releaseDossier')
    visibleConfigNames.add('erpTableSync')
  }
  if (hasRegistrationCertificateConfigPermission.value) {
    visibleConfigNames.add('registrationCertificate')
  }
  if (activeName.value === 'config' && !visibleConfigNames.has(activeConfigName.value)) {
    activeConfigName.value = resolveConfigActiveTab()
  }
}

watch(
  [
    () => route.fullPath,
    isAdminUser,
    hasGoldenFingerPermission,
    hasRegistrationCertificateConfigPermission
  ],
  () => {
    activeName.value = resolveProfileActiveTab()
    activeConfigName.value = resolveConfigActiveTab()
    ensureSocialTabVisible()
  }
)

watch(activeName, () => {
  ensureSocialTabVisible()
  refreshProfileWorkbenchTodoBadgeWhenVisible()
})

onMounted(() => {
  refreshUnreadNotifyMessageCount()
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

.profile-config-pane {
  display: flex;
  flex-direction: column;
  gap: 16px;
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
