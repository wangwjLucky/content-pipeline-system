-- 用户角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(50) NOT NULL UNIQUE,
    code        VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(200),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(50) NOT NULL UNIQUE,
    password    VARCHAR(200) NOT NULL,
    nickname    VARCHAR(50),
    role_id     BIGINT REFERENCES sys_role(id),
    status      VARCHAR(10) DEFAULT 'ENABLED',
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 选题表
CREATE TABLE IF NOT EXISTS topic (
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(200) NOT NULL,
    source      VARCHAR(50),
    source_url  VARCHAR(500),
    hot_score   INT DEFAULT 0,
    is_auto     BOOLEAN DEFAULT FALSE,
    status      VARCHAR(20) DEFAULT 'PENDING',
    created_by  BIGINT REFERENCES sys_user(id),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 任务表
CREATE TABLE IF NOT EXISTS task (
    id            BIGSERIAL PRIMARY KEY,
    topic_id      BIGINT REFERENCES topic(id),
    title         VARCHAR(200) NOT NULL,
    script_id     BIGINT,
    content_type  VARCHAR(20) NOT NULL DEFAULT 'video',
    status        VARCHAR(20) NOT NULL DEFAULT 'WAIT',
    progress      INT DEFAULT 0,
    error_message TEXT,
    fail_reason   VARCHAR(50),
    retry_count   INT DEFAULT 0,
    priority      INT DEFAULT 0,
    version       INT DEFAULT 0,
    created_by    BIGINT REFERENCES sys_user(id),
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_task_status ON task(status);
CREATE INDEX IF NOT EXISTS idx_task_created_at ON task(created_at);

-- 脚本表
CREATE TABLE IF NOT EXISTS script (
    id          BIGSERIAL PRIMARY KEY,
    topic_id    BIGINT REFERENCES topic(id),
    task_id     BIGINT REFERENCES task(id),
    title       VARCHAR(200),
    content     TEXT,
    subtitle    TEXT,
    prompt_template_id BIGINT,
    version     INT DEFAULT 1,
    status      VARCHAR(20) DEFAULT 'PENDING',
    created_by  BIGINT REFERENCES sys_user(id),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 分镜表
CREATE TABLE IF NOT EXISTS storyboard (
    id          BIGSERIAL PRIMARY KEY,
    task_id     BIGINT NOT NULL REFERENCES task(id),
    sequence    INT NOT NULL,
    duration    INT DEFAULT 5,
    scene_type  VARCHAR(50),
    character   VARCHAR(200),
    action      TEXT,
    environment VARCHAR(200),
    camera      VARCHAR(100),
    lighting    VARCHAR(100),
    style       VARCHAR(100),
    ai_prompt   TEXT,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_storyboard_task ON storyboard(task_id);

-- 素材表
CREATE TABLE IF NOT EXISTS material (
    id              BIGSERIAL PRIMARY KEY,
    task_id         BIGINT REFERENCES task(id),
    storyboard_id   BIGINT REFERENCES storyboard(id),
    type            VARCHAR(20) NOT NULL,
    model           VARCHAR(50),
    url             VARCHAR(500),
    bucket          VARCHAR(100),
    file_size       BIGINT,
    file_md5        VARCHAR(64),
    width           INT,
    height          INT,
    duration        INT,
    prompt          TEXT,
    status          VARCHAR(20) DEFAULT 'PENDING',
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_material_task ON material(task_id);

-- 配音表
CREATE TABLE IF NOT EXISTS voice (
    id          BIGSERIAL PRIMARY KEY,
    task_id     BIGINT NOT NULL REFERENCES task(id),
    voice_type  VARCHAR(50),
    voice_url   VARCHAR(500),
    speed       DECIMAL(3,2) DEFAULT 1.05,
    duration    INT,
    status      VARCHAR(20) DEFAULT 'PENDING',
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_voice_task_id ON voice(task_id);

-- 发布日志表
CREATE TABLE IF NOT EXISTS publish_log (
    id                BIGSERIAL PRIMARY KEY,
    task_id           BIGINT NOT NULL REFERENCES task(id),
    platform          VARCHAR(20) NOT NULL,
    account_id        BIGINT,
    title             VARCHAR(200),
    cover_url         VARCHAR(500),
    tags              VARCHAR(500),
    scheduled_at      TIMESTAMP,
    published_at      TIMESTAMP,
    status            VARCHAR(20) DEFAULT 'PENDING',
    platform_video_id VARCHAR(100),
    error_message     TEXT,
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_publish_log_platform ON publish_log(platform, status);

-- 任务事件表（状态机时间线）
CREATE TABLE IF NOT EXISTS task_event (
    id          BIGSERIAL PRIMARY KEY,
    task_id     BIGINT NOT NULL REFERENCES task(id),
    event_type  VARCHAR(50),
    from_status VARCHAR(20),
    to_status   VARCHAR(20) NOT NULL,
    operator    VARCHAR(50),
    comment     TEXT,
    payload     TEXT,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_task_event_task ON task_event(task_id);

-- AI 模型配置表
CREATE TABLE IF NOT EXISTS ai_model_config (
    id                  BIGSERIAL PRIMARY KEY,
    model_name          VARCHAR(100) NOT NULL,
    provider            VARCHAR(50) NOT NULL,
    api_key_encrypted   VARCHAR(500),
    endpoint            VARCHAR(500),
    model_type          VARCHAR(50),
    default_params      TEXT,
    rate_limit          TEXT,
    enabled             BOOLEAN DEFAULT TRUE,
    weight              INT DEFAULT 10,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 平台账号表
CREATE TABLE IF NOT EXISTS platform_account (
    id                BIGSERIAL PRIMARY KEY,
    platform          VARCHAR(20) NOT NULL,
    account_name      VARCHAR(100) NOT NULL,
    cookies_encrypted TEXT,
    status            VARCHAR(10) DEFAULT 'ENABLED',
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Prompt 模板表
CREATE TABLE IF NOT EXISTS prompt_template (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    type        VARCHAR(20) NOT NULL,
    content     TEXT NOT NULL,
    variables   VARCHAR(500),
    enabled     BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- AI 任务日志表
CREATE TABLE IF NOT EXISTS ai_task_log (
    id              BIGSERIAL PRIMARY KEY,
    task_id         BIGINT,
    run_id          BIGINT,
    node_run_id     BIGINT,
    provider        VARCHAR(50) NOT NULL,
    model           VARCHAR(100),
    prompt          TEXT,
    response        TEXT,
    prompt_tokens   INT,
    completion_tokens INT,
    latency_ms      INT,
    cost            DECIMAL(10,6),
    retry_count     INT DEFAULT 0,
    status          VARCHAR(20) DEFAULT 'PENDING',
    error_message   TEXT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_ai_task_log_task ON ai_task_log(task_id);
CREATE INDEX IF NOT EXISTS idx_ai_task_log_provider ON ai_task_log(provider);

-- 流水线定义表
CREATE TABLE IF NOT EXISTS pipeline_definition (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    code            VARCHAR(50) NOT NULL UNIQUE,
    description     VARCHAR(500),
    version         INT DEFAULT 1,
    enabled         BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 流水线节点表
CREATE TABLE IF NOT EXISTS pipeline_node (
    id              BIGSERIAL PRIMARY KEY,
    pipeline_id     BIGINT NOT NULL REFERENCES pipeline_definition(id),
    name            VARCHAR(100) NOT NULL,
    code            VARCHAR(50) NOT NULL,
    handler         VARCHAR(200) NOT NULL,
    sort_order      INT NOT NULL,
    required_review BOOLEAN DEFAULT FALSE,
    support_loop    BOOLEAN DEFAULT FALSE,
    parallel        BOOLEAN DEFAULT FALSE,
    retryable       BOOLEAN DEFAULT TRUE,
    timeout_seconds INT DEFAULT 300,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 流水线节点关系表（DAG）
CREATE TABLE IF NOT EXISTS pipeline_node_relation (
    id              BIGSERIAL PRIMARY KEY,
    pipeline_id     BIGINT NOT NULL REFERENCES pipeline_definition(id),
    from_node_id    BIGINT NOT NULL REFERENCES pipeline_node(id),
    to_node_id      BIGINT NOT NULL REFERENCES pipeline_node(id),
    condition_expr  VARCHAR(500),
    sort_order      INT NOT NULL
);

-- 版本 DAG 表
CREATE TABLE IF NOT EXISTS version_graph (
    id              BIGSERIAL PRIMARY KEY,
    entity_type     VARCHAR(50) NOT NULL,
    entity_id       BIGINT NOT NULL,
    version         INT NOT NULL,
    parent_version_id BIGINT REFERENCES version_graph(id),
    snapshot        TEXT,
    created_by      BIGINT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_version_graph_entity ON version_graph(entity_type, entity_id);

-- 任务执行表
CREATE TABLE IF NOT EXISTS task_run (
    id              BIGSERIAL PRIMARY KEY,
    task_id         BIGINT NOT NULL REFERENCES task(id),
    run_number      INT NOT NULL,
    status          VARCHAR(20) DEFAULT 'RUNNING',
    triggered_by    VARCHAR(50),
    started_at      TIMESTAMP,
    finished_at     TIMESTAMP,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_task_run_task ON task_run(task_id);

-- 节点执行表
CREATE TABLE IF NOT EXISTS task_node_run (
    id              BIGSERIAL PRIMARY KEY,
    run_id          BIGINT NOT NULL REFERENCES task_run(id),
    node_code       VARCHAR(50) NOT NULL,
    status          VARCHAR(20) DEFAULT 'PENDING',
    handler         VARCHAR(200),
    retry_count     INT DEFAULT 0,
    max_retries     INT DEFAULT 3,
    input_snapshot  TEXT,
    output_snapshot TEXT,
    started_at      TIMESTAMP,
    finished_at     TIMESTAMP,
    error_message   TEXT,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_task_node_run_run ON task_node_run(run_id);

-- 插入默认角色
INSERT INTO sys_role (name, code, description) VALUES ('管理员', 'ADMIN', '系统管理员');
INSERT INTO sys_role (name, code, description) VALUES ('运营', 'OPERATOR', '内容运营');
INSERT INTO sys_role (name, code, description) VALUES ('编辑', 'EDITOR', '视频编辑');

-- ========== 字段注释 ==========
COMMENT ON COLUMN sys_role.name IS '角色名称';
COMMENT ON COLUMN sys_role.code IS '角色编码';
COMMENT ON COLUMN sys_role.description IS '角色描述';

COMMENT ON COLUMN sys_user.username IS '用户名';
COMMENT ON COLUMN sys_user.password IS '密码（BCrypt 加密）';
COMMENT ON COLUMN sys_user.nickname IS '昵称';
COMMENT ON COLUMN sys_user.role_id IS '角色 ID';
COMMENT ON COLUMN sys_user.status IS '状态: ENABLED/DISABLED';

COMMENT ON COLUMN topic.title IS '选题标题';
COMMENT ON COLUMN topic.source IS '来源: MANUAL/AUTO/HOT';
COMMENT ON COLUMN topic.source_url IS '来源链接';
COMMENT ON COLUMN topic.hot_score IS '热度分值';
COMMENT ON COLUMN topic.is_auto IS '是否自动抓取';
COMMENT ON COLUMN topic.status IS '状态: PENDING/PROCESSING/COMPLETED';
COMMENT ON COLUMN topic.created_by IS '创建人';

COMMENT ON COLUMN task.topic_id IS '关联选题 ID';
COMMENT ON COLUMN task.title IS '任务标题';
COMMENT ON COLUMN task.script_id IS '关联脚本 ID';
COMMENT ON COLUMN task.content_type IS '内容产出方式: video/text/image/image_text';
COMMENT ON COLUMN task.status IS '任务状态: WAIT/SCRIPTING/SCRIPT_REVIEW/STORYBOARD/GENERATING/VOICEOVER/EDITING/REVIEW/READY/PUBLISHED/CANCELLED/ERROR';
COMMENT ON COLUMN task.progress IS '进度百分比 0-100';
COMMENT ON COLUMN task.error_message IS '错误信息';
COMMENT ON COLUMN task.fail_reason IS '失败原因归类: SCRIPT_FAILED/MATERIAL_FAILED/VOICE_FAILED/EDIT_FAILED/PUBLISH_FAILED';
COMMENT ON COLUMN task.retry_count IS '重试次数';
COMMENT ON COLUMN task.priority IS '优先级 0-100';
COMMENT ON COLUMN task.version IS '乐观锁版本号';
COMMENT ON COLUMN task.created_by IS '创建人';

COMMENT ON COLUMN script.topic_id IS '关联选题 ID';
COMMENT ON COLUMN script.task_id IS '关联任务 ID';
COMMENT ON COLUMN script.title IS '脚本标题';
COMMENT ON COLUMN script.content IS '脚本正文';
COMMENT ON COLUMN script.subtitle IS '字幕文本';
COMMENT ON COLUMN script.prompt_template_id IS '使用的 Prompt 模板 ID';
COMMENT ON COLUMN script.version IS '版本号';
COMMENT ON COLUMN script.status IS '状态: PENDING_REVIEW/APPROVED/REJECTED';
COMMENT ON COLUMN script.created_by IS '创建人';

COMMENT ON COLUMN storyboard.task_id IS '关联任务 ID';
COMMENT ON COLUMN storyboard.sequence IS '镜头序号';
COMMENT ON COLUMN storyboard.duration IS '镜头时长（秒）';
COMMENT ON COLUMN storyboard.scene_type IS '场景类型';
COMMENT ON COLUMN storyboard.character IS '角色描述';
COMMENT ON COLUMN storyboard.action IS '动作描述';
COMMENT ON COLUMN storyboard.environment IS '环境描述';
COMMENT ON COLUMN storyboard.camera IS '运镜方式';
COMMENT ON COLUMN storyboard.lighting IS '灯光描述';
COMMENT ON COLUMN storyboard.style IS '风格描述';
COMMENT ON COLUMN storyboard.ai_prompt IS 'AI 生成提示词';

COMMENT ON COLUMN material.task_id IS '关联任务 ID';
COMMENT ON COLUMN material.storyboard_id IS '关联分镜 ID';
COMMENT ON COLUMN material.type IS '素材类型: video/image/audio';
COMMENT ON COLUMN material.model IS '使用的 AI 模型';
COMMENT ON COLUMN material.url IS '素材文件地址';
COMMENT ON COLUMN material.bucket IS 'MinIO 存储桶';
COMMENT ON COLUMN material.file_size IS '文件大小（字节）';
COMMENT ON COLUMN material.file_md5 IS '文件 MD5 指纹';
COMMENT ON COLUMN material.width IS '图片/视频宽度';
COMMENT ON COLUMN material.height IS '图片/视频高度';
COMMENT ON COLUMN material.duration IS '视频/音频时长（秒）';
COMMENT ON COLUMN material.prompt IS '生成提示词';
COMMENT ON COLUMN material.status IS '状态: PENDING/SUCCESS/FAILURE';

COMMENT ON COLUMN voice.task_id IS '关联任务 ID';
COMMENT ON COLUMN voice.voice_type IS '配音类型: doubao/azure/aliyun';
COMMENT ON COLUMN voice.voice_url IS '配音文件地址';
COMMENT ON COLUMN voice.speed IS '语速倍数';
COMMENT ON COLUMN voice.duration IS '时长（秒）';
COMMENT ON COLUMN voice.status IS '状态: PENDING/SUCCESS/FAILURE';

COMMENT ON COLUMN publish_log.task_id IS '关联任务 ID';
COMMENT ON COLUMN publish_log.platform IS '发布平台: douyin/kuaishou/xiaohongshu';
COMMENT ON COLUMN publish_log.account_id IS '发布账号 ID';
COMMENT ON COLUMN publish_log.title IS '发布标题';
COMMENT ON COLUMN publish_log.cover_url IS '封面图地址';
COMMENT ON COLUMN publish_log.tags IS '标签（逗号分隔）';
COMMENT ON COLUMN publish_log.scheduled_at IS '定时发布时间';
COMMENT ON COLUMN publish_log.published_at IS '实际发布时间';
COMMENT ON COLUMN publish_log.status IS '状态: PENDING/PUBLISHED/FAILURE';
COMMENT ON COLUMN publish_log.platform_video_id IS '平台视频 ID';
COMMENT ON COLUMN publish_log.error_message IS '错误信息';

COMMENT ON COLUMN task_event.task_id IS '关联任务 ID';
COMMENT ON COLUMN task_event.event_type IS '事件类型: TASK_CREATED/SCRIPT_GENERATED/SCRIPT_REVIEWED/STORYBOARD_READY/MATERIAL_READY/VOICE_COMPLETED/EDIT_COMPLETED/TASK_CANCELLED/TASK_RETRIED/TASK_ERRORED';
COMMENT ON COLUMN task_event.from_status IS '来源状态';
COMMENT ON COLUMN task_event.to_status IS '目标状态';
COMMENT ON COLUMN task_event.operator IS '操作人';
COMMENT ON COLUMN task_event.comment IS '备注说明';
COMMENT ON COLUMN task_event.payload IS '事件数据（JSON）';

COMMENT ON COLUMN ai_model_config.model_name IS '模型名称';
COMMENT ON COLUMN ai_model_config.provider IS '供应商: openai/kling/doubao';
COMMENT ON COLUMN ai_model_config.api_key_encrypted IS 'API Key（加密存储）';
COMMENT ON COLUMN ai_model_config.endpoint IS 'API 端点';
COMMENT ON COLUMN ai_model_config.model_type IS '模型类型: text/image/video/audio';
COMMENT ON COLUMN ai_model_config.default_params IS '默认参数（JSON）';
COMMENT ON COLUMN ai_model_config.rate_limit IS '限流配置（JSON）';
COMMENT ON COLUMN ai_model_config.enabled IS '是否启用';
COMMENT ON COLUMN ai_model_config.weight IS '权重（负载均衡）';

COMMENT ON COLUMN platform_account.platform IS '平台: douyin/kuaishou/xiaohongshu';
COMMENT ON COLUMN platform_account.account_name IS '账号名称';
COMMENT ON COLUMN platform_account.cookies_encrypted IS 'Cookies（加密存储）';
COMMENT ON COLUMN platform_account.status IS '状态: ENABLED/DISABLED';

COMMENT ON COLUMN prompt_template.name IS '模板名称';
COMMENT ON COLUMN prompt_template.type IS '模板类型: script/prompt/image/voice';
COMMENT ON COLUMN prompt_template.content IS '模板内容';
COMMENT ON COLUMN prompt_template.variables IS '变量列表（逗号分隔）';
COMMENT ON COLUMN prompt_template.enabled IS '是否启用';

COMMENT ON COLUMN ai_task_log.task_id IS '关联任务 ID';
COMMENT ON COLUMN ai_task_log.run_id IS '关联执行 ID';
COMMENT ON COLUMN ai_task_log.node_run_id IS '关联节点执行 ID';
COMMENT ON COLUMN ai_task_log.provider IS 'AI 供应商: claude/gpt/deepseek/keling/veo/doubao';
COMMENT ON COLUMN ai_task_log.model IS '实际使用的模型';
COMMENT ON COLUMN ai_task_log.prompt IS '请求 Prompt';
COMMENT ON COLUMN ai_task_log.response IS '响应内容';
COMMENT ON COLUMN ai_task_log.prompt_tokens IS '输入 Token 数';
COMMENT ON COLUMN ai_task_log.completion_tokens IS '输出 Token 数';
COMMENT ON COLUMN ai_task_log.latency_ms IS '响应耗时（毫秒）';
COMMENT ON COLUMN ai_task_log.cost IS '估算费用';
COMMENT ON COLUMN ai_task_log.retry_count IS '重试次数';
COMMENT ON COLUMN ai_task_log.status IS '状态: PENDING/SUCCESS/FAILED';
COMMENT ON COLUMN ai_task_log.error_message IS '错误信息';

COMMENT ON COLUMN pipeline_definition.name IS '流水线名称';
COMMENT ON COLUMN pipeline_definition.code IS '编码: video/text/image/image_text';
COMMENT ON COLUMN pipeline_definition.description IS '描述';
COMMENT ON COLUMN pipeline_definition.version IS '版本号';
COMMENT ON COLUMN pipeline_definition.enabled IS '是否启用';

COMMENT ON COLUMN pipeline_node.pipeline_id IS '所属流水线 ID';
COMMENT ON COLUMN pipeline_node.name IS '节点名称';
COMMENT ON COLUMN pipeline_node.code IS '节点编码: TOPIC/SCRIPT/STORYBOARD/GENERATE/VOICE/EDIT/REVIEW/PUBLISH';
COMMENT ON COLUMN pipeline_node.handler IS '处理器 bean 名称';
COMMENT ON COLUMN pipeline_node.sort_order IS '排序';
COMMENT ON COLUMN pipeline_node.required_review IS '是否需要人工审核';
COMMENT ON COLUMN pipeline_node.support_loop IS '是否支持迭代循环';
COMMENT ON COLUMN pipeline_node.parallel IS '是否可并行';
COMMENT ON COLUMN pipeline_node.retryable IS '是否支持重试';
COMMENT ON COLUMN pipeline_node.timeout_seconds IS '超时时间（秒）';

COMMENT ON COLUMN pipeline_node_relation.pipeline_id IS '所属流水线 ID';
COMMENT ON COLUMN pipeline_node_relation.from_node_id IS '上游节点 ID';
COMMENT ON COLUMN pipeline_node_relation.to_node_id IS '下游节点 ID';
COMMENT ON COLUMN pipeline_node_relation.condition_expr IS '条件表达式';
COMMENT ON COLUMN pipeline_node_relation.sort_order IS '排序';

COMMENT ON COLUMN version_graph.entity_type IS '实体类型: SCRIPT/STORYBOARD/MATERIAL/VIDEO';
COMMENT ON COLUMN version_graph.entity_id IS '实体 ID';
COMMENT ON COLUMN version_graph.version IS '版本号';
COMMENT ON COLUMN version_graph.parent_version_id IS '父版本 ID（形成 DAG）';
COMMENT ON COLUMN version_graph.snapshot IS '版本快照（JSON）';
COMMENT ON COLUMN version_graph.created_by IS '创建人';

COMMENT ON COLUMN task_run.task_id IS '关联任务 ID';
COMMENT ON COLUMN task_run.run_number IS '执行序号';
COMMENT ON COLUMN task_run.status IS '状态: RUNNING/SUCCESS/FAILED';
COMMENT ON COLUMN task_run.triggered_by IS '触发方式: AUTO/MANUAL/RETRY';
COMMENT ON COLUMN task_run.started_at IS '开始时间';
COMMENT ON COLUMN task_run.finished_at IS '完成时间';

COMMENT ON COLUMN task_node_run.run_id IS '关联执行 ID';
COMMENT ON COLUMN task_node_run.node_code IS '节点编码: SCRIPT/STORYBOARD/GENERATE/VOICE/EDIT/REVIEW/PUBLISH';
COMMENT ON COLUMN task_node_run.status IS '状态: PENDING/RUNNING/SUCCESS/FAILED';
COMMENT ON COLUMN task_node_run.handler IS '处理器';
COMMENT ON COLUMN task_node_run.retry_count IS '重试次数';
COMMENT ON COLUMN task_node_run.max_retries IS '最大重试次数';
COMMENT ON COLUMN task_node_run.input_snapshot IS '输入参数快照';
COMMENT ON COLUMN task_node_run.output_snapshot IS '输出结果快照';
COMMENT ON COLUMN task_node_run.started_at IS '开始时间';
COMMENT ON COLUMN task_node_run.finished_at IS '完成时间';
COMMENT ON COLUMN task_node_run.error_message IS '错误信息';