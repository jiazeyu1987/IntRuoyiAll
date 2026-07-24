<template>
  <div aria-hidden="true" class="approval-task-redirect-root"></div>
</template>

<script lang="ts" setup>
defineOptions({ name: 'DccControlledFileApprovalTasks' })

const router = useRouter()

onMounted(() => {
  router.replace({
    path: '/approval-center',
    query: {
      moduleCode: 'DCC',
      viewType: 'TODO'
    }
  })
})
</script>
