# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Android client for Tiny Tiny RSS (tt-rss), written in Java. Single Gradle module `org.fox.ttrss` (package/namespace `org.fox.ttrss`). This repo is a community fork (github.com/tt-rss/tt-rss-android) of the original tt-rss.org project. Translations arrive via Weblate (`weblate-integration` branch merges); don't hand-edit non-English `values-*` strings unless asked.

## Build, lint, test

Requires JDK 21 (Gradle toolchain); source/target compatibility is Java 17. minSdk 24, targetSdk 36, compileSdk 37.

```sh
./gradlew check assembleDebug          # what CI runs on PRs and master (lint + unit tests + debug APK)
./gradlew assembleDebug                # debug APK -> org.fox.ttrss/build/outputs/apk/debug/
./gradlew testDebugUnitTest            # JVM unit tests (JUnit 4 + Mockito)
./gradlew testDebugUnitTest --tests org.fox.ttrss.HeadlinesPageLoaderTest            # one class
./gradlew testDebugUnitTest --tests 'org.fox.ttrss.HeadlinesPageLoaderTest.someMethod' # one method
./gradlew lintDebug                    # report: org.fox.ttrss/build/reports/lint-results-debug.html
```

- Lint has `abortOnError = true`, so lint warnings-as-errors fail `check`. Translation-related checks are disabled; `SuspiciousIndentation` is enabled.
- Error Prone runs on every Java compile (config and disabled checks in `org.fox.ttrss/build.gradle`); its errors break the build.
- Unit tests use `unitTests.returnDefaultValues = true` (Android framework stubs return defaults instead of throwing). There are no instrumented tests.
- Build types: `debug` (`.debug` app id suffix), `release` (unsigned), `signed` and `branch` (need `SIGNING_*` Gradle properties). `versionCode`/`versionName` are derived from the HEAD commit's git timestamp and hash, so the build needs a git checkout.

## Architecture

All code lives in `org.fox.ttrss/src/main/java/org/fox/ttrss/` (tests in `src/test/...`, same package).

**Networking / API.** The app speaks the tt-rss JSON API (`op` = `login`, `getFeeds`, `getCategories`, `getHeadlines`, `updateArticle`, `catchupFeed`, ...). `ApiCommon` holds the shared OkHttp client (retry interceptor, auth, progress), performs requests and maps failures to the `ApiError` enum + string resources. Callers implement `ApiCommon.ApiCaller`. `ApiRequest` is the async wrapper: runs on a cached thread pool and delivers `onPostExecute(JsonElement)` on the main thread; subclasses override it inline. ViewModels (`FeedsModel`, `ArticleModel`) call `ApiCommon` directly from their own background work.

**Session state.** `Application` is a singleton holding the session id, API level and a `m_sessionValid` flag. The session id is persisted to prefs but deliberately *not* trusted after process death — `OnlineActivity.onResume()` re-logs in when the session is not marked valid. Keep that invariant when touching login logic.

**Activities.** `CommonActivity` (theme, prefs, screen-size helpers) → `OnlineActivity` (login, menus, article actions; the largest controller) → `MasterActivity` (feeds drawer + headlines) and `DetailActivity` (article pager, plus headlines list on tablets in landscape). Both implement `HeadlinesEventListener` so `HeadlinesFragment` can report selection/navigation. `LaunchActivity` is the entry point/router. `share/` contains separate share-intent / subscribe activities with their own lightweight `CommonActivity`. Tablet layouts live in `res/layout-sw600dp-land`; `isSmallScreen()` is set by checking for the `sw600dp_anchor` view.

**Shared article list.** A single `ArticleModel` (AndroidViewModel) is owned by `Application` (`Application.getArticlesModel()` / `getArticles()`), not by an activity, so `MasterActivity`, `DetailActivity`, `HeadlinesFragment` and `ArticlePager` all observe the same `LiveData<List<Article>>`. Headlines paging (incl. recovery when the server invalidates pagination, and "adaptive" view mode resolution) is isolated in the pure, unit-tested `HeadlinesPageLoader`. Feed/category lists use `FeedsModel`/`RootCategoriesModel` per fragment.

**Other pieces.** `types/` — API data objects (Gson/Parcelable). `glide/` — Glide module with OkHttp progress reporting for images. `Gallery*` — image/video viewer for article media. `widget/` — home-screen unread widget updated by WorkManager (`WidgetUpdateWorker`, rescheduled from `DeviceBootReceiver`). Crash reports go through ACRA (mail + dialog).

## Conventions

- Java style in the existing code: 4-space indentation (despite `.editorconfig` declaring tabs for `*.java` — match the surrounding file), `m_` prefix for member fields, `TAG = X.class.getName()` for logging.
- Fastlane store metadata lives in `fastlane/metadata`.
