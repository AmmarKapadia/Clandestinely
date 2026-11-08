# AI Usage Log

Every AI-assisted task in this project gets an entry here (README §10).

## 2026-11-08 — Daniel — M6 Onboarding
- Tool: Claude Code
- Prompt(s): "Do what I am required to do for Week 5 | Daniel | M6: onboarding screens (3 slides + permissions). Follow Intan's style but change up some things that look ugly (for example the fonts)... keep it simple, do not use AI-generated icons, use public repo icons."
- Output used:
  - `ui/onboarding/OnboardingActivity.java`, `ui/onboarding/OnboardingAdapter.java`
  - `layout/activity_onboarding.xml`, `layout/item_onboarding_slide.xml`
  - `values/strings_onboarding.xml`, `values/styles_onboarding.xml`
  - `drawable/ic_onb_*.xml` (Iconoir icons, MIT licence, converted from the public repo's SVGs), `drawable/bg_onb_circle.xml`, `drawable/onb_dot.xml`
  - `AndroidManifest.xml` (onboarding is now the launcher; location + notification permissions)
  - ViewPager2 dependency in `gradle/libs.versions.toml` and `app/build.gradle.kts`
- Design input: Intan's Figma Welcome page (colours, pill buttons) and flow board ("location denied → choose location manually"). Font changed to Android's built-in sans-serif on these screens only.
- Verification: built with `./gradlew assembleDebug`; unit tests pass. Tested on the emulator (API 37):
  - fresh install shows the slides; swipe, Next, dots and Skip work;
  - both Allow buttons show the system dialog and change to "Allowed ✓";
  - Get started opens sign-in, also with both permissions denied;
  - Back on the permissions page returns to the slides;
  - relaunching skips onboarding.
- Changed by member: <fill in any changes you make after reviewing>
