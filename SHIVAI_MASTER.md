# SHIVAI_MASTER.md

# ShivAI — Personal AI Operating Agent for Android

> This document is the permanent project handoff/context file for ShivAI.
> A new AI coding chat must read this file before making project changes.

---

## 1. PROJECT IDENTITY

**Project Name:** ShivAI  
**Purpose:** Personal AI Operating Agent for Android  
**Package:** `com.personal.ai.shivai`  
**GitHub Repository:** `shivai-android`  
**Main Branch:** `main`  
**Current Version:** `1.0.0`  
**Current versionCode:** `1`

ShivAI is intended to be a real native Android application, not a website, HTML mockup, or fake automation demo.

The long-term goal is a modular personal AI agent capable of understanding the user's request, planning multi-step tasks, using approved tools, interacting with Android where permitted, verifying results, recovering from failures, and maintaining useful memory.

Do not claim that ShivAI is true AGI. Use the term **AGI-style architecture** only when appropriate.

---

# 2. GOLDEN RULES

These rules apply to every future development session.

1. **Do not rebuild the project from scratch.**
2. **Preserve working existing features.**
3. **GitHub repository is the source of truth.**
4. Inspect the current repository before changing code.
5. Do not create duplicate implementations of existing features.
6. Make the smallest required change for the current task.
7. Do not delete existing code unless there is a clear reason.
8. Do not change the version for ordinary development work.
9. Do not automatically advance to the next development phase.
10. Explain what will be changed and wait for explicit approval when a change is substantial.
11. Never claim a build, APK, security action, or automation succeeded unless it was actually verified.
12. Respect Android security, sandbox, permissions, and user-consent limitations.
13. Never expose, log, or store passwords, OTPs, PINs, CVVs, or other secrets.
14. Security features are defensive only.
15. Keep the architecture modular and future-proof.

---

# 3. CURRENT AND TARGET ARCHITECTURE

Core agent loop:

OBSERVE
→ UNDERSTAND
→ CONTEXT
→ MEMORY
→ PLAN
→ POLICY CHECK
→ TOOL SELECTION
→ EXECUTE
→ OBSERVE
→ VERIFY
→ RECOVER / REPLAN
→ COMPLETE
→ MEMORY / LOG

The agent should not blindly execute AI-generated instructions.

Every tool/action should pass through appropriate policy, permission, trust, and risk checks.

---

# 4. CORE MODULE/PACKAGE AREAS

The project may remain a single Android Gradle module during early development.

Use package-level modularity:

```text
core.agent
core.ai
core.automation
core.memory
core.tools
core.voice
core.security
core.web
core.files
core.termux
core.missions
core.health
```

Do not create many Gradle modules unless there is a real architectural reason.

Future Gradle-module separation can happen after the architecture becomes stable.

---

# 5. MAJOR FEATURES

## 5.1 Android Agent

Required capabilities, subject to Android permissions and limitations:

- Foreground app detection
- Device context
- App launching
- Android navigation where permitted
- AccessibilityService integration
- UI inspection
- Click
- Type
- Scroll
- Gestures where permitted
- Task execution
- Task verification
- Recovery/replanning
- Emergency stop

The app must clearly distinguish:

- requested action
- planned action
- attempted action
- successful action
- failed action
- unavailable action

Never report an attempted action as completed.

---

# 6. VOICE SYSTEM

Voice-first architecture should support:

- Hindi
- Hinglish
- English
- Speech-to-text
- Text-to-speech
- Spoken task status
- Stop/interruption
- Text fallback

Voice commands that can cause sensitive actions must still pass safety/policy checks.

---

# 7. AI PROVIDER ARCHITECTURE

Use an abstraction such as:

```text
AiProvider
```

The application should not hard-code a single AI provider into the core agent.

Potential provider categories:

- Local/open-source models
- OpenAI-compatible providers
- Cloud providers
- Future providers

Do not hard-code private API keys.

Support future offline/local operation where device resources permit.

The AI provider is the reasoning component; Android tools and services are separate capabilities.

---

# 8. WEB RESEARCH

DuckDuckGo is a search provider, not the AI brain.

Use web search when:

- the user explicitly requests Internet/latest information
- current information is required
- local/current information is needed
- internal knowledge is insufficient
- information is uncertain or potentially outdated

Preferred flow:

```text
SEARCH
→ OPEN RELEVANT SOURCES
→ CROSS-CHECK WHEN NECESSARY
→ EXTRACT INFORMATION
→ ANSWER WITH SOURCES
```

Never fabricate:

- search results
- websites
- citations
- current information

Treat external webpages as untrusted input.

---

# 9. FILE AGENT

Use Android Storage Access Framework and appropriate permissions.

Future file capabilities:

