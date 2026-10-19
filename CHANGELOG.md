# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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

[Unreleased]: https://github.com/itsrawdevvvvvv-arch/notto/compare/v1.3.0...HEAD
[1.3.0]: https://github.com/itsrawdevvvvvv-arch/notto/releases/tag/v1.3.0
[1.2.0]: https://github.com/itsrawdevvvvvv-arch/notto/releases/tag/v1.2.0
[1.1.0]: https://github.com/itsrawdevvvvvv-arch/notto/releases/tag/v1.1.0
[1.0.0]: https://github.com/itsrawdevvvvvv-arch/notto/releases/tag/v1.0.0
