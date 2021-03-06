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
package asl.seedscan.metrics;

import asl.metadata.*;
import asl.metadata.meta_new.*;
import asl.seedscan.database.MetricValueIdentifier;
import asl.seedsplitter.DataSet;
import asl.seedscan.event.*;

import freq.Cmplx;
import timeutils.Timeseries;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.ArrayList;
import java.util.logging.Logger;

import asl.seedsplitter.BlockLocator;
import asl.seedsplitter.ContiguousBlock;
import asl.seedsplitter.DataSet;
import asl.seedsplitter.SeedSplitter;
import asl.seedsplitter.Sequence;
import asl.seedsplitter.SequenceRangeException;
import sac.SacTimeSeries;

public abstract class Metric
{
    private static final Logger logger = Logger.getLogger("asl.seedscan.metrics.Metric");

    private Hashtable<String, String> arguments;
    private Hashtable<CrossPowerKey, CrossPower> crossPowerMap;

    private Boolean forceUpdate = false;
    private Boolean makePlots   = false;

    protected final double NO_RESULT = -999.999; 

    protected StationMeta  stationMeta  = null;
    protected MetricData   metricData   = null;
    protected MetricResult metricResult = null;

    private Hashtable<String, EventCMT> eventTable = null;
    //private Hashtable<String, SacTimeSeries> eventSynthetics = null;
    private Hashtable<String, Hashtable<String, SacTimeSeries>> eventSynthetics = null;

    public Metric()
    {
        arguments = new Hashtable<String, String>();
        crossPowerMap = new Hashtable<CrossPowerKey, CrossPower>();

        // MTH: 03-18-13: Added to allow these optional arguments to each cfg:metric in config.xml
        addArgument("makeplots");
        addArgument("forceupdate");
    }
    
    public MetricValueIdentifier createIdentifier(Channel channel)
    {
    	return new MetricValueIdentifier(	metricResult.getDate(), metricResult.getMetricName(),
    										metricResult.getStation(), channel);
    }
    
    public MetricValueIdentifier createIdentifier(Channel channelA, Channel channelB)
    {
    	return createIdentifier(MetricResult.createChannel(MetricResult.createResultId(channelA, channelB)));
    }

    public Hashtable<CrossPowerKey, CrossPower> getCrossPowerMap()
    {
        return crossPowerMap;
    }

    public void setCrossPowerMap(Hashtable<CrossPowerKey, CrossPower> crossPowerMap)
    {
        this.crossPowerMap = crossPowerMap;
    }

    protected CrossPower getCrossPower(Channel channelA, Channel channelB)
    {
        CrossPowerKey key = new CrossPowerKey(channelA, channelB);
        CrossPower crossPower;

        if (crossPowerMap.containsKey(key)) {
            crossPower = crossPowerMap.get(key);
        }
        else {
            double[] psd = null;
            double[] df  = new double[1];            // Dummy array to get params out of computePSD()
            for (int i=0; i<df.length; i++) df[i]=0;
            try {
                psd = computePSD(channelA, channelB, df);
            }
            catch (NullPointerException e) {
                System.out.println("== Metric.getCrossPower NullPointerException = " + e);
            }
            crossPower = new CrossPower(psd, df[0]);
            crossPowerMap.put(key, crossPower);
        }
        return crossPower;
    }

    //public Hashtable<String, Hashtable<String, SacTimeSeries>> getEventSynthetics()
    public Hashtable<String, SacTimeSeries> getEventSynthetics(String eventIdString)
    {
            if (eventSynthetics.containsKey( eventIdString ) ){
                return eventSynthetics.get( eventIdString );
            }
            else {
                System.out.format("== Metric.getEventSynthetics - Synthetics not found for eventIdString=[%s]\n", eventIdString);
                return null;
            }
    }

    public Hashtable<String, EventCMT> getEventTable()
    {
            return eventTable;
    }

