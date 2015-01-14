@BodyProcessing
Feature: Body processing rules

This is an overview of how the various configuration rules work.

  Scenario Outline:
    Given the unprocessed markup <before>
    When it is transformed
    Then the mark up is removed

  Examples:
   | before                                                                  |
   | <pull-quote><text>Some Text</text><source>source1</source></pull-quote> |
   | <a href="http://www.somelink.com"></a>                                  |
 #  | <blockquote class="twitter-tweet" lang="en"><p>Brilliant as always RT <a href="https://twitter.com/DeborahJaneOrr">@DeborahJaneOrr</a>: Will Cornick's 20-year sentence for the killing of Ann Maguire defies logic <a href="http://t.co/93DaG1pAaN">http://t.co/93DaG1pAaN</a></p>&mdash; Graham Linehan (@Glinner) <a href="https://twitter.com/Glinner/status/529965370577526784">November 5, 2014</a></blockquote> |