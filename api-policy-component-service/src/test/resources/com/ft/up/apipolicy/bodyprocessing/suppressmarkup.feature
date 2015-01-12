@BodyProcessing
Feature: Body processing rules

This is an overview of how the various configuration rules work.

  Scenario Outline:
    Given the unprocessed markup <before>
    When it is transformed
    Then the mark up becomes <after>

  Examples:
   | before                                                                  | after             |
   | <pull-quote><text>Some Text</text><source>source1</source></pull-quote> |                   |
   | <a href="http://www.somelink.com"></a>                                  |                   |
