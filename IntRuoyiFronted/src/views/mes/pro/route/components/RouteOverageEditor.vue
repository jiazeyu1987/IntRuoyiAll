<template>
  <el-alert v-if="parsed.error" :title="parsed.error" type="error" :closable="false" />
  <el-form v-else label-position="top" :disabled="disabled">
    <el-form-item label="允许超量比例（%）" required>
      <el-input-number
        :model-value="parsed.config.overagePercent"
        :min="0"
        :max="100"
        :value-on-clear="null"
        aria-label="允许超量比例（%）"
        placeholder="请输入0至100"
        class="w-full"
        @update:model-value="updateOverage"
      />
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
const props = defineProps<{ modelValue: string; disabled: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [value: string] }>()

const parsed = computed(() => {
  try {
    const config = JSON.parse(props.modelValue)
    if (!config || typeof config !== 'object' || Array.isArray(config)) {
      throw new Error('生产配置必须是对象')
    }
    return { config, error: '' }
  } catch (cause) {
    return {
      config: undefined,
      error: `生产配置格式无效，请在生产配置中修正：${cause instanceof Error ? cause.message : String(cause)}`
    }
  }
})

const updateOverage = (value: number | undefined | null) => {
  if (props.disabled || parsed.value.error) return
  emit('update:modelValue', JSON.stringify({
    ...parsed.value.config,
    overagePercent: value ?? null
  }, null, 2))
}
</script>
