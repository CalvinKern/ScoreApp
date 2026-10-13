@fresh
Feature: First launch
  A new install asks once whether usage data can be shared, then shows an empty game list.

  @smoke
  Scenario: The usage data question is asked on first launch
    Given a fresh install
    Then I see "Share usage data?"
    And I see "Allow"
    And I see "Don't allow"

  Scenario: The usage data question can't be skipped
    Given a fresh install
    When I go back
    Then I see "Share usage data?"
    When I tap outside the dialog
    Then I see "Share usage data?"

  Scenario Outline: The answer sets both privacy switches and isn't asked again
    Given a fresh install
    When I tap "<answer>"
    Then I don't see "Share usage data?"
    When I close and reopen the app
    Then I don't see "Share usage data?"
    When I tap the Settings icon
    Then "Share Usage Data" is <state>
    And "Send Crash Reports" is <state>

    Examples:
      | answer      | state |
      | Allow       | on    |
      | Don't allow | off   |

  @smoke
  Scenario: The game list starts empty
    Given a fresh install with the usage data question answered
    Then the title is "Games"
    And I see "No games created"
    And I see "Start by clicking the + button down below to create a new game!"

  Scenario: The empty game list's picture also starts a game
    Given a fresh install with the usage data question answered
    When I tap the picture above "No games created"
    Then the title is "New Game"

  Scenario: The player list starts empty
    Given a fresh install with the usage data question answered
    When I tap the + button
    And I tap "Edit"
    Then the title is "Pick Players"
    And I see "No players added"
    And I see "Start by clicking the + button down below to add a new player!"
