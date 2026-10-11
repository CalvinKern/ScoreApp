Feature: Device changes
  Scores survive rotating, leaving the app, and the system closing it. The layout follows the device's orientation,
  text size, language, and layout direction.

  @landscape
  Scenario: Rotating keeps the scores, including one still being typed
    Given a game "Rotate" with players "Calvin, Kim"
    And I enter "15" for Calvin in round 1
    When I tap the round 1 score for Kim
    And I type "7" on the calculator
    And I rotate the device to landscape
    Then the totals are:
      | Calvin | Kim |
      | 15     | 7   |
    When I rotate the device to portrait
    Then the totals are:
      | Calvin | Kim |
      | 15     | 7   |

  @landscape @visual @calculator
  Scenario: In landscape the keypad sits beside the scores
    Given a game "Landscape Keys" with players "Calvin, Kim, Bob"
    And I rotate the device to landscape
    When I tap the round 1 score for Calvin
    Then the calculator keyboard is beside the score table, not below it
    And no score or name is hidden behind the keypad
    When I tap "="
    Then the score table uses the whole width again

  @landscape
  Scenario: Every screen works in landscape
    Given the seed data is loaded
    And I rotate the device to landscape
    Then the game list shows
    When I open the game "Hearts"
    Then the totals are:
      | Calvin | Bob | Zed | Priya |
      | 57     | 101 | 51  | 51    |
    When I choose "Show Graph" from the game menu
    Then the graph and podium show
    When I go back
    And I choose "Edit" from the game menu
    Then the title is "Game Settings"

  @smoke
  Scenario: A score typed just before leaving the app is kept
    Given a game "Leave Mid Edit" with players "Calvin"
    When I tap the round 1 score for Calvin
    And I type "42" on the calculator
    And I press Home
    And I force stop the app
    And I open the app
    And I open the game "Leave Mid Edit"
    Then the totals are:
      | Calvin |
      | 42     |

  Scenario: Coming back to a game doesn't open the keyboard
    Given a game "No Pop Up" with players "Calvin"
    When I press Home
    And I return to the app from recents
    Then the title is "No Pop Up"
    And no keyboard shows

  Scenario: The app comes back to the same game after the system closes it in the background
    Given a game "Process Death" with players "Calvin, Kim"
    And I enter "8" for Kim in round 1
    When I press Home
    And the system kills the app in the background (adb shell am kill com.seakernel.scorepad.debug)
    And I return to the app from recents
    Then the title is "Process Death"
    And the totals are:
      | Calvin | Kim |
      | 0      | 8   |
    When I go back
    Then the title is "Games"

  @visual
  Scenario Outline: Swiping back slides back to the previous screen (<screen>)
    Given I am on <screen>
    When I start a back swipe from the edge and hold it
    Then the previous screen slides in under the gesture
    When I let go
    Then <previous> shows

    Examples:
      | screen                     | previous            |
      | a game                     | the game list       |
      | a game's graph             | the game            |
      | a game's Game Settings     | the game            |
      | Settings                   | the game list       |
      | Pick Players for a new game | New Game           |

  @smoke
  Scenario Outline: Back from each screen (<screen>)
    Given I am on <screen>
    When I go back
    Then <previous> shows

    Examples:
      | screen                      | previous      |
      | a game                      | the game list |
      | a game's graph              | the game      |
      | a game's Game Settings      | the game      |
      | New Game                    | the game list |
      | Pick Players for a new game | New Game      |
      | Settings                    | the game list |

  @visual
  Scenario: The largest text size widens the columns instead of squeezing them
    Given the device's font size is the largest
    And the seed data is loaded
    When I open the game "Five Crowns"
    Then every player's name and score can be read
    And the score table scrolls sideways
    And the player names stay above their scores while scrolling

  @locale
  Scenario: Languages that write decimals with a comma
    Given the device language is Deutsch (Deutschland)
    And a game "Komma" with players "Calvin, Kim"
    When I tap the round 1 score for Calvin
    Then the calculator's decimal key shows ","
    When I type "2,5" on the calculator
    And I tap "="
    Then the round 1 score for Calvin shows "2,5"
    When I enter "10/4" for Kim in round 1
    Then the round 1 score for Kim shows "2,5"
    When I tap the round 1 score for Calvin
    Then the score being edited shows "2,5"

  @locale @seed
  Scenario: Dates and decimals follow the device language
    Given the device language is Deutsch (Deutschland)
    And the seed data is loaded
    Then the first game's date is "05.10.2026"
    When I open the game "Trivia Night"
    Then the totals show "24,83" for Calvin

  @locale @system-keyboard
  Scenario: The system keyboard accepts the language's decimal separator
    Given the device language is Deutsch (Deutschland)
    And a game "Komma Tasten" with players "Calvin" and settings:
      | Calculator Keyboard | off |
    When I tap the round 1 score for Calvin
    And I type "7,5"
    And I hide the keyboard
    Then the round 1 score for Calvin shows "7,5"

  @locale
  Scenario: Typed calculations stay readable after changing the language
    Given a game "Language Swap" with players "Calvin"
    And I enter "2.5×2" for Calvin in round 1
    When I change the device language to Deutsch (Deutschland)
    And I open the game "Language Swap"
    And I tap the round 1 score for Calvin
    Then the score being edited shows "2,5×2"
    And the totals show "5" for Calvin

  @visual
  Scenario: Right-to-left layouts mirror the table and the arrows
    Given "Force RTL layout direction" is on in Developer options
    And a game "RTL" with players "Calvin, Kim"
    Then Calvin's column is on the right
    When I tap the round 1 score for Kim
    And I tap "<-"
    Then the round 1 score for Calvin is being edited

  @visual
  Scenario Outline: Light and dark mode (<mode>)
    Given the device is in <mode> mode
    And the seed data is loaded
    When I open each screen: the game list, "Hearts", its graph, Player Standings, Game Settings, Pick Players, and Settings
    Then all text is readable against its background
    And the leading, dealer, and error colors are easy to tell apart

    Examples:
      | mode  |
      | light |
      | dark  |
