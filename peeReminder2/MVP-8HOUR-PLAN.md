# MVP 8-Hour Development Plan
**Version:** 1.0.1  
**Date:** December 2025  
**Platform:** Android 11+ (API 30+)  
**Development Mode:** AI-Assisted Development  
**Target:** Complete MVP in 8 hours

## Progress Status

**Current Status:** ✅ **ALL HOURS COMPLETED** (100% Complete)  
**MVP Status:** ✅ **READY FOR TESTING**

### Completed Hours:
- ✅ **Hour 1:** Project Setup & Foundation
- ✅ **Hour 2:** Core Timer Logic - Part 1
- ✅ **Hour 3:** Core Timer Logic - Part 2
- ✅ **Hour 4:** Basic Notification System
- ✅ **Hour 5:** AlarmManager Integration
- ✅ **Hour 6:** Basic UI - Main Screen
- ✅ **Hour 7:** Settings & Integration
- ✅ **Hour 8:** Testing & Basic Polish

---

## Executive Summary

This document outlines a highly aggressive 8-hour MVP development plan. With AI assistance, we focus on **core functionality only** and defer non-critical features to post-MVP.

**Total Time:** 8 hours  
**Feasibility:** ⚠️ **CHALLENGING but POSSIBLE** with AI assistance and strict scope control

---

## Feasibility Analysis

### ✅ **FEASIBLE with AI Assistance IF:**
1. Strict scope control (MVP features only)
2. AI generates 70-80% of code
3. Minimal testing (basic functionality only)
4. Defer complex features (DND override, phone call detection)
5. Simple UI (functional, not polished)
6. Focus on core: 2-hour timer + notifications + sleep toggle

### ❌ **NOT FEASIBLE IF:**
1. Trying to implement all requirements
2. Extensive testing and debugging
3. Polished UI design
4. All edge cases handled
5. Complex retry mechanism
6. Battery optimization exemptions

### **Verdict:** ⚠️ **BORDERLINE FEASIBLE** - Requires:
- Aggressive scope reduction
- AI doing heavy lifting (80%+ code generation)
- Accepting "good enough" over "perfect"
- Deferring non-critical features

---

## MVP Scope Definition

### ✅ **IN SCOPE (Must Have)**
1. **Basic 2-hour timer** - Triggers every 2 hours
2. **Simple notifications** - Sound + vibration + visual
3. **Sleep mode toggle** - Pause/resume reminders
4. **Basic UI** - Show countdown, next reminder time, sleep toggle
5. **Acknowledgment button** - Reset timer (hidden by default, can enable in settings)
6. **Basic persistence** - Save sleep mode and timer state
7. **Simple retry** - Retry after 15 minutes if not acknowledged

### ❌ **OUT OF SCOPE (Defer to Post-MVP)**
1. **DND override** - Use standard notifications (may not work in DND)
2. **Phone call detection** - Standard notifications (may interrupt calls)
3. **Battery optimization exemption** - User can manually configure
4. **Full onboarding flow** - Simple permission request only
5. **Extensive edge case handling** - Basic recovery only
6. **Time zone change handling** - Basic implementation
7. **Polished UI design** - Functional, not beautiful
8. **Comprehensive testing** - Basic smoke testing only
9. **User documentation** - Minimal setup instructions

---

## Hour-by-Hour Breakdown

### **Hour 1: Project Setup & Foundation** (60 minutes) ✅ **COMPLETED**

**Tasks:**
- [x] Install Android Studio (if not installed) - 10 min
- [x] Create new Kotlin project (Android 11+, API 30) - 5 min
- [x] **AI: Generate project structure** (MVVM pattern) - 5 min
- [x] **AI: Generate build.gradle with dependencies** - 5 min
- [x] **AI: Generate SharedPreferences storage service** - 10 min
- [x] **AI: Generate data models** (TimerState, AppSettings) - 10 min
- [x] Test storage service works - 5 min
- [x] Set up Git repository - 5 min
- [x] Review AI-generated code - 5 min

