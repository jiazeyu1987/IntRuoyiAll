<template>
  <div v-if="canQuery">
    <ReminderScheduleSetting ref="scheduleRef" :can-update="canUpdate" />
    <el-button class="mt-16px" :disabled="!canUpdate" @click="reminderRef?.open()"
      >收件人通知设置</el-button
    >
    <ReminderConfigDialog ref="reminderRef" @success="scheduleRef?.loadConfig()" />
  </div>
  <el-alert v-else title="无权查看注册证配置。" type="error" :closable="false" show-icon />
</template>
<script setup lang="ts">
import { hasExplicitConfigPermission } from '@/utils/businessConfigAccess'
import ReminderScheduleSetting from './components/ReminderScheduleSetting.vue'
import ReminderConfigDialog from './ReminderConfigDialog.vue'
defineOptions({ name: 'DccRegistrationCertificateConfig' })
const scheduleRef = ref<InstanceType<typeof ReminderScheduleSetting>>()
const reminderRef = ref<InstanceType<typeof ReminderConfigDialog>>()
const canQuery = computed(() =>
  hasExplicitConfigPermission('dcc:registration-certificate:config:query')
)
const canUpdate = computed(() =>
  hasExplicitConfigPermission('dcc:registration-certificate:config:update')
)
</script>
