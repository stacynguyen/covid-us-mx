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

public class ArchivoDiario {

	/*
		Wrapper para leer archivos de excesos de muerte del gobierno federal y CDMX
		Cifras por entidad de unidad medica de muerte      
		Casos y muertes ordenados por fecha de contagio
		El archivo del dia coincide exactamente con las cifras generadas por gobierno CDMX
		(luego ya no porque se agregan hacia atrás casos y muertes)
		
		Las funciones de lectura son un batidillo monumental.
		Abro una nueva serie para meter algo de orden. Basado en SimulaENSANUT
		
		PD. Tiene demasiada basura. El existente se va a ArchivoDiarioBack y el limpio se queda aqui
	*/
	
	static DecimalFormat df0 = new DecimalFormat("#0.00");
	static DecimalFormat df1 = new DecimalFormat("#0.00");
	static DecimalFormat df3 = new DecimalFormat("##,###,###");
	static DecimalFormat df4 = new DecimalFormat("#,##0");

	final static int TOTAL_MUNICIPIOS = PoblacionMunicipio.TOTAL_MUNICIPIOS;
	static int OFFSET_CASOS = 4;		//dias entre contagio y síntomas SE MODIFICAN EN EL CONSTRUCTOR // was 5, mod june 2021
	static int OFFSET_MUERTES = 18;		//dias entre contagio y muerte

	final static int RESIDENCIA = 0;	// param lectura
	final static int ENTIDAD = 1;		// param lectura
	
	final static int FECHA_OCURRE = 2;	// casos se registran dia sintomas, muertes fecha muerte
	final static int FECHA_CONTAGIO =3;	// casos y muertes se registran a fecha estimada contagio, default
	
	final static double EXCESO_NAL_2020 = 325502d;	// mis calculos
	final static double EXCESO_NAL_2021 = 339897d;	
	final static double EXCESO_CDMX_2020 = 42155d;	
	final static double EXCESO_CDMX_2021 = 51000d;	
	
	final static double POBL_CDMX  = 92.09944d;		// 100,000s, Census 2020
	final static double POBL_NAL = 126.014024d;		// millions

	String path = "C:\\java\\datos covid\\lag\\";

	int rr = 0;							// no registros en archivo
	int tt = 0;							// numero de casos positivos en TODO EL ARCHIVO
	int cc = 0;							// numero de casos positivos en rango fechas
	int dd = 0;							// numero de defunciones en rango
	int err = 0;						// no errores en lectura
	int hh = 0;							// no hospitalizados
	int hhm = 0;						// no hospitalizados con fecha defuncion
	
	int[][] casos = null;				// Casos x estado x dia de acuerdo fecha contagio
	int[][] muertes = null;				// Muertes x estado x dia de acuerdo fecha deceso
	int[] casos_e = null;				// Casos x estado 		
	int[] muertes_e = null;				// Muertes x estado
	int[][] casos_e_d = null;			// Casos x estado x dia
	int[][] muertes_e_d = null;			// Muertes x estado x dia
	int[] casos_g_e = null;				// Casos x grupo edad
	int[] muertes_g_e = null;			// Muertes x grupo edad
	int[][] muertes_e_s = null;			// Muertes x estado x semana
	int[] casos_d = null;				// Casos x dia 		
	int[] muertes_d = null;				// Muertes x dia

	// Idea. Coding like this ...
	static ArchivoDiario nu (String arg) throws Exception { return new ArchivoDiario(arg); }
	ArchivoDiario set(int i) { OFFSET_CASOS = i; return this; }
	ArchivoDiario lee() throws Exception { lee("path","file", 1, 1000, null, "ENTIDAD_RES"); return this; }

	// ... you can concatenate the calls JS-style
	// ArchivoDiario f0 = ArchivoDiario.nu("arg").set(7).lee();
	
	public static void main(String[] args) throws Exception { 
		
		ArchivoDiario f1 = new ArchivoDiario(args[0]);
		System.out.println("Numero de registros       = "+f1.rr());
		System.out.println("Numero de errores         = "+f1.err());
		System.out.println("Numero de casos positivos = "+f1.cc());
		System.out.println("Numero de muertes covid   = "+f1.dd());
		
	}	//

	// Muertes en dia i, promedio i + (w-1) dias anteriores
	static float promedio(int i, int w, int[] m) {
		float max = 0; float n = 0;
		for (int k=0; k<w; k++) if ((i-k)>=0) { max = max+m[i-k]; n++;  }
		return max/n;
	}	

