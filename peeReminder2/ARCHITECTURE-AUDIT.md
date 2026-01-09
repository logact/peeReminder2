# Architecture Audit & Technical Analysis
**Version:** 1.0.0  
**Date:** December 2025  
**Role:** Senior Technical Manager  
**Status:** Initial Audit Complete

---

## Executive Summary

This document provides a comprehensive technical audit of the Requirements.md, identifying potential bottlenecks, resource-heavy tasks, third-party dependencies, and scalability concerns. It also defines the recommended tech stack and system architecture.

---

## 1. Requirements Audit

### 1.1 Critical Bottlenecks Identified

#### 🔴 **CRITICAL: Background Notification Reliability (REQ-001.1, REQ-006.2)**
**Issue**: Requirements mandate reminders work in ALL states (foreground, background, closed, locked, DND override)
- **Bottleneck**: iOS and Android have strict background execution limits
- **Impact**: High risk of missed reminders if not implemented correctly
- **Complexity**: Platform-specific implementation required
- **Mitigation Required**: 
  - iOS: Use UNNotificationRequest with proper scheduling
  - Android: Use WorkManager + AlarmManager (requires careful configuration)
  - Both: Need foreground service (Android) / background modes (iOS)

#### 🔴 **CRITICAL: DND Override (REQ-001.5, REQ-006.2)**
**Issue**: Must override Do Not Disturb mode
- **Bottleneck**: 
  - iOS: Limited DND override capabilities (only "Critical Alerts" category, requires special entitlement from Apple)
  - Android: Varies by manufacturer (Samsung, Xiaomi have aggressive battery optimization)
- **Impact**: May require special permissions/entitlements (not applicable for direct installation)
- **Mitigation**: 
  - iOS: Apply for Critical Alerts entitlement (medical justification required)
  - Android: Use notification channels with "Important" priority + battery optimization exemption

#### 🟡 **MODERATE: Timer State Persistence (NFR-005, REQ-001.4)**
**Issue**: Timer must persist across app restarts, device reboots, OS updates
- **Bottleneck**: Need reliable local storage + recovery mechanism
- **Impact**: Medium complexity, but critical for reliability
- **Mitigation**: Use persistent storage (SQLite/Realm) + recovery logic on app launch

#### 🟡 **MODERATE: Retry Mechanism with State Tracking (REQ-005)**
**Issue**: 15-minute retry logic requires tracking acknowledgment state
- **Bottleneck**: Multiple timer states (initial reminder, retry pending, acknowledged)
- **Impact**: State machine complexity, potential race conditions
- **Mitigation**: Use state machine pattern, atomic state updates

