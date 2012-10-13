/*
 * Copyright 2012, United States Geological Survey or
 * third-party contributors as indicated by the @author tags.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/  >.
 *
 */
package seed;

import java.util.Calendar;
import java.util.GregorianCalendar;

import asl.util.MicroCalendar;

/*
 * Calibration Blockette Layout
 * 0	[UWORD]		Blockette Type
 * 2	[UWORD]		Next Blockette
 * -------- Above Managed by Parent Class (seed.Blockette) ----------
 * 4	[BTIME]		Calibration Start Time
 */

/**
 * @author Joel D. Edwards <jdedwards@usgs.gov>
 *
 */
public abstract class CalibrationBlockette extends Blockette {
	public static final int MINIMUM_SIZE = 14;

	/**
	 * 
	 */
	public CalibrationBlockette() {
		super(MINIMUM_SIZE);
	}

	/**
	 * @param bufferSize
	 */
	public CalibrationBlockette(int bufferSize) {
		super(bufferSize);
	}

	/**
	 * @param b
	 */
	public CalibrationBlockette(byte[] b) {
		super(b);
	}
	
	public MicroCalendar getCalibrationStartTime() {
		MicroCalendar timestamp = new MicroCalendar();
		bb.position(4);
		timestamp.set(MicroCalendar.YEAR, bb.getShort());
		timestamp.set(MicroCalendar.DAY_OF_YEAR, bb.getShort());
		timestamp.set(MicroCalendar.HOUR, bb.get());
		timestamp.set(MicroCalendar.MINUTE, bb.get());
		timestamp.set(MicroCalendar.SECOND, bb.get());
		bb.get();
		timestamp.set(MicroCalendar.MICROSECOND, bb.getShort() * 100);
		return timestamp;
	}
	
	public void setCalibrationStartTime(MicroCalendar timestamp) {
		bb.position(4);
		bb.putShort((short)timestamp.get(MicroCalendar.YEAR));
		bb.putShort((short)timestamp.get(MicroCalendar.DAY_OF_YEAR));
		bb.put((byte)timestamp.get(MicroCalendar.HOUR));
		bb.put((byte)timestamp.get(MicroCalendar.MINUTE));
		bb.put((byte)timestamp.get(MicroCalendar.SECOND));
		bb.put((byte)0);
		bb.putShort((short)(timestamp.get(MicroCalendar.MICROSECOND) / 100));
	}
}
