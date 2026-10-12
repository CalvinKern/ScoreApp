# Test data

`seed.sql` fills the debug app with 30 games scored the way each game's rules score them, plus games that hit
the app's edge cases. Load it, and save or restore the database, with `scripts/emulator-db.sh`:

```bash
scripts/emulator-db.sh seed               # Replace the app's data with this test data (backs up first)
scripts/emulator-db.sh backup [label]     # Save the database to db-backups/
scripts/emulator-db.sh restore [file]     # Put a backup back, the newest when no file is given
scripts/emulator-db.sh list               # List the backups
```

Backups are ignored by git. A backup from an older schema restores into a newer app, which migrates it on launch,
so an old backup is also a quick way to test a migration.

The regression plan's [seeded games feature](../../regression/features/13_seeded_games.feature) checks every game
below opens with the right totals, leader, dealer, and goal state. Update its expected values when this data changes.

## Games

Scores are realistic for each game. Settings are abbreviated: **dealer**, **low wins** (reversed scoring),
**goal** (target score), **rounds** (target rounds), **notes** (show round notes), and **calc** (calculator).

| Game | How it scores | Settings | What it tests |
|---|---|---|---|
| Yahtzee | 13 turns, highest wins | rounds 13, notes, calc | A brand new game with one empty round, newest in the list |
| Phase 10 | Hand winner scores 0, others 5 per 1-9, 10 per 10-12, 15 per skip, 25 per wild. Lowest wins | dealer, low wins, notes, calc | Notes tracking each player's phase, calculations like `5x4+10+25`, in progress |
| Hand and Foot | Partnerships, 4 hands with meld minimums of 50/90/120/150. Clean books 500, dirty 300, card points, +100 for going out, cards left subtract | dealer, rounds 4, notes, calc | Teams as players, scores in the thousands, a negative hand, last round in progress |
| Uno | Hand winner scores everyone else's cards (numbers face value, action cards 20, wilds 50), to 500 | dealer, goal 500, calc | 10 players (the table scrolls sideways), only one score per round |
| Trivia Night | Points per round, half points allowed | rounds 8, system keyboard | 13 players with long and emoji names, decimals, a `10/3` repeating decimal |
| Gin Rummy | Hand winner scores the deadwood difference, +25 for gin or an undercut, to 100 | dealer, goal 100, notes, calc | Two players at 94 and 92, about to reach the goal |
| Wingspan | One final scoring: birds, bonus cards, end-of-round goals, eggs, cached food, tucked cards | rounds 6, notes, calc | Rounds used as scoring categories, a finished game |
| Ticket to Ride | Routes 1/2/4/7/10/15 by length, completed tickets add and failed ones subtract, longest path +10 | rounds 3, notes, calc | Long calculations, a negative category |
| Hearts | 26 points a hand (shooting the moon gives everyone else 26), game ends at 100, lowest wins | dealer, low wins, goal 100 | Goal reached with a tie for the lead, a shoot-the-moon round |
| Skull King | 20 per trick bid correctly plus bonuses, -10 per trick missed, zero bids worth 10 x the round | dealer, rounds 10, notes, calc | Negative hands, in progress |
| Alhambra | Three scorings, more places score each time, ties split points rounding down, wall length scores | rounds 3, notes, calc | Notes explaining a tie split, finished |
| Mille Bornes | Distance plus bonuses (trip complete 400, safe trip 300, shut-out 500, safeties 100, coup fourré 300), to 5000 | dealer, goal 5000, notes, calc | Four-digit totals, goal reached |
| Planted | End of game: plants, propagation jars, decorations | rounds 3, notes, calc | A tie for the win |
| Agricola | One final scoring of every category, -1 per unused farmyard space, -3 per begging card | rounds 1, notes, calc | One long calculation with `x` and subtraction, one round |
| Agricola Solo | Same as Agricola | rounds 1, calc | A one-player game |
| Golf | 9 holes, lowest wins, jokers -2 | dealer, low wins, rounds 9, calc | Negative scores, a typed `0`, finished |
| Five Crowns | 11 rounds (3s wild up to kings wild), whoever goes out scores 0, lowest wins | dealer, low wins, rounds 11, notes | 6 players, in progress |
| Spades | Partnerships, 10 per trick bid plus 1 per bag, set bids lose 10 per trick bid, 10 bags lose 100, to 500 | dealer, goal 500, notes, calc | Negative hands, notes with the bids |
| Rummy 500 | Melds minus cards left in hand, to 500 | dealer, goal 500, calc | Negative hands and a negative running total |
| Scrabble | Word scores per turn, letters left over subtract at the end | notes, calc | 25 rounds (vertical scrolling), notes holding the words |
| Mexican Train | 13 rounds from double 12 down to double blank, pips left in hand, lowest wins | low wins, rounds 13, notes | A long finished game |
| Darts 301 | Points per turn of three darts, first to 301 | goal 301, calc | Two inputs that can't be calculated (`3x20+` and `50/0`), saved as 0 and shown in red |
| Catan | Victory points, first to 10 | goal 10 | Goal reached in the first round |
| Cribbage | Pegging plus hand and crib, first to 121 | dealer, goal 121 | Two players, goal reached |
| Euchre | Partnerships, 1, 2, or 4 points a hand, first to 10 | dealer, goal 10 | Small scores, one team scoring per hand |
| Ticket to Ride | Same as above | calc | A second game with the same name (for auto-loading previous settings), parentheses like `(15+10)x2` |
| Phase 10 | Same as above | dealer, low wins | An older game with the same name, saved before typed input was kept |
| Quick Game (no rounds) | | | A game with no rounds, which the app fixes by adding one |
| Game Night (players gone) | | | Every player archived, so opening it goes to game setup |
| Old Hearts Save | | dealer, low wins | Data from old app versions: dealer stored as 0, a target score and rounds of 0 (no limit), the deprecated showRounds flag, one archived player whose scores are hidden, no typed input |

## Players

The player list includes lowercase names (`alice`) to check sorting ignores case, accented (`José`, `Zoë`), CJK
(`李雷`), emoji (`🎲 Dice Goblin`), and very long names, two teams, three archived players that never show up,
and `Never Played`, who is in no games. `Priya` and `Grandma Jo` picked their own graph colors: Priya typed one in that isn't a graph color, and
Grandma Jo picked the graph green, so she matches whoever sits third in her games.

## Rule sources

- [Phase 10 rules](https://rulespal.com/phase-10/rulebook)
- [Hand and Foot rules (Bicycle)](https://bicyclecards.com/rules/hand-and-foot)
- [Mille Bornes scoring](https://man.openbsd.org/mille.6)
- [Alhambra (Wikipedia)](https://en.wikipedia.org/wiki/Alhambra_(board_game))
- [Agricola scoring (BGA)](https://en.doc.boardgamearena.com/Gamehelpagricola)
- [Planted rulebook](https://rulespal.com/planted/rulebook)
- [Wingspan instructions](https://www.buffalolib.org/sites/default/files/gaming-unplugged/inst/Wingspan%20Instructions.pdf)
- [Gin rummy (Wikipedia)](https://en.wikipedia.org/wiki/Gin_rummy)
- [Ticket to Ride rules](https://rulespal.com/ticket-to-ride/rulebook)
