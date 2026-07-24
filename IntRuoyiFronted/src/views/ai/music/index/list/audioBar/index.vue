<template>
  <div
    class="flex h-72px items-center justify-between border-1 border-solid border-[var(--el-border-color)] bg-[var(--el-bg-color-overlay)] px-2 b-l-none"
  >
    <div class="flex gap-[10px]">
      <el-image
        :src="currentSong.imageUrl || defaultCover"
        class="flex-none w-45px h-45px"
        fit="cover"
      />
      <div>
        <div>{{ currentSong.title || currentSong.name || '未选择歌曲' }}</div>
        <div class="text-[12px] text-gray-400">
          {{ currentSong.singer || currentSong.desc || '' }}
        </div>
      </div>
    </div>

    <div class="flex items-center gap-[12px]">
      <Icon icon="majesticons:back-circle" :size="20" class="cursor-pointer text-gray-300" />
      <Icon
        :icon="audioProps.paused ? 'mdi:arrow-right-drop-circle' : 'solar:pause-circle-bold'"
        :size="30"
        class="cursor-pointer"
        @click="toggleStatus('paused')"
      />
      <Icon icon="majesticons:next-circle" :size="20" class="cursor-pointer text-gray-300" />
      <div class="flex items-center gap-[16px]">
        <span>{{ audioProps.currentTime }}</span>
        <el-slider v-model="audioProps.duration" color="#409eff" class="w-[160px!important]" />
        <span>{{ audioProps.duration }}</span>
      </div>
      <audio
        v-if="audioUrl"
        ref="audioRef"
        v-bind="audioProps"
        controls
        v-show="false"
        @timeupdate="audioTimeUpdate"
      >
        <source :src="audioUrl" />
      </audio>
    </div>

    <div class="flex items-center gap-[16px]">
      <Icon
        :icon="audioProps.muted ? 'tabler:volume-off' : 'tabler:volume'"
        :size="20"
        class="cursor-pointer"
        @click="toggleStatus('muted')"
      />
      <el-slider v-model="audioProps.volume" color="#409eff" class="w-[160px!important]" />
    </div>
  </div>
</template>

<script lang="ts" setup>
import type { Ref } from 'vue'
import defaultCover from '@/assets/imgs/logo.png'
import { formatPast } from '@/utils/formatTime'

defineOptions({ name: 'AiMusicAudioBar' })

const currentSong = inject<Ref<Record<string, any>>>('currentSong', ref({}))
const audioUrl = computed(() => currentSong.value?.audioUrl || '')

const audioRef = ref<Nullable<HTMLAudioElement>>(null)
const audioProps = reactive({
  autoplay: true,
  paused: false,
  currentTime: '00:00',
  duration: '00:00',
  muted: false,
  volume: 50
})

function toggleStatus(type: string) {
  audioProps[type] = !audioProps[type]
  if (type === 'paused' && audioRef.value) {
    if (audioProps[type]) {
      audioRef.value.pause()
    } else {
      audioRef.value.play()
    }
  }
}

function audioTimeUpdate(args: any) {
  audioProps.currentTime = formatPast(new Date(args.timeStamp), 'mm:ss')
}
</script>
