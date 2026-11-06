# 35 Domino (Java/Android) - Project Manual

| | |
|---|---|
| **Name** | Ahmed Hira |
| **Project** | 35 Domino (Java/Android) |
| **Class** | CMPS 366 - Organization of Programming Languages |
| **Date** | October 31, 2026 |

This file contains everything the submission guidelines ask for in the manual: how to run the program, the bug report, the feature report, a description of the classes and data structures, the log, the generative-AI disclosure, and the screenshots.

---

## 1. How to Run

1. Open the project folder (the one containing `settings.gradle`) in Android Studio and let Gradle sync.
2. Run the `app` configuration on a tablet or a phone held in landscape (Android 8.0 / API 26 or newer).
3. On the start screen, either type the tournament score and tap **Start new tournament**, or tap **Choose a saved game…**.

**Serialization files.** The app keeps saved games in its own folder on the device:
`Android/data/edu.ramapo.ahira.domino/files/` (the full path is shown on the start screen). On first launch it copies the sample files `test`, `case1`, `case2`, `case3`, `Case4`, and `007` there. To load another file (for example, one downloaded for the demonstration), either copy it into that folder over USB or tap **Browse device…** in the resume dialog and pick it from Downloads.

**Projecting the screen for the demonstration:** `scrcpy` (or Vysor) with the device connected over USB.

---

## 2. Bug Report

**The program has no known bugs.**

The game logic (every class in the `model` package) was tested before submission:

- All six sample save files load, and saving then reloading each one gives back identical text.
- 400 complete computer-assisted tournaments (926 rounds) were simulated with the save file written and reloaded at random points mid-round. After every move the test checked that all 28 tiles were present exactly once, that every tile on the table touches its neighbour with matching pips, and that the layout survives a save/reload unchanged. No check ever failed.
- Each sample save file was played to the end of the round by both this Java version and my C++ version, with the same moves for the human. The computer made the same move every turn in both versions, and both reported the same bonus and the same round and tournament scores.

Design decisions where the rules could be read more than one way (not defects):

1. **Spinner arms.** Once the first double is placed it becomes the spinner, and its up (U) and down (D) ends are open immediately. An empty arm adds the spinner's pips to the open-end total only when the spinner is not itself the left or right end of the line (otherwise its pips are already counted as that end). This is the same rule as in my C++ version.
2. **Drawing.** The human draws one tile per tap of **Draw from boneyard**. The button is only offered when no tile in the hand fits, so drawing stops as soon as a playable tile is drawn.

---

## 3. Feature Report

### Missing features

None.

### Extra features

- **Graphical board.** Tiles are drawn with real pip dots. Doubles are cross-wise, the spinner is outlined in gold, its arms grow above and below it, and each open end carries a badge with its letter and the pips showing there. The picture rescales to fit any screen, even with all 28 tiles down.
- **Invalid moves are impossible to enter.** After you tap a tile, only the ends where it fits get a button. **Draw** appears only when nothing fits, **Pass** only when nothing fits and the boneyard is empty, and **Help** only on your turn. The target score is chosen from a list containing just 3 and 5.
- **Help selects the tile for you.** Asking for help selects the recommended tile in your hand, so the matching end button is already on screen.
- **Resume from anywhere on the device.** Besides the app's own folder, any file can be opened with the system file picker. Sample save files come with the app.
- **Protection against losing a game.** Leaving the game screen asks for confirmation if the round has not been saved. If Android closes the screen in the background, the unfinished round is restored from save-file text when the screen is rebuilt.
- **Stricter save-file reading.** A save file with an empty hand (`Hand:`) or an empty boneyard (`Boneyard:`) loads correctly. My C++ version mistook those lines for the start of a new player section.

---

## 4. Classes and Data Structures

The program uses Model-View-Controller. The three packages under `app/src/main/java/edu/ramapo/ahira/domino/` are `model`, `view`, and `controller`.

### Model (`model` package: plain Java, no Android code)

| Class | Holds | Purpose |
|---|---|---|
| `Tile` | two `int` pip counts (immutable) | One domino. Knows its sum, whether it is a double, how to flip, and its `"2-5"` text; `equals` treats `2-5` and `5-2` as the same tile. |
| `Move` | a `Tile`, an end letter, a valid flag | One placement request passed between the players, the layout, and the round. |
| `Hand` | `ArrayList<Tile>` | The tiles one player holds. Rejects duplicates, removes tiles, totals the pips for the bonus. |
| `Boneyard` | `ArrayList<Tile>` (top first) | The stock. Builds the 28-tile set, shuffles, draws from the top, returns tiles to the bottom, restores from a save file (rejecting duplicates). |
| `Layout` | three `ArrayList<Tile>` (main line, up arm, down arm) and the spinner's index | The table. Checks legality, places tiles turned the right way, tracks the spinner, computes the open-end total, converts to and from `L 5-3 [D 3-3 U] 3-2 R`. |
| `Recommendation` | an action (PLACE, DRAW, PASS), a `Move`, a reason | What the strategy suggests and why. |
| `GameLog` | `ArrayList<String>` | Every message of the game, in order. |
| `PlayerState`, `GameState` | the fields of a save file; `LinkedHashMap<String, PlayerState>` keyed by player name | A saved game in memory. |
| `Serializer` | (static functions only) | Turns a `GameState` into the course's save-file text and back, rejecting malformed text. |
| `Round` | two `Player` references, `Layout`, `Boneyard`, whose turn it is, pass count | One round: deal, first player, the steps of a turn (draw, pass, place), scoring, the end-of-round bonus. |
| `Tournament` | `Human`, `Computer`, the goal, the current `Round`, the `GameLog` | The whole session. **The only class the controller calls to change the game.** |

