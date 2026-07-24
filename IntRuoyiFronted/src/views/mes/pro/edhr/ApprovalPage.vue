<template>
  <div aria-hidden="true" class="edhr-approval-redirect-root"></div>
</template>

<script setup lang="ts">
defineOptions({ name: 'MesProFeedbackEdhrApproval' })

const route = useRoute()
const router = useRouter()

const resolveViewType = () => (route.query.tab === 'done' ? 'DONE' : 'TODO')

onMounted(() => {
  router.replace({
    path: '/approval-center',
    query: {
      moduleCode: 'EDHR',
      viewType: resolveViewType()
    }
  })
})
</script>
