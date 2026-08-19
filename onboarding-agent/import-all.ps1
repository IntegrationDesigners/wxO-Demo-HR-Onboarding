# =============================================================================
# import-all.ps1
# Imports all tools, toolkits, and agents for the HR Onboarding demo into
# watsonx Orchestrate.
#
# Usage:
#   cd onboarding-agent
#   .\import-all.ps1
#
# Prerequisites:
#   - wxo CLI authenticated: `orchestrate env activate <env>`
# =============================================================================

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

function Info  { param([string]$msg) Write-Host "[INFO]  $msg" }
function Ok    { param([string]$msg) Write-Host "[OK]    $msg" -ForegroundColor Green }
function Fail  { param([string]$msg) Write-Host "[ERROR] $msg" -ForegroundColor Red; exit 1 }

# ---------------------------------------------------------------------------
# 1. OpenAPI tool - onboarding-app REST API
# ---------------------------------------------------------------------------
Info "Importing OpenAPI tool: onboarding_api"
orchestrate tools import -k openapi -f "$ScriptDir\tools\onboarding_api.local.yaml"
Ok "OpenAPI tool imported: onboarding_api"

# ---------------------------------------------------------------------------
# 2. Flow tools
# ---------------------------------------------------------------------------
Info "Importing flow tool: employee_onboarding"
orchestrate tools import -k flow -f "$ScriptDir\tools\employee_onboarding.json"
Ok "Flow tool imported: employee_onboarding"

Info "Importing flow tool: onboarding_cv_converter"
orchestrate tools import -k flow -f "$ScriptDir\tools\onboarding_cv_converter.json"
Ok "Flow tool imported: onboarding_cv_converter"

Info "Importing flow tool: process_cv"
orchestrate tools import -k flow -f "$ScriptDir\tools\process_cv.json"
Ok "Flow tool imported: process_cv"

Info "Importing flow tool: process_identity_card"
orchestrate tools import -k flow -f "$ScriptDir\tools\process_identity_card.json"
Ok "Flow tool imported: process_identity_card"

# ---------------------------------------------------------------------------
# 3. Slack flow tool (dummy — no Slack connection required)
# ---------------------------------------------------------------------------
Info "Importing DUMMY Slack flow tool"
orchestrate tools import -k flow -f "$ScriptDir\tools\onboarding_send_slack_message.dummy.json"
Ok "Dummy Slack flow tool imported: onboarding_send_slack_message"

# ---------------------------------------------------------------------------
# 4. Python tool - populate_word_template
# ---------------------------------------------------------------------------
Info "Importing Python tool: populate_word_template"
orchestrate tools import -k python `
    -f "$ScriptDir\tools\populate_word_template\populate_word_template.py" `
    --requirements-file "$ScriptDir\tools\populate_word_template\requirements.txt"
Ok "Python tool imported: populate_word_template"

# ---------------------------------------------------------------------------
# 5. Knowledge bases
# ---------------------------------------------------------------------------
Info "Importing knowledge base: Onboarding_Car_Policy"
orchestrate knowledge-bases import -f "$ScriptDir\knowledge-bases\car_policy.yaml"
Ok "Knowledge base imported: Onboarding_Car_Policy"

# ---------------------------------------------------------------------------
# 6. Agents (import sub-agents before the orchestrating agent)
# ---------------------------------------------------------------------------
Info "Importing agent: Onboarding_Car_Agent"
orchestrate agents import -f "$ScriptDir\agents\Onboarding_Car_Agent.yaml"
Ok "Agent imported: Onboarding_Car_Agent"

Info "Importing agent: Onboarding_CV_Agent"
orchestrate agents import -f "$ScriptDir\agents\Onboarding_CV_Agent.yaml"
Ok "Agent imported: Onboarding_CV_Agent"

Info "Importing agent: Onboarding_Employee_Agent"
orchestrate agents import -f "$ScriptDir\agents\Onboarding_Employee_Agent.yaml"
Ok "Agent imported: Onboarding_Employee_Agent"

Info "Importing agent: Onboarding_Orchestrator_Agent"
orchestrate agents import -f "$ScriptDir\agents\Onboarding_Orchestrator_Agent.yaml"
Ok "Agent imported: Onboarding_Orchestrator_Agent"

# ---------------------------------------------------------------------------
Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host " All tools, toolkits, and agents imported successfully."     -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
