package com.pipeline.admin.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pipeline.admin.entity.PipelineDefinition;
import com.pipeline.admin.entity.PipelineNode;
import com.pipeline.admin.entity.SysUser;
import com.pipeline.admin.mapper.PipelineDefinitionMapper;
import com.pipeline.admin.mapper.PipelineNodeMapper;
import com.pipeline.admin.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final PipelineDefinitionMapper pipelineDefinitionMapper;
    private final PipelineNodeMapper pipelineNodeMapper;

    @Override
    public void run(String... args) {
        initAdminUser();
        initPipelineDefinitions();
    }

    private void initAdminUser() {
        SysUser admin = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, "admin"));
        if (admin != null) return;
        SysUser user = new SysUser();
        user.setUsername("admin");
        user.setPassword(passwordEncoder.encode("admin123"));
        user.setNickname("管理员");
        user.setRoleId(1L);
        user.setStatus("ENABLED");
        userMapper.insert(user);
        log.info("默认管理员用户已创建: admin / admin123");
    }

    private void initPipelineDefinitions() {
        initVideoPipeline();
        initTextPipeline();
        initImagePipeline();
        initImageTextPipeline();
    }

    private long getOrCreatePipeline(String code, String name, String description) {
        PipelineDefinition exist = pipelineDefinitionMapper.selectOne(
                new LambdaQueryWrapper<PipelineDefinition>().eq(PipelineDefinition::getCode, code));
        if (exist != null) return exist.getId();
        PipelineDefinition def = new PipelineDefinition();
        def.setCode(code);
        def.setName(name);
        def.setDescription(description);
        def.setVersion(1);
        def.setEnabled(true);
        pipelineDefinitionMapper.insert(def);
        log.info("流水线已创建: {} ({})", name, code);
        return def.getId();
    }

    private long createNode(long pipelineId, String code, String name, String handler, int sortOrder,
                            boolean requiredReview, boolean supportLoop, boolean parallel, boolean retryable, int timeoutSeconds) {
        PipelineNode node = new PipelineNode();
        node.setPipelineId(pipelineId);
        node.setCode(code);
        node.setName(name);
        node.setHandler(handler);
        node.setSortOrder(sortOrder);
        node.setRequiredReview(requiredReview);
        node.setSupportLoop(supportLoop);
        node.setParallel(parallel);
        node.setRetryable(retryable);
        node.setTimeoutSeconds(timeoutSeconds);
        pipelineNodeMapper.insert(node);
        return node.getId();
    }

    private void initVideoPipeline() {
        long pid = getOrCreatePipeline("video", "视频生产流水线", "完整视频：选题→脚本→分镜→素材→配音→剪辑→发布");
        List<PipelineNode> existing = pipelineNodeMapper.selectList(
                new LambdaQueryWrapper<PipelineNode>().eq(PipelineNode::getPipelineId, pid));
        if (!existing.isEmpty()) return;
        createNode(pid, "TOPIC", "选题", "topicService", 1, false, false, false, false, 0);
        createNode(pid, "SCRIPT", "脚本生成", "aiScriptService", 2, true, true, false, true, 300);
        createNode(pid, "STORYBOARD", "分镜生成", "aiPromptService", 3, false, false, false, true, 300);
        createNode(pid, "GENERATE", "素材生成", "aiVideoService", 4, false, false, true, true, 600);
        createNode(pid, "VOICE", "配音生成", "aiVoiceService", 5, false, false, false, true, 300);
        createNode(pid, "EDIT", "剪辑合成", "ffmpegService", 6, false, false, false, true, 600);
        createNode(pid, "REVIEW", "人工终审", "reviewService", 7, true, false, false, false, 0);
        createNode(pid, "PUBLISH", "发布", "publishService", 8, false, false, false, true, 60);
        log.info("视频流水线节点已初始化");
    }

    private void initTextPipeline() {
        long pid = getOrCreatePipeline("text", "纯文案流水线", "纯文案：选题→脚本→审核→发布");
        List<PipelineNode> existing = pipelineNodeMapper.selectList(
                new LambdaQueryWrapper<PipelineNode>().eq(PipelineNode::getPipelineId, pid));
        if (!existing.isEmpty()) return;
        createNode(pid, "TOPIC", "选题", "topicService", 1, false, false, false, false, 0);
        createNode(pid, "SCRIPT", "脚本生成", "aiScriptService", 2, true, true, false, true, 300);
        createNode(pid, "PUBLISH", "发布", "publishService", 3, false, false, false, true, 60);
        log.info("纯文案流水线节点已初始化");
    }

    private void initImagePipeline() {
        long pid = getOrCreatePipeline("image", "纯图片流水线", "纯图片：选题→脚本→分镜→图片生成→审核→发布");
        List<PipelineNode> existing = pipelineNodeMapper.selectList(
                new LambdaQueryWrapper<PipelineNode>().eq(PipelineNode::getPipelineId, pid));
        if (!existing.isEmpty()) return;
        createNode(pid, "TOPIC", "选题", "topicService", 1, false, false, false, false, 0);
        createNode(pid, "SCRIPT", "脚本生成", "aiScriptService", 2, true, true, false, true, 300);
        createNode(pid, "STORYBOARD", "分镜生成", "aiPromptService", 3, false, false, false, true, 300);
        createNode(pid, "GENERATE", "图片生成", "aiImageService", 4, false, false, true, true, 600);
        createNode(pid, "REVIEW", "人工终审", "reviewService", 5, true, false, false, false, 0);
        createNode(pid, "PUBLISH", "发布", "publishService", 6, false, false, false, true, 60);
        log.info("纯图片流水线节点已初始化");
    }

    private void initImageTextPipeline() {
        long pid = getOrCreatePipeline("image_text", "图文流水线", "图文：选题→脚本→分镜→图片生成→审核→发布");
        List<PipelineNode> existing = pipelineNodeMapper.selectList(
                new LambdaQueryWrapper<PipelineNode>().eq(PipelineNode::getPipelineId, pid));
        if (!existing.isEmpty()) return;
        createNode(pid, "TOPIC", "选题", "topicService", 1, false, false, false, false, 0);
        createNode(pid, "SCRIPT", "脚本生成", "aiScriptService", 2, true, true, false, true, 300);
        createNode(pid, "STORYBOARD", "分镜生成", "aiPromptService", 3, false, false, false, true, 300);
        createNode(pid, "GENERATE", "图片生成", "aiImageService", 4, false, false, true, true, 600);
        createNode(pid, "REVIEW", "人工终审", "reviewService", 5, true, false, false, false, 0);
        createNode(pid, "PUBLISH", "发布", "publishService", 6, false, false, false, true, 60);
        log.info("图文流水线节点已初始化");
    }
}