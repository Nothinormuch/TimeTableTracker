# UI Style Guide - Timetable Tracker

## Overview
This document defines a comprehensive modern UI system for the Timetable Tracker Android app. The goal is to create a clean, professional, and consistent user experience that eliminates the cheap appearance and inconsistent design elements currently present in the app.

## Design Philosophy
- **Clean & Professional**: Eliminate cheap emojis and unprofessional visuals
- **Material Design 3**: Follow Google's Material Design 3 guidelines for consistency
- **High Contrast**: Ensure all UI elements have sufficient color contrast for accessibility
- **Consistent Patterns**: All app screens follow the same design language
- **Interactive & Intuitive**: Clear affordances and smooth feedback states
- **Modern Aesthetics**: Remove dated UI patterns in favor of contemporary design

## Color Palette

### Primary Colors (UPES Brand Reference)
```yaml
primary: {
  50: #F0F7FF,
  100: #E3F2FD,
  200: #BBDEFB,
  400: #2196F3,  # Primary blue
  500: #2196F3,
  600: #1E88E5,
  700: #1976D2,
  900: #0D47A1
}
secondary: {
  50: #FFF3E0,
  100: #FFE0B2,
  400: #FFA726,  # Warm orange for alerts
  500: #FF9800,
  700: #F57C00
}
error: {
  50: #FFEBEE,
  400: #EF5350,
  500: #F44336,
  700: #D32F2F
}
success: {
  50: #E8F5E9,
  400: #66BB6A,
  500: #4CAF50,
  700: #388E3C
}
warning: {
  50: #FFF8E1,
  400: #FFA726,
  500: #FF9800,
  700: #F57C00
}
gray: {
  50: #FAFAFA,
  100: #F5F5F5,
  200: #EEEEEE,
  400: #9E9E9E,
  500: #757575,
  600: #616161,
  800: #424242,
  900: #212121
}
```

### Semantic Colors
| Status | Light Mode | Dark Mode | Usage |
|--------|------------|-----------|-------|
| Added (NEW) | #4CAF50 | #66BB6A | Added classes |
| Removed (DELETED) | #F44336 | #EF5350 | Deleted classes |
| Modified (RESCHEDULED) | #FF9800 | #FFA726 | Changed classes |
| Unchanged | #757575 | #9E9E9E | Normal slots |
| Pending Check | #2196F3 | #42A5F5 | Future checks |
| Error | #D32F2F | #EF5350 | Connection errors |

## Typography

### Font Hierarchy
```yaml
Display (App Title):
  Font: Roboto Serif Bold
  Size: 28sp
  Letter Spacing: -0.5px
  Line Height: 36sp
  Weight: Bold

Heading (Screen Titles):
  Font: Roboto Bold
  Size: 20sp
  Line Height: 28sp
  Weight: Bold

Subheading (Section Headers):
  Font: Roboto Medium
  Size: 16sp
  Line Height: 24sp
  Weight: Medium

Body (Main Content):
  Font: Roboto Regular
  Size: 14sp
  Line Height: 20sp
  Weight: Regular

Body Strong:
  Font: Roboto Medium
  Size: 14sp
  Line Height: 20sp
  Weight: Medium

Caption (Secondary Text):
  Font: Roboto Regular
  Size: 12sp
  Line Height: 16sp
  Weight: Regular

Label (Buttons, Chips):
  Font: Roboto Medium
  Size: 14sp
  Line Height: 16sp
  Weight: Medium
```

### Typography Usage
| Element | Typography | Color |
|---------|------------|-------|
| App Name | Display | Primary |
| Screen Title | Heading | On Surface |
| Status Badges | Label | On Primary | |
| Class Time | Body Strong | Primary |
| Class Subject | Body Strong | On Surface |
| Venue/Faculty | Body | On Surface Variant |
| Change Details | Caption | On Surface Variant |
| Empty State Text | Heading | On Surface |

## Component Library

### 1. Cards
```xml
<com.google.android.material.card.MaterialCardView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:cardCornerRadius="12dp"
    app:cardElevation="2dp"
    app:cardBackgroundColor="@android:color/white"
    android:foreground="@drawable/card_selector"
    ...>
```

**Card States:**
- **Default**: White background, subtle elevation
- **Selected**: Primary color tint
- **Pressed**: Ripple effect with reduced elevation

### 2. Buttons

#### Primary Button
```xml
<com.google.android.material.button.MaterialButton
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    style="@style/Widget.Material3.Button.Button"
    android:backgroundTint="@color/primary"
    android:textColor="@color/white"
    ...>
```

#### Secondary Button
```xml
<com.google.android.material.button.MaterialButton
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    style="@style/Widget.Material3.Button.OutlinedButton"
    android:textColor="@color/primary"
    android:strokeColor="@color/primary"
    ...>
```

