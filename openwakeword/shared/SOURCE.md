# Shared openWakeWord frontend

Both files are shared by every wake word: mel-spectrogram and embedding
run once per 80 ms chunk, and each classifier consumes the same output.

## License

- **Apache-2.0** — https://www.apache.org/licenses/LICENSE-2.0

Unlike the per-word classifiers, these two models are Apache-2.0 and may
be bundled in the app. `embedding_model.onnx` derives from Google's
speech_embedding model, also released under Apache-2.0.

- **Project**: [openWakeWord](https://github.com/dscripka/openWakeWord)
- **Author**: David Scripka (dscripka); embedding backbone by Google
- **Upstream reference**: https://github.com/dscripka/openWakeWord/releases/tag/v0.5.1

## Files

### melspectrogram.onnx

- Torch MelSpectrogram exported to ONNX (STFT as two Conv1d layers over a windowed DFT basis, then a 257x32 mel matrix).
- 1,087,958 bytes
- SHA-256: `ba2b0e0f8b7b875369a2c89cb13360ff53bac436f2895cced9f479fa65eb176f`
- Obtained from: https://github.com/dscripka/openWakeWord/releases/download/v0.5.1/melspectrogram.onnx

### embedding_model.onnx

- Google speech_embedding, re-implemented by the openWakeWord project. 76x32 mel window -> 96-dim embedding.
- 1,326,578 bytes
- SHA-256: `70d164290c1d095d1d4ee149bc5e00543250a7316b59f31d056cff7bd3075c1f`
- Obtained from: https://github.com/dscripka/openWakeWord/releases/download/v0.5.1/embedding_model.onnx
