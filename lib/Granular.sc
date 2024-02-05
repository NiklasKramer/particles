Granular {
classvar a,b,c,d,e,f,aa,bb,cc,dd,ee,ff, allLeftBuffers, allRightBuffers;

// instantiate variables here
var <grain, <inputBus, <ptrBus, <fxBus, <inputGroup, <ptrGroup, <recGroup, <grnGroup, <fxGroup, <input, <record, <soundGood, <wobble, <vme, <grain, <pointer;

*initClass {

	StartUp.add {
		var s = Server.default;

		// we need to make sure the server is running before asking it to do anything
		s.waitForBoot {
			a = Buffer.alloc(s, s.sampleRate * 0.5, numChannels: 1);
			aa = Buffer.alloc(s, s.sampleRate * 0.5, numChannels: 1);

			b = Buffer.alloc(s, s.sampleRate * 1, numChannels: 1);
			bb = Buffer.alloc(s, s.sampleRate * 1, numChannels: 1);

			c = Buffer.alloc(s, s.sampleRate * 2, numChannels: 1);
			cc = Buffer.alloc(s, s.sampleRate * 2, numChannels: 1);

			d = Buffer.alloc(s, s.sampleRate * 3, numChannels: 1);
			dd = Buffer.alloc(s, s.sampleRate * 3, numChannels: 1);

			e = Buffer.alloc(s, s.sampleRate * 5, numChannels: 1);
			ee = Buffer.alloc(s, s.sampleRate * 5, numChannels: 1);

			f = Buffer.alloc(s, s.sampleRate * 8, numChannels: 1);
			ff = Buffer.alloc(s, s.sampleRate * 8, numChannels: 1);

			allLeftBuffers = [a,b,c,d,e,f];
			allRightBuffers = [aa,bb,cc,dd,ee,ff];

		


			SynthDef(\input2, {
				arg out=0, amp=0.5, inchan=0;
				var signal;
				signal = SoundIn.ar(inchan);
				signal = signal * amp;
				Out.ar(out, signal);
				}).add;

			SynthDef(\input, {
				arg out=0, amp=0.5, inchanL=0, inchanR=1; // Default to the first two channels for stereo input
				var signalL, signalR, stereoSignal;
				
				// Capture left and right channels separately
				signalL = SoundIn.ar(inchanL);
				signalR = SoundIn.ar(inchanR);
				
				// Apply amplitude scaling
				signalL = signalL * amp;
				signalR = signalR * amp;
				
				// Combine into a stereo signal
				stereoSignal = [signalL, signalR];
				
				// Output the stereo signal
				Out.ar(out, stereoSignal);
			}).add;


			SynthDef(\rec, {
				arg micInLeft=0, micInRight=1, leftBuf=0, rightBuf=0, pointerIn=0, feedback=0.5;
				var sigLeft, sigRight, pointer, existingLeft, existingRight, mixedLeft, mixedRight;
				
				// Read stereo input
				sigLeft = In.ar(micInLeft, 1);
				sigRight = In.ar(micInRight, 1);
				
				pointer = In.ar(pointerIn, 1);
				
				// Read existing content from buffers
				existingLeft = BufRd.ar(1, leftBuf, pointer, loop: 1);
				existingRight = BufRd.ar(1, rightBuf, pointer, loop: 1);
				
				// Mix incoming signal with existing content
				mixedLeft = ((sigLeft * (1 - feedback)) + (existingLeft * feedback));
				mixedRight = ((sigRight * (1 - feedback)) + (existingRight * feedback));
				
				// Write mixed signals back to buffers
				BufWr.ar(mixedLeft, leftBuf, pointer);
				BufWr.ar(mixedRight, rightBuf, pointer);
			}).add;

		

			SynthDef(\pointer, {
				arg out=0, buf=0, resetTrig=0;
				var signal;
				signal = Phasor.ar(trig: resetTrig, rate: BufRateScale.kr(buf), start: 0, end: BufFrames.kr(buf));
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



			SynthDef(\bufGrain, {
				arg out=0, leftSndBuf, rightSndBuf, rate=1.5, dur=1, pos=0.0, dens=10, amp=0.7, spread=0, release=0.5, jitter=1, gate=1, envbuf=(-1), triggerType=0, pointerIn=0;
				var soundLeft, soundRight, triggerSignal, env, ptrValue, shouldGenerateSound;
				var randomPosition, distanceToHead, threshold;

				ptrValue = In.ar(pointerIn, 1) / BufFrames.kr(leftSndBuf);

				threshold = (1 - rate.abs).abs * 0.05 + 0.05; 

				randomPosition = LFNoise1.kr(2).wrap(0, 1);
				distanceToHead = abs(ptrValue - randomPosition);

				shouldGenerateSound = distanceToHead > threshold;

				

				triggerSignal = Select.ar(triggerType, [Dust.ar(dens * shouldGenerateSound), Impulse.ar(dens * shouldGenerateSound)]);
				// Generate grains for left channel
				soundLeft = GrainBuf.ar(
					numChannels: 1,
					trigger: triggerSignal,
					dur: dur,
					sndbuf: leftSndBuf, // Use left channel mono buffer
					rate: rate,
					pos: randomPosition,
					interp: 2,
					pan: -1,
					envbufnum: envbuf
				);

				// Generate grains for right channel
				soundRight = GrainBuf.ar(
					numChannels: 1,
					trigger: triggerSignal,
					dur: dur,
					sndbuf: rightSndBuf, // Use right channel mono buffer
					rate: rate,
					pos: randomPosition + spread,
					interp: 2,
					pan: 1,
					envbufnum: envbuf
				);

				env = EnvGen.ar(Env.adsr(releaseTime: release), gate: gate, doneAction: 2);
				Out.ar(out, [(soundLeft * env * amp), (soundRight * env * amp)]);
			}).add;



			
			SynthDef(\vintageSamplerEmu, { |in=0, out=0, bitDepth=12, sampleRate=26040, drive=0.5, mix=0.5, filterControl=0.5|
				var signal, bitcrushed, saturated, mixed, filtered, cutoffFreqLPF, cutoffFreqHPF, dryAndHighPass;

				signal = In.ar(in, 2);

				bitcrushed = Decimator.ar(signal, sampleRate, bitDepth);
				saturated = SineShaper.ar(bitcrushed, drive);
				mixed = XFade2.ar(signal, saturated, mix * 2 - 1);

				cutoffFreqLPF = LinExp.kr(filterControl.clip(0, 0.5) * 2, 0, 1, 20, 15000);
				cutoffFreqHPF = LinExp.kr((filterControl - 0.5).clip(0, 0.5) * 2, 0, 1, 20, 15000);

				dryAndHighPass = Select.ar(filterControl > 0.51, [
					mixed,
					HPF.ar(mixed, cutoffFreqHPF)
				]);

				filtered = Select.ar(filterControl > 0.5, [
					LPF.ar(mixed, cutoffFreqLPF),
					dryAndHighPass,
				]);

				Out.ar(out, filtered);
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

	~inputBus = Bus.audio(s, 2); // Changed from 1 to 2 channels for stereo
    ~ptrBus = Bus.audio(s, 1); // Changed from 1 to 2 channels for stereo
    ~fx1Bus = Bus.audio(s, 2); // Already stereo, but ensuring clarity
    ~fx2Bus = Bus.audio(s, 2); // Already stereo, but ensuring clarity
    ~fx3Bus = Bus.audio(s, 2); // Already stereo, but ensuring clarity

    ~inputGroup = Group.new();
    ~ptrGroup = Group.after(~inputGroup);
    ~recGroup = Group.after(~ptrGroup);
    ~grnGroup = Group.after(~recGroup);



	input = Synth.new(\input, [\inchan, 0, \out, ~inputBus], ~inputGroup);  //
	record = Synth.new(\rec, [
        \micInLeft, ~inputBus.index, // Use the first channel of the stereo bus
        \micInRight, ~inputBus.index + 1, // Use the second channel of the stereo bus
        \leftBuf, allLeftBuffers[0].bufnum,
        \rightBuf, allRightBuffers[0].bufnum,
        \pointerIn, ~ptrBus.index
    ], ~recGroup);

	// ~winenv = Env.adsr(attackTime: 0, decayTime: 0.01, sustainLevel: 0.00, releaseTime: 0.0);
	// ~z = Buffer.sendCollection(s, ~winenv.asArray, 1);

	// soundGood = Synth(\soundGood,[\in, ~fx3Bus, \out, 0]);
	wobble = Synth(\wobble,[\in: ~fx2Bus, out: 0]);
	vme = Synth(\vintageSamplerEmu,[\in: ~fx1Bus, out:~fx2Bus ]);

	grain = Synth.new(\bufGrain, [ \rate, 1, \leftSndBuf, b, \rightSndBuf, bb, \out, ~fx1Bus, \amp, 1, \pointerIn, ~ptrBus]);
	pointer = Synth.new(\pointer,[\out, ~ptrBus, \buf, b]);
	
	s.sync; // sync the changes above to the server
}



setGate { arg gate;
	grain.set(\gate, gate);
}


setBuffer { arg bufferNumber;
    var leftBuffer = allLeftBuffers[bufferNumber];
    var rightBuffer = allRightBuffers[bufferNumber];
    record.set(\leftBuf, leftBuffer, \rightBuf, rightBuffer);
	grain.set(\leftSndBuf, leftBuffer, \rightSndBuf, rightBuffer);
	pointer.set(\buf, leftBuffer);
}

setRate { arg rate;
	grain.set(\rate, rate);
}

setPos { arg pos;
	grain.set(\pos, pos);
}

setSpread { arg spread;
	grain.set(\spread, spread);
}

setFeedback { arg feedback;
	record.set(\feedback, feedback);
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
setVMEFilterControl { arg filterControl;
	vme.set(\filterControl, filterControl);
}

setVMEMix { arg mix;
	vme.set(\mix, mix);
}
resetPointer {
	pointer.set(\resetTrig, 1);  
}

// IMPORTANT!
// free our synth after we're done with it:
free {

	"Free Method called".postln;

	grain.free;
	wobble.free;
	vme.free;
	record.free;
	input.free;
	soundGood.free;
	pointer.free;



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
	
	~inputBus = nil; ~ptrBus = nil; ~fx1Bus = nil; ~fx2Bus = nil; ~fx3Bus = nil;
	~inputGroup = nil; ~ptrGroup = nil; ~recGroup = nil; ~grnGroup = nil;
}

}