#### Floating Action Button
```xml
<com.google.android.material.floatingactionbutton.FloatingActionButton
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:backgroundTint="@color/primary"
    ...>
```

### 3. Chips (Day Selector)
```xml
<com.google.android.material.chip.Chip
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    app:chipBackgroundColor="@color/primary/10"
    app:chipCornerRadius="20dp"
    app:chipStrokeColor="@color/primary"
    app:chipStrokeWidth="1dp"
    app:chipIconSize="20dp"
    app:rippleColor="@color/primary/20"
    ...>
```

**Chip States:**
- **Normal**: Subtle background, primary border
- **Selected**: Solid primary color, white text
- **Active with Changes**: Red border indicator

### 4. Status Badges
```xml
<TextView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_margin="4dp"
    android:paddingHorizontal="8dp"
    android:paddingVertical="4dp"
    android:textSize="12sp"
    android:textStyle="bold"
    android:textColor="@android:color/white"
    android:background="@drawable/bg_status_badge_rounded"
    ... />
```

### 5. Inputs
```xml
<com.google.android.material.textfield.TextInputLayout
    style="@style/Widget.Material3.TextInputLayout.OutlinedBox"
    ...>

<com.google.android.material.textfield.TextInputEditText
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:textColor="@color/onSurface"
    android:textColorHint="@color/onSurfaceVariant"
    ... />
```

### 6. Switches
```xml
<com.google.android.material.switchmaterial.SwitchMaterial
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:thumbTintList="@color/primary"
    android:trackTintList="@color/primary/20"
    ... />
```

## Iconography System

### Replace Emoji Icons
**Eliminate:** 📅, ⏰, 📚, 📍, 👤, ℹ️, 🔗

**Use Material Icons:**
- Calendar: `ic_calendar` (fill=0, size=24dp)
- Time: `ic_time` (fill=0, size=24dp)
- Subject: `ic_book` (fill=0, size=24dp)
- Location: `ic_location` (fill=0, size=24dp)
- Person: `ic_person` (fill=0, size=24dp)
- Info: `ic_info` (fill=0, size=24dp)
- Link: `ic_link` (fill=0, size=24dp)

### Icon Design Principles
- **Size**: Consistent 24dp for action icons, 20dp for secondary
- **Fill**: Use outline icons (fill=0) for better integration
- **Color**: Use semantic colors matching text colors
- **Contrast**: Ensure icons have adequate color contrast
- **States**: Define hover/pressed states with appropriate opacity changes

## Notification Messages

Notifications are product copy, not chat messages. They must be terse, factual, and free of emoji and filler. Follow Android notification structure: **title = what happened**, **body = the specific detail**, no greeting, no sign-off, no exclamation marks.

### Tone Rules
- No emoji anywhere in notifications, snackbars, dialogs, or empty states.
- Sentence case, never Title Case. Full sentences in the body, fragments allowed in the title.
- Lead with the noun: "Room changed", not "Heads up! The room has been changed."
- Quantify: "2 classes changed" beats "Some changes were found".
- Never say "We" — the app is a tool, not a persona. "Credentials rejected by portal", not "We couldn't log you in".

### System Notification Templates
| Event | Title | Body |
|-------|-------|------|
| Changes found | `Timetable updated` | `2 changes: Advanced Algorithms moved to Room 10206; Database Systems cancelled on Fri.` |
| Single change | `Class change detected` | `Network Security on Tue 09:00 moved from Room 10201 to Room 10206.` |
| Class added | `New class added` | `Machine Learning added on Wed 14:00, Room 30106.` |
| Class removed | `Class removed` | `Software Engineering on Thu 11:00 is no longer on your timetable.` |
| Check failed | `Check failed` | `Could not reach the portal. Will retry at the next scheduled check.` |
| Auth failure | `Login expired` | `Open Timetable Tracker to update your portal credentials.` |
| No changes | *(no notification — silent success)* | — |

### In-App Snackbar Messages
| Situation | Message |
|-----------|---------|
| Missing credentials | `Enter your email and password first.` |
| Login success | `Credentials verified.` |
| Login failure | `Login failed: {reason}. Check your credentials.` |
| Time saved | `Daily check set for {time}.` |
| Settings saved | `Settings saved.` |
| Exact alarm not granted | `Alarms & reminders permission missing. Checks may be delayed.` |
| Manual check started | `Checking timetable…` |

### Change Detail Lines (on slot cards)
Written as what changed, old → new, no "Note:" prefix:
- `Room 10201 → 10206`
- `Time moved to 14:00 – 14:55`
- `Faculty: Dr. Sharma → Dr. Rao`
- `Cancelled`
- `Added to your timetable`

