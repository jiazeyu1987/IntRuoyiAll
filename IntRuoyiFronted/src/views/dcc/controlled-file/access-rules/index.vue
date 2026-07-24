<template>
  <ContentWrap>
    <el-result
      icon="info"
      title="目录授权已并入文控权限"
      sub-title="正在进入文控权限的目录授权页签，请稍候。"
    />
  </ContentWrap>
</template>

<script lang="ts" setup>
defineOptions({ name: 'DccControlledFileAccessRules' })
const route = useRoute()
const router = useRouter()
onMounted(async () => {
  await nextTick()
  await router.replace({
    path: '/dcc/controlled-file/categories',
    query: {
      ...route.query,
      tab: 'directory-auth'
    }
  })
})
</script>