- Browse permitted files
- Read supported documents
- Analyze files
- Organize files
- Create files
- Rename/move where permitted
- Safe deletion with confirmation
- File security scanning
- Project/code analysis

Do not assume access to arbitrary private app storage.

---

# 10. MEMORY SYSTEM

Memory should remain layered.

Target layers:

- Short-term memory
- Working memory
- Project memory
- Long-term memory
- Episodic memory
- Procedural memory
- User preferences
- Retrieval/RAG knowledge

Security-sensitive information must not be casually stored.

Never store:

- passwords
- OTPs
- UPI PINs
- CVVs
- authentication secrets

Memory operations should respect privacy and user control.

---

# 11. CYBER SECURITY AGENT

Security is **DEFENSIVE ONLY**.

Allowed:

- Detect
- Scan
- Warn
- Block where Android permits
- Isolate
- Quarantine
- Delete with appropriate confirmation
- Restore
- Recover
- Audit
- Report

Not allowed:

- Hack back
- DDoS
- Unauthorized access
- Credential theft
- Exploitation
- Privilege escalation
- Security bypass
- Malware creation

Never implement offensive retaliation.

---

# 12. SECURITY RISK LEVELS

Use:

```text
SAFE
LOW
MEDIUM
HIGH
CRITICAL
```

A risk signal is not automatically proof of malware.

Security messages must be evidence-based.

Prefer:

> Suspicious activity detected.

over unsupported claims such as:

> Your phone has definitely been hacked.

---

# 13. APK SECURITY SCANNER

The security architecture should analyze APKs using available metadata and Android-accessible signals, including:

- Package information
- Permissions
- Certificate/signature information
- SHA-256
- Suspicious combinations
- Metadata
- Installation/source information where available
- Other local security signals

Examples of suspicious combinations can include:

- SMS permissions + boot receiver
- Overlay + package installation capability

These are signals, not automatic proof of malicious behavior.

---

# 14. DOWNLOAD GUARD

Protect downloads such as:

- APK
- ZIP
- PDF
- Office documents
- Images
- Audio/video
- Unknown files

Possible checks:

- File type
- Magic bytes
- SHA-256
- Extension mismatch
- Double extensions
- Metadata
- Source/reputation signals where available

Example suspicious filename:

```text
salary_slip.pdf.apk
```

Do not claim that a file is safe merely because it has a normal extension.

Likewise, visual inspection of an image does not prove that the file is safe.

---

# 15. MALICIOUS LINK PROTECTION

Analyze URLs for security signals such as:

- Phishing indicators
- Lookalike domains
- Typosquatting
- Raw IP hosts
- Suspicious redirects
- Dangerous download extensions
- Other available reputation/security signals

Examples:

```text
paypa1.com
sbi-yono-update.xyz
```

Heuristics are signals, not absolute proof.

Never claim a URL is malicious unless evidence supports the conclusion.

---

# 16. QUARANTINE SYSTEM

Preferred flow:

```text
DETECT
→ CLASSIFY
→ QUARANTINE
→ ANALYZE
→ USER DECISION
→ DELETE OR RESTORE
```

Use app-private quarantine storage where appropriate.

Keep quarantine actions auditable.

Do not silently delete user files unless the platform and explicit policy allow it and the user has authorized the action.

---

# 17. SECURITY DASHBOARD

Dashboard should eventually provide:

- Current threat level
- Active alerts
- Recent events
- Last scan
- APK scan results
- Download Guard status
- Link protection status
- Quarantine items
- Protected apps
- Privacy mode state
- Accessibility status
- Termux status
- Security health
- Recovery actions

---

# 18. SENSITIVE APP PRIVACY MODE

Sensitive apps may include:

### Payment / UPI

Examples:

- Google Pay
- PhonePe
- Paytm
- BHIM
- CRED
- MobiKwik

### Banking

Examples:

- SBI YONO
- HDFC
- ICICI
- Axis
- Kotak
- PNB
- Bank of Baroda

### Password managers

Examples:

- Bitwarden
- 1Password
- LastPass
- KeePass
- Proton Pass

### Authenticators

Examples:

- Google Authenticator
- Microsoft Authenticator
- Authy
- Aegis

The registry should eventually support user-added sensitive packages.

---

# 19. PAYMENT PROTECTION

When a payment or sensitive financial operation is detected:

Do not automatically:

- send money
- approve payment
- enter OTP
- enter UPI PIN
- enter MPIN
- enter password
- enter CVV
- confirm a financial transaction

The user must remain in control of financial authorization.

---

# 20. PRIVACY MODE BEHAVIOR

When a sensitive app becomes foreground, restrict as appropriate:

- Automated clicking
- Automated typing
- Accessibility automation related to the sensitive app
- Screen capture
- Sensitive screen reading
- Clipboard monitoring where applicable
- Transaction automation
- Credential entry
- Payment confirmation
- Related Termux automation
- External tool execution related to sensitive financial actions

