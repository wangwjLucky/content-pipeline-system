package com.pipeline.admin.service;

import com.pipeline.admin.entity.Task;
import com.pipeline.admin.entity.Topic;
import com.pipeline.admin.mapper.TaskMapper;
import com.pipeline.admin.mapper.TopicMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TopicServiceImpl implements TopicService {
    private final TopicMapper topicMapper;
    private final TaskMapper taskMapper;

    @Override
    public Page<Topic> list(int page, int size, String status) {
        LambdaQueryWrapper<Topic> q = new LambdaQueryWrapper<Topic>()
                .eq(status != null && !status.isEmpty(), Topic::getStatus, status)
                .orderByDesc(Topic::getCreatedAt);
        return topicMapper.selectPage(new Page<>(page, size), q);
    }

    @Override
    public Topic get(Long id) {
        return topicMapper.selectById(id);
    }

    @Override
    public Topic create(Topic topic) {
        topic.setStatus("PENDING");
        topicMapper.insert(topic);
        return topic;
    }

    @Override
    public Topic update(Long id, Topic topic) {
        topic.setId(id);
        topicMapper.updateById(topic);
        return topicMapper.selectById(id);
    }

    @Override
    public void delete(Long id) {
        // 检查是否有关联任务
        Long count = taskMapper.selectCount(
                new LambdaQueryWrapper<Task>().eq(Task::getTopicId, id));
        if (count != null && count > 0) {
            throw new IllegalArgumentException("选题有关联任务，无法删除。请先删除关联任务后再操作。");
        }
        topicMapper.deleteById(id);
    }
}