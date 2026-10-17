Feature: Editing a game in progress
  "Edit" in the game menu opens "Game Settings". Changes save on their own (shortly after each change, and when
  leaving), so going back keeps them, and the line under the title says whether they're saved. "Save" also saves and
  goes back. Players picked in the player picker save as they're picked. Removing a player removes their scores.

  @smoke
  Scenario: Game Settings shows the game as it is
    Given a game "Edit Me" with players "Calvin, Lindsay" and settings:
      | Reverse Scoring | on |
      | Target Score    | 50 |
    When I choose "Edit" from the game menu
    Then the title is "Game Settings"
    And the game name field shows "Edit Me"
    And the players are "Calvin, Lindsay"
    And Calvin's dealer radio button is checked, since Calvin deals the only round
    And these settings are shown:
      | Reverse Scoring | on |
      | Target Score    | 50 |

  @smoke
  Scenario: The first dealer can be changed while the game has one round
    Given a game "First Dealer" with players "Calvin, Lindsay, Bob"
    And I enter "5" for Lindsay in round 1
    When I choose "Edit" from the game menu
    And I tap the row "Bob"
    And I go back
    Then Bob is the dealer
    And the round 1 scores are:
      | Calvin | Lindsay | Bob |
      | 0      | 5       | 0   |
    When I tap "Add Round"
    Then Calvin is the dealer
    And Bob dealt round 1

  Scenario: The first dealer can't be changed once the game has a second round
    Given a game "Two Rounds" with players "Calvin, Lindsay"
    And I tap "Add Round"
    When I choose "Edit" from the game menu
    Then no player has a dealer radio button

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

  @smoke
  Scenario: Game Settings says whether its changes are saved
    Given a game "Save Status" with players "Calvin"
    When I choose "Edit" from the game menu
    Then the subtitle is "All changes saved"
    When I tap "Reverse Scoring"
    Then the subtitle is "Saving…"
    When I wait 2 seconds
    Then the subtitle is "All changes saved"
    When I clear the game name
    Then the subtitle is "Blank name isn't saved"
    When I type "Save Status" in the game name
    And I wait 2 seconds
    Then the subtitle is "All changes saved"

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
    Given a game "Add Player" with players "Calvin, Lindsay"
    And I play these rounds:
      | Calvin | Lindsay |
      | 10     | 20      |
      | 30     | 40      |
    And the players "Bob" exist
    When I choose "Edit" from the game menu
    And I tap "EDIT"
    And I pick "Bob"
    And I tap "Save"
    And I go back
    Then the round 1 scores are:
      | Calvin | Lindsay | Bob |
      | 10     | 20      | 0   |
    And the totals are:
      | Calvin | Lindsay | Bob |
      | 40     | 60      | 0   |

  Scenario: Players picked and left with back are saved to the game
    Given a game "Back Pick" with players "Calvin, Lindsay"
    And the players "Bob" exist
    When I choose "Edit" from the game menu
    And I tap "EDIT"
    And I pick "Bob"
    And I go back
    Then the players are "Calvin, Lindsay, Bob"
    When I go back
    Then the totals read "Calvin 0, Lindsay 0, Bob 0"

  Scenario: Players picked save while the player picker is still open
    Given a game "Pick Saves" with players "Calvin"
    And the players "Lindsay" exist
    When I choose "Edit" from the game menu
    And I tap "EDIT"
    Then the subtitle is "Changes save automatically"
    When I pick "Lindsay"
    And I wait 2 seconds
    And I force stop the app
    And I open the app
    And I open the game "Pick Saves"
    Then the totals read "Calvin 0, Lindsay 0"

  Scenario: Unpicking every player says the players aren't saved
    Given a game "Keep Both" with players "Calvin, Lindsay"
    When I choose "Edit" from the game menu
    And I tap "EDIT"
    # Picks save about 1.5 s after each change, so a slower second tap first saves Lindsay alone
    And I tap the rows "Calvin" and "Lindsay" quickly
    Then the subtitle is "Empty player list isn't saved"
    When I go back
    Then the subtitle is "Empty player list isn't saved"
    When I go back
    Then the totals read "Calvin 0, Lindsay 0"

  Scenario: Removing a player removes their scores
    Given a game "Remove Player" with players "Calvin, Lindsay, Bob"
    And I play these rounds:
      | Calvin | Lindsay | Bob |
      | 10     | 20      | 30  |
    When I choose "Edit" from the game menu
    And I tap "EDIT"
    And I tap the row "Lindsay"
    And I tap "Save"
    And I go back
    Then the totals are:
      | Calvin | Bob |
      | 10     | 30  |
    And Bob is leading

  Scenario: Removing every player keeps the players the game had
    Given a game "Keep Players" with players "Calvin, Lindsay"
    And I enter "5" for Lindsay in round 1
    When I choose "Edit" from the game menu
    And I tap "EDIT"
    # Picks save about 1.5 s after each change, so a slower second tap first saves Lindsay alone
    And I tap the rows "Calvin" and "Lindsay" quickly
    And I tap "Save"
    And I go back
    Then the totals are:
      | Calvin | Lindsay |
      | 0      | 5       |

  Scenario: Reordering players moves their columns and scores together
    Given a game "Reorder" with players "Calvin, Lindsay, Bob"
    And I play these rounds:
      | Calvin | Lindsay | Bob |
      | 1      | 2       | 3   |
    When I choose "Edit" from the game menu
    And I drag "Bob" above "Calvin"
    And I go back
    Then the round 1 scores are:
      | Bob | Calvin | Lindsay |
      | 3   | 1      | 2       |

  @visual
  Scenario: Turning on Reverse Scoring changes who is leading
    Given a game "Flip Scoring" with players "Calvin, Lindsay"
    And I play these rounds:
      | Calvin | Lindsay |
      | 10     | 20      |
    And Lindsay is leading
    When I choose "Edit" from the game menu
    And I tap the row "Reverse Scoring"
    And I go back
    Then Calvin is leading

  Scenario: A target that's already been passed finishes the game
    Given a game "Late Target" with players "Calvin, Lindsay"
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
    Given a game "Drop Dealer" with players "Calvin, Lindsay" and settings:
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
      | Team Red (Calvin & Lindsay) | Team Blue (Bob & alice) |
      | 3550                        | 2730                    |
