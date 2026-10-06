-- Test data for the debug app, covering the games and edge cases listed in scripts/test-data/README.md.
-- Load it with `scripts/emulator-db.sh seed`, which backs up the current data first.
--
-- schema-version: 12
--
-- This REPLACES every player, game, round, and score in the database. It only writes to tables the app
-- created, so Room's schema and identity hash are left alone. If a migration changes these tables, update
-- this file and the schema-version above.
--
-- How to read it: the seed_* tables describe the data, and the INSERTs at the bottom expand them into
-- rows. A score row only needs to be listed when it isn't 0; everything else is filled in.

PRAGMA foreign_keys = OFF; -- One legacy game stores dealer_id 0 on purpose, which isn't a real player
BEGIN;

DELETE FROM scores;
DELETE FROM rounds;
DELETE FROM game_player_entity;
DELETE FROM games;
DELETE FROM players;
DELETE FROM sqlite_sequence WHERE name IN ('scores', 'rounds', 'games', 'players');

-- Players -------------------------------------------------------------------------------------------------------------

CREATE TEMP TABLE seed_players (uid INTEGER, name TEXT, archived INTEGER);
INSERT INTO seed_players VALUES
    (1, 'Calvin', 0),
    (2, 'Kim', 0),
    (3, 'alice', 0),                                  -- Lowercase, sorts with "Asdf"-style names ignoring case
    (4, 'Bob', 0),
    (5, 'Zed', 0),
    (6, 'José', 0),
    (7, 'Zoë', 0),
    (8, '李雷', 0),                                    -- CJK name
    (9, 'Priya', 0),
    (10, 'Marcus', 0),
    (11, 'Grandma Jo', 0),
    (12, 'Bartholomew Fitzgerald-Montgomery III', 0), -- Long name
    (13, '🎲 Dice Goblin', 0),                         -- Emoji name
    (14, 'Team Red (Calvin & Kim)', 0),               -- Partnership games use a team as a player
    (15, 'Team Blue (Bob & alice)', 0),
    (16, 'Sam', 0),
    (17, 'Taylor', 0),
    (18, 'Morgan', 0),
    (19, 'Riley', 0),
    (20, 'Avery', 0),
    (21, 'Jordan', 0),
    (22, 'Old Roommate', 1),                          -- Archived: hidden from games and the player list
    (23, 'Moved Away', 1),
    (24, 'Ex-Coworker', 1),
    (25, 'Never Played', 0);                          -- In the player list, but in no games

-- Games ---------------------------------------------------------------------------------------------------------------
--
-- played:       rounds with scores
-- open_round:   1 adds an empty current round after them (a game in progress)
-- first_dealer: seat of the first round's dealer, the dealer then moves one seat each round
-- fill_*:       when set, scores not listed below get a made-up value from fill_min to fill_max in steps of fill_step
-- auto_input:   1 stores what was typed for each score (games played since v12), 0 leaves it null (older games)
-- legacy:       1 stores dealer_id 0 and the deprecated showRounds flag, like data from old app versions

