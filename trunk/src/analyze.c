/*
 *  web100srv [port]          v1   thd@ornl.gov
 *   concurrent server
 * reap children, and thus watch out for EINTR
 *   tcp bandwidth tester from applet  Tcpbw100.java
 *  test 10s up and 10s down
 *  report web100 stats back to client 
 *  accept on server port, create 2nd and 3rd server port
 *     having server creates ports help if client is behind a firewall
 * might wish to add tcpwrappers to limit who can test....
 *  may need a better (hash) random (uncompressable) data stream
 */

#include        <stdio.h>
#include        <sys/types.h>
#include        <sys/socket.h>
#include	<sys/time.h>
#include        <netinet/in.h>
#include        <arpa/inet.h>
#include        <netdb.h>
#include        <signal.h>
#include	<getopt.h>
#include	<netinet/tcp.h>
#include	<sys/un.h>
#include	<sys/errno.h>
#include	<sys/select.h>

#define LOGFILE "web100srv.log"
#define WEB100_VARS 128	/*number of web100 variables you want to access*/
#define WEB100_FILE "web100_variables"  /*names of the variables to access*/
#define BUFFSIZE 512
#define EXIT_FAILURE 10

  char buff[BUFFSIZE+1];
  int window = 64000;
  int randomize=0;
  char *rmt_host;
  double bwin, bwout;
  char *VarFileName=NULL;
  char *LogFileName=NULL;
  int debug=0;
	double avgrtt, loss, loss2, rttsec, bw, bw2, rwin, swin, cwin, speed;
	double rwintime, cwndtime, sendtime, timesec;
	int experimental=0, n, m, one=1;
	int Timeouts, SumRTT, CountRTT, MinRTT, PktsRetrans, FastRetran, DataPktsOut;
	int AckPktsOut, CurrentMSS, DupAcksIn, AckPktsIn, MaxRwinRcvd, Sndbuf;
	int CurrentCwnd, SndLimTimeRwin, SndLimTimeCwnd, SndLimTimeSender, DataBytesOut;
	int SndLimTransRwin, SndLimTransCwnd, SndLimTransSender, MaxSsthresh;
	int CurrentRTO, CurrentRwinRcvd, CongestionSignals, PktsOut, RcvWinScale;
	int link=0, mismatch=0, bad_cable=0, half_duplex=0, congestion=0, totaltime;
	int i, spd, ret;
	double order, idle, cpu_idle1, cpu_idle2;
	char *name, ip_addr[64], ip_addr2[64];
	FILE *fp;
	int indx, links[4][16], max, total;
	float runave[4];
	int c2sdata, c2sack, s2cdata, s2cack;
	int j;
	char spds[4][256], buff2[32];
	char *str, tmpstr[32];
	float run_ave[4], mmorder;
	int c2sspd, s2cspd, iponly;
	int linkcnt, mismatch2, mismatch3;

extern int errno;


err_sys(s) char *s;
{
	perror(s);
	exit(1);
}

