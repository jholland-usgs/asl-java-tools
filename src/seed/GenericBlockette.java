package seed;

public class GenericBlockette extends Blockette {

	@Override
	protected short blocketteNumber() {
		return -1;
	}

    public GenericBlockette(byte[] b)
    {
        super(b);
    }
}
