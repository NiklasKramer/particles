Granular {
	classvar a,b,c,d,e,f, allBuffers;

	// instantiate variables here
	var <grain, <inputBus, <ptrBus, <fxBus, <inputGroup, <ptrGroup, <recGroup, <grnGroup, <fxGroup, <input, <record, <soundGood, <wobble, <vme, <grain, <pointer;
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
				a = Buffer.alloc(s, s.sampleRate * 0.5, numChannels: 1);
				b = Buffer.alloc(s, s.sampleRate * 1, numChannels: 1);
				c = Buffer.alloc(s, s.sampleRate * 2, numChannels: 1);
				d = Buffer.alloc(s, s.sampleRate * 4, numChannels: 1);
				e = Buffer.alloc(s, s.sampleRate * 8, numChannels: 1);
				f = Buffer.alloc(s, s.sampleRate * 16, numChannels: 1);

				allBuffers = [a,b,c,d,e,f];


				SynthDef(\input, {
					arg out=0, amp=0.5, inchan=0;
					var signal;
					signal = SoundIn.ar(inchan);
					// Poll.ar(Impulse.ar(1), signal, label: "Signal Amplitude");
					signal = signal * amp;
					Out.ar(out, signal);
					}).add;

				SynthDef(\rec, {
					arg micIn=0, buf=0, pointerIn=0;
					var sig, pointer;
					sig = In.ar(micIn, 1);
					pointer = In.ar(pointerIn, 1);
					// Poll.ar(Impulse.ar(10), pointer, "Pointer In Rec");
					BufWr.ar(sig, buf, pointer);
					}).add;

				SynthDef(\pointer, {
					arg out=0, buf=0;
					var signal;
					signal = Phasor.ar(0,BufRateScale.kr(buf), 0, BufFrames.kr(buf));
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


				// BufGrain
				SynthDef(\bufGrain, {
					arg out, sndbuf, rate=1, dur=1, pos=0.0, dens=10, amp=0.7, release=0.5, jitter=1, gate=1, envbuf=(-1), phasorFreq=0.5, triggerType=0, pointerIn=0;
					var sound, leveled, outputEnv, posModulation, triggerSignal, ptrValue, checkPosition, isEqual, tolerance;

					// Poll.ar(Impulse.ar(10), pointer, "Pointer In Grain");

					ptrValue = In.ar(pointerIn, 1)/BufFrames.kr(sndbuf);

					// Define a tolerance for comparison
					tolerance = 0.2;

					// Modulate position with jitter
					posModulation = pos + (LFNoise1.kr([1,1]) * jitter);
					posModulation = posModulation.clip(0, 1);

					// Check if posModulation and ptrValue are approximately equal
					isEqual = (posModulation - ptrValue).abs() < tolerance;
					// Poll.ar(Impulse.ar(15), isEqual, "isEqual");


					// If they are equal, adjust posModulation, otherwise leave it as is
					posModulation = Select.kr(isEqual, [posModulation + tolerance, posModulation]);
					posModulation = posModulation.clip(0, 1);

					// Choose between Dust and Impulse based on triggerType
					triggerSignal = Select.ar(triggerType, [[Dust.ar(dens), Dust.ar(dens)], [Impulse.ar(dens), Impulse.ar(dens)]]);

					// Poll.ar(Impulse.ar(15), posModulation, "posModulation");
					// Poll.ar(Impulse.ar(15), ptrValue, "Pointer In Grain");

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

		~inputBus=Bus.audio(s,1);
		~ptrBus=Bus.audio(s,1);
		~fx1Bus=Bus.audio(s,2);
		~fx2Bus=Bus.audio(s,2);
		~fx3Bus=Bus.audio(s,2);


		~inputGroup = Group.new();
		~ptrGroup = Group.after(~inputGroup);
		~recGroup = Group.after(~ptrGroup);
		~grnGroup = Group.after(~recGroup);



		input = Synth.new(\input, [\inchan, 0, \out, ~inputBus], ~inputGroup);  //
		record = Synth.new(\rec,[\micIn, ~inputBus, \buf, b, \pointerIn, ~ptrBus], ~recGroup);

	// ~winenv = Env.adsr(attackTime: 0, decayTime: 0.01, sustainLevel: 0.00, releaseTime: 0.0);
		// ~z = Buffer.sendCollection(s, ~winenv.asArray, 1);

		soundGood = Synth(\soundGood,[\in, ~fx3Bus, \out, 0]);
		wobble = Synth(\wobble,[\in: ~fx2Bus, out: ~fx3Bus ]);
		vme = Synth(\vintageSamplerEmu,[\in: ~fx1Bus, out:~fx2Bus ]);

		grain = Synth.new(\bufGrain, [ \rate, 1, \sndbuf, b, \out, ~fx1Bus, \amp, 1, \pointerIn, ~ptrBus]);
		pointer = Synth.new(\pointer,[\out, ~ptrBus, \buf, b]);
		
		s.sync; // sync the changes above to the server
	}

	// create a command to control the synth's 'amp' value:


	setGate { arg gate;
		grain.set(\gate, gate);
	}


	setBuffer { arg bufferNumber;
		var buffer = allBuffers[bufferNumber];
		grain.set(\sndbuf, buffer);
		record.set(\buf, buffer);
		pointer.set(\buf, buffer);
	}

	setRate { arg rate;
		grain.set(\rate, rate);
	}

	setPos { arg pos;
		grain.set(\pos, pos);
	}

	setAmp { arg amp;
		grain.set(\amp, amp);
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
		pointer.free;
		
		a.free; 
		b.free; 
		c.free; 
		d.free;
		e.free; 
		f.free; 

		~inputBus.free;
		~ptrBus.free;
		~fx1Bus.free;
		~fx2Bus.free;
		~fx3Bus.free;

		~inputGroup.free;
		~ptrGroup.free;
		~recGroup.free; 
		~grnGroup.free;

		~midside.free;


		grain = nil; wobble = nil; vme = nil;
    	record = nil; input = nil; soundGood = nil; pointer = nil;
    	a = nil; b = nil; c = nil; d = nil; e = nil; f = nil;
    	~inputBus = nil; ~ptrBus = nil; ~fx1Bus = nil; ~fx2Bus = nil; ~fx3Bus = nil;
    	~inputGroup = nil; ~ptrGroup = nil; ~recGroup = nil; ~grnGroup = nil;
	}

}