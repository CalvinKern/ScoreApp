Feature: Reaching the goal
  A game's goal is reached when any total reaches the Target Score (in both scoring modes), or when the game has as
  many rounds as Target Rounds. Then "Finish Game" replaces "Add Round" and asks whether to finish or keep playing.

  @smoke
  Scenario: Reaching the target score shows Finish Game
    Given a game "Score Goal" with players "Calvin, Lindsay" and settings:
      | Target Score | 100 |
    When I enter "99" for Calvin in round 1
    Then the button after the rounds is "Add Round"
    When I enter "100" for Calvin in round 1
    Then the button after the rounds is "Finish Game"

  Scenario: Going back under the target score brings Add Round back
    Given a game "Score Undo" with players "Calvin" and settings:
      | Target Score | 50 |
    When I enter "60" for Calvin in round 1
    Then the button after the rounds is "Finish Game"
    When I enter "40" for Calvin in round 1
    Then the button after the rounds is "Add Round"

  Scenario: With Reverse Scoring, the game still ends when someone reaches the target score
    Given a game "Reverse Goal" with players "Calvin, Lindsay" and settings:
      | Reverse Scoring | on  |
      | Target Score    | 100 |
    When I play these rounds:
      | Calvin | Lindsay |
      | 40     | 104     |
    Then the button after the rounds is "Finish Game"
    And Calvin is leading

  @smoke
  Scenario: Reaching the target rounds shows Finish Game as soon as the last round is added
    Given a game "Round Goal" with players "Calvin, Lindsay" and settings:
      | Target Rounds | 3 |
    When I tap "Add Round"
    Then the button after the rounds is "Add Round"
    When I tap "Add Round"
    Then the game has 3 rounds
    And the button after the rounds is "Finish Game"

  Scenario: A target of one round finishes from the start
    Given a game "One Round" with players "Calvin, Lindsay" and settings:
      | Target Rounds | 1 |
    Then the button after the rounds is "Finish Game"

  @smoke
  Scenario: Finish Game asks, and Finish shows the standings
    Given a game "Finish It" with players "Calvin, Lindsay" and settings:
      | Target Score | 10 |
    And I enter "12" for Lindsay in round 1
    When I tap "Finish Game"
    Then I see "Goal Reached!"
    And I see "Would you like to finish the game and see the standings?"
    When I tap "Finish"
    Then I see "Player Standings"
    And the standings are:
      | place | player  | total |
      | #1    | Lindsay | 12    |
      | #2    | Calvin  | 0     |

  @smoke
  Scenario: Another Round keeps playing past a target score
    Given a game "Keep Going" with players "Calvin, Lindsay" and settings:
      | Target Score | 10 |
    And I enter "12" for Lindsay in round 1
    When I tap "Finish Game"
    And I tap "Another Round!"
    Then the game has 2 rounds
    And the round 2 score for Calvin is being edited
    And the button after the rounds is "Finish Game"

  Scenario: Another Round keeps playing past the target rounds
    Given a game "Extra Round" with players "Calvin" and settings:
      | Target Rounds | 2 |
    And I tap "Add Round"
    When I tap "Finish Game"
    And I tap "Another Round!"
    Then the game has 3 rounds
    And the button after the rounds is "Finish Game"

  @calculator
  Scenario: The next arrow on the last score asks to finish once the goal is reached
    Given a game "Arrow Finish" with players "Calvin, Lindsay" and settings:
      | Target Score | 50 |
    When I tap the round 1 score for Calvin
    And I type "60" on the calculator
    And I tap "->"
    And I type "5" on the calculator
    And I tap "->"
    Then I see "Goal Reached!"
    And the game has 1 round

  Scenario: Going back from Goal Reached! keeps the game as it is
    Given a game "Back Out" with players "Calvin" and settings:
      | Target Rounds | 1 |
    When I tap "Finish Game"
    And I go back
    Then I don't see "Goal Reached!"
    And the game has 1 round

  @visual @smoke
  Scenario: Reaching the target score celebrates the leader with confetti
    Given a game "Confetti" with players "Calvin, Lindsay" and settings:
      | Target Score | 100 |
    When I enter "100" for Lindsay in round 1
    Then confetti bursts from Lindsay's total

  @visual
  Scenario: No confetti for reaching the target rounds
    Given a game "No Confetti Rounds" with players "Calvin, Lindsay" and settings:
      | Target Rounds | 1 |
    When I enter "10" for Lindsay in round 1
    Then no confetti shows

  @visual
  Scenario: Confetti again when the lead changes after the goal
    Given a game "Confetti Twice" with players "Calvin, Lindsay" and settings:
      | Target Score | 100 |
    And I enter "100" for Lindsay in round 1
    And confetti bursts from Lindsay's total
    When I enter "120" for Calvin in round 1
    Then confetti bursts from Calvin's total

  @visual
  Scenario: The leader reaching the target score celebrates, once a round
    Given a game "Confetti Leader" with players "Calvin, Lindsay" and settings:
      | Target Score | 100 |
    And I enter "50" for Lindsay in round 1
    And I close and reopen the app
    And I open the game "Confetti Leader"
    Then no confetti shows
    When I tap "Add Round"
    And I enter "60" for Lindsay in round 2
    Then confetti bursts from Lindsay's total
    When I enter "70" for Lindsay in round 2
    Then no confetti shows
    When I enter "10" for Lindsay in round 2
    And I enter "60" for Lindsay in round 2
    Then no confetti shows

  @visual
  Scenario: Reaching the target score again in a later round celebrates again
    Given a game "Confetti Again" with players "Calvin, Lindsay" and settings:
      | Target Score | 100 |
    And I enter "110" for Lindsay in round 1
    And confetti bursts from Lindsay's total
    When I tap "Finish Game"
    And I tap "Another Round!"
    And I enter "-60" for Lindsay in round 2
    Then no confetti shows
    When I enter "0" for Lindsay in round 2
    Then confetti bursts from Lindsay's total

  @visual @smoke
  Scenario: Finishing the game shoots confetti from the top corners over the standings
    Given a game "Confetti Finish" with players "Calvin, Lindsay" and settings:
      | Target Score | 10 |
    And I enter "12" for Lindsay in round 1
    When I tap "Finish Game"
    And I tap "Finish"
    Then confetti shoots in from both top corners, over the "Player Standings" dialog
    When I tap "Close"
    And I tap the Standings icon
    Then I see "Player Standings"
    And no confetti shows

  @seed
  Scenario Outline: Seeded games that reached their goal show Finish Game (<game>)
    Given the seed data is loaded
    When I open the game "<game>"
    Then the button after the rounds is "Finish Game"

    Examples: target score
      | game         |
      | Hearts       |
      | Mille Bornes |
      | Catan        |
      | Cribbage     |

    Examples: target rounds
      | game          |
      | Hand and Foot |
      | Wingspan      |
      | Alhambra      |
      | Golf          |
      | Mexican Train |

  @seed
  Scenario: A target score and rounds of 0 saved by old versions mean no target
    Given the seed data is loaded
    When I open the game "Old Hearts Save"
    Then the button after the rounds is "Add Round"
    When I play these rounds:
      | Calvin | Bob | Zed |
      | 50     | 50  | 50  |
    Then the button after the rounds is "Add Round"
