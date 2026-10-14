Feature: Device changes
  Scores, and a new game being set up, survive rotating, leaving the app, and Android closing it in the background.
  The layout follows the device's orientation, text size, language, and layout direction.

  @landscape
  Scenario: Rotating keeps the scores, including one still being typed
    Given a game "Rotate" with players "Calvin, Lindsay"
    And I enter "15" for Calvin in round 1
    When I tap the round 1 score for Lindsay
    And I type "7" on the calculator
    And I rotate the device to landscape
    Then the totals are:
      | Calvin | Lindsay |
      | 15     | 7       |
    When I rotate the device to portrait
    Then the totals are:
      | Calvin | Lindsay |
      | 15     | 7       |

  @landscape @visual @calculator
  Scenario: In landscape the keypad sits beside the scores
    Given a game "Landscape Keys" with players "Calvin, Lindsay, Bob"
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

  Scenario: The app comes back to the same game after Android closes it in the background
    Given a game "Process Death" with players "Calvin, Lindsay"
    And I enter "8" for Lindsay in round 1
    When Android closes the app in the background
    Then the title is "Process Death"
    And the totals are:
      | Calvin | Lindsay |
      | 0      | 8       |
    When I go back
    Then the title is "Games"

  @smoke
  Scenario: A new game being set up comes back after Android closes the app on the player picker
    Given I am on the game list
    When I tap "+"
    And I type "Comes Back" as the game name
    And I tap the row "Reverse Scoring"
    And I type "50" into "Target Score"
    And I tap "Edit"
    And I pick "Calvin" and "Bob"
    And Android closes the app in the background
    Then the title is "Pick Players"
    And "Calvin" and "Bob" are picked
    When I pick "Lindsay"
    And I go back
    Then the title is "New Game"
    And the game name field shows "Comes Back"
    And these settings are shown:
      | Reverse Scoring | on |
      | Target Score    | 50 |
    And the players are "Calvin, Bob, Lindsay"
    And Calvin's dealer radio button is checked
    When I tap "Save"
    Then the title is "Comes Back"
    And the totals read "Calvin 0, Bob 0, Lindsay 0"

  Scenario: A new game comes back after Android closes the app on New Game, with the name still being typed
    Given I am on the game list
    When I tap "+"
    And I type "Still Typing" as the game name
    And I tap the row "Show Round Notes"
    And I tap the game name
    And Android closes the app in the background
    Then the title is "New Game"
    And the game name field shows "Still Typing"
    And "Show Round Notes" is on
    When I type " Here" at the end of the game name
    And I tap "Edit"
    And I pick "Calvin"
    And I go back
    And I tap "Save"
    Then the title is "Still Typing Here"

  Scenario: A new game from a game comes back after Android closes the app
    Given a game "Copy Me" with players "Calvin, Lindsay"
    When I choose "New Game" from the game menu
    And I replace the game name with "Copy Me Again"
    And I type "3" into "Target Rounds"
    And Android closes the app in the background
    Then the title is "New Game"
    And the game name field shows "Copy Me Again"
    And these settings are shown:
      | Target Rounds | 3 |
    And the players are "Calvin, Lindsay"
    When I go back
    Then the title is "Copy Me"
    When I go back
    Then the title is "Games"

  Scenario: Closing the app from recents starts a new game over
    Given I am on the game list
    When I tap "+"
    And I type "Thrown Away" as the game name
    And I close and reopen the app
    Then the title is "Games"
    And I don't see "Thrown Away"

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
    And a game "Komma" with players "Calvin, Lindsay"
    When I tap the round 1 score for Calvin
    Then the calculator's decimal key shows ","
    When I type "2,5" on the calculator
    And I tap "="
    Then the round 1 score for Calvin shows "2,5"
    When I enter "10/4" for Lindsay in round 1
    Then the round 1 score for Lindsay shows "2,5"
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
  Scenario: Right-to-left layouts mirror the table
    Given "Force RTL layout direction" is on in Developer options
    And a game "RTL" with players "Calvin, Lindsay"
    Then Calvin's column is on the right
    When I tap the round 1 score for Calvin
    And I tap "->"
    Then the round 1 score for Lindsay is being edited

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
