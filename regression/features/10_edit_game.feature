Feature: Editing a game in progress
  "Edit" in the game menu opens "Game Settings". Changes save on their own (shortly after each change, and when
  leaving), so going back keeps them. "Save" also saves and goes back. Removing a player removes their scores.

  @smoke
  Scenario: Game Settings shows the game as it is
    Given a game "Edit Me" with players "Calvin, Kim" and settings:
      | Reverse Scoring | on |
      | Target Score    | 50 |
    When I choose "Edit" from the game menu
    Then the title is "Game Settings"
    And the game name field shows "Edit Me"
    And the players are "Calvin, Kim"
    And no player has a dealer radio button
    And these settings are shown:
      | Reverse Scoring | on |
      | Target Score    | 50 |

  @smoke
  Scenario: Renaming a game saves when going back
    Given a game "Old Name" with players "Calvin"
    When I choose "Edit" from the game menu
    And I replace the game name with "New Name"
    And I go back
    Then the title is "New Name"
    When I go back
    Then I see "New Name"
    And I don't see "Old Name"

  Scenario: Save saves and goes back to the game
    Given a game "Save Button" with players "Calvin"
    When I choose "Edit" from the game menu
    And I replace the game name with "Saved Name"
    And I tap "Save"
    Then the title is "Saved Name"

  Scenario: Changes save on their own, even if the app is closed from Game Settings
    Given a game "Auto Save" with players "Calvin"
    When I choose "Edit" from the game menu
    And I replace the game name with "Auto Saved"
    And I wait 2 seconds
    And I force stop the app
    And I open the app
    Then I see "Auto Saved"

  Scenario: A blank name keeps the old name
    Given a game "Keep Name" with players "Calvin"
    When I choose "Edit" from the game menu
    And I clear the game name
    And I go back
    Then the title is "Keep Name"

  Scenario: Save with a blank name asks for a name
    Given a game "Need Name" with players "Calvin"
    When I choose "Edit" from the game menu
    And I clear the game name
    And I tap "Save"
    Then I see "Please enter a name for the game"
    And the title is "Game Settings"

  @smoke
  Scenario: Adding a player gives them 0 in every round
    Given a game "Add Player" with players "Calvin, Kim"
    And I play these rounds:
      | Calvin | Kim |
      | 10     | 20  |
      | 30     | 40  |
    And the players "Bob" exist
    When I choose "Edit" from the game menu
    And I tap "EDIT"
    And I pick "Bob"
    And I tap "Save"
    And I go back
    Then the round 1 scores are:
      | Calvin | Kim | Bob |
      | 10     | 20  | 0   |
    And the totals are:
      | Calvin | Kim | Bob |
      | 40     | 60  | 0   |

  Scenario: Removing a player removes their scores
    Given a game "Remove Player" with players "Calvin, Kim, Bob"
    And I play these rounds:
      | Calvin | Kim | Bob |
      | 10     | 20  | 30  |
    When I choose "Edit" from the game menu
    And I tap "EDIT"
    And I tap the row "Kim"
    And I tap "Save"
    And I go back
    Then the totals are:
      | Calvin | Bob |
      | 10     | 30  |
    And Bob is leading

  Scenario: Removing every player keeps the players the game had
    Given a game "Keep Players" with players "Calvin, Kim"
    And I enter "5" for Kim in round 1
    When I choose "Edit" from the game menu
    And I tap "EDIT"
    And I tap the row "Calvin"
    And I tap the row "Kim"
    And I tap "Save"
    And I go back
    Then the totals are:
      | Calvin | Kim |
      | 0      | 5   |

  Scenario: Reordering players moves their columns and scores together
    Given a game "Reorder" with players "Calvin, Kim, Bob"
    And I play these rounds:
      | Calvin | Kim | Bob |
      | 1      | 2   | 3   |
    When I choose "Edit" from the game menu
    And I drag "Bob" above "Calvin"
    And I go back
    Then the round 1 scores are:
      | Bob | Calvin | Kim |
      | 3   | 1      | 2   |

  @visual
  Scenario: Turning on Reverse Scoring changes who is leading
    Given a game "Flip Scoring" with players "Calvin, Kim"
    And I play these rounds:
      | Calvin | Kim |
      | 10     | 20  |
    And Kim is leading
    When I choose "Edit" from the game menu
    And I tap the row "Reverse Scoring"
    And I go back
    Then Calvin is leading

  Scenario: A target that's already been passed finishes the game
    Given a game "Late Target" with players "Calvin, Kim"
    And I enter "80" for Calvin in round 1
    When I choose "Edit" from the game menu
    And I type "50" into "Target Score"
    And I go back
    Then the button after the rounds is "Finish Game"

  Scenario: Clearing the target brings Add Round back
    Given a game "Clear Target" with players "Calvin" and settings:
      | Target Rounds | 1 |
    And the button after the rounds is "Finish Game"
    When I choose "Edit" from the game menu
    And I clear "Target Rounds"
    And I go back
    Then the button after the rounds is "Add Round"

  @system-keyboard
  Scenario: Turning off the calculator switches to the system keyboard
    Given a game "Switch Keyboard" with players "Calvin"
    When I choose "Edit" from the game menu
    And I tap the row "Calculator Keyboard"
    And I go back
    And I tap the round 1 score for Calvin
    Then the system number keyboard shows
    And the calculator keyboard doesn't show

  @visual
  Scenario: Turning off the dealer hides the dealer borders
    Given a game "Drop Dealer" with players "Calvin, Kim" and settings:
      | Dealer | on |
    And Calvin is the dealer
    When I choose "Edit" from the game menu
    And I tap the row "Dealer"
    And I go back
    Then nobody has the dealer border

  @seed
  Scenario: Editing a seeded game keeps its scores
    Given the seed data is loaded
    When I open the game "Hand and Foot"
    And I choose "Edit" from the game menu
    And I replace the game name with "Hand and Foot (edited)"
    And I go back
    Then the title is "Hand and Foot (edited)"
    And the totals are:
      | Team Red (Calvin & Kim) | Team Blue (Bob & alice) |
      | 3550                    | 2730                    |
