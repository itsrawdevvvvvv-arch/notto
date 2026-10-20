# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.8.0] - 2026-10-20

### Added
- **Calendar** — a new entry in the **+** menu. A month view in the app's own style: tap any day to mark it done (the day fills and a line is drawn through it), tap again to unmark. The header shows how many days you've marked, with a progress bar that fills as you go. Arrows move between months, and the badge button jumps back to today. Everything animates on the compositor (transform/opacity only), so there is no layout jitter. Marks are saved on the device.

### Fixed
- **Calendar, Clock and Group now follow the light theme.** They were hardcoded dark, so in light mode they stayed black while the rest of the app went light. Their contents were already theme-aware — only the screen background was wrong. Clock chips and tabs got light variants too.

## [1.7.3] - 2026-10-19

### Fixed
- **Closing a note is smooth now.** Two separate causes were behind the jitter: (1) every time the home grid was rebuilt its cards were created with no position and then moved into place, so they visibly flew in from the top-left corner and overlapped on the way — a freshly built grid is now laid out in the same frame it is created, with transitions off for that first pass; (2) the whole grid was rebuilt 60 ms after the close, in the middle of the editor's slide-away animation — the rebuild now waits for the animation to finish and **is skipped entirely if the note wasn't changed**.

### Changed
- Cards use CSS layout containment, which cuts the cost of re-laying out the grid.

## [1.7.2] - 2026-10-19

### Fixed
- **Notes restored from another app are visible again.** A backup made elsewhere usually has no card colour on each note, and a card without a colour draws no background — so the dark card text sat on the dark page and the notes looked empty in dark mode (they happened to still be readable in light mode). Restored notes are now given a valid card colour, and every note is checked for the fields the app needs (colour, title, checklist items, timestamp) both when it is restored and when the app opens. Existing notes are repaired on launch, so no re-restore is needed.

## [1.7.1] - 2026-10-19

### Fixed
- **The + menu works again.** Raising the sheet layer had accidentally put the dimming overlay *above* the + dock, so opening it dimmed the screen and swallowed every tap. The layering is corrected: the dock and its panel sit above the scrim, and the dock correctly hides behind full-screen panels (Settings, Clock, a group).
- **The group rename field no longer looks foreign.** Text fields are now styled to match the app — dark rounded field, accent focus ring — instead of the raw white browser box.
- **Restore now accepts plain text**, so you can bring notes in from almost any app: a `.txt` or `.md` file imports with each blank-line-separated paragraph becoming a note (first line as its title), alongside the existing `.json` backup.
- The file picker now offers **all** the accepted types (`.json`, `.txt`, `.md`) instead of filtering down to just one.

### Added
- **Copy restore format** in Settings → Data. It copies a complete description of the backup/restore JSON shape straight to the clipboard (no dialog in the app) — paste it into any AI assistant to convert an export from another notes app.

## [1.7.0] - 2026-10-19

### Added
- **Multi-select on the home screen.** Long-press any note to start selecting, tap more to add them, then use **Select all** to grab everything. The bar gives you **Group**, **More** (the usual note options) and **Delete**.
- **Groups.** Turn a selection into a group, or open a group and add notes to it. Each group has its **own title** (tap it to rename) and its **own cover** — you can set one from your gallery or build it in the full cover editor, exactly like a note.
- Groups appear as a card on the home screen showing their cover and how many notes are inside; the notes themselves live inside the group rather than cluttering the main list.

### Fixed
- **`confirm()` dialogs now work.** A WebView silently ignores JavaScript `confirm()` unless the app handles it, which meant **Empty recycle bin** never actually ran. Native dialogs for alert/confirm/prompt are now wired up.

## [1.6.0] - 2026-10-19

