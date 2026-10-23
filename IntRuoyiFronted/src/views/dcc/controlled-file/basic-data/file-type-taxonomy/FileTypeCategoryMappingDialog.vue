<template>
  <Dialog v-model="visible" title="文件类型对应类别检查" width="620px">
    <section v-loading="loading">
      <p>文件类型：{{ typeName }}</p>
      <el-alert v-if="error" :title="error" type="error" :closable="false" />
      <el-alert v-else-if="categoryId !== undefined" title="已匹配唯一启用文件类别" type="success" :closable="false" />
      <p class="mt-12px text-[var(--el-text-color-secondary)]">
        后续申请使用此类别的正式审批规则。类别对应关系与审批规则需要分别配置和检查。
      </p>
    </section>
    <template #footer>
      <el-button :disabled="loading" @click="refresh">重新检查</el-button>
      <el-button @click="visible = false">关闭</el-button>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import { ref } from 'vue'
import { resolveFileTypeActiveCategory } from '@/api/dcc/controlledFile/fileTypeTaxonomies'
const visible = ref(false), loading = ref(false), error = ref(''), typeName = ref('')
const typeId = ref<number | string>(), categoryId = ref<number | string>()
let requestSequence = 0
const open = async (id: number | string, name: string) => {
  const sequence = ++requestSequence
  typeId.value = id; typeName.value = name; categoryId.value = undefined
  visible.value = true; loading.value = true; error.value = ''
  try {
    const resolved = await resolveFileTypeActiveCategory(id)
    if (sequence !== requestSequence) return
    if (!/^[1-9][0-9]*$/.test(String(resolved))) throw new Error('文件类型对应类别身份缺失')
    categoryId.value = resolved
  } catch (cause) {
    if (sequence === requestSequence) error.value = cause instanceof Error ? cause.message : String(cause)
  } finally { if (sequence === requestSequence) loading.value = false }
}
const refresh = () => {
  if (typeId.value !== undefined) return open(typeId.value, typeName.value)
}
defineExpose({ open })
</script>