**Deliverables:**
- ✅ Project structure
- ✅ Storage service
- ✅ Data models

**AI Assistance:** 80% code generation

---

### **Hour 2: Core Timer Logic - Part 1** (60 minutes) ✅ **COMPLETED**

**Tasks:**
- [x] **AI: Generate TimerManager class skeleton** - 10 min
- [x] **AI: Generate timer calculation logic** - 15 min
- [x] **AI: Generate basic state management** (IDLE, REMINDER_PENDING, REMINDER_ACTIVE) - 15 min
- [x] Review and understand code - 10 min
- [x] Test timer calculation manually - 5 min
- [x] Fix any obvious bugs - 5 min

**Deliverables:**
- ✅ TimerManager with basic timer logic
- ✅ Timer calculation works

**AI Assistance:** 85% code generation

---

### **Hour 3: Core Timer Logic - Part 2** (60 minutes) ✅ **COMPLETED**

**Tasks:**
- [x] **AI: Generate acknowledgment logic** - 10 min
- [x] **AI: Generate sleep mode toggle logic** - 10 min
- [x] **AI: Generate simple retry mechanism** (15-minute retry) - 15 min
- [x] **AI: Generate app restart recovery logic** - 10 min
- [x] Review and integrate all timer logic - 10 min
- [x] Basic manual testing - 5 min

**Deliverables:**
- ✅ Complete TimerManager
- ✅ Acknowledgment, sleep mode, retry working

**AI Assistance:** 85% code generation

---

### **Hour 4: Basic Notification System** (60 minutes) ✅ **COMPLETED**

**Tasks:**
- [x] **AI: Generate notification channel setup** - 10 min
- [x] **AI: Generate NotificationService class** - 15 min
- [x] **AI: Generate basic notification display** (sound + vibration + visual) - 10 min
- [x] **AI: Generate permission request flow** - 5 min
- [x] Integrate with TimerManager - 10 min
- [x] Test notifications work - 5 min
- [x] Fix notification issues - 5 min

**Deliverables:**
- ✅ Basic notifications working
- ✅ Permission request implemented

**AI Assistance:** 80% code generation

**Note:** Using standard notifications (no DND override for MVP)

---

### **Hour 5: AlarmManager Integration** (60 minutes) ✅ **COMPLETED**

**Tasks:**
- [x] **AI: Generate AlarmManager setup** - 10 min
- [x] **AI: Generate setExactAndAllowWhileIdle() implementation** - 15 min
- [x] **AI: Generate alarm scheduling logic** - 10 min
- [x] **AI: Generate alarm cancellation logic** - 5 min
- [x] Integrate with NotificationService - 10 min
- [x] Test alarms trigger correctly - 10 min
- [x] Fix alarm issues - 10 min

**Deliverables:**
- ✅ Alarms schedule correctly
- ✅ Notifications trigger at scheduled time

**AI Assistance:** 75% code generation

**Critical:** This is the hardest part - alarms must work reliably

---

### **Hour 6: Basic UI - Main Screen** (60 minutes) ✅ **COMPLETED**

**Tasks:**
- [x] **AI: Generate Jetpack Compose main screen layout** - 15 min
- [x] **AI: Generate countdown timer display** - 10 min
- [x] **AI: Generate next reminder time display** - 5 min
- [x] **AI: Generate sleep mode toggle (large button)** - 10 min
- [x] **AI: Generate acknowledgment button** (conditional visibility) - 10 min
- [x] Connect UI to ViewModel - 5 min
- [x] Test UI displays correctly - 5 min

**Implementation Notes:**
- Created `MainViewModel` with real-time countdown updates (updates every second)
- Created `MainScreen` composable with large, accessible UI elements
- Integrated ViewModel with TimerManager and StorageService
- Automatic timer start on first app launch

