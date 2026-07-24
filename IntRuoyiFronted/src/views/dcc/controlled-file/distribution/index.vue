<template>
  <ContentWrap>
    <el-result
      icon="info"
      title="分发规则已并入文控权限"
      sub-title="正在进入文控权限的分发规则页签，请稍候。"
    />
  </ContentWrap>
</template>

<script lang="ts" setup>
defineOptions({ name: 'DccControlledFileDistribution' })
const route = useRoute()
const router = useRouter()
onMounted(async () => {
  await nextTick()
  await router.replace({
    path: '/dcc/controlled-file/categories',
    query: {
      ...route.query,
      tab: 'distribution-rules'
    }
  })
})
</script>
