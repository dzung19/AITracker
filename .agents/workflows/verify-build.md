---
description: How to verify the build and check for lint errors
---
Whenever you are asked to verify the build or check for lint errors, follow these steps:

// turbo-all
1. Run `./gradlew :app:assembleDebug` to compile the app module and identify any syntax or linkage errors.
2. Run `./gradlew lintDebug` to run the Android lint tool for code quality checks.
3. If the build or lint fails, analyze the output to fix the compilation or linting errors, then repeat the process.
