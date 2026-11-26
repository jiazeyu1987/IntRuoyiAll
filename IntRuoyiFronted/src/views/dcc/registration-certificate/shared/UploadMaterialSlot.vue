<template>
  <div
    class="upload-material-slot"
    :class="{ 'is-error': errorMessage, 'has-files': files.length }"
  >
    <div class="upload-material-slot__header">
      <span class="upload-material-slot__title"
        ><span v-if="required" class="required-mark">*</span>{{ title }}</span
      >
      <div class="upload-material-slot__meta">
        <span v-if="!files.length" class="upload-material-slot__optional">{{
          required ? '必传 · 可多选' : '选填 · 可多选'
        }}</span>
        <template v-else
          ><span class="upload-material-slot__count">已选 {{ files.length }} 项</span
          ><button type="button" class="upload-material-slot__clear" @click="clear"
            >清空</button
          ></template
        >
      </div>
    </div>
    <el-upload
      v-model:file-list="files"
      class="upload-material-slot__picker"
      action="#"
      :aria-label="title"
      multiple
      drag
      :auto-upload="false"
      :show-file-list="false"
    >
      <div v-if="!files.length" class="upload-material-slot__empty"
        ><el-icon class="upload-material-slot__upload-icon"><Upload /></el-icon
        ><span class="upload-material-slot__primary">点击或拖拽上传</span
        ><span class="upload-material-slot__hint" :title="hint">{{ hint }}</span></div
      >
      <div v-else class="upload-material-slot__add"
        ><el-icon class="upload-material-slot__plus"><Plus /></el-icon
        ><span>继续拖拽或点击添加文件</span></div
      >
    </el-upload>
    <div
      v-if="files.length"
      class="upload-material-slot__list"
      :class="{ 'is-overflow': files.length > 3 }"
    >
      <div
        v-for="(file, index) in files"
        :key="file.uid ?? `${file.name}-${index}`"
        class="upload-material-slot__row"
        ><el-icon class="upload-material-slot__file-icon"><Document /></el-icon
        ><span class="upload-material-slot__file-name" :title="file.name">{{ file.name }}</span
        ><span class="upload-material-slot__file-size">{{ formatSize(file.size) }}</span
        ><button
          type="button"
          class="upload-material-slot__remove"
          :aria-label="`删除${file.name}`"
          @click="remove(index)"
          >×</button
        ></div
      >
    </div>
    <p v-if="files.length > 3" class="upload-material-slot__overflow-tip"
      >列表可上下滚动查看全部 {{ files.length }} 个文件</p
    >
    <p v-if="notice" class="upload-material-slot__notice"
      ><el-icon><InfoFilled /></el-icon><span>{{ notice }}</span></p
    >
    <p v-if="errorMessage" class="upload-material-slot__error"
      ><el-icon><Warning /></el-icon>{{ errorMessage }}</p
    >
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Document, InfoFilled, Plus, Upload, Warning } from '@element-plus/icons-vue'
import type { UploadUserFile } from 'element-plus'
const props = withDefaults(
  defineProps<{
    modelValue: UploadUserFile[]
    title: string
    hint?: string
    notice?: string
    required?: boolean
    errorMessage?: string
  }>(),
  { hint: '支持批量选择或拖拽添加文件', notice: '', required: false, errorMessage: '' }
)
const emit = defineEmits<{ 'update:modelValue': [UploadUserFile[]] }>()
const files = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})
const clear = () => emit('update:modelValue', [])
const remove = (index: number) =>
  emit(
    'update:modelValue',
    props.modelValue.filter((_, current) => current !== index)
  )
