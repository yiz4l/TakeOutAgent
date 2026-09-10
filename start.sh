#!/usr/bin/env bash
set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
AGENT_DIR="$ROOT_DIR/healthy_agent"
JAVA_DIR="$ROOT_DIR/healthy"
FRONTEND_DIR="$ROOT_DIR/frontend"
PIDS=()

cleanup() {
  trap - INT TERM EXIT
  if ((${#PIDS[@]})); then
    # exec below makes each PID the actual service process, so signals reach it.
    kill -TERM "${PIDS[@]}" 2>/dev/null || true
    wait "${PIDS[@]}" 2>/dev/null || true
    kill -KILL "${PIDS[@]}" 2>/dev/null || true
  fi
}
trap cleanup INT TERM EXIT

require_command() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "错误：找不到命令 $1" >&2
    exit 1
  }
}

require_command docker
require_command curl
require_command python
require_command npm

if [[ ! -f "$AGENT_DIR/.env" ]]; then
  echo "错误：缺少 $AGENT_DIR/.env，请先从 .env.example 创建并配置它。" >&2
  exit 1
fi

# Export the agent dotenv values so Spring Boot receives the same token.
set -a
# shellcheck disable=SC1091
source "$AGENT_DIR/.env"
set +a

if [[ -z "${AGENT_SERVICE_TOKEN:-}" ]]; then
  echo "错误：AGENT_SERVICE_TOKEN 不能为空。" >&2
  exit 1
fi

echo "启动 Qdrant..."
(cd "$AGENT_DIR" && docker compose up -d qdrant)

echo "启动 Java 后端..."
(cd "$JAVA_DIR" && exec ./mvnw spring-boot:run) &
PIDS+=("$!")

wait_for_url() {
  local url="$1"
  local name="$2"
  for _ in {1..60}; do
    if curl -sS -o /dev/null "$url" 2>/dev/null; then
      echo "$name 已就绪。"
      return 0
    fi
    sleep 1
  done
  echo "错误：等待 $name 超时，请检查对应终端输出。" >&2
  exit 1
}

wait_for_url "http://localhost:6333/readyz" "Qdrant"
wait_for_url "http://localhost:8080" "Java 后端"

echo "启动 Python Agent..."
(cd "$AGENT_DIR" && exec python main.py) &
PIDS+=("$!")

echo "启动前端..."
(cd "$FRONTEND_DIR" && exec npm run dev -- --host 0.0.0.0) &
PIDS+=("$!")

echo "全部服务已启动。按 Ctrl+C 停止 Java、Agent 和前端；Qdrant 数据会保留。"
wait -n "${PIDS[@]}"
exit $?