	// MOD 1 MAR 2021:se agrego un parametro extra para fijar en el constructor el delay.
	// Esto va a romper el codigo que usa los constructores anteriores
	// Es a proposito: hay que revisar uso por uso de archivo diario y decidir si se usa con/sin delay
	// Ya era un peligro tener que recordar cada uso, editar el fuente y recompilar antes de cada calculo.
	// El uso "normal" deberia ser FECHA_CONTAGIO en el parametro delay.
	
	
	// Uso externo; nombre de archivo abreviado ("20111"); por entidad de fallecimiento
	// call to constructor must be 1st line, sorry
	public ArchivoDiario(String args0) throws Exception {
		this(args0,"2020-01-01","20" + args0.substring(0,2)+"-"+args0.substring(2,4)+"-"+args0.substring(4,6), 
		RESIDENCIA, // ENTIDAD, //
		FECHA_OCURRE);
	}
	
	public ArchivoDiario(String fileName, int ref, int delay) throws Exception {
		if (delay==FECHA_OCURRE) {	// casos se registran en fecha sintomas, muertes en dia de ocurrencia
			OFFSET_CASOS = 0;	
			OFFSET_MUERTES = 0;	
		}
		String[] header = hdic15;
		if (Integer.parseInt(fileName)<=201115) header=hnov15;	// aprox, no tengo idea
		fileName = fileName + "COVID19MEXICO.csv";
		if (ref==RESIDENCIA) 	leeResidencia(path,fileName,-1,-1,header);
		else					leeEntidad(path,fileName,-1,-1,header);
	}
	
	public ArchivoDiario(String fileName, String args0, String args1, int ref, int delay) throws Exception {
		if (delay==FECHA_OCURRE) {	// casos se registran en fecha sintomas, muertes en dia de ocurrencia
			OFFSET_CASOS = 0;	
			OFFSET_MUERTES = 0;	
		}
		String[] header = hdic15;
		if (Integer.parseInt(fileName)<=201115) header=hnov15;	// aprox, no tengo idea
		fileName = fileName + "COVID19MEXICO.csv";
		int liminf = dia_epi(args0);
		int limsup = dia_epi(args1);
		if (ref==RESIDENCIA) 	leeResidencia(path,fileName,liminf,limsup,header);
		else					leeEntidad(path,fileName,liminf,limsup,header);
	}

	public int rr() { return rr; }
	public int tt() { return tt; }
	public int cc() { return cc; }
	public int dd() { return dd; }
	public int total_muertes() { return dd; }
	public int err() { return err; }
	public int hh() { return hh; }
	public int hhm() { return hhm; }
 
	public int casos_estado(int e) 								{ return casos_e[e-1]; }
	public int casos_estado_dia(int e, int d) 					{ return casos_e_d[e-1][d]; }		// d==0, 1 ene 2020
	public int[] casos_estado_dia(int e) 						{ return casos_e_d[e-1]; }			// d==0, 1 ene 2020
	public int[] casos_grupo_edad() 							{ return casos_g_e; }
	public int casos_dia(int d) 								{ return casos_d[d]; }
	public int[] casos_dia()		 							{ return casos_d; }

	public int muertes_estado(int e) 							{ return muertes_e[e-1]; }
	public int muertes_estado_dia(int e, int d) 				{ return muertes_e_d[e-1][d]; }		// d==0, 1 ene 2020
	public int[] muertes_estado_dia(int e) 						{ return muertes_e_d[e-1]; }		// d==0, 1 ene 2020
	public int[] muertes_grupo_edad() 							{ return muertes_g_e; }
	public int[] muertes_estado_semana(int e)					{ return muertes_e_s[e-1]; }
	public int muertes_dia(int d) 								{ return muertes_d[d]; }
	public int[] muertes_dia() 									{ return muertes_d; }
	
	/*
		Lee no. casos COVID por municipio por dia, de acuerdo a fecha contagio entre [liminf, limsup]
		Lee no. muertes COVID por municipio por dia, de acuerdo a fecha contagio entre [liminf, limsup]
		Lee archivo "formato grande". 
		OJO. Header modificado en algun momento (!= nombre campos) Header actual (15 dic) es h15dic
		
		las cifras están verificadas y cazan perfecto con las de Informes Técnicos Diarios. PERO
		- si haces búsquedas con fechas de corte recuerda que tu estas cortando por fecha de infeccion,
		  ahí va a haber diferencias
		- los reportes del gobierno asignan defunciones POR LUGAR DE MUERTE. creo que tú necesitas más
		  por lugar de residencia. ahí también va a haber diferencias
		  
		Modded for 2021 too
		
		- re cifras diarias CDMX. Como cuadra, no cuadra. 1 ene me sale 22,773 y ese dia anunciaron 21,810
		En cambio, corriendo al dia 18 con archivo de dia 18, cuadra justo: 25,152
		
	*/

