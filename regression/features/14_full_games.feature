Feature: Full games
  Real games played from a new game to the standings, scored the way each game's rules score them. Each one mixes the
  features a real table uses together: the dealer, reversed scoring, targets, calculations, notes, and starting the
  next game.

  @smoke @system-keyboard
  Scenario: Hearts, lowest wins, ends when someone passes 100
    Given a game "Hearts Night" with players "Calvin, Lindsay, Bob, Zed" and settings:
      | Dealer              | on     |
      | First dealer        | Calvin |
      | Reverse Scoring     | on     |
      | Target Score        | 100    |
      | Calculator Keyboard | off    |
    When I play these rounds:
      | Calvin | Lindsay | Bob | Zed |
      | 3      | 13      | 6   | 4   |
      | 8      | 0       | 17  | 1   |
      | 26     | 26      | 0   | 26  |
    Then the totals are:
      | Calvin | Lindsay | Bob | Zed |
      | 37     | 39      | 23  | 31  |
    And Bob is leading
    And Bob is the dealer
    When I tap "Add Round"
    And I play these rounds:
      | Calvin | Lindsay | Bob | Zed |
      | 5      | 2       | 14  | 5   |
      | 9      | 13      | 3   | 1   |
      | 2      | 18      | 0   | 6   |
      | 11     | 4       | 8   | 3   |
    Then the button after the rounds is "Add Round"
    When I tap "Add Round"
    And I play these rounds:
      | Calvin | Lindsay | Bob | Zed |
      | 1      | 25      | 0   | 0   |
    Then the totals are:
      | Calvin | Lindsay | Bob | Zed |
      | 65     | 101     | 48  | 46  |
    And Zed is leading
    And confetti bursts from Zed's total
    And the button after the rounds is "Finish Game"
    And Zed is the dealer
    When I tap "Finish Game"
    And I tap "Finish"
    Then I see "8 Rounds Played"
    And the standings are:
      | place | player  | total |
      | #1    | Zed     | 46    |
      | #2    | Bob     | 48    |
      | #3    | Calvin  | 65    |
      | #4    | Lindsay | 101   |
    When I tap "New Game"
    Then "Calvin" is marked "Dealer"
    When I tap "Save"
    Then Calvin is the dealer
    And the totals are:
      | Calvin | Lindsay | Bob | Zed |
      | 0      | 0       | 0   | 0   |

  @smoke @calculator
  Scenario: Uno, only the hand's winner scores, first to 500, then one more hand
    Given a game "Uno Night" with players "Lindsay, Bob, Priya" and settings:
      | Target Score | 500 |
    When I play these rounds:
      | Lindsay            | Bob                  | Priya   |
      | 20+50+7+3          |                      |         |
      |                    | 50+50+20+9           |         |
      | 20+20+50+50+4      |                      |         |
      |                    |                      | 50+20+6 |
      | 50+50+50+20+20+8+2 |                      |         |
      |                    | 50+50+20+20+20+9+8+7 |         |
    Then the totals are:
      | Lindsay | Bob | Priya |
      | 424     | 313 | 76    |
    And the button after the rounds is "Add Round"
    When I tap "Add Round"
    And I enter "50+20+6" for Lindsay in round 7
    Then the totals are:
      | Lindsay | Bob | Priya |
      | 500     | 313 | 76    |
    And confetti bursts from Lindsay's total
    When I tap "Finish Game"
    And I tap "Another Round!"
    Then the game has 8 rounds
    When I enter "12" for Bob in round 8
    And I tap "Finish Game"
    And I tap "Finish"
    Then the standings are:
      | place | player  | total |
      | #1    | Lindsay | 500   |
      | #2    | Bob     | 325   |
      | #3    | Priya   | 76    |

  @calculator
  Scenario: Spades, partnerships with set bids and bags, first to 500
    Given a game "Spades Night" with players "Us, Them" and settings:
      | Dealer       | on  |
      | Target Score | 500 |
    When I play these rounds:
      | Us     | Them     |
      | 50+1   | 60+1     |
      | -40    | -80      |
    Then the totals are:
      | Us | Them |
      | 11 | -19  |
    And Us is leading
    When I tap "Add Round"
    And I play these rounds:
      | Us     | Them     |
      | 100+40 | -70      |
      | 60+3   | 40       |
      | 70     | 50+2-100 |
      | 90+1   | 40       |
      | 100+5  | 30       |
      | 40+2   | -50      |
    Then the totals are:
      | Us  | Them |
      | 522 | -77  |
    And the button after the rounds is "Finish Game"
    When I choose "Show Graph" from the game menu
    Then the podium bars are Us 522, Them -77, from left to right
    And Them's bar goes below zero with its total under it

  @calculator
  Scenario: Phase 10 with notes, lowest wins, no target
    Given a game "Phase 10 Night" with players "Calvin, Lindsay, Bob" and settings:
      | Dealer           | on     |
      | First dealer     | Calvin |
      | Reverse Scoring  | on     |
      | Show Round Notes | on     |
    When I play these rounds:
      | Calvin | Lindsay | Bob     |
      | 0      | 5×3+10  | 25+10×2 |
      | 5×2    | 0       | 15+5    |
      | 25+25  | 10      | 0       |
    And I tap the name "Lindsay"
    And I type "Phase 1 ✓" into the "Round 1:" note
    # Notes capitalize each word as they're typed, so the note is written that way
    And I type "Phase 2 ✓, Went Out" into the "Round 2:" note
    And I tap "Save"
    Then the totals are:
      | Calvin | Lindsay | Bob |
      | 60     | 35      | 65  |
    And Lindsay is leading
    And Bob is the dealer
    And the button after the rounds is "Add Round"
    When I tap the name "Lindsay"
    Then the "Round 2:" note is "Phase 2 ✓, Went Out"
    When I tap "Cancel"
    And I choose "Standings" from the game menu
    Then the standings are:
      | place | player  | total |
      | #1    | Lindsay | 35    |
      | #2    | Calvin  | 60    |
      | #3    | Bob     | 65    |

  @system-keyboard
  Scenario: Trivia night with half points, four rounds
    Given a game "Trivia Night Out" with players "Calvin, Lindsay, Bob" and settings:
      | Dealer              | off |
      | Calculator Keyboard | off |
      | Target Rounds       | 4   |
    When I play these rounds:
      | Calvin | Lindsay | Bob |
      | 7.5    | 8       | 6.5 |
      | 9      | 7.5     | 10  |
      | 6      | 6       | 8.5 |
      | 10     | 9.5     | 5   |
    Then the totals are:
      | Calvin | Lindsay | Bob |
      | 32.5   | 31      | 30  |
    And Calvin is leading
    And the button after the rounds is "Finish Game"
    And no confetti shows

  @calculator
  Scenario: Agricola, everything scored in one calculation
    Given a game "Farm" with players "Calvin, Lindsay" and settings:
      | Dealer        | off |
      | Target Rounds | 1   |
    Then the button after the rounds is "Finish Game"
    When I enter "2+3+1+1+2+1+1-2+2+5×2+4×3+4+3" for Calvin in round 1
    And I enter "3+2+2+1+1+2+1+5+5×3-3+6" for Lindsay in round 1
    Then the totals are:
      | Calvin | Lindsay |
      | 40     | 35      |
    When I tap the round 1 score for Lindsay
    Then the score being edited shows "3+2+2+1+1+2+1+5+5×3-3+6"
    When I tap "="
    And I tap "Finish Game"
    And I tap "Finish"
    Then the standings are:
      | place | player  | total |
      | #1    | Calvin  | 40    |
      | #2    | Lindsay | 35    |

  @calculator
  Scenario: Wingspan, each round is a scoring category named in the notes
    Given a game "Birds" with players "Lindsay, Priya" and settings:
      | Dealer           | off |
      | Show Round Notes | on  |
      | Target Rounds    | 3   |
    When I play these rounds:
      | Lindsay | Priya |
      | 9+5+7   | 6+8   |
      | 7+3     | 6     |
      | 14      | 21    |
    And I tap the name "Lindsay"
    And I type "Birds" into the "Round 1:" note
    And I type "Bonus cards" into the "Round 2:" note
    And I type "Eggs" into the "Round 3:" note
    And I tap "Save"
    Then the totals are:
      | Lindsay | Priya |
      | 45      | 41    |
    And Lindsay is leading
    And the button after the rounds is "Finish Game"
    When I choose "Show Graph" from the game menu
    Then the "Score by rounds" graph has a line for "Lindsay" through 21, 31, 45
    And it has a line for "Priya" through 14, 20, 41

  @slow @calculator
  Scenario: Golf, nine holes, lowest wins, jokers are -2
    Given a game "Golf Day" with players "Lindsay, Zed, Sam" and settings:
      | Dealer          | on      |
      | First dealer    | Lindsay |
      | Reverse Scoring | on      |
      | Target Rounds   | 9       |
    When I play these rounds:
      | Lindsay | Zed  | Sam |
      | 10+2    | 20   | 7   |
      | 5       | 9    | 15  |
      | -2      | 11   | 3   |
      | 8       | 2    | -1  |
      | 3       | 7    | 10  |
      | 0       | -2-2 | 6   |
      | 14      | 5    | 2   |
      | 4       | 13   | 9   |
      | 6       | 1    | 8   |
    Then the game has 9 rounds
    And the totals are:
      | Lindsay | Zed | Sam |
      | 50      | 64  | 59  |
    And Lindsay is leading
    And Sam is the dealer
    And the button after the rounds is "Finish Game"
    When I tap "Finish Game"
    And I tap "Finish"
    Then the standings are:
      | place | player  | total |
      | #1    | Lindsay | 50    |
      | #2    | Sam     | 59    |
      | #3    | Zed     | 64    |

  @slow @calculator
  Scenario: A game played only with the keypad, from the first score to the standings
    Given a game "Keypad Game" with players "Calvin, Lindsay" and settings:
      | Target Rounds | 3 |
    When I tap the round 1 score for Calvin
    And I type these on the calculator, tapping "->" after each: "10", "20", "30", "40", "50"
    And I type "60" on the calculator
    And I tap "->"
    Then I see "Goal Reached!"
    When I tap "Finish"
    Then the standings are:
      | place | player  | total |
      | #1    | Lindsay | 120   |
      | #2    | Calvin  | 90    |
