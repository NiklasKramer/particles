Granular {

	// instantiate variables here
	var <effect, <grain, <passthrough, <inputBus, <ptrBus, <fxBus, <inputGroup, <ptrGroup, <recGroup, <grnGroup, <fxGroup, <input, <record, <soundGood, <wobble, <vme, <grain;
	// we want 'passthrough' to be accessible any time we instantiate this class,
	// so we prepend it with '<', to turn it into a 'getter' method.
	// see 'Getters and Setters' at https://doc.sccode.org/Guides/WritingClasses.html for more info.

	// in SuperCollider, asterisks denote functions which are specific to the class.
	// '*initClass' will be called when the 'Habitus' class is initialized.
	// see https://doc.sccode.org/Classes/Class.html#*initClass for more info.
	*initClass {

		StartUp.add {
			var s = Server.default;

			// we need to make sure the server is running before asking it to do anything
			s.waitForBoot {

				SynthDef(\input, {
					arg out=0, amp=0.5, inchan=0;
					var signal;
					signal = SoundIn.ar(inchan);
					Poll.ar(Impulse.ar(1), signal, label: "Signal Amplitude");
					signal = signal * amp;
					Out.ar(out, signal);
				}).add;



				// EFFECT
				SynthDef(\wobble, {|in = 0, out = 0, rate = 0.2, maxDelay = 0.1, minDelay = 0.05, amp = 1, mix = 0.0, depth = 1|
					var output, signal, input, lfo, voice, smoothedDepth;

					input = In.ar(in, 2);

					// Smooth out changes to the depth parameter
					smoothedDepth = Lag.kr(depth, 2);

					lfo = [
						SinOsc.ar(
							rate * rrand(0.95, 1.05),
							rrand(0.0, 1.0),
							(maxDelay - minDelay) * 0.5 * smoothedDepth,
							(maxDelay + minDelay) * 0.5 * smoothedDepth),
						SinOsc.ar(rate * rrand(0.95, 1.05),
							rrand(0.0, 1.0),
							(maxDelay - minDelay) * 0.5 * smoothedDepth,
							(maxDelay + minDelay) * 0.5 * smoothedDepth)
					];

					voice = [
						DelayC.ar(input[0], maxDelay, lfo[0]),
						DelayC.ar(input[1], maxDelay, lfo[1])
					];

					signal = XFade2.ar(input, voice, mix * 2 - 1);

					// send to output
					Out.ar(out, signal * amp);
				}).add;


				SynthDef(\rec, {
					arg micIn=0, buf=0, fadeTime=1;
					var sig, pos, fadeEnv, distanceToStart, distanceToEnd, bufFrames, bufRate;

					sig = In.ar(micIn, 1);

					// Get the buffer's total frames and rate scale
					bufFrames = BufFrames.kr(buf);
					bufRate = BufRateScale.kr(buf);

					// Current position from the Phasor
					pos = Phasor.ar(0, bufRate, 0, bufFrames);

					// Calculate the distance from the current position to the start and end of the buffer
					distanceToStart = pos;
					distanceToEnd = bufFrames - pos;

					// Create a fade envelope based on distance to start and end
					fadeEnv = min(
						Clip.kr(distanceToStart / (fadeTime * bufRate), 0, 1),
						Clip.kr(distanceToEnd / (fadeTime * bufRate), 0, 1)
					);

					// Apply the fade envelope to the signal
					sig = sig * fadeEnv;

					// Poll.ar(Impulse.ar(1), sig, label: "Signal Amplitude");

					BufWr.ar(sig, buf, pos);
				}).add;


				// BufGrain
				SynthDef(\bufGrain, {
					arg out, sndbuf, rate=1, dur=1, pos=0.0, dens=10, amp=0.7, release=0.5, jitter=1, gate=1, envbuf=(-1), phasorFreq=0.5, triggerType=0;
					var sound, leveled, outputEnv, posModulation, phasor, triggerSignal;

					// Generate a phasor signal that ramps from 0 to buffer's duration at phasorFreq rate
					phasor = Phasor.ar(0, phasorFreq / BufDur.kr(sndbuf), 0, 1);

					// Modulate position with jitter and phasor
					posModulation = pos + phasor;

					// posModulation = posModulation.wrap(0, 1); // use wrap instead of clip to keep the modulation continuous
					posModulation = pos + (LFNoise1.kr([1,1]) * jitter);
					posModulation = posModulation.clip(0.01, 1);


					// Choose between Dust and Impulse based on triggerType
					triggerSignal = Select.ar(triggerType, [
						[Dust.ar(dens), Dust.ar(dens)], // If triggerType is 0
						[Impulse.ar(dens), Impulse.ar(dens)] // If triggerType is 1
					]);

					sound = GrainBuf.ar(
						numChannels: 2,
						trigger: triggerSignal,
						dur: [dur, dur - 0.01],
						sndbuf: sndbuf,
						rate: rate,
						pos: posModulation,
						interp: 2,
						pan: 0,
						envbufnum: envbuf,
					);

					outputEnv = EnvGen.ar(Env.adsr(releaseTime: release), gate: gate, doneAction: 2);
					leveled = sound * outputEnv * amp;
					Out.ar(out, leveled);
				}).add;

				SynthDef(\vintageSamplerEmu, { |in=0, out=0, bitDepth=12, sampleRate=26040, drive=0.5, cutoffFreq=8000, mix=0.5|
					var signal, bitcrushed, saturated, mixed, filtered, output;

					signal = In.ar(in, 2); // Assuming stereo input

					// Processed signal chain
					bitcrushed = Decimator.ar(signal, sampleRate, bitDepth);
					saturated = SineShaper.ar(bitcrushed, drive);

					mixed = XFade2.ar(signal, saturated, mix * 2 - 1);

					// Apply the filter to the mixed signal
					filtered = MoogFF.ar(mixed, cutoffFreq);

					// The final output signal
					output = filtered;

					// Output the blended signal
					Out.ar(out, output);
				}).add;


				~midside = {|in, msBalance=0|
					var sig = Balance2.ar(in[0] + in[1], in[0] - in[1], msBalance);
					[sig[0] + sig[1], sig[0] - sig[1]] * sqrt ( (msBalance.max(0)+1)/2 )
				};

				SynthDef.new(\soundGood, {
					| out=0, in=0, wet=0.35, makeup=0.98 |
					var lfreq = 250, hfreq = 3000, q = 1.1;
					var dry, low, mid, high, master;
					var att = 2/1000;
					var lrel = 137/1000, lpre = dbamp(5*wet), lpos = dbamp(5.9*wet), lexp = 0.07*wet, lstereo = -1*wet;
					var mrel = 85/1000, mpre = dbamp(6*wet), mpos = 1, mstereo = 0.38*wet;
					var hrel = 75/1000, hpre = dbamp(6.8*wet), hpos = dbamp(2.9*wet), hexp = 0.14*wet, hstereo = 0.2, hsat = 1/16*wet;
					var output;

					dry = In.ar(in,2);
					dry = BHiPass4.ar(dry,20);

					low = BLowPass4.ar(dry,lfreq,q);
					mid = BHiPass4.ar(dry,lfreq,q); mid = BLowPass4.ar(mid,hfreq,q);
					high = BHiPass4.ar(dry,hfreq,q);

					low = CompanderD.ar(low*lpre,1,1+lexp,10,att,lrel,lpos);
					low = ~midside.(low, lstereo);
					low = SineShaper.ar(low);

					mid = CompanderD.ar(mid*mpre,1,1,10,att,lrel,mpos);
					mid = ~midside.(mid, mstereo);

					high = CompanderD.ar(high*hpre,1,1+hexp,10,att,hrel,hpos);
					high = ~midside.(high, hstereo);
					high = SineShaper.ar(high,hpos-(hpos*hsat));

					master = Limiter.ar(Mix.new([low,mid,high]),0.99,0.01)*makeup;

					Out.ar(out, master);
				}).add;



			} // s.waitForBoot
		} // StartUp
	} // *initClass

	// after the class is initialized...
	*new {
		^super.new.init;  // ...run the 'init' below.
	}

	init {
		var s = Server.default;
		var b = Buffer.alloc(s, s.sampleRate * 2, numChannels: 1);

		~inputBus=Bus.audio(s,1);
		~ptrBus=Bus.audio(s,1);
		~fx1Bus=Bus.audio(s,2);
		~fx2Bus=Bus.audio(s,2);
		~fx3Bus=Bus.audio(s,2);


		~inputGroup = Group.new();
		~ptrGroup = Group.after(~inputGroup);
		~recGroup = Group.after(~ptrGroup);
		~grnGroup = Group.after(~recGroup);


		// ~winenv = Env.adsr(attackTime: 0, decayTime: 0.01, sustainLevel: 0.00, releaseTime: 0.0);
		// ~z = Buffer.sendCollection(s, ~winenv.asArray, 1);


		input = Synth.new(\input, [\inchan, 0, \out, ~inputBus], ~inputGroup);  //
		record = Synth.new(\rec,[\micIn, ~inputBus, \buf, b], ~recGroup);

		soundGood = Synth(\soundGood,[\in, ~fx3Bus, \out, 0]);
		wobble = Synth(\wobble,[\in: ~fx2Bus, out: ~fx3Bus ]);
		vme = Synth(\vintageSamplerEmu,[\in: ~fx1Bus, out:~fx2Bus ]);

		grain = Synth.new(\bufGrain, [ \rate, 1, \sndbuf, b, \out, ~fx1Bus, \amp, 1]);

		s.sync; // sync the changes above to the server
	}

	// create a command to control the synth's 'amp' value:


	setGate { arg gate;
		grain.set(\gate, gate);
	}

	setRate { arg rate;
		grain.set(\rate, rate);
	}

	setPos { arg pos;
		grain.set(\pos, pos);
	}

	setDur { arg dur;
		grain.set(\dur, dur);
	}

	setDens { arg dens;
		grain.set(\dens, dens);
	}

	setJitter { arg jitter;
		grain.set(\jitter, jitter);
	}

	setRelease { arg release;
		grain.set(\release, release);
	}

	setTriggerType { arg triggerType;
		grain.set(\triggerType, triggerType);
	}

	setMaxDelay { arg maxDelay;
		wobble.set(\maxDelay, maxDelay);
	}

	setMinDelay { arg minDelay;
		wobble.set(\minDelay, minDelay);
	}

	setDepth { arg depth;
		wobble.set(\depth, depth);
	}

	setMix { arg mix;
		wobble.set(\mix, mix);
	}

	setAmp { arg amp;
		effect.set(\amp, amp);
	}
	setVMEBitDepth { arg bitDepth;
		vme.set(\bitDepth, bitDepth);
	}

	setVMESampleRate { arg sampleRate;
		vme.set(\sampleRate, sampleRate);
	}

	setVMEDrive { arg drive;
		vme.set(\drive, drive);
	}

	setVMECutoffFreq { arg cutoffFreq;
		vme.set(\cutoffFreq, cutoffFreq);
	}

	setVMEMix { arg mix;
		vme.set(\mix, mix);
	}

	// IMPORTANT!
	// free our synth after we're done with it:
	free {
		grain.free;
		wobble.free;
		vme.free;
		record.free;
		input.free;
		soundGood.free;
		// b.free; // Assuming 'b' is a buffer that should be freed
		~inputBus.free;
		~ptrBus.free;
		~fx1Bus.free;
		~fx2Bus.free;
		~fx3Bus.free;
	}

}