	// Lee por sitio de residencia
	public void leeResidencia(String path, String file, int liminf, int limsup, String[] header) throws Exception {
		lee(path, file, liminf, limsup, header, "ENTIDAD_RES");
	}

	// Lee por entidad de unidad medica 
	// OJO: no hay municipio de entidad médica. Los voy a asignar todos a 1er municipio
	public void leeEntidad(String path, String file, int liminf, int limsup, String[] header) throws Exception {
		lee(path, file, liminf, limsup, header, "ENTIDAD_UM");
	}

	// Se suprimieron aqui totales por municipio. Übertrieben. Ver ArchivoDiarioBack
	// Idem rollo hospitalizaciones
	
	public void lee(String path, String file, int liminf, int limsup, String[] header, String ref) throws Exception {
		BufferedReader in1 = new BufferedReader (new InputStreamReader (new FileInputStream(path+file),"UTF-8"));
		String data0 = null;
		String data1 = null;
		String[] data2 = null;
		// int iestado = 		field_is("ENTIDAD_RES",			header); // Entidad residencia	
		// int iestado = 		field_is("ENTIDAD_UM", header);  		// Entidad donde se trata caso	
		int iestado = field_is(ref, header);
		int iFechaSintomas = 	field_is("FECHA_SINTOMAS",		header);
		int iClasificacion = 	field_is("CLASIFICACION_FINAL",	header);
		int iDefuncion = 		field_is("FECHA_DEF",			header);
		int iEdad =		 		field_is("EDAD",				header);
		int iTipoPaciente =		field_is("TIPO_PACIENTE",		header);	// 1: ambulatorio, 2: hospitalizado
		int iUCI =				field_is("UCI",					header);	// 1: sí, 2: no
		int iFechaIngreso = 	field_is("FECHA_INGRESO",		header);
		int iFechaActuali = 	field_is("FECHA_ACTUALIZACION",	header);
		
		double suma0 =0; double suma1 =0;
		casos_e = new int[32];							// Casos x estado 		
		muertes_e = new int[32];						// Muertes x estado
		casos_e_d = new int[32][366+365+365];			// Casos x estado x dia
		muertes_e_d = new int[32][366+365+365];			// Muertes x estado x dia
		casos_g_e = new int[18];						// casos por grupo de e
		muertes_g_e = new int[18];						// muertes por grupo de edad
		muertes_e_s = new int[32][53+53+52];			// muertes por estado x semana
		casos_d = new int[366+365+365];					// Casos x dia 
		muertes_d = new int[366+365+365];				// Muertes x dia 
	
		data0=in1.readLine();								// Header, skip after calculating no.cols
		int num_cols = data0.split(",").length;				
		float dif1 = 0;
		while ( (data0=in1.readLine())!=null ) {

			try {
				data2 = data0.replace("Ex-U.R.S.S.,","Ex-URSS ").replace("\"","").split(",");
				if (data2.length!=num_cols) throw new Exception("Wrong length");
				rr++;										// no registers

				int e = Integer.parseInt(data2[iestado]);	// Comienza en 1
				int se0 = dia_epi(data2[iFechaSintomas]);				
				int se = se0-OFFSET_CASOS;					// Fecha contagio; Uso fecha sintoma como referencia; se de 0
				int edad = Integer.parseInt(data2[iEdad]);
				int q = Math.min(edad/5,17);				// grupo quinquenal
				int sf = -1;
				try { sf = dia_epi(data2[iDefuncion])-OFFSET_MUERTES; } catch (Exception e1) { } // OR usar fecha contagio
				
				if (!sars_o_covid(file,data2,iClasificacion)) continue;	// solo casos positivos
				tt++;
				
				if (liminf!=-1 && se<liminf) continue;
				if (limsup!=-1 && se>limsup) continue;

				casos_e[e-1]++;
				casos_e_d[e-1][se]++;
				casos_g_e[q]++;
				casos_d[se]++;
				cc++;
				
				if ( (sf!=-1) && (liminf==-1 || sf>=liminf) && (limsup==-1 || sf<=limsup) ) {
					
					int s = semana_epi_menos_n(data2[iDefuncion],OFFSET_MUERTES);	// REAL comienza en 1
					muertes_e[e-1]++;
					muertes_e_d[e-1][sf]++;
					muertes_g_e[q]++;
					muertes_e_s[e-1][s-1]++;
					muertes_d[sf]++;
					dd++;
					
				}

 readF			} catch (Exception e0) { 
				// Municipios fuera rango?
				// System.out.println("--------------"+e0);
				// System.out.println(data0);
				// System.out.println();
				err++; 
			}
				
		}

	in1.close();

	} //ile2


