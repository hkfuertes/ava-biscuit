# hey_rhasspy

- **Spoken phrase**: `hey rhasspy`
- **File**: `hey_rhasspy.onnx` (204,081 bytes)
- **SHA-256**: `5a9b3ed3be2910e35780e097905aa9f35a9c10038df47914cf2b3ec4d670f6ea`

## Where this file came from

- **Project**: [openWakeWord](https://github.com/dscripka/openWakeWord)
- **Author**: David Scripka (dscripka)
- **Obtained from**: https://github.com/dscripka/openWakeWord/releases/download/v0.5.1/hey_rhasspy_v0.1.onnx
- **Upstream reference**: https://github.com/dscripka/openWakeWord/releases/tag/v0.5.1

## License

- **CC-BY-NC-SA-4.0** — https://creativecommons.org/licenses/by-nc-sa/4.0/

openWakeWord's pre-trained models are CC-BY-NC-SA-4.0 (not Apache-2.0) because of training data with unknown or restrictive licensing. Attribution required, non-commercial only, share-alike on derivatives.

This file is redistributed unmodified. It is **not** covered by Ava's
own license, and must not be relabelled as such.

## Notes

Included because it ships in the official openWakeWord release.

Scores **0.9399** on an all-zero classifier window, the highest of any model
here. The embedding window must be warmed with noise before inference and on
every reset, or this model fires immediately on silence.
