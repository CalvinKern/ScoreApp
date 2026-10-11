Feature: Game list
  Every game, most recently played first, with its players, rounds, and when it was last played.

  @seed @smoke
  Scenario: Games are listed by when they were last played
    Given the seed data is loaded
    Then the first three games are "Yahtzee", "Phase 10", and "Hand and Foot"
    And the last game is "Old Hearts Save"
    And there are 30 games

  @seed
  Scenario Outline: Each row shows the game's details
    Given the seed data is loaded
    Then row <row> is "<game>"
    And it shows "<players>", "<rounds>", and "Last Played: <date>"

    Examples:
      | row | game                      | players     | rounds    | date         |
      | 1   | Yahtzee                   | 3 Players   | 1 Round   | Oct 5, 2026  |
      | 2   | Phase 10                  | 5 Players   | 8 Rounds  | Oct 5, 2026  |
      | 3   | Hand and Foot             | 2 Players   | 4 Rounds  | Oct 4, 2026  |
      | 4   | Uno                       | 10 Players  | 5 Rounds  | Oct 3, 2026  |
      | 5   | Trivia Night              | 13 Players  | 5 Rounds  | Oct 2, 2026  |
      | 6   | Gin Rummy                 | 2 Players   | 6 Rounds  | Oct 1, 2026  |
      | 7   | Wingspan                  | 4 Players   | 6 Rounds  | Sep 28, 2026 |
      | 8   | Ticket to Ride            | 5 Players   | 3 Rounds  | Sep 27, 2026 |
      | 9   | Hearts                    | 4 Players   | 8 Rounds  | Sep 26, 2026 |
      | 10  | Skull King                | 5 Players   | 6 Rounds  | Sep 24, 2026 |
      | 11  | Alhambra                  | 4 Players   | 3 Rounds  | Sep 20, 2026 |
      | 12  | Mille Bornes              | 3 Players   | 4 Rounds  | Sep 19, 2026 |
      | 13  | Planted                   | 3 Players   | 3 Rounds  | Sep 14, 2026 |
      | 14  | Agricola                  | 4 Players   | 1 Round   | Sep 13, 2026 |
      | 15  | Agricola Solo             | 1 Player    | 1 Round   | Sep 12, 2026 |
      | 16  | Golf                      | 4 Players   | 9 Rounds  | Sep 7, 2026  |
      | 17  | Five Crowns               | 6 Players   | 6 Rounds  | Sep 6, 2026  |
      | 18  | Spades                    | 2 Players   | 6 Rounds  | Aug 30, 2026 |
      | 19  | Rummy 500                 | 3 Players   | 6 Rounds  | Aug 23, 2026 |
      | 20  | Scrabble                  | 3 Players   | 25 Rounds | Aug 16, 2026 |
      | 21  | Mexican Train             | 5 Players   | 13 Rounds | Aug 9, 2026  |
      | 22  | Darts 301                 | 2 Players   | 7 Rounds  | Aug 2, 2026  |
      | 23  | Catan                     | 4 Players   | 1 Round   | Jul 26, 2026 |
      | 24  | Cribbage                  | 2 Players   | 12 Rounds | Jul 19, 2026 |
      | 25  | Euchre                    | 2 Players   | 8 Rounds  | Jul 12, 2026 |
      | 26  | Ticket to Ride            | 3 Players   | 1 Round   | Jul 4, 2026  |
      | 27  | Phase 10                  | 4 Players   | 6 Rounds  | Dec 26, 2025 |
      | 28  | Quick Game (no rounds)    | 3 Players   | 0 Rounds  | Jun 1, 2025  |
      | 29  | Game Night (players gone) | 0 Players   | 2 Rounds  | Mar 15, 2025 |
      | 30  | Old Hearts Save           | 3 Players   | 3 Rounds  | Nov 28, 2024 |

  @smoke
  Scenario: Tapping a game opens it
    Given a game "List Open Test" with players "Calvin, Kim"
    And I go back
    When I open the game "List Open Test"
    Then the title is "List Open Test"
    And I see "Add Round"

  Scenario: A new game goes to the top of the list
    Given a game "Newest Game" with players "Calvin, Kim"
    When I go back
    Then the title is "Games"
    And the first game is "Newest Game"
    And it shows "2 Players" and "1 Round"

  @seed
  Scenario: Entering a score moves the game to the top
    Given the seed data is loaded
    When I open the game "Catan"
    And I enter "9" for Kim in round 1
    And I go back
    Then the first game is "Catan"
    And the first game shows today's date

  @seed
  Scenario: Opening a game without changing it leaves its place in the list
    Given the seed data is loaded
    When I open the game "Wingspan"
    And I go back
    Then row 7 is "Wingspan"

  @smoke
  Scenario: Long pressing a game deletes it after asking
    Given a game "Delete Me" with players "Calvin, Kim"
    And I go back
    When I long press the game "Delete Me"
    Then I see "Do you want to delete this game?"
    When I tap "Delete"
    Then I don't see "Delete Me"
    When I close and reopen the app
    Then I don't see "Delete Me"

  Scenario: Cancelling a delete keeps the game
    Given a game "Keep Me" with players "Calvin, Kim"
    And I go back
    When I long press the game "Keep Me"
    And I tap "Cancel"
    Then I see "Keep Me"

  Scenario: Deleting the last game shows the empty list
    Given a fresh install with the usage data question answered
    And a game "Only Game" with players "Calvin"
    And I go back
    When I long press the game "Only Game"
    And I tap "Delete"
    Then I see "No games created"

  @seed
  Scenario: A game whose players were all deleted opens its settings to add players
    Given the seed data is loaded
    When I open the game "Game Night (players gone)"
    Then the title is "Game Settings"
    And I see "No players added"
    When I tap "EDIT"
    And I pick "Calvin" and "Kim"
    And I tap "Save"
    Then the players are "Calvin" and "Kim"
    When I tap "Save"
    Then the title is "Game Night (players gone)"
    And the totals are:
      | Calvin | Kim |
      | 0      | 0   |
    When I go back
    Then the title is "Games"

  @seed
  Scenario: A game saved without rounds gets its first round when opened
    Given the seed data is loaded
    When I open the game "Quick Game (no rounds)"
    Then the game has 1 round
    And the totals are:
      | Morgan | Riley | Avery |
      | 0      | 0     | 0     |
    When I go back
    Then the game "Quick Game (no rounds)" shows "1 Round"

  @seed
  Scenario: The settings icon opens Settings
    Given the seed data is loaded
    When I tap the Settings icon
    Then the title is "Settings"
