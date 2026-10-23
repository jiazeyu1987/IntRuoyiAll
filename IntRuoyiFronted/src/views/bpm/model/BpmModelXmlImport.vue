<template>
  <Dialog v-model="visible" title="导入 BPMN 流程图" width="680px">
    <section v-loading="loading" data-testid="bpm-model-xml-import">
      <el-alert v-if="error" :title="error" type="error" :closable="false" />
      <template v-if="model">
        <p>{{ model.name }} · {{ model.key }}</p>
        <p>保存流程图保留当前表单和管理人配置。发布后新申请使用新流程，已有实例保留原定义。</p>
        <label>选择 BPMN XML 文件 <input data-testid="bpm-model-xml-file" type="file" accept=".xml,.bpmn" :disabled="loading" @change="selectFile" /></label>
        <p v-if="fileName">已选择：{{ fileName }}</p>
      </template>
    </section>
    <template #footer>
      <el-button type="primary" :disabled="loading || !xml || !model" @click="save">保存流程图</el-button>
      <el-button :disabled="loading" @click="visible = false">取消</el-button>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import { ref, watch } from 'vue'
import * as ModelApi from '@/api/bpm/model'
import { checkPermi } from '@/utils/permission'
import { buildBpmnImport, type ImportModel } from './bpmn-import-contract'
const emit = defineEmits<{ saved: [] }>()
const message = useMessage()
const visible = ref(false), loading = ref(false), error = ref(''), xml = ref(''), fileName = ref('')
const model = ref<(ImportModel & { name: string })>()
let sequence = 0
watch(visible, value => { if (!value) { sequence++; model.value = undefined; xml.value = ''; fileName.value = ''; error.value = '' } })
const permission = () => { if (!checkPermi(['bpm:model:query']) || !checkPermi(['bpm:model:update'])) throw new Error('当前账号没有模型查询或修改权限') }
const open = async (id: string | number) => {
  const token = ++sequence
  visible.value = true; loading.value = true; error.value = ''; model.value = undefined; xml.value = ''; fileName.value = ''
  try {
    permission()
    const actual = await ModelApi.getModel(String(id))
    if (token !== sequence || !visible.value) return
    if (!actual || String(actual.id) !== String(id) || Number(actual.type) !== 10) throw new Error('当前 BPMN 模型身份不匹配')
    model.value = actual
  } catch (cause) { if (token === sequence) error.value = cause instanceof Error ? cause.message : String(cause) }
  finally { if (token === sequence) loading.value = false }
}
const selectFile = async (event: Event) => {
  const token = sequence, file = (event.target as HTMLInputElement).files?.[0]
  xml.value = ''; fileName.value = ''; error.value = ''
  if (!file) return
  try {
    const contents = await file.text()
    if (token !== sequence || !visible.value) return
    const doc = new DOMParser().parseFromString(contents, 'application/xml')
    if (doc.getElementsByTagName('parsererror').length || /<!DOCTYPE|<!ENTITY/i.test(contents)) throw new Error('流程图 XML 无效或包含不支持的外部声明')
    const ids = Array.from(doc.getElementsByTagNameNS('http://www.omg.org/spec/BPMN/20100524/MODEL', 'process')).map(p => p.getAttribute('id') || '')
    buildBpmnImport(model.value!, model.value!, contents, ids)
    xml.value = contents; fileName.value = file.name
  } catch (cause) { if (token === sequence) error.value = cause instanceof Error ? cause.message : String(cause) }
}
const save = async () => {
  if (loading.value || !model.value || !xml.value) return
  const token = sequence, original = JSON.parse(JSON.stringify(model.value)), contents = xml.value
  loading.value = true; error.value = ''
  try {
    permission()
    const current = await ModelApi.getModel(String(original.id))
    if (token !== sequence || !visible.value) return
    const doc = new DOMParser().parseFromString(contents, 'application/xml')
    const ids = Array.from(doc.getElementsByTagNameNS('http://www.omg.org/spec/BPMN/20100524/MODEL', 'process')).map(p => p.getAttribute('id') || '')
    const payload = buildBpmnImport(original, current, contents, ids)
    await ModelApi.updateModel(payload)
    if (token !== sequence || !visible.value) return
    message.success('流程图已保存，请在模型列表单独发布')
    visible.value = false; emit('saved')
  } catch (cause) { if (token === sequence) error.value = cause instanceof Error ? cause.message : String(cause) }
  finally { if (token === sequence) loading.value = false }
}
defineExpose({ open })
</script>
