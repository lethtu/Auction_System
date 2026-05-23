import wave
import struct
import math
import os

def apply_envelope(samples, sample_rate, fade_in_ms=20, fade_out_ms=50):
    num_samples = len(samples)
    fade_in_samples = int(sample_rate * fade_in_ms / 1000.0)
    fade_out_samples = int(sample_rate * fade_out_ms / 1000.0)
    
    # Apply fade in
    for i in range(min(fade_in_samples, num_samples)):
        factor = i / fade_in_samples
        samples[i] *= factor
        
    # Apply fade out
    for i in range(min(fade_out_samples, num_samples)):
        idx = num_samples - 1 - i
        factor = i / fade_out_samples
        samples[idx] *= factor
        
    return samples

def write_wav(path, sample_rate, samples):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with wave.open(path, 'w') as wav_file:
        wav_file.setnchannels(1)  # Mono
        wav_file.setsampwidth(2)  # 16-bit PCM = 2 bytes
        wav_file.setframerate(sample_rate)
        
        # Pack frames
        frames = []
        for s in samples:
            val = int(max(-32768, min(32767, s)))
            frames.append(struct.pack('<h', val))
            
        wav_file.writeframes(b''.join(frames))

def make_notification(sample_rate):
    duration = 0.35
    num_samples = int(sample_rate * duration)
    samples = []
    for i in range(num_samples):
        t = i / sample_rate
        # Overlapping 880Hz and 1175Hz sine waves with rapid exponential decay
        val = (10000 * math.sin(2 * math.pi * 880 * t) + 4000 * math.sin(2 * math.pi * 1175 * t)) * math.exp(-12 * t)
        samples.append(val)
    return apply_envelope(samples, sample_rate)

def make_success(sample_rate):
    duration = 0.5
    num_samples = int(sample_rate * duration)
    samples = []
    for i in range(num_samples):
        t = i / sample_rate
        val = 0.0
        # Bright major transition: 659Hz (E5) -> 880Hz (A5)
        val += 8000 * math.sin(2 * math.pi * 659 * t) * math.exp(-8 * t)
        if t >= 0.15:
            val += 10000 * math.sin(2 * math.pi * 880 * (t - 0.15)) * math.exp(-8 * (t - 0.15))
        samples.append(val)
    return apply_envelope(samples, sample_rate)

def make_error(sample_rate):
    duration = 0.35
    num_samples = int(sample_rate * duration)
    samples = []
    f1, f2 = 220, 180  # Low soft warning sweep
    for i in range(num_samples):
        t = i / sample_rate
        phase = 2 * math.pi * (f1 * t + 0.5 * (f2 - f1) * (t * t / duration))
        val = 12000 * math.sin(phase) * (1.0 - t / duration)
        samples.append(val)
    return apply_envelope(samples, sample_rate, fade_in_ms=20, fade_out_ms=60)

def make_outbid(sample_rate):
    duration = 0.65
    num_samples = int(sample_rate * duration)
    samples = []
    for i in range(num_samples):
        t = i / sample_rate
        val = 0.0
        # Distinct alert sequence: 740Hz -> 540Hz -> 740Hz
        if t < 0.15:
            val = 10000 * math.sin(2 * math.pi * 740 * t) * math.exp(-12 * t)
        elif t < 0.30:
            val = 10000 * math.sin(2 * math.pi * 540 * (t - 0.15)) * math.exp(-12 * (t - 0.15))
        else:
            val = 12000 * math.sin(2 * math.pi * 740 * (t - 0.30)) * math.exp(-8 * (t - 0.30))
        samples.append(val)
    return apply_envelope(samples, sample_rate)

def make_ending_soon(sample_rate):
    duration = 1.0
    num_samples = int(sample_rate * duration)
    samples = [0.0] * num_samples
    # Three brief urgent pulses (900Hz) spaced 0.3s apart
    pulse_starts = [0.0, 0.3, 0.6]
    pulse_dur = 0.15
    for start in pulse_starts:
        start_idx = int(start * sample_rate)
        end_idx = int((start + pulse_dur) * sample_rate)
        for idx in range(start_idx, min(end_idx, num_samples)):
            t_rel = (idx - start_idx) / sample_rate
            val = 12000 * math.sin(2 * math.pi * 900 * t_rel) * math.exp(-20 * t_rel)
            samples[idx] = val
    return apply_envelope(samples, sample_rate)

def make_win(sample_rate):
    duration = 1.4
    num_samples = int(sample_rate * duration)
    samples = []
    # Major chord arpeggio victory chime: C5 -> E5 -> G5 -> C6
    notes = [
        (523, 0.0, 7000),
        (659, 0.18, 7000),
        (784, 0.36, 7000),
        (1046, 0.54, 9000)
    ]
    for i in range(num_samples):
        t = i / sample_rate
        val = 0.0
        for freq, start, amp in notes:
            if t >= start:
                val += amp * math.sin(2 * math.pi * freq * (t - start)) * math.exp(-5 * (t - start))
        samples.append(val)
    return apply_envelope(samples, sample_rate)

def make_lost(sample_rate):
    duration = 0.7
    num_samples = int(sample_rate * duration)
    samples = []
    for i in range(num_samples):
        t = i / sample_rate
        val = 0.0
        # Descending neutral chord: 330Hz (E4) -> 247Hz (B3)
        val += 9000 * math.sin(2 * math.pi * 330 * t) * math.exp(-8 * t)
        if t >= 0.25:
            val += 9000 * math.sin(2 * math.pi * 247 * (t - 0.25)) * math.exp(-6 * (t - 0.25))
        samples.append(val)
    return apply_envelope(samples, sample_rate)

def main():
    sample_rate = 44100
    out_dir = "client/src/main/resources/sounds/"
    
    generators = {
        "notification.wav": make_notification,
        "success.wav": make_success,
        "error.wav": make_error,
        "outbid.wav": make_outbid,
        "ending_soon.wav": make_ending_soon,
        "win.wav": make_win,
        "lost.wav": make_lost
    }
    
    print("Generating sound effects...")
    print("-" * 60)
    print(f"{'Filename':<20} | {'Duration (s)':<12} | {'Size (bytes)':<12}")
    print("-" * 60)
    
    for filename, gen_fn in generators.items():
        dest_path = os.path.join(out_dir, filename)
        samples = gen_fn(sample_rate)
        write_wav(dest_path, sample_rate, samples)
        
        # Get actual statistics
        size_bytes = os.path.getsize(dest_path)
        duration_sec = len(samples) / sample_rate
        print(f"{filename:<20} | {duration_sec:<12.2f} | {size_bytes:<12}")
        
    print("-" * 60)
    print("Sound generation complete!")

if __name__ == "__main__":
    main()
