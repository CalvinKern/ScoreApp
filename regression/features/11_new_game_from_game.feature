Feature: Starting the next game
  "New Game" (in the game menu, or on Player Standings) sets up a new game with the same name, players, and settings.
  The dealer is the player after the last round's dealer, so the deal keeps moving around the table.

  @smoke
  Scenario: New Game copies the game's name, players, and settings
    Given a game "Rematch" with players "Calvin, Lindsay, Bob" and settings:
      | Reverse Scoring  | on  |
      | Show Round Notes | on  |
      | Target Score     | 100 |
    And I enter "40" for Lindsay in round 1
    When I choose "New Game" from the game menu
    Then the title is "New Game"
    And the game name field shows "Rematch"
    And the players are "Calvin, Lindsay, Bob"
    And these settings are shown:
      | Reverse Scoring  | on  |
      | Show Round Notes | on  |
      | Target Score     | 100 |
    When I tap "Save"
    Then the title is "Rematch"
    And the totals are:
      | Calvin | Lindsay | Bob |
      | 0      | 0       | 0   |

  @smoke
  Scenario: The new game keeps both games, and back goes to the game list
    Given a game "Two Games" with players "Calvin, Lindsay"
    And I enter "40" for Lindsay in round 1
    When I choose "New Game" from the game menu
    And I tap "Save"
    And I go back
    Then the title is "Games"
    And there are two games named "Two Games"
    And the first one shows "1 Round" and today's date
    When I open the game "Two Games" in row 2
    Then the totals are:
      | Calvin | Lindsay |
      | 0      | 40      |

  Scenario: The next player after the last dealer deals first
    Given a game "Next Dealer" with players "Calvin, Lindsay, Bob" and settings:
      | Dealer       | on     |
      | First dealer | Calvin |
    And I tap "Add Round"
    When I choose "New Game" from the game menu
    Then "Bob" is marked "Dealer"
    When I tap "Save"
    Then Bob is the dealer

  Scenario: The dealer wraps around to the first player
    Given a game "Wrap Dealer" with players "Calvin, Lindsay" and settings:
      | Dealer       | on      |
      | First dealer | Lindsay |
    When I choose "New Game" from the game menu
    Then "Calvin" is marked "Dealer"

  Scenario: New Game from Player Standings
    Given a game "From Standings" with players "Calvin, Lindsay" and settings:
      | Target Score | 10 |
    And I enter "10" for Calvin in round 1
    When I tap "Finish Game"
    And I tap "Finish"
    And I tap "New Game"
    Then the title is "New Game"
    And the game name field shows "From Standings"

  Scenario: Changing the new game's setup leaves the old game alone
    Given a game "Original" with players "Calvin, Lindsay" and settings:
      | Target Score | 100 |
    When I choose "New Game" from the game menu
    And I replace the game name with "Original II"
    And I clear "Target Score"
    And I tap "Save"
    And I go back
    And I open the game "Original"
    And I choose "Edit" from the game menu
    Then these settings are shown:
      | Target Score | 100 |

  Scenario: Going back from the new game's setup returns to the game, without making a game
    Given a game "Not Yet" with players "Calvin"
    When I choose "New Game" from the game menu
    And I go back
    Then the title is "Not Yet"
    When I go back
    Then there is one game named "Not Yet"

  @seed
  Scenario: The next seeded Hearts game starts with the next dealer
    Given the seed data is loaded
    When I open the game "Hearts"
    And I tap "Finish Game"
    And I tap "Finish"
    And I tap "New Game"
    Then the players are "Calvin, Bob, Zed, Priya"
    And "Priya" is marked "Dealer"
    And these settings are shown:
      | Dealer          | on  |
      | Reverse Scoring | on  |
      | Target Score    | 100 |