CREATE TEMP TABLE seed_games (
    uid INTEGER, name TEXT, date TEXT,
    has_dealer INTEGER, reversed INTEGER, max_score REAL, max_rounds INTEGER, show_notes INTEGER, calculator INTEGER,
    played INTEGER, open_round INTEGER, first_dealer INTEGER,
    fill_min INTEGER, fill_max INTEGER, fill_step INTEGER, auto_input INTEGER, legacy INTEGER
);
INSERT INTO seed_games VALUES
--   uid name                          last played                  dlr rev max   rnds notes calc played open 1st  fill          auto legacy
    (1,  'Phase 10',                   '2026-10-05T19:30:00-06:00', 1,  1,  NULL, NULL, 1,  1,   7,     1,   0,   NULL, NULL, NULL, 1,   0),
    (2,  'Yahtzee',                    '2026-10-05T20:15:00-06:00', 0,  0,  NULL, 13,   1,  1,   0,     1,   0,   NULL, NULL, NULL, 1,   0),
    (3,  'Hand and Foot',              '2026-10-04T21:00:00-06:00', 1,  0,  NULL, 4,    1,  1,   3,     1,   0,   NULL, NULL, NULL, 1,   0),
    (4,  'Uno',                        '2026-10-03T18:45:00-06:00', 1,  0,  500,  NULL, 0,  1,   4,     1,   2,   NULL, NULL, NULL, 1,   0),
    (5,  'Trivia Night',               '2026-10-02T20:00:00-06:00', 0,  0,  NULL, 8,    0,  0,   4,     1,   0,   NULL, NULL, NULL, 1,   0),
    (6,  'Gin Rummy',                  '2026-10-01T22:10:00-06:00', 1,  0,  100,  NULL, 1,  1,   5,     1,   1,   NULL, NULL, NULL, 1,   0),
    (7,  'Wingspan',                   '2026-09-28T16:00:00-06:00', 0,  0,  NULL, 6,    1,  1,   6,     0,   0,   NULL, NULL, NULL, 1,   0),
    (8,  'Ticket to Ride',             '2026-09-27T19:00:00-06:00', 0,  0,  NULL, 3,    1,  1,   3,     0,   0,   NULL, NULL, NULL, 1,   0),
    (9,  'Hearts',                     '2026-09-26T21:30:00-06:00', 1,  1,  100,  NULL, 0,  0,   8,     0,   3,   NULL, NULL, NULL, 1,   0),
    (10, 'Skull King',                 '2026-09-24T20:00:00-06:00', 1,  0,  NULL, 10,   1,  1,   5,     1,   0,   NULL, NULL, NULL, 1,   0),
    (11, 'Alhambra',                   '2026-09-20T15:00:00-06:00', 0,  0,  NULL, 3,    1,  1,   3,     0,   0,   NULL, NULL, NULL, 1,   0),
    (12, 'Mille Bornes',               '2026-09-19T19:30:00-06:00', 1,  0,  5000, NULL, 1,  1,   4,     0,   0,   NULL, NULL, NULL, 1,   0),
    (13, 'Planted',                    '2026-09-14T14:00:00-06:00', 0,  0,  NULL, 3,    1,  1,   3,     0,   0,   NULL, NULL, NULL, 1,   0),
    (14, 'Agricola',                   '2026-09-13T13:00:00-06:00', 0,  0,  NULL, 1,    1,  1,   1,     0,   0,   NULL, NULL, NULL, 1,   0),
    (15, 'Agricola Solo',              '2026-09-12T10:00:00-06:00', 0,  0,  NULL, 1,    0,  1,   1,     0,   0,   NULL, NULL, NULL, 1,   0),
    (16, 'Golf',                       '2026-09-07T17:00:00-06:00', 1,  1,  NULL, 9,    0,  1,   9,     0,   1,   NULL, NULL, NULL, 1,   0),
    (17, 'Five Crowns',                '2026-09-06T20:30:00-06:00', 1,  1,  NULL, 11,   1,  0,   5,     1,   0,   NULL, NULL, NULL, 1,   0),
    (18, 'Spades',                     '2026-08-30T21:00:00-06:00', 1,  0,  500,  NULL, 1,  1,   5,     1,   0,   NULL, NULL, NULL, 1,   0),
    (19, 'Rummy 500',                  '2026-08-23T19:00:00-06:00', 1,  0,  500,  NULL, 0,  1,   5,     1,   2,   NULL, NULL, NULL, 1,   0),
    (20, 'Scrabble',                   '2026-08-16T15:00:00-06:00', 0,  0,  NULL, NULL, 1,  1,   24,    1,   0,   2,    42,   1,    1,   0),
    (21, 'Mexican Train',              '2026-08-09T18:00:00-06:00', 0,  1,  NULL, 13,   1,  0,   13,    0,   0,   0,    60,   1,    1,   0),
    (22, 'Darts 301',                  '2026-08-02T20:00:00-06:00', 0,  0,  301,  NULL, 0,  1,   6,     1,   0,   NULL, NULL, NULL, 1,   0),
    (23, 'Catan',                      '2026-07-26T19:00:00-06:00', 0,  0,  10,   NULL, 0,  0,   1,     0,   0,   NULL, NULL, NULL, 1,   0),
    (24, 'Cribbage',                   '2026-07-19T15:30:00-06:00', 1,  0,  121,  NULL, 0,  0,   12,    0,   0,   NULL, NULL, NULL, 1,   0),
    (25, 'Euchre',                     '2026-07-12T20:00:00-06:00', 1,  0,  10,   NULL, 0,  0,   7,     1,   1,   NULL, NULL, NULL, 1,   0),
    (26, 'Ticket to Ride',             '2026-07-04T16:00:00-06:00', 0,  0,  NULL, NULL, 0,  1,   1,     0,   0,   NULL, NULL, NULL, 1,   0),
    (27, 'Phase 10',                   '2025-12-26T19:00:00-07:00', 1,  1,  NULL, NULL, 0,  0,   6,     0,   2,   0,    80,   5,    0,   0),
    (28, 'Quick Game (no rounds)',     '2025-06-01T12:00:00-06:00', 0,  0,  NULL, NULL, 0,  0,   0,     0,   0,   NULL, NULL, NULL, 0,   0),
    (29, 'Game Night (players gone)',  '2025-03-15T19:00:00-06:00', 0,  0,  NULL, NULL, 0,  0,   2,     0,   0,   1,    20,   1,    0,   0),
    (30, 'Old Hearts Save',            '2024-11-28T20:00:00-07:00', 1,  1,  0,    0,    0,  0,   3,     0,   0,   0,    13,   1,    0,   1);

-- Seats, in table order -----------------------------------------------------------------------------------------------