**Player hierarchy (inheritance and overriding):**

```
Player  (abstract: name, Hand, round score, tournament score)
 |-- Human     overrides chooseMove (returns the move picked on screen), isHuman, explainLastMove
 '-- Computer  overrides chooseMove (uses the strategy), isHuman, explainLastMove (gives its reason)
```

`Round` holds both players as `Player` and calls `chooseMove` without knowing which kind of player it has. The computer's strategy is the static function `Computer.recommend`. The human's help calls the same function, so the advice and the computer's own play always agree.

**Encapsulation.** All data members are private. Selectors hand out copies (`getHand()`, `getLayout()`, `getBoneyard()`, `getTiles()`). The mutators of `Player`, `Human`, `Computer`, and `Round` are package-private, so code outside the model can read the game but can change it only through `Tournament`. Every mutator validates its input and returns `false` (or `null`) without changing anything when the input is invalid.

**Composition:**

```
Tournament
 |-- Human, Computer        (for the whole tournament, so scores carry over)
 |-- GameLog
 '-- Round                  (a new one each round)
       |-- Player x2        (references to the Human and the Computer)
       |-- Layout
       '-- Boneyard
```

### View (`view` package)

| Class | Purpose |
|---|---|
| `TilePainter` | Draws one tile with pip dots, lying down or standing up, optionally outlined. |
| `BoardView` | Custom `View` that draws the layout. `update(Layout)` gives it a new copy of the table and it redraws itself. |
| `HandView` | Custom `View` that draws a wrapping row of tiles: the human's numbered, tappable hand; the computer's hand; the boneyard with its top tile marked. `update(tiles, selected)` redraws it. |

### Controller (`controller` package)

| Class | Purpose |
|---|---|
| `MainActivity` | Start screen: reads and validates the tournament score, or lists the save files and opens the chosen one. |
| `GameActivity` | Game screen. Each event handler calls one `Tournament` function, then `refresh()` redraws every view from the model. |
| `SaveFileStore` | Reads and writes save files on the device, and installs the sample files. Kept out of the model so the model has no Android code. |

### Sequence of calls (for the demonstration)

**The human asks for help:**
`Help` button → `GameActivity.onHelpClicked()` → `Tournament.getHelp()` → `Round.recommendForCurrent()` → `Computer.recommend(hand, layout, target)` → returns a `Recommendation` → the controller selects the recommended tile, calls `refresh()`, and shows `Recommendation.describe()` (tile, end, and rationale) in a dialog. The advice is also written to the game log.

**The human makes a move:**
tap a tile → `HandView` → `GameActivity.onHumanTileTapped(index)` → `refresh()` shows only the ends where that tile fits → tap an end → `GameActivity.onEndChosen(end)` → `Tournament.playHumanMove(move)` → `Human.setPendingMove(move)`, then `Round.playCurrentMove()` → `Human.chooseMove()` returns the move → `Layout.place()`, `Player.removeTile()`, score the open-end total, next player → back in the controller, `afterTurnStep()` → `refresh()`: `BoardView.update`, `HandView.update`, scores, buttons, log; if the round ended, show the result.

**The computer's strategy** (`Computer.recommend`):
1. List every legal (tile, end) pair.
2. If any pair scores, play the highest score; ties go to a double, then to the heavier tile.
3. Otherwise, if a double can be played, play the heaviest double (doubles are hard to place later and count both pips in the bonus), at the end that leaves the opponent the smallest open-end total.
4. Otherwise play the heaviest tile (to lighten the hand for the bonus), at the end that leaves the smallest open-end total.

---

## 5. Log

### 2026-10-31 - Java/Android port of the C++ project (about 1 hour, with Claude Code)