void calculate()
{

	int tail[4], i, head[4];
	float k;
	float recvbwd, cwndbwd, sendbwd;
	char btlneck[64];
	int congestion2=0;
	struct hostent *hp;
	unsigned addr;
	double acks, aspeed, pureacks;

	tail[0] = tail[1] = tail[2] = tail[3] = 0;
	head[0] = head[1] = head[2] = head[3] = 0;
	for (i=0; i<4; i++) {
	    max = 0;
	    indx = 0;
	    total = 0;
	    for (j=0; j<linkcnt-1; j++) {
		total += links[i][j];
		if (max < links[i][j]) {
		    max = links[i][j];
		    indx = j;
		}
	    }
	    for (j=indx+1; j<10; j++) {
		k = (float) links[i][j] / max;
		if (k > .1)
		    tail[i]++;
	    }
	    for (j=0; j<indx; j++) {
		k = (float) links[i][j] / max;
		if (k > .1)
		    head[i]++;
	    }
	    if (links[i][indx] == -1)
		indx = -1;
	    if ((total < 20) && (indx != -1))
	 	indx = -2;
	    switch (i) {
		case 0: c2sdata = indx;
			if (debug > 0)
		    	    printf("Client --> Server data detects link = ");
			break;
		case 1: c2sack = indx;
			if (debug > 0)
		    	    printf("Client <-- Server Ack's detect link = ");
			break;
		case 2: s2cdata = indx;
			if (debug > 0)
		    	    printf("Server --> Client data detects link = ");
			break;
		case 3: s2cack = indx;
			if (debug > 0)
		    	    printf("Server <-- Client Ack's detect link = ");
	    }
	    if (debug > 0) {
	        switch (indx) {
		    case -2: printf("Insufficent Data\n");
			     break;
		    case -1: printf("System Fault\n");
			     break;
		    case 0:  printf("RTT\n");
			     break;
		    case 1:  printf("Dial-up\n");
			     break;
		    case 2:  printf("T1\n");
			     break;
		    case 3:  printf("Ethernet\n");
			     break;
		    case 4:  printf("T3\n");
			     break;
		    case 5:  printf("FastEthernet\n");
			     break;
		    case 6:  printf("OC-12\n");
			     break;
		    case 7:  printf("Gigabit Ethernet\n");
			     break;
		    case 8:  printf("OC-48\n");
			     break;
		    case 9:  printf("10 Gigabit Enet\n");
			     break;
		    case 10: printf("Retransmissions\n");
	        }
	    }
	}
	switch (c2sdata) {
	    case -2: sprintf(btlneck, "Insufficent Data");
		     break;
	    case -1: sprintf(btlneck, "a System Fault");
		     break;
	    case 0:  sprintf(btlneck, "the Round Trip Time");
		     break;
	    case 1:  sprintf(btlneck, "a 'Dial-up modem' connection");
		     break;
	    case 2:  
		    if ((c2sspd/s2cspd > .8) && (c2sspd/s2cspd < 1.2) && (c2sspd > 1000))
			sprintf(btlneck, "a 'T1' subnet");
		    else {
		        if ((tail[3] > 1) || (s2cack == 3))
			    sprintf(btlneck, "a 'Cable Modem' connection");
		        else 
			    sprintf(btlneck, "a 'DSL' connection");
		     }
		     break;
	    case 3:  if (linkcnt == 16)
			sprintf(btlneck, "a T1 + subnet");
		     else
			sprintf(btlneck, "an 'Ethernet' subnet");
		     break;
	    case 4:  if (linkcnt == 16)
			sprintf(btlneck, "a IEEE 802.11b Wifi subnet");
		     else
			sprintf(btlneck, "a 'T3/DS-3' subnet");
		     break;
	    case 5:  if (linkcnt == 16)
			sprintf(btlneck, "a Wifi + subnet");
		     else
			sprintf(btlneck, "a 'FastEthernet' subnet");
		     break;
	    case 6:  if (linkcnt == 16)
			sprintf(btlneck, "a Ethernet subnet");
		     else
			sprintf(btlneck, "an 'OC-12' subnet");
		     break;
	    case 7:  if (linkcnt == 16)
			sprintf(btlneck, "a T3/DS3 subnet");
		     else
			sprintf(btlneck, "a 'Gigabit Ethernet' subnet");
		     break;
	    case 8:  if (linkcnt == 16)
			sprintf(btlneck, "a FastEthernet subnet");
		     else
			sprintf(btlneck, "an 'OC-48' subnet");
		     break;
	    case 9:  if (linkcnt == 16)
			sprintf(btlneck, "a OC-12 subnet");
		     else
			sprintf(btlneck, "a '10 Gigabit Enet' subnet");
		     break;
	    case 10: if (linkcnt == 16)
			sprintf(btlneck, "a Gigabit Ethernet subnet");
		     else
			sprintf(btlneck, "Retransmissions");
	    case 11: 
			sprintf(btlneck, "an 'OC-48' subnet");
		     break;
	    case 12:
			sprintf(btlneck, "a '10 Gigabit Enet' subnet");
		     break;
	    case 13:
			sprintf(btlneck, "Retransmissions");
		     break;
	}
	/* Calculate some values */
	avgrtt = (double) SumRTT/CountRTT;
	rttsec = avgrtt * .001;
	loss = (double)(PktsRetrans- FastRetran)/(double)(DataPktsOut-AckPktsOut);
	loss2 = (double)CongestionSignals/PktsOut;
	if (loss == 0)
    		loss = .0000000001;	/* set to 10^-6 for now */
	if (loss2 == 0)
    		loss2 = .0000000001;	/* set to 10^-6 for now */

	order = (double)DupAcksIn/AckPktsIn;
	bw = (CurrentMSS / (rttsec * sqrt(loss))) * 8 / 1024 / 1024;
	bw2 = (CurrentMSS / (rttsec * sqrt(loss2))) * 8 / 1024 / 1024;
	totaltime = SndLimTimeRwin + SndLimTimeCwnd + SndLimTimeSender;
	rwintime = (double)SndLimTimeRwin/totaltime;
	cwndtime = (double)SndLimTimeCwnd/totaltime;
	sendtime = (double)SndLimTimeSender/totaltime;
	timesec = totaltime/1000000;
	idle = (Timeouts * ((double)CurrentRTO/1000))/timesec;

	recvbwd = ((MaxRwinRcvd*8)/avgrtt)/1000;
	cwndbwd = ((CurrentCwnd*8)/avgrtt)/1000;
	sendbwd = ((Sndbuf*8)/avgrtt)/1000;

	spd = ((double)DataBytesOut / (double)totaltime) * 8;

	mismatch2 = 0;
	mismatch3 = 0;
	mmorder = (float)(DataPktsOut - PktsRetrans - FastRetran) / DataPktsOut;

	/* new test based on analysis of TCP behavior in duplex mismatch condition. */

	acks = (double) AckPktsIn / (double) DataPktsOut;
	pureacks = (double) (AckPktsIn - DupAcksIn) / (double) DataPktsOut;
	aspeed = (double)c2sspd / (double)s2cspd;
	printf("Acks = %0.4f,  async speed = %0.4f\n", acks, aspeed);
	/* if ((((order > .2) && (acks > .7)) || (acks < .35)) && ((aspeed > 3.0) || (aspeed < 0.005)) */
	if ((order > .2) && ((acks > .7) || (acks < .35)) && (c2sdata > 2)) {
		/* && (acks < .99)) { */
		mismatch2 = 1;
	}
	/* if ((c2sdata > 2) && (((order > .2) && (acks > .7)) || (pureacks < .40))) {
	 * 	mismatch3 = 1;
	 * } 
	 */
	/* if ((c2sdata > 2) && (mmorder < .92) && (loss2*100 < 0.5) && ((acks > .7) || (acks < .35))) { */
	if ((c2sdata > 2) && (mmorder < .92) && (loss2*100 < 0.5) && ((bw2*1000) > s2cspd)) {
		mismatch3 = 1;
	}
/* 	if ((cwndtime > .9) && (bw2 > 2) && (PktsRetrans/timesec > 2) &&
 *	    (MaxSsthresh > 0)  && (idle > .01)) {
 */
/*    (MaxSsthresh > 0)) { */
/*    (MaxSsthresh > 0)  && (idle > 1)) { */
/*   		mismatch2 = 1;
 *    		link = 0;
 *  	}
 */

	/* test for uplink with duplex mismatch condition */
/* 	if (((bwin/1000) > 50) && (spd < 5) && (rwintime > .9) && (loss < .01)) {
 *   		mismatch2 = 2;
 *    		link = 0;
 *	}
 */

	/* estimate is less than throughput, something is wrong */
	if (bw < spd)
  		link = 0;

	if (((loss*100)/timesec > 15) && (cwndtime/timesec > .6) &&
		    (loss < .01) && (MaxSsthresh > 0))
  		bad_cable = 1;

	/* test for Ethernet link (assume Fast E.) */
	/* if ((bw < 25) && (loss < .01) && (rwin/rttsec < 25) && (link > 0)) */
	if ((spd < 9.5) && (spd > 3.0) && ((bwin/1000) < 9.5) &&
		    (loss < .01) && (order < .035) && (link > 0))
  		link = 10;

	/* test for wireless link */
	if ((SndLimTimeSender < 15000) && (spd < 5) && (bw > 50) &&
		    ((SndLimTransRwin/SndLimTransCwnd) == 1) && (rwintime > .90) && (link > 0))
  		link = 3;

	/* test for DSL/Cable modem link */
	if ((SndLimTimeSender < 15000) && (spd < 2) && (spd < bw) && (link > 0))
  		link = 2;

	if (((rwintime > .95) && (SndLimTransRwin/timesec > 30) &&
		    (SndLimTransSender/timesec > 30)) || (link <= 10))
  		half_duplex = 1;

	if ((cwndtime > .02) && (mismatch2 == 0) && (cwndbwd < recvbwd))
    		congestion2 = 1;

	if (iponly == 0) {
	    addr = inet_addr(ip_addr);
	    hp = gethostbyaddr((char *) &addr, 4, AF_INET);
	    if (hp == NULL)
	        printf("Throughput to host [%s] is limited by %s\n", ip_addr, btlneck);
	    else
	        printf("Throughput to host %s [%s] is limited by %s\n", hp->h_name, ip_addr, btlneck);
	} else 
	    printf("Throughput to host [%s] is limited by %s\n", ip_addr, btlneck);

	printf("\tWeb100 says link = %d, speed-chk says link = %d\n", link, c2sdata);
	printf("\tSpeed-chk says {%d, %d, %d, %d}, Running average = {%0.1f, %0.1f, %0.1f, %0.1f}\n",
		c2sdata, c2sack, s2cdata, s2cack, runave[0], runave[1], runave[2], runave[3]);
	if (c2sspd > 1000)
	    printf("\tC2Sspeed = %0.2f Mbps, S2Cspeed = %0.2f Mbps, ", 
		(float) c2sspd/1000, (float)s2cspd/1000);
	else
	    printf("\tC2Sspeed = %d Kbps, S2Cspeed = %d Kbps, ", c2sspd, s2cspd);
	if (bw > 1)
	    printf("Estimate = %0.2f Mbps (%0.2f Mbps)\n", bw, bw2);
	else
	    printf("Estimate = %0.2f Kbps (%0.2f Kbps)\n", bw*1000, bw2*1000);

	if ((bw*1000) > s2cspd) 
	    printf("\tOld estimate is greater than measured; ");
	else
	    printf("\tOld estimate is less than measured; ");

	if (CongestionSignals == -1)
	    printf("No data collected to calculage new estimate\n");
	else {
	    if ((bw2*1000) > s2cspd) 
	        printf("New estimate is greater than measured\n");
	    else
	        printf("New estimate is less than measured\n");
	}

	printf("\tLoss = %0.2f%% (%0.2f%%), Out-of-Order = %0.2f%%, Long tail = {%d, %d, %d, %d}\n",
		loss*100, loss2*100, order*100, tail[0], tail[1], tail[2], tail[3]);
	printf("\tDistribution = {%d, %d, %d, %d}, time spent {r=%0.1f%% c=%0.1f%% s=%0.1f%%}\n",
		head[0], head[1], head[2], head[3], rwintime*100, cwndtime*100, sendtime*100);
	printf("\tAve(min) RTT = %0.2f (%d) msec, Buffers = {r=%d, c=%d, s=%d}\n",
		avgrtt, MinRTT, MaxRwinRcvd, CurrentCwnd, Sndbuf/2);
	printf("\tbw*delay = {r=%0.2f, c=%0.2f, s=%0.2f}, Transitions/sec = {r=%0.1f, c=%0.1f, s=%0.1f}\n",
		recvbwd, cwndbwd, sendbwd,
		SndLimTransRwin/timesec, SndLimTransCwnd/timesec, SndLimTransSender/timesec);
	printf("\tRetransmissions/sec = %0.1f, Timeouts/sec = %0.1f, SlowStart = %d\n", 
		(float) PktsRetrans/timesec, (float) Timeouts/timesec, MaxSsthresh);
	printf("\tMismatch = %d (%d:%d[%0.2f])", mismatch, mismatch2, mismatch3, mmorder);
	if (mismatch2 == 1) {
	    if (aspeed < 1)
		printf(" [H=F, S=H],");
	    else
		printf(" [H=H, S=F],");
	}
	else
		printf(",");
	printf(" Cable fault = %d, Congestion = %d, Duplex = %d\n\n",
		 bad_cable, congestion2, half_duplex);

	/* if (c2sdata != c2sack)
	 *    fprintf(stderr, "Warning, client to server test produced unusual results: %d != %d\n",
	 *	c2sdata, c2sack);
	 */

}

