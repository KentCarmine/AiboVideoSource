package dlife.vision2;

import dlife.robot.aibo.Aibo;
import dlife.robot.gui.DeviceWindow;

import java.awt.image.*;
import java.awt.geom.*;
import java.awt.*;
import java.net.*;
import java.io.*;
import javax.imageio.*;
import javax.swing.JFrame;

/**
 * This class provides a means to connect to a Sony AIBO, start its Raw Camera Server and
 * provide frames from its camera in java BufferedImage format to requesting client code.
 * 
 * @author Kent Carmine
 * @author Dickinson College
 * @version April 16, 2010
 */
public class AiboVideoSource extends VideoSource{

	private Aibo aibo;
	private BufferedImage currentFrame;
	private VideoPlayerGUI myGUI;
	private int width;
	private int height;
	private boolean playing;
	private static final int RAW_CAM_PORT = 10011;
	private static boolean isCamServerStarted = false;

	/**
	 * Construct a new AiboVideoSource, associate it with the specified Aibo, 
	 * start that Aibo's Raw Camera server, and start obtaining frames from it.
	 * The default frame size is set to width=208, height=160 in pixels.
	 * 
	 * @param newAibo the Aibo to obtain frames from
	 */
	public AiboVideoSource(Aibo newAibo)
	{
		super();
		currentFrame = null;
		width = 208; 
		height = 160;
		playing = false;
		aibo = newAibo;

		if(!isCamServerStarted)
		{
			aibo.sendMessage("!root \"TekkotsuMon\" \"Raw Cam Server\"");
			isCamServerStarted = true;
		}

		Thread thread = new Thread(new ImageSetterRunnable());

		thread.start();
	}

	/**
	 * Construct a new AiboVideoSource, associate it with the specified Aibo, 
	 * start that Aibo's Raw Camera server, and start obtaining frames from it with
	 * the given width and height.
	 * 
	 * @param newAibo the Aibo to obtain frames from
	 * @param frameWidth the width of the frames that will be provided by this AiboVideoSource
	 * @param frameHeight the height of the frames that will be provided by this AiboVideoSource
	 */
	public AiboVideoSource(Aibo newAibo, int frameWidth, int frameHeight)
	{
		super();
		currentFrame = null;
		width = frameWidth; 
		height = frameHeight;
		playing = false;
		aibo = newAibo;

		if(!isCamServerStarted)
		{
			aibo.sendMessage("!root \"TekkotsuMon\" \"Raw Cam Server\"");
			isCamServerStarted = true;
		}

		Thread thread = new Thread(new ImageSetterRunnable());

		thread.start();
	}

	/**
	 * Get the GUI for this AiboVideoSource. The GUI will be a VideoPlayerGUI that has
	 * buttons for play/pause and step forward.
	 * @return the VideoPlayerGUI that is associated with this AiboVideoSource
	 */
	public VideoPlayerGUI getGUI()
	{
		if (myGUI == null) {
			byte buttonMask = (byte)(VideoPlayerGUI.PLAY_BUTTON | VideoPlayerGUI.STEP_FORWARD_BUTTON);
			myGUI = new VideoPlayerGUI(this, buttonMask);
		}

		return myGUI;  
	}

	/**
	 * Provides the current frame in BufferedImage format.
	 * 
	 * @return the current frame
	 */
	public BufferedImage getVideoFrame()
	{
		return currentFrame;
	}

	/**
	 * Set the frame provided as the current frame.
	 * 
	 * @param im the frame to be set as the current frame
	 */
	private void setCurrentFrame(BufferedImage im)
	{
		synchronized(this)
		{
			currentFrame = im;
		}
		if(playing)
		{
			setChanged();
			notifyObservers(VisionEventDescriptors.VIDEO_SOURCE_STATE_CHANGED);

			setChanged();
			notifyObservers(VisionEventDescriptors.NEW_IMAGE_AVAILABLE);
		}
	}
	/**
	 * Returns dummy value -1, since this is a real-time VideoSource that does not store 
	 * previous frames, it cannot seek.
	 * @return -1, indicating that this AiboVideoSource cannot seek.
	 */
	public int getSeekLocation()
	{
		return -1;
	}

	/**
	 * Does nothing, since this is a real-time VideoSource that does not store previous 
	 * frames, it cannot seek.
	 * @param location the location to seek to
	 */
	public void seek(int location)
	{
		//DO NOTHING
	}

	/**
	 * Does nothing, since this is a real-time VideoSource that does not store previous 
	 * frames, it cannot rewind.
	 */
	public void rewind()
	{
		//DO NOTHING
	}

