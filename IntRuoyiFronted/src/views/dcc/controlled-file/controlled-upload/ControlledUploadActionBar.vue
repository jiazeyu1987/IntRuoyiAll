<template>
  <footer class="controlled-upload-action-bar" :style="{ left: sidebarWidth }">
    <p>登记时间、生效时间及上传人由系统记录。</p>
    <el-button type="primary" :loading="loading" :disabled="disabled || loading" @click="submit">
      上传已受控文件
    </el-button>
  </footer>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useAppStore } from '@/store/modules/app'

const props = defineProps<{ loading: boolean; disabled: boolean }>()
const emit = defineEmits<{ submit: [] }>()
const appStore = useAppStore()
const sidebarWidth = computed(() => {
  if (appStore.getMobile || appStore.getLayout === 'top') return '0px'
  if (appStore.getLayout === 'cutMenu') {
    const tabWidth = appStore.getCollapse ? 'var(--tab-menu-min-width)' : 'var(--tab-menu-max-width)'
    return appStore.getFixedMenu ? `calc(${tabWidth} + var(--left-menu-max-width))` : tabWidth
  }
  return appStore.getCollapse ? 'var(--left-menu-min-width)' : 'var(--left-menu-max-width)'
})
const submit = () => {
  if (!props.loading && !props.disabled) emit('submit')
}
</script>

<style scoped>
.controlled-upload-action-bar {
  position: fixed;
  right: 0;
  bottom: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 24px calc(14px + env(safe-area-inset-bottom));
  background: var(--el-bg-color);
  border-top: 1px solid var(--el-color-primary-light-8);
  box-shadow: 0 -4px 16px rgb(0 0 0 / 4%);
}
.controlled-upload-action-bar p {
  margin: 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.controlled-upload-action-bar .el-button {
  min-width: 180px;
}
@media (max-width: 767px) {
  .controlled-upload-action-bar {
    left: 0 !important;
    flex-wrap: wrap;
    gap: 8px;
    padding: 10px 16px calc(10px + env(safe-area-inset-bottom));
  }
  .controlled-upload-action-bar .el-button {
    width: 100%;
  }
}
</style>
