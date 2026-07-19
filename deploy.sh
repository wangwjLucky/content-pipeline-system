#!/bin/bash
# =============================================================================
# 内容生产流水线系统 — 一键部署脚本
# 非技术人员可按顺序执行此脚本完成部署
# 使用方法：bash deploy.sh
# =============================================================================

set -e

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║     内容生产流水线系统 — 一键部署脚本                        ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""

# 检查 Docker
if ! command -v docker &> /dev/null; then
    echo "❌ 错误：未安装 Docker，请先安装 Docker >= 24.0"
    exit 1
fi
echo "✅ Docker: $(docker --version)"

# 检查 Docker Compose
if ! docker compose version &> /dev/null; then
    echo "❌ 错误：未安装 Docker Compose，请先安装 Docker Compose >= 2.20"
    exit 1
fi
echo "✅ Docker Compose: $(docker compose version)"

# 检查 .env 文件
if [ ! -f ".env" ]; then
    if [ -f ".env.example" ]; then
        echo ""
        echo "⚠️  未找到 .env 文件，正在从 .env.example 创建..."
        cp .env.example .env
        echo "⚠️  请编辑 .env 文件，修改以下配置项："
        echo "    - DB_PASSWORD（数据库密码）"
        echo "    - JWT_SECRET（JWT 密钥）"
        echo "    - CALLBACK_TOKEN（回调令牌）"
        echo "    - 至少一个 AI API Key"
        echo ""
        echo "编辑完成后重新运行此脚本。"
        exit 1
    else
        echo "❌ 错误：未找到 .env.example 文件"
        exit 1
    fi
fi
echo "✅ .env 文件已存在"

echo ""
echo "═══════════════════════════════════════════════════════════════"
echo "  步骤 1/4：构建所有服务镜像"
echo "═══════════════════════════════════════════════════════════════"
docker compose build
echo "✅ 镜像构建完成"

echo ""
echo "═══════════════════════════════════════════════════════════════"
echo "  步骤 2/4：启动所有服务"
echo "═══════════════════════════════════════════════════════════════"
docker compose up -d
echo "✅ 服务已启动"

echo ""
echo "═══════════════════════════════════════════════════════════════"
echo "  步骤 3/4：等待服务就绪（约 30 秒）"
echo "═══════════════════════════════════════════════════════════════"
sleep 30

echo ""
echo "═══════════════════════════════════════════════════════════════"
echo "  步骤 4/4：验证服务健康状态"
echo "═══════════════════════════════════════════════════════════════"

# 检查容器状态
echo ""
echo "▶ 容器运行状态："
docker compose ps -a

# 检查 Java 后台
echo -n "▶ Java 后台 (:8080) — "
JAVA_CHECK=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/v1/health 2>/dev/null || echo "000")
if [ "$JAVA_CHECK" = "200" ]; then echo "✅ HTTP $JAVA_CHECK"; else echo "❌ HTTP $JAVA_CHECK"; fi

# 检查 AI Gateway
echo -n "▶ AI Gateway (:8001) — "
GW_CHECK=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8001/health 2>/dev/null || echo "000")
if [ "$GW_CHECK" = "200" ]; then echo "✅ HTTP $GW_CHECK"; else echo "❌ HTTP $GW_CHECK"; fi

# 检查其他 Python 服务
for port in 8002 8003 8004 8005 8006 8007; do
    NAME=$(case $port in
        8002) echo -n "▶ Script Service  " ;;
        8003) echo -n "▶ Prompt Service  " ;;
        8004) echo -n "▶ Video Service   " ;;
        8005) echo -n "▶ Voice Service   " ;;
        8006) echo -n "▶ FFmpeg Service  " ;;
        8007) echo -n "▶ Image Service   " ;;
    esac)
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:$port/health 2>/dev/null || echo "000")
    echo -n " (:${port}) — "
    if [ "$STATUS" = "200" ]; then echo "✅ HTTP $STATUS"; else echo "❌ HTTP $STATUS"; fi
done

# 检查前端
echo -n "▶ 前端 (:80) — "
FE_CHECK=$(curl -s -o /dev/null -w "%{http_code}" http://localhost/ 2>/dev/null || echo "000")
if [ "$FE_CHECK" = "200" ]; then echo "✅ HTTP $FE_CHECK"; else echo "❌ HTTP $FE_CHECK"; fi

echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║  部署完成！                                                 ║"
echo "║                                                             ║"
echo "║  访问地址：                                                 ║"
echo "║    前端管理界面:  http://localhost                           ║"
echo "║    Java API:      http://localhost:8080                     ║"
echo "║    API 文档:      http://localhost:8080/doc.html            ║"
echo "║    RabbitMQ 管理: http://localhost:15672                    ║"
echo "║                                                             ║"
echo "║  如果所有服务均显示 ✅，则系统部署成功！                    ║"
echo "║  如果有服务显示 ❌，请查看文档 docs/operations.md 第 12 节  ║"
echo "╚══════════════════════════════════════════════════════════════╝"