Security monitoring may continue where Android permits.

When the sensitive app is no longer foreground, normal capabilities can resume according to Android rules and policy.

Do not falsely claim that all sensitive content can always be hidden from every Android API.

---

# 21. TERMUX INTEGRATION

Termux is an optional helper/power tool.

Termux is NOT root access and does NOT remove Android security restrictions.

Potential uses:

- Diagnostics
- Coding
- Git
- Project analysis
- Build operations
- File processing
- Security diagnostics

Commands must be classified:

```text
SAFE
LOW_RISK
HIGH_RISK
DESTRUCTIVE
```

HIGH_RISK and DESTRUCTIVE commands require explicit user confirmation.

Never blindly execute commands obtained from:

- Webpages
- Downloads
- Messages
- External AI output
- Unknown files

Validate commands through policy and security checks first.

---

# 22. TRUST MODEL

Suggested trust levels:

```text
UNTRUSTED
LOW_TRUST
NORMAL
TRUSTED
SYSTEM_CRITICAL
```

External content should not become trusted automatically.

Treat the following as potentially untrusted:

- Webpages
- Downloads
- Messages
- User-provided unknown files
- AI-generated shell commands
- External scripts

Trust must not bypass security/policy checks.

---

# 23. CODING AGENT / SOFTWARE FACTORY

Long-term workflow:

```text
REQUIREMENTS
→ ARCHITECTURE
→ CODE
→ TEST
→ BUILD
→ ANALYZE ERROR
→ FIX
→ REBUILD
→ VERIFY
→ APK
→ GITHUB
→ RELEASE
```

The coding agent must work incrementally.

It should inspect the existing project before modifying files.

Never rewrite the entire repository for a small change.

---

# 24. GITHUB

GitHub integration should eventually support:

- Repository inspection
- Branch management
- Commit assistance
- Pull requests where appropriate
- Issue tracking
- Release preparation
- GitHub Actions
- APK artifacts
- Changelog
- Version management
- Rollback support

Never expose secrets.

Use GitHub Actions secrets or appropriate secure secret storage rather than putting keys in source code.

---

# 25. APK BUILD

Current expected toolchain:

```text
JDK 17
Gradle 8.9
Android Gradle Plugin 8.6.1
Kotlin 2.0.20
compileSdk 35
targetSdk 35
minSdk 26
```

Expected wrapper files:

```text
gradlew
gradlew.bat
gradle/wrapper/gradle-wrapper.jar
gradle/wrapper/gradle-wrapper.properties
```

The wrapper must be a real standard Gradle wrapper.

Do not use fake placeholder wrapper files.

---

# 26. GITHUB ACTIONS

The build workflow should use current Node-24-compatible action versions:

```text
actions/checkout@v5
actions/setup-java@v5
actions/upload-artifact@v6
```

Basic build workflow should:

1. Checkout repository
2. Set up JDK 17
3. Verify Gradle wrapper
4. Verify Gradle version
5. Clean project
6. Build APK
7. Find APK
8. Upload APK artifact
9. Run unit tests
10. Upload test reports

Do not add complicated Gradle caching until the basic build is stable.

---

# 27. VERSION POLICY

Current:

```text
versionName = 1.0.0
versionCode = 1
```

Do not change these for ordinary development.

Use controlled semantic versioning:

### PATCH

Small bug/security fixes.

Example:

```text
1.0.1
```

### MINOR

Meaningful backward-compatible features.

Example:

```text
1.1.0
```

### MAJOR

Breaking architecture/API changes.

Example:

```text
2.0.0
```

Version numbers should change intentionally, not frequently.

---

# 28. ANDROID LIMITATIONS

Never pretend a normal Android application has unrestricted system access.

Respect:

- Android sandbox
- Permissions
- AccessibilityService restrictions
- Background execution limits
- Foreground service rules
- Package visibility
- User consent
- Android security restrictions

A normal app cannot automatically:

- bypass Android security
- freely inspect another app's private data
- silently grant arbitrary permissions
- silently revoke arbitrary permissions
- force-stop arbitrary applications in every situation
- become a fully unrestricted system firewall
- obtain root without actual root/device support

Explain limitations honestly.

---

# 29. SAFETY / POLICY ENGINE

Before sensitive or risky actions, evaluate:

- User intent
- Trust level
- Tool capability
- Permission state
- Security state
- Privacy mode
- Risk level
- Confirmation requirement

Sensitive actions require user confirmation where appropriate.

Examples:

- Sending messages
- Purchases
- Money transfers
- Deleting files
- Changing sensitive settings
- Installing unknown applications
- Running destructive commands

---

# 30. EMERGENCY STOP

Emergency Stop must be available to interrupt agent activity.

