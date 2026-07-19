# =============================================================================
# 内容生产流水线系统 — 一键部署脚本（Windows PowerShell 版）
# 非技术人员可按顺序执行此脚本完成部署
# 使用方法：在 PowerShell 中运行 .\deploy.ps1
# =============================================================================

Write-Host "╔" -NoNewline
Write-Host "══════════════════════════════════════════════════════════════" -NoNewline
Write-Host "╗"
Write-Host "║     内容生产流水线系统 — 一键部署脚本 (PowerShell)        ║"
Write-Host "╚" -NoNewline
Write-Host "══════════════════════════════════════════════════════════════" -NoNewline
Write-Host "╝"
Write-Host ""

# 检查 Docker
try {
    $dockerVersion = docker --version
    Write-Host "✅ Docker: $dockerVersion"
} catch {
    Write-Host "❌ 错误：未安装 Docker，请先安装 Docker Desktop"
    exit 1
}

# 检查 Docker Compose
try {
    $composeVersion = docker compose version
    Write-Host "✅ Docker Compose: $composeVersion"
} catch {
    Write-Host "❌ 错误：Docker Compose 不可用"
    exit 1
}

# 检查 .env 文件
if (-not (Test-Path ".env")) {
    if (Test-Path ".env.example") {
        Write-Host ""
        Write-Host "⚠️  未找到 .env 文件，正在从 .env.example 创建..."
        Copy-Item ".env.example" ".env"
        Write-Host "⚠️  请编辑 .env 文件，修改以下配置项："
        Write-Host "    - DB_PASSWORD（数据库密码）"
        Write-Host "    - JWT_SECRET（JWT 密钥）"
        Write-Host "    - CALLBACK_TOKEN（回调令牌）"
        Write-Host "    - 至少一个 AI API Key"
        Write-Host ""
        Write-Host "编辑完成后重新运行此脚本。"
        exit 1
    } else {
        Write-Host "❌ 错误：未找到 .env.example 文件"
        exit 1
    }
}
Write-Host "✅ .env 文件已存在"

Write-Host ""
Write-Host "═══════════════════════════════════════════════════════════════"
Write-Host "  步骤 1/4：构建所有服务镜像"
Write-Host "═══════════════════════════════════════════════════════════════"
docker compose build
Write-Host "✅ 镜像构建完成"

Write-Host ""
Write-Host "═══════════════════════════════════════════════════════════════"
Write-Host "  步骤 2/4：启动所有服务"
Write-Host "═══════════════════════════════════════════════════════════════"
docker compose up -d
Write-Host "✅ 服务已启动"

Write-Host ""
Write-Host "═══════════════════════════════════════════════════════════════"
Write-Host "  步骤 3/4：等待服务就绪（约 30 秒）"
Write-Host "═══════════════════════════════════════════════════════════════"
Start-Sleep -Seconds 30

Write-Host ""
Write-Host "═══════════════════════════════════════════════════════════════"
Write-Host "  步骤 4/4：验证服务健康状态"
Write-Host "═══════════════════════════════════════════════════════════════"

# 检查容器状态
Write-Host ""
Write-Host "▶ 容器运行状态："
docker compose ps -a

# 检查 Java 后台
Write-Host -NoNewline "▶ Java 后台 (:8080) — "
try {
    $javaCheck = (Invoke-WebRequest -Uri "http://localhost:8080/api/v1/health" -TimeoutSec 5).StatusCode
    if ($javaCheck -eq 200) { Write-Host "✅ HTTP $javaCheck" } else { Write-Host "❌ HTTP $javaCheck" }
} catch { Write-Host "❌ 无法访问" }

# 检查 AI Gateway
Write-Host -NoNewline "▶ AI Gateway (:8001) — "
try {
    $gwCheck = (Invoke-WebRequest -Uri "http://localhost:8001/health" -TimeoutSec 5).StatusCode
    if ($gwCheck -eq 200) { Write-Host "✅ HTTP $gwCheck" } else { Write-Host "❌ HTTP $gwCheck" }
} catch { Write-Host "❌ 无法访问" }

# 检查其他 Python 服务
$ports = @{8002="Script Service"; 8003="Prompt Service"; 8004="Video Service"; 8005="Voice Service"; 8006="FFmpeg Service"; 8007="Image Service"}
foreach ($port in $ports.Keys) {
    Write-Host -NoNewline "▶ $($ports[$port]) (:${port}) — "
    try {
        $status = (Invoke-WebRequest -Uri "http://localhost:$port/health" -TimeoutSec 5).StatusCode
        if ($status -eq 200) { Write-Host "✅ HTTP $status" } else { Write-Host "❌ HTTP $status" }
    } catch { Write-Host "❌ 无法访问" }
}

# 检查前端
Write-Host -NoNewline "▶ 前端 (:80) — "
try {
    $feCheck = (Invoke-WebRequest -Uri "http://localhost/" -TimeoutSec 5).StatusCode
    if ($feCheck -eq 200) { Write-Host "✅ HTTP $feCheck" } else { Write-Host "❌ HTTP $feCheck" }
} catch { Write-Host "❌ 无法访问" }

Write-Host ""
Write-Host "╔" -NoNewline
Write-Host "══════════════════════════════════════════════════════════════" -NoNewline
Write-Host "╗"
Write-Host "║  部署完成！                                              ║"
Write-Host "║                                                          ║"
Write-Host "║  访问地址：                                              ║"
Write-Host "║    前端管理界面:  http://localhost                        ║"
Write-Host "║    Java API:      http://localhost:8080                  ║"
Write-Host "║    API 文档:      http://localhost:8080/doc.html         ║"
Write-Host "║    RabbitMQ 管理: http://localhost:15672                 ║"
Write-Host "║                                                          ║"
Write-Host "║  如果所有服务均显示 ✅，则系统部署成功！                 ║"
Write-Host "║  如果有服务显示 ❌，请查看 docs/operations.md 第 12 节   ║"
Write-Host "╚" -NoNewline
Write-Host "══════════════════════════════════════════════════════════════" -NoNewline
Write-Host "╝"