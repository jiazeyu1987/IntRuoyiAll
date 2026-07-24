<template>
  <el-dialog
    v-model="dialogVisible"
    :title="editingHall ? '编辑展柜' : '新增展柜'"
    class="showroom-hall-editor-dialog"
    destroy-on-close
    width="640px"
  >
    <el-form label-position="top">
      <el-form-item label="展柜编码">
        <el-input v-model="form.hallCode" :disabled="Boolean(editingHall)" />
      </el-form-item>
      <el-form-item label="展柜名称">
        <el-input v-model="form.name" />
      </el-form-item>
      <el-form-item label="英文名称">
        <el-input v-model="form.nameEn" />
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="form.description" :rows="4" type="textarea" />
      </el-form-item>
      <el-form-item label="英文描述">
        <el-input v-model="form.descriptionEn" :rows="4" type="textarea" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="dialogVisible = false">关闭</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ShowroomAdminApi } from '@/api/showroom-admin'
import {
  buildHallSavePayload,
  createHallEditorForm,
  normalizeHallRecord,
  type HallEditorForm,
  type ShowroomHallRecord
} from './contracts'

defineOptions({ name: 'ShowroomHallEditorDialog' })

const props = defineProps<{
  modelValue: boolean
  hall?: unknown | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  saved: [hall: ShowroomHallRecord | null]
}>()

const message = useMessage()
const saving = ref(false)
const editingHall = computed(() => (props.hall ? normalizeHallRecord(props.hall) : null))
const form = reactive<HallEditorForm>(createHallEditorForm())

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})

watch(
  () => [props.modelValue, props.hall] as const,
  ([visible]) => {
    if (!visible) {
      return
    }
    Object.assign(form, createHallEditorForm(editingHall.value))
  },
  { immediate: true }
)

const handleSave = async () => {
  saving.value = true
  try {
    const payload = buildHallSavePayload(editingHall.value?.hallId, form)
    if (editingHall.value?.hallId) {
      await ShowroomAdminApi.updateHall(payload)
    } else {
      await ShowroomAdminApi.createHall(payload)
    }
    message.success('展柜已保存')
    emit('saved', editingHall.value)
    dialogVisible.value = false
  } catch (error) {
    const resolved = error instanceof Error ? error : new Error(String(error))
    message.error(resolved.message)
    throw resolved
  } finally {
    saving.value = false
  }
}
</script>