#### 🟢 **LOW: Full-Screen Visual Notifications (REQ-001.2)**
**Issue**: Full-screen notifications when app is closed/locked
- **Bottleneck**: Platform limitations (iOS doesn't allow full-screen when locked)
- **Impact**: May need to compromise on "full-screen" definition
- **Mitigation**: Use high-priority notifications with rich content

### 1.2 Resource-Heavy Tasks

#### ⏱️ **Time-Intensive Tasks**

1. **Cross-Platform Development** (Estimated: 2-3x single-platform time)
   - Separate iOS and Android codebases OR React Native/Flutter learning curve
   - Platform-specific notification implementations
   - Testing on multiple devices/OS versions

2. **Background Service Implementation** (Estimated: 40-60 hours)
   - iOS background modes configuration
   - Android WorkManager + AlarmManager setup
   - Battery optimization exemptions
   - Testing edge cases (app killed, device reboot, low battery)

3. **Notification System** (Estimated: 30-40 hours)
   - Platform-specific notification APIs
   - DND override implementation
   - Phone call detection and conflict handling
   - Notification persistence logic

4. **State Management & Persistence** (Estimated: 20-30 hours)
   - Timer state machine
   - Retry mechanism logic
   - Sleep mode persistence
   - Recovery on app restart

#### 💰 **Cost-Intensive Tasks**

1. **Device Testing**: 
   - Need physical devices running Android 11+
   - Estimated cost: $200-500 for test devices (if not available)
   - **Note**: App will be installed directly (APK), no app store distribution needed

#### 🧠 **Token/Complexity-Intensive Tasks**

1. **Hybrid Timer Logic** (REQ-001.4): 
   - Complex state transitions
   - Edge case handling (acknowledged after retry, multiple retries)
   - Requires careful design to avoid bugs

2. **Conflict Handling** (REQ-001.5):
   - Phone call detection
   - DND state monitoring
   - Dynamic notification type switching

3. **Time Zone Handling** (NFR-009):
   - Automatic time zone change detection
   - Timer recalculation on time zone change
   - Edge cases (daylight saving time, travel)

### 1.3 Third-Party Dependencies

#### ✅ **Required Dependencies**

1. **Platform Notification Services** (FREE, but critical)
   - iOS: Apple Push Notification service (APNs) - **Note**: Actually NOT needed for local notifications
   - Android: Firebase Cloud Messaging (FCM) - **Note**: Actually NOT needed for local notifications
   - **Correction**: For local notifications, we use platform-native APIs, NOT push services

2. **Local Storage Libraries**:
   - iOS: CoreData or Realm (optional, can use UserDefaults for simple data)
   - Android: Room Database or SharedPreferences (for simple data)
   - **Recommendation**: Start with platform-native storage (UserDefaults/SharedPreferences), upgrade if needed

#### ⚠️ **Optional but Recommended Dependencies**

1. **State Management** (if using React Native/Flutter):
   - Redux/MobX (React Native)
   - Provider/Riverpod (Flutter)

2. **Testing Frameworks**:
   - Jest (React Native)
   - XCTest (iOS native)
   - Espresso (Android native)

#### ❌ **Dependencies to AVOID**

1. **Cloud Services**: Explicitly out of scope (NFR-010)
2. **Analytics SDKs**: Privacy requirement (NFR-010) - only basic app functionality tracking
3. **Third-party notification services**: Not needed for local notifications

### 1.4 Scalability & Implementation Concerns

#### 🚨 **Critical Scalability Issues**

1. **Single-User Design**: 
   - ✅ **GOOD**: App is designed for single-user, local-only storage
   - ✅ **GOOD**: No scalability concerns for MVP (by design)
   - ⚠️ **FUTURE**: Multi-device sync explicitly out of scope, but architecture should consider future extensibility

2. **Timer Accuracy Across Long Periods**:
   - **Issue**: System clock drift, device sleep/wake cycles
   - **Impact**: Timer may drift over days/weeks
   - **Mitigation**: Recalculate timer on app launch, use system time (not elapsed time)

3. **Battery Optimization Aggressiveness**:
   - **Issue**: Modern Android devices kill background apps aggressively
   - **Impact**: Reminders may be missed on some devices
   - **Mitigation**: 
     - Use WorkManager with AlarmManager.setExactAndAllowWhileIdle() (Android 11+)
     - Request battery optimization exemption
     - Educate users about device-specific settings
     - **Note**: Target Android 11+ (API 30+) for compatibility

#### ⚠️ **Implementation Challenges**

1. **Platform Fragmentation** (Android):
   - Different manufacturers (Samsung, Xiaomi, Huawei) have different battery optimization behaviors
   - Some require manual user configuration
   - **Mitigation**: Provide clear setup instructions, detect and warn users

2. **iOS Background Execution Limits**:
   - iOS kills background apps after ~30 seconds
   - Background refresh is unreliable
   - **Mitigation**: Use UNNotificationRequest scheduling (not background execution)

3. **Timer State Recovery After Reboot**:
   - **Issue**: Need to recalculate next reminder time after device reboot
   - **Challenge**: What if device was off for 6 hours? Resume immediately or wait?
   - **Recommendation**: Resume immediately when sleep mode is OFF (medical priority)

4. **Sleep Mode Toggle Edge Cases**:
   - **Issue**: What happens if user toggles sleep mode while reminder is active?
   - **Recommendation**: If reminder is active, dismiss it first, then toggle sleep mode

5. **Acknowledgment Button State**:
   - **Issue**: Button visibility controlled by settings, but availability depends on reminder state
   - **Complexity**: Multiple conditions to check (settings enabled + reminder active)
   - **Mitigation**: Clear state management, test all combinations

---

## 2. Recommended Tech Stack

### 2.1 Platform Strategy

**Deployment Target: Android 11+ (API 30+) Only**
- App will be installed directly via APK (no app store distribution)
- Focus on Android native development for this deployment

**Decision: Native Development vs Cross-Platform**

**Recommendation: Native Android Development**

**Rationale:**
- ✅ **Reliability**: Critical for medical app - native APIs provide best reliability
- ✅ **Background Execution**: Native background services are more reliable
- ✅ **DND Override**: Requires platform-specific entitlements/permissions
- ✅ **Battery Optimization**: Better control over battery optimization exemptions
- ✅ **Performance**: Native apps have better battery efficiency
- ❌ **Cost**: 2x development time (but acceptable for MVP reliability)

**Alternative Considered: React Native / Flutter**
- ⚠️ **Risk**: Background notification reliability concerns
- ⚠️ **Risk**: DND override may not be fully supported
- ⚠️ **Risk**: Battery optimization exemptions harder to implement
- ✅ **Benefit**: Single codebase, faster development
- **Verdict**: Not recommended for MVP due to reliability concerns

### 2.2 iOS Tech Stack

#### Core Framework
- **Language**: Swift 5.7+
- **UI Framework**: SwiftUI (modern, declarative) OR UIKit (more control)
- **Recommendation**: SwiftUI for simpler UI, easier maintenance

#### Notification System
- **Framework**: UserNotifications (UNUserNotificationCenter)
- **Background Modes**: 
  - Background fetch (optional, for state sync)
  - Remote notifications (NOT needed for local notifications)
- **Critical Alerts**: UNNotificationCategory with critical alert entitlement

#### Data Persistence
- **Primary**: UserDefaults (for simple key-value: sleep mode, settings)
- **Timer State**: UserDefaults + calculation on launch
- **Future**: CoreData (if history tracking added later)

#### Architecture Pattern
- **Pattern**: MVVM (Model-View-ViewModel)
- **State Management**: @StateObject, @ObservedObject (SwiftUI)
- **Dependency Injection**: Manual (simple enough for MVP)

#### Testing
- **Unit Tests**: XCTest
- **UI Tests**: XCTest UI Testing

### 2.3 Android Tech Stack

#### Core Framework
- **Language**: Kotlin (modern, recommended)
- **UI Framework**: Jetpack Compose (modern) OR XML Layouts (traditional)
- **Recommendation**: Jetpack Compose for simpler UI, better maintainability

#### Notification System
- **Framework**: NotificationManagerCompat (for compatibility)
- **Background Work**: WorkManager (for reliable background execution)
- **Exact Alarms**: AlarmManager.setExactAndAllowWhileIdle() (Android 11+, API 30+)
- **Notification Channels**: Create "Medical Reminders" channel with high importance
- **Target SDK**: Android 11+ (API 30+)

#### Data Persistence
- **Primary**: SharedPreferences (for simple key-value: sleep mode, settings)
- **Timer State**: SharedPreferences + calculation on launch
- **Future**: Room Database (if history tracking added later)

#### Architecture Pattern
- **Pattern**: MVVM (Model-View-ViewModel)
- **State Management**: ViewModel + LiveData/StateFlow
- **Dependency Injection**: Manual (simple enough for MVP) OR Hilt (if complexity grows)

#### Testing
- **Unit Tests**: JUnit 4/5
- **UI Tests**: Espresso

### 2.4 Development Tools

#### Version Control
- **Git**: Standard version control
- **Repository**: GitHub/GitLab/Bitbucket

#### CI/CD (Optional for MVP)
- **Android**: GitHub Actions / GitLab CI (for automated APK builds)
- **Note**: Direct APK installation, no app store submission needed

#### Project Management
- **Issue Tracking**: GitHub Issues / Jira
- **Documentation**: Markdown files (as currently used)

---

## 3. System Architecture

### 3.1 High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        User Device                           │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────┐         ┌──────────────┐                  │
│  │   iOS App    │         │ Android App  │                  │
│  │  (SwiftUI)   │         │  (Compose)   │                  │
│  └──────┬───────┘         └──────┬───────┘                  │
│         │                        │                           │
│         └───────────┬────────────┘                           │
│                     │                                        │
│         ┌───────────▼───────────┐                           │
│         │   Core Business Logic │                           │
│         │   (Timer Manager)     │                           │
│         └───────────┬───────────┘                           │
│                     │                                        │
│         ┌───────────▼───────────┐                           │
│         │  Notification Service │                           │
│         │  (Platform Native)    │                           │
│         └───────────┬───────────┘                           │
│                     │                                        │
│         ┌───────────▼───────────┐                           │
│         │   Local Storage       │                           │
│         │  (UserDefaults/       │                           │
│         │   SharedPreferences)  │                           │
│         └───────────────────────┘                           │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 Core Components

#### Component 1: Timer Manager
**Responsibility**: Manages 2-hour timer logic, state transitions, retry mechanism

**Key Methods**:
- `startTimer()`: Initialize timer from current time or last saved state
- `acknowledgeReminder()`: Reset timer from acknowledgment time
- `checkRetryNeeded()`: Check if 15-minute retry is needed
- `calculateNextReminderTime()`: Calculate next reminder time
- `handleSleepModeToggle()`: Pause/resume reminders

**State Machine**:
```
IDLE → REMINDER_PENDING → REMINDER_ACTIVE → [ACKNOWLEDGED → IDLE]
                                              [RETRY_PENDING → RETRY_ACTIVE → ...]
```

#### Component 2: Notification Service
**Responsibility**: Platform-specific notification scheduling and delivery

**Key Methods**:
- `scheduleReminder(time: Date)`: Schedule notification at specific time
- `cancelReminder()`: Cancel scheduled notification
- `showReminder()`: Display immediate notification
- `checkPhoneCallStatus()`: Detect if phone call is active
- `overrideDND()`: Configure notification to override DND

**Platform Differences**:
- iOS: UNNotificationRequest with critical alert category
- Android: NotificationChannel with IMPORTANCE_HIGH + exact alarm

#### Component 3: Storage Service
**Responsibility**: Persist app state (sleep mode, timer state, settings)

**Key Data**:
- Sleep mode state (boolean)
- Last reminder time (timestamp)
- Last acknowledgment time (timestamp)
- Acknowledgment button visibility (boolean)
- Setup completion flag (boolean)

**Storage Method**:
- iOS: UserDefaults
- Android: SharedPreferences

#### Component 4: UI Layer
**Responsibility**: Display information, handle user interactions

**Screens**:
1. **Main Screen**: Countdown timer, sleep toggle, acknowledgment button
2. **Settings Screen**: Acknowledgment button visibility toggle
3. **Onboarding Flow**: Welcome, permissions, tutorial

**Architecture Pattern**: MVVM
- View: UI components (SwiftUI Views / Compose Composables)
- ViewModel: Business logic, state management
- Model: Data structures (TimerState, Settings, etc.)

### 3.3 Interaction Flow

#### Flow 1: App Launch
```
1. App launches
2. Check if setup completed → If not, show onboarding
3. Load state from storage (sleep mode, last reminder time)
4. Calculate next reminder time
5. Schedule notification for next reminder time
6. Display main screen with countdown timer
```

#### Flow 2: Reminder Trigger
```
1. Notification triggers at scheduled time
2. Check sleep mode → If ON, ignore reminder
3. Check phone call status → If active, visual-only notification
4. Display notification (sound + vibration + visual)
5. Start 15-minute retry timer
6. Update UI (show acknowledgment button if enabled)
```

#### Flow 3: User Acknowledges Reminder
```
1. User taps acknowledgment button
2. Record acknowledgment time
3. Cancel retry timer
4. Reset 2-hour timer from acknowledgment time
5. Schedule next reminder notification
6. Update UI (hide acknowledgment button, update countdown)
```

#### Flow 4: Retry Mechanism
```
1. 15 minutes pass without acknowledgment
2. Check if reminder still active → If acknowledged, cancel retry
3. Check phone call status → If active, visual-only
4. Display retry notification
5. Start another 15-minute retry timer
6. If still not acknowledged after retry:
   - Continue timer from original reminder time (prevent cascade)
   - Schedule next reminder normally
```

#### Flow 5: Sleep Mode Toggle
```
1. User toggles sleep mode ON
2. Cancel all scheduled notifications
3. Save sleep mode state to storage
4. Update UI (show sleep mode indicator)
5. When toggled OFF:
   - Calculate next 2-hour interval from last reminder time
   - Schedule next reminder notification
   - Update UI
```

#### Flow 6: App Restart Recovery
```
1. App launches after being killed
2. Load state from storage:
   - Last reminder time
   - Sleep mode state
   - Last acknowledgment time
3. Calculate elapsed time since last reminder
4. If sleep mode OFF:
   - If < 2 hours elapsed: Schedule reminder for remaining time
   - If >= 2 hours elapsed: Schedule immediate reminder
5. If sleep mode ON: Do nothing (reminders paused)
```

### 3.4 Data Flow

```
User Action → ViewModel → Timer Manager → Notification Service
                                      ↓
                                 Storage Service
                                      ↓
                                 Local Storage
```

**Example: Acknowledgment Flow**
```
User taps "Acknowledge" 
  → ViewModel.acknowledgeReminder()
    → TimerManager.acknowledgeReminder(timestamp)
      → TimerManager.calculateNextReminderTime()
      → NotificationService.scheduleReminder(nextTime)
      → StorageService.saveLastAcknowledgmentTime(timestamp)
    → ViewModel.updateUI()
  → View updates countdown timer
```

---

## 4. Critical Questions & Recommendations

### 4.1 Questions Requiring Clarification

1. **Timer Recovery After Long Device Off Period**:
   - **Scenario**: Device off for 6 hours, sleep mode was OFF
   - **Question**: Should reminder trigger immediately on boot, or wait for next 2-hour interval?
   - **Recommendation**: Trigger immediately (medical priority), then resume normal schedule

2. **Sleep Mode Toggle During Active Reminder**:
   - **Scenario**: Reminder is active, user toggles sleep mode ON
   - **Question**: Should reminder be dismissed automatically?
   - **Recommendation**: Yes, dismiss reminder, then pause future reminders

3. **Multiple Retries**:
   - **Scenario**: User misses initial reminder, misses retry, still not acknowledged
   - **Question**: Should there be multiple retries or just one?
   - **Current Requirement**: Only one retry (REQ-005.1)
   - **Recommendation**: Keep single retry, but ensure timer continues from original time

4. **Acknowledgment Button Availability**:
   - **Scenario**: Acknowledgment button is enabled in settings, but user dismisses notification without acknowledging
   - **Question**: Should acknowledgment button still be available on main screen?
   - **Recommendation**: Yes, available until acknowledged or next reminder triggers

### 4.2 Architecture Recommendations

1. **Start Simple, Scale Later**:
   - Use UserDefaults/SharedPreferences for MVP
   - Can migrate to database later if history tracking is added

2. **Platform-Specific Optimizations**:
   - iOS: Use UNNotificationRequest scheduling (not background execution)
   - Android: Use WorkManager + AlarmManager combination for reliability

3. **Error Handling Strategy**:
   - Log all timer events for debugging
   - Graceful degradation if notification permissions denied
   - Clear error messages for users

4. **Testing Strategy**:
   - Unit tests for timer logic (critical)
   - Integration tests for notification scheduling
   - Manual testing on physical devices (critical for background behavior)

---

## 5. Risk Assessment Summary

| Risk | Severity | Likelihood | Mitigation Priority |
|------|----------|------------|---------------------|
| Notification reliability | HIGH | MEDIUM | CRITICAL |
| DND override approval | HIGH | LOW | HIGH |
| Battery optimization | MEDIUM | HIGH | HIGH |
| Timer state recovery | MEDIUM | MEDIUM | MEDIUM |
| Platform fragmentation | MEDIUM | HIGH | MEDIUM |
| Time zone handling | LOW | LOW | LOW |

---

## 6. Next Steps

1. **Review & Approve**: Review this architecture with team
2. **Clarify Questions**: Answer critical questions in Section 4.1
3. **Prototype**: Build minimal Android prototype to test notification reliability on Android 11+
4. **Development**: Begin Android implementation following defined architecture
5. **Testing**: Test on Android 11+ devices with direct APK installation

---

**End of Architecture Audit Document**

