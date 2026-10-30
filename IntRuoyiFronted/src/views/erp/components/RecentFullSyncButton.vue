<template>
  <div v-hasPermi="['erp:kingdee-sync:query']" class="recent-full-sync">
    <span>最近</span>
    <el-input-number v-model="days" :min="1" :precision="0" :step="1" :disabled="submitting"
      controls-position="right" aria-label="全量同步天数" class="recent-full-sync__days" />
    <span>天</span>
    <el-button type="primary" plain :loading="submitting" @click="submit">
      按天全量同步
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ErpKingdeeSyncApi } from '@/api/erp/sync'

const props = defineProps<{ syncType: string }>()
const days = ref<number | undefined>(7)
const submitting = ref(false)
const submit = async () => {
  const clickedAt = Date.now()
  if (!days.value || !Number.isInteger(days.value) || days.value < 1) {
    ElMessage.warning('请输入大于0的整数天数')
    return
  }
  submitting.value = true
  try {
    await ErpKingdeeSyncApi.runRecentFullSync(props.syncType, days.value, clickedAt)
    ElMessage.success('全量同步任务已提交，后台执行；可在同步运行记录中查看结果')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.recent-full-sync { display: inline-flex; align-items: center; gap: 8px; }
.recent-full-sync__days { width: 96px; }
</style>