### Added
- **Six clock themes**, all monochrome — *Bold, Mono, Thin, Flip, Condensed* and *Dial* (analog + digital). The clock is now **12-hour** with an AM/PM marker, and every theme is fully animated with no layout jitter.
- **Fullscreen clock** — hide the header, tabs and every control with one tap; tap the face to come back.
- **Paste and dictate keys on the keyboard.** The app keyboard now has a **Paste** key that pulls in whatever you copied from any other app, and a **mic key** that dictates straight into the note at the caret.

### Changed
- The keyboard is snappier: keys react on touch-down with instant visual feedback, and caret scrolling is now batched to one animation frame so typing never stutters.

### Fixed
- **Transcribing a recording no longer needs you to play it.** Notto now feeds the audio to the recogniser itself; if the device refuses that, it automatically plays the clip out loud and lets the recogniser listen to it on its own — no interaction needed either way.

## [1.5.0] - 2026-10-19

### Added
- **Clock, timer and alarm.** A new *Clock* option in the + menu: a landscape clock in a retro dot-matrix panel with small seconds, a **Seconds** on/off toggle and a soft **chime on every minute**. The Timer counts down from a preset (or your own), and the Alarm rings at a time you choose.
- **Alarms ring properly.** Timers and alarms post a notification and, when they go off, show a **full-screen takeover** in the app's own style with **Snooze** and **Stop** — over the lock screen too.
- **Transcribe a recording.** Long-press any voice recording inside a note to **Transcribe to text** or **Delete recording**.

### Fixed
- **Moodboard resize and rotate handles now work.** The board's "selected" class clashed with the text-toolbar's `.sel` rule, which sets `pointer-events: none` — so the handles could never be touched. Dragging them now resizes and rotates as intended.

## [1.4.0] - 2026-10-19

### Added
- **A proper recorder screen.** Recording now opens a full-screen recorder with a live timer, a pulsing record button, **Stop**, and then **Play / Record again / Save** — instead of recording blindly.
- **Masonry home screen.** Notes now pack together and fill the gaps, so the grid has no empty holes however tall or wide each card is.

### Fixed
- **Recording no longer clips the start.** The timer only begins once the microphone is actually recording, and Stop waits for the file to be finalised.
- **Speech to text is more reliable** — the microphone permission is now requested first and dictation starts automatically once it is granted, and the recogniser is recreated if it gets into a bad state.

## [1.3.1] - 2026-10-19

### Changed
- The app now **fills the entire screen on every device and density** — no letter-boxing or side gaps — and the status/navigation bars follow the theme, so the background is continuous edge to edge.

### Fixed
- Removed the WebView's overview zoom, which could scale the interface slightly on some devices.

## [1.3.0] - 2026-10-19

### Added
- **Splash screen** — a smooth animated launch screen with the Notto mark.
- **Pre-made widgets** — *Notto* (quick open), *Checklist* (your latest checklist) and *Moodboard* (your latest board).
- **Send a test notification** button in Settings, to check reminders work.
- Reminders re-arm themselves after a phone restart.

### Changed
- Search, filter and sort now fade smoothly instead of snapping.
- Background colour gained an **Auto** option and follows the theme by default.

### Fixed
- **Dark mode is dark again** — a previously chosen background colour could leave a light background in dark theme; it now defaults to Auto.
- **Voice recording now works** — recording is done natively, because the WebView blocked the browser recorder.
- **Notifications** now use a proper status-bar icon (the old one could be rejected) and request permission when reminders are switched on.
- **Android's copy/paste toolbar is hidden again** — this time without breaking text selection.

## [1.2.0] - 2026-10-19

### Added
- **Voice notes** — dictate with speech to text, or record audio and play it back inside a note.
- **Home-screen widget** — drop the Notto widget on your home screen; tap it to write.
- **Daily reminders** — choose a time in Settings and Notto nudges you once a day.
- **New app icon** built from the new Notto logo.
- Fonts chosen in Settings now apply across the whole app.

### Changed
- The app now fills the screen on tall phones and tablets instead of letter-boxing.
- Screens, the search bar and the Settings panel animate with the same spring feel as the home dock.
- Light theme gained soft shadows for a little depth.