**What was done:**
- Read the C++ source (newest copy, `helloworld/Ahmed_Hira/src`), the Java guidelines, the submission guidelines, and the Java rubric.
- Ported every model class to Java with the same class structure and logic: `Tile`, `Move`, `Hand`, `Boneyard`, `Layout`, `Player`/`Human`/`Computer`, `Round`, `Tournament`, `GameState`/`PlayerState`, `Serializer`. Changes needed for Java and a GUI:
  - Functions that returned results through C++ reference parameters now return objects (`Tile.fromString` returns a `Tile` or `null`; `Computer.recommend` returns a `Recommendation`).
  - The blocking loop in C++ `Round::play()` became separate steps (`drawForCurrent`, `passCurrent`, `playCurrentMove`, `playAutomaticTurn`) that the GUI calls one tap at a time.
  - `Human.chooseMove` returns the move picked on screen instead of reading the keyboard.
  - `Serializer` works on text; the Android file I/O is in `SaveFileStore`.
  - Player and round mutators became package-private so the controller can only change the game through `Tournament`.
- Wrote the view (`TilePainter`, `BoardView`, `HandView`) and the controller (`MainActivity`, `GameActivity`, `SaveFileStore`), the screen layouts, strings, colours, and theme.
- Tested the model with a test harness (not part of the app): all sample files load and save back identically; 926 simulated rounds with random save/reload checked all 28 tiles, the layout's pip chain, and the save round trip after every move; each sample file was played to the end of the round in both the C++ and Java versions, with the same moves and scores.

**Bugs found:**
- The C++ loader treated `Hand:` (empty hand) and `Boneyard:` (empty boneyard) as player section headers. Fixed in the Java `Serializer` (`isSectionHeader`).

**Next:** build and run on the tablet, take the screenshots for Section 7, fill in the rubric.

### Total time spent on the project

| Session | Hours |
|---|---|
| 2026-10-31 Java/Android port | 1.0 |
| **Total** | **1.0 hours** |

---

## 6. Generative AI Assistance

**Generative AI (Claude Code, Anthropic) wrote this Java/Android port.** The program's design, game rules, and strategy come from my C++ project, which Claude translated to Java and extended with an Android GUI. Every function header whose Reference field says "Ported from my C++ version with Claude" was produced this way.

| Function / class / file | Prompt used | Nature of help |
|---|---|---|
| All `model` classes: `Tile`, `Move`, `Hand`, `Boneyard`, `Layout`, `Player`, `Human`, `Computer`, `Recommendation`, `GameLog`, `PlayerState`, `GameState`, `Serializer`, `Round`, `Tournament` | "can you recreate the same C++ project domino I had using the attached above." (attachments: *OPL Project Submission Guidelines*, *Guidelines for Writing Java/Android Programs*, *Rubric for Assessing 35Domino in Java/Android*) | **Coding**: translation of my C++ classes to Java; redesign of the round's game loop into GUI-driven steps. **Debugging**: found and fixed the empty `Hand:`/`Boneyard:` loading bug. **Documentation**: function headers. |
| `view` classes: `TilePainter`, `BoardView`, `HandView` | same prompt | **Design and coding**: the graphical board, hands, and boneyard. |
| `controller` classes: `MainActivity`, `GameActivity`, `SaveFileStore` | same prompt | **Design and coding**: the screens, event handling, save/load on Android. |
| `res/` layouts, strings, colours, theme, icon; `AndroidManifest.xml`; Gradle files | same prompt | **Coding**: Android project setup and screen layouts. |
| Test harness (not submitted) | same prompt | **Testing**: simulated games and the C++/Java comparison described in Sections 2 and 5. |
| This manual | same prompt | **Documentation**. |

---

## 7. Screen Shots

The guidelines require these screenshots to be embedded here as JPG or GIF files of at most 100 KB each. Take them from the running app (for example with `scrcpy` or the device's screenshot button), save them in `doc/images/`, and they will appear in place of the links below. If you convert this manual to Word or PDF, paste the images in instead.

| # | Screen | What to capture |
|---|---|---|
| 7.1 | ![Tournament score](images/01-tournament-score.jpg) | Start screen with a tournament score typed in. |
| 7.2 | ![Resume](images/02-resume.jpg) | The "Choose a saved game" list. |
| 7.3 | ![Target](images/03-target.jpg) | The "choose the target score" dialog (3 or 5). |
| 7.4 | ![First player](images/04-first-player.jpg) | The "Who plays first?" dialog. |
| 7.5 | ![Whole game](images/05-overview.jpg) | **Representative screenshot**: board with a spinner and arms, both hands, boneyard, scores, next player, log. |
| 7.6 | ![Pick tile](images/06-pick-tile.jpg) | A tile selected in the hand, with the end buttons where it fits. |
| 7.7 | ![Help](images/07-help.jpg) | **Help** dialog showing the recommended tile, end, and rationale. |
| 7.8 | ![Draw](images/08-draw.jpg) | The **Draw from boneyard** button (no tile fits). |
| 7.9 | ![Pass](images/09-pass.jpg) | The **Pass turn** button (no tile fits, boneyard empty). |
| 7.10 | ![Computer move](images/10-computer-move.jpg) | Log after **Play computer's turn**, showing the move and its reason. |
| 7.11 | ![Save](images/11-save.jpg) | The **Save & quit** file-name dialog. |
| 7.12 | ![Round over](images/12-round-over.jpg) | The round-over (or tournament-over) dialog. |
