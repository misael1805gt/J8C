/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package j8c.Core;

import java.util.Random;

import j8c.Debugger;

/**
 *
 * @author Misael
 */
public class CPU implements Runnable {
	private static CPU cpu = null;
	private byte[] memory = new byte[4096];
	private int PC = 0x0;
	private short[] charAddress = new short[16];
	private byte[] regV = new byte[0x10];
	private short I = 0x00;
	private byte screen[] = new byte[64 * 32];
	protected int opcode;
	private boolean[] Keys;

	private int lastPressed = -1;
	private boolean keyIsPressed = false;
	private String asm = "";
	// private boolean drawFlag = true;
	private static Thread CPUThread;

	private static boolean pause = false;
	@SuppressWarnings("unused")
	private static boolean controllerQueue = false;
	private static boolean breakTheEmu = false;

	// private static boolean pauseTheEmu=false;
	// Hexadecimal F == Binary 1111
	// FF= 1 Byte
	private CPU() {

	}

	public static void setNull() {
		cpu = null;
		// I need this until StopCPU() works properly
	};

	public static CPU getInstance() {
		if (cpu == null) {
			cpu = new CPU();
		}
		return cpu;

	}

	@Override

	public void run() {

		while (!breakTheEmu) {
			if (!pause) {
				Keys = Keyboard.getKeyArray();
				lastPressed = Keyboard.getLastPressed();
				controllerQueue = false;
				cycle();
				
			}
		}

	}

	public void init() {
		breakTheEmu = false;
		Keys = Keyboard.getKeyArray();
		lastPressed = Keyboard.getLastPressed();
		// Setting the Charset

		short[] charset = { 0xF0, 0x90, 0x90, 0x90, 0xF0, // 0
				0x20, 0x60, 0x20, 0x20, 0x70, // 1
				0xF0, 0x10, 0xF0, 0x80, 0xF0, // 2
				0xF0, 0x10, 0xF0, 0x10, 0xF0, // 3
				0x90, 0x90, 0xF0, 0x10, 0x10, // 4
				0xF0, 0x80, 0xF0, 0x10, 0xF0, // 5
				0xF0, 0x80, 0xF0, 0x90, 0xF0, // 6
				0xF0, 0x10, 0x20, 0x40, 0x40, // 7
				0xF0, 0x90, 0xF0, 0x90, 0xF0, // 8
				0xF0, 0x90, 0xF0, 0x10, 0xF0, // 9
				0xF0, 0x90, 0xF0, 0x90, 0x90, // A
				0xE0, 0x90, 0xE0, 0x90, 0xE0, // B
				0xF0, 0x80, 0x80, 0x80, 0xF0, // C
				0xE0, 0x90, 0x90, 0x90, 0xE0, // D
				0xF0, 0x80, 0xF0, 0x80, 0xF0, // E
				0xF0, 0x80, 0xF0, 0x80, 0x80, // F
		};

		int a = 0;
		int b = 0;

		for (int i = 0; i < 80; i++) {
			memory[i] = (byte) charset[i];
		}

		for (short c : charset) {
			charset[a] = (byte) c;
			a++;
			if (a % 5 == 0 || a == 0) {
				charAddress[b] = (short) a;
				b++;
			}

		}

		// Charset has been set

		PC = 0x200;
		Stack.reset();
		loadMemory();
		CPUThread = new Thread(this);
		CPUThread.setName("Interpreter");
		CPUThread.start();

	}

	public void stopCPU() {
		if (CPUThread.isAlive()) {
			breakTheEmu = true;
			try {
				CPUThread.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				CPUThread.interrupt();
			}
		}
		for (int i = 0; i < memory.length; i++) {
			memory[i] = 0;
		}
		for (int i = 0; i < screen.length; i++) {
			screen[i] = 0;
		}
		Graphics.cleanBl();
		Stack.reset();

	}

	public void pauseCPU() {
		pause = !pause;
	}

	public void PressedKeyInterrupt() {

		controllerQueue = true;
		Keys = Keyboard.getKeyArray();
		keyIsPressed = Keyboard.someKeyIsPressed();
		lastPressed = Keyboard.getLastPressed();
		controllerQueue = true;

	}

	public void unpressedKeyInterrupt() {

		Keys = Keyboard.getKeyArray();
		keyIsPressed = false;
		controllerQueue = true;

	}

