<script setup lang="ts">
import { onMounted, ref } from 'vue'
import XButton from '../../../src/components/XButton/src/XButton.vue'

const fixture = ref<HTMLElement>()
const listenerCount = ref(0), propCount = ref(0), disabledCount = ref(0), loadingCount = ref(0)
const eventSame = ref(true)
let nativeEvent: MouseEvent | undefined
const record = (kind: string, event: MouseEvent) => {
  eventSame.value = eventSame.value && event === nativeEvent && event instanceof MouseEvent && event.type === 'click'
  if (kind === 'listener') listenerCount.value++
  else if (kind === 'prop') propCount.value++
  else if (kind === 'disabled') disabledCount.value++
  else loadingCount.value++
}
const onProp = (event: MouseEvent) => record('prop', event)
onMounted(() => fixture.value!.addEventListener('click', event => { nativeEvent = event }, { capture: true }))
</script>

<template>
  <main ref="fixture" data-testid="x-button-behavior-harness">
    <XButton data-testid="listener" title="Listener button" @click="record('listener', $event)" />
    <XButton data-testid="prop" title="Prop button" :onClick="onProp" />
    <XButton data-testid="disabled" title="Disabled button" disabled @click="record('disabled', $event)" />
    <XButton data-testid="loading" title="Loading button" loading @click="record('loading', $event)" />
    <output data-testid="state">{{ JSON.stringify({ listenerCount, propCount, disabledCount, loadingCount, eventSame }) }}</output>
  </main>
</template>

<style scoped>
main { padding: 32px; display: flex; gap: 18px; flex-wrap: wrap; }
output { width: 100%; padding: 20px; }
</style>
