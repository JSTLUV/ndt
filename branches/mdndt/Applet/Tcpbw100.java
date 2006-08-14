/*
Copyright � 2003 University of Chicago.  All rights reserved.
The Web100 Network Diagnostic Tool (NDT) is distributed subject to
the following license conditions:
SOFTWARE LICENSE AGREEMENT
Software: Web100 Network Diagnostic Tool (NDT)

1. The "Software", below, refers to the Web100 Network Diagnostic Tool (NDT)
(in either source code, or binary form and accompanying documentation). Each
licensee is addressed as "you" or "Licensee."

2. The copyright holder shown above hereby grants Licensee a royalty-free
nonexclusive license, subject to the limitations stated herein and U.S. Government
license rights.

3. You may modify and make a copy or copies of the Software for use within your
organization, if you meet the following conditions: 
    a. Copies in source code must include the copyright notice and this Software
    License Agreement.
    b. Copies in binary form must include the copyright notice and this Software
    License Agreement in the documentation and/or other materials provided with the copy.

4. You may make a copy, or modify a copy or copies of the Software or any
portion of it, thus forming a work based on the Software, and distribute copies
outside your organization, if you meet all of the following conditions: 
    a. Copies in source code must include the copyright notice and this
    Software License Agreement;
    b. Copies in binary form must include the copyright notice and this
    Software License Agreement in the documentation and/or other materials
    provided with the copy;
    c. Modified copies and works based on the Software must carry prominent
    notices stating that you changed specified portions of the Software.

5. Portions of the Software resulted from work developed under a U.S. Government
contract and are subject to the following license: the Government is granted
for itself and others acting on its behalf a paid-up, nonexclusive, irrevocable
worldwide license in this computer software to reproduce, prepare derivative
works, and perform publicly and display publicly.

6. WARRANTY DISCLAIMER. THE SOFTWARE IS SUPPLIED "AS IS" WITHOUT WARRANTY
OF ANY KIND. THE COPYRIGHT HOLDER, THE UNITED STATES, THE UNITED STATES
DEPARTMENT OF ENERGY, AND THEIR EMPLOYEES: (1) DISCLAIM ANY WARRANTIES,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO ANY IMPLIED WARRANTIES
OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, TITLE OR NON-INFRINGEMENT,
(2) DO NOT ASSUME ANY LEGAL LIABILITY OR RESPONSIBILITY FOR THE ACCURACY,
COMPLETENESS, OR USEFULNESS OF THE SOFTWARE, (3) DO NOT REPRESENT THAT USE
OF THE SOFTWARE WOULD NOT INFRINGE PRIVATELY OWNED RIGHTS, (4) DO NOT WARRANT
THAT THE SOFTWARE WILL FUNCTION UNINTERRUPTED, THAT IT IS ERROR-FREE OR THAT
ANY ERRORS WILL BE CORRECTED.

7. LIMITATION OF LIABILITY. IN NO EVENT WILL THE COPYRIGHT HOLDER, THE
UNITED STATES, THE UNITED STATES DEPARTMENT OF ENERGY, OR THEIR EMPLOYEES:
BE LIABLE FOR ANY INDIRECT, INCIDENTAL, CONSEQUENTIAL, SPECIAL OR PUNITIVE
DAMAGES OF ANY KIND OR NATURE, INCLUDING BUT NOT LIMITED TO LOSS OF PROFITS
OR LOSS OF DATA, FOR ANY REASON WHATSOEVER, WHETHER SUCH LIABILITY IS ASSERTED
ON THE BASIS OF CONTRACT, TORT (INCLUDING NEGLIGENCE OR STRICT LIABILITY), OR
OTHERWISE, EVEN IF ANY OF SAID PARTIES HAS BEEN WARNED OF THE POSSIBILITY OF
SUCH LOSS OR DAMAGES.
The Software was developed at least in part by the University of Chicago,
as Operator of Argonne National Laboratory (http://miranda.ctd.anl.gov:7123/). 
 */
import java.io.*;
import java.net.*;
import java.net.Socket;
import java.awt.*;
import java.awt.event.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.datatransfer.*;
import java.applet.*;
import java.util.*;
import java.text.*;
import java.lang.*;
import javax.swing.JLabel;
import javax.swing.JApplet;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.BorderFactory;
import javax.swing.JTextPane;
import javax.swing.text.StyledDocument;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.BadLocationException;
import javax.swing.JOptionPane;

public class Tcpbw100 extends JApplet implements ActionListener
{
  private static final String VERSION = "5.4.7";
  private static final byte TEST_MID = (1 << 0);
  private static final byte TEST_C2S = (1 << 1);
  private static final byte TEST_S2C = (1 << 2);
  private static final byte TEST_SFW = (1 << 3);

  /* we really should do some clean-up in this java code... maybe later ;) */
  private static final byte COMM_FAILURE  = 0;
  private static final byte SRV_QUEUE     = 1;
  private static final byte MSG_LOGIN     = 2;
  private static final byte TEST_PREPARE  = 3;
  private static final byte TEST_START    = 4;
  private static final byte TEST_MSG      = 5;
  private static final byte TEST_FINALIZE = 6;
  private static final byte MSG_ERROR     = 7;
  private static final byte MSG_RESULTS   = 8;
  private static final byte MSG_LOGOUT    = 9;

  private static final int SFW_NOTTESTED  = 0;
  private static final int SFW_NOFIREWALL = 1;
  private static final int SFW_UNKNOWN    = 2;
  private static final int SFW_POSSIBLE   = 3;

  private static final double VIEW_DIFF = 0.1;
  
	JTextArea diagnosis, statistics;
  MyTextPane results;
	String inresult, outresult, errmsg;
	JButton startTest;
	JButton disMiss, disMiss2;
	JButton copy, copy2;
	JButton deTails;
	JButton sTatistics;
	JButton mailTo;
  JButton options;
  JCheckBox defaultTest;
	boolean Randomize, failed, cancopy;
	URL location;
	clsFrame f, ff, optionsFrame;
	String s;
	double t;
	int ECNEnabled, NagleEnabled, MSSSent, MSSRcvd;
	int SACKEnabled, TimestampsEnabled, WinScaleRcvd;
	int FastRetran, AckPktsOut, SmoothedRTT, CurrentCwnd, MaxCwnd;
	int SndLimTimeRwin, SndLimTimeCwnd, SndLimTimeSender;
	int SndLimTransRwin, SndLimTransCwnd, SndLimTransSender, MaxSsthresh;
	int SumRTT, CountRTT, CurrentMSS, Timeouts, PktsRetrans;
	int SACKsRcvd, DupAcksIn, MaxRwinRcvd, MaxRwinSent;
	int DataPktsOut, Rcvbuf, Sndbuf, AckPktsIn, DataBytesOut;
	int PktsOut, CongestionSignals, RcvWinScale;
	int pkts, lth=8192, CurrentRTO;
	int c2sData, c2sAck, s2cData, s2cAck;
	// added for mailto url
	protected URL targetURL;
	private String TARGET1 = "U";
	private String TARGET2 = "H";
	String emailText;
	double s2cspd, c2sspd, sc2sspd, ss2cspd;
  int ssndqueue;
  double sbytes;

	int half_duplex, congestion, bad_cable, mismatch;
	double mylink;
	double loss, estimate, avgrtt, spd, waitsec, timesec, rttsec;
	double order, rwintime, sendtime, cwndtime, rwin, swin, cwin;
	double aspd;

  boolean isApplication = false;
  boolean testInProgress = false;
  String host = null;
  String tmpstr, tmpstr2;
  byte tests = TEST_MID | TEST_C2S | TEST_S2C | TEST_SFW;
  int c2sResult = SFW_NOTTESTED;
  int s2cResult = SFW_NOTTESTED;

  public void showStatus(String msg)
  {
    if (!isApplication) {
      super.showStatus(msg);
    }
  }

