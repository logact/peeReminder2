# Requirements Document: 2-Hour Reminder App

**Version:** 1.0.1  
**Date:** December 2025  
**Status:** MVP Requirements - Finalized

---

## 1. Executive Summary

### 1.1 Product Vision
A simple, reliable smartphone app that helps users maintain their 2-hour bladder rehabilitation schedule by providing timely reminders during waking hours, working seamlessly whether the user is at home or away.

### 1.2 Purpose
This document defines the functional and non-functional requirements for the MVP (Minimum Viable Product) of a 2-hour reminder application designed to support post-BPH surgery recovery by ensuring users maintain their medically prescribed 2-hour bladder exercise schedule.

### 1.3 Scope
This requirements document covers only the MVP features. Future enhancements (compliance tracking, family alerts, multi-device sync, etc.) are explicitly out of scope and documented as such.

---

## 2. User Requirements

### 2.1 Target User Profile

**Primary User:**
- **Age**: Elderly (post-BPH surgery recovery)
- **Tech Comfort**: Comfortable with smartphones/apps
- **Primary Need**: Reliable reminders every 2 hours during waking hours
- **Pain Points**: 
  - Forgetting to follow medical schedule
  - Health complications from missed reminders
  - Need for reminders both at home and away
- **Behavior**: Uses smartphone regularly, carries it throughout the day

### 2.2 Medical Context
- **Condition**: Recovery phase after surgery for benign prostatic hyperplasia (BPH)
- **Requirement**: Active peeing every 2 hours is required to exercise the bladder
- **Purpose**: Critical part of post-surgical rehabilitation
- **Constraint**: Reminders NOT needed during sleep

---

## 3. Functional Requirements

### 3.1 Reminder System (REQ-001)

**REQ-001.1: Reminder Frequency**
- The app MUST trigger reminders at exactly 2-hour intervals during waking hours
- Reminders MUST continue regardless of app state (foreground, background, closed, or phone locked)

**REQ-001.2: Reminder Types**
- Reminders MUST include:
  - Sound notification
  - Vibration notification
  - Visual notification (on-screen, full screen)
- All three types MUST be enabled by default
- Visual notification MUST be full screen and noticeable enough to catch user's attention

**REQ-001.3: Location Independence**
- Reminders MUST work at home and away (no location restrictions)
- Reminders MUST work without internet connection (local functionality)

**REQ-001.4: Hybrid Timer Behavior**
- **Primary Behavior**: When user acknowledges a reminder (clicks acknowledgment button), the 2-hour timer MUST reset from the acknowledgment time (user-driven flexibility)
- **Fallback Retry**: If a reminder is not acknowledged (user does not click acknowledgment button) within 15 minutes, the app MUST automatically retry the reminder
- **Best Effort Recovery**: 
  - If acknowledged after retry: Timer resets from acknowledgment time
  - If still not acknowledged after retry: Timer continues from original reminder time to maintain 2-hour medical rhythm (prevents cascade)

**REQ-001.5: Conflict Handling**
- The app MUST override "Do Not Disturb" (DND) mode - reminders always work even if DND is accidentally enabled
- During active phone calls: Reminders MUST appear as visual-only notifications (no sound/vibration to avoid interrupting call)
- The retry mechanism MUST handle missed reminders due to conflicts

### 3.2 Sleep Mode Toggle (REQ-002)

**REQ-002.1: Activation**
- The app MUST provide a manual on/off toggle for sleep mode
- Sleep mode toggle MUST be easily accessible from the main screen

**REQ-002.2: Behavior**
- When sleep mode is ON: All reminders MUST be paused
- When sleep mode is OFF: Reminders MUST resume from the next 2-hour interval
- Sleep mode state MUST persist even if app is closed or phone is restarted

**REQ-002.3: Visibility**
- The app MUST clearly indicate the current sleep mode status (on/off) on the main screen
- Visual indicators MUST be large and clear for elderly users

### 3.3 Main Screen Interface (REQ-003)

**REQ-003.1: Display Elements**
The main screen MUST display:
- Countdown timer showing time remaining until next reminder
- Exact time of next reminder
- Current sleep mode status (on/off indicator)
- Acknowledgment button for current active reminder (if applicable and enabled in settings)

**REQ-003.2: Design Requirements**
- Interface MUST be designed for elderly users:
  - Large touch targets (minimum 44x44 points)
  - Clear, high-contrast text
  - Simple, uncluttered layout
  - Immediate visual clarity

### 3.4 Reminder Acknowledgment (REQ-004)

