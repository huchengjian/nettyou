package com.hisign.netty.worker;

public class SDKResult {

	public enum State {
		Success(0), NotDetectFace(-1), SDKError(-2), ParameterError(-3), OtherError(-4), Null(0), MutilFaceError(0);

		public int value;

		private State(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
		
		public State setValue(int x) {
			this.value = x;
			return this;
		}
	}

	public State state;

	public byte[] data;

	public SDKResult() {
		this.state = State.Null;
		this.data = new byte[4];
	}

	public SDKResult(State state, byte[] data) {
		this.state = state;
		this.data = data;
	}
}