	/**
	 * Returns false, indicating that this VideoSource cannot rewind.
	 * @return false
	 */
	public boolean canRewind()
	{
		return false;
	}

	/**
	 * Does not actually step forward, but notifies all Observers that they should re-obtain the current frame.
	 */
	public void stepForward()
	{
		setChanged();
		notifyObservers(VisionEventDescriptors.NEW_IMAGE_AVAILABLE);
	}

	/**
	 * Returns a boolean value indicating if this VideoSource can currently step forward.
	 * True if this AiboVideoSource is currently playing, false otherwise.
	 * @return a boolean value indicating if this VideoSource can currently step forward
	 */
	public boolean canStepForward()
	{
		return !playing;
	}

	/**
	 * Does nothing, since this is a real-time VideoSource that does not store previous 
	 * frames, it cannot step backward.
	 */
	public void stepBackward()
	{
		//DO NOTHING
	}

	/**
	 * Returns false, indicating that this VideoSource cannot step backward.
	 * @return false
	 */
	public boolean canStepBackward()
	{
		return false;
	}

	/**
	 * If this AiboVideoSource can play, it begins playing. 
	 */
	public void play()
	{
		if(canPlay())
		{
			playing = true;

			setChanged();
			notifyObservers(VisionEventDescriptors.VIDEO_SOURCE_STATE_CHANGED);
		}
	}

	/**
	 * If this AiboVideoSource can be paused, it pauses. It can be paused if it is currently playing.
	 * While frames will still be obtained while this AiboVideoSource is paused, Observers will not be
	 * notified, and those frames will not be stored. Thus, any frames obtained from the Aibo while this
	 * AiboVideoSource is paused will be discarded.
	 */
	public void pause()
	{
		if(canPause())
		{
			playing = false;

			setChanged();
			notifyObservers(VisionEventDescriptors.VIDEO_SOURCE_STATE_CHANGED);
		}
	}

	/**
	 * Indicates whether this AiboVideoSource can currently play. It can play if it is
	 * currently paused.
	 * @return a boolean value, indicating if this AiboVideoSource can currently play.
	 */
	public boolean canPlay()
	{
		return !playing;
	}

	/**
	 * Indicates whether this AiboVideoSource can currently pause. It can pause if it is
	 * currently playing.
	 * @return a boolean value, indicating if this AiboVideoSource can currently be paused.
	 */
	public boolean canPause()
	{
		return playing;
	}

	/**
	 * Returns false, indicating that since this is a real-time VideoSource that does not store 
	 * previous frames, it cannot loop.
	 * @return false
	 */
	public boolean canLoop()
	{
		return false;
	}

	/**
	 * Returns false, indicating that this is a real-time VideoSource that does not store 
	 * previous frames, it cannot seek.
	 * @return false
	 */
	public boolean canSeek()
	{
		return false;
	}

	/**
	 * Returns the width of the current frame in pixels
	 * @return the width of the current frame as an int
	 */
	public int getWidth()
	{
		return width;
	}

	/**
	 * Returns the height of the current frame in pixels
	 * @return the height of the current frame as an int
	 */
	public int getHeight()
	{
		return height;
	}

	/**
	 * An inner class that provides an implementation of the Runnable interface that establishes
	 * a connection to the Aibo's Raw Camera server, obtains frames from that server, converts
	 * those frames to BufferedImage format, and sets them as the current frame in AiboVideoSource.
	 *
	 * @author Kent Carmine
	 * @author Dickinson College
	 * @version April 16, 2010
	 */
	private class ImageSetterRunnable implements Runnable
	{

		private DatagramSocket socket;
		private DatagramPacket packet;