    //public void setEventSynthetics(Hashtable<String, SacTimeSeries> eventSynthetics)
    public void setEventSynthetics(Hashtable<String, Hashtable<String, SacTimeSeries>> eventSynthetics)
    {
        this.eventSynthetics = eventSynthetics;
    }

    public void setEventTable(Hashtable<String, EventCMT> events)
    {
        eventTable = events;
    }

    public void setData(MetricData metricData)
    {
        this.metricData = metricData;
        stationMeta = metricData.getMetaData();
        metricResult = new MetricResult(stationMeta, getName());
    }

    public MetricResult getMetricResult()
    {
        return metricResult;
    }

    public String getDay()
    {
        return (EpochData.epochToDateString( stationMeta.getTimestamp() ) );
    }

    public abstract long getVersion();
    public abstract String getName();
    public abstract void process();

/**
 * MTH
 */
    private void setForceUpdate(){
        this.forceUpdate = true;
    }
    public Boolean getForceUpdate(){
        return forceUpdate;
    }
    private void setMakePlots(){
        this.makePlots = true;
    }
    public Boolean getMakePlots(){
        return makePlots;
    }


// Dynamic argumemnt managment
    protected final void addArgument(String name)
    {
        arguments.put(name, "");
    }

    public final void add(String name, String value)
    throws NoSuchFieldException
    {
        if (!arguments.containsKey(name)) {
            throw new NoSuchFieldException("Argument '" +name+ "' is not recognized.");
        }
        arguments.put(name, value);

        if (name.equals("forceupdate")) {
            if (value.toLowerCase().equals("true") || value.toLowerCase().equals("yes") ) {
                setForceUpdate();
            }
        }
        else if (name.equals("makeplots")) {
            if (value.toLowerCase().equals("true") || value.toLowerCase().equals("yes") ) {
                setMakePlots();
            }
        }
    }

    public final String get(String name)
    throws NoSuchFieldException
    {
        if (!arguments.containsKey(name)) {
            throw new NoSuchFieldException("Argument '" +name+ "' is not recognized.");
        }
        String metricResult = arguments.get(name);
        if ((metricResult == null) || (metricResult.equals(""))) {
            metricResult = null;
        }
        return metricResult;
    }