CREATE TEMP TABLE seed_seats (game INTEGER, players TEXT); -- Player uids in seat order
INSERT INTO seed_seats VALUES
    (1, '1,2,3,4,11'),
    (2, '3,7,13'),
    (3, '14,15'),
    (4, '1,2,3,4,5,6,7,8,9,10'),           -- 10 players, the table scrolls sideways
    (5, '1,2,3,4,12,13,11,16,17,18,19,20,21'), -- 13 players with long and emoji names
    (6, '1,2'),
    (7, '2,9,18,19'),
    (8, '1,2,10,9,16'),
    (9, '1,4,5,9'),
    (10, '4,5,9,10,20'),
    (11, '2,4,7,10'),
    (12, '1,6,17'),
    (13, '3,7,13'),
    (14, '1,2,4,11'),
    (15, '1'),                             -- One player
    (16, '2,5,16,21'),
    (17, '1,3,4,7,9,6'),
    (18, '14,15'),
    (19, '3,6,7'),
    (20, '2,3,8'),
    (21, '1,11,16,17,18'),
    (22, '16,17'),
    (23, '1,2,4,10'),
    (24, '11,1'),
    (25, '14,15'),
    (26, '1,3,4'),
    (27, '1,2,11,16'),
    (28, '18,19,20'),
    (29, '23,24'),                         -- Every player archived, opening it goes to game setup
    (30, '1,4,5,22');                      -- One archived player, whose scores stay in the database but are hidden

-- Scores --------------------------------------------------------------------------------------------------------------
--
-- round is 0-based. input is what was typed, in the calculator's stored form ('x' to multiply, '.' for decimals).
-- A null input is filled in from the score when the game has auto_input. A null score with a note keeps the fill value.