		/**
		 * Constructs a new ImageSetterRunnable and establishes a connection to the 
		 * Aibo's Raw Camera server
		 */
		public ImageSetterRunnable()
		{
			try
			{
				socket = new DatagramSocket();
				socket.connect(InetAddress.getByName(aibo.getIPAddress()), RAW_CAM_PORT);

				socket.setSoTimeout(500);
			}
			catch(SocketException se)
			{
				System.out.println("SocketException while trying to connect to the Aibo's Raw Camera Server!");
				se.printStackTrace();
			}
			catch(UnknownHostException uhe)
			{
				System.out.println("UnknownHostException while trying to connect to the Aibo's Raw Camera Server!");
				uhe.printStackTrace();
			}

			byte[] dummyStartBuf = (new String("connection request")).getBytes();

			byte[] incomingbuf = new byte[1<<16];
			DatagramPacket incoming = new DatagramPacket(incomingbuf, incomingbuf.length);

			packet = new DatagramPacket(dummyStartBuf, dummyStartBuf.length);

			try
			{
				while(true) {
					try {

						socket.send(packet);
						socket.receive(incoming);
						break;
					} catch (SocketTimeoutException ex) { }
					catch (SocketException ex) { 
						try
						{
							Thread.sleep(500);
						}
						catch(InterruptedException ie)
						{
							ie.printStackTrace();
						}
					}
				}
			}
			catch(IOException ioe)
			{
				System.out.println("IOException while sending and receiving connection setup packets!");
				ioe.printStackTrace();
			}


			try
			{
				socket.setSoTimeout(0);
			}
			catch(SocketException se)
			{
				System.out.println("SocketException while setting DatagramSocket to blocking mode!");
				se.printStackTrace();
			}
		}

		/**
		 * Constantly obtains frames from the Aibo's Raw Camera server (in JPEG format), 
		 * converts them to BufferedImage format, and sets them as the current frame 
		 * in AiboVideoSource.
		 */
		public void run()
		{

			byte[] incomingbuf = new byte[1<<16];
			DatagramPacket incoming = new DatagramPacket(incomingbuf, incomingbuf.length);

			while(true)
			{
				try
				{
					socket.receive(incoming);
				}
				catch(IOException ioe)
				{
					System.out.println("IOException while receiving incoming frames!");
					ioe.printStackTrace();
				}

				byte[] data = incoming.getData();



				//Create 89 byte offset constant
				byte[] data2 = new byte[data.length-89];	

				int j = 0;
				for(int i = 89; i < data.length; i++)
				{
					data2[j] = data[i];
					j++;
				}

				InputStream in2 = new ByteArrayInputStream(data2);

				BufferedImage image = null;				

				try
				{
					image = ImageIO.read(in2);
				}
				catch(IOException ioe)
				{
					System.out.println("IOException while converting JPEG to BufferedImage!");
					ioe.printStackTrace();
				}

				/*
				 * NOTE: Tekkotstu DOES scale the image before displaying it. Check paint(Graphics g) method of
				 * VisionPanel class.
				 */

				BufferedImage scaledImage = scaleImage(image);
				setCurrentFrame(scaledImage);
			}
		}

		/**
		 * A helper method called in run() that scales the BufferedImage to the desired size.
		 * @param im the image to be scaled
		 * @return the scaled image
		 */
		private BufferedImage scaleImage(BufferedImage im)
		{
			/* Credit to:
			 * http://helpdesk.objects.com.au/java/how-do-i-scale-a-bufferedimage
			 */
			BufferedImage scaledImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			Graphics2D graphics2D = scaledImage.createGraphics();

			double xScaleFactor = ((double)width / (double)im.getWidth());
			double yScaleFactor = ((double)height / (double)im.getHeight());

			AffineTransform xform = AffineTransform.getScaleInstance(xScaleFactor, yScaleFactor);
			graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			graphics2D.drawImage(im, xform, null);
			return scaledImage;
		}
	}

	/**
	 * Main method for testing.
	 * 
	 * @param args
	 * @throws UnknownHostException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws UnknownHostException, IOException, InterruptedException
	{

		AiboVideoSource avs = null;

		Aibo rob = null;
		try {
			rob = new Aibo("10.10.10.3", 10001);
		}
		catch (UnknownHostException e) {
			e.printStackTrace();
		}

		rob.startUp();

		avs = new AiboVideoSource(rob);

		VideoDisplay vd = new VideoDisplay(avs.getWidth(), avs.getHeight());
		FilterManager fm = new FilterManager(vd);

		Camera cam = new Camera(avs, fm, vd);

		DeviceWindow camWindow = new DeviceWindow("Camera GUI", cam.getGUI());
		camWindow.setVisible(true);

		DeviceWindow filterWindow = new DeviceWindow("Filters GUI", fm.getGUI());
		filterWindow.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		filterWindow.setVisible(true);

		DeviceWindow videoWindow = new DeviceWindow("Video GUI", vd.getGUI());
		videoWindow.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		videoWindow.setVisible(true);

		while (camWindow.isVisible())
			;

		//Closes the AIBO's vision server. This needs to be put in any client code after 
		//it is done using AiboVideoSource as well.
		rob.sendMessage("!root \"TekkotsuMon\" \"Raw Cam Server\"");

		rob.closeConnection();

		System.exit(0);
	}
}
