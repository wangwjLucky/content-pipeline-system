package com.pipeline.admin.service;

import java.util.List;
import java.util.Map;

public interface AnalyticsService {

    /** 总览数据：任务总数、完成数、进行中、待审核脚本、待发布、今日发布 */
    Map<String, Object> getOverview();

    /** 日报数据：指定日期范围的发布/创建趋势 */
    Map<String, Object> getDaily(String startDate, String endDate);

    /** 选题效果统计 */
    Map<String, Object> getTopics(int limit);

    /** 账号维度统计 */
    Map<String, Object> getAccounts();
}