	private void cycle() {
//		int byteOrder = 0;
////		for (byte b : memory) {
////			System.out.print(Integer.toHexString(b));
////
////			byteOrder++;
////			if (byteOrder == 1) {
////				System.out.print("\n");
////				byteOrder = 0;
////			}
////		}
		// Timers.setCurrent(System.nanoTime());
		// Timers.setAfter(System.nanoTime());
		fetchOpcode();
		decodeExecute();

		if (Debugger.isDebuggerStarted()) {
			logToRegisterWatch();
			int sleepTime = Debugger.getInstance().getSleepTimer();
			try {
				Thread.sleep(sleepTime);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else {

			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	public void logToRegisterWatch() {
		Debugger.getInstance().updateRegisters(regV, I, Stack.getData(), Stack.getLastOp(), Stack.getPointer(), PC,
				asm);
	}

	private void loadMemory() {
		byte[] rom = RomLoader.getInstance().getRom();
//		for (int i = 0; i < rom.length; ++i) {
//			memory[512 + i] = rom[i];
//		}
		System.arraycopy(rom, 0, memory, 512, rom.length);
		// teste de leitura da memoria
//		int i = 0;
//		for (byte a : memory) {
//
//			System.out.print(Integer.toHexString(a));
//			++i;
//			if (i == 2) {
//				System.out.print(" ");
//			}
//			if (i == 4) {
//				System.out.print("\n");
//				i = 0;
//			}
//
//		}
	}

	public void fetchOpcode() {
		byte opcodeFbyte = memory[PC];
		byte opcodeSbyte = memory[PC + 1];

		opcode = Byte.toUnsignedInt(opcodeFbyte);

		opcode <<= 8;

		opcode |= Byte.toUnsignedInt(opcodeSbyte);

	}

	private void decodeExecute() {

		int instructionId = (opcode & 0xf000);
		int instructionArgs = (opcode & 0x0fff);
		new Instruction(instructionId, instructionArgs).execute();
		// System.out.println(Integer.toHexString(opcode));

	}

	private static class Stack {

		private static int stack[] = new int[16];
		private static int pointer = 0;
		private static String lastOp = "null";

		public static void push(int val) {
			stack[pointer] = val;
			pointer++;
			lastOp = "push";
		}

		public static int pop() {
			lastOp = "pop";
			pointer--;
			if (pointer > -1) {
				int value = stack[pointer];
				return value;
			} else {
				System.out.println("Stack pointer is negative, W T F?");
				return 0;
			}

		}

		private static String getLastOp() {
			return lastOp;
		}

		public static int[] getData() {
			return stack;
		}

		public static int getPointer() {
			return pointer;
		}

		public static void reset() {
			pointer = 0;
		}
	}

	private class Instruction {

		// 00 System Functions
		// 80 Math functions
		// Instruction like functions
		int id = 0;
		int args = 0;

		public void logToDebugger(String logasm) {

			if (Debugger.isDebuggerStarted()) {
				asm = logasm;
			}
		}

		public int toUnsignedInt(byte value) {
			return ((int) value) & 0xff;
		}

		public Instruction(int id, int args) {
			this.id = id;
			this.args = args;

		}

		public void processMath() {
			int mathInstId = (args & 0x000f);
			byte Xreg = (byte) ((args & 0x0f00) >> 8);
			byte Yreg = (byte) ((args & 0x00f0) >> 4);
			switch (mathInstId) {
			case (0x0):
				logToDebugger("ld V[" + Xreg + "]," + "V[" + Yreg + "]");
				regV[Xreg] = regV[Yreg];
				break;
			case (0x1):
				logToDebugger("or V[" + Xreg + "],V[" + Yreg + "]");
				regV[Xreg] |= regV[Yreg];
				break;
			case (0x2):
				logToDebugger("and V[" + Xreg + "],V[" + Yreg + "]");
				regV[Xreg] &= regV[Yreg];
				break;
			case (0x3):
				logToDebugger("xor V[" + Xreg + "],V[" + Yreg + "]");
				regV[Xreg] ^= regV[Yreg];
				break;
			case (0x4):
				logToDebugger("addcarry V[" + Xreg + "],V[" + Yreg + "]");
				if ((regV[Xreg] + regV[Yreg]) > 255) {

					regV[0xf] = 1;
					// regV[Xreg] = (byte) 0xFF;

				}
				regV[Xreg] += regV[Yreg];
				break;
			case (0x5):
				logToDebugger("subcarry V[" + Xreg + "],V[" + Yreg + "]");
				if (toUnsignedInt(regV[Xreg]) > toUnsignedInt(regV[Yreg])) {

					regV[0xf] = 1;
				}
				regV[Xreg] -= regV[Yreg];
				break;
			case (0x6):
				logToDebugger("rsftob V[" + Xreg + "]");
				regV[0xf] = (byte) (regV[Xreg] & 0x1);
				regV[Xreg] >>>= 1;
				break;
			case (0x7):
				logToDebugger("subcarry V[" + Xreg + "],V[" + Yreg + "]");
				if ((regV[Xreg] < regV[Yreg])) {

					regV[0xf] = 1;

				} else {
					regV[0xf] = 0;

				}
				regV[Xreg] = (byte) (regV[Yreg] - regV[Xreg]);
				break;
			case (0xe):
				logToDebugger("lsftob V[" + Xreg + "]");
				regV[0xf] = (byte) (regV[Xreg] & 0x80);
				regV[Xreg] = (byte) ((regV[Xreg] << 1) & 0xFF);
				break;
			default:
				System.out.println("Unknown math instruction" + Integer.toHexString(mathInstId));
				break;

			}
			PC += 2;
		}

		public void execute() {
			// TODO change the if and else to a switch statement
			// Do Something
			// System.out.println("The instruction is:" + Integer.toHexString((id & args)));
			int Xreg = -1;
			int Yreg = -1;
			byte value = 0;
			switch (id) {

			case 0x0:
				switch (args) {
				case (0x0E0):
					logToDebugger("clsc");
					for (int i = 0; i < screen.length; ++i) {
						screen[i] = 0;
					}
					Graphics.Draw(screen, "");
					PC += 2;
					break;
				case (0x0ee):
					logToDebugger("ret");
					PC = Stack.pop();
					PC += 2;
					break;
				}
				break;
			case (0x1000):
				logToDebugger("goto " + (args));
				PC = args;
				break;

			case (0x2000):
				logToDebugger("call " + (args));
				Stack.push(PC);
				PC = args;
				break;

			case (0x3000):
				logToDebugger("skpfeq V[" + ((args & 0xF00) >> 8) + "]," + (args & 0x0FF));
				Xreg = (args & 0xF00) >> 8;
				value = (byte) (args & 0x0FF);
				if (regV[Xreg] == value) {
					PC += 4;
				} else {
					PC += 2;
				}
				break;

			case (0x4000):
				logToDebugger("skpfneq V[" + ((args & 0xF00) >> 8) + "]," + (args & 0x0FF));
				Xreg = (args & 0xF00) >> 8;

				value = (byte) (args & 0x0FF);
				if (regV[Xreg] != value) {
					PC += 4;
				} else {
					PC += 2;
				}
				break;

			case (0x5000):
				logToDebugger("skpfeq V[" + ((args & 0xF00) >> 8) + "],V[" + ((args & 0x0f0) >> 4) + "]");

				Xreg = (args & 0xf00) >> 8;
				Yreg = (args & 0x0f0) >> 4;
				if (regV[Xreg] == regV[Yreg]) {
					PC += 4;
				} else {
					PC += 2;
				}
				break;

			case (0x6000):
				logToDebugger("ld V[" + ((args & 0xF00) >> 8) + "]," + toUnsignedInt((byte) (args & 0x0ff)));
				Xreg = (args & 0x0f00) >> 8;
				value = (byte) toUnsignedInt((byte) (args & 0xff));
				regV[Xreg] = value;
				PC += 2;
				break;

			case (0x7000):
				logToDebugger("add V[" + ((args & 0xF00) >> 8) + "]," + toUnsignedInt((byte) (args & 0x0ff)));
				Xreg = (args & 0x0f00) >> 8;
				value = 0;
				value += (args & 0x0ff);
				regV[Xreg] += value;
				PC += 2;
				break;

			case (0x8000):
				processMath();
				break;
			// Math instructions

			case (0x9000):

				Xreg = ((args & 0x0f00) >> 8);
				Yreg = ((args & 0x00f0) >> 4);
				logToDebugger("jmpifneq V[" + Xreg + "],V[" + Yreg + "]");
				if (regV[Xreg] != regV[Yreg]) {
					PC += 4;
				} else {
					PC += 2;
				}
				break;

			case (0xA000):

				I = (short) (args & 0x0fff);
				logToDebugger("ld " + I);
				PC += 2;
				break;

			case (0xb000):
				logToDebugger("jmplded" + (args & 0x0fff));
				PC = (args & 0x0fff) + I;
				break;
			case (0xc000):
				logToDebugger("rnd" + ((args & 0xf00) >> 8) + "," + (args & 0xff) + "");
				Xreg = (args & 0xf00) >> 8;
				regV[Xreg] ^= regV[Xreg];
				regV[Xreg] += ((byte) new Random().nextInt(255)) & args & 0xff;
				PC += 2;
				break;
			}
			if (id == 0xd000) {
				logToDebugger(
						"drw V[" + ((args & 0xF00) >> 8) + "],V[" + (byte) ((args & 0x0f0) >> 4) + "]," + (args & 0xf));
				// Draw
				// Tela 64*32
				// Timers.setCurrent(System.currentTimeMillis());
				short valX = (short) toUnsignedInt(regV[(args & 0xf00) >> 8]);
				short valY = (short) toUnsignedInt(regV[(args & 0xf0) >> 4]);
				int height = args & 0xf;
				regV[0xf] = 0;
				for (int Y = 0; Y < height; Y++) {
					byte pixel = memory[I + Y];
					for (int X = 0; X < 8; X++) {
						byte num = 0;
						num += (pixel & (0x80 >>> X));
						if (num != 0) {

							int screenRX = valX + X;
							int screenRY = valY + Y;
							// System.out.println(screenRX);
							// System.out.println(screenRY);
							int a = 0;
							if ((screenRX >= 64 || screenRY >= 32)) {
								if (Options.getInstance().isXWrappingEnabled()) {
									for (a = screenRX / 64; a > 0; a--) {
										screenRX -= 64;

									}
								}
								if (Options.getInstance().isXWrappingEnabled()) {
									for (a = screenRY / 32; a > 0; a--) {
										screenRY -= 32;
									}
								}
							}
							int index = (screenRX + ((screenRY) * 64));

							if (screen[index] == 1) {
								regV[0xf] = 1;
							}
							screen[index] ^= 1;

						}
					}
				}

				Graphics.Draw(screen, "");
				// Timers.setAfter(System.currentTimeMillis());
				PC += 2;
				return;
			}
			if (id == 0xe000) {
				if (0x9e == (args & 0xff)) {
					Xreg = (args & 0xf00) >> 8;
					if (Keys[regV[Xreg]]) {
						PC += 4;
					} else {
						PC += 2;
					}
					logToDebugger("skp V[" + Xreg + "]");
				}
				if (0xA1 == (args & 0xff)) {
					Xreg = (args & 0xf00) >> 8;
					logToDebugger("sknp V[" + Xreg + "]");
					if (!Keys[regV[Xreg]]) {
						PC += 4;
					} else {
						PC += 2;
					}
				}

			}
			if (id == 0xf000) {
				int copyIndex;
				switch (args & 0xff) {
				case (0x07):
					Xreg = (args & 0xf00) >> 8;
					logToDebugger("unlddt V[" + Xreg + "]");
					regV[Xreg] = (byte) Timers.getDelayTimer();
					PC += 2;
					break;
				case (0x0a):
					Xreg = (args & 0xf00) >> 8;
					logToDebugger("waitkpld V[ " + Xreg + "]");
					if (keyIsPressed || Keyboard.getLastPressed() != -1) {
						regV[Xreg] = (byte) lastPressed;
						PC += 2;
					}
					break;
				case (0x15):
					Xreg = (args & 0xf00) >> 8;
					logToDebugger("lddtfreg V[" + Xreg + "]");
					Timers.setDelayTimer(regV[Xreg]);
					PC += 2;
					break;
				case (0x18):
					Xreg = (args & 0xf00) >> 8;
					logToDebugger("ldstfreg V[" + Xreg + "]");
					Timers.setSoundTimer(regV[Xreg]);
					PC += 2;
					break;
				case (0x1e):
					Xreg = (args & 0xf00) >> 8;
					logToDebugger("addi V[" + Xreg + "]");
					I += toUnsignedInt(regV[Xreg]);
					PC += 2;
					break;
				case (0x29):
					Xreg = (args & 0xf00) >> 8;
//					I = charAddress[regV[Xreg]];
					logToDebugger("ldspr V[" + Xreg + "]");
					I = (short) toUnsignedInt((byte) (regV[Xreg] * 0x5));
					PC += 2;
					break;
				case (0x33):
					Xreg = (args & 0xf00) >> 8;
					short valueTobcd = (short) toUnsignedInt(regV[Xreg]);
					logToDebugger("BCD V[" + Xreg + "]");
					memory[I] = (byte) (valueTobcd / 100);
					memory[I + 1] = (byte) ((valueTobcd / 10) % 10);
					memory[I + 2] = (byte) ((valueTobcd % 100) % 10);
					PC += 2;
					break;
				case (0x55):
					copyIndex = (args & 0xf00) >> 8;
					logToDebugger("rgdump [" + I + "],V[" + copyIndex + "]");

//					for (int i = 0; i <= copyIndex; i++) {
//						memory[I + i] = regV[i];
//					}
					System.arraycopy(regV, 0, memory, I, copyIndex + 1);
					PC += 2;
					break;
				case (0x65):
					copyIndex = (args & 0xf00) >> 8;
					logToDebugger("memdump V[" + copyIndex + "]," + copyIndex + "");
//					for (int i = 0; i <= copyIndex; i++) {
//						regV[i] = memory[I + i];
//					}
					System.arraycopy(memory, I + 0, regV, 0, copyIndex + 1);
					PC += 2;
					break;
				default:
					break;
				}
			}

		}

	}

}
