<template>
  <div class="flex bg-[var(--el-bg-color-overlay)] p-12px mb-12px rounded-1">
    <div class="relative" @click="playSong">
      <el-image :src="songInfo.imageUrl" class="flex-none w-80px"/>
      <div class="bg-black bg-op-40 absolute top-0 left-0 w-full h-full flex items-center justify-center cursor-pointer">
        <Icon :icon="currentSong.id === songInfo.id ?  'solar:pause-circle-bold':'mdi:arrow-right-drop-circle'" :size="30" />
      </div>
    </div>
    <div class="ml-8px">
      <div>{{ songInfo.title }}</div>
      <div class="mt-8px text-12px text-[var(--el-text-color-secondary)] line-clamp-2">
        {{ songInfo.desc }}
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import type { PropType, Ref } from 'vue'
import type { MusicVO } from '@/api/ai/music'

defineOptions({ name: 'Index' })

type MusicDisplayVO = Partial<MusicVO> & {
  id: number
  title: string
  imageUrl: string
  audioUrl: string
  videoUrl: string
  lyric: string
  desc?: string
  date?: string
}

defineProps({
  songInfo: {
    type: Object as PropType<MusicDisplayVO>,
    required: true
  }
})

const emits = defineEmits(['play'])

const currentSong = inject<Ref<Partial<MusicDisplayVO>>>('currentSong', ref({}))

function playSong () {
  emits('play')
}
</script>
