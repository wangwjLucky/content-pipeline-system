<template>
  <div>
    <a-row :gutter="16" style="margin-bottom: 16px">
      <a-col :span="6">
        <a-card><a-statistic title="总任务数" :value="overview.totalTasks" /></a-card>
      </a-col>
      <a-col :span="6">
        <a-card><a-statistic title="待审核脚本" :value="overview.pendingReviewScripts" /></a-card>
      </a-col>
      <a-col :span="6">
        <a-card><a-statistic title="待发布" :value="overview.pendingPublish" /></a-card>
      </a-col>
      <a-col :span="6">
        <a-card><a-statistic title="今日发布" :value="overview.todayPublished" /></a-card>
      </a-col>
    </a-row>

    <a-card title="近 7 天发布趋势" style="margin-bottom: 16px">
      <div ref="chartRef" style="width: 100%; height: 300px"></div>
    </a-card>

    <a-row :gutter="16">
      <a-col :span="12">
        <a-card title="选题概况">
          <a-descriptions :column="1" size="small">
            <a-descriptions-item label="总选题数">{{ topicsData.totalTopics }}</a-descriptions-item>
            <a-descriptions-item label="热门选题">{{ topicsData.hotTopics }}</a-descriptions-item>
            <a-descriptions-item label="已完成">{{ topicsData.completedTopics }}</a-descriptions-item>
          </a-descriptions>
        </a-card>
      </a-col>
      <a-col :span="12">
        <a-card title="账号概况">
          <a-descriptions :column="1" size="small">
            <a-descriptions-item label="总账号数">{{ accountsData.totalAccounts }}</a-descriptions-item>
            <a-descriptions-item label="活跃账号">{{ accountsData.activeAccounts }}</a-descriptions-item>
            <a-descriptions-item label="总发布量">{{ accountsData.totalPublished }}</a-descriptions-item>
          </a-descriptions>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getAnalyticsOverview, getAnalyticsDaily, getAnalyticsTopics, getAnalyticsAccounts } from '../../api/analytics'

const overview = ref<any>({})
const dailyData = ref<any[]>([])
const topicsData = ref<any>({})
const accountsData = ref<any>({})
const chartRef = ref<HTMLDivElement | null>(null)
let chartInstance: echarts.ECharts | null = null

function initChart() {
  if (!chartRef.value) return
  chartInstance = echarts.init(chartRef.value)
  chartInstance.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['创建数', '发布数'], bottom: 0 },
    grid: { left: 50, right: 30, bottom: 40, top: 20 },
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
        name: '创建数',
        type: 'line',
        smooth: true,
        data: [],
        lineStyle: { color: '#52c41a', width: 2 },
        itemStyle: { color: '#52c41a' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(82,196,26,0.2)' },
            { offset: 1, color: 'rgba(82,196,26,0.02)' },
          ]),
        },
      },
      {
        name: '发布数',
        type: 'line',
        smooth: true,
        data: [],
        lineStyle: { color: '#1890ff', width: 2 },
        itemStyle: { color: '#1890ff' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(24,144,255,0.2)' },
            { offset: 1, color: 'rgba(24,144,255,0.02)' },
          ]),
        },
      },
    ],
  })
}

onMounted(async () => {
  // 各 API 独立处理，单个失败不影响其他数据加载
  const [ovRes, dailyRes, topicsRes, accountsRes] = await Promise.all([
    getAnalyticsOverview().catch(() => ({ data: {} })),
    getAnalyticsDaily().catch(() => ({ data: { trends: [] } })),
    getAnalyticsTopics().catch(() => ({ data: {} })),
    getAnalyticsAccounts().catch(() => ({ data: {} })),
  ])
  overview.value = (ovRes as any).data || {}
  dailyData.value = (dailyRes as any).data?.trends || []
  topicsData.value = (topicsRes as any).data || {}
  accountsData.value = (accountsRes as any).data || {}

  await nextTick()
  initChart()
  if (chartInstance && dailyData.value.length) {
    chartInstance.setOption({
      xAxis: { data: dailyData.value.map((d: any) => d.date.slice(5)) },
      series: [
        { data: dailyData.value.map((d: any) => d.created) },
        { data: dailyData.value.map((d: any) => d.published) },
      ],
    })
  }
})

onUnmounted(() => {
  chartInstance?.dispose()
})
</script>