### Empty States
- No timetable: "No timetable yet" / "Log in and run a check to load your schedule." + primary button `Check now`
- Empty day: "No classes on this day."
- Not logged in (home): "Not connected" / "Add your portal credentials in Settings to start monitoring." + button `Go to Settings`

### Dialogs
- Title: the decision, not the topic. `Grant exact alarm permission?` not `Permission Required`.
- Body: one sentence of why, one of what happens next.
- Buttons: verb phrases (`Open settings` / `Not now`), never `OK`/`Cancel`.

### System Notifications
```xml
<com.google.android.material.snackbar.Snackbar
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:background="@drawable/bg_snackbar"
    android:textColor="@color/white"
    ... />
```

**Snackbar Design:**
- **Success**: Green background with checkmark icon
- **Error**: Red background with alert icon
- **Warning**: Orange background with warning icon
- **Info**: Blue background with info icon

### In-App Toast/Alert
```xml
<com.google.android.material.card.MaterialCardView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:cardCornerRadius="8dp"
    app:cardElevation="4dp"
    android:background="@color/surface"
    ...>
```

## Slot Cards Redesign

### Base Slot Card
```xml
<com.google.android.material.card.MaterialCardView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:cardCornerRadius="12dp"
    app:cardElevation="2dp"
    android:layout_marginBottom="12dp"
    android:foreground="@drawable/slot_card_selector"
    ...>
```

