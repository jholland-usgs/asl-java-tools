package asl.util;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class MicroCalendar extends GregorianCalendar {
	public static final long serialVersionUID = 0;
	
	private static int MILLISECOND = Calendar.MILLISECOND;
	public static int MICROSECOND = MILLISECOND;
	
	private int microExtension = 0;
	
	public Object clone() {
		MicroCalendar dup = (MicroCalendar)super.clone();
		dup.microExtension = microExtension;
		return dup;
	}
	
	public int get(int field) {
		return (field == MICROSECOND) ? super.get(Calendar.MILLISECOND) + microExtension : super.get(field);
	}
	
	public int compareTo(MicroCalendar other) {
		int diff = super.compareTo(other);
		return (diff == 0) ? microExtension - other.microExtension : diff;
	}
	
	public void add(int field, int amount) {
		if (field == MICROSECOND) {
			roll(MICROSECOND, amount % 1000);
			super.add(Calendar.MILLISECOND, amount / 1000);
		} else {
			super.add(field, amount);
		}
	}
	
	public void roll(int field, boolean up) {
		roll(field, up ? 1 : -1);
	}
	
	public void roll(int field, int amount) {
		if (field == MICROSECOND) {
			int milliAdjust = amount / 1000;
			int newMicro = microExtension + (amount % 1000);
			if (newMicro < 0) {
				microExtension = 1000 + newMicro;
				milliAdjust--;
			} else if (newMicro > 1000) {
				microExtension = newMicro - 1000;
				milliAdjust++;
			} else {
				microExtension = newMicro;
			}
			super.roll(Calendar.MILLISECOND, milliAdjust);
		}
		else {
			super.roll(field, amount);
		}
	}
	
	public void set(int field, int value) {
		if (field == MICROSECOND) {
			microExtension = value % 1000;
			super.set(Calendar.MILLISECOND, value / 1000);
		}
		else {
			super.set(field, value);
		}
	}
	
	public int getMinimum(int field) {
		if (field == MICROSECOND) {
			return super.getMinimum(Calendar.MILLISECOND) * 1000;
		} else {
			return super.getMinimum(field);
		}
	}
	
	public int getMaximum(int field) {
		if (field == MICROSECOND) {
			return super.getMaximum(Calendar.MILLISECOND) * 1000;
		} else {
			return super.getMaximum(field);
		}
	}
	
	public int getGreatestMinimum(int field) {
		if (field == MICROSECOND) {
			return super.getGreatestMinimum(Calendar.MILLISECOND) * 1000;
		} else {
			return super.getGreatestMinimum(field);
		}
	}
	
	public int getLeastMaximum(int field) {
		if (field == MICROSECOND) {
			return super.getLeastMaximum(Calendar.MILLISECOND) * 1000;
		} else {
			return super.getLeastMaximum(field);
		}
	}
	
	public int getActualMinimum(int field) {
		if (field == MICROSECOND) {
			return super.getActualMinimum(Calendar.MILLISECOND) * 1000;
		} else {
			return super.getActualMinimum(field);
		}
	}
	
	public int getActualMaximum(int field) {
		if (field == MICROSECOND) {
			return super.getActualMaximum(Calendar.MILLISECOND) * 1000;
		} else {
			return super.getActualMaximum(field);
		}
	}
	
	public int getMicroExtension() {
		return microExtension;
	}
	
	public void setMicroExtension(int extension) {
		microExtension = extension;
	}
	
	public void setTimeInMillis(long millis) {
		super.setTimeInMillis(millis);
		microExtension = 0;
	}
	
	public long getTimeInMicros() {
		return (super.getTimeInMillis() * 1000) + microExtension;
	}
	
	public void setTimeInMicros(long micros) {
		super.setTimeInMillis(micros / 1000);
		microExtension = (int)(micros % 1000);
	}
}
