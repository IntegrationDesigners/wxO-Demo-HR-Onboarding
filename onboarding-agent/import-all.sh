#!/usr/bin/env bash
# =============================================================================
# import-all.sh
# Imports all tools, toolkits, and agents for the HR Onboarding demo into
# watsonx Orchestrate.
#
# Usage:
#   cd onboarding-agent
#   ./import-all.sh
#
# Prerequisites:
#   - wxo CLI authenticated: `orchestrate env activate <env>`
# =============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------
info()  { echo "[INFO]  $*"; }
ok()    { echo "[OK]    $*"; }
error() { echo "[ERROR] $*" >&2; }

# ---------------------------------------------------------------------------
# 1. Toolkit — onboarding-app MCP server
# ---------------------------------------------------------------------------
info "Removing existing toolkit (if present): onboarding-app"
orchestrate toolkits remove -n onboarding-app 2>/dev/null || true

info "Importing toolkit: onboarding-app"
orchestrate toolkits import -f "$SCRIPT_DIR/toolkits/onboarding-app.local.yaml"
ok "Toolkit imported: onboarding-app"

# ---------------------------------------------------------------------------
# 2. Flow tools
# ---------------------------------------------------------------------------
info "Importing flow tool: employee_onboarding"
orchestrate tools import -k flow -f "$SCRIPT_DIR/tools/employee_onboarding.json"
ok "Flow tool imported: employee_onboarding"

info "Importing flow tool: onboarding_cv_converter"
orchestrate tools import -k flow -f "$SCRIPT_DIR/tools/onboarding_cv_converter.json"
ok "Flow tool imported: onboarding_cv_converter"

info "Importing flow tool: process_cv"
orchestrate tools import -k flow -f "$SCRIPT_DIR/tools/process_cv.json"
ok "Flow tool imported: process_cv"

info "Importing flow tool: process_identity_card"
orchestrate tools import -k flow -f "$SCRIPT_DIR/tools/process_identity_card.json"
ok "Flow tool imported: process_identity_card"

# ---------------------------------------------------------------------------
# 3. Slack flow tool (dummy — no Slack connection required)
# ---------------------------------------------------------------------------
info "Importing DUMMY Slack flow tool"
orchestrate tools import -k flow -f "$SCRIPT_DIR/tools/onboarding_send_slack_message.dummy.json"
ok "Dummy Slack flow tool imported: onboarding_send_slack_message"

# ---------------------------------------------------------------------------
# 4. Python tool — populate_word_template
# ---------------------------------------------------------------------------
info "Importing Python tool: populate_word_template"
orchestrate tools import -k python \
  -f "$SCRIPT_DIR/tools/populate_word_template/populate_word_template.py" \
  --requirements-file "$SCRIPT_DIR/tools/populate_word_template/requirements.txt"
ok "Python tool imported: populate_word_template"

# ---------------------------------------------------------------------------
# 5. Agents (import sub-agents before the orchestrating agent)
# ---------------------------------------------------------------------------
info "Importing agent: Onboarding_Car_Agent"
orchestrate agents import -f "$SCRIPT_DIR/agents/Onboarding_Car_Agent.yaml"
ok "Agent imported: Onboarding_Car_Agent"

info "Importing agent: Onboarding_CV_Agent"
orchestrate agents import -f "$SCRIPT_DIR/agents/Onboarding_CV_Agent.yaml"
ok "Agent imported: Onboarding_CV_Agent"

info "Importing agent: Onboarding_Employee_Agent"
orchestrate agents import -f "$SCRIPT_DIR/agents/Onboarding_Employee_Agent.yaml"
ok "Agent imported: Onboarding_Employee_Agent"

# ---------------------------------------------------------------------------
echo ""
echo "============================================================"
echo " All tools, toolkits, and agents imported successfully."
echo "============================================================"
