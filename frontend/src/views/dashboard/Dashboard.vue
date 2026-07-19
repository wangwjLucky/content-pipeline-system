<template>
  <div>
    <!-- 今日概览 -->
    <a-row :gutter="16" style="margin-bottom: 24px">
      <a-col :span="6" v-for="s in stats" :key="s.label">
        <a-card :body-style="{ padding: '20px' }">
          <a-statistic :title="s.label" :value="s.value" :value-style="{ color: s.color }">
            <template #suffix>
              <span style="font-size: 14px; color: #999">{{ s.unit || '' }}</span>
            </template>
          </a-statistic>
        </a-card>
      </a-col>
    </a-row>

    <!-- 近 7 天发布趋势 -->
    <a-card title="近 7 天发布趋势" style="margin-bottom: 24px">
      <div ref="chartRef" style="width: 100%; height: 300px"></div>
    </a-card>

    <!-- 进行中的任务 -->
    <a-card title="进行中的任务">
      <a-table
        :dataSource="tasks"
        :columns="columns"
        :loading="loading"
        rowKey="id"
        :pagination="{ pageSize: 10 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="statusColor(record.status)">{{ statusLabel(record.status) }}</a-tag>
          </template>
          <template v-if="column.key === 'progress'">
            <a-progress
              :percent="progressPercent(record.status)"
              :strokeColor="progressColor(record.status)"
              size="small"
              style="width: 140px"
            />
          </template>
          <template v-if="column.key === 'stage'">
            <span>{{ stageLabel(record.status) }}</span>
          </template>
          <template v-if="column.key === 'action'">
            <a @click="$router.push(`/tasks/${record.id}`)">查看</a>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getTasks } from '../../api/task'
import { getAnalyticsDaily, getAnalyticsOverview } from '../../api/analytics'

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 70 },
  { title: '标题', dataIndex: 'title', key: 'title' },
  { title: '当前阶段', key: 'stage', width: 120 },
  { title: '进度', key: 'progress', width: 180 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 110 },
  { title: '操作', key: 'action', width: 70 },
]

const stats = ref([
  { label: '总任务数', value: 0, color: '#1890ff', unit: '' },
  { label: '待审核脚本', value: 0, color: '#faad14', unit: '个' },
  { label: '待审核成片', value: 0, color: '#722ed1', unit: '个' },
  { label: '今日待发布', value: 0, color: '#52c41a', unit: '个' },
])

const tasks = ref<any[]>([])
const loading = ref(false)
const chartRef = ref<HTMLDivElement | null>(null)
let chartInstance: echarts.ECharts | null = null

const stageLabels: Record<string, string> = {
  WAIT: '待处理',
  SCRIPTING: '脚本生成中',
  SCRIPT_REVIEW: '脚本审核中',
  STORYBOARD: '分镜制作中',
  GENERATING: '素材生成中',
  VOICEOVER: '配音中',
  EDITING: '剪辑中',
  REVIEW: '成片审核中',
  READY: '待发布',
  PUBLISHED: '已发布',
  CANCELLED: '已取消',
  ERROR: '异常',
}

const progressMap: Record<string, number> = {
  WAIT: 0,
  SCRIPTING: 10,
  SCRIPT_REVIEW: 20,
  STORYBOARD: 30,
  GENERATING: 45,
  VOICEOVER: 60,
  EDITING: 75,
  REVIEW: 85,
  READY: 90,
  PUBLISHED: 100,
}

const progressColors: Record<string, string> = {
  WAIT: '#d9d9d9',
  SCRIPTING: '#1890ff',
  SCRIPT_REVIEW: '#722ed1',
  STORYBOARD: '#1890ff',
  GENERATING: '#1890ff',
  VOICEOVER: '#13c2c2',
  EDITING: '#faad14',
  REVIEW: '#722ed1',
  READY: '#52c41a',
  PUBLISHED: '#52c41a',
}

function statusLabel(s: string) { return stageLabels[s] || s }
function progressPercent(s: string) { return progressMap[s] || 0 }
function progressColor(s: string) { return progressColors[s] || '#1890ff' }
function stageLabel(s: string) { return stageLabels[s] || s }

function statusColor(s: string) {
  const map: Record<string, string> = {
    WAIT: 'orange', SCRIPTING: 'blue', SCRIPT_REVIEW: 'purple',
    STORYBOARD: 'geekblue', GENERATING: 'blue', VOICEOVER: 'cyan',
    EDITING: 'processing', REVIEW: 'purple', READY: 'green',
    PUBLISHED: 'green', CANCELLED: 'default', ERROR: 'red',
  }
  return map[s] || 'default'
}

function initChart() {
  if (!chartRef.value) return
  chartInstance = echarts.init(chartRef.value)
  chartInstance.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 50, right: 30, bottom: 30, top: 20 },
    xAxis: {
      type: 'category',
      data: [],
      axisLabel: { color: '#999' },
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      axisLabel: { color: '#999' },
      splitLine: { lineStyle: { color: '#f0f0f0' } },
    },
    series: [
      {
        name: '发布数',
        type: 'line',
        data: [],
        smooth: true,
        lineStyle: { color: '#1890ff', width: 2 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(24,144,255,0.25)' },
            { offset: 1, color: 'rgba(24,144,255,0.02)' },
          ]),
        },
        itemStyle: { color: '#1890ff' },
      },
      {
        name: '创建数',
        type: 'line',
        data: [],
        smooth: true,
        lineStyle: { color: '#52c41a', width: 2 },
        itemStyle: { color: '#52c41a' },
      },
    ],
  })
}

async function loadChart() {
  try {
    const res: any = await getAnalyticsDaily()
    const trends = res.data?.trends || []
    if (!chartInstance) return
    chartInstance.setOption({
      xAxis: { data: trends.map((d: any) => d.date.slice(5)) },
      series: [
        { data: trends.map((d: any) => d.published) },
        { data: trends.map((d: any) => d.created) },
      ],
    })
  } catch {
    // 模拟数据：显示空趋势
  }
}

onMounted(async () => {
  // 加载概览统计
  try {
    const overviewRes: any = await getAnalyticsOverview()
    const ov = overviewRes.data || {}
    stats.value[0].value = ov.totalTasks || 0
    stats.value[1].value = ov.pendingReviewScripts || 0
    stats.value[2].value = ov.pendingReviewVideos || 0
    stats.value[3].value = ov.pendingPublish || 0
  } catch { /* ignore */ }

  // 加载任务列表
  loading.value = true
  try {
    const res: any = await getTasks({ page: 1, size: 20 })
    tasks.value = res.data?.records || []
  } finally {
    loading.value = false
  }

  // 初始化图表
  await nextTick()
  initChart()
  loadChart()
})

onUnmounted(() => {
  chartInstance?.dispose()
})
</script>