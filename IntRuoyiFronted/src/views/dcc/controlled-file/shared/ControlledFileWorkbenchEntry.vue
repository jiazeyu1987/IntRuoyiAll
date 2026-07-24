<template>
  <el-button class="controlled-file-workbench-entry" plain type="primary" @click="openWorkbench">
    <Icon icon="ep:menu" class="mr-5px" />
    DCC 工作台
  </el-button>
</template>

<script lang="ts" setup>
defineOptions({ name: 'ControlledFileWorkbenchEntry' })

const router = useRouter()

const openWorkbench = () => {
  router.push({ name: 'DccControlledFileWorkbench' })
}
</script>

<style scoped>
.controlled-file-workbench-entry {
  flex: 0 0 auto;
}
</style>