	static int field_is(String id, String[] h) { for (int i=0; i<h.length; i++) if (h[i].equals(id)) return i; return -1; }

	// Good for nov30; not for nov11
	static String[] hdic15 = {	// header nuevo archvo grande
		"FECHA_ACTUALIZACION","ID_REGISTRO","ORIGEN","SECTOR","ENTIDAD_UM","SEXO","ENTIDAD_NAC","ENTIDAD_RES",
		"MUNICIPIO_RES","TIPO_PACIENTE","FECHA_INGRESO","FECHA_SINTOMAS","FECHA_DEF","INTUBADO","NEUMONIA",
		"EDAD","NACIONALIDAD","EMBARAZO","HABLA_LENGUA_INDIG","INDIGENA","DIABETES","EPOC","ASMA","INMUSUPR",
		"HIPERTENSION","OTRA_COM","CARDIOVASCULAR","OBESIDAD","RENAL_CRONICA","TABAQUISMO","OTRO_CASO",
		"TOMA_MUESTRA_LAB","RESULTADO_LAB","TOMA_MUESTRA_ANTIGENO","RESULTADO_ANTIGENO",
		"CLASIFICACION_FINAL","MIGRANTE","PAIS_NACIONALIDAD","PAIS_ORIGEN","UCI"
	};

	static String[] hnov15 = {		
		"FECHA_ACTUALIZACION","ID_REGISTRO","ORIGEN","SECTOR","ENTIDAD_UM","SEXO","ENTIDAD_NAC","ENTIDAD_RES",
		"MUNICIPIO_RES","TIPO_PACIENTE","FECHA_INGRESO","FECHA_SINTOMAS","FECHA_DEF","INTUBADO","NEUMONIA",
		"EDAD","NACIONALIDAD","EMBARAZO","HABLA_LENGUA_INDIG","INDIGENA","DIABETES","EPOC","ASMA","INMUSUPR",
		"HIPERTENSION","OTRA_COM","CARDIOVASCULAR","OBESIDAD","RENAL_CRONICA","TABAQUISMO","OTRO_CASO",
		"TOMA_MUESTRA","RESULTADO_LAB",
		"CLASIFICACION_FINAL","MIGRANTE","PAIS_NACIONALIDAD","PAIS_ORIGEN","UCI"
	};

	// 7 de octubre cambio el formato y se incluyeron nuevos campos
	static boolean sars_o_covid(String fileName, String[] data2, int iClasificacionFinal ) {
		int ano = Integer.parseInt(fileName.substring(0,2));
		int mes = Integer.parseInt(fileName.substring(2,4));
		int dia = Integer.parseInt(fileName.substring(4,6));
		if ( ano==2020 && ((mes<10) || (mes==10 && dia<7) )) {
			int iResultado = 30;									// formato viejo
			int rs = Integer.parseInt(data2[iResultado]);			// formato 1
			return (rs==1);
		} else {
			int cf = Integer.parseInt(data2[iClasificacionFinal]);	// formato 2
			return (cf==1 || cf==2 || cf==3) ;						// 1: covid confirmado x asociacion clinina epidem
		}															// 2: covid confirmado por comite
	}																// 3: covid confirmado por lab
																	// 4: invalido, 5: no realizado, 6: pendiente, 7: negativo

	// static void calendar_init() { FechasNuevaSerie.calendar_init(); }

	// calcula dia epidemiologico, a partir de 0 = 1 ene 2020
	static int dia_epi(String fecha) throws Exception { return FechasNuevaSerie.dia_epi(fecha); }

	// calcula semana epidemiologica REAL, a partir de 1
	static int semana_epi(String fecha) throws Exception { return FechasNuevaSerie.semana_epi(fecha); }	

	// Mod para retroceder 5 dias antes de fecha de sintomas para estimar contagio
	static int semana_epi_menos_n(String fecha, int n) throws Exception { return FechasNuevaSerie.semana_epi_menos_n(fecha,n); }	
	
	static String fecha_dia_epi(int fecha) throws Exception { return FechasNuevaSerie.fecha_dia_epi(fecha); }


}
