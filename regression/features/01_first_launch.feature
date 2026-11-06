@fresh
Feature: First launch
  A new install opens on an empty game list without asking anything. Once a game has been created, a card at the top
  of the game list asks whether usage data can be shared, until it's answered.

  @smoke
  Scenario: The game list starts empty
    Given a fresh install
    Then the title is "Games"
    And I see "No games created"
    And I see "Start by tapping the + button down below to create a new game!"
    And I don't see "Share usage data?"

  Scenario: The empty game list's picture also starts a game
    Given a fresh install
    When I tap the picture above "No games created"
    Then the title is "New Game"

  Scenario: The player list starts empty
    Given a fresh install
    When I tap the + button
    And I tap "Edit"
    Then the title is "Pick Players"
    And I see "No players added"
    And I see "Start by tapping the + button down below to add a new player!"

  Scenario: Nothing is shared before the usage data question is answered
    Given a fresh install
    When I tap the Settings icon
    Then "Share Usage Data" is off
    And "Send Crash Reports" is off

  @smoke
  Scenario: The usage data question shows above the games once a game is created
    Given a fresh install
    And a game "First Game" with players "Calvin"
    When I go back
    Then I see "Share usage data?" above the game "First Game"
    And I see "Allow"
    And I see "Don't allow"

  Scenario: The usage data question can be left unanswered
    Given a fresh install
    And a game "First Game" with players "Calvin"
    And I go back
    When I open the game "First Game"
    And I go back
    Then I see "Share usage data?"
    When I close and reopen the app
    Then I see "Share usage data?"
    When I tap the Settings icon
    Then "Share Usage Data" is off
    And "Send Crash Reports" is off

  Scenario Outline: The answer sets both privacy switches and isn't asked again
    Given a fresh install
    And a game "First Game" with players "Calvin"
    And I go back
    When I tap "<answer>"
    Then I don't see "Share usage data?"
    And I see "First Game"
    When I close and reopen the app
    Then I don't see "Share usage data?"
    When I tap the Settings icon
    Then "Share Usage Data" is <state>
    And "Send Crash Reports" is <state>

    Examples:
      | answer      | state |
      | Allow       | on    |
      | Don't allow | off   |

  Scenario: Choosing in Settings first answers the usage data question
    Given a fresh install
    When I tap the Settings icon
    And I tap the row "Send Crash Reports"
    And I go back
    And a game "First Game" with players "Calvin"
    And I go back
    Then I don't see "Share usage data?"

  Scenario: The usage data question goes away with the last game
    Given a fresh install
    And a game "Only Game" with players "Calvin"
    And I go back
    When I long press the game "Only Game"
    And I tap "Delete"
    Then I see "No games created"
    And I don't see "Share usage data?"
