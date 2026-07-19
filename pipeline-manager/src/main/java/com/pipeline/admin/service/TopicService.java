package com.pipeline.admin.service;

import com.pipeline.admin.entity.Topic;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface TopicService {

    Page<Topic> list(int page, int size, String status);

    Topic get(Long id);

    Topic create(Topic topic);

    Topic update(Long id, Topic topic);

    void delete(Long id);
}