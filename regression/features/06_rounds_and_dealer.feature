Feature: Rounds and the dealer
  "Add Round" adds a row of scores, the calculator's arrows move between scores, and "Delete Rounds" removes rows.
  In games with a dealer, the dealer moves one seat each round and can be changed for any round.

  @smoke
  Scenario: Add Round adds a round and starts on its first score
    Given a game "Add Rounds" with players "Calvin, Lindsay, Bob"
    When I tap "Add Round"
    Then the game has 2 rounds
    And the round 2 score for Calvin is being edited
    And the calculator keyboard shows

  @calculator @smoke
  Scenario: The next arrow moves through the scores, and adds a round after the last one
    Given a game "Next Arrow" with players "Calvin, Lindsay"
    When I tap the round 1 score for Calvin
    And I type "5" on the calculator
    And I tap "->"
    Then the round 1 score for Lindsay is being edited
    When I type "7" on the calculator
    And I tap "->"
    Then the game has 2 rounds
    And the round 2 score for Calvin is being edited
    And the totals are:
      | Calvin | Lindsay |
      | 5      | 7       |

  @calculator
  Scenario: The previous arrow moves back a score
    Given a game "Prev Arrow" with players "Calvin, Lindsay"
    When I tap the round 1 score for Lindsay
    And I tap "<-"
    Then the round 1 score for Calvin is being edited

  @calculator
  Scenario: Scores can be entered for a whole game with only the keypad
    Given a game "Keypad Only" with players "Calvin, Lindsay"
    When I tap the round 1 score for Calvin
    And I type these on the calculator, tapping "->" after each: "10", "20", "30", "40", "50", "60"
    Then the game has 4 rounds
    And the totals are:
      | Calvin | Lindsay |
      | 90     | 120     |

  Scenario: The scores scroll to the newest round when a game opens
    Given a game "Long Game" with players "Calvin, Lindsay"
    And I tap "Add Round" 14 times
    When I go back
    And I open the game "Long Game"
    Then I see "Add Round"
    And round 15 is on screen

  @seed
  Scenario: A long seeded game opens at its newest round
    Given the seed data is loaded
    When I open the game "Scrabble"
    Then I see "Add Round"
    When I scroll to the top
    Then the round 1 score for Lindsay shows "22"

  @seed @visual
  Scenario: A game with many players scrolls sideways, keeping names over their scores
    Given the seed data is loaded
    When I open the game "Uno"
    Then each player's column is wide enough to read
    When I scroll the scores to the right
    Then the player names scroll with them
    And I see "Marcus"
    And "Add Round" stays in the middle of the screen

  @smoke
  Scenario: Deleting a round removes its scores from the totals
    Given a game "Delete Round" with players "Calvin, Lindsay"
    And I play these rounds:
      | Calvin | Lindsay |
      | 10     | 1       |
      | 20     | 2       |
      | 30     | 3       |
    When I choose "Delete Rounds" from the game menu
    Then I see "Delete Rounds"
    And I see "Round 1", "Round 2", and "Round 3"
    When I check "Round 2"
    And I tap "Delete"
    Then the game has 2 rounds
    And the round 2 scores are:
      | Calvin | Lindsay |
      | 30     | 3       |
    And the totals are:
      | Calvin | Lindsay |
      | 40     | 4       |

  Scenario: Deleting several rounds at once
    Given a game "Delete Several" with players "Calvin"
    And I play these rounds:
      | Calvin |
      | 1      |
      | 2      |
      | 3      |
      | 4      |
    When I choose "Delete Rounds" from the game menu
    And I check "Round 1"
    And I check "Round 4"
    And I tap "Delete"
    Then the totals are:
      | Calvin |
      | 5      |

  Scenario: Deleting every round leaves a new empty round
    Given a game "Delete All" with players "Calvin, Lindsay"
    And I play these rounds:
      | Calvin | Lindsay |
      | 10     | 1       |
      | 20     | 2       |
    When I choose "Delete Rounds" from the game menu
    And I check "Round 1"
    And I check "Round 2"
    And I tap "Delete"
    Then the game has 1 round
    And the totals are:
      | Calvin | Lindsay |
      | 0      | 0       |

  Scenario: Closing Delete Rounds, or deleting nothing, keeps every round
    Given a game "Delete None" with players "Calvin"
    And I play these rounds:
      | Calvin |
      | 1      |
      | 2      |
    When I choose "Delete Rounds" from the game menu
    And I check "Round 1"
    And I tap "Close"
    Then the game has 2 rounds
    When I choose "Delete Rounds" from the game menu
    And I tap "Delete"
    Then the game has 2 rounds

  @visual @smoke
  Scenario: The dealer moves one seat each round and wraps around
    Given a game "Dealer Moves" with players "Calvin, Lindsay, Bob" and settings:
      | Dealer       | on     |
      | First dealer | Calvin |
    When I tap "Add Round" 3 times
    Then Calvin dealt round 1
    And Lindsay dealt round 2
    And Bob dealt round 3
    And Calvin is the dealer
    And the past dealers' borders are lighter than the current dealer's

  @visual
  Scenario: Long pressing a score makes that player the round's dealer
    Given a game "Change Dealer" with players "Calvin, Lindsay, Bob" and settings:
      | Dealer       | on     |
      | First dealer | Calvin |
    When I long press the round 1 score for Bob
    Then I see "Make Bob dealer for this round?"
    When I tap "Dealer"
    Then Bob is the dealer
    When I tap "Add Round"
    Then Calvin is the dealer

  Scenario: Cancelling the dealer change keeps the dealer
    Given a game "Keep Dealer" with players "Calvin, Lindsay" and settings:
      | Dealer       | on     |
      | First dealer | Calvin |
    When I long press the round 1 score for Lindsay
    And I tap "Cancel"
    Then Calvin is the dealer

  @visual
  Scenario: A past round's dealer can be changed too
    Given a game "Past Dealer" with players "Calvin, Lindsay, Bob" and settings:
      | Dealer       | on     |
      | First dealer | Calvin |
    And I tap "Add Round"
    When I long press the round 1 score for Bob
    And I tap "Dealer"
    Then Bob dealt round 1
    And Lindsay is the dealer

  Scenario: Games without a dealer don't ask about the dealer
    Given a game "No Dealer Press" with players "Calvin, Lindsay" and settings:
      | Dealer | off |
    When I long press the round 1 score for Lindsay
    Then I don't see "dealer for this round?"

  @seed @visual
  Scenario: A seeded dealer game shows its current dealer
    Given the seed data is loaded
    When I open the game "Gin Rummy"
    Then Calvin is the dealer
    When I tap "Add Round"
    Then Lindsay is the dealer

  @seed @visual
  Scenario: An old game with no saved dealer starts the next round with the first player
    Given the seed data is loaded
    When I open the game "Old Hearts Save"
    Then nobody has the dealer border
    When I tap "Add Round"
    Then Calvin is the dealer
