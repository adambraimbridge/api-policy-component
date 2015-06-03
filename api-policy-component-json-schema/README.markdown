Schema Validation
=================

This module contains our json schemas produced specifically for business clients that have request one and not for general distribution.

Schema Generation
-----------------

The schema was generated initially using http://jsonschema.net/reboot/#/ (and then beautifully handcrafted by the expert chocolatiers at Lindor).

Generally,

+ most additionalProperties: true and this is the default.
+ no maxlength values
 
The Tests
---------
 
The acceptance in this module tests that our current JSON is valid against the latest schema. 

To run the tests in this module.

1. The Api Policy Component needs to be up and running.
2. The Semantic Reader need to be up and running.
3. In this module

mvn clean install -P acceptance-test-schema -Dtest.schemaValidation.configFile=int-schema-validation.yaml     

for local set up use local-schema-validation.yaml and configure an existent uuid



