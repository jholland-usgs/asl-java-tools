package seed;

public class GenericBlockette extends Blockette {
	private short number = -1;

	public GenericBlockette(int bufferSize) {
		super(bufferSize);
	}

	public GenericBlockette() {
		super();
	}

	public GenericBlockette(byte[] buffer) {
		super.setNext(this);
		super.setPrev(this);
		reload(buffer);
	}

	public void reload(byte [] buffer)
	{
		number = peekBlocketteType(buffer);
		super.reload(buffer);
	}

	@Override
	protected short blocketteNumber() {
		return number;
	}
}
