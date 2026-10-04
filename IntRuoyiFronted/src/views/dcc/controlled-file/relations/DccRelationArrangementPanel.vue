<template>
  <section v-loading="loading">
    <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon />
    <DccRelationArrangements v-model="rows" :relations="relations" :assignees="assignees" :readonly="readonly || loading || !loaded" />
  </section>
</template>
<script setup lang="ts">
import {ref,watch} from 'vue'
import DccRelationArrangements from './DccRelationArrangements.vue'
import {validateArrangementForm} from './arrangement-form'
import type {ArrangementForm,ArrangementCommand} from './arrangement-form'
import type {FileCandidate} from './selector-state'
const props=defineProps<{contextKey:string;sourceFileId:string;applicationRound:string;readonly:boolean;relations:FileCandidate[];assignees:{id:string;name:string}[];loadArrangements:(sourceFileId:string,applicationRound:string)=>Promise<ArrangementCommand[]>}>()
const emit=defineEmits<{change:[rows:ArrangementForm[]]}>()
const rows=ref<ArrangementForm[]>([]),loading=ref(false),loaded=ref(false),error=ref('')
let generation=0
watch(()=>[props.contextKey,props.sourceFileId,props.applicationRound] as const,async()=>{
  const token=++generation;rows.value=[];loaded.value=false;error.value='';loading.value=true
  try{
    if(!props.applicationRound.trim()||!/^[1-9]\d*$/.test(props.sourceFileId))throw new Error('请先取得正式申请版本和轮次')
    const saved=await props.loadArrangements(props.sourceFileId,props.applicationRound)
    if(token!==generation)return
    // Frozen historical people can become unavailable today; retain them for traceability.
    // Current signed submission still validates against the actual current candidate list below.
    rows.value=saved.map(row=>({...row}));loaded.value=true
  }catch(cause){if(token===generation)error.value=String(cause)}
  finally{if(token===generation)loading.value=false}
},{immediate:true})
watch(rows,value=>{if(loaded.value&&!props.readonly)emit('change',value.map(row=>({...row})))},{deep:true})
const validate=()=>{
  if(props.readonly)throw new Error('历史整改安排只读')
  if(!loaded.value||loading.value)throw new Error('整改安排尚未读取成功')
  try{const payload=validateArrangementForm(rows.value,props.relations,props.assignees);error.value='';return payload}
  catch(cause){error.value=String(cause);throw cause}
}
defineExpose({validate})
</script>