CREATE TEMP TABLE seed_scores (game INTEGER, round INTEGER, player INTEGER, score REAL, input TEXT, note TEXT);
INSERT INTO seed_scores VALUES
    -- Phase 10: lowest wins, the hand winner scores 0, notes track each player's phase
    (1, 0, 1, 0, NULL, 'Phase 1 ✓, went out'), (1, 0, 2, 15, '5+10', 'Phase 1 ✓'), (1, 0, 3, 55, '5x4+10+25', 'Missed phase 1'),
    (1, 0, 4, 30, '5x2+10x2', 'Phase 1 ✓'), (1, 0, 11, 10, NULL, 'Phase 1 ✓'),
    (1, 1, 1, 25, NULL, 'Phase 2 ✓'), (1, 1, 2, 0, NULL, 'Phase 2 ✓, went out'), (1, 1, 3, 40, '5x5+15', 'Phase 1 ✓'),
    (1, 1, 4, 65, '10x4+25', 'Missed phase 2'), (1, 1, 11, 5, NULL, 'Phase 2 ✓'),
    (1, 2, 1, 35, '10+25', NULL), (1, 2, 2, 50, '5x3+10+25', NULL), (1, 2, 3, 0, NULL, 'Phase 2 ✓, went out'),
    (1, 2, 4, 20, '5x4', NULL), (1, 2, 11, 15, NULL, NULL),
    (1, 3, 2, 45, '15+5x6', NULL), (1, 3, 3, 30, '10x3', NULL), (1, 3, 4, 85, '25x2+15+10+10', NULL), (1, 3, 11, 10, NULL, NULL),
    (1, 4, 1, 20, NULL, NULL), (1, 4, 2, 10, NULL, NULL), (1, 4, 3, 60, '25+25+10', NULL), (1, 4, 11, 35, '5x3+10x2', NULL),
    (1, 5, 1, 55, '25+15+5+10', NULL), (1, 5, 3, 25, NULL, NULL), (1, 5, 4, 40, NULL, NULL), (1, 5, 11, 5, NULL, NULL),
    (1, 6, 1, 10, NULL, NULL), (1, 6, 2, 30, NULL, NULL), (1, 6, 3, 15, NULL, NULL), (1, 6, 4, 15, NULL, NULL),

    -- Hand and Foot: partnerships, books + card points + 100 for going out, a team can go negative
    (3, 0, 14, 1415, '500+300+300+215+100', 'Went out: 1 clean, 2 dirty books (meld 50)'), (3, 0, 15, 915, '500+300+180-65', NULL),
    (3, 1, 14, -85, '-85', 'No books, caught holding the foot (meld 90)'), (3, 1, 15, 1660, '500+500+300+260+100', 'Went out'),
    (3, 2, 14, 2220, '500+500+500+300+320+100', 'Went out (meld 120)'), (3, 2, 15, 155, '300+95-240', NULL),

    -- Uno: only the hand winner scores, the total of everyone else's cards
    (4, 0, 2, 123, '20+50+7+3+9+4+2+20+8', NULL),
    (4, 1, 5, 139, '50+50+20+6+4+9', NULL),
    (4, 2, 8, 104, '40+20+20+5+8+3+7+1', NULL),
    (4, 3, 2, 98, '50+20+9+9+6+4', NULL),

    -- Trivia Night: half points, typed on the system keyboard, and one repeating decimal from the calculator
    (5, 0, 1, 7, NULL, NULL), (5, 0, 2, 8.5, NULL, NULL), (5, 0, 3, 6, NULL, NULL), (5, 0, 4, 5.5, NULL, NULL),
    (5, 0, 12, 9, NULL, NULL), (5, 0, 13, 4, NULL, NULL), (5, 0, 11, 10, NULL, NULL), (5, 0, 16, 6.5, NULL, NULL),
    (5, 0, 17, 7, NULL, NULL), (5, 0, 18, 3, NULL, NULL), (5, 0, 19, 8, NULL, NULL), (5, 0, 20, 5, NULL, NULL),
    (5, 0, 21, 6, NULL, NULL),
    (5, 1, 1, 6.5, NULL, NULL), (5, 1, 2, 7, NULL, NULL), (5, 1, 3, 9, NULL, NULL), (5, 1, 4, 4, NULL, NULL),
    (5, 1, 12, 8, NULL, NULL), (5, 1, 13, 5.5, NULL, NULL), (5, 1, 11, 9.5, NULL, NULL), (5, 1, 16, 7, NULL, NULL),
    (5, 1, 17, 6, NULL, NULL), (5, 1, 18, 4.5, NULL, NULL), (5, 1, 19, 7.5, NULL, NULL), (5, 1, 20, 8, NULL, NULL),
    (5, 1, 21, 5, NULL, NULL),
    (5, 2, 1, 8, NULL, NULL), (5, 2, 2, 6, NULL, NULL), (5, 2, 3, 7.5, NULL, NULL), (5, 2, 4, 6, NULL, NULL),
    (5, 2, 12, 10, NULL, NULL), (5, 2, 13, 3, NULL, NULL), (5, 2, 11, 8.5, NULL, NULL), (5, 2, 16, 5, NULL, NULL),
    (5, 2, 17, 9, NULL, NULL), (5, 2, 18, 6, NULL, NULL), (5, 2, 19, 6.5, NULL, NULL), (5, 2, 20, 7, NULL, NULL),
    (5, 2, 21, 4.5, NULL, NULL),
    (5, 3, 1, 3.3333333333333335, '10/3', NULL), (5, 3, 2, 9, NULL, NULL), (5, 3, 3, 8, NULL, NULL), (5, 3, 4, 7.5, NULL, NULL),
    (5, 3, 12, 7, NULL, NULL), (5, 3, 13, 6, NULL, NULL), (5, 3, 11, 9, NULL, NULL), (5, 3, 16, 8, NULL, NULL),
    (5, 3, 17, 5.5, NULL, NULL), (5, 3, 18, 5, NULL, NULL), (5, 3, 19, 9, NULL, NULL), (5, 3, 20, 6.5, NULL, NULL),
    (5, 3, 21, 7, NULL, NULL),

    -- Gin Rummy: only the hand winner scores (knock difference, gin and undercut bonuses), 94-92 near the goal
    (6, 0, 1, 29, '41-12', 'Knock'), (6, 1, 2, 63, '25+38', 'Gin'), (6, 2, 1, 9, '18-9', 'Knock'),
    (6, 3, 2, 29, '25+4', 'Undercut'), (6, 4, 1, 56, '25+31', 'Gin'),

    -- Wingspan: one final scoring, each "round" is a scoring category (named in Kim's notes)
    (7, 0, 2, 56, '9+5+7+4+3+6+8+2+5+4+3', 'Birds'), (7, 0, 9, 49, '6+8+9+5+4+3+7+2+5', NULL),
    (7, 0, 18, 51, '4+3+5+6+9+2+1+7+5+3+4+2', NULL), (7, 0, 19, 42, '8+7+6+5+4+9+3', NULL),
    (7, 1, 2, 10, '7+3', 'Bonus cards'), (7, 1, 9, 6, NULL, NULL), (7, 1, 18, 8, '4+4', NULL), (7, 1, 19, 9, NULL, NULL),
    (7, 2, 2, 18, '4+3+5+6', 'End-of-round goals'), (7, 2, 9, 15, '5+5+2+3', NULL), (7, 2, 18, 10, '1+2+3+4', NULL),
    (7, 2, 19, 15, '3+4+1+7', NULL),
    (7, 3, 2, 14, NULL, 'Eggs'), (7, 3, 9, 21, NULL, NULL), (7, 3, 18, 9, NULL, NULL), (7, 3, 19, 17, NULL, NULL),
    (7, 4, 2, 3, NULL, 'Cached food'), (7, 4, 18, 11, NULL, NULL), (7, 4, 19, 2, NULL, NULL),
    (7, 5, 2, 6, NULL, 'Tucked cards'), (7, 5, 9, 4, NULL, NULL), (7, 5, 18, 12, NULL, NULL), (7, 5, 19, 8, NULL, NULL),

    -- Ticket to Ride: routes (1/2/4/7/10/15 by length), tickets (failed ones subtract), then the longest path bonus
    (8, 0, 1, 50, '15+10+7+7+4+4+2+1', 'Routes'), (8, 0, 2, 49, '15+15+10+4+2+2+1', NULL),
    (8, 0, 10, 51, '10+10+7+7+7+4+4+2', NULL), (8, 0, 9, 40, '15+7+4+4+4+2+2+1+1', NULL), (8, 0, 16, 34, '10+7+7+4+2+2+2', NULL),
    (8, 1, 1, 35, '20+13+8-6', 'Destination tickets'), (8, 1, 2, 18, '21+11-9-5', NULL), (8, 1, 10, 27, '12+8+7', NULL),
    (8, 1, 9, 55, '22+20+10+9+5-11', NULL), (8, 1, 16, -15, '-13-8+6', 'Failed two tickets'),
    (8, 2, 9, 10, NULL, 'Longest path'),

    -- Hearts: lowest wins, 26 points a hand, Zed shoots the moon in round 3, Bob passes 100 and Zed/Priya tie for the lead
    (9, 0, 1, 3, NULL, NULL), (9, 0, 4, 13, NULL, NULL), (9, 0, 5, 6, NULL, NULL), (9, 0, 9, 4, NULL, NULL),
    (9, 1, 4, 8, NULL, NULL), (9, 1, 5, 17, NULL, NULL), (9, 1, 9, 1, NULL, NULL),
    (9, 2, 1, 26, NULL, NULL), (9, 2, 4, 26, NULL, NULL), (9, 2, 9, 26, NULL, NULL),
    (9, 3, 1, 5, NULL, NULL), (9, 3, 4, 2, NULL, NULL), (9, 3, 5, 14, NULL, NULL), (9, 3, 9, 5, NULL, NULL),
    (9, 4, 1, 9, NULL, NULL), (9, 4, 4, 13, NULL, NULL), (9, 4, 5, 3, NULL, NULL), (9, 4, 9, 1, NULL, NULL),
    (9, 5, 1, 2, NULL, NULL), (9, 5, 4, 18, NULL, NULL), (9, 5, 9, 6, NULL, NULL),
    (9, 6, 1, 11, NULL, NULL), (9, 6, 4, 4, NULL, NULL), (9, 6, 5, 7, NULL, NULL), (9, 6, 9, 4, NULL, NULL),
    (9, 7, 1, 1, NULL, NULL), (9, 7, 4, 17, NULL, NULL), (9, 7, 5, 4, NULL, NULL), (9, 7, 9, 4, NULL, NULL),

    -- Skull King: 20 per trick bid correctly, -10 per trick off, zero bids worth 10 x the round
    (10, 0, 4, 20, NULL, NULL), (10, 0, 5, -10, NULL, NULL), (10, 0, 9, 10, NULL, 'Zero bid'), (10, 0, 10, 20, NULL, NULL),
    (10, 0, 20, -10, NULL, NULL),
    (10, 1, 4, 40, NULL, NULL), (10, 1, 5, 20, NULL, NULL), (10, 1, 9, -20, NULL, NULL), (10, 1, 10, 20, NULL, NULL),
    (10, 1, 20, 20, NULL, NULL),
    (10, 2, 4, -10, NULL, NULL), (10, 2, 5, 70, '40+30', 'Bonus: Skull King took a pirate'), (10, 2, 9, 20, NULL, NULL),
    (10, 2, 10, -20, NULL, NULL), (10, 2, 20, 30, NULL, 'Zero bid'),
    (10, 3, 4, 60, '3x20', NULL), (10, 3, 5, -20, NULL, NULL), (10, 3, 9, 40, '20+20', NULL), (10, 3, 10, 40, NULL, NULL),
    (10, 3, 20, -40, NULL, 'Zero bid missed'),
    (10, 4, 4, 40, NULL, NULL), (10, 4, 5, 20, NULL, NULL), (10, 4, 9, -10, NULL, NULL), (10, 4, 10, 60, NULL, NULL),
    (10, 4, 20, 50, NULL, 'Zero bid'),

    -- Alhambra: three scorings, more places score each time, ties split the points rounding down
    (11, 0, 2, 8, '1+4+3', 'Pavilion, chambers, wall of 3'), (11, 0, 4, 9, '2+5+2', NULL), (11, 0, 7, 9, '3+6', NULL),
    (11, 0, 10, 2, NULL, 'Wall only'),
    (11, 1, 2, 25, '8+2+10+5', NULL), (11, 1, 4, 18, '9+3+6', NULL), (11, 1, 7, 24, '12+1+4+7', NULL),
    (11, 1, 10, 31, '13+11+4+3', NULL),
    (11, 2, 2, 52, '16+17+8+2+9', NULL), (11, 2, 4, 60, '18+9+3+19+11', NULL),
    (11, 2, 7, 45, '13+20+12', 'Tied Bob for the seraglio: (18+9)/2 rounded down'), (11, 2, 10, 44, '21+3+1+14+5', NULL),

    -- Mille Bornes: distance plus trip, safe trip, shut-out, safety, and coup fourré bonuses, first to 5000
    (12, 0, 1, 1400, '700+400+300', 'Trip complete, safe trip'), (12, 0, 6, 550, '450+100', NULL),
    (12, 0, 17, 1000, '600+100+300', 'Coup fourré'),
    (12, 1, 1, 425, '325+100', NULL), (12, 1, 6, 1600, '700+400+500', 'Shut-out'), (12, 1, 17, 500, NULL, NULL),
    (12, 2, 1, 1800, '700+400+300+100+300', 'Safe trip, coup fourré'), (12, 2, 6, 375, NULL, NULL),
    (12, 2, 17, 725, '625+100', NULL),
    (12, 3, 1, 1500, '700+400+300+100', NULL), (12, 3, 6, 475, NULL, NULL), (12, 3, 17, 1000, '600+100+300', NULL),

    -- Planted: end-game categories (plants, propagation jars, decorations), alice and Zoë tie
    (13, 0, 3, 25, '7+5+6+4+3', 'Plants'), (13, 0, 7, 27, '6+6+5+7+2+1', NULL), (13, 0, 13, 19, '4+5+3+7', NULL),
    (13, 1, 3, 3, NULL, 'Propagation'), (13, 1, 7, 1, NULL, NULL), (13, 1, 13, 5, NULL, NULL),
    (13, 2, 3, 5, '3+2', 'Decorations'), (13, 2, 7, 5, '1+3+1', 'Tie: Zoë wins on growth tokens'),
    (13, 2, 13, 7, '3+3+1', NULL),

    -- Agricola: every category in one calculation, -1 per unused space and -3 per begging card
    (14, 0, 1, 40, '2+3+1+1+2+1+1-2+2+5x2+4x3+4+3', NULL),
    (14, 0, 2, 35, '3+2+2+1+1+2+1+5+5x3-3+6', NULL),
    (14, 0, 4, 9, '1-1+1+1-1-1-1-4+3+3x2+3x3-3x2+2', '2 begging cards'),
    (14, 0, 11, 62, '4+4+4+3+3+3+4+4+5x2+5x3+8', NULL),
    (15, 0, 1, 43, '2+3+2+1+3+2+1-3+2+4x2+5x3+7', NULL),

    -- Golf: 9 holes, lowest wins, jokers are -2
    (16, 0, 2, 12, '10+2', NULL), (16, 1, 2, 5, NULL, NULL), (16, 2, 2, -2, NULL, NULL), (16, 3, 2, 8, NULL, NULL),
    (16, 4, 2, 3, NULL, NULL), (16, 5, 2, 0, '0', NULL), (16, 6, 2, 14, NULL, NULL), (16, 7, 2, 4, NULL, NULL),
    (16, 8, 2, 6, NULL, NULL),
    (16, 0, 5, 20, NULL, NULL), (16, 1, 5, 9, NULL, NULL), (16, 2, 5, 11, NULL, NULL), (16, 3, 5, 2, NULL, NULL),
    (16, 4, 5, 7, NULL, NULL), (16, 5, 5, -4, '-2-2', NULL), (16, 6, 5, 5, NULL, NULL), (16, 7, 5, 13, NULL, NULL),
    (16, 8, 5, 1, NULL, NULL),
    (16, 0, 16, 7, NULL, NULL), (16, 1, 16, 15, NULL, NULL), (16, 2, 16, 3, NULL, NULL), (16, 3, 16, -1, NULL, NULL),
    (16, 4, 16, 10, NULL, NULL), (16, 5, 16, 6, NULL, NULL), (16, 6, 16, 2, NULL, NULL), (16, 7, 16, 9, NULL, NULL),
    (16, 8, 16, 8, NULL, NULL),
    (16, 0, 21, 25, NULL, NULL), (16, 1, 21, 4, NULL, NULL), (16, 2, 21, 6, NULL, NULL), (16, 3, 21, 12, NULL, NULL),
    (16, 4, 21, -2, NULL, NULL), (16, 5, 21, 8, NULL, NULL), (16, 6, 21, 10, NULL, NULL), (16, 7, 21, 3, NULL, NULL),
    (16, 8, 21, 11, NULL, NULL),

    -- Five Crowns: 11 rounds (3s wild up to kings wild), lowest wins, whoever goes out scores 0
    (17, 0, 1, 0, NULL, 'Went out (3s wild)'), (17, 0, 3, 8, NULL, NULL), (17, 0, 4, 13, NULL, NULL),
    (17, 0, 7, 3, NULL, NULL), (17, 0, 9, 20, NULL, NULL), (17, 0, 6, 5, NULL, NULL),
    (17, 1, 1, 12, NULL, NULL), (17, 1, 4, 6, NULL, NULL), (17, 1, 7, 9, NULL, NULL), (17, 1, 9, 4, NULL, NULL),
    (17, 1, 6, 25, NULL, NULL),
    (17, 2, 1, 21, NULL, NULL), (17, 2, 3, 17, NULL, NULL), (17, 2, 7, 14, NULL, NULL), (17, 2, 9, 9, NULL, NULL),
    (17, 2, 6, 11, NULL, NULL),
    (17, 3, 1, 5, NULL, NULL), (17, 3, 3, 30, NULL, NULL), (17, 3, 4, 10, NULL, NULL), (17, 3, 9, 18, NULL, NULL),
    (17, 3, 6, 8, NULL, NULL),
    (17, 4, 1, 16, NULL, NULL), (17, 4, 3, 9, NULL, NULL), (17, 4, 4, 22, NULL, NULL), (17, 4, 7, 7, NULL, NULL),
    (17, 4, 6, 41, NULL, NULL),

    -- Spades: partnerships, 10 per trick bid plus 1 per bag, set bids and 10 bags lose points
    (18, 0, 14, 51, '50+1', 'Bid 5, took 6'), (18, 0, 15, 61, '60+1', 'Bid 6, took 7'),
    (18, 1, 14, -40, '-40', 'Bid 4, took 3: set'), (18, 1, 15, 82, '80+2', 'Bid 8, took 10'),
    (18, 2, 14, 140, '100+40', 'Nil made, bid 4'), (18, 2, 15, -70, '-70', 'Bid 7, took 5: set'),
    (18, 3, 14, 63, '60+3', NULL), (18, 3, 15, 40, NULL, NULL),
    (18, 4, 14, 70, NULL, NULL), (18, 4, 15, -48, '50+2-100', '10 bags: -100'),

    -- Rummy 500: melds minus cards left in hand, so hands and totals can go negative
    (19, 0, 3, 70, '85-15', NULL), (19, 0, 6, -15, '40-55', NULL), (19, 0, 7, 115, '120-5', NULL),
    (19, 1, 3, -35, '-35', NULL), (19, 1, 6, 85, '95-10', NULL), (19, 1, 7, 40, '60-20', NULL),
    (19, 2, 3, 150, NULL, NULL), (19, 2, 6, -60, '5x2-70', NULL), (19, 2, 7, 50, '75-25', NULL),
    (19, 3, 3, 15, '45-30', NULL), (19, 3, 6, 130, NULL, NULL), (19, 3, 7, -15, NULL, NULL),
    (19, 4, 3, 105, NULL, NULL), (19, 4, 6, -25, '20-45', NULL), (19, 4, 7, 90, NULL, NULL),

    -- Scrabble: a long game (vertical scrolling), notes hold the words, letters left over subtract at the end
    (20, 0, 2, 22, '(1+1+3+4)x2+4', 'HOVEL'),
    (20, 3, 3, 70, '(1+1+1+2+1+1+3)x2+50', 'RETAINED, bingo'),
    (20, 7, 8, 33, '10x3+3', 'QI and QIS'),
    (20, 23, 3, -8, '-8', 'Letters left over'),

    -- Mexican Train: 13 rounds from double 12 down to double blank, lowest wins (fill values for the rest)
    (21, 0, 1, NULL, NULL, 'Double 12'), (21, 6, 1, NULL, NULL, 'Double 6'), (21, 12, 1, NULL, NULL, 'Double blank'),

    -- Darts 301: first to 301, two inputs that can't be calculated (saved as 0 and shown in red)
    (22, 0, 16, 60, '20+20+20', NULL), (22, 0, 17, 45, '3x5+20+10', NULL),
    (22, 1, 16, 81, '3x20+20+1', NULL), (22, 1, 17, 57, '3x19', NULL),
    (22, 2, 16, 26, '20+5+1', NULL), (22, 2, 17, 100, '3x20+2x20', NULL),
    (22, 3, 16, 41, '20+20+1', NULL), (22, 3, 17, 0, '3x20+', NULL),
    (22, 4, 16, 0, '50/0', NULL), (22, 4, 17, 60, '2x20+20', NULL),
    (22, 5, 16, 45, '25+20', NULL), (22, 5, 17, 21, '(20+1)', NULL),

    -- Catan: the goal of 10 victory points is reached in the only round
    (23, 0, 1, 10, NULL, NULL), (23, 0, 2, 8, NULL, NULL), (23, 0, 4, 7, NULL, NULL), (23, 0, 10, 9, NULL, NULL),

    -- Cribbage: first to 121
    (24, 0, 11, 12, NULL, NULL), (24, 1, 11, 9, NULL, NULL), (24, 2, 11, 18, NULL, NULL), (24, 3, 11, 7, NULL, NULL),
    (24, 4, 11, 10, NULL, NULL), (24, 5, 11, 14, NULL, NULL), (24, 6, 11, 6, NULL, NULL), (24, 7, 11, 11, NULL, NULL),
    (24, 8, 11, 9, NULL, NULL), (24, 9, 11, 16, NULL, NULL), (24, 10, 11, 4, NULL, NULL), (24, 11, 11, 7, NULL, NULL),
    (24, 0, 1, 8, NULL, NULL), (24, 1, 1, 15, NULL, NULL), (24, 2, 1, 6, NULL, NULL), (24, 3, 1, 13, NULL, NULL),
    (24, 4, 1, 9, NULL, NULL), (24, 5, 1, 7, NULL, NULL), (24, 6, 1, 17, NULL, NULL), (24, 7, 1, 5, NULL, NULL),
    (24, 8, 1, 12, NULL, NULL), (24, 9, 1, 8, NULL, NULL), (24, 10, 1, 10, NULL, NULL), (24, 11, 1, 4, NULL, NULL),

    -- Euchre: partnerships to 10, one team scores 1, 2, or 4 each hand
    (25, 0, 14, 1, NULL, NULL), (25, 1, 15, 2, NULL, NULL), (25, 2, 14, 2, NULL, NULL), (25, 3, 15, 1, NULL, NULL),
    (25, 4, 14, 4, NULL, NULL), (25, 5, 14, 1, NULL, NULL), (25, 6, 15, 1, NULL, NULL),

    -- Ticket to Ride (an older game with the same name): everything in one calculation, with parentheses
    (26, 0, 1, 53, '1+2+4+7+10+15+12-8+10', NULL), (26, 0, 3, 36, '(15+10)x2+7-21', NULL), (26, 0, 4, 54, '4x7+2x3+20', NULL);

-- Expand the seed into the app's tables -------------------------------------------------------------------------------

CREATE TEMP TABLE nums (n INTEGER);
WITH RECURSIVE r(n) AS (SELECT 0 UNION ALL SELECT n + 1 FROM r WHERE n < 99) INSERT INTO nums SELECT n FROM r;

-- One row per seat, splitting the comma separated player lists
CREATE TEMP TABLE seats (game INTEGER, pos INTEGER, player INTEGER);
WITH RECURSIVE split(game, pos, player, rest) AS (
    SELECT game, -1, NULL, players || ',' FROM seed_seats
    UNION ALL
    SELECT game, pos + 1, CAST(substr(rest, 1, instr(rest, ',') - 1) AS INTEGER), substr(rest, instr(rest, ',') + 1)
    FROM split WHERE rest <> ''
)
INSERT INTO seats SELECT game, pos, player FROM split WHERE pos >= 0;

INSERT INTO players (uid, name, archived) SELECT uid, name, archived FROM seed_players;

INSERT INTO games (uid, name, date, hasDealer, showRounds, reversedScoring, maxScore, maxRounds, showRoundNotes, useCalculator)
SELECT uid, name, date, has_dealer, legacy, reversed, max_score, max_rounds, show_notes, calculator FROM seed_games;

INSERT INTO game_player_entity (gameId, playerId, playerPosition) SELECT game, player, pos FROM seats;

-- Round ids are game * 100 + round, the dealer moves one seat each round
INSERT INTO rounds (uid, game_id, dealer_id, round_number)
SELECT g.uid * 100 + n.n, g.uid,
       CASE
           WHEN g.legacy = 1 THEN 0
           WHEN g.has_dealer = 1 THEN (
               SELECT s.player FROM seats s
               WHERE s.game = g.uid
                 AND s.pos = (g.first_dealer + n.n) % (SELECT COUNT(*) FROM seats c WHERE c.game = g.uid)
           )
       END,
       n.n
FROM seed_games g JOIN nums n ON n.n < g.played + g.open_round;

INSERT INTO scores (player_id, round_id, score, score_data, score_input)
SELECT player, round_id, score, note,
       CASE
           WHEN input IS NOT NULL THEN input
           WHEN auto_input = 1 AND score <> 0 THEN
               CASE WHEN score = CAST(score AS INTEGER) THEN CAST(CAST(score AS INTEGER) AS TEXT) ELSE CAST(score AS TEXT) END
       END
FROM (
    SELECT s.player, r.uid AS round_id, g.auto_input, x.input, COALESCE(x.note, '') AS note,
           COALESCE(
               x.score,
               CASE
                   WHEN g.fill_min IS NOT NULL AND r.round_number < g.played THEN
                       g.fill_min + ((r.round_number + 1) * 7919 + (s.pos + 1) * 104729 + g.uid * 31)
                           % ((g.fill_max - g.fill_min) / g.fill_step + 1) * g.fill_step
                   ELSE 0
               END
           ) AS score
    FROM rounds r
    JOIN seed_games g ON g.uid = r.game_id
    JOIN seats s ON s.game = g.uid
    LEFT JOIN seed_scores x ON x.game = g.uid AND x.round = r.round_number AND x.player = s.player
    ORDER BY r.uid, s.pos
);

COMMIT;