main(argc, argv)
int	argc;
char	*argv[];
{

	int c;
	/* char buff[1024*8]; */
	char tmpstr[256];

	iponly = 0;
	while ((c = getopt(argc, argv, "dhnl:")) != -1){
	    switch (c) {
		case 'h':
			printf("Usage: %s -d (debug) -n (No DNS) -l Log_FN\n", argv[0]);
			exit(0);
		case 'l':
			LogFileName = optarg;
			break;
		case 'n':
			iponly=1;
			break;
		case 'd':
			debug++;
			break;
	    }
	}
	if (LogFileName == NULL) {
		/* LogFileName = BASEDIR; */
		sprintf(tmpstr, "%s/%s", BASEDIR, LOGFILE);
		LogFileName = tmpstr;
	}
	if (debug > 0)
	    printf("log file = %s\n", LogFileName);


	if ((fp = fopen(LogFileName, "r")) == NULL)
	    err_sys("Missing Log file ");

	n = 0;
	m = 0;
	while ((fgets(buff, 512, fp)) != NULL) {
	    if (strncmp(buff, "spds", 4) == 0) {
	    	str = strchr(buff, 0x27);
		str++;
	        linkcnt = sscanf(str, "%d %d %d %d %d %d %d %d %d %d %d %d %d %d %d %f",
			&links[n][0], &links[n][1], &links[n][2], &links[n][3], &links[n][4],
			&links[n][5], &links[n][6], &links[n][7], &links[n][8],
			&links[n][9], &links[n][10], &links[n][11], &links[n][12],
			&links[n][13], &links[n][14], &runave[n]);
		if (linkcnt != 16) 
	            linkcnt = sscanf(str, "%d %d %d %d %d %d %d %d %d %d %d %d %f",
			&links[n][0], &links[n][1], &links[n][2], &links[n][3], &links[n][4],
			&links[n][5], &links[n][6], &links[n][7], &links[n][8],
			&links[n][9], &links[n][10], &links[n][11], &runave[n]);
		n++;
		continue;
	    }
	    if (strncmp(buff, "Running", 6) == 0) {
		ret = sscanf(buff, "%*s %*s %*s %f %*s", &run_ave[m]);
		if (debug > 0) {
		    printf("read %d tokens from buffer\n", ret);
		    printf("running average[%d] = %0.2f\n", m, run_ave[m]);
		}
	        if (runave[m] == 0)
		    runave[m] = run_ave[m];
		m++;
		continue;
	    }
	    if ((str = strchr(buff, 'p')) != NULL) {
		if ((strncmp(buff, "Apr", 3) == 0) || (strncmp(buff, "Sep", 3) == 0)) {
skip2:
		    str++;
		    str = strchr(str, 'p');
		    if (str == NULL)
			goto skip1;
		}
		if (strncmp(str, "port", 4) != 0)
		    goto skip2;
		sscanf(buff, "%*s %*s %*s %s %*s", &ip_addr);
		if (debug > 0)
		    printf("Start of New Packet trace\n");
		n = 0;
		m = 0;
		run_ave[0] = 0;
		run_ave[1] = 0;
		run_ave[2] = 0;
		run_ave[3] = 0;
		runave[0] = 0;
		runave[1] = 0;
		runave[2] = 0;
		runave[3] = 0;
	    }
skip1:
	    if ((str = strchr(buff, ',')) != NULL) {
		str++;
		sscanf(str, "%[^,]s", ip_addr2);
		if ((str = strchr(str, ',')) == NULL)
		    continue;
		str++;
		if (sscanf(str, "%[^,]s", tmpstr) < 1)
		    goto display;
		s2cspd = atoi(tmpstr);

		str = strchr(str, ',') +1;
		if (sscanf(str, "%[^,]s", tmpstr) < 1)
		    goto display;
		c2sspd = atoi(tmpstr);

		str = strchr(str, ',') +1;
		if (sscanf(str, "%[^,]s", tmpstr) < 1)
		    goto display;
		Timeouts = atoi(tmpstr);

		str = strchr(str, ',') +1;
		if (sscanf(str, "%[^,]s", tmpstr) < 1)
		    goto display;
		SumRTT = atoi(tmpstr);

		str = strchr(str, ',') +1;
		if (sscanf(str, "%[^,]s", tmpstr) < 1)
		    goto display;
		CountRTT = atoi(tmpstr);

		str = strchr(str, ',') +1;
		if (sscanf(str, "%[^,]s", tmpstr) < 1)
		    goto display;
		PktsRetrans = atoi(tmpstr);

		str = strchr(str, ',') +1;
		if (sscanf(str, "%[^,]s", tmpstr) < 1)
		    goto display;
		FastRetran = atoi(tmpstr);

		str = strchr(str, ',') +1;
		if (sscanf(str, "%[^,]s", tmpstr) < 1)
		    goto display;
		DataPktsOut = atoi(tmpstr);

		str = strchr(str, ',') +1;
		if (sscanf(str, "%[^,]s", tmpstr) < 1)
		    goto display;
		AckPktsOut = atoi(tmpstr);

		str = strchr(str, ',') +1;
		if (sscanf(str, "%[^,]s", tmpstr) < 1)
		    goto display;
		CurrentMSS = atoi(tmpstr);

		str = strchr(str, ',') +1;
		if (sscanf(str, "%[^,]s", tmpstr) < 1)
		    goto display;
		DupAcksIn = atoi(tmpstr);

		str = strchr(str, ',') +1;
		if (sscanf(str, "%[^,]s", tmpstr) < 1)
		    goto display;
		AckPktsIn = atoi(tmpstr);

		str = strchr(str, ',') +1;
		if (sscanf(str, "%[^,]s", tmpstr) < 1)
		    goto display;
		MaxRwinRcvd = atoi(tmpstr);

		str = strchr(str, ',') +1;
		if (sscanf(str, "%[^,]s", tmpstr) < 1)
		    goto display;
		Sndbuf = atoi(tmpstr);

		str = strchr(str, ',') +1;
		if (sscanf(str, "%[^,]s", tmpstr) < 1)
		    goto display;
		CurrentCwnd = atoi(tmpstr);

		str = strchr(str, ',') +1;
		if (sscanf(str, "%[^,]s", tmpstr) < 1)
		    goto display;
		SndLimTimeRwin = atoi(tmpstr);

		str = strchr(str, ',') +1;
		if (sscanf(str, "%[^,]s", tmpstr) < 1)
		    goto display;
		SndLimTimeCwnd = atoi(tmpstr);

		str = strchr(str, ',') +1;
		if (sscanf(str, "%[^,]s", tmpstr) < 1)
		    goto display;
		SndLimTimeSender = atoi(tmpstr);

		str = strchr(str, ',') +1;
		if (sscanf(str, "%[^,]s", tmpstr) < 1)
		    goto display;
		DataBytesOut = atoi(tmpstr);

		str = strchr(str, ',') +1;
		if (sscanf(str, "%[^,]s", tmpstr) < 1)
		    goto display;
		SndLimTransRwin = atoi(tmpstr);

		str = strchr(str, ',') +1;
		if (sscanf(str, "%[^,]s", tmpstr) < 1)
		    goto display;
		SndLimTransCwnd = atoi(tmpstr);

		str = strchr(str, ',') +1;
		if (sscanf(str, "%[^,]s", tmpstr) < 1)
		    goto display;
		SndLimTransSender = atoi(tmpstr);

		str = strchr(str, ',') +1;
		if (sscanf(str, "%[^,]s", tmpstr) < 1)
		    goto display;
		MaxSsthresh = atoi(tmpstr);

		str = strchr(str, ',') +1;
		if (sscanf(str, "%[^,]s", tmpstr) < 1)
		    goto display;
		CurrentRTO = atoi(tmpstr);

		str = strchr(str, ',') +1;
		if (sscanf(str, "%[^,]s", tmpstr) < 1)
		    goto display;
		CurrentRwinRcvd = atoi(tmpstr);

		str = strchr(str, ',') +1;
		if (sscanf(str, "%[^,]s", tmpstr) < 1)
		    goto display;
		link = atoi(tmpstr);

		str = strchr(str, ',') +1;
		if (sscanf(str, "%[^,]s", tmpstr) < 1)
		    goto display;
		mismatch = atoi(tmpstr);

		str = strchr(str, ',') +1;
		if (sscanf(str, "%[^,]s", tmpstr) < 1)
		    goto display;
		bad_cable = atoi(tmpstr);

		str = strchr(str, ',') +1;
		if (sscanf(str, "%[^,]s", tmpstr) < 1)
		    goto display;
		half_duplex = atoi(tmpstr);

		str = strchr(str, ',') +1;
		if (sscanf(str, "%[^,]s", tmpstr) < 1)
		    goto display;
		congestion = atoi(tmpstr);

		str = strchr(str, ',');
		if (str == NULL) {
		    CongestionSignals = -1;
		    goto display;
		}
		str += 1;
		if (sscanf(str, "%[^,]s", tmpstr) < 1)
		    goto display;
		c2sdata = atoi(tmpstr);

		str = strchr(str, ',') +1;
		if (sscanf(str, "%[^,]s", tmpstr) < 1)
		    goto display;
		c2sack = atoi(tmpstr);

		str = strchr(str, ',') +1;
		if (sscanf(str, "%[^,]s", tmpstr) < 1)
		    goto display;
		s2cdata = atoi(tmpstr);

		str = strchr(str, ',') +1;
		if (sscanf(str, "%[^,]s", tmpstr) < 1)
		    goto display;
		s2cack = atoi(tmpstr);

		str = strchr(str, ',');
		if (str == NULL) {
		    CongestionSignals = -1;
		    goto display;
		}
		str += 1;
		if (sscanf(str, "%[^,]s", tmpstr) < 1)
		    goto display;
		CongestionSignals = atoi(tmpstr);

		str = strchr(str, ',') +1;
		if (sscanf(str, "%[^,]s", tmpstr) < 1)
		    goto display;
		PktsOut = atoi(tmpstr);

		str = strchr(str, ',');
		if (str == NULL) {
		    MinRTT = -1;
		    goto display;
		}
		else {
		    str += 1;
		    if (sscanf(str, "%[^,]s", tmpstr) < 1)
		    	goto display;
		    MinRTT = atoi(tmpstr);
		}

		str = strchr(str, ',');
		if (str == NULL) {
		    RcvWinScale = -1;
		    goto display;
		}
		else {
		    str += 1;
		    if (sscanf(str, "%[^,]s", tmpstr) < 1)
		    	goto display;
		    RcvWinScale = atoi(tmpstr);
		}

		str = strchr(str, ',');
		if (str == NULL) {
		    cpu_idle1 = -1;
		    goto display;
		}
		else {
		    str += 1;
		    if (sscanf(str, "%[^,]s", tmpstr) < 1)
		    	goto display;
		    cpu_idle1 = atol(tmpstr);
		}

		str = strchr(str, ',');
		if (str == NULL) {
		    cpu_idle2 = -1;
		    goto display;
		}
		else {
		    str += 1;
		    if (sscanf(str, "%[^,]s", tmpstr) < 1)
		    	goto display;
		    cpu_idle2 = atol(tmpstr);
		}

display:
		if (debug > 0)
		    printf("Web100 variables line received\n\n");
		calculate(); 
	    }

	}

}
