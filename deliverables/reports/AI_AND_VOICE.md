# Assistant and voice capability report

The assistant performs deterministic local organization: reviewed task drafts, supported date/category/recurrence terms, possible duplicate detection, saved-record summaries, general checklists and questions for a contractor. A proposal opens the normal editor and requires Save. Completion commands require confirmation and a saved-title match. No remote model or paid inference service is connected, and home records are not sent to an AI provider.

Language understanding is limited. Unsupported relative dates, arbitrary time zones and ambiguous task matches require manual review. The app does not perform safety inspections or supply hazardous repair instructions. Remote AI is an optional future integration, not a dependency of this release.

## Physical Fire evidence

Device: Amazon KFTRWI, Android 9 / API 28. The report is `android/build-logs/voice-compatibility.json`.

| Capability | Result |
| --- | --- |
| Recognition service detection | PASS: Android reports no compatible SpeechRecognizer service on this Fire. Alexa availability is a separate capability. |
| Acoustic recognition | NOT AVAILABLE on the tested device; no successful microphone transcription or accent coverage is claimed. |
| Text alternative | PASS in localized real-screen journeys. A typed proposal is reviewed, saved, persisted and shown in calendar/reports. |
| Missing-recognizer UI | PASS in the language journeys: invoking push-to-talk shows the localized fallback and leaves typing available. |
| TTS initialization | PASS. |
| en-GB, en-US, en-AU synthesis | PASS for each: the installed engine produced a nonempty WAV via `synthesizeToFile`. Test audio was removed. Audible speaker quality was not assessed. |
| Other 19 interface locale voices | Unavailable in the installed engine. No voice download or substitute engine was installed. |
| Permission denial during a real recognition session | NOT TESTED because no recognizer is installed. Runtime permission/error handling remains implemented. |

There are 22 command dictionaries covering six intents, including Korean and both Chinese scripts. Dictionary coverage is distinct from an engine's acoustic language support. Voice processing may occur online under the chosen engine's policy, disclosed before use. Fixavora does not continuously listen or retain recordings. Spoken feedback is optional and can be disabled. TTS errors and unavailable languages have a text fallback.
