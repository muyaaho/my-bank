# Tiltfile for MyBank - Local Kubernetes Development
# Tilt provides hot-reload, live updates, and a beautiful web UI
#
# Usage:
#   tilt up                 # Start all services
#   tilt up auth-service    # Start specific service
#   tilt down               # Stop all services
#
# Web UI: http://localhost:10350

# Load Kubernetes configuration
load('ext://helm_resource', 'helm_resource', 'helm_repo')

# Configuration
config.define_bool("skip-infrastructure", False, "Skip infrastructure deployment")
config.define_bool("skip-frontend", False, "Skip frontend deployment")
config.define_string_list("only", [], "Only run specific services")

cfg = config.parse()

# Allow customization via tilt_config.json
settings = {
    "namespace": "mybank",
    "default_registry": "",  # Local Kind cluster, no registry needed
}

# Helper: Check if service should be enabled
def should_enable(service_name):
    if len(cfg.get("only", [])) > 0:
        return service_name in cfg.get("only", [])
    return True

# ============================================================================
# INFRASTRUCTURE LAYER
# ============================================================================

if not cfg.get("skip-infrastructure", False) and should_enable("infrastructure"):
    print("📦 Deploying Infrastructure...")

    # Deploy infrastructure using Helm
    helm_resource(
        'mybank-infrastructure',
        'helm/infrastructure',
        namespace=settings["namespace"],
        flags=[
            '--create-namespace',
            '--wait',
            '--timeout=10m',
        ],
        labels=['infrastructure'],
        resource_deps=[],
    )

# ============================================================================
# BACKEND SERVICES LAYER
# ============================================================================

# Service definitions
services = [
    {
        'name': 'api-gateway',
        'port': 8080,
        'path': './api-gateway',
        'dockerfile': './api-gateway/Dockerfile',
        'labels': ['backend', 'gateway'],
    },
    {
        'name': 'auth-service',
        'port': 8081,
        'path': './auth-service',
        'dockerfile': './auth-service/Dockerfile',
        'labels': ['backend', 'auth'],
    },
    {
        'name': 'user-service',
        'port': 8085,
        'path': './user-service',
        'dockerfile': './user-service/Dockerfile',
        'labels': ['backend', 'user'],
    },
    {
        'name': 'payment-service',
        'port': 8083,
        'path': './payment-service',
        'dockerfile': './payment-service/Dockerfile',
        'labels': ['backend', 'payment'],
    },
    {
        'name': 'investment-service',
        'port': 8084,
        'path': './investment-service',
        'dockerfile': './investment-service/Dockerfile',
        'labels': ['backend', 'investment'],
    },
]

# Build and deploy backend services
for service in services:
    if should_enable(service['name']):
        # Build Docker image with live reload
        docker_build(
            'mybank/' + service['name'],
            service['path'],
            dockerfile=service['dockerfile'],
            # Live update for faster development (optional, for Java hot reload)
            live_update=[
                sync(service['path'] + '/build/libs', '/app/libs'),
                run(
                    'echo "Restarting application..."',
                    trigger=[service['path'] + '/build/libs']
                ),
            ],
        )

# Deploy services using Helm
if should_enable("services"):
    helm_resource(
        'mybank-services',
        'helm/services',
        namespace=settings["namespace"],
        flags=[
            '--create-namespace',
            '--wait',
            '--timeout=10m',
        ],
        labels=['backend'],
        resource_deps=['mybank-infrastructure'],
        port_forwards=[
            '8080:8080',  # API Gateway
            '8081:8081',  # Auth Service
            '8083:8083',  # Payment
            '8084:8084',  # Investment
            '8085:8085',  # User
        ],
    )

# ============================================================================
# FRONTEND LAYER
# ============================================================================

if not cfg.get("skip-frontend", False) and should_enable("frontend"):
    print("🎨 Deploying Frontend...")

    # Build frontend Docker image with hot reload
    docker_build(
        'mybank/frontend',
        './app',
        dockerfile='./app/Dockerfile',
        # Live update for Next.js
        live_update=[
            sync('./app', '/app'),
            run(
                'cd /app && npm install',
                trigger=['./app/package.json']
            ),
        ],
    )

    # Deploy frontend using Helm
    helm_resource(
        'mybank-frontend',
        'helm/frontend',
        namespace=settings["namespace"],
        flags=[
            '--create-namespace',
            '--wait',
            '--timeout=5m',
        ],
        labels=['frontend'],
        resource_deps=['mybank-services'],
        port_forwards=['3000:3000'],
    )

# ============================================================================
# LOCAL RESOURCES (Run commands locally, not in K8s)
# ============================================================================

# Gradle build (run locally before Docker build)
local_resource(
    'gradle-build',
    'GRADLE_USER_HOME=.gradle ./gradlew clean build -x test --no-daemon --parallel',
    deps=[
        './api-gateway/src',
        './auth-service/src',
        './user-service/src',
        './payment-service/src',
        './investment-service/src',
    ],
    labels=['build'],
    allow_parallel=True,
)

# ============================================================================
# LINKS (Show in Tilt UI)
# ============================================================================

# Add helpful links to Tilt UI
k8s_resource(
    'mybank-services',
    links=[
        link('http://localhost:8080/actuator/health', 'API Gateway Health'),
        link('http://localhost:8081/actuator/health', 'Auth Service Health'),
    ],
)

k8s_resource(
    'mybank-frontend',
    links=[
        link('http://localhost:3000', 'Frontend (Local)'),
        link('https://app.mybank.com', 'Frontend (Istio)'),
    ],
)

# ============================================================================
# CUSTOM BUTTONS (Add actions to Tilt UI)
# ============================================================================

# Add button to run tests
local_resource(
    'run-tests',
    './gradlew test',
    auto_init=False,
    trigger_mode=TRIGGER_MODE_MANUAL,
    labels=['tests'],
)

# Add button to view logs
local_resource(
    'view-all-logs',
    'kubectl logs -f -l tier=backend -n mybank --all-containers --max-log-requests=10',
    auto_init=False,
    trigger_mode=TRIGGER_MODE_MANUAL,
    labels=['logs'],
)

# Add button to restart all services
local_resource(
    'restart-all',
    'kubectl rollout restart deployment -n mybank',
    auto_init=False,
    trigger_mode=TRIGGER_MODE_MANUAL,
    labels=['ops'],
)

# ============================================================================
# WELCOME MESSAGE
# ============================================================================

print("""
╔══════════════════════════════════════════════════════════════╗
║                                                              ║
║                   MyBank - Tilt Development                  ║
║                                                              ║
║  🚀 Hot reload enabled for all services                      ║
║  📊 Web UI: http://localhost:10350                          ║
║  🔍 Logs: Click on any service in the UI                    ║
║                                                              ║
╚══════════════════════════════════════════════════════════════╝

Available commands:
  tilt up                    Start all services
  tilt up auth-service       Start specific service
  tilt down                  Stop all services
  tilt args -- --only=auth-service  Run only auth-service

Accessing services:
  Frontend:    http://localhost:3000
  API Gateway: http://localhost:8080
  Auth:        http://localhost:8081

Press Ctrl+C to stop
""")
