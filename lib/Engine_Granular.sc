Engine_Granular : CroneEngine {
	var kernel, debugPrinter;

	*new { arg context, doneCallback;
		^super.new(context, doneCallback);
	}

	alloc {
		kernel = Granular.new(Crone.server);

		this.addCommand(\gate, "f", { arg msg;
			var gate = msg[1].asFloat;
			kernel.setGate(gate);
		});

		this.addCommand(\buffer, "f", { arg msg;
			var buffer = msg[1].asFloat;
			kernel.setBuffer(buffer);
		});

		this.addCommand(\rate, "f", { arg msg;
			var rate = msg[1].asFloat;
			kernel.setRate(rate);
		});

		this.addCommand(\pos, "f", { arg msg;
			var pos = msg[1].asFloat;
			kernel.setPos(pos);
		});

		this.addCommand(\dur, "f", { arg msg;
			var dur = msg[1].asFloat;
			kernel.setDur(dur);
		});

		this.addCommand(\dens, "f", { arg msg;
			var dens = msg[1].asFloat;
			kernel.setDens(dens);
		});

		this.addCommand(\jitter, "f", { arg msg;
			var jitter = msg[1].asFloat;
			kernel.setJitter(jitter);
		});

		this.addCommand(\release, "f", { arg msg;
			var release = msg[1].asFloat;
			kernel.setRelease(release);
		});

		this.addCommand(\triggerType, "f", { arg msg;
			var triggerType = msg[1].asFloat;
			kernel.setTriggerType(triggerType);
		});

		this.addCommand(\maxDelay, "f", { arg msg;
			var maxDelay = msg[1].asFloat;
			kernel.setMaxDelay(maxDelay);
		});

		this.addCommand(\minDelay, "f", { arg msg;
			var minDelay = msg[1].asFloat;
			kernel.setMinDelay(minDelay);
		});

		this.addCommand(\depth, "f", { arg msg;
			var depth = msg[1].asFloat;
			kernel.setDepth(depth);
		});

		this.addCommand(\mix, "f", { arg msg;
			var mix = msg[1].asFloat;
			kernel.setMix(mix);
		});

		this.addCommand(\amp, "f", { arg msg;
			var amp = msg[1].asFloat;
			kernel.setAmp(amp);
		});

		this.addCommand(\feedback, "f", { arg msg;
			var feedback = msg[1].asFloat;
			kernel.setFeedback(feedback);
		});


		// Additional VME related commands
		this.addCommand(\bitDepth, "f", { arg msg;
			var bitDepth = msg[1].asFloat;
			kernel.setVMEBitDepth(bitDepth);
		});

		this.addCommand(\sampleRate, "f", { arg msg;
			var sampleRate = msg[1].asFloat;
			kernel.setVMESampleRate(sampleRate);
		});

		

		this.addCommand(\drive, "f", { arg msg;
			var drive = msg[1].asFloat;
			kernel.setVMEDrive(drive);
		});

		this.addCommand(\cutoffFreq, "f", { arg msg;
			var cutoffFreq = msg[1].asFloat;
			kernel.setVMECutoffFreq(cutoffFreq);
		});

		this.addCommand(\vmeMix, "f", { arg msg;
			var vmeMix = msg[1].asFloat;
			kernel.setVMEMix(vmeMix);
		});

		this.addCommand(\resetPointer, "f", { arg msg;
			kernel.resetPointer;
		});



		// debugPrinter = { loop { [context.server.peakCPU, context.server.avgCPU].postln; 3.wait; } }.fork;
	}

	free {
		kernel.free;
		// debugPrinter.stop;
	}
}
