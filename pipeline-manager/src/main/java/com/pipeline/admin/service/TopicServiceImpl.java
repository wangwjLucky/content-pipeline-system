package com.pipeline.admin.service;

import com.pipeline.admin.entity.Topic;
import com.pipeline.admin.mapper.TopicMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TopicServiceImpl implements TopicService {
    private final TopicMapper topicMapper;

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
        topicMapper.deleteById(id);
    }
}