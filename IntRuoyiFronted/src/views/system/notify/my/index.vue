<template>
  <doc-alert title="站内信配置" url="https://doc.iocoder.cn/notify/" />
  <MyNotifyMessageList />
</template>

<script lang="ts" setup>
import MyNotifyMessageList from './components/MyNotifyMessageList.vue'

defineOptions({ name: 'SystemMyNotify' })
</script>
