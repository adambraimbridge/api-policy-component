# Prepare Environment

# Standard imports
import sys
sys.path.append("/root/provisioner/python/deploymodules")
import logging
from functions import Functions
from Orchestration import ActionStep
logger = logging.getLogger('provisioner')
f = Functions(logger)
f.check_root()

# Import sufficient PDS/library code to load nodegroup information
from PDS import PDS
from deploymodules.functions import Functions
from deploymodules.routines import Routines
from deploymodules.PDS import PDS
functions = Functions(logger)
pds = PDS(False, functions=functions)

# Run loop. (The options variable is passed in by deploy.py)
def run(options):
    # Define our actions
    stop_puppet_agents = ActionStep('stop_puppet', parallel=True, action='stop_puppet_service',
                           node_identifier_method='nodegroup', node_identifier_qualifier='')

    start_puppet_agents = ActionStep('start_puppet', parallel=True, action='start_puppet_service',
                           node_identifier_method='nodegroup', node_identifier_qualifier='')

    kick_puppet_agents = ActionStep('kick_puppet_agents', parallel=False, action='run_puppet_agent',
                           node_identifier_method='nodegroup', node_identifier_qualifier='')

    # Build our list of steps
    deploy_steps = []
    deploy_steps.append(stop_puppet_agents)
    deploy_steps.append(kick_puppet_agents)
    deploy_steps.append(start_puppet_agents)

    print "Performing Deployment steps ..."
    for step in deploy_steps:
        step.execute(options)