### Slot Border Indicators
**Replace emoji badges with visual borders:**
- **Added**: 4dp green border (#4CAF50)
- **Deleted**: 4dp red border with strikethrough (#F44336)
- **Rescheduled**: 4dp orange border (#FF9800)
- **Unchanged**: 2dp light gray border (#E0E0E0)

### Slot Text Hierarchy
```xml
<!-- Time (Strong) -->
<TextView
    android:textSize="16sp"
    android:textStyle="bold"
    android:textColor="@color/primary" />

<!-- Subject (Strong) -->
<TextView
    android:textSize="15sp"
    android:textStyle="bold"
    android:textColor="@color/onSurface" />

<!-- Venue/Faculty (Regular) -->
<TextView
    android:textSize="13sp"
    android:textColor="@color/onSurfaceVariant" />

<!-- Change Detail (Caption) -->
<TextView
    android:textSize="11sp"
    android:textStyle="italic"
    android:textColor="@color/primary" />
```

## Home Screen Layout

### Status Card
```xml
<com.google.android.material.card.MaterialCardView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:cardCornerRadius="16dp"
    app:cardElevation="4dp"
    android:background="@color/surface"
    android:elevation="4dp">

    <LinearLayout
        android:orientation="vertical"
        android:padding="20dp">
        
        <!-- Header with Status Badge -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal">
            
            <TextView
                android:text="Monitoring Status"
                android:textSize="16sp"
                android:textStyle="bold"
                android:layout_weight="1" />
            
            <TextView
                android:id="@+id/tv_status_badge"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:paddingHorizontal="12dp"
                android:paddingVertical="6dp"
                android:textSize="12sp"
                android:textStyle="bold"
                android:textColor="@android:color/white"
                android:background="@drawable/bg_status_badge_rounded" />
        </LinearLayout>
        
        <!-- Last/Next Check Times -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:layout_marginTop="8dp">
            
            <TextView
                android:id="@+id/tv_last_checked"
                android:textSize="14sp"
                android:textColor="@color/onSurfaceVariant" />
            
            <TextView
                android:id="@+id/tv_next_check"
                android:textSize="14sp"
                android:textColor="@color/primary"
                android:textStyle="medium" />
        </LinearLayout>
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

### Day Selector (Improved)
```xml
<HorizontalScrollView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:scrollbars="none"
    android:layout_marginBottom="16dp">

    <com.google.android.material.chip.ChipGroup
        android:id="@+id/chip_group_days"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        app:singleSelection="true"
        app:chipSpacingHorizontal="8dp"
        app:chipSpacingVertical="8dp">
    </com.google.android.material.chip.ChipGroup>
</HorizontalScrollView>
```

**Chip Design:**
- **Active Day**: Solid primary color, white text
- **Day with Changes**: Red border indicator + change count badge
- **Inactive Day**: Subtle background, muted text
- **Hover State**: Scale animation + subtle background change

## Settings Screen Layout

### Login Card
```xml
<com.google.android.material.card.MaterialCardView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:cardCornerRadius="12dp"
    app:cardElevation="2dp"
    android:background="@color/surface">

    <LinearLayout
        android:orientation="vertical"
        android:padding="20dp"
        android:layout_marginBottom="16dp">
        
        <!-- Section Title -->
        <TextView
            android:text="Login Settings"
            android:textSize="16sp"
            android:textStyle="bold"
            android:layout_marginBottom="16dp" />
        
        <!-- Email Input -->
        <com.google.android.material.textfield.TextInputLayout
            style="@style/Widget.Material3.TextInputLayout.OutlinedBox"
            android:layout_marginBottom="12dp">
            
            <com.google.android.material.textfield.TextInputEditText
                android:hint="Email"
                android:inputType="textEmailAddress" />
        </com.google.android.material.textfield.TextInputLayout>
        
        <!-- Password Input -->
        <com.google.android.material.textfield.TextInputLayout
            style="@style/Widget.Material3.TextInputLayout.OutlinedBox"
            android:layout_marginBottom="16dp">
            
            <com.google.android.material.textfield.TextInputEditText
                android:hint="Password"
                android:inputType="textPassword"
                android:passwordToggleEnabled="true" />
        </com.google.android.material.textfield.TextInputLayout>
        
        <!-- Login Button -->
        <com.google.android.material.button.MaterialButton
            android:text="Login & Verify"
            android:layout_height="wrap_content"
            android:layout_marginBottom="12dp" />
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

## Animation & Interaction

### Shared Element Transitions
- **Card Layout Changes**: Smooth scale and fade animations
- **Chip Selection**: Ripple effect + scale animation
- **Slot Expansion**: Slide up animation for details

### Loading States
```xml
<ProgressBar
    android:layout_width="match_parent"
    android:layout_height="3dp"
    android:layout_marginTop="8dp"
    android:visibility="gone"
    android:indeterminate="true"
    style="@style/Widget.AppCompat.ProgressBar.Horizontal" />
```

## Dark Mode Support

### Color Adjustments for Dark Theme
- **Surface**: #121212 (vs #FFFFFF)
- **On Surface**: #FFFFFF (vs #000000)
- **On Surface Variant**: #B3B3B3 (vs #757575)
- **Primary**: #4A90E2 (slightly brighter for visibility)
- **Error**: #EF5350 (unchanged)
- **Success**: #66BB6A (slightly brighter)

### Contrast Verification
Ensure all color combinations meet WCAG AA standards:
- **Normal Text**: 4.5:1 minimum
- **Large Text**: 3:1 minimum
- **Icons**: 3:1 minimum

## Component State Definitions

### Button States
| State | Background | Text | Border | Icon |
|-------|------------|------|--------|------|
| Enabled | Primary | White | None | White |
| Disabled | Surface Variant | On Surface / 38% | Surface Variant | On Surface / 38% |
| Pressed | Primary Dark | White | None | White |
| Focused | Primary / 10% | Primary | Primary | Primary |

### Chip States
| State | Background | Text | Border | Icon |
|-------|------------|------|--------|------|
| Normal | Transparent | Primary | Primary | Primary |
| Selected | Primary | White | None | White |
| Active with Changes | Transparent | Primary | Red | Primary |
| Disabled | Surface | On Surface / 38% | Surface Variant | On Surface / 38% |

### Card States
| State | Elevation | Background | Ripple | Border |
|-------|-----------|------------|--------|--------|
| Default | 2dp | Surface | Primary / 8% | None |
| Selected | 4dp | Primary / 10% | Primary / 12% | Primary |
| Pressed | 1dp | Primary / 5% | Primary / 12% | Primary |

## Accessibility Guidelines

### Text Contrast
- Ensure all text meets minimum contrast ratios
- Use semantic colors for status indicators
- Provide color-independent indicators (icons, patterns)

### Touch Targets
- Minimum touch target: 48dp × 48dp
- Icon-only buttons: Ensure adequate padding around icon
- Switch targets: Follow Material Design touch target guidelines

### Focus Indicators
- Visible focus ring on all interactive elements
- High contrast focus indicator
- Don't rely on color alone for state indication

## Implementation Notes

### Migration Steps
1. **Phase 1**: Update color resources to new palette
2. **Phase 2**: Replace emoji icons with Material icons
3. **Phase 3**: Update layout files to use new component styles
4. **Phase 4**: Implement state animations and transitions
5. **Phase 5**: Add accessibility improvements
6. **Phase 6**: Dark mode support
7. **Phase 7**: Component consistency checks

### Resources to Update
- `res/colors.xml` - New color palette
- `res/drawable/` - New icon set and selectors
- `res/layout/` - Updated XML layouts
- `res/values/styles.xml` - Component styles
- `app/src/main/res/menu/` - Menu icons
- All drawable resources

### Testing Checklist
- [ ] Visual consistency across all screens
- [ ] Color contrast compliance
- [ ] Touch target sizes
- [ ] Focus navigation
- [ ] Screen reader compatibility
- [ ] Dark mode support
- [ ] Animation performance
- [ ] State consistency

This style guide provides a foundation for creating a modern, consistent, and accessible UI for the Timetable Tracker app. The design eliminates cheap aesthetics, ensures proper contrast, and establishes clear component patterns that work across all app screens.