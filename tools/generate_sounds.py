"""Synthesize the timer's sounds into app/src/main/res/raw.

The sounds are made from sine partials here instead of taken from a sound
library, so they carry no third-party license. Run from the repository root:

    python3 tools/generate_sounds.py

Only the standard library is needed. The output is deterministic.
"""

import math
import random
import struct
import wave
from pathlib import Path

RATE = 44100
OUT = Path(__file__).resolve().parent.parent / "app" / "src" / "main" / "res" / "raw"

# A struck bell after Jean-Claude Risset: inharmonic partials as
# (ratio to the base, relative amplitude, relative decay time).
BELL_PARTIALS = [
    (0.56, 1.0, 1.0),
    (0.5622, 0.67, 0.9),
    (0.92, 1.0, 0.65),
    (0.9219, 1.8, 0.55),
    (1.19, 2.67, 0.325),
    (1.7, 1.67, 0.35),
    (2.0, 1.46, 0.25),
    (2.74, 1.33, 0.2),
    (3.0, 1.33, 0.15),
    (3.76, 1.0, 0.1),
    (4.07, 1.33, 0.075),
]


def silence(seconds):
    return [0.0] * int(seconds * RATE)


def mix(target, source, at_seconds):
    start = int(at_seconds * RATE)
    if len(target) < start + len(source):
        target.extend([0.0] * (start + len(source) - len(target)))
    for i, sample in enumerate(source):
        target[start + i] += sample


def bell(base=880.0, seconds=2.2, decay=2.0):
    """One strike of a bright bell that carries over noise in a gym."""
    samples = []
    for n in range(int(seconds * RATE)):
        t = n / RATE
        value = 0.0
        for ratio, amplitude, duration in BELL_PARTIALS:
            value += amplitude * math.exp(-t / (decay * duration) * 4.6) * math.sin(2 * math.pi * base * ratio * t)
        # A few milliseconds of attack keep the strike from clicking.
        samples.append(value * min(1.0, t / 0.002))
    return samples


def strikes(count, spacing=0.38):
    out = silence(0)
    for i in range(count):
        mix(out, bell(), i * spacing)
    return out


def beep(frequency, seconds):
    """A tone with a few odd harmonics, which sounds more urgent than a pure sine."""
    out = []
    for n in range(int(seconds * RATE)):
        t = n / RATE
        envelope = min(1.0, t / 0.005, (seconds - t) / 0.02)
        value = sum(math.sin(2 * math.pi * frequency * k * t) / k for k in (1, 3, 5))
        out.append(envelope * value)
    return out


def warning():
    """Two short high tones: the lead-in begins."""
    out = beep(1320, 0.12)
    mix(out, beep(1320, 0.12), 0.18)
    return out


def tick():
    """A short wood-block click for the last seconds of the lead-in."""
    noise = random.Random(7)
    out = []
    for n in range(int(0.06 * RATE)):
        t = n / RATE
        envelope = math.exp(-t * 90)
        value = 0.7 * math.sin(2 * math.pi * 1900 * t) + 0.3 * math.sin(2 * math.pi * 3150 * t)
        value += 0.25 * noise.uniform(-1, 1) * math.exp(-t * 400)
        out.append(envelope * value * min(1.0, t / 0.0005))
    return out


def write(name, samples, peak=0.89):
    """Normalized to about -1 dBFS, mono 16 bit."""
    scale = peak / max(abs(s) for s in samples)
    frames = b"".join(struct.pack("<h", int(round(s * scale * 32767))) for s in samples)
    with wave.open(str(OUT / f"{name}.wav"), "wb") as file:
        file.setnchannels(1)
        file.setsampwidth(2)
        file.setframerate(RATE)
        file.writeframes(frames)


if __name__ == "__main__":
    OUT.mkdir(parents=True, exist_ok=True)
    write("gong_round_start", strikes(1))
    write("gong_round_end", strikes(2))
    write("gong_finish", strikes(3))
    write("warning", warning())
    write("tick", tick(), peak=0.8)