const formatSize = (size?: number) => {
  if (size === undefined || !Number.isFinite(size)) return ''
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / (1024 * 1024)).toFixed(1)} MB`
}
</script>

<style scoped>
.upload-material-slot {
  min-width: 0;
}
.upload-material-slot__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-height: 24px;
  margin-bottom: 8px;
}
.upload-material-slot__title {
  min-width: 0;
  color: var(--el-text-color-primary);
  font-size: 13px;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.upload-material-slot__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  color: var(--el-text-color-secondary);
  font-size: 11px;
}
.upload-material-slot__count {
  padding: 3px 7px;
  border-radius: 5px;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}
.upload-material-slot__clear {
  padding: 0;
  border: 0;
  color: var(--el-text-color-secondary);
  background: transparent;
  cursor: pointer;
}
.upload-material-slot__clear:hover,
.upload-material-slot__remove:hover {
  color: var(--el-color-danger);
}
.upload-material-slot__picker,
.upload-material-slot__picker :deep(.el-upload),
.upload-material-slot__picker :deep(.el-upload-dragger) {
  width: 100%;
}
.upload-material-slot__picker :deep(.el-upload-dragger) {
  min-height: 116px;
  padding: 16px;
  border-radius: 10px;
  border-color: var(--el-border-color);
  background: var(--el-bg-color);
}
.upload-material-slot__empty {
  display: flex;
  min-height: 82px;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 4px;
}
.upload-material-slot__upload-icon,
.upload-material-slot__plus {
  color: var(--el-color-primary);
  font-size: 26px;
  line-height: 1;
}
.upload-material-slot__primary {
  color: var(--el-text-color-primary);
  font-size: 13px;
  font-weight: 500;
}
.upload-material-slot__hint {
  max-width: 100%;
  color: var(--el-text-color-secondary);
  font-size: 11px;
  line-height: 17px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.upload-material-slot__add {
  display: flex;
  min-height: 42px;
  align-items: center;
  justify-content: center;
  gap: 6px;
  color: var(--el-text-color-regular);
  font-size: 12px;
}
.upload-material-slot__add .upload-material-slot__plus {
  font-size: 18px;
}
.upload-material-slot__list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 8px;
}
.upload-material-slot__list.is-overflow {
  max-height: 164px;
  padding-right: 4px;
  overflow-y: auto;
  scrollbar-width: thin;
}
.upload-material-slot__row {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 7px;
  min-height: 38px;
  padding: 0 8px;
  border-radius: 7px;
  background: var(--el-fill-color-light);
}
.upload-material-slot__file-icon {
  flex-shrink: 0;
  color: var(--el-color-primary);
}
.upload-material-slot__file-name {
  min-width: 0;
  flex: 1;
  color: var(--el-text-color-regular);
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.upload-material-slot__file-size {
  flex-shrink: 0;
  color: var(--el-text-color-secondary);
  font-size: 11px;
}
.upload-material-slot__remove {
  width: 22px;
  height: 22px;
  padding: 0;
  border: 0;
  color: var(--el-text-color-secondary);
  background: transparent;
  font-size: 18px;
  line-height: 20px;
  cursor: pointer;
}
.upload-material-slot__overflow-tip {
  margin: 4px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 11px;
  text-align: right;
}
.upload-material-slot__error {
  display: flex;
  align-items: center;
  gap: 5px;
  margin: 6px 0 0;
  color: var(--el-color-danger);
  font-size: 12px;
}
.upload-material-slot.is-error .upload-material-slot__picker :deep(.el-upload-dragger) {
  border-color: var(--el-color-danger);
  background: var(--el-color-danger-light-9);
}

.required-mark {
  margin-right: 4px;
  color: var(--el-color-danger);
}
.upload-material-slot.has-files .upload-material-slot__picker :deep(.el-upload-dragger) {
  min-height: 44px;
  padding: 0 8px;
}
.upload-material-slot__notice {
  display: flex;
  align-items: flex-start;
  gap: 5px;
  margin: 8px 0 0;
  padding: 6px 8px;
  border: 1px solid var(--el-color-warning-light-7);
  border-radius: 6px;
  background: var(--el-color-warning-light-9);
  color: var(--el-color-warning-dark-2);
  font-size: 11px;
  line-height: 17px;
}
.upload-material-slot__notice .el-icon {
  flex-shrink: 0;
  margin-top: 2px;
}
</style>
