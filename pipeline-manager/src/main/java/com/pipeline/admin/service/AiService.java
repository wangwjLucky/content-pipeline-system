package com.pipeline.admin.service;

import com.pipeline.admin.mq.MessageSender;
import com.pipeline.admin.mq.TaskMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

import static com.pipeline.admin.config.RabbitConfig.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final MessageSender messageSender;

    @Value("${pipeline.callback-base-url:http://host.docker.internal:8080}")
    private String callbackBaseUrl;

    private String callbackUrl() {
        return callbackBaseUrl + "/api/v1/tasks/callback";
    }

    public void sendScriptGenerate(Long taskId, String topicTitle, Map<String, Object> params) {
        TaskMessage msg = new TaskMessage();
        msg.setTaskId(taskId);
        msg.setAction("generate_script");
        msg.setParams(params);
        msg.setCallbackUrl(callbackUrl());
        msg.setTimestamp(LocalDateTime.now());
        messageSender.send(QUEUE_SCRIPT_GENERATE, msg);
        log.info("脚本生成任务已发送: taskId={}, topic={}", taskId, topicTitle);
    }

    public void sendVideoGenerate(Long taskId, Map<String, Object> params) {
        TaskMessage msg = new TaskMessage();
        msg.setTaskId(taskId);
        msg.setAction("generate_video");
        msg.setParams(params);
        msg.setCallbackUrl(callbackUrl());
        msg.setTimestamp(LocalDateTime.now());
        messageSender.send(QUEUE_VIDEO_GENERATE, msg);
    }

    public void sendPromptGenerate(Long taskId, Map<String, Object> params) {
        TaskMessage msg = new TaskMessage();
        msg.setTaskId(taskId);
        msg.setAction("generate_prompt");
        msg.setParams(params);
        msg.setCallbackUrl(callbackUrl());
        msg.setTimestamp(LocalDateTime.now());
        messageSender.send(QUEUE_PROMPT_GENERATE, msg);
    }

    public void sendImageGenerate(Long taskId, Map<String, Object> params) {
        TaskMessage msg = new TaskMessage();
        msg.setTaskId(taskId);
        msg.setAction("generate_image");
        msg.setParams(params);
        msg.setCallbackUrl(callbackUrl());
        msg.setTimestamp(LocalDateTime.now());
        messageSender.send(QUEUE_IMAGE_GENERATE, msg);
    }

    public void sendVoiceGenerate(Long taskId, Map<String, Object> params) {
        TaskMessage msg = new TaskMessage();
        msg.setTaskId(taskId);
        msg.setAction("generate_voice");
        msg.setParams(params);
        msg.setCallbackUrl(callbackUrl());
        msg.setTimestamp(LocalDateTime.now());
        messageSender.send(QUEUE_VOICE_GENERATE, msg);
    }

    public void sendFfmpegCompile(Long taskId, Map<String, Object> params) {
        TaskMessage msg = new TaskMessage();
        msg.setTaskId(taskId);
        msg.setAction("ffmpeg_compile");
        msg.setParams(params);
        msg.setCallbackUrl(callbackUrl());
        msg.setTimestamp(LocalDateTime.now());
        messageSender.send(QUEUE_FFMPEG_COMPILE, msg);
    }
}