    public final Enumeration<String> names()
    {
        return arguments.keys();
    }


/**
 * computePSD - Done here so that it can be passed from metric to metric,
 *              rather than re-computing it for each metric that needs it
 *
 * Use Peterson's algorithm (24 hrs = 13 segments with 75% overlap, etc.)
 *
 * From Bendat & Piersol p.328:
 *  time segment averaging --> reduces the normalized standard error by sqrt (1 / nsegs)
 *                             and increases the resolution bandwidth to nsegs * df
 *  frequency smoothing --> has same effect with nsegs replaced by nfrequencies to smooth
 *  The combination of both will reduce error by sqrt(1 / nfreqs * nsegs)
 *
 * @param channelX - X-channel used for power-spectral-density computation
 * @param channelY - Y-channel used for power-spectral-density computation
 * @param params[] - Dummy array used to pass df (frequency spacing) back up 
 * 
 * @return psd[f] - Contains smoothed crosspower-spectral density
 *                  computed for nf = nfft/2 + 1 frequencies (+ve freqs + DC + Nyq)
 * @author Mike Hagerty
*/
    private final double[] computePSD(Channel channelX, Channel channelY, double[] params) {

        //System.out.format("== Metric.computePSD(channelX=%s, channelY=%s)\n", channelX, channelY);

        int ndata      = 0;
        double srate   = 0;  // srate = sample frequency, e.g., 20Hz

// This would give us 2 channels with the SAME number of (overlapping) points, but 
//   they might not represent a complete day (e.g., could be a single block of data in the middle of the day)
//      double[][] channelOverlap = metricData.getChannelOverlap(channelX, channelY);
//      double[]   chanXData = channelOverlap[0];
//      double[]   chanYData = channelOverlap[1];

// Instead, getPaddedDayData() gives us a complete (zero padded if necessary) array of data for 1 day:
        double[] chanXData = metricData.getPaddedDayData(channelX);
        double[] chanYData = metricData.getPaddedDayData(channelY);

        double srateX = metricData.getChannelData(channelX).get(0).getSampleRate();
        double srateY = metricData.getChannelData(channelY).get(0).getSampleRate();
        ChannelMeta chanMetaX = stationMeta.getChanMeta(channelX);
        ChannelMeta chanMetaY = stationMeta.getChanMeta(channelY);

        if (srateX != srateY) {
            String message = "computePSD() ERROR: srateX (=" + srateX + ") != srateY (=" + srateY + ")";
            throw new RuntimeException(message);
        }
        srate = srateX;
        ndata = chanXData.length; 

        //ndata = (ndataX < ndataY) ? ndataX : ndataY;

     // Compute PSD for this channel using the following algorithm:
     //   Break up the data (one day) into 13 overlapping segments of 75% 
     //   Remove the trend and mean 
     //   Apply a taper (cosine) 
     //   Zero pad to a power of 2 
     //   Compute FFT 
     //   Average all 13 FFTs 
     //   Remove response 

     // For 13 windows with 75% overlap, each window will contain ndata/4 points
     // ** Still need to handle the case of multiple datasets with gaps!

        int nseg_pnts = ndata / 4;  
        int noff      = nseg_pnts / 4;  

     // Find smallest power of 2 >= nseg_pnts:
        int nfft=1;
        while (nfft < nseg_pnts) nfft = (nfft << 1);

     // We are going to do an nfft point FFT which will return 
     //   nfft/2+1 +ve frequencies (including  DC + Nyq)
        int nf=nfft/2 + 1;

        if (srate == 0) throw new RuntimeException("Error: Got srate=0");
        double dt = 1./srate;
        double df = 1./(nfft*dt);

        params[0] = df;

        double[] xseg = new double[nseg_pnts];
        double[] yseg = new double[nseg_pnts];

        Cmplx[]  xfft = null;
        Cmplx[]  yfft = null;
        double[] psd  = new double[nf];
        Cmplx[]  psdC = new Cmplx[nf];
        double   wss  = 0.;

        int iwin=0;
        int ifst=0;
        int ilst=nseg_pnts-1;
        int offset = 0;

// Initialize the Cmplx array
       for(int k = 0; k < nf; k++){
            psdC[k] = new Cmplx(0., 0.);
        }

        while (ilst < ndata) // ndata needs to come from largest dataset
        {
           for(int k=0; k<nseg_pnts; k++) {     // Load current window
            xseg[k] = chanXData[k+offset]; 
            yseg[k] = chanYData[k+offset]; 
            //xseg[k]=(double)intArrayX[k+offset]; 
            //yseg[k]=(double)intArrayY[k+offset]; 
           }
           //Timeseries.timeout(xseg,"xseg");
           Timeseries.detrend(xseg);
           Timeseries.detrend(yseg);
           Timeseries.debias(xseg);
           Timeseries.debias(yseg);

           wss = Timeseries.costaper(xseg,.10);
           wss = Timeseries.costaper(yseg,.10);
// MTH: Maybe want to assert here that wss > 0 to avoid divide-by-zero below ??

        // fft2 returns just the (nf = nfft/2 + 1) positive frequencies
           xfft = Cmplx.fft2(xseg);
           yfft = Cmplx.fft2(yseg);

        // Load up the 1-sided PSD:
           for(int k = 0; k < nf; k++){
            // when X=Y, X*Y.conjg is Real and (X*Y.conjg).mag() simply returns the Real part as a double 
                psd[k] = psd[k] + Cmplx.mul(xfft[k], yfft[k].conjg()).mag() ;
                psdC[k]= Cmplx.add(psdC[k], Cmplx.mul(xfft[k], yfft[k].conjg()) );
           }

           iwin ++;
           offset += noff;
           ilst   += noff;
           ifst   += noff;
        } //end while
        int nwin = iwin;    // Should have nwin = 13

     // Divide the summed psd[]'s by the number of windows (=13) AND
     //   Normalize the PSD ala Bendat & Piersol, to units of (time series)^2 / Hz AND
     //   At same time, correct for loss of power in window due to 10% cosine taper

        double psdNormalization = 2.0 * dt / (double)nfft;
        double windowCorrection = wss / (double)nseg_pnts;  // =.875 for 10% cosine taper
        psdNormalization = psdNormalization / windowCorrection;
        psdNormalization = psdNormalization / (double)nwin; 

        double[] freq = new double[nf];

        for(int k = 0; k < nf; k++){
            psd[k]  = psd[k]*psdNormalization;
            psdC[k]  = Cmplx.mul(psdC[k], psdNormalization);
            freq[k] = (double)k * df;
        }

     // Get the instrument response for Acceleration and remove it from the PSD
        Cmplx[]  instrumentResponseX = chanMetaX.getResponse(freq, 3);
        Cmplx[]  instrumentResponseY = chanMetaY.getResponse(freq, 3);

        double[] responseMag        = new double[nf];
        Cmplx[] responseMagC        = new Cmplx[nf];

     // We're computing the squared magnitude as we did with the FFT above
     //   Start from k=1 to skip DC (k=0) where the response=0
        psd[0]=0; 
        for(int k = 1; k < nf; k++){

            responseMag[k]  = Cmplx.mul(instrumentResponseX[k], instrumentResponseY[k].conjg()).mag() ;
            responseMagC[k] = Cmplx.mul(instrumentResponseX[k], instrumentResponseY[k].conjg()) ;
            if (responseMag[k] == 0) {
                throw new RuntimeException("NLNMDeviation Error: responseMag[k]=0 --> divide by zero!");
            }
            else {   // Divide out (squared)instrument response & Convert to dB:
                psdC[k] = Cmplx.div(psdC[k], responseMagC[k]);
                psd[k] = psd[k]/responseMag[k];
            }
        }

     // We still have psd[f] so this is a good point to do any smoothing over neighboring frequencies:
        int nsmooth = 11;
        int nhalf   = 5;
        int nw = nf - nsmooth;
        double[] psdFsmooth = new double[nf];
        Cmplx[] psdCFsmooth = new Cmplx[nf];

        int iw=0;

        for (iw = 0; iw < nhalf; iw++) {
            psdFsmooth[iw] = psd[iw];
            psdCFsmooth[iw]= psdC[iw];
        }

        // iw is really icenter of nsmooth point window
        for (; iw < nf - nhalf; iw++) {
            int k1 = iw - nhalf;
            int k2 = iw + nhalf;

            double sum = 0;
            Cmplx sumC = new Cmplx(0., 0.);
            for (int k = k1; k < k2; k++) {
                sum  = sum + psd[k];
                sumC = Cmplx.add(sumC, psdC[k]);
            }
            psdFsmooth[iw] = sum / (double)nsmooth;
            psdCFsmooth[iw]= Cmplx.div(sumC, (double)nsmooth);
        }

     // Copy the remaining point into the smoothed array
        for (; iw < nf; iw++) {
            psdFsmooth[iw] = psd[iw];
            psdCFsmooth[iw]= psdC[iw];
        }

     // Copy Frequency smoothed spectrum back into psd[f] and proceed as before
        for ( int k = 0; k < nf; k++){
            //psd[k]  = psdFsmooth[k];
            psd[k]  = psdCFsmooth[k].mag();
            //psd[k]  = psdC[k].mag();
        }
        psd[0]=0; // Reset DC

        return psd;

    } // end computePSD


} // end class
