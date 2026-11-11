# TV tools PR

- Application labels are read on the connected device using a tiny DEX helper and its PackageManager. No root or helper APK installation. The helper is built from `app/src/tvHelper/java` with the SDK D8, pushed to a unique `/data/local/tmp` path, made read-only, invoked as ADB shell, and removed in a cancellation-safe finally block. No labels are shared across devices. If an OEM blocks shell metadata access, the full package name is displayed, never a guessed package suffix or the controlling phone?s label.
- **TV files** is a separate device tool opening `/sdcard` in a full-width remote pane. Existing directory navigation, jump-to-path, mkdir, rename, confirmed deletion, upload and download are reused. The local/TV switch selects the transfer source; arrows transfer selected files to the other side?s current directory. Protected paths still require permission on the target; this does not bypass Android storage security.
- **Process manager** reads target `/proc/meminfo` and `dumpsys meminfo`. Used = total - available; older kernels fall back to free + buffers + cached + reclaimable - shmem. Each running process displays PID and PSS, sorted descending. PSS is not RSS and its sum is not system used RAM.
- Stopping uses confirmed `am force-stop` against an installed application package, affecting all of that application?s processes. Native/unidentified processes are read-only; no unsafe PID kill or root escalation. The TV can restart system services. Errors and unsupported OEM output are shown rather than fabricated as zero RAM.
- The PR workflow runs unit tests, release lint and debug assembly and uploads an installable debug APK and reports. It does not merge, tag or publish a production release. Debug signing differs from an existing release install; use a separate test device or back up/export settings before uninstalling the existing app if Android rejects an update.

## Device acceptance checks (requires the user?s TV)
1. Connect over Wi-Fi ADB and verify TV-only Chinese names, including Settings, the TV launcher and vendor apps. Check another TV and refresh to ensure labels are not reused.
2. Open TV files, browse `/sdcard`, create/rename/delete a test folder, upload/download a file and compare bytes. Confirm protected-directory errors; do not test deletion on real personal files.
3. Open Process manager, compare total/available RAM and PID/PSS against ADB shell output. Search by Chinese name/package/PID. Stop an expendable user application, confirm its processes disappear, and ensure cancel makes no change.
4. Disconnect during each read/transfer and check error recovery. Verify old-kernel memory fallback and vendor restrictions on a real TV if available.

No live TV is available in CI: a successful build and parser tests are not a claim of TV hardware verification.

CI also boots Android 35, executes the exact helper extracted from the APK as ADB shell, checks Settings metadata and the end marker, and installs/launches the app. This emulator test does not replace vendor TV acceptance.
