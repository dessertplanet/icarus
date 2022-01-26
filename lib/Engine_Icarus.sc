// Engine_Icarus

// Inherit methods from CroneEngine
Engine_Icarus : CroneEngine {

	// MxSamples specific
	var icarusPlayer;
	var osfun;
	// MxSamples ^

	*new { arg context, doneCallback;
		^super.new(context, doneCallback);
	}

	alloc {

		(0..5).do({arg i; 
			SynthDef("icarussynth"++i,{ 
				arg amp=0.5, hz=220, pan=0, envgate=0,
				pulse=0,saw=0,bend=0,subpitch=1,
				attack=0.015,decay=1,release=2,sustain=0.9,
				lpf=20000,resonance=0,portamento=0.1,tremelo=0,destruction=0,
				pwmcenter=0.5,pwmwidth=0.05,pwmfreq=10,detuning=0.1,
				feedback=0.5,delaytime=0.25, delaytimelag=0.1, sublevel=0;

				// vars
				var ender,snd,local,in,ampcheck,hz_dream,hz_sub,subdiv;

				// envelope stuff
				ender = EnvGen.ar(
					Env.new(
						curve: 'cubed',
						levels: [0,1,sustain,0],
						times: [attack+0.015,decay,release],
						releaseNode: 2,
					),
					gate: envgate,
				);

				// dreamcrusher++
				hz_dream=(Lag.kr(hz+(SinOsc.kr(LFNoise0.kr(1))*(((hz).cpsmidi+1).midicps-(hz))*detuning),portamento).cpsmidi + bend).midicps;
				in = VarSaw.ar(hz_dream,
					width:
					LFTri.kr(pwmfreq+rrand(0.1,0.3),mul:pwmwidth/2,add:pwmcenter),
					mul:0.5
				);
				// add suboscillator
				subdiv=2**subpitch;
				hz_sub=(Lag.kr(hz/subdiv+(SinOsc.kr(LFNoise0.kr(1))*(((hz/subdiv).cpsmidi+1).midicps-(hz/subdiv))*detuning),portamento).cpsmidi + bend).midicps;
				in = in + Pulse.ar(hz_sub,
					width:
					LFTri.kr(pwmfreq+rrand(0.1,0.3),mul:pwmwidth/2,add:pwmcenter),
					mul:0.5*sublevel	
				);
				in = Splay.ar(in);

				// random panning
				in = Balance2.ar(in[0] ,in[1],SinOsc.kr(
					LinLin.kr(LFNoise0.kr(0.1),-1,1,0.05,0.2)
				)*0.1);

				in = in * ender;
			    ampcheck = Amplitude.kr(Mix.ar(in));
			    in = in * (ampcheck > 0.02); // noise gate
			    local = LocalIn.ar(2);
			    local = OnePole.ar(local, 0.4);
			    local = OnePole.ar(local, -0.08);
			    local = Rotate2.ar(local[0], local[1],0.2);
				local = DelayC.ar(local, 0.5,
					Lag.kr(delaytime,0.2)
				);
			    local = LeakDC.ar(local);
			    local = ((local + in) * 1.25).softclip;

			    local = MoogLadder.ar(local,Lag.kr(lpf,1),res:Lag.kr(resonance,1));
				// add destruction thing
				local = ((local*((1-EnvGen.kr(
				        Env(
				            levels: [0, 1,0], 
				            times: [0.1,0.1],
							curve:\sine,
				        ),
				        gate: Dust.kr(destruction)
				))))+local)/2;
				// add tremelo
                // local = local * ((tremelo>0)*SinOsc.kr(tremelo,0,0.4)+(tremelo<0.0001));

			    LocalOut.ar(local*Lag.kr(feedback,1));
				
				snd= Balance2.ar(local[0]*0.2,local[1]*0.2,SinOsc.kr(
					LinLin.kr(LFNoise0.kr(0.1),-1,1,0.05,0.2)
				)*0.1);

				// manual pan
				snd = Mix.ar([
					Pan2.ar(snd[0],-1+(2*pan),amp),
					Pan2.ar(snd[1],1+(2*pan),amp),
				]);
			    SendTrig.kr(Dust.kr(30.0),0,Amplitude.kr(snd[0]+snd[1],3,3));
				Out.ar(0,snd)
			}).add;	
		});

	    osfun = OSCFunc(
	    	{ 
	    		arg msg, time; 
	    		if (msg[3]>0, {
		    		// [time, msg].postln;
					NetAddr("127.0.0.1", 10111).sendMsg("ampcheck",time,msg[3]);   //sendMsg works out the correct OSC message for you
	    		},{})
	    	},'/tr', context.server.addr);

		icarusPlayer = Array.fill(4,{arg i;
			Synth("icarussynth"++i, target:context.xg);
		});

		this.addCommand("icaruson","if", { arg msg;
			// lua is sending 1-index
			icarusPlayer[msg[1]-1].set(
				\envgate,1,
				\hz,msg[2],
			);
		});

		this.addCommand("icarusoff","i", { arg msg;
			// lua is sending 1-index
			icarusPlayer[msg[1]-1].set(
				\envgate,0,
			);
		});

		this.addCommand("amp","f", { arg msg;
			(0..5).do({arg i; 
				icarusPlayer[i].set(\amp,msg[1]);
			});
		});

		this.addCommand("pan","f", { arg msg;
			(0..5).do({arg i; 
				icarusPlayer[i].set(\pan,msg[1]);
			});
		});

		this.addCommand("attack","f", { arg msg;
			(0..5).do({arg i; 
				icarusPlayer[i].set(\attack,msg[1]);
			});
		});

		this.addCommand("release","f", { arg msg;
			(0..5).do({arg i; 
				icarusPlayer[i].set(\release,msg[1]);
			});
		});

		this.addCommand("decay","f", { arg msg;
			(0..5).do({arg i; 
				icarusPlayer[i].set(\decay,msg[1]);
			});
		});

		this.addCommand("sustain","f", { arg msg;
			(0..5).do({arg i; 
				icarusPlayer[i].set(\sustain,msg[1]);
			});
		});

		this.addCommand("delaytime","f", { arg msg;
			(0..5).do({arg i; 
				icarusPlayer[i].set(\delaytime,msg[1]);
			});
		});

		this.addCommand("delaytimelag","f", { arg msg;
			(0..5).do({arg i; 
				icarusPlayer[i].set(\delaytimelag,msg[1]);
			});
		});

		this.addCommand("feedback","f", { arg msg;
			(0..5).do({arg i; 
				icarusPlayer[i].set(\feedback,msg[1]);
			});
		});

		this.addCommand("destruction","f", { arg msg;
			(0..5).do({arg i; 
				icarusPlayer[i].set(\destruction,msg[1]);
			});
		});

		this.addCommand("tremelo","f", { arg msg;
			(0..5).do({arg i; 
				icarusPlayer[i].set(\tremelo,msg[1]);
			});
		});

		this.addCommand("lpf","f", { arg msg;
			(0..5).do({arg i; 
				icarusPlayer[i].set(\lpf,msg[1]);
			});
		});
		this.addCommand("resonance","f", { arg msg;
			(0..5).do({arg i; 
				icarusPlayer[i].set(\resonance,msg[1]);
			});
		});

		this.addCommand("portamento","f", { arg msg;
			(0..5).do({arg i; 
				icarusPlayer[i].set(\portamento,msg[1]);
			});
		});

		this.addCommand("detuning","f", { arg msg;
			(0..5).do({arg i; 
				icarusPlayer[i].set(\detuning,msg[1]);
			});
		});

		this.addCommand("pulse","f", { arg msg;
			(0..5).do({arg i; 
				icarusPlayer[i].set(\pulse,msg[1]);
			});
		});

		this.addCommand("saw","f", { arg msg;
			(0..5).do({arg i; 
				icarusPlayer[i].set(\saw,msg[1]);
			});
		});

		this.addCommand("sub","f", { arg msg;
			(0..5).do({arg i; 
				icarusPlayer[i].set(\sublevel,msg[1]);
			});
		});

		this.addCommand("pwmcenter","f", { arg msg;
			(0..5).do({arg i; 
				icarusPlayer[i].set(\pwmcenter,msg[1]);
			});
		});
		this.addCommand("pwmwidth","f", { arg msg;
			(0..5).do({arg i; 
				icarusPlayer[i].set(\pwmwidth,msg[1]);
			});
		});
		this.addCommand("pwmfreq","f", { arg msg;
			(0..5).do({arg i; 
				icarusPlayer[i].set(\pwmfreq,msg[1]);
			});
		});
		this.addCommand("bend","f", { arg msg;
			(0..5).do({arg i;
				icarusPlayer[i].set(\bend,msg[1]);
			});
		});
		this.addCommand("subpitch","f", { arg msg;
			(0..5).do({arg i;
				icarusPlayer[i].set(\subpitch,msg[1]);
			});
		});

	}

	free {
		(0..5).do({arg i; icarusPlayer[i].free});
		osfun.free;
	}
}