	public void init() {
		setLayout(new BorderLayout());
		showStatus("Tcpbw100 ready");
		failed = false ;
		Randomize = false;
		cancopy = false;
    results = new MyTextPane();
    results.append("TCP/Web100 Network Diagnostic Tool v" + VERSION + "\n");
		results.setEditable(false);
		add(new JScrollPane(results));
		results.append("click START to begin\n");
		Panel mPanel = new Panel();
		startTest = new JButton("START");
		startTest.addActionListener(this);
		mPanel.add(startTest);
		sTatistics = new JButton("Statistics");
		sTatistics.addActionListener(this);
		mPanel.add(sTatistics);
		sTatistics.setEnabled(false);
		deTails = new JButton("More Details...");
		deTails.addActionListener(this);
		mPanel.add(deTails);
		deTails.setEnabled(false);
		mailTo = new JButton("Report Problem");
		mailTo.addActionListener(this);
		mPanel.add(mailTo);
		mailTo.setEnabled(false);
    options = new JButton("Options");
    options.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        options.setEnabled(false);
        showOptions();
        options.setEnabled(true);
      }
      
    });
    mPanel.add(options);
		add(BorderLayout.SOUTH, mPanel);
	}

  class MyTextPane extends JTextPane
  {
    public void append(String text)
    {
      try {
        getStyledDocument().insertString(getStyledDocument().getLength(), text, null);
      }
      catch (BadLocationException e) {
        System.out.println("WARNING: failed to append text to the text pane! [" + text + "]");
      }
    }

    public void insertComponent(Component c)
    {
      setSelectionStart(results.getStyledDocument().getLength());
      setSelectionEnd(results.getStyledDocument().getLength());
      super.insertComponent(c);
    }
  }

	class TestWorker implements Runnable
  {
    public void run()
    {
      if (!testInProgress) {
        testInProgress = true;
        diagnose();
        statistics();
        startTest.setEnabled(false);
        deTails.setEnabled(false);
        sTatistics.setEnabled(false);
        mailTo.setEnabled(false);
        options.setEnabled(false);

        try {
          dottcp();
        } catch(IOException e) {
          e.printStackTrace();
          failed=true;
          errmsg = "Server busy: Please wait 30 seconds for previous test to finish\n";
        }

        if (failed) {
          results.append(errmsg);
        }

        deTails.setEnabled(true);
        sTatistics.setEnabled(true);
        mailTo.setEnabled(true);
        options.setEnabled(true);
        showStatus("Tcpbw100 done");
        results.append("\nclick START to re-test\n");
        startTest.setEnabled(true);
        testInProgress = false;
      }
    }
  }

	synchronized public void runtest() {
    new Thread(new TestWorker()).start();
	}

  class Message {
    byte type;
    byte[] body;
  }
  
  class Protocol {
    private InputStream _ctlin;
    private OutputStream _ctlout;

    public Protocol(Socket ctlSocket) throws IOException
    {
      _ctlin = ctlSocket.getInputStream();
      _ctlout = ctlSocket.getOutputStream();
    }
    
    public void send_msg(byte type, byte toSend) throws IOException
    {
      byte[] tab = new byte[] { toSend };
      send_msg(type, tab);
    }

    public void send_msg(byte type, byte[] tab) throws IOException
    {
      byte[] header = new byte[3];
      header[0] = type;
      header[1] = (byte) (tab.length >> 8);
      header[2] = (byte) tab.length;
      
      _ctlout.write(header);
      _ctlout.write(tab);
    }

    public int readn(Message msg, int amount) throws IOException
    {
      int read = 0; 
      int tmp;
      msg.body = new byte[amount];
      while (read != amount) {
        tmp = _ctlin.read(msg.body, read, amount - read);
        if (tmp <= 0) {
          return read;
        }
        read += tmp;
      }
      return read;
    }
    
    public int recv_msg(Message msg) throws IOException
    {
      int length;
      if (readn(msg, 3) != 3) {
        return 1;
      }
      msg.type = msg.body[0];
      length = ((int) msg.body[1] & 0xFF) << 8;
      length += (int) msg.body[2] & 0xFF; 
      if (readn(msg, length) != length) {
        return 3;
      }
      return 0;
    }

    public void close()
    {
      try {
        _ctlin.close();
        _ctlout.close();
      }
      catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public boolean test_mid(Protocol ctl) throws IOException
  {
		byte buff[] = new byte[8192];
    Message msg = new Message();
    if ((tests & TEST_MID) == TEST_MID) {
      /* now look for middleboxes (firewalls, NATs, and other boxes that
       * muck with TCP's end-to-end priciples
       */
      showStatus("Tcpbw100 Middlebox test...");
      results.append("Checking for Middleboxes . . . . . . . . . . . . . . . . . .  ");
      statistics.append("Checking for Middleboxes . . . . . . . . . . . . . . . . . .  ");
      emailText = "Checking for Middleboxes . . . . . . . . . . . . . . . . . .  ";

      if (ctl.recv_msg(msg) != 0) {
        errmsg = "Protocol error!\n";
        failed = true;
        return true;
      }
      if (msg.type != TEST_PREPARE) {
        errmsg = "Middlebox test: Received wrong type of the message\n";
        failed = true;
        return true;
      }
      int midport = Integer.parseInt(new String(msg.body));

      Socket in2Socket = null;
      try {
        in2Socket = new Socket(host, midport);
      } catch (UnknownHostException e) {
        System.err.println("Don't know about host: " + host);
        errmsg = "unknown server\n" ;
        failed = true;
        return true;
      } catch (IOException e) {
        System.err.println("Couldn't perform middlebox testing to: " + host);
        errmsg = "Server Failed while middlebox testing\n" ;
        failed = true;
        return true;
      }

      InputStream srvin2 = in2Socket.getInputStream();
      OutputStream srvout2 = in2Socket.getOutputStream();

      int largewin = 128*1024;

      in2Socket.setSoTimeout(6500);
      int bytes = 0;
      int inlth;
      t = System.currentTimeMillis();

      try {  
        while ((inlth=srvin2.read(buff,0,buff.length)) > 0) {
          bytes += inlth;
          if ((System.currentTimeMillis() - t) > 5500)
            break;
        }
      } 
      catch (IOException e) {}

      t =  System.currentTimeMillis() - t;
      System.out.println(bytes + " bytes " + (8.0 * bytes)/t + " kb/s " + t/1000 + " secs");
      s2cspd = ((8.0 * bytes) / 1000) / t;

      if (ctl.recv_msg(msg) != 0) {
        errmsg = "Protocol error!\n";
        failed = true;
        return true;
      }
      if (msg.type != TEST_MSG) {
        errmsg = "Middlebox test results: Received wrong type of the message\n";
        failed = true;
        return true;
      }
      tmpstr2 = new String(msg.body);

      String tmpstr4 = Double.toString(s2cspd*1000);
      System.out.println("Sending '" + tmpstr4 + "' back to server");
      ctl.send_msg(TEST_MSG, tmpstr4.getBytes());

      try {
        tmpstr2 += in2Socket.getInetAddress() + ";";
      } catch (SecurityException e) {
        System.err.println("Unable to obtain Servers IP addresses: using " + host);
        errmsg = "getInetAddress() called failed\n" ;
        tmpstr2 += host + ";";
        results.append("Unable to obtain remote IP address\n");
      }

      System.err.println("calling in2Socket.getLocalAddress()");
      try {
        tmpstr2 += in2Socket.getLocalAddress() + ";";
      } catch (SecurityException e) {
        System.err.println("Unable to obtain local IP address: using 127.0.0.1");
        errmsg = "getLocalAddress() call failed\n" ;
        tmpstr2 += "127.0.0.1;";
        // results.append("Unable to obtain local IP address: Using 127.0.0.1 instead\n");
      }

      srvin2.close();
      srvout2.close();
      in2Socket.close();

      if (ctl.recv_msg(msg) != 0) {
        errmsg = "Protocol error!\n";
        failed = true;
        return true;
      }
      if (msg.type != TEST_FINALIZE) {
        errmsg = "Middlebox test: Received wrong type of the message\n";
        failed = true;
        return true;
      }
      results.append("Done\n");
      statistics.append("Done\n");
      emailText += "Done\n%0A";
    }
    return false;
  }

  public boolean test_sfw(Protocol ctl) throws IOException
  {
    Message msg = new Message();
    if ((tests & TEST_SFW) == TEST_SFW) {
      showStatus("Simple firewall test...");
      results.append("checking for firewalls . . . . . . . . . . . . . . . . . . .  ");
      statistics.append("checking for firewalls . . . . . . . . . . . . . . . . . . .  ");
      emailText = "checking for firewalls . . . . . . . . . . . . . . . . . . .  ";
      
      if (ctl.recv_msg(msg) != 0) {
        errmsg = "Protocol error!\n";
        failed = true;
        return true;
      }
      if (msg.type != TEST_PREPARE) {
        errmsg = "Simple firewall test: Received wrong type of the message\n";
        failed = true;
        return true;
      }

      String message = new String(msg.body);

      int srvPort, testTime;
      try {
        int k = message.indexOf(" ");
        srvPort = Integer.parseInt(message.substring(0,k));
        testTime = Integer.parseInt(message.substring(k+1));
      }
      catch (Exception e) {
        errmsg = "Simple firewall test: Received improper message\n";
        failed = true;
        return true;
      }

      System.out.println("SFW: port=" + srvPort);
      System.out.println("SFW: testTime=" + testTime);

      ServerSocket srvSocket;
      try {
        srvSocket = new ServerSocket(0);
      }
      catch (Exception e) {
        e.printStackTrace();
        errmsg = "Simple firewall test: Cannot create listen socket\n";
        failed = true;
        return true;
      }

      System.out.println("SFW: oport=" + srvSocket.getLocalPort());
      ctl.send_msg(TEST_MSG, Integer.toString(srvSocket.getLocalPort()).getBytes());

      if (ctl.recv_msg(msg) != 0) {
        errmsg = "Protocol error!\n";
        failed = true;
        return true;
      }
      if (msg.type != TEST_START) {
        errmsg = "Simple firewall test: Received wrong type of the message\n";
        failed = true;
        return true;
      }     

      OsfwWorker osfwTest = new OsfwWorker(srvSocket, testTime);
      new Thread(osfwTest).start();

      Socket sfwSocket = new Socket();
      try {
        sfwSocket.connect(new InetSocketAddress(host, srvPort), testTime * 1000);

        Protocol sfwCtl = new Protocol(sfwSocket);
        sfwCtl.send_msg(TEST_MSG, new String("Simple firewall test").getBytes());
      }
      catch (Exception e) {
        e.printStackTrace();
      }

      if (ctl.recv_msg(msg) != 0) {
        errmsg = "Protocol error!\n";
        failed = true;
        return true;
      }
      if (msg.type != TEST_MSG) {
        errmsg = "Simple firewall test: Received wrong type of the message\n";
        failed = true;
        return true;
      }
      c2sResult = Integer.parseInt(new String(msg.body));

      osfwTest.finalize();

      if (ctl.recv_msg(msg) != 0) {
        errmsg = "Protocol error!\n";
        failed = true;
        return true;
      }
      if (msg.type != TEST_FINALIZE) {
        errmsg = "Simple firewall test: Received wrong type of the message\n";
        failed = true;
        return true;
      }
      results.append("Done\n");
      statistics.append("Done\n");
      emailText += "Done\n%0A";
    }
    return false;
  }

  public boolean test_c2s(Protocol ctl) throws IOException
  {
		byte buff2[] = new byte[8192];
    Message msg = new Message();
    if ((tests & TEST_C2S) == TEST_C2S) {
      showStatus("Tcpbw100 outbound test...");
      results.append("running 10s outbound test (client-to-server [C2S]) . . . . . ");
      statistics.append("running 10s outbound test (client-to-server [C2S]) . . . . . ");
      emailText += "running 10s outbound test (client-to-server [C2S]) . . . . . ";
      
      if (ctl.recv_msg(msg) != 0) {
        errmsg = "Protocol error!\n";
        failed = true;
        return true;
      }
      if (msg.type != TEST_PREPARE) {
        errmsg = "C2S throughput test: Received wrong type of the message\n";
        failed = true;
        return true;
      }
      int c2sport = Integer.parseInt(new String(msg.body));

      Socket outSocket = null;
      try {
        outSocket = new Socket(host, c2sport);
      } catch (UnknownHostException e) {
        System.err.println("Don't know about host: " + host);
        errmsg = "unknown server\n" ;
        failed = true;
        return true;
      } catch (IOException e) {
        System.err.println("Couldn't get 2nd connection to: " + host);
        errmsg = "Server Busy: Please wait 15 seconds for previous test to finish\n" ;
        failed = true;
        return true;
      }

      OutputStream out = outSocket.getOutputStream();

      // wait here for signal from server application 
      // This signal tells the client to start pumping out data
      if (ctl.recv_msg(msg) != 0) {
        errmsg = "Protocol error!\n";
        failed = true;
        return true;
      }
      if (msg.type != TEST_START) {
        errmsg = "C2S throughput test: Received wrong type of the message\n";
        failed = true;
        return true;
      }

      Random rng = new Random();
      byte c = '0';
      int i;
      for (i=0; i<lth; i++) {
        if (c == 'z')
          c = '0';
        buff2[i] = c++;
      }
      System.err.println("Send buffer size =" + i);
      outSocket.setSoTimeout(15000); 
      pkts = 0;
      t = System.currentTimeMillis();
      double stop_time = t + 10000; // ten seconds
      do {
        // if (Randomize) rng.nextBytes(buff2);
        try {
          out.write(buff2, 0, buff2.length);
        }
        catch (SocketException e) {
          System.out.println(e);
          break;
        }
        pkts++;
      } while (System.currentTimeMillis() < stop_time);

      t =  System.currentTimeMillis() - t;
      if (t == 0) {
        t = 1;
      }
      out.close();
      outSocket.close();
      System.out.println((8.0 * pkts * lth) / t + " kb/s outbound");
      c2sspd = ((8.0 * pkts * lth) / 1000) / t;
      /* receive the c2sspd from the server */
      if (ctl.recv_msg(msg) != 0) {
        errmsg = "Protocol error!\n";
        failed = true;
        return true;
      }
      if (msg.type != TEST_MSG) {
        errmsg = "C2S throughput test: Received wrong type of the message\n";
        failed = true;
        return true;
      }
      String tmpstr3 = new String(msg.body);
      sc2sspd = Double.parseDouble(tmpstr3) / 1000.0;

      if (sc2sspd < 1.0) {
        results.append(prtdbl(sc2sspd*1000) + "kb/s\n");
        statistics.append(prtdbl(sc2sspd*1000) + "kb/s\n");
        emailText += prtdbl(sc2sspd*1000) + "kb/s\n%0A";
      } 
      else {
        results.append(prtdbl(sc2sspd) + "Mb/s\n");
        statistics.append(prtdbl(sc2sspd) + "Mb/s\n");
        emailText += prtdbl(sc2sspd) + "Mb/s\n%0A";
      }
      
      if (ctl.recv_msg(msg) != 0) {
        errmsg = "Protocol error!\n";
        failed = true;
        return true;
      }
      if (msg.type != TEST_FINALIZE) {
        errmsg = "C2S throughput test: Received wrong type of the message\n";
        failed = true;
        return true;
      }
    }
    return false;
  }

  public boolean test_s2c(Protocol ctl, Socket ctlSocket) throws IOException
  {
		byte buff[] = new byte[8192];
    Message msg = new Message();
    if ((tests & TEST_S2C) == TEST_S2C) {
      showStatus("Tcpbw100 inbound test...");
      results.append("running 10s inbound test (server-to-client [S2C]) . . . . . . ");
      statistics.append("running 10s inbound test (server-to-client [S2C]) . . . . . . ");
      emailText += "running 10s inbound test (server-to-client [S2C]) . . . . . . ";
      
      if (ctl.recv_msg(msg) != 0) {
        errmsg = "Protocol error!\n";
        failed = true;
        return true;
      }
      if (msg.type != TEST_PREPARE) {
        errmsg = "C2S throughput test: Received wrong type of the message\n";
        failed = true;
        return true;
      }
      int s2cport = Integer.parseInt(new String(msg.body));

      Socket inSocket;
      try {
        inSocket = new Socket(host, s2cport);
      } 
      catch (UnknownHostException e) {
        System.err.println("Don't know about host: " + host);
        errmsg = "unknown server\n" ;
        failed = true;
        return true;
      } 
      catch (IOException e) {
        System.err.println("Couldn't get 3rd connection to: " + host);
        errmsg = "Server Failed while receiving data\n" ;
        failed = true;
        return true;
      }

      InputStream srvin = inSocket.getInputStream();
      int bytes = 0;
      int inlth;

      // wait here for signal from server application 
      if (ctl.recv_msg(msg) != 0) {
        errmsg = "Protocol error!\n";
        failed = true;
        return true;
      }
      if (msg.type != TEST_START) {
        errmsg = "S2C throughput test: Received wrong type of the message\n";
        failed = true;
        return true;
      }

      inSocket.setSoTimeout(15000);
      t = System.currentTimeMillis();

      try {  
        while ((inlth=srvin.read(buff,0,buff.length)) > 0) {
          bytes += inlth;
          if ((System.currentTimeMillis() - t) > 14500)
            break;
        }
      } 
      catch (IOException e) {}

      t =  System.currentTimeMillis() - t;
      System.out.println(bytes + " bytes " + (8.0 * bytes)/t + " kb/s " + t/1000 + " secs");
      s2cspd = ((8.0 * bytes) / 1000) / t;

      /* receive the s2cspd from the server */
      if (ctl.recv_msg(msg) != 0) {
        errmsg = "Protocol error!\n";
        failed = true;
        return true;
      }
      if (msg.type != TEST_MSG) {
        errmsg = "S2C throughput test: Received wrong type of the message\n";
        failed = true;
        return true;
      }
      try {
        String tmpstr3 = new String(msg.body);
        int k1 = tmpstr3.indexOf(" ");
        int k2 = tmpstr3.substring(k1+1).indexOf(" ");
        ss2cspd = Double.parseDouble(tmpstr3.substring(0, k1)) / 1000.0;
        ssndqueue = Integer.parseInt(tmpstr3.substring(k1+1).substring(0, k2));
        sbytes = Double.parseDouble(tmpstr3.substring(k1+1).substring(k2+1));
      }
      catch (Exception e) {
        e.printStackTrace();
        errmsg = "S2C throughput test: Received improper message\n";
        failed = true;
        return true;
      }

      if (s2cspd < 1.0) {
        results.append(prtdbl(s2cspd*1000) + "kb/s\n");
        statistics.append(prtdbl(s2cspd*1000) + "kb/s\n");
        emailText += prtdbl(s2cspd*1000) + "kb/s\n%0A";
      } else {
        results.append(prtdbl(s2cspd) + "Mb/s\n");
        statistics.append(prtdbl(s2cspd) + "Mb/s\n");
        emailText += prtdbl(s2cspd) + "Mb/s\n%0A";
      }

      srvin.close();
      inSocket.close();

      buff = Double.toString(s2cspd*1000).getBytes();
      String tmpstr4 = new String(buff, 0, buff.length);
      System.out.println("Sending '" + tmpstr4 + "' back to server");
      ctl.send_msg(TEST_MSG, buff);

      /* get web100 variables from server */
      tmpstr = "";
      int i = 0;

      // Try setting a 5 second timer here to break out if the read fails.
      ctlSocket.setSoTimeout(5000);
      try {  
        for (;;) {
          if (ctl.recv_msg(msg) != 0) {
            errmsg = "Protocol error!\n";
            failed = true;
            return true;
          }
          if (msg.type == TEST_FINALIZE) {
            break;
          }
          if (msg.type != TEST_MSG) {
            errmsg = "S2C throughput test: Received wrong type of the message\n";
            failed = true;
            return true;
          }
          tmpstr += new String(msg.body);
          i++;
        }
      } catch (IOException e) {}
      ctlSocket.setSoTimeout(0);
    }
    return false;
  }

	public void dottcp() throws IOException {
		Socket ctlSocket = null;
    if (!isApplication) {
		  host = getCodeBase().getHost();
    }
		int ctlport = 3001;
		double wait2;
		int sbuf, rbuf;
		int i, wait;

		failed = false;
		try {
			ctlSocket = new Socket(host, ctlport);
		} catch (UnknownHostException e) {
			System.err.println("Don't know about host: " + host);
			errmsg = "unknown server\n" ;
			failed = true;
			return;
		} catch (IOException e) {
			System.err.println("Couldn't get the connection to: " + host + " " +ctlport);
			errmsg = "Server process not running: start web100srv process on remote server (" + host + ":" + ctlport + ")\n" ;
			failed = true;
			return;
		}
    Protocol ctl = new Protocol(ctlSocket);
    Message msg = new Message();

    /* The beginning of the protocol */

    /* write our test suite request */
    ctl.send_msg(MSG_LOGIN, tests);
    /* read the specially crafted data that kicks off the old clients */
    if (ctl.readn(msg, 13) != 13) {
      errmsg = "Information: The server does not support this command line client\n";
      failed = true;
      return;
    }
    
    for (;;) {
      if (ctl.recv_msg(msg) != 0) {
        errmsg = "Protocol error!\n";
        failed = true;
        return;
      }
      if (msg.type != SRV_QUEUE) {
        errmsg = "Logging to server: Received wrong type of the message\n";
        failed = true;
        return;
      }
      String tmpstr3 = new String(msg.body);
      wait = Integer.parseInt(tmpstr3);
      System.out.println("wait flag received = " + wait);
      
      if (wait == 0) {
        break;
      }

      if (wait == 9999) {
        errmsg = "Server Busy: Please wait 60 seconds for the current test to finish\n";
        failed = true;
        return;
      }
      // Each test should take less than 30 seconds, so tell them 45 sec * number of 
      // tests in the queue.
      wait = (wait * 45);
      results.append("Another client is currently being served, your test will " +
          "begin within " + wait + " seconds\n");
    }

		f.toBack();
		ff.toBack();

    if (ctl.recv_msg(msg) != 0) {
      errmsg = "Protocol error!\n";
      failed = true;
      return;
    }
    if (msg.type != MSG_LOGIN) {
      errmsg = "Negotiating NDT version: Received wrong type of the message\n";
      failed = true;
      return;
    }

    String vVersion = new String(msg.body);
    if (!vVersion.startsWith("v")) {
      errmsg = "Incompatible version number";
      failed = true;
      return;
    }
    System.out.println("Server version: " + vVersion.substring(1));
    
    if (ctl.recv_msg(msg) != 0) {
      errmsg = "Protocol error!\n";
      failed = true;
      return;
    }
    if (msg.type != MSG_LOGIN) {
      errmsg = "Negotiating test suite: Received wrong type of the message\n";
      failed = true;
      return;
    }
    StringTokenizer tokenizer = new StringTokenizer(new String(msg.body), " ");

    while (tokenizer.hasMoreTokens()) {
      int testId = Integer.parseInt(tokenizer.nextToken());
      switch (testId) {
        case TEST_MID:
          if (test_mid(ctl)) {
            return;
          }
          break;
        case TEST_SFW:
          if (test_sfw(ctl)) {
            return;
          }
          break;
        case TEST_C2S:
          if (test_c2s(ctl)) {
            return;
          }
          break;
        case TEST_S2C:
          if (test_s2c(ctl, ctlSocket)) {
            return;
          }
          break;
        default:
          errmsg = "Unknown test ID\n";
          failed = true;
          return;
      }
    }

    i = 0;

    try {  
      for (;;) {
        if (ctl.recv_msg(msg) != 0) {
          errmsg = "Protocol error!\n";
          failed = true;
          return;
        }
        if (msg.type == MSG_LOGOUT) {
          break;
        }
        if (msg.type != MSG_RESULTS) {
          errmsg = "Tests results: Received wrong type of the message\n";
          failed = true;
          return;
        }
        tmpstr += new String(msg.body);
        i++;
      }
    } catch (IOException e) {}

    if (i == 0) {
      results.append("Warning! Client time-out while reading data, possible duplex mismatch exists\n");
    }
		System.err.println("Calling InetAddress.getLocalHost() twice");
		try {
			diagnosis.append("Client: " + InetAddress.getLocalHost() + "\n");
		} catch (SecurityException e) {
			diagnosis.append("Client: 127.0.0.1\n");
			results.append("Unable to obtain local IP address\n");
			System.err.println("Unable to obtain local IP address: using 127.0.0.1");
		}
		
		try {
			emailText += "Client: " + InetAddress.getLocalHost() + "\n%0A";
		} catch (SecurityException e) {
			emailText += "Client: 127.0.0.1\n%0A";
		}

    ctl.close();
		ctlSocket.close();

		testResults(tmpstr);
		middleboxResults(tmpstr2);
	}


	class OsfwWorker implements Runnable
  {
    private ServerSocket srvSocket;
    private int testTime;
    private boolean finalized = false;
    
    OsfwWorker(ServerSocket srvSocket, int testTime)
    {
      this.srvSocket = srvSocket;
      this.testTime = testTime;
    }

    public void finalize()
    {
      while (!finalized) {
        try {
          Thread.currentThread().sleep(1000);
        }
        catch (InterruptedException e) {
          // do nothing.
        }
      }
    }
    
    public void run()
    {
      Message msg = new Message();
      Socket sock = null;

      try {
        srvSocket.setSoTimeout(testTime * 1000);
        try {
          sock = srvSocket.accept();
        }
        catch (Exception e) {
          e.printStackTrace();
          s2cResult = SFW_POSSIBLE;
          srvSocket.close();
          finalized = true;
          return;
        }
        Protocol sfwCtl = new Protocol(sock);

        if (sfwCtl.recv_msg(msg) != 0) {
          System.out.println("Simple firewall test: unrecognized message");
          s2cResult = SFW_UNKNOWN;
          sock.close();
          srvSocket.close();
          finalized = true;
          return;
        }
        if (msg.type != TEST_MSG) {
          s2cResult = SFW_UNKNOWN;
          sock.close();
          srvSocket.close();
          finalized = true;
          return;
        }
        if (! new String(msg.body).equals("Simple firewall test")) {
          System.out.println("Simple firewall test: Improper message");
          s2cResult = SFW_UNKNOWN;
          sock.close();
          srvSocket.close();
          finalized = true;
          return;
        }
        s2cResult = SFW_NOFIREWALL;
      }
      catch (IOException ex) {
        s2cResult = SFW_UNKNOWN;
      }
      try {
        sock.close();
        srvSocket.close();
      }
      catch (IOException e) {
        // do nothing
      }
      finalized = true;
    }
  }

  public void showBufferedBytesInfo()
  {
    JOptionPane.showMessageDialog(null,
        "The bytes written by the sender need some time to reach the receiver.\n" +
        "In the throughput tests it's possible that not all bytes would be read by\n" +
        "the receiver's application. This is caused by the time limitation in the\n" +
        "reading process.\n\n" +
        "However, the missing bytes cannot disappear. They must be buffered in some of\n" +
        "the places:\n" +
        " - sender's buffers\n" +
        " - middleboxes' buffers (like long data queues in the DSL modems)\n" +
        " - receiver's buffers\n\n" +
        "The high percentage of the buffered bytes means that sender writes the data\n" +
        "much faster than capabilities of the link allows. Such situation decreases\n" +
        "the responsiveness of the connection (the latency is very high), but has\n" +
        "almost no influence for the throughput test results.\n\n" +
        "In order to avoid too high percentage of the buffered bytes, the size of the\n" +
        "sender's buffers should be decreased.",
        "Buffered bytes",
        JOptionPane.INFORMATION_MESSAGE);
  }

	public void testResults(String tmpstr) {
		StringTokenizer tokens;
		int i=0;
		String sysvar, strval;
		int sysval, Zero=0, bwdelay, minwin;
		double sysval2, j;
		String osName, osArch, osVer, javaVer, javaVendor, client;

		tokens = new StringTokenizer(tmpstr);
		sysvar = null;
		strval = null;
		while(tokens.hasMoreTokens()) {
			if(++i%2 == 1) {
				sysvar = tokens.nextToken();
			}
			else {
				strval = tokens.nextToken();
				diagnosis.append(sysvar + " " + strval + "\n");
				emailText += sysvar + " " + strval + "\n%0A";
				if (strval.indexOf(".") == -1) {
					sysval = Integer.parseInt(strval);
					save_int_values(sysvar, sysval);
				}
				else {
					sysval2 = Double.valueOf(strval).doubleValue();
					save_dbl_values(sysvar, sysval2);
				}
			}
		}

		// Grap some client details from the applet environment
		osName = System.getProperty("os.name");
		osArch = System.getProperty("os.arch");
		osVer = System.getProperty("os.version");
		javaVer = System.getProperty("java.version");
		javaVendor = System.getProperty("java.vendor");

		if (osArch.startsWith("x86") == true) {
			client = "PC";
		}
		else {
			client = "Workstation";
		}

		// Calculate some variables and determine path conditions
		// Note: calculations now done in server and the results are shipped
		//    back to the client for printing.

		if (CountRTT>0) {
			// Now write some messages to the screen
			if (c2sData < 3) {
				if (c2sData < 0) {
					results.append("Server unable to determine bottleneck link type.\n");
					emailText += "Server unable to determine bottleneck link type.\n%0A";
				} 
				else {
					results.append("Your " + client + " is connected to a ");
					emailText += "Your " + client + " is connected to a ";
					if (c2sData == 1) {
						results.append("Dial-up Modem\n");
						emailText += "Dial-up Modem\n%0A";
						mylink = .064;
					} 
					else {
						results.append("Cable/DSL modem\n");
						emailText += "Cable/DSL modem\n%0A";
						mylink = 3;
					}
				}
			} 
			else {
				results.append("The slowest link in the end-to-end path is a ");
				emailText += "The slowest link in the end-to-end path is a ";
				if (c2sData == 3) {
					results.append("10 Mbps Ethernet subnet\n");
					emailText += "10 Mbps Ethernet subnet\n%0A";
					mylink = 10;
				} 
				else if (c2sData == 4) {
					results.append("45 Mbps T3/DS3 subnet\n");
					emailText += "45 Mbps T3/DS3 subnet\n%0A";
					mylink = 45;
				} 
				else if (c2sData == 5) {
					results.append("100 Mbps ");
					emailText += "100 Mbps ";
					mylink = 100;
					if (half_duplex == 0) {
						results.append("Full duplex Fast Ethernet subnet\n");
						emailText += "Full duplex Fast Ethernet subnet\n%0A";
					} 
					else {
						results.append("Half duplex Fast Ethernet subnet\n");
						emailText += "Half duplex Fast Ethernet subnet\n%0A";
					}
				} 
				else if (c2sData == 6) {
					results.append("a 622 Mbps OC-12 subnet\n");
					emailText += "a 622 Mbps OC-12 subnet\n%0A";
					mylink = 622;
				} 
				else if (c2sData == 7) {
					results.append("1.0 Gbps Gigabit Ethernet subnet\n");
					emailText += "1.0 Gbps Gigabit Ethernet subnet\n%0A";
					mylink = 1000;
				} 
				else if (c2sData == 8) {
					results.append("2.4 Gbps OC-48 subnet\n");
					emailText += "2.4 Gbps OC-48 subnet\n%0A";
					mylink = 2400;
				} 
				else if (c2sData == 9) {
					results.append("10 Gbps 10 Gigabit Ethernet/OC-192 subnet\n");
					emailText += "10 Gbps 10 Gigabit Ethernet/OC-192 subnet\n%0A";
					mylink = 10000;
				}
			}

			if (mismatch == 1) {
				results.append("Warning: Old Duplex mismatch condition detected: ");
				emailText += "Warning: Old Duplex mismatch condition detected: ";
			}
 			else if (mismatch == 2) {
				results.append("Alarm: Duplex Mismatch condition detected Switch=Full and Host=half\n ");
				emailText += "Alarm: Duplex Mismatch condition detected Switch=Full and Host=half\n%0A ";
			}
 			else if (mismatch == 4) {
				results.append("Alarm: Possible Duplex Mismatch condition detected Switch=Full and Host=half\n ");
				emailText += "Alarm: Possible Duplex Mismatch condition detected Switch=Full and Host=half\n%0A ";
			}
 			else if (mismatch == 3) {
				results.append("Alarm: Duplex Mismatch condition detected Switch=half and Host=full\n ");
				emailText += "Alarm: Duplex Mismatch condition detected Switch=half and Host=full\n%0A ";
			}
 			else if (mismatch == 5) {
				results.append("Alarm: Possible Duplex Mismatch condition detected Switch=half and Host=full\n ");
				emailText += "Alarm: Possible Duplex Mismatch condition detected Switch=half and Host=full\n%0A ";
			}
 			else if (mismatch == 7) {
				results.append("Warning: Possible Duplex Mismatch condition detected Switch=half and Host=full\n ");
				emailText += "Warning: Possible Duplex Mismatch condition detected Switch=half and Host=full\n%0A ";
			}

			if (mismatch == 0) {
			    if (bad_cable == 1) {
				results.append("Alarm: Excessive errors, check network cable(s).\n");
				emailText += "Alarm: Excessive errors, check network cable(s).\n%0A";
			    }
			    if (congestion == 1) {
				results.append("Information: Other network traffic is congesting the link\n");
				emailText += "Information: Other network traffic is congesting the link\n%0A";
			    }
			    if (((2*rwin)/rttsec) < mylink) {
			        j = (float)((mylink * avgrtt) * 1000) / 8 / 1024;
			        if (j > (float)MaxRwinRcvd) {
				    results.append("Information: The receive buffer should be " +
				    prtdbl(j) + " kbytes to maximize throughput\n");
				    emailText += "Information: The receive buffer should be " +
				    prtdbl(j) + " kbytes to maximize throughput\n";
			        }
			    }
			}
     
      if ((tests & TEST_C2S) == TEST_C2S) {
        if (sc2sspd < (c2sspd  * (1.0 - VIEW_DIFF))) {
          // TODO:  distinguish the host buffering from the middleboxes buffering
          JLabel info = new JLabel("Information ");
          info.addMouseListener(new MouseAdapter()
              {

                public void mouseClicked(MouseEvent e) {
                  showBufferedBytesInfo();
                }

              });
          info.setForeground(Color.BLUE);
          info.setCursor(new Cursor(Cursor.HAND_CURSOR));
          info.setAlignmentY((float) 0.8);
          results.insertComponent(info);
          results.append("[C2S]: " + prtdbl(100 * (c2sspd - sc2sspd) / c2sspd) + "% of the transmitted bytes were buffered.\n");
        }
      }
      
      if ((tests & TEST_S2C) == TEST_S2C) {
        if (s2cspd < (ss2cspd  * (1.0 - VIEW_DIFF))) {
          // TODO:  distinguish the host buffering from the middleboxes buffering
          JLabel info = new JLabel("Information ");
          info.addMouseListener(new MouseAdapter()
              {

                public void mouseClicked(MouseEvent e) {
                  showBufferedBytesInfo();
                }

              });
          info.setForeground(Color.BLUE);
          info.setCursor(new Cursor(Cursor.HAND_CURSOR));
          info.setAlignmentY((float) 0.8);
          results.insertComponent(info);
          results.append("[S2C]: " + prtdbl(100 * (ss2cspd - s2cspd) / ss2cspd) + "% of the transmitted bytes were buffered.\n");
        }
      }

			statistics.append("\n\t------  Client System Details  ------\n");
			statistics.append("OS data: Name = " + osName + ", Architecture = " + osArch);
			statistics.append(", Version = " + osVer + "\n");
			statistics.append("Java data: Vendor = " + javaVendor + ", Version = " + javaVer + "\n");
			// statistics.append(" java.class.version=" + System.getProperty("java.class.version") + "\n");

			statistics.append("\n\t------  Web100 Detailed Analysis  ------\n");
			if (c2sData == -2)
				statistics.append("Insufficent data collected to determine link type.\n");
			else if (c2sData == -1)
				statistics.append("Interprocess communications failed, unknown link type.\n");
			else if (c2sData == 0)
				statistics.append("Link detection algorithm failed due to excessive Round Trip Times.\n");
			else if (c2sData == 1)
				statistics.append("Dial-up modem link found.\n");
			else if (c2sData == 2)
				statistics.append("Cable modem/DSL/T1 link found.\n");
			else if (c2sData == 3)
				statistics.append("10 Mbps Ethernet link found.\n");
			else if (c2sData == 4)
				statistics.append("45 Mbps T3/DS3 link found.\n");
			else if (c2sData == 5)
				statistics.append("100 Mbps FastEthernet link found.\n");
			else if (c2sData == 6)
				statistics.append("622 Mbps OC-12 link found.\n");
			else if (c2sData == 7)
				statistics.append("1 Gbps GigabitEthernet link found.\n");
			else if (c2sData == 8)
				statistics.append("2.4 Gbps OC-48 link found.\n");
			else if (c2sData == 9)
				statistics.append("10 Gbps 10 GigEthernet/OC-192 link found.\n");

			if (half_duplex == 0)
				statistics.append("Link set to Full Duplex mode\n");
			else
				statistics.append("Link set to Half Duplex mode\n");

			if (congestion == 0)
				statistics.append("No network congestion discovered.\n");
			else
				statistics.append("Information: throughput is limited by other network traffic.\n");

			if (bad_cable == 0)
				statistics.append("Good network cable(s) found\n");
			else
				statistics.append("Warning: excessive network errors, check network cable(s)\n");

			if (mismatch == 0)
				statistics.append("Normal duplex operation found.\n");
			else if (mismatch == 1) {
				statistics.append("Warning: Old Duplex mismatch condition detected: ");
				emailText += "Warning: Old Duplex mismatch condition detected: ";
			}
 			else if (mismatch == 2) {
				statistics.append("Alarm: Duplex Mismatch condition detected Switch=Full and Host=half\n ");
				emailText += "Alarm: Duplex Mismatch condition detected Switch=Full and Host=half\n%0A ";
			}
 			else if (mismatch == 3) {
				statistics.append("Alarm: Duplex Mismatch condition detected Switch=half and Host=full\n ");
				emailText += "Alarm: Duplex Mismatch condition detected Switch=half and Host=full\n%0A ";
			}

			statistics.append("\nWeb100 reports the Round trip time = " + prtdbl(avgrtt) + " msec; ");
			emailText += "\n%0AWeb100 reports the Round trip time = " + prtdbl(avgrtt)	+ " msec; ";

			statistics.append("the Packet size = " + CurrentMSS + " Bytes; and \n");
			emailText += "the Packet size = " + CurrentMSS + " Bytes; and \n%0A";
			if (PktsRetrans > 0) {
				statistics.append("There were " + PktsRetrans + " packets retransmitted");
				statistics.append(", " + DupAcksIn + " duplicate acks received");
				statistics.append(", and " + SACKsRcvd + " SACK blocks received\n");
				emailText += "There were " + PktsRetrans + " packets retransmitted";
				emailText += ", " + DupAcksIn + " duplicate acks received";
				emailText += ", and " + SACKsRcvd + " SACK blocks received\n%0A";
				if (Timeouts > 0) {
					statistics.append("The connection stalled " + Timeouts + " times due to packet loss\n");
				}
	
				statistics.append("The connection was idle " + prtdbl(waitsec) + " seconds (" + 
					prtdbl((waitsec/timesec)*100) + "%) of the time\n");
				emailText += "The connection stalled " + Timeouts + " times due to packet loss\n%0A";
				emailText += "The connection was idle " + prtdbl(waitsec) + " seconds (" + 
				prtdbl((waitsec/timesec)*100) + "%) of the time\n%0A";
			} 
			else if (DupAcksIn > 0) {
				statistics.append("No packet loss - ");
				statistics.append("but packets arrived out-of-order " + prtdbl(order*100) +
				"% of the time\n");
				emailText += "No packet loss - ";
				emailText += "but packets arrived out-of-order " + prtdbl(order*100) +
				"% of the time\n%0A";
			} 
			else {
				statistics.append("No packet loss was observed.\n");
				emailText += "No packet loss was observed.\n%0A";
			}

      if ((tests & TEST_C2S) == TEST_C2S) {
        if (c2sspd > sc2sspd) {
          statistics.append(prtdbl(100 * (c2sspd - sc2sspd) / c2sspd) + "% of the transmitted bytes were buffered in the C2S throughput test.\n");
        }
        else {
          statistics.append("All transmitted bytes in the C2S throughput test were received.\n");
        }
      }
      
      if ((tests & TEST_S2C) == TEST_S2C) {
        if (ss2cspd > s2cspd) {
          statistics.append(prtdbl(100 * (ss2cspd - s2cspd) / ss2cspd) + "% of the transmitted bytes were buffered in the S2C throughput test.\n");
        }
        else {
          statistics.append("All transmitted bytes in the S2C throughput test were received.\n");
        }
      }
      
			if (rwintime > .015) {
				statistics.append("This connection is receiver limited " + prtdbl(rwintime*100) +
				"% of the time.\n");
				emailText += "This connection is receiver limited " + prtdbl(rwintime*100) +
				"% of the time.\n%0A";

			// I think there is a bug here, it sometimes tells you to increase the buffer
			// size, but the new size is smaller than the current.

				if (((2*rwin)/rttsec) < mylink) {
					statistics.append("  Increasing the the client's receive buffer (" + prtdbl(MaxRwinRcvd/1024) +
					" KB) will improve performance\n");
				}
			}
			if (sendtime > .015) {
				statistics.append("This connection is sender limited " + prtdbl(sendtime*100) +
				"% of the time.\n");
				emailText += "This connection is sender limited " + prtdbl(sendtime*100) +
				"% of the time.\n%0A";
				if ((2*(swin/rttsec)) < mylink) {
					statistics.append("  Increasing the NDT server's send buffer (" + prtdbl(Sndbuf/2048) +
					" KB) will improve performance\n");
				}
			}
			if (cwndtime > .005) {
				statistics.append("This connection is network limited " + prtdbl(cwndtime*100) + "% of the time.\n");
				emailText += "This connection is network limited " + prtdbl(cwndtime*100) + "% of the time.\n%0A";
				// if (cwndtime > .15)
				//	statistics.append("  Contact your local network administrator to report a network problem\n");
				// if (order > .15)
				//	statistics.append("  Contact your local network admin and report excessive packet reordering\n");
			}
			if ((spd < 4) && (loss > .01)) {
				statistics.append("Excessive packet loss is impacting your performance, check the ");
				statistics.append("auto-negotiate function on your local PC and network switch\n");
			}
			
			statistics.append("\nWeb100 reports TCP negotiated the optional Performance Settings to: \n");
			statistics.append("RFC 2018 Selective Acknowledgment: ");
			if(SACKEnabled == Zero)
				statistics.append ("OFF\n");
			else
				statistics.append ("ON\n");

			statistics.append("RFC 896 Nagle Algorithm: ");
			if(NagleEnabled == Zero)
				statistics.append ("OFF\n");
			else
				statistics.append ("ON\n");

			statistics.append("RFC 3168 Explicit Congestion Notification: ");
			if(ECNEnabled == Zero)
				statistics.append ("OFF\n");
			else
				statistics.append ("ON\n");

			statistics.append("RFC 1323 Time Stamping: ");
			if(TimestampsEnabled == 0)
				statistics.append ("OFF\n");
			else
				statistics.append ("ON\n");

			statistics.append("RFC 1323 Window Scaling: ");
			if (MaxRwinRcvd < 65535)
			    WinScaleRcvd = 0;
			if((WinScaleRcvd == 0) || (WinScaleRcvd > 20))
				statistics.append ("OFF\n");
			else
				statistics.append ("ON\n");

      statistics.append("\n");

      if ((tests & TEST_SFW) == TEST_SFW) {
        switch (c2sResult) {
          case SFW_NOFIREWALL:
            statistics.append("Server '" + host + "' is not behind a firewall. [Connection to the ephemeral port was successful]\n");
            emailText += "Server '" + host + "' is not behind a firewall. [Connection to the ephemeral port was successful]\n%0A";
            break;
          case SFW_POSSIBLE:
            statistics.append("Server '" + host + "' is probably behind a firewall. [Connection to the ephemeral port failed]\n");
            emailText += "Server '" + host + "' is probably behind a firewall. [Connection to the ephemeral port failed]\n%0A";
            break;
          case SFW_UNKNOWN:
          case SFW_NOTTESTED:
            break;
        }
        switch (s2cResult) {
          case SFW_NOFIREWALL:
            statistics.append("Client is not behind a firewall. [Connection to the ephemeral port was successful]\n");
            emailText += "Client is not behind a firewall. [Connection to the ephemeral port was successful]\n%0A";
            break;
          case SFW_POSSIBLE:
            statistics.append("Client is probably behind a firewall. [Connection to the ephemeral port failed]\n");
            emailText += "Client is probably behind a firewall. [Connection to the ephemeral port failed]\n%0A";
            break;
          case SFW_UNKNOWN:
          case SFW_NOTTESTED:
            break;
        }
      }

//			diagnosis.append("\nEstimate = " + prtdbl(estimate) + " based on packet size = "
//				+ (CurrentMSS*8/1024) + "kbits, RTT = " + prtdbl(avgrtt) + "msec, " + "and loss = " + loss + "\n");
      diagnosis.append("\n");

			diagnosis.append("The theoretical network limit is " + prtdbl(estimate) + " Mbps\n");
			emailText += "The theoretical network limit is " + prtdbl(estimate) + " Mbps\n%0A";

			diagnosis.append("The NDT server has a " + prtdbl(Sndbuf/2048) + 
				" KByte buffer which limits the throughput to " + prtdbl(swin/rttsec) + " Mbps\n");
			emailText += "The NDT server has a " + prtdbl(Sndbuf/2048) + 
				" KByte buffer which limits the throughput to " + prtdbl(swin/rttsec) + " Mbps\n%0A";

			diagnosis.append("Your PC/Workstation has a " + prtdbl(MaxRwinRcvd/1024) +
				" KByte buffer which limits the throughput to " + prtdbl(rwin/rttsec) + " Mbps\n");
			emailText += "Your PC/Workstation has a " + prtdbl(MaxRwinRcvd/1024) +
				" KByte buffer which limits the throughput to " + prtdbl(rwin/rttsec) + " Mbps\n%0A";

			diagnosis.append("The network based flow control limits the throughput to " +	prtdbl(cwin/rttsec) + " Mbps\n");
			emailText += "The network based flow control limits the throughput to " +	prtdbl(cwin/rttsec) + " Mbps\n%0A";

			diagnosis.append("\nClient Data reports link is '" + prttxt(c2sData) +
				"', Client Acks report link is '" + prttxt(c2sAck) + "'\n" + 
				"Server Data reports link is '" + prttxt(s2cData) +
				"', Server Acks report link is '" + prttxt(s2cAck) + "'\n");
		}
	}  // testResults()
	
	

	/* this routine decodes the middlebox test results.  The data is returned
	* from the server is a specific order.  This routine pulls the string apart
	* and puts the values into the proper variable.  It then compares the values
	* to known values and writes out the specific results.
	*
	* server data is first
	* order is Server IP; Client IP; CurrentMSS; WinScaleRcvd; WinScaleSent;
	* Client then adds
	* Server IP; Client IP.
	*/

	public void middleboxResults(String tmpstr2) {
		StringTokenizer tokens;
		int k;

		// results.append("Mbox stats: ");
		tokens = new StringTokenizer(tmpstr2, ";");
		String ssip = tokens.nextToken();
		String scip = tokens.nextToken();
		// results.append("ssip=" + ssip + " scip=" + scip + "\n");

		// String mss = tokens.nextToken();
		// String winsrecv = tokens.nextToken();
		// String winssent = tokens.nextToken();
		int mss = Integer.parseInt(tokens.nextToken());
		int winsrecv = Integer.parseInt(tokens.nextToken());
		int winssent = Integer.parseInt(tokens.nextToken());

		String csip = tokens.nextToken();
		k = csip.indexOf("/");
		csip = csip.substring(k+1);

		String ccip = tokens.nextToken();
		k = ccip.indexOf("/");
		ccip = ccip.substring(k+1);

		// results.append("csip=" + csip + " ccip=" + ccip + "\n");
		// results.append("mss=" + mss + " winsrecv=" + winsrecv + " winssent=" +
		// 	winssent + "\n");

		if (mss == 1456)
			statistics.append("Packet size is preserved End-to-End\n");
		else
			statistics.append("Information: Network Middlebox is modifying MSS variable\n");

		// if ((winsrecv == -1) && (winssent == -1))
		//     statistics.append("Window scaling option is preserved End-to-End\n");
		// else
		//     statistics.append("Information: Network Middlebox is modifying Window scaling option\n");

    boolean preserved = false;
    try {
      preserved = InetAddress.getByName(ssip).equals(InetAddress.getByName(csip));
    }
    catch (UnknownHostException e) {
      preserved = ssip.equals(csip);
    }
		if (preserved) {
			statistics.append("Server IP addresses are preserved End-to-End\n");
		}
		else {
			statistics.append("Information: Network Address Translation (NAT) box is " +
			"modifying the Server's IP address\n");
			statistics.append("\tServer says [" + ssip + "] but Client says [" + csip + "]\n");
		}

		if (ccip.equals("127.0.0.1")) {
			statistics.append("Client IP address not found.  For IE users, modify the Java parameters\n");
			statistics.append("\tclick Tools - Internet Options - Security - Custom Level, scroll down to\n");
			statistics.append("\tMicrosoft VM - Java permissions and click Custom, click Java Custom Settings\n");
			statistics.append("\tEdit Permissions - Access to all Network Addresses, click Eanble and save changes\n");
		}
		else {
      try {
        preserved = InetAddress.getByName(scip).equals(InetAddress.getByName(ccip));
      }
      catch (UnknownHostException e) {
        preserved = scip.equals(ccip);
      }
			if (preserved)
				statistics.append("Client IP addresses are preserved End-to-End\n");
			else {
				statistics.append("Information: Network Address Translation (NAT) box is modifying the Client's IP address\n");
				statistics.append("\tServer says [" + scip + "] but Client says [" + ccip + "]\n");
			}
		}
	} // middleboxResults()



	public String prtdbl(double d) {
		String str = null;
		int i;

		if (d == 0) {
			return ("0");
		}
		str = Double.toString(d);
		i = str.indexOf(".");
		i = i + 3;
		if (i > str.length()) {
			i = i - 1;
		}
		if (i > str.length()) {
			i = i - 1;
		}
		return (str.substring(0, i));
	} // prtdbl()



	public String prttxt(int val) {
		String str = null;

		if (val == -1)
			str = "System Fault";
		else if (val == 0)
			str = "RTT";
		else if (val == 1)
			str = "Dial-up";
		else if (val == 2)
			str = "T1";
		else if (val == 3)
			str = "Ethernet";
		else if (val == 4)
			str = "T3";
		else if (val == 5)
			str = "FastE";
		else if (val == 6)
			str = "OC-12";
		else if (val == 7)
			str = "GigE";
		else if (val == 8)
			str = "OC-48";
		else if (val == 9)
			str = "10 Gig";
		return(str);
	} // prttxt()
	
	

	/* This routine saves the specific value into the variable of the
	* same name.  There should probably be an easier way to do this.
	*/
	public void save_dbl_values(String sysvar, double sysval) {
		if(sysvar.equals("bw:")) 
			estimate = sysval;
		else if(sysvar.equals("loss:")) 
			loss = sysval;
		else if(sysvar.equals("avgrtt:")) 
			avgrtt = sysval;
		else if(sysvar.equals("waitsec:")) 
			waitsec = sysval;
		else if(sysvar.equals("timesec:")) 
			timesec = sysval;
		else if(sysvar.equals("order:")) 
			order = sysval;
		else if(sysvar.equals("rwintime:")) 
			rwintime = sysval;
		else if(sysvar.equals("sendtime:")) 
			sendtime = sysval;
		else if(sysvar.equals("cwndtime:")) 
			cwndtime = sysval;
		else if(sysvar.equals("rttsec:")) 
			rttsec = sysval;
		else if(sysvar.equals("rwin:")) 
			rwin = sysval;
		else if(sysvar.equals("swin:")) 
			swin = sysval;
		else if(sysvar.equals("cwin:")) 
			cwin = sysval;
		else if(sysvar.equals("spd:")) 
			spd = sysval;
		else if(sysvar.equals("aspd:")) 
			aspd = sysval;
	} // save_dbl_values()



	public void save_int_values(String sysvar, int sysval) {
		/*  Values saved for interpretation:
		*	SumRTT 
		*	CountRTT
		*	CurrentMSS
		*	Timeouts
		*	PktsRetrans
		*	SACKsRcvd
		*	DupAcksIn
		*	MaxRwinRcvd
		*	MaxRwinSent
		*	Sndbuf
		*	Rcvbuf
		*	DataPktsOut
		*	SndLimTimeRwin
		*	SndLimTimeCwnd
		*	SndLimTimeSender
		*/   
		if(sysvar.equals("MSSSent:")) 
			MSSSent = sysval;
		else if(sysvar.equals("MSSRcvd:")) 
			MSSRcvd = sysval;
		else if(sysvar.equals("ECNEnabled:")) 
			ECNEnabled = sysval;
		else if(sysvar.equals("NagleEnabled:")) 
			NagleEnabled = sysval;
		else if(sysvar.equals("SACKEnabled:")) 
			SACKEnabled = sysval;
		else if(sysvar.equals("TimestampsEnabled:")) 
			TimestampsEnabled = sysval;
		else if(sysvar.equals("WinScaleRcvd:")) 
			WinScaleRcvd = sysval;
		else if(sysvar.equals("SumRTT:")) 
			SumRTT = sysval;
		else if(sysvar.equals("CountRTT:")) 
			CountRTT = sysval;
		else if(sysvar.equals("CurMSS:"))
			CurrentMSS = sysval;
		else if(sysvar.equals("Timeouts:")) 
			Timeouts = sysval;
		else if(sysvar.equals("PktsRetrans:")) 
			PktsRetrans = sysval;
		else if(sysvar.equals("SACKsRcvd:")) 
			SACKsRcvd = sysval;
		else if(sysvar.equals("DupAcksIn:")) 
			DupAcksIn = sysval;
		else if(sysvar.equals("MaxRwinRcvd:")) 
			MaxRwinRcvd = sysval;
		else if(sysvar.equals("MaxRwinSent:")) 
			MaxRwinSent = sysval;
		else if(sysvar.equals("Sndbuf:")) 
			Sndbuf = sysval;
		else if(sysvar.equals("X_Rcvbuf:")) 
			Rcvbuf = sysval;
		else if(sysvar.equals("DataPktsOut:")) 
			DataPktsOut = sysval;
		else if(sysvar.equals("FastRetran:")) 
			FastRetran = sysval;
		else if(sysvar.equals("AckPktsOut:")) 
			AckPktsOut = sysval;
		else if(sysvar.equals("SmoothedRTT:")) 
			SmoothedRTT = sysval;
		else if(sysvar.equals("CurCwnd:")) 
			CurrentCwnd = sysval;
		else if(sysvar.equals("MaxCwnd:")) 
			MaxCwnd = sysval;
		else if(sysvar.equals("SndLimTimeRwin:")) 
			SndLimTimeRwin = sysval;
		else if(sysvar.equals("SndLimTimeCwnd:")) 
			SndLimTimeCwnd = sysval;
		else if(sysvar.equals("SndLimTimeSender:")) 
			SndLimTimeSender = sysval;
		else if(sysvar.equals("DataBytesOut:")) 
			DataBytesOut = sysval;
		else if(sysvar.equals("AckPktsIn:")) 
			AckPktsIn = sysval;
		else if(sysvar.equals("SndLimTransRwin:"))
			SndLimTransRwin = sysval;
		else if(sysvar.equals("SndLimTransCwnd:"))
			SndLimTransCwnd = sysval;
		else if(sysvar.equals("SndLimTransSender:"))
			SndLimTransSender = sysval;
		else if(sysvar.equals("MaxSsthresh:"))
			MaxSsthresh = sysval;
		else if(sysvar.equals("CurRTO:"))
			CurrentRTO = sysval;
		else if(sysvar.equals("c2sData:"))
			c2sData = sysval;
		else if(sysvar.equals("c2sAck:"))
			c2sAck = sysval;
		else if(sysvar.equals("s2cData:"))
			s2cData = sysval;
		else if(sysvar.equals("s2cAck:"))
			s2cAck = sysval;
		else if(sysvar.equals("PktsOut:"))
			PktsOut = sysval;
		else if(sysvar.equals("mismatch:"))
			mismatch = sysval;
		else if(sysvar.equals("congestion:"))
			congestion = sysval;
		else if(sysvar.equals("bad_cable:"))
			bad_cable = sysval;
		else if(sysvar.equals("half_duplex:"))
			half_duplex = sysval;
		else if(sysvar.equals("CongestionSignals:"))
			CongestionSignals = sysval;
		else if(sysvar.equals("RcvWinScale:")) {
			if (RcvWinScale > 15)
				RcvWinScale = 0;
			else
				RcvWinScale = sysval;
		}
	}  // save_int_values()


	public void diagnose() {
		showStatus("Get WEB100 Variables");
		
		if (f == null) {
			f = new clsFrame();
		}
		f.setTitle("Web100 Variables");
		Panel buttons = new Panel();
		f.add("South", buttons);
		
		disMiss = new JButton("Close");
		disMiss.addActionListener(this);
		
		copy = new JButton("Copy");
		copy.addActionListener(this);
		
		diagnosis = new JTextArea("WEB100 Kernel Variables:\n", 15,30);
		diagnosis.setEditable(true);
		disMiss.setEnabled(true);
		copy.setEnabled(cancopy);
		
		buttons.add("West", disMiss);
		buttons.add("East", copy);
		f.add(new JScrollPane(diagnosis));
		f.pack();
	}  // diagnose()
	
	

	public void statistics() {
		showStatus("Print Detailed Statistics");

		if (ff == null) {
			ff = new clsFrame();
		}
		ff.setTitle("Detailed Statistics");
		Panel buttons = new Panel();
		ff.add("South", buttons);
		
		disMiss2 = new JButton("Close");
		disMiss2.addActionListener(this);
		
		copy2 = new JButton("Copy");
		copy2.addActionListener(this);
		
		statistics = new JTextArea("WEB100 Enabled Statistics:\n", 25,70);
		statistics.setEditable(true);
		disMiss2.setEnabled(true);
		copy2.setEnabled(cancopy);
		
		buttons.add("West", disMiss2);
		buttons.add("East", copy2);
		ff.add(new JScrollPane(statistics));
		ff.pack();
	}  // statistics()


  public void showOptions() {
    showStatus("Show options");

    if (optionsFrame == null) {
      optionsFrame = new clsFrame();
      optionsFrame.setTitle("Options");
      JPanel testsPanel = new JPanel();
      testsPanel.setBorder(BorderFactory.createTitledBorder("Performed tests"));
      defaultTest = new JCheckBox("Default tests");
      defaultTest.setSelected(true);
      defaultTest.setEnabled(false);
      testsPanel.add(defaultTest);
      optionsFrame.add(testsPanel);
      
      Panel buttons = new Panel();
      optionsFrame.add("South", buttons);

      JButton okButton= new JButton("OK");
      okButton.addActionListener(new ActionListener() {

        public void actionPerformed(ActionEvent e) {
          optionsFrame.toBack();
          optionsFrame.dispose();
        }
        
      });

      buttons.add("West", okButton);

      optionsFrame.pack();
    }
    optionsFrame.setResizable(true);
    optionsFrame.setVisible(true);
  }

	public void actionPerformed(ActionEvent event) {
		Object source = event.getSource();
		// System.err.println("Processing WINDOW event #" +event.getID());
		// System.err.println("Processing event " + source);

		if (source == startTest) {
			if(f != null) {
				f.toBack();
				f.dispose();
				f = null;
			}
			
			if(ff != null) {
				ff.toBack();
				ff.dispose();
				ff = null;
			}
			runtest();
		}

		else if (source == deTails) {
			deTails.setEnabled(false);
			f.setResizable(true);
      f.setVisible(true);
			deTails.setEnabled(true);
		}

		else if (source == disMiss) {
			f.toBack();
			f.dispose();
		}

		else if (source == disMiss2) {
			ff.toBack();
			ff.dispose();
		}

		else if (source == copy) {
			try {
				Clipboard clipbd = getToolkit().getSystemClipboard();
				cancopy = true;
				String s = diagnosis.getText();
				StringSelection ss = new StringSelection(s);
				clipbd.setContents(ss, ss);
				diagnosis.selectAll();
			} catch (SecurityException e) {
				cancopy = false;
			}
		}

		else if (source == copy2) {
			Clipboard clipbd = getToolkit().getSystemClipboard();
			String s = statistics.getText();
			StringSelection ss = new StringSelection(s);
			clipbd.setContents(ss, ss);
			statistics.selectAll();
		}

		else if (source == sTatistics) {
			sTatistics.setEnabled(false);
			ff.setResizable(true);
      ff.setVisible(true);
			sTatistics.setEnabled(true);
		}

		else if (source == mailTo) {
			int i;
			char key;
			String to[], from[], comments[];
			String Name, Host;

			mailTo.setEnabled(false);
			// envoke mailto: function
			showStatus("Tcpbw100 Invoking Mailto function...");
			
			results.append("Generating Trouble Report:  This report will be" +
			" emailed to the person you specify\n");
			try {
				if ((Name = getParameter(TARGET1)) == null) {
					throw new IllegalArgumentException("U parameter Required:");
				}
				if ((Host = getParameter(TARGET2)) == null) {
					throw new IllegalArgumentException("H parameter Required:");
				}

				String theURL = "mailto:" + Name + "@" + Host;
				String subject = getParameter("subject");
				
				if (subject == null) {
					subject = "Trouble Report from NDT on " + getCodeBase().getHost();
				}
				theURL += "?subject=" + subject;
				theURL += "&body=Comments:%0A%0A" + emailText + " End Of Email Message\n%0A";
				// System.out.println("Message body is '" + emailText + "'\n");
				targetURL = new URL(theURL);

			} catch (MalformedURLException rsi) {
				throw new IllegalArgumentException("Can't create mailto: URL" + rsi.getMessage());
			}

			getAppletContext().showDocument(targetURL);
		}
	}  // actionPerformed()


	public class clsFrame extends JFrame {
		public clsFrame() {
			addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent event) {
					// System.err.println("Handling window closing event");
					dispose();
				}
			});
			// System.err.println("Extended Frame class - RAC9/15/03");
		}
	} // class: clsFrame

  public static void main(String[] args)
  {
    JFrame frame = new JFrame("ANL/Internet2 NDT (applet)");
    if (args.length != 1) {
      System.out.println("Usage: java -jar Tcpbw100.jar " + "HOST");
      System.exit(0);
    }
    final Tcpbw100 applet = new Tcpbw100();
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        applet.destroy();
        System.exit(0);
      }
    });
    applet.isApplication = true;
    applet.host = args[0];
    frame.getContentPane().add(applet);
    frame.setSize(700, 320);
    applet.init();
    applet.start();
    frame.setVisible(true);
  }

} // class: Tcpbw100