**Deliverables:**
- ✅ Main screen functional
- ✅ All UI elements visible and working

**AI Assistance:** 85% code generation

**Note:** Functional UI, not polished design

---

### **Hour 7: Settings & Integration** (60 minutes) ✅ **COMPLETED**

**Tasks:**
- [x] **AI: Generate settings screen** - 10 min
- [x] **AI: Generate acknowledgment button visibility toggle** - 5 min
- [x] **AI: Generate simple onboarding** (permission request only) - 10 min
- [x] Integrate all components - 15 min
- [x] Test end-to-end flow - 10 min
- [x] Fix integration bugs - 10 min

**Implementation Notes:**
- Created `SettingsViewModel` for managing app settings
- Created `SettingsScreen` composable with acknowledgment button visibility toggle
- Implemented simple state-based navigation between MainScreen and SettingsScreen
- Added settings button to MainScreen header
- Onboarding completion marked after first permission request
- MainViewModel observes settings changes to update acknowledgment button visibility in real-time

**Deliverables:**
- ✅ Settings screen working
- ✅ Basic onboarding (permission request flow)
- ✅ Components integrated

**Deliverables:**
- ✅ Settings screen working
- ✅ Basic onboarding
- ✅ Components integrated

**AI Assistance:** 80% code generation

---

### **Hour 8: Testing & Basic Polish** (60 minutes) ✅ **COMPLETED**

**Tasks:**
- [x] **AI: Generate basic unit tests** (critical functions only) - 10 min
- [x] Run tests and fix critical bugs - 15 min
- [x] Review code for obvious issues and polish - 15 min
- [x] Fix deprecation warnings - 5 min
- [x] Build configuration verified - 5 min

**Implementation Notes:**
- All existing unit tests pass (TimerManager, NotificationService)
- Fixed deprecation warning in SettingsScreen (Icons.AutoMirrored.Filled.ArrowBack)
- Code review completed - no critical bugs found
- Build configuration verified - release APK can be built with `./gradlew assembleRelease`
- Note: Physical device testing recommended before deployment

**Deliverables:**
- ✅ All unit tests passing
- ✅ Code reviewed and polished
- ✅ Deprecation warnings fixed
- ✅ Build configuration ready

**AI Assistance:** 50% (testing requires human verification)

---

## Critical Success Factors

### 1. **AI Prompting Efficiency**
- Use detailed, specific prompts
- Generate complete classes, not snippets
- Iterate quickly on AI suggestions

### 2. **Scope Discipline**
- **MUST RESIST:** Adding features not in MVP scope
- **MUST ACCEPT:** "Good enough" over "perfect"
- **MUST DEFER:** Non-critical features

