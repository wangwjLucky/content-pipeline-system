<template>
  <a-card :title="`任务详情 #${task?.id}`" :loading="loading">
    <a-descriptions v-if="task" bordered :column="2">
      <a-descriptions-item label="标题">{{ task.title }}</a-descriptions-item>
      <a-descriptions-item label="状态">
        <a-tag :color="statusColor(task.status)">{{ statusLabel(task.status) }}</a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="进度">{{ task.progress }}%</a-descriptions-item>
      <a-descriptions-item label="错误信息">{{ task.errorMessage || '-' }}</a-descriptions-item>
      <a-descriptions-item label="创建时间">{{ task.createdAt }}</a-descriptions-item>
    </a-descriptions>

    <a-divider>时间线</a-divider>
    <a-timeline v-if="timeline.length">
      <a-timeline-item v-for="e in timeline" :key="e.id">
        <template #dot>
          <clock-circle-outlined v-if="e.toStatus === 'CANCELLED'" style="color: red" />
          <check-circle-outlined v-else style="color: green" />
        </template>
        <span>{{ statusLabel(e.fromStatus) || '开始' }} → {{ statusLabel(e.toStatus) }}</span>
        <br /><small>{{ e.createdAt }} {{ e.operator ? `[${operatorLabel(e.operator)}]` : '' }} {{ e.comment ? `- ${e.comment}` : '' }}</small>
      </a-timeline-item>
    </a-timeline>
    <a-empty v-else description="暂无时间线" />

    <template #extra>
      <a-space>
        <a-button v-if="task?.status === 'ERROR'" @click="handleRetry">重试</a-button>
        <a-button v-if="task?.scriptId" @click="$router.push(`/scripts/${task.scriptId}`)">查看脚本</a-button>
        <a-button v-if="task?.status === 'STORYBOARD'" @click="$router.push(`/storyboards/${task.id}`)">编辑分镜</a-button>
        <a-button v-if="!['PUBLISHED', 'CANCELLED'].includes(task?.status)" danger @click="handleCancel">取消任务</a-button>
      </a-space>
    </template>
  </a-card>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { ClockCircleOutlined, CheckCircleOutlined } from '@ant-design/icons-vue'
import { getTask, getTaskTimeline, retryTask, cancelTask } from '../../api/task'

const route = useRoute()
const router = useRouter()
const task = ref<any>(null)
const timeline = ref<any[]>([])
const loading = ref(false)

function statusColor(s: string) {
  const map: Record<string, string> = { WAIT: 'orange', SCRIPTING: 'blue', SCRIPT_REVIEW: 'purple', STORYBOARD: 'geekblue', GENERATING: 'blue', VOICEOVER: 'cyan', EDITING: 'processing', REVIEW: 'purple', READY: 'green', PUBLISHED: 'green', CANCELLED: 'default', ERROR: 'red' }
  return map[s] || 'default'
}

function statusLabel(s: string) {
  const map: Record<string, string> = { WAIT: '等待中', SCRIPTING: '脚本生成中', SCRIPT_REVIEW: '脚本审核', STORYBOARD: '分镜中', GENERATING: '素材生成中', VOICEOVER: '配音中', EDITING: '剪辑中', REVIEW: '终审中', READY: '待发布', PUBLISHED: '已发布', CANCELLED: '已取消', ERROR: '异常' }
  return map[s] || s
}

function operatorLabel(s: string) {
  const map: Record<string, string> = { SYSTEM: '系统', admin: '管理员' }
  return map[s] || s
}

async function load() {
  const id = Number(route.params.id)
  loading.value = true
  try {
    const [taskRes, timelineRes]: any = await Promise.all([getTask(id), getTaskTimeline(id)])
    task.value = taskRes.data
    timeline.value = timelineRes.data || []
  } finally {
    loading.value = false
  }
}

function handleRetry() {
  Modal.confirm({ title: '确定重试？', onOk: async () => { await retryTask(Number(route.params.id)); message.success('已重试'); load() } })
}

function handleCancel() {
  Modal.confirm({ title: '确定取消？', onOk: async () => { await cancelTask(Number(route.params.id)); message.success('已取消'); load() } })
}

onMounted(() => load())
</script>