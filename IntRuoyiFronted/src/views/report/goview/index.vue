<template>
  <doc-alert title="大屏设计器" url="https://doc.iocoder.cn/report/screen/" />

  <ContentWrap :bodyStyle="{ padding: '0px' }" class="!mb-0">
    <el-alert
      v-if="!goViewUrl"
      title="大屏设计器地址未配置，请配置 VITE_GOVIEW_URL 并启动对应服务。"
      type="error"
      :closable="false"
      show-icon
    />
    <IFrame v-else :src="src" />
  </ContentWrap>
</template>
<script lang="ts" setup>
import { getAccessToken, getRefreshToken } from '@/utils/auth'

defineOptions({ name: 'GoView' })

const goViewUrl = import.meta.env.VITE_GOVIEW_URL?.trim() || ''
const src = computed(() => {
  if (!goViewUrl) return ''
  const url = new URL(goViewUrl)
  url.searchParams.set('accessToken', getAccessToken() || '')
  url.searchParams.set('refreshToken', getRefreshToken() || '')
  return url.toString()
})
</script>
