import java.io.*;
import java.util.*;
import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.text.DecimalFormat;
import java.io.File;
import java.time.*;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.text.DecimalFormat;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class FechasNuevaSerie {

	/*
		Origen: LeeArchivoExcesos
		Cuidado: hay otra familia de funciones, mejor hecha, en la serie vieja
	*/

	public static void main(String[] args) throws Exception {
		//calendar_init();
		//System.out.println(Arrays.toString(offset_days));
		//System.out.println(dia_epi(args[0])+" :"+semana_epi(args[0]));
		//System.out.println(fecha_dia_epi(Integer.parseInt(args[1])));
		
		/*
		// OK
		for (int i=0; i<426; i++) {
			String f = fecha_dia_epi(i);
			int j = dia_epi(f);
			int s = semana_epi(f);
			System.out.println(i+"\t"+f+"\t"+j+"\t"+s);
		}
		*/
		
		System.out.println(dia_epi(args[0])+",\t SE="+semana_epi(args[0])+",\t SE="+fecha_dia_epi(dia_epi(args[0])));
		
	}
	
	/*
	Semanas epidemiologicas
	Comienzan en domingo menos 1a
	1	1- 4	ene			docs oficiales ponen semana 1 a partir del 1o, CHECK
	2	5-11	ene	
	3	12-18	ene
	4	19-25	ene
	5	26 ene - 1 feb
	6	2-8 	feb
	7	9		feb
	8	16		feb
	9	23 feb-29 mar
	10	1-7		mar
	11	8-14	mar
	12	15-21	mar
	13	22-28	mar
	14	29 mar-4 abr
	15	5-11	abr
	16	12-18	abr
	17	19-25	abr
	18	26 abr-2 may
	19	3-9		may
	20	10-16	may
	21	17-23 	may
	22	24-30	may
	23	31 may-6 jun
	24	7-13	jun
	25	14-20	jun
	26	21-27	jun
	27	28 jun-4 jul    
	28	5-11 	jul      ---------------------
	29	12-18	jul
	30	19-25	jul
	31	26 jul-1 ago
	32	2-8		ago
	33	9-15	ago
	34	16-22	ago
	35	23-29	ago
	36	30 ago-5 sept
	37	6-12	sept
	38	13-19	sept
	39	20-26	sept
	40	27 sept-3 oct
	41	4-10	oct
	42	11-17	oct
	43	18-24	oct
	44	25-31	oct   
	45  1-7 nov
	46	-14
	47	-21
	48	-28
	49	-5 dic
	50	-12
	51	-19
	52	-26
	53	1	27 dic	- 2 ene		2021
	54	2	-9
	55	3	-16
	56	4	-23
	57	5	-30
	*/	
	
	// Modded for 2021
	// Modded for 2022
	
	static int[] offset_days = null;
	static int[] days_month = null;
	
	static int offset(int i) { if (offset_days==null) calendar_init(); return offset_days[i]; }
	
	static void calendar_init() {
		offset_days = new int[36];	// accumulates offset for 1st day of each month
		days_month = new int[36];	// no days each month 
		int c = 0;
		for (int i=0; i<36; i++) { 
			offset_days[i]=c;
			Calendar cal = new GregorianCalendar(2020+i/12, i%12, 1);
			days_month[i] = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
			c=c+days_month[i];
		}
	}
	
	// calcula dia epidemiologico, a partir de 0 = 1 ene 2020
	// para calculos internos, dias van corridos: 2020-12-31 es 365 y 2021-01-01 es 366 
	static int dia_epi(String fecha) throws Exception { // AAAA-MM-DD
		String[] s = fecha.split("-");
		int a = Integer.parseInt(s[0]);
		int m = Integer.parseInt(s[1]);
		int d = Integer.parseInt(s[2]);
		int dn = offset(12*(a-2020)+m-1)+d-1;		// day number, from 0
		return dn;
	}
	
	// De dia epi a fecha
	static String fecha_dia_epi(int dn) throws Exception { 
		int i=0; 
		while(i<36 && offset(i+1)<=dn) i++; 
		if (i==36) throw new Exception ("fecha_dia_epi: dn="+dn+" out of range");
		String a = "2022";
		if (i<24) a = "2021";
		if (i<12) a = "2020";
		String m = "0"+(i%12+1);			if (m.length()>2) m = m.substring(1,3);	
		String d = "0"+(dn-offset(i)+1);	if (d.length()>2) d = d.substring(1,3);	
		return a+"-"+m+"-"+d;	
	}
		

	// calcula semana epidemiologica REAL, a partir de 1
	// como dias, va corrida
	static int semana_epi(String fecha) throws Exception { // AAAA-MM-DD		
		return 1+(dia_epi(fecha)+3)/7;					// real days 1-4 week 1, 5-11 week 2, 12-18 week 3, etc
	}

	// Mod para retroceder 5 dias antes de fecha de sintomas para estimar contagio
	static int semana_epi_menos_5(String fecha) throws Exception { return semana_epi_menos_n(fecha,5);	}
	
	// Mod para retroceder n dias antes de fecha de sintomas para estimar contagio
	static int semana_epi_menos_n(String fecha, int n) throws Exception { // AAAA-MM-DD		
		return 1+(dia_epi(fecha)+3-n)/7;					// real days 1-4 week 1, 5-11 week 2, 12-18 week 3, etc
	}

	// En qué mes está cada semana, i:1...52
	static int mes_de_semana_epid(int i, int ano) {
		if (ano!=2020) { System.out.println("mes_de_semana_epid, solo 2020"); System.exit(0); }
		String f = semanas_epid_2020[i-1];
		int m = Integer.parseInt(f.substring(5,7));
		return m;
	}
	
	static String[] semanas_epid_2020 =	{

	"2020-01-04",	 // sem epid 1 termina en dia	
	"2020-01-11",	 // sem epid 2 
	"2020-01-18",	 // sem epid 3 
	"2020-01-25",	 // sem epid 4 
	"2020-02-01",	 // sem epid 5 
	"2020-02-08",	 // sem epid 6 
	"2020-02-15",	 // sem epid 7 
	"2020-02-22",	 // sem epid 8 
	"2020-02-29",	 // sem epid 9 
	"2020-03-07",	 // sem epid 10
	"2020-03-14",	 // sem epid 11
	"2020-03-21",	 // sem epid 12	
	"2020-03-28",	 // sem epid 13
	"2020-04-04",    // sem epid 14	
	"2020-04-11",    // sem epid 15	
	"2020-04-18",    // sem epid 16	
	"2020-04-25",    // sem epid 17	
	"2020-05-02",    // sem epid 18	
	"2020-05-09",    // sem epid 19	
	"2020-05-16",    // sem epid 20	
	"2020-05-23",    // sem epid 21	
	"2020-05-30",    // sem epid 22	
	"2020-06-06",    // sem epid 23	
	"2020-06-13",    // sem epid 24	
	"2020-06-20",    // sem epid 25	
	"2020-06-27",    // sem epid 26	
	"2020-07-04",    // sem epid 27	
	"2020-07-11",    // sem epid 28	
	"2020-07-18",    // sem epid 29	
	"2020-07-25",    // sem epid 30	~
	"2020-08-01",    // sem epid 31	
	"2020-08-08",    // sem epid 32	
	"2020-08-15",    // sem epid 33	
	"2020-08-22",    // sem epid 34	
	"2020-08-29",    // sem epid 35	
	"2020-09-05",    // sem epid 36	
	"2020-09-12",    // sem epid 37	
	"2020-09-19",    // sem epid 38	
	"2020-09-26",    // sem epid 39	
	"2020-10-03",    // sem epid 40	
	"2020-10-10",    // sem epid 41	
	"2020-10-17",    // sem epid 42	
	"2020-10-24",    // sem epid 43	
	"2020-10-31",    // sem epid 44	
	"2020-11-07",	// 45
	"2020-11-14",	// 46
	"2020-11-21",	// 47
	"2020-11-28",    // 48
	"2020-12-05",	// 49
	"2020-12-12",	// 50
	"2020-12-19",	// 51
	"2020-12-26",	// 52
	"2021-01-02",		//53
	"2021-01-09",		// 1
	"2021-01-16",		// 2
	"2021-01-23",		// 3
	"2021-01-30",		// 4
	"2021-02-06",		// 5
	"2021-02-13",		// 6
	"2021-02-20",		// 7
	"2021-02-27",		// 8
	"2021-03-06",		// 9
	"2021-03-13",		// 10
	"2021-03-20",		// 11
	"2021-03-27",		// 12
	"2021-04-03",		// 13
	"2021-04-10",		// 14
	"2021-04-17",		// 15
	"2021-04-24",		// 16
	"2021-05-01",		// 17
	"2021-05-08",		// 18
	"2021-05-15",		// 19
	"2021-05-22",		// 20
	"2021-05-29",		// 21
	"2021-06-05",		// 22
	"2021-06-12",		// 23
	"2021-06-19",		// 24
	"2021-06-26",		// 25
	"2021-07-03",		// 26
	"2021-07-10",		// 27
	"2021-07-17",		// 28
	"2021-07-24",		// 29
	"2021-07-31",		// 30
	"2021-08-07",		// 31
	"2021-08-14",		// 32
	"2021-08-21",		// 33
	"2021-08-28",		// 34
	"2021-09-04",		// 35
	"2021-09-11",		// 36
	"2021-09-18",		// 37
	"2021-09-25",		// 38
	"2021-10-02",		// 39
	"2021-10-09",		// 40
	"2021-10-16",		// 41
	"2021-10-23",		// 42
	"2021-10-30",		// 43
	"2021-11-06",		// 44
	"2021-11-13",		// 45
	"2021-11-20",		// 46
	"2021-11-27",		// 47
	"2021-12-04",		// 48
	"2021-12-11",		// 49
	"2021-12-18",		// 50
	"2021-12-25",		// 51
	"2022-01-01",		// 52
	"2022-01-08",		// 01 2022
	"2022-01-15",		// 02
	"2022-01-22",		// 03
	"2022-01-29",		// 04
	"2022-02-05",		// 05
	"2022-02-12",		// 06
	"2022-02-19",		// 07
	"2022-02-26",		// 08
	"2022-03-05",		// 09
	"2022-03-12",		// 10
	"2022-03-19",		// 11
	"2022-03-26",		// 12
	"2022-04-02",		// 13
	"2022-04-09",		// 14
	"2022-04-16",		// 15
	"2022-04-23",		// 16
	"2022-04-30",		// 17
	"2022-05-07",		// 18
	"2022-05-14",		// 19
	"2022-05-21",		// 20
	"2022-05-28",		// 21
	"2022-06-04",		// 22
	"2022-06-11",		// 23
	"2022-06-18",		// 24
	"2022-06-25",		// 25
	"2022-07-02",		// 26
	"2022-07-09",		// 27
	"2022-07-16",		// 28
	"2022-07-23",		// 29
	"2022-07-30",		// 30
	"2022-08-06"		// 31	
	};
	
}
