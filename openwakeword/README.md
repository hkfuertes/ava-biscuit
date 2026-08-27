# openWakeWord models for Ava

The wake-word model set behind Ava's openWakeWord engine. Every file records its
exact upstream URL, author, and license in a `SOURCE.md` next to it, and again
in `index.json`.

Nothing here was trained by us, and nothing here is Ava-licensed.

## Layout

```
openwakeword/
├── index.json                  machine-readable catalog (frontend params + every model)
├── shared/                     Apache-2.0 frontend, shared by all wake words
│   ├── melspectrogram.onnx
│   ├── embedding_model.onnx
│   └── SOURCE.md
├── models/                     the main set
│   └── <id>/<id>.onnx + SOURCE.md
├── extras/                     additions, including the Chinese wake words
│   └── <id>/<id>.onnx + SOURCE.md
└── licenses/                   verbatim upstream license texts
```

Each `.onnx` also has a sibling `.url` file holding the exact download URL.

## What ships in the APK

Only the shared frontend and `ok_nabu` are bundled, under
`app/src/main/assets/openwakeword/`. Everything else is fetched on demand, so
the per-model license can be shown before the user downloads it.

There is no openWakeWord stop model. The stop word stays on the microWakeWord
engine (`app/src/main/assets/stopWords/`, Apache-2.0).

## Licensing — read before shipping

Two different licenses apply, and they are not interchangeable.

**`shared/` is Apache-2.0.** The mel-spectrogram and embedding models can be
bundled in the APK. Keep the `NOTICE` entry.

**Every classifier under `models/` and `extras/` is non-commercial.** The
official ones are explicitly **CC-BY-NC-SA-4.0**: attribution required,
non-commercial only, share-alike on derivatives. The community ones sit in an
MIT-licensed repository but were trained with the openWakeWord notebooks, so the
same non-commercial stance may reach them. Treat all of them as attribution-
required and non-commercial.

Concretely:

- Do not relabel these files under Ava's license.
- Keep each `SOURCE.md` alongside its model wherever the model is published.
- Name the source project and author in the README and the wiki.
- Prefer serving classifiers from the online catalog, which displays the
  license, rather than baking non-commercial files into the shipped APK.

## Runtime contract

Measured against the real graphs, not copied from documentation:

| item | value |
|---|---|
| audio chunk | 1280 samples @ 16 kHz (80 ms) |
| audio scale | int16-range float32 — do **not** normalise to ±1 |
| mel input | `[history 480 \| chunk 1280]` = 1760 samples |
| mel output | 8 frames × 32 bins per chunk |
| mel transform | `x / 10 + 2` applied to every frame |
| mel buffer init | filled with `1.0`, keeps a 76-frame window |
| embedding input | `[1, 76, 32, 1]` |
| embedding output | 96 dims |
| classifier input | `[1, 16, 96]` (latest 16 embeddings) |
| classifier output | `[1, 1]`, a probability |

Graph tensor names: the mel model is `input` → `output`, the embedding model is
`input_1` → `conv2d_19`. Classifier tensor names carry a per-model hash suffix,
so bind them by index rather than by name.

Cost per 80 ms chunk: mel ≈ 2.2 MMAC, embedding ≈ 42.0 MMAC, each classifier
≈ 0.2 MMAC. The embedding stage is ~95% of the work and is computed once no
matter how many wake words are active.

### Warmup is mandatory

An all-zero classifier window is **not** neutral. Measured scores on a zeroed
`[1,16,96]` input:

| model | score on all-zero window |
|---|---|
| `hey_rhasspy` | **0.9399** |
| `alexa` | **0.4877** |
| `hey_alfred` | 0.2400 |
| `xiao_bu` | 0.1002 |
| `ok_computer` | 0.0868 |
| `hey_jarvis` | 0.0002 |

So the embedding window must be pre-filled with noise embeddings at startup, and
`reset()` must restore that noise snapshot rather than zeroing. Zeroing would
fire `hey_rhasspy` and `alexa` on silence immediately.

## Op set

Union across all 17 graphs — 28 distinct ONNX ops, opset 13:

```
Add Cast Clip Constant Conv Div Flatten Gemm GreaterOrEqual Identity If
LeakyRelu Log MatMul Max MaxPool Mul Pow Reciprocal ReduceMax ReduceMean
Relu Reshape Sigmoid Sqrt Sub Transpose Unsqueeze
```

Only **Conv2D**, **MaxPool**, **Gemm** and **MatMul** are real work; the rest are
elementwise or shape ops.

Two notes for the C++ implementation:

- The mel graph's two `Conv` nodes are a **windowed real-DFT basis** (verified to
  within 5.6e-08 against `window[k]·cos(2πfk/512)` and `−window[k]·sin(...)`), so
  they can be replaced by the `kissfft` real FFT already vendored in
  `microfeatures`.
- `hey_jarvis` uses `If` to gate a second verifier network on the main network's
  output. Evaluating `If` lazily makes the verifier free when idle.
