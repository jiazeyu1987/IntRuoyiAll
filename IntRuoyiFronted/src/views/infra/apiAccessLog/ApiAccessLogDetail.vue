<template>
  <Dialog v-model="dialogVisible" :max-height="500" :scroll="true" title="详情" width="800">
    <el-alert v-if="detailError" :title="detailError" type="error" :closable="false" show-icon />
    <div v-loading="detailLoading" :style="detailLoading ? { minHeight: '160px' } : undefined">
    <el-descriptions v-if="detailData" :column="1" border>
      <el-descriptions-item label="日志主键" min-width="120">
        {{ detailData.id }}
      </el-descriptions-item>
      <el-descriptions-item label="链路追踪">
        {{ detailData.traceId }}
      </el-descriptions-item>
      <el-descriptions-item label="应用名">
        {{ detailData.applicationName }}
      </el-descriptions-item>
      <el-descriptions-item label="用户信息">
        {{ detailData.userId }}
        <dict-tag :type="DICT_TYPE.USER_TYPE" :value="detailData.userType" />
      </el-descriptions-item>
      <el-descriptions-item label="用户 IP">
        {{ detailData.userIp }}
      </el-descriptions-item>
      <el-descriptions-item label="用户 UA">
        {{ detailData.userAgent }}
      </el-descriptions-item>
      <el-descriptions-item label="请求信息">
        {{ detailData.requestMethod }} {{ detailData.requestUrl }}
      </el-descriptions-item>
      <el-descriptions-item label="请求参数">
        {{ detailData.requestParams }}
      </el-descriptions-item>
      <el-descriptions-item label="请求结果">
        {{ detailData.responseBody }}
      </el-descriptions-item>
      <el-descriptions-item label="请求时间">
        {{ formatDate(detailData.beginTime) }} ~ {{ formatDate(detailData.endTime) }}
      </el-descriptions-item>
      <el-descriptions-item label="请求耗时">{{ detailData.duration }} ms</el-descriptions-item>
      <el-descriptions-item label="操作结果">
        <div v-if="detailData.resultCode === 0">正常</div>
        <div v-else-if="detailData.resultCode > 0">
          失败 | {{ detailData.resultCode }} | {{ detailData.resultMsg }}
        </div>
      </el-descriptions-item>
      <el-descriptions-item label="操作模块">
        {{ detailData.operateModule }}
      </el-descriptions-item>
      <el-descriptions-item label="操作名">
        {{ detailData.operateName }}
      </el-descriptions-item>
      <el-descriptions-item label="操作名">
        <dict-tag :type="DICT_TYPE.INFRA_OPERATE_TYPE" :value="detailData.operateType" />
      </el-descriptions-item>
    </el-descriptions>
    </div>
  </Dialog>
</template>

<script lang="ts" setup>
import { DICT_TYPE } from '@/utils/dict'
import { formatDate } from '@/utils/formatTime'
import * as ApiAccessLog from '@/api/infra/apiAccessLog'
import { getTenantId } from '@/utils/auth'
import { useUserStore } from '@/store/modules/user'

defineOptions({ name: 'ApiAccessLogDetail' })

const dialogVisible = ref(false) // 弹窗的是否展示
const detailLoading = ref(false) // 表单地加载中
const detailData = ref<ApiAccessLog.ApiAccessLogVO>()
const detailError = ref('')
const userStore = useUserStore()
const detailOwner = () => `${getTenantId()}:${userStore.getUser.id}`
let detailSequence = 0
watch(dialogVisible, (visible) => { if (!visible) { detailSequence++; detailData.value = undefined } })
onBeforeUnmount(() => { detailSequence++ })

/** 打开弹窗 */
const open = async (data: ApiAccessLog.ApiAccessLogVO) => {
  dialogVisible.value = true
  const sequence = ++detailSequence
  const owner = detailOwner()
  const current = () => sequence === detailSequence && dialogVisible.value && owner === detailOwner()
  detailData.value = undefined
  detailError.value = ''
  detailLoading.value = true
  try {
    const detail = await ApiAccessLog.getApiAccessLog(data.id, current)
    if (!current()) return
    if (!detail || String(detail.id) !== String(data.id)) throw new Error('日志详情与所选记录不一致，请重新查询。')
    detailData.value = detail
  } catch (error) {
    if (current()) detailError.value = error instanceof Error ? error.message : '日志详情读取失败，请重试。'
  } finally {
    if (current()) detailLoading.value = false
  }
}

defineExpose({ open }) // 提供 open 方法，用于打开弹窗
</script>
