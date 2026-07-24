<template>
  <ContentWrap>
    <el-result
      icon="info"
      title="培训规则已并入文控权限"
      sub-title="正在进入文控权限的培训规则页签，请稍候。"
    />
  </ContentWrap>
</template>

<script lang="ts" setup>
defineOptions({ name: 'DccControlledFileTraining' })
const route = useRoute()
const router = useRouter()
onMounted(async () => {
  await nextTick()
  await router.replace({
    path: '/dcc/controlled-file/categories',
    query: {
      ...route.query,
      tab: 'training-rules'
    }
  })
})
</script>
