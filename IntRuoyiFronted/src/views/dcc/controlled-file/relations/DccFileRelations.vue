<template>
  <section>
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
    <el-button v-if="mode !== 'history' && canEdit" :disabled="loading || Boolean(error)" @click="open">关联</el-button>
    <p v-if="mode === 'history'">本次审批当时的关联版本</p>
    <el-table :data="files" v-loading="loading">
      <el-table-column prop="fileName" label="关联文件" /><el-table-column prop="fileNumber" label="编号" /><el-table-column prop="versionNo" label="受控版本" />
      <el-table-column label="状态"><template #default="{ row }">{{ mode === 'history' ? '历史审批版本' : row.status === 'OBSOLETE' ? '源文件已作废' : row.pendingEffect ? '待生效' : row.controlled ? '受控版本' : '无可用受控版本' }}</template></el-table-column>
      <el-table-column label="正文"><template #default="{ row }"><el-button link :disabled="!row.canPreview" @click="preview(row)">查看</el-button></template></el-table-column>
    </el-table>
    <DccFileSelector v-if="mode !== 'history'" v-model="visible" :source="source" purpose="relations" :directories="directories" :selected="files" :load-page="loadPage" :open-preview="openPreview" :persist="save" @confirmed="saved">
      <template v-if="mode === 'current'" #confirmation-fields><el-input v-model="editor.reason" type="textarea" placeholder="关联操作原因（必填）" /></template>
    </DccFileSelector>
  </section>
</template>
<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import DccFileSelector from './DccFileSelector.vue'
import type { SelectorSource, DirectoryNode } from './DccFileSelector.vue'
import type { FileCandidate, SelectorQuery, SelectorPage } from './selector-state'
import { RelationEditorState } from './relation-editor-state'
import type { CurrentRelationContext, RelationSaveCommand, RelationSaveResult } from './relation-editor-state'
const props=defineProps<{mode:'upload'|'current'|'history';source:SelectorSource;directories:DirectoryNode[];canEdit:boolean;autoOpenEditor?:boolean;modelValue:FileCandidate[];loadPage:(query:SelectorQuery)=>Promise<SelectorPage>;loadCurrent:(sourceFileId:string)=>Promise<CurrentRelationContext>;loadHistory:(sourceFileId:string)=>Promise<FileCandidate[]>;persistCurrent:(sourceFileId:string,command:RelationSaveCommand)=>Promise<RelationSaveResult>;openPreview:(row:FileCandidate)=>Promise<unknown>}>()
const emit=defineEmits<{'update:modelValue':[files:FileCandidate[]];changed:[result:RelationSaveResult];'editor-opened':[]}>()
const files=ref<FileCandidate[]>([]),visible=ref(false),loading=ref(false),error=ref('')
const editor=reactive(new RelationEditorState(()=>crypto.randomUUID()))
let generation=0
let autoOpenedSource:string|undefined
const open=()=>{if(props.mode!=='history'&&props.canEdit&&!loading.value&&!error.value)visible.value=true}
const save=async(selected:FileCandidate[])=>{
  if(!props.canEdit||props.mode==='history')throw new Error('当前文件关联只读')
  if(props.mode==='upload'){emit('update:modelValue',selected.map(row=>({...row})));return}
  const result=await editor.save(selected,props.persistCurrent);files.value=selected.map(row=>({...row}));emit('changed',result)
}
const saved=()=>{visible.value=false}
const preview=async(row:FileCandidate)=>{
  if(!row.canPreview)return
  const token=generation
  try{await props.openPreview({...row})}catch(cause){if(token===generation)error.value=String(cause)}
}
watch(()=>[props.source.contextKey,props.mode] as const,async()=>{
  const token=++generation;editor.invalidate();visible.value=false;files.value=[];error.value='';loading.value=true
  try {
    if(props.mode==='upload'){files.value=props.modelValue.map(row=>({...row}));return}
    if(!props.source.controlledFileId)throw new Error('请先选择正式文件版本')
    if(props.mode==='history'){
      const history=await props.loadHistory(props.source.controlledFileId);if(token===generation)files.value=history
    }else{
      const current=await props.loadCurrent(props.source.controlledFileId);if(token===generation){
        if(current.sourceControlledFileId!==props.source.controlledFileId)throw new Error('当前文件关联来源版本已变化，请重新选择文件')
        editor.setCurrent(current);files.value=current.files
      }
    }
  }catch(cause){if(token===generation)error.value=String(cause)}
  finally{if(token===generation)loading.value=false}
},{immediate:true})
watch(()=>[props.source.contextKey,props.mode,props.autoOpenEditor,props.canEdit,loading.value,error.value] as const,()=>{
  if(props.mode==='current'&&props.autoOpenEditor&&props.canEdit&&!loading.value&&!error.value&&autoOpenedSource!==props.source.contextKey){
    autoOpenedSource=props.source.contextKey;visible.value=true;emit('editor-opened')
  }
},{immediate:true})
watch(()=>[props.modelValue,visible.value] as const,([selected,open])=>{if(props.mode==='upload'&&!open)files.value=selected.map(row=>({...row}))},{deep:true})
</script>
