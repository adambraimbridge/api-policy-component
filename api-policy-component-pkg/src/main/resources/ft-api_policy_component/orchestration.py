# Prepare Environment
import sys
sys.path.append("/root/provisioner/python/deploymodules")
from functions import check_root
from Orchestration import ActionStep
check_root()

# Define our actions
stop_puppet = ActionStep('Stop Puppet', parallel=True, node_identifier_method = 'nodegroup', node_identifier_qualifier="", action="stop_puppet_service")
kick_puppet_agents = ActionStep('Kick Puppet agents', parallel=False, node_identifier_method='nodegroup', node_identifier_qualifier="", action="run_puppet_agent")
start_puppet = ActionStep('Start Puppet', parallel=True, node_identifier_method = 'nodegroup', node_identifier_qualifier = "", action = "start_puppet_service")

# Build our list of steps
steps = []
steps.append(stop_puppet)
steps.append(kick_puppet_agents)
steps.append(start_puppet)

# Run loop. (The options variable is passed in by deploy.py)
def run(options):
    for step in steps:
        step.execute(options)