**REQ-004.1: Acknowledgment Action**
- The app MUST provide an acknowledgment button when a reminder is active
- Acknowledgment button MUST be hidden by default
- Acknowledgment button can be shown/hidden via settings page
- Acknowledgment MUST be a single tap action (no confirmation required)
- Acknowledgment MUST reset the 2-hour timer from the acknowledgment time

**REQ-004.2: Acknowledgment Availability**
- Acknowledgment button MUST be available (when enabled in settings):
  - From the notification itself
  - From the main screen when reminder is active
  - From notification center

### 3.5 Retry Mechanism (REQ-005)

**REQ-005.1: Automatic Retry**
- If a reminder is not acknowledged (user does not click acknowledgment button) within 15 minutes, the app MUST automatically retry the reminder
- Retry MUST use the same notification types (sound, vibration, visual) unless phone call is active

**REQ-005.2: Retry Behavior**
- Retry reminders MUST follow the same conflict handling rules as initial reminders
- After retry, timer behavior MUST follow REQ-001.4 (Best Effort Recovery)

### 3.6 Notification System (REQ-006)

**REQ-006.1: Notification Permissions**
- The app MUST request notification permissions on first launch
- The app MUST handle permission denial gracefully with clear instructions
- The app MUST explain why notifications are critical for medical purposes

**REQ-006.2: Notification Reliability**
- Notifications MUST work when:
  - App is in foreground
  - App is in background
  - App is closed
  - Phone is locked
  - Phone is in DND mode (overridden)

**REQ-006.3: Notification Persistence**
- Reminder notifications MUST persist until dismissed by user
- Notifications MUST appear in notification center even if initially missed

### 3.7 First-Time Setup (REQ-007)

**REQ-007.1: Onboarding Flow**
- The app MUST provide a first-time setup flow that includes:
  - Welcome screen explaining app purpose
  - Notification permission request
  - Optional: Wake time and sleep time collection (for future use)
  - Simple tutorial (3-4 screens) showing main features

**REQ-007.2: Setup Completion**
- After setup, the app MUST immediately start the reminder schedule
- The app MUST remember setup completion (no repeat setup on subsequent launches)

### 3.8 Settings (REQ-008)

**REQ-008.1: Settings Page**
- The app MUST provide a settings page accessible from the main screen
- Settings page MUST include option to show/hide acknowledgment button
- Acknowledgment button visibility setting MUST be saved and persist across app restarts

**REQ-008.2: Default Behavior**
- Acknowledgment button MUST be hidden by default
- User can enable acknowledgment button visibility via settings page

---

## 4. Non-Functional Requirements

### 4.1 Performance Requirements

**NFR-001: Reminder Accuracy**
- Reminders MUST trigger within ±1 minute of scheduled time
- Timer calculations MUST maintain accuracy across app restarts and device reboots

**NFR-002: Battery Efficiency**
- The app MUST minimize battery drain while maintaining reminder reliability
- Background processes MUST be optimized for battery life

**NFR-003: App Responsiveness**
- Main screen MUST load within 1 second
- User interactions (dismissal, sleep toggle) MUST respond within 0.5 seconds

### 4.2 Reliability Requirements

**NFR-004: Uptime**
- Reminder delivery rate MUST be >99%
- The app MUST handle system interruptions gracefully (low battery, OS updates, etc.)

**NFR-005: Data Persistence**
- Sleep mode state MUST persist across app restarts
- Reminder schedule MUST persist across app restarts
- Timer state MUST be recoverable after unexpected app termination

### 4.3 Usability Requirements

**NFR-006: Accessibility**
- The app MUST support users with visual impairments (large text, high contrast)
- The app MUST support users with hearing impairments (visual and vibration notifications)
- All interactive elements MUST meet minimum touch target size (44x44 points)

**NFR-007: Learnability**
- Users MUST be able to use core features without reading documentation
- Interface MUST be immediately understandable
- Tutorial MUST be optional and skippable

### 4.4 Compatibility Requirements

**NFR-008: Platform Support**
- The app MUST support iOS 12+ and Android 8+ (minimum versions)
- The app MUST work on both smartphones and tablets
- The app MUST adapt to different screen sizes

**NFR-009: Time Zone Handling**
- The app MUST automatically adjust to local time zone changes
- Reminder schedule MUST maintain consistency across time zone changes

### 4.5 Security and Privacy Requirements

**NFR-010: Data Privacy**
- All data MUST be stored locally on device (no cloud storage for MVP)
- No user data MUST be transmitted to external servers
- No user tracking or analytics beyond basic app functionality

**NFR-011: Permissions**
- The app MUST request only necessary permissions
- Permission requests MUST include clear explanations of why they are needed

