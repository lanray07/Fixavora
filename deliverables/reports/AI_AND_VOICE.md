# Assistant and voice capability report

## Local assistant

Implemented: deterministic task drafts, limited category/date/recurrence parsing, possible duplicate detection, overdue/upcoming/warranty queries, spending summaries from saved records, general organisation checklists and questions to ask a contractor. Proposed tasks open the normal editor and require Save; completion commands require confirmation and an exact saved task-title match. No remote model is connected and no private home records are sent to one.

The assistant is an organisation tool. Natural-language understanding is limited; relative-date parsing is strongest for the explicitly implemented English/French/Spanish/German terms. Friday appointments, arbitrary time zones and all natural-language date expressions are not inferred. Users must review/edit dates, categories and recurrence. No automated appliance verification, safety inspection, legal certification or hazardous repair instructions are claimed. General professional-assessment guidance is always accessible.

Remote AI status: **BLOCKED** for service configuration and informed-consent design; no provider credentials were supplied. A remote-model adapter is not implemented. Local functionality does not depend on it. Premium does not include paid inference or imply unlimited calls.

## Tested device

Amazon KFTRWI, Android 9 / API 28, build PS7331.4463N, physical display 1200×1920 at density 240. Machine-readable evidence is `deliverables/android/build-logs/voice-compatibility.json`.

| Capability | Status | Result |
| --- | --- | --- |
| Recognition engine capability detection | PASS | Android `SpeechRecognizer.isRecognitionAvailable` reports false on the connected Fire. |
| Actual push-to-talk recognition on this Fire | BLOCKED | No compatible Android recognition service is installed/available. Alexa availability does not establish SpeechRecognizer compatibility. |
| Text fallback | PASS | Actual text-to-task proposal, explicit editor Save, persistence, calendar navigation and report preview passed on both tested layouts with Wi-Fi disabled. This is not microphone recognition. |
| TTS initialisation | PASS | Installed engine initialised successfully. |
| en-GB TTS synthesis | PASS | Engine generated a nonempty audio file via `synthesizeToFile`; file was then removed. Audible speaker quality was NOT TESTED. |
| en-US and en-AU TTS | NOT TESTED | Engine reports these language variants available; synthesis for these variants was not separately executed. |
| Other 16 requested locale voices | BLOCKED | Installed TTS engine reports unavailable for each. No voice download or alternative engine was installed. |
| Microphone-denial UI | NOT TESTED | Runtime permission handling is implemented. No real recognition session or microphone recording was performed on an unavailable recogniser. |
| Recognition in any target locale | NOT TESTED | No acoustic recognition result exists. |

Nineteen draft dictionaries contain six command intents each. Structural generation checks and JVM routing tests exist. They are not a claim of linguistic review, accent coverage, recognition accuracy or offline recognition. No continuous microphone listening is implemented; Fixavora does not store audio recordings. The installed speech engine may process audio online and the interface discloses this before invocation. Spoken feedback can be disabled.
