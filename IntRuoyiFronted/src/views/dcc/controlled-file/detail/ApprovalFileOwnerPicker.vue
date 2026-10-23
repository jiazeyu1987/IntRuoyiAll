<template>
  <div v-loading="loading" class="w-full" data-testid="dcc-approval-file-owner-picker">
    <el-alert v-if="error" :title="error" type="error" :closable="false" />
    <el-select :model-value="modelValue" :disabled="loading || disabled || Boolean(error)" placeholder="请选择文件负责人" filterable class="w-full" @update:model-value="choose">
      <el-option v-for="user in users" :key="user.id" :value="user.id" :label="`${user.name}（${user.username || '#' + user.id}）`" />
    </el-select>
    <p class="text-12px">本次选择与批准签名保存；负责人选择不增加文件操作权限。</p>
  </div>
</template>
<script setup lang="ts">
import { onBeforeUnmount, ref } from 'vue'
import { getSimpleUserList } from '@/api/system/user'
import { referenceIdentity } from '../relations/project-reference-contract'
const props = defineProps<{ modelValue?: string; disabled?: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [value: string | undefined] }>()
const users = ref<{ id: string; name: string; username?: string }[]>([]), loading = ref(true), error = ref('')
let current = true
const choose = (value: unknown) => {
  try {
    if (loading.value || props.disabled || error.value) return
    const id = referenceIdentity(value)
    if (!users.value.some(user => user.id === id)) throw new Error('所选负责人不在正式启用账号列表中')
    emit('update:modelValue', id)
  } catch (cause) { error.value = cause instanceof Error ? cause.message : String(cause) }
}
void (async () => {
  try {
    const list = await getSimpleUserList()
    if (!current) return
    if (!Array.isArray(list)) throw new Error('文件负责人正式账号列表缺失')
    const mapped = list.map(user => {
      const id = referenceIdentity(user.id)
      if (!user.nickname?.trim()) throw new Error('文件负责人正式账号名称缺失')
      return { id, name: user.nickname, username: user.username }
    })
    if (new Set(mapped.map(user => user.id)).size !== mapped.length) throw new Error('文件负责人账号身份重复')
    users.value = mapped
    if (props.modelValue && !mapped.some(user => user.id === props.modelValue))
      throw new Error('本轮已确认负责人账号当前不可用，请核对正式批准记录')
  } catch (cause) { if (current) error.value = cause instanceof Error ? cause.message : String(cause) }
  finally { if (current) loading.value = false }
})()
onBeforeUnmount(() => { current = false; users.value = [] })
</script>