---

## 5. User Interface Requirements

### 5.1 Design Principles

**UI-001: Simplicity First**
- Every screen MUST be immediately understandable
- No unnecessary features or options on main screen

**UI-002: Large Touch Targets**
- All buttons and interactive elements MUST be at least 44x44 points
- Touch targets MUST have adequate spacing between them

**UI-003: Clear Visual Hierarchy**
- Next reminder time MUST be the most prominent information
- Sleep mode status MUST be clearly visible
- Countdown timer MUST be easily readable

**UI-004: Minimal Cognitive Load**
- Users MUST NOT need to remember anything
- All necessary information MUST be visible on main screen
- No hidden menus or complex navigation

**UI-005: Forgiving Design**
- Accidental actions (e.g., toggling sleep mode) MUST be easily reversible
- Clear visual feedback for all actions

**UI-006: Non-Intrusive**
- Reminders MUST be helpful, not annoying
- Notification messages MUST be simple and friendly

**UI-007: Accessibility**
- Support for system accessibility features (VoiceOver, TalkBack)
- High contrast mode support
- Scalable text support

### 5.2 Screen Requirements

**UI-008: Main Screen**
- MUST display countdown timer (large, prominent)
- MUST display next reminder time
- MUST display sleep mode toggle (large, clear)
- MUST display acknowledgment button when reminder is active (if enabled in settings)
- MUST use clear, readable fonts (minimum 16pt for body text)

**UI-009: Notification Design**
- Notification MUST include clear message about reminder purpose
- Notification MUST provide quick acknowledgment action (if enabled in settings)
- Notification MUST be visually distinct and attention-grabbing
- Notification MUST be full screen and noticeable enough

---

## 6. Edge Cases and Scenarios

### 6.1 Scenario 1: User Forgets to Toggle Sleep Mode

**Problem**: Reminders continue during sleep

**Requirements**:
- Sleep toggle MUST be large and prominent on main screen
- Clear visual indicators MUST show sleep mode status
- App MAY provide gentle reminder if sleep mode hasn't been toggled in 24 hours

### 6.2 Scenario 2: User Misses Reminder

**Problem**: User doesn't see/hear reminder

**Requirements**:
- Multiple notification types MUST be used (sound + vibration + visual)
- Reminder MUST persist until acknowledged (or dismissed from notification center)
- Retry mechanism MUST activate after 15 minutes if not acknowledged

### 6.3 Scenario 3: User Travels/Changes Time Zone

**Problem**: Reminder schedule may be disrupted

**Requirements**:
- App MUST automatically adjust to local time zone
- Reminder schedule MUST maintain 2-hour intervals relative to local time
- No user action required for time zone changes

### 6.4 Scenario 4: Phone Battery Dies

**Problem**: Reminders stop working

**Requirements**:
- App SHOULD warn user if battery is below 20%
- Reminders MUST resume automatically when phone is charged and powered on
- Timer MUST continue from last known state (best effort)

### 6.5 Scenario 5: Reminder Conflicts with Phone Call

**Problem**: Reminder triggers during important phone call

**Requirements**:
- During active phone call: Visual notification ONLY (no sound/vibration)
- Reminder MUST still appear and persist
- Retry mechanism MUST handle extended calls (retry after 15 minutes)
- Timer behavior MUST follow hybrid approach (REQ-001.4)

### 6.6 Scenario 6: User Accidentally Enables DND

**Problem**: DND blocks critical medical reminders

**Requirements**:
- App MUST override DND mode for reminders
- Health reminders MUST have priority over convenience settings
- This behavior MUST be clearly documented to user

### 6.7 Scenario 7: App Killed by Operating System

**Problem**: OS may kill app to free memory

**Requirements**:
- Reminder schedule MUST be recoverable after app restart
- Background service MUST be configured for high priority
- App MUST reschedule reminders on launch if needed

---

## 7. Out of Scope (Explicitly Excluded from MVP)

The following features are **NOT** included in MVP and must not be implemented:

- ❌ Compliance history view
- ❌ Statistics dashboard
- ❌ Family alert system
- ❌ Multi-device sync
- ❌ Automatic sleep detection
- ❌ Customization options (sounds, vibration patterns, etc.)
- ❌ Cloud backup
- ❌ User accounts or authentication
- ❌ Integration with health apps (Apple Health, Google Fit)
- ❌ Reminder frequency customization (fixed at 2 hours)
- ❌ Multiple reminder schedules
- ❌ Medication tracking
- ❌ Doctor/provider data sharing

---