### 3. **Testing Strategy**
- Test as you build (don't wait until end)
- Focus on critical path only
- Accept that some edge cases won't be tested

### 4. **Code Quality**
- AI-generated code may need quick fixes
- Don't over-engineer
- Functional > Beautiful

---

## Risk Assessment

### 🔴 **HIGH RISK - May Exceed 8 Hours**

1. **AlarmManager Integration** (Hour 5)
   - **Risk:** Alarms may not work correctly on first try
   - **Mitigation:** Use AI to generate complete, tested code
   - **Contingency:** If fails, use WorkManager as fallback (adds 30 min)

2. **Integration Bugs** (Hour 7)
   - **Risk:** Components may not work together
   - **Mitigation:** Test integration points as you build
   - **Contingency:** Cut non-critical features if time runs out

3. **Notification Permissions** (Hour 4)
   - **Risk:** Permission flow may have issues
   - **Mitigation:** Use AI to generate standard permission flow
   - **Contingency:** Simplify permission request

### 🟡 **MEDIUM RISK**

1. **Timer Logic Bugs** (Hours 2-3)
   - **Risk:** Timer calculations may be wrong
   - **Mitigation:** Use AI to generate tested calculation logic
   - **Contingency:** Manual testing and quick fixes

2. **UI Integration** (Hour 6)
   - **Risk:** UI may not update correctly
   - **Mitigation:** Use StateFlow/ViewModel pattern (AI generates)
   - **Contingency:** Simplify UI if needed

### 🟢 **LOW RISK**

1. **Project Setup** (Hour 1) - Standard Android setup
2. **Storage Service** (Hour 1) - Simple SharedPreferences
3. **Settings Screen** (Hour 7) - Simple UI

---

## Contingency Plan (If Time Runs Out)

### Priority Order (Cut from Bottom if needed):

1. **MUST HAVE (Cannot Cut):**
   - 2-hour timer
   - Basic notifications
   - Sleep mode toggle
   - Basic UI

2. **SHOULD HAVE (Cut if < 1 hour left):**
   - Acknowledgment button
   - Settings screen
   - Retry mechanism

3. **NICE TO HAVE (Cut if < 2 hours left):**
   - Onboarding flow
   - App restart recovery
   - Polished UI

---

## Realistic Assessment

### ⚠️ **8 Hours is TIGHT but POSSIBLE if:**

1. **AI generates 80%+ of code** ✅ Feasible
2. **Strict scope control** ✅ Feasible
3. **Accept "good enough"** ✅ Feasible
4. **No major debugging** ⚠️ Risky
5. **Alarms work first try** ⚠️ Risky
6. **No integration issues** ⚠️ Risky

### **More Realistic: 10-12 Hours**

If you want a **more comfortable buffer**, consider:
- **10 hours:** Adds 2 hours for debugging and polish
- **12 hours:** Adds 4 hours for proper testing and edge cases

### **Recommendation:**

**Option 1: Aggressive 8-Hour MVP**
- Accept minimal testing
- Defer non-critical features
- May have bugs
- **Feasibility: 60-70%**

**Option 2: Realistic 10-Hour MVP**
- Basic testing included
- Core features working well
- Fewer bugs
- **Feasibility: 85-90%**

**Option 3: Comfortable 12-Hour MVP**
- Proper testing
- Most features working
- Good quality
- **Feasibility: 95%+**

---

## Success Metrics for 8-Hour MVP

### ✅ **MVP is Successful if:**
1. ✅ App installs and runs
2. ✅ Timer triggers every 2 hours (approximately)
3. ✅ Notifications appear (sound + vibration + visual)
4. ✅ Sleep mode toggle works
5. ✅ Acknowledgment button resets timer (UI implemented, settings pending)
6. ✅ App survives app restart (basic recovery)

### Current Status:
- ✅ **Core functionality:** Timer, notifications, alarms, UI all implemented
- ⏳ **Settings screen:** Pending (Hour 7)
- ⏳ **Testing:** Pending (Hour 8)

### ❌ **MVP is NOT Successful if:**
1. App crashes on launch
2. Notifications don't trigger
3. Timer doesn't work
4. Sleep mode doesn't work

---

## AI Prompting Examples for 8-Hour MVP

### Hour 1 - Storage Service
```
Generate a complete SharedPreferences-based storage service for Android 
in Kotlin. Include methods to save/load:
- sleep mode state (Boolean)
- last reminder time (Long)
- last acknowledgment time (Long)
- acknowledgment button visibility (Boolean)
- setup completed flag (Boolean)

Include error handling and use coroutines for async operations.
```

### Hour 2 - Timer Manager
```
Generate a TimerManager class in Kotlin for Android that:
- Manages a 2-hour reminder timer
- Calculates next reminder time from current time or last acknowledgment
- Has states: IDLE, REMINDER_PENDING, REMINDER_ACTIVE
- Includes method to acknowledge reminder (resets timer from acknowledgment time)
- Handles sleep mode (pause/resume)
- Persists state using SharedPreferences

Use StateFlow for state management and include error handling.
```

### Hour 5 - AlarmManager
```
Generate complete AlarmManager integration for Android 11+ that:
- Uses setExactAndAllowWhileIdle() to schedule exact alarms
- Schedules reminders at specific times
- Cancels alarms when needed
- Handles alarm permissions (SCHEDULE_EXACT_ALARM)
- Includes BroadcastReceiver to handle alarm triggers
- Triggers notifications when alarm fires

Include error handling and permission checks.
```

---

## Final Verdict

### **8 Hours: ⚠️ BORDERLINE FEASIBLE**

**Can it be done?** Yes, with:
- Aggressive AI assistance (80%+ code generation)
- Strict scope control
- Accepting "good enough"
- Some luck (no major bugs)

**Will it be perfect?** No - expect:
- Some bugs
- Minimal testing
- Basic UI
- Deferred features

**Recommendation:** 
- **If you MUST do 8 hours:** Follow this plan, accept limitations
- **If you CAN do 10-12 hours:** Much more comfortable, better quality

---

## Implementation Summary

### ✅ Completed Components (Hours 1-6)

#### Hour 1: Project Setup & Foundation
- ✅ Project structure with MVVM pattern
- ✅ `StorageService` using SharedPreferences
- ✅ Data models: `TimerData`, `TimerState`, `AppSettings`

#### Hour 2-3: Core Timer Logic
- ✅ `TimerManager` class with complete state machine
- ✅ 2-hour timer calculation logic
- ✅ Acknowledgment handling
- ✅ Sleep mode toggle functionality
- ✅ 15-minute retry mechanism
- ✅ App restart recovery logic

#### Hour 4: Notification System
- ✅ `NotificationService` class
- ✅ Notification channel setup
- ✅ Sound + vibration + visual notifications
- ✅ Permission request flow (Android 13+)

#### Hour 5: AlarmManager Integration
- ✅ `AlarmService` wrapper class
- ✅ `setExactAndAllowWhileIdle()` implementation
- ✅ Alarm scheduling and cancellation logic
- ✅ `ReminderReceiver` for alarm triggers
- ✅ `BootReceiver` for device reboot recovery

#### Hour 6: Basic UI - Main Screen
- ✅ `MainViewModel` with real-time countdown updates
- ✅ `MainScreen` composable with:
  - Large countdown timer display (HH:MM:SS format)
  - Next reminder time display
  - Large sleep mode toggle button
  - Conditional acknowledgment button
  - Settings button in header
- ✅ Automatic timer start on first launch

#### Hour 7: Settings & Integration
- ✅ `SettingsViewModel` for managing app settings
- ✅ `SettingsScreen` composable with:
  - Acknowledgment button visibility toggle
  - About section
  - Back navigation
- ✅ Simple state-based navigation (MainScreen ↔ SettingsScreen)
- ✅ Onboarding completion tracking (marked after first permission request)
- ✅ Real-time settings synchronization between screens

### ⏳ Remaining Components (Hour 8)

#### Hour 8: Testing & Polish
- [ ] Unit tests for critical functions
- [ ] Physical device testing
- [ ] Bug fixes
- [ ] Release APK build

### Key Files Created

**Data Layer:**
- `data/model/TimerData.kt`
- `data/model/TimerState.kt`
- `data/model/AppSettings.kt`
- `data/storage/StorageService.kt`

**Domain Layer:**
- `domain/TimerManager.kt`

**Notification Layer:**
- `data/notification/NotificationService.kt`
- `data/notification/AlarmService.kt`
- `data/notification/ReminderReceiver.kt`
- `data/notification/BootReceiver.kt`

**UI Layer:**
- `ui/MainViewModel.kt`
- `ui/MainScreen.kt`
- `ui/SettingsViewModel.kt`
- `ui/SettingsScreen.kt`
- `MainActivity.kt` (updated with navigation)

---

**End of 8-Hour MVP Plan**

