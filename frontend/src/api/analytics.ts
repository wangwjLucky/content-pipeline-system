import request from './request'

export function getAnalyticsOverview() {
  return request.get('/analytics/overview')
}

export function getAnalyticsDaily(startDate?: string, endDate?: string) {
  return request.get('/analytics/daily', { params: { startDate, endDate } })
}

export function getAnalyticsTopics(limit = 20) {
  return request.get('/analytics/topics', { params: { limit } })
}

export function getAnalyticsAccounts() {
  return request.get('/analytics/accounts')
}