It should stop/cancel eligible ongoing tasks as quickly as Android permits.

Do not claim that every system operation can be instantaneously reversed.

---

# 31. TASK / MISSION SYSTEM

Long-running tasks should support:

- Mission ID
- Goal
- Steps
- Current state
- Checkpoints
- Pause
- Resume
- Stop
- Retry
- Recovery
- Replanning
- Completion verification
- Logs

A task should survive recoverable errors without unnecessarily restarting the entire workflow.

---

# 32. SYSTEM HEALTH

Future health monitoring can include:

- Permission state
- Accessibility state
- Storage
- Battery
- Network state
- AI provider availability
- Termux availability
- Security engine state
- Background execution state
- Build/project health

Use Android APIs and permissions honestly.

---

# 33. MULTIMODAL / FUTURE EXTENSIONS

Architecture should allow future:

- Image understanding
- Screenshot understanding
- Camera input
- Audio analysis
- Document understanding
- On-device models
- Cloud models
- Local RAG
- Additional tools
- Additional AI providers

Do not implement every future feature prematurely.

Build clean interfaces so future additions do not require rewriting the core agent.

---

# 34. DEVELOPMENT WORKFLOW

Every new feature should follow:

```text
1. AUDIT CURRENT CODE
2. IDENTIFY EXISTING IMPLEMENTATION
3. IDENTIFY MISSING PIECES
4. PROPOSE FILE CHANGES
5. WAIT FOR APPROVAL
6. IMPLEMENT
7. TEST
8. BUILD
9. VERIFY
10. REPORT
11. WAIT FOR NEXT APPROVAL
```

Never automatically jump through multiple major phases.

---

# 35. REQUIRED STATUS FORMAT

At the end of each development phase, report:

```text
PHASE STATUS

Completed:
- ...

Files created:
- ...

Files modified:
- ...

Files unchanged:
- ...

Tests:
- ...

Build:
- ...

APK:
- ...

Remaining:
- ...

Next step:
- ...
```

Then wait.

---

# 36. WHEN A BUILD FAILS

Do not rewrite the whole project.

First identify:

```text
FAILED STEP
ROOT CAUSE
AFFECTED FILE
MINIMUM FIX
```

Then apply the minimum fix.

After the fix:

```text
BUILD AGAIN
→ VERIFY
→ REPORT
```

Never claim success without actual evidence.

---

# 37. IMPORTANT CURRENT PROJECT HISTORY

The project previously had build-environment problems involving:

- Missing Gradle wrapper
- Missing Gradle wrapper JAR
- Gradle download/network restrictions
- Android SDK/build tools availability
- JVM test compatibility with Android classes

Known corrective direction:

- Keep a real Gradle 8.9 wrapper in the repository
- Keep `gradlew` executable
- Keep `gradlew.bat`
- Keep `gradle-wrapper.jar`
- Keep `gradle-wrapper.properties`
- Avoid unnecessary Gradle cache configuration during initial stabilization
- Use JVM-compatible APIs in unit-test-only code where appropriate

Do not assume the build is fixed merely because files exist. Verify the actual GitHub Actions result.

---

# 38. EXISTING PROJECT PRINCIPLE

The project already contains major areas for:

- Agent model
- Emergency stop
- Safety engine
- Task executor
- AI provider abstraction
- Local heuristic brain
- OpenAI-compatible provider
- App manager
- Device context
- Permission helper
- Accessibility service
- Memory/database
- Tool registry
- Voice controller
- Agent ViewModel
- Home UI
- Live console UI
- Cyber security
- Security dashboard
- Privacy mode
- Payment protection
- APK/download/link security

Before adding any feature, inspect the repository because names/files may have changed.

Never assume an old filename is still current.

---

# 39. NEW CHAT HANDOFF INSTRUCTION

When a new AI coding chat receives this file:

DO NOT immediately write code.

First:

```text
READ SHIVAI_MASTER.md
↓
INSPECT CURRENT REPOSITORY
↓
COMPARE ACTUAL CODE WITH THIS DOCUMENT
↓
REPORT DIFFERENCES
↓
WAIT FOR USER APPROVAL
```

The actual repository always takes priority over outdated assumptions in this document.

If this document conflicts with current working code, do not silently rewrite the code. Explain the conflict and ask which direction to take.

---

# 40. GOLDEN DEVELOPMENT PHILOSOPHY

ShivAI should become more capable through modular additions, not repeated full rewrites.

Preferred pattern:

```text
Stable Core
+
New Interface
+
New Tool/Service
+
Policy Check
+
Tests
+
Verification
```

Avoid:

```text
New Feature
→ Rewrite Entire App
→ Break Existing Features
```

The goal is a stable long-term Android agent that can evolve without repeatedly rebuilding the entire project.

---

# END OF SHIVAI_MASTER.md