### Fixed
- **Text selection works again** — the native text toolbar is no longer force-hidden, so you can select text and use the app's own cut/copy/paste bar.
- **Custom font import** and **backup restore** now open the file picker properly.
- A cover image set from the gallery now keeps the image's own shape (a square photo gives a square note).
- The Settings and Recycle-bin screens now use the same background as the home screen.

## [1.1.0] - 2026-10-19

### Added
- **Search** — find notes instantly from the home header.
- **Sort & filter menu** — sort by newest, oldest or title, and show only notes, checklists, images, moodboards or important notes.
- **Recycle bin** — deleted notes can be restored instead of disappearing.
- **Backup & restore** — export every note to a single file and import it back.
- **Settings screen** — appearance, fonts and data in one place.
- **Light theme** (System / Dark / Light) alongside the default dark theme.
- **Background options** — dots, plain, grid, diagonal or circles, plus any background colour.
- **14 built-in fonts** and **custom font import** (.ttf / .otf / .woff / .woff2) for cover and moodboard text.
- **Cover from gallery** — set a note's cover straight from your photos.
- **Keyboard hide button** in the note editor.
- The system clipboard is pulled into the editor's Clipboard tab automatically.

### Changed
- The cover editor now shows only the controls for what is selected, so it is far less cluttered.
- The editor's own cut/copy/paste bar is now the only one — Android's duplicate text toolbar is suppressed.

### Fixed
- Moodboard items now select and move reliably; tapping just beside an element no longer pans the whole canvas.
- Resize and rotate handles are easier to grab.

## [1.0.0] - 2026-10-19

### Added
- First public release.
- Notes with rich text: bold, italic, underline, highlight, bullet and numbered lists, font and size controls.
- Checklists with tap-to-complete items and progress on the home card.
- Image notes and drag-and-drop moodboards with stickers, text, rotate, scale and layering.
- Free-hand sketch canvas.
- Voice dictation into new notes.
- AI import: generate a prompt, paste an AI answer back, and auto-format it.
- Export notes as PNG, PDF and TXT.
- Pin important notes, filter by type and sort by recency.
- Bundled webfonts for full offline use.
- Signed release APK and automated GitHub Actions release pipeline.

[Unreleased]: https://github.com/itsrawdevvvvvv-arch/notto/compare/v1.8.0...HEAD
[1.8.0]: https://github.com/itsrawdevvvvvv-arch/notto/releases/tag/v1.8.0
[1.7.3]: https://github.com/itsrawdevvvvvv-arch/notto/releases/tag/v1.7.3
[1.7.2]: https://github.com/itsrawdevvvvvv-arch/notto/releases/tag/v1.7.2
[1.7.1]: https://github.com/itsrawdevvvvvv-arch/notto/releases/tag/v1.7.1
[1.7.0]: https://github.com/itsrawdevvvvvv-arch/notto/releases/tag/v1.7.0
[1.6.0]: https://github.com/itsrawdevvvvvv-arch/notto/releases/tag/v1.6.0
[1.5.0]: https://github.com/itsrawdevvvvvv-arch/notto/releases/tag/v1.5.0
[1.4.0]: https://github.com/itsrawdevvvvvv-arch/notto/releases/tag/v1.4.0
[1.3.1]: https://github.com/itsrawdevvvvvv-arch/notto/releases/tag/v1.3.1
[1.3.0]: https://github.com/itsrawdevvvvvv-arch/notto/releases/tag/v1.3.0
[1.2.0]: https://github.com/itsrawdevvvvvv-arch/notto/releases/tag/v1.2.0
[1.1.0]: https://github.com/itsrawdevvvvvv-arch/notto/releases/tag/v1.1.0
[1.0.0]: https://github.com/itsrawdevvvvvv-arch/notto/releases/tag/v1.0.0
