<template>
  <a-card title="发布日历">
    <template #extra>
      <a-button @click="$router.push('/publish')">返回列表</a-button>
    </template>
    <a-calendar v-model:value="currentDate">
      <template #dateCellRender="{ current }">
        <ul style="list-style: none; padding: 0; margin: 0">
          <li v-for="item in getDayData(current)" :key="item.id" style="font-size: 12px">
            <a-tag :color="item.status === 'PUBLISHED' ? 'green' : 'orange'" style="margin: 1px 0; cursor: pointer" @click="$router.push(`/tasks/${item.taskId}`)">
              {{ item.title || `任务 #${item.taskId}` }}
            </a-tag>
          </li>
        </ul>
      </template>
    </a-calendar>
  </a-card>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getPublishes } from '../../api/publish'

const currentDate = ref(new Date())
const publishes = ref<any[]>([])

function getDayData(date: Date) {
  // 使用本地日期而非 UTC，避免时区偏移导致日期错位
  const dateStr = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
  return publishes.value.filter((p: any) => {
    const d = (p.publishedAt || p.scheduledAt || p.createdAt || '').slice(0, 10)
    return d === dateStr
  })
}

async function load() {
  try {
    const res: any = await getPublishes(0)
    publishes.value = res.data || []
  } catch { /* ignore */ }
}

onMounted(() => load())
</script>