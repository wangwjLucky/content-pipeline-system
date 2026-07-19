import request from './request'

export function getPublishes(taskId?: number) {
  return request.get('/publish', { params: { taskId } })
}

export function createPublish(data: any) {
  return request.post('/publish', data)
}

export function doPublish(id: number) {
  return request.post(`/publish/${id}/publish`)
}

export function schedulePublish(id: number, scheduledAt: string) {
  return request.post(`/publish/${id}/schedule`, { scheduledAt })
}

export function cancelPublishItem(id: number) {
  return request.post(`/publish/${id}/cancel`)
}

export function getPublishCalendar(startDate?: string, endDate?: string) {
  return request.get('/publish/calendar', { params: { startDate, endDate } })
}

export function getPlatformAccounts(platform?: string) {
  return request.get('/publish/accounts', { params: { platform } })
}