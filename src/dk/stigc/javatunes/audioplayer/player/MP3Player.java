package dk.stigc.javatunes.audioplayer.player;

import javax.sound.sampled.LineUnavailableException;

import dk.stigc.javatunes.audioplayer.other.*;
import javazoom.jl.decoder.*;

public class MP3Player extends BasePlayer
{	  
	private Bitstream bitstream;
	private Decoder	decoder; 
	private boolean vbr;

    private void initMP3Player() throws Exception
    {
		bitstream = new Bitstream(bin);      	        
	    decoder = new Decoder();
	        
		//http://urn1350.net/
		//L�ser bitrate og l�ngde
		Header h = bitstream.readFrame();
		if (h==null)
			throw new Exception("Missing mp3 header");
		 
		int playLength = (int)(h.total_ms((int)audioInfo.lengthInBytes)/1000);
		vbr = h.vbr();
		int bitrate = h.bitrate()/1000;

		//Hmm no length in header. Calculate from filesize and CBR header.
		if (playLength==0 && audioInfo.lengthInBytes>0 && bitrate>0)
			playLength = (int)audioInfo.lengthInBytes/((bitrate/8)*1000);
		
		audioInfo.kbps = bitrate;
		audioInfo.lengthInSeconds = playLength;
    }  
     
	public void decode() throws Exception
	{
		initMP3Player();
		//System.out.println("mp3 1 " + System.currentTimeMillis());
		boolean ret = true;
		
		while (ret)
		{
			ret = decodeFrame();
		}
	}

	
	/**
	 * Decodes a single frame.
	 * 
	 * @return true if there are no more frames to decode, false otherwise.
	 */
	boolean init = false;
	protected boolean decodeFrame() throws DecoderException, BitstreamException, LineUnavailableException
	{		
		if (!running) 
			return false;

		Header h = bitstream.readFrame();

		if (h==null) return false;
		int bitrateNow =  h.bitrate_instant();
		
		if (vbr)
			audioInfo.addVariableBitrate(bitrateNow);

		//Log.write("mp3.3");
		// sample buffer set when decoder constructed
		SampleBuffer output = (SampleBuffer)decoder.decodeFrame(h, bitstream);
		//Log.write("mp3.4");
		
		if (!init)
		{
			initAudioLine(decoder.getOutputChannels(), decoder.getOutputFrequency()
					, 16, true, false);	
			init = true;
		}
		
		int len = output.getBufferLength();
		byte[] b = toByteArray(output.getBuffer(), 0, len);
		write(b, len*2);	
																		
		bitstream.closeFrame();

		return true;
	}
	private byte[] byteBuf = new byte[4608];
	
	private byte[] getByteArray(int length)
	{
		if (byteBuf.length < length)
			byteBuf = new byte[length+1024];
		return byteBuf;
	}
		
	private byte[] toByteArray(short[] samples, int offs, int len)
	{
		byte[] b = getByteArray(len*2);
		int idx = 0;
		short s;
		while (len-- > 0)
		{
			s = samples[offs++];
			b[idx++] = (byte)s;
			b[idx++] = (byte)(s>>>8);
		}
		return b;
	}
}