## 8. Success Criteria

### 8.1 Primary Success Metrics

**METRIC-001: Reminder Delivery Rate**
- **Target**: >99% of scheduled reminders actually appear
- **Measurement**: System logs and notification delivery tracking

**METRIC-002: Daily Active Usage**
- **Target**: >80% of days user opens app or acknowledges reminder
- **Measurement**: App analytics

**METRIC-003: User Retention**
- **Target**: >70% of users continue using app after 30 days
- **Measurement**: User analytics

### 8.2 Secondary Success Metrics

**METRIC-004: Sleep Mode Usage**
- **Target**: Daily usage (once in morning, once at night)
- **Purpose**: Understand user sleep patterns and app effectiveness

**METRIC-005: Acknowledgment Rate**
- **Target**: >80% of reminders acknowledged (either initially or after retry)
- **Purpose**: Measure user engagement and reminder effectiveness
- **Note**: Only applicable if acknowledgment feature is enabled in settings

### 8.3 User Satisfaction Metrics

**METRIC-006: App Store Rating**
- **Target**: >4.0 stars average rating
- **Measurement**: App store reviews

**METRIC-007: Support Requests**
- **Target**: <5% of users need support
- **Measurement**: Support tickets and user inquiries

### 8.4 MVP Launch Criteria

The MVP is considered ready for launch when ALL of the following are true:

- ✅ App successfully delivers reminders every 2 hours
- ✅ Sleep mode toggle works reliably
- ✅ User can understand and use app without help
- ✅ Reminders work when app is closed/phone is locked
- ✅ DND override works correctly
- ✅ Retry mechanism functions as specified
- ✅ No critical bugs that prevent core functionality
- ✅ All functional requirements (REQ-001 through REQ-007) are implemented and tested

---

## 9. Assumptions and Constraints

### 9.1 Assumptions

- User has a smartphone (iOS or Android)
- User is comfortable with basic smartphone usage
- User carries phone throughout the day
- User has internet connection for app download (not required for operation)
- User understands the medical need for 2-hour reminders

### 9.2 Constraints

- **Budget**: Cost-controlled development approach
- **Timeline**: MVP focus (no future features)
- **Platform**: Must work on both iOS and Android
- **Storage**: Local-only (no cloud for MVP)
- **Medical Requirement**: Fixed 2-hour interval (not configurable)
- **Sleep Constraint**: Reminders only during waking hours

---

## 10. Dependencies

### 10.1 External Dependencies

- iOS App Store and Google Play Store for distribution
- Platform notification services (APNs for iOS, FCM for Android)
- Device operating system (iOS 12+, Android 8+)

### 10.2 Internal Dependencies

- User must grant notification permissions
- Device must have sufficient battery for background operation
- Device must have local storage available

---

## 11. Risk Mitigation

### 11.1 Technical Risks

**RISK-001: Notification Reliability**
- **Mitigation**: Use platform-native notification APIs, test thoroughly across devices
- **Fallback**: Multiple notification types ensure at least one is received

**RISK-002: Battery Drain**
- **Mitigation**: Optimize background service, use efficient scheduling
- **Monitoring**: Track battery usage during development and testing

**RISK-003: App Killed by OS**
- **Mitigation**: Configure background service with high priority, implement recovery on app restart

### 11.2 User Experience Risks

**RISK-004: User Forgets to Use App**
- **Mitigation**: Simple interface, prominent app icon, reminders work in background

**RISK-005: User Doesn't Understand Sleep Mode**
- **Mitigation**: Clear visual indicators, simple tutorial, help text

**RISK-006: Reminders Become Annoying**
- **Mitigation**: Smart conflict handling, visual-only during calls, retry mechanism

**RISK-007: Elderly User Accidentally Enables DND**
- **Mitigation**: App overrides DND mode - reminders always work

---

## 12. Glossary

- **BPH**: Benign Prostatic Hyperplasia
- **DND**: Do Not Disturb mode
- **MVP**: Minimum Viable Product
- **APNs**: Apple Push Notification service
- **FCM**: Firebase Cloud Messaging (Android push notifications)

---

## 13. Document History

| Version | Date | Author | Changes |
|--------|------|--------|---------|
| 1.0.1 | December 2024 | PM | Updated: Date format, full-screen notification requirement, acknowledgment logic clarification, acknowledgment button hidden by default with settings option |
| 1.0.0 | December 2024 | PM | Initial requirements document based on finalized product plan |

---

## 14. Approval

This requirements document has been finalized and approved for MVP development.

**Status**: ✅ Approved for Implementation

---

**End of Requirements Document**

